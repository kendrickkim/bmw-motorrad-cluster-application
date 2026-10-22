package com.dooderge.bmw.motorrad.cluster.tpms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.dooderge.bmw.motorrad.cluster.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Zontes 스타일 센서 QR 스캔.
 * QR 내용 → [TpmsQrParser] → 센서 hex ID 또는 BLE MAC.
 */
class TpmsQrScanActivity : ComponentActivity() {
    companion object {
        const val EXTRA_RESULT = "tpms_qr_result"
        private const val TAG = "TpmsQrScan"
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val handled = AtomicBoolean(false)
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39)
            .build()
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tpms_qr_scan)
        findViewById<TextView>(R.id.tpms_qr_hint).text =
            "센서 QR을 프레임 안에 맞추세요\n(HHHHHH&HHHHHH 또는 6hex ID)"
        findViewById<View>(R.id.tpms_qr_close).setOnClickListener { finish() }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val previewView = findViewById<PreviewView>(R.id.tpms_qr_preview)
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { proxy ->
                if (handled.get()) {
                    proxy.close()
                    return@setAnalyzer
                }
                val media = proxy.image
                if (media == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                        if (!raw.isNullOrBlank() && handled.compareAndSet(false, true)) {
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(EXTRA_RESULT, raw)
                            )
                            finish()
                        }
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "barcode fail", e) }
                    .addOnCompleteListener { proxy.close() }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "bind camera", e)
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        scanner.close()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
    }
}
