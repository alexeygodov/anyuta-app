[CmdletBinding()]
param(
    [ValidateSet('Doctor', 'Targeted', 'Full')]
    [string]$Mode = 'Full',
    [string[]]$Tests = @(),
    [string]$AndroidSdk,
    [string]$GradleCache,
    [string]$BuildRoot,
    [switch]$Offline,
    [switch]$WindowsSocketFallback,
    [switch]$RecordScreenshots
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$logDirectory = Join-Path $projectRoot '.local/checks'
$originalLocation = Get-Location
$originalAndroidHome = $env:ANDROID_HOME
$originalGradleCache = $env:GRADLE_USER_HOME
$originalJavaOptions = $env:JAVA_TOOL_OPTIONS
$originalErrorPreference = $ErrorActionPreference
$exitCode = 1

try {
    Set-Location -LiteralPath $projectRoot
    if ($Mode -eq 'Targeted' -and $Tests.Count -eq 0) {
        throw 'Targeted requires -Tests, for example ru.family.rasti.sync.*'
    }
    if ($Mode -ne 'Targeted' -and $Tests.Count -gt 0) {
        throw '-Tests is only supported with -Mode Targeted.'
    }
    if ($RecordScreenshots -and ($Mode -ne 'Targeted' -or $env:CI -eq 'true')) {
        throw 'Screenshot recording is allowed only in local Targeted mode.'
    }
    $javaCommand = if ($env:JAVA_HOME) {
        Join-Path $env:JAVA_HOME 'bin/java.exe'
    } else {
        (Get-Command java -ErrorAction Stop).Source
    }
    if (-not (Test-Path -LiteralPath $javaCommand)) { throw 'JDK not found. Set JAVA_HOME to JDK 17.' }
    $javaOutput = (& $javaCommand -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $javaOutput -notmatch 'version "17[.\"]') {
        throw 'This project uses JDK 17. Set JAVA_HOME and retry.'
    }

    $sdkCandidates = @($AndroidSdk, $env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
    $localProperties = Join-Path $projectRoot 'local.properties'
    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) { $sdkCandidates += ($sdkLine.Substring(8) -replace '\\:', ':' -replace '\\\\', '\') }
    }
    if ($env:LOCALAPPDATA) { $sdkCandidates += Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
    $selectedSdk = $sdkCandidates | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'platforms')) } | Select-Object -First 1
    if (-not $selectedSdk) { throw 'Android SDK not found. Pass -AndroidSdk or set ANDROID_HOME.' }
    $buildScript = Get-Content -LiteralPath 'app/build.gradle.kts' -Raw
    if ($buildScript -notmatch 'compileSdk\s*=\s*(\d+)') { throw 'Cannot find compileSdk in app/build.gradle.kts.' }
    $sdkVersion = $Matches[1]
    $platforms = Get-ChildItem -LiteralPath (Join-Path $selectedSdk 'platforms') -Directory
    if (-not ($platforms | Where-Object { $_.Name -match "^android-$sdkVersion(\.0)?$" })) {
        throw "Android platform $sdkVersion missing in $selectedSdk."
    }
    if (-not (Test-Path -LiteralPath 'gradle/wrapper/gradle-wrapper.jar')) { throw 'Gradle wrapper JAR is missing.' }
    $env:ANDROID_HOME = $selectedSdk
    if ($GradleCache) { $env:GRADLE_USER_HOME = $GradleCache }
    if ($WindowsSocketFallback) {
        # JDK falls back to TCP when it cannot bind a Unix-domain socket in this directory.
        $unavailableSocketPath = Join-Path $projectRoot '.local/disabled-unix-sockets'
        if (Test-Path -LiteralPath $unavailableSocketPath) { throw 'Socket fallback requires .local/disabled-unix-sockets to be absent.' }
        $env:JAVA_TOOL_OPTIONS = ($originalJavaOptions + ' -Djdk.net.unixdomain.tmpdir="' + $unavailableSocketPath + '"').Trim()
    }
    Write-Host "Environment OK: JDK 17, Android $sdkVersion."
    if ($Mode -eq 'Doctor') {
        $exitCode = 0
    } else {
        New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
        $logPath = Join-Path $logDirectory ("{0}-{1}-{2}.log" -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $PID, $Mode)
        $gradleArguments = @('--console=plain', '--warning-mode=summary')
        if ($Offline) { $gradleArguments += '--offline' }
        if ($BuildRoot) { $gradleArguments += "-PanyutaBuildRoot=$BuildRoot" }
        if ($RecordScreenshots) { $gradleArguments += '-PrecordScreenshots=true' }
        if ($Mode -eq 'Targeted') {
            $gradleArguments += ':app:testDebugUnitTest'
            foreach ($testPattern in $Tests) { $gradleArguments += @('--tests', $testPattern) }
        } else {
            $gradleArguments += @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug')
        }
        Write-Host "Running $Mode checks. Full log: $logPath"
        # Windows PowerShell treats native stderr as ErrorRecord, even for normal Gradle output.
        $ErrorActionPreference = 'Continue'
        & (Join-Path $projectRoot 'gradlew.bat') @gradleArguments *> $logPath
        $exitCode = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        if ($exitCode -eq 0) {
            $reportsPath = if ($BuildRoot) { Join-Path $BuildRoot 'app/reports' } else { 'app/build/reports' }
            Write-Host "PASS: $Mode. Reports: $reportsPath."
        } else {
            Write-Host "FAIL: $Mode (exit $exitCode). Last 60 log lines:"
            Get-Content -LiteralPath $logPath -Tail 60 | Write-Host
        }
    }
} catch {
    Write-Host "FAIL: $($_.Exception.Message)"
    $exitCode = 1
} finally {
    $env:ANDROID_HOME = $originalAndroidHome
    $env:GRADLE_USER_HOME = $originalGradleCache
    $env:JAVA_TOOL_OPTIONS = $originalJavaOptions
    Set-Location -LiteralPath $originalLocation.Path
    $ErrorActionPreference = $originalErrorPreference
}
exit $exitCode
