package com.tans.tasciiartplayer.glide

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MediaImageModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        // ModelLoader
        registry.replace(
            MediaImageModel::class.java,
            Bitmap::class.java,
            MediaImageModelLoader.Companion.Factory()
        )

        // ResourceDecoder
        registry.append(
            Registry.BUCKET_BITMAP,
            Bitmap::class.java,
            Bitmap::class.java,
            BitmapResourceDecoder()
        )

        // Encoder
        registry.append(Bitmap::class.java, BitmapEncoder(glide.arrayPool))
    }

}