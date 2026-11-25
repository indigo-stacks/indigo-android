package com.example.indigo.feature.noteslist

import android.text.format.DateUtils
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.*
import com.example.indigo.components.*
import com.example.indigo.core.model.Note
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable data object NotesListRoute : NavKey

fun EntryProviderScope<NavKey>.notesListEntry(
    onAccountClick: () -> Unit,
    onNewNote: () -> Unit,
    onOpenNote: (UUID) -> Unit,
) {
    entry<NotesListRoute> { NotesListScreen(onAccountClick, onNewNote, onOpenNote) }
}

@Composable
fun NotesListScreen(
    onAccountClick: () -> Unit = {},
    onNewNote: () -> Unit = {},
    onOpenNote: (UUID) -> Unit = {},
    viewModel: NotesListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotesListContent(state, viewModel::send, onAccountClick, onNewNote, onOpenNote)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListContent(
    state: NotesListState,
    onAction: (NotesListAction) -> Unit,
    onAccountClick: () -> Unit,
    onNewNote: () -> Unit,
    onOpenNote: (UUID) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_title)) },
                actions = {
                    IconButton(onClick = onAccountClick) {
                        Icon(Icons.Default.AccountCircle, stringResource(R.string.account))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewNote,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.new_note)) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.hasError) {
                ErrorText(
                    stringResource(R.string.notes_error),
                    Modifier.padding(horizontal = 16.dp),
                )
                TextButton(onClick = { onAction(NotesListAction.Retry) }) {
                    Text(stringResource(R.string.retry))
                }
            }
            when {
                state.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
                state.notes.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            stringResource(R.string.no_notes),
                            stringResource(R.string.no_notes_description),
                        )
                    }
                else ->
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(state.notes, key = { it.id.toString() }) { note ->
                            NoteRow(
                                note,
                                { onOpenNote(note.id) },
                                { onAction(NotesListAction.Delete(note.id)) },
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                    }
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val actionsLabel = stringResource(R.string.note_actions)
    val deleteLabel = stringResource(R.string.delete_note)
    Row(
        Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true },
                onLongClickLabel = actionsLabel,
            )
            .semantics {
                customActions =
                    listOf(
                        CustomAccessibilityAction(deleteLabel) {
                            onDelete()
                            true
                        }
                    )
            }
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                note.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.body.isNotEmpty())
                Text(
                    note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            Text(
                relativeDate(note.updatedAt ?: note.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, actionsLabel) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(deleteLabel) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun relativeDate(time: Instant): String {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = Instant.now()
        }
    }
    val minutes = Duration.between(time, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.just_now)
        minutes < 60 ->
            pluralStringResource(R.plurals.minutes_ago, minutes.toInt(), minutes.toInt())
        minutes < 1440 ->
            (minutes / 60).toInt().let { pluralStringResource(R.plurals.hours_ago, it, it) }
        minutes < 10080 ->
            (minutes / 1440).toInt().let { pluralStringResource(R.plurals.days_ago, it, it) }
        else ->
            DateUtils.formatDateTime(
                androidx.compose.ui.platform.LocalContext.current,
                time.toEpochMilli(),
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
            )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotesPreview() {
    IndigoTheme(dynamicColor = false) {
        NotesListContent(
            NotesListState(
                notes = listOf(Note(title = "A first note", body = "A few things to remember.")),
                isLoading = false,
            ),
            {},
            {},
            {},
            {},
        )
    }
}
