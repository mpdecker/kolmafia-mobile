package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5336–5350 — thin HTTP residual registerRequest hubs (Behavioral Deepen XXIV).
 */

object Crimbo23ElfBarRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo23_elf_bar", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Elf Guard Officers' Club")
        return true
    }
}

object Crimbo23ElfCafeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo23_elf_cafe", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Elf Guard Mess Hall")
        return true
    }
}

object Crimbo23ElfFactoryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo23_elf_factory", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Elf Guard Toy and Munitions Factory")
        return true
    }
}

object Crimbo23PirateArmoryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo23_pirate_armory", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbuccaneer Junkworks")
        return true
    }
}

object Crimbo23PirateBarRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo23_pirate_bar", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbuccaneer Bar")
        return true
    }
}

object Crimbo23PirateCafeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo23_pirate_cafe", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbuccaneer Grub Hall")
        return true
    }
}

object Crimbo23PirateFactoryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo23_pirate_factory", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbuccaneer Foundry")
        return true
    }
}

object Crimbo24BarRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo24_bar", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo24 Bar")
        return true
    }
}

object Crimbo24CafeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo24_cafe", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo24 Cafe")
        return true
    }
}

object Crimbo24FactoryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo24_factory", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo24 Factory")
        return true
    }
}

object FunALogRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=piraterealm", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting PirateRealm Fun-a-Log")
        return true
    }
}

object JunkMagazineRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=junkmagazine", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Worse Homes and Gardens")
        return true
    }
}

object KOLHSArtRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=kolhs_art", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Art Class (After School)")
        return true
    }
}

object KOLHSChemRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=kolhs_chem", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Chemistry Class (After School)")
        return true
    }
}

object KOLHSShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=kolhs_shop", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Shop Class (After School)")
        return true
    }
}
