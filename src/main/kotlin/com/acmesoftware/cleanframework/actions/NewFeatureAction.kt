package com.acmesoftware.cleanframework.actions

import com.acmesoftware.cleanframework.generators.feature.FeatureGeneratorFactory
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiFileFactory

class NewFeatureAction : AnAction(), GenerateFeatureDialog.Callback {
    private lateinit var dataContext: DataContext

    override fun actionPerformed(e: AnActionEvent) {
        val dialog = GenerateFeatureDialog(this)
        dialog.show()
    }

    override fun update(e: AnActionEvent) {
        e.dataContext.let {
            this.dataContext = it
            val presentation = e.presentation
            presentation.isEnabled = true
        }
    }

    override fun onGenerateFeature(featureName: String) {
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val view = LangDataKeys.IDE_VIEW.getData(dataContext)
        val directory = view!!.orChooseDirectory!!

        ApplicationManager.getApplication().runWriteAction {
            val runnable = Runnable {
                val featureDirectory = directory.createSubdirectory(FeatureGeneratorFactory.snakeCase(featureName))

                FeatureGeneratorFactory.getGenerators(featureName).forEach {
                    var targetDirectory = featureDirectory.findSubdirectory(it.layer)
                    if(targetDirectory == null){
                      targetDirectory = featureDirectory.createSubdirectory(it.layer)
                    }

                    val file = PsiFileFactory.getInstance(project).createFileFromText(it.fileName, JavaLanguage.INSTANCE, it.generate())
                    targetDirectory.add(file)
                }
            }

            CommandProcessor.getInstance().executeCommand(project, runnable, "Generate Feature Directory", null)
        }
    }
}