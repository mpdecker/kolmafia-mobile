package net.sourceforge.kolmafia.webui

object UseItemDecorator {
    fun decorate(location: String, buffer: StringBuilder) {
        if (buffer.indexOf("mafia-useitem") >= 0) return
        RequestEditorKit.insertBefore(
            buffer,
            "</body>",
            """<span class="mafia-useitem" data-loc="$location"></span>""",
        )
    }
}

object BasementDecorator {
    fun decorate(buffer: StringBuilder) {
        if (buffer.indexOf("mafia-basement") >= 0) return
        RequestEditorKit.insertBefore(
            buffer,
            "</body>",
            """<div class="mafia-basement"><b>Basement:</b> check HP/MP and elemental resistance before descending.</div>""",
        )
    }
}

object MineDecorator {
    fun decorate(buffer: StringBuilder) {
        MineDecoratorDepth.decorate(buffer)
        if (buffer.indexOf("mafia-mine") < 0) {
            RequestEditorKit.insertBefore(buffer, "</body>", """<span class="mafia-mine"></span>""")
        }
    }
}

object BeerPongDecorator {
    fun decorate(buffer: StringBuilder) {
        if (buffer.indexOf("mafia-beerpong") >= 0) return
        RequestEditorKit.insertBefore(
            buffer,
            "</body>",
            """<div class="mafia-beerpong">Pirate insult retort helper active.</div>""",
        )
    }
}

object IslandDecorator {
    fun decorateBigIsland(location: String, buffer: StringBuilder) {
        if (buffer.indexOf("mafia-island") >= 0) return
        RequestEditorKit.insertBefore(
            buffer,
            "</body>",
            """<div class="mafia-island" data-loc="$location">Island War map helpers.</div>""",
        )
    }
}

object HobopolisDecorator {
    fun decorate(location: String, buffer: StringBuilder) {
        if (buffer.indexOf("mafia-hobopolis") >= 0) return
        RequestEditorKit.insertBefore(
            buffer,
            "</body>",
            """<div class="mafia-hobopolis" data-loc="$location">Hobopolis progress helpers.</div>""",
        )
    }
}

object ValhallaDecorator {
    fun decorateGashJump(location: String, buffer: StringBuilder) {
        if (buffer.indexOf("mafia-valhalla") >= 0) return
        val link =
            """<div class="mafia-valhalla"><a href="/KoLmafia/redirectedCommand?cmd=ascend&pwd=MAFIAHIT">Prepare for ascension (KoLmafia)</a></div>"""
        RequestEditorKit.insertBefore(buffer, "</body>", link)
        // Also near the gash image if present
        val gash = buffer.indexOf("thegash")
        if (gash >= 0) {
            RequestEditorKit.insertBefore(buffer, "</body>", """<!-- mafia-gash $location -->""")
        }
    }
}
