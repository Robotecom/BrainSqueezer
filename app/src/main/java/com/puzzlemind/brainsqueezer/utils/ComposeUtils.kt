package com.puzzlemind.brainsqueezer.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import java.io.File


//utility function that accept drawable resources and image files uris
@Composable
fun painterFile(uri: String, @DrawableRes id:Int): Painter {

    val value = remember { TypedValue() }
    val context = LocalContext.current


    if (id != 0){
        val res = context.resources
        res.getValue(id, value, true)
        val path = value.string

        val imageBitmap = remember(path, id) {
            loadImageBitmapResource(res, id)
        }
        return BitmapPainter(imageBitmap)
    }else {

        val path = value.string

        println("path is not nulll:$uri")
        // Otherwise load the bitmap resource
        val imageBitmap = remember(path, uri) {
            loadImageBitmapFile(uri,context)
        }
        return BitmapPainter(imageBitmap)
    }

}

private fun loadImageBitmapFile(uri: String,context: Context): ImageBitmap {
    try {
        return ImageBitmap.loadImageFile(uri,context)
    } catch (throwable: Throwable) {
        throw IllegalArgumentException(throwable.message)
    }
}

fun ImageBitmap.Companion.loadImageFile(uri:String,context: Context): ImageBitmap {
    return BitmapFactory.decodeFile(File(context.filesDir,uri).path).asImageBitmap()
}


/**
 * Helper method to validate the asset resource is a supported resource type and returns
 * an ImageBitmap resource. Because this throws exceptions we cannot have this implementation
 * as part of the composable implementation it is invoked in.
 */
private fun loadImageBitmapResource(res: Resources, id: Int): ImageBitmap {
    try {
        return ImageBitmap.imageResource(res, id)
    } catch (throwable: Throwable) {
        throw IllegalArgumentException(throwable.message)
    }
}