package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActionLog
import com.example.data.model.Customer
import com.example.data.model.Measurement
import com.example.data.model.ProductModel
import com.example.ui.viewmodel.MeasurementsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MeasurementsViewModel) {
    val context = LocalContext.current
    
    // Force RTL globally for perfect Arabic (ar-YE) UX
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        
        var currentTab by remember { mutableStateOf("home") } // "home", "archive", "history", "settings"
        val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        
        var showCustomerOverview by remember { mutableStateOf(false) }

        LaunchedEffect(selectedCustomer) {
            if (selectedCustomer == null) {
                showCustomerOverview = false
            }
        }

        // Navigation stack simulation: if a customer is selected, show "measurements" view or details overview
        val activeScreen = if (selectedCustomer != null) {
            if (showCustomerOverview) "customer_overview" else "measurements"
        } else {
            currentTab
        }

        // Dialog states
        var showAddCustomerDialog by remember { mutableStateOf(false) }
        var showEditCustomerDialog by remember { mutableStateOf<Customer?>(null) }
        var showAddMeasurementDialog by remember { mutableStateOf(false) }
        var showEditMeasurementDialog by remember { mutableStateOf<Measurement?>(null) }
        var showShareDialog by remember { mutableStateOf<List<Measurement>?>(null) }
        var showMoveMeasurementDialog by remember { mutableStateOf<Measurement?>(null) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var showImportDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (activeScreen == "home" || activeScreen == "archive") {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        text = if (activeScreen == "home") "بحث في العملاء..." else "بحث في الأرشيف...",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "بحث",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "مسح",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            )
                        } else {
                            Text(
                                text = when (activeScreen) {
                                    "customer_overview" -> "بيانات العميل: ${selectedCustomer?.name}"
                                    "measurements" -> selectedCustomer?.name ?: ""
                                    "history" -> "سجل النشاط والتحديثات"
                                    "settings" -> "الإعدادات العامة"
                                    else -> "دفتر القياسات"
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    navigationIcon = {
                        if (activeScreen != "home") {
                            IconButton(onClick = {
                                if (activeScreen == "customer_overview") {
                                    showCustomerOverview = false
                                } else if (activeScreen == "measurements") {
                                    viewModel.selectCustomer(null)
                                } else {
                                    currentTab = "home"
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    },
                    actions = {
                        if (activeScreen == "home") {
                            var showMenu by remember { mutableStateOf(false) }
                            
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "القائمة",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("الأرشيف", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        currentTab = "archive"
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("سجل النشاط", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        currentTab = "history"
                                        showMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        currentTab = "settings"
                                        showMenu = false
                                    }
                                )
                            }
                        } else if (activeScreen == "measurements") {
                            var showMenu by remember { mutableStateOf(false) }
                            
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "القائمة",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بيانات العميل", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showCustomerOverview = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                when (activeScreen) {
                    "home" -> {
                        ExtendedFloatingActionButton(
                            onClick = { showAddCustomerDialog = true },
                            icon = { Icon(Icons.Default.PersonAdd, "إضافة عميل") },
                            text = { Text("عميل جديد", fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_customer_fab")
                        )
                    }
                    "measurements" -> {
                        ExtendedFloatingActionButton(
                            onClick = { showAddMeasurementDialog = true },
                            icon = { Icon(Icons.Default.AddCircle, "إضافة قياس") },
                            text = { Text("قياس جديد", fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_measurement_fab")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    )
            ) {
                Crossfade(targetState = activeScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        "home" -> CustomersTab(
                            viewModel = viewModel,
                            isArchive = false,
                            onCustomerClick = { viewModel.selectCustomer(it) },
                            onEditCustomer = { showEditCustomerDialog = it }
                        )
                        "archive" -> CustomersTab(
                            viewModel = viewModel,
                            isArchive = true,
                            onCustomerClick = { viewModel.selectCustomer(it) },
                            onEditCustomer = { showEditCustomerDialog = it }
                        )
                        "measurements" -> MeasurementsScreenContent(
                            viewModel = viewModel,
                            onAddMeasurementClick = { showAddMeasurementDialog = true },
                            onEditMeasurement = { showEditMeasurementDialog = it },
                            onShareClick = { showShareDialog = it },
                            onMoveMeasurement = { showMoveMeasurementDialog = it }
                        )
                        "customer_overview" -> CustomerOverviewScreenContent(
                            viewModel = viewModel,
                            onShareClick = { showShareDialog = it }
                        )
                        "history" -> HistoryTab(viewModel = viewModel)
                        "settings" -> SettingsTab(
                            viewModel = viewModel,
                            onImportClick = { showImportDialog = true }
                        )
                    }
                }
            }
        }

        // --- All Application Dialogs ---

        // 1. Add Customer Dialog
        if (showAddCustomerDialog) {
            AddEditCustomerDialog(
                customer = null,
                onDismiss = { showAddCustomerDialog = false },
                onSave = { name, phone, address, note ->
                    viewModel.addCustomer(name, phone, address, note,
                        onSuccess = {
                            showAddCustomerDialog = false
                            Toast.makeText(context, "تمت إضافة العميل بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        // 2. Edit Customer Dialog
        showEditCustomerDialog?.let { customer ->
            AddEditCustomerDialog(
                customer = customer,
                onDismiss = { showEditCustomerDialog = null },
                onSave = { name, phone, address, note ->
                    viewModel.updateCustomer(customer, name, phone, address, note,
                        onSuccess = {
                            showEditCustomerDialog = null
                            Toast.makeText(context, "تم تحديث بيانات العميل", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        // 3. Add Measurement Dialog
        if (showAddMeasurementDialog) {
            selectedCustomer?.let { customer ->
                AddEditMeasurementDialog(
                    viewModel = viewModel,
                    measurement = null,
                    customerId = customer.id,
                    customerName = customer.name,
                    onDismiss = { showAddMeasurementDialog = false },
                    onSave = { modelId, modelName, values ->
                        viewModel.addMeasurement(customer.id, customer.name, modelId, modelName, values) {
                            showAddMeasurementDialog = false
                            Toast.makeText(context, "تم حفظ القياس بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // 4. Edit Measurement Dialog
        showEditMeasurementDialog?.let { measurement ->
            selectedCustomer?.let { customer ->
                AddEditMeasurementDialog(
                    viewModel = viewModel,
                    measurement = measurement,
                    customerId = customer.id,
                    customerName = customer.name,
                    onDismiss = { showEditMeasurementDialog = null },
                    onSave = { _, _, values ->
                        viewModel.updateMeasurement(measurement, customer.name, values) {
                            showEditMeasurementDialog = null
                            Toast.makeText(context, "تم تعديل القياس", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // 5. Share Options Dialog
        showShareDialog?.let { measurements ->
            ShareOptionsDialog(
                onDismiss = { showShareDialog = null },
                onShareText = { withInfo ->
                    viewModel.shareAsText(selectedCustomer, measurements, withInfo)
                    showShareDialog = null
                },
                onSharePdf = { withInfo ->
                    viewModel.shareAsPdf(selectedCustomer, measurements, withInfo)
                    showShareDialog = null
                }
            )
        }

        // 6. Move Measurement Dialog (re-assign measurement to another customer)
        showMoveMeasurementDialog?.let { measurement ->
            MoveMeasurementDialog(
                viewModel = viewModel,
                measurement = measurement,
                onDismiss = { showMoveMeasurementDialog = null },
                onMoved = { targetCustomer ->
                    viewModel.deleteMeasurement(measurement, selectedCustomer?.name ?: "")
                    viewModel.addMeasurement(
                        customerId = targetCustomer.id,
                        customerName = targetCustomer.name,
                        productModelId = measurement.productModelId,
                        productName = measurement.productName,
                        values = viewModel.parseValues(measurement.fieldsValueJson)
                    ) {
                        showMoveMeasurementDialog = null
                        Toast.makeText(context, "تم نقل القياس بنجاح للعميل ${targetCustomer.name}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // 7. About App Dialog
        if (showAboutDialog) {
            AboutAppDialog(onDismiss = { showAboutDialog = false })
        }

        // 8. Import JSON Backup Dialog
        if (showImportDialog) {
            ImportBackupDialog(
                onDismiss = { showImportDialog = false },
                onImport = { json ->
                    viewModel.restoreLocalBackup(json,
                        onSuccess = {
                            showImportDialog = false
                            Toast.makeText(context, "تم استعادة النسخة الاحتياطية بنجاح!", Toast.LENGTH_LONG).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }
}

// ==========================================
// --- SCREEN TABS & SUB-COMPOSABLES ---
// ==========================================

@Composable
fun CustomersTab(
    viewModel: MeasurementsViewModel,
    isArchive: Boolean,
    onCustomerClick: (Customer) -> Unit,
    onEditCustomer: (Customer) -> Unit
) {
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()

    // Filter and Sort local computation for extreme responsiveness
    val filteredCustomers = remember(customers, searchQuery, isArchive, sortBy) {
        customers
            .filter { it.isArchived == isArchive }
            .filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
            .sortedWith { c1, c2 ->
                if (sortBy == "NAME") {
                    c1.name.compareTo(c2.name)
                } else {
                    c2.creationDate.compareTo(c1.creationDate) // newer first
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "العدد الإجمالي: ${filteredCustomers.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("الترتيب: ", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = sortBy == "DATE",
                            onClick = { viewModel.updateSortBy("DATE") },
                            label = { Text("الأحدث") }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = sortBy == "NAME",
                            onClick = { viewModel.updateSortBy("NAME") },
                            label = { Text("الاسم") }
                        )
                    }
                }
            }
        }

        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = if (isArchive) Icons.Default.Inventory else Icons.Default.PeopleOutline,
                        contentDescription = "قائمة فارغة",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة لبحثك" else if (isArchive) "الأرشيف فارغ حالياً" else "دفتر قياساتك فارغ. أضف أول عميل للبدء!",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    CustomerListItem(
                        customer = customer,
                        onClick = { onCustomerClick(customer) },
                        onEdit = { onEditCustomer(customer) },
                        onArchiveToggle = {
                            if (customer.isArchived) {
                                viewModel.unarchiveCustomer(customer)
                            } else {
                                viewModel.archiveCustomer(customer)
                            }
                        },
                        onDelete = { viewModel.deleteCustomer(customer) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerListItem(
    customer: Customer,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "هاتف",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (customer.address.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "عنوان",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = customer.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Actions & Dropdown Menu
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "خيارات")
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("تعديل البيانات") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            expandedMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (customer.isArchived) "إلغاء الأرشفة" else "نقل للأرشيف") },
                        leadingIcon = { Icon(if (customer.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) },
                        onClick = {
                            expandedMenu = false
                            onArchiveToggle()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("حذف النهائي", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerOverviewScreenContent(
    viewModel: MeasurementsViewModel,
    onShareClick: (List<Measurement>) -> Unit
) {
    val measurements by viewModel.measurementsForSelectedCustomer.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        selectedCustomer?.let { customer ->
            val context = LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بيانات الاتصال والتوجيهات",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Actions group
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Share action
                            IconButton(
                                onClick = { onShareClick(measurements) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "مشاركة الكل",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Contact details list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (customer.phoneNumber.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "رقم الهاتف",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الهاتف: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = customer.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                try {
                                                    val intent = android.content.Intent(
                                                        android.content.Intent.ACTION_DIAL,
                                                        android.net.Uri.parse("tel:${customer.phoneNumber}")
                                                    )
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "لا يمكن إجراء المكالمة", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                // Quick action buttons for phone: Call and WhatsApp!
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledIconButton(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_DIAL,
                                                    android.net.Uri.parse("tel:${customer.phoneNumber}")
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "لا يمكن إجراء المكالمة", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "اتصال", modifier = Modifier.size(14.dp))
                                    }
                                    
                                    FilledIconButton(
                                        onClick = {
                                            try {
                                                val cleanPhone = customer.phoneNumber.replace("+", "").replace(" ", "")
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "فشل فتح الواتساب", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF25D366).copy(alpha = 0.15f),
                                            contentColor = Color(0xFF128C7E)
                                        ),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Message, contentDescription = "واتساب", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        if (customer.address.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "العنوان",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "العنوان: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Text(
                                    text = customer.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (customer.note.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = "ملاحظة",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "ملاحظات وتوجيهات خاصة:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = customer.note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementsScreenContent(
    viewModel: MeasurementsViewModel,
    onAddMeasurementClick: () -> Unit,
    onEditMeasurement: (Measurement) -> Unit,
    onShareClick: (List<Measurement>) -> Unit,
    onMoveMeasurement: (Measurement) -> Unit
) {
    val measurements by viewModel.measurementsForSelectedCustomer.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val models by viewModel.allProductModels.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (measurements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Straighten,
                        contentDescription = "لا توجد قياسات",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("لا توجد أي قياسات مسجلة لهذا العميل حالياً.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onAddMeasurementClick) {
                        Text("إضافة أول قياس")
                    }
                }
            }
        } else {
            // Group and sort data tables by Group/Product Type
            val groupedMeasurements = remember(measurements) {
                measurements.groupBy { it.productName }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp)
            ) {
                groupedMeasurements.forEach { (groupName, mList) ->
                    item {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                        
                        mList.forEach { measurement ->
                            val values = remember(measurement.fieldsValueJson) { viewModel.parseValues(measurement.fieldsValueJson) }
                            
                            // Map values to individual product models
                            val productTables = remember(models, values) {
                                models.mapNotNull { model ->
                                    val modelFields = viewModel.parseFields(model.fieldsJson)
                                    val modelValues = values.filterKeys { modelFields.contains(it) }
                                    if (modelValues.isNotEmpty()) {
                                        model to modelValues
                                    } else {
                                        null
                                    }
                                }
                            }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header with Date & Actions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val format = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
                                        Text(
                                            text = "تاريخ القياس: ${format.format(Date(measurement.creationDate))}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onShareClick(listOf(measurement)) }) {
                                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.secondary)
                                            }
                                            IconButton(onClick = { onEditMeasurement(measurement) }) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            Box {
                                                var showOptions by remember { mutableStateOf(false) }
                                                IconButton(onClick = { showOptions = true }) {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "المزيد")
                                                }
                                                DropdownMenu(expanded = showOptions, onDismissRequest = { showOptions = false }) {
                                                    DropdownMenuItem(
                                                        text = { Text("نقل إلى عميل آخر") },
                                                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                                                        onClick = {
                                                            showOptions = false
                                                            onMoveMeasurement(measurement)
                                                        }
                                                    )
                                                    HorizontalDivider()
                                                    DropdownMenuItem(
                                                        text = { Text("حذف القياس", color = MaterialTheme.colorScheme.error) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            showOptions = false
                                                            viewModel.deleteMeasurement(measurement, selectedCustomer?.name ?: "")
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Products displayed separately inside the card
                                    productTables.forEach { (model, modelValues) ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            // Product Name Header/Badge
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = model.name,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                            
                                            // Data Table
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .horizontalScroll(rememberScrollState())
                                            ) {
                                                Column {
                                                    // Header Row
                                                    Row(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                                            .padding(vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        modelValues.toList().forEachIndexed { index, (field, _) ->
                                                            if (index > 0) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .width(1.dp)
                                                                        .height(16.dp)
                                                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                                                )
                                                            }
                                                            Text(
                                                                text = field,
                                                                modifier = Modifier
                                                                    .width(90.dp)
                                                                    .padding(horizontal = 6.dp),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                    
                                                    // Value Row
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        modelValues.toList().forEachIndexed { index, (_, value) ->
                                                            if (index > 0) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .width(1.dp)
                                                                        .height(16.dp)
                                                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                                                )
                                                            }
                                                            Text(
                                                                text = value.ifEmpty { "-" },
                                                                modifier = Modifier
                                                                    .width(90.dp)
                                                                    .padding(horizontal = 6.dp),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Fallback if no matching product models found
                                    if (productTables.isEmpty() && values.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .horizontalScroll(rememberScrollState())
                                            ) {
                                                Column {
                                                    // Header Row
                                                    Row(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                                                            .padding(vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        values.toList().forEachIndexed { index, (field, _) ->
                                                            if (index > 0) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .width(1.dp)
                                                                        .height(16.dp)
                                                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                                                )
                                                            }
                                                            Text(
                                                                text = field,
                                                                modifier = Modifier
                                                                    .width(90.dp)
                                                                    .padding(horizontal = 6.dp),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                    
                                                    // Value Row
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        values.toList().forEachIndexed { index, (_, value) ->
                                                            if (index > 0) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .width(1.dp)
                                                                        .height(16.dp)
                                                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                                                )
                                                            }
                                                            Text(
                                                                text = value.ifEmpty { "-" },
                                                                modifier = Modifier
                                                                    .width(90.dp)
                                                                    .padding(horizontal = 6.dp),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center,
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductModelsTab(viewModel: MeasurementsViewModel) {
    val models by viewModel.allProductModels.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<ProductModel?>(null) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("النماذج والمنتجات المتاحة حالياً", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (models.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد نماذج قياس مضافة حالياً. اضغط على الزر لإضافة نموذج.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(models) { model ->
                        val fields = remember(model.fieldsJson) { viewModel.parseFields(model.fieldsJson) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = model.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(onClick = { editingModel = model }) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل النموذج", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.deleteProductModel(model) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف النموذج", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "حقول القياس: " + fields.joinToString(" ، "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, "إضافة نموذج") },
            text = { Text("نموذج جديد", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_model_fab")
        )
    }

    if (showAddDialog) {
        AddProductModelDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, fields ->
                viewModel.addProductModel(
                    name,
                    fields,
                    onSuccess = {
                        showAddDialog = false
                        Toast.makeText(context, "تم حفظ النموذج الجديد بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (editingModel != null) {
        val initialFields = remember(editingModel) { viewModel.parseFields(editingModel!!.fieldsJson) }
        AddProductModelDialog(
            initialName = editingModel!!.name,
            initialFields = initialFields,
            isEdit = true,
            onDismiss = { editingModel = null },
            onSave = { name, fields ->
                viewModel.updateProductModel(
                    editingModel!!,
                    name,
                    fields,
                    onSuccess = {
                        editingModel = null
                        Toast.makeText(context, "تم تعديل النموذج بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun HistoryTab(viewModel: MeasurementsViewModel) {
    val logs by viewModel.allActionLogs.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("سجل النشاط والتعديلات الكامل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد أي أنشطة مسجلة حالياً.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = when (log.actionType) {
                                            "CREATE" -> Color(0xFFE8F5E9)
                                            "UPDATE", "MEASURE_UPDATE" -> Color(0xFFFFF3E0)
                                            "ARCHIVE" -> Color(0xFFECEFF1)
                                            "DELETE", "MEASURE_DELETE" -> Color(0xFFFFEBEE)
                                            else -> Color(0xFFE3F2FD)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (log.actionType) {
                                        "CREATE" -> Icons.Default.PersonAdd
                                        "UPDATE" -> Icons.Default.Edit
                                        "ARCHIVE" -> Icons.Default.Archive
                                        "UNARCHIVE" -> Icons.Default.Unarchive
                                        "MEASURE_CREATE" -> Icons.Default.AddCircle
                                        "MEASURE_UPDATE" -> Icons.Default.EditCalendar
                                        "MEASURE_DELETE" -> Icons.Default.DeleteForever
                                        else -> Icons.Default.History
                                    },
                                    contentDescription = null,
                                    tint = when (log.actionType) {
                                        "CREATE" -> Color(0xFF2E7D32)
                                        "UPDATE", "MEASURE_UPDATE" -> Color(0xFFEF6C00)
                                        "ARCHIVE" -> Color(0xFF37474F)
                                        "DELETE", "MEASURE_DELETE" -> Color(0xFFC62828)
                                        else -> Color(0xFF1565C0)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("العميل: ${log.customerName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            val format = SimpleDateFormat("HH:mm yyyy/MM/dd", Locale("ar"))
                            Text(
                                text = format.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: MeasurementsViewModel,
    onImportClick: () -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("الحساب والنسخ", "نماذج القياسات", "عن التطبيق")
    val tabIcons = listOf(Icons.Default.AccountCircle, Icons.Default.Style, Icons.Default.Info)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(tabIcons[index], contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedSubTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
                        val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (userInfo != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userInfo?.name?.take(1) ?: "",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(userInfo?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(userInfo?.email ?: "", style = MaterialTheme.typography.bodyMedium)
                                    userInfo?.phoneNumber?.let {
                                        if (it.isNotEmpty()) {
                                            Text("رقم الهاتف: $it", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
                                    Text("تاريخ إنشاء الحساب: ${format.format(Date(userInfo?.creationDate ?: 0))}", style = MaterialTheme.typography.bodySmall)

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { viewModel.signOutGoogle() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Logout, "تسجيل الخروج")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تسجيل الخروج من حساب Google")
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "الملف الشخصي",
                                        modifier = Modifier.size(72.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("لم تقم بتسجيل الدخول بعد", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("قم بربط حسابك لحفظ قياساتك ومزامنتها على السحابة بنقرة زر واحدة.", textAlign = TextAlign.Center, fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { viewModel.signInGoogle() }
                                    ) {
                                        Icon(Icons.Default.Login, "دخول")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تسجيل الدخول السريع بـ Google")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("مظهر التطبيق", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val options = listOf("system" to "تلقائي", "light" to "مضيء", "dark" to "مظلم")
                                options.forEach { (key, label) ->
                                    val isSelected = themeSetting == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateThemeSetting(key) },
                                        label = { Text(label, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("النسخ الاحتياطي ومزامنة البيانات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                backupStatus?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.makeLocalBackup() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إنشاء نسخة محلية", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = onImportClick,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("استعادة نسخة", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { viewModel.syncGoogleDrive() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("المزامنة مع Google Drive السحابي")
                                }
                            }
                        }
                    }
                }
                1 -> {
                    ProductModelsTab(viewModel = viewModel)
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("دفتر القياسات اليمني المطور", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("الإصدار المطور v2.1.0", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "تطبيق متكامل لورش الخياطة وتصميم الأزياء لإدارة بيانات العملاء والقياسات وحفظها ومزامنتها على السحابة مع دعم كامل للنماذج المخصصة والطباعة والمشاركة كملفات PDF.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("مميزات التطبيق", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• إضافة عملاء وتصنيفهم مع الأرشفة السريعة", style = MaterialTheme.typography.bodyMedium)
                                Text("• إنشاء نماذج قياس مخصصة لمختلف أنواع الملابس", style = MaterialTheme.typography.bodyMedium)
                                Text("• مزامنة سحابية تلقائية مع Google Drive", style = MaterialTheme.typography.bodyMedium)
                                Text("• تصدير ومشاركة القياسات بصيغة PDF أنيقة ومطورة", style = MaterialTheme.typography.bodyMedium)
                                Text("• العمل بدون اتصال بالإنترنت وحفظ محلي آمن", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// --- DETAILED DIALOGS IMPLEMENTATION ---
// ==========================================

@Composable
fun AddProductModelDialog(
    initialName: String = "",
    initialFields: List<String> = emptyList(),
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val tempFields = remember {
        val list = mutableStateListOf<String>()
        list.addAll(initialFields)
        list
    }
    var currentFieldInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "تعديل نموذج القياس" else "إضافة نموذج قياس مخصص جديد") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المنتج (مثال: جلابية، ثوب يمني)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = currentFieldInput,
                        onValueChange = { currentFieldInput = it },
                        label = { Text("أضف حقل قياس (مثال: الصدر، الطول)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (currentFieldInput.trim().isNotEmpty()) {
                                tempFields.add(currentFieldInput.trim())
                                currentFieldInput = ""
                            }
                        }
                    ) {
                        Text("إضافة")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("الحقول المضافة للنموذج الجديد: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (tempFields.isEmpty()) {
                        Text("لم يتم إضافة أي حقل بعد", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        tempFields.forEach { field ->
                            SuggestionChip(
                                onClick = { tempFields.remove(field) },
                                label = { Text(field) },
                                icon = { Icon(Icons.Default.Close, contentDescription = "إزالة", modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty() && tempFields.isNotEmpty()) {
                        onSave(name.trim(), tempFields.toList())
                    }
                },
                enabled = name.trim().isNotEmpty() && tempFields.isNotEmpty()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AddEditCustomerDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phoneNumber ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var note by remember { mutableStateOf(customer?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "إضافة عميل جديد" else "تعديل بيانات العميل") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل بالكامل") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text("رقم الهاتف (9 أرقام فقط)")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات أخرى (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, phone, address, note) }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AddEditMeasurementDialog(
    viewModel: MeasurementsViewModel,
    measurement: Measurement?,
    customerId: Int,
    customerName: String,
    onDismiss: () -> Unit,
    onSave: (Int, String, Map<String, String>) -> Unit
) {
    val models by viewModel.allProductModels.collectAsStateWithLifecycle()
    
    // Support selecting multiple models
    var selectedModels by remember {
        mutableStateOf<List<ProductModel>>(emptyList())
    }

    // Support custom group name
    var customGroupName by remember {
        mutableStateOf(measurement?.productName ?: "")
    }

    // Set initial models once list of models is loaded
    LaunchedEffect(models, measurement) {
        if (models.isNotEmpty() && selectedModels.isEmpty()) {
            selectedModels = if (measurement != null) {
                val loadedValues = viewModel.parseValues(measurement.fieldsValueJson)
                models.filter { model ->
                    measurement.productName.contains(model.name) || 
                    viewModel.parseFields(model.fieldsJson).any { loadedValues.containsKey(it) }
                }
            } else {
                listOfNotNull(models.firstOrNull())
            }
        }
    }

    // Dynamic fields values state
    val valuesMap = remember { mutableStateMapOf<String, String>() }

    // Combined fields from all selected models
    val combinedFields = remember(selectedModels) {
        selectedModels.flatMap { model ->
            viewModel.parseFields(model.fieldsJson)
        }.distinct()
    }

    LaunchedEffect(selectedModels, measurement) {
        if (selectedModels.isNotEmpty()) {
            if (measurement != null && valuesMap.isEmpty()) {
                val loadedValues = viewModel.parseValues(measurement.fieldsValueJson)
                valuesMap.putAll(loadedValues)
            }
            
            // Add any missing fields as empty string
            combinedFields.forEach { field ->
                if (!valuesMap.containsKey(field)) {
                    valuesMap[field] = ""
                }
            }
            
            // Remove fields that are not in combined fields anymore
            val keysToRemove = valuesMap.keys.filter { !combinedFields.contains(it) }
            keysToRemove.forEach { valuesMap.remove(it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (measurement == null) "تسجيل قياس جديد" else "تعديل القياس") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Group Name Input
                OutlinedTextField(
                    value = customGroupName,
                    onValueChange = { customGroupName = it },
                    label = { Text("اسم المجموعة (مثال: تفصيل العيد، قياسات الشتاء...)") },
                    placeholder = {
                        val defaultPlaceholder = if (selectedModels.isNotEmpty()) {
                            selectedModels.joinToString(" + ") { it.name }
                        } else {
                            "المجموعة الافتراضية"
                        }
                        Text(defaultPlaceholder)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("اختر المنتجات / الملابس المراد قياسها (يمكنك اختيار أكثر من منتج):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    models.forEach { model ->
                        val isSelected = selectedModels.any { it.id == model.id }
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    if (isSelected) {
                                        if (selectedModels.size > 1) {
                                            selectedModels = selectedModels.filter { it.id != model.id }
                                        }
                                    } else {
                                        selectedModels = selectedModels + model
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = model.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("أدخل تفاصيل المقاسات لكل منتج:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    selectedModels.forEach { model ->
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = model.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        val modelFields = viewModel.parseFields(model.fieldsJson)
                        items(modelFields) { field ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(field, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    OutlinedTextField(
                                        value = valuesMap[field] ?: "",
                                        onValueChange = { valuesMap[field] = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedModels.isNotEmpty()) {
                        val firstModelId = selectedModels.first().id
                        val finalGroupName = customGroupName.trim().ifEmpty {
                            selectedModels.joinToString(" + ") { it.name }
                        }
                        onSave(firstModelId, finalGroupName, valuesMap.toMap())
                    }
                },
                enabled = selectedModels.isNotEmpty()
            ) {
                Text("حفظ القياسات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ShareOptionsDialog(
    onDismiss: () -> Unit,
    onShareText: (Boolean) -> Unit,
    onSharePdf: (Boolean) -> Unit
) {
    var withFullInfo by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("خيارات مشاركة القياسات") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("يرجى تحديد خيارات الخصوصية والمشاركة أدناه:")
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = withFullInfo, onCheckedChange = { withFullInfo = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تضمين كامل تفاصيل العميل (الاسم والهاتف)")
                }
                if (!withFullInfo) {
                    Text("سيتم تضمين رمز العميل التعريفي (ID) فقط لتأمين السرية.", fontSize = 11.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onShareText(withFullInfo) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة كـ نص عادي (WhatsApp، إلخ)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onSharePdf(withFullInfo) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة كـ ملف PDF احترافي")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun MoveMeasurementDialog(
    viewModel: MeasurementsViewModel,
    measurement: Measurement,
    onDismiss: () -> Unit,
    onMoved: (Customer) -> Unit
) {
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val otherCustomers = remember(customers, measurement) {
        customers.filter { it.id != measurement.customerId && !it.isArchived }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نقل قياس إلى عميل آخر") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("القياس الحالي: ${measurement.productName}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("اختر العميل المستهدف لنقل هذا القياس إليه:")
                Spacer(modifier = Modifier.height(12.dp))

                if (otherCustomers.isEmpty()) {
                    Text("لا يوجد عملاء متاحين آخرين حالياً.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(otherCustomers) { customer ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onMoved(customer) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(customer.name, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دفتر القياسات 🧵") },
        text = {
            Column {
                Text(
                    text = "تطبيق متكامل مصمم خصيصاً للخياطين والمصممين لتنظيم وتتبع قياسات زبائنهم بسهولة وسرية تامة.",
                    textAlign = TextAlign.Justify
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("الميزات الرئيسية:", fontWeight = FontWeight.Bold)
                Text("• تسيير كامل للعملاء النشطين وأرشفتهم.")
                Text("• إنشاء نماذج تفصيلية مخصصة لكل منتج.")
                Text("• مشاركة وتصدير البيانات كـ نصوص أو ملفات PDF.")
                Text("• حماية وتأمين الخصوصية والبيانات للزبائن.")
                Text("• نظام نسخ احتياطي محلي وسحابي على Google Drive.")
                Spacer(modifier = Modifier.height(12.dp))
                Text("النسخة: 1.0.0 (الجمهورية اليمنية 🇾🇪)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("شكراً")
            }
        }
    )
}

@Composable
fun ImportBackupDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("استعادة البيانات") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("ألصق محتويات ملف النسخة الاحتياطية (تنسيق JSON) أدناه لاستعادة كافة بياناتك فوراً:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("محتويات ملف النسخة الاحتياطية (JSON)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(jsonText) },
                enabled = jsonText.trim().isNotEmpty()
            ) {
                Text("استعادة الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
