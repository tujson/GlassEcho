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
package dev.synople.glassecho.glassgesturedetector

import android.view.GestureDetector
import android.view.MotionEvent

/**
 * Gesture detector for Google Glass usage purposes.
 */
class GlassGestureDetector(
    context: android.content.Context?,
    onGestureListener: OnGestureListener
) :
    GestureDetector.OnGestureListener {
    /**
     * Currently handled gestures.
     */
    enum class Gesture {
        TAP, SWIPE_FORWARD, SWIPE_BACKWARD, SWIPE_UP, SWIPE_DOWN
    }

    /**
     * Listens for the gestures.
     */
    interface OnGestureListener {
        /**
         * Should notify about detected gesture.
         *
         * @param gesture is a detected gesture.
         * @return TRUE if gesture is handled by the medhod. FALSE otherwise.
         */
        fun onGesture(gesture: Gesture?): Boolean
    }

    private val gestureDetector: GestureDetector = GestureDetector(context, this)
    private val onGestureListener: OnGestureListener = onGestureListener

    /**
     * Passes the [MotionEvent] object from the activity to the Android [GestureDetector].
     *
     * @param motionEvent is a detected [MotionEvent] object.
     * @return TRUE if event is handled by the Android [GestureDetector]. FALSE otherwise.
     */
    fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(motionEvent)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return onGestureListener.onGesture(Gesture.TAP)
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {}

    /**
     * Swipe detection depends on the:
     * - movement tan value,
     * - movement distance,
     * - movement velocity.
     *
     * To prevent unintentional SWIPE_DOWN and SWIPE_UP gestures, they are detected if movement
     * angle is only between 60 and 120 degrees.
     * Any other detected swipes, will be considered as SWIPE_FORWARD and SWIPE_BACKWARD, depends
     * on deltaX value sign.
     *
     * ______________________________________________________________
     * |                     \        UP         /                    |
     * |                       \               /                      |
     * |                         60         120                       |
     * |                           \       /                          |
     * |                             \   /                            |
     * |  BACKWARD  <-------  0  ------------  180  ------>  FORWARD  |
     * |                             /   \                            |
     * |                           /       \                          |
     * |                         60         120                       |
     * |                       /               \                      |
     * |                     /       DOWN        \                    |
     * --------------------------------------------------------------
     */
    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val deltaX: Float = e2.getX() - e1.getX()
        val deltaY: Float = e2.getY() - e1.getY()
        val tan =
            (if (deltaX != 0f) Math.abs(deltaY / deltaX) else Double.MAX_VALUE).toDouble()
        return if (tan > TAN_ANGLE_DEGREES) {
            if (Math.abs(deltaY) < SWIPE_DISTANCE_THRESHOLD_PX || Math.abs(
                    velocityY
                ) < SWIPE_VELOCITY_THRESHOLD_PX
            ) {
                false
            } else if (deltaY < 0) {
                onGestureListener.onGesture(Gesture.SWIPE_UP)
            } else {
                onGestureListener.onGesture(Gesture.SWIPE_DOWN)
            }
        } else {
            if (Math.abs(deltaX) < SWIPE_DISTANCE_THRESHOLD_PX || Math.abs(
                    velocityX
                ) < SWIPE_VELOCITY_THRESHOLD_PX
            ) {
                false
            } else if (deltaX < 0) {
                onGestureListener.onGesture(Gesture.SWIPE_FORWARD)
            } else {
                onGestureListener.onGesture(Gesture.SWIPE_BACKWARD)
            }
        }
    }

    companion object {
        const val SWIPE_DISTANCE_THRESHOLD_PX = 100
        const val SWIPE_VELOCITY_THRESHOLD_PX = 100
        private val TAN_ANGLE_DEGREES =
            Math.tan(Math.toRadians(60.0))
    }

}