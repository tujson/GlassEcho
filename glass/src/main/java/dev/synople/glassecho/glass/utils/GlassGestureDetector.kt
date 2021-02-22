package dev.synople.glassecho.glass.utils

import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration

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

/**
 * From https://raw.githubusercontent.com/googlesamples/glass-enterprise-samples/master/GestureLibrarySample/main/src/main/java/com/example/glass/ui/GlassGestureDetector.java
 */

/**
 * Gesture detector for Google Glass usage purposes.
 *
 * It detects one and two finger gestures like:
 *
 *  * TAP
 *  * TWO_FINGER_TAP
 *  * SWIPE_FORWARD
 *  * TWO_FINGER_SWIPE_FORWARD
 *  * SWIPE_BACKWARD
 *  * TWO_FINGER_SWIPE_BACKWARD
 *  * SWIPE_UP
 *  * TWO_FINGER_SWIPE_UP
 *  * SWIPE_DOWN
 *  * TWO_FINGER_SWIPE_DOWN
 *
 *
 * Swipe detection depends on the:
 *
 *  * movement tan value
 *  * movement distance
 *  * movement velocity
 *
 *
 * To prevent unintentional SWIPE_DOWN, TWO_FINGER_SWIPE_DOWN, SWIPE_UP and TWO_FINGER_SWIPE_UP
 * gestures, they are detected if movement angle is only between 60 and 120 degrees to the
 * Glass touchpad horizontal axis.
 * Any other detected swipes, will be considered as SWIPE_FORWARD and SWIPE_BACKWARD gestures,
 * depends on the sign of the axis x movement value.
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
class GlassGestureDetector(context: Context?, onGestureListener: OnGestureListener) {
    /**
     * Currently handled gestures.
     */
    enum class Gesture {
        TAP, TWO_FINGER_TAP, SWIPE_FORWARD, TWO_FINGER_SWIPE_FORWARD, SWIPE_BACKWARD, TWO_FINGER_SWIPE_BACKWARD, SWIPE_UP, TWO_FINGER_SWIPE_UP, SWIPE_DOWN, TWO_FINGER_SWIPE_DOWN
    }

    /**
     * Listens for the gestures.
     */
    interface OnGestureListener {
        /**
         * Should notify about detected gesture.
         *
         * @param gesture is a detected gesture.
         * @return TRUE if gesture is handled by the method. FALSE otherwise.
         */
        fun onGesture(gesture: Gesture?): Boolean

        /**
         * Notifies when a scroll occurs with the initial on down [MotionEvent] and the current
         * move [MotionEvent]. The distance in x and y is also supplied for convenience.
         *
         * @param e1 The first down motion event that started the scrolling.
         * @param e2 The move motion event that triggered the current onScroll.
         * @param distanceX The distance along the X axis that has been scrolled since the last call to
         * onScroll. This is NOT the distance between `e1` and `e2`.
         * @param distanceY The distance along the Y axis that has been scrolled since the last call to
         * onScroll. This is NOT the distance between `e1` and `e2`.
         * @return true if the event is consumed, else false
         */
        fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return false
        }

        /**
         * Notifies when touch is ended.
         */
        fun onTouchEnded() {}
    }

    private val touchSlopSquare: Int

    /**
     * This flag is set to true each time the [MotionEvent.ACTION_DOWN] action appears
     * and it remains true until the finger moves out of the tap region.
     * Checking of the finger movement takes place each time the [MotionEvent.ACTION_MOVE]
     * action appears, until finger moves out of the tap region.
     * If this flag is set to false, [Gesture.TAP] and [Gesture.TWO_FINGER_TAP]
     * gestures won't be notified as detected.
     *
     * Tap region is calculated from the [ViewConfiguration.getScaledTouchSlop] value.
     * It prevents from detecting [Gesture.TAP] and [Gesture.TWO_FINGER_TAP] gestures
     * during the scrolling on the touchpad.
     */
    private var isInTapRegion = false
    private var isTwoFingerGesture = false
    private var isActionDownPerformed = false
    private var firstFingerDownX = 0f
    private var firstFingerDownY = 0f
    private var firstFingerLastFocusX = 0f
    private var firstFingerLastFocusY = 0f
    private var firstFingerVelocityX = 0f
    private var firstFingerVelocityY = 0f
    private var firstFingerDistanceX = 0f
    private var firstFingerDistanceY = 0f
    private var secondFingerDownX = 0f
    private var secondFingerDownY = 0f
    private var secondFingerDistanceX = 0f
    private var secondFingerDistanceY = 0f
    private var velocityTracker: VelocityTracker? = null
    private var currentDownEvent: MotionEvent? = null
    private val onGestureListener: OnGestureListener

    /**
     * Passes the [MotionEvent] object from the activity to the Android [ ].
     *
     * @param motionEvent is a detected [MotionEvent] object.
     * @return TRUE if event is handled by the Android [GestureDetector]. FALSE otherwise.
     */
    fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(motionEvent)
        var handled = false
        when (motionEvent.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                run {
                    firstFingerLastFocusX = motionEvent.x
                    firstFingerDownX = firstFingerLastFocusX
                }
                run {
                    firstFingerLastFocusY = motionEvent.y
                    firstFingerDownY = firstFingerLastFocusY
                }
                isActionDownPerformed = true
                isInTapRegion = true
                if (currentDownEvent != null) {
                    currentDownEvent!!.recycle()
                }
                currentDownEvent = MotionEvent.obtain(motionEvent)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                isTwoFingerGesture = true
                secondFingerDownX = motionEvent.getX(motionEvent.actionIndex)
                secondFingerDownY = motionEvent.getY(motionEvent.actionIndex)
            }
            MotionEvent.ACTION_MOVE -> {
                val firstFingerFocusX = motionEvent.getX(FIRST_FINGER_POINTER_INDEX)
                val firstFingerFocusY = motionEvent.getY(FIRST_FINGER_POINTER_INDEX)
                val scrollX = firstFingerLastFocusX - firstFingerFocusX
                val scrollY = firstFingerLastFocusY - firstFingerFocusY
                firstFingerDistanceX = firstFingerFocusX - firstFingerDownX
                firstFingerDistanceY = firstFingerFocusY - firstFingerDownY
                if (motionEvent.pointerCount > 1) {
                    secondFingerDistanceX =
                        motionEvent.getX(SECOND_FINGER_POINTER_INDEX) - secondFingerDownX
                    secondFingerDistanceY =
                        motionEvent.getY(SECOND_FINGER_POINTER_INDEX) - secondFingerDownY
                }
                if (isInTapRegion) {
                    val distance = (firstFingerDistanceX * firstFingerDistanceX
                            + firstFingerDistanceY * firstFingerDistanceY)
                    var distanceSecondFinger = 0f
                    if (motionEvent.pointerCount > 1) {
                        distanceSecondFinger = (secondFingerDistanceX * secondFingerDistanceX
                                + secondFingerDistanceY * secondFingerDistanceY)
                    }
                    if (distance > touchSlopSquare || distanceSecondFinger > touchSlopSquare) {
                        isInTapRegion = false
                    }
                }
                if (Math.abs(scrollX) >= 1 || Math.abs(scrollY) >= 1) {
                    handled = onGestureListener
                        .onScroll(currentDownEvent, motionEvent, scrollX, scrollY)
                    firstFingerLastFocusX = firstFingerFocusX
                    firstFingerLastFocusY = firstFingerFocusY
                }
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker!!.computeCurrentVelocity(VELOCITY_UNIT)
                firstFingerVelocityX = velocityTracker!!
                    .getXVelocity(motionEvent.getPointerId(motionEvent.actionIndex))
                firstFingerVelocityY = velocityTracker!!
                    .getYVelocity(motionEvent.getPointerId(motionEvent.actionIndex))
                handled = detectGesture()
                onTouchEnded()
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker!!.recycle()
                velocityTracker = null
                isInTapRegion = false
            }
        }
        return handled
    }

    private fun detectGesture(): Boolean {
        if (!isActionDownPerformed) {
            return false
        }
        val tan =
            if (firstFingerDistanceX != 0f) Math.abs(firstFingerDistanceY / firstFingerDistanceX)
                .toDouble() else Double.MAX_VALUE
        return if (isTwoFingerGesture) {
            val tanSecondFinger =
                if (secondFingerDistanceX != 0f) Math.abs(secondFingerDistanceY / secondFingerDistanceX)
                    .toDouble() else Double.MAX_VALUE
            detectTwoFingerGesture(tan, tanSecondFinger)
        } else {
            detectOneFingerGesture(tan)
        }
    }

    private fun detectOneFingerGesture(tan: Double): Boolean {
        if (tan > TAN_ANGLE_DEGREES) {
            if (Math.abs(firstFingerDistanceY) < SWIPE_DISTANCE_THRESHOLD_PX
                || Math.abs(firstFingerVelocityY) < SWIPE_VELOCITY_THRESHOLD_PX
            ) {
                if (isInTapRegion) {
                    return onGestureListener.onGesture(Gesture.TAP)
                }
            } else if (firstFingerDistanceY < 0) {
                return onGestureListener.onGesture(Gesture.SWIPE_UP)
            } else if (firstFingerDistanceY > 0) {
                return onGestureListener.onGesture(Gesture.SWIPE_DOWN)
            }
        } else {
            if (Math.abs(firstFingerDistanceX) < SWIPE_DISTANCE_THRESHOLD_PX
                || Math.abs(firstFingerVelocityX) < SWIPE_VELOCITY_THRESHOLD_PX
            ) {
                if (isInTapRegion) {
                    return onGestureListener.onGesture(Gesture.TAP)
                }
            } else if (firstFingerDistanceX < 0) {
                return onGestureListener.onGesture(Gesture.SWIPE_FORWARD)
            } else if (firstFingerDistanceX > 0) {
                return onGestureListener.onGesture(Gesture.SWIPE_BACKWARD)
            }
        }
        return false
    }

    private fun detectTwoFingerGesture(tan: Double, tanSecondFinger: Double): Boolean {
        if (tan > TAN_ANGLE_DEGREES && tanSecondFinger > TAN_ANGLE_DEGREES) {
            if (Math.abs(firstFingerDistanceY) < SWIPE_DISTANCE_THRESHOLD_PX
                || Math.abs(firstFingerVelocityY) < SWIPE_VELOCITY_THRESHOLD_PX
            ) {
                if (isInTapRegion) {
                    return onGestureListener.onGesture(Gesture.TWO_FINGER_TAP)
                }
            } else if (firstFingerDistanceY < 0 && secondFingerDistanceY < 0) {
                return onGestureListener.onGesture(Gesture.TWO_FINGER_SWIPE_UP)
            } else if (firstFingerDistanceY > 0 && secondFingerDistanceY > 0) {
                return onGestureListener.onGesture(Gesture.TWO_FINGER_SWIPE_DOWN)
            }
        } else {
            if (Math.abs(firstFingerDistanceX) < SWIPE_DISTANCE_THRESHOLD_PX
                || Math.abs(firstFingerVelocityX) < SWIPE_VELOCITY_THRESHOLD_PX
            ) {
                if (isInTapRegion) {
                    return onGestureListener.onGesture(Gesture.TWO_FINGER_TAP)
                }
            } else if (firstFingerDistanceX < 0 && secondFingerDistanceX < 0) {
                return onGestureListener.onGesture(Gesture.TWO_FINGER_SWIPE_FORWARD)
            } else if (firstFingerDistanceX > 0 && secondFingerDistanceX > 0) {
                return onGestureListener.onGesture(Gesture.TWO_FINGER_SWIPE_BACKWARD)
            }
        }
        return false
    }

    private fun onTouchEnded() {
        isTwoFingerGesture = false
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
        isActionDownPerformed = false
        onGestureListener.onTouchEnded()
    }

    companion object {
        private const val VELOCITY_UNIT = 1000
        private const val FIRST_FINGER_POINTER_INDEX = 0
        private const val SECOND_FINGER_POINTER_INDEX = 1
        private val TAN_ANGLE_DEGREES = Math.tan(Math.toRadians(60.0))
        const val SWIPE_DISTANCE_THRESHOLD_PX = 100
        const val SWIPE_VELOCITY_THRESHOLD_PX = 100
    }

    /**
     * [GlassGestureDetector] object is constructed by usage of this method.
     *
     * @param context is a context of the application.
     * @param onGestureListener is a listener for the gestures.
     */
    init {
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        touchSlopSquare = touchSlop * touchSlop
        this.onGestureListener = onGestureListener
    }
}