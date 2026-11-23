package com.example.mrprivate.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mrprivate.MainActivity
import com.example.mrprivate.ScheduleSlot
import com.example.mrprivate.StudentProfile
import com.example.mrprivate.TeacherProfile
import com.example.mrprivate.formatHour12
import com.example.mrprivate.getTodayDateKey
import com.example.mrprivate.isBreakValue
import com.example.mrprivate.slotHourKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object StudentNotificationHelper {
    const val CHANNEL_ID = "mrprivate_student_sessions_channel"
    const val ACTION_MORNING_SCHEDULE = "com.example.mrprivate.ACTION_MORNING_SCHEDULE"
    const val ACTION_SESSION_REMINDER = "com.example.mrprivate.ACTION_SESSION_REMINDER"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات جدول الطالب والحصص القادمة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الصباح الباكر للجدول الدراسي والتذكير قبل الحصة بساعة"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showImmediateNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        ensureNotificationChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.notify(notificationId, notification)
    }

    /**
     * جدولة:
     * 1) إشعار يومي في الصباح الباكر (07:30 صباحاً) للأيام التي بها جدول حصص دراسية.
     * 2) إشعار قبل موعد الحصة بساعة واحدة يتضمن تفاصيل المعلم، المادة، ووقت بدء الحصة.
     */
    fun scheduleStudentPushNotifications(
        context: Context,
        student: StudentProfile,
        allTeachers: List<TeacherProfile>,
        allSchedules: List<ScheduleSlot>
    ) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val nowMillis = System.currentTimeMillis()
        val todayKey = getTodayDateKey()

        // جدولة إشعارات للأيام القادمة (حتى 14 يوماً) من جدول الطالب
        val upcomingDays = student.dailyTimetable.entries
            .filter { (dateKey, dayData) ->
                dateKey >= todayKey && !dayData.isVacation &&
                dayData.hourlySlots.any { it.value.isNotBlank() && !isBreakValue(it.value) }
            }
            .sortedBy { it.key }
            .take(14)

        upcomingDays.forEachIndexed { dayIndex, (dateKey, dayData) ->
            val parsedDate = try { fmt.parse(dateKey) } catch (_: Exception) { null } ?: return@forEachIndexed
            val activeSlots = dayData.hourlySlots.entries
                .filter { it.value.isNotBlank() && !isBreakValue(it.value) }
                .sortedBy { it.key }

            if (activeSlots.isEmpty()) return@forEachIndexed

            // 1. إشعار الصباح الباكر (07:30 صباحاً)
            val morningCal = Calendar.getInstance().apply {
                time = parsedDate
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (morningCal.timeInMillis > nowMillis) {
                val subjectsSummary = activeSlots.joinToString(" • ") { (h, label) ->
                    "${formatHour12(h, true)}: $label"
                }
                val morningIntent = Intent(context, StudentSessionAlarmReceiver::class.java).apply {
                    action = ACTION_MORNING_SCHEDULE
                    putExtra("notif_id", 10_000 + dayIndex)
                    putExtra("title", "صباح الخير ${student.name} ☀️ جدولك الدراسي اليوم")
                    putExtra("body", "لديك اليوم ${activeSlots.size} حصص مجدولة: $subjectsSummary")
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    10_000 + dayIndex,
                    morningIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExactOrFallback(alarmManager, morningCal.timeInMillis, pi)
            }

            // 2. إشعار قبل موعد كل حصة بساعة واحدة
            activeSlots.forEachIndexed { slotIdx, (hourKey, rawLabel) ->
                val hour24 = hourKey.substringBefore(":").toIntOrNull() ?: return@forEachIndexed
                val sessionCal = Calendar.getInstance().apply {
                    time = parsedDate
                    set(Calendar.HOUR_OF_DAY, hour24)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val reminderTriggerMillis = sessionCal.timeInMillis - (60L * 60L * 1000L) // قبل الحصة بساعة
                if (reminderTriggerMillis > nowMillis) {
                    val time12 = formatHour12(hourKey, true)
                    val isTeacherClass = rawLabel.contains("(أ/")
                    val subjectName = if (isTeacherClass) rawLabel.substringBefore(" (").trim() else rawLabel
                    val teacherName = if (isTeacherClass) {
                        rawLabel.substringAfter("(").substringBefore(")").trim()
                    } else {
                        val matchedSlot = allSchedules.firstOrNull { s ->
                            s.enrollments.any { it.studentId == student.id && it.isConfirmed } &&
                            slotHourKey(s.time) == hourKey
                        }
                        allTeachers.find { it.id == matchedSlot?.teacherId }?.fullName ?: "مذاكرة ذاتية حسب الجدول"
                    }

                    val reqCode = 20_000 + (dayIndex * 30) + slotIdx
                    val reminderIntent = Intent(context, StudentSessionAlarmReceiver::class.java).apply {
                        action = ACTION_SESSION_REMINDER
                        putExtra("notif_id", reqCode)
                        putExtra("title", "⏰ تذكير: حصتك تبدأ بعد ساعة ($time12)")
                        putExtra("body", "المادة: $subjectName | المعلم: $teacherName | موعد البدء: $time12")
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    scheduleExactOrFallback(alarmManager, reminderTriggerMillis, pi)
                }
            }
        }
    }

    private fun scheduleExactOrFallback(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}

class StudentSessionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == StudentNotificationHelper.ACTION_MORNING_SCHEDULE ||
            action == StudentNotificationHelper.ACTION_SESSION_REMINDER
        ) {
            val notifId = intent.getIntExtra("notif_id", (System.currentTimeMillis() % 100000).toInt())
            val title = intent.getStringExtra("title") ?: "تذكير جدول الحصص - MR Private"
            val body = intent.getStringExtra("body") ?: "لديك حصة دراسية قادمة في جدولك."
            StudentNotificationHelper.showImmediateNotification(context, notifId, title, body)
        }
    }
}
