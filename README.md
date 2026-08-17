# ACME Salary Management

Web-based salary management for an HR team that currently lives in Excel: an auditable
compensation record for 10,000 employees across 10 countries, and answers to "how does the org
pay people?" — cost by department and country, pay by level, salary distribution, and a
within-level gender pay-gap view.

**Stack:** Java 17 · Spring Boot 3 · SQLite — Angular 19 · Angular Material

The design idea the whole system hangs off: **an employee has no salary *field*.** Pay is an
append-only timeline of `CompensationRecord`s — a raise closes the current period and opens the
next, so "what did she earn before the promotion, and why did it change?" is always answerable.
That is the thing the spreadsheet could never do. The reasoning behind this and every other
non-obvious choice is in [docs/DECISIONS.md](docs/DECISIONS.md).

## Documents

| | |
|---|---|
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Goal, scope, what is deliberately left out and why |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System shape, domain model, query strategy, testing |
| [docs/DECISIONS.md](docs/DECISIONS.md) | ADRs: the trade-offs, with the rejected alternatives |
| [docs/AI_WORKFLOW.md](docs/AI_WORKFLOW.md) | How AI tooling was used to build this |

## Run it

Prerequisites: **Java 17+** and **Node 20+**. Maven is not required (wrapper included); the
database is a file SQLite creates itself.

### Fastest: one process

```bash
cd frontend && npm install && npx ng build && cd ../backend && ./mvnw -DskipTests -Pbundle-ui package && java -jar target/salary-management-1.0.0.jar
```

Open **http://localhost:8080**. First start seeds 10,000 employees (~5 s, deterministic — every
checkout sees the same organisation); subsequent starts detect the existing data and skip it.

### Development: two processes

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npx ng serve --proxy-config proxy.conf.json
```

UI on http://localhost:4200, API proxied to 8080.

### Docker

```bash
docker build -t acme-salary . && docker run -p 8080:8080 -v salary-data:/data acme-salary
```

The image is what deploys to Render/Fly/Railway as a single service; the SQLite file lives on the
mounted volume.

## Tests

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npx ng test --watch=false --browsers=ChromeHeadless
```

68 tests, all deterministic — fixed clock, fixed FX table, fixed seed; nothing touches the
network. The split: domain rules (timeline invariants, Money/FX arithmetic) as plain JUnit with
no Spring; API behaviour via MockMvc against in-memory SQLite, with the aggregate maths asserted
against a five-person org small enough to check by hand; seeder properties verified over a full
10,000-person generation; UI formatting and chart scaling in Jasmine.

## API sketch

```
GET  /api/employees                     search/filter/sort/page (all in SQL; page size capped)
GET  /api/employees/{id}                profile + full compensation history
POST /api/employees                     hire (creates person + opening salary atomically)
PUT  /api/employees/{id}                edit profile (never touches compensation)
POST /api/employees/{id}/compensation   record a raise/promotion/correction — appends, never edits
GET  /api/employees/export              current filtered view as CSV (streamed)
GET  /api/insights/dashboard            overview, breakdowns, distribution, pay equity
GET  /api/reference-data                dropdown contents in one call
```

Business rejections (backdated raise, wrong currency, second hire record) return **422** with a
message written for the HR manager, and the UI shows it verbatim.
