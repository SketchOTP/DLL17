#!/usr/bin/env bash
# D016-D reviewer jail.
#
# Runs one reviewer CLI with no repository, no home files and no host /tmp; a
# synthetic HOME whose only credential is a read-only BIND of the real auth file
# (bound, never copied, never printed); and network shared, because the provider
# API needs it.
#
# STATUS: this half works and is verified. Inside the jail, /home/sketch contains
# only the interpreter, the bound auth file and an empty working directory, and
# /home/sketch/Projects does not exist. It is committed because it is the
# reusable part of the D016-D attempt and the next attempt should not rebuild it.
#
# It is NOT sufficient on its own, and committing it must not imply otherwise.
# D016-D returned BLOCKED_AGENTIC_REVIEW_ISOLATION_UNAVAILABLE because both
# candidate reviewers carried tools provisioned by the provider account and
# executed server-side — including tools that read this repository from GitHub,
# which is public. Nothing this script does is in the path of those calls. See
# evidence/AGENTIC_REVIEW_ISOLATION_PREFLIGHT.txt.
#
# Usage: reviewer_jail.sh <codex|agy> <workdir> -- <command...>
set -euo pipefail

WHICH="$1"; shift
WORK="$1"; shift
[ "$1" = "--" ] && shift

COMMON=(
  bwrap
  --ro-bind /usr /usr
  --ro-bind /etc/resolv.conf /etc/resolv.conf
  --ro-bind /etc/ssl /etc/ssl
  --ro-bind /etc/passwd /etc/passwd
  --ro-bind /etc/group /etc/group
  --ro-bind /etc/nsswitch.conf /etc/nsswitch.conf
  --ro-bind /etc/hosts /etc/hosts
  --proc /proc
  --dev /dev
  --tmpfs /tmp
  --tmpfs /home
  --tmpfs /run
  --dir /home/sketch
  --dir /home/sketch/work
  --setenv HOME /home/sketch
  --setenv TMPDIR /tmp
  --chdir /home/sketch/work
  --unshare-user --unshare-pid --unshare-ipc --unshare-uts
  --share-net
  --die-with-parent
  --new-session
)
for l in /lib /lib64 /bin /sbin; do
  [ -e "$l" ] && COMMON+=(--symlink "usr${l}" "$l")
done

case "$WHICH" in
  codex)
    EXTRA=(
      --ro-bind /home/sketch/.nvm /home/sketch/.nvm
      --dir /home/sketch/.codex
      --ro-bind /home/sketch/.codex/auth.json /home/sketch/.codex/auth.json
      --setenv CODEX_HOME /home/sketch/.codex
      --setenv PATH /home/sketch/.nvm/versions/node/v20.19.0/bin:/usr/bin:/bin
    )
    ;;
  agy)
    EXTRA=(
      --ro-bind /home/sketch/.local/bin/agy /home/sketch/.local/bin/agy
      --dir /home/sketch/.gemini/antigravity-cli
      --ro-bind /home/sketch/.gemini/antigravity-cli/antigravity-oauth-token \
                /home/sketch/.gemini/antigravity-cli/antigravity-oauth-token
      --setenv PATH /home/sketch/.local/bin:/usr/bin:/bin
    )
    ;;
  *) echo "unknown reviewer: $WHICH" >&2; exit 2 ;;
esac

# The evidence bundle is the only project-supplied input, mounted read-only.
if [ -d "$WORK" ]; then
  EXTRA+=(--ro-bind "$WORK" /home/sketch/input)
fi

exec "${COMMON[@]}" "${EXTRA[@]}" "$@"
