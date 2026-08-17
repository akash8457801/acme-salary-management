# AI workflow

This project was built with Claude Code (Opus) as the implementation partner, working from
directions I set. This document records how, because *how* AI is used is part of the assessment.

## The process

**1. Product thinking before any code.** The first artefacts were REQUIREMENTS.md,
ARCHITECTURE.md and DECISIONS.md, committed before a single Java file existed. Writing the
decision log first forced the important call — salary as an append-only timeline rather than a
mutable column — to be made deliberately, not stumbled into halfway through a controller.

**2. Domain first, framework second.** The compensation timeline, Money and FX types were built
and tested as plain Java before Spring entered the picture. This ordering is deliberate for AI
work: the domain layer is where subtle bugs are expensive, so it gets tests with hand-computed
expected values (80,000 EUR × 1.0850 = 86,800 USD — checked on paper, not against the code's own
output). If a generated implementation is wrong, a test asserting the code's own output would
enshrine the bug.

**3. Small verified increments.** Each layer was compiled, tested and run before the next:
domain → repositories/API → seeder → UI. When something failed (SQLite refusing to create its own
directory, MockMvc needing an async dispatch for the streaming CSV), the fix was made and
verified before moving on. Nothing was committed untested.

**4. AI writes, judgment stays human.** The choices AI was *not* allowed to make by default:
stack (Java/Angular per the JD), the timeline model, denormalising USD at write time, seeding
through the domain rather than bulk INSERTs, hand-rolled SVG over a chart library. Each of those
is a trade-off recorded in DECISIONS.md with the rejected alternative.

## Prompt patterns that mattered

- **Persona-grounded framing:** "the HR manager needs to answer what did she earn before the
  promotion" produced better domain modelling than "build a salary CRUD".
- **Invariants as prompts:** listing the timeline rules (one open record, no gaps, no overlaps,
  supersede-never-rewrite) and asking for the object that owns them — rather than asking for
  "an entity" — is what put the rules in one testable place.
- **Hand-checkable test data:** asking for tests whose expected values can be verified on paper
  (a five-person org for the aggregate maths) instead of asserting whatever the code returns.
- **Honest failure reporting:** test failures were pasted back verbatim and fixed at the root
  (e.g. the CSV test needed `asyncDispatch`, not a looser assertion).

## Where the AI saved the most time

Boilerplate-heavy layers (DTO mapping, Material form scaffolding, criteria queries), the seed
data generator's realistic distributions, and the SQL median/percentile window queries — each of
which would have been an afternoon of reference-checking by hand.

## Where human review caught things

- The first seeder draft stored pre-computed USD amounts directly; it was reworked to write
  through `CompensationTimeline` so seeded data obeys the same invariants as user-entered data.
- The generator originally compounded raises *forward* from a starting salary, which produced
  absurd senior salaries after a few years; inverted to work *backwards* from a believable
  current salary.
- `Date.toISOString()` in the raise dialog would have shifted effective dates by a day for any
  user east of UTC; replaced with a local-date formatter and pinned with a test.
