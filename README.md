# User Management - KMP "UX Innovator" Challenge

A cross-platform User Management System built with **Kotlin Multiplatform** and **Compose Multiplatform**, interfacing with the [GoRest Public API](https://gorest.co.in).

---

## Setup

### Prerequisites

- Android Studio latest version
- Xcode latest version
- A GoRest API token - register free at [gorest.co.in](https://gorest.co.in)

### Android

1. Clone the repository
2. Add your GoRest token to `local.properties` in the project root:
   ```
   GOREST_TOKEN=your_token_here
   ```
3. Open in Android Studio and run on a device or emulator

### iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Build the project once to generate the KMP framework (`Cmd+B`)
3. The token is currently hardcoded in `composeApp/src/iosMain/kotlin/.../PlatformConfig.ios.kt` for demo purposes - see [Secret Management](#secret-management) below for the production approach
4. Run on a simulator or device

---

## Architecture

The project follows **Clean Architecture** with a strict separation between `data`, `domain`, and `ui` layers, all living inside a single `composeApp` KMP module targeting Android and iOS.

```
composeApp/
└── src/
    ├── commonMain/
    │   ├── data/
    │   │   ├── network/        # Ktor HTTP client, DTOs, API service
    │   │   ├── repository/     # UserRepositoryImpl - offline-first logic
    │   │   └── mapper/         # DTO ↔ Entity ↔ Domain mappers
    │   ├── domain/
    │   │   ├── model/          # User, Gender, UserStatus
    │   │   ├── repository/     # UserRepository interface
    │   │   └── usecase/        # RelativeTimestampUseCase, ValidationUseCase
    │   ├── ui/
    │   │   ├── UserViewModel.kt
    │   │   ├── UserScreen.kt
    │   │   └── components/     # UserCard, ShimmerList, AddUserBottomSheet, etc.
    │   ├── db/                 # SQLDelight schema + expect DatabaseDriverFactory
    │   └── di/                 # Koin modules
    ├── androidMain/            # Android actuals: DatabaseDriverFactory, PlatformConfig
    └── iosMain/                # iOS actuals: DatabaseDriverFactory, PlatformConfig
```

### MVI Pattern

The UI layer follows **MVI (Model-View-Intent)**:

- **Model** - `UserUiState`: a single immutable `data class` representing the entire screen state
- **View** - `UserScreen` and composables: pure functions of state, never mutating it directly
- **Intent** - `UserEvent`: a sealed class of all possible user actions

All state changes flow unidirectionally through `UserViewModel.onEvent()`, which processes events and emits new state via a `MutableStateFlow`. This eliminates state inconsistency bugs and makes the ViewModel trivially testable.

---

## Features

### Smart User Feed
- Fetches the most recently created users from the GoRest API (page 1, which returns newest users first)
- **Shimmer loading states** during initial fetch, implemented with an `infiniteRepeatable` animated gradient. The `uiState` is not in an `isLoading` state for too long so the shimmer won't be visible in most situations - however an artifical delay can be added in `observeUsers()` to show the shimmer on first load.
- **Offline support**: SQLDelight caches users locally; the cached list is served immediately while a background refresh runs
- **Relative timestamps** (e.g. "5 minutes ago", "yesterday") computed entirely in `commonMain` using `kotlinx-datetime` - no platform code required

### Add User Flow
- **Floating Action Button** opens a `ModalBottomSheet` with name, email, and gender fields
- **Real-time validation** on every keystroke: email checked against RFC-5321 regex, name validated for length and character set
- The submit button is disabled until both fields pass validation
- On **201 success**, the new user is inserted into the local SQLDelight cache immediately and appears at the top of the list with a highlight animation
- On **422 error**, GoRest's field-level validation messages (e.g. "email has already been taken") are parsed and surfaced directly in the form

### Destructive Actions with Undo
- **Long-press** a user card to trigger a delete confirmation dialog
- On confirmation, the user is **removed from the local cache immediately** (optimistic delete)
- A **Snackbar with "Undo"** appears - the user has 5 seconds to reverse the action
- If Undo is tapped, the user is restored locally and the API delete is cancelled
- If the Snackbar is dismissed or times out, `DELETE /users/{id}` is called to commit the deletion
- If the ViewModel is cleared while a delete is pending, `onCleared()` commits it immediately

### Adaptive Layout
- Portrait: single `LazyColumn` list
- Landscape / tablet (Expanded `WindowWidthSizeClass`): side-by-side master-detail layout - list on the left, user detail panel on the right
- Driven by `WindowSizeClass` from the `material3-window-size-class-multiplatform` library - no `expect/actual` or platform code needed

---

## Tech Stack

| Concern | Library |
|---|---|
| UI | Compose Multiplatform |
| Architecture | MVI with `StateFlow` |
| Networking | Ktor + kotlinx.serialization |
| Local persistence | SQLDelight |
| Dependency injection | Koin |
| Async | Kotlin Coroutines |
| Date/time | kotlinx-datetime |

---

## `expect/actual` Usage

KMP's `expect/actual` mechanism is used only where genuinely platform-specific behaviour is required - keeping shared code maximised:

| Declaration | Android actual | iOS actual |
|---|---|---|
| `DatabaseDriverFactory` | `AndroidSqliteDriver` | `NativeSqliteDriver` |
| `goRestToken()` | Reads from `BuildConfig.GOREST_TOKEN` | Reads from `Info.plist` (see below) |

Everything else - networking, business logic, validation, timestamp formatting, all UI - lives in `commonMain`.

---

## Secret Management

API tokens are kept out of source control using platform-native mechanisms:

**Android** - `local.properties` (gitignored by default) is read at compile time into `BuildConfig`:

```kotlin
// build.gradle.kts
buildConfigField("String", "GOREST_TOKEN", "\"${localProperties.getProperty("gorest_token", "")}\"")
```

The `androidMain` actual then reads it:
```kotlin
actual fun goRestToken(): String = BuildConfig.GOREST_TOKEN
```

**iOS (production approach)** - The token is injected via an Xcode scheme environment variable, referenced in `Info.plist` as `$(GOREST_TOKEN)`, and read at runtime:

```kotlin
actual fun goRestToken(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("GOREST_TOKEN") as? String ?: ""
```

The Xcode scheme environment variable is stored in `xcuserdata/` which is gitignored, so the token never appears in committed code.

---

## GoRest API Notes

GoRest is a shared public API - all registered users write to the same global dataset. A few behaviours worth noting:

- Created users are scoped to your API token and do not appear in the global `/users` feed
- The global feed returns users in descending ID order, so page 1 always contains the most recently created users
- Newly created users are inserted into the local SQLDelight cache immediately and persist across refreshes, independent of the API feed
- Email addresses must be globally unique across all GoRest users - use a sufficiently unique email when testing

---

## Unit Tests

Tests live in `commonMain/commonTest` and run on both platforms. Coverage includes:

**`UserViewModelTest`** - 15 tests covering:
- Initial loading state
- Add user flow (success and failure paths)
- Real-time validation feedback
- Optimistic delete and undo
- API delete commitment on snackbar dismiss
- Error handling and dismissal
- User selection for master-detail

**`RelativeTimestampUseCaseTest`** - Edge cases: just now, singular/plural minutes/hours/days, invalid input

**`ValidationUseCaseTest`** - Valid and invalid email formats, name length and character constraints, `isFormValid` combinations

Run tests with:
```bash
./gradlew :composeApp:testDebugUnitTest        # Android
./gradlew :composeApp:iosSimulatorArm64Test    # iOS simulator
```

---

## Design Decisions

**Why fetch page 1 instead of the last page?** After investigating the GoRest API, page 1 returns the most recently created users (descending ID order). Fetching the "last page" as stated in the brief would return the oldest users, which is the opposite of what a "Smart User Feed" should show. Page 1 is the correct interpretation of the intent.

**Why keep created users in local cache rather than fetching them from the API?** GoRest scopes created resources to the authenticated token - newly created users do not appear in the global `/users` feed. The offline-first SQLDelight cache is therefore the source of truth for user-created records, which is architecturally sound and mirrors real-world offline-first patterns.

**Why a single module rather than a multi-module build-logic setup?** Convention plugins shine when sharing Gradle configuration across 5+ modules. With a single `composeApp` module, a `build-logic` layer adds boilerplate without real benefit.

---

## AI Assistance
 
This project was built with Claude as an AI coding assistant. In line with the spirit of the challenge — evaluating the ability to direct AI toward production-grade output — here is an honest account of how it was used and where human judgement was applied.
 
### What AI generated
 
- Initial project scaffolding: Gradle files, `libs.versions.toml`, module structure
- Boilerplate-heavy files: SQLDelight schema, Koin modules, `expect/actual` implementations, mappers
- UI components: `ShimmerComponents`, `UserCard`, `AddUserBottomSheet`, `DeleteConfirmDialog`, `UserDetailPanel`, `ErrorState`
- Unit test skeletons for `UserViewModel`, `RelativeTimestampUseCase`, and `ValidationUseCase`
- The `FakeUserRepository` test double
 
### Where human judgement overrode or corrected AI output
 
- **GoRest API behaviour**: AI assumed the last page would contain the newest users. Investigation revealed GoRest returns users in descending ID order, making page 1 the correct fetch target. The AI-suggested last-page approach was replaced entirely.
- **User creation visibility**: AI initially suggested refreshing from the API after creating a user to show it in the list. Testing revealed GoRest scopes created resources to the authenticated token — they never appear in the global feed. The solution (SQLDelight local cache with created users persisting across refreshes) was a deliberate architectural decision made after understanding the API's actual behaviour.
- **Secret management**: AI generated a `BuildConfig`-based solution for Android but the iOS equivalent required understanding of `expect/actual`, `Info.plist`, and Xcode scheme environment variables — applied and verified manually.
- **Status code handling**: The initial AI-generated `UserApiService` only handled the happy path and a few error codes. A full audit identified missing cases (`400`, `404`, `429`, `502`, `503`) and the repeated error mapping logic was refactored into shared private helpers.
- **Koin initialisation crash on iOS**: AI suggested `KoinInitKt.doInitKoin()` but the actual generated Swift-facing symbol was `ComposeAppKoinInitKt.doInitKoin()` — identified by reading the generated framework header directly. Then adding the `ComposeApp` import allows us to use `ComposeAppKoinInitKt` as `KoinInitKt`.
- **`encodeDefaults` omission**: Kotlinx Serialization silently drops fields with default values unless `encodeDefaults = true` is set. This caused a `422` from GoRest ("status: can't be blank") that wasn't immediately obvious — diagnosed and fixed after inspecting the actual serialised request body.

---

## What I'd Do With More Time

- Add pagination to the user list (infinite scroll loading more pages)
- Add user detail editing (PATCH endpoint)
- Expand test coverage to include repository-level tests with a fake SQLDelight driver
- Set up CICD running tests on both Android and iOS
- Set up multi-module with a `build-logic` layer to allow the application to scale
- Add screenshot tests
- Implement proper SQLDelight migrations for schema changes rather than clearing app data