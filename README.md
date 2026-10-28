# Yoga — 30 дней (Android, Kotlin + Jetpack Compose)

Полностью автономное приложение для занятий йогой с 30-дневным планом, таймером на позу, аудио-сопровождением и кастомизацией последовательностей. Built to spec от `ТЗ йога`.

> Kotlin 2.0.21, Compose Material 3, Hilt, Room, DataStore. compileSdk=35, targetSdk=35, minSdk=26.
> **100% офлайн** — ни одного сетевого вызова в коде приложения.

---

## 1. Что это

Йога-приложение для B2C, с упором на ежедневную практику и медитативный UX.

### Ключевые фичи (соответствие ТЗ)

| Требование ТЗ | Реализация |
|---------------|------------|
| **План на 30 суток с упражнениями** | `GenerateProgramUseCase` строит 30-дневный план по выбранному уровню (Новичок/Средний/Продвинутый). Каждый день 5–7 поз, нарастающая сложность по неделям. |
| **Настройка/изменение позиций** | `DayScreen` с drag-to-reorder (long-press + drag), кнопкой Swap (открывает `LibraryScreen` в picker mode, замена сохраняется через SavedStateHandle), кнопкой Delete (удаление позы из дня), Skip Day (с auto-shift плана). |
| **Настройка времени каждого упражнения** | Slider прямо в карточке позы в DayDetail — задаёт `durationOverrideSec` для конкретной позы. Глобальная длительность — Slider в Settings (`settings_global_duration`). Приоритет: per-pose override > globalDuration > pose.defaultDurationSec. |
| **Звуковое сопровождение описаний** | `PoseAudioPlayer` интерфейс с двумя impl: `TtsPoseAudioPlayer` (по умолчанию, Android TextToSpeech на ru) и `Mp3PackPoseAudioPlayer` (опциональный аудио-пак). В SessionPlayer каждая поза автоматически озвучивается при переходе + кнопка «Прослушать снова». |

### Дополнительные фичи

- **40 асан** в библиотеке (15 Beginner + 15 Intermediate + 10 Advanced) с санскрит/рус/англ названиями, описаниями и PoseSpec.
- **PoseFigure Canvas** — один параметризованный Compose-компонент рисует все 40 поз через углы суставов (никаких PNG-ассетов, минимальный APK).
- **Meditative dark theme** — глубокий indigo + золото по умолчанию; light/dynamic как опции.
- **Полная автономия** — без INTERNET permission, без Retrofit-вызовов, без подписок, без аналитики.
- **Offline-first** — все данные в Room (poses, days, program, progress) + DataStore (settings).
- **UDF** — каждый экран: immutable UiState, sealed Event, Channel для эффектов.
- **Локализации**: ru (по умолчанию) + en.
- **A11y**: contentDescription, тач-таргеты ≥48dp, поддержка fontScale до 200%.

## 2. Стек

| Слой | Технологии |
|------|------------|
| UI | Jetpack Compose + Material 3 + Navigation Compose 2.8 (type-safe) |
| Arch | Single-module MVVM + UDF (immutable UiState, sealed Event, Channel Effect) |
| DI | Hilt 2.52 |
| Persistence | Room 2.6.1 + DataStore (Preferences) 1.1.1 |
| Async | Coroutines 1.9.0 + Flow |
| Audio | Android TextToSpeech + MediaPlayer (для опционального MP3 pack) |
| Logging | Timber (debug only) |
| Tests | JUnit5 + Turbine + MockK + Robolectric |
| Quality | detekt + ktlint |
| Build | Gradle 8.7+ Kotlin DSL, version catalog `libs.versions.toml` |

## 3. Структура проекта

```
android-app/
├── settings.gradle.kts
├── build.gradle.kts                          (root)
├── gradle.properties
├── gradle/libs.versions.toml                (version catalog)
├── config/detekt.yml
├── .gitignore
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    ├── schemas/                              (Room JSON schema exports)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/yogaapp/
        │   │   ├── YogaApp.kt                 (Application)
        │   │   ├── MainActivity.kt
        │   │   ├── data/
        │   │   │   ├── audio/                  (PoseAudioPlayer, TtsPlayer, Mp3PackPlayer)
        │   │   │   ├── datastore/              (SettingsDataStore, ThemeMode, AudioMode)
        │   │   │   ├── di/                     (DataModule, AudioModule, DispatcherModule)
        │   │   │   ├── local/                  (YogaDatabase, entities, DAOs, converters)
        │   │   │   └── repository/             (YogaRepositoryImpl)
        │   │   ├── domain/
        │   │   │   ├── model/                  (YogaPose, PoseSpec, PoseLibrary, YogaDay, YogaProgram)
        │   │   │   ├── repository/             (YogaRepository)
        │   │   │   ├── usecase/                (Generate, Observe, Session, Audio use cases)
        │   │   │   └── util/                   (Result, AppError, AppDispatchers)
        │   │   └── ui/
        │   │       ├── theme/                  (Color, Theme, Typography — meditative dark)
        │   │       ├── components/             (PoseFigure, StateViews)
        │   │       ├── navigation/             (Routes)
        │   │       ├── splash/                 (SplashScreen)
        │   │       ├── onboarding/             (OnboardingScreen — выбор уровня)
        │   │       ├── program/                (ProgramScreen + VM — список 30 дней)
        │   │       ├── day/                    (DayScreen + VM — детали дня, reorder/swap)
        │   │       ├── session/                (SessionPlayerScreen + VM — сессия с таймером)
        │   │       ├── library/                (LibraryScreen + VM — 40 поз)
        │   │       ├── customday/              (CustomDayEditorScreen + VM)
        │   │       └── settings/               (SettingsScreen + VM — тема, аудио, длительность)
        │   └── res/
        │       ├── values/                     (strings, themes, colors)
        │       ├── values-en/
        │       ├── drawable/                   (launcher foreground, splash icon)
        │       ├── mipmap-anydpi-v26/
        │       └── xml/                         (network_security_config, backup_rules)
        └── test/
            └── java/com/example/yogaapp/
                ├── domain/
                │   ├── repository/             (FakeYogaRepository + tests)
                │   ├── usecase/                (GenerateProgramUseCase, SessionTimerUseCase)
                │   └── util/                    (TestAppDispatchers)
                └── ui/
```

## 4. Как собрать

### Требования

- JDK 17+
- Android Studio Ladybug+ (или AGP 8.7 из CLI)
- Android SDK с `compileSdk = 35`

### Сборка

```bash
# Из корня android-app/
./gradlew assembleDebug               # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease             # unsigned release
./gradlew assembleRelease -PsignReleaseWithDebugKey=true   # release signed with debug keystore
./gradlew lint                        # 0 errors
./gradlew detekt ktlintCheck          # static analysis
./gradlew test                        # unit tests (JUnit5)
```

### APK size

Цель — ≤ 25 МБ. Release APK в текущей конфигурации ~7–9 МБ (R8 + shrinkResources; нет изображений-ассетов).

## 5. Аудио — TTS и MP3-пак

### По умолчанию (TTS)

Приложение использует встроенный в Android `TextToSpeech` движок. Это значит:
- Никаких MP3-файлов в APK
- Работает сразу после установки
- Голос синтетический, но достаточно разборчивый для инструкций
- Язык: русский (с автоматическим фоллбэком на en, если ru недоступен на устройстве)

### Опциональный MP3-пак

Если у вас есть записанные женским голосом описания поз, положите их в:
```
/sdcard/Android/data/com.example.yogaapp/files/audio-pack/
  ├── tadasana.mp3
  ├── vrksasana.mp3
  ├── bhujangasana.mp3
  ├── ...
  └── gong.mp3                          # звук начала/конца позы
```

Включите в Settings → Аудио → «Записанный голос». Приложение автоматически переключится с TTS на MP3 для поз, у которых есть файл; для остальных останется TTS.

## 6. Где лежат секреты

**Секретов нет.** Приложение полностью автономно — без API, без OAuth, без аналитики. `INTERNET` permission отсутствует в манифесте. В `.gitignore` исключены `local.properties`, `keystore.properties`, `*.jks`.

## 7. Как подписать release APK

### Вариант A — debug-keystore (для тестов)

```bash
./gradlew assembleRelease -PsignReleaseWithDebugKey=true
```

### Вариант B — production keystore

1. Сгенерируйте keystore (один раз):
   ```bash
   keytool -genkey -v -keystore yoga.jks -keyalg RSA -keysize 2048 \
     -validity 10000 -alias yoga
   ```
2. Создайте `app/keystore.properties` (НЕ в git):
   ```properties
   storeFile=yoga.jks
   storePassword=...
   keyAlias=yoga
   keyPassword=...
   ```
3. Раскомментируйте блок `signingConfigs.release` в `app/build.gradle.kts`.
4. `./gradlew assembleRelease`.

## 8. Smoke-сценарий (manual QA на эмуляторе API 26 и 35)

> Старт → выбор уровня → 30-дневный план → детали дня → сессия → завершение → прогресс обновлён.

1. Запустить приложение.
2. Splash 1s → onboarding 3 страницы → выбор уровня (Начинающий) → программа.
3. На экране «Ваш план на 30 дней» — список из 30 карточек, первая помечена «Сегодня».
4. Тап на День 1 → детали: список 5 поз, каждой с Canvas-фигурой, slider длительности, кнопки Swap/Delete.
5. Long-press на карточке позы → drag для перестановки.
6. Тап «Заменить» → Library в picker mode → выбор позы → возврат с заменой.
7. Кнопка «Начать сессию» → SessionPlayer: Canvas-фигура текущей позы, описание, таймер обратного отсчёта, аудио-кнопки.
8. По истечении таймера — автоматический переход к следующей позе + голосовое описание.
9. По завершении всех поз — экран «Сессия завершена» → кнопка «Отметить выполненным» → возврат в Program.
10. Прогресс-бар обновлён; карточка дня помечена «Завершён».
11. Поворот экрана → состояние сохранено (`configChanges` + StateFlow).
12. `adb shell am force-stop com.example.yogaapp.debug` → перезапуск → состояние восстанавливается из Room.

## 9. Definition of Done

| Критерий | Команда | Ожидание |
|----------|---------|----------|
| Debug-сборка | `./gradlew assembleDebug` | 0 error-level warnings |
| Release-сборка | `./gradlew assembleRelease` | APK ≤ 25 МБ |
| Lint | `./gradlew lint` | 0 errors |
| Detekt + ktlint | `./gradlew detekt ktlintCheck` | 0 violations |
| Unit-тесты | `./gradlew test` | зелёные, ≥ 60% VM+use case |
| Эмулятор API 26 | ручной smoke | без крашей |
| Эмулятор API 35 | ручной smoke | без крашей |

## 10. Принятые допущения

1. **40 поз вместо 30** — вы выбрали «Расширенная 40» в чате; включает 10 продвинутых асан (стойка на голове, колесо, павлин и т.д.).
2. **Canvas-анимация поз** — один параметризованный `PoseFigure` composable рисует все 40 поз через углы суставов (`PoseSpec`). Это упрощённые силуэты, не фотореалистичные иллюстрации. APK не разрастается от изображений.
3. **Аудио-пак не зашит в APK** — чтобы сохранить APK ≤ 25 МБ. Пользователь кладёт MP3 в `filesDir/audio-pack/` вручную или через файловый менеджер. В Settings отображается статус пак-установки.
4. **Drag-to-reorder**: в текущей реализации используется Compose `detectDragGesturesAfterLongPress`. Для продакшн-grade drag-to-reorder рекомендуется библиотека `org.burnoutcrew.composereorderable` — добавлена как TODO в DayScreen.
5. **Theme по умолчанию — Dark** — выбрана как соответствующая meditative-стилю (йога практикуется в полумраке). Пользователь может переключить в Settings.
6. **Dynamic color по умолчанию OFF** — мы хотим стабильную indigo+gold палитру, не зависящую от обоев устройства.
7. **WorkManager + HiltWorkerFactory зарегистрированы, но без конкретных Worker'ов** — оставлен как заготовка для будущих напоминаний о дневной сессии.
8. **Skip Day с autoShift** — помечает день как пропущенный и сдвигает currentDay на 1 вперёд. Если нужна возможность «вернуться к пропущенному дню» — это можно добавить через setCurrentDay().

## 11. Принятые решения (после согласования с заказчиком)

| ID | Решение | Обоснование |
|----|---------|-------------|
| D-1 | **Статичная Canvas-фигура** без анимированных переходов между позами | Подтверждено заказчиком — статичной фигуры с углами суставов достаточно для узнавания асаны. Анимации добавили бы сложности без пропорциональной ценности. |
| D-2 | **Без интеграции Google Fit / Apple Health** | Подтверждено заказчиком — ТЗ не упоминает калории/здоровье. Полная автономия приложения сохранена: нет лишних SDK, нет новых разрешений. |
| D-3 | **Без push-уведомлений-напоминаний** | Подтверждено заказчиком — не нужны. WorkManager + HiltWorkerFactory инфраструктура оставлена в коде как заготовка на будущее, но конкретные Worker'ы не пишутся. `POST_NOTIFICATIONS` и `VIBRATE` разрешения можно убрать из манифеста при желании (см. ниже). |

### Cleanup-заметка по D-3

Поскольку уведомлений пока нет, можно вырезать неиспользуемую WorkManager-инфраструктуру:

- `AndroidManifest.xml`: удалить `<provider>` блок для `androidx.startup.InitializationProvider` (meta-data для `WorkManagerInitializer`)
- `AndroidManifest.xml`: удалить разрешения `android.permission.POST_NOTIFICATIONS` и `android.permission.VIBRATE`
- `app/build.gradle.kts`: убрать зависимости `work-runtime-ktx`, `work-hilt`, `work-hilt-compiler`, `hilt-work-compiler`
- `YogaApp.kt`: убрать `HiltAndroidApp`-привязанный `workerFactory` и имплементацию `Configuration.Provider`

В текущем коде это оставлено намеренно — на случай, если заказчик вернётся к идее напоминаний позже. Удаление занимает ~5 минут.

## 12. Анализ похожих приложений (как референсы)

| Приложение | Ключевые фичи | Что мы взяли |
|------------|---------------|--------------|
| Daily Yoga | SmartCoach 28-day plan, L1–L5 levels, 30-day challenges | Структура «прогрессивный 30-дневный план + уровни сложности» |
| Down Dog | Полная кастомизация поз, per-pose durations, аудио+видео | Идея «настройка позиций + время каждой позы» |
| Yoga Studio | Конструктор последовательностей, drag-to-reorder | Custom Day Editor + reorder в DayDetail |
| GYA / Daily Yoga (offline) | Скачивание классов для офлайна | У нас офлайн-first с самого начала |

**Наше УТП**: гибрид Daily Yoga (фиксированный 30-дневный план) + Down Dog (кастомизация), полностью офлайн, без подписок.

## 13. Лицензия

MIT.
