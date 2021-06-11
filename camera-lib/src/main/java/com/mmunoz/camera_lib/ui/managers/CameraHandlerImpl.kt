package com.mmunoz.camera_lib.ui.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.mmunoz.camera_lib.R
import com.mmunoz.camera_lib.data.analysis.ImagesAnalyzer
import com.mmunoz.camera_lib.ui.activities.PreviewActivityResultContract
import com.mmunoz.camera_lib.ui.views.CameraView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHandlerImpl constructor(
    private val activity: AppCompatActivity,
    private val cameraView: CameraView,
    private val viewLifecycleOwner: LifecycleOwner? = null
) : CameraHandler, CameraView.Listener {

    private var cameraInfo: CameraInfo? = null
    private var permissions: Permissions? = null
    private var imagePreview: Preview? = null
    private var videoCapture: VideoCapture? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraControl: CameraControl? = null
    private var imageCaptureHandler: ImageCaptureHandler? = null
    private var videoCaptureHandler: VideoCaptureHandler? = null

    private var recording = false
    private var linearZoom = 0f
    private var defaultCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File

    private val previewActivityResult =
        activity.registerForActivityResult(PreviewActivityResultContract()) { result ->
            if (!result) {
                cameraView.clearPreviewImage()
            }
        }

    override fun start() {
        cameraView.setListener(this)
        permissions = Permissions()
        permissions!!.requestPermissions()
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()
    }

    override fun clear() {
        cameraInfo = null
        permissions = null
        imagePreview = null
        videoCapture = null
        cameraControl = null
        imageAnalyzer = null
        cameraExecutor.shutdown()
        imageCaptureHandler = null
    }

    override fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = createFile(outputDirectory, PHOTO_EXTENSION)

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCaptureHandler = ImageCaptureHandler(photoFile)
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            imageCaptureHandler!!
        )
    }

    override fun switchCamera() {
        defaultCameraSelector = if (defaultCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    override fun toggleTorch() {
        if (cameraInfo?.torchState?.value == TorchState.ON) {
            cameraControl?.enableTorch(false)
        } else {
            cameraControl?.enableTorch(true)
        }
    }

    override fun openPreviewImage() {
        imageCaptureHandler?.openPreviewImageInGallery()
    }

    override fun startRecording() {
        videoCaptureHandler = VideoCaptureHandler()
        videoCaptureHandler!!.recordVideo()
        recording = true
    }

    override fun stopRecording() {
        if (videoCaptureHandler != null && recording) {
            videoCaptureHandler!!.stopRecording()
            recording = false
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
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
                    viewLifecycleOwner ?: activity,
                    defaultCameraSelector,
                    imagePreview,
                    imageCapture,
                    //videoCapture,
                    imageAnalyzer
                )
                cameraInfo = camera.cameraInfo
                imagePreview?.setSurfaceProvider(cameraView.getSurfaceProvider())
                cameraControl = camera.cameraControl
                setTorchStateObserver()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun setTorchStateObserver() {
        cameraInfo?.torchState?.observe(viewLifecycleOwner ?: activity, { state ->
            if (state == TorchState.ON) {
                cameraView.setCameraTorchButtonDrawable(R.drawable.camera_lib_ic_flash_on)
            } else {
                cameraView.setCameraTorchButtonDrawable(R.drawable.camera_lib_ic_flash_off)
            }
        })
    }

    private fun setZoomStateObserver() {
        cameraInfo?.zoomState?.observe(viewLifecycleOwner ?: activity, { state ->
            // state.linearZoom
            // state.zoomRatio
            // state.maxZoomRatio
            // state.minZoomRatio
            Log.d(TAG, "${state.linearZoom}")
        })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = activity.externalMediaDirs.firstOrNull()?.let {
            File(it, activity.resources.getString(R.string.camera_lib_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            activity.filesDir
        }
    }


    private fun createFile(baseFolder: File, extension: String) =
        File(
            baseFolder, SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + extension
        )

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
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
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
                    it.setSurfaceProvider(cameraView.getSurfaceProvider())
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
            Glide.with(activity)
                .load(savedUri)
                .circleCrop()
                .into(cameraView.cameraPreviewView)
        }

        fun openPreviewImageInGallery() {
            previewActivityResult.launch(photoFile.path)
        }
    }

    private inner class VideoCaptureHandler : VideoCapture.OnVideoSavedCallback {

        private var countDownTimer: CountDownTimer? = null

        @SuppressLint("RestrictedApi")
        fun recordVideo() {
            val file = createFile(outputDirectory, VIDEO_EXTENSION)
            val outputFileOptions = VideoCapture.OutputFileOptions.Builder(file).build()
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            videoCapture?.startRecording(outputFileOptions, cameraExecutor, this)


        }

        @SuppressLint("RestrictedApi")
        fun stopRecording() {
            videoCapture?.stopRecording()
        }

        override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
            val msg = "Video capture succeeded: ${outputFileResults.savedUri}"
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }

        override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
            val msg = "Video capture failed: $message"
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    private inner class Permissions {

        private val requestPermissions =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                if (result.all { it.value == true }) {
                    startCamera()
                } else {
                    requestPermissions()
                }
            }

        fun requestPermissions() {
            requestPermissions.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}