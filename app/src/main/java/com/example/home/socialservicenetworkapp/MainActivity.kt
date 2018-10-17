package com.example.home.socialservicenetworkapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.example.home.socialservicenetworkapp.R.drawable.*
import com.example.home.socialservicenetworkapp.R.layout.*
import com.example.home.socialservicenetworkapp.R.style.NavigationText
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.all_post_layout.view.*
import kotlinx.android.synthetic.main.navigation_header.view.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast

//General comments mentioned in the ClickPostActivity also apply to this Activity and are not repeated
//for convenience. Additional comments, specifically relevant for this Activity are included below.

class MainActivity : AppCompatActivity() {
    private lateinit var actBarToggle: ActionBarDrawerToggle
    private lateinit var mTbar: Toolbar
    lateinit var profileImg: CircleImageView
    lateinit var profileUserName: TextView
    lateinit var mAuth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference
    lateinit var postsRef: DatabaseReference
    lateinit var likesRef: DatabaseReference
    internal var likeChecker: Boolean? = false
    internal lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_main)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser!!.uid
        usersRef = FirebaseDatabase.getInstance().reference.child("Users")
        postsRef = FirebaseDatabase.getInstance().reference.child("Posts")
        likesRef = FirebaseDatabase.getInstance().reference.child("Likes")
//Please refer to the comment in PostActivity on the Toolbar element.
        mTbar = mainPgToolbar as Toolbar
        setSupportActionBar(mTbar)
        supportActionBar?.title = "Menu"
        mTbar.title = "Menu"
        mTbar.setTitleTextColor(Color.BLACK)
        mTbar.setTitleTextAppearance(this, NavigationText)

        actBarToggle = ActionBarDrawerToggle(this@MainActivity, drawableLayout, R.string.drawer_open, R.string.drawer_close)
        drawableLayout.addDrawerListener(actBarToggle)
        actBarToggle.syncState()
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(hmenu)
        allUsersPosts.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true
        allUsersPosts.layoutManager = linearLayoutManager

        val navView = navigationView.inflateHeaderView(navigation_header)
        profileImg = navView.navProfileImg
        profileUserName = navView.navUserFullName

        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.hasChild("fullname")) {
                        val fullname = dataSnapshot.child("fullname").value.toString()
                        profileUserName.text = fullname
                    }
                    if (dataSnapshot.hasChild("profileimage")) {
                        val image = dataSnapshot.child("profileimage").value.toString()
                        Picasso.with(this@MainActivity).load(image).placeholder(profile).into(profileImg)
                    } else {
                        toast("Profile name does not exist")
                        Log.e("TAG", "Profile name does not exist")
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })

        navigationView.setNavigationItemSelectedListener { item ->
            menuSelector(item)
            false
        }

        addNewPostBtn.onClick { sndToPostAct() }
        displayAllPosts()
    }

    private fun displayAllPosts() {

        val firebaseRAdapter = object : FirebaseRecyclerAdapter<Posts, PostsViewHolder>(Posts::class.java, all_post_layout, PostsViewHolder::class.java, postsRef) {
            override fun populateViewHolder(viewHolder: PostsViewHolder, model: Posts, position: Int) {
                val PostKey = getRef(position).key
//Getting the key of the post, the key step in this method.
                viewHolder.setFullName(model.fullname)
                viewHolder.setTime(model.time)
                viewHolder.setDate(model.date)
                viewHolder.setDesc(model.description)
                viewHolder.setProfileImg(applicationContext, model.profileimage)
                viewHolder.setPostImg(applicationContext, model.postimage)

                viewHolder.setBtnStatus(PostKey)

                viewHolder.mView.onClick {
                    startActivity(intentFor<ClickPostActivity>("PostKey" to PostKey))
                }

                viewHolder.commentPostBtn.onClick {
                    startActivity(intentFor<CommentsActivity>("PostKey" to PostKey))
                }

                viewHolder.likePostBtn.onClick {
                    likeChecker = true
                    likesRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (likeChecker == true) {
                                likeChecker = if (dataSnapshot.child(PostKey).hasChild(currentUserId)) {
                                    likesRef.child(PostKey).child(currentUserId).removeValue()
                                    false
                                } else {
                                    likesRef.child(PostKey).child(currentUserId).setValue(true)
                                    false
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                        }
                    })
                }
            }
        }

        allUsersPosts.adapter = firebaseRAdapter
    }

    class PostsViewHolder(internal var mView: View) : RecyclerView.ViewHolder(mView) {
        internal var likePostBtn: ImageButton = mView.likeBtn
        internal var commentPostBtn: ImageButton = mView.commentBtn
        internal var dispLikes: TextView = mView.displayLikes
        internal var countLikes: Int = 0
        internal var currentUserId: String = FirebaseAuth.getInstance().currentUser!!.uid
        private var likesRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Likes")

        fun setBtnStatus(PostKey: String) {
            likesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.child(PostKey).hasChild(currentUserId)) {
                        countLikes = dataSnapshot.child(PostKey).childrenCount.toInt()
                        likePostBtn.setImageResource(like)
                        dispLikes.text = Integer.toString(countLikes)
                    } else {
                        countLikes = dataSnapshot.child(PostKey).childrenCount.toInt()
                        likePostBtn.setImageResource(dislike)
                        dispLikes.text = Integer.toString(countLikes)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                }
            })
        }

        fun setFullName(fullname: String) {
            mView.postUserName.text = fullname
        }

        fun setProfileImg(ctx: Context, profileimage: String) {
            Picasso.with(ctx).load(profileimage).into(mView.postProfileImg)
        }

        fun setTime(time: String) {
            mView.postTime.text = "   $time"
        }

        fun setDate(date: String) {
            mView.postDate.text = "   $date"
        }

        fun setDesc(description: String) {
            mView.postDesc.text = " $description"
        }

        fun setPostImg(ctx: Context, postimage: String) {
            Picasso.with(ctx).load(postimage).into(mView.postImg)
            mView.postImg.clipToOutline = true
        }
    }

    private fun sndToPostAct() {
        startActivity(intentFor<PostActivity>())
    }

    override fun onStart() {
        super.onStart()

        val curUser = mAuth.currentUser

        if (curUser == null) {
            sndToLoginAct()
        } else {
            chkIfUserExists()
        }
    }

    private fun chkIfUserExists() {
        val curUserId = mAuth.currentUser!!.uid

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.hasChild(curUserId)) {
                    sndToSetupAct()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun sndToSetupAct() {
        startActivity(intentFor<SetupActivity>().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun sndToLoginAct() {
        startActivity(intentFor<LoginActivity>().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (actBarToggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }

    private fun menuSelector(item: MenuItem) {
        when (item.itemId) {
            R.id.nav_post -> sndToPostAct()
            R.id.nav_settings -> sndToSettingsAct()
            R.id.nav_logout -> {
                mAuth.signOut()
                sndToLoginAct()
            }
        }
    }

    private fun sndToSettingsAct() {
        startActivity(intentFor<SettingsActivity>())
    }
}

