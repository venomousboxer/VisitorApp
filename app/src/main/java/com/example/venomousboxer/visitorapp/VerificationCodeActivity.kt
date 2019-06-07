package com.example.venomousboxer.visitorapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class VerificationCodeActivity : AppCompatActivity() {

    private lateinit var verificationCode: EditText
    private lateinit var verificationButton: Button
    var codeSent: String? = null
    private var downloadUri: String? = null
    private var phoneNo: String? = null
    private val auth: FirebaseAuth? = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    var entered = false

    //Data Classes
    data class User(var visitCount:Int, var id:String, var downloadURL:String)

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_code)
        entered=false

        verificationCode = findViewById(R.id.verification_code_ET)
        verificationButton = findViewById(R.id.verification_button)

        phoneNo = intent.getStringExtra("phone")
        downloadUri = intent.getStringExtra("downloadUri")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationFailed(p0: FirebaseException?) {
                Log.w(TAG, "onVerificationFailed", p0)
            }

            override fun onVerificationCompleted(p0: PhoneAuthCredential?) {
                Log.d(TAG, "onVerificationCompleted:$p0")
                //sign in immediately
                entered=true
                signInWithPhoneAuthCredential(p0!!)
            }

            override fun onCodeSent(p0: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(p0, p1)
                codeSent = p0
                Log.d(TAG, "Code : $codeSent")
            }
        }
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNo!!, 120
            , TimeUnit.SECONDS, this@VerificationCodeActivity, callbacks)
        Log.d(TAG,"OTP code sent")
        verificationButton.setOnClickListener {
            val code = verificationCode.text.toString()
            try {
                val credential = PhoneAuthProvider.getCredential(codeSent!!, code)
                signInWithPhoneAuthCredential(credential)
            }catch (e:Exception){
                val s = Snackbar.make(verificationButton, "Invalid code", Snackbar.LENGTH_SHORT)
                s.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                val tv = s.view.findViewById<TextView>(R.id.snackbar_text)
                tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                s.show()
                finish()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    //To make node in Visitors and populate with fresh data
                    var vc = 1
                    if(entered){
                        val dbProfile = db.child("Visitors")
                        dbProfile.child(phoneNo!!).addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(p0: DataSnapshot) {
                                try {
                                    vc = p0.child("visitCount").getValue(Int::class.java)!!
                                }catch (e: Exception){
                                    e.printStackTrace()
                                }
                                vc++
                                val dbProfile = db.child("Visitors")
                                val key = dbProfile.child(phoneNo!!).push().key
                                val user = User(vc, key!!, downloadUri!!)
                                dbProfile.child(phoneNo!!).setValue(user)
                                val intent = Intent(this@VerificationCodeActivity, VisitorActivity::class.java)
                                intent.putExtra("VisitorCount", vc)
                                startActivity(intent)
                            }
                            override fun onCancelled(p0: DatabaseError) {
                                //To change body of created functions use File | Settings | File Templates.
                            }
                        })
                    }else{
                        val dbProfile = db.child("Visitors")
                        val key = dbProfile.child(phoneNo!!).push().key
                        val user = User(vc, key!!, downloadUri!!)
                        dbProfile.child(phoneNo!!).setValue(user)
                        val intent = Intent(this@VerificationCodeActivity, VisitorActivity::class.java)
                        intent.putExtra("VisitorCount", vc)
                        startActivity(intent)
                    }
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        //To add this user in SuspiciousUsers with fresh data
                        val dbProfile = db.child("SuspiciousUsers")
                        val key = dbProfile.child(phoneNo!!).push().key
                        val user = User(1, key!!, downloadUri!!)
                        dbProfile.child(phoneNo!!).setValue(user)
                        val s = Snackbar.make(verificationButton, "Incorrect Phone Number or verification code", Snackbar.LENGTH_SHORT)
                        s.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        val tv = s.view.findViewById<TextView>(R.id.snackbar_text)
                        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                        s.show()
                        finish()
                    }
                }
            }
    }

    companion object {
        const val TAG = "VERIFY_CODE_ACTIVITY"
    }
}
