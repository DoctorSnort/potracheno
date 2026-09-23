package kz.chaykin.potracheno.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.data.photo.PhotoStore

/** Одна фотография: показать, заменить, убрать. Человеку больше одной и не нужно. */
@Composable
fun SinglePhotoPicker(
    fileName: String?,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.person_photo), style = MaterialTheme.typography.labelLarge)

        if (fileName != null) {
            Box {
                AsyncImage(
                    model = PhotoStore.fileIn(context, fileName),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.photo_remove),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.photo_camera),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(onClick = onPickGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.photo_gallery),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
