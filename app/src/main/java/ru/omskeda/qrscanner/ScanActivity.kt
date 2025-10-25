package ru.omskeda.qrscanner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.KeyEvent
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import org.json.JSONObject
import timber.log.Timber

/**
 * Активность для сканирования QR-кодов
 * Поддерживает камеру и 2D HID-сканер NETUM K20
 */
class ScanActivity : AppCompatActivity() {
    
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var beepManager: BeepManager
    private lateinit var tvScanStatus: TextView
    private lateinit var inputHid: EditText
    private var isScanning = true
    
    // HID-сканер поддержка
    private var hidBuffer = StringBuilder()
    private val hidHandler = Handler(Looper.getMainLooper())
    private var lastKeyTime = 0L
    private val hidTimeoutMs = 100L  // Если пауза больше 100мс, считаем новое сканирование
    
    companion object {
        const val TAG = "ScanActivity"
    }
    
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            try {
                Timber.tag(TAG).d("📸 barcodeResult() вызван, result=${result?.text?.take(30) ?: "null"}, isScanning=$isScanning")
                LogManager.logDebug("📸 Camera callback: result=${result?.text?.take(30) ?: "null"}, isScanning=$isScanning")
                
                if (result == null || !isScanning) {
                    return
                }
                
                isScanning = false
                val qrData = result.text
                
                Timber.tag(TAG).i("📸 Камера отсканировала QR: ${qrData.take(50)}")
                LogManager.logInfo("📸 Камера успешно отсканировала QR-код (${qrData.length} символов)")
                
                // Вибрация и звук при успешном сканировании
                playSuccessSound()
                
                // Обработка отсканированного QR-кода
                handleQRCode(qrData, "Камера")
                
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Ошибка в barcodeResult callback")
                LogManager.logError("❌ Ошибка в camera callback: ${e.message}")
                isScanning = true
            }
        }
        
        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            // Не используется
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            Timber.tag(TAG).i("📱 onCreate() - Запуск активности сканирования")
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            LogManager.log("═══════════════════════════════════════════════")
            LogManager.log("📱 ScanActivity onCreate() - Запуск сканирования")
            LogManager.log("═══════════════════════════════════════════════")
            
            setContentView(R.layout.activity_scan)
            
            // Инициализация UI
            barcodeView = findViewById(R.id.barcode_scanner)
            tvScanStatus = findViewById(R.id.tvScanStatus)
            inputHid = findViewById(R.id.input_hid)
            beepManager = BeepManager(this)
            
            Timber.tag(TAG).d("✅ UI элементы инициализированы")
            LogManager.logDebug("✅ UI элементы сканера инициализированы")
            
            // Настройка HID input для 2D сканера
            setupHIDScanner()
            
            // Настройка сканера для QR-кодов
            setupBarcodeScanner()
            
            updateStatus("🔍 Готов к сканированию\n📷 Наведите камеру на QR-код\nили используйте 2D-сканер NETUM K20")
            
            Timber.tag(TAG).i("✅ onCreate() завершён, сканер готов")
            LogManager.logInfo("✅ ScanActivity onCreate() завершён успешно")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ КРИТИЧЕСКАЯ ОШИБКА в onCreate()")
            LogManager.logError("❌ КРИТИЧЕСКАЯ ОШИБКА в ScanActivity onCreate(): ${e.message}")
            LogManager.logError("Stack trace: ${e.stackTraceToString()}")
            
            showError("Ошибка инициализации сканера", 
                "Произошла ошибка при запуске сканера:\n${e.message}\n\nВозможно, камера недоступна.")
        }
    }
    
    private fun setupHIDScanner() {
        try {
            Timber.tag(TAG).d("🔧 Настройка HID-сканера")
            LogManager.logDebug("🔧 Настройка поддержки 2D HID-сканера NETUM K20")
            
            // Устанавливаем фокус на скрытый EditText для получения ввода от HID-сканера
            inputHid.requestFocus()
            
            // Обработка нажатия Enter от HID-сканера
            inputHid.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    val data = inputHid.text.toString().trim()
                    if (data.isNotEmpty()) {
                        Timber.tag(TAG).i("⌨️ HID сканер (Enter): $data")
                        LogManager.logInfo("⌨️ HID сканер отсканировал через EditText: ${data.take(30)}...")
                        inputHid.setText("")
                        hidBuffer.clear()
                        handleQRCode(data, "2D HID-сканер NETUM K20")
                    }
                    true
                } else {
                    false
                }
            }
            
            Timber.tag(TAG).d("✅ HID-сканер настроен")
            LogManager.logDebug("✅ HID-сканер настроен и готов к работе")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка настройки HID-сканера")
            LogManager.logError("❌ Ошибка настройки HID-сканера: ${e.message}")
        }
    }
    
    private fun setupBarcodeScanner() {
        try {
            Timber.tag(TAG).d("🔧 Настройка камеры для QR-кодов")
            LogManager.logDebug("🔧 Настройка камеры для сканирования QR-кодов")
            
            // Настройка сканера только для QR-кодов
            val formats = listOf(BarcodeFormat.QR_CODE)
            barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
            barcodeView.initializeFromIntent(intent)
            barcodeView.decodeContinuous(callback)
            
            // Настройка статусной строки
            barcodeView.statusView?.text = "Наведите камеру на QR-код"
            
            Timber.tag(TAG).d("✅ Камера настроена")
            LogManager.logDebug("✅ Камера настроена для QR-кодов")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка настройки камеры")
            LogManager.logError("❌ Ошибка настройки камеры: ${e.message}")
            updateStatus("⚠️ Камера недоступна\nИспользуйте 2D-сканер")
        }
    }
    
    /**
     * Перехватываем события клавиатуры от HID-сканера на уровне Activity
     * Это резервный метод на случай, если EditText не получает фокус
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        try {
            // Проверяем, это ли символьная клавиша (не системная)
            if (event != null && event.unicodeChar != 0) {
                val currentTime = System.currentTimeMillis()
                
                // Если прошло много времени с последнего символа, начинаем новый скан
                if (currentTime - lastKeyTime > hidTimeoutMs) {
                    if (hidBuffer.isNotEmpty()) {
                        Timber.tag(TAG).d("⏱️ Тайм-аут HID буфера, очистка")
                    }
                    hidBuffer.clear()
                }
                lastKeyTime = currentTime
                
                // Добавляем символ в буфер
                val char = event.unicodeChar.toChar()
                hidBuffer.append(char)
                Timber.tag(TAG).d("⌨️ HID символ: '$char', буфер: ${hidBuffer.length} символов")
                
                return true
            }
            
            // Если Enter, обрабатываем накопленные данные
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                val data = hidBuffer.toString().trim()
                if (data.isNotEmpty()) {
                    Timber.tag(TAG).i("✅ HID сканер завершил: $data")
                    LogManager.logInfo("✅ HID сканер (onKeyDown) отсканировал: ${data.take(30)}...")
                    hidBuffer.clear()
                    lastKeyTime = 0L
                    handleQRCode(data, "2D HID-сканер NETUM K20 (Direct)")
                }
                return true
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка обработки onKeyDown")
            LogManager.logError("❌ Ошибка обработки HID onKeyDown: ${e.message}")
        }
        
        return super.onKeyDown(keyCode, event)
    }
    
    /**
     * Обработка отсканированного QR-кода
     */
    private fun handleQRCode(qrData: String, source: String) {
        try {
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            Timber.tag(TAG).i("🔍 QR-код отсканирован из источника: $source")
            Timber.tag(TAG).i("📝 Данные (первые 100 символов): ${qrData.take(100)}")
            Timber.tag(TAG).i("📏 Длина данных: ${qrData.length} символов")
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            
            LogManager.log("═══════════════════════════════════════════════")
            LogManager.log("🔍 QR-код отсканирован!")
            LogManager.log("📱 Источник: $source")
            LogManager.log("📝 Данные (первые 100 символов): ${qrData.take(100)}")
            LogManager.log("📏 Длина данных: ${qrData.length} символов")
            LogManager.log("═══════════════════════════════════════════════")
            
            updateStatus("✅ QR-код отсканирован!\nИсточник: $source\nОбработка...")
            
            // Очищаем отсканированные данные от возможных артефактов
            val cleanedData = qrData.trim()
                .replace("Штрихкод", "")  // Убираем префикс от некоторых 2D-сканеров
                .replace("\\s+".toRegex(), "")  // Убираем все пробелы
                .trim()
            
            Timber.tag(TAG).d("🧹 Данные после очистки (первые 50): ${cleanedData.take(50)}")
            LogManager.logDebug("🧹 Данные после очистки: ${cleanedData.take(50)}...")
            
            var token: String? = null
            
            // Пытаемся парсить как JSON (старый формат)
            if (cleanedData.startsWith("{") && cleanedData.endsWith("}")) {
                try {
                    val json = JSONObject(cleanedData)
                    Timber.tag(TAG).d("✅ Распознан JSON формат (старый)")
                    LogManager.logDebug("✅ Распознан JSON формат QR-кода")
                    
                    token = json.optString("token", "")
                    val type = json.optString("type", "")
                    
                    if (type.isNotEmpty() && type != "pickup") {
                        Timber.tag(TAG).w("⚠️ Неверный тип QR-кода: $type (ожидается pickup)")
                        LogManager.logWarning("⚠️ Неверный тип QR-кода: $type")
                        showError("Неверный тип QR-кода", "Этот QR-код не предназначен для выдачи заказа.")
                        return
                    }
                } catch (jsonError: Exception) {
                    Timber.tag(TAG).w(jsonError, "⚠️ Ошибка парсинга JSON")
                    LogManager.logWarning("⚠️ Не удалось распарсить как JSON: ${jsonError.message}")
                }
            }
            
            // Если JSON не распарсился, используем всю строку как токен
            if (token.isNullOrEmpty()) {
                token = cleanedData
                Timber.tag(TAG).d("ℹ️ Обрабатываем весь QR-код как токен")
                LogManager.logInfo("ℹ️ Используем QR-код как токен напрямую")
            }
            
            // Валидация токена
            if (token.length < 10) {
                Timber.tag(TAG).e("❌ Токен слишком короткий: ${token.length} символов")
                LogManager.logError("❌ Токен слишком короткий: ${token.length} символов (минимум 10)")
                showError("Неверный QR-код", "QR-код слишком короткий. Возможно, он поврежден или это не QR-код заказа.")
                return
            }
            
            if (token.length > 100) {
                Timber.tag(TAG).w("⚠️ Токен очень длинный: ${token.length} символов")
                LogManager.logWarning("⚠️ Токен подозрительно длинный: ${token.length} символов")
            }
            
            Timber.tag(TAG).i("✅ Токен валиден, открываем детали заказа")
            Timber.tag(TAG).i("🔑 Токен (первые 16 символов): ${token.take(16)}...")
            LogManager.logInfo("✅ Токен валиден: ${token.take(16)}...")
            LogManager.logInfo("🚀 Открываем OrderDetailActivity")
            
            // Открываем детали заказа
            val intent = Intent(this, OrderDetailActivity::class.java).apply {
                putExtra("token", token)
                putExtra("source", source)
            }
            startActivity(intent)
            
            Timber.tag(TAG).i("✅ OrderDetailActivity запущена")
            LogManager.logInfo("✅ OrderDetailActivity успешно запущена с токеном")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ КРИТИЧЕСКАЯ ОШИБКА обработки QR")
            LogManager.logError("❌ КРИТИЧЕСКАЯ ОШИБКА обработки QR: ${e.message}")
            LogManager.logError("Stack trace: ${e.stackTraceToString()}")
            
            showError("Ошибка обработки QR-кода", 
                "Произошла ошибка при обработке отсканированного QR-кода:\n${e.message}")
        } finally {
            // Возобновляем сканирование после небольшой задержки
            Handler(Looper.getMainLooper()).postDelayed({
                isScanning = true
                updateStatus("🔍 Готов к следующему сканированию")
            }, 1000)
        }
    }
    
    private fun playSuccessSound() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(200)
            beepManager.playBeepSoundAndVibrate()
            Timber.tag(TAG).d("🔊 Звук и вибрация воспроизведены")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка воспроизведения звука/вибрации")
            LogManager.logError("❌ Ошибка воспроизведения обратной связи: ${e.message}")
        }
    }
    
    private fun updateStatus(text: String) {
        runOnUiThread {
            try {
                tvScanStatus.text = text
                Timber.tag(TAG).d("📝 Статус обновлен: $text")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Ошибка обновления статуса")
            }
        }
    }
    
    private fun showError(title: String, message: String) {
        runOnUiThread {
            try {
                isScanning = false
                updateStatus("❌ $title")
                
                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Повторить") { _, _ ->
                        isScanning = true
                        updateStatus("🔍 Готов к сканированию")
                        inputHid.requestFocus()
                    }
                    .setNegativeButton("Назад") { _, _ ->
                        finish()
                    }
                    .setNeutralButton("Просмотр логов") { _, _ ->
                        startActivity(Intent(this, LogViewerActivity::class.java))
                    }
                    .setCancelable(false)
                    .show()
                    
                Timber.tag(TAG).d("✅ Диалог ошибки показан")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Ошибка показа диалога ошибки")
                finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            Timber.tag(TAG).i("▶️ onResume() - Возобновление сканирования")
            LogManager.logInfo("▶️ ScanActivity onResume()")
            
            barcodeView.resume()
            isScanning = true
            inputHid.requestFocus()
            updateStatus("🔍 Готов к сканированию")
            
            Timber.tag(TAG).d("✅ Сканирование возобновлено")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onResume()")
            LogManager.logError("❌ Ошибка в ScanActivity onResume(): ${e.message}")
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            Timber.tag(TAG).i("⏸️ onPause() - Приостановка сканирования")
            LogManager.logInfo("⏸️ ScanActivity onPause()")
            
            barcodeView.pause()
            isScanning = false
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onPause()")
            LogManager.logError("❌ Ошибка в ScanActivity onPause(): ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            Timber.tag(TAG).i("🔚 onDestroy()")
            LogManager.logInfo("🔚 ScanActivity onDestroy()")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onDestroy()")
        }
    }
    
    override fun onBackPressed() {
        Timber.tag(TAG).i("🔙 Пользователь нажал кнопку назад")
        LogManager.logInfo("🔙 Пользователь покинул ScanActivity")
        super.onBackPressed()
    }
}

