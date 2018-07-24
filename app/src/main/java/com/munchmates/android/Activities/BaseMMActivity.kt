package com.munchmates.android.Activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.munchmates.android.App

abstract class BaseMMActivity: AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        if(App.users.size == 0) {
            val intent = Intent(this, MMActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}