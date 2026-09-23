package kz.chaykin.potracheno.model

/** Деньги в кошелёк поездки или из него. Долги сюда не относятся: они живут отдельно. */
enum class OperationType {
    TOP_UP,
    EXPENSE,
}
