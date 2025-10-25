package ru.omskeda.qrscanner

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Активность отображения деталей заказа и его выдачи
 * Показывает список товаров, количество и позволяет изменить статус на "Выдан"
 */
class OrderDetailActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    private lateinit var progressBar: ProgressBar
    private lateinit var orderInfoLayout: LinearLayout
    private lateinit var tvOrderNumber: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvOrderAmount: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvDeliveryType: TextView
    private lateinit var tvItemsCount: TextView
    private lateinit var llItemsList: LinearLayout
    private lateinit var tvSource: TextView
    private lateinit var btnIssue: Button
    private lateinit var btnCancel: Button
    private lateinit var btnRetry: Button
    
    private var client: OkHttpClient? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var token: String = ""
    private var source: String = ""
    private var orderData: JSONObject? = null
    
    companion object {
        const val TAG = "OrderDetailActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            Timber.tag(TAG).i("📱 onCreate() - Открытие деталей заказа")
            Timber.tag(TAG).i("═══════════════════════════════════════════════")
            LogManager.log("═══════════════════════════════════════════════")
            LogManager.log("📱 OrderDetailActivity onCreate()")
            LogManager.log("═══════════════════════════════════════════════")
            
            setContentView(R.layout.activity_order_detail)
            
            // Настройка Toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Детали заказа"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            
            // Получаем данные из Intent
            token = intent.getStringExtra("token") ?: ""
            source = intent.getStringExtra("source") ?: "Неизвестный источник"
            
            Timber.tag(TAG).i("🔑 Токен получен: ${token.take(16)}...")
            Timber.tag(TAG).i("📱 Источник сканирования: $source")
            LogManager.logInfo("🔑 Получен токен: ${token.take(16)}...")
            LogManager.logInfo("📱 Источник: $source")
            
            if (token.isEmpty()) {
                Timber.tag(TAG).e("❌ Токен не передан!")
                LogManager.logError("❌ ОШИБКА: Токен не передан в OrderDetailActivity")
                showFatalError("Токен не передан")
                return
            }
            
            // Инициализация SharedPreferences
            prefs = getSharedPreferences("QRScannerPrefs", MODE_PRIVATE)
            
            // Инициализация HTTP клиента
            initHttpClient()
            
            // Инициализация UI
            initializeViews()
            
            // Обработчики кнопок
            setupButtonListeners()
            
            // Загружаем информацию о заказе
            loadOrderInfo()
            
            Timber.tag(TAG).i("✅ onCreate() завершён")
            LogManager.logInfo("✅ OrderDetailActivity onCreate() завершён")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ КРИТИЧЕСКАЯ ОШИБКА в onCreate()")
            LogManager.logError("❌ КРИТИЧЕСКАЯ ОШИБКА в OrderDetailActivity onCreate(): ${e.message}")
            LogManager.logError("Stack trace: ${e.stackTraceToString()}")
            showFatalError("Ошибка инициализации: ${e.message}")
        }
    }
    
    private fun initHttpClient() {
        try {
            Timber.tag(TAG).d("🔧 Инициализация HTTP клиента")
            LogManager.logDebug("🔧 Инициализация HTTP клиента с логированием")
            
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Timber.tag("HTTP").d(message)
                LogManager.logDebug("HTTP: $message")
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()
            
            Timber.tag(TAG).d("✅ HTTP клиент инициализирован")
            LogManager.logDebug("✅ HTTP клиент готов к работе")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка инициализации HTTP клиента")
            LogManager.logError("❌ Ошибка инициализации HTTP клиента: ${e.message}")
        }
    }
    
    private fun initializeViews() {
        try {
            Timber.tag(TAG).d("🔧 Инициализация UI элементов")
            
            progressBar = findViewById(R.id.progressBar)
            orderInfoLayout = findViewById(R.id.orderInfoLayout)
            tvOrderNumber = findViewById(R.id.tvOrderNumber)
            tvCustomerName = findViewById(R.id.tvCustomerName)
            tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
            tvOrderAmount = findViewById(R.id.tvOrderAmount)
            tvOrderStatus = findViewById(R.id.tvOrderStatus)
            tvDeliveryType = findViewById(R.id.tvDeliveryType)
            tvItemsCount = findViewById(R.id.tvItemsCount)
            llItemsList = findViewById(R.id.llItemsList)
            tvSource = findViewById(R.id.tvSource)
            btnIssue = findViewById(R.id.btnIssue)
            btnCancel = findViewById(R.id.btnCancel)
            btnRetry = findViewById(R.id.btnRetry)
            
            tvSource.text = "Источник: $source"
            
            Timber.tag(TAG).d("✅ UI элементы инициализированы")
            LogManager.logDebug("✅ UI элементы инициализированы")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка инициализации UI")
            LogManager.logError("❌ Ошибка инициализации UI: ${e.message}")
            throw e
        }
    }
    
    private fun setupButtonListeners() {
        try {
            btnIssue.setOnClickListener {
                Timber.tag(TAG).i("🖱️ Нажата кнопка 'Выдать заказ'")
                LogManager.logInfo("🖱️ Пользователь нажал 'Выдать заказ'")
                showIssueConfirmation()
            }
            
            btnCancel.setOnClickListener {
                Timber.tag(TAG).i("🖱️ Нажата кнопка 'Отмена'")
                LogManager.logInfo("🖱️ Пользователь нажал 'Отмена'")
                finish()
            }
            
            btnRetry.setOnClickListener {
                Timber.tag(TAG).i("🖱️ Нажата кнопка 'Повторить'")
                LogManager.logInfo("🖱️ Пользователь нажал 'Повторить загрузку'")
                loadOrderInfo()
            }
            
            Timber.tag(TAG).d("✅ Обработчики кнопок настроены")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка настройки обработчиков")
            LogManager.logError("❌ Ошибка настройки обработчиков кнопок: ${e.message}")
        }
    }
    
    /**
     * Загрузка информации о заказе с сервера
     */
    private fun loadOrderInfo() {
        val apiUrl = prefs.getString("api_url", "") ?: ""
        
        if (apiUrl.isEmpty()) {
            Timber.tag(TAG).e("❌ API URL не настроен")
            LogManager.logError("❌ API URL не настроен в настройках")
            showError("API URL не настроен", "Необходимо настроить URL сервера в настройках приложения")
            return
        }
        
        Timber.tag(TAG).i("🌐 Начало загрузки информации о заказе")
        Timber.tag(TAG).i("🔗 API URL: $apiUrl")
        LogManager.logInfo("🌐 Загрузка информации о заказе с сервера")
        LogManager.logInfo("🔗 API URL: $apiUrl")
        
        progressBar.visibility = View.VISIBLE
        orderInfoLayout.visibility = View.GONE
        btnRetry.visibility = View.GONE
        
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    getOrderInfoByToken(apiUrl, token)
                }
                
                Timber.tag(TAG).i("📦 Ответ от сервера получен")
                LogManager.logInfo("📦 Ответ от сервера получен")
                
                if (result != null) {
                    orderData = result
                    displayOrderInfo(result)
                } else {
                    Timber.tag(TAG).e("❌ Сервер вернул пустой ответ")
                    LogManager.logError("❌ Не удалось загрузить информацию о заказе")
                    showError("Ошибка загрузки", "Не удалось загрузить информацию о заказе. Проверьте подключение к серверу.")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Исключение при загрузке заказа")
                LogManager.logError("❌ Исключение при загрузке заказа: ${e.message}")
                LogManager.logError("Stack trace: ${e.stackTraceToString()}")
                showError("Ошибка загрузки", "Ошибка при загрузке данных заказа:\n${e.message}")
            }
        }
    }
    
    /**
     * Запрос информации о заказе с сервера
     */
    private fun getOrderInfoByToken(apiUrl: String, token: String): JSONObject? {
        try {
            Timber.tag(TAG).d("🔍 Формирование запроса к API")
            LogManager.logDebug("🔍 POST запрос к /api/qrcode/info-by-token")
            
            val json = JSONObject()
            json.put("token", token)
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$apiUrl/api/qrcode/info-by-token")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            Timber.tag(TAG).d("📡 Отправка запроса...")
            LogManager.logDebug("📡 Отправка запроса на сервер...")
            
            client?.newCall(request)?.execute()?.use { response ->
                Timber.tag(TAG).i("📨 Ответ получен: HTTP ${response.code}")
                LogManager.logInfo("📨 HTTP ответ: ${response.code} ${response.message}")
                
                if (!response.isSuccessful) {
                    Timber.tag(TAG).e("❌ Неуспешный HTTP код: ${response.code}")
                    LogManager.logError("❌ HTTP ошибка: ${response.code}")
                    return null
                }
                
                val responseBody = response.body?.string() ?: ""
                Timber.tag(TAG).d("📄 Тело ответа (первые 200 символов): ${responseBody.take(200)}")
                LogManager.logDebug("📄 Получено ${responseBody.length} байт данных")
                
                val responseJson = JSONObject(responseBody)
                val success = responseJson.optBoolean("success", false)
                
                if (!success) {
                    val message = responseJson.optString("message", "Неизвестная ошибка")
                    Timber.tag(TAG).e("❌ API вернул success=false: $message")
                    LogManager.logError("❌ API ошибка: $message")
                    return null
                }
                
                Timber.tag(TAG).i("✅ Данные заказа успешно получены")
                LogManager.logInfo("✅ Данные заказа успешно получены от сервера")
                
                return responseJson.getJSONObject("order")
            }
            
            return null
            
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "❌ Ошибка сети")
            LogManager.logError("❌ Ошибка сети: ${e.message}")
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка обработки ответа")
            LogManager.logError("❌ Ошибка обработки ответа: ${e.message}")
            throw e
        }
    }
    
    /**
     * Отображение информации о заказе
     */
    private fun displayOrderInfo(order: JSONObject) {
        try {
            Timber.tag(TAG).i("📋 Отображение информации о заказе")
            LogManager.logInfo("📋 Отображение информации о заказе")
            
            progressBar.visibility = View.GONE
            orderInfoLayout.visibility = View.VISIBLE
            btnRetry.visibility = View.GONE
            
            // Основная информация о заказе
            val orderNumber = order.optString("orderNumber", "N/A")
            tvOrderNumber.text = "Заказ #$orderNumber"
            
            val userName = order.optString("userName", "N/A")
            tvCustomerName.text = "👤 $userName"
            
            val userPhone = order.optString("userPhone", "")
            tvCustomerPhone.text = if (userPhone.isNotEmpty()) "📱 $userPhone" else "📱 Телефон не указан"
            tvCustomerPhone.visibility = if (userPhone.isNotEmpty()) View.VISIBLE else View.GONE
            
            val finalAmount = order.optDouble("finalAmount", 0.0)
            tvOrderAmount.text = "💰 ${finalAmount.toInt()} ₽"
            
            // Статус заказа
            val status = order.optString("status", "unknown")
            tvOrderStatus.text = getStatusText(status)
            tvOrderStatus.setBackgroundColor(getStatusColor(status))
            
            val deliveryType = order.optString("deliveryType", "unknown")
            tvDeliveryType.text = if (deliveryType == "pickup") "📦 Самовывоз" else "🚚 Доставка"
            
            // Товары в заказе
            val items = order.optJSONArray("items")
            val itemsCount = items?.length() ?: 0
            tvItemsCount.text = "📦 Товаров: $itemsCount"
            
            Timber.tag(TAG).i("📦 Заказ #$orderNumber, статус: $status, товаров: $itemsCount")
            LogManager.logInfo("📦 Заказ #$orderNumber | Статус: $status | Товаров: $itemsCount | Сумма: $finalAmount ₽")
            
            // Отображаем список товаров
            displayItemsList(items)
            
            // Настраиваем кнопку выдачи в зависимости от статуса
            configureIssueButton(status, orderNumber)
            
            Timber.tag(TAG).i("✅ Информация о заказе отображена")
            LogManager.logInfo("✅ Информация о заказе успешно отображена")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка отображения информации о заказе")
            LogManager.logError("❌ Ошибка отображения информации: ${e.message}")
            showError("Ошибка отображения", "Не удалось отобразить информацию о заказе:\n${e.message}")
        }
    }
    
    /**
     * Отображение списка товаров
     */
    private fun displayItemsList(items: org.json.JSONArray?) {
        try {
            llItemsList.removeAllViews()
            
            if (items == null || items.length() == 0) {
                Timber.tag(TAG).w("⚠️ Список товаров пуст")
                LogManager.logWarning("⚠️ В заказе нет товаров")
                
                val emptyView = TextView(this).apply {
                    text = "Список товаров пуст"
                    setPadding(16, 16, 16, 16)
                }
                llItemsList.addView(emptyView)
                return
            }
            
            Timber.tag(TAG).d("📝 Отображение ${items.length()} товаров")
            LogManager.logInfo("📝 Отображение списка из ${items.length()} товаров:")
            
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val product = item.optJSONObject("product")
                
                val productName = product?.optString("name", "Неизвестный товар") ?: "Неизвестный товар"
                val quantity = item.optInt("quantity", 0)
                val price = item.optDouble("price", 0.0)
                val total = quantity * price
                
                Timber.tag(TAG).d("  ${i + 1}. $productName x$quantity = $total ₽")
                LogManager.logInfo("  ${i + 1}. $productName | Кол-во: $quantity | Цена: $price ₽ | Итого: $total ₽")
                
                val itemView = createItemView(i + 1, productName, quantity, price, total)
                llItemsList.addView(itemView)
            }
            
            Timber.tag(TAG).d("✅ Список товаров отображен")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка отображения списка товаров")
            LogManager.logError("❌ Ошибка отображения списка товаров: ${e.message}")
        }
    }
    
    /**
     * Создание View для отображения товара
     */
    private fun createItemView(num: Int, name: String, quantity: Int, price: Double, total: Double): View {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
        }
        
        val tvItemTitle = TextView(this).apply {
            text = "$num. $name"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val tvItemDetails = TextView(this).apply {
            text = "Количество: $quantity шт. • Цена: ${price.toInt()} ₽ • Итого: ${total.toInt()} ₽"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
        
        itemLayout.addView(tvItemTitle)
        itemLayout.addView(tvItemDetails)
        
        // Разделитель
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                topMargin = 8
            }
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
        itemLayout.addView(divider)
        
        return itemLayout
    }
    
    /**
     * Настройка кнопки выдачи в зависимости от статуса
     */
    private fun configureIssueButton(status: String, orderNumber: String) {
        try {
            when (status) {
                "issued" -> {
                    btnIssue.isEnabled = false
                    btnIssue.text = "✅ Заказ уже выдан"
                    Timber.tag(TAG).i("ℹ️ Заказ #$orderNumber уже выдан")
                    LogManager.logInfo("ℹ️ Заказ #$orderNumber уже выдан ранее")
                }
                "ready_for_pickup" -> {
                    btnIssue.isEnabled = true
                    btnIssue.text = "✅ Выдать заказ"
                    Timber.tag(TAG).i("✅ Заказ #$orderNumber готов к выдаче")
                    LogManager.logInfo("✅ Заказ #$orderNumber готов к выдаче")
                }
                else -> {
                    btnIssue.isEnabled = false
                    btnIssue.text = "⚠️ Заказ не готов к выдаче"
                    Timber.tag(TAG).w("⚠️ Заказ #$orderNumber в статусе '$status' - выдача невозможна")
                    LogManager.logWarning("⚠️ Заказ #$orderNumber в статусе '$status' - не готов к выдаче")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка настройки кнопки выдачи")
            LogManager.logError("❌ Ошибка настройки кнопки выдачи: ${e.message}")
        }
    }
    
    /**
     * Показать диалог подтверждения выдачи
     */
    private fun showIssueConfirmation() {
        try {
            val orderNumber = orderData?.optString("orderNumber", "N/A") ?: "N/A"
            val itemsCount = orderData?.optJSONArray("items")?.length() ?: 0
            
            Timber.tag(TAG).i("🔔 Показ диалога подтверждения выдачи")
            LogManager.logInfo("🔔 Пользователь запросил подтверждение выдачи заказа #$orderNumber")
            
            AlertDialog.Builder(this)
                .setTitle("✅ Подтверждение выдачи")
                .setMessage("Выдать заказ #$orderNumber покупателю?\n\nТоваров в заказе: $itemsCount шт.")
                .setPositiveButton("Выдать") { _, _ ->
                    Timber.tag(TAG).i("✅ Пользователь подтвердил выдачу")
                    LogManager.logInfo("✅ Подтверждена выдача заказа #$orderNumber")
                    issueOrder()
                }
                .setNegativeButton("Отмена") { _, _ ->
                    Timber.tag(TAG).i("❌ Пользователь отменил выдачу")
                    LogManager.logInfo("❌ Отменена выдача заказа #$orderNumber")
                }
                .show()
                
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка показа диалога подтверждения")
            LogManager.logError("❌ Ошибка показа диалога подтверждения: ${e.message}")
        }
    }
    
    /**
     * Выдача заказа (изменение статуса на "Выдан")
     */
    private fun issueOrder() {
        val apiUrl = prefs.getString("api_url", "") ?: ""
        
        Timber.tag(TAG).i("═══════════════════════════════════════════════")
        Timber.tag(TAG).i("🚀 Начало процесса выдачи заказа")
        Timber.tag(TAG).i("═══════════════════════════════════════════════")
        LogManager.log("═══════════════════════════════════════════════")
        LogManager.log("🚀 ВЫДАЧА ЗАКАЗА")
        LogManager.log("═══════════════════════════════════════════════")
        
        progressBar.visibility = View.VISIBLE
        btnIssue.isEnabled = false
        
        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    validateAndIssueByToken(apiUrl, token)
                }
                
                progressBar.visibility = View.GONE
                
                if (success) {
                    Timber.tag(TAG).i("✅ Заказ успешно выдан!")
                    LogManager.logInfo("✅ ЗАКАЗ УСПЕШНО ВЫДАН!")
                    showSuccessDialog()
                } else {
                    Timber.tag(TAG).e("❌ Не удалось выдать заказ")
                    LogManager.logError("❌ Не удалось выдать заказ - сервер вернул ошибку")
                    showError("Ошибка выдачи", "Не удалось выдать заказ. Попробуйте еще раз.")
                    btnIssue.isEnabled = true
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Timber.tag(TAG).e(e, "❌ Исключение при выдаче заказа")
                LogManager.logError("❌ Исключение при выдаче заказа: ${e.message}")
                LogManager.logError("Stack trace: ${e.stackTraceToString()}")
                showError("Ошибка выдачи", "Ошибка при выдаче заказа:\n${e.message}")
                btnIssue.isEnabled = true
            }
        }
    }
    
    /**
     * Отправка запроса на выдачу заказа
     */
    private fun validateAndIssueByToken(apiUrl: String, token: String): Boolean {
        try {
            Timber.tag(TAG).d("📡 Отправка запроса на выдачу заказа")
            LogManager.logDebug("📡 POST запрос к /api/qrcode/validate-by-token")
            
            val json = JSONObject()
            json.put("token", token)
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$apiUrl/api/qrcode/validate-by-token")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            Timber.tag(TAG).d("📡 Отправка запроса на сервер...")
            
            client?.newCall(request)?.execute()?.use { response ->
                Timber.tag(TAG).i("📨 Ответ получен: HTTP ${response.code}")
                LogManager.logInfo("📨 HTTP ответ: ${response.code} ${response.message}")
                
                if (!response.isSuccessful) {
                    Timber.tag(TAG).e("❌ Неуспешный HTTP код: ${response.code}")
                    LogManager.logError("❌ HTTP ошибка при выдаче: ${response.code}")
                    return false
                }
                
                val responseBody = response.body?.string() ?: ""
                Timber.tag(TAG).d("📄 Ответ сервера: ${responseBody.take(200)}")
                LogManager.logDebug("📄 Ответ сервера получен (${responseBody.length} байт)")
                
                val responseJson = JSONObject(responseBody)
                val success = responseJson.optBoolean("success", false)
                
                if (success) {
                    Timber.tag(TAG).i("✅ Сервер подтвердил выдачу заказа")
                    LogManager.logInfo("✅ Сервер подтвердил: заказ выдан, статус изменен на 'issued'")
                } else {
                    val message = responseJson.optString("message", "Неизвестная ошибка")
                    Timber.tag(TAG).e("❌ Сервер отклонил выдачу: $message")
                    LogManager.logError("❌ Сервер отклонил выдачу: $message")
                }
                
                return success
            }
            
            return false
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка запроса выдачи")
            LogManager.logError("❌ Ошибка при отправке запроса выдачи: ${e.message}")
            throw e
        }
    }
    
    /**
     * Показать диалог успешной выдачи
     */
    private fun showSuccessDialog() {
        try {
            val orderNumber = orderData?.optString("orderNumber", "N/A") ?: "N/A"
            
            AlertDialog.Builder(this)
                .setTitle("✅ Успешно!")
                .setMessage("Заказ #$orderNumber успешно выдан покупателю.\n\nСтатус заказа изменен на 'Выдан'.")
                .setPositiveButton("OK") { _, _ ->
                    Timber.tag(TAG).i("🏁 Завершение работы с заказом")
                    LogManager.logInfo("🏁 Пользователь завершил работу с заказом")
                    finish()
                }
                .setCancelable(false)
                .show()
                
            Timber.tag(TAG).i("✅ Диалог успеха показан")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка показа диалога успеха")
            LogManager.logError("❌ Ошибка показа диалога успеха: ${e.message}")
            finish()
        }
    }
    
    private fun showError(title: String, message: String) {
        runOnUiThread {
            try {
                progressBar.visibility = View.GONE
                orderInfoLayout.visibility = View.GONE
                btnRetry.visibility = View.VISIBLE
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                
                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Просмотр логов") { _, _ ->
                        startActivity(Intent(this, LogViewerActivity::class.java))
                    }
                    .show()
                    
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Ошибка показа диалога ошибки")
            }
        }
    }
    
    private fun showFatalError(message: String) {
        runOnUiThread {
            try {
                progressBar.visibility = View.GONE
                
                AlertDialog.Builder(this)
                    .setTitle("Критическая ошибка")
                    .setMessage(message)
                    .setPositiveButton("Закрыть") { _, _ ->
                        finish()
                    }
                    .setNeutralButton("Просмотр логов") { _, _ ->
                        startActivity(Intent(this, LogViewerActivity::class.java))
                    }
                    .setCancelable(false)
                    .show()
            } catch (e: Exception) {
                finish()
            }
        }
    }
    
    private fun getStatusText(status: String): String {
        return when (status) {
            "created" -> "📝 Создан"
            "unpaid" -> "💳 Не оплачен"
            "paid" -> "✅ Оплачен"
            "ready_for_pickup" -> "✅ Готов к выдаче"
            "transferred_to_courier" -> "🚚 Передан курьеру"
            "issued" -> "✅ Выдан"
            "cancelled" -> "❌ Отменен"
            else -> "❓ $status"
        }
    }
    
    private fun getStatusColor(status: String): Int {
        return when (status) {
            "ready_for_pickup" -> ContextCompat.getColor(this, android.R.color.holo_green_light)
            "issued" -> ContextCompat.getColor(this, android.R.color.holo_blue_light)
            "cancelled" -> ContextCompat.getColor(this, android.R.color.holo_red_light)
            "paid" -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
            else -> ContextCompat.getColor(this, android.R.color.darker_gray)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            scope.cancel()
            Timber.tag(TAG).i("🔚 onDestroy()")
            LogManager.logInfo("🔚 OrderDetailActivity onDestroy()")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Ошибка в onDestroy()")
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        Timber.tag(TAG).i("🔙 Навигация назад")
        LogManager.logInfo("🔙 Пользователь вернулся назад из OrderDetailActivity")
        finish()
        return true
    }
}

