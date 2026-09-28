package kz.chaykin.potracheno.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncAlertPolicyTest {

    @Test
    fun `без человека не починить — будим сразу`() {
        assertTrue(SyncAlertPolicy.shouldNotify(SyncFailureKind.NEEDS_USER, attempt = 0))
    }

    @Test
    fun `мигнувшая сеть — две попытки молчим, на третьей сообщаем`() {
        assertFalse(SyncAlertPolicy.shouldNotify(SyncFailureKind.TRANSIENT, attempt = 0))
        assertFalse(SyncAlertPolicy.shouldNotify(SyncFailureKind.TRANSIENT, attempt = 1))
        assertTrue(SyncAlertPolicy.shouldNotify(SyncFailureKind.TRANSIENT, attempt = 2))
        assertTrue(SyncAlertPolicy.shouldNotify(SyncFailureKind.TRANSIENT, attempt = 7))
    }
}
