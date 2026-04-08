package com.milkaijun.militaryalarm

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyNotificationListener : NotificationListenerService() {
    private val TARGET_PACKAGE = "kr.ac.kaist.krp"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        if (packageName == TARGET_PACKAGE) {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            val sharedPref = getSharedPreferences("WorkPrefs", Context.MODE_PRIVATE)
            val calendar = java.util.Calendar.getInstance()
            if (calendar.get(java.util.Calendar.HOUR_OF_DAY) < 5) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) // 💡 찍힌 시간 추가

            if (text.contains("출근처리")) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "✅ KAIST 출근 기록 감지됨! ($currentTime)", Toast.LENGTH_LONG).show()
                }
                // 💡 "IN_2026-04-06" 이라는 이름의 칸에 "08:45"를 저장
                sharedPref.edit().putString("IN_$currentDate", currentTime).apply()
            }

            if (text.contains("퇴근처리")) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "✅ KAIST 퇴근 기록 감지됨! ($currentTime)", Toast.LENGTH_LONG).show()
                }
                // 💡 "OUT_2026-04-06" 이라는 이름의 칸에 "18:10"을 저장
                sharedPref.edit().putString("OUT_$currentDate", currentTime).apply()
            }
        }
    }
}