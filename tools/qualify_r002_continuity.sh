#!/usr/bin/env bash
#
# R002 cross-target continuity and durability qualification harness.
#
# Runs the shared R001 and R002 qualification kernels on one Android target and
# records what that target computed. Both are captured in one instrumented run
# because R002 acceptance requires R001 to remain green: a continuity change that
# perturbed the deterministic core would be a regression, not a feature.
#
# The claim being tested is byte identity. Every target must produce the same
# R002_EVIDENCE_DIGEST as the desktop JVM reference runner and as the frozen
# golden constant compiled into core-continuity. A single differing hex digit is
# a qualification failure, not noise.
#
# Usage:
#   tools/qualify_r002_continuity.sh <adb-serial> <target-label>
#
# Example:
#   tools/qualify_r002_continuity.sh emulator-5556 x86_emulator
#   tools/qualify_r002_continuity.sh 49121FDAS0025V tensor_device

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

OUT_DIR="$REPO_ROOT/qualification/device-matrix/R002"
mkdir -p "$OUT_DIR"
OUT_FILE="$OUT_DIR/${LABEL}.txt"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

adbs() { "$ADB" -s "$SERIAL" "$@"; }

echo "== R002 continuity qualification =="
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

echo "running instrumented continuity tests..."
GRADLE_LOG="$(mktemp)"
ANDROID_SERIAL="$SERIAL" ./gradlew :android-host:connectedDebugAndroidTest \
  --console=plain >"$GRADLE_LOG" 2>&1
GRADLE_STATUS=$?

TEST_LINE="$(grep -E 'Finished [0-9]+ tests' "$GRADLE_LOG" | tail -1)"

# Logcat is filtered to this project's own tags. The full buffer of a physical
# phone inventories its owner's installed applications and account activity;
# none of that is evidence about continuity, and this repository is public.
R002_OUTPUT="$(adbs logcat -d -s DLL17-R002 2>/dev/null \
  | sed 's/^.*DLL17-R002[: ]*//' \
  | grep -v '^--------- beginning')"
R001_OUTPUT="$(adbs logcat -d -s DLL17-R001 2>/dev/null \
  | sed 's/^.*DLL17-R001[: ]*//' \
  | grep -v '^--------- beginning')"

R002_DIGEST="$(printf '%s\n' "$R002_OUTPUT" | grep -oE 'R002_EVIDENCE_DIGEST=[0-9a-f]{64}' | tail -1 | cut -d= -f2)"
R001_DIGEST="$(printf '%s\n' "$R001_OUTPUT" | grep -oE 'R001_EVIDENCE_DIGEST=[0-9a-f]{64}' | tail -1 | cut -d= -f2)"

R002_GOLDEN="$(grep -A1 'GOLDEN_EVIDENCE_DIGEST: String' \
  core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/R002QualificationKernel.kt \
  | grep -oE '[0-9a-f]{64}' | head -1)"
R001_GOLDEN="$(grep -A1 'GOLDEN_EVIDENCE_DIGEST: String' \
  core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/R001QualificationKernel.kt \
  | grep -oE '[0-9a-f]{64}' | head -1)"

# --------------------------------------------------------------------- verdict
VERDICT="FAILED"
if [[ $GRADLE_STATUS -eq 0 \
      && -n "$R002_DIGEST" && "$R002_DIGEST" == "$R002_GOLDEN" \
      && -n "$R001_DIGEST" && "$R001_DIGEST" == "$R001_GOLDEN" ]]; then
  VERDICT="PASSED"
fi

{
  echo "R002 continuity and durability qualification - $LABEL"
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
  echo "R002 kernel output (logcat, filtered to this project's tag)"
  printf '%s\n' "$R002_OUTPUT" | sed 's/^/  /'
  echo
  echo "comparison"
  echo "  R002 golden digest   : $R002_GOLDEN"
  echo "  R002 computed digest : ${R002_DIGEST:-none captured}"
  echo "  R002 byte identical  : $([[ "$R002_DIGEST" == "$R002_GOLDEN" ]] && echo yes || echo no)"
  echo "  R001 golden digest   : $R001_GOLDEN"
  echo "  R001 computed digest : ${R001_DIGEST:-none captured}"
  echo "  R001 byte identical  : $([[ "$R001_DIGEST" == "$R001_GOLDEN" ]] && echo yes || echo no)"
  echo
  echo "VERDICT: $VERDICT"
} >"$OUT_FILE"

rm -f "$GRADLE_LOG"

echo
echo "wrote $OUT_FILE"
echo "VERDICT: $VERDICT"
[[ "$VERDICT" == "PASSED" ]] || exit 1
