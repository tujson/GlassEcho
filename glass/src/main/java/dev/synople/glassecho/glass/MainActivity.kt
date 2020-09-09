package dev.synople.glassecho.glass

import android.app.Activity
import android.os.Bundle
import android.view.View
import dev.synople.glassecho.glass.fragments.ConnectFragment

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        fragmentManager
            .beginTransaction()
            .replace(R.id.frameLayoutMain, ConnectFragment.newInstance())
            .commit()
    }
}