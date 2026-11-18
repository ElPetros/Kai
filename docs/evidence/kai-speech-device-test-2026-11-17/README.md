# Kai 9000 Speech Edition — on-device test evidence

This directory documents the real-device smoke test performed on 2026-11-17 using the Kai 9000 Speech Test build on a Samsung Galaxy A36 (SM-S366V).

## Evidence files

The seven still images and their SHA-256 hashes are listed below. The original video was 47.85 seconds long. Six stills were extracted from that recording; the before-change baseline is a separate original screenshot supplied by the user.

| File | Source | What it documents | SHA-256 |
|---|---|---|---|
| `00-before-speech-microphone.jpg` | User-supplied screenshot | Original composer with attachment and lightbulb/action controls; no microphone visible | `47f462ed88d6f6007681ef3ae5c503e9183f8f0fc4765972fea4f4a695975dc6` |
| `01-idle-speech-control.jpg` | Video, 0s | Speech interface idle; microphone visible | `b50176b982dbdc1033f00bb3f4d873b47cdc72d26a16296ada3a4d26d7fed11a` |
| `02-recording-active.jpg` | Video, 1s | Recording state and Stop instruction | `d73cd6196c9439fd6fcf6a5757ee9cf23d7527404007adb4f70cb06a040a94d8` |
| `03-on-device-transcription.jpg` | Video, 18s | On-device transcription state after Stop | `1f4cf5994f47be351ff973fc97fd4badff49596c878b3aa3252c0abc7f5b630d` |
| `04-first-transcript-review.jpg` | Video, 20s | First transcript inserted into composer for review | `8dfe6ae5324b31d1b827d9fc622624f24d240ba04cb51fd1e3dece8879a52067` |
| `05-second-transcript-submitted.jpg` | Video, 35s | Second transcript submitted; assistant processing | `3da4e2ca4d14a521c8d011a61602dac54b58a9a5ed37739369da020f914ea9f1` |
| `06-provider-limit-separate-from-asr.jpg` | Video, 44s | Separate Free FAST / Free EXPERT provider limit | `dd6e820c6f2f825e7029f2e51b3e80c0cdf7e3c2c4dbad68cc1d1d0e3f1c752c` |

## What the test demonstrates

- The microphone control is integrated into the composer and has an idle state.
- The app enters a visible recording state.
- After Stop, the app displays an on-device transcription state.
- Two speech samples were transcribed successfully; the user reported that both matched the spoken words exactly.
- The first transcript was inserted into the composer with an explicit review-before-send instruction.
- Recognized text is a draft and is not automatically submitted.

## Keep the provider failure separate

After the second transcript was submitted, the app displayed a Free FAST / Free EXPERT provider limit. This is a separate chat-provider limitation, not an offline speech-recognition failure. The recording does not show a completed AI response for that second request.

## Evidence limitations

Two successful examples establish a positive functional smoke test, not a statistically meaningful accuracy benchmark. No broad claim is made about accents, background noise, overlapping speech, or long recordings. The video begins after the speech interface was installed; the original no-microphone composer is shown by the separately supplied baseline screenshot.

## Reproduction context

- Device: Samsung Galaxy A36 (`SM-S366V`)
- App: Kai 9000 Speech Test build
- Speech engine: sherpa-onnx 2.0.1 with quantized Moonshine v2 Tiny English
- Audio: 16 kHz mono PCM
- Interaction: tap mic, speak, tap Stop, wait for local transcription, review draft, submit explicitly

## Provenance

The six workflow stills are direct frame extractions from the user-provided screen recording. The baseline is the user's separate screenshot. Preserve the original video as the primary evidence source.