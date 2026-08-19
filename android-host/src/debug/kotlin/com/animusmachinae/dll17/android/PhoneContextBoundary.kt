package com.animusmachinae.dll17.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.animusmachinae.dll17.core.continuity.ClockObservation
import com.animusmachinae.dll17.core.continuity.ClockTrust
import com.animusmachinae.dll17.core.continuity.DurableTimeAnchor
import com.animusmachinae.dll17.core.continuity.TimeConfidence
import com.animusmachinae.dll17.research.aliveness.CapabilityState
import com.animusmachinae.dll17.research.aliveness.CoarsePlaceNormalizer
import com.animusmachinae.dll17.research.aliveness.CoarsePlaceObservation
import com.animusmachinae.dll17.research.aliveness.ObservationFamily
import com.animusmachinae.dll17.research.aliveness.ObservationMeta
import com.animusmachinae.dll17.research.aliveness.ObservationProvenance
import com.animusmachinae.dll17.research.aliveness.PermissionState
import com.animusmachinae.dll17.research.aliveness.SensoriumMode
import com.animusmachinae.dll17.research.aliveness.TimeTrustClass
import com.animusmachinae.dll17.research.aliveness.TrustedTimeObservation
import com.animusmachinae.dll17.research.aliveness.WorldObservation
import java.time.ZonedDateTime

/**
 * D016-AC debug-only platform adapter. It emits bounded evidence and discards
 * raw location values before a WorldObservation is created.
 */
internal class PhoneContextBoundary(private val context: Context) {
    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var anchor: DurableTimeAnchor? = null
    private var sequence: Long = 0L

    internal fun trustedTimeObservation(): WorldObservation {
        val now = ZonedDateTime.now()
        val clock = ClockObservation(
            wallClockUtcMillis = System.currentTimeMillis(),
            elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            bootIdentityPresent = bootCount() >= 0L,
            bootIdentity = bootCount().coerceAtLeast(0L),
        )
        val evidence = anchor?.let { ClockTrust.classify(it, clock) }
        val trust = if (evidence == null) {
            TimeTrustClass.UNVERIFIED_REBOOT
        } else {
            evidence.confidence.toResearchTrust()
        }
        val confidence = when (trust) {
            TimeTrustClass.VERIFIED_MONOTONIC,
            TimeTrustClass.AUTHENTICATED,
            -> 900_000
            TimeTrustClass.UNVERIFIED_REBOOT,
            TimeTrustClass.ANOMALOUS,
            -> 400_000
            TimeTrustClass.UNAVAILABLE -> 0
        }
        val observation = TrustedTimeObservation.fromLocal(
            hourOfDay = now.hour,
            isWeekend = now.dayOfWeek.value >= 6,
            trust = trust,
            confidencePpm = confidence,
        )
        anchor = if (anchor == null) {
            DurableTimeAnchor(
                anchorSequence = 1L,
                wallClockUtcMillis = clock.wallClockUtcMillis,
                elapsedRealtimeMillis = clock.elapsedRealtimeMillis,
                bootIdentityPresent = clock.bootIdentityPresent,
                bootIdentity = clock.bootIdentity,
                logicalTime = 0L,
                timeConfidence = TimeConfidence.UNVERIFIED_REBOOT,
                authenticatedTimePresent = false,
                authenticatedTimeMillis = 0L,
            )
        } else {
            ClockTrust.anchorFor(anchor!!, clock, evidence!!, anchor!!.logicalTime)
        }
        val elapsed = clock.elapsedRealtimeMillis
        return WorldObservation(
            family = ObservationFamily.TRUSTED_TIME,
            trustedTime = observation,
            meta = meta(
                capturedAtMillis = elapsed,
                freshForMillis = 30_000L,
                confidencePpm = confidence,
                uncertaintyPpm = 1_000_000 - confidence,
                provenance = ObservationProvenance.ANDROID_SYSTEM_CLOCK,
                permission = PermissionState.NOT_APPLICABLE,
                capability = CapabilityState.AVAILABLE,
            ),
        )
    }

    internal fun requestCurrentPlace(onObservation: (WorldObservation) -> Unit) {
        val permission = locationPermission()
        if (permission != PermissionState.GRANTED) {
            onObservation(unknownPlace(permission, CapabilityState.UNAVAILABLE))
            return
        }
        val manager = locationManager
        val provider = manager?.let(::availableProvider)
        if (manager == null || provider == null) {
            onObservation(unknownPlace(PermissionState.GRANTED, CapabilityState.UNAVAILABLE))
            return
        }
        if (!manager.isLocationEnabled) {
            onObservation(unknownPlace(PermissionState.GRANTED, CapabilityState.RESTRICTED))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                onObservation(location?.let(::placeObservation)
                    ?: unknownPlace(PermissionState.GRANTED, CapabilityState.RESTRICTED))
            }
        } else {
            onObservation(manager.getLastKnownLocation(provider)?.let(::placeObservation)
                ?: unknownPlace(PermissionState.GRANTED, CapabilityState.RESTRICTED))
        }
    }

    private fun placeObservation(location: Location): WorldObservation {
        // The only use of latitude/longitude is this in-memory quantization.
        // Neither value is placed in the returned object, status text or log.
        val identity = CoarsePlaceNormalizer.fromCoordinates(location.latitude, location.longitude)
        return WorldObservation(
            family = ObservationFamily.LOCATION_PLACE,
            trustedTime = trustedTimeObservation().trustedTime,
            place = CoarsePlaceObservation(identity),
            meta = meta(
                capturedAtMillis = SystemClock.elapsedRealtime(),
                freshForMillis = 120_000L,
                confidencePpm = 700_000,
                uncertaintyPpm = 300_000,
                provenance = ObservationProvenance.ANDROID_LOCATION,
                permission = PermissionState.GRANTED,
                capability = CapabilityState.AVAILABLE,
            ),
        )
    }

    private fun unknownPlace(permission: PermissionState, capability: CapabilityState): WorldObservation =
        WorldObservation(
            family = ObservationFamily.LOCATION_PLACE,
            trustedTime = trustedTimeObservation().trustedTime,
            place = CoarsePlaceObservation(com.animusmachinae.dll17.research.aliveness.CoarsePlaceIdentity.UNKNOWN),
            meta = meta(
                capturedAtMillis = SystemClock.elapsedRealtime(),
                freshForMillis = 30_000L,
                confidencePpm = 0,
                uncertaintyPpm = 1_000_000,
                provenance = ObservationProvenance.ANDROID_LOCATION,
                permission = permission,
                capability = capability,
            ),
        )

    private fun meta(
        capturedAtMillis: Long,
        freshForMillis: Long,
        confidencePpm: Int,
        uncertaintyPpm: Int,
        provenance: ObservationProvenance,
        permission: PermissionState,
        capability: CapabilityState,
    ): ObservationMeta = ObservationMeta(
        capturedAtMillis = capturedAtMillis,
        sequence = sequence++,
        freshUntilMillis = capturedAtMillis + freshForMillis,
        confidencePpm = confidencePpm,
        uncertaintyPpm = uncertaintyPpm,
        provenance = provenance,
        permission = permission,
        capability = capability,
        sensoriumMode = SensoriumMode.FOREGROUND_SENSORIUM,
    )

    private fun locationPermission(): PermissionState = when {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED -> PermissionState.GRANTED
        else -> PermissionState.DENIED
    }

    private fun availableProvider(manager: LocationManager): String? = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
    ).firstOrNull { manager.isProviderEnabled(it) }

    private fun bootCount(): Long = try {
        Settings.Global.getLong(context.contentResolver, Settings.Global.BOOT_COUNT, -1L)
    } catch (_: RuntimeException) {
        -1L
    }

    private fun TimeConfidence.toResearchTrust(): TimeTrustClass = when (this) {
        TimeConfidence.VERIFIED_MONOTONIC -> TimeTrustClass.VERIFIED_MONOTONIC
        TimeConfidence.AUTHENTICATED -> TimeTrustClass.AUTHENTICATED
        TimeConfidence.UNVERIFIED_REBOOT -> TimeTrustClass.UNVERIFIED_REBOOT
        TimeConfidence.ANOMALOUS -> TimeTrustClass.ANOMALOUS
    }
}
