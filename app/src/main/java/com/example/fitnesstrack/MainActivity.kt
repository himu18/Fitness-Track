package com.example.fitnesstrack

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fitnesstrack.ui.theme.FitnessTrackTheme
import com.example.fitnesstrack.ui.viewmodel.StepTrackingViewModel
import com.example.fitnesstrack.ui.viewmodel.StepTrackingUiState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class MainActivity : ComponentActivity() {
    
    private lateinit var stepSensorListener: StepSensorListener
    private var isTracking = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            if (::stepSensorListener.isInitialized) {
                stepSensorListener.startListening()
                isTracking = true
            }
        }
    }
    
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Notification permission granted/denied
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request permissions if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        stepSensorListener = StepSensorListener(this)
        
        // Set default goal if not set
        val prefs = getSharedPreferences("fitness_tracker_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("daily_goal")) {
            StepCountManager.setDailyGoal(this, 10000)
        }
        
        setContent {
            FitnessTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitnessTrackerScreen(this@MainActivity)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::stepSensorListener.isInitialized && !isTracking) {
            stepSensorListener.startListening()
            isTracking = true
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (::stepSensorListener.isInitialized) {
            stepSensorListener.stopListening()
            isTracking = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::stepSensorListener.isInitialized) {
            stepSensorListener.stopListening()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessTrackerScreen(context: Context) {
    val viewModel: StepTrackingViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Today", "History", "Settings")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fitness Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> TodayTab(viewModel, uiState)
                1 -> HistoryTab(viewModel, uiState)
                2 -> SettingsTab(viewModel, uiState)
            }
        }
    }
}

@Composable
fun TodayTab(
    viewModel: StepTrackingViewModel,
    uiState: StepTrackingUiState
) {
    val currentSteps = uiState.currentSteps
    val goal = uiState.dailyGoal
    val progress = uiState.progress
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Step Counter Circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(250.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$currentSteps",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "steps",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // Progress Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Goal: $goal steps",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            if (currentSteps >= goal) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎉 Goal Achieved!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        
        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Remaining",
                value = "${(goal - currentSteps).coerceAtLeast(0)}",
                icon = android.R.drawable.ic_input_add
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Progress",
                value = "${(progress * 100).toInt()}%",
                icon = android.R.drawable.star_on
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: Any
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                icon is androidx.compose.ui.graphics.vector.ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                icon is Int -> {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_mylocation),
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun HistoryTab(
    viewModel: StepTrackingViewModel,
    uiState: StepTrackingUiState
) {
    var selectedPeriod by remember { mutableStateOf(7) }
    
    LaunchedEffect(selectedPeriod) {
        viewModel.loadHistoryData(selectedPeriod)
    }
    
    val historyData = uiState.historyData
    val stats = uiState.historyStats
    val average = (stats["average"] as? Number)?.toInt() ?: 0
    val total = (stats["total"] as? Number)?.toInt() ?: 0
    val max = (stats["max"] as? Number)?.toInt() ?: 0
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPeriod == 7,
                onClick = { selectedPeriod = 7 },
                label = { Text("7 Days") }
            )
            FilterChip(
                selected = selectedPeriod == 30,
                onClick = { selectedPeriod = 30 },
                label = { Text("30 Days") }
            )
        }
        
        // Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Total",
                value = formatNumber(total),
                icon = android.R.drawable.ic_menu_mylocation
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Average",
                value = formatNumber(average),
                icon = android.R.drawable.checkbox_off_background
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Max",
                value = formatNumber(max),
                icon = android.R.drawable.star_on
            )
        }
        
        Text(
            text = "Daily History",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        if (historyData.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.checkbox_off_background),
                        contentDescription = "No data",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No history yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            if (historyData.any { it.second > 0 }) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyData) { (date, steps) ->
                        HistoryItem(date = date, steps = steps, goal = uiState.dailyGoal)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.checkbox_off_background),
                            contentDescription = "No data",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No history yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Start walking to see your history",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(date: String, steps: Int, goal: Int = 10000) {
    val progress = if (goal > 0) {
        (steps.toFloat() / goal).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatDate(date),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (steps > 0) {
                        Text(
                            text = "${(progress * 100).toInt()}% of goal",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${formatNumber(steps)} steps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (steps > 0 && goal > 0) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun formatDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE)
        when {
            date == LocalDate.now() -> "Today"
            date == LocalDate.now().minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
    } catch (e: DateTimeParseException) {
        dateStr
    }
}

fun formatNumber(number: Int): String {
    return when {
        number >= 1000000 -> "${number / 1000000.0}M"
        number >= 1000 -> "${number / 1000.0}K"
        else -> number.toString()
    }
}

@Composable
fun SettingsTab(
    viewModel: StepTrackingViewModel,
    uiState: StepTrackingUiState
) {
    var showDialog by remember { mutableStateOf(false) }
    val dailyGoal = uiState.dailyGoal.toString()
    val backgroundTracking = uiState.backgroundTrackingEnabled
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.star_on),
                    contentDescription = "Goal",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Daily Goal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$dailyGoal steps",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(painter = painterResource(android.R.drawable.ic_menu_edit), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Daily Goal")
        }
        
        // Background Tracking Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_preferences),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Background Tracking",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
    Text(
                            text = "Track steps even when app is closed",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = backgroundTracking,
                        onCheckedChange = { isEnabled ->
                        viewModel.updateBackgroundTracking(isEnabled)
                    }
                )
            }
        }
        
        InfoCard()
    }
    
    if (showDialog) {
        GoalDialog(
            currentGoal = dailyGoal,
            onDismiss = { showDialog = false },
            onConfirm = { newGoal ->
                val goal = newGoal.toIntOrNull()
                if (goal != null && goal > 0) {
                    viewModel.setDailyGoal(goal)
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun GoalDialog(
    currentGoal: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputGoal by remember { mutableStateOf(currentGoal) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Goal") },
        text = {
            OutlinedTextField(
                value = inputGoal,
                onValueChange = { inputGoal = it },
                label = { Text("Steps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(inputGoal) }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_dialog_info),
                contentDescription = "Info",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "How it works",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This app tracks your steps using your device's built-in sensors. No internet connection required. Your data is stored locally on your device.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}
