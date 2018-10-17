package com.example.home.socialservicenetworkapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import com.example.home.socialservicenetworkapp.R.layout.activity_register
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_register.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast

//General comments mentioned in the ClickPostActivity also apply to this Activity and are not repeated
//for convenience. Additional comments, specifically relevant for this Activity are included below.

class RegisterActivity : AppCompatActivity() {

    private lateinit var loadingBar: ProgressDialog
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_register)

        mAuth = FirebaseAuth.getInstance()
        loadingBar = ProgressDialog(this)
        regCreateAcc.onClick { createNewAcc() }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            sndToMainAct()
        }
    }

    private fun sndToMainAct() { startActivity(intentFor<MainActivity>().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()}

    private fun createNewAcc() {
        val email = regEmail.text.toString()
        val pass = regPassword.text.toString()
        val confPass = regConfPassword.text.toString()

        when {
            TextUtils.isEmpty(email) -> {
                toast("Please add your email")
                Log.e("TAG", "Registration: email requested")
            }
            TextUtils.isEmpty(pass) -> {
                toast("Please set the password")
                Log.e("TAG", "Registration: password requested")
            }
            TextUtils.isEmpty(confPass) -> {
                toast("Please set the password confirmation")
                Log.e("TAG", "Registration: password confirmation requested")
            }
//Password confirmation is implemented here.
            pass != confPass -> {
                toast("Password and confirmation do not match")
                Log.e("TAG", "Registration: password and confirmation do not match")
            }
            else -> {
                loadingBar.setTitle("Creating new account")
                loadingBar.setMessage("Account is being created")
                loadingBar.show()
                loadingBar.setCanceledOnTouchOutside(true)

                mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        sndToSetupAct()
                        toast("Authentication successful")
                        val messageSuccess = task.result.toString()
                        Log.e("TAG", "Authentication successful: $messageSuccess")
                        loadingBar.dismiss()
                    } else {
                        val message = task.exception.toString()
                        toast("Authentication failed: $message")
                        Log.e("TAG", "Authentication failed: $message")
                        loadingBar.dismiss()
                    }
                }
            }
        }
    }

    private fun sndToSetupAct() { startActivity(intentFor<SetupActivity>().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()}
}
