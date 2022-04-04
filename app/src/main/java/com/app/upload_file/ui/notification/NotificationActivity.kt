package com.app.upload_file.ui.notification

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.app.upload_file.R
import kotlinx.android.synthetic.main.activity_notification.*

class NotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        layout_main.setBackgroundColor(resources.getColor(R.color.colorAccent))
        layout_main.gravity = Gravity.NO_GRAVITY
        layout_main.orientation = LinearLayout.HORIZONTAL
        layout_main.weightSum = 8.0f
        layout_main.invalidate()

        layout_main.isFocusable = true
        layout_main.setBackgroundColor(resources.getColor(R.color.colorAccent))

    }

    private fun checkInvalidateView() {

    }
}