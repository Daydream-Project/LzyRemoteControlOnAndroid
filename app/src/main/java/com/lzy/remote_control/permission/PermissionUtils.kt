package com.lzy.remote_control.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.app.ActivityCompat


class PermissionUtils {
    companion object {
        //Get all permissions registered in AndroidManifest.xml
        fun readNeededPermissions(context: Context): Array<String>?
        {
            val packageInfo: PackageInfo = context.applicationContext.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            return packageInfo.requestedPermissions
        }
        //Get a array of permissions, test this permissions is allowed
        fun isPermissionsAllowed(permissions: Array<String>, context: Context): Array<Boolean> {
            val result = Array<Boolean>(permissions.size) { _ -> false }
            for ((index, permission) in permissions.withIndex()) {
                if (context.checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED)
                    result[index] = true
            }
            return result
        }
        //Get all not allowed permissions registered in AndroidManifest.xml
        fun getNotAllowedPermissions(context: Context): Array<String> {
            val allPermissions = readNeededPermissions(context) ?: throw RuntimeException("Can not get permissions for application.")

            val permissionsAllowed = isPermissionsAllowed(allPermissions, context)

            val notAllowedPermissionsIndex = mutableListOf<Int>()

            for ((index, isAllowed) in permissionsAllowed.withIndex()) {
                if (!isAllowed)
                    notAllowedPermissionsIndex.add(index)
            }

            val notAllowedPermissions = mutableListOf<String>()

            for (index in notAllowedPermissionsIndex)
                notAllowedPermissions.add(allPermissions[index])

            return notAllowedPermissions.toTypedArray()
        }
        //Get a array of permissions, request them foreach and the requestCode is the index of permission.
        fun requestPermissions(permissions: Array<String>, activity: Activity): Unit {
            for ((index, permission) in permissions.withIndex()) {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), index)
            }
        }
    }
}