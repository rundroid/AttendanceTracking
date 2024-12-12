package com.learning.attendancetracking.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


inline fun <reified Type> preferenceOf(
    preferences: SharedPreferences,
    key: String,
    defaultValue: Type
): ReadWriteProperty<Any, Type?> {

    return object : ReadWriteProperty<Any, Type?> {

        override fun getValue(thisRef: Any, property: KProperty<*>): Type? {
            return when (Type::class) {
                Boolean::class -> preferences.getBoolean(key, defaultValue as Boolean) as Type
                Int::class -> preferences.getInt(key, defaultValue as Int) as Type
                else -> throw IllegalArgumentException("Can't get value of type ${Type::class.java.name} in preferences")
            }
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Type?) {
            preferences.edit {
                when (Type::class) {
                    Boolean::class -> putBoolean(key, value as Boolean)
                    Int::class -> putInt(key, value as Int)
                    else -> throw IllegalArgumentException("Can't save value of type ${Type::class.java.name} in preferences")
                }
            }
        }
    }
}
