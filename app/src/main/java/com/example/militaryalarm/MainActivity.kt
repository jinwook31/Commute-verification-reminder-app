package com.example.militaryalarm // 패키지명 확인!

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.militaryalarm.ui.theme.MilitaryalarmTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MilitaryalarmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmMainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }

        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        lifecycleScope.launch {
            while (isActive) {
                // 10초 대기 (10000ms = 10초)
                delay(10000)

                // 여기에 데이터를 다시 불러오거나 화면을 갱신하는 함수 호출
                refreshMyAppData()
            }
        }
    }

    private fun refreshMyAppData() {
        // DB에서 데이터 다시 읽어오기, UI 업데이트 등
    }
}

@Composable
fun AlarmMainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("WorkPrefs", Context.MODE_PRIVATE)
    val isPermissionGranted = isNotificationServiceEnabled(context)
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var inHour by remember { mutableStateOf(sharedPref.getInt("inHour", 8)) }
    var inMinute by remember { mutableStateOf(sharedPref.getInt("inMinute", 50)) }
    var outHour by remember { mutableStateOf(sharedPref.getInt("outHour", 18)) }
    var outMinute by remember { mutableStateOf(sharedPref.getInt("outMinute", 10)) }
    var skipWeekends by remember { mutableStateOf(sharedPref.getBoolean("skipWeekends", true)) }

    val todayInTime = sharedPref.getString("IN_$currentDate", "미확인")
    val todayOutTime = sharedPref.getString("OUT_$currentDate", "미확인")

    val inTimeDialog = TimePickerDialog(
        context,
        // 다이얼로그 테마를 다크모드스럽게 띄우고 싶다면 시스템 설정에 따르거나 별도 스타일 지정 필요
        { _, h, m ->
            inHour = h; inMinute = m
            sharedPref.edit().putInt("inHour", inHour).putInt("inMinute", inMinute).apply()
            scheduleAlarm(context, "IN", 100, inHour, inMinute, skipWeekends)
        }, inHour, inMinute, false
    )

    val outTimeDialog = TimePickerDialog(
        context,
        { _, h, m ->
            outHour = h; outMinute = m
            sharedPref.edit().putInt("outHour", outHour).putInt("outMinute", outMinute).apply()
            scheduleAlarm(context, "OUT", 200, outHour, outMinute, skipWeekends)
        }, outHour, outMinute, false
    )

    // 💡 전체 배경색을 다크 모드 표준 색상인 #121212로 변경
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "전문연 출퇴근 인증 리마인더",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White, // 💡 메인 타이틀 흰색
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!isPermissionGranted) {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679)), // 다크모드용 약간 톤다운된 레드
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) { Text("❌ 알림 접근 권한 허용하기", color = Color.White) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                if (!notificationManager.canUseFullScreenIntent()) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) { Text("🚨 전체 화면 알람 권한 허용하기", color = Color.White) }
                }
            }
        }

        // 🟢 카드 1: 오늘의 현황
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                // 💡 다크 모드 카드 배경색 #1E1E1E
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("오늘의 출퇴근 현황", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("출근 시간", fontSize = 16.sp, color = Color.LightGray)
                        Text(text = if(todayInTime == "미확인") "🔴 미확인" else "🟢 $todayInTime", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("퇴근 시간", fontSize = 16.sp, color = Color.LightGray)
                        Text(text = if(todayOutTime == "미확인") "⏳ 대기 중" else "🟢 $todayOutTime", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // ⚙️ 카드 2: 알람 설정
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("인증 데드라인 설정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("주말(토, 일) 제외", fontSize = 16.sp, color = Color.White)
                        Switch(
                            checked = skipWeekends,
                            onCheckedChange = {
                                skipWeekends = it
                                sharedPref.edit().putBoolean("skipWeekends", it).apply()
                                scheduleAlarm(context, "IN", 100, inHour, inMinute, skipWeekends)
                                scheduleAlarm(context, "OUT", 200, outHour, outMinute, skipWeekends)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { inTimeDialog.show() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)) // 다크 톤 버튼
                    ) {
                        Text("출근 인증 데드라인 : ${String.format("%02d:%02d", inHour, inMinute)}", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { outTimeDialog.show() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                    ) {
                        Text("퇴근 인증 데드라인 : ${String.format("%02d:%02d", outHour, outMinute)}", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, AlarmReceiver::class.java).apply { putExtra("ALARM_TYPE", "IN") }
                            context.sendBroadcast(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("🔔 강제 알람 TEST")
                    }
                }
            }
        }

        // 📅 카드 3: 최근 4일 기록
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("최근 4일 기록", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)

                    for (i in 0..3) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DATE, -i) }
                        val loopDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                        val displayDate = SimpleDateFormat("MM월 dd일 (E)", Locale.KOREA).format(cal.time)

                        val historyIn = sharedPref.getString("IN_$loopDate", "미확인")
                        val historyOut = sharedPref.getString("OUT_$loopDate", "미확인")

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(displayDate, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(
                                text = "IN: ${if(historyIn == "미확인") "❌" else historyIn} | OUT: ${if(historyOut == "미확인") "❌" else historyOut}",
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

fun scheduleAlarm(context: Context, type: String, requestCode: Int, hour: Int, minute: Int, skipWeekends: Boolean) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply { putExtra("ALARM_TYPE", type) }

    val pendingIntent = PendingIntent.getBroadcast(
        context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        if (before(Calendar.getInstance())) {
            add(Calendar.DATE, 1)
        }

        if (skipWeekends) {
            while (get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                add(Calendar.DATE, 1)
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            val permissionIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(permissionIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(pkgName) == true
}