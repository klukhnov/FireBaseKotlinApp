package com.example.home.socialservicenetworkapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Color.BLACK
import android.graphics.PorterDuff
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.home.socialservicenetworkapp.R.drawable.backbutton
import com.example.home.socialservicenetworkapp.R.drawable.profile
import com.example.home.socialservicenetworkapp.R.layout.activity_settings
import com.example.home.socialservicenetworkapp.R.style.NavigationText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import java.util.*

//General comments mentioned in the ClickPostActivity also apply to this Activity and are not repeated
//for convenience. Additional comments, specifically relevant for this Activity are included below.

class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var mToolbar: Toolbar
    private lateinit var setUserRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var curUserId: String
    private lateinit var loadingBar: ProgressDialog
    private lateinit var profImgRef: StorageReference
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_settings)
//Please refer to the comment in PostActivity on the Toolbar element.
        mToolbar = setTbar as Toolbar
        setSupportActionBar(mToolbar)
        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mToolbar.title = "Profile"
        mToolbar.setTitleTextColor(BLACK)
        mToolbar.setTitleTextAppearance(this, NavigationText)
        mToolbar.setNavigationIcon(backbutton)
        loadingBar = ProgressDialog(this)

        val statAdapter = ArrayAdapter
                .createFromResource(this, R.array.status_array,
                        android.R.layout.simple_spinner_item)
        statAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//Java code in Oracle(2017) served as a basis for the below Kotlin implementation.
        spinnerStatus.adapter = statAdapter
        spinnerStatus.onItemSelectedListener = this
        spinnerStatus.background.setColorFilter(Color.parseColor("#000000"), PorterDuff.Mode.SRC_ATOP)

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        val status = pref.getString("status", "")
        if (!status.equals("", ignoreCase = true)) {
            val spinnerPosition = statAdapter.getPosition(status)
            spinnerStatus.setSelection(spinnerPosition)

        }

        mAuth = FirebaseAuth.getInstance()
        curUserId = mAuth.currentUser!!.uid
        setUserRef = FirebaseDatabase.getInstance().reference.child("Users").child(curUserId)
        profImgRef = FirebaseStorage.getInstance().reference.child("Profile Images")

        setUserRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userProfImg = dataSnapshot.child("profileimage").value.toString()
                    val userProfName = dataSnapshot.child("fullname").value.toString()
                    val userProfStatus = dataSnapshot.child("status").value.toString()
                    val userProfDOB = dataSnapshot.child("dob").value.toString()
                    val userProfCountry = dataSnapshot.child("country").value.toString()
                    val userProfGender = dataSnapshot.child("gender").value.toString()
                    val userRelStatus = dataSnapshot.child("relationshipstatus").value.toString()
                    val mUserName = dataSnapshot.child("username").value.toString()

                    Picasso.with(this@SettingsActivity).load(userProfImg).placeholder(profile).into(setProfImg)
                    setUserName.setText(mUserName)
                    setFullName.setText(userProfName)
                    setStatus.setText(userProfStatus)
                    setDOB.setText(userProfDOB)
                    setCountry.setText(userProfCountry)
                    setGender.setText(userProfGender)
                    setRelStatus.setText(userRelStatus)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })

        updateAccSetBtn.onClick { validAccInfo() }
        setProfImg.onClick {
            val galleryIntent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, GalPick)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//The library of ArthurHub(2018) was used for image cropping.
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
                loadingBar.setTitle("Profile image")
                loadingBar.setMessage("Profile image is being updated")
                loadingBar.setCanceledOnTouchOutside(true)
                loadingBar.show()

                val resultUri = result.uri
                val filePath = profImgRef.child(curUserId + ".jpg")

                filePath.putFile(resultUri).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        toast("Profile image saved to Firebase")
                        val downloadUrl = task.result.downloadUrl.toString()

                        setUserRef.child("profileimage").setValue(downloadUrl)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        startActivity(intentFor<SettingsActivity>())
                                        toast("Profile Image saved to Firebase.")
                                        loadingBar.dismiss()
                                    } else {
                                        val message = task.exception?.message
                                        toast("Error: $message")
                                        loadingBar.dismiss()
                                    }
                                }
                    }
                }
            } else {
                toast("Image can not be cropped.")
                loadingBar.dismiss()
            }
        }
    }

    private fun validAccInfo() {
        val userName = setUserName.text.toString()
        val userProfName = setFullName.text.toString()
        val userStatus = setStatus.text.toString()
        val userDOB = setDOB.text.toString()
        val userCountry = setCountry.text.toString()
        val userGender = setGender.text.toString()
        val userRelStatus = setRelStatus.text.toString()

        when {
            TextUtils.isEmpty(userName) -> toast("Please add your name")
            TextUtils.isEmpty(userProfName) -> toast("Please add your profile name")
            TextUtils.isEmpty(userStatus) -> toast("Please add your status")
            TextUtils.isEmpty(userDOB) -> toast("Please add your date of birth")
            TextUtils.isEmpty(userCountry) -> toast("Please add your country")
            TextUtils.isEmpty(userGender) -> toast("Please add your gender")
            TextUtils.isEmpty(userRelStatus) -> toast("Please add your relationship status")
            else -> {
                loadingBar.setTitle("Profile Image")
                loadingBar.setMessage("Updating settings")
                loadingBar.setCanceledOnTouchOutside(true)
                loadingBar.show()
                updateAccInfo(userName, userProfName, userStatus, userDOB, userCountry, userGender, userRelStatus)
            }
        }
    }

    private fun updateAccInfo(username: String, usersprofilename: String, userstatus: String, usersdob: String, userscountry: String, usersgender: String, usersrelationshipstatus: String) {
        val settingsMap = HashMap<String, Any>()
        settingsMap["username"] = username
        settingsMap["fullname"] = usersprofilename
        settingsMap["status"] = userstatus
        settingsMap["dob"] = usersdob
        settingsMap["country"] = userscountry
        settingsMap["gender"] = usersgender
        settingsMap["relationshipstatus"] = usersrelationshipstatus
        setUserRef.updateChildren(settingsMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                sndToMainAct()
                toast("Settings updated")
                loadingBar.dismiss()
            } else {
                toast("Error")
                loadingBar.dismiss()
            }
        }
    }

    private fun sndToMainAct() {
        startActivity(intentFor<MainActivity>())
        finish()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        when (position) {
            0 -> setStatus.setText("Active")
            1 -> setStatus.setText("Inactive")
        }
        val edt = pref.edit()
        edt.putString("status", spinnerStatus.selectedItem.toString())
        edt.apply()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
    }

    companion object {
        internal const val GalPick = 1
    }
}
