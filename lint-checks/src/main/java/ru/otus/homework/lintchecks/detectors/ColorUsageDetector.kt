package ru.otus.homework.lintchecks.detectors

import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScannerConstants
import org.w3c.dom.Attr
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class ColorUsageDetector : ResourceXmlDetector() {
    private val arbitraryColors = ArrayList<Pair<String, Location>>()
    private val colorMap = HashMap<String, ArrayList<String>>()

    companion object {
        val ISSUE: Issue = Issue.create(
            id = "ColorUsage",
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

    override fun getApplicableAttributes(): Collection<String> = XmlScannerConstants.ALL

    override fun getApplicableElements(): Collection<String> = XmlScannerConstants.ALL

    override fun appliesTo(folderType: ResourceFolderType) =
        folderType == ResourceFolderType.LAYOUT ||
                folderType == ResourceFolderType.DRAWABLE ||
                folderType == ResourceFolderType.COLOR ||
                folderType == ResourceFolderType.VALUES

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val value = attribute.value ?: return
        if (value.isColor()) {
            arbitraryColors.add(value.lowercase() to context.getValueLocation(attribute))
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (!context.file.path.contains("colors.xml")) return
        val value = element.firstChild.nodeValue.lowercase()
        if (value.isColor()) {
            val colorName = element.attributes.item(0)?.nodeValue ?: return
            if (!colorMap.containsKey(value)) {
                colorMap[value] = ArrayList()
            }
            colorMap[value]!!.add(colorName)
        }
    }

    override fun beforeCheckRootProject(context: Context) {
        arbitraryColors.clear()
        colorMap.clear()
    }

    override fun afterCheckRootProject(context: Context) {
        arbitraryColors.forEach { (key, location) ->
            val fixValues = colorMap[key]

            if (!fixValues.isNullOrEmpty()) {
                context.report(
                    ISSUE,
                    location,
                    "Используемые цвета должны браться из палитры.",
                    createFix(fixValues[0], location)
                )
            } else {
                context.report(
                    ISSUE,
                    location,
                    "Используемые цвета должны браться из палитры."
                )
            }
        }
    }

    private fun createFix(
        fixValue: String,
        location: Location
    ) = fix()
        .replace()
        .range(location)
        .all()
        .with("@color/$fixValue")
        .build()

    private fun String.isColor(): Boolean = (length == 7 || length == 9) && startsWith("#")

}