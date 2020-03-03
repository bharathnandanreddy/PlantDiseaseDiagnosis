package com.example.plantdiseasediagnosis

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        var custom_font: Typeface? = Typeface.createFromAsset(assets, "fonts/comicSans.ttf")
        titleText.setTypeface(custom_font)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        Handler().postDelayed({
            startActivity(Intent(this@SplashActivity,MainActivity::class.java))
            finishAffinity()
        }, 1000)
    }
}
