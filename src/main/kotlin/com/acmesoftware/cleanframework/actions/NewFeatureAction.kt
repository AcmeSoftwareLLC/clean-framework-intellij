package com.acmesoftware.cleanframework.actions

import com.acmesoftware.cleanframework.generators.feature.FeatureGeneratorFactory
import com.google.common.base.CaseFormat
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiFileFactory
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.util.PubspecYamlUtil

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


        val pubspecFile = PubspecYamlUtil.findPubspecYamlFile(project!!, directory.virtualFile)
        val packageName = PubspecYamlUtil.getDartProjectName(pubspecFile!!)!!

        ApplicationManager.getApplication().runWriteAction {
            val runnable = Runnable {
                val featureDirectory = directory.createSubdirectory(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, featureName))

                FeatureGeneratorFactory.getGenerators(packageName, featureName).forEach {
                    var targetDirectory = featureDirectory.findSubdirectory(it.layer)
                    if(targetDirectory == null){
                      targetDirectory = featureDirectory.createSubdirectory(it.layer)
                    }

                    val file = PsiFileFactory.getInstance(project).createFileFromText(it.fileName, DartLanguage.INSTANCE, it.generate())
                    targetDirectory.add(file)
                }
            }

            CommandProcessor.getInstance().executeCommand(project, runnable, "Generate Feature Directory", null)
        }
    }
}