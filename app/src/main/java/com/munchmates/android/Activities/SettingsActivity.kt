package com.munchmates.android.Activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.munchmates.android.App
import com.munchmates.android.Prefs
import com.munchmates.android.R
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.toast
import java.io.ByteArrayOutputStream

class SettingsActivity : AppCompatActivity(), View.OnClickListener {

    var usersRef = FirebaseDatabase.getInstance().reference
    var stoRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://munch-mates-marquette.appspot.com/imgProfilePictures/")
    val CODE = 7
    var newImage: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings_button_logout.setOnClickListener(this)
        settings_button_save.setOnClickListener(this)
        settings_text_head.setOnClickListener(this)
        settings_button_pwreset.setOnClickListener(this)
        settings_button_delacct.setOnClickListener(this)
        settings_button_helpdesk.setOnClickListener(this)

        val schools = arrayListOf<String>()
        for (college in App.colleges) {
            schools.add(college.collegeName)
        }

        val mates = arrayListOf<String>()
        for (mate in App.mates) {
            mates.add(mate.mateTypeName)
        }

        val gAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mates)
        gAdapter.setDropDownViewResource(R.layout.item_spinner)
        settings_spinner_type.adapter = gAdapter

        val cAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, schools)
        cAdapter.setDropDownViewResource(R.layout.item_spinner)
        settings_spinner_school.adapter = cAdapter
    }

    override fun onResume() {
        super.onResume()
        fillPage()
    }

    private fun fillPage() {
        val user = App.user

        settings_edit_first.setText(user.firstName)
        settings_edit_last.setText(user.lastName)
        settings_edit_town.setText(user.city)
        settings_edit_state.setText(user.stateCountry)

        settings_switch_mute.isChecked = user.muteMode
        settings_switch_meal.isChecked = user.mealPlan
        settings_switch_notif.isChecked = user.emailNotifications

        stoRef = stoRef.child("${user.uid}.png")
        Glide.with(this)
                .load(stoRef)
                .into(settings_image_head)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CODE && resultCode == Activity.RESULT_OK && data != null) {
            val inStream = contentResolver.openInputStream(data.data)
            newImage = BitmapFactory.decodeStream(inStream)
            val width = newImage!!.width
            val height = newImage!!.height
            val widthSmaller = width < height
            if(widthSmaller) {
                val delta = height - width
                newImage = Bitmap.createBitmap(newImage, 0, delta/2, width, width)
            }
            else {
                val delta = width - height
                newImage = Bitmap.createBitmap(newImage, delta/2, 0, height, height)
            }
            newImage = Bitmap.createScaledBitmap(newImage, 1024, 1024, false)
            settings_image_head.setImageBitmap(newImage)
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.settings_button_logout -> {
                Prefs.instance.put(Prefs.EMAIL_PREF, "")
                Prefs.instance.put(Prefs.PASSWORD_PREF, "")
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, MMActivity::class.java))
            }
            R.id.settings_text_head -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.setType("image/*")
                intent.setAction(Intent.ACTION_GET_CONTENT)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), CODE)
            }
            R.id.settings_button_save -> {
                val user = App.user
                user.firstName = settings_edit_first.text.toString()
                user.lastName = settings_edit_last.text.toString()
                user.city = settings_edit_town.text.toString()
                user.stateCountry = settings_edit_state.text.toString()
                user.mateType = settings_spinner_type.selectedItem as String
                user.college = settings_spinner_school.selectedItem as String
                user.muteMode = settings_switch_mute.isChecked
                user.mealPlan = settings_switch_meal.isChecked
                user.emailNotifications = settings_switch_notif.isChecked
                usersRef.setValue(user)

                if(newImage != null) {
                    val stream = ByteArrayOutputStream()
                    newImage!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    stoRef.putBytes(stream.toByteArray())
                }
                finish()
            }
            R.id.settings_button_helpdesk -> {
                intent = Intent(Intent.ACTION_SEND)
                intent.setType("plain/text")
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("munchmates@marquette.edu"))
                intent.putExtra(Intent.EXTRA_SUBJECT, "HELPDESK REQUEST")
                startActivity(Intent.createChooser(intent, "Send helpdesk email..."))
            }
            R.id.settings_button_pwreset -> {
                FirebaseAuth.getInstance().sendPasswordResetEmail(App.user.email).addOnCompleteListener(this) { task ->
                    if(task.isSuccessful) {
                        toast("Password reset email sent")
                    }
                    else {
                        toast("Error sending password reset email")
                    }
                }
            }
            R.id.settings_button_delacct -> {
                FirebaseAuth.getInstance().currentUser!!.delete().addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        toast("Account successfully deleted")
                        Prefs.instance.put(Prefs.EMAIL_PREF, "")
                        Prefs.instance.put(Prefs.PASSWORD_PREF, "")
                        startActivity(Intent(this, MMActivity::class.java))
                    } else {
                        toast("Error deleting account")
                    }
                }
            }
        }
    }
}