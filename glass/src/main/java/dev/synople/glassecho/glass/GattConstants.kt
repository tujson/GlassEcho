package dev.synople.glassecho.glass

import java.util.UUID

object GattConstants {
    val DESCRIPTOR_UUID: UUID = UUID
        .fromString("00002902-0000-1000-8000-00805f9b34fb")

    fun getName(uuid: UUID) = when (uuid) {
        Apple.NOTIFICATION_SOURCE -> {
            "ANCS Notification Source"
        }
        Apple.CONTROL_POINT -> {
            "ANCS Control Point"
        }
        Apple.DATA_SOURCE -> {
            "ANCS Data Source"
        }
        DESCRIPTOR_UUID -> {
            "UpdateDescriptor"
        }
        else -> {
            "Unknown"
        }
    }

    object Apple {
        val ANCS: UUID = UUID
            .fromString("7905F431-B5CE-4E99-A40F-4B1E122D00D0")

        val NOTIFICATION_SOURCE: UUID = UUID
            .fromString("9FBF120D-6301-42D9-8C58-25E699A21DBD")

        val CONTROL_POINT: UUID = UUID
            .fromString("69D1D8F3-45E1-49A8-9821-9BBDFDAAD9D9")

        val DATA_SOURCE: UUID = UUID
            .fromString("22EAC6E9-24D6-4BB5-BE44-B36ACE7C7BFB")
    }
}