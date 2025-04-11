package com.thingsenz.flashlight

import android.Manifest
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import com.thingsenz.flashlight.ui.theme.FlashlightTheme

class MainActivity : ComponentActivity() {

    private var cameraManager: CameraManager? = null
    private var sosThread: Thread? = null
    private var sosInterrupt = true
    private var isFlashlightEnabled = mutableStateOf(false)
    private var sosColor = mutableStateOf(Color.Red)
    private var openPermDialog = mutableStateOf(false)
    private var openInfoDialog = mutableStateOf(false)
    private var msgType = mutableIntStateOf(0)
    private var cameraId = ""
    private var flashMode = false
    private var hasFlash = true
    private val enabledColor = Color(0xff6dd288)

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            initCamera()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)) {
                msgType.intValue=0
            } else {
                msgType.intValue=1
            }
            openPermDialog.value=true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCamera()
        enableEdgeToEdge()
        setContent {
            FlashlightTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(
                    topBar = {
                        Column {
                            TopAppBar(
                                title = { Text(text = "Flashlight") },
                                backgroundColor = if (isSystemInDarkTheme()) Color.Black else Color.White,
                                actions = {
                                    Row(modifier = Modifier.padding(end = 16.dp)) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        IconButton(
                                            onClick = { openInfoDialog.value = true },
                                            modifier = Modifier
                                                .then(Modifier.size(40.dp))
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colors.secondary,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(android.R.drawable.ic_dialog_info),
                                                contentDescription = "Info",
                                                tint = MaterialTheme.colors.secondary
                                            )
                                        }
                                    }
                                })
                        }
                    },
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            ),
                        color = MaterialTheme.colors.background
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Image(
                                painter = painterResource(if (isFlashlightEnabled.value) R.drawable.ic_torch_on else R.drawable.ic_torch_off),
                                contentDescription = "turn-on",
                                modifier = Modifier.size(60.dp, 60.dp).clickable {
                                    try {
                                        if (!sosInterrupt) {
                                            sosMode()
                                        }
                                        toggleFlash()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "An Unknown Error occurred. Kindly operate through device Flashlight feature",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Log.e("MainActivity", e.message ?: "")
                                    }
                                },
                                colorFilter = ColorFilter.tint(if (isFlashlightEnabled.value) enabledColor else Color.Red)
                            )

                            OutlinedButton(
                                onClick = {
                                    try {
                                        sosMode()
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", e.message ?: "")
                                    }
                                },
                                modifier = Modifier.size(100.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, sosColor.value)
                            ) {
                                Text(
                                    text = "SOS",
                                    style = TextStyle(color = sosColor.value, fontSize = 18.sp),
                                    maxLines = 1
                                )
                            }
                        }


                        if (openInfoDialog.value) {
                            ShowInfoDialog()
                        }
                    }
                }
            }

        }
    }

    private fun initCamera() {
        if (cameraManager==null)
            cameraManager = getSystemService(CameraManager::class.java)
        cameraId = cameraManager!!.cameraIdList[0]
        val c = cameraManager!!.getCameraCharacteristics(cameraId)
        if (c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == false) {
            hasFlash=false
            Toast.makeText(this,"Selected Camera does not support Flash",Toast.LENGTH_SHORT).show()
        }
    }

    @Synchronized
    private fun toggleFlash() {
        if (!hasFlash) {
                Toast.makeText(this,"Selected Camera does not support Flash",Toast.LENGTH_SHORT).show()
                return
            }
        isFlashlightEnabled.value = !isFlashlightEnabled.value
        cameraManager?.setTorchMode(cameraId, isFlashlightEnabled.value)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun finish() {
        finishAndRemoveTask()
    }

    //https://stackoverflow.com/questions/40007331/sos-flashlight-how-to
    //3xShort -> 3xLong -> 3xShort
    private fun sosMode() {
        if (sosThread==null) {
            sosInterrupt=false
            sosColor.value = Color.Green
            sosThread=Thread {
                while (!sosInterrupt) {
                    try {
                        if (flashMode)
                            toggleFlash()
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(1000L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(1000L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(1000L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(250L)
                        toggleFlash()
                        Thread.sleep(500L)
                    } catch (e: Exception) {
                        try {
                            if (flashMode)
                                toggleFlash()
                        } catch (ignored: Exception) {}
                        Log.e("Flashlight",e.message?: "")
                    }
                }
            }
            sosThread?.start()
        } else {
            try {
                sosInterrupt = true
                sosThread?.interrupt()
                sosThread = null
                sosColor.value = Color.Red
            } catch (e: Exception) {

            }
        }
    }

    private fun stroboscope() {
        //TODO(Implement)
    }

    override fun onDestroy() {
        if (sosThread!=null) {
            sosMode()
        }
        if (flashMode) {
            cameraManager?.setTorchMode(cameraId,false)
        }
        cameraManager = null
        super.onDestroy()
    }

    @Composable
    fun ShowPermDialog() {
        Dialog(onDismissRequest = {  }) {
            Card(shape = RoundedCornerShape(10.dp),modifier = Modifier.padding(10.dp,5.dp,10.dp,10.dp),elevation = 10.dp) {
                Column(modifier = Modifier.background(MaterialTheme.colors.background)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Camera Permission Required", textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth(),style = MaterialTheme.typography.subtitle1,
                            overflow = TextOverflow.Ellipsis, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                        Text(text = if (msgType.intValue==0) "Camera Permission is required to use Flashlight. Clicking allow will request for permission again. Clicking Deny will close the app" else "Camera permission is required to use flashlight. Please grant from settings. Clicking Deny will close the app", color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(MaterialTheme.colors.background)) {
                        TextButton(onClick = { openPermDialog.value=false
                            finish()
                        }) {
                            Text(text = "Deny",fontWeight = FontWeight.SemiBold,color = Color.Blue,modifier = Modifier.padding(top = 5.dp,bottom = 5.dp))
                        }
                        TextButton(onClick = {
                            openPermDialog.value=false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },enabled = msgType.intValue==0) {
                            Text(text = "Allow",fontWeight = FontWeight.SemiBold,color = if (msgType.intValue==0) Color.Blue else Color.Gray,modifier = Modifier.padding(top = 5.dp,bottom = 5.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ShowInfoDialog() {
        Dialog(onDismissRequest = { }) {
            Card(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(10.dp, 5.dp, 10.dp, 10.dp),
                elevation = 10.dp
            ) {
                Column(modifier = Modifier.background(MaterialTheme.colors.background)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "About", textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth(), style = MaterialTheme.typography.subtitle1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = buildAnnotatedString {
                            append("Flashlight\n\n")
                            append("Version: ${BuildConfig.VERSION_NAME}\n")
                        })
                        val uriHandler = LocalUriHandler.current
                        val openGithubAction = buildAnnotatedString {
                            withLink(
                                LinkAnnotation.Url(
                                    url = "https://github.com/JohnX4321/Compose_Flashlight",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = MaterialTheme.colors.secondary,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ),
                                    linkInteractionListener = {
                                        uriHandler.openUri("https://github.com/JohnX4321/Compose_Flashlight")
                                    }
                                )
                            ) {
                                append("Source Code")
                            }
                        }
                        Text(text = openGithubAction)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(MaterialTheme.colors.background)
                    ) {
                        TextButton(onClick = {
                            openInfoDialog.value = false
                        }) {
                            Text(
                                text = "Close",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.secondary,
                                modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}



@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlashlightTheme {
        Greeting("Android")
    }
}

