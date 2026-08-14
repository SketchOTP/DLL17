package com.animusmachinae.dll17.services.identity

import com.animusmachinae.dll17.core.recovery.AuthorityOutcome
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityProtocol
import com.animusmachinae.dll17.core.recovery.RecoveryRoot
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IdentityAuthorityServiceTest {

    private val root: File = Files.createTempDirectory("dll17-authority-test").toFile()
    private val organism = 0x0D11L
    private val deviceA = 0xA1L
    private val deviceB = 0xB2L
    private val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
    private val verificationKey = recoveryRoot.authorityProofKey(organism)

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    private fun service(name: String = "authority") =
        IdentityAuthorityService(File(root, name).apply { mkdirs() })

    private fun proof(nonce: ByteArray, epoch: Int, device: Long) =
        IdentityAuthorityProtocol.activationProof(recoveryRoot, organism, nonce, epoch, device)

    private fun registered(name: String = "authority"): IdentityAuthorityService =
        service(name).also { it.register(organism, verificationKey, deviceA, 1_000L) }

    @Test
    fun `registration establishes epoch one and is idempotent for the same device`() {
        val authority = service()
        assertEquals(
            AuthorityOutcome.REGISTERED,
            authority.register(organism, verificationKey, deviceA, 1_000L).outcome,
        )
        val again = authority.register(organism, verificationKey, deviceA, 1_100L)
        assertEquals(AuthorityOutcome.REGISTERED, again.outcome)
        assertEquals(1, again.currentEpoch)
    }

    @Test
    fun `a second device cannot register over an existing organism`() {
        val authority = registered()
        val conflict = authority.register(organism, verificationKey, deviceB, 1_100L)
        assertEquals(AuthorityOutcome.EPOCH_CONFLICT, conflict.outcome)
        assertEquals(1, conflict.currentEpoch)
    }

    @Test
    fun `a valid proof advances the epoch exactly once and grants a lease`() {
        val authority = registered()
        val challenge = authority.challenge(organism, 1_100L)
        assertEquals(AuthorityOutcome.CHALLENGE_ISSUED, challenge.outcome)
        val granted = authority.activate(
            organism, 2, challenge.nonce, proof(challenge.nonce, 2, deviceB), deviceB, 1_200L,
        )
        assertEquals(AuthorityOutcome.ACTIVATION_GRANTED, granted.outcome)
        assertEquals(2, granted.currentEpoch)
        assertTrue(granted.leaseExpiresAtMillis > 1_200L)
    }

    @Test
    fun `a nonce is single use`() {
        val authority = registered()
        val challenge = authority.challenge(organism, 1_100L)
        authority.activate(
            organism, 2, challenge.nonce, proof(challenge.nonce, 2, deviceB), deviceB, 1_200L,
        )
        // A different device replaying the same nonce for the next epoch.
        val replay = authority.activate(
            organism, 3, challenge.nonce, proof(challenge.nonce, 3, 0xC3L), 0xC3L, 1_300L,
        )
        assertEquals(AuthorityOutcome.CHALLENGE_INVALID, replay.outcome)
    }

    @Test
    fun `the same device retrying inside its lease is an idempotent grant`() {
        val authority = registered()
        val challenge = authority.challenge(organism, 1_100L)
        val first = authority.activate(
            organism, 2, challenge.nonce, proof(challenge.nonce, 2, deviceB), deviceB, 1_200L,
        )
        val retry = authority.activate(
            organism, 2, challenge.nonce, proof(challenge.nonce, 2, deviceB), deviceB, 1_250L,
        )
        assertEquals(AuthorityOutcome.ALREADY_GRANTED, retry.outcome)
        assertEquals(first.currentEpoch, retry.currentEpoch)
        assertTrue(retry.granted)
    }

    @Test
    fun `a captured proof is useless to a different device`() {
        val authority = registered()
        val challenge = authority.challenge(organism, 1_100L)
        val captured = proof(challenge.nonce, 2, deviceB)
        val stolen = authority.activate(organism, 2, challenge.nonce, captured, 0xC3L, 1_200L)
        assertEquals(AuthorityOutcome.PROOF_REJECTED, stolen.outcome)
        assertEquals(1, stolen.currentEpoch)
    }

    @Test
    fun `an epoch that is not exactly one past current is refused`() {
        val authority = registered()
        for (requested in listOf(1, 3, 7)) {
            val challenge = authority.challenge(organism, 1_100L + requested)
            val response = authority.activate(
                organism, requested, challenge.nonce,
                proof(challenge.nonce, requested, deviceB), deviceB, 1_200L + requested,
            )
            assertEquals(AuthorityOutcome.EPOCH_CONFLICT, response.outcome, "epoch $requested")
        }
    }

    @Test
    fun `two destinations racing on the same package cannot both win`() {
        val authority = registered()
        val challengeOne = authority.challenge(organism, 1_100L)
        val challengeTwo = authority.challenge(organism, 1_110L)
        val winner = authority.activate(
            organism, 2, challengeOne.nonce, proof(challengeOne.nonce, 2, deviceB), deviceB, 1_200L,
        )
        val loser = authority.activate(
            organism, 2, challengeTwo.nonce, proof(challengeTwo.nonce, 2, 0xC3L), 0xC3L, 1_210L,
        )
        assertEquals(AuthorityOutcome.ACTIVATION_GRANTED, winner.outcome)
        assertEquals(AuthorityOutcome.EPOCH_CONFLICT, loser.outcome)
    }

    @Test
    fun `a challenge expires`() {
        val authority = registered()
        val challenge = authority.challenge(organism, 1_000L)
        val late = authority.activate(
            organism, 2, challenge.nonce, proof(challenge.nonce, 2, deviceB), deviceB,
            1_000L + IdentityAuthorityProtocol.CHALLENGE_VALIDITY_MILLIS + 1L,
        )
        assertEquals(AuthorityOutcome.CHALLENGE_INVALID, late.outcome)
    }

    @Test
    fun `repeated failed proofs are rate limited`() {
        val authority = registered()
        var last = AuthorityOutcome.REGISTERED
        for (attempt in 1..IdentityAuthorityProtocol.MAX_FAILED_PROOFS + 2) {
            val challenge = authority.challenge(organism, 2_000L + attempt)
            last = if (challenge.outcome == AuthorityOutcome.RATE_LIMITED) {
                challenge.outcome
            } else {
                authority.activate(
                    organism, 2, challenge.nonce, ByteArray(32), deviceB, 2_000L + attempt,
                ).outcome
            }
        }
        assertEquals(AuthorityOutcome.RATE_LIMITED, last)
    }

    @Test
    fun `a superseded device is told on its next contact and the current one is not`() {
        val authority = registered()
        val challenge = authority.challenge(organism, 1_100L)
        authority.activate(
            organism, 2, challenge.nonce, proof(challenge.nonce, 2, deviceB), deviceB, 1_200L,
        )
        assertEquals(
            AuthorityOutcome.SUPERSEDED,
            authority.heartbeat(organism, 1, deviceA, 1_300L).outcome,
        )
        assertEquals(
            AuthorityOutcome.REGISTERED,
            authority.heartbeat(organism, 2, deviceB, 1_300L).outcome,
        )
    }

    @Test
    fun `an unknown organism is refused rather than created`() {
        val authority = service()
        assertEquals(
            AuthorityOutcome.UNKNOWN_ORGANISM,
            authority.challenge(0xDEADL, 1_000L).outcome,
        )
        assertEquals(
            AuthorityOutcome.UNKNOWN_ORGANISM,
            authority.activate(0xDEADL, 2, ByteArray(16), ByteArray(32), deviceB, 1_000L).outcome,
        )
    }

    @Test
    fun `the epoch survives a restart of the service`() {
        val directory = File(root, "durable").apply { mkdirs() }
        val first = IdentityAuthorityService(directory)
        first.register(organism, verificationKey, deviceA, 1_000L)
        val challenge = first.challenge(organism, 1_100L)
        first.activate(
            organism, 2, challenge.nonce, proof(challenge.nonce, 2, deviceB), deviceB, 1_200L,
        )
        // A restarted authority that forgot the epoch would let a superseded
        // device reclaim the organism, which is the whole failure this prevents.
        val restarted = IdentityAuthorityService(directory)
        assertEquals(2, restarted.record(organism)?.currentEpoch)
        assertEquals(
            AuthorityOutcome.SUPERSEDED,
            restarted.heartbeat(organism, 1, deviceA, 1_400L).outcome,
        )
    }

    @Test
    fun `the stored record holds only identity metadata`() {
        val authority = registered("minimal")
        val record = authority.record(organism)
        assertNotNull(record)
        // Exhaustive: if a field is ever added that is organism-shaped, this list
        // is where the reviewer will see it.
        val fields = IdentityAuthorityService.AuthorityRecord::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()
        assertEquals(
            setOf(
                "organismId", "currentEpoch", "activeDeviceFingerprint", "verificationKey",
                "leaseExpiresAtMillis", "leaseDeviceFingerprint", "grantedEpoch",
                "failedProofs", "failureWindowStartMillis",
            ),
            fields,
        )
    }

    @Test
    fun `the audit trail records outcomes without organism content`() {
        val authority = registered("audit")
        val challenge = authority.challenge(organism, 1_100L)
        authority.activate(
            organism, 2, challenge.nonce, proof(challenge.nonce, 2, deviceB), deviceB, 1_200L,
        )
        val trail = authority.auditTrail()
        assertTrue(trail.any { it.contains("granted") })
        assertFalse(trail.any { it.contains("payload") || it.contains("memory") })
    }
}
