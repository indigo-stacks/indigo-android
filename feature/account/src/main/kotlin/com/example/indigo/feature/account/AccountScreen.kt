package com.example.indigo.feature.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object AccountRoute : NavKey

@Composable
fun AccountScreen(onBack: () -> Unit = {}, viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountContent(state, viewModel::send, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountContent(
    state: AccountState,
    onAction: (AccountAction) -> Unit,
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.done)) }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoading || state.isSigningOut) {
                val description = stringResource(R.string.loading)
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().semantics { contentDescription = description }
                )
            }
            state.profile?.let { profile ->
                Text(stringResource(R.string.name, profile.name))
                Text(stringResource(R.string.email, profile.email))
                Text(
                    stringResource(
                        R.string.verified,
                        stringResource(if (profile.emailVerified) R.string.yes else R.string.no),
                    )
                )
            }
            if (state.failed) {
                Text(
                    state.errorMessage ?: stringResource(R.string.generic_error),
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(
                    onClick = { onAction(AccountAction.Retry) },
                    enabled = !state.isLoading && !state.isSigningOut,
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
            Button(onClick = { onAction(AccountAction.SignOut) }, enabled = !state.isSigningOut) {
                Text(stringResource(R.string.sign_out))
            }
        }
    }
    if (state.confirmSignOut)
        AlertDialog(
            onDismissRequest = { onAction(AccountAction.CancelSignOut) },
            title = { Text(stringResource(R.string.sign_out_title)) },
            text = { Text(stringResource(R.string.sign_out_warning)) },
            confirmButton = {
                TextButton(onClick = { onAction(AccountAction.ConfirmSignOut) }) {
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AccountAction.CancelSignOut) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
}

@Preview
@Composable
private fun AccountPreview() {
    MaterialTheme { AccountContent(AccountState(isLoading = true), {}) }
}
