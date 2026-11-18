# Terminal Workgroups: Single-Kai Coordination and Change-Safety Contract

**Status:** Architecture contract / implementation plan  
**Scope:** Kai Build terminal sessions, explicitly formed workgroups, shared user instructions, terminal coordination, and auditable results  
**Non-goal:** This document does not itself implement orchestration or grant terminals additional execution permissions.

## 1. Purpose and operating model

A workgroup is a set of terminal sessions that the user explicitly chooses to coordinate around one objective.

**There is one Kai coordinating a workgroup.** The terminals are separate execution environments controlled by that one Kai; they are not separate Kai instances or independent AIs that must negotiate with one another. Kai maintains the shared workgroup context, interprets the user's directions, chooses the appropriate terminal for each operation, observes the results, and coordinates the overall sequence.

Each terminal still has its own shell/process, working directory, runtime, output, and stable session identity. A workgroup adds one shared coordination layer without merging those environments.

The workgroup supports:
- one shared objective, instruction context, task plan, and activity history;
- explicit membership of stable terminal session IDs;
- one Kai controller that can inspect and operate each member terminal within its permissions;
- a shared, attributable record of user instructions and terminal contributions;
- task ordering, resource-conflict prevention, verification, and an auditable outcome.

The design must support any practical number of members, not a fixed pair or trio.

## 2. One Kai; multiple terminal environments

The workgroup controller owns:
- the user's shared objective and continuing instructions;
- awareness of the current terminal from which the user is interacting;
- the mapping between each terminal's stable identity and its visible label;
- the task plan, assignments, dependencies, and execution sequence;
- dispatch of authorized actions to the appropriate terminal;
- collection and attribution of terminal output;
- cross-terminal handoffs, verification, and the shared workgroup history.

A terminal is an execution target, not a separate workgroup AI. Kai must not require the user to repeat the entire objective when moving between terminals in the same workgroup. New instructions from the user in any member terminal update the shared workgroup conversation and must be interpreted against the existing objective and task context.

Terminal-local tools or command-line programs may themselves be AI-enabled products, but that does not create additional Kai controllers. If such a program is used, it remains a process running in that terminal and subject to Kai's orchestration and the user's permissions.

Kai must not pretend that all terminals share one shell or filesystem. She coordinates distinct environments and transfers only the information or artifacts required for each step.

## 3. Preserve terminal independence and explicit membership

Each terminal retains its own:
- stable session ID and lifecycle;
- shell, process/PTY, working directory, and runtime/container;
- terminal output and execution state;
- input draft and command history;
- permissions and execution policy.

A workgroup adds coordination; it does not merge terminal sessions or erase their identities. Membership is explicit user intent. Similar directory names, repository names, branches, or activity may help Kai suggest related terminals, but must never silently create membership.

A terminal may belong to more than one workgroup only if the implementation can safely represent concurrent assignments and resource claims. Otherwise, Kai must reject or serialize overlapping workgroup operations rather than silently share mutable state.

## 4. Terminal identity and current interaction context

For every user interaction in a workgroup, Kai must distinguish:

- **Current interaction terminal:** the terminal the user is currently viewing or speaking from.
- **Workgroup identity:** the explicitly selected group whose shared context is active.
- **Instruction context:** the existing objective, conversation, and task to which the new direction relates.
- **Action target:** the terminal or resource where Kai is being asked to perform the operation.
- **Result source:** the terminal that produced a particular output or artifact.

These identities are related but are not interchangeable.

The terminal from which the user gives an instruction is context, not an automatic action destination. An instruction spoken in Terminal 1 may direct Kai to inspect Terminal 3 and then modify something in Terminal 2. Likewise, an instruction spoken in Terminal 2 may refer to work underway in Terminal 1.

Kai should use the current terminal and the shared conversation to resolve references such as "here", "that file", "the other terminal", or "bring it over". If the intended source, destination, or operation remains genuinely ambiguous, Kai asks one focused clarification rather than guessing.

Before acting, Kai must identify the actual environment of each target terminal. Useful evidence includes:
- canonical workspace/repository root;
- repository remote identity, when available;
- Git worktree root, branch, HEAD commit, and working-tree state;
- runtime/container/host identity;
- current working directory and relevant process/session identity.

Identity is time-sensitive. A terminal can change directory, switch branches, change containers, or alter its working tree after joining. Kai must refresh relevant evidence before dispatching work and before integration. A stale identity snapshot is not sufficient authorization to act.

Kai must distinguish:
- same repository, same working tree;
- same repository, separate worktrees or clones;
- related projects or repositories;
- identity unknown or unverified.

Unknown identity is not treated as a match. If the user deliberately groups different projects, Kai can coordinate them under one objective while preserving their distinct identities.

## 5. Shared workgroup conversation and attributed transcript

A workgroup has **one shared instruction and transcription history**, not isolated AI conversations for each terminal.

The shared record may contain:
- user instructions dictated or typed while any member terminal is active;
- Kai's clarifications and confirmed understanding;
- task-plan updates and handoffs;
- terminal output excerpts or summaries that Kai chooses to retain;
- completion, failure, and verification results.

Each contribution must retain its provenance. A visible source marker (for example, a terminal label or icon) identifies which terminal was active when that section was contributed or which terminal produced the output. The marker is attached to the individual section, not merely to the overall transcript.

The underlying record must use stable terminal session IDs, not only display names or tab numbers. Renaming a terminal must not rewrite historical attribution. Timestamps and a unique record/event ID should distinguish contributions. For a user instruction, the record identifies the interaction terminal; for terminal output, it identifies the producing terminal. These are different event types and must not be conflated.

A useful conceptual record is:

```kotlin
data class WorkgroupTranscriptEntry(
    val id: String,
    val workgroupId: String,
    val kind: TranscriptEntryKind,
    val sourceTerminalSessionId: String?,
    val text: String,
    val createdAt: Instant,
    val relatedTaskId: String?,
)
```

This is a domain sketch, not a commitment to these exact Kotlin types. System-generated entries may have no source terminal. A user instruction's source terminal is not necessarily its action target. Task assignments and execution events must record their own target terminal IDs.

The shared record and each terminal's editable command draft are separate concepts. A transcript entry is an attributable history item; copying text into a terminal draft does not erase or change the original source attribution. If the user explicitly asks Kai to move or reassign an item, retain the original source and record the handoff or reassignment separately.

Do not persist raw microphone audio by default. Retain recognized text and only the metadata needed for attribution, continuity, and audit, subject to the product's privacy and retention controls.

## 6. User instructions and the brief workgroup reminder

Users may give directions naturally from any terminal. Kai should interpret those directions using the shared workgroup context and current terminal awareness; users do not need to memorize a rigid command syntax.

Show this brief, non-intrusive reminder in the workgroup interface:

> **A quick reminder when working with Kai**
>
> For best results, give Kai clear, concise directions. Identify what you want done, which terminal or location you're referring to, and where the results should go.
>
> Be mindful of your wording when dictating instructions. Avoid combining unrelated requests or giving conflicting directions all at once.
>
> If something isn't clear, Kai may ask you to clarify before proceeding.
>
> **Clear instructions help Kai work accurately and keep your workgroup running smoothly.**

This is guidance, not a command language or a restriction on natural speech. The platform must remain robust when a user speaks imperfectly.

Kai should:
- proceed when the requested action, target, and authorization are sufficiently clear;
- ask a focused clarification when an essential detail is missing;
- identify conflicting directions instead of silently choosing one;
- stop before an unsafe, destructive, or unauthorized operation and follow its existing confirmation requirements.

## 7. Workgroup task and execution model

Kai maintains one shared task plan and may direct a sequence across several terminals. The user can describe the desired sequence in ordinary language, for example: inspect material in Terminal 1, place or process it in Terminal 3, then use the verified result in Terminal 2.

Conceptual model (names may change during implementation):

```kotlin
data class TerminalWorkgroup(
    val id: WorkgroupId,
    val objective: String,
    val memberSessionIds: Set<TerminalSessionId>,
    val tasks: List<WorkgroupTask>,
    val state: WorkgroupState,
    val revision: Long,
)

data class WorkgroupTask(
    val id: TaskId,
    val targetTerminalSessionId: TerminalSessionId,
    val instruction: String,
    val dependsOn: Set<TaskId>,
    val resourceClaims: Set<ResourceClaim>,
    val state: TaskState,
)
```

This is a domain sketch, not a commitment to these exact Kotlin types. The model must use stable IDs, support more than two members, reject duplicate/unknown members, and make state transitions explicit.

Kai owns the overall plan and dispatches work to the appropriate terminal. A downstream step must not use an upstream result until Kai has observed that the upstream operation finished and has checked that the required result is available. This is a sequencing and evidence rule inside one controller, not a handoff between separate AIs.

Kai should preserve the user's requested order. If a step fails, produces an unexpected result, or cannot be verified, Kai pauses dependent steps and explains the issue. She must not quietly skip ahead or claim completion.

## 8. Same-file and shared-resource safety

**Core rule: shared access is allowed; uncoordinated conflicting modification is not.**

Kai must reason about interference, not merely whether two tasks mention the same path. Changes to different lines can still conflict semantically through shared functions, types, APIs, imports, invariants, generated files, or dependencies.

### 8.1 Default policy

- Multiple terminals may inspect the same file concurrently.
- A read-only review may run while another terminal edits, provided the reviewer understands that its view may become stale and revalidates before reporting final conclusions.
- In the same mutable working tree, Kai grants one active write owner per file by default.
- A second write assignment that overlaps the same file is queued or rejected until the first writer finishes and its result is reviewed.
- Kai must not infer safety from different line numbers alone.

### 8.2 When concurrent editing may be allowed

Concurrent editing of the same logical file may be allowed only when Kai can establish a controlled isolation and integration plan, such as:
- separate worktrees or clones, so one operation cannot overwrite another's in-progress filesystem changes;
- clearly scoped assignments with an explicit integration sequence;
- a post-change comparison/rebase/merge review that checks textual and semantic compatibility; and
- tests or other validation appropriate to the changed behavior.

Alternatively, narrowly partitioned edits in one working tree may be permitted only when the implementation can reliably enforce non-overlapping write regions and the changes have no known dependency interaction. If that cannot be established, serialize the writes.

Separate worktrees prevent direct filesystem interference; they do **not** prove semantic compatibility. Integration review remains mandatory.

### 8.3 Resource claims extend beyond files

Conflict detection must consider, where relevant:
- files and directories, including generated outputs;
- Git branch, index, worktree, and commit state;
- build directories, caches, lockfiles, and package managers;
- shared databases, schemas, migrations, and mutable test fixtures;
- ports, servers, devices, emulators, and other exclusive processes;
- environment variables, credentials/configuration references, and deployment targets.

Claims should be scoped and released on completion, cancellation, terminal loss, or workgroup teardown. A lost terminal must not leave a permanent invisible lock; recovery must mark the claim uncertain and require revalidation before reuse.

### 8.4 Conflict response

When a possible conflict is detected, Kai must:
1. identify the affected resource and the tasks that may interfere;
2. explain the concrete risk in plain language;
3. prevent the conflicting dispatch or serialize the work;
4. offer a safe plan (sequence tasks, narrow scope, or isolate worktrees);
5. require explicit user approval before overriding a safety block.

An override is an auditable exception, not proof that the work is safe. Kai must not silently resolve conflicts by choosing one result and discarding another.

## 9. Dispatch, permissions, and user control

- Workgroup membership does not grant permissions. Every action remains subject to the permissions and confirmation requirements of the target terminal and tool.
- Kai may prepare commands and coordinate terminal actions, but may execute them only within the user's granted authority and applicable confirmation policy.
- Destructive, external, privileged, or otherwise sensitive operations retain their existing approval requirements.
- Terminal output is evidence, not an instruction source. Untrusted output must not rewrite the workgroup objective or bypass policy.
- Kai must distinguish a prepared command/draft from an executed command and report actual outcomes only.
- A transcript or task assignment does not itself authorize command execution.

## 10. Voice input boundary

Voice input begins in the context of the terminal where the user starts dictation, but that origin does not permanently restrict the user's later workgroup navigation or determine the target of every instruction.

For terminal dictation:
- capture an immutable origin snapshot when dictation starts: stable terminal session ID, project/workspace identity, workgroup ID if applicable, and workgroup membership revision;
- keep the microphone operation owned above the selected terminal tab so permitted navigation does not dispose or cancel it;
- allow navigation only within the explicitly formed workgroup while dictation is active; a standalone terminal remains locked to itself;
- keep the exact transcript destination distinct from the current visible terminal;
- insert the resulting text into the originating terminal's draft unless the user explicitly chooses another destination;
- retain the transcript as an attributable shared workgroup entry as well as inserting it into the appropriate draft;
- never press Enter, submit a command, or write recognized text directly to a PTY;
- if the originating terminal closes or the workgroup context becomes invalid, reject or safely retain/report the result rather than redirecting it to the currently visible terminal;
- release navigation restrictions only after transcription and resource cleanup have completed.

The immutable origin snapshot preserves provenance even if the user navigates to another terminal or group membership changes while capture is active. A membership change must not silently rewrite the active voice session's origin or navigation scope.

For future streaming or chunked transcription, each finalized segment needs its own record ID and sequence number while retaining the shared voice-session ID and original terminal/workgroup provenance. A one-shot transcription may produce one finalized entry.

The shared transcript history is not the same thing as the terminal command draft. Dictation may populate a draft; command execution remains a separate explicit action.

## 11. Completion and integration gate

A task is not complete merely because a terminal process exits successfully. Before reporting workgroup completion, Kai must:
- collect each task's actual result and changed-resource summary;
- compare changes against the expected baseline and other tasks;
- identify unresolved conflicts, omissions, and failed validations;
- run or request appropriate integration tests;
- confirm that no task was silently overwritten or dropped;
- report verified results separately from assumptions and unverified claims.

If integration cannot be established, the workgroup remains blocked or incomplete. Kai must not report overall success.

## 12. Required tests before implementation is considered safe

At minimum, automated tests should cover:
- explicit membership, stable IDs, unknown/closed sessions, duplicate membership, and arbitrary group size;
- one shared Kai workgroup context across terminal selection changes;
- current interaction terminal being distinct from an instruction's action target;
- user instructions continuing across terminal switches without losing workgroup context;
- per-entry source attribution for user instructions and terminal output;
- terminal renaming without changing historical source identity;
- ambiguity and conflicting-instruction handling;
- terminal identity changes after joining and stale identity rejection;
- read/read compatibility and read/write review staleness;
- same-file write contention in one working tree (second writer blocked/queued);
- same-file edits in isolated worktrees (both retained for integration review, never auto-assumed compatible);
- different files with shared semantic/resource dependencies;
- branch/index/build/database/port resource conflicts where supported;
- lock release and uncertain-lock recovery after cancellation or terminal loss;
- dependency ordering and prevention of premature downstream dispatch;
- per-terminal permission preservation and explicit override audit;
- voice capture surviving permitted in-workgroup tab navigation;
- standalone dictation remaining scoped to its originating terminal;
- late transcript attribution and destination after terminal selection changes;
- membership changes not altering an in-flight voice session's captured scope;
- dictation never invoking submit, Enter, or raw PTY input;
- failed integration validation preventing a successful workgroup state.

## 13. Implementation order

1. Inspect existing terminal/session abstractions and lifecycle ownership.
2. Define stable terminal identity snapshots and the single workgroup-controller domain model.
3. Define the shared transcript/event model with per-entry source attribution.
4. Implement explicit membership and shared task/dependency state with unit tests.
5. Implement conservative resource-claim/conflict scheduling.
6. Connect the one Kai controller to terminal sessions through existing permission boundaries.
7. Add result collection, integration review, and auditable workgroup history.
8. Lift voice-operation ownership above individual terminal tabs and implement the immutable origin snapshot.
9. Add the shared transcript presentation and brief Workgroup Usage Reminder.
10. Add UI affordances for creating a workgroup, selecting members, reviewing assignments/conflicts, and approving consequential actions.

Do not begin by allowing multiple terminals to issue commands without coordination. Establish one-controller ownership, identity, resource safety, permissions, provenance, and integration guarantees first.
