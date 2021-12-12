package com.app.location.ui.detail_book

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.location.R


class DetailBookActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_book)

        //dcgpagecurlPageCurlView1
    }

    override fun onDestroy() {
        super.onDestroy()
        System.gc()
        finish()
    }

    /**
     * Set the current orientation to landscape. This will prevent the OS from changing
     * the app's orientation.
     */
    fun lockOrientationLandscape() {
        lockOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    /**
     * Set the current orientation to portrait. This will prevent the OS from changing
     * the app's orientation.
     */
    fun lockOrientationPortrait() {
        lockOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    /**
     * Locks the orientation to a specific type.  Possible values are:
     *
     *  * [ActivityInfo.SCREEN_ORIENTATION_BEHIND]
     *  * [ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE]
     *  * [ActivityInfo.SCREEN_ORIENTATION_NOSENSOR]
     *  * [ActivityInfo.SCREEN_ORIENTATION_PORTRAIT]
     *  * [ActivityInfo.SCREEN_ORIENTATION_SENSOR]
     *  * [ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED]
     *  * [ActivityInfo.SCREEN_ORIENTATION_USER]
     *
     * @param orientation
     */
    private fun lockOrientation(orientation: Int) {
        requestedOrientation = orientation
    }
}