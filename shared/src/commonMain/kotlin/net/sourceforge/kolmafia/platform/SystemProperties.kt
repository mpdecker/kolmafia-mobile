package net.sourceforge.kolmafia.platform

/** Desktop [System.getProperty] for ASH `get_property("System.*")`. */
expect fun systemProperty(name: String): String?
