# CLAUDE.md - QuestFlow Gamification App

Project-specific guidance for the QuestFlow RPG task management app.

## 🎮 App Overview

**QuestFlow** transforms daily tasks into an RPG experience with XP, levels, skills, and rewards.

### Current Version Info
- **Database Version**: 5
- **Last Update**: XP formula changed to total-based (not difference)
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
- `MemeCollectionViewModel` - Meme rewards
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
- ✅ Meme collection system (50 memes)
- ✅ XpLevelBadge in all screen TopBars
- ✅ Calendar integration with XP claiming
- ✅ German/English mixed UI
- ✅ Skill tree with XP multipliers

### Database Migrations
- v1→v2: Added gamification tables
- v2→v3: Added xpPercentage to tasks
- v3→v4: Added xpPercentage to calendar_links
- v4→v5: Added meme unlock fields

## 🐛 Known Issues & Fixes

### Applied Fixes
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

### Debug Points
- XP calculations logged in `RecordCalendarXpUseCase`
- Level progressions in `GrantXpUseCase`
- Animation sizing in `XpBurstAnimation`

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