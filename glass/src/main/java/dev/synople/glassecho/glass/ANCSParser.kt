package dev.synople.glassecho.glass

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.os.Handler
import android.os.Message
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

class ANCSParser(var mContext: Context) {
    private val pendingNotifications: MutableList<ANCSData> = LinkedList()
    private val notificationHandler: Handler


    private var ancs: BluetoothGattService? = null
    private var gatt: BluetoothGatt? = null

    private val notificationListeners = ArrayList<NotificationListener>()
    private var currData: ANCSData? = null

    interface NotificationListener {
        fun onNotificationAdd(n: IOSNotification?)
        fun onNotificationRemove(uid: Int)
    }

    fun addNotificationListener(listener: NotificationListener) {
        if (!notificationListeners.contains(listener)) notificationListeners.add(listener)
    }

    fun setService(ancs: BluetoothGattService?, gatt: BluetoothGatt?) {
        this.ancs = ancs
        this.gatt = gatt
    }

    private fun sendNotification(notif: IOSNotification) {
        IOSNotification.log("[Add Notification] : " + notif.uid)
        for (lis in notificationListeners) {
            lis.onNotificationAdd(notif)
        }
    }

    private fun cancelNotification(uid: Int) {
        IOSNotification.log("[cancel Notification] : $uid")
        for (lis in notificationListeners) {
            lis.onNotificationRemove(uid)
        }
    }

    private inner class ANCSData internal constructor(
        val notifyData // 8 bytes
        : ByteArray?
    ) {
        var timeExpired: Long
        var curStep = 0

        var bout: ByteArrayOutputStream? = null
        var noti: IOSNotification
        fun clear() {
            if (bout != null) {
                bout!!.reset()
            }
            bout = null
            curStep = 0
        }

        val uID: Int
            get() = (0xff and notifyData!![7].toInt() shl 24 or (0xff and notifyData[6].toInt() shl 16)
                    or (0xff and notifyData[5].toInt() shl 8) or (0xff and notifyData[4]
                .toInt()))

        fun finish() {
            if (null == bout) {
                return
            }
            val data = bout!!.toByteArray()
            if (data.size < 5) {
                return  //
            }
            val cmdId = data[0].toInt()
            if (cmdId != 0) {
                IOSNotification.log("bad cmdId: $cmdId")
                return
            }
            val uid = (0xff and data[4].toInt() shl 24 or (0xff and data[3]
                .toInt() shl 16)
                    or (0xff and data[2].toInt() shl 8) or (0xff and data[1].toInt()))
            if (uid != currData!!.uID) {
                IOSNotification.log("bad uid: " + uid + " -> " + currData!!.uID)
                return
            }

            // read attributes
            noti.uid = uid
            var curIdx = 5
            while (true) {
                if (noti.isAllInit) {
                    break
                }
                if (data.size < curIdx + 3) {
                    return
                }
                // attributes head
                val attrId = data[curIdx].toInt()
                val attrLen: Int =
                    (data[curIdx + 1] and 0xFF.toByte() or (0xFF.toByte() and (data[curIdx + 2].toInt() shl 8).toByte())).toInt()
                curIdx += 3
                if (data.size < curIdx + attrLen) {
                    return
                }
                val `val` = String(data, curIdx, attrLen)
                if (attrId == NotificationAttributeIDTitle) {
                    noti.title = `val`
                } else if (attrId == NotificationAttributeIDMessage) {
                    noti.message = `val`
                } else if (attrId == NotificationAttributeIDDate) {
                    noti.date = `val`
                } else if (attrId == NotificationAttributeIDSubtitle) {
                    noti.subtitle = `val`
                } else if (attrId == NotificationAttributeIDMessageSize) {
                    noti.messageSize = `val`
                }
                curIdx += attrLen
            }
            IOSNotification.log("got a notification! data size = " + data.size)
            currData = null
            //			mHandler.sendEmptyMessage(MSG_DO_NOTIFICATION); // continue next!
            sendNotification(noti)
        }

        init {
            curStep = 0
            timeExpired = System.currentTimeMillis()
            noti = IOSNotification()
        }
    }

    private fun processNotificationList() {
        notificationHandler.removeMessages(MSG_DO_NOTIFICATION)

        if (currData == null) {
            if (pendingNotifications.size == 0) {
                return
            }
            currData = pendingNotifications.removeAt(0)
            IOSNotification.log("ANCS New CurData")
        } else if (currData!!.curStep == 0) { // parse notify data
            do {
                if (currData!!.notifyData == null
                    || currData!!.notifyData!!.size != 8
                ) {
                    currData = null // ignore
                    IOSNotification.logw("ANCS Bad Head!")
                    break
                }
                if (EventIDNotificationRemoved == currData!!.notifyData!![0].toInt()) {
                    val uid: Int = (currData!!.notifyData!![4] and 0xff.toByte() or
                            ((currData!!.notifyData!![5] and 0xff.toByte()).toInt() shl 8).toByte() or
                            ((currData!!.notifyData!![6] and 0xff.toByte()).toInt() shl 16).toByte() or
                            ((currData!!.notifyData!![7] and 0xff.toByte()).toInt() shl 24).toByte()).toInt()
                    cancelNotification(uid)
                    currData = null
                    break
                }
                if (EventIDNotificationAdded != currData!!.notifyData!![0].toInt()) {
                    currData = null // ignore
                    IOSNotification.logw("ANCS NOT Add!")
                    break
                }
                // get attribute if needed!
                val cha = ancs
                    ?.getCharacteristic(GattConstants.Apple.CONTROL_POINT)
                if (null != cha) {
                    val bout = ByteArrayOutputStream()
                    // command ，commandID
                    bout.write(0)
                    // notify id ，
                    bout.write(currData!!.notifyData!![4].toInt())
                    bout.write(currData!!.notifyData!![5].toInt())
                    bout.write(currData!!.notifyData!![6].toInt())
                    bout.write(currData!!.notifyData!![7].toInt())

                    bout.write(NotificationAttributeIDTitle)
                    bout.write(50)
                    bout.write(0)

                    bout.write(NotificationAttributeIDSubtitle)
                    bout.write(100)
                    bout.write(0)

                    bout.write(NotificationAttributeIDMessage)
                    bout.write(500)
                    bout.write(0)

                    bout.write(NotificationAttributeIDMessageSize)
                    bout.write(10)
                    bout.write(0)

                    bout.write(NotificationAttributeIDDate)
                    bout.write(10)
                    bout.write(0)
                    val data = bout.toByteArray()
                    cha.value = data
                    IOSNotification.log(
                        "request ANCS(CP) the data of Notification. ？= "
                                + gatt!!.writeCharacteristic(cha)
                    )
                    currData!!.curStep = 1
                    currData!!.bout = ByteArrayOutputStream()
                    currData!!.timeExpired = System.currentTimeMillis() + TIMEOUT
                    //					mHandler.removeMessages(MSG_CHECK_TIME);
//					mHandler.sendEmptyMessageDelayed(MSG_CHECK_TIME, TIMEOUT);
                    return
                } else {
                    IOSNotification.logw("ANCS has No Control Point !")
                    // has no control!// just vibrate ...
                    currData!!.bout = null
                    currData!!.curStep = 1
                }
            } while (false)
        } else if (currData!!.curStep == 1) {
            // check if finished!
            currData!!.finish();
            return
        } else {
            return
        }
        notificationHandler.sendEmptyMessage(MSG_DO_NOTIFICATION) // do next step
    }

    fun onDSNotification(data: ByteArray?) {
        if (currData == null) {
            IOSNotification.logw("got ds notify without cur data")
            return
        }
        try {
            notificationHandler.removeMessages(MSG_FINISH)
            currData!!.bout!!.write(data)
            notificationHandler.sendEmptyMessageDelayed(MSG_FINISH, FINISH_DELAY.toLong())
        } catch (e: IOException) {
            IOSNotification.loge(e.toString())
        }
    }

    fun onWrite(characteristic: BluetoothGattCharacteristic?, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            IOSNotification.log("write err: $status")
            notificationHandler.sendEmptyMessage(MSG_ERR)
        } else {
            IOSNotification.log("write OK")
            notificationHandler.sendEmptyMessage(MSG_DO_NOTIFICATION)
        }
    }

    fun onNotification(data: ByteArray?) {
        if (data == null || data.size != 8) {
            IOSNotification.loge("bad ANCS notification data")
            return
        }
        logD(data)
        val msg = notificationHandler.obtainMessage(MSG_ADD_NOTIFICATION)
        msg.obj = data
        msg.sendToTarget()
    }

    fun reset() {
        notificationHandler.sendEmptyMessage(MSG_RESET)
    }

    fun logD(d: ByteArray) {
        val sb = StringBuffer()
        val len = d.size
        for (i in 0 until len) {
            sb.append(d[i].toString() + ", ")
        }
        IOSNotification.log("log Data size[$len] : $sb")
    }

    companion object {
        // ANCS constants
        const val NotificationAttributeIDAppIdentifier = 0
        const val NotificationAttributeIDTitle =
            1 //, (Needs to be followed by a 2-bytes max length parameter)
        const val NotificationAttributeIDSubtitle =
            2 //, (Needs to be followed by a 2-bytes max length parameter)
        const val NotificationAttributeIDMessage =
            3 //, (Needs to be followed by a 2-bytes max length parameter)
        const val NotificationAttributeIDMessageSize = 4 //,
        const val NotificationAttributeIDDate = 5 //,
        const val AppAttributeIDDisplayName = 0
        const val CommandIDGetNotificationAttributes = 0
        const val CommandIDGetAppAttributes = 1
        const val EventFlagSilent = 1 shl 0
        const val EventFlagImportant = 1 shl 1
        const val EventIDNotificationAdded = 0
        const val EventIDNotificationModified = 1
        const val EventIDNotificationRemoved = 2
        const val CategoryIDOther = 0
        const val CategoryIDIncomingCall = 1
        const val CategoryIDMissedCall = 2
        const val CategoryIDVoicemail = 3
        const val CategoryIDSocial = 4
        const val CategoryIDSchedule = 5
        const val CategoryIDEmail = 6
        const val CategoryIDNews = 7
        const val CategoryIDHealthAndFitness = 8
        const val CategoryIDBusinessAndFinance = 9
        const val CategoryIDLocation = 10
        const val CategoryIDEntertainment = 11

        // !ANCS constants
        private const val MSG_ADD_NOTIFICATION = 100
        private const val MSG_DO_NOTIFICATION = 101
        private const val MSG_RESET = 102
        private const val MSG_ERR = 103
        private const val MSG_CHECK_TIME = 104
        private const val MSG_FINISH = 105
        private const val FINISH_DELAY = 700
        private const val TIMEOUT = 15 * 1000
        private var sInst: ANCSParser? = null

        fun getDefault(c: Context): ANCSParser? {
            if (sInst == null) {
                sInst = ANCSParser(c)
            }
            return sInst
        }

        fun get(): ANCSParser? {
            return sInst
        }
    }

    init {
        notificationHandler = object : Handler(mContext.mainLooper) {
            override fun handleMessage(msg: Message) {
                val what = msg.what
                if (MSG_CHECK_TIME == what) {
                    if (currData == null) {
                        return
                    }
                    if (System.currentTimeMillis() >= currData!!.timeExpired) {
                        IOSNotification.loge("msg timeout!")
                    }
                } else if (MSG_ADD_NOTIFICATION == what) {
                    pendingNotifications.add(ANCSData(msg.obj as ByteArray))
                    this.sendEmptyMessage(MSG_DO_NOTIFICATION)
                } else if (MSG_DO_NOTIFICATION == what) {
                    processNotificationList()
                } else if (MSG_RESET == what) {
                    this.removeMessages(MSG_ADD_NOTIFICATION)
                    this.removeMessages(MSG_DO_NOTIFICATION)
                    this.removeMessages(MSG_RESET)
                    this.removeMessages(MSG_ERR)
                    pendingNotifications.clear()
                    currData = null
                    IOSNotification.log("ANCSHandler reset")
                } else if (MSG_ERR == what) {
                    IOSNotification.log("error, skip curr data")
                    currData!!.clear()
                    currData = null
                    this.sendEmptyMessage(MSG_DO_NOTIFICATION)
                } else if (MSG_FINISH == what) {
                    IOSNotification.log("msg data.finish()")
                    if (null != currData) currData!!.finish()
                }
            }
        }
    }
}