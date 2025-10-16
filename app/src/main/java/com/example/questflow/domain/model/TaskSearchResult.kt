package com.example.questflow.domain.model

/**
 * Wrapper for a task with search match information
 * Shows which filter criteria matched during search
 */
data class TaskSearchResult(
    val task: Task,
    val matchedFilters: List<SearchMatchInfo>
)

/**
 * Information about which specific filter matched
 */
sealed class SearchMatchInfo {
    data class TaskTitle(val matchedText: String) : SearchMatchInfo()
    data class TaskDescription(val matchedText: String) : SearchMatchInfo()
    data class CategoryName(val categoryName: String) : SearchMatchInfo()
    data class ContactDisplayName(val contactName: String) : SearchMatchInfo()
    data class ContactGivenName(val contactName: String) : SearchMatchInfo()
    data class ContactFamilyName(val contactName: String) : SearchMatchInfo()
    data class ContactPhone(val phoneNumber: String) : SearchMatchInfo()
    data class ContactEmail(val email: String) : SearchMatchInfo()
    data class ContactOrganization(val organization: String) : SearchMatchInfo()
    data class ContactJobTitle(val jobTitle: String) : SearchMatchInfo()
    data class ContactNote(val note: String) : SearchMatchInfo()
    data class ParentTaskTitle(val parentTitle: String) : SearchMatchInfo()
    data class ParentTaskDescription(val parentDescription: String) : SearchMatchInfo()
    data class DifficultyLevel(val difficulty: String) : SearchMatchInfo()
    data class DueDate(val dateString: String) : SearchMatchInfo()
    data class CreatedDate(val dateString: String) : SearchMatchInfo()
    data class CompletedDate(val dateString: String) : SearchMatchInfo()
}

/**
 * Extension function to get display text for UI
 */
fun SearchMatchInfo.getDisplayText(): String {
    return when (this) {
        is SearchMatchInfo.TaskTitle -> "Titel"
        is SearchMatchInfo.TaskDescription -> "Beschreibung"
        is SearchMatchInfo.CategoryName -> "Kategorie: $categoryName"
        is SearchMatchInfo.ContactDisplayName -> "Kontakt: $contactName"
        is SearchMatchInfo.ContactGivenName -> "Kontakt (Vorname): $contactName"
        is SearchMatchInfo.ContactFamilyName -> "Kontakt (Nachname): $contactName"
        is SearchMatchInfo.ContactPhone -> "Telefon: $phoneNumber"
        is SearchMatchInfo.ContactEmail -> "E-Mail: $email"
        is SearchMatchInfo.ContactOrganization -> "Firma: $organization"
        is SearchMatchInfo.ContactJobTitle -> "Position: $jobTitle"
        is SearchMatchInfo.ContactNote -> "Kontakt-Notiz"
        is SearchMatchInfo.ParentTaskTitle -> "Übergeordnete Task"
        is SearchMatchInfo.ParentTaskDescription -> "Übergeordnete Task (Beschreibung)"
        is SearchMatchInfo.DifficultyLevel -> "Schwierigkeitsgrad: $difficulty"
        is SearchMatchInfo.DueDate -> "Fälligkeitsdatum"
        is SearchMatchInfo.CreatedDate -> "Erstellungsdatum"
        is SearchMatchInfo.CompletedDate -> "Abschlussdatum"
    }
}
