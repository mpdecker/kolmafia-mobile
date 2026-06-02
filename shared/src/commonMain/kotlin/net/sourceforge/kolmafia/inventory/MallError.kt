package net.sourceforge.kolmafia.inventory

sealed class MallError : Exception() {
    object SoldOut : MallError()
    object InsufficientMeat : MallError()
    object StoreClosed : MallError()
    data class Unknown(override val message: String) : MallError()
}
