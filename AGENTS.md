# AGENTS.md — продолжение разработки YogaApp

Документ для **агента-разработчика** (новый чат / другая машина). Источник правды по коду — этот репозиторий, не устаревший README целиком.

## Проект

| | |
|---|---|
| Имя | YogaApp / «Йога — 30 дней» / бренд на сплэше «ДенисМурыс-Йога» |
| Корень | `F:\Project\android-app` (локально) |
| GitHub | https://github.com/xdenix/yoga-app |
| Ветка | `main` |
| Язык UI / строк | русский (+ `values-en`) |
| Пакет | `com.example.yogaapp` (debug: `com.example.yogaapp.debug`) |
| Стек | Kotlin 2.0.21, AGP 8.7.2, Gradle 8.9, Compose BOM 2024.10.01, Hilt 2.52, Room 2.6.1, DataStore, Coil 3 |

## Цель продукта

Офлайн-first 30-дневный план йоги: библиотека поз, день с правкой состава/времени, сессия с таймером и озвучкой, настройки темы/аудио/мелодий. Дистрибуция сейчас через **GitHub Releases** (не Google Play).

## Обязательные правила агента

1. **Не менять** `applicationId`, архитектуру слоёв, версии библиотек — кроме случая, когда без этого сборка невозможна.
2. **Минимальный diff**, без спекулятивных рефакторингов и лишних markdown-файлов.
3. **Коммиты** — только по явной просьбе пользователя.
4. APK / AAB / keystore **не коммитить** (см. `.gitignore`). Релизный APK — asset GitHub Release с именем `yoga-app-{versionCode}-{versionName}-debug.apk`.
5. Перед ответом пользователю в этом workspace обычно нужен compression API (`http://localhost:8080/v1`); если недоступен — один повтор, затем короткий ответ без полного дампа.
6. Windows PowerShell 5.1: не использовать `&&`; пути в кавычках.

## Сборка и окружение

```powershell
cd F:\Project\android-app
# local.properties (gitignore): sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

- JDK 17 (Adoptium). `ksp.useKSP2=false` в `gradle.properties` — иначе Room 2.6.1 ломается на suspend Insert.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Установка: `adb install -r ...`
- Эмулятор AVD: `YogaTest`

## Автообновление

- `GithubAppUpdater` в `onResume` MainActivity.
- API: `https://api.github.com/repos/xdenix/yoga-app/releases/latest`
- Asset **обязан** матчить `yoga-app-(\d+)-.+\.apk`; remote `versionCode` > `BuildConfig.VERSION_CODE`.
- Нужны `INTERNET`, `REQUEST_INSTALL_PACKAGES`, FileProvider.
- Play In-App Updates **удалены**.

Текущие версии в `app/build.gradle.kts`: `versionCode = 3`, `versionName = "1.0.2"`.

## Ключевые пути

| Область | Путь |
|---------|------|
| Позы (данные) | `app/.../domain/model/PoseLibrary.kt` (~42 позы) |
| Фото поз | `app/src/main/assets/poses/{id}.jpg` |
| UI фигуры | `app/.../ui/components/PoseFigure.kt` (Coil + assets) |
| Сессия | `app/.../ui/session/SessionPlayerScreen.kt` |
| Мелодии | `app/.../data/audio/PracticeMelody*.kt`, `assets/melodies/*.wav` |
| GitHub update | `app/.../update/GithubAppUpdater.kt` |
| Версии libs | `gradle/libs.versions.toml` |

## Релизы

| Tag | Asset | Notes |
|-----|-------|-------|
| v1.0.0 | `app-debug.apk` | первая сборка, без автообновления по имени |
| v1.0.1 | `yoga-app-2-1.0.1-debug.apk` | GitHub updater |
| v1.0.2 | `yoga-app-3-1.0.2-debug.apk` | студийные фото 42 поз |

Страница: https://github.com/xdenix/yoga-app/releases

## Что читать дальше

1. `docs/PROJECT.md` — актуальное описание продукта и расхождения со старым README.
2. Последний файл в `handoffs/` — состояние сессии.
3. Исходники в `app/src/main/...` — выше любых сводок.

## Явно отложено / незавершено

- Замена синтетических WAV на 5 бесплатных суфийских треков: скачаны черновики в `tools/melody-dl/` (не в git), в приложение ещё **не** встроены.
- Google Play публикация — отменена пользователем в пользу GitHub.
- На экране сессии `PoseFigure` уменьшен до **48.dp** (иконка) — локально может быть незакоммичено; проверить `git status`.
