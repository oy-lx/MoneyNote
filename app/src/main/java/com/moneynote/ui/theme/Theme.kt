package com.moneynote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Color(0xFF4C6359),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9DC),
    onSecondaryContainer = Color(0xFF092017),
    tertiary = Color(0xFF3F6375),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC2E8FC),
    onTertiaryContainer = Color(0xFF001E2B),
    background = NeutralLightBg,
    onBackground = Color(0xFF171D1A),
    surface = NeutralLightSurface,
    onSurface = Color(0xFF171D1A),
    surfaceVariant = NeutralLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C2),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal10,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Color(0xFFB3CCC1),
    onSecondary = Color(0xFF1F352C),
    secondaryContainer = Color(0xFF354B42),
    onSecondaryContainer = Color(0xFFCFE9DC),
    tertiary = Color(0xFFA7CCE0),
    onTertiary = Color(0xFF093544),
    tertiaryContainer = Color(0xFF264B5C),
    onTertiaryContainer = Color(0xFFC2E8FC),
    background = NeutralDarkBg,
    onBackground = Color(0xFFDEE4DF),
    surface = NeutralDarkSurface,
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = NeutralDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3F4945),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** 收支语义色随亮暗主题切换，避免暗色下红绿刺眼。 */
data class LedgerColors(
    val expense: Color,
    val income: Color,
    val warn: Color,
)

private val LocalLedgerColors = staticCompositionLocalOf {
    LedgerColors(expense = ExpenseRed, income = IncomeGreen, warn = WarnAmber)
}

object LedgerTheme {
    val colors: LedgerColors
        @Composable get() = LocalLedgerColors.current
}

@Composable
fun MoneyNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val ledgerColors = if (darkTheme) {
        LedgerColors(expense = ExpenseRedDark, income = IncomeGreenDark, warn = WarnAmber)
    } else {
        LedgerColors(expense = ExpenseRed, income = IncomeGreen, warn = WarnAmber)
    }

    CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
