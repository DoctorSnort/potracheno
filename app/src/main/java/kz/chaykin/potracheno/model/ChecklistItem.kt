package kz.chaykin.potracheno.model

data class ChecklistItem(
    val id: Long = 0,
    /** null — пункт «на все поездки»: паспорт, зарядка, страховка. */
    val tripId: Long?,
    val text: String,
    val done: Boolean = false,
    val createdAt: Long = 0,
) {
    val isUniversal: Boolean get() = tripId == null
}
