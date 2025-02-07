package eu.kanade.tachiyomi.data.library

import android.content.Context
import com.github.salomonbrys.kotson.nullInt
import com.github.salomonbrys.kotson.nullLong
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import java.io.File
import java.util.Scanner

class CustomMangaManager(val context: Context) {

    private val editJson = File(context.getExternalFilesDir(null), "edits.json")

    private var customMangaMap = mutableMapOf<Long, Manga>()

    init {
        fetchCustomData()
    }

    fun getManga(manga: Manga): Manga? = customMangaMap[manga.id]

    private fun fetchCustomData() {
        if (!editJson.exists() || !editJson.isFile) return

        val json = try {
            Gson().fromJson(
                Scanner(editJson).useDelimiter("\\Z").next(),
                JsonObject::class.java
            )
        } catch (e: Exception) {
            null
        } ?: return

        val mangasJson = json.get("mangas").asJsonArray ?: return
        customMangaMap = mangasJson.mapNotNull { element ->
            val mangaObject = element.asJsonObject ?: return@mapNotNull null
            val id = mangaObject["id"]?.nullLong ?: return@mapNotNull null
            val manga = MangaImpl().apply {
                this.id = id
                title = mangaObject["title"]?.nullString ?: ""
                author = mangaObject["author"]?.nullString
                artist = mangaObject["artist"]?.nullString
                description = mangaObject["description"]?.nullString
                genre = mangaObject["genre"]?.asJsonArray?.mapNotNull { it.nullString }
                    ?.joinToString(", ")
                status = mangaObject["status"]?.nullInt ?: -1
            }
            id to manga
        }.toMap().toMutableMap()
    }

    fun saveMangaInfo(manga: MangaJson) {
        if (manga.title == null && manga.author == null && manga.artist == null &&
            manga.description == null && manga.genre == null &&
            manga.status ?: -1 == -1
        ) {
            customMangaMap.remove(manga.id)
        } else {
            customMangaMap[manga.id] = MangaImpl().apply {
                id = manga.id
                title = manga.title ?: ""
                author = manga.author
                artist = manga.artist
                description = manga.description
                genre = manga.genre?.joinToString(", ")
                status = manga.status ?: -1
            }
        }
        saveCustomInfo()
    }

    private fun saveCustomInfo() {
        val jsonElements = customMangaMap.values.map { it.toJson() }
        if (jsonElements.isNotEmpty()) {
            val gson = GsonBuilder().create()
            val root = JsonObject()
            val mangaEntries = gson.toJsonTree(jsonElements)

            root["mangas"] = mangaEntries
            editJson.delete()
            editJson.writeText(gson.toJson(root))
        }
    }

    fun Manga.toJson(): MangaJson {
        return MangaJson(
            id!!,
            title,
            author,
            artist,
            description,
            genre?.split(", ")?.toTypedArray(),
            status.takeUnless { it == -1 }
        )
    }

    data class MangaJson(
        var id: Long,
        val title: String? = null,
        val author: String? = null,
        val artist: String? = null,
        val description: String? = null,
        val genre: Array<String>? = null,
        val status: Int? = null
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as MangaJson
            if (id != other.id) return false
            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }
}
