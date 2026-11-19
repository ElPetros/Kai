# Kai Offline Speech Input — System Map

This is the high-level flow for maintainers and testers. It shows the boundaries between the Android app, offline speech pipeline, GitHub build/review, and the physical test device.

```mermaid
flowchart TB
    U["User presses microphone (push-to-talk)"]
    P["Android foreground capture<br/>16 kHz mono PCM"]
    V["Silero VAD<br/>speech-region detection"]
    Q{"Speech region found?"}
    N["No-speech result<br/>leave composer unchanged"]
    M["Moonshine Tiny English<br/>via sherpa-onnx"]
    T["Transcript text"]
    C["Kai editable composer<br/>review before send"]
    A["User explicitly submits"]
    X["Kai conversation / tools<br/>normal permission rules"]

    U --> P --> V --> Q
    Q -- "No" --> N
    Q -- "Yes" --> M --> T --> C --> A --> X

    subgraph Android["Android device — isolated validation app"]
      P
      V
      Q
      N
      M
      T
    end

    subgraph KaiUI["Kai UI and authorization boundary"]
      C
      A
      X
    end

    subgraph ModelSetup["First-use model setup"]
      D["Download approved model files"]
      H["Verify expected size + SHA-256"]
      I["Install in app-private storage"]
      D --> H --> I
      I -. "available to VAD / ASR" .-> V
      I -. "available to VAD / ASR" .-> M
    end
```

## Engineering and review map

```mermaid
flowchart LR
    SRC["Source branch<br/>fix/speech-vad-60s-hardening<br/>d816db8"]
    CI["GitHub Actions Run #60<br/>build + shared tests"]
    ART["Debug + release APK artifact<br/>expires 2026-11-26"]
    DEV["Samsung Galaxy A36<br/>five VAD checks reported PASS"]
    CP["Checkpoint branch<br/>device validation record"]
    HAND["Handoff branch<br/>report + safe connection list + diagram"]
    PR["PR #4<br/>Draft / open / unmerged"]
    SIMON["Simon: maintainer review<br/>accept / revise / defer / decline"]

    SRC --> CI --> ART --> DEV
    SRC --> CP --> HAND
    HAND --> SIMON
    PR --> SIMON
    ART -. "temporary build evidence" .-> HAND
    DEV -. "user-reported evidence" .-> CP
```

## What each part is

| Part | Responsibility |
|---|---|
| Android capture | Foreground push-to-talk audio capture, bounded to the app's recording limit |
| Silero VAD | Detects speech regions before recognition; rejects silence/noise-only input |
| Moonshine Tiny | Performs local English speech recognition through sherpa-onnx |
| Editable composer | Receives transcript as draft text for user review |
| User submission | Explicit user action; spoken words alone do not authorize tools |
| GitHub Actions | Builds Android FOSS debug/release variants and runs shared desktop tests |
| Artifact | Temporary ZIP containing validation APKs; download before expiry |
| Galaxy A36 | One-device functional test evidence; not a broad compatibility claim |
| Checkpoint / handoff branches | Preserve validation record and forwardable maintainer documentation |
| PR #4 | Existing draft contribution; keep unmerged pending review and explicit approval |

## Boundaries

- No always-on microphone, wake word, diarization, cloud transcription, or paid speech API.
- No automatic tool authorization from recognized speech.
- Keep optional auto-send disabled during the initial preview.
- Run #60 is tied to the VAD validation branch and commit, not assumed to validate PR #4's current head.
- The Galaxy A36 results are reported by the user; no statistical benchmark is claimed.
