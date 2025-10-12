# CLAUDE.md - QuestFlow Gamification App

Project-specific guidance for the QuestFlow RPG task management app.

## 🎮 App Overview

**QuestFlow** transforms daily tasks into an RPG experience with XP, levels, skills, and rewards.

### Current Version Info
- **Database Version**: 9
- **Last Update**: Code optimization and cleanup (Jan 2025)
- **Status**: Fully functional with animations
- **Git Repository**: Yes (initialized in questflow directory)

## 🔑 Core Systems

### XP Formula (UPDATED)
```kotlin
// NEW: Based on TOTAL XP for next level (motivational scaling)
val nextLevelTotal = (currentLevel + 1)² × 100
val xpReward = (nextLevelTotal * percentage / 100)

// Example Level 26→27:
// Total: 72,900 XP
// Trivial (20%): 14,580 XP
// Episch (100%): 72,900 XP
```

### Difficulty Levels
- **Trivial**: 20% (Trivial)
- **Einfach**: 40% (Easy)
- **Mittel**: 60% (Medium)
- **Schwer**: 80% (Hard)
- **Episch**: 100% (Epic)

## 📂 Key Files & Components

### ViewModels
- `TodayViewModel` - Main task management
- `CalendarXpViewModel` - Calendar XP claims
- `CollectionViewModel` - Collection item display
- `CollectionManageViewModel` - Collection item uploads
- `SkillTreeViewModel` - Skill progression

### Important Use Cases
- `CalculateXpRewardUseCase` - XP calculation (TOTAL-based)
- `CompleteTaskUseCase` - Task completion flow
- `GrantXpUseCase` - XP granting with multipliers
- `RecordCalendarXpUseCase` - Calendar event XP

### UI Components
- `XpLevelBadge` - Shows in all TopBars
- `XpBurstAnimation` - Dynamic sizing for large numbers
- `TaskDialog` - Scrollable with difficulty selection

## 🎨 Recent Implementations

### Completed Features
- ✅ XP animations with dynamic text sizing
- ✅ User-uploadable Collection system (replaced 50 predefined memes)
- ✅ Category-specific collections with file storage
- ✅ XpLevelBadge in all screen TopBars
- ✅ Calendar integration with XP claiming
- ✅ German/English mixed UI
- ✅ Skill tree with XP multipliers

### Database Migrations
- v1→v2: Added gamification tables
- v2→v3: Added xpPercentage to tasks
- v3→v4: Added xpPercentage to calendar_links
- v4→v5: Added meme unlock fields (legacy)
- v5→v6: Category system
- v6→v7: Category XP tracking
- v7→v8: Skill tree prerequisites
- v8→v9: Calendar link improvements
- v9→v10: Collection system with file storage

## 🔄 Recent Refactorings (Jan 2025)

### Code Cleanup Completed
- ✅ Removed duplicate `deleteCalendarEvent` method
- ✅ Removed debug logging from production code (40+ logs cleaned)
- ✅ Removed unused `TaskDifficulty` enum
- ✅ Centralized task update logic via `UpdateTaskWithCalendarUseCase`

### Naming Refactoring (Oct 2025)
- ✅ Renamed `CalendarFilterPreferences` → `TaskFilterPreferences`
- ✅ Renamed `CalendarFilterSettings` → `TaskFilterSettings`
- ✅ Removed obsolete `CalendarXpUiState` typealias
- ✅ Updated SharedPreferences keys for consistency

## 🧪 Debug Test Data System

### Overview
The app includes a comprehensive test data initialization system for development and testing.
**File**: `DebugDataInitializer.kt`

### Automatic Initialization
- Triggers on first app install (checks for Debug category existence)
- All initialization runs in a single database transaction for consistency
- **Robust**: Will re-initialize if app data is cleared

### Test Data Contents
**Generated on every fresh install:**
- **2 Contacts** (Fabian Test 1 & 2) with complete metadata:
  - Phone: +4915159031829
  - Email: fabian_beckmann@outlook.de
  - Address: Baumgarten 42, 27654 Bremerhaven
- **3 Locations**: Bremerhaven (home), Berlin (office), München (branch)
- **5 Text Templates**: Meeting invites, reminders, status updates, phone notes, project kickoffs
- **5 Global Tags**: VIP, Dringend, Privat, Arbeit, Büro
- **39 Comprehensive Tasks**:
  - 13 completed historical tasks (-30 to -1 days)
  - 2 current tasks (today)
  - 24 future tasks (+1 to +30 days)
  - All dates relative to `LocalDateTime.now()` (stays relevant for 3+ months)
  - Mix of priorities (URGENT, HIGH, MEDIUM, LOW)
  - Various XP percentages (20%, 40%, 60%, 80%, 100%)
  - Parent-child task relationships demonstrated
  - Recurring tasks with all trigger modes (FIXED_INTERVAL, AFTER_COMPLETION, AFTER_EXPIRY)
- **39 Calendar Links**: One for each task with a dueDate
  - Status: CLAIMED (completed), EXPIRED (past incomplete), PENDING (future)
  - Enables XP claiming system testing
- **XP Transactions**: Generated for all completed tasks (enables statistics)
- **4 Skills**: Global XP boost, Task master, Streak guardian, Debug specialist
- **User Stats**: Level 5 with 15,000 XP and 12 skill points

### Time-Relative Data
All task dates use `LocalDateTime.now()` as the reference point:
- **Past**: `now.minusDays(30)` to `now.minusDays(1)` — completed tasks for statistics
- **Present**: `now.withHour(X)` — current day tasks
- **Future**: `now.plusDays(1)` to `now.plusDays(30)` — upcoming tasks

This ensures test data remains relevant even months after implementation.

### Usage for Statistics
The test data is specifically designed to enable testing of:
- **XP trend charts**: 13 completed tasks with XP transactions over 30 days
- **Task completion rates**: Mix of completed and active tasks
- **Calendar integration**: All tasks have calendar links for XP claiming
- **Category analytics**: All tasks assigned to Debug category
- **Time distribution**: Tasks spread evenly across 60-day span

### Persistence Guarantee
Test data will ALWAYS be recreated on fresh installs because:
1. `shouldInitialize()` checks for Debug category existence
2. Category is only created after successful full initialization
3. Database transaction ensures atomic creation (all-or-nothing)
4. App data clear (`adb shell pm clear`) triggers re-initialization

## ⚠️ Known Technical Debt

### Performance
- SyncManager runs every 60s (consider 5-15 min)
- Large screens need component extraction (CalendarXpScreen 989 lines)

### Code Organization
- Task dialogs could be consolidated
- Recurring tasks backend disabled (UI ready)
- Some entity fields marked for deprecation

## 🐛 Previously Applied Fixes

### Historical Fixes
- WorkManager initialization conflict resolved
- Priority enum mapping (HARD→HIGH)
- Dialog scrollability implemented
- XP formula changed to total-based

## 🚀 Development Notes

### Testing Checklist
1. Build with `./gradlew.bat assembleDebug`
2. Install with `adb install -r [apk]`
3. Check logs with `adb logcat | grep -i questflow`
4. Test XP calculations at different levels

### Important Use Cases
- `UpdateTaskWithCalendarUseCase` - Centralized task updates with calendar sync
- `CalculateXpRewardUseCase` - XP calculations based on level and percentage
- `GrantXpUseCase` - XP granting with skill multipliers
- `RecordCalendarXpUseCase` - Calendar event XP claiming

## 👤 User Preferences

### Fabian's Preferences
- **XP Numbers**: Likes seeing big, growing numbers (motivational)
- **Language**: German UI text, English technical terms
- **Testing**: Always wants APK installed after changes
- **Debugging**: Prefers detailed ADB log analysis

### Design Decisions
- XP scales exponentially (not linear)
- Animations adapt to number size
- German difficulty names kept
- Thousands separators in XP display

## 📝 Pending Features
1. Streak system implementation
2. Settings screen
3. Subtask support
4. Achievement badges
5. Statistics dashboard

## ⚡ CRITICAL: Development Workflow

### 🔄 Mandatory Workflow for EVERY Task

When receiving a new task/feature request (not a fix for previous task):

1. **Git Commit Previous Work**
   - Automatically commit and push if new feature request
   - Assumption: Previous task was completed successfully

2. **Task Analysis Phase** (ULTRATHINKING)
   - Deeply analyze the request
   - Cross-reference with existing project structure
   - Check Android device logs for last app execution
   - Consider ALL potential impacts and side effects

3. **Planning Phase**
   - Create comprehensive implementation plan
   - Identify potential chain reactions in other parts
   - Plan strategic debug points (not redundant)
   - Consider preventive measures for issues

4. **Implementation Phase**
   - Execute plan without breaking existing features
   - Preserve all working functionality unless explicitly replacing
   - Add intelligent debug logs at critical points
   - Maintain code integrity and patterns

5. **Pre-Deployment Review**
   - Double-check implementation matches requirements
   - Verify no unintended side effects
   - Ensure debug strategy is complete
   - Final code review for issues

6. **Deployment Phase**
   - Build APK with `./gradlew.bat assembleDebug`
   - Install on device with `adb install -r [apk]`
   - Monitor logs for immediate feedback

### 🎯 Workflow Rules

- **NO USER INPUT REQUESTS**: Complete entire workflow in one response
- **ULTRATHINKING**: Take time to think deeply, quality over speed
- **FOCUS**: Stay strictly on task, no unnecessary additions
- **SINGLE RESPONSE**: Only contact user when task is 100% complete
- **GIT DISCIPLINE**: Commit/push at start of new features automatically

### 🧠 Implementation Philosophy

- **Quality First**: Use as many tokens as needed for best result
- **Deep Analysis**: Consider every angle and impact
- **Strategic Debugging**: Smart placement of debug logs
- **Preserve Stability**: Never break working features
- **User Trust**: Complete tasks fully before reporting back