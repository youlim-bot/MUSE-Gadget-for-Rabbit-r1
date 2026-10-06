package dev.cameronpak.muser1

import dev.cameronpak.muser1.transport.PhotoPayload
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class PhotoPayloadTest {
    private fun jpeg(size: Int) = ByteArray(size).apply { this[0] = 0xff.toByte(); this[1] = 0xd8.toByte(); this[size-2] = 0xff.toByte(); this[size-1] = 0xd9.toByte() }
    @Test fun boundedPhotoPreservesBytesAndMultilingualQuestion() {
        val image = jpeg(180000)
        val question = "사진について 설명해 줘."
        val encoded = PhotoPayload.encode(image, question)
        assertTrue(encoded.size <= 256 * 1024)
        val body = JSONObject(encoded.toString(Charsets.UTF_8))
        assertEquals(question, body.getString("message"))
        val item = body.getJSONArray("items").getJSONObject(0)
        assertEquals("image/jpeg", item.getString("mime_type"))
        assertArrayEquals(image, Base64.getDecoder().decode(item.getString("data_base64")))
    }
    @Test fun rejectsOversizeAndInvalidPhotosAndEmptyQuestions() {
        for ((photo, question) in listOf(jpeg(180001) to "test", ByteArray(4) to "test", jpeg(4) to " ", jpeg(4) to "x".repeat(1001))) {
            assertThrows(IllegalArgumentException::class.java) { PhotoPayload.encode(photo, question) }
        }
    }
}
