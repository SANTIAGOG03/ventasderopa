package com.example.ecotrend // Reemplaza con el nombre de tu paquete

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ecotrend.ui.theme.EcotrendTheme // Asegúrate de que este sea el nombre correcto de tu tema
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// MainActivity ahora extiende ComponentActivity para usar Compose
class MainActivity : ComponentActivity() {

    // Estas propiedades y métodos se mantienen en la Activity para manejar
    // los resultados de las actividades y permisos de forma nativa.
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1
    private val CAMERA_PERMISSION_REQUEST_CODE = 2
    private var mCameraPhotoPath: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usamos setContent para definir la UI con Compose
        setContent {
            EcotrendTheme { // Envuelve tu UI con tu tema de Compose
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Llamamos a una función Composable que contendrá la WebView
                    WebViewScreen(this) // Pasamos la instancia de la Activity
                }
            }
        }
    }

    // onActivityResult se mantiene en la Activity para manejar los resultados de selección de archivos
    @Deprecated("Deprecated in Java") // Añadida la anotación @Deprecated
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mUploadMessage == null) return
            val results: Array<Uri>? =
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || data.data == null) {
                        if (mCameraPhotoPath != null) {
                            arrayOf(Uri.parse(mCameraPhotoPath!!))
                        } else {
                            null
                        }
                    } else {
                        val dataString = data.dataString
                        if (dataString != null) {
                            arrayOf(Uri.parse(dataString))
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
            mUploadMessage!!.onReceiveValue(results)
            mUploadMessage = null
        }
    }

    // onRequestPermissionsResult se mantiene en la Activity para manejar las respuestas de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // El permiso fue concedido. La WebView puede intentar acceder a la cámara ahora.
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para crear un archivo de imagen temporal para la cámara
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intents
        mCameraPhotoPath = "file:" + image.absolutePath
        return image
    }

    // Función auxiliar para crear el WebChromeClient
    private fun createWebChromeClient(activity: Activity): WebChromeClient {
        return object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    activity.runOnUiThread {
                        if (request.resources[0] == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                            if (ContextCompat.checkSelfPermission(
                                    activity,
                                    Manifest.permission.CAMERA
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    activity,
                                    arrayOf(Manifest.permission.CAMERA),
                                    CAMERA_PERMISSION_REQUEST_CODE
                                )
                                request.deny()
                            } else {
                                request.grant(request.resources)
                            }
                        } else {
                            request.grant(request.resources)
                        }
                    }
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                mUploadMessage = filePathCallback // Asignar a la propiedad de la Activity
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile() // Usar el método de la Activity
                    } catch (ex: IOException) {
                        Log.e("WebView", "Unable to create Image File", ex)
                    }
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        val photoURI = FileProvider.getUriForFile(
                            activity,
                            activity.applicationContext.packageName + ".provider",
                            photoFile
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val intentArray: Array<Intent> =
                    if (takePictureIntent != null) arrayOf(takePictureIntent) else arrayOf()

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Seleccionar Imagen")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                activity.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
                return true
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String?, capture: String?) {
                // Implementación para versiones antiguas
            }
        }
    }

    // Función auxiliar para crear el WebViewClient
    private fun createWebViewClient(activity: Activity): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("https://wa.me/")) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    activity.startActivity(intent)
                    return true
                }
                view.loadUrl(url)
                return false
            }
        }
    }

    // Función Composable que contiene la WebView
    @Composable
    fun WebViewScreen(activity: MainActivity) { // Recibe la instancia de MainActivity
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                // Aquí se crea la WebView
                WebView(context).apply {
                    // Configuración de la WebView
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.mediaPlaybackRequiresUserGesture = false

                    // Asignar los clientes creados por los métodos auxiliares de la Activity
                    webViewClient = activity.createWebViewClient(activity)
                    webChromeClient = activity.createWebChromeClient(activity)

                    // Cargar tu archivo HTML
                    loadUrl("file:///android_asset/web_content/index.html") // Ajusta la ruta si es necesario
                }
            },
            update = { webView ->
                // Este bloque se llama cuando el estado de AndroidView cambia.
                // En este caso simple, no necesitamos actualizar nada aquí.
            }
        )
    }

    // onBackPressed se mantiene en la Activity, pero su lógica debe ser adaptada para Compose
    // o para que la WebView maneje su propio historial de navegación si es posible.
    // Para simplificar, por ahora solo llamaremos a super.onBackPressed().
    override fun onBackPressed() {
        // Si necesitas que la WebView maneje el botón de retroceso (navegar hacia atrás en el historial web),
        // necesitarías una referencia a la WebView dentro de este método.
        // Con AndroidView, es más complejo obtener esa referencia directamente.
        // Una solución más avanzada implicaría gestionar el estado de la WebView en el Composable.
        // Por ahora, para evitar el error 'findViewById' en Compose, simplemente llamamos a super.
        super.onBackPressed()
    }
}
