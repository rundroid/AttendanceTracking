package com.learning.attendancetracking.camera

import android.content.Context
import android.graphics.Bitmap
import android.view.OrientationEventListener
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.face.Face
import com.learning.attendancetracking.detection.FaceDetectionHelper
import com.learning.attendancetracking.detection.hasMaxOpenProbability
import com.learning.attendancetracking.detection.hasMinOpenProbability
import com.learning.attendancetracking.prefs.AppPreferences
import com.learning.attendancetracking.utils.DataState
import com.learning.attendancetracking.utils.FACE_MATCH_MIN_CONFIDENCE
import com.learning.attendancetracking.utils.LIVE_FACE_MAX_PROBABILITY
import com.learning.attendancetracking.utils.LIVE_FACE_MIN_PROBABILITY
import com.learning.attendancetracking.utils.getModelFileName
import com.learning.attendancetracking.utils.rotate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named

@ExperimentalImageCaptureOutputFormat
@HiltViewModel
class CameraPreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("default") private val defaultDispatcher: CoroutineDispatcher,
    private val appPreferences: AppPreferences,
) : ViewModel(), DefaultLifecycleObserver {

    @Inject
    lateinit var faceDetectionHelper: FaceDetectionHelper

    private val _isBackCamera = MutableStateFlow(false)
    val isBackCamera = _isBackCamera.asStateFlow()

    private val _faceRectangles = MutableStateFlow(persistentListOf<Rect>())
    val faceRectangles = _faceRectangles.asStateFlow()
    private val _faceMatchedFlow = MutableStateFlow(false)
    val faceMatchedFlow = _faceMatchedFlow.distinctUntilChanged { old, new -> old == new }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    private val requiresFaceComparisonFlow = MutableStateFlow(false)
    private val _minEyeOpenProbabilityFlow = MutableStateFlow(false)
    private val _maxEyeOpenProbabilityFlow = MutableStateFlow(false)
    private val _liveFaceIdentifiedFlow =
        combine(_minEyeOpenProbabilityFlow, _maxEyeOpenProbabilityFlow) { min, max ->
            min && max
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val liveFaceIdentifiedFlow =
        _liveFaceIdentifiedFlow.distinctUntilChanged { old, new -> old == new }
            .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var sourceFaceData: FloatArray? = null

    var canvasSize = IntSize.Zero

    val captureImageWhenLiveRef = AtomicBoolean(false)
    var capturedImageListener: ((DataState<Bitmap>) -> Unit)? = null
    private var isFaceComparatorInitialized = false

    private val imageAnalyzerExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val capturePictureExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    val previewUseCase by lazy {
        Preview.Builder().setResolutionSelector(getResolutionSelector()).build()
    }

    val imageAnalysisUseCase by lazy {
        ImageAnalysis.Builder().setResolutionSelector(getResolutionSelector())
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer(imageAnalyzerExecutor, imageAnalyzer)
            }
    }

    private var imageCaptureUseCase: ImageCapture? = null
    fun getImageCaptureUseCase(): ImageCapture {
        imageCaptureUseCase?.let { return it }
        val mImageCaptureUseCase =
            ImageCapture.Builder().setResolutionSelector(getResolutionSelector())
                .setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
        imageCaptureUseCase = mImageCaptureUseCase
        return mImageCaptureUseCase
    }


    private val imageAnalyzer by lazy {
        ImageAnalysis.Analyzer { imageProxy ->
            canvasSize.takeIf { cs -> cs != IntSize.Zero } ?: run {
                imageProxy.close()
                return@Analyzer
            }
            findFaces(imageProxy)
        }
    }

    private fun getResolutionSelector(): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
            .build()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                screenRotationFlow().distinctUntilChanged().collectLatest { rotation ->
                    Timber.d("Screen rotation in flow: $rotation")
                    previewUseCase.targetRotation = rotation
                    imageAnalysisUseCase.targetRotation = rotation
                    imageCaptureUseCase?.targetRotation = rotation
                }
            }
        }
    }

    private fun screenRotationFlow(): Flow<Int> {
        return callbackFlow {
            val orientationEventListener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    runCatching {
                        UseCase.snapToSurfaceRotation(orientation)
                    }.onSuccess { rotation ->
                        Timber.d("Screen rotation: $rotation")
                        trySend(rotation)
                    }.onFailure { Timber.e(it) }
                }
            }
            orientationEventListener.enable()
            Timber.d("OrientationEventListener enabled")
            awaitClose {
                Timber.d("OrientationEventListener disabled")
                orientationEventListener.disable()
            }
        }
    }

    fun takePicture() {
        imageCaptureUseCase?.takePicture(
            capturePictureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureStarted() {
                    super.onCaptureStarted()
                    emitCaptureState(DataState.Loading)
                }

                override fun onCaptureProcessProgressed(progress: Int) {
                    super.onCaptureProcessProgressed(progress)
                    emitCaptureState(DataState.Progress(progress))
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val bitmap = image.toBitmap().rotate(image.imageInfo.rotationDegrees.toFloat())
                    image.close()
                    emitCaptureState(DataState.Success(bitmap))
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    emitCaptureState(DataState.Failure(exception))
                }
            })
    }

    private fun emitCaptureState(state: DataState<Bitmap>) {
        runBlocking {
            withContext(Dispatchers.Main.immediate) {
                capturedImageListener?.invoke(state)
            }
        }
    }

    fun initializeFaceComparator() {
        if (isFaceComparatorInitialized) {
            return
        }
        isFaceComparatorInitialized = true
        faceDetectionHelper.initializeInterpreter(
            context,
            getModelFileName(appPreferences.useFaceNet)
        )
        viewModelScope.launch(defaultDispatcher) {
            combine(
                _faceMatchedFlow,
                faceDetectionHelper.faceNetInterpreter.initializationState
            ) { faceMatched, initialized ->
                initialized && faceMatched.not()
            }.collect { value ->
                requiresFaceComparisonFlow.update {
                    value
                }
            }
        }
    }

    private fun findFaces(imageProxy: ImageProxy) {
        viewModelScope.launch(defaultDispatcher) {
            val captureImage = captureImageWhenLiveRef.get()
            runCatching {
                faceDetectionHelper.findFacesInImageProxy(
                    context,
                    imageProxy,
                    canvasSize,
                    _isBackCamera.value.not()
                )
            }.onSuccess { pair ->
                val (faces, rectangles) = pair
                onFacesDetected(imageProxy, faces, rectangles, captureImage)
            }.onFailure {
                Timber.e(it, "Error finding faces")
            }
            imageProxy.close()
        }
    }

    private suspend fun onFacesDetected(
        imageProxy: ImageProxy,
        faces: List<Face>,
        rectangles: List<Rect>,
        captureImage: Boolean
    ) {
        _faceRectangles.update { currentValue ->
            currentValue.mutate { list ->
                list.clear()
                list.addAll(rectangles)
            }
        }
        if (faces.size != 1) {
            _minEyeOpenProbabilityFlow.update { false }
            _maxEyeOpenProbabilityFlow.update { false }
            _faceMatchedFlow.update { false }
        } else {
            val face = faces[0]
            val mLiveFaceIdentified = face.hasMinOpenProbability(LIVE_FACE_MIN_PROBABILITY)
            _minEyeOpenProbabilityFlow.update {
                it || mLiveFaceIdentified
            }
            _maxEyeOpenProbabilityFlow.update {
                it || face.hasMaxOpenProbability(LIVE_FACE_MAX_PROBABILITY)
            }
            val mCaptureImage = mLiveFaceIdentified && captureImage
            if (requiresFaceComparisonFlow.value) {
                compareFace(imageProxy, faces[0], mCaptureImage.not())
            }
            if (mCaptureImage) {
                val bitmap = imageProxy.toBitmap()
                    .rotate(imageProxy.imageInfo.rotationDegrees.toFloat()) {
                        if (_isBackCamera.value.not()) {
                            postScale(-1F, 1F)
                        }
                    }
                captureImageWhenLiveRef.set(false)
                withContext(Dispatchers.Main.immediate) {
                    capturedImageListener?.invoke(DataState.Success(bitmap))
                }
            }
        }
    }

    private fun compareFace(imageProxy: ImageProxy, face: Face, recycle: Boolean) {
        val sourceData = sourceFaceData ?: return
        val distance =
            faceDetectionHelper.compareFace(sourceData, imageProxy, face.boundingBox, recycle)
                ?: return
        if (distance <= FACE_MATCH_MIN_CONFIDENCE) {
            _faceMatchedFlow.update { true }
        }
    }

    fun setSourceBitmap(bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        viewModelScope.launch(defaultDispatcher) {
            faceDetectionHelper.initializationState.filter { it }.first()
            kotlin.runCatching {
                faceDetectionHelper.readFacesData(
                    bitmap,
                    true
                ).values.firstOrNull()
            }.getOrNull()?.let {
                sourceFaceData = it
            }
        }
    }

    fun switchCamera() {
        resetFaceDetectionData()
        _isBackCamera.update {
            it.not()
        }
    }

    private fun resetFaceDetectionData() {
        _maxEyeOpenProbabilityFlow.update { false }
        _minEyeOpenProbabilityFlow.update { false }
        _faceMatchedFlow.update { false }
        clearFaceRectangles()
    }

    fun clearFaceRectangles() {
        _faceRectangles.update { currentValue ->
            currentValue.mutate { it.clear() }
        }
    }

    fun updateCanvasSize(newSize: IntSize) {
        Timber.d("updateCanvasSize: newSize=%s", newSize)
        canvasSize = newSize
    }

    override fun onCleared() {
        super.onCleared()
        imageAnalyzerExecutor.shutdown()
        capturePictureExecutor.shutdown()
        imageAnalysisUseCase.clearAnalyzer()
        faceDetectionHelper.dispose()
    }
}