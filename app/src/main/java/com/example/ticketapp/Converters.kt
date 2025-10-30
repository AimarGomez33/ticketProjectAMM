// In a new file, e.g., Converters.kt
package com.example.ticketapp

import androidx.compose.ui.input.key.type
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.ticketapp.AppDatabase
import com.example.ticketapp.OrderEntity

class Converters {


    @TypeConverter
    fun fromStringList(value: String): List<String> {
        // Define the type of the list for Gson
        val listType = object : TypeToken<List<String>>() {}.type
        // Use an instance of Gson to convert the JSON string back to a List
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        // Use an instance of Gson to convert the List to a JSON string
        return Gson().toJson(list)
    }

    // You can add other converters for different types here.
    // For example, for a list of OrderItemEntity if you were to use that approach:
    @TypeConverter
    fun fromOrderItemList(value: String): List<OrderItemEntity> {
        val listType = object : TypeToken<List<OrderItemEntity>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toOrderItemList(list: List<OrderItemEntity>): String {
        return Gson().toJson(list)
    }


    @TypeConverter
    fun fromTicketItemList(value: List<OrderEntity>?): String? {
        if (value == null) {
            return null
        }
        val type = object : TypeToken<List<OrderEntity>>() {}.type
        return Gson().toJson(value, type)
    }
    // You can add other converters here for different types, like Date, etc.
}
