package com.example.questflow.presentation.screens.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.questflow.data.database.entity.*
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.viewmodels.MetadataLibraryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: MetadataLibraryViewModel = hiltViewModel(),
    todayViewModel: com.example.questflow.presentation.viewmodels.TodayViewModel = hiltViewModel(),
    calendarViewModel: com.example.questflow.presentation.screens.calendar.CalendarXpViewModel = hiltViewModel()
) {
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()
    var previousXp by remember { mutableStateOf(globalStats?.xp ?: 0L) }

    // Load contact data
    val contacts by viewModel.contacts.collectAsState()
    val contact = contacts.find { it.id == contactId }

    // State for editing a task
    var selectedEditLink by remember { mutableStateOf<com.example.questflow.data.database.entity.CalendarEventLinkEntity?>(null) }

    LaunchedEffect(globalStats?.xp) {
        globalStats?.xp?.let { currentXp ->
            if (currentXp != previousXp) {
                previousXp = currentXp
            }
        }
    }

    // Load related data via ViewModel
    LaunchedEffect(contactId) {
        viewModel.loadContactDetails(contactId)
    }

    val phones by viewModel.contactPhones.collectAsState()
    val emails by viewModel.contactEmails.collectAsState()
    val addresses by viewModel.contactAddresses.collectAsState()
    val linkedTasks by viewModel.linkedTasks.collectAsState()

    var showPhoneDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var editingPhone by remember { mutableStateOf<MetadataPhoneEntity?>(null) }
    var editingEmail by remember { mutableStateOf<MetadataEmailEntity?>(null) }
    var editingAddress by remember { mutableStateOf<MetadataAddressEntity?>(null) }

    // Task filtering state
    var taskSearchQuery by remember { mutableStateOf("") }
    var showCompletedTasks by remember { mutableStateOf(true) }
    var showActiveTasks by remember { mutableStateOf(true) }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistable URI permission to keep access after app restart
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                android.util.Log.e("ContactDetailScreen", "Failed to take persistable permission", e)
            }

            contact?.let { c ->
                viewModel.updateContact(c.copy(photoUri = it.toString()))
            }
        }
    }

    Scaffold(
        topBar = {
            QuestFlowTopBar(
                title = contact?.displayName ?: "Kontakt",
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = appViewModel::selectCategory,
                onManageCategoriesClick = { navController.navigate("categories") },
                level = globalStats?.level ?: 1,
                totalXp = globalStats?.xp ?: 0,
                previousXp = previousXp,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (contact == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Kontakt nicht gefunden")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Contact Info Card
                item {
                    ContactInfoCard(
                        contact = contact,
                        onPhotoClick = { photoPickerLauncher.launch(arrayOf("image/*")) }
                    )
                }

                // Tags Section
                item {
                    val tagViewModel: com.example.questflow.presentation.viewmodels.TagViewModel = hiltViewModel()
                    val contactTags by tagViewModel.getTagsForContact(contactId).collectAsState(initial = emptyList())
                    val allContactTags by tagViewModel.getContactTags().collectAsState(initial = emptyList())
                    var showTagSelectionDialog by remember { mutableStateOf(false) }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        com.example.questflow.presentation.components.ContactTagManagementSection(
                            contactId = contactId,
                            assignedTags = contactTags,
                            onAddTagsClick = { showTagSelectionDialog = true },
                            onRemoveTag = { tag ->
                                tagViewModel.removeTagFromContact(contactId, tag.id)
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    if (showTagSelectionDialog) {
                        com.example.questflow.presentation.components.ContactTagSelectionDialog(
                            availableTags = allContactTags,
                            currentlyAssignedTagIds = contactTags.map { it.id }.toSet(),
                            onDismiss = { showTagSelectionDialog = false },
                            onConfirm = { selectedTagIds ->
                                tagViewModel.setContactTags(contactId, selectedTagIds)
                                showTagSelectionDialog = false
                            },
                            onCreateNewTag = { tagName ->
                                tagViewModel.createContactTag(tagName)
                            }
                        )
                    }
                }

                // Phone Numbers Section
                item {
                    SectionHeader(
                        title = "Telefonnummern",
                        icon = Icons.Default.Phone,
                        count = phones.size,
                        onAddClick = {
                            editingPhone = null
                            showPhoneDialog = true
                        }
                    )
                }
                items(phones) { phone ->
                    PhoneItemCard(
                        phone = phone,
                        onEdit = {
                            editingPhone = phone
                            showPhoneDialog = true
                        },
                        onDelete = { viewModel.deletePhone(phone) }
                    )
                }

                // Emails Section
                item {
                    SectionHeader(
                        title = "E-Mails",
                        icon = Icons.Default.Email,
                        count = emails.size,
                        onAddClick = {
                            editingEmail = null
                            showEmailDialog = true
                        }
                    )
                }
                items(emails) { email ->
                    EmailItemCard(
                        email = email,
                        onEdit = {
                            editingEmail = email
                            showEmailDialog = true
                        },
                        onDelete = { viewModel.deleteEmail(email) }
                    )
                }

                // Addresses Section
                item {
                    SectionHeader(
                        title = "Adressen",
                        icon = Icons.Default.Home,
                        count = addresses.size,
                        onAddClick = {
                            editingAddress = null
                            showAddressDialog = true
                        }
                    )
                }
                items(addresses) { address ->
                    AddressItemCard(
                        address = address,
                        onEdit = {
                            editingAddress = address
                            showAddressDialog = true
                        },
                        onDelete = { viewModel.deleteAddress(address) }
                    )
                }

                // Linked Tasks Section
                item {
                    SectionHeader(
                        title = "Verknüpfte Tasks",
                        icon = Icons.Default.CheckCircle,
                        count = linkedTasks.size
                    )
                }

                // Task search and filter
                if (linkedTasks.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = taskSearchQuery,
                            onValueChange = { taskSearchQuery = it },
                            label = { Text("Suchen...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, "Suchen")
                            },
                            trailingIcon = {
                                if (taskSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { taskSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, "Löschen")
                                    }
                                }
                            }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showActiveTasks,
                                onClick = { showActiveTasks = !showActiveTasks },
                                label = { Text("Aktiv") },
                                leadingIcon = if (showActiveTasks) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            FilterChip(
                                selected = showCompletedTasks,
                                onClick = { showCompletedTasks = !showCompletedTasks },
                                label = { Text("Erledigt") },
                                leadingIcon = if (showCompletedTasks) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Filter and display tasks
                val filteredTasks = linkedTasks.filter { task ->
                    val matchesSearch = task.title.contains(taskSearchQuery, ignoreCase = true) ||
                                       (task.description?.contains(taskSearchQuery, ignoreCase = true) == true)
                    val matchesFilter = (task.isCompleted && showCompletedTasks) ||
                                       (!task.isCompleted && showActiveTasks)
                    matchesSearch && matchesFilter
                }

                if (filteredTasks.isEmpty() && linkedTasks.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Keine Tasks gefunden",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(filteredTasks) { task ->
                    TaskItemCard(
                        task = task,
                        onClick = {
                            // Load the calendar link for this task and open edit dialog
                            coroutineScope.launch {
                                val link = calendarViewModel.getLinkByTaskId(task.id)
                                if (link != null) {
                                    selectedEditLink = link
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showPhoneDialog) {
        EditPhoneDialog(
            phone = editingPhone,
            contactId = contactId,
            onDismiss = { showPhoneDialog = false },
            onSave = { phone ->
                if (phone.id == 0L) {
                    viewModel.addPhone(phone)
                } else {
                    viewModel.updatePhone(phone)
                }
            }
        )
    }

    if (showEmailDialog) {
        EditEmailDialog(
            email = editingEmail,
            contactId = contactId,
            onDismiss = { showEmailDialog = false },
            onSave = { email ->
                if (email.id == 0L) {
                    viewModel.addEmail(email)
                } else {
                    viewModel.updateEmail(email)
                }
            }
        )
    }

    if (showAddressDialog) {
        EditAddressDialog(
            address = editingAddress,
            contactId = contactId,
            onDismiss = { showAddressDialog = false },
            onSave = { address ->
                if (address.id == 0L) {
                    viewModel.addAddress(address)
                } else {
                    viewModel.updateAddress(address)
                }
            }
        )
    }

    // Edit Task Dialog
    selectedEditLink?.let { link ->
        com.example.questflow.presentation.screens.calendar.EditCalendarTaskDialog(
            calendarLink = link,
            viewModel = todayViewModel,
            calendarViewModel = calendarViewModel,
            onDismiss = {
                selectedEditLink = null
                // Refresh the linked tasks
                viewModel.loadContactDetails(contactId)
            }
        )
    }
}

@Composable
private fun ContactInfoCard(
    contact: MetadataContactEntity,
    onPhotoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPhotoClick),
                contentAlignment = Alignment.Center
            ) {
                if (contact.photoUri != null) {
                    AsyncImage(
                        model = Uri.parse(contact.photoUri),
                        contentDescription = "Kontaktfoto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Foto auswählen",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "Foto ändern",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onPhotoClick)
            )
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            contact.organization?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            contact.jobTitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    onAddClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "($count)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        onAddClick?.let { addClick ->
            IconButton(onClick = addClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Hinzufügen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PhoneItemCard(
    phone: MetadataPhoneEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phone.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (phone.phoneType) {
                        PhoneType.MOBILE -> "Mobil"
                        PhoneType.HOME -> "Privat"
                        PhoneType.WORK -> "Arbeit"
                        PhoneType.FAX -> "Fax"
                        PhoneType.OTHER -> "Sonstiges"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Bearbeiten"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailItemCard(
    email: MetadataEmailEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = email.emailAddress,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (email.emailType) {
                        EmailType.PERSONAL -> "Privat"
                        EmailType.WORK -> "Arbeit"
                        EmailType.OTHER -> "Sonstiges"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Bearbeiten"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressItemCard(
    address: MetadataAddressEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(address.street)
                        address.houseNumber?.let { append(" $it") }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${address.postalCode} ${address.city}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = address.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (address.addressType) {
                        AddressType.HOME -> "Privat"
                        AddressType.WORK -> "Arbeit"
                        AddressType.BILLING -> "Rechnung"
                        AddressType.SHIPPING -> "Versand"
                        AddressType.OTHER -> "Sonstiges"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Bearbeiten"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    task: com.example.questflow.data.database.TaskEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                task.description?.let {
                    if (it.isNotBlank()) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Star,
                contentDescription = null,
                tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
