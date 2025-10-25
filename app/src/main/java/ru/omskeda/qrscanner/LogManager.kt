package ru.omskeda.qrscanner

import android.content.Context
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Менеджер логов - сохраняет все логи в файл и память
 */
object LogManager {
    private val logs = mutableListOf<LogEntry>()
    private val maxLogsInMemory = 1000
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    data class LogEntry(
        val timestamp: String,
        val message: String,
        val level: LogLevel = LogLevel.INFO
    )
    
    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
    
    fun init(context: Context) {
        try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val filename = "qr_scanner_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.log"
            logFile = File(logDir, filename)
            
            Timber.i("📝 Логи сохраняются в: ${logFile?.absolutePath}")
            log("📝 Логи сохраняются в: ${logFile?.absolutePath}")
            
            // Очистка старых логов (старше 7 дней)
            cleanOldLogs(logDir, 7)
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Ошибка инициализации логирования")
        }
    }
    
    fun log(message: String, level: LogLevel = LogLevel.INFO) {
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(timestamp, message, level)
        
        synchronized(logs) {
            logs.add(entry)
            
            // Ограничиваем количество логов в памяти
            if (logs.size > maxLogsInMemory) {
                logs.removeAt(0)
            }
        }
        
        // Записываем в файл
        try {
            logFile?.appendText("[$timestamp] [${level.name}] $message\n")
        } catch (e: Exception) {
            Timber.e(e, "❌ Ошибка записи лога в файл")
        }
        
        // Также выводим в Timber
        when (level) {
            LogLevel.DEBUG -> Timber.d(message)
            LogLevel.INFO -> Timber.i(message)
            LogLevel.WARNING -> Timber.w(message)
            LogLevel.ERROR -> Timber.e(message)
        }
    }
    
    fun logDebug(message: String) = log(message, LogLevel.DEBUG)
    fun logInfo(message: String) = log(message, LogLevel.INFO)
    fun logWarning(message: String) = log(message, LogLevel.WARNING)
    fun logError(message: String) = log(message, LogLevel.ERROR)
    
    fun getLogs(): List<LogEntry> {
        synchronized(logs) {
            return logs.toList()
        }
    }
    
    fun clearLogs() {
        synchronized(logs) {
            logs.clear()
        }
        try {
            logFile?.writeText("")
        } catch (e: Exception) {
            Timber.e(e, "❌ Ошибка очистки файла логов")
        }
    }
    
    fun getLogFile(): File? = logFile
    
    private fun cleanOldLogs(logDir: File, daysToKeep: Int) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            logDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    file.delete()
                    Timber.i("🗑️ Удален старый лог: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Ошибка очистки старых логов")
        }
    }
}

