package com.example.questflow.domain.usecase.metadata

import android.util.Log
import com.example.questflow.data.database.dao.*
import com.example.questflow.domain.model.TaskMetadataItem
import javax.inject.Inject

/**
 * Deletes metadata from a task. Handles cleanup of both registry and specific data tables.
 */
class DeleteTaskMetadataUseCase @Inject constructor(
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
    suspend operator fun invoke(item: TaskMetadataItem): Result<Unit> = runCatching {
        Log.d(TAG, "Deleting metadata ${item.metadataId} of type ${item::class.simpleName}")

        // Delete from specific table first
        when (item) {
            is TaskMetadataItem.Location -> {
                Log.d(TAG, "Deleting location: ${item.location.placeName}")
                locationDao.delete(item.location)
            }
            is TaskMetadataItem.Contact -> {
                Log.d(TAG, "Deleting contact: ${item.contact.displayName}")
                contactDao.delete(item.contact)
            }
            is TaskMetadataItem.Phone -> {
                Log.d(TAG, "Deleting phone: ${item.phone.phoneNumber}")
                phoneDao.delete(item.phone)
            }
            is TaskMetadataItem.Address -> {
                Log.d(TAG, "Deleting address: ${item.address.city}")
                addressDao.delete(item.address)
            }
            is TaskMetadataItem.Email -> {
                Log.d(TAG, "Deleting email: ${item.email.emailAddress}")
                emailDao.delete(item.email)
            }
            is TaskMetadataItem.Url -> {
                Log.d(TAG, "Deleting URL: ${item.url.url}")
                urlDao.delete(item.url)
            }
            is TaskMetadataItem.Note -> {
                Log.d(TAG, "Deleting note")
                noteDao.delete(item.note)
            }
            is TaskMetadataItem.FileAttachment -> {
                Log.d(TAG, "Deleting file: ${item.file.fileName}")
                fileDao.delete(item.file)
            }
        }

        // Delete from registry table
        taskMetadataDao.deleteById(item.metadataId)
        Log.d(TAG, "Deleted metadata registry entry ${item.metadataId}")
        Unit
    }.onFailure { exception ->
        Log.e(TAG, "Failed to delete metadata ${item.metadataId}", exception)
    }

    companion object {
        private const val TAG = "DeleteTaskMetadata"
    }
}
