package com.example.indigo.feature.noteeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.indigo.core.model.Note
import com.example.indigo.core.notes.NotesRepository
import dagger.assisted.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NoteEditorState(
    val title: String = "",
    val body: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val isMissing: Boolean = false,
    val loadFailed: Boolean = false,
) {
    val canSave: Boolean
        get() = title.isNotBlank() && !isLoading && !isSaving && !isMissing && !loadFailed
}

sealed interface NoteEditorAction {
    data class TitleChanged(val value: String) : NoteEditorAction

    data class BodyChanged(val value: String) : NoteEditorAction

    data object Save : NoteEditorAction

    data object Delete : NoteEditorAction

    data object Cancel : NoteEditorAction

    data object Retry : NoteEditorAction
}

sealed interface NoteEditorEvent {
    data object Close : NoteEditorEvent
}

@HiltViewModel(assistedFactory = NoteEditorViewModel.Factory::class)
class NoteEditorViewModel
@AssistedInject
constructor(private val repository: NotesRepository, @Assisted private val noteId: UUID?) :
    ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(noteId: UUID?): NoteEditorViewModel
    }

    private val mutableState =
        MutableStateFlow(NoteEditorState(isEditing = noteId != null, isLoading = noteId != null))
    val state: StateFlow<NoteEditorState> = mutableState.asStateFlow()
    private val eventChannel = Channel<NoteEditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var original: Note? = null

    init {
        if (noteId != null) load()
    }

    private fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, hasError = false, loadFailed = false) }
            try {
                original = repository.observeNote(requireNotNull(noteId)).first()
                mutableState.update {
                    it.copy(
                        title = original?.title.orEmpty(),
                        body = original?.body.orEmpty(),
                        isLoading = false,
                        isMissing = original == null,
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.update {
                    it.copy(isLoading = false, hasError = true, loadFailed = true)
                }
            }
        }
    }

    fun send(action: NoteEditorAction) {
        if (state.value.isSaving || state.value.isLoading) return
        when (action) {
            is NoteEditorAction.TitleChanged ->
                mutableState.update { it.copy(title = action.value) }
            is NoteEditorAction.BodyChanged -> mutableState.update { it.copy(body = action.value) }
            NoteEditorAction.Retry -> if (noteId != null) load()
            NoteEditorAction.Cancel ->
                viewModelScope.launch { eventChannel.send(NoteEditorEvent.Close) }
            NoteEditorAction.Save ->
                if (state.value.canSave && (noteId == null || original != null)) {
                    val draft = state.value
                    persist {
                        val note =
                            original?.copy(
                                title = draft.title.trim(),
                                body = draft.body,
                                updatedAt = Instant.now(),
                            ) ?: Note(title = draft.title.trim(), body = draft.body)
                        repository.upsert(note)
                    }
                }
            NoteEditorAction.Delete ->
                if (original != null) persist { repository.delete(requireNotNull(noteId)) }
        }
    }

    private fun persist(operation: suspend () -> Unit) {
        mutableState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            try {
                operation()
                eventChannel.send(NoteEditorEvent.Close)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.update { it.copy(isSaving = false, hasError = true) }
            }
        }
    }
}
