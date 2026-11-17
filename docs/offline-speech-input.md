# Offline speech input

## Scope

The first speech-input implementation is Android-only and English-only. It is a deliberate, foreground-only workflow:

1. The user taps the microphone.
2. On first use, Kai requests microphone permission and downloads the speech model.
3. The user taps the microphone again to start recording, then taps Stop.
4. Kai transcribes locally and inserts the result at the composer's current selection.
5. The user reviews and edits the text before using Kai's existing Send control.

Speech input does not submit messages, run in the background, or keep the microphone open between recordings. Audio is held in memory for a maximum of 60 seconds and is not written to persistent storage or uploaded.

The first-use model download requires an internet connection. After installation, recognition itself is offline.

## Runtime and model

- Runtime: [sherpa-onnx 2.0.1](https://github.com/k2-fsa/sherpa-onnx), Apache-2.0.
- Model: Moonshine v2 Tiny, English, quantized, [MIT license](https://github.com/moonshine-ai/moonshine).
- Model archive: `sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27.tar.bz2`.
- Archive size: 29,858,559 bytes.
- Pinned SHA-256: `9ec31b342d8fa3240c3b81b8f82e1cf7e3ac467c93ca5a999b741d5887164f8d`.
- Extracted model files: approximately 43 MiB.

The model is downloaded to app-private storage, checked against its pinned SHA-256 before extraction, and installed from a staging directory. Only the encoder, merged decoder, token vocabulary, and upstream model license are retained. The recognizer is created after recording and released after each transcription so its native model memory is not kept resident beside Kai's local language model.

The sherpa-onnx Android AAR is a multi-ABI package (approximately 47.2 MB in the upstream v2.0.1 release). This is a meaningful application-size cost and should be reconsidered if Kai later adopts a smaller, verified runtime package.

## Accuracy and limitations

Moonshine's published quantized Tiny Streaming results report 4.83% WER on LibriSpeech test-clean and a 12.00% average on the Open ASR Leaderboard's eight-dataset evaluation. These figures use different evaluation sets and should not be compared as if they were the same test. They are reference results, not a promise of accuracy on noisy rooms, overlapping speech, spontaneous conversation, names, or every accent.

This first version is a bounded, stop-to-transcribe workflow. It does not yet provide live partial results, voice activity detection, automatic language detection, or a second-pass verifier. Those features should be added only after device testing shows a clear benefit.
