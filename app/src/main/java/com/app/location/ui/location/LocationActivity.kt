package com.app.location.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.app.location.R
import com.app.location.services.location.LocationUpdatesService
import kotlinx.android.synthetic.main.activity_location.*

class LocationActivity : AppCompatActivity() {

    // A reference to the service used to get location updates.
    private var mService: LocationUpdatesService? = null;
    // Tracks the bound state of the service.
    private var mBound: Boolean = false

    private val MY_PERMISSIONS_REQUEST_LOCATION = 68
    private val REQUEST_CHECK_SETTINGS = 129

    private var broadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        welcomeMessage.text = ""

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@LocationActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
        } else {
            Log.e("MainActivity:","Location Permission Already Granted")
            if (getLocationMode() == 3) {
                Log.e("MainActivity:","Already set High Accuracy Mode")
                initializeService()
            } else {
                Log.e("MainActivity:","Alert Dialog Shown")
                showAlertDialog(this@LocationActivity)
            }
        }

        broadcastReceiver = object : BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            override fun onReceive(contxt: Context?, intent: Intent?) {

                when (intent?.action) {
                    "NotifyUser" -> {
                        try {
                            val name = intent.getStringExtra("pinned_location_name")
                            val lat = intent.getStringExtra("pinned_location_lat")
                            val long = intent.getStringExtra("pinned_location_long")
                            welcomeMessage?.text = "You are around at " + name + "Pinned Location Latitude: " + lat +
                            ", " + "Pinned Location Longitude: " + long
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("NotifyUser")
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.e("MainActivity:","Location Permission Granted")
                    if (getLocationMode() == 3) {
                        Log.e("MainActivity:","Already set High Accuracy Mode")
                        initializeService()
                    } else {
                        Log.e("MainActivity:","Alert Dialog Shown")
                        showAlertDialog(this@LocationActivity)
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }
    }

    private fun showAlertDialog(context: Context?) {
        try {
            context?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle(it.resources.getString(R.string.app_name))
                    .setMessage("Please select High accuracy Location Mode from Mode Settings")
                    .setPositiveButton(it.resources.getString(android.R.string.ok)) { dialog, which ->
                        dialog.dismiss()
                        startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_CHECK_SETTINGS)
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocationMode(): Int {
        return Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            initializeService()
        }
    }

    // Monitors the state of the connection to the service.
    private var mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: LocationUpdatesService.LocalBinder = service as LocationUpdatesService.LocalBinder
            mService = binder.service
            mBound = true
            // Check that the user hasn't revoked permissions by going to Settings.

            mService?.requestLocationUpdates()

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
        }
    }

    private fun initializeService(){
        bindService(Intent(this, LocationUpdatesService::class.java), mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }
}