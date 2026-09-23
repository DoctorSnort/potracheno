package kz.chaykin.potracheno

import android.app.Application
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.di.AppContainer

class PotrachenoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Снимки, сделанные в редакторе человека и брошенные без сохранения, убираются при старте:
        // в момент выхода с экрана делать это уже некому.
        container.applicationScope.launch {
            container.personRepository.discardUnsavedPhotos()
        }
    }
}
