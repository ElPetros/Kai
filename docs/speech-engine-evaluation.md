# Speech engine evaluation

## Decision

Use **sherpa-onnx as the Android inference runtime**, but treat the recognition model as replaceable. The first code path uses Moonshine v2 Tiny English because it is a compact, modern baseline with a simple offline API. It is not yet proven to be the most accurate model for Kai's conversational use.

The next model to compare in the same test harness is sherpa-onnx Zipformer GigaSpeech. An independent Android benchmark on a Snapdragon 690 / 6 GB device reported strong results for it on unseen conference-call and TED speech. That is more relevant to spontaneous use than clean audiobook results, although the benchmark is small and does not include Moonshine.

## Technology comparison

| Option | Practical strengths | Main costs and weaknesses |
|---|---|---|
| **Vosk** | Mature Android API; Apache-2.0; small English model around 40 MB; supports streaming recognition and vocabulary adaptation. | Official model guidance estimates about 300 MB runtime memory for small models. In an independent Snapdragon 690 test, the small English model reached 30.6% average WER on Earnings-22 and TED-LIUM 3, despite lower published scores on its own LibriSpeech/TED-LIUM test setup. |
| **sherpa-onnx** | Android Kotlin API; one native runtime supports several model families, offline and streaming recognition, and VAD. This lets Kai compare models without replacing the whole audio stack. | Android AAR is about 47.2 MB and multi-ABI. Model selection still matters: small models can be weak on unfamiliar speech, while larger models consume more memory. |
| **whisper.cpp** | MIT-licensed runtime; broad multilingual support; quantized Tiny English model can be as small as 31 MB; useful as an independent accuracy reference. | Requires a separate C++/NDK/JNI integration. In the cited Snapdragon 690 test, Tiny English Q5 ran at RTF 5.98 (slower than real time) and averaged 20.7% WER on Earnings-22 plus TED-LIUM 3. That is a poor default for quick phone dictation. |

## Model evidence

### Moonshine v2 Tiny English

- 34 million parameters; approximately 43 MiB of quantized model files.
- 4.83% WER on LibriSpeech test-clean for the shipped quantized model.
- 12.00% average WER on the Open ASR Leaderboard's eight-dataset evaluation.
- MIT license.

Those are two different test aggregates. LibriSpeech test-clean is read audiobook speech; the eight-dataset average is broader. Neither number alone predicts accuracy in a noisy room or on overlapping conversation.

### Zipformer GigaSpeech

An independent Android benchmark on a Boox Palma 2 Pro (Snapdragon 690, 6 GB RAM) reported:

- 6.7% WER on Earnings-22 conference calls.
- 18.0% WER on TED-LIUM 3 talks.
- 12.3% average WER across those two sets.
- RTF 0.06 and approximately 216 MB additional native RAM.

The model uses an int8 encoder with smaller decoder and joiner files. The benchmark used sherpa-onnx AAR 6.25.12, not the 2.0.1 runtime currently pinned in Kai, and is a small independent project. Treat these numbers as promising evidence, not a guarantee that the same results will transfer to Kai or the Galaxy A36.

### Vosk small English

The Vosk model catalogue reports 9.85% WER on LibriSpeech test-clean and 10.38% on TED-LIUM for `vosk-model-small-en-us-0.15`. It describes the model as approximately 40 MB and estimates around 300 MB runtime memory for small Vosk models.

Those official figures use a different evaluation setup from the independent Android benchmark above. The discrepancy is a reminder not to compare WER values across unrelated test harnesses.

### whisper.cpp Tiny English

The upstream model catalogue lists the full Tiny English model at 75 MiB and the Q5_1 quantized variant at 31 MiB. A separate Android benchmark on Snapdragon 690 measured Tiny English Q5_1 at 20.7% average WER across Earnings-22 and TED-LIUM 3, with RTF 5.98. This makes it useful as a reference model, but not the leading choice for a responsive default on a mid-range phone.

## Next evaluation

Before calling any model the winner, run Moonshine Tiny v2 and Zipformer GigaSpeech through the same harness on the same public test clips:

1. Earnings-22 for conference-call audio, accents, and domain vocabulary.
2. TED-LIUM 3 for clear but spontaneous presentation speech.
3. LibriSpeech test-clean as a reproducibility check, not the deciding metric.

Record WER, real-time factor, model load time, peak memory, and failure rate. Do not compare confidence scores from different recognizers as if they were calibrated to the same scale.

The current implementation intentionally keeps the runtime and composer integration separate from model-specific logic so the recognition backend can be changed after that evaluation.
