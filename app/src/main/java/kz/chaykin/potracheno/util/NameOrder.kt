package kz.chaykin.potracheno.util

import java.text.Collator
import java.util.Locale

/**
 * Алфавитный порядок считает [Collator], а не SQLite: `COLLATE NOCASE` умеет только латиницу,
 * и «Ёжик» с «ёжиком» в SQL разъезжаются.
 */
object NameOrder {
    private val collator: Collator = Collator.getInstance(Locale.forLanguageTag("ru")).apply {
        strength = Collator.SECONDARY
    }

    val comparator: Comparator<String> = Comparator { left, right -> collator.compare(left, right) }
}
