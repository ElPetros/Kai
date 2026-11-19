# Kai Android Offline Speech Input — Working Preview Handoff

**Prepared:** 2026-11-19  
**Prepared collaboratively by:** EL and ChatGPT  
**Repository:** [ElPetros/Kai](https://github.com/ElPetros/Kai)

## Decision requested from Simon

Please review this as a **working-preview contribution for maintainer review and broader testing**. The contributor-led implementation and initial device validation have reached a sensible handoff point. The next phase should be maintainer review and testing by a wider range of users and devices, rather than extending one-person testing indefinitely.

This is **not** a claim of production readiness, universal compatibility, or maintainer acceptance. Simon may accept, revise, defer, or decline the contribution. No merge or release is authorized by this document.

## What the contribution does

- Android foreground, push-to-talk offline dictation.
- Moonshine Tiny English recognition through sherpa-onnx.
- Silero VAD runs before ASR so silence/noise-only input can be rejected without sending it to Moonshine.
- A verified, app-private VAD model is downloaded and checked for expected size and SHA-256 before use.
- Recognized text is inserted into Kai's existing editable composer for review.
- No always-on listening, wake word, diarization, cloud transcription, or paid speech API is introduced.
- Speech text is not tool authorization.

## Evidence and current status

### Automated build

GitHub Actions [Run #60](https://github.com/ElPetros/Kai/actions/runs/44472952350) completed successfully on 2026-11-19 for:

- Branch: `fix/speech-vad-60s-hardening`
- Commit: `d816db8f51216e404608a151748c5c9d7de29235`
- Job: “Build Kai Android variants and run shared tests”
- Successful steps included Java 21 setup, Android SDK setup, Gradle build, FOSS debug and release APK assembly, shared desktop tests, and APK artifact upload.

Artifact: `kai-android-validation-apks`  
Artifact ID: `14505966707`  
SHA-256: `cf099096d345b99788bd4bbfc7c37171ea2012d8d8c7b7b9d27ffda360368d9d`  
Created: 2026-11-19 06:43:48 UTC  
Expires: **2026-11-26 06:43:42 UTC**  
[Download artifact ZIP from GitHub](https://api.github.com/repos/ElPetros/Kai/actions/artifacts/14505966707/zip)

The artifact is temporary. Simon should download and preserve it before expiry if this exact build is needed.

### Galaxy A36 device validation

EL installed the isolated Speech Test Debug APK on a Samsung Galaxy A36 (SM-S366V) and reported all five VAD checks passed:

1. Silence rejection.
2. Fan-noise rejection.
3. Quiet-speech detection.
4. Normal-speech detection and transcription.
5. Clipped-start handling, including retaining the beginning of words.

This is user-reported functional evidence from one physical device. No numerical accuracy, latency, or acoustic-threshold benchmark was collected. It does not establish compatibility across all Android devices, microphones, accents, or environments.

Earlier device tests on prior builds also demonstrated editable transcript insertion, review-before-send, repeated sessions, and recovery after silence/interruption. Those earlier tests should not be conflated with a complete regression pass on the final VAD build.

### Important branch / PR distinction

**Run #60 validates the commit on `fix/speech-vad-60s-hardening`. It does not, by itself, prove that PR #4's current head was built or tested.** The VAD validation branch and PR #4 are distinct refs. Review their commit ancestry/diff before deciding how to consolidate them.

- [PR #4 — Fix immediate offline speech transcript delivery](https://github.com/ElPetros/Kai/pull/4): keep **Draft, open, and unmerged** pending Simon's review and an explicit decision from EL.
- [PR #1 — Draft: Add Android offline speech input](https://github.com/ElPetros/Kai/pull/1): earlier contribution context; also remains unmerged.

The PR #4 description contains historical validation notes from earlier stages. Treat the run/branch/commit evidence above and the linked device checkpoint as the authoritative record for the VAD preview.

## Recommended preview boundaries

- Describe this as a working preview / candidate contribution for maintainer review and expanded testing.
- Keep optional auto-send OFF for initial wider testing. Review-before-send remains the safe default.
- Keep original Kai app package `com.inspiredandroid.kai` separate from the isolated test package `com.inspiredandroid.kai.speechtest`; do not replace the original app during validation.
- Do not claim broad device compatibility or production readiness.
- Collect reports from different Android models, OS versions, microphones, accents, speech rates, and noise conditions.
- Triage reproducible bugs and improve the feature iteratively after maintainer review.

## Bug reporting

The repository already has a general bug form at [bug_report.yml](https://github.com/ElPetros/Kai/blob/main/.github/ISSUE_TEMPLATE/bug_report.yml). A speech-input-specific form is proposed alongside this handoff at [speech_input_bug_report.yml](../../.github/ISSUE_TEMPLATE/speech_input_bug_report.yml). It is not active on the default branch unless Simon chooses to merge it.

Useful reports should include Kai version, device model, Android version, speech conditions, expected result, actual transcript/behavior, reproducible steps, and optional redacted logs/screenshots. Users should not be asked to upload private audio by default.

## Source records

- [Device validation checkpoint](../device-validation-2026-11-19.md)
- [Successful GitHub Actions Run #60](https://github.com/ElPetros/Kai/actions/runs/44472952350)
- [PR #4](https://github.com/ElPetros/Kai/pull/4)
- [PR #1](https://github.com/ElPetros/Kai/pull/1)

## Requested next step

Simon reviews the implementation, branch relationship, evidence, and proposed bug-report form, then decides whether and how to integrate it. The contribution is ready to be reviewed; it is not being represented as merged or released.

**Prepared collaboratively — EL and ChatGPT**
