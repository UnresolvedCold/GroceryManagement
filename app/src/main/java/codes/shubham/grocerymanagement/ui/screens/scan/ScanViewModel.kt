package codes.shubham.grocerymanagement.ui.screens.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import codes.shubham.grocerymanagement.data.remote.GeminiService
import codes.shubham.grocerymanagement.data.remote.PendingScan
import codes.shubham.grocerymanagement.data.remote.ProductScanResult
import codes.shubham.grocerymanagement.data.remote.ScanResultStore
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor

enum class ScanMode { BARCODE, PHOTO }

sealed interface ScanEvent {
    data class NavigateToProduct(val productId: Long) : ScanEvent
    data object NavigateToAddEdit : ScanEvent
}

data class ScanUiState(
    val mode: ScanMode = ScanMode.BARCODE,
    val cameraPermissionGranted: Boolean = false,
    val detectedBarcode: String? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val scanResult: ProductScanResult? = null
)

class ScanViewModel(
    private val groceryRepository: GroceryRepository,
    private val geminiService: GeminiService,
    private val prefsRepository: UserPreferencesRepository,
    private val scanResultStore: ScanResultStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<ScanEvent?>(null)
    val events: StateFlow<ScanEvent?> = _events.asStateFlow()

    private var imageCapture: ImageCapture? = null
    private var isBarcodeProcessing = false

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(cameraPermissionGranted = granted) }
    }

    fun setMode(mode: ScanMode) {
        _uiState.update { it.copy(mode = mode, detectedBarcode = null, errorMessage = null) }
    }

    fun setImageCapture(capture: ImageCapture) {
        imageCapture = capture
    }

    @ExperimentalGetImage
    fun processImageForBarcode(imageProxy: ImageProxy) {
        if (isBarcodeProcessing || _uiState.value.detectedBarcode != null) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let { barcode ->
                    isBarcodeProcessing = true
                    _uiState.update { it.copy(detectedBarcode = barcode) }
                    handleBarcodeDetected(barcode)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun handleBarcodeDetected(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val existing = groceryRepository.getProductByBarcode(barcode)
            if (existing != null) {
                _events.value = ScanEvent.NavigateToProduct(existing.id)
            } else {
                val info = geminiService.lookupBarcode(barcode)
                _uiState.update { it.copy(scanResult = info, isProcessing = false) }
            }
        }
    }

    fun proceedWithBarcode() {
        scanResultStore.store(
            PendingScan(
                barcode = _uiState.value.detectedBarcode,
                scanResult = _uiState.value.scanResult
            )
        )
        _events.value = ScanEvent.NavigateToAddEdit
    }

    fun capturePhoto(executor: Executor, filesDir: File) {
        val capture = imageCapture ?: return
        _uiState.update { it.copy(isProcessing = true) }
        val imageFile = File(filesDir, "images/${System.currentTimeMillis()}.jpg").also {
            it.parentFile?.mkdirs()
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
        capture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap != null) analyzePhoto(bitmap, imageFile.absolutePath)
                else {
                    scanResultStore.store(PendingScan(imagePath = imageFile.absolutePath))
                    _uiState.update { it.copy(isProcessing = false) }
                    _events.value = ScanEvent.NavigateToAddEdit
                }
            }
            override fun onError(exception: ImageCaptureException) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Photo capture failed") }
            }
        })
    }

    private fun analyzePhoto(bitmap: Bitmap, imagePath: String) {
        viewModelScope.launch {
            val apiKey = prefsRepository.userPreferences.first().geminiApiKey
            val result = if (apiKey.isNotBlank()) geminiService.analyzeProductImage(bitmap, apiKey) else null
            scanResultStore.store(PendingScan(imagePath = imagePath, scanResult = result))
            _uiState.update { it.copy(isProcessing = false, scanResult = result) }
            _events.value = ScanEvent.NavigateToAddEdit
        }
    }

    fun resetBarcode() {
        isBarcodeProcessing = false
        _uiState.update { it.copy(detectedBarcode = null, scanResult = null, errorMessage = null) }
    }

    fun consumeEvent() {
        _events.value = null
        isBarcodeProcessing = false
    }
}
