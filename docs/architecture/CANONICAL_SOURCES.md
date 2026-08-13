# Canonical sources

The authoritative specifications for this program are external Notion pages.
This repository implements them; it does not define them.

| Source | Role |
|---|---|
| `Digital Living Lifeform` (Notion) | Canonical organism architecture. Highest specification authority. |
| `Implementation Plan E2E` (Notion) | Phase plan, module topology, registry requirements, exit gates. |
| `Architect Directives & Outcomes` (Notion) | Issued directives and their recorded outcomes. |
| `.agent/DIRECTIVES.md` | Local append-only mirror of directives issued to this repository. |

Rules:

- A value that appears in neither the canonical pages nor a registry may not be
  invented by the implementer.
- Any architecture change made during implementation must be recorded as an
  explicit amendment and must reopen all affected regression gates.
