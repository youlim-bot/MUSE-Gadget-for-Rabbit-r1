package dev.cameronpak.muser1.transport

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

internal object PhotoPayload {
    fun encode(jpeg: ByteArray, question: String): ByteArray {
        require(jpeg.size in 4..180000 && question.isNotBlank() && question.length <= 1000)
        require(jpeg[0] == 0xff.toByte() && jpeg[1] == 0xd8.toByte() && jpeg[jpeg.lastIndex - 1] == 0xff.toByte() && jpeg.last() == 0xd9.toByte())
        val item = JSONObject().put("type", "file").put("mime_type", "image/jpeg")
            .put("filename", "camera.jpg").put("data_base64", Base64.getEncoder().encodeToString(jpeg))
        val body = JSONObject().put("message", question).put("output_modality", "text")
            .put("items", JSONArray().put(item)).toString().toByteArray(Charsets.UTF_8)
        require(body.size <= 256 * 1024)
        return body
    }
}
