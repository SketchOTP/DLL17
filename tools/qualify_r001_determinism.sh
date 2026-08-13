#!/usr/bin/env bash
#
# R001 cross-target determinism qualification harness.
#
# Runs the shared R001 qualification kernel on one Android target and records
# what that target computed. The claim being tested is byte identity: every
# target must produce the same R001_EVIDENCE_DIGEST as the desktop JVM reference
# runner and as the frozen golden constant compiled into core-state.
#
# Unlike the R000 launch harness, this one expects its evidence to be *identical*
# on every run and every target. Timing and process IDs are irrelevant here; a
# single differing hex digit is a qualification failure.
#
# Usage:
#   tools/qualify_r001_determinism.sh <adb-serial> <target-label>
#
# Example:
#   tools/qualify_r001_determinism.sh emulator-5556 x86_emulator
#   tools/qualify_r001_determinism.sh 49121FDAS0025V tensor_device

set -uo pipefail

SERIAL="${1:-}"
LABEL="${2:-}"

if [[ -z "$SERIAL" || -z "$LABEL" ]]; then
  echo "usage: $0 <adb-serial> <target-label>" >&2
  exit 2
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT" || exit 1

: "${ANDROID_HOME:=$HOME/Android/Sdk}"
: "${JAVA_HOME:=$HOME/.local/toolchains/jdk-17.0.20+8}"
export ANDROID_HOME JAVA_HOME
ADB="$ANDROID_HOME/platform-tools/adb"

OUT_DIR="$REPO_ROOT/qualification/device-matrix/R001"
mkdir -p "$OUT_DIR"
OUT_FILE="$OUT_DIR/${LABEL}.txt"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

adbs() { "$ADB" -s "$SERIAL" "$@"; }

echo "== R001 determinism qualification =="
echo "serial: $SERIAL"
echo "label:  $LABEL"

# ---------------------------------------------------------------- target ready
"$ADB" start-server >/dev/null 2>&1
if ! "$ADB" devices | awk '{print $1}' | grep -qx "$SERIAL"; then
  fail "device $SERIAL is not attached"
fi

for _ in $(seq 1 30); do
  state="$(adbs shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  [[ "$state" == "1" ]] && break
  sleep 2
done
[[ "$(adbs shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] \
  || fail "device $SERIAL did not report boot_completed"

ABI="$(adbs shell getprop ro.product.cpu.abi | tr -d '\r')"
DEVICE="$(adbs shell getprop ro.product.device | tr -d '\r')"
MODEL="$(adbs shell getprop ro.product.model | tr -d '\r')"
SDK="$(adbs shell getprop ro.build.version.sdk | tr -d '\r')"
RELEASE="$(adbs shell getprop ro.build.version.release | tr -d '\r')"
FINGERPRINT="$(adbs shell getprop ro.build.fingerprint | tr -d '\r')"
HARDWARE="$(adbs shell getprop ro.hardware | tr -d '\r')"
SOC_MODEL="$(adbs shell getprop ro.soc.model | tr -d '\r')"
SOC_MANUFACTURER="$(adbs shell getprop ro.soc.manufacturer | tr -d '\r')"

echo "abi=$ABI device=$DEVICE sdk=$SDK soc=$SOC_MANUFACTURER/$SOC_MODEL"

# ------------------------------------------------------------------- execution
adbs logcat -c >/dev/null 2>&1

echo "running instrumented determinism tests..."
GRADLE_LOG="$(mktemp)"
ANDROID_SERIAL="$SERIAL" ./gradlew :android-host:connectedDebugAndroidTest \
  --console=plain >"$GRADLE_LOG" 2>&1
GRADLE_STATUS=$?

TEST_LINE="$(grep -E 'Finished [0-9]+ tests' "$GRADLE_LOG" | tail -1)"

# Logcat is filtered to this project's own tag. The full buffer of a physical
# phone inventories its owner's installed applications and account activity;
# none of that is evidence about determinism, and this repository is public.
KERNEL_OUTPUT="$(adbs logcat -d -s DLL17-R001 2>/dev/null \
  | sed 's/^.*DLL17-R001[: ]*//' \
  | grep -v '^--------- beginning')"

DIGEST="$(printf '%s\n' "$KERNEL_OUTPUT" | grep -oE 'R001_EVIDENCE_DIGEST=[0-9a-f]{64}' | tail -1 | cut -d= -f2)"

GOLDEN="$(grep -A1 'GOLDEN_EVIDENCE_DIGEST: String' \
  core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/R001QualificationKernel.kt \
  | grep -oE '[0-9a-f]{64}' | head -1)"

# --------------------------------------------------------------------- verdict
VERDICT="FAILED"
if [[ $GRADLE_STATUS -eq 0 && -n "$DIGEST" && "$DIGEST" == "$GOLDEN" ]]; then
  VERDICT="PASSED"
fi

{
  echo "R001 determinism qualification - $LABEL"
  echo
  echo "target"
  echo "  serial            : $SERIAL"
  echo "  model             : $MODEL"
  echo "  device            : $DEVICE"
  echo "  abi               : $ABI"
  echo "  hardware          : $HARDWARE"
  echo "  soc manufacturer  : ${SOC_MANUFACTURER:-unreported}"
  echo "  soc model         : ${SOC_MODEL:-unreported}"
  echo "  android release   : $RELEASE"
  echo "  api level         : $SDK"
  echo "  build fingerprint : $FINGERPRINT"
  echo
  echo "instrumented run"
  echo "  gradle exit status : $GRADLE_STATUS"
  echo "  ${TEST_LINE:-no test summary line captured}"
  echo
  echo "kernel output (logcat, filtered to this project's tag)"
  printf '%s\n' "$KERNEL_OUTPUT" | sed 's/^/  /'
  echo
  echo "comparison"
  echo "  golden digest   : $GOLDEN"
  echo "  computed digest : ${DIGEST:-none captured}"
  echo "  byte identical  : $([[ "$DIGEST" == "$GOLDEN" ]] && echo yes || echo no)"
  echo
  echo "VERDICT: $VERDICT"
} >"$OUT_FILE"

rm -f "$GRADLE_LOG"

echo
echo "wrote $OUT_FILE"
echo "VERDICT: $VERDICT"
[[ "$VERDICT" == "PASSED" ]] || exit 1
