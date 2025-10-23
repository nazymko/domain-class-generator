package com.example.domaingenerator.actions

import com.example.domaingenerator.services.DomainGeneratorService
import com.example.domaingenerator.ui.GeneratorConfigDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

/**
 * Main action for generating domain classes from library classes.
 * Triggered by Ctrl+Shift+E (Cmd+Shift+E on Mac).
 */
class GenerateDomainClassesAction : AnAction("Generate Domain Classes"), DumbAware {

    /**
     * Specify threading model for action updates (lightweight operations on BGT).
     */
    override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    /**
     * Update action state - enable only when we have a project and Java file context.
     */
    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.PSI_FILE)

        // Enable only if we have a project and a file
        e.presentation.isEnabledAndVisible = project != null && file != null
    }

    /**
     * Called when user triggers the action (Ctrl+Shift+E).
     * Opens configuration dialog and generates domain classes.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Show configuration dialog
        val dialog = GeneratorConfigDialog(project)
        if (dialog.showAndGet()) {
            // User clicked OK, get configuration
            val config = dialog.getConfiguration()

            // Delegate to service for actual generation
            val service = DomainGeneratorService.getInstance(project)
            service.generateDomainClasses(config)
        }
    }
}
