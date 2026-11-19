package com.example.petinder

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.example.petinder.ui.theme.PetinderTheme

val Ink = Color(0xFF28362E)
val Muted = Color(0xFF7D8278)
val Coral = Color(0xFFE8755D)
val Cream = Color(0xFFFFFCF7)
val Sage = Color(0xFF4F7560)
val Pale = Color(0xFFF3EFE7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent { PetinderTheme { PetinderApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetinderApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { PetStore(context.applicationContext, scope) }
    var pendingConnection by rememberSaveable { mutableStateOf<String?>(null) }
    val networkPermission = "android.permission.ACCESS_LOCAL_NETWORK"
    val networkPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val url = pendingConnection
        pendingConnection = null
        if (granted && url != null) store.connect(url)
        else store.notice = "Local network access is needed to reach a backend on your computer. You can keep exploring demo pets."
    }
    val connect: (String) -> Unit = { url ->
        if (Build.VERSION.SDK_INT >= 37 && isLocalBackend(url) && ContextCompat.checkSelfPermission(context, networkPermission) != PackageManager.PERMISSION_GRANTED) {
            pendingConnection = url
            networkPermissionLauncher.launch(networkPermission)
        } else store.connect(url)
    }
    val location = remember { LocationHelper(context.applicationContext) }
    var locating by remember { mutableStateOf(false) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            locating = true
            location.locate({ store.setLocation(it.latitude, it.longitude); locating = false }, { store.notice = it; locating = false })
        } else store.notice = "Location access was declined. You can still explore Chicago sample pets."
    }
    val requestLocation = { permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    DisposableEffect(Unit) { onDispose { location.cancel() } }
    LaunchedEffect(Unit) {
        val needsPermission = Build.VERSION.SDK_INT >= 37 && isLocalBackend(store.baseUrl) && ContextCompat.checkSelfPermission(context, networkPermission) != PackageManager.PERMISSION_GRANTED
        if (!needsPermission) store.connect()
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var applying by remember { mutableStateOf<Pet?>(null) }
    var filters by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    // Clear only after display, so a state change cannot cancel the active snackbar.
    LaunchedEffect(store.notice) { store.notice?.let { snack.showSnackbar(it); store.notice = null } }
    Scaffold(containerColor = Cream, snackbarHost = { SnackbarHost(snack) }, bottomBar = {
        Column(Modifier.background(Cream)) {
            HorizontalDivider(color = Color(0xFFECE8E0))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().height(70.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Discover" to "paw", "Nearby" to "pin", "Saved" to "heart", "Profile" to "person").forEachIndexed { index, item ->
                    Column(Modifier.weight(1f).fillMaxHeight().clickable { tab = index }.semantics { selected = tab == index; role = Role.Tab }.padding(top = 11.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(44.dp, 27.dp).background(if (tab == index) Coral.copy(alpha = .12f) else Color.Transparent, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { AppIcon(item.second, if (tab == index) Coral else Muted, 23.dp) }
                        Text(item.first, fontSize = 10.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium, color = if (tab == index) Coral else Muted)
                    }
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(Coral, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { AppIcon("paw", Color.White, 24.dp) }
                Text("petinder", Modifier.padding(start = 9.dp), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = Ink)
                Spacer(Modifier.weight(1f))
                Column(Modifier.clickable(onClick = requestLocation), horizontalAlignment = Alignment.End) {
                    Text(if (locating) "FINDING YOU" else "YOUR LOCATION", fontSize = 8.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, color = Muted)
                    Row(verticalAlignment = Alignment.CenterVertically) { AppIcon("pin", Coral, 13.dp); Text(if (store.gpsLocation) " GPS location" else " Chicago, IL", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Ink); Text("  ⌄", color = Coral) }
                }
            }
            when (tab) {
                0 -> DiscoverScreen(store, { filters = true }, { selectedPet = it })
                1 -> NearbyScreen(store, locating, requestLocation, { selectedPet = it }, { filters = true })
                2 -> SavedScreen(store, { selectedPet = it })
                3 -> ProfileScreen(store, locating, requestLocation, connect)
            }
        }
    }
    if (filters) {
        var radius by remember { mutableFloatStateOf(store.radius) }
        AlertDialog(onDismissRequest = { filters = false }, title = { Text("A little closer to love", fontFamily = FontFamily.Serif) }, text = {
            Column { Text("Search within ${radius.toInt()} km", color = Ink); Slider(radius, { radius = it }, valueRange = 1f..200f); Text("Distance is measured from your selected location. Try a wider radius to meet more pets.", color = Muted, fontSize = 13.sp) }
        }, confirmButton = { TextButton(onClick = { store.radius = radius; filters = false }) { Text("Show pets") } }, dismissButton = { TextButton(onClick = { filters = false }) { Text("Cancel") } })
    }
    selectedPet?.let { pet ->
        ModalBottomSheet(onDismissRequest = { selectedPet = null }, containerColor = Cream) {
            PetDetails(pet, store) { selectedPet = null; applying = pet }
        }
    }
    applying?.let { pet -> ModalBottomSheet(onDismissRequest = { applying = null }, containerColor = Cream) { AdoptionForm(pet, store) { applying = null } } }
}

private fun isLocalBackend(url: String): Boolean {
    val host = Uri.parse(url.trim()).host?.lowercase() ?: return false
    val secondOctet = host.split('.').getOrNull(1)?.toIntOrNull() ?: -1
    return host == "localhost" || host.startsWith("127.") || host.startsWith("10.") ||
        host.startsWith("192.168.") || host.startsWith("169.254.") ||
        (host.startsWith("172.") && secondOctet in 16..31) || host.endsWith(".local") ||
        host.contains(':') || !host.contains('.')
}
