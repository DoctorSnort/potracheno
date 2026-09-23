package kz.chaykin.potracheno.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kz.chaykin.potracheno.PotrachenoApp
import kz.chaykin.potracheno.di.AppContainer

/** Достаёт контейнер зависимостей внутри `viewModelFactory { initializer { ... } }`. */
val CreationExtras.appContainer: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PotrachenoApp).container
