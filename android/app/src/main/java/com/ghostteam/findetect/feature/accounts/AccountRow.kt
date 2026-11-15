package com.ghostteam.findetect.feature.accounts

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinIconTile
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.drawableResource
import com.ghostteam.findetect.core.designsystem.financeIcon
import com.ghostteam.findetect.core.designsystem.nameResource
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.formatMoney
import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.AccountBalance
import com.ghostteam.findetect.domain.model.AppCurrency
import java.math.BigDecimal
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant

/** The row opens editing; its balance has a separate action with a visible 200 ms press highlight. */
@Composable
fun AccountRow(
    balance: AccountBalance,
    onEdit: () -> Unit,
    onBalance: () -> Unit,
    enabled: Boolean = true,
) {
    val account = balance.account
    val locale = LocalConfiguration.current.locales[0]
    val amount = formatMoney(balance.balance, account.currency, locale)
    val action = stringResource(R.string.account_balance_action, account.name, amount)
    val interactions = remember(account.id) { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    var opening by remember(account.id) { mutableStateOf(false) }
    val openBalance by rememberUpdatedState(onBalance)

    // A successful click retains the highlight even when the tap was shorter than one frame.
    // Leaving the composition or disabling the action cancels the delayed navigation.
    LaunchedEffect(account.id, opening, enabled) {
        if (opening) {
            if (enabled) {
                delay(200)
                openBalance()
            }
            opening = false
        }
    }

    val currency = AppCurrency.entries.firstOrNull { it.name == account.currency }
    FinRow(
        title = account.name,
        subtitle = currency?.let { stringResource(it.nameResource) } ?: account.currency,
        onClick = if (enabled && !opening) onEdit else null,
        leading = {
            FinIconTile {
                Icon(painterResource(financeIcon(account.iconId).drawableResource), null,
                    tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp))
            }
        },
        trailing = {
            Box(
                modifier = Modifier.heightIn(min = 48.dp)
                    .semantics { contentDescription = action }
                    .clickable(
                        enabled = enabled && !opening,
                        interactionSource = interactions,
                        indication = null,
                        role = Role.Button,
                        onClick = { opening = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.background(if (enabled && (pressed || opening)) FinTheme.colors.line else FinTheme.colors.white,
                    FinTheme.shapes.tiny).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(amount, style = FinTheme.typography.label, color = FinTheme.colors.navy)
                }
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountRowPreview() {
    val account = Account("preview-account", "Наличные", "wallet", "RUB", BigDecimal("1000.00"),
        Instant.parse("2026-11-14T12:00:00Z"))
    FinDetectTheme {
        AccountRow(AccountBalance(account, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), {}, {})
    }
}
