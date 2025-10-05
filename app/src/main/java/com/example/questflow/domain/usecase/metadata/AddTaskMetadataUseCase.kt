package com.example.questflow.domain.usecase.metadata

import android.util.Log
import com.example.questflow.data.database.dao.*
import com.example.questflow.data.database.entity.*
import com.example.questflow.domain.model.TaskMetadataItem
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Adds metadata to a task. Handles polymorphic insertion into correct tables.
 */
class AddTaskMetadataUseCase @Inject constructor(
    private val taskMetadataDao: TaskMetadataDao,
    private val locationDao: MetadataLocationDao,
    private val contactDao: MetadataContactDao,
    private val phoneDao: MetadataPhoneDao,
    private val addressDao: MetadataAddressDao,
    private val emailDao: MetadataEmailDao,
    private val urlDao: MetadataUrlDao,
    private val noteDao: MetadataNoteDao,
    private val fileDao: MetadataFileAttachmentDao
) {
    suspend operator fun invoke(
        taskId: Long,
        item: TaskMetadataItem
    ): Result<Long> = runCatching {
        Log.d(TAG, "Adding metadata to task $taskId: ${item::class.simpleName}")

        // Get current max display order
        val currentCount = taskMetadataDao.getMetadataCount(taskId)
        val displayOrder = item.displayOrder.takeIf { it > 0 } ?: currentCount

        // Insert into specific table and get reference ID
        val referenceId = when (item) {
            is TaskMetadataItem.Location -> {
                Log.d(TAG, "Inserting location: ${item.location.placeName}")
                locationDao.insert(item.location)
            }
            is TaskMetadataItem.Contact -> {
                Log.d(TAG, "Inserting contact: ${item.contact.displayName}")
                contactDao.insert(item.contact)
            }
            is TaskMetadataItem.Phone -> {
                Log.d(TAG, "Inserting phone: ${item.phone.phoneNumber}")
                phoneDao.insert(item.phone)
            }
            is TaskMetadataItem.Address -> {
                Log.d(TAG, "Inserting address: ${item.address.city}")
                addressDao.insert(item.address)
            }
            is TaskMetadataItem.Email -> {
                Log.d(TAG, "Inserting email: ${item.email.emailAddress}")
                emailDao.insert(item.email)
            }
            is TaskMetadataItem.Url -> {
                Log.d(TAG, "Inserting URL: ${item.url.url}")
                urlDao.insert(item.url)
            }
            is TaskMetadataItem.Note -> {
                Log.d(TAG, "Inserting note: ${item.note.content.take(20)}...")
                noteDao.insert(item.note)
            }
            is TaskMetadataItem.FileAttachment -> {
                Log.d(TAG, "Inserting file: ${item.file.fileName}")
                fileDao.insert(item.file)
            }
        }

        Log.d(TAG, "Inserted metadata with referenceId: $referenceId")

        // Insert into registry table
        val metadataType = when (item) {
            is TaskMetadataItem.Location -> MetadataType.LOCATION
            is TaskMetadataItem.Contact -> MetadataType.CONTACT
            is TaskMetadataItem.Phone -> MetadataType.PHONE
            is TaskMetadataItem.Address -> MetadataType.ADDRESS
            is TaskMetadataItem.Email -> MetadataType.EMAIL
            is TaskMetadataItem.Url -> MetadataType.URL
            is TaskMetadataItem.Note -> MetadataType.NOTE
            is TaskMetadataItem.FileAttachment -> MetadataType.FILE_ATTACHMENT
        }

        val metadataId = taskMetadataDao.insert(
            TaskMetadataEntity(
                taskId = taskId,
                metadataType = metadataType,
                referenceId = referenceId,
                displayOrder = displayOrder,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        Log.d(TAG, "Created metadata registry entry with ID: $metadataId")
        metadataId
    }.onFailure { exception ->
        Log.e(TAG, "Failed to add metadata to task $taskId", exception)
    }

    companion object {
        private const val TAG = "AddTaskMetadata"
    }
}
