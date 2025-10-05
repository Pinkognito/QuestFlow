package com.example.questflow.presentation.screens.library

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactDialog(
    onDismiss: () -> Unit,
    onContactSelected: (ContactData) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            val contactData = parseContactData(context, contactUri)
            if (contactData != null) {
                onContactSelected(contactData)
                onDismiss()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kontakt importieren",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Schließen")
                    }
                }

                Text(
                    text = "Wähle einen Kontakt aus deinem Telefonbuch. Alle zugehörigen Daten (Telefonnummern, E-Mails, Adressen) werden automatisch importiert.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!hasPermission) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kontaktzugriff erlauben")
                    }
                } else {
                    Button(
                        onClick = {
                            contactPickerLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kontakt auswählen")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abbrechen")
                }
            }
        }
    }
}

/**
 * Data class holding all extracted contact information
 */
data class ContactData(
    val displayName: String,
    val phoneNumbers: List<PhoneData> = emptyList(),
    val emails: List<EmailData> = emptyList(),
    val addresses: List<AddressData> = emptyList()
)

data class PhoneData(
    val number: String,
    val type: String
)

data class EmailData(
    val email: String,
    val type: String
)

data class AddressData(
    val formattedAddress: String,
    val street: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val type: String
)

/**
 * Parses all data from a contact URI
 */
private fun parseContactData(
    context: android.content.Context,
    contactUri: android.net.Uri
): ContactData? {
    try {
        val contentResolver = context.contentResolver
        var displayName = ""
        val phoneNumbers = mutableListOf<PhoneData>()
        val emails = mutableListOf<EmailData>()
        val addresses = mutableListOf<AddressData>()

        // Get contact ID first
        val contactCursor = contentResolver.query(
            contactUri,
            arrayOf(android.provider.ContactsContract.Contacts._ID, android.provider.ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )

        contactCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)

                val contactId = if (idIndex >= 0) cursor.getString(idIndex) else null
                displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""

                // Get phone numbers
                if (contactId != null) {
                    val phoneCursor = contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )

                    phoneCursor?.use { pc ->
                        while (pc.moveToNext()) {
                            val numberIndex = pc.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val typeIndex = pc.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE)

                            if (numberIndex >= 0) {
                                val number = pc.getString(numberIndex)
                                val type = if (typeIndex >= 0) {
                                    when (pc.getInt(typeIndex)) {
                                        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "MOBILE"
                                        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
                                        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
                                        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
                                        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "FAX"
                                        else -> "OTHER"
                                    }
                                } else "OTHER"

                                phoneNumbers.add(PhoneData(number, type))
                            }
                        }
                    }

                    // Get emails
                    val emailCursor = contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )

                    emailCursor?.use { ec ->
                        while (ec.moveToNext()) {
                            val emailIndex = ec.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.ADDRESS)
                            val typeIndex = ec.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.TYPE)

                            if (emailIndex >= 0) {
                                val email = ec.getString(emailIndex)
                                val type = if (typeIndex >= 0) {
                                    when (ec.getInt(typeIndex)) {
                                        android.provider.ContactsContract.CommonDataKinds.Email.TYPE_HOME -> "HOME"
                                        android.provider.ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "WORK"
                                        else -> "OTHER"
                                    }
                                } else "OTHER"

                                emails.add(EmailData(email, type))
                            }
                        }
                    }

                    // Get addresses
                    val addressCursor = contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null,
                        "${android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )

                    addressCursor?.use { ac ->
                        while (ac.moveToNext()) {
                            val formattedIndex = ac.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                            val streetIndex = ac.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.STREET)
                            val cityIndex = ac.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CITY)
                            val postalIndex = ac.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
                            val countryIndex = ac.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
                            val typeIndex = ac.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE)

                            val formattedAddress = if (formattedIndex >= 0) ac.getString(formattedIndex) else null
                            if (formattedAddress != null) {
                                val type = if (typeIndex >= 0) {
                                    when (ac.getInt(typeIndex)) {
                                        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> "HOME"
                                        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> "WORK"
                                        else -> "OTHER"
                                    }
                                } else "OTHER"

                                addresses.add(
                                    AddressData(
                                        formattedAddress = formattedAddress,
                                        street = if (streetIndex >= 0) ac.getString(streetIndex) else null,
                                        city = if (cityIndex >= 0) ac.getString(cityIndex) else null,
                                        postalCode = if (postalIndex >= 0) ac.getString(postalIndex) else null,
                                        country = if (countryIndex >= 0) ac.getString(countryIndex) else null,
                                        type = type
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return if (displayName.isNotBlank()) {
            ContactData(displayName, phoneNumbers, emails, addresses)
        } else null

    } catch (e: Exception) {
        android.util.Log.e("ImportContactDialog", "Error parsing contact", e)
        return null
    }
}
