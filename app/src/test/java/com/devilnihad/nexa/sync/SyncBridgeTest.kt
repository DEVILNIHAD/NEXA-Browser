package com.devilnihad.nexa.sync
import com.devilnihad.nexa.sync.core.SyncBridge
import com.devilnihad.nexa.sync.core.SyncDataObserver
import com.devilnihad.nexa.sync.model.BookmarkPayload
import com.devilnihad.nexa.sync.model.SyncEntityType
import com.devilnihad.nexa.sync.model.SyncOpType
import com.devilnihad.nexa.sync.model.SyncOperation
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SyncBridgeTest {

    @Test
    fun testSyncBridgeObserverReceivesMutation() {
        val bridge = SyncBridge.getInstance()
        val latch = CountDownLatch(1)
        var receivedOp: SyncOperation? = null

        val observer = object : SyncDataObserver {
            override fun onBookmarkMutation(operation: SyncOperation) {
                receivedOp = operation
                latch.countDown()
            }
        }

        bridge.registerObserver(observer)

        val op = SyncOperation(
            opId = "test_op_1",
            opType = SyncOpType.CREATE,
            entityType = SyncEntityType.BOOKMARK,
            entityId = "bmk_1",
            hlc = bridge.clock.now(),
            bookmarkPayload = BookmarkPayload(title = "Test Bookmark", url = "https://example.com")
        )

        bridge.recordBookmarkMutation(op)
        latch.await(2, TimeUnit.SECONDS)

        assertNotNull(receivedOp)
        assertEquals("bmk_1", receivedOp?.entityId)
        assertEquals("Test Bookmark", receivedOp?.bookmarkPayload?.title)

        bridge.unregisterObserver(observer)
    }

    @Test
    fun testClockMonotonicity() {
        val bridge = SyncBridge.getInstance()
        val t1 = bridge.clock.now()
        val t2 = bridge.clock.now()

        assertTrue(t2 > t1 || (t2.physicalTime == t1.physicalTime && t2.counter > t1.counter))
    }
}
