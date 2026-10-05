package de.easynfp.app

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.easynfp.app.reminders.ReminderScheduler
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestTest {
    @Test
    fun `N-01 no network permission, N-02 no cloud backup`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions.orEmpty().toList()
        assertFalse("android.permission.INTERNET" in permissions, permissions.toString())
        assertFalse(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
    }

    @Test
    fun `F-23 next reminder time`() {
        val now = LocalDateTime.of(2026, 10, 5, 7, 0)
        assertEquals(LocalDateTime.of(2026, 10, 6, 6, 30), ReminderScheduler.nextOccurrence(now, LocalTime.of(6, 30)))
        assertEquals(LocalDateTime.of(2026, 10, 5, 21, 0), ReminderScheduler.nextOccurrence(now, LocalTime.of(21, 0)))
    }
}
