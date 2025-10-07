package com.example.questflow.domain.contact

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages contact selection with tag-based logic and user priority
 *
 * Rules:
 * 1. User manual selections ALWAYS have priority
 * 2. Tag selection adds contacts automatically
 * 3. Tag deselection removes contacts ONLY if:
 *    - Contact is not in another active tag
 *    - Contact was not manually selected by user
 * 4. Manual deselection prevents tag from adding contact again
 */
@Singleton
class ContactSelectionManager @Inject constructor() {

    /**
     * Apply tag selection change and return new selected contacts
     *
     * @param currentSelected Currently selected contact IDs
     * @param activeTags Currently active tags
     * @param tagToToggle Tag being toggled (added or removed)
     * @param isAdding true if adding tag, false if removing
     * @param tagContactMap Map of tag -> list of contact IDs
     * @param manuallySelected Contact IDs manually selected by user
     * @param manuallyDeselected Contact IDs manually deselected by user
     *
     * @return Triple of:
     *   - New selected contacts
     *   - Updated manually selected set
     *   - Updated manually deselected set
     */
    fun applyTagChange(
        currentSelected: Set<Long>,
        activeTags: Set<String>,
        tagToToggle: String,
        isAdding: Boolean,
        tagContactMap: Map<String, List<Long>>,
        manuallySelected: Set<Long>,
        manuallyDeselected: Set<Long>
    ): SelectionResult {
        val newSelected = currentSelected.toMutableSet()
        val newActiveTags = activeTags.toMutableSet()

        if (isAdding) {
            // Adding tag
            newActiveTags.add(tagToToggle)

            // Add contacts from this tag
            val contactsToAdd = tagContactMap[tagToToggle] ?: emptyList()
            for (contactId in contactsToAdd) {
                // Only add if NOT manually deselected
                if (contactId !in manuallyDeselected) {
                    newSelected.add(contactId)
                }
            }
        } else {
            // Removing tag
            newActiveTags.remove(tagToToggle)

            // Try to remove contacts from this tag
            val contactsToRemove = tagContactMap[tagToToggle] ?: emptyList()
            for (contactId in contactsToRemove) {
                // Can only remove if:
                // 1. Not manually selected by user
                // 2. Not in any other active tag
                if (contactId !in manuallySelected) {
                    val isInOtherTag = newActiveTags.any { otherTag ->
                        tagContactMap[otherTag]?.contains(contactId) == true
                    }
                    if (!isInOtherTag) {
                        newSelected.remove(contactId)
                    }
                }
            }
        }

        return SelectionResult(
            selectedContacts = newSelected,
            activeTags = newActiveTags,
            manuallySelected = manuallySelected, // Unchanged by tag operations
            manuallyDeselected = manuallyDeselected // Unchanged by tag operations
        )
    }

    /**
     * Apply manual contact selection change
     *
     * @param currentSelected Currently selected contact IDs
     * @param contactId Contact being toggled
     * @param isAdding true if selecting, false if deselecting
     * @param manuallySelected Contact IDs manually selected
     * @param manuallyDeselected Contact IDs manually deselected
     * @param activeTags Currently active tags
     * @param tagContactMap Map of tag -> list of contact IDs
     *
     * @return Updated selection state
     */
    fun applyManualChange(
        currentSelected: Set<Long>,
        contactId: Long,
        isAdding: Boolean,
        manuallySelected: Set<Long>,
        manuallyDeselected: Set<Long>,
        activeTags: Set<String>,
        tagContactMap: Map<String, List<Long>>
    ): SelectionResult {
        val newSelected = currentSelected.toMutableSet()
        val newManuallySelected = manuallySelected.toMutableSet()
        val newManuallyDeselected = manuallyDeselected.toMutableSet()

        if (isAdding) {
            // Manual selection
            newSelected.add(contactId)
            newManuallySelected.add(contactId)
            newManuallyDeselected.remove(contactId) // Clear any previous deselection
        } else {
            // Manual deselection
            newSelected.remove(contactId)
            newManuallyDeselected.add(contactId)
            newManuallySelected.remove(contactId) // Clear any previous selection

            // Note: Even if contact is in active tags, it stays deselected due to user priority
        }

        return SelectionResult(
            selectedContacts = newSelected,
            activeTags = activeTags,
            manuallySelected = newManuallySelected,
            manuallyDeselected = newManuallyDeselected
        )
    }

    /**
     * Initialize selection state from existing tag assignments
     * Useful when opening dialog for editing
     */
    fun initializeFromTags(
        tagContactMap: Map<String, List<Long>>,
        activeTags: Set<String>
    ): SelectionResult {
        val selectedContacts = mutableSetOf<Long>()

        // Add all contacts from active tags
        for (tag in activeTags) {
            val contacts = tagContactMap[tag] ?: emptyList()
            selectedContacts.addAll(contacts)
        }

        return SelectionResult(
            selectedContacts = selectedContacts,
            activeTags = activeTags,
            manuallySelected = emptySet(),
            manuallyDeselected = emptySet()
        )
    }

    /**
     * Get contacts that should be highlighted as "from tags" vs "manually selected"
     */
    fun categorizeContacts(
        selectedContacts: Set<Long>,
        manuallySelected: Set<Long>,
        activeTags: Set<String>,
        tagContactMap: Map<String, List<Long>>
    ): ContactCategories {
        val fromTags = mutableSetOf<Long>()
        val manualOnly = mutableSetOf<Long>()

        for (contactId in selectedContacts) {
            // Check if contact is in any active tag
            val isInTag = activeTags.any { tag ->
                tagContactMap[tag]?.contains(contactId) == true
            }

            if (isInTag && contactId !in manuallySelected) {
                fromTags.add(contactId)
            } else if (contactId in manuallySelected) {
                manualOnly.add(contactId)
            }
        }

        return ContactCategories(
            fromTags = fromTags,
            manualOnly = manualOnly
        )
    }
}

data class SelectionResult(
    val selectedContacts: Set<Long>,
    val activeTags: Set<String>,
    val manuallySelected: Set<Long>,
    val manuallyDeselected: Set<Long>
)

data class ContactCategories(
    val fromTags: Set<Long>,
    val manualOnly: Set<Long>
)
