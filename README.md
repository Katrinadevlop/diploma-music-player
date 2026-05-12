# Musikkk Player для Android

Дипломный проект курса «Android-разработчик» Нетологии: нативный
Android-клиент к собственному облачному музыкальному сервису
[`musikkk.ru`](https://musikkk.ru). Параллельно с приложением живёт
веб-клиент со своим бэкендом — этот же сервер обслуживает и мобилку.

Тема: **«Мобильный аудиоплеер с облачной библиотекой пользователя».**

## Скриншоты

> Скриншоты добавляются по мере полировки UI. Сейчас планируемый набор:
>
> - `screenshots/login.png` — экран входа с glass-формой
> - `screenshots/register.png` — регистрация
> - `screenshots/library.png` — библиотека: grid карточек, фон-blur
> - `screenshots/release_detail.png` — экран релиза с треклистом
> - `screenshots/player.png` — fullscreen-плеер
> - `screenshots/hub.png` — «Моя музыка»: лайки/плейлисты/recent/top
> - `screenshots/settings.png` — экран настроек

## Функционал

**Авторизация и аккаунт**
- Регистрация (`POST /api/auth/register`) + подтверждение почты с
  countdown'ом и кнопкой «Отправить ещё раз»
- Вход по логину/паролю (`POST /api/auth/token` → Bearer)
- Logout (`POST /api/auth/logout`)
- Автоматический gating: если токен есть — на Library, если ожидает
  верификации — на VerifyEmail, иначе на Login

**Библиотека**
- Получение `GET /api/library`, кэширование в Room (artists / releases /
  tracks с FK), UI наблюдает локальную БД и работает офлайн
- Grid карточек релизов с обложкой, тапом — экран релиза с треклистом
- Поиск по локальному кэшу (`LIKE … COLLATE NOCASE` с дебаунсом 300 мс)
- Фильтры (тип релиза, только скачанные) и сортировка из настроек

**Воспроизведение**
- Media3 ExoPlayer + `MediaSessionService` для фонового воспроизведения
- OkHttp `DataSource` с `AuthInterceptor` — Bearer на каждый запрос
  стрима, никаких отдельных `Cookie`
- Mini-player над навигацией, fullscreen player с обложкой / прогрессом /
  seek-слайдером / shuffle / repeat / очередью (`ModalBottomSheet`)
- Media notification на локскрине и в шторке (через `MediaSessionService`)
- Continue: на каждые 5 секунд сохраняем позицию, на главной — карточка
  «Продолжить» с резюмом с того же места
- Качество стрима по сети: WiFi → оригинал, мобильная → `aac_128`
  (настраивается в Settings)

**Скачивание**
- Скачивание треков на телефон с прогрессом (WorkManager + Range-resume)
- Локальный файл подменяет URL стриминга — играет без сети
- Удаление скачанного, повтор при ошибках

**Загрузка**
- SAF multi-select (`OpenMultipleDocuments(["audio/*"])`)
- Multipart upload с прогрессом через `AtomicLong` + WorkManager
- После успеха worker дёргает `LibraryRepository.refresh()` — новый
  трек появляется в библиотеке без ручного refresh

**Персональные данные**
- Лайки: сердечко на каждой строке трека (`POST /api/user/likes/toggle`)
- Плейлисты: CRUD + добавление трека через `ModalBottomSheet`
- Recent / Top: авто-обновляются при смене текущего трека в плеере
- Continue: авто-сохранение позиции каждые 5 сек, карточка «Продолжить»

**Настройки**
- Тема: системная / светлая / тёмная (`AppCompatDelegate`-friendly)
- Язык: системный / Русский / English (через
  `AppCompatDelegate.setApplicationLocales`)
- Качество стрима: авто / оригинал / aac_128
- Фильтры библиотеки: тип релиза, только скачанные
- Подписка: ссылка на `musikkk.ru/billing` (открывается в браузере с
  пользовательской сессией)

**Прочее**
- Локализация EN (default) + RU
- Offline: библиотека читается из Room без сети; скачанные треки играют
  без сети
- CI: GitHub Actions, `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:assembleDebug` на каждый push в `main`

## Стек

- **Kotlin 2.0.21**, Coroutines/Flow, JVM target 17
- **Jetpack Compose** (BOM 2024.12.01) + Material 3 + Navigation Compose
- **Dagger Hilt 2.52** + KSP — DI
- **Retrofit 2.11** + **OkHttp 4.12** + **kotlinx.serialization 1.7.3**
- **Room 2.6.1** — локальный кэш библиотеки / загрузок / скачиваний
- **DataStore 1.1.1** — токен, настройки, pending verification
- **Media3 1.5.0** (ExoPlayer + Session + DataSource-OkHttp) — плеер
- **WorkManager 2.10** + `androidx.hilt:hilt-work` — фон-загрузки/выгрузки
- **Coil 2.7** — обложки (через `@AuthClient` OkHttp)
- **AppCompat 1.7** — `AppCompatDelegate.setApplicationLocales`
- Тесты: JUnit 4 + MockK + Turbine + OkHttp MockWebServer +
  kotlinx-coroutines-test

Документация по компонентам не из программы курса:
- [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer)
- [Media3 MediaSessionService](https://developer.android.com/media/media3/session)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Coil](https://coil-kt.github.io/coil/)
- [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- [Room](https://developer.android.com/training/data-storage/room)

## Архитектура

Single Activity + Compose Navigation. По слоям:

```
ru.musikkk.player
├─ app/                  — корневая навигация и MainScaffold
├─ core/
│  ├─ database/          — Room (entities, dao, MusikkkDatabase v3)
│  ├─ datastore/         — TokenStore, SettingsStore, PendingVerificationStore
│  ├─ media/             — ExoPlayer factory + MediaSessionService + NetworkQualityResolver
│  ├─ network/           — OkHttp/Retrofit/Auth, DTO, API
│  └─ locale/            — LocaleApplier (смена языка приложения)
├─ data/
│  ├─ auth/, library/, settings/, download/, upload/, user/, playback/
│  └─ каждый репозиторий — interface + Impl + Hilt module
├─ domain/               — чистые модели (Track, Release, UploadStatus, …)
└─ feature/
   ├─ auth/   (Login, Register, VerifyEmail)
   ├─ library/, release/, search/
   ├─ player/   (mini + fullscreen)
   ├─ uploads/, settings/, hub/, userdata/
```

**MVVM по фичам**: каждый экран — `@HiltViewModel` + `@Composable`
с `collectAsStateWithLifecycle()`. ViewModel'и оперируют доменными
типами, репозитории прячут Retrofit/Room/DataStore.

**Network**: один OkHttp клиент, `AuthInterceptor` подставляет
`Authorization: Bearer <token>` синхронно из in-memory кэша
`TokenStore.cachedToken()` (кэш прогревается на старте, никакого
`runBlocking`). Coil и Media3 шарят тот же клиент — обложки и стримы
ходят с тем же токеном.

**Offline-режим**: `LibraryRepository.observeReleases()` отдаёт `Flow`
из Room, refresh — отдельным suspend-вызовом. На офлайне UI продолжает
жить, видны кэш + скачанные треки.

## Сборка и запуск

Требования:
- Android Studio Koala+ (или JDK 17 + AGP 8.13)
- Android устройство/эмулятор с API 26+

```bash
git clone https://github.com/Katrinadevlop/diploma-music-player.git
cd diploma-music-player
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

В Android Studio: Open project → Run.

**Backend.** Приложение работает с https://musikkk.ru — публичный
сервер автора, не требует развёртывания. Доступен без VPN. Для входа
нужно зарегистрироваться (в приложении или на сайте) и подтвердить
почту по ссылке из письма. После этого можно загружать собственные
треки через мобильный SAF-пикер или веб-клиент.

## Сервер / API

Используется собственный backend
[`worryeed/musikkPlayer`](https://github.com/worryeed/musikkPlayer)
(Flask + Postgres + Redis + nginx). Исходники в репо не публикуются,
сервер развёрнут на отдельной VPS.

Аутентификация: для мобильного клиента добавлен `POST /api/auth/token`
(возвращает Bearer-токен). Под капотом — та же таблица `sessions`,
что у cookie-флоу веб-клиента.

Ключевые эндпоинты:
- Auth: `/api/auth/{register,token,logout,verify/resend}`
- Library: `/api/library`, `/api/library/upload`, `/api/track/<blob_id>`,
  `/api/cover/<blob_id>`
- User data: `/api/user/{likes,playlists,recent,playcounts,continue}`

## Тесты

```bash
./gradlew :app:testDebugUnitTest
```

Покрытие:
- `AuthInterceptorTest` (3) — Bearer header injection
- `LoginViewModelTest` (5) — валидация полей и серверные ошибки
- `RegisterViewModelTest` (~10) — клиентская валидация + маппинг
  серверных ошибок
- `VerifyEmailViewModelTest` (~7) — countdown / resend / state
- `LibraryMappersTest` (4) — DTO → Room snapshot
- `NetworkQualityResolverTest` (~6) — выбор варианта стрима
- `RepeatModeMapperTest` (3) — Media3 ↔ доменный enum
- `DownloadStatusMapperTest` (2), `UploadStatusMapperTest` (2) —
  round-trip enum ↔ строки
- `SectionFilterTest` (2) — фильтрация по типу релиза

Всё на JUnit 4 + MockK + Turbine + `StandardTestDispatcher`. Без
инструмент-тестов на CI (Compose-UI и Worker — отдельная история).

## Лицензия

Учебный проект — публикация в рамках сдачи диплома Нетологии.
