# Kai Offline Speech Input — Handoff Package

Prepared 2026-11-19 by EL and ChatGPT.

Start here:

1. [Maintainer handoff report](kai-offline-speech-input-handoff-2026-11-19.md) — project summary, build evidence, device-test results, PR boundaries, and the decision requested from Simon.
2. [Safe reconnection list](kai-development-connection-safe-list-2026-11-19.md) — repository, Codespaces, GitHub CLI, Termux, Debian, wireless ADB, build command, and package identity. Contains no credentials or one-time codes.
3. [Architecture and workflow diagrams](kai-offline-speech-architecture-2026-11-19.md) — speech pipeline and contributor-to-maintainer handoff map.
4. [Proposed speech bug-report form](../../.github/ISSUE_TEMPLATE/speech_input_bug_report.yml) — structured device, OS, environment, expected/actual behavior, and reproduction details. It becomes active only if integrated into the default branch.

Evidence:
- [Device-validation checkpoint](../device-validation-2026-11-19.md)
- [GitHub Actions Run #60](https://github.com/ElPetros/Kai/actions/runs/44472952350)
- [PR #4 — remains Draft and unmerged](https://github.com/ElPetros/Kai/pull/4)

**Important:** Run #60 validates `fix/speech-vad-60s-hardening` at `d816db8`. Do not treat it as validation of PR #4's current head without checking the branch relationship. The APK artifact expires 2026-11-26.
