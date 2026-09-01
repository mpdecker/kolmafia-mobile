package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.request.ClanLogRequest
import net.sourceforge.kolmafia.request.ClanMembersRequest
import net.sourceforge.kolmafia.request.ClanStashRequest

/** Read-only clan CLI orchestration over the canonical [ClanManager] cache. */
open class ClanCliManager(
    private val membersRequest: ClanMembersRequest? = null,
    private val logRequest: ClanLogRequest? = null,
    private val stashRequest: ClanStashRequest? = null,
) {
    data class RefreshResult(
        val members: Boolean,
        val ranks: Boolean,
        val stash: Boolean,
        val log: Boolean,
    ) {
        val succeeded: Boolean get() = members || ranks || stash || log
    }

    fun statusLines(): List<String> {
        val name = ClanManager.getClanName().ifBlank { "(unknown)" }
        val id = ClanManager.getClanId()
        return listOf(
            "Clan: $name (#$id)",
            "Members: ${ClanManager.getMembers().size}",
            "Stash: ${ClanManager.getStash().size} item types" +
                if (ClanManager.isStashRetrieved()) " (cached)" else " (not loaded)",
            "Stash log: ${ClanManager.getStashLog().size} entries",
        )
    }

    suspend fun refresh(): RefreshResult {
        val members = membersRequest?.fetchMembers()?.isSuccess == true
        val ranks = membersRequest?.fetchRanks()?.isSuccess == true
        val stash = stashRequest?.let {
            it.fetchContents()
            ClanManager.isStashRetrieved()
        } == true
        val log = logRequest?.fetch()?.isSuccess == true
        return RefreshResult(members, ranks, stash, log)
    }

    suspend fun snapshot(): List<String> {
        val result = membersRequest?.fetchMembers(detailed = true)
        return listOf(
            "Clan snapshot: ${ClanManager.getClanName().ifBlank { "(unknown)" }} (#${ClanManager.getClanId()})",
            "Members: ${ClanManager.getMembers().size}",
        ) + ClanManager.getMembers().map { member ->
            val title = member.title.takeIf { it.isNotBlank() }?.let { " [$it]" }.orEmpty()
            "${member.name} (#${member.playerId})$title"
        } + result?.exceptionOrNull()?.let { listOf("Snapshot failed: ${it.message ?: "request failed"}") }.orEmpty()
    }

    suspend fun stashLog(): List<String> {
        val result = logRequest?.fetch()
        return if (result?.isSuccess == true) {
            ClanManager.getStashLog().ifEmpty { listOf("Clan stash log is empty.") }
        } else {
            listOf("Unable to refresh clan stash log: ${result?.exceptionOrNull()?.message ?: "request unavailable"}.")
        }
    }
}
