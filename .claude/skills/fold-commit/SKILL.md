---
name: fold-commit
description: >-
  Dissolve a throwaway "WIP"/refactor/fixes commit by folding each of its changes
  into the feature commit it belongs to, so the branch history looks as if the
  changes were always there (no leftover commit on top). Use when the user says
  things like "слить коммит с общей работой", "разнести коммит по родным коммитам",
  "влить via-коммит в ветку", "fold/squash this commit into the feature commits",
  "integrate this refactor/fixes commit into history". Often run right after
  /code-review. Rewrites branch history via a conflict-free last-touch fixup rebase.
---

# fold-commit — fold a WIP commit into the feature commits

A developer reviews a feature branch, makes a pile of fixes/refactors, and commits
them as one throwaway commit on top (often named "via", "wip", "fixes"). This skill
**dissolves that commit**, distributing each hunk into the feature commit that
introduced the code it touches, so the final history is clean and the throwaway
commit disappears — with **zero change to the net tree**.

It is usually preceded by `/code-review`. This skill does NOT review; if the user
asked for a review too, run `/code-review` first, then fold. Match the review effort
to the change: a small WIP folded into a few commits does not need a high/ultra
multi-agent review — that pass can cost orders of magnitude more than the fold
itself. Reach for high/ultra only when the user asks or the diff is large/risky.

## Inputs (ask only if you can't infer them)

- `WIP` — the commit to dissolve. Default: `HEAD`.
- `MAIN` — the branch the feature forks from (for the fork point). Default: try
  `dev`, then `main`, then `master`. `FORK = git merge-base <MAIN> <WIP>`.
- `FIRST` — the first feature commit, `FORK`'s child on this branch. Used as the
  home for "orphan" changes (see below).

Confirm the resolved `WIP`, `FORK`, and `FIRST` with the user in one line before
rewriting history if any of them was guessed.

## The core invariant (why this is conflict-free)

Take every file's delta **relative to the tip-without-WIP** (i.e. `WIP^`), and
fold it into that file's **last-touch** feature commit — the most recent feature
commit that modified the file. Because nothing between the target and the tip
touches that file, its content at the target equals its content at `WIP^`, so the
fixup patch applies with no conflict. Hold to this and the rebase is clean.

## Procedure

**Decide everything before you mutate.** Do the whole mapping read-only (step 2),
resolve every orphan/alien/ambiguity question in a *single* user round-trip, then
run backup → reset → fixups → rebase mechanically. Re-inspecting the tree between
mutations is the main way this skill wastes effort — plan once, execute once.

1. **Preconditions & inputs.** Require a clean working tree (`git status
   --porcelain` empty). If dirty, stop and ask the user to commit/stash — do not
   proceed. Resolve `WIP`, `FORK`, `FIRST`. **If the user then commits/amends to
   clean the tree, re-resolve `WIP`/`FORK`/`FIRST`** — an amend changes WIP's SHA
   and may sweep extra files (config, skills, settings) into it.

2. **Plan the mapping (read-only, one pass) against `<WIP>^..<WIP>`.** Renames and
   splits must be detected here, or rename targets show up as bogus "orphans" and
   cost extra round-trips. Run both:
   ```
   git diff -M --summary <WIP>^ <WIP>          # renames / creates / deletes at a glance
   git diff -M --name-status <WIP>^ <WIP>       # R = rename, A = add, D = delete, M = modify
   ```
   Last-touch of any path: `git log -1 --format='%h' <FORK>..<WIP>^ -- <path>`.
   Assign each path to a target with these rules:
   - **Modified / deleted** file → its own last-touch commit.
   - **Rename** `A => B` → `A`'s last-touch; put the delete of `A` **and** the add
     of `B` in that one fixup so the rename stays atomic.
   - **Split** (a deleted `P` plus new children carved out of it, children have no
     last-touch) → `P`'s last-touch; stage `P` + all children together.
   - **Orphan** (new file, no related delete, no last-touch) → `FIRST`, unless it
     clearly belongs with a specific feature (e.g. a chat-list state file → the
     chat-list commit).
   - **Alien** (file unrelated to the feature, swept into WIP by an amend —
     tooling/config/skill/settings) → do **not** fold into a feature commit. Ask
     the user: separate commit on the tip, or leave uncommitted. Note that an
     excluded file makes the step-7 verify differ by exactly that file until it is
     committed/restored, so account for it.

   Present the full file→target map. Ask **once** only if a non-trivial orphan is
   ambiguous or an alien file needs a decision — batch all such questions together.

3. **Backup.** `git tag -f backup/fold-<shortsha> <WIP>` and tell the user the tag
   name. This is the undo point (`git reset --hard <tag>`).

4. **Rewind the WIP commit, keep its changes.**
   `git reset --mixed <WIP>^`
   Now HEAD is the last feature commit and all of WIP's changes sit unstaged in the
   working tree (rename/split sources show as deletes, targets as untracked). If WIP
   is several stacked throwaway commits, reset to the oldest one's parent so the
   combined delta lands in the tree.

5. **Build one `fixup!` per target** using the map from step 2. For each target,
   stage exactly its files (include rename sources + targets / split parents +
   children together) and commit:
   ```
   git add -A <files for this target>          # -A captures deletes, renames, untracked adds
   git commit -q --fixup=<target-sha>
   ```
   Leave any **alien** file unstaged for now. After the last group, **verify
   `git status --porcelain` shows only the deliberately-excluded alien file(s)** —
   any other leftover means a path was dropped and the rebase will silently lose it.

6. **Autosquash, non-interactively.**
   ```
   GIT_SEQUENCE_EDITOR=true GIT_EDITOR=true git rebase -i --autosquash <FORK>
   ```
   (`-i` is required for autosquash; `GIT_SEQUENCE_EDITOR=true` accepts git's
   auto-arranged todo, so it runs without an editor.) Then handle any alien file as
   agreed (e.g. its own commit on the tip).

7. **Verify (mandatory gate).**
   - `git diff --quiet backup/fold-<shortsha> HEAD && echo IDENTICAL` — the net
     tree MUST be byte-identical to the original WIP. If an alien file was excluded,
     it is the *only* allowed difference (`git diff --name-only backup/... HEAD`
     should list just that file) — once it's committed on the tip, this becomes
     IDENTICAL too. Any other DIFFERENT means you mis-assigned or dropped a file:
     restore (`git reset --hard backup/fold-<shortsha>`) and redo the map.
   - `git status --porcelain` empty.
   - `git log --oneline <MAIN>..HEAD` contains no `fixup!` and none of the
     throwaway commit's subject.

8. **Report.** Summarize which feature commit absorbed which changes. Note that
   every commit SHA on the branch changed (a rebase rewrites them), so a previously
   pushed branch needs `git push --force-with-lease`. **Do not push** unless the
   user asked. Leave the backup tag; tell the user to delete it with
   `git tag -d backup/fold-<shortsha>` once satisfied.

## If the rebase hits a conflict

It shouldn't, if the last-touch invariant held. If it does, the correct final
content for the conflicting file is known — it's that file's version in the backup
tag (`git show backup/fold-<shortsha>:<path>`). Resolve toward it, `git add`, and
`git rebase --continue`. If anything looks off or you're unsure, do not guess on
history: `git rebase --abort` then `git reset --hard backup/fold-<shortsha>`, and
report what conflicted so the user can decide.

## Guardrails

- This rewrites history — treat it as hard-to-reverse. Always create the backup
  tag first, and never `push` without an explicit request.
- When a hunk's true home is ambiguous (cross-cutting refactor of pre-existing
  code), prefer asking over guessing; the file→commit map is cheap to show.
- When verifying a suspicious diff/line during the paired review, check the real
  bytes (`sed -n 'Np' file | xxd`) — terminal/`git diff` rendering can hide
  non-printable characters and mislead you.
