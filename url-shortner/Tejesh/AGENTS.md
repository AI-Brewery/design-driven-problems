# AGENTS.md - URL Shortener (Tejesh)

This file is scoped to `url-shortner/Tejesh/`. It provides repo-specific context to help OpenCode sessions work efficiently in this submission folder.

## Location & Scope
- Workspace root: `C:\Design problems\design-driven-problems`
- Problem folder: `url-shortner/`
- Submission folder (this scope): `url-shortner/Tejesh/`
- Git branch: `tejesh/url-shortner`
- Git remote: `origin` (your fork)

Submission rules: only add/change files inside `url-shortner/Tejesh/`.

## Known State
- Implementation directories (`HLD/`, `LLD/`, `src/`, `tests/`) are present but currently empty.
- `README.md` (this folder) contains capacity estimation:
  - New URLs: 1M/day (~12 QPS write, ~1200 QPS read, 5x peak: 60 write, 6000 read)
  - Storage: ~1KB/URL -> ~1GB/day, ~365GB/year (excludes replication/backups/indexes)
  - Short code: Base62, 6 chars (62^6 ~ 5.68e10) - sufficient
- Problem requirements (from `url-shortner/readme.md`): `POST /shorten`, `GET /{shortCode}` (302 redirect, 404 if missing/expired), `GET /stats/{shortCode}`, `DELETE /{shortCode}`. Support optional `customAlias`, optional `expiresAt`, track click counts. Open questions must be answered in design.

## Expected Deliverables (submission checklist)
- Functional & non-functional requirements
- Capacity estimation (storage, QPS)
- HLD: architecture diagram (DB, cache, services)
- LLD: DB schema, classes, short-code generation logic
- Working implementation of all APIs
- Open questions answered (generation, collision, duplicate long URL, custom alias taken, 301 vs 302+tracking, enumeration prevention)
- Edge cases (invalid URL, expired link, duplicate alias)

## Working Guidance
- If HLD/LLD use diagrams, store images/diagrams alongside docs (e.g. in `HLD/` and `LLD/`). Reference them from markdown.
- Implementation language not specified; pick one and document setup in `url-shortner/Tejesh/README.md` (add exact run/test commands). Current env has Node, Python, Java/Maven available.
- Keep design and implementation consistent (review criterion). Update design artifacts if code changes.
- Do not modify files outside `url-shortner/Tejesh/` unless explicitly instructed.

## Development Workflow
1. Complete HLD/LLD first (document assumptions/decisions). 
2. Implement `src/` to satisfy all APIs and edge cases.
3. Add tests in `tests/` if feasible.
4. Update this folder's `README.md` with setup instructions and any design decisions/assumptions/limitations/future improvements.
5. Commit only when explicitly requested (per repo instructions). This submission work is on branch `tejesh/url-shortner`.

## Signals to Preserve
- The Base62 6-char choice is stated and justified by capacity - keep consistent.
- Scale numbers drive design (reads 100x writes, low redirect latency target <50ms) - factor into caching/reads.
- All four endpoints + optional fields + click tracking + delete must be implemented.