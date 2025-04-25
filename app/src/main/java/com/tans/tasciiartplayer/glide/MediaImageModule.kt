package com.tans.tasciiartplayer.glide

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.tans.tasciiartplayer.App
import java.io.InputStream

@GlideModule
class MediaImageModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        // OkHttp
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(App.okhttpClient)
        )

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