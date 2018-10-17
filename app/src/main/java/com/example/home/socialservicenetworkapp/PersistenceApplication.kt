package com.example.home.socialservicenetworkapp

import android.app.Application

import com.google.firebase.database.FirebaseDatabase

//The Java code of Saurabh (2017) served as a basis for the below Kotlin implementation.

class PersistenceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
