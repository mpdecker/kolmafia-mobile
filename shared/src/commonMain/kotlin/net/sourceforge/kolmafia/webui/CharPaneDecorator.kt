package net.sourceforge.kolmafia.webui

/** Desktop CharPaneDecorator — inject effects/counters QoL into charpane HTML. */
object CharPaneDecorator {
    fun decorate(buffer: StringBuilder) {
        // Mark relay-decorated charpane; inject counter/status tip block before </body>
        if (buffer.indexOf("mafia-charpane") >= 0) return
        val tip = """<div class="mafia-charpane" id="mafia-charpane-decorated"></div>"""
        val bodyClose = buffer.lastIndexOf("</body>")
        if (bodyClose >= 0) {
            buffer.insert(bodyClose, tip)
        } else {
            buffer.append(tip)
        }
    }
}
