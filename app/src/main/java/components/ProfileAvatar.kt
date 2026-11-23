package com.pocketbudget.app.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.pocketbudget.app.repository.ProfileRepository

@Composable
fun ProfileAvatar(
    ruta: String?,
    modifier: Modifier = Modifier,
    colorDefecto: Color = Color(0xFFA78BFA)
) {
    val repository = remember { ProfileRepository() }
    var foto by remember(ruta) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(ruta) {
        foto = if (ruta.isNullOrBlank()) null else {
            runCatching {
                val bytes = repository.descargarAvatar(ruta)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier.clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (foto != null) {
            // La foto ocupa TODO el círculo y se recorta redonda
            Image(
                bitmap = foto!!.asImageBitmap(),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Foto de perfil predeterminada",
                tint = colorDefecto,
                modifier = Modifier.fillMaxSize(0.75f)
            )
        }
    }
}