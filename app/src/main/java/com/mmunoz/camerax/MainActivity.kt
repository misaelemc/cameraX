package com.mmunoz.camerax

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import com.mmunoz.camera.api.ui.loader.CameraFragmentLoader
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var cameraFragmentLoader: CameraFragmentLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addToStack(cameraFragmentLoader.getFragment())
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    private fun addToStack(fragment: Fragment) {
        if (supportFragmentManager.isDestroyed) return
        val tagName = fragment::class.java.name
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment, tagName)
            .commitAllowingStateLoss()
    }
}