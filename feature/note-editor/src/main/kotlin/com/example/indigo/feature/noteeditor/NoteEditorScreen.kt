package com.example.indigo.feature.noteeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.*
import com.example.indigo.components.*
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable data class NoteEditorRoute(val noteId: String? = null) : NavKey

fun EntryProviderScope<NavKey>.noteEditorEntry(onBack: () -> Unit) {
    entry<NoteEditorRoute> { route ->
        val viewModel =
            hiltViewModel<NoteEditorViewModel, NoteEditorViewModel.Factory>(
                creationCallback = { it.create(route.noteId?.let(UUID::fromString)) }
            )
        NoteEditorScreen(onBack, viewModel)
    }
}

@Composable
fun NoteEditorScreen(onBack: () -> Unit, viewModel: NoteEditorViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { currentOnBack() }
        }
    }
    NoteEditorContent(state, viewModel::send)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorContent(state: NoteEditorState, onAction: (NoteEditorAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.edit_note else R.string.new_note
                        )
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = { onAction(NoteEditorAction.Cancel) },
                        enabled = !state.isSaving && !state.isLoading,
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                actions = {
                    PrimaryButton(
                        stringResource(R.string.save),
                        { onAction(NoteEditorAction.Save) },
                        enabled = state.canSave,
                        loading = state.isSaving,
                    )
                },
            )
        }
    ) { padding ->
        if (state.isLoading) LoadingIndicator(Modifier.fillMaxSize().padding(padding))
        else
            Column(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.hasError) {
                    ErrorText(stringResource(R.string.editor_error))
                    if (state.loadFailed)
                        TextButton(onClick = { onAction(NoteEditorAction.Retry) }) {
                            Text(stringResource(R.string.retry))
                        }
                }
                if (state.isMissing) ErrorText(stringResource(R.string.missing_note))
                IndigoTextField(
                    state.title,
                    { onAction(NoteEditorAction.TitleChanged(it)) },
                    stringResource(R.string.title),
                    Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && !state.isMissing && !state.loadFailed,
                )
                IndigoTextField(
                    state.body,
                    { onAction(NoteEditorAction.BodyChanged(it)) },
                    stringResource(R.string.content),
                    Modifier.fillMaxWidth().heightIn(min = 240.dp),
                    enabled = !state.isSaving && !state.isMissing && !state.loadFailed,
                    singleLine = false,
                )
                if (state.isEditing)
                    TextButton(
                        onClick = { onAction(NoteEditorAction.Delete) },
                        enabled = !state.isSaving && !state.isMissing && !state.loadFailed,
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                    ) {
                        Text(stringResource(R.string.delete_note))
                    }
            }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditorPreview() {
    IndigoTheme(dynamicColor = false) {
        NoteEditorContent(
            NoteEditorState(
                title = "A first note",
                body = "A few things to remember.",
                isEditing = true,
            ),
            {},
        )
    }
}
