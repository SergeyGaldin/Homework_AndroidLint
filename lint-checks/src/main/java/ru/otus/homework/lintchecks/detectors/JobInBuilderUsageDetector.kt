package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.getUMethod
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression

@Suppress("UnstableApiUsage")
class JobInBuilderUsageDetector : Detector(), Detector.UastScanner {

    companion object {
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
        private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
        private const val JOB = "kotlinx.coroutines.Job"
        private const val SUPERVISOR_JOB = "kotlinx.coroutines.SupervisorJob"
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

    override fun getApplicableMethodNames(): List<String> = listOf("launch", "async")

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
                    quickfixData = createFix(
                        context = context,
                        node = arg,
                        parentNode = node
                    )
                )
            }
        }
    }

    private fun createFix(
        context: JavaContext,
        node: UExpression,
        parentNode: UExpression
    ): LintFix? {
        val containingClass = node.getContainingUClass()

        if (
            containingClass?.isExtendClass(VIEW_MODEL_CLASS_QUALIFIED_NAME) == true
            && context.isArtifactInDependencies(LIFECYCLE_VIEW_MODEL_DEPENDENCY)
            && isSupervisorJob(context, node)
        ) {
            return createSupervisorJobFix(context, node)
        }

        if (
            containingClass?.isExtendClass(VIEW_MODEL_CLASS_QUALIFIED_NAME) == true
            && context.isArtifactInDependencies(LIFECYCLE_VIEW_MODEL_DEPENDENCY)
            && isNonCancelableJob(context, node)
        ) {
            return createNonCancelableJobFix(context, parentNode)
        }

        return null
    }

    private fun isSupervisorJob(
        context: JavaContext,
        node: UExpression
    ): Boolean {
        val param = context.evaluator.getTypeClass(node.getExpressionType())

        if (node.isSupervisorJob(context)) return true

        if (
            node is KotlinUBinaryExpression
            && context.evaluator.inheritsFrom(param, COROUTINE_CONTEXT, false)
        ) {
            node.operands.forEach {
                if (it.isSupervisorJob(context)) return true
            }
        }

        return false
    }

    private fun UExpression.isSupervisorJob(
        context: JavaContext
    ): Boolean = if (this is UCallExpression) {
        val uMethod = resolve()?.getUMethod()
        val packageName = context.evaluator.getPackage(uMethod!!)?.qualifiedName
        val methodName = methodName
        "$packageName.$methodName" == SUPERVISOR_JOB
    } else false

    private fun createSupervisorJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix {
        val newTextStringBuilder = StringBuilder()

        val oldText = node.sourcePsi?.text
        val operands = oldText?.split("+")?.toMutableList()
        operands?.removeIf { it.contains("SupervisorJob") }
        operands?.forEachIndexed { index, string ->
            if (index == 0) newTextStringBuilder.append(string.trim())
            else newTextStringBuilder.append(" + " + string.trim())
        }

        val newText = newTextStringBuilder.toString().trim()

        return fix()
            .replace()
            .range(context.getLocation(node))
            .all()
            .with(newText)
            .name("Удалить вызов функции SupervisorJob")
            .build()
    }

    private fun isNonCancelableJob(
        context: JavaContext,
        node: UExpression
    ): Boolean {
        val param = context.evaluator.getTypeClass(node.getExpressionType())
        return context.evaluator.inheritsFrom(param, NON_CANCELABLE, false)
    }

    private fun createNonCancelableJobFix(
        context: JavaContext,
        node: UExpression
    ): LintFix? = if (isInCoroutine(node)) fix()
        .replace()
        .range(context.getLocation(node))
        .text((node as UCallExpression).methodName)
        .with("withContext")
        .build()
    else null

    private fun isInCoroutine(uElement: UElement?): Boolean = when {
        uElement == null || uElement is UMethod -> false
        uElement is UCallExpression && getApplicableMethodNames().any { it == uElement.methodName } -> true
        else -> this.isInCoroutine(uElement.uastParent)
    }
}