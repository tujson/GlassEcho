package dev.synople.glassecho.glass

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import dev.synople.glassecho.glass.fragments.BaseFragment
import dev.synople.glassecho.glass.fragments.MainLayoutFragment
import dev.synople.glassecho.glassgesturedetector.GlassGestureDetector
import java.util.*

/**
 * Main activity of the application. It provides viewPager to move between fragments.
 */
class MainActivity : BaseActivity() {
    private val fragments: MutableList<BaseFragment> =
        ArrayList<BaseFragment>()
    private var viewPager: ViewPager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_pager_layout)

        val screenSlidePagerAdapter =
            ScreenSlidePagerAdapter(
                supportFragmentManager
            )
        viewPager = findViewById(R.id.viewPager)
        viewPager!!.adapter = screenSlidePagerAdapter
        fragments.add(
            MainLayoutFragment
                .newInstance(
                    getString(R.string.different_options), getString(R.string.empty_string),
                    getString(R.string.empty_string), R.menu.main_menu
                )
        )
        fragments.add(
            MainLayoutFragment
                .newInstance(
                    getString(R.string.text_sample), getString(R.string.footnote_sample),
                    getString(R.string.timestamp_sample), null
                )
        )
        fragments.add(
            MainLayoutFragment
                .newInstance(
                    getString(R.string.like_this_sample), getString(R.string.empty_string),
                    getString(R.string.empty_string), null
                )
        )

        screenSlidePagerAdapter.notifyDataSetChanged()
        val tabLayout: TabLayout = findViewById(R.id.page_indicator)
        tabLayout.setupWithViewPager(viewPager, true)
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture?): Boolean {
        return when (gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                fragments[viewPager!!.currentItem].onSingleTapUp()
                true
            }
            else -> super.onGesture(gesture)
        }
    }

    private inner class ScreenSlidePagerAdapter internal constructor(fm: FragmentManager?) :
        FragmentStatePagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }
    }
}
