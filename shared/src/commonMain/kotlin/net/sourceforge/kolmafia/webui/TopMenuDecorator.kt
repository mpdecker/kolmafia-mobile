package net.sourceforge.kolmafia.webui

/** Desktop TopMenuDecorator — inject script/cli/chat/KoLmafia links into top menus. */
object TopMenuDecorator {
    fun decorate(buffer: StringBuilder, location: String) {
        if (location.contains("awesomemenu.php?icons=1")) return
        if (buffer.indexOf("mafia-topmenu") >= 0) return
        val links = buildString {
            append("""<span class="mafia-topmenu">""")
            append(""" [<a href="/cli.html" target="mainpane">gCLI</a>]""")
            append(""" [<a href="/chat.html" target="mainpane">chat</a>]""")
            append(""" [<a href="/KoLmafia/sideCommand?cmd=refresh+status&pwd=MAFIAHIT" target="charpane">refresh</a>]""")
            append("</span>")
        }
        // Prefer inserting after first </td> in the menu table
        val idx = buffer.indexOf("</td>")
        if (idx >= 0) {
            buffer.insert(idx, links)
        } else {
            RequestEditorKit.insertBefore(buffer, "</body>", links)
        }
        // Logout through mafia
        RequestEditorKit.replaceOnce(
            buffer,
            "logout.php",
            "KoLmafia/logout?pwd=MAFIAHIT",
        )
    }
}
