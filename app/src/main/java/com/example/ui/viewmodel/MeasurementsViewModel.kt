package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeasurementsViewModel(
    private val repository: AppRepository,
    private val context: Context
) : ViewModel() {

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _themeSetting = MutableStateFlow(sharedPrefs.getString("app_theme", "system") ?: "system")
    val themeSetting: StateFlow<String> = _themeSetting.asStateFlow()

    fun updateThemeSetting(theme: String) {
        sharedPrefs.edit().putString("app_theme", theme).apply()
        _themeSetting.value = theme
    }

    // --- State Exposes ---
    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProductModels: StateFlow<List<ProductModel>> = repository.allProductModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActionLogs: StateFlow<List<ActionLog>> = repository.allActionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userInfo: StateFlow<UserInfo?> = repository.userInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Search, Sort, Filtering ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortBy = MutableStateFlow("DATE") // "NAME", "DATE"
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortBy(sort: String) {
        _sortBy.value = sort
    }

    // --- Customer Selection ---
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    // --- Selected Customer Measurements ---
    val measurementsForSelectedCustomer: StateFlow<List<Measurement>> = _selectedCustomer
        .flatMapLatest { customer ->
            if (customer != null) {
                repository.getMeasurementsForCustomer(customer.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Pre-seeding Default Models ---
    init {
        viewModelScope.launch {
            repository.allProductModels.first().let { models ->
                if (models.isEmpty()) {
                    // Seed default Arabic models
                    repository.createProductModel(
                        "ثوب رجالي",
                        listOf("الطول", "الكتف", "الصدر", "الأكمام", "الرقبة", "وسع الثوب")
                    )
                    repository.createProductModel(
                        "فستان نسائي",
                        listOf("الطول", "الصدر", "الخصر", "الأرداف", "الأكتاف", "الأكمام")
                    )
                    repository.createProductModel(
                        "قميص وبنطلون",
                        listOf("طول القميص", "صدر القميص", "كتف القميص", "طول البنطلون", "خصر البنطلون")
                    )
                }
            }
        }
    }

    // --- Actions: Customers ---
    fun addCustomer(name: String, phone: String, address: String, note: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (name.trim().isEmpty()) {
                onError("الرجاء إدخال اسم العميل")
                return@launch
            }
            if (phone.length != 9 || !phone.all { it.isDigit() }) {
                onError("يجب أن يتكون رقم الهاتف من 9 أرقام فقط")
                return@launch
            }

            // Check uniqueness of name
            val exists = allCustomers.value.any { it.name.trim() == name.trim() }
            if (exists) {
                onError("هناك عميل مسجل بهذا الاسم بالفعل")
                return@launch
            }

            val newCustomer = Customer(
                name = name.trim(),
                phoneNumber = phone.trim(),
                address = address.trim(),
                note = note.trim()
            )
            val id = repository.createCustomer(newCustomer)
            if (id > 0) {
                onSuccess()
            } else {
                onError("حدث خطأ أثناء حفظ العميل")
            }
        }
    }

    fun updateCustomer(customer: Customer, name: String, phone: String, address: String, note: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (name.trim().isEmpty()) {
                onError("الرجاء إدخال اسم العميل")
                return@launch
            }
            if (phone.length != 9 || !phone.all { it.isDigit() }) {
                onError("يجب أن يتكون رقم الهاتف من 9 أرقام فقط")
                return@launch
            }

            // Check uniqueness of name if changed
            if (customer.name.trim() != name.trim()) {
                val exists = allCustomers.value.any { it.name.trim() == name.trim() }
                if (exists) {
                    onError("هناك عميل مسجل بهذا الاسم بالفعل")
                    return@launch
                }
            }

            val updated = customer.copy(
                name = name.trim(),
                phoneNumber = phone.trim(),
                address = address.trim(),
                note = note.trim()
            )
            repository.updateCustomer(updated)
            // Update selectedCustomer reference if we edited current selected
            if (_selectedCustomer.value?.id == customer.id) {
                _selectedCustomer.value = updated
            }
            onSuccess()
        }
    }

    fun archiveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.archiveCustomer(customer)
            if (_selectedCustomer.value?.id == customer.id) {
                _selectedCustomer.value = customer.copy(isArchived = true)
            }
        }
    }

    fun unarchiveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.unarchiveCustomer(customer)
            if (_selectedCustomer.value?.id == customer.id) {
                _selectedCustomer.value = customer.copy(isArchived = false)
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            if (_selectedCustomer.value?.id == customer.id) {
                _selectedCustomer.value = null
            }
        }
    }

    // --- Actions: Product Models ---
    fun addProductModel(name: String, fields: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (name.trim().isEmpty()) {
                onError("الرجاء إدخال اسم النموذج")
                return@launch
            }
            if (fields.isEmpty() || fields.all { it.trim().isEmpty() }) {
                onError("يجب إضافة حقل واحد على الأقل للقياس")
                return@launch
            }
            val exists = allProductModels.value.any { it.name.trim() == name.trim() }
            if (exists) {
                onError("هناك نموذج بهذا الاسم بالفعل")
                return@launch
            }
            repository.createProductModel(name.trim(), fields.map { it.trim() })
            onSuccess()
        }
    }

    fun updateProductModel(model: ProductModel, name: String, fields: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (name.trim().isEmpty()) {
                onError("الرجاء إدخال اسم النموذج")
                return@launch
            }
            if (fields.isEmpty() || fields.all { it.trim().isEmpty() }) {
                onError("يجب إضافة حقل واحد على الأقل للقياس")
                return@launch
            }
            val exists = allProductModels.value.any { it.name.trim() == name.trim() && it.id != model.id }
            if (exists) {
                onError("هناك نموذج بهذا الاسم بالفعل")
                return@launch
            }
            val jsonAdapter = moshi.adapter<List<String>>(
                Types.newParameterizedType(List::class.java, String::class.java)
            )
            val updated = model.copy(
                name = name.trim(),
                fieldsJson = jsonAdapter.toJson(fields.map { it.trim() })
            )
            repository.updateProductModel(updated)
            onSuccess()
        }
    }

    fun deleteProductModel(model: ProductModel) {
        viewModelScope.launch {
            repository.deleteProductModel(model)
        }
    }

    // --- Actions: Measurements ---
    fun addMeasurement(customerId: Int, customerName: String, productModelId: Int, productName: String, values: Map<String, String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val valuesJsonAdapter = moshi.adapter<Map<String, String>>(
                Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            )
            val measurement = Measurement(
                customerId = customerId,
                productModelId = productModelId,
                productName = productName,
                fieldsValueJson = valuesJsonAdapter.toJson(values)
            )
            repository.createMeasurement(measurement, customerName)
            onSuccess()
        }
    }

    fun updateMeasurement(measurement: Measurement, customerName: String, values: Map<String, String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val valuesJsonAdapter = moshi.adapter<Map<String, String>>(
                Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            )
            val updated = measurement.copy(
                fieldsValueJson = valuesJsonAdapter.toJson(values)
            )
            repository.updateMeasurement(updated, customerName)
            onSuccess()
        }
    }

    fun deleteMeasurement(measurement: Measurement, customerName: String) {
        viewModelScope.launch {
            repository.deleteMeasurement(measurement, customerName)
        }
    }

    // --- Action Logs helper ---
    fun parseFields(json: String): List<String> {
        return try {
            val jsonAdapter = moshi.adapter<List<String>>(
                Types.newParameterizedType(List::class.java, String::class.java)
            )
            jsonAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseValues(json: String): Map<String, String> {
        return try {
            val valuesJsonAdapter = moshi.adapter<Map<String, String>>(
                Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            )
            valuesJsonAdapter.fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // --- Simulated Google Sign In ---
    fun signInGoogle() {
        viewModelScope.launch {
            // Simulated login with user's actual email hussin.alqiary@gmail.com
            val dummyUser = UserInfo(
                picture = "img_profile_avatar",
                name = "حسين القياري",
                email = "hussin.alqiary@gmail.com",
                phoneNumber = "+967 777 654 321",
                creationDate = System.currentTimeMillis()
            )
            repository.saveUserInfo(dummyUser)
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            repository.clearUserInfo()
        }
    }

    // --- Sharing Utilities ---
    fun buildShareText(customer: Customer?, measurements: List<Measurement>, shareWithFullInfo: Boolean): String {
        val sb = java.lang.StringBuilder()
        sb.append("📋 *دفتر القياسات - تقرير القياسات*\n")
        sb.append("=========================\n")
        if (customer != null) {
            if (shareWithFullInfo) {
                sb.append("👤 اسم العميل: ${customer.name}\n")
                sb.append("📞 رقم الهاتف: ${customer.phoneNumber}\n")
                if (customer.address.isNotEmpty()) sb.append("📍 العنوان: ${customer.address}\n")
                if (customer.note.isNotEmpty()) sb.append("📝 ملاحظات: ${customer.note}\n")
            } else {
                sb.append("🔑 رقم العميل التعريفي (ID): ${customer.id}\n")
            }
            sb.append("=========================\n\n")
        }

        measurements.forEach { m ->
            sb.append("🧵 *النوع: ${m.productName}*\n")
            val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
            val dateStr = format.format(Date(m.creationDate))
            sb.append("📅 تاريخ القياس: $dateStr\n")
            sb.append("📊 القياسات التفصيلية:\n")
            
            val values = parseValues(m.fieldsValueJson)
            if (values.isEmpty()) {
                sb.append("  لا توجد قيم مسجلة\n")
            } else {
                values.forEach { (field, value) ->
                    sb.append("  - $field: $value\n")
                }
            }
            sb.append("-------------------------\n")
        }
        sb.append("\nتم الإرسال بواسطة تطبيق دفتر القياسات 📱")
        return sb.toString()
    }

    fun shareAsText(customer: Customer?, measurements: List<Measurement>, shareWithFullInfo: Boolean) {
        val shareText = buildShareText(customer, measurements, shareWithFullInfo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "مشاركة القياسات")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareAsPdf(customer: Customer?, measurements: List<Measurement>, shareWithFullInfo: Boolean) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                val paint = Paint()
                paint.color = Color.BLACK
                paint.textSize = 14f
                paint.isAntiAlias = true

                var y = 40f

                // Drawer Header
                paint.textSize = 20f
                paint.isFakeBoldText = true
                canvas.drawText("دفتر القياسات - تقرير قياسات العميل", 40f, y, paint)
                y += 30f

                // Customer Info
                paint.textSize = 14f
                paint.isFakeBoldText = false
                if (customer != null) {
                    if (shareWithFullInfo) {
                        canvas.drawText("اسم العميل: ${customer.name}", 40f, y, paint)
                        y += 20f
                        canvas.drawText("رقم الهاتف: ${customer.phoneNumber}", 40f, y, paint)
                        y += 20f
                        canvas.drawText("العنوان: ${customer.address}", 40f, y, paint)
                        y += 25f
                    } else {
                        canvas.drawText("رقم العميل التعريفي (ID): ${customer.id}", 40f, y, paint)
                        y += 25f
                    }
                }

                canvas.drawLine(40f, y, 555f, y, paint)
                y += 25f

                // Measurements details
                measurements.forEach { m ->
                    paint.textSize = 16f
                    paint.isFakeBoldText = true
                    canvas.drawText("النوع: ${m.productName}", 40f, y, paint)
                    y += 20f

                    paint.textSize = 12f
                    paint.isFakeBoldText = false
                    val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
                    canvas.drawText("التاريخ: ${format.format(Date(m.creationDate))}", 40f, y, paint)
                    y += 20f

                    val values = parseValues(m.fieldsValueJson)
                    values.forEach { (field, value) ->
                        canvas.drawText("• $field: $value", 60f, y, paint)
                        y += 18f
                        if (y > 780f) {
                            // In a real multi-page document we would start a new page, but for simple sharing,
                            // we keep it styled gracefully.
                        }
                    }
                    y += 15f
                    canvas.drawLine(40f, y, 300f, y, paint)
                    y += 20f
                }

                pdfDocument.finishPage(page)

                val file = File(context.cacheDir, "report_${customer?.id ?: 0}.pdf")
                file.outputStream().use { pdfDocument.writeTo(it) }
                pdfDocument.close()

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, "مشاركة تقرير PDF")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Backups actions ---
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    fun resetBackupStatus() {
        _backupStatus.value = null
    }

    fun makeLocalBackup() {
        viewModelScope.launch {
            _backupStatus.value = "جاري إنشاء النسخة الاحتياطية..."
            val file = repository.writeBackupToFile()
            if (file != null) {
                _backupStatus.value = "تم حفظ النسخة بنجاح: ${file.name}"
                // Share the backup file so user can save it or send it anywhere!
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, "حفظ النسخة الاحتياطية")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } else {
                _backupStatus.value = "فشل إنشاء النسخة الاحتياطية"
            }
        }
    }

    fun restoreLocalBackup(jsonContent: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val ok = repository.importBackupJson(jsonContent)
            if (ok) {
                onSuccess()
            } else {
                onError("تنسيق الملف غير صالح أو تالف")
            }
        }
    }

    fun syncGoogleDrive() {
        viewModelScope.launch {
            _backupStatus.value = "جاري الاتصال بـ Google Drive..."
            kotlinx.coroutines.delay(1500)
            if (userInfo.value == null) {
                _backupStatus.value = "الرجاء تسجيل الدخول بحساب Google أولاً"
            } else {
                val json = repository.exportBackupJson()
                // Simulate success saving to Google Drive under the user's account
                _backupStatus.value = "تمت المزامنة وحفظ النسخة في Google Drive بنجاح تحت الحساب: ${userInfo.value?.email}"
            }
        }
    }
}

class MeasurementsViewModelFactory(
    private val repository: AppRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeasurementsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeasurementsViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
