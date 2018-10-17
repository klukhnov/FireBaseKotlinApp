package com.example.home.socialservicenetworkapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.example.home.socialservicenetworkapp.R.layout.activity_comments
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_comments.*
import kotlinx.android.synthetic.main.all_comments_layout.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*

//General comments mentioned in the ClickPostActivity also apply to this Activity and are not repeated
//for convenience. Additional comments, specifically relevant for this Activity are included below.

class CommentsActivity : AppCompatActivity() {

    private lateinit var usersRef: DatabaseReference
    lateinit var postsRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var postKey: String
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_comments)

        postKey = intent.extras!!.get("PostKey")!!.toString()

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser!!.uid
        usersRef = FirebaseDatabase.getInstance().reference.child("Users")
        postsRef = FirebaseDatabase.getInstance().reference.child("Posts").child(postKey).child("Comments")
        commentsList.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true
        commentsList.layoutManager = linearLayoutManager

        postCommentBtn.onClick {
            usersRef.child(currentUserId!!).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val userName = dataSnapshot.child("username").value!!.toString()
                        valComment(userName)
                        commentInput.setText("")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()

        val firebaseRecyclerAdapter = object : FirebaseRecyclerAdapter<Comments, CommentsViewHolder>(
                Comments::class.java, R.layout.all_comments_layout, CommentsViewHolder::class.java, postsRef
        ) {
            override fun populateViewHolder(viewHolder: CommentsViewHolder, model: Comments, position: Int) {
                viewHolder.setUsername(model.username)
                viewHolder.setComment(model.comment)
                viewHolder.setDate(model.date)
                viewHolder.setTime(model.time)
            }
        }
        commentsList.adapter = firebaseRecyclerAdapter
    }

    class CommentsViewHolder(private var mView: View) : RecyclerView.ViewHolder(mView) {
//Kotlin allows using the ($) sign to refer to a variables in a String.
        fun setUsername(username: String) {
            mView.commentUsername.text = " $username "
        }

        fun setComment(comment: String) {
            mView.commentText.text = comment
        }

        fun setDate(date: String) {
            mView.commentDate.text = " on: $date"
        }

        fun setTime(time: String) {
            mView.commentTime.text = " at: $time"
        }
    }

    private fun valComment(userName: String) {
        val comText = commentInput.text.toString()
        if (TextUtils.isEmpty(comText)) {
            toast("Add comment please")
            Log.e("TAG", "Adding comment requested")
        } else {
            val calDate = Calendar.getInstance()
            val curDate = SimpleDateFormat("dd-MMMM-yyyy")
            val saveCurDate = curDate.format(calDate.time)
            val calTime = Calendar.getInstance()
            val curTime = SimpleDateFormat("HH:mm")
            val saveCurTime = curTime.format(calTime.time)
//Random key is necessary to ensure unique entries in the NoSQL Firebase database.
            val randKey = "$currentUserId$saveCurDate-$saveCurTime"

            val id = mAuth.currentUser!!.uid
//Assignment method is used with HashMap to populate the Firebase database with multiple values at the same time.
            val comMap = HashMap<String, Any>()
            comMap["uid"] = id
            comMap["comment"] = comText
            comMap["date"] = saveCurDate
            comMap["time"] = saveCurTime
            comMap["username"] = userName

            postsRef.child(randKey).updateChildren(comMap)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            toast("Comment saved")
                            Log.e("TAG", "Comment with id {$id} saved on {$saveCurDate}")
                        } else {
                            toast("Error!")
//${task.exception.toString()} is called to obtain more meaningful information in LogCat View, as discussed in the FPR.
                            Log.e("TAG", "Saving comment failed: ${task.exception.toString()}")
                        }
                    }
        }
    }
}
