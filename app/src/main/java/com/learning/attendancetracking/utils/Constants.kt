package com.learning.attendancetracking.utils

import timber.log.Timber

const val MIN_FACE_SIZE = 0.3F
const val LIVE_FACE_MIN_PROBABILITY = 0.8F
const val LIVE_FACE_MAX_PROBABILITY = 0.2F
const val FACE_MATCH_MIN_CONFIDENCE = 0.8F
const val FACE_MATCH_COMPARISON_CONFIDENCE = 1F
const val USER_ID_MIN_LENGTH = 1

//const val FACE_NET_INPUT_IMAGE_SIZE = 112
//const val FACE_NET_INPUT_IMAGE_SIZE_LARGE = 160
//const val FACE_OUTPUT_SIZE = 192
//const val FACE_OUTPUT_SIZE_LARGE = 512
const val DEFAULT_GEOFENCE_RADIUS: Int = 20
const val FACE_NET_MODEL_FILE_NAME = "mobile_face_net.tflite"
const val FACE_NET_MODEL_FILE_NAME_LARGE = "mobile_face_net_large.tflite"

fun getModelFileName(useFaceNet: Boolean): String {
    return if (useFaceNet) {
        FACE_NET_MODEL_FILE_NAME_LARGE
    } else {
        FACE_NET_MODEL_FILE_NAME
    }.also {
        Timber.d("Using facenet model $it")
    }
}