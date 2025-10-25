package ru.omskeda.qrscanner

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import timber.log.Timber

/**
 * Главная активность приложения для сканирования QR-кодов заказов
 * Версия 3.0.0 - Максимальная отладка с детальным логированием
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    private lateinit var tvApiUrl: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvDeviceInfo: TextView
    private lateinit var btnScan: Button
    private lateinit var btnSettings: Button
    private lateinit var btnViewLogs: Button
    
    companion object {
        const val APP_VERSION = "3.0.0"
        const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            Timber.tag(TAG).i("📱 onCreate() - Запуск главной активности")
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            LogManager.log("═══════════════════════════════════════════════")
            LogManager.log("📱 MainActivity onCreate() - Запуск главной активности")
            LogManager.log("═══════════════════════════════════════════════")
            
            setContentView(R.layout.activity_main)
            
            // Настройка Toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Сканер заказов Эвотор"
            Timber.tag(TAG).d("✅ Toolbar настроен")
            
            // Инициализация SharedPreferences
            prefs = getSharedPreferences("QRScannerPrefs", MODE_PRIVATE)
            Timber.tag(TAG).d("✅ SharedPreferences инициализированы")
            LogManager.logDebug("SharedPreferences инициализированы")
            
            // Инициализация UI элементов
            initializeViews()
            
            // Показываем версию приложения и информацию об устройстве
            displayVersionInfo()
            
            // Обработчики кнопок
            setupButtonListeners()
            
            // Загружаем настройки
            loadSettings()
            
            Timber.tag(TAG).i("✅ onCreate() завершён успешно")
            LogManager.logInfo("✅ MainActivity onCreate() завершён успешно")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ КРИТИЧЕСКАЯ ОШИБКА в onCreate()")
            LogManager.logError("❌ КРИТИЧЕСКАЯ ОШИБКА в MainActivity onCreate(): ${e.message}")
            LogManager.logError("Stack trace: ${e.stackTraceToString()}")
            
            // Показываем диалог с ошибкой, но НЕ закрываем приложение
            showErrorDialog("Ошибка при запуске", 
                "Произошла ошибка при инициализации приложения:\n${e.message}\n\nПриложение будет работать в ограниченном режиме.",
                shouldFinish = false)
        }
    }
    
    private fun initializeViews() {
        try {
            Timber.tag(TAG).d("🔧 Инициализация UI элементов")
            
            tvApiUrl = findViewById(R.id.tvApiUrl)
            tvStatus = findViewById(R.id.tvStatus)
            tvVersion = findViewById(R.id.tvVersion)
            tvDeviceInfo = findViewById(R.id.tvDeviceInfo)
            btnScan = findViewById(R.id.btnScan)
            btnSettings = findViewById(R.id.btnSettings)
            btnViewLogs = findViewById(R.id.btnViewLogs)
            
            Timber.tag(TAG).d("✅ Все UI элементы инициализированы")
            LogManager.logDebug("✅ Все UI элементы инициализированы")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка инициализации UI элементов")
            LogManager.logError("❌ Ошибка инициализации UI элементов: ${e.message}")
            throw e
        }
    }
    
    private fun displayVersionInfo() {
        try {
            tvVersion.text = "Версия: $APP_VERSION (отладочная)"
            
            val deviceInfo = """
                Модель: ${android.os.Build.MODEL}
                Производитель: ${android.os.Build.MANUFACTURER}
                SDK: ${android.os.Build.VERSION.SDK_INT}
            """.trimIndent()
            
            tvDeviceInfo.text = deviceInfo
            
            Timber.tag(TAG).d("📱 Информация об устройстве: $deviceInfo")
            LogManager.logInfo("📱 Информация об устройстве отображена")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка отображения информации о версии")
            LogManager.logError("❌ Ошибка отображения информации о версии: ${e.message}")
        }
    }
    
    private fun setupButtonListeners() {
        try {
            Timber.tag(TAG).d("🔧 Настройка обработчиков кнопок")
            
            btnScan.setOnClickListener {
                Timber.tag(TAG).i("🖱️ Нажата кнопка 'Сканировать QR-код'")
                LogManager.logInfo("🖱️ Пользователь нажал кнопку 'Сканировать QR-код'")
                startScanActivity()
            }
            
            btnSettings.setOnClickListener {
                Timber.tag(TAG).i("🖱️ Нажата кнопка 'Настройки'")
                LogManager.logInfo("🖱️ Пользователь нажал кнопку 'Настройки'")
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            
            btnViewLogs.setOnClickListener {
                Timber.tag(TAG).i("🖱️ Нажата кнопка 'Просмотр логов'")
                LogManager.logInfo("🖱️ Пользователь нажал кнопку 'Просмотр логов'")
                startActivity(Intent(this, LogViewerActivity::class.java))
            }
            
            Timber.tag(TAG).d("✅ Обработчики кнопок настроены")
            LogManager.logDebug("✅ Обработчики кнопок настроены")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка настройки обработчиков кнопок")
            LogManager.logError("❌ Ошибка настройки обработчиков кнопок: ${e.message}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            Timber.tag(TAG).i("▶️ onResume() - Активность возобновлена")
            LogManager.logInfo("▶️ MainActivity onResume() - Активность возобновлена")
            loadSettings()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onResume()")
            LogManager.logError("❌ Ошибка в MainActivity onResume(): ${e.message}")
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            Timber.tag(TAG).i("⏸️ onPause() - Активность приостановлена")
            LogManager.logInfo("⏸️ MainActivity onPause() - Активность приостановлена")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onPause()")
            LogManager.logError("❌ Ошибка в MainActivity onPause(): ${e.message}")
        }
    }
    
    /**
     * Загрузка настроек из SharedPreferences
     */
    private fun loadSettings() {
        try {
            Timber.tag(TAG).d("🔍 Загрузка настроек из SharedPreferences")
            LogManager.logDebug("🔍 Загрузка настроек из SharedPreferences")
            
            val apiUrl = prefs.getString("api_url", "") ?: ""
            
            Timber.tag(TAG).d("📝 API URL из настроек: ${if (apiUrl.isEmpty()) "не задан" else apiUrl}")
            LogManager.logInfo("📝 API URL: ${if (apiUrl.isEmpty()) "не задан" else apiUrl}")
            
            if (apiUrl.isEmpty()) {
                tvApiUrl.text = "⚠️ API не настроен"
                tvStatus.text = "❌ Необходима настройка"
                btnScan.isEnabled = false
                
                Timber.tag(TAG).w("⚠️ API URL не настроен - кнопка сканирования отключена")
                LogManager.logWarning("⚠️ API URL не настроен - требуется настройка")
                
                showApiNotConfiguredDialog()
            } else {
                tvApiUrl.text = "API: $apiUrl"
                tvStatus.text = "✅ Готов к работе"
                btnScan.isEnabled = true
                
                Timber.tag(TAG).i("✅ API настроен, приложение готово к работе")
                LogManager.logInfo("✅ API настроен, приложение готово к работе")
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка загрузки настроек")
            LogManager.logError("❌ Ошибка загрузки настроек: ${e.message}")
            
            tvApiUrl.text = "❌ Ошибка загрузки настроек"
            tvStatus.text = "❌ Ошибка"
            btnScan.isEnabled = false
        }
    }
    
    /**
     * Запуск активности сканирования
     */
    private fun startScanActivity() {
        try {
            Timber.tag(TAG).i("🚀 Запуск активности сканирования")
            LogManager.logInfo("🚀 Запуск ScanActivity")
            
            val apiUrl = prefs.getString("api_url", "") ?: ""
            
            if (apiUrl.isEmpty()) {
                Timber.tag(TAG).w("⚠️ Попытка запуска сканирования без настроенного API")
                LogManager.logWarning("⚠️ Попытка запуска сканирования без настроенного API")
                
                Toast.makeText(this, "⚠️ Сначала настройте API URL в настройках", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, SettingsActivity::class.java))
                return
            }
            
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
            
            Timber.tag(TAG).i("✅ ScanActivity запущена")
            LogManager.logInfo("✅ ScanActivity успешно запущена")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка запуска ScanActivity")
            LogManager.logError("❌ Ошибка запуска ScanActivity: ${e.message}")
            LogManager.logError("Stack trace: ${e.stackTraceToString()}")
            
            showErrorDialog("Ошибка запуска сканера", 
                "Не удалось запустить сканер:\n${e.message}",
                shouldFinish = false)
        }
    }
    
    /**
     * Показать диалог о необходимости настройки API
     */
    private fun showApiNotConfiguredDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("⚙️ Требуется настройка")
                .setMessage("Для работы приложения необходимо настроить URL сервера.\n\nПерейти в настройки?")
                .setPositiveButton("Настроить") { _, _ ->
                    Timber.tag(TAG).i("👤 Пользователь перешел в настройки из диалога")
                    LogManager.logInfo("👤 Пользователь перешел в настройки из диалога")
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                .setNegativeButton("Позже", null)
                .show()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка показа диалога настройки API")
            LogManager.logError("❌ Ошибка показа диалога настройки API: ${e.message}")
        }
    }
    
    /**
     * Показать диалог с ошибкой
     */
    private fun showErrorDialog(title: String, message: String, shouldFinish: Boolean = false) {
        try {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { _, _ ->
                    if (shouldFinish) {
                        finish()
                    }
                }
                .setNeutralButton("Просмотреть логи") { _, _ ->
                    startActivity(Intent(this, LogViewerActivity::class.java))
                }
                .setCancelable(!shouldFinish)
                .show()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка показа диалога ошибки")
            LogManager.logError("❌ Ошибка показа диалога ошибки: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            Timber.tag(TAG).i("🔚 onDestroy() - Активность уничтожена")
            LogManager.logInfo("🔚 MainActivity onDestroy() - Активность уничтожена")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onDestroy()")
            LogManager.logError("❌ Ошибка в MainActivity onDestroy(): ${e.message}")
        }
    }
}

