package com.survive.app.data.maps

import com.survive.app.tools.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCatalogTest {
    // Extrait réel d'une page d'index du serveur Mapsforge.
    private val html = """
<tr><th colspan="5"><hr></th></tr>
<tr><td valign="top"><img src="/icons/back.gif" alt="[PARENTDIR]"></td><td><a href="/maps/v5/">Parent Directory</a>       </td><td>&nbsp;</td><td align="right">  - </td><td>&nbsp;</td></tr>
<tr><td valign="top"><img src="/icons/folder.gif" alt="[DIR]"></td><td><a href="france/">france/</a>                </td><td align="right">2026-10-19 19:20  </td><td align="right">  - </td><td>&nbsp;</td></tr>
<tr><td valign="top"><img src="/icons/unknown.gif" alt="[   ]"></td><td><a href="france.map">france.map</a>             </td><td align="right">2026-10-20 03:10  </td><td align="right">4.8G</td><td>&nbsp;</td></tr>
<tr><td valign="top"><img src="/icons/unknown.gif" alt="[   ]"></td><td><a href="south-africa-and-lesotho.map">south-africa-and-les..&gt;</a></td><td align="right">2026-11-05 23:07  </td><td align="right"> 67M</td><td>&nbsp;</td></tr>
<tr><td valign="top"><img src="/icons/text.gif" alt="[TXT]"></td><td><a href="README.txt">README.txt</a>             </td><td align="right">2026-10-20 03:10  </td><td align="right">1.2K</td><td>&nbsp;</td></tr>
"""

    @Test fun parsesFoldersAndMapsOnly() {
        val entries = MapCatalog.parseIndex(html, "europe/")
        assertEquals(listOf("europe/france/", "europe/france.map", "europe/south-africa-and-lesotho.map"), entries.map { it.path })
        assertTrue(entries[0].isFolder)
        assertEquals("4.8G", entries[1].sizeLabel)
        assertEquals("67M", entries[2].sizeLabel)
        assertEquals("South Africa And Lesotho", entries[2].displayName)
        assertEquals("europe__france.map", entries[1].localFileName)
        val local = LocalMap(java.io.File("europe__france__alsace.map"))
        assertEquals("Alsace", local.name)
        assertEquals("Europe › France", local.region)
    }

    @Test fun parsesSizes() {
        assertEquals(67L * 1024 * 1024, MapCatalog.parseSize("67M"))
        assertEquals((4.8 * 1024 * 1024 * 1024).toLong(), MapCatalog.parseSize("4.8G"))
        assertEquals(null, MapCatalog.parseSize("-"))
    }

    @Test fun formatsSizes() {
        val u = listOf("o", "Ko", "Mo", "Go")
        assertEquals("302 Mo", formatBytes(302L shl 20, u).replace(',', '.'))
        assertEquals("4.8 Go", formatBytes((4.8 * (1L shl 30)).toLong(), u).replace(',', '.'))
        assertEquals("346 Ko", formatBytes(354_495, u))
    }

    @Test fun formatsCoordinates() {
        assertEquals("36.80650, 10.18150", Coordinates.decimal(36.8065, 10.1815))
        assertEquals("48°51'29.6\"N 2°17'40.2\"E", Coordinates.dms(48.858222, 2.2945))
        assertEquals("33°52'07.7\"S 151°12'33.5\"W", Coordinates.dms(-33.8688, -151.2093))
    }
}
