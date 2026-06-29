# СветОк Карта

Мобильное Android-приложение для жителей г. Светлоград. Показывает на интерактивной карте улицы с активными и запланированными отключениями электроэнергии, отправляет push-уведомления и принимает жалобы.

## Связанные репозитории

| Репозиторий | Тип | Назначение |
|---|---|---|
| **SvetOk-Map** (этот) | public | Android-приложение жителя — карта отключений, push, жалобы |
| [`SvetOk-Admin`](https://github.com/vesslina/SvetOk-Admin) | private | Android-приложение диспетчера (АО «Ставэлектросеть») |
| [`svetok-service`](https://github.com/vesslina/SvetOk-API) | private | FastAPI-бэкенд + SQLite + FCM push + доставка обновлений |

Бэкенд и административное приложение закрытые. СветОк Карта работает с публичным (`x-svetok-api-key`) токеном, который НЕ даёт прав на управление отключениями — даже при декомпиляции этого открытого APK.

## Возможности

- Интерактивная карта города на OpenStreetMap / osmdroid
- Подсветка улиц с активными и запланированными отключениями
- Детальная карточка выбранной улицы: статус, причина, время, затронутые отключения
- Экран текущих отключений со списком затронутых улиц
- Отправка жалобы по выбранной улице с номером дома и описанием проблемы
- Подписка на push-уведомления по конкретным улицам
- Получение FCM push-уведомлений при создании отключения на backend
- Локальный GeoJSON улиц в assets для быстрой отрисовки линий на карте
- Офлайн-режим: «Нет соединения с сервером» при отсутствии сети, автоматическое обновление при восстановлении связи
- Синяя цветовая схема (Material 3)

## Обновление данных

Приложение не использует периодический polling. Стратегия:
1. **Стартовая последовательность** — 1 запрос + до 2 повторных попыток с разбросом ~20с
2. **По действиям** — кнопка обновления, открытие раздела «Текущие отключения»
3. **ConnectivityMonitor** — `NetworkCallback` автоматически триггерит refresh при восстановлении сети

## Технологии

- Kotlin, Jetpack Compose, Material 3
- Compose Navigation, ViewModel + StateFlow, Kotlin Coroutines
- Koin (DI)
- Ktor Client (HTTP API)
- Room (локальный кэш отключений)
- kotlinx.serialization
- Firebase Cloud Messaging (push)
- osmdroid / OpenStreetMap (карта)
- R8/ProGuard для release-сборки
- Min SDK 26

## Архитектура

```text
UI: Compose screens
  |
ViewModels: StateFlow + coroutines
  |
Repositories
  |-- Ktor HTTP API client (GET /api/outages, POST /api/complaints, /api/subscriptions)
  |-- Room cache (последние полученные отключения)
  |-- GeoJSON asset parser (streets.geojson)
  |-- ConnectivityMonitor (NetworkCallback)
  |
Backend API (svetok-service) + Firebase Cloud Messaging
```

Основные модули:

- `ui/map` — карта, статусная панель, выбор улицы, кнопка «Текущие отключения»
- `ui/outages` — список текущих отключений (DEMO vs API source detection)
- `ui/onboarding` — онбординг (тёмно-синий градиент)
- `data/outage` — API, Room-кэш, ConnectivityMonitor, модели отключений
- `data/geo` — чтение `streets.geojson` из assets
- `data/subscription` — регистрация FCM-токенов и подписок
- `data/complaint` — отправка жалоб

## Связь с backend

Приложение обращается к `svetok-service` (FastAPI) и передаёт публичный токен в HTTP-заголовке:

```http
X-Svetok-Api-Key: <API_CLIENT_TOKEN>
```

Используемые API-сценарии:
- получить активные отключения (`GET /api/outages`)
- отправить жалобу (`POST /api/complaints`)
- создать/удалить push-подписку (`POST/DELETE /api/subscriptions`)

Если API недоступен, публичная карта и список отключений используют локальный Room-кэш либо показывают сообщение «Нет соединения с сервером».

## Что не публикуется

В репозиторий не входят:
- `app.local.properties` с API URL и клиентским токеном
- настоящий `app/google-services.json` Firebase-проекта
- `local.properties` с локальным Android SDK path
- `keystore.properties`
- release keystore (`*.jks`, `*.keystore`)
- APK/AAB-файлы и Gradle build-кэш

`app/src/main/assets/streets.geojson` намеренно оставлен в проекте — карта зависит от этих данных.

## Локальная настройка

1. Скопировать шаблон конфигурации:

```powershell
Copy-Item app.local.properties.example app.local.properties
```

2. Заполнить `app.local.properties`:

```properties
API_BASE_URL=https://your-api-host.example
API_CLIENT_TOKEN_A=part-a
API_CLIENT_TOKEN_B=part-b
```

`API_CLIENT_TOKEN_A + API_CLIENT_TOKEN_B` должны давать полный `API_CLIENT_TOKEN` из backend `.env`.

3. Для FCM скопировать Firebase config:

```powershell
Copy-Item app\google-services.example.json app\google-services.json
```

Затем заменить значения в `app/google-services.json` файлом из Firebase Console. Если файла нет, проект собирается, но Firebase Messaging не будет настроен.

4. Собрать release-версию:

```powershell
.\gradlew.bat :app:assembleRelease
```

## Автор

vesslina@gmail.com / [github.com/vesslina](https://github.com/vesslina)

2026, АО «Ставэлектросеть»
