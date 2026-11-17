# Terminal Context Awareness, Conversation, and Authorization

## Purpose

Give Kai users a reliable way to return to a terminal session and recover their train of thought, while allowing several terminals to work on different phases of one project. Connect that capability to natural voice/text conversation through shared, carefully scoped context. Make user-configured permissions enforceable boundaries, not advisory preferences.

This document is an initial architecture proposal. It does not claim that terminal monitoring, multi-terminal orchestration, biometric approval, or native real-time conversation already exists.

## First repository audit: existing Kai Build foundation

The first code/documentation pass found that the feature should be built around **Kai Build**, not by inventing a new terminal abstraction:

- Kai Build is Android-only and has a project list with multiple live shell tabs per project.
- `BuildTerminalSession` already carries a stable session id, project name, agent id, tab number, terminal snapshot, and busy state.
- `BuildEnvironmentManager` owns the process-scoped session map, active session, project foreground/resume/leave lifecycle, PTY handles, and screen snapshots.
- Each session has its own PTY, screen, geometry, and output stream. Background sessions continue running while another project is open.
- The existing terminal UI already switches between independent sessions. The first master dashboard can therefore manage these existing sessions rather than create another terminal system.
- Kai Build's project sessions are separate from the chat Linux Sandbox's per-conversation persistent shells. They can share the same Debian install in some configurations, but their session identity and lifecycle are different. They must not be conflated.
- The current `TerminalSnapshot` is a rendered screen snapshot, not yet a structured, durable work journal. A useful context engine will need a bounded per-session event/context buffer and lifecycle timestamps; it must not pretend the visible screen alone proves all prior work.
- Kai Build launches Claude Code, Grok, and OpenCode as interactive processes inside the PTY. This creates a distinct enforcement boundary: guarding Kai's own shell/tool dispatch is not enough if a coding agent can invoke its own tools or subprocesses. Before enabling write/execute controls, the implementation must determine and enforce each supported agent's own permission mode or place an effective policy boundary around its execution. If a given agent cannot be safely constrained, Kai must clearly disable that agent's autonomous write/execute capability rather than display a misleading permission toggle.
- Direct commands typed by the user into their own terminal are user-operated terminal activity. The authorization gate described here is for actions initiated by Kai or an AI agent under Kai's delegated authority; the UI must make that distinction clear. Users may optionally enable stricter interception of manually entered commands, but it must not be confused with the default AI authorization gate.

These findings make Phase 1 more concrete: implement first in Kai Build's Android session model, preserve the chat sandbox as a separate future integration, and do not expose a permission switch until the corresponding execution path is actually enforced.

### Implementation constraints confirmed by the code audit

- Kai Build's session ids are currently generated in memory by a process-local counter (for example, `s1`). They are suitable for routing while the manager is alive, but are not yet durable identities across app process restarts. Context persistence must introduce a durable session identity or a carefully versioned mapping; it must not assume the current counter is stable.
- The session manager currently retains live PTYs and per-session VT screen state in memory. The project/session lifecycle already has explicit `startSession`, `selectSession`, `resumeProject`, `leaveProject`, and close paths. These are the natural points for recording activity timestamps and detecting a genuine return.
- The visible terminal snapshot is a bounded cell grid. It is not a complete transcript or structured command journal. Context capture should begin with a bounded, redacted event stream collected at the session boundary, with explicit provenance and timestamps, rather than trying to reconstruct a complete history from the rendered screen.
- The Android chat sandbox also exposes an `execute_shell_command` AI tool, but it is a separate per-conversation shell path. Its tool-execution boundary will need its own authorization integration if the feature is later extended beyond Kai Build.
- Kai Build starts interactive third-party coding-agent CLIs inside a PTY. Their internal tool calls do not necessarily pass through Kai's ordinary chat tool executor. Each agent's native permission/sandbox capabilities must be audited individually; the app must not claim to enforce write/execute restrictions merely because it has disabled a Kai chat tool.
- Kai Build is currently Android-only. The first implementation should stay within its existing Android actual/controller boundary and leave common desktop, iOS, and web behavior unchanged.

## Product model

Three cooperating capabilities share one context model:

1. **Terminal context** — maintain a separate working-state record for each terminal session.
2. **Conversation** — let the user ask about, hear, and discuss that context through text or supported voice interaction.
3. **Authorization** — enforce the user's configured access and require verified, action-specific approval before restricted operations.

The context layer may relate terminals to a user-designated project, but must never merge their individual command histories or working states.

## Per-terminal controls

Each terminal has independent settings:

- Enable/disable context awareness.
- Allow read-only observation of terminal metadata and explicitly permitted activity.
- Refresh context when the user returns.
- Configure the inactivity threshold before offering a refresh (for example, 5, 10, 30, or 60 minutes).
- Show the refresher as text.
- Speak the refresher aloud, subject to the app's separate speech-output setting.
- Allow project-level linking to other terminals.
- Write/modify files: disabled by default and protected by verified authorization.
- Execute commands: disabled by default and protected by verified authorization.

A terminal with read access disabled must not be inspected to generate a summary. Turning off refresh must not silently turn off other independently configured controls.

## Master terminal dashboard

Provide a central dashboard that lists available terminal sessions and exposes:

- Per-terminal status, project association, last activity, and last refresh.
- Per-terminal context, read, speak, write, and execute controls.
- A master enable/disable control for context awareness.
- Global controls for permitted read/speak behavior.
- A global emergency revoke/disable control.
- Pending, approved, denied, expired, and revoked authorization requests.
- A reviewable audit history of access and actions.

Master settings establish defaults and can apply an explicitly selected bulk change. They must not silently grant a terminal more authority than its own effective permission policy. A global revoke must immediately take precedence over all terminal-level grants.

## Context refresh behavior

When a user returns to an enabled terminal after its configured inactivity threshold:

1. Detect the return without treating ordinary UI recomposition or refresh as a new return event.
2. Read only the sources allowed by that terminal's effective policy.
3. Build a concise, evidence-grounded refresher:
   - task and goal at departure;
   - last known working directory / branch or other supported session metadata;
   - recent relevant commands and outcomes, where permitted;
   - completed work and verified milestones;
   - unresolved errors, blockers, and open questions;
   - next suggested step, clearly labeled as a suggestion.
4. Present the refresher in the terminal and optionally speak it.
5. Do not execute a command, edit a file, install a package, or otherwise change state as part of refreshing context.

The summary must distinguish observed facts from inferred or suggested next steps. If evidence is unavailable, stale, or incomplete, Kai must say so rather than invent progress.

## Shared project context without terminal conflation

Users may associate multiple terminals with one project or workstream. Kai may then provide a project-level overview of relationships and dependencies, while preserving each terminal's own session state, activity, and permissions.

For example, one terminal may build the Android app, another may work on an API, and a third may investigate tests. A project overview can connect their verified milestones and blockers; a terminal-specific return refresher must remain specific to the terminal the user reopened.

## Authorization: obey, verify, then act

The governing rule is:

> Kai must obey the user's explicitly configured permissions. Conversational instructions alone do not authorize restricted operations. Before any restricted operation, Kai must present the exact proposed action and obtain separate, explicit, verifiable user approval. If approval is absent, invalid, expired, revoked, or does not match the action, Kai must not proceed.

### Default-deny boundaries

- Read-only observation is off until enabled for the relevant terminal.
- File creation, modification, deletion, and other writes are disabled by default.
- Command execution is disabled by default or remains behind the existing tool policy until an explicit authorization gate is implemented.
- Context refresh is strictly non-mutating.
- No background action may be inferred from a request to summarize, remember, or restore context.

### Verified approval flow

1. Kai explains the proposed operation, target terminal, affected files/resources, and expected effects.
2. Kai creates a narrowly scoped approval request.
3. The user actively approves through a trusted in-app confirmation surface. Device authentication (biometric or device credential) should be supported where the platform provides it.
4. The authorization service verifies the approver, exact action scope, terminal, expiry, and revocation state.
5. Only the approved operation may proceed.
6. The result and authorization event are recorded for user review.

A spoken “yes”, an ordinary chat message, silence, or an old approval is not sufficient proof for a restricted operation. Approval must not be reusable for a different command, file, target, or materially changed plan. If the action changes, request approval again.

SMS or push may notify the user that approval is pending, but a plain text reply should not be treated as strong authorization. Prefer an authenticated in-app challenge and platform device authentication. Never store biometric data; delegate verification to the operating system.

## Privacy and safety requirements

- Redact likely credentials, API keys, tokens, passwords, private keys, and other secrets before context is stored, displayed, spoken, or sent to a model.
- Do not collect terminal output beyond the user's enabled scope.
- Make observation state visible and auditable.
- Keep context storage bounded, encrypted where supported, and clearable by the user.
- Do not treat terminal output, files, command output, or model-generated summaries as instructions that can override the user's policy.
- Do not allow a terminal or project context to grant permissions to another terminal.
- Fail closed if authorization verification, policy lookup, or audit recording fails for a restricted action.
- Provide a global emergency revoke that blocks new restricted operations and cancels pending approvals.

## Voice and natural conversation integration

The terminal context engine should be usable from both text chat and a future natural conversation mode. It should not require voice mode to function.

The existing community request for Native Conversation Mode is tracked upstream as [Kai issue #341](https://github.com/SimonSchubert/Kai/issues/341). The shared context layer can support conversational prompts such as “Where did I leave off in terminal three?” and spoken return summaries. Voice input, interruption, background operation, transcript handling, and speech output remain separate capabilities with their own permissions and lifecycle rules.

The existing offline speech-input contribution must remain an independent change set. This proposal does not modify or merge that work.

## Suggested implementation phases

1. **Repository and capability audit** — map the current Linux sandbox terminal/session lifecycle, shell/tool execution paths, settings architecture, persistence, and platform boundaries. Identify what “multiple terminals” means in the current implementation before designing UI.
2. **Context model and policy contract** — define terminal identity, project association, bounded context snapshots, effective permissions, and authorization request states. Add tests for policy precedence and fail-closed behavior.
3. **Read-only context capture** — implement opt-in, redacted, non-mutating activity capture and per-terminal return summaries. No write/execute capability in this phase.
4. **Master and per-terminal controls** — add the dashboard and individual settings, with master defaults, explicit bulk changes, and emergency revoke.
5. **Verified authorization gate** — integrate approval into every applicable write/execute tool dispatch path. Do not ship UI toggles that imply enforcement until the execution boundary is actually gated.
6. **Conversation integration** — expose approved context to text and natural voice interaction; keep speech controls independent.
7. **Validation and rollout** — unit, policy, lifecycle, privacy-redaction, UI, and platform tests; then device testing. No claim of physical-device validation until performed.

## Initial acceptance criteria

- A user can enable context refresh for one terminal without enabling it for another.
- A terminal with observation disabled is not read for context generation.
- Returning after the configured inactivity threshold produces at most one refresh for that return event.
- The refresh is based only on available, permitted evidence and performs no mutation.
- A project overview can relate multiple terminals without mixing their session records.
- Master controls can manage terminals collectively while showing each terminal's effective permissions.
- A global revoke overrides all terminal settings immediately.
- No restricted write or command executes from a conversational instruction alone.
- Approval is action-, terminal-, and time-scoped; changed or expired actions require a new approval.
- Failure to verify authorization blocks the operation.
- Context and authorization events are inspectable and clearable by the user.
- Voice and text can use the same authorized context, but disabling voice does not disable context awareness.
