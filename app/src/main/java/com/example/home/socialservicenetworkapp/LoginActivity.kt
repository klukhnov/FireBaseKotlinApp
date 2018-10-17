package com.example.home.socialservicenetworkapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import com.example.home.socialservicenetworkapp.R.layout.activity_login
import com.example.home.socialservicenetworkapp.R.string.default_web_client_id
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast

//General comments mentioned in the ClickPostActivity also apply to this Activity and are not repeated
//for convenience. Additional comments, specifically relevant for this Activity are included below.

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var loadBar: ProgressDialog
    private lateinit var gSignIn: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_login)

        mAuth = FirebaseAuth.getInstance()
        loadBar = ProgressDialog(this)

        regAccLink.onClick { sndToRegisterAct() }
        loginBtn.onClick { userLogin() }
//The Java code of CodingCafe(2018) and the code provided in Google(2018) generally served as
//a basis for the Kotlin implementation of Google sign-in functionality.
        val gSIOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(default_web_client_id))
                .requestEmail()
                .build()
        gSignIn = GoogleApiClient.Builder(this)
                .enableAutoManage(this) {
                    toast("Connection to Google account failed")
                    Log.e("TAG", "Connection to Google account failed")
                }
                .addApi(Auth.GOOGLE_SIGN_IN_API, gSIOptions)
                .build()
        gSigninBtn.onClick { signIn() }
    }

    private fun signIn() {
        val sIntent = Auth.GoogleSignInApi.getSignInIntent(gSignIn)
        startActivityForResult(sIntent, RcSignIn)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RcSignIn) {
            val res = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            if (res.isSuccess) {
                val acc = res.signInAccount
                acc?.let { firebaseGoogleAuth(it) }
                toast("Authorisation in process")
                Log.e("TAG", "Google account authorization in process ${res.status}")
            } else {
                toast("Cannot authorise")
                Log.e("TAG", "Google account authorization failure ${res.status}")
            }
        }
    }

    private fun firebaseGoogleAuth(acct: GoogleSignInAccount) {
        val aCred = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth.signInWithCredential(aCred)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val msgSuccess = task.result.toString()
                        Log.e("TAG", "Login using Google account is successful: $msgSuccess")
                        sndToMainAct()
                    } else {
                        val msg = task.exception.toString()
                        sndToLoginAct()
                        toast("Error$msg")
                        Log.e("TAG", "Login using Google account failed: $msg")
                    }
                }
    }

    private fun sndToLoginAct() {
        startActivity(intentFor<LoginActivity>().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onStart() {
        super.onStart()

        val curUser = mAuth.currentUser
        if (curUser != null) {
            sndToMainAct()
        }
    }

    private fun userLogin() {
        val email = loginEmail.text.toString()
        val pass = loginPsw.text.toString()
//Kotlin's more efficient (when) operator is used instead of if...else cascaded statements, which are more verbose.
        when {
            TextUtils.isEmpty(email) -> {
                toast("Email please")
                Log.e("TAG", "Login: email requested")
            }
            TextUtils.isEmpty(pass) -> {
                toast("Password please")
                Log.e("TAG", "Login: password requested")
            }
            else -> {
                loadBar.setTitle("Login")
                loadBar.setMessage("Processing")
                loadBar.show()
                loadBar.setCanceledOnTouchOutside(true)

                mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        sndToMainAct()
                        toast("You are logged in")
                        val msgSuccess = task.result.toString()
                        Log.e("TAG", "Login successful: $msgSuccess")
                        loadBar.dismiss()
                    } else {
                        val msg = task.exception.toString()
                        toast("Error occurred $msg")
                        Log.e("TAG", "Login failed: $msg")
                        loadBar.dismiss()
                    }
                }
            }
        }
    }

    private fun sndToMainAct() {
        startActivity(intentFor<MainActivity>().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun sndToRegisterAct() {
        startActivity(intentFor<RegisterActivity>())
    }

    companion object {
        private const val RcSignIn = 1
    }
}
