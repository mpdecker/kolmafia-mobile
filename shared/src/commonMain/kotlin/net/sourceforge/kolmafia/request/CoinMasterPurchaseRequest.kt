package net.sourceforge.kolmafia.request

/** Desktop [net.sourceforge.kolmafia.request.coinmaster.CoinMasterPurchaseRequest] shop.php. */
object CoinMasterPurchaseRequest {
    fun registerRequest(url: String): Boolean =
        url.contains("shop.php", ignoreCase = true) &&
            url.contains("whichshop=", ignoreCase = true)
}
