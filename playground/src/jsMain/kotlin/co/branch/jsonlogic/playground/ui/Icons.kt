package co.branch.jsonlogic.playground.ui

import kotlinx.browser.document
import org.w3c.dom.Element

/*
 * Drawn, not typed. As characters, ☀ ☽ and ▶ are handed to whichever symbol font the machine
 * reaches for, which settles their weight, their size and on some machines their colour; these
 * three sit beside controls whose weight this page does settle. Each takes its colour from the
 * text colour around it, and its size from its own attributes, since a glyph's two levers —
 * font-size and color — reach only one of those on an <svg>.
 */

/**
 * A disc with eight rays at 45 degrees, each a stub between 1.7 and 2.4 core radii out. They reach
 * past the 16-unit square the shape is centred in, which is what the wider box is for.
 */
internal const val SunIcon =
    """<svg width="20" height="20" viewBox="-2 -2 20 20" aria-hidden="true">""" +
        """<circle cx="8" cy="8" r="3.84" fill="currentColor"/>""" +
        """<path fill="none" stroke="currentColor" stroke-width="1.36" stroke-linecap="round" """ +
        """d="M14.53 8L17.22 8M12.62 12.62L14.52 14.52M8 14.53L8 17.22M3.38 12.62L1.48 14.52""" +
        """M1.47 8L-1.22 8M3.38 3.38L1.48 1.48M8 1.47L8 -1.22M12.62 3.38L14.52 1.48"/></svg>"""

/**
 * One closed crescent: the long way round a disc of radius 6.72, then back along the arc of the
 * circle that bites it. Not two circles filled even-odd — the biting circle is centred outside
 * the disc and reaches well past it, so even-odd would fill that outside part as well.
 */
internal const val MoonIcon =
    """<svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">""" +
        """<path fill="currentColor" d="M7.34 1.31A6.72 6.72 0 1 0 13.97 11.09""" +
        """A6.18 6.18 0 0 1 7.34 1.31Z"/></svg>"""

/** Points right; the stylesheet turns it a quarter clockwise to mark the expanded state. */
internal const val ChevronIcon =
    """<svg class="chevron" width="10" height="10" viewBox="0 0 10 10" aria-hidden="true">""" +
        """<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" """ +
        """stroke-linejoin="round" d="M3.4 2L7.2 5L3.4 8"/></svg>"""

/** Parses one of the icons above. The markup is a literal in this file, never anything read in. */
internal fun icon(markup: String): Element {
    val host = document.createElement("div")
    host.innerHTML = markup

    return host.firstElementChild!!
}
