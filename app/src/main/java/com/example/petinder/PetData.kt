package com.example.petinder

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

data class Pet(
    val id: String, val name: String, val species: String, val breed: String,
    val age: Int, val sex: String, val size: String, val imageKey: String,
    val latitude: Double, val longitude: Double, val city: String, val shelter: String,
    val description: String, val traits: List<String>, val fee: Int
) {
    fun distance(lat: Double, lon: Double): Double {
        val dLat = Math.toRadians(latitude - lat)
        val dLon = Math.toRadians(longitude - lon)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(latitude)) * sin(dLon / 2).pow(2)
        return 6371.0088 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}

val demoPets = listOf(
    Pet("buddy", "Buddy", "Dog", "Golden Retriever", 2, "Male", "Large", "buddy", 41.891, -87.628, "Chicago, IL", "PAWS Chicago", "A sunny soul with a very waggy tail. Buddy loves long walks, tennis balls, and being right by your side.", listOf("Playful", "Gentle", "Good with kids"), 250),
    Pet("luna", "Luna", "Cat", "Domestic Shorthair", 1, "Female", "Small", "luna", 41.921, -87.651, "Chicago, IL", "Tree House Humane Society", "Your next windowsill companion. Luna is curious, affectionate, and always ready for a quiet afternoon together.", listOf("Curious", "Affectionate", "Indoor"), 125),
    Pet("milo", "Milo", "Dog", "Labrador Mix", 3, "Male", "Medium", "milo", 41.873, -87.659, "Chicago, IL", "One Tail at a Time", "An adventure buddy with a heart of gold. Milo is happiest exploring a trail and winding down with his favorite people.", listOf("Active", "Friendly", "House trained"), 200),
    Pet("willow", "Willow", "Rabbit", "Holland Lop", 1, "Female", "Small", "willow", 41.951, -87.678, "Chicago, IL", "Red Door Animal Shelter", "Small paws, big personality. Willow enjoys fresh greens, cozy hideaways, and gentle company.", listOf("Gentle", "Quiet", "Curious"), 80)
)

data class Adoption(val id: Int, val petName: String, val status: String)

class PetApi(private val baseUrl: String, private val token: String = "") {
    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 4000
            connection.readTimeout = 6000
            connection.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(response).optString("detail") }.getOrDefault("")
                throw IllegalStateException(if (detail.startsWith("[") || detail.isBlank()) "Please check your details and try again ($code)." else detail)
            }
            response
        } finally { connection.disconnect() }
    }
}

fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
fun JSONObject.toPet() = Pet(getString("id"), getString("name"), getString("species"), getString("breed"), getInt("age"), getString("sex"), getString("size"), getString("image_key"), getDouble("latitude"), getDouble("longitude"), getString("city"), getString("shelter"), getString("description"), getJSONArray("traits").let { a -> (0 until a.length()).map { a.getString(it) } }, getInt("fee"))

@Stable
class PetStore(context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("petinder", Context.MODE_PRIVATE)
    var baseUrl by mutableStateOf(prefs.getString("baseUrl", "http://10.0.2.2:8000")!!)
        private set
    private var token = prefs.getString("token", "").orEmpty()
    var pets by mutableStateOf(demoPets)
        private set
    var decisions by mutableStateOf(readDecisions())
        private set
    var adoptions by mutableStateOf(emptyList<Adoption>())
        private set
    var online by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var submitting by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)
    var latitude by mutableDoubleStateOf(prefs.getFloat("lat", 41.8781f).toDouble())
        private set
    var longitude by mutableDoubleStateOf(prefs.getFloat("lon", -87.6298f).toDouble())
        private set
    var gpsLocation by mutableStateOf(prefs.getBoolean("gps", false))
        private set
    var species by mutableStateOf("All pets")
    var radius by mutableFloatStateOf(50f)
    var lastDecision by mutableStateOf<Pair<String, String?>?>(null)
        private set
    private fun readDecisions(): Map<String, String> = runCatching {
        val data = JSONObject(prefs.getString("demoDecisions", "{}")!!)
        data.keys().asSequence().associateWith { data.getString(it) }
    }.getOrDefault(emptyMap())
    val filtered: List<Pet> get() = pets.filter { (species == "All pets" || it.species == species) && it.distance(latitude, longitude) <= radius }.sortedBy { it.distance(latitude, longitude) }
    val deck: List<Pet> get() = filtered.filter { !decisions.containsKey(it.id) }
    val favorites: List<Pet> get() = pets.filter { decisions[it.id] == "like" }
    private fun api() = PetApi(baseUrl, token)

    fun connect(url: String = baseUrl) {
        if (busy || submitting) return
        val normalized = url.trim().trimEnd('/')
        if (!normalized.matches(Regex("https?://[^\\s]+"))) { notice = "Enter a valid http:// or https:// server address."; return }
        val changed = normalized != baseUrl
        baseUrl = normalized
        if (changed) { token = ""; prefs.edit().remove("token").apply() }
        prefs.edit().putString("baseUrl", baseUrl).apply()
        busy = true
        scope.launch {
            try {
                api().request("/health")
                if (token.isNotEmpty()) {
                    try { api().request("/me/decisions") } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        if (e.message == "Invalid session") token = "" else throw e
                    }
                }
                if (token.isEmpty()) {
                    token = JSONObject(PetApi(baseUrl).request("/sessions", "POST")).getString("token")
                    prefs.edit().putString("token", token).apply()
                }
                val loadedPets = JSONArray(api().request("/pets")).objects().map { it.toPet() }
                val loadedDecisions = JSONArray(api().request("/me/decisions")).objects().associate { it.getString("pet_id") to it.getString("decision") }
                val loadedApplications = loadApplications()
                pets = loadedPets; decisions = loadedDecisions; adoptions = loadedApplications
                online = true; lastDecision = null
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                online = false; pets = demoPets; decisions = readDecisions(); adoptions = emptyList()
                notice = "Backend unavailable. Explore sample pets, or update the server in Profile."
            } finally { busy = false }
        }
    }

    fun decide(pet: Pet, decision: String) {
        if (busy || submitting) return
        val previous = decisions[pet.id]
        if (!online) {
            lastDecision = pet.id to previous
            decisions = decisions + (pet.id to decision)
            prefs.edit().putString("demoDecisions", JSONObject(decisions).toString()).apply()
            if (decision == "like") notice = "${pet.name} added to your demo favorites."
            return
        }
        busy = true
        scope.launch {
            try {
                api().request("/me/decisions/${pet.id}", "PUT", JSONObject().put("decision", decision))
                lastDecision = pet.id to previous
                decisions = decisions + (pet.id to decision)
                if (decision == "like") notice = "${pet.name} saved. Your new beginning is a little closer."
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                notice = "Could not save your choice. Please try again."
            } finally { busy = false }
        }
    }

    fun undo() {
        val (id, previous) = lastDecision ?: return
        if (busy) return
        fun update() {
            decisions = if (previous == null) decisions - id else decisions + (id to previous)
            lastDecision = null
            if (!online) prefs.edit().putString("demoDecisions", JSONObject(decisions).toString()).apply()
        }
        if (!online) { update(); return }
        busy = true
        scope.launch {
            try {
                if (previous == null) api().request("/me/decisions/$id", "DELETE")
                else api().request("/me/decisions/$id", "PUT", JSONObject().put("decision", previous))
                update()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                notice = "Could not undo. Try again."
            } finally { busy = false }
        }
    }

    private suspend fun loadApplications() = JSONArray(api().request("/me/applications")).objects().map { Adoption(it.getInt("id"), it.getString("pet_name"), it.getString("status")) }
    fun apply(pet: Pet, name: String, email: String, message: String, onSuccess: () -> Unit) {
        if (!online) { notice = "Connect to FastAPI in Profile to submit an application."; return }
        if (submitting || busy) return
        submitting = true
        scope.launch {
            try {
                val result = JSONObject(api().request("/me/applications", "POST", JSONObject().put("pet_id", pet.id).put("name", name.trim()).put("email", email.trim()).put("message", message.trim())))
                adoptions = listOf(Adoption(result.getInt("id"), pet.name, result.getString("status"))) + adoptions
                notice = "Application #${result.getInt("id")} saved for ${pet.name}."
                onSuccess()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                notice = e.message?.takeIf { it.contains("already") || it.contains("available") } ?: "Submission could not be confirmed. Reconnect to check your applications before retrying."
            } finally { submitting = false }
        }
    }

    fun setLocation(lat: Double, lon: Double) {
        latitude = lat; longitude = lon; gpsLocation = true
        prefs.edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).putBoolean("gps", true).apply()
        notice = "Location updated. Nearby pets are sorted by distance."
    }
    fun useChicago() {
        latitude = 41.8781; longitude = -87.6298; gpsLocation = false
        prefs.edit().remove("lat").remove("lon").putBoolean("gps", false).apply()
    }
}
