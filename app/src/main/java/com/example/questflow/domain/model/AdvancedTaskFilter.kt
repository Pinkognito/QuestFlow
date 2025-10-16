package com.example.questflow.domain.model

import java.time.LocalDateTime

/**
 * Comprehensive filter system for tasks
 * Supports multiple filter criteria that can be combined
 */
data class AdvancedTaskFilter(
    // === STATUS FILTERS ===
    val statusFilters: StatusFilter = StatusFilter(),

    // === PRIORITY FILTERS ===
    val priorityFilters: PriorityFilter = PriorityFilter(),

    // === CATEGORY FILTERS ===
    val categoryFilters: CategoryFilter = CategoryFilter(),

    // === DATE & TIME FILTERS ===
    val dateFilters: DateFilter = DateFilter(),

    // === XP & DIFFICULTY FILTERS ===
    val xpFilters: XpFilter = XpFilter(),

    // === TAG FILTERS ===
    val tagFilters: TagFilter = TagFilter(),

    // === METADATA FILTERS ===
    val metadataFilters: MetadataFilter = MetadataFilter(),

    // === RECURRING FILTERS ===
    val recurringFilters: RecurringFilter = RecurringFilter(),

    // === RELATIONSHIP FILTERS ===
    val relationshipFilters: RelationshipFilter = RelationshipFilter(),

    // === TEXT SEARCH (from search bar) ===
    val textSearch: String = "",

    // === SORTING ===
    val sortOptions: List<SortOption> = listOf(SortOption.DEFAULT),

    // === GROUPING ===
    val groupBy: GroupByOption = GroupByOption.NONE,

    // === FILTER NAME (for presets) ===
    val presetName: String = ""
) {
    /**
     * Check if any filters are active
     */
    fun isActive(): Boolean {
        return statusFilters.isActive() ||
                priorityFilters.isActive() ||
                categoryFilters.isActive() ||
                dateFilters.isActive() ||
                xpFilters.isActive() ||
                tagFilters.isActive() ||
                metadataFilters.isActive() ||
                recurringFilters.isActive() ||
                relationshipFilters.isActive() ||
                textSearch.isNotBlank() ||
                sortOptions.size > 1 || sortOptions.firstOrNull() != SortOption.DEFAULT ||
                groupBy != GroupByOption.NONE
    }

    /**
     * Get count of active filter sections
     */
    fun getActiveFilterCount(): Int {
        var count = 0
        if (statusFilters.isActive()) count++
        if (priorityFilters.isActive()) count++
        if (categoryFilters.isActive()) count++
        if (dateFilters.isActive()) count++
        if (xpFilters.isActive()) count++
        if (tagFilters.isActive()) count++
        if (metadataFilters.isActive()) count++
        if (recurringFilters.isActive()) count++
        if (relationshipFilters.isActive()) count++
        return count
    }
}

/**
 * Status-based filters
 */
data class StatusFilter(
    val enabled: Boolean = false,
    val showCompleted: Boolean = true,
    val showOpen: Boolean = true,
    val showExpired: Boolean = true,
    val showClaimed: Boolean = true,
    val showUnclaimed: Boolean = true
) {
    fun isActive(): Boolean = enabled && (!showCompleted || !showOpen || !showExpired || !showClaimed || !showUnclaimed)
}

/**
 * Priority-based filters
 */
data class PriorityFilter(
    val enabled: Boolean = false,
    val showUrgent: Boolean = true,
    val showHigh: Boolean = true,
    val showMedium: Boolean = true,
    val showLow: Boolean = true
) {
    fun isActive(): Boolean = enabled && (!showUrgent || !showHigh || !showMedium || !showLow)
}

/**
 * Category-based filters
 */
data class CategoryFilter(
    val enabled: Boolean = false,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val includeUncategorized: Boolean = true,
    val useSelectedCategory: Boolean = false,  // Use the category from dropdown (left top)
    val useAllExceptSelected: Boolean = false  // Use all categories EXCEPT the one from dropdown
) {
    fun isActive(): Boolean = enabled && (
        selectedCategoryIds.isNotEmpty() ||
        !includeUncategorized ||
        useSelectedCategory ||
        useAllExceptSelected
    )
}

/**
 * Date and time based filters
 */
data class DateFilter(
    val enabled: Boolean = false,
    val filterType: DateFilterType = DateFilterType.ALL,
    val customRangeStart: LocalDateTime? = null,
    val customRangeEnd: LocalDateTime? = null,
    val dueDateFilter: DateFieldFilter = DateFieldFilter(),
    val createdDateFilter: DateFieldFilter = DateFieldFilter(),
    val completedDateFilter: DateFieldFilter = DateFieldFilter()
) {
    fun isActive(): Boolean = enabled && (filterType != DateFilterType.ALL ||
            dueDateFilter.enabled || createdDateFilter.enabled || completedDateFilter.enabled)
}

enum class DateFilterType {
    ALL,
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    NEXT_7_DAYS,
    NEXT_30_DAYS,
    CUSTOM_RANGE
}

data class DateFieldFilter(
    val enabled: Boolean = false,
    val hasValue: Boolean = true,  // true = must have date, false = must be null
    val relativeTime: RelativeTimeFilter = RelativeTimeFilter.ANY
)

enum class RelativeTimeFilter {
    ANY,
    PAST,
    TODAY,
    FUTURE,
    OVERDUE
}

/**
 * XP and difficulty filters
 */
data class XpFilter(
    val enabled: Boolean = false,
    val difficultyLevels: Set<Int> = setOf(20, 40, 60, 80, 100),  // XP percentages
    val xpRewardMin: Int? = null,
    val xpRewardMax: Int? = null
) {
    fun isActive(): Boolean = enabled && (difficultyLevels.size < 5 || xpRewardMin != null || xpRewardMax != null)
}

/**
 * Tag-based filters
 */
data class TagFilter(
    val enabled: Boolean = false,
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val matchMode: TagMatchMode = TagMatchMode.ANY
) {
    fun isActive(): Boolean = enabled && (includedTags.isNotEmpty() || excludedTags.isNotEmpty())
}

enum class TagMatchMode {
    ANY,   // Match if task has any of the included tags
    ALL    // Match only if task has all included tags
}

/**
 * Metadata-based filters
 */
data class MetadataFilter(
    val enabled: Boolean = false,
    val hasContacts: Boolean? = null,  // null = don't filter, true = must have, false = must not have
    val hasLocations: Boolean? = null,
    val hasNotes: Boolean? = null,
    val hasAttachments: Boolean? = null,
    val hasCalendarEvent: Boolean? = null,
    val contactIds: Set<String> = emptySet(),  // Filter by specific contacts
    val locationIds: Set<Long> = emptySet()    // Filter by specific locations
) {
    fun isActive(): Boolean = enabled && (hasContacts != null || hasLocations != null ||
            hasNotes != null || hasAttachments != null || hasCalendarEvent != null ||
            contactIds.isNotEmpty() || locationIds.isNotEmpty())
}

/**
 * Recurring task filters
 */
data class RecurringFilter(
    val enabled: Boolean = false,
    val showRecurring: Boolean = true,
    val showNonRecurring: Boolean = true,
    val recurringTypes: Set<String> = emptySet(),  // DAILY, WEEKLY, MONTHLY, etc.
    val triggerModes: Set<String> = emptySet()     // FIXED_INTERVAL, AFTER_COMPLETION, etc.
) {
    fun isActive(): Boolean = enabled && (!showRecurring || !showNonRecurring ||
            recurringTypes.isNotEmpty() || triggerModes.isNotEmpty())
}

/**
 * Relationship filters (parent/subtask)
 */
data class RelationshipFilter(
    val enabled: Boolean = false,
    val showParentTasks: Boolean = true,
    val showSubtasks: Boolean = true,
    val showStandalone: Boolean = true,
    val specificParentId: Long? = null  // Filter subtasks of specific parent
) {
    fun isActive(): Boolean = enabled && (!showParentTasks || !showSubtasks ||
            !showStandalone || specificParentId != null)
}

/**
 * Sorting options with priority order
 */
enum class SortOption(val displayName: String) {
    DEFAULT("Standard (Fälligkeit aufsteigend)"),
    DUE_DATE_ASC("Fälligkeit ↑"),
    DUE_DATE_DESC("Fälligkeit ↓"),
    CREATED_DATE_ASC("Erstellt ↑"),
    CREATED_DATE_DESC("Erstellt ↓"),
    COMPLETED_DATE_ASC("Abgeschlossen ↑"),
    COMPLETED_DATE_DESC("Abgeschlossen ↓"),
    PRIORITY_ASC("Priorität niedrig → hoch"),
    PRIORITY_DESC("Priorität hoch → niedrig"),
    XP_REWARD_ASC("XP Belohnung ↑"),
    XP_REWARD_DESC("XP Belohnung ↓"),
    DIFFICULTY_ASC("Schwierigkeit ↑"),
    DIFFICULTY_DESC("Schwierigkeit ↓"),
    TITLE_ASC("Titel A → Z"),
    TITLE_DESC("Titel Z → A"),
    CATEGORY_ASC("Kategorie A → Z"),
    CATEGORY_DESC("Kategorie Z → A"),
    STATUS_ASC("Status (Offen → Erledigt)"),
    STATUS_DESC("Status (Erledigt → Offen)");

    companion object {
        fun fromString(value: String): SortOption {
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}

/**
 * Grouping options
 */
enum class GroupByOption(val displayName: String) {
    NONE("Keine Gruppierung"),
    PRIORITY("Nach Priorität"),
    CATEGORY("Nach Kategorie"),
    STATUS("Nach Status"),
    DUE_DATE("Nach Fälligkeit (Heute/Woche/Monat)"),
    DIFFICULTY("Nach Schwierigkeit"),
    COMPLETED("Nach Erledigt/Offen"),
    RECURRING("Nach Wiederkehrend/Einmalig"),
    HAS_CONTACTS("Nach Kontakten (Ja/Nein)"),
    HAS_CALENDAR("Nach Kalendereintrag (Ja/Nein)"),
    PARENT_TASK("Nach Übergeordnet/Subtask/Eigenständig");

    companion object {
        fun fromString(value: String): GroupByOption {
            return entries.find { it.name == value } ?: NONE
        }
    }
}
