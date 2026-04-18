package codes.shubham.grocerymanagement.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onNavigateToProduct: (Long) -> Unit,
    onNavigateToAddEdit: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ScanViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(event) {
        when (val e = event) {
            is ScanEvent.NavigateToProduct -> {
                viewModel.consumeEvent()
                onNavigateToProduct(e.productId)
            }
            is ScanEvent.NavigateToAddEdit -> {
                viewModel.consumeEvent()
                onNavigateToAddEdit()
            }
            null -> {}
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onCameraPermissionResult(granted) }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.onCameraPermissionResult(true)
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!state.cameraPermissionGranted) {
        CameraPermissionDenied(onNavigateBack = onNavigateBack)
        return
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            try { cameraProviderFuture.get()?.unbindAll() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(state.cameraPermissionGranted, state.mode) {
        if (!state.cameraPermissionGranted) return@LaunchedEffect
        bindCamera(
            cameraProviderFuture = cameraProviderFuture,
            previewView = previewView,
            lifecycleOwner = lifecycleOwner,
            executor = cameraExecutor,
            mode = state.mode,
            onImageCapture = { viewModel.setImageCapture(it) },
            onBarcodeFrame = { proxy -> if (state.mode == ScanMode.BARCODE) viewModel.processImageForBarcode(proxy) }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(16.dp)
                .statusBarsPadding()
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
        }

        // Scan frame overlay (barcode mode)
        if (state.mode == ScanMode.BARCODE) {
            ScanFrameOverlay(detected = state.detectedBarcode != null)
        }

        // Bottom panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode switcher
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.8f)) {
                SegmentedButton(
                    selected = state.mode == ScanMode.BARCODE,
                    onClick = { viewModel.setMode(ScanMode.BARCODE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(16.dp))
                        Text("Barcode")
                    }
                }
                SegmentedButton(
                    selected = state.mode == ScanMode.PHOTO,
                    onClick = { viewModel.setMode(ScanMode.PHOTO) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                        Text("Photo")
                    }
                }
            }

            AnimatedContent(targetState = state.mode, label = "scan_content") { mode ->
                when (mode) {
                    ScanMode.BARCODE -> BarcodePanel(
                        barcode = state.detectedBarcode,
                        scanResult = state.scanResult,
                        isProcessing = state.isProcessing,
                        onProceed = viewModel::proceedWithBarcode,
                        onRetry = viewModel::resetBarcode
                    )
                    ScanMode.PHOTO -> PhotoPanel(
                        isProcessing = state.isProcessing,
                        onCapture = {
                            viewModel.capturePhoto(cameraExecutor, context.filesDir)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanFrameOverlay(detected: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val color = if (detected) Color(0xFF4CAF50) else Color.White
        Box(
            modifier = Modifier
                .size(250.dp, 150.dp)
                .border(2.dp, color, RoundedCornerShape(12.dp))
        )
        Text(
            text = if (detected) "Barcode detected!" else "Align barcode within the frame",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 90.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BarcodePanel(
    barcode: String?,
    scanResult: codes.shubham.grocerymanagement.data.remote.ProductScanResult?,
    isProcessing: Boolean,
    onProceed: () -> Unit,
    onRetry: () -> Unit
) {
    if (isProcessing) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Text("Looking up product...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    if (barcode != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scanned: $barcode", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    scanResult?.let {
                        Text(it.name, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        it.brand?.let { b -> Text(b, color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(0.5f))
                ) { Text("Scan again") }
                Button(onClick = onProceed) { Text("Add Product") }
            }
        }
    } else {
        Text(
            "Point your camera at a product barcode",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhotoPanel(isProcessing: Boolean, onCapture: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Take a photo of the product for AI analysis",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        if (isProcessing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Text("Analyzing photo...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            IconButton(
                onClick = onCapture,
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.CameraAlt, "Capture", tint = Color.Black, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun CameraPermissionDenied(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("Camera Permission Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Camera access is needed to scan barcodes and take product photos. Please grant permission in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNavigateBack) { Text("Go Back") }
    }
}

private fun bindCamera(
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    executor: ExecutorService,
    mode: ScanMode,
    onImageCapture: (ImageCapture) -> Unit,
    onBarcodeFrame: (ImageProxy) -> Unit
) {
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        onImageCapture(imageCapture)

        val useCases = when (mode) {
            ScanMode.BARCODE -> {
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { analysis ->
                        analysis.setAnalyzer(executor) { proxy -> onBarcodeFrame(proxy) }
                    }
                arrayOf(preview, analyzer, imageCapture)
            }
            ScanMode.PHOTO -> arrayOf(preview, imageCapture)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                *useCases
            )
        } catch (_: Exception) {}
    }, ContextCompat.getMainExecutor(previewView.context))
}
