package com.example.questflow.domain.model

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Serializer for AdvancedTaskFilter
 * Uses pipe-delimited format similar to TaskDisplayLayoutHelper
 * Format: section|key:value|key:value;section|key:value...
 */
object TaskFilterSerializer {

    private const val TAG = "TaskFilterSerializer"
    private const val SECTION_DELIMITER = ";"
    private const val FIELD_DELIMITER = "|"
    private const val KEY_VALUE_DELIMITER = ":"
    private const val LIST_DELIMITER = ","

    /**
     * Serialize AdvancedTaskFilter to string
     */
    fun serialize(filter: AdvancedTaskFilter): String {
        val sections = mutableListOf<String>()

        // Status filters
        if (filter.statusFilters.enabled) {
            sections.add(serializeStatusFilter(filter.statusFilters))
        }

        // Priority filters
        if (filter.priorityFilters.enabled) {
            sections.add(serializePriorityFilter(filter.priorityFilters))
        }

        // Category filters
        if (filter.categoryFilters.enabled) {
            sections.add(serializeCategoryFilter(filter.categoryFilters))
        }

        // Date filters
        if (filter.dateFilters.enabled) {
            sections.add(serializeDateFilter(filter.dateFilters))
        }

        // XP filters
        if (filter.xpFilters.enabled) {
            sections.add(serializeXpFilter(filter.xpFilters))
        }

        // Tag filters
        if (filter.tagFilters.enabled) {
            sections.add(serializeTagFilter(filter.tagFilters))
        }

        // Metadata filters
        if (filter.metadataFilters.enabled) {
            sections.add(serializeMetadataFilter(filter.metadataFilters))
        }

        // Recurring filters
        if (filter.recurringFilters.enabled) {
            sections.add(serializeRecurringFilter(filter.recurringFilters))
        }

        // Relationship filters
        if (filter.relationshipFilters.enabled) {
            sections.add(serializeRelationshipFilter(filter.relationshipFilters))
        }

        // Sort options
        if (filter.sortOptions.isNotEmpty() && filter.sortOptions.first() != SortOption.DEFAULT) {
            sections.add(serializeSortOptions(filter.sortOptions))
        }

        // Group by
        if (filter.groupBy != GroupByOption.NONE) {
            sections.add("GROUP${FIELD_DELIMITER}${filter.groupBy.name}")
        }

        // Preset name
        if (filter.presetName.isNotBlank()) {
            sections.add("NAME${FIELD_DELIMITER}${filter.presetName}")
        }

        return sections.joinToString(SECTION_DELIMITER)
    }

    /**
     * Deserialize string to AdvancedTaskFilter
     */
    fun deserialize(data: String): AdvancedTaskFilter {
        if (data.isBlank()) return AdvancedTaskFilter()

        return try {
            val sections = data.split(SECTION_DELIMITER)
            var filter = AdvancedTaskFilter()

            sections.forEach { section ->
                val parts = section.split(FIELD_DELIMITER)
                if (parts.isEmpty()) return@forEach

                when (parts[0]) {
                    "STATUS" -> filter = filter.copy(statusFilters = deserializeStatusFilter(parts))
                    "PRIORITY" -> filter = filter.copy(priorityFilters = deserializePriorityFilter(parts))
                    "CATEGORY" -> filter = filter.copy(categoryFilters = deserializeCategoryFilter(parts))
                    "DATE" -> filter = filter.copy(dateFilters = deserializeDateFilter(parts))
                    "XP" -> filter = filter.copy(xpFilters = deserializeXpFilter(parts))
                    "TAG" -> filter = filter.copy(tagFilters = deserializeTagFilter(parts))
                    "META" -> filter = filter.copy(metadataFilters = deserializeMetadataFilter(parts))
                    "RECURRING" -> filter = filter.copy(recurringFilters = deserializeRecurringFilter(parts))
                    "RELATION" -> filter = filter.copy(relationshipFilters = deserializeRelationshipFilter(parts))
                    "SORT" -> filter = filter.copy(sortOptions = deserializeSortOptions(parts))
                    "GROUP" -> filter = filter.copy(groupBy = GroupByOption.fromString(parts.getOrNull(1) ?: ""))
                    "NAME" -> filter = filter.copy(presetName = parts.getOrNull(1) ?: "")
                }
            }

            filter
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize filter", e)
            AdvancedTaskFilter()
        }
    }

    // === SECTION SERIALIZERS ===

    private fun serializeStatusFilter(filter: StatusFilter): String {
        return buildString {
            append("STATUS")
            append(FIELD_DELIMITER)
            append("completed:${filter.showCompleted}")
            append(FIELD_DELIMITER)
            append("open:${filter.showOpen}")
            append(FIELD_DELIMITER)
            append("expired:${filter.showExpired}")
            append(FIELD_DELIMITER)
            append("claimed:${filter.showClaimed}")
            append(FIELD_DELIMITER)
            append("unclaimed:${filter.showUnclaimed}")
        }
    }

    private fun serializePriorityFilter(filter: PriorityFilter): String {
        return buildString {
            append("PRIORITY")
            append(FIELD_DELIMITER)
            append("urgent:${filter.showUrgent}")
            append(FIELD_DELIMITER)
            append("high:${filter.showHigh}")
            append(FIELD_DELIMITER)
            append("medium:${filter.showMedium}")
            append(FIELD_DELIMITER)
            append("low:${filter.showLow}")
        }
    }

    private fun serializeCategoryFilter(filter: CategoryFilter): String {
        return buildString {
            append("CATEGORY")
            append(FIELD_DELIMITER)
            append("ids:${filter.selectedCategoryIds.joinToString(LIST_DELIMITER)}")
            append(FIELD_DELIMITER)
            append("uncategorized:${filter.includeUncategorized}")
        }
    }

    private fun serializeDateFilter(filter: DateFilter): String {
        return buildString {
            append("DATE")
            append(FIELD_DELIMITER)
            append("type:${filter.filterType.name}")
            if (filter.customRangeStart != null) {
                append(FIELD_DELIMITER)
                append("start:${filter.customRangeStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
            }
            if (filter.customRangeEnd != null) {
                append(FIELD_DELIMITER)
                append("end:${filter.customRangeEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
            }
        }
    }

    private fun serializeXpFilter(filter: XpFilter): String {
        return buildString {
            append("XP")
            append(FIELD_DELIMITER)
            append("levels:${filter.difficultyLevels.joinToString(LIST_DELIMITER)}")
            if (filter.xpRewardMin != null) {
                append(FIELD_DELIMITER)
                append("min:${filter.xpRewardMin}")
            }
            if (filter.xpRewardMax != null) {
                append(FIELD_DELIMITER)
                append("max:${filter.xpRewardMax}")
            }
        }
    }

    private fun serializeTagFilter(filter: TagFilter): String {
        return buildString {
            append("TAG")
            append(FIELD_DELIMITER)
            append("mode:${filter.matchMode.name}")
            append(FIELD_DELIMITER)
            append("included:${filter.includedTags.joinToString(LIST_DELIMITER)}")
            append(FIELD_DELIMITER)
            append("excluded:${filter.excludedTags.joinToString(LIST_DELIMITER)}")
        }
    }

    private fun serializeMetadataFilter(filter: MetadataFilter): String {
        return buildString {
            append("META")
            append(FIELD_DELIMITER)
            append("contacts:${filter.hasContacts}")
            append(FIELD_DELIMITER)
            append("locations:${filter.hasLocations}")
            append(FIELD_DELIMITER)
            append("notes:${filter.hasNotes}")
            append(FIELD_DELIMITER)
            append("attachments:${filter.hasAttachments}")
            append(FIELD_DELIMITER)
            append("calendar:${filter.hasCalendarEvent}")
        }
    }

    private fun serializeRecurringFilter(filter: RecurringFilter): String {
        return buildString {
            append("RECURRING")
            append(FIELD_DELIMITER)
            append("recurring:${filter.showRecurring}")
            append(FIELD_DELIMITER)
            append("nonrecurring:${filter.showNonRecurring}")
        }
    }

    private fun serializeRelationshipFilter(filter: RelationshipFilter): String {
        return buildString {
            append("RELATION")
            append(FIELD_DELIMITER)
            append("parent:${filter.showParentTasks}")
            append(FIELD_DELIMITER)
            append("subtask:${filter.showSubtasks}")
            append(FIELD_DELIMITER)
            append("standalone:${filter.showStandalone}")
        }
    }

    private fun serializeSortOptions(options: List<SortOption>): String {
        return "SORT${FIELD_DELIMITER}${options.joinToString(LIST_DELIMITER) { it.name }}"
    }

    // === SECTION DESERIALIZERS ===

    private fun deserializeStatusFilter(parts: List<String>): StatusFilter {
        val map = parseKeyValues(parts)
        return StatusFilter(
            enabled = true,
            showCompleted = map["completed"]?.toBoolean() ?: true,
            showOpen = map["open"]?.toBoolean() ?: true,
            showExpired = map["expired"]?.toBoolean() ?: true,
            showClaimed = map["claimed"]?.toBoolean() ?: true,
            showUnclaimed = map["unclaimed"]?.toBoolean() ?: true
        )
    }

    private fun deserializePriorityFilter(parts: List<String>): PriorityFilter {
        val map = parseKeyValues(parts)
        return PriorityFilter(
            enabled = true,
            showUrgent = map["urgent"]?.toBoolean() ?: true,
            showHigh = map["high"]?.toBoolean() ?: true,
            showMedium = map["medium"]?.toBoolean() ?: true,
            showLow = map["low"]?.toBoolean() ?: true
        )
    }

    private fun deserializeCategoryFilter(parts: List<String>): CategoryFilter {
        val map = parseKeyValues(parts)
        val ids = map["ids"]?.split(LIST_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet() ?: emptySet()

        return CategoryFilter(
            enabled = true,
            selectedCategoryIds = ids,
            includeUncategorized = map["uncategorized"]?.toBoolean() ?: true
        )
    }

    private fun deserializeDateFilter(parts: List<String>): DateFilter {
        val map = parseKeyValues(parts)
        return DateFilter(
            enabled = true,
            filterType = DateFilterType.valueOf(map["type"] ?: DateFilterType.ALL.name),
            customRangeStart = map["start"]?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) },
            customRangeEnd = map["end"]?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        )
    }

    private fun deserializeXpFilter(parts: List<String>): XpFilter {
        val map = parseKeyValues(parts)
        val levels = map["levels"]?.split(LIST_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: setOf(20, 40, 60, 80, 100)

        return XpFilter(
            enabled = true,
            difficultyLevels = levels,
            xpRewardMin = map["min"]?.toIntOrNull(),
            xpRewardMax = map["max"]?.toIntOrNull()
        )
    }

    private fun deserializeTagFilter(parts: List<String>): TagFilter {
        val map = parseKeyValues(parts)
        val included = map["included"]?.split(LIST_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?.toSet() ?: emptySet()
        val excluded = map["excluded"]?.split(LIST_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?.toSet() ?: emptySet()

        return TagFilter(
            enabled = true,
            includedTags = included,
            excludedTags = excluded,
            matchMode = TagMatchMode.valueOf(map["mode"] ?: TagMatchMode.ANY.name)
        )
    }

    private fun deserializeMetadataFilter(parts: List<String>): MetadataFilter {
        val map = parseKeyValues(parts)
        return MetadataFilter(
            enabled = true,
            hasContacts = map["contacts"]?.toBooleanStrictOrNull(),
            hasLocations = map["locations"]?.toBooleanStrictOrNull(),
            hasNotes = map["notes"]?.toBooleanStrictOrNull(),
            hasAttachments = map["attachments"]?.toBooleanStrictOrNull(),
            hasCalendarEvent = map["calendar"]?.toBooleanStrictOrNull()
        )
    }

    private fun deserializeRecurringFilter(parts: List<String>): RecurringFilter {
        val map = parseKeyValues(parts)
        return RecurringFilter(
            enabled = true,
            showRecurring = map["recurring"]?.toBoolean() ?: true,
            showNonRecurring = map["nonrecurring"]?.toBoolean() ?: true
        )
    }

    private fun deserializeRelationshipFilter(parts: List<String>): RelationshipFilter {
        val map = parseKeyValues(parts)
        return RelationshipFilter(
            enabled = true,
            showParentTasks = map["parent"]?.toBoolean() ?: true,
            showSubtasks = map["subtask"]?.toBoolean() ?: true,
            showStandalone = map["standalone"]?.toBoolean() ?: true
        )
    }

    private fun deserializeSortOptions(parts: List<String>): List<SortOption> {
        return parts.getOrNull(1)
            ?.split(LIST_DELIMITER)
            ?.mapNotNull { name ->
                try {
                    SortOption.valueOf(name)
                } catch (e: Exception) {
                    null
                }
            } ?: listOf(SortOption.DEFAULT)
    }

    // === HELPER FUNCTIONS ===

    private fun parseKeyValues(parts: List<String>): Map<String, String> {
        return parts.drop(1)  // Skip section name
            .mapNotNull { part ->
                val kv = part.split(KEY_VALUE_DELIMITER, limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }
            .toMap()
    }
}
