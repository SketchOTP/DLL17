# Decision log

Implementation decisions that are not architect directives. Architect decisions
live in the canonical Notion pages and in `.agent/RECORD.md`.

| ID | Date | Decision | Rationale |
|---|---|---|---|
| IMPL-0001 | 2026-08-12 | `core-math`, `core-crypto` and `core-state` apply only the Kotlin JVM plugin, and each carries a test asserting the Android framework is absent from its classpath. | Makes the E2E module boundary an enforced build fact rather than a convention. |
| IMPL-0002 | 2026-08-12 | `android-host` applies `com.android.application` and the Compose compiler plugin, and does not apply `org.jetbrains.kotlin.android`. | AGP 9 provides built-in Kotlin support and fails the build if the standalone plugin is applied. Frozen Kotlin 2.4.10 still governs the module. |
| IMPL-0003 | 2026-08-12 | The R000 registries are stored as versioned Markdown under `docs/architecture/registries/`. | Implementation Plan E2E mandates the registries but does not fix their location; Markdown keeps them reviewable and diffable before any code depends on them. |
