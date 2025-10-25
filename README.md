# QR Scanner для Evotor

Мобильное приложение для сканирования QR-кодов на кассовых аппаратах Evotor.

## Функции

- 📷 Сканирование QR-кодов через камеру
- ⌨️ Поддержка 2D HID-сканера NETUM K20
- 📝 Просмотр истории сканирований
- 🎯 Обработка заказов по токенам

## Требования

- Android 5.1+ (API 22+)
- Java 21+
- Gradle 8.7+

## Установка

### Локальная сборка

1. Установите Java 21:
   ```bash
   # Скачайте с https://adoptium.net/
   ```

2. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/YOUR_USERNAME/evotor-qr-scanner-v3.git
   cd evotor-qr-scanner-v3
   ```

3. Соберите проект:
   ```bash
   ./gradlew assembleDebug
   ```

4. APK будет в: `app/build/outputs/apk/debug/app-debug.apk`

### Установка на Evotor через ADB

```bash
# Подключитесь к устройству
adb connect 192.168.1.100:5555

# Установите приложение
adb install app/build/outputs/apk/debug/app-debug.apk

# Запустите приложение
adb shell am start -n ru.omskeda.qrscanner/.MainActivity
```

## Структура проекта

```
app/
├── src/main/
│   ├── java/ru/omskeda/qrscanner/
│   │   ├── MainActivity.kt
│   │   ├── ScanActivity.kt
│   │   ├── OrderDetailActivity.kt
│   │   ├── SettingsActivity.kt
│   │   ├── LogViewerActivity.kt
│   │   ├── QRScannerApplication.kt
│   │   └── LogManager.kt
│   └── res/
│       ├── layout/
│       ├── values/
│       └── xml/
├── build.gradle
└── proguard-rules.pro
```

## Зависимости

- AndroidX
- Kotlin 1.9.20
- ZXing (QR код сканирование)
- OkHttp (сетевые запросы)
- Timber (логирование)
- Gson (JSON обработка)

## Версия

- **Версия:** 3.0.0
- **Код версии:** 10
- **Min SDK:** 22
- **Target SDK:** 34

## Лицензия

MIT License

## Автор

Разработано для Evotor

