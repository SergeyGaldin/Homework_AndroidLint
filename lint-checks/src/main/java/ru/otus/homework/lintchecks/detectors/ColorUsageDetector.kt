package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

@Suppress("UnstableApiUsage")
class ColorUsageDetector: ResourceXmlDetector() {

    companion object {
        val ISSUE: Issue = Issue.create(
            id = "JobInBuilderUsage",
            briefDescription = "Используемые цвета должны браться из палитры",
            explanation = """
            Все цвета, которые используются в ресурсах приложения должны находится в палитре. 
            За палитру следует принимать цвета, описанные в файле `colors.xml`
        """,
            category = Category.LINT,
            severity = Severity.WARNING,
            implementation = Implementation(
                ColorUsageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }



}