package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.ActionLog
import com.example.data.model.Customer
import com.example.data.model.Measurement
import com.example.data.model.ProductModel
import com.example.data.model.UserInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInfoDao {
    @Query("SELECT * FROM user_info WHERE id = 1 LIMIT 1")
    fun getUserInfo(): Flow<UserInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInfo(userInfo: UserInfo)

    @Query("DELETE FROM user_info")
    suspend fun deleteUserInfo()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY creationDate DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): Customer?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)
}

@Dao
interface ProductModelDao {
    @Query("SELECT * FROM product_models ORDER BY id DESC")
    fun getAllProductModels(): Flow<List<ProductModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductModel(productModel: ProductModel): Long

    @Update
    suspend fun updateProductModel(productModel: ProductModel)

    @Delete
    suspend fun deleteProductModel(productModel: ProductModel)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE customerId = :customerId ORDER BY creationDate DESC")
    fun getMeasurementsForCustomer(customerId: Int): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements ORDER BY creationDate DESC")
    fun getAllMeasurements(): Flow<List<Measurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: Measurement): Long

    @Update
    suspend fun updateMeasurement(measurement: Measurement)

    @Delete
    suspend fun deleteMeasurement(measurement: Measurement)
}

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActionLog>>

    @Query("SELECT * FROM action_logs WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getLogsForCustomer(customerId: Int): Flow<List<ActionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActionLog)
}

@Database(
    entities = [
        UserInfo::class,
        Customer::class,
        ProductModel::class,
        Measurement::class,
        ActionLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userInfoDao(): UserInfoDao
    abstract fun customerDao(): CustomerDao
    abstract fun productModelDao(): ProductModelDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun actionLogDao(): ActionLogDao
}
