package com.example.venomousboxer.visitorapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.camerakit.CameraKitView
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import id.zelory.compressor.Compressor
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var cameraKitView: CameraKitView? = null
    lateinit var checkIn: Button
    lateinit var signup: Button
    lateinit var takePhoto: Button
    private lateinit var phoneET: EditText
    private lateinit var imageView: ImageView

    var takePhotoButton : Boolean = true
    var proceed: Boolean = false

    private var image: ByteArray? = null
    var downloadUri: Uri? = null

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraKitView = findViewById(R.id.camera)
        checkIn = findViewById(R.id.check_in_button)
        phoneET = findViewById(R.id.phone_edit_text)
        takePhoto = findViewById(R.id.take_photo_button)
        imageView = findViewById(R.id.image_view)
        signup = findViewById(R.id.sign_up_button)

        auth?.useAppLanguage()

        var savedPhoto: File?

        phoneET.clearFocus()

        takePhoto.setOnClickListener {
            Log.d(TAG,"tk")
            //capture image
            if(takePhotoButton){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    val s = Snackbar.make(takePhoto, "Please allow camera permission first", Snackbar.LENGTH_SHORT)
                    s.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    val tv = s.view.findViewById<TextView>(R.id.snackbar_text)
                    tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    s.show()
                    return@setOnClickListener
                }
                takePhotoButton=false
                Log.d(TAG,"tk2")
                //take and compress pic
                cameraKitView?.captureImage { _, capturedImage ->
                    Log.d(TAG,"Inside camera kit")
                    savedPhoto = try{
                        createImageFile()
                    } catch (e: IOException){
                        e.printStackTrace()
                        Log.e(TAG, "error", e)
                        return@captureImage
                    }
                    Log.d(TAG,"File created")
                    image = capturedImage
                    val outputStream = FileOutputStream(savedPhoto?.path)
                    outputStream.write(image)
                    outputStream.close()
                    Log.d(TAG, image.toString())
                    //compress image
                    compressAndSavePhoto(savedPhoto)
                }
            }else{
                takePhotoButton=true
                //change name of button and invert views
                cameraKitView?.visibility = View.VISIBLE
                imageView.visibility = View.INVISIBLE
                takePhoto.text = "TAKE PHOTO"
            }

        }

        signup.setOnClickListener {
            val phoneNo = phoneET.text.toString()

            if (!(phoneNo.length==13&& phoneNo[0]=='+')&&phoneNo.length != 12){
                val s = Snackbar.make(checkIn, "Please enter a valid phone number with Country Code."
                    , Snackbar.LENGTH_SHORT)
                s.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                val tv = s.view.findViewById<TextView>(R.id.snackbar_text)
                tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                s.show()
                //Toast.makeText(this@MainActivity, "Incorrect phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(takePhotoButton){
                val s = Snackbar.make(checkIn, "PLease take photo first", Snackbar.LENGTH_SHORT)
                s.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                val tv = s.view.findViewById<TextView>(R.id.snackbar_text)
                tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                s.show()
                return@setOnClickListener
            }
            if(!proceed){
                Log.d(TAG,"Upload Not Finished")
                val s = Snackbar.make(checkIn, "Please wait while we upload your image to our servers."
                    , Snackbar.LENGTH_SHORT)
                s.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                val tv = s.view.findViewById<TextView>(R.id.snackbar_text)
                tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                s.show()
                //wait for upload to finish
                return@setOnClickListener
            }
            verifyPhone(phoneNo)
        }

        checkIn.setOnClickListener {
            //check if phone no is correct
            val phoneNo = phoneET.text.toString()

            if (!(phoneNo.length==13&& phoneNo[0]=='+')&&(phoneNo.length!=12)){
                val s = Snackbar.make(checkIn, "Please enter a valid phone number with Country Code."
                    , Snackbar.LENGTH_SHORT)
                s.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                val tv = s.view.findViewById<TextView>(R.id.snackbar_text)
                tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                s.show()
                //Toast.makeText(this@MainActivity, "Incorrect phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cameraKitView?.setErrorListener { _, e ->
                e.printStackTrace()
                Log.e(TAG, "Error : ", e)
            }

            //sign in or sign up
            Log.d(TAG, "Past all if st.")
            val dbProfile = db.child("Visitors")
            val query = dbProfile.orderByKey().equalTo(phoneNo)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    for (p1: DataSnapshot in p0.children){
                        if(p1.exists()){
                            //sign in
                            var vc = p1.child("visitCount").getValue(Int::class.java)
                            if (vc!=null){
                                vc++
                                // update vc and downloadUrl
                                dbProfile.child(phoneNo).child("visitCount").setValue(vc)
                                val intent = Intent(this@MainActivity, VisitorActivity::class.java)
                                intent.putExtra("VisitorCount", vc)
                                startActivity(intent)
                            } else{
                                Log.d(TAG, "vc null")
                                //vc null error
                            }
                        }else{
                            Log.d(TAG, "p1 does not exists")
                        }
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    Log.e(TAG, "onCancelled", p0.toException())
                }
            })
        }
    }

    private lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun verifyPhone(phone:String) {
        //Goto verification code checking activity
        val intent = Intent(this@MainActivity, VerificationCodeActivity::class.java)
        intent.putExtra("phone", phone)
        intent.putExtra("downloadUri", downloadUri.toString())
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("CheckResult")
    fun compressAndSavePhoto(savedPhoto: File?){
        var compressedImage: File?
        try {
            Compressor(this)
                .compressToFileAsFlowable(savedPhoto)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        compressedImage = it
                        Log.d(TAG, "h" + compressedImage?.path)
                        val b = BitmapFactory.decodeFile(compressedImage?.path)
                        this.image = File(compressedImage?.path).readBytes()
                        Log.d(TAG, "2 : $image.toString()")
                        //make cameraKit invisible and show image view with compressed image
                        cameraKitView?.visibility = View.INVISIBLE
                        imageView.visibility = View.VISIBLE
                        imageView.setImageBitmap(b)
                        takePhoto.text = "REDO"
                        // save image in firebase storage
                        val imageRef = storage.reference
                            .child("images/image"+System.currentTimeMillis()+".jpg")
                        val metadata = StorageMetadata.Builder()
                            .setContentType("image/jpg")
                            .build()
                        val uploadTask = imageRef.putBytes(image!!, metadata)
                        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                            if (!task.isSuccessful) {
                                task.exception?.let {
                                    e->throw e
                                }
                            }
                            return@Continuation imageRef.downloadUrl
                        }).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                downloadUri = task.result
                                Log.d(TAG,"url : "+downloadUri.toString())
                                proceed=true
                            } else {
                                throw task.exception!!
                            }
                        }
                    },
                    {
                        it.printStackTrace()
                        val s = Snackbar.make(takePhoto, "Error occurred while compressing image"
                            , Snackbar.LENGTH_SHORT)
                        s.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        val tv = s.view.findViewById<TextView>(R.id.snackbar_text)
                        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                        s.show()
                        //Toast.makeText(this, "Error occurred while compressing image", Toast.LENGTH_SHORT).show()
                    })

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
//        takePhotoButton = true
//        cameraKitView?.visibility = View.VISIBLE
//        imageView.visibility = View.GONE
        cameraKitView?.onStart()
//        takePhoto.text = "TAKE PHOTO"
    }

    override fun onResume() {
        super.onResume()
//        takePhotoButton = true
//        cameraKitView?.visibility = View.VISIBLE
//        imageView.visibility = View.GONE
        cameraKitView?.onResume()
//        takePhoto.text = "TAKE PHOTO"
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

    companion object {
        const val TAG = "MAIN_ACTIVITY"
    }
}
