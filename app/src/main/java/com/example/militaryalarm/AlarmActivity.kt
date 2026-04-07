package com.example.militaryalarm // 패키지명 확인!

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.militaryalarm.ui.theme.MilitaryalarmTheme

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmType = intent.getStringExtra("ALARM_TYPE") ?: "IN"

        startAlarm()

        setContent {
            MilitaryalarmTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFEBEE))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (alarmType == "IN") "출근 미확인!" else "퇴근 미확인!",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "기록을 확인하고 알람을 끄세요.", fontSize = 20.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(48.dp))

                    // 💡 새로 추가된 1시간 뒤 다시 알림 버튼
                    Button(
                        onClick = { snoozeAlarm(alarmType) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.size(width = 250.dp, height = 60.dp)
                    ) {
                        Text("1시간 뒤 다시 알림", fontSize = 20.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 기존 완전 종료 버튼
                    Button(
                        onClick = { stopAlarmAndFinish() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.size(width = 250.dp, height = 60.dp)
                    ) {
                        Text("알람 끄기", fontSize = 24.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // 💡 1시간 뒤 일회용 알람을 스케줄링하는 함수
    private fun snoozeAlarm(alarmType: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TYPE", alarmType)
        }

        // 기존 매일 울리는 알람(100, 200)을 덮어쓰지 않도록 스누즈 전용 아이디(101, 201) 사용
        val requestCode = if (alarmType == "IN") 101 else 201
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 현재 시간으로부터 딱 1시간 뒤 (60분 * 60초 * 1000밀리초)
        val snoozeTime = System.currentTimeMillis() + (60 * 60 * 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
        }

        Toast.makeText(this, "1시간 뒤에 다시 알림이 울립니다.", Toast.LENGTH_SHORT).show()
        stopAlarmAndFinish()
    }

    private fun startAlarm() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }
    }

    private fun stopAlarmAndFinish() {
        vibrator?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        vibrator?.cancel()
        mediaPlayer?.release()
    }
}