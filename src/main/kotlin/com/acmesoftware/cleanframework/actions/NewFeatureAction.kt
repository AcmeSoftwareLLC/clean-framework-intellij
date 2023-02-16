package com.acmesoftware.cleanframework.actions

import com.acmesoftware.cleanframework.generators.feature.FeatureGeneratorFactory
import com.google.common.base.CaseFormat
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.util.IncorrectOperationException
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
        val application = ApplicationManager.getApplication()
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val view = LangDataKeys.IDE_VIEW.getData(dataContext)

        val focusedVirtualFile = if (view == null) {
            val editor = LangDataKeys.EDITOR.getData(dataContext)!!
            FileDocumentManager.getInstance().getFile(editor.document)!!.parent
        } else {
            view.orChooseDirectory!!.virtualFile
        }

        val pubspecFile = PubspecYamlUtil.findPubspecYamlFile(project!!, focusedVirtualFile)
        val libDir = pubspecFile!!.parent.findChild(PubspecYamlUtil.LIB_DIR_NAME)!!
        val packageName = PubspecYamlUtil.getDartProjectName(pubspecFile)!!

        application.runWriteAction {
            val runnable = Runnable {
                val libDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(libDir)
                val featuresDirectory = libDirectory.findSubdirectory("features")!!
                val fileFactory = PsiFileFactory.getInstance(project)

                try {
                    val lastGeneratedFile = createFeatureDirectory(
                        featuresDirectory, fileFactory, packageName, featureName
                    )
                    createUseCaseProvider(libDirectory, fileFactory, packageName, featureName)

                    FileEditorManager.getInstance(project).openTextEditor(
                        OpenFileDescriptor(project, lastGeneratedFile), true
                    )
                } catch (e: IncorrectOperationException) {
                    e.message?.let { message ->
                        application.invokeLater {
                            Notifications.Bus.notify(
                                Notification(
                                    "com.acmesoftware.notification",
                                    "FEATURE ALREADY EXISTS",
                                    "The feature \"$featureName\" already exists. $message",
                                    NotificationType.ERROR
                                )
                            )
                        }
                    }
                }
            }

            CommandProcessor.getInstance().executeCommand(project, runnable, "Generate Feature Directory", null)
        }
    }

    private fun createFeatureDirectory(
        directory: PsiDirectory,
        fileFactory: PsiFileFactory,
        packageName: String,
        featureName: String,
    ): VirtualFile {
        val featureNameSnake = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, featureName)
        val featureDirectory = directory.createSubdirectory(featureNameSnake)

        lateinit var file: PsiFile
        FeatureGeneratorFactory.getGenerators(packageName, featureName).forEach {
            var targetDirectory = featureDirectory.findSubdirectory(it.layer)
            if (targetDirectory == null) {
                targetDirectory = featureDirectory.createSubdirectory(it.layer)
            }

            file = fileFactory.createFileFromText(it.fileName, DartLanguage.INSTANCE, it.generate())
            targetDirectory.add(file)
        }

        return file.virtualFile
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