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
        //val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val calendar = java.util.Calendar.getInstance()
        if (calendar.get(java.util.Calendar.HOUR_OF_DAY) < 5) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1) // 하루 빼기
        }
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)
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

        // 🚨 1. 화면이 켜져 있을 때도 강제로 Activity를 띄우기 위한 핵심 로직 추가
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (android.provider.Settings.canDrawOverlays(context)) {
                // '다른 앱 위에 표시' 권한이 있다면 알림 팝업 대신 즉시 화면을 전환합니다.
                context.startActivity(fullscreenIntent)
            }
        } else {
            // 안드로이드 6.0 미만은 권한 체크 없이 바로 실행 가능
            context.startActivity(fullscreenIntent)
        }

        // 2. 기존 Notification 생성 로직 (상태 표시줄 알림용 및 권한 없을 때 대비용)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("[Reminder]")
            .setContentText("전문연 출/퇴근 기록이 확인되지 않았습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // 화면 꺼져있을 땐 여전히 유효
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarmType.hashCode(), notification)
    }
}