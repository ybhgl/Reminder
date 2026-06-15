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
import androidx.core.content.ContextCompat
import com.ybhgl.reminder.R
import java.lang.ref.WeakReference

object CustomToast {
    const val LENGTH_SHORT = 2000L
    const val LENGTH_LONG = 3500L

    // 使用弱引用持有当前处于活动状态的 Dialog，从根本上杜绝单例持有宿主 Activity 导致的内存泄漏
    private var activeToastDialogRef: WeakReference<android.app.Dialog>? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    // Fine-tuned Cubic Bezier Interpolator matching cubic-bezier(0.4, 0, 0.2, 1) for premium "快进慢出" transitions
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
        duration: Long = LENGTH_SHORT
    ) {
        // Find the active activity context to attach the dialog window
        val activity = findActivity(context) ?: return

        // Run on the main thread safely
        handler.post {
            if (activity.isFinishing || activity.isDestroyed) return@post

            // 1. 立即清除上一个挂起的 dismiss 任务，并在 show 前安全地 dismiss 历史 Dialog，防止 Window 泄漏
            dismissRunnable?.let { handler.removeCallbacks(it) }
            dismissRunnable = null

            activeToastDialogRef?.get()?.let { oldDialog ->
                try {
                    if (oldDialog.isShowing) {
                        oldDialog.dismiss()
                    }
                } catch (e: Exception) {
                    // Ignore window attachment crashes
                } finally {
                    activeToastDialogRef = null
                }
            }

            // 提取 density 以复用
            val density = activity.resources.displayMetrics.density

            // 2. 移除原 runBlocking { themeOptionFlow(activity).first() } 的磁盘 I/O 阻塞。
            // 宿主 Activity 的配置已完全同步应用了暗色/亮色状态，此处直接通过 Activity Configuration
            // 进行无阻塞、秒级读取，100% 避免主线程因 I/O 造成卡顿与 ANR 隐患。
            val isDark = (activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

            // 3. Background Color setting:
            // "背景需实现类似 Windows Mica Alt 的质感，即黑白半透明色调结合深层背景模糊效果（Backdrop Blur）"
            // "无边框"
            val primaryColor = getThemeColor(activity, android.R.attr.colorPrimary, if (isDark) 0xFFD0BCFF.toInt() else 0xFF6650a4.toInt())
            
            // To achieve the perfect Glassmorphism frosted glass effect:
            // We use 50% opacity (alpha = 128) mixed with 12% primary in dark mode and 8% in light mode.
            val bgColor = if (isDark) {
                getMicaColor(0xFF000000.toInt(), primaryColor, 0.12f, 128)
            } else {
                getMicaColor(0xFFFFFFFF.toInt(), primaryColor, 0.08f, 128)
            }

            // High contrast text/icon colors:
            val textColor = if (isDark) {
                0xFFF4EFF4.toInt() // Soft white for M3 Dark theme
            } else {
                0xFF313033.toInt() // Soft charcoal for M3 Light theme
            }

            // 4. Create the single capsule-shaped background drawable
            // This is set on the Dialog Window itself to perfectly wrap and clip blur to the capsule shape
            val capsuleBgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100 * density // Perfect capsule shape (Pill)
                setColor(bgColor)
            }

            // Create the Toast view container (fully transparent background, background is drawn by the window instead to prevent double-layer)
            val toastView = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                
                // Capsule horizontal padding is wider (24dp) for a sleek pill layout
                val padHorizontal = (24 * density).toInt()
                val padVertical = (12 * density).toInt()
                setPadding(padHorizontal, padVertical, padHorizontal, padVertical)

                background = null // Fully transparent container! No double background!

                // A) Icon
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

                // B) Text
                val textView = TextView(activity).apply {
                    text = message
                    setTextColor(textColor)
                    textSize = 14f // sp (M3 Body Medium)
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                addView(textView)
            }

            // 5. Create a Toast Dialog with transparent window parameters
            val dialog = android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar).apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }

            // 彻底禁用系统 Dialog 内置动画，并预设窗口完全透明，防止在 dialog.show() 一瞬间产生闪烁或跳变
            dialog.window?.apply {
                setWindowAnimations(0)
                val lp = attributes ?: WindowManager.LayoutParams()
                lp.alpha = 0f
                attributes = lp
            }

            // Set content view inside dialog
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            dialog.setContentView(toastView, params)

            // Show Dialog safely
            try {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    dialog.show()
                    activeToastDialogRef = WeakReference(dialog)

                    // 监听 Dialog 的 Dismiss 事件以在销毁时立即置空，避免任何可能的闭包/实例残留泄露
                    dialog.setOnDismissListener {
                        if (activeToastDialogRef?.get() == dialog) {
                            activeToastDialogRef = null
                        }
                    }

                    // CRUCIAL: Apply all window layout parameters AFTER calling show()!
                    dialog.window?.apply {
                        // Clear dimming background
                        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        
                        // Set the capsule drawable directly as the window background.
                        setBackgroundDrawable(capsuleBgDrawable)
                        
                        // Clear default margins and padding to ensure absolute centering and WRAP_CONTENT wrapping
                        decorView.setPadding(0, 0, 0, 0)
                        
                        val lp = attributes ?: WindowManager.LayoutParams()
                        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
                        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        lp.y = (64 * density).toInt() // Float bottom offset
                        
                        // Pass touches through the Dialog to the Activity below (act exactly like Toast)
                        lp.flags = lp.flags or 
                                   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                                   WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                   
                        // 确保启动动画前的初始透明度依旧是完全透明 (0)
                        lp.alpha = 0f
                        attributes = lp

                        // Windows Mica Alt Backdrop Blur (Android 12+ / API 31+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                setBackgroundBlurRadius((30 * density).toInt()) // Deep Backdrop Blur
                            } catch (e: Exception) {
                                // Robust fail-safe fallback
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                return@post
            }

            // 6. Unified Enter Animation (使用 ValueAnimator 线性更新 window.attributes.alpha)
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

            // 7. Schedule Dismissal (在 ValueAnimator 归 0 之后才安全 dismiss 视图，保证背景与文字 100% 同步线性淡出)
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
                                        // Robust fail-safe fallback
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

    // 重载方法支持原系统的 Toast Int 时长 (Toast.LENGTH_SHORT / Toast.LENGTH_LONG)
    fun show(context: Context, message: String, type: Type = Type.NORMAL, duration: Int) {
        show(context, message, type, resolveDuration(duration))
    }

    fun show(context: Context, message: String, duration: Int) {
        show(context, message, Type.NORMAL, resolveDuration(duration))
    }

    // 辅助调用方法以保持完全向后兼容
    fun showSuccess(context: Context, message: String, duration: Long = LENGTH_SHORT) {
        show(context, message, Type.SUCCESS, duration)
    }

    fun showSuccess(context: Context, message: String, duration: Int) {
        show(context, message, Type.SUCCESS, resolveDuration(duration))
    }

    fun showError(context: Context, message: String, duration: Long = LENGTH_SHORT) {
        show(context, message, Type.ERROR, duration)
    }

    fun showError(context: Context, message: String, duration: Int) {
        show(context, message, Type.ERROR, resolveDuration(duration))
    }

    fun showNormal(context: Context, message: String, duration: Long = LENGTH_SHORT) {
        show(context, message, Type.NORMAL, duration)
    }

    fun showNormal(context: Context, message: String, duration: Int) {
        show(context, message, Type.NORMAL, resolveDuration(duration))
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