package com.ybhgl.reminder.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.ybhgl.reminder.R
import com.ybhgl.reminder.data.AppThemeOption
import java.lang.ref.WeakReference

object CustomToast {
    const val LENGTH_SHORT = 2000L
    const val LENGTH_LONG = 3500L

    var currentAppTheme: AppThemeOption = AppThemeOption.SYSTEM

    // 弱引用防止内存泄漏
    private var activeToastDialogRef: WeakReference<android.app.Dialog>? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    private val motionInterpolator = PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)

    enum class Type {
        SUCCESS,
        ERROR,
        NORMAL
    }

    fun show(
        context: Context,
        message: String,
        type: Type = Type.NORMAL,
        duration: Long = LENGTH_SHORT,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        val activity = findActivity(context) ?: return

        handler.post {
            if (activity.isFinishing || activity.isDestroyed) return@post

            dismissRunnable?.let { handler.removeCallbacks(it) }
            dismissRunnable = null

            activeToastDialogRef?.get()?.let { oldDialog ->
                try {
                    if (oldDialog.isShowing) {
                        oldDialog.dismiss()
                    }
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    activeToastDialogRef = null
                }
            }

            val density = activity.resources.displayMetrics.density

            val isDark = when (themeOption) {
                AppThemeOption.SYSTEM -> {
                    (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                }
                AppThemeOption.LIGHT -> false
                AppThemeOption.DARK -> true
            }

            val primaryColor = getThemeColor(activity, android.R.attr.colorPrimary, if (isDark) 0xFFD0BCFF.toInt() else 0xFF6650a4.toInt())
            
            val bgColor = if (isDark) {
                getMicaColor(0xFF000000.toInt(), primaryColor, 0.12f, 128)
            } else {
                getMicaColor(0xFFFFFFFF.toInt(), primaryColor, 0.08f, 128)
            }

            val textColor = if (isDark) {
                0xFFF4EFF4.toInt()
            } else {
                0xFF313033.toInt()
            }

            val capsuleBgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100 * density
                setColor(bgColor)
            }

            val toastView = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                
                val padHorizontal = (24 * density).toInt()
                val padVertical = (12 * density).toInt()
                setPadding(padHorizontal, padVertical, padHorizontal, padVertical)

                background = null

                if (type != Type.NORMAL) {
                    val iconView = ImageView(activity).apply {
                        val iconRes = when (type) {
                            Type.SUCCESS -> R.drawable.ic_check_circle_filled
                            Type.ERROR -> R.drawable.ic_error_filled
                            else -> 0
                        }
                        setImageResource(iconRes)
                        val iconSize = (24 * density).toInt()
                        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                            marginEnd = (12 * density).toInt()
                        }
                    }
                    addView(iconView)
                }

                val textView = TextView(activity).apply {
                    text = message
                    setTextColor(textColor)
                    textSize = 14f
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                addView(textView)
            }

            val dialog = android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar).apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }

            dialog.window?.apply {
                setWindowAnimations(0)
                val lp = attributes ?: WindowManager.LayoutParams()
                lp.alpha = 0f
                attributes = lp
            }

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            dialog.setContentView(toastView, params)

            try {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    dialog.show()
                    activeToastDialogRef = WeakReference(dialog)

                    dialog.setOnDismissListener {
                        if (activeToastDialogRef?.get() == dialog) {
                            activeToastDialogRef = null
                        }
                    }

                    dialog.window?.apply {
                        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        setBackgroundDrawable(capsuleBgDrawable)
                        decorView.setPadding(0, 0, 0, 0)
                        
                        val lp = attributes ?: WindowManager.LayoutParams()
                        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
                        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        lp.y = (64 * density).toInt()
                        
                        lp.flags = lp.flags or 
                                   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                                   WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                   
                        lp.alpha = 0f
                        attributes = lp

                        // Android 12+ Backdrop Blur
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                setBackgroundBlurRadius((30 * density).toInt())
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                return@post
            }

            // 进场动画
            dialog.window?.let { window ->
                android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                    setDuration(280)
                    setInterpolator(motionInterpolator)
                    addUpdateListener { animator ->
                        if (activity.isFinishing || activity.isDestroyed || !dialog.isShowing) {
                            cancel()
                            return@addUpdateListener
                        }
                        val animAlpha = animator.animatedValue as Float
                        val currentLp = window.attributes ?: return@addUpdateListener
                        currentLp.alpha = animAlpha
                        window.attributes = currentLp
                    }
                    start()
                }
            }

            // 自动消失动画及处理
            dismissRunnable = Runnable {
                val d = activeToastDialogRef?.get()
                if (d != null && d.isShowing) {
                    d.window?.let { window ->
                        android.animation.ValueAnimator.ofFloat(window.attributes.alpha, 0f).apply {
                            setDuration(240)
                            setInterpolator(motionInterpolator)
                            addUpdateListener { animator ->
                                if (activity.isFinishing || activity.isDestroyed || !d.isShowing) {
                                    cancel()
                                    return@addUpdateListener
                                }
                                val animAlpha = animator.animatedValue as Float
                                val currentLp = window.attributes ?: return@addUpdateListener
                                currentLp.alpha = animAlpha
                                window.attributes = currentLp
                            }
                            addListener(object : android.animation.AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: android.animation.Animator) {
                                    try {
                                        if (!activity.isFinishing && !activity.isDestroyed && d.isShowing) {
                                            d.dismiss()
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    } finally {
                                        if (activeToastDialogRef?.get() == d) {
                                            activeToastDialogRef = null
                                        }
                                    }
                                }
                            })
                            start()
                        }
                    }
                }
            }
            handler.postDelayed(dismissRunnable!!, duration)
        }
    }

    fun show(
        context: Context,
        message: String,
        type: Type = Type.NORMAL,
        duration: Int,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        show(context, message, type, resolveDuration(duration), themeOption)
    }

    fun show(
        context: Context,
        message: String,
        duration: Int,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        show(context, message, Type.NORMAL, resolveDuration(duration), themeOption)
    }

    fun showSuccess(
        context: Context,
        message: String,
        duration: Long = LENGTH_SHORT,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        show(context, message, Type.SUCCESS, duration, themeOption)
    }

    fun showSuccess(
        context: Context,
        message: String,
        duration: Int,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        show(context, message, Type.SUCCESS, resolveDuration(duration), themeOption)
    }

    fun showError(
        context: Context,
        message: String,
        duration: Long = LENGTH_SHORT,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        show(context, message, Type.ERROR, duration, themeOption)
    }

    fun showError(
        context: Context,
        message: String,
        duration: Int,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        show(context, message, Type.ERROR, resolveDuration(duration), themeOption)
    }

    fun showNormal(
        context: Context,
        message: String,
        duration: Long = LENGTH_SHORT,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        show(context, message, Type.NORMAL, duration, themeOption)
    }

    fun showNormal(
        context: Context,
        message: String,
        duration: Int,
        themeOption: AppThemeOption = currentAppTheme
    ) {
        show(context, message, Type.NORMAL, resolveDuration(duration), themeOption)
    }

    private fun resolveDuration(duration: Int): Long {
        return if (duration == Toast.LENGTH_LONG) LENGTH_LONG else LENGTH_SHORT
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun getThemeColor(context: Context, attr: Int, fallbackColor: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        return if (theme.resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                typedValue.data
            } else {
                try {
                    ContextCompat.getColor(context, typedValue.resourceId)
                } catch (e: Exception) {
                    fallbackColor
                }
            }
        } else {
            fallbackColor
        }
    }

    private fun getMicaColor(baseColor: Int, overlayColor: Int, ratio: Float, alpha: Int): Int {
        val redBase = Color.red(baseColor)
        val greenBase = Color.green(baseColor)
        val blueBase = Color.blue(baseColor)

        val redOverlay = Color.red(overlayColor)
        val greenOverlay = Color.green(overlayColor)
        val blueOverlay = Color.blue(overlayColor)

        val r = (redBase * (1f - ratio) + redOverlay * ratio).toInt().coerceIn(0, 255)
        val g = (greenBase * (1f - ratio) + greenOverlay * ratio).toInt().coerceIn(0, 255)
        val b = (blueBase * (1f - ratio) + blueOverlay * ratio).toInt().coerceIn(0, 255)

        return Color.argb(alpha, r, g, b)
    }
}