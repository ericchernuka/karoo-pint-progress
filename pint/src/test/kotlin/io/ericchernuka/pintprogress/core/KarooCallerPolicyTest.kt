package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KarooCallerPolicyTest {
    @Test
    fun `allows only the Karoo system package`() {
        assertFalse(KarooCallerPolicy.allows(null))
        assertFalse(KarooCallerPolicy.allows(emptyArray()))
        assertFalse(KarooCallerPolicy.allows(arrayOf("com.example.untrusted")))
        assertTrue(
            KarooCallerPolicy.allows(
                arrayOf("com.example.shareduid", KarooCallerPolicy.KAROO_SYSTEM_PACKAGE),
            ),
        )
    }
}
