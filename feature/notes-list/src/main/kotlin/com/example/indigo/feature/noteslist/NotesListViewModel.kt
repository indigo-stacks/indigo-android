package com.example.indigo.feature.noteslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.indigo.core.model.Note
import com.example.indigo.core.notes.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotesListState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)

sealed interface NotesListAction {
    data class Delete(val id: UUID) : NotesListAction

    data object Retry : NotesListAction

    data object DismissError : NotesListAction
}

@HiltViewModel
class NotesListViewModel @Inject constructor(private val repository: NotesRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(NotesListState())
    val state: StateFlow<NotesListState> = mutableState.asStateFlow()
    private var observation: kotlinx.coroutines.Job? = null

    init {
        observe()
    }

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, hasError = false) }
            repository
                .observeNotes()
                .catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(isLoading = false, hasError = true) }
                }
                .collect { notes ->
                    mutableState.update { it.copy(notes = notes, isLoading = false) }
                }
        }
    }

    fun send(action: NotesListAction) {
        when (action) {
            is NotesListAction.Delete ->
                viewModelScope.launch {
                    try {
                        repository.delete(action.id)
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(hasError = true) }
                    }
                }
            NotesListAction.Retry -> observe()
            NotesListAction.DismissError -> mutableState.update { it.copy(hasError = false) }
        }
    }
}
