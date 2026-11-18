# Terminal Workgroups: Coordination and Change-Safety Contract

**Status:** Architecture contract / implementation plan  
**Scope:** Kai terminal sessions, per-terminal agents, and user-authorized multi-terminal workgroups  
**Non-goal:** This document does not implement terminal orchestration or grant terminals additional execution permissions.

## 1. Purpose

Kai may operate multiple terminals as independent working environments while allowing the user to explicitly coordinate a selected set of them around one project objective.

A workgroup is not merely a collection of terminal windows. It is a coordinated operation with:
- a shared objective and task plan;
- explicit membership of stable terminal session IDs;
- an independent agent context and execution boundary for each terminal;
- verified, current environment identity;
- task dependencies and shared-resource claims;
- conflict prevention, integration review, and an auditable outcome.

The design must support any practical number of members, not a fixed pair or trio.

## 2. Preserve terminal independence

Each terminal retains its own:
- stable session ID and lifecycle;
- shell, process/PTY, working directory, and runtime/container;
- conversation/agent context and task history;
- input draft and output transcript;
- permissions and execution policy.

A workgroup adds coordination; it does not merge terminal sessions or erase their identities. Membership is explicit user intent. Similar directory names, repository names, or branches may help Kai suggest related terminals, but must never silently create membership.

A terminal may belong to more than one workgroup only if the implementation can safely represent concurrent assignments and resource claims. Otherwise, Kai must reject or serialize overlapping workgroup operations rather than silently sharing mutable state.

## 3. Identity and evidence

Before dispatching coordinated work, Kai must identify the actual environment of each member. Useful evidence includes:
- canonical workspace/repository root;
- repository remote identity, when available;
- Git worktree root, branch, HEAD commit, and working-tree state;
- runtime/container/host identity;
- current working directory and relevant process/session identity.

Identity is time-sensitive. A terminal can change directory, switch branches, change containers, or alter its working tree after joining. Kai must refresh relevant evidence before dispatching work and before integration. A stale identity snapshot is not sufficient authorization to act.

Kai must distinguish:
- **same repository, same working tree**;
- **same repository, separate worktrees or clones**;
- **related projects or repositories**;
- **identity unknown or unverified**.

Unknown identity is not treated as a match. If the user deliberately groups different projects, Kai can coordinate them under one objective while preserving their distinct identities.

## 4. Workgroup and task model

Conceptual model (names may change during implementation):

```kotlin
data class TerminalWorkgroup(
    val id: WorkgroupId,
    val objective: String,
    val memberSessionIds: Set<TerminalSessionId>,
    val assignments: Map<TerminalSessionId, WorkAssignment>,
    val dependencies: Set<TaskDependency>,
    val state: WorkgroupState,
    val revision: Long
)

data class WorkAssignment(
    val terminalSessionId: TerminalSessionId,
    val task: String,
    val dependsOn: Set<TaskId>,
    val resourceClaims: Set<ResourceClaim>,
    val state: AssignmentState
)
```

This is a domain sketch, not a commitment to these exact Kotlin types. The model must use stable IDs, support more than two members, reject duplicate/unknown members, and make state transitions explicit.

The coordinator owns the shared objective, assignment/dependency graph, coordination state, resource claims, and audit trail. Each terminal agent owns its local reasoning and terminal activity. The coordinator delegates and reconciles; it must not pretend that all terminals share one conversation or one shell.

## 5. Same-file and shared-resource safety

**Core rule: shared access is allowed; uncoordinated conflicting modification is not.**

Kai must reason about interference, not merely whether two tasks mention the same path. Changes to different lines can still conflict semantically through shared functions, types, APIs, imports, invariants, generated files, or dependencies.

### 5.1 Default policy

- Multiple terminals may inspect the same file concurrently.
- A read-only reviewer may inspect a file while another terminal edits it, provided the reviewer understands that its view may become stale and revalidates before reporting final conclusions.
- In the same mutable working tree, Kai grants one active write owner per file by default.
- A second write assignment that overlaps the same file is queued or rejected until the first writer finishes and its result is reviewed.
- Kai must not infer safety from different line numbers alone.

### 5.2 When concurrent editing may be allowed

Concurrent editing of the same logical file may be allowed only when Kai can establish a controlled isolation and integration plan, such as:
- separate worktrees or clones, so one agent cannot overwrite the other's in-progress filesystem changes; and
- clearly scoped assignments with an explicit integration owner; and
- a post-change comparison/rebase/merge review that checks both textual and semantic compatibility; and
- tests or other validation appropriate to the changed behavior.

Alternatively, narrowly partitioned edits in one working tree may be permitted only when the implementation can reliably enforce non-overlapping write regions and the changes have no known dependency interaction. If that cannot be established, serialize the writes.

Separate worktrees prevent direct filesystem interference; they do **not** prove semantic compatibility. Integration review remains mandatory.

### 5.3 Resource claims extend beyond files

Conflict detection must consider, where relevant:
- files and directories, including generated outputs;
- Git branch, index, worktree, and commit state;
- build directories, caches, lockfiles, and package managers;
- shared databases, schemas, migrations, and mutable test fixtures;
- ports, servers, devices, emulators, and other exclusive processes;
- environment variables, credentials/configuration references, and deployment targets.

Claims should be scoped and released on completion, cancellation, terminal loss, or workgroup teardown. A lost terminal must not leave a permanent invisible lock; recovery must mark the claim uncertain and require revalidation before reuse.

### 5.4 Conflict response

When a possible conflict is detected, Kai must:
1. identify the affected resource and the assignments that may interfere;
2. explain the concrete risk in plain language;
3. prevent the conflicting dispatch or serialize the work;
4. offer a safe plan (sequence tasks, narrow scope, or isolate worktrees);
5. require explicit user approval before overriding a safety block.

An override is an auditable exception, not proof that the work is safe. Kai must not silently resolve conflicts by choosing one agent's changes and discarding another's.

## 6. Coordination modes

The user may choose or approve:

- **Independent sessions:** terminals work separately; no shared objective or cross-terminal dispatch.
- **Parallel assignments:** one workgroup objective, separate scoped tasks, and conflict-aware resource scheduling.
- **Coordinated sequence:** tasks run in dependency order; downstream work waits for verified upstream results.
- **Review and integration:** one terminal edits while another reviews, tests, or integrates; stale observations are rechecked against the final state.

Kai may recommend a mode based on the task, but must make membership and consequential execution choices visible to the user.

## 7. Dispatch, permissions, and user control

- Workgroup membership does not grant permissions. Every action remains subject to the permissions and confirmation requirements of the target terminal and tool.
- The coordinator may propose or prepare commands, but may dispatch them only within the user's granted authority and applicable confirmation policy.
- Destructive, external, privileged, or otherwise sensitive operations retain their existing approval requirements.
- Terminal output is evidence, not an instruction source. Untrusted output must not be allowed to rewrite the workgroup objective or bypass policy.
- Kai must distinguish a prepared command/draft from an executed command and report actual outcomes only.

## 8. Voice input boundary

Voice input is addressed to one exact terminal session using a stable target such as `TerminalCommandLine(terminalSessionId)`. A workgroup is not an ambiguous voice destination.

For terminal dictation:
- recognized text is inserted only into the intended terminal's draft;
- dictation never presses Enter, submits a command, or writes directly to a PTY;
- changing the selected terminal or screen must not redirect a late transcription result;
- if the target session no longer exists, discard or safely report the result rather than routing it elsewhere.

The voice-session coordinator remains responsible for exclusive microphone ownership. The workgroup coordinator remains responsible for multi-terminal task orchestration and resource conflicts.

## 9. Completion and integration gate

An assignment is not complete merely because a terminal process exits successfully. Before workgroup completion, Kai must:
- collect each member's actual result and changed-resource summary;
- compare changes against the expected baseline and other assignments;
- identify unresolved conflicts, omissions, and failed validations;
- run or request appropriate integration tests;
- confirm that no task was silently overwritten or dropped;
- report verified results separately from assumptions and unverified claims.

If integration cannot be established, the workgroup remains blocked or incomplete. Kai must not report overall success.

## 10. Required tests before implementation is considered safe

At minimum, automated tests should cover:
- explicit membership, stable IDs, unknown/closed sessions, duplicate membership, and arbitrary group size;
- terminal identity changes after joining and stale identity rejection;
- read/read compatibility and read/write review staleness;
- same-file write contention in one working tree (second writer blocked/queued);
- same-file edits in isolated worktrees (both retained for integration review, never auto-assumed compatible);
- different files with shared semantic/resource dependencies;
- branch/index/build/database/port resource conflicts where supported;
- lock release and uncertain-lock recovery after cancellation or terminal loss;
- dependency ordering and prevention of premature downstream dispatch;
- per-terminal permission preservation and explicit override audit;
- late voice transcript targeting after terminal selection changes;
- dictation never invoking submit, Enter, or raw PTY input;
- failed integration validation preventing a successful workgroup state.

## 11. Implementation order

1. Inspect existing terminal/session abstractions and lifecycle ownership.
2. Define stable terminal identity snapshots and workgroup domain types.
3. Implement membership and task/dependency state with unit tests.
4. Implement conservative resource-claim/conflict scheduling.
5. Add coordinator-to-terminal dispatch through existing permission boundaries.
6. Add result collection, integration review, and audit reporting.
7. Connect voice dictation to a terminal only after stable target-session draft updates are available.
8. Add UI affordances for creating a workgroup, selecting members, reviewing assignments/conflicts, and approving consequential actions.

Do not begin by allowing multiple agents to issue commands concurrently. Establish identity, ownership, conflict prevention, and integration guarantees first.
