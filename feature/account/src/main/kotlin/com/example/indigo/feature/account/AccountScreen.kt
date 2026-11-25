package com.example.indigo.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.indigo.components.ErrorText
import com.example.indigo.components.IndigoTheme
import com.example.indigo.components.PrimaryButton
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
                title = { Text(stringResource(R.string.account_account)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.account_done)) }
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
                val description = stringResource(R.string.account_loading)
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().semantics { contentDescription = description }
                )
            }
            state.profile?.let { profile ->
                Text(stringResource(R.string.account_name, profile.name))
                Text(stringResource(R.string.account_email, profile.email))
                Text(
                    stringResource(
                        R.string.account_verified,
                        stringResource(
                            if (profile.emailVerified) R.string.account_yes else R.string.account_no
                        ),
                    )
                )
            }
            if (state.failed) {
                ErrorText(state.errorMessage ?: stringResource(R.string.account_generic_error))
                TextButton(
                    onClick = { onAction(AccountAction.Retry) },
                    enabled = !state.isLoading && !state.isSigningOut,
                ) {
                    Text(stringResource(R.string.account_retry))
                }
            }
            PrimaryButton(
                stringResource(R.string.account_sign_out),
                { onAction(AccountAction.SignOut) },
                enabled = !state.isSigningOut,
            )
        }
    }
    if (state.confirmSignOut)
        AlertDialog(
            onDismissRequest = { onAction(AccountAction.CancelSignOut) },
            title = { Text(stringResource(R.string.account_sign_out_title)) },
            text = { Text(stringResource(R.string.account_sign_out_warning)) },
            confirmButton = {
                TextButton(onClick = { onAction(AccountAction.ConfirmSignOut) }) {
                    Text(stringResource(R.string.account_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AccountAction.CancelSignOut) }) {
                    Text(stringResource(R.string.account_cancel))
                }
            },
        )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountPreview() {
    IndigoTheme(dynamicColor = false) { AccountContent(AccountState(isLoading = true), {}) }
}
