package com.acmesoftware.cleanframework.actions

import com.acmesoftware.cleanframework.generators.feature.FeatureGeneratorFactory
import com.google.common.base.CaseFormat
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
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
        val libDir = pubspecFile!!.parent.findChild(PubspecYamlUtil.LIB_DIR_NAME)!!
        val packageName = PubspecYamlUtil.getDartProjectName(pubspecFile)!!

        ApplicationManager.getApplication().runWriteAction {
            val runnable = Runnable {
                val libDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(libDir)
                val fileFactory = PsiFileFactory.getInstance(project)

                createFeatureDirectory(directory, fileFactory, packageName, featureName)
                createUseCaseProvider(libDirectory, fileFactory, packageName, featureName)
            }

            CommandProcessor.getInstance().executeCommand(project, runnable, "Generate Feature Directory", null)
        }
    }

    private fun createFeatureDirectory(
        directory: PsiDirectory,
        fileFactory: PsiFileFactory,
        packageName: String,
        featureName: String,
    ) {
        val featureNameSnake = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, featureName)
        val featureDirectory = directory.createSubdirectory(featureNameSnake)

        FeatureGeneratorFactory.getGenerators(packageName, featureName).forEach {
            var targetDirectory = featureDirectory.findSubdirectory(it.layer)
            if (targetDirectory == null) {
                targetDirectory = featureDirectory.createSubdirectory(it.layer)
            }

            val file = fileFactory.createFileFromText(it.fileName, DartLanguage.INSTANCE, it.generate())
            targetDirectory.add(file)
        }
    }

    private fun createUseCaseProvider(
        libDirectory: PsiDirectory,
        fileFactory: PsiFileFactory,
        packageName: String,
        featureName: String,
    ) {
        val featureNameSnake = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, featureName)
        val providersDirectory = libDirectory.findSubdirectory("providers")
        val useCaseProvidersFileName = "use_case_providers.dart"

        providersDirectory?.findFile(useCaseProvidersFileName)?.let {
            val featureNameCamel = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, featureName)

            val providersBuilder = StringBuilder(it.text)

            val cfImport = "import 'package:clean_framework/clean_framework.dart';"
            if (!providersBuilder.contains(cfImport)) {
                providersBuilder.insert(0, "$cfImport\n")
            }

            val lastImportIndex = providersBuilder.lastIndexOf("import")
            val importIndex = providersBuilder.indexOf("\n", lastImportIndex)


            providersBuilder.insert(
                importIndex + 1,
                "import 'package:$packageName/features/${featureNameSnake}/domain/${featureNameSnake}_use_case.dart';\n"
            )
            providersBuilder.append(
                "\nfinal ${featureNameCamel}UseCaseProvider = UseCaseProvider(${featureName}UseCase.new);\n"
            )
            val file = fileFactory.createFileFromText(
                useCaseProvidersFileName, DartLanguage.INSTANCE, providersBuilder.toString()
            )
            it.delete()
            providersDirectory.add(file)
        }
    }
}