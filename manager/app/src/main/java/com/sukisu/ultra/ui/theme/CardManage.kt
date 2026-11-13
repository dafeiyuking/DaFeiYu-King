package com.sukisu.ultra.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
object CardConfig {
    // 卡片透明度
    var cardAlpha by mutableFloatStateOf(0.62f)
        internal set
    // 卡片亮度
    var cardDim by mutableFloatStateOf(0f)
        internal set
    // 卡片阴影
    var cardElevation by mutableStateOf(0.dp)
        internal set

    // 功能开关
    var isShadowEnabled by mutableStateOf(true)
        internal set
    var isCustomBackgroundEnabled by mutableStateOf(false)
        internal set

    var isCustomAlphaSet by mutableStateOf(false)
        internal set
    var isCustomDimSet by mutableStateOf(false)
        internal set
    var isUserDarkModeEnabled by mutableStateOf(false)
        internal set
    var isUserLightModeEnabled by mutableStateOf(false)
        internal set

    // 配置键名
    private object Keys {
        const val CARD_ALPHA = "card_alpha"
        const val CARD_DIM = "card_dim"
        const val CUSTOM_BACKGROUND_ENABLED = "custom_background_enabled"
        const val IS_SHADOW_ENABLED = "is_shadow_enabled"
        const val IS_CUSTOM_ALPHA_SET = "is_custom_alpha_set"
        const val IS_CUSTOM_DIM_SET = "is_custom_dim_set"
        const val IS_USER_DARK_MODE_ENABLED = "is_user_dark_mode_enabled"
        const val IS_USER_LIGHT_MODE_ENABLED = "is_user_light_mode_enabled"
    }

    fun updateAlpha(alpha: Float, isCustom: Boolean = true) {
        cardAlpha = alpha.coerceIn(0f, 1f)
        if (isCustom) isCustomAlphaSet = true
    }

    fun updateDim(dim: Float, isCustom: Boolean = true) {
        cardDim = dim.coerceIn(0f, 1f)
        if (isCustom) isCustomDimSet = true
    }

    fun updateShadow(enabled: Boolean, elevation: Dp = cardElevation) {
        isShadowEnabled = enabled
        cardElevation = if (enabled) elevation else cardElevation
    }

    fun updateBackground(enabled: Boolean) {
        isCustomBackgroundEnabled = enabled
        // 自定义背景时自动禁用阴影以获得更好的视觉效果
        if (enabled) {
            updateShadow(false)
        }
    }

    fun updateThemePreference(darkMode: Boolean?, lightMode: Boolean?) {
        isUserDarkModeEnabled = darkMode ?: false
        isUserLightModeEnabled = lightMode ?: false
    }

    fun reset() {
        cardAlpha = 1f
        cardDim = 0f
        cardElevation = 0.dp
        isShadowEnabled = true
        isCustomBackgroundEnabled = false
        isCustomAlphaSet = false
        isCustomDimSet = false
        isUserDarkModeEnabled = false
        isUserLightModeEnabled = false
    }

    fun setThemeDefaults(isDarkMode: Boolean) {
        if (!isCustomAlphaSet) {
            updateAlpha(if (isDarkMode) 0.70f else 0.62f, false)
        }
        if (!isCustomDimSet) {
            updateDim(if (isDarkMode) 0.25f else 0f, false)
        }
        // 暗色模式下默认启用轻微阴影
        if (isDarkMode && !isCustomBackgroundEnabled) {
            updateShadow(true, 2.dp)
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("card_settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(Keys.CARD_ALPHA, cardAlpha)
            putFloat(Keys.CARD_DIM, cardDim)
            putBoolean(Keys.CUSTOM_BACKGROUND_ENABLED, isCustomBackgroundEnabled)
            putBoolean(Keys.IS_SHADOW_ENABLED, isShadowEnabled)
            putBoolean(Keys.IS_CUSTOM_ALPHA_SET, isCustomAlphaSet)
            putBoolean(Keys.IS_CUSTOM_DIM_SET, isCustomDimSet)
            putBoolean(Keys.IS_USER_DARK_MODE_ENABLED, isUserDarkModeEnabled)
            putBoolean(Keys.IS_USER_LIGHT_MODE_ENABLED, isUserLightModeEnabled)
            apply()
        }
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("card_settings", Context.MODE_PRIVATE)
        cardAlpha = prefs.getFloat(Keys.CARD_ALPHA, 0.62f).coerceIn(0f, 1f)
        cardDim = prefs.getFloat(Keys.CARD_DIM, 0f).coerceIn(0f, 1f)
        // 是否启用壁纸以 theme_prefs 里实际有没有背景为准，
        // 否则进「更多设置」时这里重新 load() 会把内存里的 true 冲回 false，整个 UI 变回不透明。
        isCustomBackgroundEnabled = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getString("custom_background", null) != null
        isShadowEnabled = prefs.getBoolean(Keys.IS_SHADOW_ENABLED, true)
        isCustomAlphaSet = prefs.getBoolean(Keys.IS_CUSTOM_ALPHA_SET, false)
        isCustomDimSet = prefs.getBoolean(Keys.IS_CUSTOM_DIM_SET, false)
        isUserDarkModeEnabled = prefs.getBoolean(Keys.IS_USER_DARK_MODE_ENABLED, false)
        isUserLightModeEnabled = prefs.getBoolean(Keys.IS_USER_LIGHT_MODE_ENABLED, false)

        // 应用阴影设置
        updateShadow(isShadowEnabled, if (isShadowEnabled) cardElevation else 0.dp)
    }

    @Deprecated("使用 updateShadow 替代", ReplaceWith("updateShadow(enabled)"))
    fun updateShadowEnabled(enabled: Boolean) {
        updateShadow(enabled)
    }
}

object CardStyleProvider {

    @Composable
    fun getCardColors(originalColor: Color) = CardDefaults.cardColors(
        // 用乘法而不是直接覆盖，否则 homeCardColor()/半透明配色里的 alpha 会被 cardAlpha 冲掉
        containerColor = originalColor.copy(alpha = minOf(originalColor.alpha, CardConfig.cardAlpha)),
        contentColor = determineContentColor(originalColor),
        disabledContainerColor = originalColor.copy(alpha = minOf(originalColor.alpha, CardConfig.cardAlpha) * 0.38f),
        disabledContentColor = determineContentColor(originalColor).copy(alpha = 0.38f)
    )

    @Composable
    fun getCardElevation() = CardDefaults.cardElevation(
        defaultElevation = CardConfig.cardElevation,
        pressedElevation = if (CardConfig.isShadowEnabled) {
            (CardConfig.cardElevation.value + 0).dp
        } else 0.dp,
        focusedElevation = if (CardConfig.isShadowEnabled) {
            (CardConfig.cardElevation.value + 0).dp
        } else 0.dp,
        hoveredElevation = if (CardConfig.isShadowEnabled) {
            (CardConfig.cardElevation.value + 0).dp
        } else 0.dp,
        draggedElevation = if (CardConfig.isShadowEnabled) {
            (CardConfig.cardElevation.value + 0).dp
        } else 0.dp,
        disabledElevation = 0.dp
    )

    @Composable
    private fun determineContentColor(originalColor: Color): Color {
        val isDarkTheme = isDarkThemeNow()

        return when {
            ThemeConfig.isThemeChanging -> {
                if (isDarkTheme) Color.White else Color.Black
            }
            CardConfig.isUserLightModeEnabled -> Color.Black
            CardConfig.isUserDarkModeEnabled -> Color.White
            else -> {
                val luminance = originalColor.luminance()
                val threshold = if (isDarkTheme) 0.4f else 0.6f
                if (luminance > threshold) Color.Black else Color.White
            }
        }
    }
}

// 向后兼容
@Composable
fun getCardColors(originalColor: Color) = CardStyleProvider.getCardColors(originalColor)

@Composable
fun getCardElevation() = CardStyleProvider.getCardElevation()
