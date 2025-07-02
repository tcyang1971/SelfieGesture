package tw.edu.pu.csim.tcyang.selfiegesture

import android.graphics.Bitmap
import android.graphics.Matrix
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
import androidx.compose.runtime.LaunchedEffect
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
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
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

    // 手勢辨識結果
    var gestureResult by remember { mutableStateOf("手勢：無") }

    // MediaPipe手部關鍵點辨識器
    var handLandmarker: HandLandmarker? by remember { mutableStateOf(null) }


    // 初始化 MediaPipe 手部關鍵點辨識器

    //LaunchedEffect 會在其 key（這裡是 Unit）不變的情況下，只執行一次
    //使用 Unit 作為 key，確保當 CameraPreview首次進入組合時，只會被執行一次

    LaunchedEffect(Unit) {
        // 1. 建立 BaseOptions Builder
        // BaseOptions 是 MediaPipe 模型的通用配置選項
        val baseOptionsBuilder =
            BaseOptions.builder().setModelAssetPath("hand_landmarker.task")

        // 2. 建立 HandLandmarkerOptions Builder
        val optionsBuilder =
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.LIVE_STREAM) // 設置為直播模式
                .setNumHands(2) // 偵測最多兩隻手
                .setMinHandDetectionConfidence(0.5f)  // 設定最小手部偵測置信度
                .setMinTrackingConfidence(0.5f)  // 設定最小手部追蹤置信度
                .setMinHandPresenceConfidence(0.5f)  // 設定最小手部存在置信度
                .setResultListener { result, inputImage -> // 設定結果監聽器
                    // 處理 MediaPipe 的辨識結果
                    val gesture = processHandLandmarkerResult(result)
                    gestureResult = "手勢：$gesture"
                }
                .setErrorListener { error ->  // 處理錯誤
                    gestureResult = "手勢辨識錯誤: ${error.message}"
                }

        // 3. 創建 HandLandmarker 實例
        handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
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
                handLandmarker?.let { landmarker ->
                    // 將 ImageProxy 轉換為 MediaPipe 圖像格式
                    val bitmap = imageProxy.toBitmap()
                    val matrix = Matrix()
                    // 根據相機旋轉角度調整圖像方向
                    matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    // 前置鏡頭，需要水平翻轉
                    matrix.postScale(-1f, 1f,
                        bitmap.width / 2f, bitmap.height / 2f)

                    // 獲取圖像的旋轉角度 (CameraX 提供的值)
                    val rotationDegrees =
                        imageProxy.imageInfo.rotationDegrees


                    //創建一個新的、已旋轉的 Bitmap
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width,
                        bitmap.height, matrix, true
                    )

                    //將Bitmap物件轉換成 MediaPipe可以處理的影像格式
                    val mpImage =
                        BitmapImageBuilder(rotatedBitmap).build()

                    // 執行手部地標辨識(影像格式與時間戳)
                    landmarker.detectAsync(mpImage,
                        imageProxy.imageInfo.timestamp)
                }

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
            text = gestureResult,
            color = Color.Blue,
            modifier = Modifier
                .align(Alignment.BottomCenter) // 對齊Box的底部中心
                .offset(y = (-50).dp) //往上一些
                .background(Color.Yellow) // 背景顏色
                .padding(16.dp) // 添加一些內邊距
        )
    }

}

//處理 MediaPipe 手部地標辨識結果，並判斷手勢。
private fun processHandLandmarkerResult(result: HandLandmarkerResult): String {
    if (result.landmarks().isEmpty()) {
        return "無手部"
    }

    // 這裡只是簡單的範例，實際的手勢辨識會更複雜
    // 你需要根據手部關節點的座標來判斷手勢

    val landmarks = result.landmarks()[0] // 假設只考慮第一隻手

    // 簡單判斷：
    // 剪刀：食指和中指伸直
    // 石頭：所有手指彎曲
    // 布：所有手指伸直

    val thumbTip = landmarks.get(4)
    val indexTip = landmarks.get(8)
    val middleTip = landmarks.get(12)
    val ringTip = landmarks.get(16)
    val pinkyTip = landmarks.get(20)

    val indexMcp = landmarks.get(5)
    val middleMcp = landmarks.get(9)
    val ringMcp = landmarks.get(13)
    val pinkyMcp = landmarks.get(17)

    // 簡單判斷剪刀：食指和中指尖端高於其掌指關節，其他手指尖端低於其掌指關節 (非常簡化)
    // 實際需要更精確的幾何判斷
    val isScissor = indexTip.y() < indexMcp.y() && middleTip.y() < middleMcp.y() &&
            ringTip.y() > ringMcp.y() && pinkyTip.y() > pinkyMcp.y()

    // 簡單判斷布：所有手指尖端都高於其掌指關節 (非常簡化)
    val isPaper = indexTip.y() < indexMcp.y() && middleTip.y() < middleMcp.y() &&
            ringTip.y() < ringMcp.y() && pinkyTip.y() < pinkyMcp.y()

    // 簡單判斷石頭：所有手指尖端都低於其掌指關節 (非常簡化)
    val isRock = indexTip.y() > indexMcp.y() && middleTip.y() > middleMcp.y() &&
            ringTip.y() > ringMcp.y() && pinkyTip.y() > pinkyMcp.y()

    return when {
        isScissor -> "剪刀"
        isPaper -> "布"
        isRock -> "石頭"
        else -> "未知手勢"
    }
}
