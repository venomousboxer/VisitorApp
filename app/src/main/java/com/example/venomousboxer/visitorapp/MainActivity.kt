package com.example.venomousboxer.visitorapp

import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.camerakit.CameraKitView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var cameraKitView: CameraKitView? = null
    lateinit var checkIn: Button
    lateinit var phoneET: EditText
    lateinit var snackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraKitView = findViewById(R.id.camera)
        checkIn = findViewById(R.id.check_in_button)
        phoneET = findViewById(R.id.phone_edit_text)
        checkIn.setOnClickListener {
            val phoneNo = phoneET.text
            if (!(phoneNo.toString().length==10 || (phoneNo.toString().length==13&&phoneNo.toString()[0]=='+')
                || phoneNo.toString().length==12)){
                //Snackbar.make(checkIn, "Incorrect Phone Number", Snackbar.LENGTH_SHORT)
                Toast.makeText(this@MainActivity, "Incorrect phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            cameraKitView?.captureImage { cameraKitView, capturedImage ->
                val savedPhoto = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "photo.jpg")
                Log.d("MAIN_ACTIVITY",""+Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath)
                try {
//                    Compressor(this)
//                        .compressToFileAsFlowable(savedPhoto)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(object : Consumer<File>() {
//                            fun accept(file: File) {
//                                compressedImage = file
//                            }
//                        }, object : Consumer<Throwable>() {
//                            fun accept(throwable: Throwable) {
//                                throwable.printStackTrace()
//                                showError(throwable.message)
//                            }
//                        })
                    val outputStream = FileOutputStream(savedPhoto.getPath())
                    outputStream.write(capturedImage)
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        cameraKitView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        cameraKitView?.onResume()
    }

    override fun onPause() {
        cameraKitView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        cameraKitView?.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraKitView?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
