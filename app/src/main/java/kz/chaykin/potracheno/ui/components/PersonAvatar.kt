package kz.chaykin.potracheno.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kz.chaykin.potracheno.data.photo.PhotoStore
import kz.chaykin.potracheno.model.Person
import kz.chaykin.potracheno.ui.theme.AvatarPalette

/** Фото человека, а если его нет — первая буква имени на цветном кружке. */
@Composable
fun PersonAvatar(person: Person, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val context = LocalContext.current
    val photo = person.photoFileName
    if (photo != null) {
        AsyncImage(
            model = PhotoStore.fileIn(context, photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        val color = AvatarPalette[(person.id % AvatarPalette.size).toInt().coerceAtLeast(0)]
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = person.name.trim().take(1).uppercase().ifEmpty { "?" },
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}
