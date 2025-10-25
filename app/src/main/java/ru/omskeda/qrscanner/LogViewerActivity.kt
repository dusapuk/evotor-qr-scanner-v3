package ru.omskeda.qrscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import timber.log.Timber

/**
 * Активность просмотра логов
 */
class LogViewerActivity : AppCompatActivity() {
    
    private lateinit var tvLogs: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnRefresh: Button
    private lateinit var btnClear: Button
    private lateinit var btnShare: Button
    
    companion object {
        const val TAG = "LogViewerActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Timber.tag(TAG).i("📋 Открытие просмотра логов")
            
            setContentView(R.layout.activity_log_viewer)
            
            // Настройка Toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Логи приложения"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            
            // Инициализация UI
            tvLogs = findViewById(R.id.tvLogs)
            scrollView = findViewById(R.id.scrollView)
            btnRefresh = findViewById(R.id.btnRefresh)
            btnClear = findViewById(R.id.btnClear)
            btnShare = findViewById(R.id.btnShare)
            
            // Обработчики кнопок
            btnRefresh.setOnClickListener {
                Timber.tag(TAG).d("🔄 Обновление логов")
                loadLogs()
            }
            
            btnClear.setOnClickListener {
                Timber.tag(TAG).d("🗑️ Запрос очистки логов")
                showClearConfirmation()
            }
            
            btnShare.setOnClickListener {
                Timber.tag(TAG).d("📤 Экспорт логов")
                shareLogs()
            }
            
            // Загружаем логи
            loadLogs()
            
            Timber.tag(TAG).i("✅ LogViewerActivity инициализирована")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onCreate()")
            finish()
        }
    }
    
    private fun loadLogs() {
        try {
            val logs = LogManager.getLogs()
            
            if (logs.isEmpty()) {
                tvLogs.text = "📝 Логи пусты"
                Timber.tag(TAG).d("ℹ️ Логи пусты")
                return
            }
            
            val logsText = buildString {
                appendLine("═══════════════════════════════════════")
                appendLine("       ЛОГИ ПРИЛОЖЕНИЯ (${logs.size})")
                appendLine("═══════════════════════════════════════")
                appendLine()
                
                logs.forEach { entry ->
                    val levelIcon = when (entry.level) {
                        LogManager.LogLevel.DEBUG -> "🐛"
                        LogManager.LogLevel.INFO -> "ℹ️"
                        LogManager.LogLevel.WARNING -> "⚠️"
                        LogManager.LogLevel.ERROR -> "❌"
                    }
                    appendLine("[$levelIcon ${entry.timestamp}]")
                    appendLine(entry.message)
                    appendLine()
                }
                
                appendLine("═══════════════════════════════════════")
                appendLine("           КОНЕЦ ЛОГОВ")
                appendLine("═══════════════════════════════════════")
            }
            
            tvLogs.text = logsText
            
            // Прокручиваем в конец
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
            
            Timber.tag(TAG).d("✅ Загружено ${logs.size} записей лога")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка загрузки логов")
            tvLogs.text = "❌ Ошибка загрузки логов:\n${e.message}"
        }
    }
    
    private fun showClearConfirmation() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Очистить логи?")
                .setMessage("Вы уверены, что хотите очистить все логи?")
                .setPositiveButton("Очистить") { _, _ ->
                    clearLogs()
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка показа диалога")
        }
    }
    
    private fun clearLogs() {
        try {
            LogManager.clearLogs()
            loadLogs()
            
            Timber.tag(TAG).i("✅ Логи очищены")
            LogManager.logInfo("🗑️ Логи были очищены пользователем")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка очистки логов")
            tvLogs.text = "❌ Ошибка очистки логов:\n${e.message}"
        }
    }
    
    private fun shareLogs() {
        try {
            val logFile = LogManager.getLogFile()
            
            if (logFile == null || !logFile.exists()) {
                Timber.tag(TAG).w("⚠️ Файл логов не найден")
                tvLogs.text = "⚠️ Файл логов не найден"
                return
            }
            
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                logFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Логи QR Scanner v${MainActivity.APP_VERSION}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Экспорт логов"))
            
            Timber.tag(TAG).i("✅ Логи экспортированы")
            LogManager.logInfo("📤 Пользователь экспортировал логи")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка экспорта логов")
            AlertDialog.Builder(this)
                .setTitle("Ошибка экспорта")
                .setMessage("Не удалось экспортировать логи:\n${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

