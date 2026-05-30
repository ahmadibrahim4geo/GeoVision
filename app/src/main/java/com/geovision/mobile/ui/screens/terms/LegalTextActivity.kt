package com.geovision.mobile.ui.screens.terms

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.geovision.mobile.R
import java.util.Locale

abstract class LegalTextActivity : ComponentActivity() {

    abstract fun provideSections(): List<Pair<String, String>>
    abstract fun provideTitleRes(): Int
    abstract fun provideLogoVisible(): Boolean

    protected var currentLang: String = "ar"

    override fun attachBaseContext(newBase: Context) {
        currentLang = newBase.getSharedPreferences("geovision_prefs", Context.MODE_PRIVATE)
            .getString("language", "ar") ?: "ar"
        val locale = Locale.Builder().setLanguage(currentLang).build()
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        Locale.setDefault(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private fun cardBg(color: Int, radiusDp: Int = 20): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp).toFloat() }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(provideTitleRes())

        val ctx = this
        val prefs = ctx.getSharedPreferences("geovision_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", true)
        val isArabic = currentLang == "ar"

        val bgColor = if (isDark) 0xFF131313.toInt() else 0xFFEEF0F4.toInt()
        val cardColor = if (isDark) 0xFF1C1B1B.toInt() else 0xFFFFFFFF.toInt()
        val headerBg = if (isDark) 0xFF1C1B1B.toInt() else 0xFFFFFFFF.toInt()
        val titleColor = if (isDark) 0xFFE5E2E1.toInt() else 0xFF1C1C1C.toInt()
        val bodyColor = if (isDark) 0xFFC4C6CE.toInt() else 0xFF3D3D3D.toInt()
        val accentColor = if (isDark) 0xFFB0C8EB.toInt() else 0xFF2C4A6E.toInt()
        val dividerColor = if (isDark) 0xFF43474D.toInt() else 0xFFE8E8EE.toInt()
        val cardSubColor = if (isDark) 0xFF2A2A2A.toInt() else 0xFFF8F9FB.toInt()

        val scrollView = ScrollView(ctx).apply { setBackgroundColor(bgColor) }
        val container = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        // ── Header ──
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(headerBg)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            elevation = 4f
        }
        val backBtn = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            val arrowIcon = ImageView(ctx).apply {
                setImageResource(if (isArabic) R.drawable.ic_arrow_big_right else R.drawable.ic_arrow_big_left)
                val size = dp(24)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setColorFilter(accentColor)
            }
            addView(arrowIcon)
            val backLabel = TextView(ctx).apply {
                text = if (isArabic) "تراجع" else "Back"
                textSize = 16f
                setTextColor(accentColor)
                setTypeface(null, Typeface.BOLD)
                setPadding(if (isArabic) dp(6) else 0, 0, 0, 0)
            }
            addView(backLabel)
        }
        header.addView(backBtn)
        val headerTitle = TextView(ctx).apply {
            text = getString(provideTitleRes())
            textSize = 18f
            setTextColor(titleColor)
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(if (isArabic) 0 else dp(8), 0, if (isArabic) dp(8) else 0, 0)
            }
        }
        header.addView(headerTitle)
        container.addView(header)

        // ── Content ──
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }

        // ── Logo (remains centered) ──
        if (provideLogoVisible()) {
            val logo = ImageView(ctx).apply {
                setImageResource(R.drawable.logo)
                layoutParams = LinearLayout.LayoutParams(dp(120), dp(120)).apply {
                    bottomMargin = dp(4)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            content.addView(logo)
            val appName = TextView(ctx).apply {
                text = "GeoVision"
                textSize = 24f
                setTextColor(titleColor)
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = dp(24)
                }
            }
            content.addView(appName)
        }

        // ── Cards ──
        val sections = provideSections()
        val textDir = if (isArabic) TextView.TEXT_DIRECTION_RTL else TextView.TEXT_DIRECTION_LTR
        val textGravity = if (isArabic) Gravity.RIGHT else Gravity.LEFT

        sections.forEach { (sectionTitle, sectionBody) ->
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(18), dp(20), dp(18))
                background = cardBg(cardColor)
                elevation = dp(2).toFloat()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                    clipToOutline = true
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(14) }
            }
            if (sectionTitle.isNotEmpty()) {
                val tv = TextView(ctx).apply {
                    text = sectionTitle
                    textSize = 17f
                    setTextColor(accentColor)
                    setTypeface(null, Typeface.BOLD)
                    textDirection = textDir
                    gravity = textGravity
                    setLineSpacing(0f, 1.3f)
                }
                card.addView(tv)
            }
            if (sectionBody.isNotEmpty()) {
                if (sectionTitle.isNotEmpty()) {
                    val line = View(ctx)
                    line.layoutParams = LinearLayout.LayoutParams(dp(40), dp(1)).apply {
                        topMargin = dp(10)
                        bottomMargin = dp(10)
                    }
                    line.setBackgroundColor(dividerColor)
                    card.addView(line)
                }
                val bv = TextView(ctx).apply {
                    text = sectionBody
                    textSize = 15f
                    setTextColor(bodyColor)
                    textDirection = textDir
                    gravity = textGravity
                    setLineSpacing(0f, 1.6f)
                }
                card.addView(bv)
            }
            content.addView(card)
        }

        container.addView(content)
        scrollView.addView(container)
        setContentView(scrollView)
    }
}
