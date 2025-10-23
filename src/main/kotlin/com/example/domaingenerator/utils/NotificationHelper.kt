package com.example.domaingenerator.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * Helper for showing notifications to users.
 */
object NotificationHelper {

    private const val NOTIFICATION_GROUP = "DomainGenerator.Notifications"

    fun showInfo(project: Project, message: String) {
        show(project, message, NotificationType.INFORMATION)
    }

    fun showWarning(project: Project, message: String) {
        show(project, message, NotificationType.WARNING)
    }

    fun showError(project: Project, message: String) {
        show(project, message, NotificationType.ERROR)
    }

    private fun show(
        project: Project,
        message: String,
        type: NotificationType
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(message, type)
            .notify(project)
    }
}
