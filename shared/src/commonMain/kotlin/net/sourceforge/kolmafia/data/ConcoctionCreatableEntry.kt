package net.sourceforge.kolmafia.data

/** Desktop CreateItemRequest creatableList row — post-refresh creatable + pullable snapshot. */
data class ConcoctionCreatableEntry(
    val resultName: String,
    val itemId: Int,
    val creatable: Int,
    val pullable: Int,
    val methods: Set<String>,
)
