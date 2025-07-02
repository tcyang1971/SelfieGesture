package tw.edu.pu.csim.tcyang.selfiegesture

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreview() {

    // Obtain the current context and lifecycle owner
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 儲存圖像分析結果的狀態
    var analysisResult by remember { mutableStateOf("分析結果：等待中...")}

    // 專用於 ImageAnalysis 的執行緒
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // Remember a LifecycleCameraController for this composable
    val camCrl = LifecycleCameraController(context) // 先創建實例
    camCrl.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA //前置鏡頭
    camCrl.bindToLifecycle(lifecycleOwner) // 再調用方法
    val cameraController = remember {
        camCrl
    }

    // 關鍵點： ImageAnalysis use case 的設定與綁定------------
    //為了確保 ImageAnalysis 在 Composable 存活期間有效，
    // 並在 Composable 離開組合時正確釋放資源
    // 我們將 setImageAnalysisAnalyzer 的設定和執行緒的關閉
    // 都放在 DisposableEffect 的生命週期管理中。
    DisposableEffect (lifecycleOwner, cameraController) { // 當 lifecycleOwner 或 cameraController 改變時重新執行
        // 設定 ImageAnalysis use case
        // 注意：LifecycleCameraController 簡化了綁定，直接設定 analyzer 即可
        cameraController.setImageAnalysisAnalyzer(
            analysisExecutor, // 使用專用的分析執行緒池
            ImageAnalysis.Analyzer { imageProxy ->
                // 在這裡處理每一幀圖像
                // 以下是一個簡單的模擬分析，例如：簡單計算圖像的尺寸資訊
                val width = imageProxy.width  //圖像的寬度
                val height = imageProxy.height  //圖像的高度
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees  //圖像的旋轉資訊

                // 更新 Compose 狀態 (需要在主執行緒上更新 UI)
                analysisResult = "圖像 ($width x $height), 旋轉: $rotationDegrees"
                // 實際應用中，您會在這裡調用您的 MediaPipe 辨識器
                imageProxy.close() // *** 非常重要：處理完畢後關閉 ImageProxy ***
            }
        )
        // 將 LifecycleCameraController 綁定到 LifecycleOwner
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            // 在 Composable 離開組合時釋放資源
            cameraController.clearImageAnalysisAnalyzer() // 清除分析器
            cameraController.unbind() // 解綁相機
            analysisExecutor.shutdown() // 關閉分析執行緒
        }
    }


    // Key Point: Displaying the Camera Preview
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // Initialize the PreviewView and configure it
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_START
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = cameraController // Set the controller to manage the camera lifecycle
            }
        },
        onRelease = {
            // Release the camera controller when the composable is removed from the screen
            cameraController.unbind()
        }
    )

    // 顯示圖像分析結果
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = analysisResult,
            color = Color.Blue,
            modifier = Modifier
                .align(Alignment.BottomCenter) // 對齊Box的底部中心
                .offset(y = (-50).dp) //往上一些
                .background(Color.Yellow) // 背景顏色
                .padding(16.dp) // 添加一些內邊距
        )
    }

}
