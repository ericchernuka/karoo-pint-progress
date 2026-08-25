package io.ericchernuka.pintprogress

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ExtensionInfoContractTest {
    @Test
    fun `text field uses Karoo native numeric rendering`() {
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
