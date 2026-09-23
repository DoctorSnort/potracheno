# kotlinx.serialization сохраняет метаданные сериализаторов через рефлексию имён классов.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kz.chaykin.potracheno.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class kz.chaykin.potracheno.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Типобезопасные маршруты Navigation Compose ищут классы по полному имени.
# Без этого R8 их переименовывает и приложение падает при старте с
# «Cannot find class with name ...» — в debug-сборке это не воспроизводится
# (у брата «Заказано» так уже падало).
-keep class kz.chaykin.potracheno.ui.navigation.** { *; }

# Перечисления живут в маршрутах, в базе и в резервной копии через valueOf().
-keep class kz.chaykin.potracheno.model.OperationType { *; }
-keep class kz.chaykin.potracheno.model.Category { *; }
-keep class kz.chaykin.potracheno.model.CoverColor { *; }
-keep class kz.chaykin.potracheno.model.LocalRateDirection { *; }
-keepclassmembers enum kz.chaykin.potracheno.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Ответы Google Диска разбираются теми же сериализаторами.
-keepclassmembers class kz.chaykin.potracheno.data.sync.** {
    *** Companion;
}
-keepclasseswithmembers class kz.chaykin.potracheno.data.sync.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager создаёт задачу по имени класса — R8 об этом не догадывается.
-keep class kz.chaykin.potracheno.data.sync.DriveSyncWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
