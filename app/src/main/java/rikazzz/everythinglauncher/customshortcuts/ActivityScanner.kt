package rikazzz.everythinglauncher.customshortcuts

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScannedActivity(
    val componentName: ComponentName,
    val label: String,
    val isExported: Boolean,
    val isLauncher: Boolean
)

data class ScannedApp(
    val packageName: String,
    val appName: String,
    val activities: List<ScannedActivity>
)

object ActivityScanner {
    suspend fun scanInstalledApps(context: Context): List<ScannedApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
            } else 0
            pm.getInstalledPackages(flags)
        } catch (e: Exception) {
            emptyList()
        }

        val result = mutableListOf<ScannedApp>()

        for (pkg in packages) {
            val packageName = pkg.packageName
            val appInfo = pkg.applicationInfo ?: continue
            val appName = appInfo.loadLabel(pm)?.toString() ?: packageName

            val fullPkgInfo = try {
                var actFlags = PackageManager.GET_ACTIVITIES
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    actFlags = actFlags or PackageManager.MATCH_DISABLED_COMPONENTS
                }
                pm.getPackageInfo(packageName, actFlags)
            } catch (e: Exception) {
                null
            }

            val activitiesArray = fullPkgInfo?.activities ?: emptyArray()
            if (activitiesArray.isEmpty()) continue

            val activityList = mutableListOf<ScannedActivity>()
            for (act in activitiesArray) {
                val actLabel = act.loadLabel(pm)?.toString() ?: act.name.substringAfterLast('.')
                activityList.add(
                    ScannedActivity(
                        componentName = ComponentName(packageName, act.name),
                        label = actLabel,
                        isExported = act.exported,
                        isLauncher = act.name == appInfo.className
                    )
                )
            }

            result.add(
                ScannedApp(
                    packageName = packageName,
                    appName = appName,
                    activities = activityList.sortedBy { it.label }
                )
            )
        }

        result.sortedBy { it.appName.lowercase() }
    }
}
