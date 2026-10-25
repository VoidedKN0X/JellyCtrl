package com.vknxworks.jellyctrl.ui.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R

/** Photos are always shown on black, in the dark palette, whatever theme the phone uses. */
@Composable
fun PhotoViewerScreen(parentId: String, startId: String) {
    com.vknxworks.jellyctrl.ui.theme.ForceDarkSignal { PhotoViewerContent(parentId, startId) }
}

@Composable
private fun PhotoViewerContent(parentId: String, startId: String) {
    val c = LocalContext.current.container
    val client = c.api
    val nav = LocalNavigator.current
    val loader = rememberLoader("photos-$parentId") {
        client.items(
            ItemQuery(parentId = parentId, recursive = false, includeItemTypes = listOf("Photo"), sortBy = listOf("SortName")),
            limit = 2000,
        ).items
    }
    var chrome by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LoadContent(loader) { photos ->
            val start = photos.indexOfFirst { it.id == startId }.coerceAtLeast(0)
            val pager = rememberPagerState(initialPage = start) { photos.size }
            HorizontalPager(pager, Modifier.fillMaxSize(), key = { photos[it].id }) { page ->
                ZoomablePhoto(photos[page], client.imageUrl(photos[page].id, "Primary", photos[page].imageTags["Primary"], 2400)) {
                    chrome = !chrome
                }
            }
            if (chrome) {
                Text(
                    "${pager.currentPage + 1} / ${photos.size}  ·  ${photos.getOrNull(pager.currentPage)?.name.orEmpty()}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                )
            }
        }
        if (chrome) {
            IconButton(onClick = nav::back, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
        }
    }
}

@Composable
private fun ZoomablePhoto(photo: BaseItemDto, url: String, onTap: () -> Unit) {
    var scale by remember(photo.id) { mutableFloatStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset = if (scale > 1f) offset + pan else Offset.Zero
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(photo.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                    },
                )
            }
            .transformable(state, canPan = { scale > 1f }),
    ) {
        ArtImage(
            url,
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
            contentScale = ContentScale.Fit,
        )
    }
}
