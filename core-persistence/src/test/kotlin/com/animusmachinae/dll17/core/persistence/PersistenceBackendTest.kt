package com.animusmachinae.dll17.core.persistence

import com.animusmachinae.dll17.core.continuity.StorageFault
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistenceBackendTest {

    private val root: File = Files.createTempDirectory("dll17-backend-test").toFile()

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    private fun dir(name: String) = File(root, name).apply { mkdirs() }

    private fun payload(i: Int) = ByteArray(64) { ((i * 7 + it) % 251).toByte() }

    @Test
    fun `acknowledged records survive a close and reopen byte-identically`() {
        val d = dir("roundtrip")
        SegmentedJournalMedium(d).use { medium ->
            for (i in 1..50) medium.append(i.toLong(), payload(i))
        }
        SegmentedJournalMedium(d).use { medium ->
            val records = medium.records()
            assertEquals(50, records.size)
            records.forEach { (sequence, body) ->
                assertTrue(body.contentEquals(payload(sequence.toInt())))
            }
        }
    }

    @Test
    fun `a sequence that does not advance is refused`() {
        SegmentedJournalMedium(dir("single-writer")).use { medium ->
            medium.append(5L, payload(1))
            assertFailsWith<IllegalArgumentException> { medium.append(5L, payload(2)) }
            assertFailsWith<IllegalArgumentException> { medium.append(4L, payload(3)) }
            medium.append(6L, payload(4))
            assertEquals(2, medium.records().size)
        }
    }

    @Test
    fun `a torn tail is dropped and the next append starts from a clean boundary`() {
        val d = dir("torn")
        SegmentedJournalMedium(d).use { medium ->
            for (i in 1..5) medium.append(i.toLong(), payload(i))
        }
        // A header and half a body, with no trailer: a write that reached the
        // device and stopped.
        val journal = File(d, PersistenceBackendContract.JOURNAL_FILE)
        FileChannel.open(journal.toPath(), StandardOpenOption.WRITE, StandardOpenOption.APPEND)
            .use { channel ->
                val body = payload(6)
                val buffer = ByteBuffer.allocate(
                    PersistenceBackendContract.FRAME_HEADER_BYTES + body.size / 2,
                )
                buffer.putInt(PersistenceBackendContract.RECORD_MAGIC)
                buffer.putInt(body.size)
                buffer.putLong(6L)
                buffer.put(body, 0, body.size / 2)
                buffer.flip()
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }

        SegmentedJournalMedium(d).use { medium ->
            assertEquals(5, medium.records().size)
            medium.append(6L, payload(6))
            assertEquals(6, medium.records().size)
        }
        SegmentedJournalMedium(d).use { medium ->
            assertEquals(6, medium.records().size)
            assertTrue(medium.records().last().second.contentEquals(payload(6)))
        }
    }

    @Test
    fun `a corrupt first frame is a fault, never an empty journal`() {
        // The failure mode this guards against is the worst one available: a
        // corrupt installation presenting itself as a device with no organism.
        val d = dir("corrupt-first")
        SegmentedJournalMedium(d).use { medium ->
            for (i in 1..5) medium.append(i.toLong(), payload(i))
        }
        val journal = File(d, PersistenceBackendContract.JOURNAL_FILE)
        val bytes = journal.readBytes()
        bytes[4] = (bytes[4].toInt() xor 0xFF).toByte()
        journal.writeBytes(bytes)

        assertFailsWith<StorageFault> { SegmentedJournalMedium(d).use { it.records() } }
    }

    @Test
    fun `a wrong magic is corruption wherever it appears`() {
        val d = dir("corrupt-magic")
        SegmentedJournalMedium(d).use { medium ->
            for (i in 1..5) medium.append(i.toLong(), payload(i))
        }
        val journal = File(d, PersistenceBackendContract.JOURNAL_FILE)
        val bytes = journal.readBytes()
        bytes[0] = (bytes[0].toInt() xor 0xFF).toByte()
        journal.writeBytes(bytes)
        val failure = assertFailsWith<StorageFault> { SegmentedJournalMedium(d) }
        assertTrue(failure.message!!.contains("corruption rather than a torn tail"))
    }

    @Test
    fun `compaction keeps exactly the survivors and shrinks the journal`() {
        val d = dir("compaction")
        SegmentedJournalMedium(d).use { medium ->
            for (i in 1..100) medium.append(i.toLong(), payload(i))
            val before = medium.usedBytes
            medium.prune(60L)
            val after = medium.records()
            assertEquals(40, after.size)
            assertEquals(61L, after.first().first)
            assertTrue(medium.usedBytes < before)
            // Appending after a compaction must continue from the surviving tail.
            medium.append(101L, payload(101))
            assertEquals(41, medium.records().size)
        }
        SegmentedJournalMedium(d).use { assertEquals(41, it.records().size) }
    }

    @Test
    fun `an unrenamed compaction staging file is discarded rather than adopted`() {
        val d = dir("staging")
        SegmentedJournalMedium(d).use { medium ->
            for (i in 1..5) medium.append(i.toLong(), payload(i))
        }
        File(d, PersistenceBackendContract.JOURNAL_COMPACTION_FILE).writeBytes(ByteArray(512) { 0x7F })
        SegmentedJournalMedium(d).use { medium ->
            assertEquals(5, medium.records().size)
        }
        assertFalse(File(d, PersistenceBackendContract.JOURNAL_COMPACTION_FILE).exists())
    }

    @Test
    fun `an exhausted medium refuses the write and stays readable and compactable`() {
        val d = dir("capacity")
        SegmentedJournalMedium(d, capacityBytes = 4096L).use { medium ->
            var accepted = 0
            assertFailsWith<StorageFault> {
                for (i in 1..1000) {
                    medium.append(i.toLong(), payload(i))
                    accepted++
                }
            }
            assertTrue(accepted in 1..999)
            // The whole point of the emergency reserve: a full journal must still
            // be readable and compactable, or there is no way out but data loss.
            assertEquals(accepted, medium.records().size)
            medium.prune(accepted / 2L)
            assertEquals(accepted - accepted / 2, medium.records().size)
            medium.append(10_000L, payload(1))
        }
    }

    @Test
    fun `the self-test exercises a real write, force and read-back`() {
        SegmentedJournalMedium(dir("selftest")).use { medium ->
            assertTrue(medium.selfTest())
            assertFalse(File(dir("selftest"), "selftest.dll17").exists())
        }
    }

    @Test
    fun `class O batching writes the same frames as class W`() {
        val batched = dir("class-o")
        SegmentedJournalMedium(batched).use { medium ->
            for (i in 1..20) medium.appendWithoutForce(i.toLong(), payload(i))
            medium.forceNow()
        }
        val acknowledged = dir("class-w")
        SegmentedJournalMedium(acknowledged).use { medium ->
            for (i in 1..20) medium.append(i.toLong(), payload(i))
        }
        assertTrue(
            File(batched, PersistenceBackendContract.JOURNAL_FILE).readBytes()
                .contentEquals(File(acknowledged, PersistenceBackendContract.JOURNAL_FILE).readBytes()),
            "the durability class must change when bytes are forced, not what they are",
        )
    }

    @Test
    fun `repeated restarts recover the identical history`() {
        val d = dir("restart")
        SegmentedJournalMedium(d).use { medium ->
            for (i in 1..30) medium.append(i.toLong(), payload(i))
        }
        val counts = (1..8).map { SegmentedJournalMedium(d).use { m -> m.records().size } }
        assertEquals(listOf(30), counts.distinct())
    }
}
