package com.example.questflow.domain.usecase

import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.domain.model.*
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use Case for applying comprehensive task filtering, sorting, and grouping
 * This is the heart of the advanced filter system
 */
class ApplyAdvancedTaskFilterUseCase @Inject constructor() {

    /**
     * Apply all filters, sorting, and grouping to a list of calendar links
     * Returns filtered, sorted, and optionally grouped results
     */
    fun execute(
        links: List<CalendarEventLinkEntity>,
        filter: AdvancedTaskFilter,
        categories: List<CategoryEntity>,
        textSearchQuery: String = ""
    ): FilteredTaskResult {
        var filtered = links

        // Apply text search first (if provided - comes from search bar)
        if (textSearchQuery.isNotBlank()) {
            // Text search is handled separately in TasksViewModel with TaskSearchFilterSettings
            // This filter only applies structured filters
        }

        // Apply status filters
        if (filter.statusFilters.enabled) {
            filtered = applyStatusFilter(filtered, filter.statusFilters)
        }

        // Apply priority filters
        if (filter.priorityFilters.enabled) {
            filtered = applyPriorityFilter(filtered, filter.priorityFilters)
        }

        // Apply category filters
        if (filter.categoryFilters.enabled) {
            filtered = applyCategoryFilter(filtered, filter.categoryFilters)
        }

        // Apply date filters
        if (filter.dateFilters.enabled) {
            filtered = applyDateFilter(filtered, filter.dateFilters)
        }

        // Apply XP filters
        if (filter.xpFilters.enabled) {
            filtered = applyXpFilter(filtered, filter.xpFilters)
        }

        // Apply tag filters
        if (filter.tagFilters.enabled) {
            filtered = applyTagFilter(filtered, filter.tagFilters)
        }

        // Apply metadata filters
        if (filter.metadataFilters.enabled) {
            filtered = applyMetadataFilter(filtered, filter.metadataFilters)
        }

        // Apply recurring filters
        if (filter.recurringFilters.enabled) {
            filtered = applyRecurringFilter(filtered, filter.recurringFilters)
        }

        // Apply relationship filters
        if (filter.relationshipFilters.enabled) {
            filtered = applyRelationshipFilter(filtered, filter.relationshipFilters)
        }

        // Apply sorting (multi-level)
        val sorted = applySorting(filtered, filter.sortOptions, categories)

        // Apply grouping
        val grouped = if (filter.groupBy != GroupByOption.NONE) {
            applyGrouping(sorted, filter.groupBy, categories)
        } else {
            mapOf("" to sorted)  // No grouping, single unnamed group
        }

        return FilteredTaskResult(
            allTasks = sorted,
            groupedTasks = grouped,
            totalCount = links.size,
            filteredCount = sorted.size,
            appliedFilters = filter
        )
    }

    // === FILTER IMPLEMENTATIONS ===

    private fun applyStatusFilter(
        links: List<CalendarEventLinkEntity>,
        filter: StatusFilter
    ): List<CalendarEventLinkEntity> {
        android.util.Log.d("StatusFilter", "=== Applying Status Filter to ${links.size} links ===")

        val filtered = links.filter { link ->
            val now = LocalDateTime.now()
            val isCompleted = link.status == "CLAIMED" || link.rewarded
            val isExpired = link.startsAt.isBefore(now) && !isCompleted
            val isClaimed = link.status == "CLAIMED"
            val isUnclaimed = link.status != "CLAIMED"
            val isOpen = !isCompleted

            // New logic: Check if task matches ANY of the enabled "show" conditions
            // This treats each checkbox as "include tasks with this property"
            var passes = false

            // Check completion status (completed vs open are mutually exclusive)
            if (isCompleted && filter.showCompleted) passes = true
            if (isOpen && filter.showOpen) passes = true

            // Check claim status (claimed vs unclaimed are mutually exclusive)
            // These can override the above if specified
            if (isClaimed && filter.showClaimed) passes = true
            if (isUnclaimed && filter.showUnclaimed) passes = true

            // Expired is an additional filter on top of open/unclaimed
            if (isExpired && !filter.showExpired) passes = false

            // Log first 3 tasks for debugging
            if (links.indexOf(link) < 3) {
                android.util.Log.d("StatusFilter", "Task '${link.title}' (status=${link.status}, rewarded=${link.rewarded})")
                android.util.Log.d("StatusFilter", "  isCompleted=$isCompleted, isOpen=$isOpen, isExpired=$isExpired, isClaimed=$isClaimed, isUnclaimed=$isUnclaimed")
                android.util.Log.d("StatusFilter", "  showCompleted=${filter.showCompleted}, showOpen=${filter.showOpen}, showExpired=${filter.showExpired}")
                android.util.Log.d("StatusFilter", "  showClaimed=${filter.showClaimed}, showUnclaimed=${filter.showUnclaimed}")
                android.util.Log.d("StatusFilter", "  RESULT: ${if (passes) "KEEP" else "FILTER OUT"}")
            }

            passes
        }

        android.util.Log.d("StatusFilter", "Result: ${filtered.size}/${links.size} tasks passed filter")
        return filtered
    }

    private fun applyPriorityFilter(
        links: List<CalendarEventLinkEntity>,
        filter: PriorityFilter
    ): List<CalendarEventLinkEntity> {
        // CalendarEventLinkEntity doesn't have priority field directly
        // Priority filter would require joining with TaskEntity
        // For now, pass through all tasks if priority filter is enabled
        return links
    }

    private fun applyCategoryFilter(
        links: List<CalendarEventLinkEntity>,
        filter: CategoryFilter
    ): List<CalendarEventLinkEntity> {
        return links.filter { link ->
            if (link.categoryId == null) {
                filter.includeUncategorized
            } else {
                filter.selectedCategoryIds.isEmpty() || filter.selectedCategoryIds.contains(link.categoryId)
            }
        }
    }

    private fun applyDateFilter(
        links: List<CalendarEventLinkEntity>,
        filter: DateFilter
    ): List<CalendarEventLinkEntity> {
        val now = LocalDateTime.now()

        return links.filter { link ->
            when (filter.filterType) {
                DateFilterType.ALL -> true
                DateFilterType.TODAY -> link.startsAt.toLocalDate() == now.toLocalDate()
                DateFilterType.YESTERDAY -> link.startsAt.toLocalDate() == now.toLocalDate().minusDays(1)
                DateFilterType.THIS_WEEK -> {
                    val weekStart = now.toLocalDate().minusDays(now.dayOfWeek.value.toLong() - 1)
                    val weekEnd = weekStart.plusDays(6)
                    link.startsAt.toLocalDate() >= weekStart && link.startsAt.toLocalDate() <= weekEnd
                }
                DateFilterType.LAST_WEEK -> {
                    val weekStart = now.toLocalDate().minusDays(now.dayOfWeek.value.toLong() + 6)
                    val weekEnd = weekStart.plusDays(6)
                    link.startsAt.toLocalDate() >= weekStart && link.startsAt.toLocalDate() <= weekEnd
                }
                DateFilterType.THIS_MONTH -> {
                    link.startsAt.year == now.year && link.startsAt.month == now.month
                }
                DateFilterType.LAST_MONTH -> {
                    val lastMonth = now.minusMonths(1)
                    link.startsAt.year == lastMonth.year && link.startsAt.month == lastMonth.month
                }
                DateFilterType.THIS_YEAR -> link.startsAt.year == now.year
                DateFilterType.NEXT_7_DAYS -> {
                    link.startsAt.toLocalDate() >= now.toLocalDate() &&
                            link.startsAt.toLocalDate() <= now.toLocalDate().plusDays(7)
                }
                DateFilterType.NEXT_30_DAYS -> {
                    link.startsAt.toLocalDate() >= now.toLocalDate() &&
                            link.startsAt.toLocalDate() <= now.toLocalDate().plusDays(30)
                }
                DateFilterType.CUSTOM_RANGE -> {
                    val start = filter.customRangeStart ?: now
                    val end = filter.customRangeEnd ?: now
                    link.startsAt >= start && link.startsAt <= end
                }
            }
        }
    }

    private fun applyXpFilter(
        links: List<CalendarEventLinkEntity>,
        filter: XpFilter
    ): List<CalendarEventLinkEntity> {
        return links.filter { link ->
            val matchesDifficulty = filter.difficultyLevels.isEmpty() || filter.difficultyLevels.contains(link.xpPercentage)
            val matchesMin = filter.xpRewardMin?.let { link.xp >= it } ?: true
            val matchesMax = filter.xpRewardMax?.let { link.xp <= it } ?: true

            matchesDifficulty && matchesMin && matchesMax
        }
    }

    private fun applyTagFilter(
        links: List<CalendarEventLinkEntity>,
        filter: TagFilter
    ): List<CalendarEventLinkEntity> {
        // Tags not fully implemented in current schema, placeholder for future
        return links
    }

    private fun applyMetadataFilter(
        links: List<CalendarEventLinkEntity>,
        filter: MetadataFilter
    ): List<CalendarEventLinkEntity> {
        return links.filter { link ->
            val hasCalendar = link.calendarEventId != 0L
            val matchesCalendar = filter.hasCalendarEvent?.let { it == hasCalendar } ?: true

            // Other metadata checks would require joining with task metadata tables
            // This is a simplified implementation
            matchesCalendar
        }
    }

    private fun applyRecurringFilter(
        links: List<CalendarEventLinkEntity>,
        filter: RecurringFilter
    ): List<CalendarEventLinkEntity> {
        return links.filter { link ->
            val isRecurring = link.isRecurring
            (!isRecurring || filter.showRecurring) && (isRecurring || filter.showNonRecurring)
        }
    }

    private fun applyRelationshipFilter(
        links: List<CalendarEventLinkEntity>,
        filter: RelationshipFilter
    ): List<CalendarEventLinkEntity> {
        return links.filter { link ->
            val hasParent = link.taskId != null && link.taskId != 0L
            val isParent = false  // Would need to check if other tasks reference this
            val isStandalone = !hasParent && !isParent

            (!hasParent || filter.showSubtasks) &&
                    (!isParent || filter.showParentTasks) &&
                    (!isStandalone || filter.showStandalone)
        }
    }

    // === SORTING IMPLEMENTATION ===

    private fun applySorting(
        links: List<CalendarEventLinkEntity>,
        sortOptions: List<SortOption>,
        categories: List<CategoryEntity>
    ): List<CalendarEventLinkEntity> {
        if (sortOptions.isEmpty()) return links

        val categoryMap = categories.associateBy { it.id }

        // Build comparator chain for multi-level sorting
        val finalComparator = sortOptions.reversed().fold<SortOption, Comparator<CalendarEventLinkEntity>?>(null) { acc, option ->
            val nextComparator = createComparator(option, categoryMap)
            if (acc == null) {
                nextComparator
            } else {
                nextComparator.thenComparing(acc)
            }
        }

        return if (finalComparator != null) {
            links.sortedWith(finalComparator.thenBy { it.id })
        } else {
            links
        }
    }

    private fun createComparator(
        option: SortOption,
        categoryMap: Map<Long, CategoryEntity>
    ): Comparator<CalendarEventLinkEntity> {
        return when (option) {
            SortOption.DEFAULT, SortOption.DUE_DATE_ASC -> compareBy { it.startsAt }
            SortOption.DUE_DATE_DESC -> compareByDescending { it.startsAt }
            SortOption.CREATED_DATE_ASC, SortOption.CREATED_DATE_DESC -> compareBy { it.id } // Fallback to ID
            SortOption.COMPLETED_DATE_ASC, SortOption.COMPLETED_DATE_DESC -> compareBy {
                if (it.status == "CLAIMED") 0 else 1
            }
            SortOption.PRIORITY_ASC, SortOption.PRIORITY_DESC -> compareBy { it.id } // Priority not in entity
            SortOption.XP_REWARD_ASC -> compareBy { it.xp }
            SortOption.XP_REWARD_DESC -> compareByDescending { it.xp }
            SortOption.DIFFICULTY_ASC -> compareBy { it.xpPercentage }
            SortOption.DIFFICULTY_DESC -> compareByDescending { it.xpPercentage }
            SortOption.TITLE_ASC -> compareBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> compareByDescending { it.title.lowercase() }
            SortOption.CATEGORY_ASC -> compareBy {
                it.categoryId?.let { id -> categoryMap[id]?.name?.lowercase() } ?: "zzz"
            }
            SortOption.CATEGORY_DESC -> compareByDescending {
                it.categoryId?.let { id -> categoryMap[id]?.name?.lowercase() } ?: ""
            }
            SortOption.STATUS_ASC -> compareBy { if (it.status == "CLAIMED") 1 else 0 }
            SortOption.STATUS_DESC -> compareByDescending { if (it.status == "CLAIMED") 1 else 0 }
        }
    }

    // === GROUPING IMPLEMENTATION ===

    private fun applyGrouping(
        links: List<CalendarEventLinkEntity>,
        groupBy: GroupByOption,
        categories: List<CategoryEntity>
    ): Map<String, List<CalendarEventLinkEntity>> {
        val categoryMap = categories.associateBy { it.id }

        return when (groupBy) {
            GroupByOption.NONE -> mapOf("" to links)
            GroupByOption.PRIORITY -> links.groupBy {
                // Priority not in CalendarEventLinkEntity, would need TaskEntity join
                "📋 Alle Tasks"
            }
            GroupByOption.CATEGORY -> links.groupBy { link ->
                if (link.categoryId != null) {
                    val cat = categoryMap[link.categoryId]
                    "${cat?.emoji ?: "📁"} ${cat?.name ?: "Unbekannt"}"
                } else {
                    "📋 Ohne Kategorie"
                }
            }
            GroupByOption.STATUS -> links.groupBy { link ->
                when (link.status) {
                    "CLAIMED" -> "✅ Erledigt"
                    "EXPIRED" -> "⏰ Abgelaufen"
                    "PENDING" -> "⏳ Ausstehend"
                    else -> "❓ Unbekannt"
                }
            }
            GroupByOption.DUE_DATE -> {
                val now = LocalDateTime.now()
                links.groupBy { link ->
                    when {
                        link.startsAt.toLocalDate() == now.toLocalDate() -> "📅 Heute"
                        link.startsAt.toLocalDate() == now.toLocalDate().plusDays(1) -> "📆 Morgen"
                        link.startsAt.toLocalDate().isBefore(now.toLocalDate()) -> "⏪ Vergangen"
                        link.startsAt.toLocalDate() <= now.toLocalDate().plusDays(7) -> "📅 Diese Woche"
                        link.startsAt.toLocalDate() <= now.toLocalDate().plusDays(30) -> "📅 Dieser Monat"
                        else -> "📅 Später"
                    }
                }
            }
            GroupByOption.DIFFICULTY -> links.groupBy { link ->
                when (link.xpPercentage) {
                    20 -> "⭐ Trivial (20%)"
                    40 -> "⭐⭐ Einfach (40%)"
                    60 -> "⭐⭐⭐ Mittel (60%)"
                    80 -> "⭐⭐⭐⭐ Schwer (80%)"
                    100 -> "⭐⭐⭐⭐⭐ Episch (100%)"
                    else -> "❓ Unbekannt"
                }
            }
            GroupByOption.COMPLETED -> links.groupBy { link ->
                if (link.status == "CLAIMED" || link.rewarded) "✅ Erledigt" else "⏳ Offen"
            }
            GroupByOption.RECURRING -> links.groupBy { link ->
                if (link.isRecurring) "🔄 Wiederkehrend" else "📋 Einmalig"
            }
            GroupByOption.HAS_CONTACTS -> links.groupBy { link ->
                // Would need to check metadata
                "👤 Mit Kontakten"  // Placeholder
            }
            GroupByOption.HAS_CALENDAR -> links.groupBy { link ->
                if (link.calendarEventId != 0L) "📅 Mit Kalendereintrag" else "📋 Ohne Kalendereintrag"
            }
            GroupByOption.PARENT_TASK -> links.groupBy { link ->
                when {
                    link.taskId != null && link.taskId != 0L -> "📎 Subtask"
                    else -> "📄 Eigenständig"
                }
            }
        }
    }
}

/**
 * Result of applying filters, sorting, and grouping
 */
data class FilteredTaskResult(
    val allTasks: List<CalendarEventLinkEntity>,
    val groupedTasks: Map<String, List<CalendarEventLinkEntity>>,
    val totalCount: Int,
    val filteredCount: Int,
    val appliedFilters: AdvancedTaskFilter
)
