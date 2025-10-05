package com.example.questflow.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.dao.*
import com.example.questflow.data.database.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the Metadata Library
 * Handles CRUD operations for reusable metadata objects
 */
@HiltViewModel
class MetadataLibraryViewModel @Inject constructor(
    private val locationDao: MetadataLocationDao,
    private val contactDao: MetadataContactDao,
    private val phoneDao: MetadataPhoneDao,
    private val addressDao: MetadataAddressDao,
    private val emailDao: MetadataEmailDao,
    private val urlDao: MetadataUrlDao,
    private val noteDao: MetadataNoteDao,
    private val fileDao: MetadataFileAttachmentDao
) : ViewModel() {

    companion object {
        private const val TAG = "MetadataLibraryViewModel"
    }

    // Flows for all metadata types
    val locations: StateFlow<List<MetadataLocationEntity>> = locationDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<MetadataContactEntity>> = contactDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val phoneNumbers: StateFlow<List<MetadataPhoneEntity>> = phoneDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val addresses: StateFlow<List<MetadataAddressEntity>> = addressDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emails: StateFlow<List<MetadataEmailEntity>> = emailDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val urls: StateFlow<List<MetadataUrlEntity>> = urlDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<MetadataNoteEntity>> = noteDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<MetadataFileAttachmentEntity>> = fileDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CRUD operations for Locations
    fun addLocation(location: MetadataLocationEntity) {
        viewModelScope.launch {
            try {
                val id = locationDao.insert(location)
                Log.d(TAG, "Location added with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding location", e)
            }
        }
    }

    fun updateLocation(location: MetadataLocationEntity) {
        viewModelScope.launch {
            try {
                locationDao.update(location)
                Log.d(TAG, "Location updated: ${location.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating location", e)
            }
        }
    }

    fun deleteLocation(location: MetadataLocationEntity) {
        viewModelScope.launch {
            try {
                locationDao.delete(location)
                Log.d(TAG, "Location deleted: ${location.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting location", e)
            }
        }
    }

    // CRUD operations for Contacts
    fun addContact(contact: MetadataContactEntity) {
        viewModelScope.launch {
            try {
                val id = contactDao.insert(contact)
                Log.d(TAG, "Contact added with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding contact", e)
            }
        }
    }

    fun updateContact(contact: MetadataContactEntity) {
        viewModelScope.launch {
            try {
                contactDao.update(contact)
                Log.d(TAG, "Contact updated: ${contact.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating contact", e)
            }
        }
    }

    fun deleteContact(contact: MetadataContactEntity) {
        viewModelScope.launch {
            try {
                contactDao.delete(contact)
                Log.d(TAG, "Contact deleted: ${contact.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting contact", e)
            }
        }
    }

    // CRUD operations for Phone Numbers
    fun addPhone(phone: MetadataPhoneEntity) {
        viewModelScope.launch {
            try {
                val id = phoneDao.insert(phone)
                Log.d(TAG, "Phone added with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding phone", e)
            }
        }
    }

    fun updatePhone(phone: MetadataPhoneEntity) {
        viewModelScope.launch {
            try {
                phoneDao.update(phone)
                Log.d(TAG, "Phone updated: ${phone.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating phone", e)
            }
        }
    }

    fun deletePhone(phone: MetadataPhoneEntity) {
        viewModelScope.launch {
            try {
                phoneDao.delete(phone)
                Log.d(TAG, "Phone deleted: ${phone.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting phone", e)
            }
        }
    }

    // CRUD operations for Addresses
    fun addAddress(address: MetadataAddressEntity) {
        viewModelScope.launch {
            try {
                val id = addressDao.insert(address)
                Log.d(TAG, "Address added with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding address", e)
            }
        }
    }

    fun updateAddress(address: MetadataAddressEntity) {
        viewModelScope.launch {
            try {
                addressDao.update(address)
                Log.d(TAG, "Address updated: ${address.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating address", e)
            }
        }
    }

    fun deleteAddress(address: MetadataAddressEntity) {
        viewModelScope.launch {
            try {
                addressDao.delete(address)
                Log.d(TAG, "Address deleted: ${address.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting address", e)
            }
        }
    }

    // CRUD operations for Emails
    fun addEmail(email: MetadataEmailEntity) {
        viewModelScope.launch {
            try {
                val id = emailDao.insert(email)
                Log.d(TAG, "Email added with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding email", e)
            }
        }
    }

    fun updateEmail(email: MetadataEmailEntity) {
        viewModelScope.launch {
            try {
                emailDao.update(email)
                Log.d(TAG, "Email updated: ${email.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating email", e)
            }
        }
    }

    fun deleteEmail(email: MetadataEmailEntity) {
        viewModelScope.launch {
            try {
                emailDao.delete(email)
                Log.d(TAG, "Email deleted: ${email.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting email", e)
            }
        }
    }

    // CRUD operations for URLs
    fun addUrl(url: MetadataUrlEntity) {
        viewModelScope.launch {
            try {
                val id = urlDao.insert(url)
                Log.d(TAG, "URL added with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding URL", e)
            }
        }
    }

    fun updateUrl(url: MetadataUrlEntity) {
        viewModelScope.launch {
            try {
                urlDao.update(url)
                Log.d(TAG, "URL updated: ${url.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating URL", e)
            }
        }
    }

    fun deleteUrl(url: MetadataUrlEntity) {
        viewModelScope.launch {
            try {
                urlDao.delete(url)
                Log.d(TAG, "URL deleted: ${url.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting URL", e)
            }
        }
    }

    // CRUD operations for Notes
    fun addNote(note: MetadataNoteEntity) {
        viewModelScope.launch {
            try {
                val id = noteDao.insert(note)
                Log.d(TAG, "Note added with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding note", e)
            }
        }
    }

    fun updateNote(note: MetadataNoteEntity) {
        viewModelScope.launch {
            try {
                noteDao.update(note)
                Log.d(TAG, "Note updated: ${note.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating note", e)
            }
        }
    }

    fun deleteNote(note: MetadataNoteEntity) {
        viewModelScope.launch {
            try {
                noteDao.delete(note)
                Log.d(TAG, "Note deleted: ${note.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting note", e)
            }
        }
    }

    // CRUD operations for Files
    fun addFile(file: MetadataFileAttachmentEntity) {
        viewModelScope.launch {
            try {
                val id = fileDao.insert(file)
                Log.d(TAG, "File added with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding file", e)
            }
        }
    }

    fun updateFile(file: MetadataFileAttachmentEntity) {
        viewModelScope.launch {
            try {
                fileDao.update(file)
                Log.d(TAG, "File updated: ${file.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating file", e)
            }
        }
    }

    fun deleteFile(file: MetadataFileAttachmentEntity) {
        viewModelScope.launch {
            try {
                fileDao.delete(file)
                Log.d(TAG, "File deleted: ${file.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting file", e)
            }
        }
    }
}
