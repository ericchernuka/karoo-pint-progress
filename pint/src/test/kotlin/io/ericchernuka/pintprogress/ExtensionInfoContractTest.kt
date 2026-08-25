package io.ericchernuka.pintprogress

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ExtensionInfoContractTest {
    @Test
    fun `field labels distinguish the mug from the native count`() {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
        val strings = document.getElementsByTagName("string")
        val values = (0 until strings.length)
            .map(strings::item)
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }

        assertEquals("Pints", values["pint_progress"])
        assertEquals("Pints Count", values["pint_progress_text"])
    }

    @Test
    fun `count field uses Karoo native numeric rendering`() {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/xml/extension_info.xml"))
        val dataTypes = document.getElementsByTagName("DataType")
        val textFields = (0 until dataTypes.length)
            .map(dataTypes::item)
            .filter { it.attributes.getNamedItem("typeId").nodeValue == "pint-progress-text" }

        assertEquals(1, textFields.size)
        assertEquals("false", textFields.single().attributes.getNamedItem("graphical").nodeValue)
    }
}
