package com.yumy.intruderselfie

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF16856B)
private val Ink = Color(0xFF17352D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Color(0xFFF4F8F6))) { Dashboard() } }
    }
}

@Composable
private fun Dashboard() {
    val context = LocalContext.current
    var events by remember { mutableStateOf(EventStore.all(context)) }
    var adminEnabled by remember { mutableStateOf(isAdminEnabled(context)) }
    var photos by remember { mutableStateOf(PhotoStore.all(context)) }
    var selectedPhoto by remember { mutableStateOf<File?>(null) }
    var photoToDelete by remember { mutableStateOf<File?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(PinStore.isEnabled(context)) }
    var appLocked by remember { mutableStateOf(pinEnabled) }
    var showPinSettings by remember { mutableStateOf(false) }
    var lockOnNextStart by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Sẵn sàng bảo vệ nhật ký sự kiện") }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentPinEnabled by rememberUpdatedState(pinEnabled)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (currentPinEnabled) lockOnNextStart = true
                Lifecycle.Event.ON_START -> {
                    if (lockOnNextStart && currentPinEnabled) appLocked = true
                    lockOnNextStart = false
                    events = EventStore.all(context)
                    adminEnabled = isAdminEnabled(context)
                    photos = PhotoStore.all(context)
                }
                Lifecycle.Event.ON_RESUME -> {
                    events = EventStore.all(context)
                    adminEnabled = isAdminEnabled(context)
                    photos = PhotoStore.all(context)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        adminEnabled = isAdminEnabled(context)
        status = if (adminEnabled) "Đã bật quyền quản trị thiết bị" else "Chưa bật quyền quản trị thiết bị"
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showCamera = true else status = "Bạn chưa cấp quyền camera"
    }

    if (appLocked && pinEnabled) {
        AppLockScreen(onUnlock = { entered ->
            if (PinStore.verify(context, entered)) { appLocked = false; status = "Đã mở khóa ứng dụng"; true } else false
        })
        return
    }

    if (showCamera) {
        CameraTestScreen(onDone = { message -> showCamera = false; status = message; photos = PhotoStore.all(context) })
        return
    }

    LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF4F8F6)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YUMY", color = Green, style = MaterialTheme.typography.labelLarge)
                    Text("Intruder Selfie", color = Ink, style = MaterialTheme.typography.headlineMedium)
                    Text("Bảo vệ thiết bị · Xiaomi / HyperOS", color = Color(0xFF63766F))
                }
                IconButton(onClick = { showPinSettings = true }) { Icon(Icons.Default.Lock, contentDescription = "Cài đặt khóa ứng dụng", tint = Green) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (pinEnabled) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text("KHÓA BẢO MẬT ỨNG DỤNG", color = Ink, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(if (pinEnabled) "Đã bật mã PIN 6 số. Ứng dụng sẽ yêu cầu PIN khi mở lại sau khi chạy nền." else "Chưa thiết lập mã PIN. Tạo mã PIN để ngăn người khác mở ứng dụng.", color = Color(0xFF63766F), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showPinSettings = true }, modifier = Modifier.weight(1f)) { Text(if (pinEnabled) "Đổi mã PIN" else "Thiết lập PIN") }
                        if (pinEnabled) Button(onClick = { appLocked = true }, modifier = Modifier.weight(1f)) { Text("Khóa ngay") }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F3ED))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TRẠNG THÁI BẢO VỆ", color = Green, style = MaterialTheme.typography.labelMedium)
                    Text(if (adminEnabled) "Đã bật theo dõi sự kiện hệ thống" else "Chưa bật theo dõi hệ thống", color = Ink, style = MaterialTheme.typography.titleLarge)
                    Text("Android chỉ gửi một số sự kiện sai mật khẩu. Face Unlock thất bại trên màn hình khóa không được ứng dụng bên thứ ba truy cập.", color = Color(0xFF49645A))
                    Button(onClick = {
                        val component = ComponentName(context, IntrusionAdminReceiver::class.java)
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component).putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Cho phép YUMY ghi nhận sự kiện nhập sai mật khẩu do Android cung cấp. Không đọc mật khẩu hoặc sinh trắc học.")
                        adminLauncher.launch(intent)
                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Security, null); Spacer(Modifier.width(8.dp)); Text(if (adminEnabled) "Kiểm tra quyền quản trị" else "Bật theo dõi hệ thống") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("${events.count { it.type == "PASSWORD_FAILED" }}", "Lần sai mật khẩu", Modifier.weight(1f))
                StatCard("${events.size}", "Tổng sự kiện", Modifier.weight(1f))
            }
        }
        item {
            OutlinedButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) showCamera = true else cameraPermission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Kiểm tra camera trước (chụp có chủ động)")
            }
            Text(status, color = Color(0xFF63766F), style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ẢNH ĐÃ LƯU", color = Ink, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("${photos.size} ảnh", color = Green, style = MaterialTheme.typography.labelMedium)
                if (photos.isNotEmpty()) TextButton(onClick = { confirmDeleteAll = true }) { Text("Xóa tất cả", color = Color(0xFFB54738)) }
            }
        }
        if (photos.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text("Chưa có ảnh. Nhấn ‘Kiểm tra camera trước’, sau đó nhấn ‘Chụp ảnh thử’. Ảnh sẽ được lưu trong vùng riêng tư của ứng dụng và hiển thị tại đây.", Modifier.padding(16.dp), color = Color(0xFF63766F), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            items(photos, key = { it.absolutePath }) { photo ->
                Card(modifier = Modifier.fillMaxWidth().clickable { selectedPhoto = photo }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val bitmap = remember(photo.absolutePath, photo.lastModified()) { BitmapFactory.decodeFile(photo.absolutePath) }
                        if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "Ảnh đã lưu", modifier = Modifier.size(64.dp))
                        Column(Modifier.weight(1f).clickable { selectedPhoto = photo }) {
                            Text(photo.name, color = Ink, style = MaterialTheme.typography.titleSmall)
                            Text("${photo.length() / 1024} KB · ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(photo.lastModified()))}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text("Chạm để xem ảnh", color = Green, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { photoToDelete = photo }) { Icon(Icons.Default.Delete, contentDescription = "Xóa ảnh", tint = Color(0xFFB54738)) }
                    }
                }
            }
        }
        item { Text("LỊCH SỬ BẢO MẬT", color = Ink, style = MaterialTheme.typography.titleMedium) }
        if (events.isEmpty()) item { Text("Chưa có sự kiện. Android chỉ gửi callback khi đủ điều kiện hệ thống.", color = Color.Gray) }
        items(events) { event ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(if (event.type == "PASSWORD_FAILED") "Nhập sai mật khẩu" else "Xác thực thành công", color = if (event.type == "PASSWORD_FAILED") Color(0xFFB54738) else Green, style = MaterialTheme.typography.titleSmall)
                    Text(event.time, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    Text(event.detail, color = Ink, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Ảnh không được tự chụp từ màn hình khóa. Camera chỉ hoạt động khi người dùng mở chức năng và cấp quyền.", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
    }
    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Xóa ảnh này?") },
            text = { Text("${photo.name} sẽ bị xóa khỏi bộ nhớ riêng của ứng dụng. Không thể hoàn tác.") },
            confirmButton = { TextButton(onClick = {
                val deleted = runCatching { photo.delete() }.getOrDefault(false)
                photos = PhotoStore.all(context)
                status = if (deleted) "Đã xóa ảnh ${photo.name}" else "Không thể xóa ảnh. Hãy thử lại."
                photoToDelete = null
            }) { Text("Xóa", color = Color(0xFFB54738)) } },
            dismissButton = { TextButton(onClick = { photoToDelete = null }) { Text("Hủy") } }
        )
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Xóa toàn bộ ảnh?") },
            text = { Text("Tất cả ${photos.size} ảnh đã lưu sẽ bị xóa vĩnh viễn khỏi ứng dụng.") },
            confirmButton = { TextButton(onClick = {
                val total = photos.size
                val count = photos.count { runCatching { it.delete() }.getOrDefault(false) }
                photos = PhotoStore.all(context)
                status = "Đã xóa $count/$total ảnh"
                confirmDeleteAll = false
            }) { Text("Xóa tất cả", color = Color(0xFFB54738)) } },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Hủy") } }
        )
    }
    if (showPinSettings) {
        PinSettingsDialog(
            enabled = pinEnabled,
            onDismiss = { showPinSettings = false },
            onSave = { oldPin, newPin ->
                val allowed = !pinEnabled || PinStore.verify(context, oldPin)
                if (!allowed) "Mã PIN hiện tại không đúng"
                else if (newPin.length != 6 || newPin.any { !it.isDigit() }) "Mã PIN phải gồm đúng 6 chữ số"
                else { PinStore.set(context, newPin); pinEnabled = true; showPinSettings = false; "Đã bật/cập nhật mã PIN" }
            },
            onDisable = { oldPin ->
                if (!PinStore.verify(context, oldPin)) "Mã PIN hiện tại không đúng"
                else { PinStore.clear(context); pinEnabled = false; appLocked = false; showPinSettings = false; "Đã tắt khóa ứng dụng" }
            }
        )
    }

    selectedPhoto?.let { photo ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedPhoto = null }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val bitmap = remember(photo.absolutePath, photo.lastModified()) { BitmapFactory.decodeFile(photo.absolutePath) }
                    if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "Ảnh đã lưu", modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp))
                    Text(photo.name, color = Ink, style = MaterialTheme.typography.bodySmall)
                    Text("${photo.length()} bytes · ${if (photo.exists() && photo.length() > 0) "Tệp hợp lệ" else "Không đọc được tệp"}", color = Green, style = MaterialTheme.typography.labelMedium)
                    Button(onClick = { selectedPhoto = null }, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) { Text(value, color = Green, style = MaterialTheme.typography.headlineMedium); Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun CameraTestScreen(onDone: (String) -> Unit) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var message by remember { mutableStateOf("Camera trước đang khởi tạo…") }
    Column(Modifier.fillMaxSize().background(Color(0xFF101A17)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Kiểm tra camera trước", color = Color.White, style = MaterialTheme.typography.titleLarge)
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                runCatching {
                    val provider = providerFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    provider.unbindAll()
                    provider.bindToLifecycle(context as ComponentActivity, CameraSelector.DEFAULT_FRONT_CAMERA, preview, capture)
                    imageCapture = capture
                    message = "Camera sẵn sàng. Nhấn chụp để lưu ảnh cục bộ."
                }.onFailure { message = "Không mở được camera trước: ${it.message}" }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }, modifier = Modifier.weight(1f).fillMaxWidth())
        Text(message, color = Color.White)
        Button(onClick = {
            val capture = imageCapture ?: run { message = "Camera chưa sẵn sàng"; return@Button }
            val dir = File(context.filesDir, "intruder_photos").apply { mkdirs() }
            val file = File(dir, "selfie_${System.currentTimeMillis()}.jpg")
            capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val saved = file.exists() && file.length() > 0L
                    message = if (saved) "ĐÃ CHỤP VÀ LƯU THÀNH CÔNG\n${file.name} · ${file.length() / 1024} KB\nẢnh nằm trong mục ẢNH ĐÃ LƯU trên màn hình chính."
                    else "Camera báo hoàn tất nhưng không tìm thấy tệp ảnh. Hãy thử lại và kiểm tra dung lượng bộ nhớ."
                }
                override fun onError(exception: ImageCaptureException) { message = "CHỤP ẢNH THẤT BẠI: ${exception.message} (mã ${exception.imageCaptureError})" }
            })
        }, modifier = Modifier.fillMaxWidth()) { Text("Chụp ảnh thử") }
        OutlinedButton(onClick = { onDone(message) }, modifier = Modifier.fillMaxWidth()) { Text("Quay lại", color = Color.White) }
    }
}

@Composable
private fun AppLockScreen(onUnlock: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFFF4F8F6)).padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Green, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(16.dp))
        Text("YUMY Intruder Selfie", color = Ink, style = MaterialTheme.typography.headlineSmall)
        Text("Nhập mã PIN 6 số để mở khóa", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it; error = "" }, label = { Text("Mã PIN") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        if (error.isNotBlank()) Text(error, color = Color(0xFFB54738), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Button(onClick = { if (pin.length != 6) error = "Vui lòng nhập đủ 6 chữ số" else if (onUnlock(pin)) pin = "" else { error = "Mã PIN không đúng"; pin = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Mở khóa") }
    }
}

@Composable
private fun PinSettingsDialog(enabled: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> String, onDisable: (String) -> String) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (enabled) "Đổi mã PIN ứng dụng" else "Thiết lập khóa ứng dụng") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (enabled) OutlinedTextField(value = oldPin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) oldPin = it }, label = { Text("PIN hiện tại") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                OutlinedTextField(value = newPin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it }, label = { Text("PIN mới (6 số)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it }, label = { Text("Nhập lại PIN mới") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                if (message.isNotBlank()) Text(message, color = Color(0xFFB54738), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = {
            if (newPin != confirmPin) message = "Hai mã PIN không trùng nhau"
            else { val result = onSave(oldPin, newPin); if (result.isNotBlank()) message = result }
        }) { Text("Lưu PIN") } },
        dismissButton = {
            Row {
                if (enabled) TextButton(onClick = { val result = onDisable(oldPin); if (result.isNotBlank()) message = result }) { Text("Tắt khóa", color = Color(0xFFB54738)) }
                TextButton(onClick = onDismiss) { Text("Đóng") }
            }
        }
    )
}

private object PinStore {
    private const val PREFS = "app_lock"
    private const val SALT = "pin_salt"
    private const val HASH = "pin_hash"
    fun isEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(HASH)
    fun set(context: Context, pin: String) {
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(SALT, android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)).putString(HASH, android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)).apply()
    }
    fun verify(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saltText = prefs.getString(SALT, null) ?: return false
        val hashText = prefs.getString(HASH, null) ?: return false
        return runCatching {
            val expected = android.util.Base64.decode(hashText, android.util.Base64.NO_WRAP)
            val actual = derive(pin, android.util.Base64.decode(saltText, android.util.Base64.NO_WRAP))
            java.security.MessageDigest.isEqual(expected, actual)
        }.getOrDefault(false)
    }
    fun clear(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(SALT).remove(HASH).apply() }
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try { javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
}

private fun isAdminEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return manager.isAdminActive(ComponentName(context, IntrusionAdminReceiver::class.java))
}


private object PhotoStore {
    fun all(context: Context): List<File> = File(context.filesDir, "intruder_photos")
        .listFiles { file -> file.isFile && file.extension.equals("jpg", ignoreCase = true) }
        ?.filter { it.length() > 0L }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}
