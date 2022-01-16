package dev.synople.glassecho.common.models

import android.content.Intent
import java.io.Serializable

data class EchoHTTPRequest (
    var intent: Intent
        ) : Serializable {

    override fun hashCode(): Int {
        return intent.hashCode()
    }

}