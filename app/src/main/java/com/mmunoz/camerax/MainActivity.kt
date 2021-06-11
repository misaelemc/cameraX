package com.mmunoz.camerax

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mmunoz.camera_lib.ui.fragments.CameraFragment

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addToStack(CameraFragment.newInstance())
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