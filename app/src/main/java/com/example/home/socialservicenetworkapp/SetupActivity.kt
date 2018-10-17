package com.example.home.socialservicenetworkapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import com.example.home.socialservicenetworkapp.R.drawable.profile
import com.example.home.socialservicenetworkapp.R.layout.activity_setup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_setup.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import java.util.*

//General comments mentioned in the ClickPostActivity also apply to this Activity and are not repeated
//for convenience. Additional comments, specifically relevant for this Activity are included below.

class SetupActivity : AppCompatActivity() {

    private lateinit var loadingBar: ProgressDialog
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var userProfImgRef: StorageReference
    private lateinit var currentUserID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_setup)

        mAuth = FirebaseAuth.getInstance()
        currentUserID = mAuth.currentUser!!.uid
        userRef = FirebaseDatabase.getInstance().reference.child("Users").child(currentUserID)
        userProfImgRef = FirebaseStorage.getInstance().reference.child("Profile Images")
        loadingBar = ProgressDialog(this)

        setupInfoBtn.onClick { saveAccSetupInfo() }
//onClick method is a part of Anko Coroutine library, discussed in the Report.
        setupProfImg.onClick {
            val galleryIntent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, GalPick)
        }

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.hasChild("profileimage")) {
                        val image = dataSnapshot.child("profileimage").value.toString()
//Please refer to the comment on Picasso library in ClickPostActivity.
                        Picasso.with(this@SetupActivity).load(image).placeholder(profile).into(setupProfImg)
                    } else {
                        toast("Please select profile image.")
                        Log.e("TAG", "Setup: profile image selection requested")
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GalPick && resultCode == Activity.RESULT_OK && data != null) {
            val ImageUri = data.data

            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this)
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                loadingBar.setTitle("Profile Image")
                loadingBar.setMessage("Please wait, profile image is being updated")
                loadingBar.setCanceledOnTouchOutside(true)
                loadingBar.show()

                val resultUri = result.uri

                val filePath = userProfImgRef.child("$currentUserID.jpg")

                filePath.putFile(resultUri).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        toast("Profile Image saved to Firebase.")
                        val downloadUrl = task.result.downloadUrl.toString()
                        userRef.child("profileimage").setValue(downloadUrl)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        startActivity(intentFor<SetupActivity>())
                                        toast("Profile image saved to Firebase")
                                        val messageSuccess = task.result.toString()
                                        Log.e("TAG", "Profile image saved to Firebase: $messageSuccess")
                                        loadingBar.dismiss()
                                    } else {
                                        val message = task.exception.toString()
                                        toast("Saving of profile image failed: $message")
                                        Log.e("TAG", "Saving of profile image failed: $message")
                                        loadingBar.dismiss()
                                    }
                                }
                    }
                }
            } else {
                toast("Cropping of image failed")
                Log.e("TAG", "Cropping of image failed")
                loadingBar.dismiss()
            }
        }
    }

    private fun saveAccSetupInfo() {
        val username = setupUserName.text.toString()
        val fullname = setupFullName.text.toString()
        val country = setupCountry.text.toString()
        if (TextUtils.isEmpty(username)) {
            toast("Your username please")
            Log.e("TAG", "Setup: username requested")
        }
        if (TextUtils.isEmpty(fullname)) {
            toast("Full name please")
            Log.e("TAG", "Setup: full name requested")
        }
        if (TextUtils.isEmpty(country)) {
            toast("Your home country please.")
            Log.e("TAG", "Setup: home country requested")
        } else {
            loadingBar.setTitle("Saving Information")
            loadingBar.setMessage("The new account is being created")
            loadingBar.show()
            loadingBar.setCanceledOnTouchOutside(true)

            val userMap = HashMap<String, Any>()
            userMap["username"] = username
            userMap["fullname"] = fullname
            userMap["country"] = country
            userMap["status"] = "none"
            userMap["gender"] = "none"
            userMap["dob"] = "none"
            userMap["relationshipstatus"] = "none"
            if (userRef.updateChildren(userMap).isSuccessful) {
                loadingBar.dismiss()
                toast("Account created")
                Log.e("TAG", "Setup: Account of $username created")
                sndToMainAct()
            } else {
                loadingBar.dismiss()
                toast("Error")
                Log.e("TAG", "Setup: Account creation failed")
                sndToMainAct()
            }
        }
    }

    private fun sndToMainAct() {
        startActivity(intentFor<MainActivity>().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        internal const val GalPick = 1
    }
}