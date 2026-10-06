package dev.cameronpak.muser1

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

internal data class TaskItem(val text: String, val done: Boolean = false)
internal data class SavedItem(val id: String, val kind: String, val title: String, val text: String, val time: Long, val tasks: List<TaskItem> = emptyList())
internal fun favoriteId(question: String, answer: String): String = MessageDigest.getInstance("SHA-256")
    .digest((question + "\u0000" + answer).toByteArray()).joinToString("") { "%02x".format(it) }
internal fun matchesQuery(title: String, text: String, query: String): Boolean =
    query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.all { (title + "\n" + text).contains(it, ignoreCase = true) }
internal fun encodeLibrary(items: List<SavedItem>): String = JSONArray().apply {
    items.forEach { put(JSONObject().put("id", it.id).put("kind", it.kind).put("title", it.title).put("text", it.text).put("time", it.time).put("tasks", JSONArray().apply { it.tasks.forEach { task -> put(JSONObject().put("text",task.text).put("done",task.done)) } })) }
}.toString()
internal fun decodeLibrary(text: String): List<SavedItem> = JSONArray(text).let { rows ->
    (0 until rows.length()).map { rows.getJSONObject(it).let { row -> SavedItem(row.getString("id"), row.getString("kind"), row.getString("title"), row.getString("text"), row.getLong("time"), row.optJSONArray("tasks")?.let { tasks -> (0 until tasks.length()).map { index -> tasks.getJSONObject(index).let { TaskItem(it.getString("text"),it.optBoolean("done",false)) } } } ?: emptyList()) } }
}
internal class LibraryStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "saved-library.json"))
    suspend fun load(): List<SavedItem> = withContext(Dispatchers.IO) {
        if (file.baseFile.exists()) decodeLibrary(file.readFully().toString(Charsets.UTF_8)) else emptyList()
    }
    suspend fun save(items: List<SavedItem>) = withContext(Dispatchers.IO) {
        val bytes = encodeLibrary(items).toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
}
