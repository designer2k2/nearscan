# Contributing to NearScan

**Everything is welcome here.** Bug reports, half-formed ideas, a one-word typo fix, a whole new
feature, a translation into a language you speak, a screenshot, a question that turns out to be a
docs gap — all of it helps, and none of it is too small or too rough.

If you're not sure whether something is wanted, or whether your idea fits: **open an issue and
ask, or just open a draft pull request.** The worst case is a friendly conversation. You will not
be told off for trying.

There is **no Contributor License Agreement**, no checklist you have to perfect before you're
allowed to post, and no expectation that you know Android, Kotlin, or Git well. First-time
contributors are genuinely welcome, and the maintainer is happy to help you get a change over the
line.

---

## Ways to contribute (you do not need to write code)

- **Report a bug** — even "it feels slow on my phone" is useful. See [Reporting bugs](#reporting-bugs).
- **Suggest a feature or change** — open an issue describing what you'd like and why. Rough is fine.
- **Test it on your device** — try a build on hardware/Android versions the maintainer doesn't
  have and report back what works and what doesn't.
- **Translate** — NearScan ships in 10 languages; adding or fixing one is very welcome, and doing
  only part of a language is fine. See [Translations](#translations).
- **Improve the docs** — the README, this file, in-app help text, code comments. If something
  confused you, that's a bug in the docs.
- **Contribute screenshots or store assets** — clean screenshots (with a *fake* location set!),
  icons, graphics.
- **Triage** — reproduce reported bugs, add detail, link duplicates, suggest labels.
- **Tell people about it** — blog posts, forum answers, a mention in a group chat.

## Reporting bugs

Open a [GitHub issue](https://github.com/designer2k2/nearscan/issues). Include whatever you can —
partial information is still worth posting:

- What you did, what you expected, what happened instead
- Your phone model and Android version
- NearScan version (Settings / About, or the APK you installed)
- Which scan types were on, and roughly how long the session ran
- A logcat snippet if you can grab one:
  `adb logcat | grep -i nearscan` (feel free to trim anything you'd rather not share)

Screenshots help a lot — just **blank out or fake your coordinates** first.

## Suggesting features

Open an issue. Say what problem you're trying to solve, not just the solution you have in mind —
that leaves room for a better approach to surface. "Would you accept a PR for X?" is a perfectly
good issue.

Things that are explicitly in scope: more export formats, more RF metadata, better automation
hooks, battery and reliability improvements, accessibility, translations. Things that need
discussion first: anything that adds a network dependency, a tracking/analytics component, or a
proprietary library — NearScan is deliberately FOSS with no backend, and F-Droid-eligible.

## Development setup

Requirements: **JDK 17** and the Android SDK (compile/target SDK **36**).

```bash
git clone https://github.com/designer2k2/nearscan.git
cd nearscan

export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

./gradlew assembleDebug        # -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
```

No signing keystore is needed for debug builds or tests — the release `signingConfig` is skipped
automatically when `keystore.properties` is absent.

Architecture, module layout, and implementation notes are in [`CLAUDE.md`](CLAUDE.md).

## Pull requests

The bar is low. In rough order of "nice to have":

1. **It builds** (`./gradlew assembleDebug`).
2. **Tests pass** (`./gradlew testDebugUnitTest`), and if you changed logic, a test covering it is
   appreciated — but a PR without tests is still worth opening.
3. **Keep it reasonably focused** — one change per PR makes review easier. If your PR grows a
   second unrelated fix, that's fine too; we can sort it out.
4. **Match the surrounding code** — the existing style, naming, and comment density. There's no
   enforced linter to fight with.
5. **Conventional Commit messages** (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`) are
   appreciated but **not required** — the maintainer can tidy the message on merge.
6. Don't worry about squashing, rebasing, or a perfect history. The maintainer will squash on
   merge if needed.

Draft PRs are welcome for work in progress or to get early feedback. CI runs the build and tests
on every PR.

### If your change adds or edits user-facing text

Strings live in `app/src/main/res/values/strings.xml` (English) and `values-<lang>/strings.xml`
for the others. Adding a new string ideally means adding it to all locales, but if you only speak
one or two languages, **add what you can** and note it in the PR — a partial translation is
better than none, and someone else can fill the rest.

## Translations

NearScan is translated into: English, Spanish, Chinese (Simplified), Hindi, Portuguese (Brazil),
Russian, Japanese, German, French, Korean.

To improve one:

1. Edit `app/src/main/res/values-<lang>/strings.xml` (e.g. `values-de` for German).
2. Keep the `name="..."` keys identical to `values/strings.xml`; only translate the text.
3. Leave any `%1$s` / `%d` placeholders and `\n` / `\'` escapes intact.
4. Open a PR — even a single corrected phrase is a valid contribution.

New language not on the list? Open an issue or a PR adding a `values-<lang>/` directory; the
maintainer will help wire it in.

## Code of conduct

Be kind and assume good faith. Harassment, discrimination, or hostility toward other
contributors is not tolerated, and the maintainer may edit, remove, or block as needed. Disagree
about the code all you want — do it respectfully.

## Security and privacy issues

If you find a vulnerability or a privacy leak, please **open an issue describing the class of
problem** (e.g. "exported component X is reachable without permission") rather than posting a
working exploit or a step-by-step extraction path. If you'd rather report privately first, say so
in the issue and the maintainer will follow up.

## Licensing of contributions

NearScan is [MIT licensed](LICENSE). By submitting a contribution you agree that it is licensed
under the same MIT terms. Don't paste in code you don't have the right to relicense.

---

Thanks for helping. If any of the above is a barrier, ignore the part that's in your way and open
the issue or PR anyway.
