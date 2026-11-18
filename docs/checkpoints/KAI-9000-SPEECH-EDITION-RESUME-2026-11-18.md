# Kai 9000 Speech Edition — resume checkpoint (2026-11-18)

## Identity and repository

- Project: **Kai 9000 Speech Edition**. This is not Chat Sentinel.
- Working repository: `ElPetros/Kai` (fork of `SimonSchubert/Kai`).
- Active branch at checkpoint: `feature/offline-speech-input`.
- Verified branch head before this checkpoint: `34215393be02322bf7dcb64e16a13a39bfd6d5c4`.
- Workspace fact: the user's Kai Debian shell was at `/root`, which is not a Git repository. A local directory `/root/projects/test-repo` was being identity-checked; do not assume it is Kai. The correct local checkout path is not yet confirmed.
- Do not touch Chat Sentinel or its repositories/environments.

## Completed implementation and validation

- Android FOSS debug build passed at the verified head.
- Unit-test workflow passed at the verified head.
- Draft PR #1 remains open and unmerged: https://github.com/ElPetros/Kai/pull/1
- CI workflow is verification-only; it no longer formats, updates screenshots, commits, or pushes automatically.
- Speech implementation uses sherpa-onnx 2.0.1 with quantized Moonshine v2 Tiny English, 16 kHz mono PCM, a 60-second recording cap, and a pinned model SHA-256 of `9ec31b342d8fa3240c3b81b8f82e1cf7e3ac467c93ca5a999b741d5887164f8d`.
- Model is downloaded and verified on first use, stored in app-private storage, and reused offline. Audio is not uploaded or persisted; temporary PCM is cleared and the recognizer released.
- Mic is integrated in the composer. Transcript insertion preserves existing text and caret position.
- Dictation into generated UI fields and terminal drafts is supported; dictated terminal commands are drafts and are never auto-executed.
- Voice ownership is coordinated and bound to the target session; lifecycle stop cancels recording/work and clears temporary audio.
- Debug Speech Test variant uses application ID suffix `.speechtest` and can coexist with normal Kai.

## Completed real-device test (do not ask user to repeat)

On 2026-11-17, user installed the Kai Speech Test app on a Samsung Galaxy A36 (SM-S366V), granted microphone permission, downloaded the Moonshine model (about 45–60 seconds), and completed two successful stop-to-transcribe tests. User reported both transcripts matched their spoken words exactly.

The uploaded 47.85-second screen recording confirms:
1. Idle speech interface and mic control.
2. Recording state with “Recording. Tap stop when you're finished.”
3. “Transcribing on this device…” after Stop.
4. Transcript inserted into composer with “Transcript added. Review it before sending.”
5. A second, longer transcript inserted into the composer.
6. A submitted request enters “Thinking…” / “Working…”, then the app displays the Free FAST / Free EXPERT provider limit.

Important distinction: the local speech recognition succeeded. The later AI-provider limit is a separate chat-provider failure. The recording does not show a completed AI answer for the second request.

## Evidence package

The evidence ZIP delivered in the conversation is `kai9000-speech-evidence.zip`. It contains **seven still images** and a README with descriptions, source timestamps where applicable, SHA-256 hashes, provenance, and limitations. The evidence index has now been committed to the active feature branch at `docs/evidence/kai-speech-device-test-2026-11-17/README.md` (commit `cdb611616656dfbb2bc9c16b05f581038308fa5d`).

- `00-before-speech-microphone.jpg` — separate user-supplied original Kai 9000 screen capture showing the composer before speech-microphone integration. The composer displays “Ask a question”, an attachment control, and the lightbulb/action control; no microphone control is visible. SHA-256: `47f462ed88d6f6007681ef3ae5c503e9183f8f0fc4765972fea4f4a695975dc6`.
- `01-idle-speech-control.jpg` — 0s, idle speech interface.
- `02-recording-active.jpg` — 1s, recording active.
- `03-on-device-transcription.jpg` — 18s, on-device transcription.
- `04-first-transcript-review.jpg` — 20s, first transcript review.
- `05-second-transcript-submitted.jpg` — 35s, second transcript submitted / assistant processing.
- `06-provider-limit-separate-from-asr.jpg` — 44s, provider limit.

The pre-feature image is a user-supplied screenshot, not a video frame. The six workflow images are extracted from the supplied 47.85-second video. The screenshot and video together now support a before/after interface comparison as well as the end-to-end workflow. The ZIP has been regenerated to include the baseline screenshot.

**Repository binary status:** the screenshot/JPEG files are packaged in the conversation ZIP but have not yet been committed as binary assets in GitHub. Do not claim they are already present in the repository. The evidence README and manifest are committed; adding the image binaries remains a discrete task.

## Evaluation status and next work

- Two successful on-device examples are a positive functional smoke test, not a statistically meaningful accuracy benchmark.
- Moonshine v2 Tiny remains the initial baseline, not a final model winner.
- A controlled model-evaluation plan is now committed at `docs/speech-engine-evaluation-plan.md` (commit `e00d1cbba0ec59fc4b30d1902f7a19b07ce4512c`). It keeps Moonshine as the working baseline and defines Zipformer GigaSpeech as a challenger, with matched Earnings-22, TED-LIUM 3, and LibriSpeech test-clean clips; WER, real-time factor, model load time, memory, footprint, and failures are measured on the actual A36. No comparative device result exists yet.
- Do not implement live partials, VAD, automatic language detection, or second-pass verification until evidence shows they are needed.
- Terminal workgroups are an architecture/design contract, not a completed multi-terminal implementation.

## Immediate resume instructions

1. Resume in `ElPetros/Kai`, branch `feature/offline-speech-input`, not any Chat Sentinel repository.
2. Re-establish the local checkout identity before running Git commands. Do not initialize or overwrite `/root/projects/test-repo` unless its remote and contents prove it is the Kai checkout.
3. Evidence README/index is committed. Upload the seven JPEG binaries from the prepared ZIP to `docs/evidence/kai-speech-device-test-2026-11-17/`; the GitHub connection used so far can create text files but has no direct binary upload from the ZIP.
4. PR #1 description has been updated to reflect successful real-device smoke testing, the separate provider-limit issue, the evidence index, and the remaining benchmark work.
5. Continue with the reproducible benchmark harness and candidate model compatibility investigation. No merge, release, deployment, or changes to the user-facing default model without explicit approval.

## Build and CI evidence

- Android build workflow run: https://github.com/ElPetros/Kai/actions/runs/44233385291
- Unit test workflow run: https://github.com/ElPetros/Kai/actions/runs/44233385114
- Artifact `kai-foss-debug` was reported available until 2026-11-25.
