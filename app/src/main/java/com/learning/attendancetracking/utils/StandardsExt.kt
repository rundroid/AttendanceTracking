package com.learning.attendancetracking.utils

inline fun <reified T> T?.orElse(default: T): T {
    return this ?: default
}

inline fun <reified T> T?.orElse(default: () -> T): T {
    return this ?: default()
}