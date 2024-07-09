package com.tans.tasciiartplayer.ui.audioplayer

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.shape.MaterialShapeDrawable
import com.tans.tuiutils.dialog.createBottomSheetDialog
import com.tans.tuiutils.systembar.SystemBarThemeStyle

fun FragmentActivity.createAudioBottomSheetDialog(contentView: View): Dialog {
    ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(0, 0, 0, systemInsets.bottom + systemInsets.top)
        insets
    }
    val d = this.createBottomSheetDialog(
        contentView = contentView,
        navigationThemeStyle = SystemBarThemeStyle.Light,
        statusBarThemeStyle = SystemBarThemeStyle.Light,
        dimAmount = 0.1f
    ) { b ->
        b.isDraggable = true
        b.isHideable = true
        try {
            val method = BottomSheetBehavior::class.java.getDeclaredMethod("getMaterialShapeDrawable")
            method.isAccessible = true
            val d = method.invoke(b) as MaterialShapeDrawable
            d.fillColor = ColorStateList.valueOf(Color.WHITE)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
//    d.window?.setWindowAnimations(com.tans.tuiutils.R.style.tUiDefaultBottomDialogAnima)
//    val touchOutside = d.findViewById<View>(com.google.android.material.R.id.touch_outside)
//    touchOutside.setBackgroundColor(getColor(R.color.black_transparent1))
    return d
}