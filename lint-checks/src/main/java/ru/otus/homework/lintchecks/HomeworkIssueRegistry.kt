package ru.otus.homework.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue
import ru.otus.homework.lintchecks.detectors.ColorUsageDetector
import ru.otus.homework.lintchecks.detectors.GlobalScopeUsageDetector
import ru.otus.homework.lintchecks.detectors.JobInBuilderUsageDetector

@Suppress("UnstableApiUsage")
class HomeworkIssueRegistry : IssueRegistry() {

    override val issues: List<Issue>
        get() = listOf(
            GlobalScopeUsageDetector.ISSUE,
            JobInBuilderUsageDetector.ISSUE,
            ColorUsageDetector.ISSUE
        )

    override val api: Int get() = com.android.tools.lint.detector.api.CURRENT_API
}