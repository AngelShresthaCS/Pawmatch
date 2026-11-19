package com.example.petinder

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import java.util.Locale
import kotlin.math.abs

@Composable
fun DiscoverScreen(store: PetStore, onFilters: () -> Unit, onPet: (Pet) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val contentHeight = maxOf(maxHeight, 590.dp * LocalDensity.current.fontScale)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    Column(Modifier.fillMaxWidth().height(contentHeight).padding(horizontal = 24.dp)) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your person has paws.", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = Ink, letterSpacing = (-.8).sp)
                Text("Find a friend. Change a life.", Modifier.padding(top = 5.dp), fontSize = 13.sp, color = Muted)
            }
            Box(Modifier.size(43.dp).border(1.dp, Color(0xFFE6E3DA), RoundedCornerShape(14.dp)).clickable(onClick = onFilters).semantics { contentDescription = "Search filters" }, contentAlignment = Alignment.Center) { AppIcon("filter", Ink, 21.dp) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 19.dp, bottom = 15.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All pets" to "paw", "Dog" to "dog", "Cat" to "cat", "Rabbit" to "rabbit").forEach { (species, icon) ->
                val active = store.species == species
                Row(Modifier.weight(1f).height(37.dp).background(if (active) Ink else Pale, RoundedCornerShape(30.dp)).clickable { store.species = species }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    AppIcon(icon, if (active) Color.White else Muted, 15.dp)
                    Text(if (species == "All pets") " All" else if (species == "Rabbit") " Other" else " ${species}s", fontSize = 11.sp, color = if (active) Color.White else Ink, fontWeight = FontWeight.Medium)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(Sage, CircleShape)); Text("  ${store.filtered.size} little hearts near you", fontSize = 11.sp, color = Muted)
            Spacer(Modifier.weight(1f)); Text("Within ${store.radius.toInt()} km", fontSize = 10.sp, color = Sage, modifier = Modifier.clickable(onClick = onFilters))
        }
        val pet = store.deck.firstOrNull()
        Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 6.dp), contentAlignment = Alignment.Center) {
            if (pet != null) {
                Box(Modifier.fillMaxSize().padding(horizontal = 9.dp).offset(y = 7.dp).rotate(2f).background(Color(0xFFE9E4D9), RoundedCornerShape(26.dp)))
                var drag by remember(pet.id) { mutableFloatStateOf(0f) }
                val offset by animateFloatAsState(drag, label = "swipe")
                Box(Modifier.fillMaxSize().offset { IntOffset(offset.toInt(), 0) }.rotate(offset / 40f).clip(RoundedCornerShape(25.dp)).background(Color(0xFFD8E3D6)).pointerInput(pet.id, store.busy) {
                    detectHorizontalDragGestures(onDragEnd = {
                        if (drag > 100f) store.decide(pet, "like") else if (drag < -100f) store.decide(pet, "pass")
                        drag = 0f
                    }, onDragCancel = { drag = 0f }) { change, amount -> change.consume(); drag += amount }
                }.clickable { onPet(pet) }) {
                    PetArtwork(pet, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xE622372C)))))
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Tag("LOOKING FOR A HOME", Ink, Cream.copy(alpha = .94f), 8)
                        Box(Modifier.size(32.dp).background(Color.White.copy(alpha = .25f), CircleShape), contentAlignment = Alignment.Center) { AppIcon("heart", Color.White, 18.dp) }
                    }
                    if (abs(drag) > 45) Text(if (drag > 0) "LOVE" else "PASS", Modifier.align(Alignment.Center).rotate(-12f).border(3.dp, Color.White, RoundedCornerShape(10.dp)).padding(14.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Column(Modifier.align(Alignment.BottomStart).padding(21.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("${pet.name}, ${pet.age}", fontFamily = FontFamily.Serif, fontSize = 35.sp, color = Color.White); Spacer(Modifier.width(9.dp)); AppIcon(if (pet.sex == "Male") "male" else "female", Color.White.copy(alpha = .8f), 19.dp) }
                        Text(pet.breed, fontSize = 13.sp, color = Color.White.copy(alpha = .85f))
                        Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) { AppIcon("pin", Color.White, 13.dp); Text(" ${distanceLabel(pet, store)} away", fontSize = 11.sp, color = Color.White); Text("  ·  ", color = Color.White.copy(alpha = .6f)); Text(pet.traits.first(), fontSize = 11.sp, color = Color.White) }
                    }
                    Box(Modifier.align(Alignment.BottomEnd).padding(20.dp).size(30.dp).border(1.dp, Color.White.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) { Text("i", color = Color.White, fontFamily = FontFamily.Serif, fontSize = 19.sp) }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(25.dp)) {
                    AppIcon("paw", Coral, 55.dp)
                    Text("Love takes a little exploring.", fontFamily = FontFamily.Serif, fontSize = 26.sp, textAlign = TextAlign.Center, color = Ink)
                    Text("You've met everyone in this search. Adjust your filters, undo a swipe, or revisit your saved pets.", textAlign = TextAlign.Center, color = Muted, fontSize = 13.sp)
                    TextButton(onClick = onFilters) { Text("Explore a little further") }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 17.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundAction("undo", "Undo last swipe", Color(0xFFA38B61), 43.dp, store.lastDecision != null && !store.busy) { store.undo() }
            RoundAction("close", "Pass on this pet", Muted, 57.dp, pet != null && !store.busy) { pet?.let { store.decide(it, "pass") } }
            RoundAction("heart", "Save this pet", Color.White, 67.dp, pet != null && !store.busy, Coral) { pet?.let { store.decide(it, "like") } }
            RoundAction("info", "View pet profile", Sage, 43.dp, pet != null) { pet?.let(onPet) }
        }
        Text(if (store.busy) "Connecting with a little love…" else "Swipe right for your next best friend", Modifier.fillMaxWidth(), fontSize = 10.sp, textAlign = TextAlign.Center, color = Muted)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { AppIcon("heart", Coral, 11.dp); Text(if (store.online) "  Demo catalog · Connected to FastAPI" else "  Demo mode · Connect your backend in Profile", fontSize = 9.sp, color = Muted) }
    }
    }
    }
}

@Composable
fun NearbyScreen(store: PetStore, locating: Boolean, locate: () -> Unit, onPet: (Pet) -> Unit, filters: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp, 8.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("A friend around the corner.", "Small distances. Big beginnings.") }
        item {
            Column(Modifier.fillMaxWidth().background(Color(0xFFEAF0E7), RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { AppIcon("pin", Sage, 25.dp); Spacer(Modifier.width(10.dp)); Text(if (store.gpsLocation) "Using your GPS location" else "Exploring Chicago, IL", color = Ink, fontWeight = FontWeight.SemiBold) }
                Text("Find adoptable pets within ${store.radius.toInt()} km. Shelter directions open in your maps app.", fontSize = 13.sp, color = Sage, lineHeight = 20.sp)
                Button(onClick = locate, enabled = !locating, colors = ButtonDefaults.buttonColors(containerColor = Sage), shape = RoundedCornerShape(12.dp)) { Text(if (locating) "Finding your location…" else "Use my current location") }
                TextButton(onClick = filters, contentPadding = PaddingValues(0.dp)) { Text("Adjust search radius", color = Sage) }
            }
        }
        item { Text("${store.filtered.size} pets nearby", fontSize = 19.sp, fontFamily = FontFamily.Serif, color = Ink) }
        if (store.filtered.isEmpty()) item { EmptyNote("No pets within this radius", "Try a wider search or explore the Chicago sample location from Profile.") }
        items(store.filtered, key = { it.id }) { pet ->
            Column(Modifier.fillMaxWidth().background(Pale, RoundedCornerShape(19.dp)).padding(12.dp)) {
                PetListRow(pet, store, { onPet(pet) })
                TextButton(onClick = {
                    val uri = Uri.parse("geo:${pet.latitude},${pet.longitude}?q=${pet.latitude},${pet.longitude}(${Uri.encode(pet.shelter)})")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure { store.notice = "No maps app is installed on this device." }
                }) { AppIcon("pin", Coral, 14.dp); Text("  Directions to sample shelter", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun SavedScreen(store: PetStore, onPet: (Pet) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp, 8.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("Little hearts you love.", "Your favorites, and your next chapter.") }
        item { Text("SAVED PETS · ${store.favorites.size}", fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, color = Sage) }
        if (store.favorites.isEmpty()) item { EmptyNote("A place for your favorites", "Tap a heart or swipe right to save a pet. Your future friend could be one swipe away.") }
        items(store.favorites, key = { it.id }) { pet ->
            Row(Modifier.fillMaxWidth().background(Pale, RoundedCornerShape(19.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { PetListRow(pet, store, { onPet(pet) }) }
                IconButton(onClick = { store.decide(pet, "pass") }, enabled = !store.busy) { AppIcon("close", Muted, 18.dp) }
            }
        }
        item { Spacer(Modifier.height(10.dp)); Text("YOUR APPLICATIONS", fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, color = Sage) }
        if (store.adoptions.isEmpty()) item { EmptyNote("New beginnings start here", if (store.online) "Open a pet's profile to send an adoption application." else "Connect your backend in Profile to submit and track adoption applications.") }
        items(store.adoptions, key = { it.id }) { adoption ->
            Row(Modifier.fillMaxWidth().background(Color(0xFFEAF0E7), RoundedCornerShape(18.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon("heart", Sage, 25.dp)
                Column(Modifier.weight(1f).padding(start = 15.dp)) { Text(adoption.petName, fontWeight = FontWeight.SemiBold, color = Ink); Text("Application #${adoption.id}", fontSize = 12.sp, color = Muted) }
                Tag(adoption.status.replaceFirstChar { it.uppercase() }, Sage, Color.White)
            }
        }
    }
}

@Composable
fun ProfileScreen(store: PetStore, locating: Boolean, locate: () -> Unit, connect: (String) -> Unit) {
    var url by remember(store.baseUrl) { mutableStateOf(store.baseUrl) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp, 8.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        PageTitle("Good things start with you.", "Make room for a little more love.")
        Row(Modifier.fillMaxWidth().background(Pale, RoundedCornerShape(22.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(Coral.copy(alpha = .13f), CircleShape), contentAlignment = Alignment.Center) { AppIcon("person", Coral, 29.dp) }
            Column(Modifier.padding(start = 16.dp)) { Text("Future best friend", fontFamily = FontFamily.Serif, fontSize = 22.sp, color = Ink); Text("${store.favorites.size} saved pets · ${store.adoptions.size} applications", fontSize = 12.sp, color = Muted) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Your neighborhood", fontFamily = FontFamily.Serif, fontSize = 23.sp, color = Ink)
            Text(if (store.gpsLocation) "GPS · ${String.format(Locale.US, "%.3f, %.3f", store.latitude, store.longitude)}" else "Chicago, IL · Sample location", fontSize = 13.sp, color = Muted)
            OutlinedButton(onClick = locate, enabled = !locating, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) { AppIcon("pin", Coral, 17.dp); Text(if (locating) "  Finding your location…" else "  Use GPS location") }
            if (store.gpsLocation) TextButton(onClick = { store.useChicago() }) { Text("Explore Chicago sample pets") }
        }
        HorizontalDivider(color = Color(0xFFE8E4DC))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Your connection", fontFamily = FontFamily.Serif, fontSize = 23.sp, color = Ink); Spacer(Modifier.weight(1f)); Tag(if (store.online) "Connected" else "Demo mode", if (store.online) Sage else Coral, if (store.online) Color(0xFFEAF0E7) else Color(0xFFFBE8DF)) }
            Text("Connect FastAPI to keep your favorites and adoption applications in your private device session.", fontSize = 13.sp, color = Muted, lineHeight = 21.sp)
            OutlinedTextField(url, { url = it }, label = { Text("Backend URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            Button(onClick = { connect(url) }, enabled = !store.busy && !store.submitting, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(13.dp)) { Text(if (store.busy) "Connecting…" else "Connect & refresh") }
            Text("Android emulator: http://10.0.2.2:8000\nPhysical device: use your computer's local IP address.", color = Muted, fontSize = 11.sp, lineHeight = 19.sp)
        }
        Text("Made for second chances.", fontFamily = FontFamily.Serif, fontSize = 19.sp, color = Sage, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("Petinder is a working prototype with sample pet and shelter listings. Applications stay in your backend; shelter notifications and staff review are not yet integrated.", fontSize = 11.sp, color = Muted, lineHeight = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun PetDetails(pet: Pet, store: PetStore, adopt: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Meet ${pet.name}", fontFamily = FontFamily.Serif, fontSize = 32.sp, color = Ink); Text("${pet.breed} · ${pet.city}", color = Muted, fontSize = 13.sp) }
            RoundAction("heart", "Save ${pet.name}", Coral, 48.dp, !store.busy) { store.decide(pet, "like") }
        }
        PetArtwork(pet, Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(22.dp)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("AGE" to "${pet.age} years", "SEX" to pet.sex, "SIZE" to pet.size, "ADOPTION" to "$${pet.fee}").forEach { (label, value) ->
                Column { Text(label, fontSize = 9.sp, color = Muted, letterSpacing = 1.sp); Text(value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        Text("A little about me", fontFamily = FontFamily.Serif, fontSize = 23.sp, color = Ink)
        Text(pet.description, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { pet.traits.forEach { Tag(it, Sage, Color(0xFFEAF0E7)) } }
        Text("Listed by ${pet.shelter}", fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Medium)
        Text("Sample listing · Applications are stored in your connected backend. Shelter notifications are not yet integrated.", fontSize = 11.sp, color = Muted, lineHeight = 17.sp)
        Button(onClick = adopt, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { AppIcon("heart", Color.White, 19.dp); Spacer(Modifier.width(8.dp)); Text("I'd love to adopt ${pet.name}") }
    }
}

@Composable
fun AdoptionForm(pet: Pet, store: PetStore, close: () -> Unit) {
    var name by rememberSaveable(pet.id) { mutableStateOf("") }
    var email by rememberSaveable(pet.id) { mutableStateOf("") }
    var message by rememberSaveable(pet.id) { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val valid = name.trim().length >= 2 && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && message.trim().length >= 20 && consent
    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("A home for ${pet.name}.", fontFamily = FontFamily.Serif, fontSize = 30.sp, color = Ink)
        Text("Tell us a little about the life you could share.", fontSize = 13.sp, color = Muted)
        OutlinedTextField(name, { if (it.length <= 100) name = it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = attempted && name.trim().length < 2, shape = RoundedCornerShape(13.dp))
        OutlinedTextField(email, { if (it.length <= 254) email = it }, label = { Text("Email address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, isError = attempted && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches(), shape = RoundedCornerShape(13.dp))
        OutlinedTextField(message, { if (it.length <= 2000) message = it }, label = { Text("About your home & experience") }, supportingText = { Text("At least 20 characters · ${message.length}/2000") }, minLines = 3, modifier = Modifier.fillMaxWidth(), isError = attempted && message.trim().length < 20, shape = RoundedCornerShape(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(consent, { consent = it }); Text("I understand this is a sample listing. My application is stored in this prototype's backend.", fontSize = 12.sp, color = Muted, lineHeight = 18.sp) }
        if (attempted && !valid) Text("Add your name, a valid email, at least 20 characters about your home, and acknowledge the sample listing.", fontSize = 12.sp, color = Coral)
        if (!store.online) Text("Connect the backend from Profile before submitting.", color = Coral, fontSize = 12.sp)
        Button(onClick = { attempted = true; if (valid) store.apply(pet, name, email, message, close) }, enabled = store.online && !store.submitting && !store.busy, modifier = Modifier.fillMaxWidth().height(51.dp), shape = RoundedCornerShape(14.dp)) { Text(if (store.submitting) "Sending application…" else "Send adoption application") }
    }
}

@Composable
fun PageTitle(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontFamily = FontFamily.Serif, fontSize = 29.sp, color = Ink, lineHeight = 35.sp); Text(subtitle, color = Muted, fontSize = 13.sp) } }
@Composable
fun EmptyNote(title: String, text: String) { Column(Modifier.fillMaxWidth().background(Pale, RoundedCornerShape(20.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontFamily = FontFamily.Serif, fontSize = 22.sp, color = Ink); Text(text, fontSize = 13.sp, color = Muted, lineHeight = 21.sp) } }
@Composable
fun PetListRow(pet: Pet, store: PetStore, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = click), verticalAlignment = Alignment.CenterVertically) {
        PetArtwork(pet, Modifier.size(77.dp).clip(RoundedCornerShape(15.dp)))
        Column(Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("${pet.name}, ${pet.age}", fontFamily = FontFamily.Serif, fontSize = 22.sp, color = Ink); Text(pet.breed, fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${distanceLabel(pet, store)} away", fontSize = 11.sp, color = Sage) }
    }
}
fun distanceLabel(pet: Pet, store: PetStore): String = String.format(Locale.US, "%.1f km", pet.distance(store.latitude, store.longitude))
@Composable
fun Tag(text: String, foreground: Color, background: Color, fontSize: Int = 10) { Text(text, Modifier.background(background, RoundedCornerShape(25.dp)).padding(horizontal = 10.dp, vertical = 7.dp), color = foreground, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
@Composable
fun RoundAction(icon: String, label: String, color: Color, size: Dp, enabled: Boolean = true, background: Color = Color.White, action: () -> Unit) {
    Box(Modifier.size(size).background(if (enabled) background else Pale, CircleShape).border(1.dp, if (background == Coral) Coral else Color(0xFFE9E5DD), CircleShape).clickable(enabled = enabled, onClick = action).semantics { contentDescription = label }, contentAlignment = Alignment.Center) { AppIcon(icon, if (enabled) color else Color(0xFFC8C7BF), size * .43f) }
}
