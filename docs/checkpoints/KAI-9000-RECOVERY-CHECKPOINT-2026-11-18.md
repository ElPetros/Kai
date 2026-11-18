# Kai 9000 Speech Edition — verified recovery checkpoint (2026-11-18)

**Purpose:** This is the latest recovery handoff after the November 18 continuity investigation. Use it first when resuming. It supplements, and does not replace, the earlier resume checkpoint.

## 1. Project identity — do not cross streams

- Project: **Kai 9000 Speech Edition**.
- GitHub repository: [ElPetros/Kai](https://github.com/ElPetros/Kai), a fork of SimonSchubert/Kai.
- Primary speech implementation branch: `feature/offline-speech-input`.
- Isolated benchmark development branch: `feature/speech-benchmark-harness` (based on the verified recovery checkpoint).
- Benchmark branch head immediately before this checkpoint update: `1fa9dba4e8b0f6f610507dcb9006231ebb50817a`.
- Original speech implementation head before checkpoint-only updates: `e00d1cbba0ec59fc4b30d1902f7a19b07ce4512c`.
- Verified commit: [Add controlled offline speech model evaluation plan](https://github.com/ElPetros/Kai/commit/e00d1cbba0ec59fc4b30d1902f7a19b07ce4512c).
- Open draft PR [#1 — Add Android offline speech input](https://github.com/ElPetros/Kai/pull/1) remains the original speech feature PR, targeting `main`; do not merge it as part of benchmark work.
- Open draft PR [#2 — Add isolated offline speech benchmark scoring core](https://github.com/ElPetros/Kai/pull/2) targets `feature/offline-speech-input`, keeping benchmark work separate from PR #1.
- Other known branches: `checkpoint/kai-speech-edition-2026-11-18` and `feature/offline-speech-input-privacy-hardening`. Do not switch or merge branches casually; inspect first.
- **Kai and Chat Sentinel are separate projects.** Do not run Chat Sentinel commands, modify its repository, or mix its HIB work into this Kai task.

## 2. What is verified complete

- Android-only offline speech-input implementation is committed on the active feature branch.
- Runtime: sherpa-onnx 2.0.1; model baseline: quantized Moonshine v2 Tiny English.
- Audio: 16 kHz mono PCM; maximum recording duration 60 seconds; Stop-to-transcribe.
- First-use model download is size/hash checked; model is stored in app-private storage and reused offline.
- Transcript is inserted into the composer as editable text; it is not auto-submitted. Existing selection/caret behavior is preserved.
- Speech control is in the composer. Generated UI fields and terminal command drafts can receive dictated text; terminal commands remain drafts and are never automatically executed.
- Lifecycle/session cancellation and temporary PCM cleanup are implemented.
- Debug test build uses application ID suffix `.speechtest`, so it can coexist with the normal Kai app.
- On 2026-11-17, two successful stop-to-transcribe tests were completed on the Samsung Galaxy A36 (SM-S366V). The user reported both transcripts matched the spoken words exactly. **Do not ask the user to repeat these tests just to re-establish continuity.**
- The recording shows successful local transcription and transcript review/insertion. A later Free FAST / Free EXPERT provider limit occurred after submission; that is a separate AI-provider limitation, not an ASR failure.
- The existing checkpoint records successful Android build and unit-test workflows at implementation head `34215393be02322bf7dcb64e16a13a39bfd6d5c4`. Later documentation/evidence-index commits followed. Do not imply fresh CI was run for the latest documentation-only head; check CI if a new code change is made.
- The controlled model evaluation plan is committed at `docs/speech-engine-evaluation-plan.md`. It keeps Moonshine as the working baseline and identifies Zipformer GigaSpeech as a challenger. No comparative Galaxy A36 benchmark has been run.

## 3. Evidence state — important distinction

- The evidence index/README is committed at `docs/evidence/kai-speech-device-test-2026-11-17/README.md`.
- The prepared ZIP `kai9000-speech-evidence.zip` contains seven still images and a README, but it was delivered in the ChatGPT conversation workspace.
- **The JPEG binary evidence files are not yet committed to GitHub.** The index is committed; the actual images still need to be added from the ZIP if they are available in the user's storage.
- Do not claim the ZIP or its image binaries are stored in GitHub, Debian, the phone, SD card, or persistent Library storage until that location is directly verified.
- The GitHub Actions debug APK artifact was previously reported available until 2026-11-25. Treat that retention date as historical checkpoint information; verify artifact availability before relying on it.

## 4. November 18 recovery investigation — findings

### GitHub is the source of truth for committed project state

- The active PR head and evaluation-plan commit were fetched and matched at `e00d1cbba0ec59fc4b30d1902f7a19b07ce4512c`.
- The existing resume checkpoint was fetched from the checkpoint branch. It remains useful, but it predates the latest evaluation-plan commit and this recovery investigation. This file is the newer handoff.
- PR #1 was fetched and verified open, draft, and unmerged. No merge or release was performed.
- The linked GitHub workflow configuration `.github/workflows/test.yml` is CI automation, not a Kai application settings file.

### The `Kai_settings.json` lead

- A repository search and recursive branch-tree inspection previously found no `Kai_settings.json` on the three known Kai branches.
- The only similarly named file found was `.claude/settings.local.json`; it is a Claude Code local permission/command allowlist, **not** a general Kai app settings or storage configuration file.
- The user also mentioned `.github/workflows/test.yml`; that is a GitHub Actions CI workflow, not the missing settings file.
- No conclusion has been reached that the user's notebook reference was mistaken. It may refer to a file outside the repository or to a different location. Do not spend another long session searching blindly unless that file becomes necessary for a specific task.

### Debian / local workspace identity

- The inspected Kai Build shell was Debian 12 Bookworm ARM64, with `HOME=/root`.
- The inspected `/root/projects` contained `elpetros-chat-sentinel` and `test-repo`; the latter had no commits and no remote. It was not verified as Kai.
- `/root/projects/elpetros-chat-sentinel` is the separate Chat Sentinel checkout. Leave it alone during Kai work.
- The inspected `/mnt` and `/media` were empty, and the bounded search did not locate a Kai checkout or evidence archive in the searched paths. This does not prove that no Kai workspace exists elsewhere in app-managed storage or a Codespace.
- A live Codespace name/ID and its working-tree state were **not** verified. Do not assume uncommitted Codespace files are safely stored in GitHub.
- Do not initialize, overwrite, clone over, delete, or repurpose `/root/projects/test-repo`. First identify the actual Kai checkout and its remote/status.

## 5. Earlier Kai product and architecture decisions to preserve

These are carried forward from earlier Kai discussions. They are requirements or design directions, not all completed features.

### Speech and voice interaction

- The composer microphone belongs inside the message composer, on the right, near but clearly separate from the lightning/action control. Do not restore a separate label above the composer such as “Voice input · English, offline.”
- AI-provider connectivity and speech-engine availability are separate states. A local speech engine can be offline-capable while the chat AI provider is online, unavailable, or rate-limited; do not collapse these into one status indicator. Earlier UI direction was clear red/green provider connectivity, with speech-engine status represented separately where useful.
- The current implemented speech workflow is bounded, foreground-only dictation: user starts/stops capture, reviews editable text, and explicitly submits it. Speech recognition must not auto-send a chat message, activate an action button, press Enter, or execute a terminal command.
- Dictation destinations are explicit and session-bound. Terminal command text remains a draft pending a separate user action.
- The shared voice-session model includes DICTATION, SPOKEN_REQUEST, and LIVE_CONVERSATION modes, but the full end-to-end behavior of all three modes is not established as complete merely because the coordinator enum exists.
- Local/offline recognition after first-use model download, app-private model storage, no raw-audio upload, and temporary audio cleanup are core privacy/sovereignty boundaries.

### Terminal workgroups — planned, not yet implemented

The intended direction is one Kai coordinating multiple terminal workgroups, rather than independent terminals acting without shared coordination. Earlier design discussions included:
- Stable session IDs and clear workgroup ownership.
- A shared objective and shared task context, with source attribution for contributed output.
- Ordered tasks and explicit dependencies.
- Resource locking / conflict prevention when workgroups touch shared files or resources.
- Verification of work before results are accepted or handed off.

The current code does not yet implement full workgroup membership, history, scheduling, dependency execution, or resource locking. Treat this as an architecture requirement and future design task, not a shipped capability.

### Project values, portability, and preservation

- The user strongly values Kai as open-source, free, sovereign, and portable. Prefer local-first behavior and user-controlled data; explain any unavoidable external dependency plainly.
- Keep the Kai source repository, Kai Build/Debian filesystem, local model files, exports/settings, and Chat Sentinel as distinct preservation surfaces. A GitHub repository does not automatically back up the Debian filesystem, installed packages, local models, or uncommitted workspace files.
- A previous discussion described Kai export coverage as conversations, memories, tools/settings, heartbeat, tasks, and provider configurations/API keys, while excluding Debian filesystem/packages/Build files/local models. Treat that as the previously discussed export scope, not as a newly verified export guarantee; inspect the actual exporter before relying on it.
- Any export or settings JSON containing provider credentials/API keys is private. Never commit secrets or private exports into the public GitHub repository. Use placeholders/redaction in documentation and keep credentials in their intended secure local settings store.
- The user's Kai Build environment is Debian 12 Bookworm ARM64 on the Samsung Galaxy A36. The separate Chat Sentinel checkout and all HIB environments remain out of scope for Kai work.

## 6. November 18 benchmark work — current progress

Work has started on the separate branch `feature/speech-benchmark-harness` and draft PR #2. This branch is based on the recovery checkpoint commit and does not modify the user-facing speech implementation.

Committed foundations:
- `composeApp/src/commonTest/kotlin/com/inspiredandroid/kai/speechbenchmark/WordErrorRate.kt`: deterministic Levenshtein WER scorer with fixed normalization policy v1 and stable edit-operation tie-break.
- `WordErrorRateTest.kt`: exact match, substitution, deletion, insertion, punctuation/case/apostrophe handling, empty-reference behavior, and number-token policy tests.
- `SpeechBenchmarkManifest.kt` and `SpeechBenchmarkManifestValidatorTest.kt`: versioned manifest data model and validation for unique IDs, safe relative paths, SHA-256 syntax, mono 16 kHz prepared audio, positive durations, and non-empty references.
- `SpeechBenchmarkResults.kt` and `SpeechBenchmarkAggregatorTest.kt`: per-clip success/failure model and aggregate accuracy/latency/failure metrics. Corpus WER is weighted by reference word count; failed clips remain visible in failure rate but are excluded from WER and latency.
- `SpeechBenchmarkJson.kt` and `SpeechBenchmarkJsonTest.kt`: strict local JSON encode/decode for manifests and reports. Manifest validation runs on read/write; report aggregates are recomputed and checked against per-clip results.
- `docs/speech-benchmark-harness.md`: design, privacy boundaries, candidate licensing status, current status, and next steps.
- `docs/speech-engine-evaluation-plan.md`: candidate review now puts MIT-licensed Moonshine v2 Base English first for investigation; GigaSpeech Zipformer is held pending model-weight redistribution rights; LibriSpeech Zipformer weight license remains unverified.
- `.github/workflows/speech-benchmark-core.yml`: PR-scoped workflow runs `spotlessCheck` and `:composeApp:desktopTest`, with concurrency cancellation to avoid duplicate/superseded checks.

**Verification status:** CI exposed two successive Spotless formatting differences in `WordErrorRate.kt`; both have been corrected in commit `e102ad30b6461c76d2f7888856fd442e70a3d30b`. Earlier CI attempts stopped at `spotlessKotlinCheck`, so `:composeApp:desktopTest` has not yet been confirmed. Several newer PR checks were canceled or left pending while the workflow concurrency group was refreshed; no green CI result has been confirmed. Inspect the latest workflow run live before claiming formatting or tests pass. No Android device runner or actual device benchmark has been implemented or run. JSON codec logic is implemented and has unit tests in source, but test execution is still pending.



### Android device-test path confirmed (not yet enabled)

The current module uses the Android KMP library plugin and currently opts in only to `withHostTest {}`. Official AGP documentation confirms that `withDeviceTest` creates the `androidDeviceTest` source set. To reuse shared benchmark code in that device-test compilation, the documented builder form can set `sourceSetTreeName = "test"`, with `AndroidJUnitRunner` configured. This has been researched but **not yet added to the build**; instrumentation dependencies and the actual A36 task still need validation. Reference: https://developer.android.com/kotlin/multiplatform/plugin

## 7. Exact next development point

The next planned engineering task is to finish the testable benchmark core, then prepare a **separate, reproducible Android benchmark runner/variant** that:

1. Loads only one selected ASR model at a time.
2. Uses a fixed clip manifest and reference transcripts.
3. Writes machine-readable per-clip results.
4. Reports aggregate WER, latency/real-time factor, model load time, memory/footprint where measurable, and failures.
5. Keeps benchmark code separate from the user-facing Kai speech path until the candidate passes the documented decision gate.

Before implementation, inspect the current branch tree and existing test/build structure so we extend existing work rather than create duplicate infrastructure. Keep Moonshine as the known-working baseline. Validate the Zipformer GigaSpeech candidate's files, license, checksums, and sherpa-onnx 2.0.1 / Android compatibility before integrating it.

Other discrete outstanding work:
- Add the seven evidence JPEG binaries to the committed evidence directory if the ZIP can be located and verified.
- Identify the actual Kai local checkout or Codespace only if local build/device work requires it.
- Do not change the default speech model, merge PR #1, release, deploy, or alter unrelated settings without explicit approval.

## 8. Resume protocol — use this instead of starting over

When the user returns:

1. Open this file first: [verified recovery checkpoint](https://github.com/ElPetros/Kai/blob/feature/offline-speech-input/docs/checkpoints/KAI-9000-RECOVERY-CHECKPOINT-2026-11-18.md).
2. Connect to GitHub and verify the current `feature/offline-speech-input` branch head and PR #1 head. If they differ from this checkpoint, inspect the intervening commits before doing anything.
3. Confirm PR #1 is still open/unmerged; do not merge it as part of a resume check.
4. Read `docs/speech-engine-evaluation-plan.md` and inspect existing benchmark/test code before proposing edits.
5. If local work is needed, identify the actual Kai checkout by its Git remote and branch. Never infer identity from a directory name alone.
6. Continue from the first unfinished item in Sections 5 and 3. Do not repeat the already-passed device smoke test or re-run completed setup without a concrete reason.
7. At the end of each substantial work session, update this recovery checkpoint with the new verified commit, tests, evidence locations, outstanding work, and exact next action.

**Safety rule:** A checkpoint is a verified map, not permission to perform destructive actions. No deletion, overwrite, branch reset, force push, merge, release, deployment, or cross-project change without explicit user approval.

## 9. Quick resume phrase

The user can start a future session with:

> Resume Kai 9000 from the verified November 18 recovery checkpoint in ElPetros/Kai. Connect to GitHub, verify the speech branch, benchmark branch, and PR #1/#2 heads, inspect CI, then continue from the first unfinished benchmark task. Do not restart completed work or touch Chat Sentinel.
