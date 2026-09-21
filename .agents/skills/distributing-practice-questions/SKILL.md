---
name: distributing-practice-questions
description: Use when a scratch file of practice or interview Q&As must be sorted into topic practice pages, de-duplicated into one primary question, or rewritten into memorable answers
argument-hint: "[scratch-file]"
---

# Distributing Practice Questions

A scratch file (e.g. `docs/temporary-practise.md`) collects raw practice Q&As. This skill turns it
into per-topic practice pages under `docs/topics/<slug>/practice.md` — de-duplicated and rewritten
so the answers are easy to recall.

**REQUIRED READING:** `docs/spec/curriculum.md` (slug registry) and
`docs/spec/documentation-conventions.md`. The canonical banks
(`docs/topics/<slug>/questions.md`) are a **different deliverable** — see Rules.

## Output shape

One page per topic that receives at least one question: `docs/topics/<slug>/practice.md`

```markdown
# <Topic> — Practice Q&A

Quick-recall questions. Answers are collapsed; think before revealing.

### Q1. <Primary question>

Merged from: Q1 + Q2
Canonical: [Q4 — <title>](../<slug>/questions.md#<heading-anchor>)
Also relevant: [Core Java](../core-java/index.md)

??? question "Reveal answer"
    <easy-to-remember answer>

### Q2. ...

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
```

- `<slug>` is taken **verbatim** from the curriculum slug registry; the folder must already exist.
- Numbering restarts per page: `### Q1.`, `### Q2.`, …
- `Merged from:`, `Canonical:` and `Also relevant:` go **directly under the `### Qn.` heading**, above
  the admonition — they are provenance, not answer text, so they stay visible while the answer is
  collapsed. Emit each line only when it applies.
- `Canonical:` links the same-intent question in that topic's `questions.md` (anchor to its heading)
  when one exists; omit it otherwise.
- `Also relevant:` links the sibling topic's **`index.md`** when the question also belongs to a
  second topic — never a concept page, never a second copy of the question.
- Nav entry: label it `Practice Q&A` and place it **directly after that topic's `Interview Questions`**
  line. Link it from `docs/topics/<slug>/index.md`.
- End every page with `## Related` linking at least `concepts.md` and `questions.md`.

## Steps

1. **Read the whole scratch file.** Number every Q&A before classifying anything.
2. **Classify** each Q&A to exactly one slug from the registry. Put secondary topics on an
   `Also relevant:` line — never a second copy.
3. **De-duplicate.** Same intent → one **Primary Question**. Prefer the clearest source wording;
   if no single variant covers every one, write a new short question that does. Record the variants
   under it as `Merged from: Q1 + Q2`.
4. **Check the repo for collisions.** *Same intent* means the same concept **and** the same expected
   answer — a related-but-different question stays separate. An existing `practice.md` question with
   the same intent is updated in place, never duplicated. A match in the canonical `questions.md` is
   **not** a reason to skip the practice question, and **never** a reason to edit `questions.md` —
   add a `Canonical:` link instead (see Rules).
5. **Rewrite each answer** using the Answer contract below.
6. **Write or update** `docs/topics/<slug>/practice.md` for every topic that received a question.
7. **Wire it up:** add the `Practice Q&A` nav entry and link the page from the topic `index.md`.
8. **Report** the distribution table (below). Leave the scratch file in place unless asked to clear it.

## Answer contract (easy-to-remember)

Every answer is a **bold one-line verdict**, then **3–6 short bullets**, then a **one-line takeaway**.
Use plain indented bullets inside the admonition — the repo's existing question style — not blockquotes.
This deliberately differs from the canonical `questions.md` reveal contract (which prefixes
`**Short Answer:**`): practice pages are recall aids, not the canonical bank.

- **Plain words.** Say "the object is a different instance", not "reference inequality holds".
- **Short sentences.** One idea each; aim under ~15 words.
- **Bold the key terms and the results** (`false`, `visibility`, `atomicity`, `at-least-once`).
- **Bullets for anything list-shaped**: causes, fixes, steps, trade-offs.
- **Always say the fix.** Every "why is this broken" answer ends with what to do instead.
- Keep it accurate: never drop a caveat just to make it shorter.

```markdown
??? question "Reveal answer"
    - **<One-line verdict.>**
    - <What happens.>
    - <Why it happens.>
    - <Consequence in practice.>
    - **Fix:** <what to do instead.>
    - **Takeaway:** <one memorable line.>
```

## Rules

| Rule | Why |
|---|---|
| Write `practice.md`, never `questions.md` | `questions.md` is the canonical bank: level sections, `--8<--` example classes, sequential numbering. Practice pages are lightweight recall aids. |
| A `questions.md` match is linked, not merged | The canonical bank keeps its own levels and example classes; the practice page stays a recall aid. |
| The practice page answers; it does not re-explain | `concepts.md` is the canonical explanation — link it, never repeat it. |
| One primary slug per question | A question stored under two topics drifts; questions are found by topic. |
| Slug from the registry, verbatim | Never invent a topic folder — route an off-topic question to the nearest existing slug. |
| Merged questions keep the clearest wording | The Primary Question is what gets recalled, so it must be short. |
| Illustrative fragments stay inline; real lab code is a `--8<--` snippet | A short `em.find(...)` fragment is teaching text; copies of `modules/` source drift. |
| The scratch file is not published content | Never add it to nav or link it from topic pages — it is raw input. |
| Re-running is idempotent | The scratch file grows over time — distribute only what is new, update what changed. |

## Distribution report

End with a table so the user can see where everything landed:

| Topic (slug) | Questions | Merged from |
|---|---|---|
| jpa-hibernate | 1 | Q1 + Q2 |
| concurrency | 1 | Q3 + Q4 |

Plus one summary line: `N scratch Q&As → M unique questions across K topics`.

## Common mistakes

- Routing practice questions into `questions.md` and inheriting its example-class and numbering work.
- Editing `questions.md` to "avoid duplication" — link the canonical question instead.
- Answering "what is X" but never "so what breaks / what do I do instead".
- Merging duplicates by keeping the *longest* question instead of the clearest.
- Duplicating an existing `practice.md` question instead of updating it in place.
- A new page left out of `mkdocs.yml` nav, so it never appears in the site.
- Treating the scratch file as a real page — adding it to nav or linking it.
- Inventing a slug folder for an off-topic question instead of routing it to the nearest topic.
