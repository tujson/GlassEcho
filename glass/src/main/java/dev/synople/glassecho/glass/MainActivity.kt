package dev.synople.glassecho.glass

import android.os.Bundle
import android.view.MotionEvent
import androidx.fragment.app.FragmentActivity
import dev.synople.glassecho.glass.fragments.ConnectFragment
import org.greenrobot.eventbus.EventBus


private val TAG = MainActivity::class.java.simpleName

class MainActivity : FragmentActivity() {

    private lateinit var gestureDetector: GlassGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gestureDetector =
            GlassGestureDetector(this, object : GlassGestureDetector.OnGestureListener {
                override fun onGesture(gesture: GlassGestureDetector.Gesture?): Boolean {
                    val isHandled = when (gesture) {
                        GlassGestureDetector.Gesture.SWIPE_FORWARD -> true
                        GlassGestureDetector.Gesture.SWIPE_BACKWARD -> true
                        GlassGestureDetector.Gesture.SWIPE_UP -> true
                        GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_FORWARD -> true
                        GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_BACKWARD -> true
                        GlassGestureDetector.Gesture.TAP -> true
                        else -> false
                    }

                    if (isHandled) {
                        EventBus.getDefault().post(GlassGesture(gesture!!))
                    }

                    return isHandled
                }
            })

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frameLayoutMain, ConnectFragment.newInstance())
            .commit()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        event?.let {
            return gestureDetector.onTouchEvent(it)
        }
        return super.onGenericMotionEvent(event)
    }
}