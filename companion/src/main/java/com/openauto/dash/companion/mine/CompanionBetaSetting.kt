package com.openauto.dash.companion.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.openauto.dash.companion.CompanionColors
import com.openauto.dash.companion.CompanionUpdate
import com.openauto.dash.companion.Edition
import com.openauto.dash.companion.Panel
import com.openauto.dash.companion.R

@Composable
internal fun CompanionBetaSetting() {
    if (Edition.play) return
    val context = LocalContext.current
    var beta by remember { mutableStateOf(CompanionUpdateChannel.beta(context)) }
    val checked by CompanionUpdate.checked.collectAsState()
    val status by CompanionUpdate.status.collectAsState()
    val label = stringResource(R.string.mine_update_beta)
    Panel {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.mine_update_beta_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = CompanionColors.Muted
                )
            }
            Switch(
                checked = beta,
                enabled = checked != CompanionUpdate.Check.Checking && status !is CompanionUpdate.Status.Downloading,
                onCheckedChange = {
                    CompanionUpdateChannel.setBeta(context, it)
                    beta = it
                    CompanionUpdate.channelChanged(context)
                },
                modifier = Modifier.semantics { contentDescription = label }
            )
        }
    }
}
