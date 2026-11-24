# Выбор технологий и официальные источники

Проверка источников: 20 октября 2026 года. Версии фиксированы для воспроизводимой учебной сборки; это выбранный совместимый стабильный набор, а не утверждение, что каждая библиотека самая новая.

| Компонент | Версия | Причина |
|---|---|---|
| Android Gradle Plugin | 8.9.2 | Стабильная ветка, compile/target SDK 35 |
| Gradle | 8.11.1 | Версия из таблицы совместимости AGP 8.9 |
| Kotlin + Compose Compiler plugin | 2.1.20 | Одинаковая версия Kotlin и плагина Compose |
| KSP | 2.1.20-1.0.32 | Процессор Room для Kotlin 2.1.20 |
| Compose BOM | 2025.04.01 | Согласованные версии Compose/Material 3 |
| Activity Compose | 1.10.1 | Совместим с API 35 |
| Lifecycle | 2.9.0 | ViewModel и сбор StateFlow с учётом жизненного цикла |
| Core KTX | 1.16.0 | LocationManagerCompat и AndroidX API |
| Room | 2.7.2 | Стабильная версия с поддержкой Kotlin 2.x/KSP |
| Coroutines | 1.10.2 | StateFlow и suspend-операции |
| MapLibre Native Android | 12.3.1 | Стабильный релиз; поддерживаемый проект без обязательного ключа |
| OkHttp | 4.12.0 | HTTP-кеш и User-Agent, соответствует зависимости MapLibre |
| Robolectric (только тесты) | 4.14.1 | Проверка настоящей Room/SQLite на JVM, тестовая среда API 28 |

Официальные источники, открытые при разработке:

- [AGP 8.9: таблица совместимости Gradle/JDK/SDK](https://developer.android.com/build/releases/past-releases/agp-8-9-0-release-notes): Gradle 8.11.1, JDK 17, SDK 35.
- [Compose и Kotlin](https://developer.android.com/jetpack/androidx/releases/compose-kotlin): для Kotlin 2.0+ используется Compose Compiler Gradle plugin.
- [Room: примечания к релизам, включая 2.7.2](https://developer.android.com/jetpack/androidx/releases/room).
- [Kotlin: стабильные релизы](https://kotlinlang.org/docs/releases.html).
- [Официальный Compose BOM 2025.04.01 в Google Maven](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.04.01/compose-bom-2025.04.01.pom).
- [MapLibre Native: релиз Android 12.3.1](https://github.com/maplibre/maplibre-native/releases/tag/android-v12.3.1), [документация Android](https://maplibre.org/maplibre-native/android/api/).
- [MapLibre SDK в Maven Central](https://repo.maven.apache.org/maven2/org/maplibre/gl/android-sdk/12.3.1/): проверены POM и исходники API `HttpRequestUtil`, MapView, MapLibreMap.
- [Репозиторий osmdroid](https://github.com/osmdroid/osmdroid), [GitHub API](https://api.github.com/repos/osmdroid/osmdroid): `archived: true`, последнее изменение 20.11.2024. Для нового проекта архивирование — существенный риск дальнейшей совместимости; поэтому выбран MapLibre.
- [Правила использования тайлов OSM](https://operations.osmfoundation.org/policies/tiles/): атрибуция, идентифицируемый User-Agent, кеширование, запрет массовой предварительной загрузки.
- [Условия и авторство OpenStreetMap](https://www.openstreetmap.org/copyright).

MapLibre рендерит карту, OSM предоставляет тайлы и геоданные. Это разные роли. JSON-стиль встроен в приложение; секретов в нём нет. Логотип MapLibre в SDK отключён, а обязательная атрибуция источника данных вынесена в видимую кликабельную подпись на русском. Географические подписи на самих тайлах задаёт OSM.
