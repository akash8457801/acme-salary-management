# Decision log

Short ADRs. Each records the decision, the alternative I rejected, and what it would cost to change
my mind later.

---

## ADR-001 — Spring Boot 3 + Angular 19

**Decision.** Java 17 / Spring Boot 3 on the backend, Angular 19 with Angular Material on the front.

**Why.** The role is "Software Craftsperson / Java / Angular", and the brief names that pairing as
preferred. Spring Data's `Specification` API gives dynamic filtering without string-concatenated
SQL, and Angular's standalone components + signals remove most of the ceremony the framework used
to be criticised for.

**Rejected.** Node/Next would have been faster to stand up on this machine, but answering a Java
question with a TypeScript backend is the wrong answer to the question that was asked.

---

## ADR-002 — SQLite

**Decision.** SQLite via `hibernate-community-dialects`, one file on disk.

**Why.** The brief offers it explicitly. At 10k employees / ~30k compensation rows the whole
dataset is a few megabytes; SQLite indexes it fine and answers every aggregate in single-digit
milliseconds. Zero-install matters more than scale here — a reviewer clones and runs.

**Cost of changing.** Low, and deliberately so: JPA + standard SQL, no SQLite-specific features.
Swapping to Postgres is a dependency and a datasource URL. The one thing I would revisit is the
percentile query, which uses a window function (supported by both) rather than `PERCENTILE_CONT`
(Postgres only) — chosen for portability.

---

## ADR-003 — No `salary` column on Employee

**Decision.** Current pay is *derived* from an append-only `CompensationRecord` timeline.

**Why.** This is the central product insight. The spreadsheet's real failure isn't that it's a
spreadsheet — it's that editing a cell destroys the previous value. "What did she earn before the
promotion?" and "show me every change we made this year" are unanswerable the moment you model
salary as a mutable field. An interval-based timeline makes the audit trail a consequence of the
data model rather than a feature bolted on beside it.

**Cost.** Every read of "current salary" is a join. Mitigated by an index on
`(employee_id, effective_to)`; measured at ~15 ms for a 50-row page over the full seed.

**Rejected.** `Employee.currentSalary` plus a `salary_audit` table. Cheaper reads, but then two
places claim to know the truth, and they drift.

---

## ADR-004 — Denormalise an annualised USD amount onto each record

**Decision.** At write time, convert the record's Money to an annual USD figure using the FX table
and store it alongside the original amount and currency.

**Why.** Every insight query aggregates across countries. Without this, "payroll cost by
department" means converting 10,000 amounts per request in application code, which is both slow and
impossible to `GROUP BY`. With it, every aggregate is plain SQL.

**Trade-off, stated honestly.** This is denormalisation: the derived value can go stale if a rate
changes. I accept that deliberately — a compensation report *should* be reproducible, so a record
converted at January's rate staying at January's rate is a feature, not a bug. The stored value
carries the `fxRateVersion` that produced it, so a re-conversion job is possible if the business
ever wants live restatement.

**Rejected.** Converting in the query with a joined rates table. Correct, but it makes every
aggregate depend on a join to a slowly-changing dimension and gives you silently-mutating history.

---

## ADR-005 — Deterministic seeding

**Decision.** The 10,000-employee generator is seeded with a fixed constant and runs only against
an empty database.

**Why.** Reviewers, screenshots, the demo video and the integration tests should all see the same
world. Random seed data means a failing test you can't reproduce and a demo where the numbers move
between takes. The distribution is shaped deliberately — realistic level pyramid, country-specific
salary bands, a small number of intentional outliers and pay-gap patterns — so the insights screens
have something true to say instead of uniform noise.

---

## ADR-006 — Server-side everything for the list

**Decision.** Search, filter, sort, and pagination all execute in SQL; the browser never holds more
than one page.

**Why.** The requirement is explicitly 10,000 employees. Shipping the array to the client and
filtering with JavaScript works at 200 rows and is the exact mistake this dataset size is there to
catch.

---

## ADR-007 — Hand-rolled SVG charts

**Decision.** Small purpose-built chart components instead of a charting library.

**Why.** The dashboard needs three chart shapes. A general-purpose library is a large dependency,
a configuration surface, and a theming fight, to produce three shapes. ~120 lines of SVG is less
code than the config would have been, and it renders identically everywhere.

**When I'd change my mind.** The moment someone asks for interactive drill-down, zoom, or a
tenth chart type. Then buy, don't build.

---

## ADR-008 — Money as `BigDecimal` + `Currency`, never a `double`

**Decision.** A `Money` value object; arithmetic and rounding are its responsibility.

**Why.** Standard, but non-negotiable in a salary system. Floating point loses cents, and a bare
`amount` field with the currency implied elsewhere is how you get a €95,000 salary displayed as
$95,000.
