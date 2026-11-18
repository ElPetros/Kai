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

A six-still evidence set was extracted from the supplied screen recording and delivered in the conversation as `kai9000-speech-evidence.zip`. It contains the JPEGs and a README with timestamps, descriptions, SHA-256 hashes, provenance, and limitations.

Selected source timestamps:
- 0s — idle speech control
- 1s — recording active
- 18s — on-device transcription
- 20s — first transcript review
- 35s — second transcript submitted / assistant processing
- 44s — provider limit

Evidence limitation: the video starts after the speech interface is already present. It shows the mic idle, not the original pre-feature composer without a microphone. Do not claim it proves a before/after UI comparison. The stills are extracted from the video, not original device screenshots.

## Evaluation status and next work

- Two successful on-device examples are a positive functional smoke test, not a statistically meaningful accuracy benchmark.
- Moonshine v2 Tiny remains the initial baseline, not a final model winner.
- Planned model comparison remains Zipformer GigaSpeech versus Moonshine, using matched Earnings-22, TED-LIUM 3, and LibriSpeech test-clean clips. Measure WER, real-time factor, model load time, peak memory, and failure rate.
- Do not implement live partials, VAD, automatic language detection, or second-pass verification until evidence shows they are needed.
- Terminal workgroups are an architecture/design contract, not a completed multi-terminal implementation.

## Immediate resume instructions

1. Resume in `ElPetros/Kai`, branch `feature/offline-speech-input`, not any Chat Sentinel repository.
2. Re-establish the local checkout identity before running Git commands. Do not initialize or overwrite `/root/projects/test-repo` unless its remote and contents prove it is the Kai checkout.
3. Add the six extracted JPEGs under `docs/evidence/kai-speech-device-test-2026-11-17/` and link them from an evidence index. The ZIP is available in the prior ChatGPT conversation; it is not yet committed by this checkpoint.
4. Review PR #1 and update its description to reflect that real-device smoke testing has passed, while preserving the caveat that the sample is small and accuracy evaluation remains outstanding.
5. Continue with documentation/evidence first. No merge, release, deployment, or unrelated source changes without explicit approval.

## Build and CI evidence

- Android build workflow run: https://github.com/ElPetros/Kai/actions/runs/44233385291
- Unit test workflow run: https://github.com/ElPetros/Kai/actions/runs/44233385114
- Artifact `kai-foss-debug` was reported available until 2026-11-25.
