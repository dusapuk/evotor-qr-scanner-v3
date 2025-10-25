package ru.omskeda.qrscanner

import android.app.Application
import timber.log.Timber

/**
 * Главный класс приложения для инициализации глобальных компонентов
 */
class QRScannerApplication : Application() {
    
    companion object {
        const val VERSION_NAME = "3.0.0"
        const val IS_DEBUG = true
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация Timber для логирования
        if (IS_DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // В production режиме также включаем логирование для отладки
            Timber.plant(Timber.DebugTree())
        }
        
        // Инициализация менеджера логов
        LogManager.init(this)
        
        Timber.i("═══════════════════════════════════════════════════════")
        Timber.i("🚀 QR Scanner Application v$VERSION_NAME запущено")
        Timber.i("📱 SDK версия: ${android.os.Build.VERSION.SDK_INT}")
        Timber.i("📱 Модель устройства: ${android.os.Build.MODEL}")
        Timber.i("📱 Производитель: ${android.os.Build.MANUFACTURER}")
        Timber.i("═══════════════════════════════════════════════════════")
        
        LogManager.log("═══════════════════════════════════════════════════════")
        LogManager.log("🚀 QR Scanner Application v$VERSION_NAME запущено")
        LogManager.log("📱 SDK версия: ${android.os.Build.VERSION.SDK_INT}")
        LogManager.log("📱 Модель устройства: ${android.os.Build.MODEL}")
        LogManager.log("📱 Производитель: ${android.os.Build.MANUFACTURER}")
        LogManager.log("═══════════════════════════════════════════════════════")
    }
}

