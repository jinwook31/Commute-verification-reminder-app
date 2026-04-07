package com.example.militaryalarm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPref = context.getSharedPreferences("WorkPrefs", Context.MODE_PRIVATE)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val alarmType = intent.getStringExtra("ALARM_TYPE") ?: "IN"
        val skipWeekends = sharedPref.getBoolean("skipWeekends", true)

        var shouldAlarmRing = false

        if (alarmType == "IN") {
            // 💡 "IN_오늘날짜" 칸에 데이터가 없으면 알람을 울립니다.
            val checkInTime = sharedPref.getString("IN_$currentDate", null)
            if (checkInTime == null) shouldAlarmRing = true

            val inHour = sharedPref.getInt("inHour", 8)
            val inMinute = sharedPref.getInt("inMinute", 50)
            scheduleAlarm(context, "IN", 100, inHour, inMinute, skipWeekends)

        } else if (alarmType == "OUT") {
            // 💡 "OUT_오늘날짜" 칸에 데이터가 없으면 알람을 울립니다.
            val checkOutTime = sharedPref.getString("OUT_$currentDate", null)
            if (checkOutTime == null) shouldAlarmRing = true

            val outHour = sharedPref.getInt("outHour", 18)
            val outMinute = sharedPref.getInt("outMinute", 10)
            scheduleAlarm(context, "OUT", 200, outHour, outMinute, skipWeekends)
        }

        if (shouldAlarmRing) {
            Log.d("AlarmLog", "기록 누락! 전체 화면 알람 시작")
            fireFullScreenAlarm(context, alarmType)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fireFullScreenAlarm(context: Context, alarmType: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "military_alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "출퇴근 알람", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val fullscreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("ALARM_TYPE", alarmType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmType.hashCode(),
            fullscreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("[Reminder]")
            .setContentText("전문연 출/퇴근 기록이 확인되지 않았습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarmType.hashCode(), notification)
    }
}