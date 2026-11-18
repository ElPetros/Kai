# Kai 9000 — offline speech benchmark harness

Status: **scoring, manifest validation, and aggregation foundations added; device runner and comparative measurements not yet implemented.**

This work is isolated on `feature/speech-benchmark-harness`, based on the verified recovery checkpoint. It does not change the user-facing speech implementation or the original feature branch.

## Goals

Build a repeatable evaluation path for offline ASR models on the Samsung Galaxy A36. It must compare models on identical audio and references, report accuracy and device cost, and leave the working Moonshine path untouched until an evidence-based decision is made.

## Non-goals and safety boundaries

- Do not replace or reconfigure the working Moonshine speech input.
- Do not run two large recognizers concurrently on the phone.
- Do not upload audio or transcripts to a service.
- Do not commit third-party corpus audio or reference transcripts unless the dataset license explicitly permits redistribution.
- Do not interpret ordinary ASR WER as speaker diarization, overlap detection, or turn attribution.
- Do not merge, release, or change the default model as part of benchmark development.

## Current implementation

### Word-error-rate scorer

`composeApp/src/commonTest/kotlin/com/inspiredandroid/kai/speechbenchmark/WordErrorRate.kt` contains a deterministic Levenshtein-based word-error-rate scorer. It is in the shared test source set, not app production source, so it is not shipped in Kai.

Normalization policy v1:

1. Apply Kotlin's locale-independent Unicode lowercase operation.
2. Convert curly apostrophes to straight apostrophes.
3. Treat Unicode letters and digits as tokens; preserve an apostrophe only when internal to a token.
4. Treat punctuation and whitespace as separators.
5. Do not rewrite number words, digits, abbreviations, or disfluencies.

The scorer reports substitutions, deletions, insertions, reference and hypothesis word counts, total errors, and WER. If the reference has zero scored words, WER is undefined and is returned as null; insertion counts remain visible. Equal-cost edit paths use a deterministic substitution-then-deletion-then-insertion tie-break. The total edit distance is invariant, but operation-category counts can depend on tie-breaking, so the policy is fixed and documented.

Tests cover exact matches, each edit operation, punctuation/case normalization, apostrophes, empty references/hypotheses, and the fact that number words are not silently converted to digits.

### Manifest validation

\`SpeechBenchmarkManifest.kt\` defines a versioned manifest and validates non-empty dataset/release/split metadata, unique clip IDs, safe relative audio paths (no absolute paths, traversal, or Windows separators), SHA-256 syntax, mono 16 kHz prepared audio, positive duration, and non-empty reference text. Validation is structural; the future device runner must still verify the actual file bytes against each SHA-256.

### Result and aggregate model

\`SpeechBenchmarkResults.kt\` records model/device/build metadata and per-clip success or failure. Failed clips remain visible in the failure rate but are excluded from WER and latency summaries. Corpus WER is computed from summed S/D/I counts divided by summed reference words. Median inference time and median real-time factor are reported, along with min/max latency and optional model-size/memory observations. Empty or all-failed runs return null accuracy/latency values instead of invented zeros.

### Local JSON format

\`SpeechBenchmarkJson.kt\` provides strict JSON encoding/decoding for manifests and reports. Manifest encoding and decoding both run structural validation. Report encoding and decoding verify the schema version and recompute the aggregate from per-clip results, rejecting a report whose summary does not match its underlying measurements. The codec is test-source-only at this stage; a device runner still needs to write the JSON to an explicit local destination.

## Required benchmark runner design

The device runner should be a test/debug-only path and should implement these stages:

1. **Input manifest validation:** require a unique clip ID, dataset/release/split, local audio path, SHA-256, sample rate, channel count, clip duration, and reference transcript source.
2. **Audio preparation:** decode each clip once to a documented PCM representation. Verify audio hashes before processing. Do not silently resample or trim; record any conversion explicitly.
3. **One-model-at-a-time lifecycle:** load one recognizer, record cold-load duration and memory baseline, run the fixed clips sequentially, release all native resources, then load the next model.
4. **Per-clip measurement:** record model ID/revision, clip ID/hash, audio duration, recognition duration, real-time factor, WER counts/rate, empty output, exception/timeout, and other failure state.
5. **Aggregate report:** calculate corpus and per-dataset WER from total S/D/I counts (not the unweighted average of clip WERs), median/range inference time, real-time factor, load time, installed model footprint, memory observations, and failure counts.
6. **Reproducibility metadata:** record app/build revision, Android version, device model/RAM, sherpa-onnx version, model checksums, decoder settings, thread count, normalization policy version, and benchmark date.
7. **Local-only output:** write machine-readable JSON results to an explicit user-selected or app-private benchmark output location. No network transmission.

## Candidate comparison

- Baseline: quantized Moonshine v2 Tiny English, sherpa-onnx 2.0.1.
- First challenger to investigate: quantized Moonshine v2 Base English. The official catalogue lists English Base as MIT-licensed and approximately 135 MB; exact package size/hash and Galaxy A36 load/resource behavior still need verification. This is a same-family size/accuracy comparison against Tiny.
- Secondary architecture lead: Zipformer GigaSpeech English offline transducer, upstream ID `sherpa-onnx-zipformer-gigaspeech-2023-12-12`.
- **GigaSpeech model-weight license is unresolved.** Dataset access terms restrict the audio to non-commercial research/education, while the repository's Apache license does not by itself establish rights to the audio or the derived model weights. Do not bundle or distribute this model with Kai until weight redistribution rights are clarified.
- sherpa-onnx 2.0.1's Kotlin API includes the transducer encoder/decoder/joiner configuration shape, but the candidate has not been loaded on Android.
- Alternative lead: sherpa-onnx Zipformer Small trained on LibriSpeech, with a listed 25 MB int8 encoder. Its model-weight license and checksum also require verification before use.
- Primary corpora in the evaluation plan: Earnings-22 and TED-LIUM 3; LibriSpeech test-clean is a clean-speech sanity check, not the deciding real-world metric.
- Do not use a candidate's own training corpus as the independent primary test set.
- AMI meeting audio is a possible later conversational/overlap evaluation, with speaker attribution evaluated separately from WER.

## Current verification status

- WER scorer, manifest validator, aggregate result model, strict JSON codec, and corresponding unit tests have been committed to this branch.
- The dedicated GitHub Actions workflow has started; its final conclusion still needs to be checked before calling the test suite green. The existing repository workflow only runs on pushes to `main` and pull requests targeting `main`; this feature branch is intentionally not the base of the existing speech PR. A dedicated branch-scoped test workflow is being added to run ` :composeApp:desktopTest ` for this isolated benchmark foundation.
- No Android benchmark runner exists yet.
- No candidate model has been downloaded, loaded, or benchmarked on the A36.
- No comparative result or model-selection decision exists.

## Next implementation steps

1. Confirm the current CI run and fix any compile/test/format issues.
2. Enable a dedicated Android device-test source set only after confirming its Gradle configuration and instrumentation dependencies.
3. Implement a one-model-at-a-time Android runner that can reuse the existing Moonshine recognizer path without changing the production speech flow.
4. Add explicit local corpus import and audio-hash verification; do not bundle or upload dataset recordings.
5. Validate the Zipformer candidate package, checksum, license, runtime API compatibility, and actual installed footprint before loading it on-device.

Update this document and the recovery checkpoint after each verified milestone. A source commit alone is not proof that tests passed.
