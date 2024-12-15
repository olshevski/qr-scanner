package dev.olshevski.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import dev.olshevski.qrscanner.databinding.FragmentCameraBinding
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private val viewModel by activityViewModels<MainViewModel>()

    private lateinit var viewBinding: FragmentCameraBinding
    private lateinit var cameraController: LifecycleCameraController
    private lateinit var barcodeScanner: BarcodeScanner

    private lateinit var mainExecutor: Executor
    private val takePictureExecutor = Executors.newSingleThreadExecutor()

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    requireContext(),
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainExecutor = ContextCompat.getMainExecutor(requireContext())

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun startCamera() {
        cameraController = LifecycleCameraController(requireContext())
        val previewView: PreviewView = viewBinding.root

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        cameraController.setImageAnalysisAnalyzer(
            mainExecutor,
            MlKitAnalyzer(
                listOf(barcodeScanner),
                ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                mainExecutor
            ) { result: MlKitAnalyzer.Result? ->
                val barcodeResults = result?.getValue(barcodeScanner)
                if ((barcodeResults == null) ||
                    (barcodeResults.size == 0) ||
                    (barcodeResults.first() == null)
                ) {
                    previewView.overlay.clear()
                    // noinspection ClickableViewAccessibility
                    previewView.setOnTouchListener { _, _ -> false } //no-op
                    return@MlKitAnalyzer
                }

                previewView.setOnTouchListener(
                    QrCodeTouchListener(
                        barcodeResults,
                        ::handleBarcodeClick
                    )
                )
                previewView.overlay.clear()
                previewView.overlay.add(QrCodeDrawable(barcodeResults))
            }
        )

        // ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG may be used here for even faster performance,
        // but the quality of the taken photo is awful (at least on my Pixel 3 device)
        cameraController.imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        cameraController.isPinchToZoomEnabled = false

        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
    }

    private fun handleBarcodeClick(barcode: Barcode) {
        val barcodeBoundingBox = barcode.boundingBox
        val barcodeRawValue = barcode.rawValue
        if (barcodeBoundingBox == null || barcodeBoundingBox.isEmpty || barcodeRawValue.isNullOrEmpty()) return

        val previewSize = viewBinding.root.let { Size(it.width, it.height) }

        cameraController.takePicture(takePictureExecutor, object : OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val croppedBitmap = cropQRCodeFromImage(
                    image = image,
                    previewSize = previewSize,
                    barcodeRect = barcodeBoundingBox
                )
                mainExecutor.execute {
                    if (croppedBitmap != null) {
                        viewModel.setBarcodeData(
                            BarcodeData(
                                text = barcodeRawValue,
                                image = croppedBitmap
                            )
                        )
                    } else {
                        context?.let {
                            Toast.makeText(it, "Failed to crop QR code", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barcodeScanner.close()
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).toTypedArray()
    }

}