# Repository Map Template

After adoption, maintain a concise navigation aid. This template intentionally contains no map of its own repository.

## Recommended sections after adoption

- Entry points.
- Core modules.
- Interfaces and contracts.
- Tests and validation.
- Configuration.
- Generated areas.
- External integration points.
- Areas that must not be edited manually.

## Inclusion rules

- Explain why every mapped path matters.
- Prefer important entry points and boundaries over exhaustive listings.
- Exclude vendored dependencies, caches, temporary task notes, and generated files unless their role matters.
- Update the map when a touched or newly understood area changes.

## Entry format after adoption

Use entries like this only in an adopted repository:

```text
<path/to/important-area> — why the path matters
```
