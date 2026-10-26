package com.darkempire78.opencalculator

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * The two themes of the app. They only change colors and the logo: every screen reads the same theme
 * attributes (res/values/attrs.xml), so the calculators do not know which theme is active.
 *
 * Logos live in res/drawable-nodpi (logo_flamengo.png, logo_cavaliers.png); see docs/TEMAS.md.
 */
enum class ThemeOption(
    val id: String,
    @StringRes val nameRes: Int,
    @StyleRes val style: Int,
    @DrawableRes val logo: Int
) {
    FLAMENGO("FLAMENGO", R.string.theme_flamengo, R.style.Theme_Calc_Flamengo, R.drawable.logo_flamengo),
    CAVALIERS("CAVALIERS", R.string.theme_cavaliers, R.style.Theme_Calc_Cavaliers, R.drawable.logo_cavaliers);

    companion object {
        val DEFAULT = FLAMENGO

        /** Unknown or missing ids (including values saved by older versions) fall back to [DEFAULT]. */
        fun fromId(id: String?): ThemeOption = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

class Themes(private val context: Context) {

    fun getSelected(): ThemeOption = ThemeOption.fromId(MyPreferences(context).appThemeId)

    /** Style resource to pass to setTheme() before inflating any layout. */
    fun getTheme(): Int = getSelected().style

    companion object {

        /**
         * Dialog with both themes: logo, name, a color preview taken from the theme itself and a mark on
         * the one in use. Choosing a theme saves it and recreates the activity, so it applies immediately.
         */
        fun openDialogThemeSelector(context: Context) {
            val preferences = MyPreferences(context)
            val selected = ThemeOption.fromId(preferences.appThemeId)
            lateinit var dialog: androidx.appcompat.app.AlertDialog

            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(context, 20), dp(context, 8), dp(context, 20), 0)
            }
            for (option in ThemeOption.entries) {
                val row = themeRow(context, option, option == selected)
                row.setOnClickListener {
                    dialog.dismiss()
                    if (option != selected) {
                        preferences.appThemeId = option.id
                        (context as? Activity)?.recreate()
                    }
                }
                column.addView(
                    row,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        .apply { bottomMargin = dp(context, 12) }
                )
            }
            dialog = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_general_theme)
                .setView(column)
                .setNegativeButton(R.string.fin_cancel, null)
                .show()
        }

        private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

        private fun themeColor(themed: Context, @AttrRes attr: Int): Int {
            val value = TypedValue()
            themed.theme.resolveAttribute(attr, value, true)
            return value.data
        }

        private fun themeRow(context: Context, option: ThemeOption, isSelected: Boolean): View {
            // resolve the colors from the option's own theme, not from the theme in use
            val themed = ContextThemeWrapper(context, option.style)
            val text = themeColor(themed, R.attr.calc_text_primary)
            val accent = themeColor(themed, R.attr.calc_accent)

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
                minimumHeight = dp(context, 72)
                isClickable = true
                isFocusable = true
                background = GradientDrawable().apply {
                    setColor(themeColor(themed, R.attr.calc_background))
                    cornerRadius = dp(context, 16).toFloat()
                    setStroke(dp(context, if (isSelected) 3 else 1), if (isSelected) accent else themeColor(themed, R.attr.calc_outline))
                }
                contentDescription = context.getString(option.nameRes) +
                    if (isSelected) ", " + context.getString(R.string.theme_selected) else ""
            }

            row.addView(
                ImageView(context).apply {
                    setImageResource(option.logo)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                LinearLayout.LayoutParams(dp(context, 48), dp(context, 56)).apply { marginEnd = dp(context, 14) }
            )

            val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(TextView(context).apply {
                this.text = context.getString(option.nameRes)
                setTextColor(text)
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            })
            val swatches = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (attr in listOf(R.attr.calc_primary, R.attr.calc_accent, R.attr.calc_key_number, R.attr.calc_key_operator, R.attr.calc_key_enter)) {
                swatches.addView(
                    View(context).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(themeColor(themed, attr))
                            setStroke(dp(context, 1), themeColor(themed, R.attr.calc_outline))
                        }
                    },
                    LinearLayout.LayoutParams(dp(context, 22), dp(context, 22)).apply { marginEnd = dp(context, 6); topMargin = dp(context, 6) }
                )
            }
            texts.addView(swatches)
            if (isSelected) {
                texts.addView(TextView(context).apply {
                    this.text = "✓ " + context.getString(R.string.theme_selected)
                    setTextColor(accent)
                    textSize = 13f
                    setPadding(0, dp(context, 6), 0, 0)
                })
            }
            row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            return row
        }
    }
}
