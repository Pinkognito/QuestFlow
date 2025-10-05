package com.example.questflow.domain.usecase.metadata

import android.util.Log
import com.example.questflow.data.database.dao.*
import com.example.questflow.data.database.entity.MetadataType
import com.example.questflow.domain.model.TaskMetadataItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Retrieves all metadata items for a task and combines them into a single stream.
 * Handles polymorphic loading of different metadata types.
 */
class GetTaskMetadataUseCase @Inject constructor(
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
    operator fun invoke(taskId: Long): Flow<List<TaskMetadataItem>> {
        Log.d(TAG, "Getting metadata for task $taskId")

        return taskMetadataDao.getMetadataForTask(taskId).map { metadataList ->
            Log.d(TAG, "Processing ${metadataList.size} metadata items for task $taskId")

            metadataList.mapNotNull { metadata ->
                try {
                    when (metadata.metadataType) {
                        MetadataType.LOCATION -> {
                            locationDao.getById(metadata.referenceId)?.let { location ->
                                Log.d(TAG, "Loaded location: ${location.placeName}")
                                TaskMetadataItem.Location(
                                    metadataId = metadata.id,
                                    taskId = metadata.taskId,
                                    displayOrder = metadata.displayOrder,
                                    location = location
                                )
                            }
                        }
                        MetadataType.CONTACT -> {
                            contactDao.getById(metadata.referenceId)?.let { contact ->
                                Log.d(TAG, "Loaded contact: ${contact.displayName}")
                                TaskMetadataItem.Contact(
                                    metadataId = metadata.id,
                                    taskId = metadata.taskId,
                                    displayOrder = metadata.displayOrder,
                                    contact = contact
                                )
                            }
                        }
                        MetadataType.PHONE -> {
                            phoneDao.getById(metadata.referenceId)?.let { phone ->
                                Log.d(TAG, "Loaded phone: ${phone.phoneNumber}")
                                TaskMetadataItem.Phone(
                                    metadataId = metadata.id,
                                    taskId = metadata.taskId,
                                    displayOrder = metadata.displayOrder,
                                    phone = phone
                                )
                            }
                        }
                        MetadataType.ADDRESS -> {
                            addressDao.getById(metadata.referenceId)?.let { address ->
                                Log.d(TAG, "Loaded address: ${address.city}")
                                TaskMetadataItem.Address(
                                    metadataId = metadata.id,
                                    taskId = metadata.taskId,
                                    displayOrder = metadata.displayOrder,
                                    address = address
                                )
                            }
                        }
                        MetadataType.EMAIL -> {
                            emailDao.getById(metadata.referenceId)?.let { email ->
                                Log.d(TAG, "Loaded email: ${email.emailAddress}")
                                TaskMetadataItem.Email(
                                    metadataId = metadata.id,
                                    taskId = metadata.taskId,
                                    displayOrder = metadata.displayOrder,
                                    email = email
                                )
                            }
                        }
                        MetadataType.URL -> {
                            urlDao.getById(metadata.referenceId)?.let { url ->
                                Log.d(TAG, "Loaded URL: ${url.url}")
                                TaskMetadataItem.Url(
                                    metadataId = metadata.id,
                                    taskId = metadata.taskId,
                                    displayOrder = metadata.displayOrder,
                                    url = url
                                )
                            }
                        }
                        MetadataType.NOTE -> {
                            noteDao.getById(metadata.referenceId)?.let { note ->
                                Log.d(TAG, "Loaded note: ${note.content.take(20)}...")
                                TaskMetadataItem.Note(
                                    metadataId = metadata.id,
                                    taskId = metadata.taskId,
                                    displayOrder = metadata.displayOrder,
                                    note = note
                                )
                            }
                        }
                        MetadataType.FILE_ATTACHMENT -> {
                            fileDao.getById(metadata.referenceId)?.let { file ->
                                Log.d(TAG, "Loaded file: ${file.fileName}")
                                TaskMetadataItem.FileAttachment(
                                    metadataId = metadata.id,
                                    taskId = metadata.taskId,
                                    displayOrder = metadata.displayOrder,
                                    file = file
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading metadata ${metadata.id} of type ${metadata.metadataType}", e)
                    null
                }
            }.sortedBy { it.displayOrder }
        }
    }

    companion object {
        private const val TAG = "GetTaskMetadata"
    }
}
