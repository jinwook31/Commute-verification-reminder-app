package com.milkaijun.militaryalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPref = context.getSharedPreferences("WorkPrefs", Context.MODE_PRIVATE)

            val inHour = sharedPref.getInt("inHour", 8)
            val inMinute = sharedPref.getInt("inMinute", 50)
            val outHour = sharedPref.getInt("outHour", 18)
            val outMinute = sharedPref.getInt("outMinute", 10)
            val skipWeekends = sharedPref.getBoolean("skipWeekends", true)

            // 부팅 직후 출근/퇴근 알람 재스케줄링
            scheduleAlarm(context, "IN", 100, inHour, inMinute, skipWeekends)
            scheduleAlarm(context, "OUT", 200, outHour, outMinute, skipWeekends)
        }
    }
}