package kz.chaykin.potracheno.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.CoverColor

// Светлая: малиновый + фиолетовый + мята на розоватом белом. Все surfaceContainer заданы явно —
// иначе Material достроит их серыми, и яркая палитра поблекнет.
internal val LightPrimary = Color(0xFFE6177A)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFFFD9E6)
internal val LightOnPrimaryContainer = Color(0xFF3E0020)
internal val LightSecondary = Color(0xFF6C3BFF)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFE6DEFF)
internal val LightOnSecondaryContainer = Color(0xFF1F0063)
internal val LightTertiary = Color(0xFF00A884)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFB8F5E0)
internal val LightOnTertiaryContainer = Color(0xFF002117)
internal val LightError = Color(0xFFD32F2F)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)
internal val LightBackground = Color(0xFFFFF7FB)
internal val LightOnBackground = Color(0xFF21191D)
internal val LightSurface = Color(0xFFFFF7FB)
internal val LightOnSurface = Color(0xFF21191D)
internal val LightSurfaceVariant = Color(0xFFF4DDE7)
internal val LightOnSurfaceVariant = Color(0xFF564249)
internal val LightOutline = Color(0xFF8A7179)
internal val LightOutlineVariant = Color(0xFFDEC0CA)
internal val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val LightSurfaceContainerLow = Color(0xFFFFF0F6)
internal val LightSurfaceContainer = Color(0xFFFCE8F1)
internal val LightSurfaceContainerHigh = Color(0xFFF7E1EC)
internal val LightSurfaceContainerHighest = Color(0xFFF1DAE6)
internal val LightSurfaceDim = Color(0xFFE8D6DE)
internal val LightSurfaceBright = Color(0xFFFFF7FB)
internal val LightInverseSurface = Color(0xFF372E32)
internal val LightInverseOnSurface = Color(0xFFFDEDF3)
internal val LightInversePrimary = Color(0xFFFF7AB6)

// Тёмная: те же акценты посветлее на «баклажане».
internal val DarkPrimary = Color(0xFFFF7AB6)
internal val DarkOnPrimary = Color(0xFF5C0030)
internal val DarkPrimaryContainer = Color(0xFF8A0049)
internal val DarkOnPrimaryContainer = Color(0xFFFFD9E6)
internal val DarkSecondary = Color(0xFFB7A4FF)
internal val DarkOnSecondary = Color(0xFF2E0A8F)
internal val DarkSecondaryContainer = Color(0xFF4A23C7)
internal val DarkOnSecondaryContainer = Color(0xFFE6DEFF)
internal val DarkTertiary = Color(0xFF4FE0B5)
internal val DarkOnTertiary = Color(0xFF00382A)
internal val DarkTertiaryContainer = Color(0xFF00513E)
internal val DarkOnTertiaryContainer = Color(0xFFB8F5E0)
internal val DarkError = Color(0xFFFF8A95)
internal val DarkOnError = Color(0xFF690005)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val DarkBackground = Color(0xFF140F1A)
internal val DarkOnBackground = Color(0xFFEDE0EA)
internal val DarkSurface = Color(0xFF140F1A)
internal val DarkOnSurface = Color(0xFFEDE0EA)
internal val DarkSurfaceVariant = Color(0xFF4A3F48)
internal val DarkOnSurfaceVariant = Color(0xFFD2C2CC)
internal val DarkOutline = Color(0xFF9B8C96)
internal val DarkOutlineVariant = Color(0xFF4A3F48)
internal val DarkSurfaceContainerLowest = Color(0xFF0E0A13)
internal val DarkSurfaceContainerLow = Color(0xFF1C1623)
internal val DarkSurfaceContainer = Color(0xFF221B2A)
internal val DarkSurfaceContainerHigh = Color(0xFF2C2435)
internal val DarkSurfaceContainerHighest = Color(0xFF372E40)
internal val DarkSurfaceDim = Color(0xFF140F1A)
internal val DarkSurfaceBright = Color(0xFF3B3342)
internal val DarkInverseSurface = Color(0xFFEDE0EA)
internal val DarkInverseOnSurface = Color(0xFF342C38)
internal val DarkInversePrimary = Color(0xFFE6177A)

/** Цвета со смыслом: деньги пришли, ушли, впритык. Своя пара на каждую тему. */
@Immutable
data class BrandColors(
    val positive: Color,
    val negative: Color,
    val warning: Color,
    /** Градиент карточки на главной, если у поездки не выбрана своя обложка. */
    val hero: List<Color>,
)

internal val LightBrand = BrandColors(
    positive = Color(0xFF00A57A),
    negative = Color(0xFFE8334A),
    warning = Color(0xFFE09000),
    hero = listOf(Color(0xFF7B2FF7), Color(0xFFF107A3), Color(0xFFFF8A00)),
)

internal val DarkBrand = BrandColors(
    positive = Color(0xFF4FE0B5),
    negative = Color(0xFFFF8A95),
    warning = Color(0xFFFFC95C),
    hero = listOf(Color(0xFF5B21D1), Color(0xFFC2067F), Color(0xFFE06A00)),
)

@Immutable
class CategoryColors(private val colors: Map<Category, Color>) {
    operator fun get(category: Category): Color = colors.getValue(category)
}

internal val LightCategoryColors = CategoryColors(
    mapOf(
        Category.FOOD to Color(0xFFFF7A00),
        Category.CAFE to Color(0xFFB5651D),
        Category.TRANSPORT to Color(0xFFFFB800),
        Category.HOUSING to Color(0xFF3D5AFE),
        Category.FUN to Color(0xFFF107A3),
        Category.SHOPPING to Color(0xFF7B2FF7),
        Category.SOUVENIRS to Color(0xFFFF4757),
        Category.CONNECTION to Color(0xFF00B8D4),
        Category.HEALTH to Color(0xFF00C389),
        Category.OTHER to Color(0xFF78909C),
    ),
)

internal val DarkCategoryColors = CategoryColors(
    mapOf(
        Category.FOOD to Color(0xFFFFA552),
        Category.CAFE to Color(0xFFE0A36B),
        Category.TRANSPORT to Color(0xFFFFD95A),
        Category.HOUSING to Color(0xFF8C9EFF),
        Category.FUN to Color(0xFFFF6FCF),
        Category.SHOPPING to Color(0xFFB388FF),
        Category.SOUVENIRS to Color(0xFFFF8A95),
        Category.CONNECTION to Color(0xFF62EBFF),
        Category.HEALTH to Color(0xFF5EF0B8),
        Category.OTHER to Color(0xFFB0BEC5),
    ),
)

/**
 * Градиенты обложек поездок. Одинаковые в обеих темах: на карточке всегда белый текст,
 * и контраст с ним важнее, чем подстройка под фон.
 */
fun CoverColor.gradient(): List<Color> = when (this) {
    CoverColor.SUNSET -> listOf(Color(0xFFFF8A00), Color(0xFFF107A3))
    CoverColor.GRAPE -> listOf(Color(0xFF7B2FF7), Color(0xFFF107A3), Color(0xFFFF8A00))
    CoverColor.OCEAN -> listOf(Color(0xFF1FA8E8), Color(0xFF3D5AFE))
    CoverColor.MINT -> listOf(Color(0xFF12B58E), Color(0xFF1FA8E8))
    CoverColor.LEMON -> listOf(Color(0xFFF5B700), Color(0xFFFF6A00))
    CoverColor.CHERRY -> listOf(Color(0xFFFF4757), Color(0xFFC2067F))
    CoverColor.NIGHT -> listOf(Color(0xFF3D5AFE), Color(0xFF2A1454))
    CoverColor.LIME -> listOf(Color(0xFF7CC417), Color(0xFF12B58E))
}

/** Кружки-аватарки людей без фото: цвет по id, чтобы у Васи он не менялся от запуска к запуску. */
internal val AvatarPalette = listOf(
    Color(0xFFFF7A00),
    Color(0xFF7B2FF7),
    Color(0xFF00B8D4),
    Color(0xFFF107A3),
    Color(0xFF00C389),
    Color(0xFF3D5AFE),
    Color(0xFFFF4757),
    Color(0xFFB5651D),
)
