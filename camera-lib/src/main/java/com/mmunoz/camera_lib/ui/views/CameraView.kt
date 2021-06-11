package com.mmunoz.camera_lib.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.core.Preview
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mmunoz.camera_lib.databinding.CameraLibViewBinding

class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private var _binding: CameraLibViewBinding? = null
    private val binding get() = _binding!!

    private var listener: Listener? = null
    private var isSpeakButtonLongPressed = false

    val cameraPreviewView: AppCompatImageView
        get() = binding.cameraPreviewView

    init {
        _binding = CameraLibViewBinding.inflate(LayoutInflater.from(context), this)
        binding.cameraPreviewView.setOnClickListener { listener?.openPreviewImage() }
        binding.cameraTorchButton.setOnClickListener { listener?.toggleTorch() }
        binding.cameraSwitchButton.setOnClickListener { listener?.switchCamera() }
        setCaptureListeners()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _binding = null
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun clearPreviewImage() {
        binding.cameraPreviewView.setImageResource(0)
    }

    fun getSurfaceProvider(): Preview.SurfaceProvider {
        return binding.viewFinder.surfaceProvider
    }

    fun setCameraTorchButtonDrawable(@DrawableRes drawableRes: Int) {
        binding.cameraTorchButton.setImageDrawable(
            ContextCompat.getDrawable(context, drawableRes)
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setCaptureListeners() {
        binding.cameraCaptureButton.setOnClickListener {
            listener?.takePhoto()
        }
        binding.cameraCaptureButton.setOnLongClickListener {
            listener?.startRecording()
            isSpeakButtonLongPressed = true
            return@setOnLongClickListener true
        }
        binding.cameraCaptureButton.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (isSpeakButtonLongPressed) {
                    listener?.stopRecording()
                    isSpeakButtonLongPressed = false
                }
            }
            return@setOnTouchListener false
        }
    }

    interface Listener {
        fun takePhoto()
        fun toggleTorch()
        fun switchCamera()
        fun startRecording()
        fun stopRecording()
        fun openPreviewImage()
    }
}