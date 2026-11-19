# Submitting the microphone contribution to Simon

**Target repository:** https://github.com/SimonSchubert/Kai  
**Prepared source branch:** `submission/kai-offline-mic-upstream-2026-11-19` in `ElPetros/Kai`  
**Target base:** `main`

## One-click comparison

Open this GitHub comparison while signed in:

https://github.com/SimonSchubert/Kai/compare/main...ElPetros:submission/kai-offline-mic-upstream-2026-11-19?expand=1

The comparison is based on Simon's current `main` and contains the isolated microphone feature files. Select **Create pull request**, keep it as a **Draft pull request**, and use the title and description below.

The connected GitHub integration can read Simon's repository but GitHub returned HTTP 403 when asked to create a pull request there. The source branch is already pushed to EL's fork; this single GitHub UI action is the remaining permission boundary.

## Suggested title

`Draft: Add Android offline push-to-talk speech input`

## Pull request description

```markdown
## Summary

This draft proposes an Android-only, foreground, push-to-talk microphone input path for Kai 9000. It addresses the speech-to-text portion of [issue #357](https://github.com/SimonSchubert/Kai/issues/357) without expanding into the real-time, hands-free conversation mode proposed in [issue #341](https://github.com/SimonSchubert/Kai/issues/341).

The design preserves Kai's existing composer:
- Tap the microphone to start/stop a bounded recording.
- Process speech locally with Moonshine Tiny English through sherpa-onnx.
- Run on-device Silero VAD before ASR so silence/noise-only recordings can return no transcript instead of fabricated words.
- Insert recognized text into the existing editable composer at the current selection.
- The user reviews/edits and explicitly presses Send. Speech is never auto-submitted.
- Existing typing and composer behavior remain available.
- No always-on listening, wake word, diarization, cloud transcription, or paid speech API.

## Validation evidence

The VAD implementation was built and tested on source branch `fix/speech-vad-60s-hardening`, commit `d816db8f51216e404608a151748c5c9d7de29235`:
- [GitHub Actions Run #60](https://github.com/ElPetros/Kai/actions/runs/44472952350) passed Android FOSS debug/release assembly and shared desktop tests.
- EL installed the isolated Speech Test debug APK on a Samsung Galaxy A36 (SM-S366V) and reported five checks passing: silence rejection, fan-noise rejection, quiet speech, normal speech, and clipped-start handling.

Run #60 validates the source branch/commit above, not this newly isolated upstream-based PR head. This PR should remain Draft so upstream CI and maintainer review can validate the exact proposed branch. The device evidence is one-device functional evidence, not a universal compatibility or statistical accuracy claim.

## Maintainer review requested

Please review the implementation and decide whether to accept, modify, defer, or decline it. This is not represented as production-ready or approved for release. Broader device, microphone, accent, and environment testing remains appropriate after maintainer review. Keep review-before-send as the default.

Prepared collaboratively by EL and ChatGPT.
```

## Important

- Keep the pull request **Draft**.
- Do not merge it or mark it ready for review without Simon's feedback and EL's explicit approval.
- Do not close or merge PR #4 in the fork; it remains a separate historical draft.
