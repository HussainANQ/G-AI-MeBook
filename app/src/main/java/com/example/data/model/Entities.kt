package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_info")
data class UserInfo(
    @PrimaryKey val id: Int = 1,
    val picture: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val creationDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["name"], unique = true)]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String, // Nine digits only
    val address: String,
    val note: String,
    val creationDate: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

@Entity(
    tableName = "product_models",
    indices = [Index(value = ["name"], unique = true)]
)
data class ProductModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // e.g., "ثوب يمني"
    val fieldsJson: String // JSON array of field names, e.g., ["الطول", "الكتف", "الصدر"]
)

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val productModelId: Int,
    val productName: String,
    val creationDate: Long = System.currentTimeMillis(),
    val fieldsValueJson: String // JSON map of fieldName to value, e.g., {"الطول":"142", "الكتف":"44"}
)

@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val actionType: String, // "CREATE", "UPDATE", "ARCHIVE", "UNARCHIVE", "MEASURE_CREATE", "MEASURE_UPDATE"
    val description: String, // Details in Arabic
    val timestamp: Long = System.currentTimeMillis()
)
