package ru.omskeda.qrscanner

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Активность настроек приложения
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    private lateinit var etApiUrl: EditText
    private lateinit var tvConnectionStatus: TextView
    private lateinit var btnSave: Button
    private lateinit var btnTest: Button
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    companion object {
        const val TAG = "SettingsActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            Timber.tag(TAG).i("⚙️ onCreate() - Открытие настроек")
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            LogManager.log("═══════════════════════════════════════════════")
            LogManager.log("⚙️ SettingsActivity onCreate()")
            LogManager.log("═══════════════════════════════════════════════")
            
            setContentView(R.layout.activity_settings)
            
            // Настройка Toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Настройки"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            
            // Инициализация SharedPreferences
            prefs = getSharedPreferences("QRScannerPrefs", MODE_PRIVATE)
            Timber.tag(TAG).d("✅ SharedPreferences инициализированы")
            
            // Инициализация UI элементов
            etApiUrl = findViewById(R.id.etApiUrl)
            tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
            btnSave = findViewById(R.id.btnSave)
            btnTest = findViewById(R.id.btnTest)
            
            // Загружаем сохраненные настройки
            loadSettings()
            
            // Обработчик кнопки сохранения
            btnSave.setOnClickListener {
                Timber.tag(TAG).i("🖱️ Нажата кнопка 'Сохранить'")
                LogManager.logInfo("🖱️ Пользователь нажал 'Сохранить настройки'")
                saveSettings()
            }
            
            // Обработчик кнопки проверки
            btnTest.setOnClickListener {
                Timber.tag(TAG).i("🖱️ Нажата кнопка 'Проверить подключение'")
                LogManager.logInfo("🖱️ Пользователь запустил проверку подключения")
                testConnection()
            }
            
            Timber.tag(TAG).i("✅ onCreate() завершён")
            LogManager.logInfo("✅ SettingsActivity onCreate() завершён")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ КРИТИЧЕСКАЯ ОШИБКА в onCreate()")
            LogManager.logError("❌ КРИТИЧЕСКАЯ ОШИБКА в SettingsActivity onCreate(): ${e.message}")
            Toast.makeText(this, "Ошибка инициализации настроек: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Загрузка настроек из SharedPreferences
     */
    private fun loadSettings() {
        try {
            Timber.tag(TAG).d("🔍 Загрузка сохраненных настроек")
            LogManager.logDebug("🔍 Загрузка настроек из SharedPreferences")
            
            val apiUrl = prefs.getString("api_url", "") ?: ""
            etApiUrl.setText(apiUrl)
            
            if (apiUrl.isNotEmpty()) {
                tvConnectionStatus.text = "📝 URL: $apiUrl"
                Timber.tag(TAG).d("✅ Загружен API URL: $apiUrl")
                LogManager.logInfo("✅ Загружен сохраненный API URL: $apiUrl")
            } else {
                tvConnectionStatus.text = "⚠️ API URL не настроен"
                Timber.tag(TAG).w("⚠️ API URL не настроен")
                LogManager.logWarning("⚠️ API URL не настроен")
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка загрузки настроек")
            LogManager.logError("❌ Ошибка загрузки настроек: ${e.message}")
        }
    }
    
    /**
     * Сохранение настроек в SharedPreferences
     */
    private fun saveSettings() {
        try {
            val apiUrl = etApiUrl.text.toString().trim()
            
            Timber.tag(TAG).d("💾 Сохранение настроек")
            Timber.tag(TAG).d("📝 API URL для сохранения: $apiUrl")
            LogManager.logInfo("💾 Сохранение настроек")
            LogManager.logInfo("📝 API URL: $apiUrl")
            
            if (apiUrl.isEmpty()) {
                Timber.tag(TAG).w("⚠️ Попытка сохранить пустой API URL")
                LogManager.logWarning("⚠️ Попытка сохранить пустой API URL")
                Toast.makeText(this, "⚠️ Введите API URL", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Проверяем формат URL
            if (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://")) {
                Timber.tag(TAG).w("⚠️ Неверный формат URL: $apiUrl")
                LogManager.logWarning("⚠️ Неверный формат URL (отсутствует http:// или https://)")
                Toast.makeText(this, "⚠️ URL должен начинаться с http:// или https://", Toast.LENGTH_LONG).show()
                return
            }
            
            // Убираем trailing slash если есть
            val cleanUrl = apiUrl.trimEnd('/')
            
            // Сохраняем настройки
            with(prefs.edit()) {
                putString("api_url", cleanUrl)
                apply()
            }
            
            Timber.tag(TAG).i("✅ Настройки успешно сохранены: $cleanUrl")
            LogManager.logInfo("✅ Настройки успешно сохранены: $cleanUrl")
            
            tvConnectionStatus.text = "✅ Настройки сохранены"
            Toast.makeText(this, "✅ Настройки сохранены успешно", Toast.LENGTH_SHORT).show()
            
            // Возвращаемся на главный экран
            finish()
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка сохранения настроек")
            LogManager.logError("❌ Ошибка сохранения настроек: ${e.message}")
            Toast.makeText(this, "❌ Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Проверка подключения к API
     */
    private fun testConnection() {
        val apiUrl = etApiUrl.text.toString().trim()
        
        if (apiUrl.isEmpty()) {
            Timber.tag(TAG).w("⚠️ Попытка проверки без URL")
            LogManager.logWarning("⚠️ Попытка проверки подключения без URL")
            Toast.makeText(this, "⚠️ Введите API URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://")) {
            Timber.tag(TAG).w("⚠️ Неверный формат URL для проверки")
            LogManager.logWarning("⚠️ Неверный формат URL для проверки подключения")
            Toast.makeText(this, "⚠️ URL должен начинаться с http:// или https://", Toast.LENGTH_SHORT).show()
            return
        }
        
        Timber.tag(TAG).i("🔍 Начало проверки подключения к: $apiUrl")
        LogManager.logInfo("🔍 Проверка подключения к серверу: $apiUrl")
        
        btnTest.isEnabled = false
        btnTest.text = "⏳ Проверка..."
        tvConnectionStatus.text = "⏳ Проверка подключения..."
        
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    testApi(apiUrl)
                }
                
                if (result) {
                    Timber.tag(TAG).i("✅ API доступен и отвечает")
                    LogManager.logInfo("✅ Сервер доступен и отвечает на запросы")
                    
                    tvConnectionStatus.text = "✅ Сервер доступен"
                    Toast.makeText(this@SettingsActivity, "✅ Подключение успешно!", Toast.LENGTH_LONG).show()
                } else {
                    Timber.tag(TAG).e("❌ API недоступен")
                    LogManager.logError("❌ Сервер недоступен или не отвечает")
                    
                    tvConnectionStatus.text = "❌ Сервер недоступен"
                    Toast.makeText(this@SettingsActivity, "❌ Сервер недоступен. Проверьте URL и подключение к сети.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Ошибка проверки подключения")
                LogManager.logError("❌ Ошибка проверки подключения: ${e.message}")
                
                tvConnectionStatus.text = "❌ Ошибка: ${e.message}"
                Toast.makeText(this@SettingsActivity, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnTest.isEnabled = true
                btnTest.text = "🔗 Проверить подключение"
            }
        }
    }
    
    /**
     * Тестирование API
     */
    private fun testApi(apiUrl: String): Boolean {
        return try {
            val cleanUrl = apiUrl.trimEnd('/')
            val testUrl = "$cleanUrl/api/qrcode/info-by-token"
            
            Timber.tag(TAG).d("📡 Тестовый запрос к: $testUrl")
            LogManager.logDebug("📡 Отправка тестового запроса: $testUrl")
            
            val request = Request.Builder()
                .url(testUrl)
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseCode = response.code
            
            Timber.tag(TAG).d("📨 HTTP ответ: $responseCode")
            LogManager.logInfo("📨 Тестовый запрос: HTTP $responseCode ${response.message}")
            
            // Считаем успешным, если сервер отвечает (даже с ошибкой, важно что отвечает)
            val isSuccess = responseCode in 200..499
            
            if (isSuccess) {
                Timber.tag(TAG).i("✅ Сервер отвечает (код $responseCode)")
                LogManager.logInfo("✅ Сервер отвечает корректно (код $responseCode)")
            } else {
                Timber.tag(TAG).w("⚠️ Сервер вернул код $responseCode")
                LogManager.logWarning("⚠️ Сервер вернул необычный код: $responseCode")
            }
            
            isSuccess
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка тестового запроса")
            LogManager.logError("❌ Ошибка тестового запроса: ${e.message}")
            false
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        Timber.tag(TAG).i("🔙 Навигация назад")
        LogManager.logInfo("🔙 Пользователь вернулся из настроек")
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            scope.cancel()
            Timber.tag(TAG).i("🔚 onDestroy()")
            LogManager.logInfo("🔚 SettingsActivity onDestroy()")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onDestroy()")
        }
    }
}

