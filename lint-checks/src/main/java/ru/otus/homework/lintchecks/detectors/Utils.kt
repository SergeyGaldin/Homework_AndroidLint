package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.UClass

fun UClass.isExtendClass(nameClass: String) = this.supers.any {
    it.qualifiedName == nameClass
}

fun JavaContext.isArtifactInDependencies(dependency: String) =
    evaluator.dependencies?.getAll()?.any {
        it.identifier.contains(dependency)
    } ?: false