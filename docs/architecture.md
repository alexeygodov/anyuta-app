# Карта проекта

Это указатель для точечного чтения кода. Версии зависимостей — в Gradle, текущее
пользовательское поведение — в README, история — в CHANGELOG.

## Стек и запуск

- Один Android-модуль `app`, Kotlin и Jetpack Compose, namespace `ru.family.rasti`.
- `MainActivity.kt`: тема, разрешения, жизненный цикл, действия из виджетов.
- `ui/RastiApp.kt`: навигация Сегодня / История / Графики / Настройки;
  при проблемах с локальными данными показывает `StorageRecoveryScreen`.
- `RastiViewModel.kt`: состояние экранов, ввод/редактирование/удаление, запуск sync.

Все пути Kotlin ниже относятся к `app/src/main/java/ru/family/rasti/`.

## Где искать изменение

| Область | Реализация | Тесты в `app/src/test/java/ru/family/rasti/` |
| --- | --- | --- |
| Сегодня, кормления, сон, витамин D | `ui/TodayScreen.kt`, `ui/EntryDialogs.kt`, `ui/BottleAmountPicker.kt` | `ui/`, `feeding/`, `sleep/` |
| История | `ui/HistoryScreen.kt` | `data/`, `sleep/` |
| Графики и развитие | `ui/ChartsScreen.kt`, `ui/ChartsLlmExport.kt`, `ui/*Chart.kt`, `ui/DevelopmentCalendarCard.kt`, `growth/GrowthStandards.kt` | `ui/*ChartTest.kt`, `ui/ChartsLlmExportTest.kt`, `ui/MeasuredGrowthTest.kt`, `ui/DevelopmentCalendarTest.kt` |
| Данные, JSON, настройки | `data/Models.kt`, `data/JsonCodec.kt`, `data/LocalStore.kt`, `data/JournalFile.kt` | `data/` |
| Синхронизация | `sync/GitHubSync.kt`, `sync/GitHubTransport.kt`, `RastiViewModel.sync` | `sync/` |
| Виджеты | `widget/`, цвета состояний `widget/WidgetColors.kt`, палитра `res/values/widget_colors.xml`, XML в `res/layout/widget_*.xml` и `res/xml/anyuta_*widget_info.xml` | `widget/` |
| Уведомления и MAX | `notify/`, `max/MaxBotApi.kt` | `notify/` |
| Обновления APK | `update/AppUpdater.kt` | `update/AppUpdaterTest.kt` |
| Цвета и шрифты | `ui/theme/RastiTheme.kt`, `ui/DesignComponents.kt` | `ui/ThemeContrastTest.kt` |
| Время мигания витамина D | `ui/VitaminDReminder.kt`: условие, ближайшая граница, lifecycle и системные события времени | `ui/VitaminDReminderTest.kt`, `ui/VitaminDReminderUiTest.kt` |

## Поток данных

```text
Экран / действие виджета → RastiViewModel → LocalStore → JournalFile
                                  ↓                     ↓
                             GitHubSync             все виджеты
                                  ↓
                         GitHubTransport → GitHub API
                                  ↓
                      merge с текущим состоянием → LocalStore
```

`ReminderWorker` отдельно читает LocalStore, проверяет напоминания и выполняет
фоновую синхронизацию. После сети он повторно читает локальные данные перед merge.
ViewModel объединяет результат запроса с текущим `data`, а не только со снимком
до запроса. Эти правила защищают изменения, сделанные во время ожидания сети.

## Хранение и восстановление

- `filesDir/rasti-data.json` — весь дневник; JSON version 4, `profile` и `days`.
- `rasti-data.backup.json` — предыдущая проверенная локальная версия. Повторное
  сохранение одинакового содержимого не вытесняет её.
- `JournalFile` использует Android AtomicFile и общий замок для операций с дневником
  внутри процесса. Это не транзакция между телефоном и GitHub и не межпроцессный lock.
- Только отсутствие основного файла, резервной копии и их атомарных файлов означает
  новый дневник. Ошибка чтения/структуры не превращается в успешный пустой результат.
- При восстановлении повреждённый оригинал копируется в `rasti-data.corrupt-UUID.json`.
  Маркер `rasti-data.recovered` сохраняет уведомление до подтверждения в приложении.
- До подтверждения восстановления или при ошибке чтения ввод и синхронизация
  приостановлены; виджеты предлагают открыть приложение, worker не отправляет данные.
- Локальная копия не заменяет внешнюю резервную копию: удаление данных приложения
  удаляет и её. Восстановленная версия может не содержать последние изменения.
- `rasti-sync-state.json` — восстанавливаемый кеш ETag, SHA файлов и timestamps;
  потеря кеша ведёт к повторному чтению удалённых данных.
- Настройки — SharedPreferences; GitHub и MAX-токены — отдельные SecureTokenStore
  с AES/GCM и Android Keystore. В JSON дневника токены не записываются.

## Протокол синхронизации

- Отдельный приватный data-репозиторий: `profile.json` и `data/YYYY/MM/YYYY-MM-DD.json`.
- Чтение дерева с ETag; HTTP 304 повторно использует известные SHA. Неизменённые дни
  не скачиваются и не отправляются снова.
- Записи объединяются по ID и `updatedAt`. Множества удалённых ID объединяются,
  затем удалённые записи исключаются. У замера есть отдельный timestamp удаления;
  у отметки беспокойства — собственный timestamp.
- Профиль выбирается по timestamp с учётом профиля-заглушки «Малыш».
- Дневной PUT при HTTP 409 перечитывает файл, объединяет данные и делает один повтор.
  Второй конфликт возвращает ошибку. Профиль при 409 также перечитывается и выбирается по timestamp с учётом профиля-заглушки; если удалённый профиль новее, повторная запись не нужна.
- `GitHubTransport` позволяет тестировать порядок HTTP-ответов без сети и токенов.
- В открытом приложении интервал 5 секунд; параллельный запрос в ViewModel ставит
  флаг повторной синхронизации. Worker запланирован через WorkManager на 15 минут;
  Android определяет фактическое время запуска.

## Проверки и дальнейшее разделение

Команды, визуальные проверки и ограничения окружения — в [testing.md](testing.md).
При доработке крупных UI-файлов выносить самостоятельные карточки/диалоги с явными
параметрами. Не переносить историю задачи в код и не разбивать файлы только по числу строк.
Новые существенные решения описывать здесь кратко и вместе с изменением реализации.
