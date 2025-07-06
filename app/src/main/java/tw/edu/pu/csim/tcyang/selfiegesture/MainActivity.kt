package tw.edu.pu.csim.tcyang.selfiegesture

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import tw.edu.pu.csim.tcyang.selfiegesture.ui.theme.SelfieGestureTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SelfieGestureTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraPermission()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermission() {
    // 獲取當前 Composable 的 Context
    val context = LocalContext.current
    // 使用 rememberPermissionState 記住相機權限的狀態
    val permissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

    // 副作用處理器，它會在 Composable 首次進入組合時執行一次其內部的程式碼塊。
    LaunchedEffect(Unit) { // 'Unit' 表示這個 LaunchedEffect 只會執行一次
        if (!permissionState.status.isGranted && !permissionState.status.shouldShowRationale) {
            // 如果權限未授予，且不應顯示理由 (表示是第一次請求或被永久拒絕)
            // 這裡我們直接啟動權限請求
            permissionState.launchPermissionRequest()
        }
    }

    if (permissionState.status.isGranted) {
        Text("您允許拍照權限，歡迎使用拍照功能！")
        CameraPreview()
    }
    else{
        if (permissionState.status.shouldShowRationale){
            Button(onClick = {
                permissionState.launchPermissionRequest()
            })
            {
                Text("使用者拒絕但未勾選'不再詢問'，本應用程式需要相機權限才能正常運作，請授予權限。")
            }
        }
        else{
            Button(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            })
            {
                Text("使用者拒絕並勾選了'不再詢問'，或從未被詢問過。請到設定->應用程式，選擇App手動允許相機權限。")
            }
        }
    }
}
