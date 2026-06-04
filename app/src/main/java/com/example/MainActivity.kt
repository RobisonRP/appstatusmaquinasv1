package com.example

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Equipment
import com.example.data.EquipmentStatusLog
import com.example.data.EquipmentTab
import com.example.ui.theme.*
import com.example.viewmodel.EquipmentViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RPStatusApp()
            }
        }
    }
}

@Composable
fun RPStatusApp(
    viewModel: EquipmentViewModel = viewModel(
        factory = EquipmentViewModel.provideFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val context = LocalContext.current
    val equipments by viewModel.equipmentsState.collectAsStateWithLifecycle()
    val logs by viewModel.logsState.collectAsStateWithLifecycle()
    val tabs by viewModel.tabsState.collectAsStateWithLifecycle()
    val selectedTabId by viewModel.selectedTabId.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var novoEquipamentoNome by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Dialog control states
    var showAddTabDialog by remember { mutableStateOf(false) }
    var tabActionTarget by remember { mutableStateOf<EquipmentTab?>(null) }
    var renameTabTarget by remember { mutableStateOf<EquipmentTab?>(null) }
    var deleteTabTarget by remember { mutableStateOf<EquipmentTab?>(null) }
    var renameEquipmentTarget by remember { mutableStateOf<Equipment?>(null) }
    var showSelectReportTypeForShare by remember { mutableStateOf(false) }
    var showSelectReportTypeForCopy by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Filtered equipments belong to current tab
    val filteredEquipments = remember(equipments, selectedTabId) {
        equipments.filter { it.tabId == selectedTabId }
    }

    // Modal dialogs rendering
    if (showAddTabDialog) {
        AddTabDialog(
            onDismiss = { showAddTabDialog = false },
            onConfirm = { name -> viewModel.addTab(name) }
        )
    }

    if (showSelectReportTypeForShare) {
        SelectReportTypeDialog(
            onDismiss = { showSelectReportTypeForShare = false },
            onSelectSimple = {
                val activeTabName = tabs.find { it.id == selectedTabId }?.name ?: "Frota"
                val report = generateSimpleReportText(filteredEquipments, activeTabName)
                shareReport(context, report)
            },
            onSelectDetailed = {
                val activeTabName = tabs.find { it.id == selectedTabId }?.name ?: "Frota"
                val report = generateDetailedReportText(filteredEquipments, activeTabName)
                shareReport(context, report)
            }
        )
    }

    if (showSelectReportTypeForCopy) {
        SelectReportTypeDialog(
            onDismiss = { showSelectReportTypeForCopy = false },
            onSelectSimple = {
                val activeTabName = tabs.find { it.id == selectedTabId }?.name ?: "Frota"
                val report = generateSimpleReportText(filteredEquipments, activeTabName)
                copyToClipboard(context, report) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "📊 Relatório Simples copiado para a área de transferência!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onSelectDetailed = {
                val activeTabName = tabs.find { it.id == selectedTabId }?.name ?: "Frota"
                val report = generateDetailedReportText(filteredEquipments, activeTabName)
                copyToClipboard(context, report) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "📊 Relatório Detalhado copiado para a área de transferência!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false },
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope
        )
    }

    tabActionTarget?.let { tab ->
        TabActionOptionsDialog(
            tab = tab,
            onDismiss = { tabActionTarget = null },
            onRenameClick = { renameTabTarget = tab },
            onDeleteClick = { deleteTabTarget = tab }
        )
    }

    renameTabTarget?.let { tab ->
        RenameTabDialog(
            tab = tab,
            onDismiss = { renameTabTarget = null },
            onConfirm = { newName -> viewModel.renameTab(tab.id, newName) }
        )
    }

    renameEquipmentTarget?.let { eq ->
        RenameEquipmentDialog(
            equipment = eq,
            onDismiss = { renameEquipmentTarget = null },
            onConfirm = { newName -> viewModel.updateEquipmentName(eq, newName) }
        )
    }

    deleteTabTarget?.let { tab ->
        ConfirmDeleteTabDialog(
            tab = tab,
            onDismiss = { deleteTabTarget = null },
            onConfirm = { viewModel.deleteTab(tab.id) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding()
        ) {
            val isWideScreen = maxWidth >= 720.dp

            if (isWideScreen) {
                // Wide Screen Side-By-Side Adaptive Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column (Controls & Add Form)
                    Column(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeaderSection(
                            onShare = {
                                if (filteredEquipments.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um equipamento antes de gerar o relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showSelectReportTypeForShare = true
                                }
                            },
                            onCopy = {
                                if (filteredEquipments.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um equipamento antes de gerar o relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showSelectReportTypeForCopy = true
                                }
                            },
                            onSettingsClick = {
                                showSettingsDialog = true
                            }
                        )

                        QuickStatsBlock(equipments = filteredEquipments)

                        AddEquipmentForm(
                            nome = novoEquipamentoNome,
                            onNameChange = { novoEquipamentoNome = it },
                            onAddClick = {
                                if (novoEquipamentoNome.isNotBlank()) {
                                    viewModel.addEquipment(novoEquipamentoNome)
                                    novoEquipamentoNome = ""
                                    focusManager.clearFocus()
                                } else {
                                    Toast.makeText(context, "Digite o nome do equipamento.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // Right Column (Fleet Cards)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TabsRow(
                            tabs = tabs,
                            selectedTabId = selectedTabId,
                            onTabSelect = { viewModel.selectTab(it) },
                            onAddTabClick = { showAddTabDialog = true },
                            onTabLongClick = { tabActionTarget = it }
                        )

                        FleetHeader(count = filteredEquipments.size)

                        if (filteredEquipments.isEmpty()) {
                            EmptyStateBlock(modifier = Modifier.weight(1f))
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 250.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredEquipments, key = { it.id }) { item ->
                                    EquipmentCard(
                                        equipment = item,
                                        onStatusChange = { newStatus ->
                                            viewModel.updateEquipmentStatus(item, newStatus)
                                        },
                                        onObsChange = { newObs ->
                                            viewModel.updateEquipmentObs(item, newObs)
                                        },
                                        onRenameClick = {
                                            renameEquipmentTarget = item
                                        },
                                        onDeleteClick = {
                                            viewModel.deleteEquipment(item)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Compact Screen Scrolling List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HeaderSection(
                            onShare = {
                                if (filteredEquipments.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um equipamento antes de gerar o relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showSelectReportTypeForShare = true
                                }
                            },
                            onCopy = {
                                if (filteredEquipments.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um equipamento antes de gerar o relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showSelectReportTypeForCopy = true
                                }
                            },
                            onSettingsClick = {
                                showSettingsDialog = true
                            }
                        )
                    }

                    item {
                        TabsRow(
                            tabs = tabs,
                            selectedTabId = selectedTabId,
                            onTabSelect = { viewModel.selectTab(it) },
                            onAddTabClick = { showAddTabDialog = true },
                            onTabLongClick = { tabActionTarget = it }
                        )
                    }

                    item {
                        QuickStatsBlock(equipments = filteredEquipments)
                    }

                    item {
                        AddEquipmentForm(
                            nome = novoEquipamentoNome,
                            onNameChange = { novoEquipamentoNome = it },
                            onAddClick = {
                                if (novoEquipamentoNome.isNotBlank()) {
                                    viewModel.addEquipment(novoEquipamentoNome)
                                    novoEquipamentoNome = ""
                                    focusManager.clearFocus()
                                } else {
                                    Toast.makeText(context, "Digite o nome do equipamento.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    item {
                        FleetHeader(count = filteredEquipments.size)
                    }

                    if (filteredEquipments.isEmpty()) {
                        item {
                            EmptyStateBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                        }
                    } else {
                        items(filteredEquipments, key = { it.id }) { item ->
                            EquipmentCard(
                                equipment = item,
                                onStatusChange = { newStatus ->
                                    viewModel.updateEquipmentStatus(item, newStatus)
                                },
                                onObsChange = { newObs ->
                                    viewModel.updateEquipmentObs(item, newObs)
                                },
                                onRenameClick = {
                                    renameEquipmentTarget = item
                                },
                                onDeleteClick = {
                                    viewModel.deleteEquipment(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title & Brand Badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand: RP.Status with styled dot
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "RP",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = ".",
                        color = Indigo400,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Status",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Pulser synchronized badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(color = StatusDisponivel.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp))
                        .border(width = 1.dp, color = StatusDisponivel.copy(alpha = 0.22f), shape = RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    // Small green glow indicator circle
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(color = StatusDisponivel, shape = CircleShape)
                    )
                    Text(
                        text = "Sincronizado",
                        color = StatusDisponivel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Description text
            Text(
                text = "Controle de Disponibilidade e Monitoramento Operacional de Frota",
                color = Slate400,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Sharing buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main Share Button (WhatsApp / Chooser)
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartilhar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Compartilhar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Copy to Clipboard Button
                OutlinedButton(
                    onClick = onCopy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Slate700),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Copiar",
                        tint = Slate100,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copiar",
                        color = Slate100,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Configuration button (Gear icon)
            Button(
                onClick = onSettingsClick,
                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuração",
                    tint = Slate100,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Configuração",
                    color = Slate100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun AddEquipmentForm(
    nome: String,
    onNameChange: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ADICIONAR NOVO EQUIPAMENTO",
                color = Slate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = nome,
                onValueChange = onNameChange,
                placeholder = {
                    Text(
                        text = "Ex: Escavadeira ES218, Pá Carregadeira...",
                        color = Slate600,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedBorderColor = Indigo600,
                    unfocusedBorderColor = Slate700,
                    focusedLabelColor = Indigo600,
                    unfocusedLabelColor = Slate400
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nome_equipamento_input")
            )

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incluir_maquina_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Incluir",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Incluir Máquina",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun QuickStatsBlock(
    equipments: List<Equipment>,
    modifier: Modifier = Modifier
) {
    val totalCount = equipments.size
    val disponivelCount = equipments.count { it.status == "Disponível" }
    val paradaCount = equipments.count { it.status != "Disponível" }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fleet card
        StatCard(
            label = "Frota",
            value = totalCount.toString(),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        // Available card
        StatCard(
            label = "Disp.",
            value = disponivelCount.toString(),
            color = StatusDisponivel,
            modifier = Modifier.weight(1f)
        )
        // Stopped card
        StatCard(
            label = "Parada",
            value = paradaCount.toString(),
            color = StatusManutencao,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = if (color == Color.White) Slate400 else color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun FleetHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Frota Monitorada",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )

        Box(
            modifier = Modifier
                .background(color = Slate800, shape = CircleShape)
                .border(width = 1.dp, color = Slate700, shape = CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = count.toString(),
                color = Slate400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyStateBlock(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(color = Slate800.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = Slate700.copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Vazio",
                tint = Slate600,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "Nenhum equipamento cadastrado no momento.",
                color = Slate400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Use o painel acima para iniciar o monitoramento.",
                color = Slate600,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentCard(
    equipment: Equipment,
    onStatusChange: (String) -> Unit,
    onObsChange: (String) -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when (equipment.status) {
        "Disponível" -> StatusDisponivel
        "Em Manutenção" -> StatusManutencao
        "Preventiva" -> StatusPreventiva
        else -> Slate700
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var obsText by remember(equipment.id) { mutableStateOf(equipment.obs) }

    LaunchedEffect(equipment.obs) {
        if (obsText != equipment.obs) {
            obsText = equipment.obs
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("equipment_card_${equipment.id}")
            .drawWithContent {
                // Draw the card content first
                drawContent()
                // Left border-l-4 style vertical color bar
                val stripeWidth = 5.dp.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(stripeWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Soft glowing status dot
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Surface(
                            color = borderColor.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(16.dp)
                        ) {}
                        Surface(
                            color = borderColor,
                            shape = CircleShape,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }

                    Text(
                        text = equipment.nome,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onRenameClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("rename_equipment_${equipment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Renomear Equipamento",
                            tint = Slate600,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_equipment_${equipment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir Equipamento",
                            tint = Slate600,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Dropdown Status Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "STATUS ATUAL",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Slate900, shape = RoundedCornerShape(12.dp))
                        .border(width = 1.dp, color = Slate700, shape = RoundedCornerShape(12.dp))
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayText = when (equipment.status) {
                            "Disponível" -> "🟢 Disponível"
                            "Em Manutenção" -> "🔴 Em Manutenção"
                            "Preventiva" -> "🟡 Preventiva"
                            else -> equipment.status
                        }
                        Text(
                            text = displayText,
                            color = Slate100,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expandir status",
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .background(color = Slate800)
                            .border(width = 1.dp, color = Slate700, shape = RoundedCornerShape(4.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("🟢 Disponível", color = Slate100) },
                            onClick = {
                                onStatusChange("Disponível")
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔴 Em Manutenção", color = Slate100) },
                            onClick = {
                                onStatusChange("Em Manutenção")
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🟡 Preventiva", color = Slate100) },
                            onClick = {
                                onStatusChange("Preventiva")
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Observations Text Box
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "OBSERVAÇÕES",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = obsText,
                    onValueChange = {
                        obsText = it
                        onObsChange(it)
                    },
                    placeholder = {
                        Text(
                            text = "Notas rápidas (ex: falha mecânica)",
                            color = Slate600,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedContainerColor = Slate900.copy(alpha = 0.6f),
                        unfocusedContainerColor = Slate900.copy(alpha = 0.6f),
                        focusedBorderColor = Slate600,
                        unfocusedBorderColor = Slate800,
                        focusedLabelColor = Slate400,
                        unfocusedLabelColor = Slate600
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("equipment_obs_input_${equipment.id}")
                )
            }
        }
    }
}

// FORMATTING & UTILITY FUNCTIONS
fun generateReportText(equipments: List<Equipment>, tabName: String = "Frota"): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    val currentDateTimeStatus = dateFormat.format(Date())
    
    val sb = java.lang.StringBuilder()
    sb.append("📊 *STATUS: ${tabName.uppercase()} - $currentDateTimeStatus* 📊\n\n")
    
    for (item in equipments) {
        val emoji = when (item.status) {
            "Disponível" -> "🟢"
            "Em Manutenção" -> "🔴"
            "Preventiva" -> "🟡"
            else -> "⚪"
        }
        sb.append("$emoji *${item.nome}:* ${item.status}")
        if (item.obs.trim().isNotEmpty()) {
            sb.append(" - Obs: ${item.obs.trim()}")
        }
        sb.append("\n")
    }
    
    sb.append("\n_Gerado automaticamente pelo RP.Status_")
    return sb.toString()
}

fun shareReport(context: Context, reportText: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Enviar Relatório")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao compartilhar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun copyToClipboard(context: Context, text: String, onCopied: () -> Unit) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Relatório RP.Status", text)
        clipboard.setPrimaryClip(clip)
        onCopied()
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao copiar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun StatusHistorySection(
    logs: List<EquipmentStatusLog>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTÓRICO DE ALTERAÇÕES",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Box(
                    modifier = Modifier
                        .background(color = Slate900, shape = CircleShape)
                        .border(width = 1.dp, color = Slate700, shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = logs.size.toString(),
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (logs.isEmpty()) {
                Text(
                    text = "Nenhuma alteração registrada ainda.",
                    color = Slate600,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // Keep only the last 15 elements to avoid infinite list expansion
                val displayLogs = logs.take(15)
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    displayLogs.forEach { log ->
                        StatusHistoryRow(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusHistoryRow(
    log: EquipmentStatusLog,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Slate900.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
            .border(width = 1.dp, color = Slate700.copy(alpha = 0.3f), shape = RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top: Equipment Name & Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.equipmentName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = formattedTime,
                color = Slate600,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom: Status Transition badges
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusTransitionBadge(status = log.statusAnterior)
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "mudou para",
                tint = Slate600,
                modifier = Modifier.size(12.dp)
            )
            
            StatusTransitionBadge(status = log.statusNovo)
        }
    }
}

@Composable
fun StatusTransitionBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        "Disponível" -> StatusDisponivel to "Disponível"
        "Em Manutenção" -> StatusManutencao to "Manutenção"
        "Preventiva" -> StatusPreventiva to "Preventiva"
        "Criado" -> Slate400 to "Criado"
        else -> Slate400 to status
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(color = color.copy(alpha = 0.08f), shape = RoundedCornerShape(6.dp))
            .border(width = 1.dp, color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabsRow(
    tabs: List<EquipmentTab>,
    selectedTabId: Long,
    onTabSelect: (Long) -> Unit,
    onAddTabClick: () -> Unit,
    onTabLongClick: (EquipmentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ABAS / SETORES",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                IconButton(
                    onClick = onAddTabClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar nova aba",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val isSelected = tab.id == selectedTabId
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Indigo600 else Slate900,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else Slate700.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .combinedClickable(
                                onClick = { onTabSelect(tab.id) },
                                onLongClick = { onTabLongClick(tab) }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab.name.uppercase(),
                            color = if (isSelected) Color.White else Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .background(color = Slate900.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
                            .border(width = 1.dp, color = Slate700.copy(alpha = 0.3f), shape = RoundedCornerShape(14.dp))
                            .clickable { onAddTabClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Mais",
                                tint = Slate400,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "OBRA",
                                color = Slate400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabActionOptionsDialog(
    tab: EquipmentTab,
    onDismiss: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = {
            Text(
                text = "Aba: ${tab.name}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Button(
                    onClick = {
                        onRenameClick()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = "Renomear Aba", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        onDeleteClick()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusManutencao),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = "Excluir Aba e Equipamentos", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Slate700),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Cancelar", fontWeight = FontWeight.Medium)
                }
            }
        },
        containerColor = Slate800,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun AddTabDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Adicionar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Slate400)
            ) {
                Text("Cancelar")
            }
        },
        title = {
            Text(
                text = "Nova Seção / Aba",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da aba") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedBorderColor = Indigo600,
                    unfocusedBorderColor = Slate700,
                    focusedLabelColor = Indigo600,
                    unfocusedLabelColor = Slate400
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Slate800,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RenameTabDialog(
    tab: EquipmentTab,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(tab.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Slate400)
            ) {
                Text("Cancelar")
            }
        },
        title = {
            Text(
                text = "Renomear Aba",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da aba") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedBorderColor = Indigo600,
                    unfocusedBorderColor = Slate700,
                    focusedLabelColor = Indigo600,
                    unfocusedLabelColor = Slate400
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Slate800,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RenameEquipmentDialog(
    equipment: Equipment,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(equipment.nome) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Slate400)
            ) {
                Text("Cancelar")
            }
        },
        title = {
            Text(
                text = "Renomear Equipamento",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do equipamento") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedBorderColor = Indigo600,
                    unfocusedBorderColor = Slate700,
                    focusedLabelColor = Indigo600,
                    unfocusedLabelColor = Slate400
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Slate800,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ConfirmDeleteTabDialog(
    tab: EquipmentTab,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusManutencao),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Excluir", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Slate400)
            ) {
                Text("Cancelar")
            }
        },
        title = {
            Text(
                text = "Excluir Aba?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Tem certeza que deseja excluir a aba \"${tab.name}\"? Todo o maquinário associado a ela também será removido permanentemente.",
                color = Slate100,
                fontSize = 14.sp
            )
        },
        containerColor = Slate800,
        shape = RoundedCornerShape(24.dp)
    )
}


@Composable
fun SelectReportTypeDialog(
    onDismiss: () -> Unit,
    onSelectSimple: () -> Unit,
    onSelectDetailed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = {
            Text(
                text = "Tipo de Relatório",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Selecione o formato de relatório para compartilhar:",
                    color = Slate400,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        onSelectSimple()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = "Relatório Simples", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        onSelectDetailed()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo700),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = "Relatório Detalhado", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Slate700),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Cancelar", fontWeight = FontWeight.Medium)
                }
            }
        },
        containerColor = Slate800,
        shape = RoundedCornerShape(24.dp)
    )
}

fun generateSimpleReportText(equipments: List<Equipment>, tabName: String = "Frota"): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    val currentDateTimeStatus = dateFormat.format(Date())
    
    val sb = java.lang.StringBuilder()
    sb.append("📊 *STATUS: ${tabName.uppercase()} - $currentDateTimeStatus* 📊\n\n")
    
    for (item in equipments) {
        val emoji = when (item.status) {
            "Disponível" -> "🟢"
            "Em Manutenção" -> "🔴"
            "Preventiva" -> "🟡"
            else -> "⚪"
        }
        sb.append("$emoji *${item.nome}:* ${item.status}")
        if (item.obs.trim().isNotEmpty()) {
            sb.append(" - Obs: ${item.obs.trim()}")
        }
        sb.append("\n")
    }
    
    sb.append("\n")
    
    val available = equipments.filter { it.status == "Disponível" }
    
    var paCount = 0
    var miCount = 0
    var escCount = 0
    var emCount = 0
    var vassourasCount = 0
    val others = mutableListOf<Equipment>()
    
    for (item in available) {
        val nameLower = item.nome.trim().lowercase(Locale.ROOT)
        when {
            nameLower.contains("vassoura") -> vassourasCount++
            nameLower.startsWith("mini") || nameLower.startsWith("mi ") || nameLower == "mi" || nameLower.startsWith("mi-") -> miCount++
            nameLower.startsWith("pa") || nameLower.startsWith("pá") || nameLower.startsWith("pcarregadeira") -> paCount++
            nameLower.startsWith("esc") -> escCount++
            nameLower.startsWith("em") -> emCount++
            else -> others.add(item)
        }
    }
    
    if (paCount > 0) {
        sb.append(String.format(Locale("pt", "BR"), "%02d Pa liberadas\n", paCount))
    }
    if (miCount > 0) {
        sb.append(String.format(Locale("pt", "BR"), "%02d Mi liberadas\n", miCount))
    }
    if (escCount > 0) {
        sb.append(String.format(Locale("pt", "BR"), "%02d Esc liberadas\n", escCount))
    }
    if (emCount > 0) {
        sb.append(String.format(Locale("pt", "BR"), "%02d Em liberadas\n", emCount))
    }
    if (vassourasCount > 0) {
        val label = if (vassourasCount == 1) "vassoura" else "vassouras"
        sb.append(String.format(Locale("pt", "BR"), "%02d %s\n", vassourasCount, label))
    }
    
    val otherGroups = others.groupBy { it.nome.trim().split(" ").firstOrNull()?.trim() ?: "Outros" }
    for ((groupName, items) in otherGroups) {
        sb.append(String.format(Locale("pt", "BR"), "%02d %s liberadas\n", items.size, groupName))
    }
    
    sb.append("\n_Gerado automaticamente pelo RP.Status_")
    return sb.toString()
}

fun generateDetailedReportText(equipments: List<Equipment>, tabName: String = "Frota"): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    val currentDateTimeStatus = dateFormat.format(Date())
    
    val sb = java.lang.StringBuilder()
    sb.append("📊 *STATUS: ${tabName.uppercase()} - $currentDateTimeStatus* 📊\n\n")
    
    for (item in equipments) {
        val emoji = when (item.status) {
            "Disponível" -> "🟢"
            "Em Manutenção" -> "🔴"
            "Preventiva" -> "🟡"
            else -> "⚪"
        }
        sb.append("$emoji *${item.nome}:* ${item.status}")
        if (item.obs.trim().isNotEmpty()) {
            sb.append(" - Obs: ${item.obs.trim()}")
        }
        sb.append("\n")
    }
    
    sb.append("\n")
    
    val available = equipments.filter { it.status == "Disponível" }
    
    val pas = mutableListOf<Equipment>()
    var miCount = 0
    var escCount = 0
    var emCount = 0
    var vassourasCount = 0
    val others = mutableListOf<Equipment>()
    
    for (item in available) {
        val nameLower = item.nome.trim().lowercase(Locale.ROOT)
        when {
            nameLower.contains("vassoura") -> vassourasCount++
            nameLower.startsWith("mini") || nameLower.startsWith("mi ") || nameLower == "mi" || nameLower.startsWith("mi-") -> miCount++
            nameLower.startsWith("pa") || nameLower.startsWith("pá") || nameLower.startsWith("pcarregadeira") -> pas.add(item)
            nameLower.startsWith("esc") -> escCount++
            nameLower.startsWith("em") -> emCount++
            else -> others.add(item)
        }
    }
    
    if (pas.isNotEmpty()) {
        sb.append(String.format(Locale("pt", "BR"), "%02d Pa liberadas\n", pas.size))
        
        var simpleCount = 0
        var hiTipCount = 0
        var engCount = 0
        
        for (pa in pas) {
            val nameLower = pa.nome.trim().lowercase(Locale.ROOT)
            when {
                nameLower.contains("hi tip") || nameLower.contains("hitip") || nameLower.contains("hi-tip") -> hiTipCount++
                nameLower.contains("eng") || nameLower.contains("engate") -> engCount++
                else -> simpleCount++
            }
        }
        
        if (simpleCount > 0) {
            sb.append(String.format(Locale("pt", "BR"), "%02d Pa simples\n", simpleCount))
        }
        if (hiTipCount > 0) {
            sb.append(String.format(Locale("pt", "BR"), "%02d Pa hi tip\n", hiTipCount))
        }
        if (engCount > 0) {
            sb.append(String.format(Locale("pt", "BR"), "%02d Pa eng\n", engCount))
        }
    }
    
    if (miCount > 0) {
        sb.append(String.format(Locale("pt", "BR"), "%02d Mi liberadas\n", miCount))
    }
    if (escCount > 0) {
        sb.append(String.format(Locale("pt", "BR"), "%02d Esc liberadas\n", escCount))
    }
    if (emCount > 0) {
        sb.append(String.format(Locale("pt", "BR"), "%02d Em liberadas\n", emCount))
    }
    if (vassourasCount > 0) {
        val label = if (vassourasCount == 1) "vassoura" else "vassouras"
        sb.append(String.format(Locale("pt", "BR"), "%02d %s\n", vassourasCount, label))
    }
    
    val otherGroups = others.groupBy { it.nome.trim().split(" ").firstOrNull()?.trim() ?: "Outros" }
    for ((groupName, items) in otherGroups) {
        sb.append(String.format(Locale("pt", "BR"), "%02d %s liberadas\n", items.size, groupName))
    }
    
    sb.append("\n_Gerado automaticamente pelo RP.Status_")
    return sb.toString()
}

@Composable
fun SettingsDialog(
    viewModel: EquipmentViewModel,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    var isImportMode by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    
    val fileExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = viewModel.getBackupJsonString()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "💾 Backup exportado e salvo no armazenamento!",
                        duration = SnackbarDuration.Short
                    )
                }
                Toast.makeText(context, "Arquivo de backup salvo com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao salvar o arquivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.reader().readText()
                }
                if (jsonString != null) {
                    viewModel.importBackupJsonString(
                        jsonStr = jsonString,
                        onSuccess = {
                            Toast.makeText(context, "Configurações importadas e restauradas com sucesso!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, "Erro ao importar: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao carregar o arquivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Configurações",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                if (!isImportMode) {
                    Text(
                        text = "Gerencie os dados das suas abas e equipamentos desta frota salvando ou importando diretamente do seu dispositivo.",
                        color = Slate400,
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "EXPORTAR / SALVAR",
                        color = Indigo400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Card 1: Save file directly to phone
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    try {
                                        fileExportLauncher.launch("rp_status_backup.json")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Não foi possível abrir o seletor de arquivos.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Indigo400,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Salvar Arquivo de Backup",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Salva o arquivo JSON com todas as configurações na memória do seu aparelho para uso futuro.",
                                        color = Slate400,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: Copy to Clipboard / Share via other apps
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    val backupJsonStr = viewModel.getBackupJsonString()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("RPStatusBackup", backupJsonStr)
                                    clipboard.setPrimaryClip(clip)
                                    
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Backup RP.Status")
                                        putExtra(Intent.EXTRA_TEXT, backupJsonStr)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Enviar código de configuração"))
                                    
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "💾 Texto copiado e pronto para compartilhar!",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Copiar e Compartilhar Texto",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Copia os dados formatados em texto JSON para compartilhar rápido em mensageiros.",
                                        color = Slate400,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "IMPORTAR / RESTAURAR",
                        color = Indigo400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Card 3: Load Backup file from storage
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    try {
                                        fileImportLauncher.launch(arrayOf("*/*"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Não foi possível abrir o seletor de arquivos.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Indigo400,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Importar Arquivo de Backup",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Selecione um arquivo .json salvo no seu gerenciador de arquivos ou downloads para restaurar.",
                                        color = Slate400,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Card 4: Paste text manually
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    isImportMode = true
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val item = clipboard.primaryClip?.getItemAt(0)
                                        val clipText = item?.text?.toString() ?: ""
                                        if (clipText.contains("\"tabs\"") && clipText.contains("\"equipments\"")) {
                                            importText = clipText
                                        }
                                    } catch (e: Exception) {
                                        // Ignore clipboard read errors
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Importar Copiando e Colando",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Se preferir, cole o texto JSON de backup copiado diretamente na caixa de texto na próxima tela.",
                                        color = Slate400,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Import Input Mode
                    Text(
                        text = "Cole o texto JSON de backup abaixo para restaurar as suas configurações de frota:",
                        color = Slate400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900,
                            focusedBorderColor = Indigo400,
                            unfocusedBorderColor = Slate700,
                            focusedLabelColor = Indigo400,
                            unfocusedLabelColor = Slate400
                        ),
                        placeholder = {
                            Text(
                                text = "Cole seu JSON de backup aqui...",
                                color = Slate600,
                                fontSize = 12.sp
                            )
                        }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Paste helper button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipText.isNotBlank()) {
                                        importText = clipText
                                        Toast.makeText(context, "Texto colado!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Área de transferência vazia.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Não foi possível acessar a área de transferência.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            border = BorderStroke(1.dp, Slate700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Colar da Área", fontSize = 12.sp)
                        }
                        
                        // Import Confirm Button
                        Button(
                            onClick = {
                                if (importText.isBlank()) {
                                    Toast.makeText(context, "Por favor, insira o JSON do backup.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.importBackupJsonString(
                                    jsonStr = importText,
                                    onSuccess = {
                                        Toast.makeText(context, "Configurações restauradas com sucesso!", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, "Erro na importação: $errorMsg", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Restaurar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    TextButton(
                        onClick = { isImportMode = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Slate400),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("← Voltar")
                    }
                }
                
                if (!isImportMode) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Slate700),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Fechar", fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        containerColor = Slate800,
        shape = RoundedCornerShape(24.dp)
    )
}

