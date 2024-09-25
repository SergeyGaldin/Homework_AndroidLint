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
        private const val GLOBAL_SCOPE = "GlobalScope"
        private const val VIEW_MODEL = "androidx.lifecycle.ViewModel"
        private const val FRAGMENT = "androidx.fragment.app.Fragment"

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
            if (node.identifier == GLOBAL_SCOPE) handleGlobalScopeUsage(context, node)
        }
    }

    private fun handleGlobalScopeUsage(context: JavaContext, node: UElement) {
        val containingClass = node.getContainingUClass() ?: return

        val evaluator = context.evaluator
        val isViewModel = evaluator.extendsClass(containingClass.javaPsi, VIEW_MODEL, false)
        val isFragment = evaluator.extendsClass(containingClass.javaPsi, FRAGMENT, false)

        val hasViewModelScope = hasViewModelScope(context)
        val hasLifecycleScope = hasLifecycleScope(context)

        val fix: LintFix? = if (isViewModel && hasViewModelScope) createFixForViewModel()
        else if (isFragment && hasLifecycleScope) createFixForFragment()
        else null

        context.report(
            ISSUE,
            node,
            context.getLocation(node),
            "Использование GlobalScope может привести к утечкам памяти",
            fix
        )
    }

    private fun hasViewModelScope(context: JavaContext): Boolean {
        return context.evaluator.findClass("androidx.lifecycle.viewModelScope") != null
    }

    private fun hasLifecycleScope(context: JavaContext): Boolean {
        return context.evaluator.findClass("androidx.lifecycle.lifecycleScope") != null
    }

    private fun createFixForViewModel(): LintFix {
        return quickFix("Replace with viewModelScope", "viewModelScope")
    }

    private fun createFixForFragment(): LintFix {
        return quickFix("Replace with lifecycleScope", "lifecycleScope")
    }

    private fun quickFix(name: String, scopeReplacement: String): LintFix {
        return fix().name(name)
            .replace()
            .text("GlobalScope")
            .with(scopeReplacement)
            .build()
    }
}