package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.crypto.CanonicalCodecException
import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305

/** An unrecoverable durability condition. Maps to `STORAGE_FAULT`. */
public class StorageFault(message: String) : RuntimeException(message)

/** A write that started and did not complete. Nothing is acknowledged. */
public class InterruptedWrite(message: String) : RuntimeException(message)

/**
 * The durable medium.
 *
 * Deliberately minimal. Every rule in `ContinuityDurabilityContractV1` is
 * expressed in terms of "a write that was acknowledged" and nothing else, so
 * this interface is what any storage engine has to provide and no more. Choosing
 * Room, SQLite or a file format is out of the contract's scope and out of this
 * interface's.
 */
public interface DurableMedium {
    public val capacityBytes: Long
    public val usedBytes: Long

    /** Appends and returns the assigned sequence, or throws. */
    public fun append(sequence: Long, record: ByteArray)

    /** Every acknowledged record, in sequence order. */
    public fun records(): List<Pair<Long, ByteArray>>

    /** Drops records at or below [throughSequence]. */
    public fun prune(throughSequence: Long)

    /** Durability self-test, required before leaving `STORAGE_FAULT`. */
    public fun selfTest(): Boolean
}

/**
 * In-memory medium with explicit fault injection.
 *
 * The fault switches exist because every interesting durability property is a
 * statement about a failure, and a failure that can only be produced by
 * physically filling a device is a property nobody re-tests.
 */
public class InMemoryDurableMedium(
    override val capacityBytes: Long = ContinuityContract.JOURNAL_BYTE_BUDGET,
) : DurableMedium {

    private val stored: LinkedHashMap<Long, ByteArray> = LinkedHashMap()
    private var used: Long = 0L

    /** Fails the next append outright. Models a rejected write. */
    public var failNextAppend: Boolean = false

    /** Writes a truncated record and fails. Models a torn tail. */
    public var interruptNextAppend: Boolean = false

    /** Refuses every operation, including the self-test. */
    public var faulted: Boolean = false

    override val usedBytes: Long get() = used

    override fun append(sequence: Long, record: ByteArray) {
        if (faulted) throw StorageFault("medium is faulted")
        if (failNextAppend) {
            failNextAppend = false
            throw StorageFault("simulated write failure at sequence $sequence")
        }
        if (used + record.size > capacityBytes) {
            throw StorageFault(
                "capacity exhausted: ${used + record.size} bytes required, $capacityBytes available",
            )
        }
        if (interruptNextAppend) {
            interruptNextAppend = false
            // A torn tail: some bytes reached the medium, the write never
            // completed, and nothing acknowledged it. Recovery must treat this
            // as absent rather than as data.
            val torn = record.copyOf(record.size / 2)
            stored[sequence] = torn
            used += torn.size
            throw InterruptedWrite("write interrupted at sequence $sequence")
        }
        stored[sequence] = record.copyOf()
        used += record.size
    }

    override fun records(): List<Pair<Long, ByteArray>> =
        stored.entries.sortedBy { it.key }.map { it.key to it.value.copyOf() }

    override fun prune(throughSequence: Long) {
        val doomed = stored.keys.filter { it <= throughSequence }
        for (key in doomed) {
            used -= stored.getValue(key).size
            stored.remove(key)
        }
    }

    override fun selfTest(): Boolean = !faulted

    /**
     * Accounts for history the medium already holds.
     *
     * Storage pressure is a property of a nearly full medium, and filling eight
     * megabytes one record at a time to reach it would make the pressure
     * fixtures slow enough that nobody runs them. This models the prior history
     * without fabricating records that recovery would then have to read.
     */
    public fun simulateAdditionalUsage(bytes: Long) {
        if (bytes < 0L) throw IllegalArgumentException("usage must not be negative")
        used += bytes
    }

    /** Flips one bit of an already-stored record. Models silent corruption. */
    public fun corrupt(sequence: Long, byteIndex: Int) {
        val record = stored[sequence] ?: throw IllegalArgumentException("no record $sequence")
        record[byteIndex] = (record[byteIndex].toInt() xor 0x01).toByte()
    }
}

/**
 * The device key container, contract section 13.5.
 *
 * Provider-neutral on purpose. Android Keystore is one implementation and is
 * **not** canonical authority: canonical correctness must be provable in a JVM
 * test with no Keystore anywhere near it.
 */
public interface KeyContainer {

    /**
     * The **wrapping** epoch: how many times the container material that protects
     * the data key has been rotated. Amended by
     * `LocalStorageCryptographyContractV2`.
     *
     * This value has no part in record encryption. Conflating it with the data
     * key's identity is exactly the defect V2 corrects: a wrapping rotation
     * changes only how the DEK is stored, so binding record nonces and AAD to it
     * orphaned the entire journal on rotation.
     */
    public val keyEpoch: Int

    /**
     * The identity of the data-encryption key itself.
     *
     * Stable across ordinary wrapping rotations, because the DEK is unchanged by
     * them. It advances only under an actual DEK rotation, which is a separate
     * capability that is not implemented and is not authorized here.
     */
    public val dataKeyId: Int get() = INITIAL_DATA_KEY_ID

    public val deviceFingerprint: Long

    /** Returns the 32-byte data key, or throws if the container cannot unwrap it. */
    public fun dataKey(): ByteArray

    public companion object {
        /** The data-key identity every organism is born with. */
        public const val INITIAL_DATA_KEY_ID: Int = 1
    }
}

/** A key container backed by an in-process key. Test and desktop use only. */
public class InMemoryKeyContainer(
    override val keyEpoch: Int,
    override val deviceFingerprint: Long,
    private val key: ByteArray,
    override val dataKeyId: Int = KeyContainer.INITIAL_DATA_KEY_ID,
) : KeyContainer {
    init {
        if (key.size != ChaCha20Poly1305.KEY_SIZE) {
            throw IllegalArgumentException("data key must be ${ChaCha20Poly1305.KEY_SIZE} bytes")
        }
    }

    /** Set to simulate a container that exists but cannot unwrap — a copied database. */
    public var refuseUnwrap: Boolean = false

    override fun dataKey(): ByteArray {
        if (refuseUnwrap) throw StorageFault("key container refused to unwrap the data key")
        return key.copyOf()
    }
}

/** One decoded durable record. */
public class DurableRecord(
    public val sequence: Long,
    public val generationId: Long,
    public val schemaId: Int,
    public val schemaVersion: Int,
    public val payload: ByteArray,
)

/**
 * The encrypted-record boundary, contract section 13.
 *
 * Encryption sits below the canonical byte layer and above the medium.
 * Canonical hashing and replay operate on the plaintext this class hands back,
 * so nothing about determinism depends on a key — which is the whole reason the
 * boundary is here and not inside the state hash.
 */
public class EncryptedRecordStore(
    private val medium: DurableMedium,
    private val keys: KeyContainer,
    private val organismId: Long,
) {
    public val usedBytes: Long get() = medium.usedBytes
    public val capacityBytes: Long get() = medium.capacityBytes

    /**
     * Header is plaintext: the AAD and nonce must be derivable before decryption.
     *
     * The trailing `u32` is the record's **encryption context** — the identity of
     * the data key it was sealed under. Byte layout is unchanged from V1; what
     * changed under `LocalStorageCryptographyContractV2` is where the value comes
     * from. It used to be the container's current wrapping epoch, which made a
     * record's readability depend on state that legitimately moves underneath it.
     */
    private fun header(
        schemaId: Int,
        schemaVersion: Int,
        generationId: Long,
        sequence: Long,
        context: Int,
    ): ByteArray = CanonicalWriter(32)
        .putU32(schemaId)
        .putU32(schemaVersion)
        .putI64(generationId)
        .putI64(sequence)
        .putU32(context)
        .toByteArray()

    private fun associatedData(
        schemaId: Int,
        schemaVersion: Int,
        generationId: Long,
        sequence: Long,
        context: Int,
    ): ByteArray = CanonicalWriter(40)
        .putU32(schemaId)
        .putU32(schemaVersion)
        .putI64(generationId)
        .putI64(sequence)
        .putI64(organismId)
        .putU32(context)
        .toByteArray()

    /**
     * Nonce derived from `(context, sequence)`, never random.
     *
     * The sequence is monotonic and never reused under one data key, so reuse is
     * structurally impossible rather than probabilistically unlikely — and the
     * write path needs no randomness source at all. The context prefix keeps that
     * true across an eventual data-key rotation as well.
     */
    private fun nonce(sequence: Long, context: Int): ByteArray = CanonicalWriter(12)
        .putU32(context)
        .putI64(sequence)
        .toByteArray()

    public fun append(
        sequence: Long,
        generationId: Long,
        schemaId: Int,
        schemaVersion: Int,
        plaintext: ByteArray,
    ) {
        val context = keys.dataKeyId
        val head = header(schemaId, schemaVersion, generationId, sequence, context)
        val sealed = ChaCha20Poly1305.seal(
            key = keys.dataKey(),
            nonce = nonce(sequence, context),
            aad = associatedData(schemaId, schemaVersion, generationId, sequence, context),
            plaintext = plaintext,
        )
        val record = CanonicalEnvelope.wrap(
            RECORD_SCHEMA_ID,
            RECORD_SCHEMA_VERSION,
            CanonicalWriter(head.size + sealed.size + 16)
                .putBytes(head)
                .putBytes(sealed)
                .toByteArray(),
        )
        medium.append(sequence, record)
    }

    /**
     * Every readable record, stopping at a torn tail.
     *
     * Two failures that look similar are treated very differently.
     *
     * A **structurally incomplete** record at the tail is a write that started
     * and never finished. The length-prefixed envelope makes that detectable, and
     * the contract says recovery begins at the last acknowledged record — so the
     * torn tail is skipped.
     *
     * A **structurally complete record that fails authentication** is something
     * else entirely: corruption, relocation, a foreign key epoch, or another
     * organism's data. That is never tolerated, at the tail or anywhere else,
     * because tolerating it would let a record be silently dropped or silently
     * accepted out of position.
     */
    public fun readAll(): List<DurableRecord> {
        val raw = medium.records()
        val decoded = ArrayList<DurableRecord>(raw.size)
        for ((index, entry) in raw.withIndex()) {
            val (sequence, bytes) = entry
            val record = try {
                decode(sequence, bytes)
            } catch (torn: CanonicalCodecException) {
                if (index == raw.size - 1) break
                throw StorageFault(
                    "record $sequence is structurally incomplete but is not the tail; " +
                        "acknowledged history cannot be skipped (${torn.message})",
                )
            } catch (unauthentic: ChaCha20Poly1305.AuthenticationFailure) {
                throw StorageFault(
                    "record $sequence failed authentication: ${unauthentic.message}",
                )
            }
            decoded += record
        }
        return decoded
    }

    private fun decode(sequence: Long, bytes: ByteArray): DurableRecord {
        val contents = CanonicalEnvelope.unwrap(bytes)
        if (contents.payloadSchemaId != RECORD_SCHEMA_ID) {
            throw StorageFault("record $sequence carries schema ${contents.payloadSchemaId}")
        }
        val reader = CanonicalReader(contents.payload)
        val head = reader.readBytes()
        val sealed = reader.readBytes()
        reader.requireExhausted()

        val headReader = CanonicalReader(head)
        val schemaId = headReader.readU32()
        val schemaVersion = headReader.readU32()
        val generationId = headReader.readI64()
        val storedSequence = headReader.readI64()
        val context = headReader.readU32()
        headReader.requireExhausted()

        if (storedSequence != sequence) {
            throw StorageFault("record header sequence $storedSequence does not match $sequence")
        }

        // The record's own immutable context, never the container's current
        // wrapping epoch. `LocalStorageCryptographyContractV2`: a wrapping
        // rotation rewraps the data key and rewrites no history, so a record must
        // not become unreadable because the material protecting its key moved.
        //
        // This is not a weakening. The context is inside the AAD, so a record
        // whose context was altered — to relocate it, to reinterpret it, or to
        // make a foreign record decrypt here — fails authentication. What is
        // gone is the *pre*-check that refused an honest record for carrying a
        // value that had legitimately changed; what does the refusing now is the
        // cryptography rather than a comparison against mutable state.
        val plaintext = ChaCha20Poly1305.open(
            key = keys.dataKey(),
            nonce = nonce(sequence, context),
            aad = associatedData(schemaId, schemaVersion, generationId, sequence, context),
            sealed = sealed,
        )
        return DurableRecord(sequence, generationId, schemaId, schemaVersion, plaintext)
    }

    public fun prune(throughSequence: Long) {
        medium.prune(throughSequence)
    }

    public fun selfTest(): Boolean = medium.selfTest()

    public companion object {
        public const val RECORD_SCHEMA_ID: Int = 213
        public const val RECORD_SCHEMA_VERSION: Int = 1
    }
}

/** A verified checkpoint. Canonical schema `214`. */
public class Checkpoint(
    public val generationId: Long,
    public val throughSequence: Long,
    public val stateBytes: ByteArray,
    public val stateHash: ByteArray,
    public val engineContractVersion: Int,
    public val eventContractVersion: Int,
    public val randomContractVersion: Int,
) {
    public fun canonicalBytes(): ByteArray = CanonicalEnvelope.wrap(
        SCHEMA_ID,
        SCHEMA_VERSION,
        CanonicalWriter(stateBytes.size + 96)
            .putI64(generationId)
            .putI64(throughSequence)
            .putBytes(stateBytes)
            .putBytes(stateHash)
            .putI32(engineContractVersion)
            .putI32(eventContractVersion)
            .putI32(randomContractVersion)
            .toByteArray(),
    )

    /** The checkpoint is self-verifying: its hash must match its own state bytes. */
    public fun verifySelf(): Boolean =
        CanonicalHash.ofEnvelope(stateBytes).contentEquals(stateHash)

    public companion object {
        public const val SCHEMA_ID: Int = 214
        public const val SCHEMA_VERSION: Int = 1

        public fun decode(payload: ByteArray): Checkpoint {
            val contents = CanonicalEnvelope.unwrap(payload)
            if (contents.payloadSchemaId != SCHEMA_ID) {
                throw StorageFault("expected checkpoint schema, got ${contents.payloadSchemaId}")
            }
            val reader = CanonicalReader(contents.payload)
            val checkpoint = Checkpoint(
                generationId = reader.readI64(),
                throughSequence = reader.readI64(),
                stateBytes = reader.readBytes(),
                stateHash = reader.readBytes(),
                engineContractVersion = reader.readI32(),
                eventContractVersion = reader.readI32(),
                randomContractVersion = reader.readI32(),
            )
            reader.requireExhausted()
            return checkpoint
        }
    }
}
