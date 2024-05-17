package com.daw.galeriacamara

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.daw.galeriacamara.ui.theme.GaleriaCamaraTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val SOLICITUD_CAPTURA_IMAGEN = 1
    private val SOLICITUD_GALERIA_IMAGEN = 2
    private val CODIGO_PERMISO_CAMARA = 101
    private val CODIGO_PERMISO_ALMACENAMIENTO = 102

    private val imagenCapturada = mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaleriaCamaraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContenidoApp()
                }
            }
        }
    }

    @Composable
    fun ContenidoApp() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            imagenCapturada.value?.let { bitmapImagen ->
                Box(
                    modifier = Modifier
                        .size(480.dp)
                ) {
                    Image(
                        bitmap = bitmapImagen.asImageBitmap(),
                        contentDescription = "Imagen Capturada",
                        modifier = Modifier.fillMaxSize()
                            .border(4.dp, Color.Black),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BotonAbrirGaleria()
                Spacer(modifier = Modifier.width(50.dp))
                BotonTomarFoto()
            }
        }
    }

    @Composable
    fun BotonAbrirGaleria() {
        Button(
            onClick = {
                if (tienePermisoAlmacenamiento()) {
                    abrirGaleria()
                } else {
                    solicitarPermisoAlmacenamiento()
                }
            }
        ) {
            Icon(painterResource(R.drawable.galeria), contentDescription = "Abrir Galería")
        }
    }

    @Composable
    fun BotonTomarFoto() {
        Button(
            onClick = {
                if (tienePermisoCamara()) {
                    tomarFoto()
                } else {
                    solicitarPermisoCamara()
                }
            }
        ) {
            Icon(painterResource(R.drawable.camara), contentDescription = "Tomar Foto")
        }
    }

    private fun tienePermisoCamara(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisoCamara() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CODIGO_PERMISO_CAMARA)
        }
    }

    private fun tienePermisoAlmacenamiento(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisoAlmacenamiento() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                CODIGO_PERMISO_ALMACENAMIENTO
            )
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, SOLICITUD_GALERIA_IMAGEN)
    }

    private fun tomarFoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, SOLICITUD_CAPTURA_IMAGEN)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODIGO_PERMISO_CAMARA || requestCode == CODIGO_PERMISO_ALMACENAMIENTO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == CODIGO_PERMISO_CAMARA) {
                    tomarFoto()
                } else {
                    abrirGaleria()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SOLICITUD_CAPTURA_IMAGEN && resultCode == RESULT_OK) {
            val bitmapImagen = data?.extras?.get("data") as Bitmap
            guardarImagenEnGaleria(bitmapImagen)
            imagenCapturada.value = bitmapImagen
        } else if (requestCode == SOLICITUD_GALERIA_IMAGEN && resultCode == RESULT_OK) {
            val uriImagenSeleccionada = data?.data
            uriImagenSeleccionada?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val bitmapImagen = BitmapFactory.decodeStream(inputStream)
                imagenCapturada.value = bitmapImagen
            }
        }
    }

    private fun guardarImagenEnGaleria(bitmapImagen: Bitmap) {
        val directorioImagenes = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val marcaTiempo = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val nombreArchivoImagen = "IMG_${marcaTiempo}.png"
        val archivoImagen = File(directorioImagenes, nombreArchivoImagen)

        FileOutputStream(archivoImagen).use { outputStream ->
            bitmapImagen.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        val intentEscaneoMedios = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intentEscaneoMedios.data = Uri.fromFile(archivoImagen)
        sendBroadcast(intentEscaneoMedios)
    }

    @Preview(showBackground = true)
    @Composable
    fun VistaPreviaApp() {
        GaleriaCamaraTheme {
            ContenidoApp()
        }
    }
}
