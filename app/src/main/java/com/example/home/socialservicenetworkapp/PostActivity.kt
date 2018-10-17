package com.example.home.socialservicenetworkapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color.BLACK
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import com.example.home.socialservicenetworkapp.R.drawable.backbutton
import com.example.home.socialservicenetworkapp.R.layout.activity_post
import com.example.home.socialservicenetworkapp.R.style.NavigationText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_post.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*

//General comments mentioned in the ClickPostActivity also apply to this Activity and are not repeated
//for convenience. Additional comments, specifically relevant for this Activity are included below.

class PostActivity : AppCompatActivity() {
    private lateinit var mTbar: Toolbar
    lateinit var loadingBar: ProgressDialog
    private lateinit var imageUri: Uri
    lateinit var desc: String
    private lateinit var postImageRef: StorageReference
    lateinit var usersRef: DatabaseReference
    lateinit var postsRef: DatabaseReference
    lateinit var mAuth: FirebaseAuth
    lateinit var saveCurDate: String
    lateinit var saveCurTime: String
    lateinit var postRandName: String
    lateinit var downloadUrl: String
    lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_post)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser!!.uid

        postImageRef = FirebaseStorage.getInstance().reference
        usersRef = FirebaseDatabase.getInstance().reference.child("Users")
        postsRef = FirebaseDatabase.getInstance().reference.child("Posts")
        loadingBar = ProgressDialog(this)
//Java code Android Developers(2018) served as a basis for the Kotlin implementation in this Application.
        mTbar = updatePostTbar as Toolbar
        setSupportActionBar(mTbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true) // for adding a back button
        supportActionBar?.title = "Add Post"
        mTbar.title = "Add Post"
        mTbar.setTitleTextColor(BLACK)
        mTbar.setTitleTextAppearance(this, NavigationText)
        mTbar.setNavigationIcon(backbutton)

        selectPostImg.onClick { openGal() }

        updatePostBtn.onClick { validPostInfo() }
    }

    private fun validPostInfo() {
        desc = postDesc.text.toString()

        when {
            imageUri == null -> {
                toast("Please select an image...")
                Log.e("TAG", "Image selection requested")

            }
            TextUtils.isEmpty(desc) -> {
                toast("Please add description...")
                Log.e("TAG", "Description requested")
            }
            else -> {
                loadingBar.setTitle("Add Post")
                loadingBar.setMessage("The post is being updated")
                loadingBar.show()
                loadingBar.setCanceledOnTouchOutside(true)

                storeImgToFirebase()
            }
        }
    }

    private fun storeImgToFirebase() {
        val calDate = Calendar.getInstance()
        val curDate = SimpleDateFormat("dd-MMMM-yyyy")
        saveCurDate = curDate.format(calDate.time)

        val calTime = Calendar.getInstance()
        val curTime = SimpleDateFormat("HH:mm")
        saveCurTime = curTime.format(calTime.time)
        postRandName = saveCurDate + saveCurTime

        val fPath = postImageRef.child("Post Images").child(imageUri.lastPathSegment + postRandName + ".jpg")

        fPath.putFile(imageUri).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                downloadUrl = task.result.downloadUrl.toString()
                toast("Upload successful")
                val messageSuccess = task.result.toString()
                Log.e("TAG", "Upload successful: $messageSuccess")

                savePostInfoToDtb()

            } else {
                val message = task.exception.toString()
                toast("Error occurred $message")
                Log.e("TAG", "Upload failed: $message")
            }
        }
    }

    private fun savePostInfoToDtb() {
        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userFullName = dataSnapshot.child("fullname").value.toString()
                    val userProfileImg = dataSnapshot.child("profileimage").value.toString()
                    val id = mAuth.currentUser!!.uid
                    val curDate = saveCurDate
                    val curTime = saveCurTime
                    val desc = desc
                    val downUrl = downloadUrl

                    val postsMap = HashMap<String, Any>()
                    postsMap["uid"] = id
                    postsMap["date"] = curDate
                    postsMap["time"] = curTime
                    postsMap["description"] = desc
                    postsMap["postimage"] = downUrl
                    postsMap["profileimage"] = userProfileImg
                    postsMap["fullname"] = userFullName
                    postsRef.child(currentUserId + postRandName).updateChildren(postsMap)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    sndToMainAct()
                                    toast("Database updated successfully")
                                    Log.e("TAG", "Firebase database updated successfully")
                                    loadingBar.dismiss()
                                } else {
                                    toast("Error occurred")
                                    Log.e("TAG", "Firebase database update failed")
                                    loadingBar.dismiss()
                                }
                            }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }
//The Java code of Shaikh(2018) was used as a basis for the below Kotlin implementation.
    private fun openGal() {
        val galIntent = Intent()
        galIntent.action = Intent.ACTION_GET_CONTENT
        galIntent.type = "image/*"
        startActivityForResult(galIntent, GalPick)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GalPick && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            selectPostImg.setImageURI(imageUri)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            sndToMainAct()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun sndToMainAct() {
        startActivity(intentFor<MainActivity>())
    }

    companion object {
        private const val GalPick = 1
    }
}