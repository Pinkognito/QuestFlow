package com.example.questflow.presentation.screens.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: MetadataLibraryViewModel = hiltViewModel()
) {
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()
    var previousXp by remember { mutableStateOf(globalStats?.xp ?: 0L) }

    // Load contact data
    val contacts by viewModel.contacts.collectAsState()
    val contact = contacts.find { it.id == contactId }

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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
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
                        onPhotoClick = { photoPickerLauncher.launch("image/*") }
                    )
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
                items(linkedTasks) { task ->
                    TaskItemCard(
                        task = task,
                        onClick = { navController.navigate("task_detail/${task.id}") }
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
