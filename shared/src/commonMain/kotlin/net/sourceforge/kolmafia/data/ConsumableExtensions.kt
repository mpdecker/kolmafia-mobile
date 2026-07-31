package net.sourceforge.kolmafia.data

fun ConsumableData.isMartini(): Boolean = notes.contains("MARTINI")

fun ConsumableData.isWine(): Boolean = notes.contains("WINE")

fun ConsumableData.isLasagna(): Boolean = notes.contains("LASAGNA")

fun ConsumableData.isPizza(): Boolean = notes.contains("PIZZA")

fun ConsumableData.isBeans(): Boolean = notes.contains("BEANS")

fun ConsumableData.isSaucy(): Boolean = notes.contains("SAUCY")
