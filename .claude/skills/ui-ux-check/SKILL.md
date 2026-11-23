---
name: ui-ux-check
description: An unbiased check of the Offline Translator's UI from a user's point of view, someone who knows nothing about why the app is built the way it is and only needs it to work and feel obvious. Reports what the app is lacking, most important first. Use it whenever the owner asks what the app is missing, for a UX or UI review, a usability or accessibility audit, "what would a new user think", or a check of one screen (Type or speak, Conversation, Manage languages, Settings) before or after a change, even if they don't say "review".
---

# UI/UX check

Judge the app the way a person who just installed it would: they never read a spec, a rule or a commit message, and
they don't care why something is the way it is. They only notice whether they can tell what to do, whether it worked,
and whether it felt natural. The result is a short report of what gets in their way, most important first. Nothing is
changed unless the owner asks.

## Why the order of the steps matters

Whoever built a screen knows why each choice was made, and that knowledge quietly excuses problems a user would trip
on ("that's on purpose", "the help explains it"). So the review looks at the app before reading anything that
explains it, writes its findings down, and only then opens code or documents, to locate fixes. Reasons found later
never remove a finding: a user still runs into it. If the owner decided otherwise in `docs/ui-rules.md`, the finding
stays in the report with that rule named, and the owner chooses.

## 1. The blind pass: only what a user sees

**Run it in a fresh agent when the Agent tool is available.** A session that has been building the app carries its
reasons in its context. Give a new general-purpose agent only this skill's "blind pass" instructions, the list of
screenshot files (or the emulator's screen captures) and the tasks below, and nothing about the specs, the rules, the
code or the conversation. Without that tool, do the pass yourself and look at the images before any code.

What the blind pass may look at:

- **The screens as drawn:** the screenshot baselines in `androidApp/src/test/screenshots/` (light, dark, 200 % text,
  small windows), or, when the owner asks for it, the app running on the emulator (`scripts/ci.sh` owns the emulator;
  on this machine heavy runs take turns through `flock /tmp/onova-heavy-build.lock`). Make sure the screenshots match
  the code being checked: when screens changed after the last re-record, re-record first or use the emulator.
- **The words on screen,** as the user reads them, in the language of the screenshot.

What it must not open: `docs/`, `specs/`, `CLAUDE.md`, code comments, commit messages, help texts' sources.

### The tasks a user comes to do

Walk through each one screen by screen. At every step ask: would a first-time user know what to tap next? After the
tap, can they tell it worked? If something went wrong, do they know why and what to do?

1. Translate a sentence by typing it; then by speaking it; hear it read aloud.
2. Have a spoken conversation with someone across the table who speaks another language; the other person has never
   seen the app.
3. Use a language that isn't on the phone yet: notice that, get it, and know when it's ready.
4. Find out what works offline and what still needs a download; free some space.
5. Fix a misheard sentence before or after the other person hears it.
6. Change the app's look or language; find help.

### What to judge, in a user's words

- **Can I tell what to do?** One obvious next step; controls look like controls; labels say what happens.
- **Did it work?** Every tap answers at once; every wait shows progress; success and failure are unmistakable.
- **Do I understand the words?** Plain, everyday words; the same word for the same thing on every screen; nothing cut
  off or wrapped badly; the three app languages (English, Portuguese, Spanish) read naturally.
- **Can I recover?** Back does what I expect; nothing I typed or said is lost by mistake; a destructive action asks or
  can be undone; errors say what to do next.
- **Can everyone use it?** Readable at 200 % text and in a small window; enough contrast in light and dark; nothing
  told by colour alone; big enough to tap; a screen reader would have something to say for every control.
- **Does it feel calm?** Nothing flashes, jumps or moves while I use it; the screen isn't crowded; the important thing
  is the biggest thing.

Write every finding as what the user runs into, with the screenshot (or step) that shows it. Don't propose fixes yet.

## 2. Locate and suggest fixes

Now open the code to find where each finding lives (`feature/*/presentation/`, `core/designsystem/`, the strings in
`composeResources/values*/strings.xml`) and suggest a fix that fits the app's components. Then read
`docs/ui-rules.md`: where a finding goes against one of the owner's rules, keep it and name the rule, because only the
owner can decide to change it.

## Report

In the owner's language for this conversation:

```markdown
# What <screen or "the app"> lacks, as a user sees it

## Blocks or confuses
1. **<what the user runs into, one line>** — task <n>, <screen>. <what happens, in a user's words>.
   Seen in: `<screenshot>` / step. Where: `<file:line>`. Fix: <one or two sentences>. Effort: small / medium / large.
   <If it goes against an owner's rule: "Your rule <id> says <one line>; changing it is your call.">

## Annoys or slows down
- **<finding>** — <screen>. Seen in: <...>. Fix: <...>.

## Polish
- ...

## Works well
- <two to five things a user would like, so they aren't undone>
```

Order by what a person would feel: blocked or confused first, then annoyed, then polish. Five to ten real findings
beat thirty small ones, and each needs what shows it, so the owner can see it too. Then offer to turn the findings the
owner picks into tasks in the current spec.
