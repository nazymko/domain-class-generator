package com.example.domaingenerator.actions

import com.example.domaingenerator.services.DomainGeneratorService
import com.example.domaingenerator.ui.GeneratorConfigDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiTreeUtil

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
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        // Enable only if we have a project and a Java file
        e.presentation.isEnabledAndVisible =
            project != null && psiFile is PsiJavaFile
    }

    /**
     * Called when user triggers the action (Ctrl+Shift+E).
     * Opens configuration dialog and generates domain classes.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return

        // Read PSI data in a read action
        val contextData = runReadAction {
            // Try to find the PsiClass at cursor position
            var detectedClass: PsiClass? = null
            var detectedPackage: String? = null

            if (editor != null) {
                // Find class at caret position
                val offset = editor.caretModel.offset
                val element = psiFile.findElementAt(offset)
                detectedClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
            }

            // If no class at cursor, try to get the first top-level class from the file
            if (detectedClass == null) {
                detectedClass = psiFile.classes.firstOrNull()
            }

            // Extract package from the detected class or file
            if (detectedClass != null) {
                detectedPackage = (detectedClass.containingFile as? PsiJavaFile)?.packageName
            } else {
                detectedPackage = psiFile.packageName
            }

            Pair(detectedClass, detectedPackage)
        }

        val (detectedClass, detectedPackage) = contextData

        // Show dialog on EDT
        invokeLater {
            // Show configuration dialog with detected class and package
            val dialog = GeneratorConfigDialog(
                project = project,
                detectedClass = detectedClass,
                detectedPackage = detectedPackage
            )

            if (dialog.showAndGet()) {
                // User clicked OK, get configuration
                val config = dialog.getConfiguration()

                // Delegate to service for actual generation
                val service = DomainGeneratorService.getInstance(project)
                service.generateDomainClasses(config)
            }
        }
    }
}
