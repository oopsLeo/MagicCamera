package com.frost.magiccamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.frost.magiccamera.http.HttpHelper
import com.squareup.picasso.Picasso
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private lateinit var uri:Uri
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        val extras = intent.extras!!
        uri = Uri.parse(extras.getString("uri_string"))
        val rotation = extras.getFloat("rotation")

        val iv: ImageView = findViewById(R.id.iv)
        val btnAccept:Button = findViewById(R.id.preview_accept)
        val btnCancel:Button = findViewById(R.id.preview_cancel)

        PushDownAnim.setPushDownAnimTo(btnAccept)
        PushDownAnim.setPushDownAnimTo(btnCancel)


        Picasso.get()
            .load(uri)
            .rotate(rotation)
            .into(iv)

        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        btnAccept.setOnClickListener{ jumpToEdit() }

    }

    private fun jumpToEdit() {

        val intent = Intent(
            this,
            EditActivity::class.java
        )
        intent.data = uri
        startActivity(intent)

        setResult(RESULT_OK)
        finish()
    }

}