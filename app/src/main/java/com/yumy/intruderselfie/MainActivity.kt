package com.yumy.intruderselfie

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File

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
    var showCamera by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Sẵn sàng bảo vệ nhật ký sự kiện") }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                events = EventStore.all(context)
                adminEnabled = isAdminEnabled(context)
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

    if (showCamera) {
        CameraTestScreen(onDone = { message -> showCamera = false; status = message })
        return
    }

    LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF4F8F6)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("YUMY", color = Green, style = MaterialTheme.typography.labelLarge)
            Text("Intruder Selfie", color = Ink, style = MaterialTheme.typography.headlineMedium)
            Text("Bảo vệ thiết bị · Xiaomi / HyperOS", color = Color(0xFF63766F))
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
                override fun onImageSaved(output: ImageCapture.OutputFileResults) { message = "Đã lưu ảnh riêng tư trong bộ nhớ ứng dụng." }
                override fun onError(exception: ImageCaptureException) { message = "Chụp ảnh thất bại: ${exception.message}" }
            })
        }, modifier = Modifier.fillMaxWidth()) { Text("Chụp ảnh thử") }
        OutlinedButton(onClick = { onDone(message) }, modifier = Modifier.fillMaxWidth()) { Text("Quay lại", color = Color.White) }
    }
}

private fun isAdminEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return manager.isAdminActive(ComponentName(context, IntrusionAdminReceiver::class.java))
}
