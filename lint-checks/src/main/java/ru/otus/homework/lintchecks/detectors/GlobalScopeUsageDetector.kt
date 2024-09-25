package ru.otus.homework.lintchecks.detectors

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass

@Suppress("UnstableApiUsage")
class GlobalScopeUsageDetector : Detector(), Detector.UastScanner {

    companion object {
        private const val GLOBAL_SCOPE = "kotlinx.coroutines.GlobalScope"

        private const val VIEW_MODEL_CLASS_QUALIFIED_NAME = "androidx.lifecycle.ViewModel"
        private const val FRAGMENT_CLASS_QUALIFIED_NAME = "androidx.fragment.app.Fragment"

        private const val LIFECYCLE_VIEW_MODEL_DEPENDENCY =
            "androidx.lifecycle:lifecycle-viewmodel-ktx"
        private const val LIFECYCLE_RUNTIME_DEPENDENCY = "androidx.lifecycle:lifecycle-runtime-ktx"

        val ISSUE: Issue = Issue.create(
            id = "GlobalScopeUsage",
            briefDescription = "Недопустимое использование GlobalScope",
            explanation = """
            Использование GlobalScope может приводить к утечкам памяти и ненужному потреблению ресурсов.
            В классах ViewModel используйте viewModelScope, а в Fragment — lifecycleScope.
        """,
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                GlobalScopeUsageDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        USimpleNameReferenceExpression::class.java
    )

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
            if (node.identifier == "GlobalScope") {
                val receiver = context.evaluator.getTypeClass(node.getExpressionType())
                if (context.evaluator.inheritsFrom(receiver, GLOBAL_SCOPE, false)) {
                    handleGlobalScopeUsage(context, node)
                }
            }
        }
    }

    private fun handleGlobalScopeUsage(context: JavaContext, node: UElement) {
        val containingClass = node.getContainingUClass() ?: return

        val fix = when {
            containingClass.isExtendClass(VIEW_MODEL_CLASS_QUALIFIED_NAME)
                    && context.isArtifactInDependencies(LIFECYCLE_VIEW_MODEL_DEPENDENCY)
            -> createFixForViewModel()

            containingClass.isExtendClass(FRAGMENT_CLASS_QUALIFIED_NAME)
                    && context.isArtifactInDependencies(LIFECYCLE_RUNTIME_DEPENDENCY)
            -> createFixForFragment()

            else -> null
        }

        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = "Использование GlobalScope может привести к утечкам памяти",
            quickfixData = fix
        )
    }

    private fun createFixForViewModel(): LintFix = quickFix(
        name = "Replace with viewModelScope",
        scopeReplacement = "viewModelScope"
    )

    private fun createFixForFragment(): LintFix = quickFix(
        name = "Replace with lifecycleScope",
        scopeReplacement = "lifecycleScope"
    )

    private fun quickFix(name: String, scopeReplacement: String): LintFix = fix()
        .name(name)
        .replace()
        .text("GlobalScope")
        .with(scopeReplacement)
        .build()
}