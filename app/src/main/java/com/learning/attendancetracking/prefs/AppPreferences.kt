package com.learning.attendancetracking.prefs

import android.content.Context
import android.content.SharedPreferences
import com.learning.attendancetracking.extensions.defaultPrefs
import com.learning.attendancetracking.utils.DEFAULT_GEOFENCE_RADIUS
import com.learning.attendancetracking.utils.orElse

class AppPreferences(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.defaultPrefs()
    }

    private var _useFaceNet: Boolean? by preferenceOf<Boolean>(
        sharedPreferences,
        KEY_USE_FACE_NET_MODEL,
        false
    )

    var useFaceNet: Boolean
        set(value) {
            _useFaceNet = value
        }
        get() = _useFaceNet.orElse { false }

    private var _geofenceRadius: Int? by preferenceOf<Int>(
        sharedPreferences,
        KEY_GEOFENCE_RADIUS,
        DEFAULT_GEOFENCE_RADIUS
    )

    var geofenceRadius: Int
        set(value) {
            _geofenceRadius = value
        }
        get() = _geofenceRadius.orElse { DEFAULT_GEOFENCE_RADIUS }

    companion object {
        private const val KEY_USE_FACE_NET_MODEL = "use_face_net_model"
        private const val KEY_GEOFENCE_RADIUS = "geofence_radius"
    }
}