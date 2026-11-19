# Kai Android speech device validation — 2026-11-19

## Preserved build checkpoint

- Repository: `ElPetros/Kai`
- Source branch: `fix/speech-vad-60s-hardening`
- Source commit: `d816db8f51216e404608a151748c5c9d7de29235`
- GitHub Actions run: [#60](https://github.com/ElPetros/Kai/actions/runs/44472952350) — successful
- Artifact: `kai-android-validation-apks`
- Artifact SHA-256: `cf099096d345b99788bd4bbfc7c37171ea2012d8d8c7b7b9d27ffda360368d9d`
- Artifact expiry: 2026-11-26
- Device: Samsung Galaxy A36
- Device-test app: isolated Kai Speech Test debug APK; original Kai installation was not to be replaced.
- Preservation branch: `checkpoint/vad-device-validation-2026-11-19`

## User-reported device results

On 2026-11-19, the user installed the Debug APK and reported that all five VAD tests passed:

1. Silence rejection.
2. Fan-noise rejection.
3. Quiet-speech detection.
4. Normal-speech detection and transcription.
5. Clipped-start handling.

These are user-reported results. No numerical accuracy, latency, or acoustic threshold measurements were collected in this test session.

## Remaining validation checklist

Run these on the isolated Speech Test app before declaring the microphone workflow complete:

- [ ] Fast, naturally spoken sentence.
- [ ] Sentence with deliberate short and longer pauses; confirm speech resumes after pauses without losing words.
- [ ] Clipped ending / Stop pressed near the end of a phrase; assess whether the final word is retained.
- [ ] Longer utterance, including a recording approaching the 60-second capture limit; confirm no crash, truncation surprise, or stuck state.
- [ ] Multiple consecutive VAD-enabled record/stop/transcribe sessions.
- [ ] Stop or interrupt during active speech, then immediately record again.
- [ ] Leave the foreground or navigate away during capture; confirm capture is stopped and the microphone can be used again.
- [ ] Deny or revoke microphone permission, then grant it and retry; confirm the UI does not remain stuck.
- [ ] Confirm no-speech result leaves the composer unchanged and does not invoke an unintended send.

## Kai composer and safety acceptance

Previously recorded in the PR history: transcript insertion into the editable composer, editing the transcript, review-before-send, and repeated-session / interruption recovery had passed on the Galaxy A36 using an earlier Speech Test build. Recheck on the VAD-enabled build where practical.

- [ ] Confirm transcript appears in the existing editable composer and can be edited.
- [ ] Confirm auto-send is OFF by default and remains OFF after app restart.
- [ ] If explicitly enabled, confirm auto-send occurs only with an empty composer, no attachments, and Kai not already responding.
- [ ] With an existing draft, attachment, or active response, confirm speech text falls back to draft review rather than sending.
- [ ] Confirm blank or failed recognition never sends.
- [ ] Confirm spoken words alone do not authorize tool execution or other privileged actions.

## Release boundary

This checkpoint does not authorize merging or releasing. PR #4 must remain open, Draft, and unmerged until the user reviews the completed validation and explicitly approves. The VAD validation branch and PR #4 are separate refs; do not assume the successful Run #60 validates PR #4's current head.
