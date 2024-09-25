package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {

    companion object {
        private const val VIEW_MODEL_CLASS_QUALIFIED_NAME = "androidx.lifecycle.ViewModel"
        private const val LIFECYCLE_VIEW_MODEL_DEPENDENCY = "androidx.lifecycle:lifecycle-viewmodel-ktx"

        val ISSUE: Issue = Issue.create(
            id = "JobInBuilderUsage",
            briefDescription = "Job не должен передаваться в конструктор сопрограмм",
            explanation = """
            Использование экземпляра Job внутри конструкторов сопрограмм не имеет никакого эффекта,
            так же может нарушить ожидаемую обработку ошибок и отмену сопрограмм
        """,
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                JobInBuilderUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

    }
}