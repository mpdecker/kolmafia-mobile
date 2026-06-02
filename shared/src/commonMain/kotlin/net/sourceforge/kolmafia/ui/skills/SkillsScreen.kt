package net.sourceforge.kolmafia.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import org.koin.compose.koinInject

private val TABS = listOf("All", "Combat", "Noncombat", "Buff", "Summon", "Passive")

@Composable
fun SkillsScreen() {
    val skillManager: SkillManager = koinInject()
    val state by skillManager.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var castingSkillId by remember { mutableStateOf<Int?>(null) }
    var castError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val filteredSkills = remember(selectedTab, state.skills) {
        state.skills.filter { skill ->
            when (selectedTab) {
                0 -> true
                1 -> skill.type == SkillType.COMBAT
                2 -> skill.type == SkillType.NONCOMBAT
                3 -> skill.type == SkillType.BUFF
                4 -> skill.type == SkillType.SUMMON
                5 -> skill.type == SkillType.PASSIVE
                else -> true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Skills", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp))

        ScrollableTabRow(selectedTabIndex = selectedTab) {
            TABS.forEachIndexed { i, label ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i; castError = null }) {
                    Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                }
            }
        }

        castError?.let { error ->
            Text(error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredSkills, key = { it.id }) { skill ->
                SkillRow(
                    skill = skill,
                    isCasting = castingSkillId == skill.id,
                    onCast = {
                        castError = null
                        castingSkillId = skill.id
                        scope.launch {
                            skillManager.cast(skill, 1).fold(
                                onSuccess = { castingSkillId = null },
                                onFailure = { e ->
                                    castingSkillId = null
                                    castError = "${skill.name}: ${e.message}"
                                }
                            )
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SkillRow(skill: SkillData, isCasting: Boolean, onCast: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(skill.name, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(skill.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (skill.mpCost > 0) {
                    Text("${skill.mpCost} MP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (skill.isActive) {
            Spacer(Modifier.size(8.dp))
            BadgedBox(
                badge = {
                    if (skill.dailyLimit > 0) {
                        Badge {
                            Text("${skill.timesCast}/${skill.dailyLimit}",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            ) {
                Button(
                    onClick = onCast,
                    enabled = !isCasting && skill.canCastMore
                ) {
                    if (isCasting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Cast")
                    }
                }
            }
        }
    }
}
