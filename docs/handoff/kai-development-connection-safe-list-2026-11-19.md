# Kai Development — Safe Reconnection List

**Prepared:** 2026-11-19  
**Purpose:** A reusable, non-secret checklist to restore the Kai development context in a future session.

> This is a connection map, not a credential vault. It deliberately contains no passwords, access tokens, private keys, recovery codes, browser device codes, pairing codes, or session cookies. Re-authorize through the official app/device flow when required. Never paste secrets into a chat or commit them to Git.

## 1. Project identity

| Item | Known value |
|---|---|
| Project | Kai Android offline speech input |
| GitHub repository | `ElPetros/Kai` |
| Repository URL | https://github.com/ElPetros/Kai |
| Clone URL | https://github.com/ElPetros/Kai.git |
| GitHub account | `ElPetros` |
| Default branch | `main` |
| Visibility | Public |
| Latest VAD validation branch | `fix/speech-vad-60s-hardening` |
| VAD validation commit | `d816db8f51216e404608a151748c5c9d7de29235` |
| Preserved validation branch | `checkpoint/vad-device-validation-2026-11-19` |
| Handoff package branch | `handoff/offline-speech-preview-2026-11-19` |
| Earlier feature branch | `feature/offline-speech-input` |
| PR #4 | Keep Draft/open/unmerged pending maintainer review and EL's approval |
| Active Codespace previously observed | `crispy doodle` |
| Codespace repo/branch previously observed | `ElPetros/Kai` / `feature/offline-speech-input` |
| Codespace size previously observed | 2 cores / 8 GB RAM / 32 GB storage |

Codespace details are historical observations, not a guarantee it is still running or on that branch. Verify before using it.

## 2. Reconnect to GitHub

### Browser
1. Open https://github.com/ and sign in to the `ElPetros` account.
2. Open https://github.com/ElPetros/Kai.
3. Check the current branch, PRs, Actions, and Codespaces before making changes.

### GitHub CLI (gh)
From an authorized shell:

```sh
gh auth status
gh repo view ElPetros/Kai
gh pr list --repo ElPetros/Kai
gh run list --repo ElPetros/Kai --limit 10
```

If Codespaces commands report missing scope, reauthorize using GitHub's official browser device flow and request the Codespaces scope:

```sh
gh auth login --hostname github.com --git-protocol https --web --scopes codespace
```

Complete the one-time authorization only on the official GitHub device-login page. The code is temporary and must never be saved in this document, a repository, or a chat. A previous attempt failed because the phone environment could not reach GitHub; retry only with working connectivity.

Useful checks after authorization:

```sh
gh codespace list
gh codespace view
```

If the active Codespace is listed, use the current GitHub UI/CLI connection rather than assuming an old session is still valid.

## 3. GitHub Codespaces

Previously observed workspace:
- Name: `crispy doodle`
- Repository: `ElPetros/Kai`
- Branch at observation: `feature/offline-speech-input`
- Machine: 2-core / 8-GB / 32-GB.

Reopen it from the repository's **Code → Codespaces** menu or the Codespaces page after authenticating. Then verify:

```sh
pwd
git status -sb
git remote -v
git branch --show-current
git log -1 --oneline
```

Expected remote is the Kai repository above. Do not assume the Codespace is clean or on the desired branch; inspect first. A prior connection could see the Codespace record but could not operate its terminal directly, so browser/VS Code attachment may be needed.

## 4. Samsung Galaxy A36 + wireless ADB

Device identity:
- Samsung Galaxy A36
- Model: `SM-S366V`
- Android/ADB device codename previously shown: `a36xq`

Wireless debugging IP and ports can change. A previously used address/port is stale and intentionally omitted.

### Termux ADB server

The user's Termux ADB server uses port **5038** because port 5037 was in conflict.

1. On the phone, enable Developer options → Wireless debugging.
2. On the Termux device, use the current IP and pairing port shown by Android's **Pair device with pairing code** screen:

```sh
adb -P 5038 pair <current-phone-ip>:<pairing-port>
```

Enter the temporary pairing code only at the prompt. Do not save it.

3. Use the separate current connection IP/port shown on the Wireless debugging main screen:

```sh
adb -P 5038 start-server
adb -P 5038 connect <current-phone-ip>:<connection-port>
adb -P 5038 devices -l
```

Expected device identity is similar to `SM_S366V` / `a36xq`. If the list is empty, check that Wireless debugging is enabled and both devices are on a reachable network; refresh the displayed port. Do not reuse an old port.

### Share Termux ADB with the Kai Debian shell

When using the Debian ARM64 environment on the phone, point its ADB client at the Termux server:

```sh
export ADB_SERVER_SOCKET=tcp:127.0.0.1:5038
adb devices -l
```

This was previously confirmed to expose the Samsung A36 from the Debian shell. Reapply the export in a new shell session.

## 5. Termux and local shell notes

- Termux is used for phone-side utilities and the ADB server.
- A previous project path `~/elpetros-chat-sentinel` belongs to the separate Chat Sentinel project, **not Kai**. Do not treat it as the Kai checkout.
- Kai's active development workspace was a GitHub Codespace. A verified, current Kai clone path inside Termux has not been established in this record.
- Debian 12 Bookworm ARM64 was available on the phone for shell tasks; do not assume its current working directory or mounted paths without checking.

## 6. Build and validation reference

Run from the root of a verified Kai checkout:

```sh
./gradlew :androidApp:assembleFossDebug :androidApp:assembleFossRelease :composeApp:desktopTest --no-daemon --stacktrace
```

The command above passed in Actions Run #60 on commit `d816db8f51216e404608a151748c5c9d7de29235`. It is a reference command, not proof that a new local checkout or later commit builds.

Relevant Actions run:
https://github.com/ElPetros/Kai/actions/runs/44472952350

Artifact:
- Name: `kai-android-validation-apks`
- ID: `14505966707`
- SHA-256: `cf099096d345b99788bd4bbfc7c37171ea2012d8d8c7b7b9d27ffda360368d9d`
- Expires: 2026-11-26 06:43:42 UTC

## 7. App identity — do not confuse packages

| App | Package | Role |
|---|---|---|
| Original Kai | `com.inspiredandroid.kai` | Existing app; do not replace during speech-input validation |
| Isolated Speech Test | `com.inspiredandroid.kai.speechtest` | Debug validation package |

Check installed packages before install/uninstall operations. Do not uninstall or overwrite the original Kai app as part of testing the isolated Speech Test build.

## 8. Laptop limitation

The Acer laptop is a limited file-transfer bridge only. It is not the development/build machine. Keep coding, Gradle builds, Git operations, and Android builds in the Codespace or the established phone-side environment, not on that laptop.

## 9. Next-session quick-start checklist

- [ ] Sign in to GitHub as `ElPetros`.
- [ ] Open the Kai repository and verify current branches/PRs.
- [ ] Reauthorize GitHub CLI if `gh auth status` or Codespaces scope checks require it.
- [ ] Reopen and inspect `crispy doodle` or create/open the intended Codespace.
- [ ] Confirm repository, branch, clean/dirty worktree, and latest commit.
- [ ] Enable Android Wireless debugging and pair/connect using the current on-screen IP/ports.
- [ ] Start Termux ADB on port 5038 and verify `adb devices -l`.
- [ ] Export `ADB_SERVER_SOCKET=tcp:127.0.0.1:5038` in Debian if that shell needs the phone.
- [ ] Confirm original Kai and Speech Test package identities before any APK operation.
- [ ] Recheck the handoff branch and PR #4 state before changing anything.

**Security rule:** the safe list restores the map and the steps. Actual authorization remains with the user and the official GitHub/Android prompts.
