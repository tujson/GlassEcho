package dev.synople.glassecho.glass

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import dev.synople.glassecho.glass.LiveCardService.Companion.CONFIG_CHANGE
import dev.synople.glassecho.glass.LiveCardService.Companion.bpm

class LiveCardMenuActivity : Activity() {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        openOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.live_card, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_connect -> {
                bpm -= 10
                Intent().also { intent ->
                    intent.action = CONFIG_CHANGE
                    sendBroadcast(intent)
                }
                return true
            }
            R.id.action_disconnect -> {
                bpm += 10
                Intent().also { intent ->
                    intent.action = CONFIG_CHANGE
                    sendBroadcast(intent)
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onOptionsMenuClosed(menu: Menu) {
        super.onOptionsMenuClosed(menu)
        finish()
    }
}
