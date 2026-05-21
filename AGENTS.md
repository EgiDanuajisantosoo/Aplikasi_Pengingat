# AGENTS.md - PengingatSholat (Prayer Reminder App)

## Project Overview
Android application that displays Islamic prayer times and sends reminders at scheduled times using device location. Built with Kotlin, Jetpack Compose (partial), Room database, and Retrofit API integration.

## Core Architecture

### Data Layer (`app/src/main/java/.../data/`)
- **AppDatabase**: Singleton Room database with 3 entities
  - `PrayerScheduleEntity`: Daily prayer times (subuh, dzuhur, ashar, maghrib, isya)
  - `CityEntity`: Supported cities from API
  - `PrayerLogEntity`: User prayer completion logs (for streak calculation)
  - Uses `fallbackToDestructiveMigration()` for dev - increment `version` when schema changes
- **DAOs**: `PrayerDao`, `CityDao` access different entities
- **Repositories**: `PrayerRepository`, `CityRepository` handle API + DB coordination
- **ApiClient**: Retrofit instance for prayer times API (uses city ID, year/month/day parameters)

### Key Data Flow
1. **MainActivity loads prayer schedule**:
   - Request location permission
   - Get device location → reverse geocode to city name
   - Fallback to Jakarta (cityId: "1301") if location fails
   - Fetch prayer times via API → save to Room → display in RecyclerView
   - Calculate prayer streak from `PrayerLogEntity.getPerfectDays()`

2. **Notification & Alarm Pipeline**:
   - `PrayerAlarmScheduler.schedule()` → sets `AlarmManager` with exact-and-allow-idle
   - Alarm fires → `PrayerAlarmReceiver` → `NotificationHelper.showPrayerNotification()`
   - User taps "Sudah" (Done) or "Nanti" (Later) → `PrayerActionReceiver` handles action
   - Request codes use `.hashCode()` of (date+prayerName) to avoid duplicates

3. **Bootstrap**:
   - `BootReceiver` (exported, listens to BOOT_COMPLETED) → reschedules alarms on device restart

## Critical Patterns

### Permission Handling
- Sequential chain: Notification (100) → Location (101)
- Check `Build.VERSION.SDK_INT >= 33` for POST_NOTIFICATIONS
- Location permissions use `ActivityCompat.requestPermissions()`

### Room Database Lifecycle
- Create in `MainActivity`: `AppDatabase.getInstance(this)`
- Always use suspend functions in `lifecycleScope.launch(Dispatchers.IO)`
- Clear example: `MainActivity.setupTodaySchedule()` lines 69-133

### Location & Geocoding
- `LocationHelper`: Wraps Google Play Services Location for last known location
- `GeocoderUtil.getCityName()`: Reverse geocode Location → city name string
- **Fallback pattern**: Try location → try city name lookup → defaults to Jakarta

### PendingIntent IDs
- Generate unique request codes: `(prayerName + context).hashCode()` or `(date + prayerName).hashCode()`
- Use `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` consistently
- See: `NotificationHelper` lines 36-40, `PrayerAlarmScheduler` lines 25-31

### Coroutine Scoping
- Use only `lifecycleScope` in Activities
- Launch `Dispatchers.IO` for database/API work
- Switch to `Dispatchers.Main` via `withContext()` before UI updates
- Pattern: `lifecycleScope.launch(Dispatchers.IO) { ... withContext(Dispatchers.Main) { } }`

## Component Breakdown

| Component | Purpose | Key Files |
|-----------|---------|-----------|
| **UI Layer** | RecyclerView adapter + Compose theme | `MainActivity`, `ui/adapter/PrayerAdapter` |
| **Business Logic** | Prayer repo, city repo | `data/repository/` |
| **Database** | Room entities & DAOs | `data/local/entity/`, `data/local/dao/` |
| **Scheduling** | AlarmManager wrapper | `scheduler/PrayerAlarmScheduler.kt` |
| **Receivers** | Broadcast event handlers | `receiver/{PrayerAlarmReceiver,BootReceiver,PrayerActionReceiver}` |
| **Notifications** | Channel creation + actions | `notification/NotificationHelper.kt` |
| **Location** | GPS + geocoding | `location/{LocationHelper,GeocoderUtil}` |

## Build & Dependencies

- **Kotlin**: 2.0.21
- **Compose**: 2024.09.00 (partial UI, RecyclerView still active)
- **Room**: 2.6.1 (KSP compiler `androidx.room.compiler`)
- **Retrofit**: 2.9.0 + OkHttp 4.12.0 logging interceptor
- **Google Play Services**: Location 21.0.1
- **Material**: Material3 composition, Material Design Classic for material (R.drawable icons)

### Build Commands
- Clean: `./gradlew clean`
- Build debug: `./gradlew assembleDebug`
- Run: `./gradlew installDebug` (device required)
- Database version: Increment `AppDatabase.version` and clear app data during development

## Important Notes for AI Agents

1. **Mixed UI Stack**: MainActivity uses RecyclerView + some Compose components. Be cautious when adding UI features.
2. **Prayer Times Source**: API returns prayer times in format (subuh, dzuhur, ashar, maghrib, isya) - always validate count.
3. **Timezone Awareness**: All dates use `LocalDate.now().toString()` (YYYY-MM-DD format) - important for international day boundaries.
4. **Database Migrations**: `fallbackToDestructiveMigration()` is only for development. For production, write proper migration blocks.
5. **Location Sensitivity**: App may crash if location provider is unavailable. Always use try-catch around `LocationHelper`.
6. **Notification Permissions**: Always check permission before `NotificationManagerCompat.notify()` - silent fail on denied.
7. **Request Code Collision**: Ensure `.hashCode()` values are unique across alarm contexts - use full (date+prayerName) tuple.

## File Reference Guide

- `app/build.gradle.kts` - Dependencies, Compose feature flags
- `gradle/libs.versions.toml` - Centralized version management
- `app/src/main/AndroidManifest.xml` - Permissions, receivers, activities
- `app/src/main/java/com/egidanuajisantoso/pengingatsholat/MainActivity.kt` - Entry point (study flow here)
- `app/src/main/java/com/egidanuajisantoso/pengingatsholat/data/local/AppDatabase.kt` - Database singleton (critical)
- `app/src/main/res/` - Drawables (ic_mosque, ic_check, ic_later) and layouts

