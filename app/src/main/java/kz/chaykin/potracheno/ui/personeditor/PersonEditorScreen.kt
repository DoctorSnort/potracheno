package kz.chaykin.potracheno.ui.personeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.data.repo.PersonRepository
import kz.chaykin.potracheno.model.Person
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.components.ConfirmDialog
import kz.chaykin.potracheno.ui.components.SinglePhotoPicker
import kz.chaykin.potracheno.ui.components.rememberCameraCapture
import kz.chaykin.potracheno.ui.navigation.PersonEditorRoute
import java.io.File

data class PersonEditorState(
    val isNew: Boolean = true,
    val name: String = "",
    val photoFileName: String? = null,
    val nameError: Boolean = false,
    val saving: Boolean = false,
    /** Картинку не удалось прочитать — экран покажет снекбар. */
    val photoFailed: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
)

class PersonEditorViewModel(
    private val personRepository: PersonRepository,
    private val personId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(PersonEditorState(isNew = personId == 0L))
    val state: StateFlow<PersonEditorState> = _state.asStateFlow()

    private var original: Person? = null

    init {
        if (personId != 0L) {
            viewModelScope.launch {
                val person = personRepository.observe(personId).first() ?: return@launch
                original = person
                _state.update { it.copy(isNew = false, name = person.name, photoFileName = person.photoFileName) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = false) }

    fun newCameraTarget(): File = personRepository.newCameraTarget()

    fun addPhotoFromGallery(uri: Uri) = importPhoto { personRepository.importPhoto(uri) }

    fun addPhotoFromCamera(file: File) = importPhoto { personRepository.importPhoto(file) }

    fun onPhotoErrorShown() = _state.update { it.copy(photoFailed = false) }

    /** Облачный файл не скачался или картинка битая — это не повод ронять приложение. */
    private fun importPhoto(read: suspend () -> String) = viewModelScope.launch {
        runCatching { read() }
            .onSuccess { name -> _state.update { it.copy(photoFileName = name) } }
            .onFailure { _state.update { it.copy(photoFailed = true) } }
    }

    fun removePhoto() = _state.update { it.copy(photoFileName = null) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            personRepository.save(
                Person(
                    id = personId,
                    name = current.name.trim(),
                    photoFileName = current.photoFileName,
                    createdAt = original?.createdAt ?: 0L,
                ),
            )
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (personId == 0L) return
        viewModelScope.launch {
            personRepository.delete(personId)
            _state.update { it.copy(isSaved = true, isDeleted = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route: PersonEditorRoute = createSavedStateHandle().toRoute()
                PersonEditorViewModel(appContainer.personRepository, route.personId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditorScreen(
    onDone: (deleted: Boolean) -> Unit,
    viewModel: PersonEditorViewModel = viewModel(factory = PersonEditorViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val cameraUnavailable = stringResource(R.string.photo_camera_unavailable)

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.addPhotoFromGallery(uri)
    }
    val takePhoto = rememberCameraCapture(
        createTarget = viewModel::newCameraTarget,
        onCaptured = viewModel::addPhotoFromCamera,
        onUnavailable = { scope.launch { snackbarHost.showSnackbar(cameraUnavailable) } },
    )

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone(state.isDeleted)
    }

    val photoFailedText = stringResource(R.string.photo_failed)
    LaunchedEffect(state.photoFailed) {
        if (state.photoFailed) {
            snackbarHost.showSnackbar(photoFailedText)
            viewModel.onPhotoErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.person_new else R.string.person_edit)) },
                navigationIcon = {
                    IconButton(onClick = { onDone(false) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(onClick = viewModel::save, enabled = !state.saving) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.person_name)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SinglePhotoPicker(
                fileName = state.photoFileName,
                onPickGallery = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onTakePhoto = takePhoto,
                onRemove = viewModel::removePhoto,
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.person_delete_title),
            text = stringResource(R.string.person_delete_text, state.name),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
