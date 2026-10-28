package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class BackupData(
    val customers: List<Customer> = emptyList(),
    val productModels: List<ProductModel> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val actionLogs: List<ActionLog> = emptyList()
)

class AppRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    private val userInfoDao = db.userInfoDao()
    private val customerDao = db.customerDao()
    private val productModelDao = db.productModelDao()
    private val measurementDao = db.measurementDao()
    private val actionLogDao = db.actionLogDao()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // --- User Info ---
    val userInfo: Flow<UserInfo?> = userInfoDao.getUserInfo()

    suspend fun saveUserInfo(userInfo: UserInfo) {
        userInfoDao.insertUserInfo(userInfo)
    }

    suspend fun clearUserInfo() {
        userInfoDao.deleteUserInfo()
    }

    // --- Customers ---
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun getCustomerById(id: Int): Customer? = customerDao.getCustomerById(id)

    suspend fun createCustomer(customer: Customer): Long {
        return withContext(Dispatchers.IO) {
            val customerId = customerDao.insertCustomer(customer)
            val log = ActionLog(
                customerId = customerId.toInt(),
                customerName = customer.name,
                actionType = "CREATE",
                description = "تم إنشاء ملف العميل: ${customer.name}"
            )
            actionLogDao.insertLog(log)
            customerId
        }
    }

    suspend fun updateCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            val oldCustomer = customerDao.getCustomerById(customer.id)
            customerDao.updateCustomer(customer)

            val changes = mutableListOf<String>()
            if (oldCustomer != null) {
                if (oldCustomer.phoneNumber != customer.phoneNumber) {
                    changes.add("رقم الهاتف (${oldCustomer.phoneNumber} -> ${customer.phoneNumber})")
                }
                if (oldCustomer.address != customer.address) {
                    changes.add("العنوان (${oldCustomer.address} -> ${customer.address})")
                }
                if (oldCustomer.note != customer.note) {
                    changes.add("الملاحظات")
                }
            }

            val changeDesc = if (changes.isNotEmpty()) {
                "تم تعديل: ${changes.joinToString("، ")}"
            } else {
                "تم تحديث بيانات العميل"
            }

            val log = ActionLog(
                customerId = customer.id,
                customerName = customer.name,
                actionType = "UPDATE",
                description = changeDesc
            )
            actionLogDao.insertLog(log)
        }
    }

    suspend fun archiveCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            val updated = customer.copy(isArchived = true)
            customerDao.updateCustomer(updated)
            val log = ActionLog(
                customerId = customer.id,
                customerName = customer.name,
                actionType = "ARCHIVE",
                description = "تم نقل العميل إلى الأرشيف"
            )
            actionLogDao.insertLog(log)
        }
    }

    suspend fun unarchiveCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            val updated = customer.copy(isArchived = false)
            customerDao.updateCustomer(updated)
            val log = ActionLog(
                customerId = customer.id,
                customerName = customer.name,
                actionType = "UNARCHIVE",
                description = "تم استعادة العميل من الأرشيف"
            )
            actionLogDao.insertLog(log)
        }
    }

    suspend fun deleteCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            customerDao.deleteCustomer(customer)
            val log = ActionLog(
                customerId = customer.id,
                customerName = customer.name,
                actionType = "DELETE",
                description = "تم حذف ملف العميل بالكامل"
            )
            actionLogDao.insertLog(log)
        }
    }

    // --- Product Models ---
    val allProductModels: Flow<List<ProductModel>> = productModelDao.getAllProductModels()

    suspend fun createProductModel(name: String, fields: List<String>): Long {
        return withContext(Dispatchers.IO) {
            val jsonAdapter = moshi.adapter<List<String>>(
                Types.newParameterizedType(List::class.java, String::class.java)
            )
            val fieldsJson = jsonAdapter.toJson(fields)
            val model = ProductModel(name = name, fieldsJson = fieldsJson)
            productModelDao.insertProductModel(model)
        }
    }

    suspend fun updateProductModel(model: ProductModel) {
        withContext(Dispatchers.IO) {
            productModelDao.updateProductModel(model)
        }
    }

    suspend fun deleteProductModel(model: ProductModel) {
        withContext(Dispatchers.IO) {
            productModelDao.deleteProductModel(model)
        }
    }

    // --- Measurements ---
    val allMeasurements: Flow<List<Measurement>> = measurementDao.getAllMeasurements()

    fun getMeasurementsForCustomer(customerId: Int): Flow<List<Measurement>> {
        return measurementDao.getMeasurementsForCustomer(customerId)
    }

    suspend fun createMeasurement(measurement: Measurement, customerName: String) {
        withContext(Dispatchers.IO) {
            val id = measurementDao.insertMeasurement(measurement)
            val log = ActionLog(
                customerId = measurement.customerId,
                customerName = customerName,
                actionType = "MEASURE_CREATE",
                description = "تم إضافة قياس جديد: ${measurement.productName}"
            )
            actionLogDao.insertLog(log)
        }
    }

    suspend fun updateMeasurement(measurement: Measurement, customerName: String) {
        withContext(Dispatchers.IO) {
            measurementDao.updateMeasurement(measurement)
            val log = ActionLog(
                customerId = measurement.customerId,
                customerName = customerName,
                actionType = "MEASURE_UPDATE",
                description = "تم تعديل قياس: ${measurement.productName}"
            )
            actionLogDao.insertLog(log)
        }
    }

    suspend fun deleteMeasurement(measurement: Measurement, customerName: String) {
        withContext(Dispatchers.IO) {
            measurementDao.deleteMeasurement(measurement)
            val log = ActionLog(
                customerId = measurement.customerId,
                customerName = customerName,
                actionType = "MEASURE_DELETE",
                description = "تم حذف قياس: ${measurement.productName}"
            )
            actionLogDao.insertLog(log)
        }
    }

    // --- Action Logs ---
    val allActionLogs: Flow<List<ActionLog>> = actionLogDao.getAllLogs()

    fun getLogsForCustomer(customerId: Int): Flow<List<ActionLog>> {
        return actionLogDao.getLogsForCustomer(customerId)
    }

    // --- Backup & Restore ---
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val customers = customerDao.getAllCustomers().first()
        val models = productModelDao.getAllProductModels().first()
        val measurements = measurementDao.getAllMeasurements().first()
        val logs = actionLogDao.getAllLogs().first()

        val backup = BackupData(
            customers = customers,
            productModels = models,
            measurements = measurements,
            actionLogs = logs
        )

        val adapter = moshi.adapter(BackupData::class.java)
        adapter.toJson(backup)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = adapter.fromJson(jsonString) ?: return@withContext false

            // Import them cleanly
            // To prevent conflicts or clean import, we can insert them one by one.
            // Since this is a restore, we can overwrite or insert on conflict replace.
            // For simple implementation, we insert customers first, then models, then measurements, then logs.
            backup.customers.forEach {
                try { customerDao.insertCustomer(it) } catch (e: Exception) { customerDao.updateCustomer(it) }
            }
            backup.productModels.forEach {
                productModelDao.insertProductModel(it)
            }
            backup.measurements.forEach {
                measurementDao.insertMeasurement(it)
            }
            backup.actionLogs.forEach {
                actionLogDao.insertLog(it)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun writeBackupToFile(): File? = withContext(Dispatchers.IO) {
        try {
            val json = exportBackupJson()
            val format = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH)
            val dateStr = format.format(Date())
            val fileName = "measurements_backup_$dateStr.json"
            
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, fileName)
            file.writeText(json)
            file
        } catch (e: Exception) {
            null
        }
    }
}
