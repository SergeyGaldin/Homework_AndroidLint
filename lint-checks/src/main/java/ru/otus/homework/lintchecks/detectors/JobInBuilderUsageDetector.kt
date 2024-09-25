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
        private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
        private const val JOB = "kotlinx.coroutines.Job"
        private const val SUPERVISOR_JOB = "kotlinx.coroutines.SupervisorJob"
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
        private const val NON_CANCELABLE = "kotlinx.coroutines.NonCancellable"

        private const val VIEW_MODEL_CLASS_QUALIFIED_NAME = "androidx.lifecycle.ViewModel"
        private const val LIFECYCLE_VIEW_MODEL_DEPENDENCY =
            "androidx.lifecycle:lifecycle-viewmodel-ktx"

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
        val receiver = context.evaluator.getTypeClass(node.receiverType)
        if (!context.evaluator.inheritsFrom(receiver, COROUTINE_SCOPE, false)) return

        node.valueArguments.forEach { arg ->
            val param = context.evaluator.getTypeClass(arg.getExpressionType())
            if (
                context.evaluator.inheritsFrom(param, JOB, false) ||
                context.evaluator.inheritsFrom(param, COROUTINE_CONTEXT, false)
            ) {
                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(arg),
                    message = "Использование Job или SupervisorJob в конструкторе сопрограмм не допускается.",
                    quickfixData = null
                )
            }
        }
    }
}