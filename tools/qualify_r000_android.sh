#!/usr/bin/env bash
#
# R000 Android qualification harness.
#
# Installs the R000 shell on a connected Android target, launches it, verifies
# the visible shell state, checks for crashes, terminates it, relaunches it, and
# records measured baseline observations.
#
# This harness proves the shell runs. It does not exercise organism behavior,
# because none exists in R000.
#
# Usage:
#   tools/qualify_r000_android.sh <serial> [apk-path] [evidence-dir]
#
# Exits non-zero on any qualification failure. Failure is a real result and must
# not be edited out of the recorded evidence.

set -u -o pipefail

SERIAL="${1:-}"
APK="${2:-android-host/build/outputs/apk/debug/android-host-debug.apk}"
EVIDENCE="${3:-qualification/device-matrix/R000}"

PKG="com.animusmachinae.dll17"
ACTIVITY="${PKG}/.android.MainActivity"

# Strings the R000 shell must actually display. Sourced from
# android-host/src/main/kotlin/com/animusmachinae/dll17/android/MainActivity.kt.
EXPECTED=(
  "Digital Living Lifeform"
  "R000 - greenfield project initialization"
  "No organism exists yet. Canonical logic is gated on R001."
  "core module: core-math"
  "core module: core-crypto"
  "core module: core-state"
)

if [ -z "$SERIAL" ]; then
  echo "FAIL: no device serial given" >&2
  exit 2
fi
if [ ! -f "$APK" ]; then
  echo "FAIL: APK not found at $APK" >&2
  exit 2
fi

ADB=(adb -s "$SERIAL")
mkdir -p "$EVIDENCE"

# The target can report sys.boot_completed while system_server is still
# settling, which makes `am start` fail with a broken pipe. Wait for the
# services this harness actually calls before trusting any result.
# A real phone will dim, lock or start its screensaver mid-run, which pre-empts
# the activity under test and looks exactly like a launch failure. Keep the
# screen awake for the duration and put the setting back afterwards: this is
# someone's device, not a lab fixture.
STAYON_CHANGED="no"
keep_awake() {
  if "${ADB[@]}" shell svc power stayon usb >/dev/null 2>&1; then
    STAYON_CHANGED="yes"
  fi
}
restore_power() {
  if [ "$STAYON_CHANGED" = "yes" ]; then
    "${ADB[@]}" shell svc power stayon false >/dev/null 2>&1 || true
  fi
}
trap restore_power EXIT

wake_screen() {
  # KEYCODE_WAKEUP is idempotent; it never turns a screen off.
  "${ADB[@]}" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  # Waking the screen is not enough: behind a keyguard the activity starts but
  # never becomes resumed, which reads as a launch failure. On a device with a
  # secure lock this shows the PIN prompt instead of unlocking, and the run will
  # legitimately fail rather than silently pass.
  "${ADB[@]}" shell wm dismiss-keyguard >/dev/null 2>&1 || true
  # Dismiss a running screensaver if one took over.
  if "${ADB[@]}" shell dumpsys activity activities 2>/dev/null \
      | grep -q "DreamActivity"; then
    "${ADB[@]}" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    sleep 2
  fi
  sleep 2
}

wait_for_services() {
  local i
  for i in $(seq 1 60); do
    if "${ADB[@]}" shell 'service check activity' 2>/dev/null | grep -q "found" \
      && "${ADB[@]}" shell 'service check package' 2>/dev/null | grep -q "found" \
      && [ "$("${ADB[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
      sleep 3
      return 0
    fi
    sleep 5
  done
  return 1
}
LOG="$EVIDENCE/qualification_run.log"
: > "$LOG"

FAILURES=0
say() { echo "$@" | tee -a "$LOG"; }
step_fail() { say "  RESULT: FAILED - $1"; FAILURES=$((FAILURES + 1)); }

say "R000 Android qualification run"
say "started: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
say "serial: $SERIAL"
say "apk: $APK"
say ""

keep_awake
if wait_for_services; then
  say "target services ready (activity, package, boot_completed)"
  say "screen kept awake for the run: stayon_changed=$STAYON_CHANGED"
else
  step_fail "target never became ready"
fi
say ""

# ---------------------------------------------------------------- target facts
say "== target =="
for prop in ro.build.version.release ro.build.version.sdk ro.product.cpu.abi \
            ro.product.model ro.product.device ro.build.fingerprint \
            ro.kernel.qemu ro.hardware; do
  value="$("${ADB[@]}" shell getprop "$prop" 2>/dev/null | tr -d '\r')"
  say "$prop = $value"
done
say ""

# ------------------------------------------------------------------ apk facts
say "== artifact =="
APK_BYTES="$(stat -c %s "$APK")"
APK_SHA="$(sha256sum "$APK" | cut -d' ' -f1)"
say "apk_size_bytes = $APK_BYTES"
say "apk_sha256 = $APK_SHA"
say ""

# -------------------------------------------------------------------- install
say "== install =="
"${ADB[@]}" uninstall "$PKG" >/dev/null 2>&1 || true
"${ADB[@]}" logcat -c 2>/dev/null || true

INSTALL_OUT="$("${ADB[@]}" install -r "$APK" 2>&1)"
say "$INSTALL_OUT"
if echo "$INSTALL_OUT" | grep -q "Success"; then
  say "  RESULT: install PASSED"
else
  step_fail "install did not report Success"
fi

INSTALLED_PATH="$("${ADB[@]}" shell pm path "$PKG" 2>/dev/null | tr -d '\r' | sed 's/^package://')"
say "installed_path = $INSTALLED_PATH"

# PackageManager can take a moment after a streamed install before the launcher
# activity resolves. Starting before then yields a spurious "does not exist".
RESOLVED="no"
for i in $(seq 1 24); do
  if "${ADB[@]}" shell cmd package resolve-activity --brief \
      -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "$PKG" \
      2>/dev/null | tr -d '\r' | grep -q "MainActivity"; then
    RESOLVED="yes"
    break
  fi
  sleep 5
done
say "launcher_activity_resolves = $RESOLVED"
if [ "$RESOLVED" != "yes" ]; then
  step_fail "installed package never resolved a LAUNCHER activity"
fi
say ""

# ------------------------------------------------------- launch (cold), timed
launch_and_verify() {
  local label="$1"
  local dumpfile="$2"

  say "== $label =="
  wake_screen
  local out attempt
  # One retry: a broken pipe from the activity service is a target-readiness
  # artifact, not an app result, and must not be recorded as a launch failure.
  for attempt in 1 2 3; do
    out="$("${ADB[@]}" shell am start -W -n "$ACTIVITY" 2>&1 | tr -d '\r')"
    if echo "$out" | grep -q "Status: ok"; then break; fi
    say "  attempt $attempt did not report Status: ok; retrying after target settle"
    sleep 8
  done
  say "$out"

  if ! echo "$out" | grep -q "Status: ok"; then
    step_fail "$label did not report Status: ok"
  fi

  # Startup timings, straight from `am start -W`.
  local total wait_ms
  total="$(echo "$out" | grep -E "^TotalTime:" | awk '{print $2}')"
  wait_ms="$(echo "$out" | grep -E "^WaitTime:" | awk '{print $2}')"
  say "  total_time_ms = ${total:-unavailable}"
  say "  wait_time_ms = ${wait_ms:-unavailable}"

  # Settle, then confirm the activity is actually resumed. `dumpsys` prints the
  # fully qualified class name, not the `.android.MainActivity` short form that
  # `am start` echoes, so match on the class rather than the intent notation.
  # Poll rather than sample once: resume can lag the launch on a real device.
  local resumed top r
  resumed=0
  top=""
  for r in 1 2 3 4 5 6 7 8; do
    sleep 3
    resumed="$("${ADB[@]}" shell dumpsys activity activities 2>/dev/null \
      | tr -d '\r' | grep -c "$PKG.android.MainActivity")"
    top="$("${ADB[@]}" shell dumpsys activity activities 2>/dev/null \
      | tr -d '\r' | grep -E "topResumedActivity" | head -1)"
    if [ "$resumed" -ge 1 ] && echo "$top" | grep -q "$PKG"; then break; fi
    wake_screen
  done
  say "activity_records_matching = $resumed"
  say "top_resumed = $top"
  if [ "$resumed" -lt 1 ]; then
    step_fail "$label: MainActivity not present in the activity stack"
  fi
  if ! echo "$top" | grep -q "$PKG"; then
    step_fail "$label: $PKG is not the top resumed activity"
  fi

  # Visible state: Compose publishes its text through the accessibility tree,
  # so the uiautomator dump is the on-screen text, not the source constant.
  # uiautomator itself can fail while the target settles, so retry it.
  local d
  for d in 1 2 3; do
    "${ADB[@]}" shell rm -f /sdcard/window_dump.xml >/dev/null 2>&1
    "${ADB[@]}" shell uiautomator dump /sdcard/window_dump.xml >/dev/null 2>&1
    "${ADB[@]}" pull /sdcard/window_dump.xml "$dumpfile" >/dev/null 2>&1
    [ -s "$dumpfile" ] && break
    sleep 5
  done

  if [ ! -s "$dumpfile" ]; then
    step_fail "$label: could not capture the view hierarchy"
    return
  fi

  local missing=0
  for want in "${EXPECTED[@]}"; do
    if grep -qF "$want" "$dumpfile"; then
      say "  visible: OK   \"$want\""
    else
      say "  visible: MISS \"$want\""
      missing=$((missing + 1))
    fi
  done
  if [ "$missing" -gt 0 ]; then
    step_fail "$label: $missing expected shell string(s) not visible on screen"
  else
    say "  RESULT: $label visible state PASSED"
  fi
  say ""
}

launch_and_verify "launch 1 (cold)" "$EVIDENCE/ui_hierarchy_launch1.xml"

"${ADB[@]}" exec-out screencap -p > "$EVIDENCE/shell_launch1.png" 2>/dev/null
if [ -s "$EVIDENCE/shell_launch1.png" ]; then
  say "screenshot: $EVIDENCE/shell_launch1.png ($(stat -c %s "$EVIDENCE/shell_launch1.png") bytes)"
else
  say "screenshot: NOT CAPTURED"
fi
say ""

# --------------------------------------------------------------- measurements
say "== measured baseline =="
PID="$("${ADB[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
say "pid = ${PID:-none}"

"${ADB[@]}" shell dumpsys meminfo "$PKG" 2>/dev/null | tr -d '\r' \
  > "$EVIDENCE/meminfo_launch1.txt"
PSS_TOTAL="$(grep -E "^\s+TOTAL PSS:" "$EVIDENCE/meminfo_launch1.txt" | awk '{print $3}' | head -1)"
if [ -z "$PSS_TOTAL" ]; then
  PSS_TOTAL="$(grep -E "TOTAL" "$EVIDENCE/meminfo_launch1.txt" | head -1 | awk '{print $2}')"
fi
say "total_pss_kb = ${PSS_TOTAL:-unavailable}"

if [ -n "${PID:-}" ]; then
  CPU_LINE="$("${ADB[@]}" shell top -n 1 -b -p "$PID" 2>/dev/null | tr -d '\r' | tail -1)"
  say "top_line = $CPU_LINE"
fi

STORAGE="$("${ADB[@]}" shell du -s -k /data/data/$PKG 2>/dev/null | tr -d '\r' | awk '{print $1}')"
say "app_data_dir_kb = ${STORAGE:-unavailable-without-root}"

CODE_SIZE="$("${ADB[@]}" shell du -s -k "$(dirname "$INSTALLED_PATH")" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
say "installed_code_kb = ${CODE_SIZE:-unavailable}"
say ""

# ------------------------------------------------------------ crash inspection
#
# Scoped to this package on purpose. A `FATAL EXCEPTION` anywhere in the global
# log is not evidence about this app: unrelated system commands (uiautomator,
# for one) can crash in the same buffer. A crash counts only when the crash
# block names our process, or when an ANR or a forced finish names our package.
#
# Privacy: the full logcat buffer of a real phone inventories the owner's
# installed applications and their telephony, bluetooth and account activity.
# None of that is evidence about this shell, and this repository is public. The
# full buffer is analysed in a temporary file and never written to the evidence
# directory; only lines naming this package, plus crash blocks, are retained.
crash_inspect() {
  local label="$1" logfile="$2"
  say "== crash inspection ($label) =="

  local raw
  raw="$(mktemp)"
  "${ADB[@]}" logcat -d 2>/dev/null | tr -d '\r' > "$raw"

  local ours
  ours="$(awk -v pkg="$PKG" '
    /FATAL EXCEPTION/ { inblock = 6 }
    inblock > 0 {
      if (index($0, "Process: " pkg) > 0) { hits++; inblock = 0 }
      else { inblock-- }
    }
    END { print hits + 0 }
  ' "$raw")"

  local anrs
  anrs="$(grep -cE "ANR in $PKG|Force finishing activity .*$PKG" "$raw" || true)"

  say "app_fatal_exceptions = $ours"
  say "app_anr_or_force_finish = $anrs"

  local other
  other="$(grep -c "FATAL EXCEPTION" "$raw" || true)"
  say "other_fatal_exceptions_in_buffer = $((other - ours)) (not attributed to $PKG)"

  # Retain only package-relevant lines and crash blocks as committed evidence.
  {
    echo "# Filtered logcat for $PKG ($label)."
    echo "# Full-buffer lines unrelated to this package are deliberately excluded:"
    echo "# they describe the owner's device, not this application."
    echo "# Buffer totals: app_fatal_exceptions=$ours app_anr_or_force_finish=$anrs"
    echo "# other_fatal_exceptions_in_buffer=$((other - ours))"
    echo
    grep -E "$PKG" "$raw" || true
    echo
    echo "# Crash blocks present anywhere in the buffer, for attribution:"
    grep -A 12 "FATAL EXCEPTION" "$raw" || echo "# none"
  } > "$logfile"

  if [ "$ours" -gt 0 ] || [ "$anrs" -gt 0 ]; then
    step_fail "crash or ANR observed for $PKG during $label"
    grep -B2 -A12 "FATAL EXCEPTION" "$raw" | head -40 | tee -a "$LOG"
  else
    say "  RESULT: no crash or ANR for $PKG PASSED"
  fi
  rm -f "$raw"
  say ""
}

crash_inspect "launch 1" "$EVIDENCE/logcat_launch1.txt"

# ------------------------------------------------------------------ terminate
say "== terminate =="
"${ADB[@]}" shell am force-stop "$PKG"
sleep 2
PID_AFTER="$("${ADB[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r')"
if [ -z "$PID_AFTER" ]; then
  say "  RESULT: terminate PASSED (no process remains)"
else
  step_fail "process $PID_AFTER still alive after force-stop"
fi
say ""

# ------------------------------------------------------------------- relaunch
"${ADB[@]}" logcat -c 2>/dev/null || true
launch_and_verify "launch 2 (relaunch after terminate)" "$EVIDENCE/ui_hierarchy_launch2.xml"

"${ADB[@]}" exec-out screencap -p > "$EVIDENCE/shell_launch2.png" 2>/dev/null

crash_inspect "launch 2 (relaunch)" "$EVIDENCE/logcat_launch2.txt"

say "finished: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
if [ "$FAILURES" -eq 0 ]; then
  say "OVERALL: PASSED"
  exit 0
fi
say "OVERALL: FAILED ($FAILURES failing step(s))"
exit 1
