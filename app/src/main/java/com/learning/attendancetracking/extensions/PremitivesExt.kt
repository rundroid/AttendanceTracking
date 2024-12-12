package com.learning.attendancetracking.extensions

fun Boolean.yesOrNo(): String {
    return if (this) {
        "YES"
    } else {
        "NO"
    }
}