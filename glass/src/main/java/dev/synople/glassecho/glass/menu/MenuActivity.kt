/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.synople.glassecho.glass.menu

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import dev.synople.glassecho.glass.BaseActivity
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glassgesturedetector.GlassGestureDetector
import java.util.*

/**
 * Activity which provides the menu functionality. It creates the horizontal recycler view to move
 * between menu items.
 */
class MenuActivity : BaseActivity(),
    GlassGestureDetector.OnGestureListener {
    private var adapter: MenuAdapter? = null
    private val menuItems: MutableList<GlassMenuItem> =
        ArrayList()
    private var currentMenuItemIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_layout)
        val recyclerView =
            findViewById<RecyclerView>(R.id.menuRecyclerView)
        adapter = MenuAdapter(menuItems)
        val layoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL, false
            )
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.isFocusable = true
        val snapHelper: SnapHelper =
            PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                val foundView = snapHelper.findSnapView(layoutManager) ?: return
                currentMenuItemIndex = layoutManager.getPosition(foundView)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuResource = intent
            .getIntExtra(
                EXTRA_MENU_KEY,
                EXTRA_MENU_ITEM_DEFAULT_VALUE
            )
        if (menuResource != EXTRA_MENU_ITEM_DEFAULT_VALUE) {
            val inflater = menuInflater
            inflater.inflate(menuResource, menu)
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                menuItems.add(
                    GlassMenuItem(
                        menuItem.itemId, menuItem.icon,
                        menuItem.title.toString()
                    )
                )
                adapter!!.notifyDataSetChanged()
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture?): Boolean {
        if (gesture === GlassGestureDetector.Gesture.TAP) {
            val intent = Intent()
            intent.putExtra(
                EXTRA_MENU_ITEM_ID_KEY,
                menuItems[currentMenuItemIndex].id
            )
            setResult(Activity.RESULT_OK, intent)
            finish()
            return true
        }
        return super.onGesture(gesture)
    }

    companion object {
        /**
         * Key for the menu item id.
         */
        const val EXTRA_MENU_ITEM_ID_KEY = "id"

        /**
         * Default value for the menu item.
         */
        const val EXTRA_MENU_ITEM_DEFAULT_VALUE = -1

        /**
         * Key for the menu.
         */
        const val EXTRA_MENU_KEY = "menu_key"
    }
}