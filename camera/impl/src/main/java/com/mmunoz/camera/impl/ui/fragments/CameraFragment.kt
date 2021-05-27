package com.mmunoz.camera.impl.ui.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.mmunoz.camera.impl.R
import com.mmunoz.camera.impl.data.analysis.ImagesAnalyzer
import com.mmunoz.camera.impl.databinding.CameraImplFragmentBinding
import com.mmunoz.camera.impl.di.components.inject
import com.mmunoz.core.BaseFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class CameraFragment : BaseFragment() {

    private var _binding: CameraImplFragmentBinding? = null
    private val binding get() = _binding!!

    private var cameraInfo: CameraInfo? = null
    private var permissions: Permissions? = null
    private var imagePreview: Preview? = null
    private var videoCapture: VideoCapture? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraControl: CameraControl? = null
    private var imageCaptureHandler: ImageCaptureHandler? = null

    private var linearZoom = 0f
    private var defaultCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File

    override fun onAttach(context: Context) {
        inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissions = Permissions()
        permissions!!.requestPermissions()
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CameraImplFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cameraTorchButton.setOnClickListener { toggleTorch() }
        binding.cameraSwitchButton.setOnClickListener { switchCamera() }
        binding.cameraCaptureButton.setOnClickListener { takePhoto() }
        binding.root.setOnKeyListener { _, keyCode, _ ->
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (linearZoom <= 0.9) {
                        linearZoom += 0.1f
                    }
                    cameraControl?.setLinearZoom(linearZoom)
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (linearZoom >= 0.1) {
                        linearZoom -= 0.1f
                    }
                    cameraControl?.setLinearZoom(linearZoom)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        cameraInfo = null
        permissions = null
        imagePreview = null
        videoCapture = null
        cameraControl = null
        imageAnalyzer = null
        cameraExecutor.shutdown()
        imageCaptureHandler = null
        super.onDestroyView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (this.permissions?.allPermissionsGranted() == true) {
                startCamera()
            } else {
                this.permissions?.requestPermissions()
            }
        }
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraConfiguration = CameraConfiguration()
            cameraConfiguration.setImagePreview()  // Preview
            cameraConfiguration.setImageCapture()  // ImageCapture Builder
            cameraConfiguration.setVideoCapture()  // VideoCapture
            cameraConfiguration.setImageAnalysis() // Analyzer

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    defaultCameraSelector,
                    imagePreview,
                    imageCapture,
                    //videoCapture,
                    imageAnalyzer
                )
                cameraInfo = camera.cameraInfo
                imagePreview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                cameraControl = camera.cameraControl
                setTorchStateObserver()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCaptureHandler = ImageCaptureHandler(photoFile)
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            imageCaptureHandler!!
        )
    }

    private fun switchCamera() {
        defaultCameraSelector = if (defaultCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun toggleTorch() {
        if (cameraInfo?.torchState?.value == TorchState.ON) {
            cameraControl?.enableTorch(false)
        } else {
            cameraControl?.enableTorch(true)
        }
    }

    private fun setTorchStateObserver() {
        cameraInfo?.torchState?.observe(viewLifecycleOwner, { state ->
            if (state == TorchState.ON) {
                binding.cameraTorchButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.camera_impl_ic_flash_on
                    )
                )
            } else {
                binding.cameraTorchButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.camera_impl_ic_flash_off
                    )
                )
            }
        })
    }

    private fun setZoomStateObserver() {
        cameraInfo?.zoomState?.observe(this, { state ->
            // state.linearZoom
            // state.zoomRatio
            // state.maxZoomRatio
            // state.minZoomRatio
            Log.d(TAG, "${state.linearZoom}")
        })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.camera_impl_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            requireActivity().filesDir
        }
    }

    private inner class CameraConfiguration {

        fun setImageAnalysis() {
            imageAnalyzer = ImageAnalysis.Builder()
                .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImagesAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }
        }

        fun setVideoCapture() {
            videoCapture = VideoCapture.Builder()
                .build()
        }

        fun setImageCapture() {
            imageCapture = ImageCapture.Builder()
                .setFlashMode(FLASH_MODE_AUTO)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                //.setTargetRotation(binding.viewFinder.display.rotation)
                .build()
        }

        fun setImagePreview() {
            imagePreview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                //.setTargetRotation(binding.viewFinder.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
        }
    }

    private inner class ImageCaptureHandler constructor(
        private val photoFile: File
    ) : ImageCapture.OnImageSavedCallback {

        override fun onError(exc: ImageCaptureException) {
            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
        }

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val savedUri = Uri.fromFile(photoFile)
            Glide.with(requireContext())
                .load(savedUri)
                .circleCrop()
                .into(binding.cameraPreviewView)
        }
    }

    private inner class VideoCaptureHandler constructor(
        private val photoFile: File
    ) : VideoCapture.OnVideoSavedCallback {

        override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
            TODO("Not yet implemented")
        }

        override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
            TODO("Not yet implemented")
        }
    }

    private inner class Permissions {
        fun requestPermissions() {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        fun newInstance(): CameraFragment {
            return CameraFragment().apply { }
        }
    }
}