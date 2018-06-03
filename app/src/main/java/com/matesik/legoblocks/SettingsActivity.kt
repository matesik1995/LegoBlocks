package com.matesik.legoblocks

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_settings.*



class SettingsActivity : AppCompatActivity() {
    companion object {
        val PREFERENCES = "myPreferences"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val prefs = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        apiUrl.setText(prefs.getString("apiUrl", "http://fcds.cs.put.poznan.pl/MyWeb/BL/"))
    }

    override fun onDestroy() {
        val editor = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
        editor.putString("apiUrl", apiUrl.text.toString())
        editor.apply()
        super.onDestroy()
    }
}
