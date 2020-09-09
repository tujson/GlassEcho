package dev.synople.glassecho.common

import android.os.Parcel
import android.os.Parcelable

object ParcelableUtil {
    fun marshall(parceable: Parcelable): ByteArray {
        val parcel: Parcel = Parcel.obtain()
        parceable.writeToParcel(parcel, 0)
        val bytes: ByteArray = parcel.marshall()
        parcel.recycle()
        return bytes
    }

    fun unmarshall(bytes: ByteArray): Parcel {
        val parcel: Parcel = Parcel.obtain()
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0) // This is extremely important!
        return parcel
    }

    fun <T> unmarshall(bytes: ByteArray, creator: Parcelable.Creator<T>): T {
        val parcel: Parcel = unmarshall(bytes)
        val result: T = creator.createFromParcel(parcel)
        parcel.recycle()
        return result
    }
}
