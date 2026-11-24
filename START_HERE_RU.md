# Запуск «Рыбных мест» на другом компьютере

1. На GitHub нажмите Code → Download ZIP и распакуйте архив.
2. В Android Studio: Open → выберите папку с settings.gradle.kts (не app).
3. Попросите у автора google-services.json и положите в app/google-services.json.
   Файл передаётся отдельно; пароль от аккаунта автора не нужен. Для демонстрации
   зарегистрируйте свой аккаунт: точки разных пользователей разделены.
4. Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK:
   выберите JDK 21 или 17, не 25.
5. Tools → SDK Manager: установите Android SDK Platform 35 и Build Tools 35.0.0.
   Android Studio должна создать local.properties с путём к SDK этого компьютера.
6. File → Sync Project with Gradle Files. Дождитесь завершения без ошибок.
7. Выберите конфигурацию app и телефон с USB-отладкой либо запущенный эмулятор
   Android API 23 или выше. Нажмите Run (зелёный треугольник).
8. Зарегистрируйтесь, зажмите место на карте, добавьте точку. Перезапустите приложение:
   сессия и точки должны сохраниться. Для первого входа нужен интернет.

Проверка в PowerShell из корня проекта:

```powershell
.\gradlew.bat :app:assembleDebug testDebugUnitTest lintDebug
```

APK после сборки: app/build/outputs/apk/debug/app-debug.apk.
На компьютере приложение показывается через Android Emulator в Android Studio;
это Android-приложение, а не программа Windows.

Если конфигурация app отсутствует, проверьте выбранную корневую папку и Gradle Sync.
Если Firebase сообщает CONFIGURATION_NOT_FOUND, автор должен включить
Authentication → Email/Password в проекте, соответствующем google-services.json.
Правила и настройка собственного Firebase описаны в docs/FIREBASE.md.
