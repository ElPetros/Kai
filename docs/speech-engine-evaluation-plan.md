# Kai 9000 Speech Edition — model evaluation plan

Status: planned; no comparative A36 benchmark has yet been run.

## Decision to make

Keep the current quantized Moonshine v2 Tiny English integration as the functional baseline. Evaluate a sherpa-onnx Zipformer GigaSpeech English model as a challenger before changing the default engine. Do not select a winner from headline WER figures measured on different datasets, runtimes, devices, or model variants.

## Current baseline

- Android integration: sherpa-onnx 2.0.1.
- Model: quantized Moonshine v2 Tiny English.
- Archive: `sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27.tar.bz2`.
- Archive size: 29,858,559 bytes.
- Pinned SHA-256: `9ec31b342d8fa3240c3b81b8f82e1cf7e3ac467c93ca5a999b741d5887164f8d`.
- App behavior: bounded 60-second capture, Stop-to-transcribe, transcript inserted as an editable draft; no automatic submission.
- Real-device status: two successful user-reported exact transcriptions on Samsung Galaxy A36 (SM-S366V). This is functional smoke-test evidence, not a comparative accuracy benchmark.

## Candidate A — Moonshine v2 Base English

- Model: quantized Moonshine v2 Base English, archive `sherpa-onnx-moonshine-base-en-quantized-2026-02-27.tar.bz2`, using the same sherpa-onnx Moonshine runtime family as the current Tiny baseline.
- The official Moonshine model catalogue lists English Base as MIT-licensed and reports a quantized package around 135 MB, compared with the current Tiny package around 43 MiB. The larger decoder/model footprint is a real device-cost risk, not an automatic quality win.
- The official catalogue's published English non-streaming table lists Base at 10.07% and Tiny at 12.66% WER on its eight-dataset Open ASR average (58M vs 26M parameters). These are upstream reference results, not measurements of Kai's exact quantized packages on the Galaxy A36; use them only as motivation to run the controlled device comparison.
- The sherpa-onnx v2.0.1 Kotlin API exposes the Moonshine v2 `encoder` and `mergedDecoder` fields, matching the model's two-file runtime layout.
- This is a same-family size/accuracy challenger, not an independent architecture comparison. Verify exact archive size, SHA-256, license notice, and load/runtime behavior on the Galaxy A36 before benchmarking.
- Do not change the installed Tiny baseline or the user-facing default while evaluating Base.

## Candidate B — Zipformer GigaSpeech (license hold)

- Model family: sherpa-onnx Zipformer, GigaSpeech-trained English offline transducer.
- Upstream model identifier: `sherpa-onnx-zipformer-gigaspeech-2023-12-12`.
- Upstream catalogue: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/zipformer-transducer-models.html
- The official model page lists both fp32 and int8 files. The documented int8 configuration uses the int8 encoder and int8 joiner with the fp32 decoder; the int8 encoder is listed at about 68 MB, the fp32 encoder at about 249 MB, and the package also includes both decoder/joiner variants and tokens. The archive size, extracted size, and SHA-256 have not yet been independently recorded.
- **License is a blocker, not cleared.** The GigaSpeech repository's Apache-2.0 license file and the dataset's access terms are not interchangeable: the dataset page states non-commercial research/education terms for audio, and the maintainers' licensing discussion notes that Apache-2.0 covers repository scripts rather than automatically granting rights to the audio. The exact license/redistribution rights for this converted model checkpoint are not explicitly established by the sherpa model page. Do not bundle or distribute these weights with Kai unless the model-weight rights are clarified.
- The sherpa-onnx v2.0.1 Kotlin API source defines `OfflineTransducerModelConfig(encoder, decoder, joiner)` and `OfflineModelConfig.transducer`, so the required transducer configuration shape exists in the pinned runtime. This confirms API shape only; it does not prove that the candidate model loads successfully on Android or the Galaxy A36.
- Before integration or device benchmarking, verify the model files, exact archive size/checksum, model-weight license and redistribution rights, and runtime compatibility. Do not infer total installed footprint or runtime memory from the encoder file alone.

## Alternative candidate to investigate

The sherpa-onnx catalogue also lists `csukuangfj/sherpa-onnx-zipformer-small-en-2023-06-26`, converted from a LibriSpeech-trained Zipformer Small model. The catalogue lists an int8 encoder around 25 MB, an int8 decoder around 1.2 MB, and an int8 joiner around 253 KB. This may be a more practical A36 challenger than the GigaSpeech model on footprint, but its model-weight license and exact package checksum still need verification. If it is evaluated, do not use LibriSpeech test-clean as the primary deciding corpus because this candidate was trained on LibriSpeech; keep Earnings-22 and TED-LIUM 3 as independent primary comparisons.

## Candidate decision gate

The GigaSpeech candidate remains a research lead only until the checkpoint's weight-license and redistribution rights are clear. The LibriSpeech Zipformer alternative is also not cleared until its actual weight license is verified. Moonshine v2 Base English is the current clearly documented MIT-licensed challenger to investigate first, subject to exact package provenance and on-device resource checks. If its resource cost is unacceptable, keep Tiny as the baseline and continue looking for another clearly licensed challenger.

## Test corpus

Use the same fixed, documented clip manifest and reference transcripts for every model.

1. Earnings-22 test clips: financial calls, varied accents, spontaneous speech, teleconference audio, and domain vocabulary.
2. TED-LIUM 3 test clips: prepared public talks and longer-form speech.
3. LibriSpeech test-clean: clean read-speech sanity/reproducibility check, not the deciding real-world metric.
4. Optional follow-up: AMI meeting clips to test conversational turn-taking and multi-speaker conditions. Treat overlap and diarization separately; ordinary ASR WER alone does not measure speaker attribution.

Do not use a model's training corpus as its independent test set. In particular, because the challenger is GigaSpeech-trained, do not use GigaSpeech test clips as the primary independent comparison set.

Dataset background and published split/licensing information:
- Open ASR Leaderboard dataset overview: https://huggingface.co/datasets/hf-audio/open-asr-leaderboard
- Long-form evaluation set overview: https://huggingface.co/datasets/hf-audio/asr-leaderboard-longform

## Required controls

- Run every model on the same exact audio bytes, sample rate, channel conversion, clip boundaries, and reference transcript.
- Use the same text-normalization and WER implementation. Publish whether punctuation, casing, number normalization, and disfluencies are normalized.
- Disable VAD for already segmented benchmark clips unless VAD is explicitly the feature under test. Evaluate continuous-audio segmentation separately.
- Keep model decoding settings documented and fixed. Do not compare confidence scores as if they were calibrated.
- Record exact app build, Android version, device model/RAM configuration, sherpa-onnx version, model revision, and thread settings.
- Run cold-load and warm inference separately. Repeat timing runs and report median plus range.
- Do not run two large models simultaneously on the phone. Release the first recognizer and confirm cleanup before loading the next.

## Measurements

For each model, report:

- Corpus-level WER and per-dataset WER (substitutions, deletions, insertions).
- Real-time factor (inference duration divided by audio duration), plus end-to-end stop-to-transcript latency.
- Cold model-load time and first-use setup/download time, separately.
- Download archive size and installed model footprint.
- Peak process/native memory where measurable; record baseline and peak, not just a single unexplained number.
- Failure rate, crashes, empty/partial output, and timeout count.
- Thermal state and battery impact as qualitative observations if reliable instrumentation is unavailable.

A model is not a winner if a lower WER comes with unacceptable memory pressure, load time, crashes, or app instability on the actual device.

## Decision gate

1. Preserve Moonshine as the known-working default while evaluation is underway.
2. Validate the candidate's model files, licensing, and sherpa-onnx 2.0.1 compatibility without changing the production/default path.
3. Run the controlled corpus on the Galaxy A36.
4. Compare accuracy, latency, memory, footprint, and failures.
5. Choose the model only if the measured trade-off is materially better for Kai's intended use.
6. If results are inconclusive, retain Moonshine and document the uncertainty.

## Published results: context, not Kai results

Moonshine's current model documentation reports quantized Tiny Streaming WER of 4.83% on LibriSpeech test-clean and 12.00% on its eight-dataset Open ASR average. These are different metrics over different evaluation sets and are not Kai-device measurements:
- https://github.com/moonshine-ai/moonshine/blob/main/docs/models/available-models.md
- https://github.com/moonshine-ai/moonshine/blob/main/docs/models/accuracy.md

An independent Android benchmark repository reports results for several sherpa Zipformer variants on a Snapdragon 690 device. Its reported numbers vary by model variant; they are not measurements of Kai's exact candidate on the Galaxy A36. Use them only as motivation to benchmark locally:
- https://github.com/imedwei/android-asr-benchmark

## Current next action

Prepare a reproducible Android benchmark harness/variant that can load one selected model at a time, consume the fixed clip manifest, write machine-readable per-clip results, and emit aggregate WER/latency/memory summaries. Keep this separate from the user-facing Kai speech flow until the candidate passes the decision gate.
