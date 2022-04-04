package com.app.upload_file.ui.upload_files

import android.Manifest
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.storage.StorageReference

import com.google.firebase.storage.FirebaseStorage

import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_upload_file.*
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.view.View
import android.provider.OpenableColumns
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.Dexter

import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.karumi.dexter.listener.PermissionRequest
import android.widget.Toast
import com.app.upload_file.R


class UploadFileActivity : AppCompatActivity() {

    private val RESULT_IMAGE = 10
    lateinit var fileNameList: ArrayList<String>
    lateinit var fileDoneList: ArrayList<String>
    lateinit var mAdapter: UploadListAdapter
    lateinit var mStorageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_file)

        fileNameList = ArrayList()
        fileDoneList = ArrayList()
        mAdapter = UploadListAdapter(fileNameList, fileDoneList)

        recycler_upload!!.layoutManager = LinearLayoutManager(this)
        recycler_upload.setHasFixedSize(true)
        recycler_upload.adapter = mAdapter

        mStorageReference = FirebaseStorage.getInstance().reference

        // set on-click listener
        btnUpload.setOnClickListener {
            requestPermission()
            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_IMAGE)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_IMAGE && resultCode == RESULT_OK) {
            if (data?.clipData != null) {
                //Toast.makeText(this, "Selected Multiple Files", Toast.LENGTH_SHORT).show();
                val totalItems = data.clipData!!.itemCount
                for (i in 0 until totalItems) {
                    val fileUri = data.clipData!!.getItemAt(i).uri
                    val fileName = getFileName(fileUri)
                    fileNameList.add(fileName)
                    fileDoneList.add("Uploading")
                    mAdapter.notifyDataSetChanged()
                    val fileToUpload = mStorageReference.child("Images").child(fileName)
                    fileToUpload.putFile(fileUri)
                        .addOnSuccessListener { //Toast.makeText(MainActivity.this, "Done", Toast.LENGTH_SHORT).show();
                            fileDoneList.removeAt(i)
                            fileDoneList.add(i, "Done")
                            mAdapter.notifyDataSetChanged()
                        }
                }
            } else if (data?.data != null) {
                Toast.makeText(this, "Selected Single File", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme.equals("content")) {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    private fun showSettingsDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@UploadFileActivity)
        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton("GO TO SETTINGS") { dialog, which ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton("Cancel"
        ) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }

    private fun requestPermission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse) {}
                override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse) {
                    val dialogPermissionListener: PermissionListener = DialogOnDeniedPermissionListener.Builder
                        .withContext(this@UploadFileActivity)
                        .withTitle("Read External Storage permission")
                        .withMessage("Read External Storage  permission is needed")
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build()


//                        Intent permIntent = new Intent();
//                        permIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                        Uri uri = Uri.fromParts("package",getPackageName(),null);
//                        permIntent.setData(uri);
//                        startActivity(permIntent);
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissionRequest: PermissionRequest?,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).check()
    }

}