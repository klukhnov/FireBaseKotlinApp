package com.example.home.socialservicenetworkapp

import android.R.color.holo_blue_dark
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import com.example.home.socialservicenetworkapp.R.layout.activity_click_post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_click_post.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast

//Kotlin implementation of the Application is original. Detailed comments on the process of Kotlin
//implementation are provided in the relevant Chapter 2 of the FPR. This comment applies to all project files of
// the Kotlin Application (the source code is provided in this Annex to the FPR).


class ClickPostActivity : AppCompatActivity() {
//Kotlin does not require (;) after each line of code. This makes the code cleaner and more readable.
    private lateinit var PostKey: String
    lateinit var currentUserId: String
    lateinit var dtbUserID: String
    lateinit var description: String
    lateinit var image: String
    private lateinit var ClickPostRef: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_click_post)
//Kotlin implementation of the interaction with Firebase is generally based on the Java code suggested by CodingCafe(2018)
//referred to in the FPR and is consistent with the samples of Java code, suggested by the official website of the Firebase
// (Firebase, 2018). This approach is discussed in detail in the FPR, applies to all Activities included in this Annex and
// is not repeated for convenience.

// Getting instances from the Firebase.
        firebaseAuth = FirebaseAuth.getInstance()
        currentUserId = firebaseAuth.currentUser!!.uid
        PostKey = intent.extras.get("PostKey").toString()
        ClickPostRef = FirebaseDatabase.getInstance().reference.child("Posts").child(PostKey)

        delPostBtn.visibility = View.INVISIBLE
        edtPostBtn.visibility = View.INVISIBLE

        ClickPostRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
//The below approach will be used throughout the Application: the relevant values of the Firebase's
//NoSQL database will be assigned based on the pre-defined matching fields and ID.
                    description = dataSnapshot.child("description").value.toString()
                    image = dataSnapshot.child("postimage").value.toString()
                    dtbUserID = dataSnapshot.child("uid").value.toString()

                    clickPostDesc.text = description
//Picasso library (Wharton et.al., 2018) is used in the Application for loading images.
                    Picasso.with(this@ClickPostActivity).load(image).into(clickPostImg)
//Users are able to delete and edit their own posts only (see the code below). In order to provide this functionality to
//an Administrator as well, the following line "|| currentUserId == databaseAdminID" could be added here, with definition of
//the Administrator's ID in advance.
                    if (currentUserId == dtbUserID) {
                        delPostBtn.visibility = View.VISIBLE
                        edtPostBtn.visibility = View.VISIBLE
                    }
//delPostBtn and editPostBtn are .xml fields. As discussed in the FPR, Anko library allows to access these fields directly.
                    edtPostBtn.onClick { edtCurPost(description) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
//In the Application, standard activities are defined as methods and are reused in various activities.
        delPostBtn.onClick { delCurPost() }
    }
//In the Application, the Kotlin code for AlertDialog is based on the Java code proposed by Sutradhar(2018).
    private fun edtCurPost(description: String?) {
// All methods and variables follow a standard naming convention.
        val builder = AlertDialog.Builder(this@ClickPostActivity)
        builder.setTitle("Edit Post")

        val inputField = EditText(this@ClickPostActivity)
        inputField.setText(description)
        builder.setView(inputField)

        builder.setPositiveButton("Update") { _, _ ->
            ClickPostRef.child("description").setValue(inputField.text.toString())
            sndToMainAct()
//toast() and longtoast() are methods of Anko library, as discussed in the FPR.
            toast("Post updated")
            Log.e("TAG", "Post of $currentUserId updated")
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
        dialog.window.setBackgroundDrawableResource(holo_blue_dark)
    }

    private fun delCurPost() {
        ClickPostRef.removeValue()
        sndToMainAct()
        longToast("Post deleted")
        Log.e("TAG", "Post of $currentUserId deleted")
    }
//intentFor() is another method of Anko library, discussed in the FPR.
private fun sndToMainAct() { startActivity(intentFor<MainActivity>().addFlags(FLAG_ACTIVITY_NEW_TASK))
    finish()}
}
