plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}

// Optional Windows workaround for JDK/Gradle test workers and non-ASCII output paths.
providers.gradleProperty("anyutaBuildRoot").orNull?.let { outputRoot ->
    allprojects {
        layout.buildDirectory.set(file(outputRoot).resolve(name))
    }
}
