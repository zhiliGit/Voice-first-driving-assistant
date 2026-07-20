# Voice-First Driving Assistant — Project Postmortem

**Project:** Android/Kotlin voice-first driving assistant  
**Repository:** <https://github.com/zhiliGit/Voice-first-driving-assistant>  
**Project status:** Ended after feasibility prototype  
**Report date:** 2026-07-20

## 1. Executive summary

The project successfully produced a compilable Android/Kotlin/Jetpack Compose prototype, a mock agent-planning flow, a small backend interface, and a repeatable GitHub Actions build that generated APK artifacts. It also demonstrated a confirmation-first safety pattern for actions requested while driving.

The project did not reach a dependable voice-input experience on the selected Huawei environment. The decisive limitation was not the microphone setting or ordinary Android permission handling. The Huawei test environment did not expose a compatible Android `RecognitionService`. Huawei's alternative, ML Kit ASR, requires Huawei-specific SDK integration, cloud connectivity, AppGallery configuration, authentication, and careful treatment of client credentials. At that point, the project had moved beyond a small cross-platform prototype into vendor-specific product engineering.

The main lesson is that the riskiest platform capability—speech recognition on a Huawei device without Google Mobile Services—should have been validated before building the application around it.

## 2. Original objective and delivered scope

The intended product was a voice-first Android driving assistant that could:

1. Receive a spoken driver request.
2. Send the request to GPT or another agent model.
3. Produce a structured action plan.
4. Ask the driver for confirmation.
5. Execute an approved action such as creating a note or reminder.
6. Eventually connect to an in-car environment.

The prototype delivered:

- Kotlin and Jetpack Compose application structure.
- Typed request input.
- Local deterministic mock mode.
- Structured action-plan parsing.
- Confirm/cancel interaction.
- Backend URL configuration without placing an OpenAI key in the Android source.
- Unit tests for action-plan parsing.
- Gradle wrapper and GitHub Actions CI.
- Successful debug APK generation.
- A dependency check intended to avoid accidental Google Play Services coupling.
- Initial Huawei device identification and speech-input experiments.

It did not deliver:

- Reliable Huawei speech recognition.
- A deployed production backend.
- Real note/reminder execution.
- Continuous voice dialogue or text-to-speech.
- Android Auto or Android Automotive integration.
- Driving-mode safety validation.
- Signed release APK/AAB or AppGallery submission.

## 3. Problems encountered and root-cause analysis

### 3.1 Scope expanded before the core risk was validated

The initial idea combined several independent projects: Android UI development, speech recognition, agent orchestration, backend deployment, device actions, Huawei compatibility, and car integration. Each component was feasible individually, but their combination created a large dependency chain.

**Root cause:** The project was scoped by features rather than by technical risks. Development started with repository structure, UI, descriptions, and build automation before proving that voice input worked on the target Huawei system.

**Lesson:** For a new platform project, rank unknowns by the probability and impact of failure. Test the highest-risk external dependency first.

### 3.2 “Huawei compatibility” was initially defined too loosely

The first interpretation was “an Android APK with no Google-only dependencies.” That is necessary but not sufficient. A Huawei phone may run an Android-compatible application while differing in system services, HMS availability, EMUI/HarmonyOS behavior, AppGallery requirements, background restrictions, and speech-provider implementation.

**Root cause:** Hardware compatibility, operating-system compatibility, service compatibility, and store compatibility were treated as one requirement.

**Lesson:** Define compatibility as a test matrix:

| Layer | Required evidence |
| --- | --- |
| APK installation | Installs and launches on named Huawei models |
| UI/runtime | Compose UI and lifecycle work correctly |
| Permissions | Microphone and network permissions behave correctly |
| Speech service | A supported recognizer returns text |
| HMS integration | Required SDK, region, authentication, and service version work |
| Distribution | Signing and AppGallery checks pass |

### 3.3 The first speech implementation checked the wrong integration surface

The first voice version relied on `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and searched for an external activity. The Huawei environment did not expose such an activity, producing the message that no speech-recognition service was installed.

**Root cause:** The implementation assumed that a voice-input setting implied the presence of a launchable recognition activity. Keyboard voice input, Huawei AI Voice, and Android speech-recognition APIs are distinct integration surfaces.

**Corrective attempt:** The application was changed to use Android's direct `SpeechRecognizer` API and the manifest query was corrected to `android.speech.RecognitionService`.

### 3.4 Direct Android `SpeechRecognizer` also failed on the target environment

Even after using the correct API and manifest visibility, `SpeechRecognizer.isRecognitionAvailable()` reported that no compatible service was available.

**Root cause:** The target Huawei system did not register its voice capability as a standard Android `RecognitionService`. Correct user settings could not create an API service that the firmware did not expose.

**Lesson:** Device settings are not proof of SDK-level availability. Capability detection must be tested on the actual target device or remote real device before architecture decisions are finalized.

### 3.5 Huawei ML Kit ASR introduced vendor lock-in and credential requirements

Huawei ML Kit ASR was identified as the native fallback. It supports relevant languages, but it is cloud-backed, requires HMS SDK dependencies, AppGallery Connect setup, API authentication, network access, compatible regions/devices, and a lit screen.

**Root cause:** The original architecture assumed speech recognition was a local operating-system capability. On the target system it became an authenticated vendor cloud service.

**Security issue:** Injecting a credential into an APK does not make it secret. Mobile applications can be reverse-engineered. Any client-side identifier or API key must be designed for public-client use and restricted by package name, signing certificate, service permissions, quotas, and monitoring. Sensitive server credentials must remain on a backend.

**Lesson:** Decide early whether speech is provided by the OS, a vendor SDK, or the application's backend. Do not defer this decision until UI integration.

### 3.6 GitHub Actions exposed build-system issues incrementally

The first CI build failed because Kotlin 2.4 rejects the old `kotlinOptions.jvmTarget` syntax. After migration to `compilerOptions`, the JVM unit test failed because Android's stub `org.json` implementation throws outside an Android runtime. Adding a JVM JSON implementation fixed the test.

**Root causes:**

- Dependencies were selected near the latest available versions without first validating migration requirements.
- Android framework classes were used in local JVM tests without recognizing that some classes are only stubs there.
- The project was not built locally in a fully provisioned Android environment before the first CI run.

**Lesson:** Pin a known-compatible toolchain, compile the smallest skeleton immediately, and distinguish JVM unit tests from instrumentation/device tests.

### 3.7 CI success was sometimes mistaken for product validation

A green workflow proved compilation, unit tests, dependency checks, and APK packaging. It did not prove that speech recognition, permissions, Huawei services, microphone capture, backend networking, or in-car behavior worked.

**Root cause:** Build verification and runtime validation were not clearly separated in the project milestones.

**Lesson:** Use explicit evidence levels:

- **L1 — Static:** source review and dependency checks.
- **L2 — Build:** compilation, unit tests, APK generation.
- **L3 — Emulator:** UI and generic Android behavior.
- **L4 — Target device:** Huawei/HMS capability validation.
- **L5 — End-to-end:** device, backend, model, and action execution.
- **L6 — Driving safety:** distraction, failure recovery, latency, and automotive-policy validation.

### 3.8 The backend boundary was correct but incomplete

The project correctly kept the OpenAI API key out of the Android application and defined a backend planning endpoint. However, the backend was not deployed and the APK defaulted to mock mode.

**Root cause:** The system was developed as code components rather than as a deployable vertical slice.

**Lesson:** A thin, deployed end-to-end path is more informative than several partially implemented layers. The first real milestone should have been typed text → deployed backend → structured plan → confirmation.

### 3.9 The car-integration objective was premature

Android Auto and Android Automotive impose product categories, templates, permissions, distraction rules, emulator requirements, and review constraints. The prototype was still a normal phone application.

**Root cause:** “Car connection” was treated as a later UI target instead of a separate platform qualification.

**Lesson:** Validate the core assistant on a phone first, then confirm that the intended automotive use case is allowed and supported before adapting the UI.

## 4. What worked well

- Starting with mock mode allowed the UI and confirmation flow to be demonstrated without a backend.
- Keeping the OpenAI credential on the backend was the correct security boundary.
- Structured action plans were safer and easier to test than accepting arbitrary model text.
- Requiring confirmation before state-changing actions was appropriate for a driving assistant.
- GitHub Actions made builds reproducible and quickly exposed configuration and test problems.
- APK artifacts made remote-device testing possible without requiring a local Android build environment.
- Stopping after the key architectural limitation became clear avoided continued investment without a validated path.

## 5. A more efficient development process

### Phase 0 — Define the target precisely

Write one page containing:

- Exact phone models and operating-system versions.
- GMS/HMS availability.
- Supported languages and countries.
- Whether Internet access is required.
- Intended car platform: phone projection, Android Auto, or Android Automotive.
- One primary user action.
- Maximum acceptable response latency.
- Safety and privacy constraints.

Avoid broad phrases such as “supports Huawei” or “connects to cars.”

### Phase 1 — Run capability spikes before app development

Create disposable projects, each lasting no more than one or two days:

1. Record microphone audio on the target Huawei device.
2. Test standard Android `SpeechRecognizer`.
3. Test Huawei ML Kit ASR with a temporary AppGallery application.
4. Measure recognition latency and accuracy for English, German, and Chinese as required.
5. Confirm the intended automotive category and emulator path.

The result should be an evidence table, not production code. Reject unsupported architecture options immediately.

### Phase 2 — Choose the speech architecture

Use this decision order:

1. **Backend transcription:** most portable and keeps provider credentials off the phone; requires audio upload and a network connection.
2. **Huawei ML Kit ASR:** suitable for Huawei-focused distribution; adds HMS/AppGallery coupling and cloud-service requirements.
3. **Android system recognizer:** simplest when guaranteed on managed target devices; unreliable as a universal assumption.
4. **Bundled offline model:** strongest independence and privacy, but increases APK size, CPU use, integration effort, and model-management work.

For a cross-vendor hackathon prototype, backend transcription is usually the most efficient choice.

### Phase 3 — Build one vertical slice

Implement only:

> Press microphone → obtain transcript → send to backend → receive one structured action → confirm/cancel → show simulated completion.

Do not add reminders, navigation, continuous dialogue, Android Auto, multiple agents, databases, or account systems until this slice passes on the target Huawei device.

### Phase 4 — Add automation with separate gates

The CI pipeline should contain:

1. Formatting and static analysis.
2. JVM unit tests for pure Kotlin code.
3. Debug APK build.
4. Dependency/security checks.
5. Emulator instrumentation tests.
6. Optional signed build from protected release credentials.

Target-device tests should be tracked separately because GitHub Actions cannot prove Huawei runtime behavior by compilation alone.

### Phase 5 — Expand only after measurable success

Use exit criteria for each milestone:

- Speech succeeds in at least 9 of 10 controlled trials.
- Median transcript latency is within the defined target.
- Backend errors produce a safe retry or typed fallback.
- No action executes before explicit confirmation.
- No provider secret is committed or embedded contrary to its public-client security model.
- The app passes tests on at least one real target Huawei model.

## 6. Recommended architecture for a future restart

```text
Huawei/Android app
  ├─ microphone recording
  ├─ typed fallback
  ├─ confirmation UI
  └─ HTTPS client
          │
          ▼
Application backend
  ├─ authentication and rate limiting
  ├─ speech-to-text provider
  ├─ agent/model request
  ├─ structured-output validation
  └─ audit-safe action response
          │
          ▼
Android action adapters
  └─ execute only after confirmation
```

This architecture avoids dependence on a device-provided recognition service and keeps model-provider credentials on the server. A vendor SDK can remain an optional optimization rather than a required foundation.

## 7. Practical efficiency checklist

### Before coding

- [ ] Identify the exact target device and OS.
- [ ] List all external services and required credentials.
- [ ] Test the least certain device capability first.
- [ ] Choose one user journey and one action.
- [ ] Define measurable success and stop conditions.

### During implementation

- [ ] Keep domain parsing and planning logic independent of Android framework classes.
- [ ] Pin compatible Gradle, Android Gradle Plugin, Kotlin, and Compose versions.
- [ ] Build and run CI after the smallest skeleton commit.
- [ ] Use mock interfaces, but replace them with one deployed vertical slice early.
- [ ] Test on the real target device after every platform-dependent change.
- [ ] Treat client APK contents as inspectable, not secret.

### Before adding features

- [ ] Confirm that the previous milestone works end-to-end.
- [ ] Measure latency and failure rate.
- [ ] Review platform/store restrictions.
- [ ] Confirm the feature does not increase driver distraction.
- [ ] Update the compatibility matrix and risk register.

## 8. Suggested project artifacts for future apps

Maintain these files from the first day:

- `requirements.md` — explicit functional and non-functional requirements.
- `target-matrix.md` — devices, OS versions, services, languages, and evidence.
- `architecture-decisions.md` — short ADRs explaining major choices.
- `risk-register.md` — probability, impact, owner, experiment, and deadline.
- `test-plan.md` — unit, emulator, target-device, and end-to-end tests.
- `release-checklist.md` — signing, secrets, privacy, store, and rollback steps.
- `project-log.md` — decisions and results, not a chronological transcript of all activity.

## 9. Final lessons

1. Validate the hardest platform dependency before building the surrounding application.
2. “Android compatible” does not imply “Huawei service compatible.”
3. A user-visible voice setting does not guarantee an SDK-accessible recognition service.
4. A successful APK build is necessary but is not runtime or product validation.
5. Vendor SDKs change architecture, authentication, distribution, and maintenance costs.
6. Credentials placed in a mobile binary must be treated as recoverable.
7. A small deployed vertical slice provides more knowledge than a broad collection of mocks.
8. Explicit stop criteria improve efficiency by preventing escalating investment after a core assumption fails.

## 10. Closure decision

Ending the project at this stage is reasonable. The prototype answered its most valuable feasibility question: reliable cross-platform speech input cannot be assumed on the target Huawei environment. Restarting should happen only after selecting and validating a speech architecture—preferably backend transcription or a fully configured Huawei ML Kit proof of concept—and documenting the exact device and automotive targets.
