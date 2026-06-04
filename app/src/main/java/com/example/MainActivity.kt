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
import com.example.ui.theme.*
import com.example.viewmodel.EquipmentViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var novoEquipamentoNome by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

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
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeaderSection(
                            onShare = {
                                if (equipments.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um equipamento antes de gerar o relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val report = generateReportText(equipments)
                                    shareReport(context, report)
                                }
                            },
                            onCopy = {
                                if (equipments.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um equipamento antes de gerar o relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val report = generateReportText(equipments)
                                    copyToClipboard(context, report) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "📊 Relatório copiado para a área de transferência!",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        QuickStatsBlock(equipments = equipments)

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
                            .fillMaxHeight()
                    ) {
                        FleetHeader(count = equipments.size)
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (equipments.isEmpty()) {
                            EmptyStateBlock(modifier = Modifier.weight(1f))
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 250.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(equipments, key = { it.id }) { item ->
                                    EquipmentCard(
                                        equipment = item,
                                        onStatusChange = { newStatus ->
                                            viewModel.updateEquipmentStatus(item, newStatus)
                                        },
                                        onObsChange = { newObs ->
                                            viewModel.updateEquipmentObs(item, newObs)
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
                                if (equipments.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um equipamento antes de gerar o relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val report = generateReportText(equipments)
                                    shareReport(context, report)
                                }
                            },
                            onCopy = {
                                if (equipments.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um equipamento antes de gerar o relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val report = generateReportText(equipments)
                                    copyToClipboard(context, report) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "📊 Relatório copiado para a área de transferência!",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    item {
                        QuickStatsBlock(equipments = equipments)
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
                        FleetHeader(count = equipments.size)
                    }

                    if (equipments.isEmpty()) {
                        item {
                            EmptyStateBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                        }
                    } else {
                        items(equipments, key = { it.id }) { item ->
                            EquipmentCard(
                                equipment = item,
                                onStatusChange = { newStatus ->
                                    viewModel.updateEquipmentStatus(item, newStatus)
                                },
                                onObsChange = { newObs ->
                                    viewModel.updateEquipmentObs(item, newObs)
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
                    value = equipment.obs,
                    onValueChange = onObsChange,
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
fun generateReportText(equipments: List<Equipment>): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    val currentDateTimeStatus = dateFormat.format(Date())
    
    val sb = java.lang.StringBuilder()
    sb.append("📊 *DISPONIBILIDADE DE MÁQUINAS - $currentDateTimeStatus* 📊\n\n")
    
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
