package com.learning.attendancetracking.detection

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import timber.log.Timber
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FaceNetInterpreter {

    var interpreter: Interpreter? = null
        private set
    private val _initializationState = MutableStateFlow(false)
    val initializationState = _initializationState.asStateFlow()
    private var inputSize: Int = 0
    var outputSize: Int = 0
        private set
    lateinit var imageProcessor: ImageProcessor
        private set

    @OptIn(ExperimentalUuidApi::class)
    suspend fun initializeInterpreter(context: Context, assetFileName: String) {
        if (_initializationState.value) {
            Timber.d("Interpreter already initialized")
            return
        }
        val owner = Uuid.random().toHexString()
        INIT_LOCK.lock(owner)
        if (_initializationState.value) {
            Timber.d("Interpreter already initialized")
            INIT_LOCK.unlock(owner)
            return
        }
        val mInterpreter = Interpreter(
            FileUtil.loadMappedFile(context, assetFileName),
            Interpreter.Options().apply {
                useNNAPI = true
                numThreads = INTERPRETER_THREADS
                isCancellable = true
                runtime = InterpreterApi.Options.TfLiteRuntime.PREFER_SYSTEM_OVER_APPLICATION
            })
        inputSize = mInterpreter.getInputTensor(0).shape()[1]
        outputSize = mInterpreter.getOutputTensor(0).shape()[1]
        interpreter = mInterpreter
        imageProcessor = createImageProcessor()
        _initializationState.update { true }
        Timber.d("Interpreter initialized")
        INIT_LOCK.unlock(owner)
    }

    fun releaseInterpreter() {
        val tempInterpreter = interpreter
        interpreter = null
        _initializationState.update { false }
        tempInterpreter?.run {
            setCancelled(true)
            close()
            Timber.d("Interpreter closed")
        }
    }

    private fun createImageProcessor(): ImageProcessor {
        return ImageProcessor.Builder()
            .add(
                ResizeOp(
                    inputSize,
                    inputSize,
                    ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                )
            )
            .add(NormalizeOp(0f, 255f))
            .build()
    }

    companion object {
        const val INTERPRETER_THREADS = 3
        private val INIT_LOCK = Mutex()
    }
}