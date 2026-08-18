# Анюта

Небольшое Android-приложение для семейного учёта питания, витаминов, роста и веса ребёнка. Работает без собственного сервера: данные сначала сохраняются на телефоне, а по кнопке синхронизируются с отдельным приватным GitHub-репозиторием.

## Что уже есть

- питание и питьё с количеством и временем;
- отметки о витаминах;
- рост, вес и заметка за день;
- история и короткая сводка за 7 дней;
- графики роста и веса с коридором `−2…+2 SD` WHO для возраста 0–5 лет;
- ручная двусторонняя синхронизация через GitHub Contents API;
- объединение записей двух телефонов и повтор записи при конфликте;
- token хранится зашифрованным ключом Android Keystore;
- сборка APK в GitHub Actions.

Приложение не является медицинским изделием и не ставит диагнозы.

## Репозитории

Рекомендуется использовать два репозитория:

1. `anyuta-app` — этот проект, только исходники.
2. `anyuta-data` — приватный репозиторий с семейными данными.

Создайте `anyuta-data` как **Private repository** и включите создание `README`. После первой синхронизации структура будет такой:

```text
profile.json
data/
  2026/
    08/
      2026-08-18.json
```

## Настройка доступа к данным

На GitHub откройте:

```text
Settings → Developer settings → Personal access tokens → Fine-grained tokens
```

Для token задайте:

- Repository access: только `anyuta-data`;
- Repository permissions → Contents: `Read and write`;
- разумный срок действия.

В приложении откройте «Настройки», введите владельца, имя data-репозитория, ветку `main` и token. У каждого взрослого желательно иметь свой token. Не добавляйте token в исходники, JSON или GitHub Actions secrets.

## Получение APK без Android Studio

1. Загрузите исходники в репозиторий `anyuta-app`.
2. Откройте вкладку **Actions**.
3. Выберите **Build Android APK** → **Run workflow**.
4. После завершения скачайте artifact `anyuta-...` и распакуйте APK.

Без настроенной подписи workflow создаёт debug APK. Он устанавливается на телефон, но APK из разных запусков может иметь разные ключи, поэтому для обновлений настройте постоянную release-подпись.

## Постоянная подпись APK

Сгенерируйте ключ один раз и сохраните резервную копию вне GitHub:

```powershell
keytool -genkeypair -v -keystore anyuta-release.jks -alias anyuta -keyalg RSA -keysize 2048 -validity 10000
```

Получите Base64 и скопируйте его в буфер:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("anyuta-release.jks")) | Set-Clipboard
```

В `anyuta-app → Settings → Secrets and variables → Actions` добавьте:

- `SIGNING_KEY_BASE64` — содержимое буфера;
- `KEYSTORE_PASSWORD`;
- `KEY_ALIAS` — обычно `anyuta`;
- `KEY_PASSWORD`.

Следующий workflow соберёт подписанный release APK. Файл `anyuta-release.jks` и пароли нельзя коммитить. Если ключ потерять, Android не позволит устанавливать новые версии поверх старой.

## Локальная сборка

Нужны JDK 17 и Android SDK 37:

```powershell
.\gradlew.bat assembleDebug
```

APK появится в `app/build/outputs/apk/debug/`.

## Справочные данные WHO

CSV в `app/src/main/assets/who/` получены из официальных расширенных таблиц WHO:

- https://www.who.int/tools/child-growth-standards/standards/length-height-for-age
- https://www.who.int/tools/child-growth-standards/standards/weight-for-age

Скрипт `tools/extract_who.py` оставлен для воспроизводимого повторного преобразования исходных XLSX.

## Приватность

Данные в data-репозитории хранятся в обычном JSON и видны всем, у кого есть доступ к приватному репозиторию. Не используйте публичный data-репозиторий. Для первой версии желательно указывать псевдоним ребёнка и не хранить медицинские документы или фотографии.
