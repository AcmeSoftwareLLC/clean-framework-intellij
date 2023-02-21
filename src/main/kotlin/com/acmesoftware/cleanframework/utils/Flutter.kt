package com.acmesoftware.cleanframework.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.flutter.pub.PubRoot
import io.flutter.sdk.FlutterSdk

object Flutter {
    fun getPackages(project: Project, pubspecFile: VirtualFile) {
        PubRoot.forDirectory(pubspecFile.parent)?.let { pubRoot ->
            pubRoot.getModule(project)?.let { module ->
                FlutterSdk.getFlutterSdk(project)?.flutterPackagesGet(pubRoot)
                    ?.startInModuleConsole(module, { pubRoot.refresh() }, null)
            }
        }
    }
}