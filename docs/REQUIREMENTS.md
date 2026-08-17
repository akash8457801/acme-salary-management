# Requirements — ACME Salary Management

**Persona:** Priya, HR Manager at ACME. ~10,000 employees, multiple countries and currencies.
Today she keeps compensation in a set of Excel workbooks that are emailed around, so she cannot
trust the numbers and cannot answer questions quickly.

## Goal

Replace the spreadsheets with a web application that lets an HR manager (a) maintain accurate,
auditable salary data for every employee and (b) answer questions about **how the org pays people**
in seconds instead of hours.

The two jobs-to-be-done, in the persona's words:

1. *"Sanjay is being promoted to Senior — record his new salary from 1 September, and don't lose
   what he used to earn."*
2. *"What do we spend on payroll in Germany? Is Engineering paying women less than men at the same
   level? Who are our 20 most expensive people?"*

## Scope — what ships

**Employee & compensation records**
- Employee directory of 10,000 seeded employees: name, employee code, email, department, job title,
  level (L1–L7), country, manager, hire date, employment status.
- Server-side search (name / code / email), filter (department, country, level, status, salary
  band), sort and pagination. 10,000 rows never travel to the browser.
- Employee detail page with the full **compensation history** as a timeline.
- Record a compensation change (raise, promotion, market correction, demotion) with an effective
  date and a reason. History is append-only: the previous record is closed off, never overwritten.
  This is the single most important invariant in the system — it is what a spreadsheet cannot give you.
- Create and edit employees.
- CSV export of the currently filtered view, so Priya can still hand a spreadsheet to Finance.

**Compensation insights** (the "answer questions" half of the product)
- Org overview: headcount, annualised payroll cost, mean/median salary.
- Breakdown by department, country, and level: headcount, total cost, median, p25/p75.
- Salary distribution histogram, with outlier detection (people paid far outside their level band).
- Pay-equity view: median pay by gender within a level, and the resulting gap.
- Top earners.

**Multi-currency.** Salaries are stored in the currency they are paid in. Every record also carries
an annualised USD figure computed at write time from a versioned FX table, so that cross-country
aggregation is a plain SQL `SUM` rather than 10,000 conversions per request.

## Out of scope — and why

| Left out | Reasoning |
|---|---|
| Authentication, users, RBAC | The brief specifies exactly one persona. Adding login would cost real effort and demonstrate nothing about the compensation domain, which is where the interesting modelling is. The API is designed so an auth filter drops in front of it unchanged. |
| Payroll *execution* — payslips, tax, deductions, disbursement | This is a system of record for *what we pay*, not a payroll engine. Tax logic is jurisdiction-specific and would swamp the domain. |
| Bonuses, equity, benefits, total-rewards modelling | Real, but each is its own model. Base salary first; `CompensationRecord` is deliberately shaped so a `component` dimension can be added without migrating the timeline logic. |
| Live FX rates from an external provider | An external call would make tests non-deterministic and reports irreproducible. A versioned rate table is *more* correct for reporting anyway — you want last quarter's report to still produce last quarter's numbers. |
| Approval workflows / maker-checker | Genuinely needed at 10k employees, but it is a process feature layered on top of an audit trail. The append-only history is the foundation it would be built on; the workflow itself is not what this exercise is testing. |
| Bulk Excel *import* | Would be the real migration path in production. Export is included because it is cheap and unblocks Priya's downstream reporting; import needs validation/dry-run/rollback UX that is a project of its own. |
| Org-chart visualisation | `managerId` is modelled and seeded, so the data is there. Drawing the tree is presentation polish, not a compensation question. |
| i18n / accessibility beyond baseline | Single-locale internal tool. Semantic markup and keyboard access come from the component library; full a11y audit is out. |

## Non-functional targets

- Employee list and every insights query respond in **< 300 ms** on the 10,000-row seed.
- Seeding is **deterministic** (fixed random seed) — the same data every run, so demos, screenshots
  and tests all agree.
- Money is never a `double`. `BigDecimal` end to end, currency always travels with the amount.
- Core domain logic (compensation timeline, FX, banding, statistics) is unit-tested without a
  database; the API is covered by fast integration tests against an in-memory SQLite.

## Success criteria

Priya can, in one sitting and without opening Excel: find an employee, give them a raise effective
next month, see what they used to earn, and tell her CFO what ACME spends on Engineering in Germany.
