package dev.synople.glassecho.glass

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem

const val CONNECT = "connect"
const val UNPUBLISH_LIVE_CARD = "unpublishLiveCard"

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
                Intent().also { intent ->
                    intent.action = CONNECT
                    sendBroadcast(intent)
                }
                return true
            }
            R.id.action_disconnect -> {
                Intent().also { intent ->
                    intent.action = UNPUBLISH_LIVE_CARD
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
