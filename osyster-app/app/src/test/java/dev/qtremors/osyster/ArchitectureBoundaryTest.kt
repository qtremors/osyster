package dev.qtremors.osyster

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import java.io.File
import org.junit.Assert.fail
import org.junit.Test

/**
 * The architecture rules as tests. Class rules read compiled classes from each module's build
 * folder, so run after a debug compile (any testDebugUnitTest does this). Source rules read files.
 *
 * Budgets: files 700 lines, ViewModels 500, public composables 15 parameters. Larger files that
 * already exist go in the baseline maps as ceilings and must shrink, never grow.
 */
class ArchitectureBoundaryTest {

    // ---------- Class graph rules (ArchUnit) ----------

    @Test
    fun `features do not depend on each other`() {
        slices().matching("$BASE.feature.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .check(productionClasses())
    }

    @Test
    fun `features do not depend on concrete data implementations`() {
        noClasses().that().resideInAPackage("$BASE.feature..")
            .should().dependOnClassesThat().resideInAPackage("$BASE.core..data..")
            .because("features talk to domain contracts")
            .allowEmptyShould(true)
            .check(productionClasses())
    }

    @Test
    fun `core does not depend on the app shell or features`() {
        noClasses().that().resideInAPackage("$BASE.core..")
            .should().dependOnClassesThat().resideInAnyPackage("$BASE.feature..", "$BASE.presentation..")
            .because("dependencies point inward")
            .allowEmptyShould(true)
            .check(productionClasses())
    }

    @Test
    fun `domain and api packages have no android or compose classes`() {
        noClasses().that().resideInAnyPackage("$BASE..domain..", "$BASE..api..")
            .should().dependOnClassesThat().resideInAnyPackage("android..", "androidx..")
            .because("domain code stays pure Kotlin")
            .allowEmptyShould(true)
            .check(productionClasses())
    }

    @Test
    fun `data layers do not depend on presentation features or theme`() {
        noClasses().that().resideInAPackage("$BASE.core..data..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "$BASE.presentation..", "$BASE.feature..", "$BASE.core.ui.."
            )
            .allowEmptyShould(true)
            .check(productionClasses())
    }

    // ---------- Source rules ----------

    @Test
    fun `gradle module dependencies follow architecture direction`() {
        val dependency = Regex("""project\(\s*"(:[^"]+)"\s*\)""")
        val offenders = buildFiles().flatMap { file ->
            val owner = ":" + requireNotNull(file.parentFile).relativeTo(projectRoot()).invariantSeparatorsPath.replace('/', ':')
            file.readLines().flatMapIndexed { index, line ->
                dependency.findAll(line).mapNotNull { match ->
                    val target = match.groupValues[1]
                    val invalid = (owner.startsWith(":feature:") && target.startsWith(":feature:")) ||
                        (owner.startsWith(":core:") && (target == ":app" || target.startsWith(":feature:")))
                    "$owner -> $target at ${file.name}:${index + 1}".takeIf { invalid }
                }.toList()
            }
        }.toList()
        failIfAny("Module dependencies point the wrong way", offenders)
    }

    @Test
    fun `production ui strings come from resources`() {
        val literal = """"(?=[^"]*[A-Za-z])(?:[^"\\$]|\\.|[$]\{[^}]*\}|[$]\w+)*""""
        val patterns = listOf(
            Regex("""\bText\(\s*$literal"""),
            Regex("""\b(?:contentDescription|placeholder|title|text|onClickLabel)\s*=\s*$literal"""),
            Regex("""\b(?:Toast\.makeText)\([^"]*$literal""")
        )
        val offenders = productionKotlinFiles()
            .filterNot { "${File.separator}theme${File.separator}" in it.path }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || "android.os.Build." in trimmed) return@mapIndexedNotNull null
                    location(file, index, trimmed).takeIf { patterns.any { it.containsMatchIn(trimmed) } }
                }
            }.toList()
        failIfAny("Move hardcoded UI strings into strings.xml", offenders)
    }

    @Test
    fun `production code uses the expressive switch`() {
        val offenders = productionKotlinFiles().flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                location(file, index, line.trim()).takeIf { line.trim() == "import androidx.compose.material3.Switch" }
            }
        }.toList()
        failIfAny("Use ExpressiveSwitch so every switch looks and behaves the same", offenders)
    }

    @Test
    fun `data layers take dispatchers from AppDispatchers`() {
        val offenders = productionKotlinFiles()
            .filter { "${File.separator}data${File.separator}" in it.path }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    location(file, index, line.trim()).takeIf { Regex("""\bDispatchers\.IO\b""").containsMatchIn(line) }
                }
            }.toList()
        failIfAny("Data layers must use injected AppDispatchers", offenders)
    }

    @Test
    fun `production files identify a cohesive responsibility`() {
        val bucket = Regex("""(?:Models|Helpers|Utils|Contracts)\.kt$""")
        failIfAny(
            "Generic file names hide ownership; name files for what they own",
            productionKotlinFiles().filter { bucket.containsMatchIn(it.name) }.map { it.relativeTo(projectRoot()).path }.toList()
        )
    }

    @Test
    fun `large files stay within the architecture budget`() {
        val offenders = productionKotlinFiles().mapNotNull { file ->
            val path = file.relativeTo(projectRoot()).invariantSeparatorsPath
            budgetViolation(path, file.readLines().size, MAX_FILE_LINES, LARGE_FILE_BASELINE[path])
        }.toList()
        failIfAny("Files above $MAX_FILE_LINES lines must be split or baselined", offenders)
    }

    @Test
    fun `viewmodels stay within the architecture budget`() {
        val offenders = productionKotlinFiles().filter { it.name.endsWith("ViewModel.kt") }.mapNotNull { file ->
            val path = file.relativeTo(projectRoot()).invariantSeparatorsPath
            budgetViolation(path, file.readLines().size, MAX_VIEWMODEL_LINES, LARGE_VIEWMODEL_BASELINE[path])
        }.toList()
        failIfAny("ViewModels above $MAX_VIEWMODEL_LINES lines must be split", offenders)
    }

    @Test
    fun `public composables stay within the parameter budget`() {
        val offenders = productionKotlinFiles().flatMap { file ->
            val lines = file.readLines()
            lines.indices.filter { lines[it].trim() == "@Composable" }.mapNotNull { annotation ->
                var at = annotation + 1
                while (at < lines.size && lines[at].trim().startsWith("@")) at++
                val name = PUBLIC_FUNCTION.matchEntire(lines.getOrElse(at) { "" }.trim())?.groupValues?.get(1)
                    ?: return@mapNotNull null
                val count = parameterCount(lines, at)
                val key = "${file.relativeTo(projectRoot()).invariantSeparatorsPath}:$name"
                budgetViolation(key, count, MAX_COMPOSABLE_PARAMETERS, COMPOSABLE_PARAMETER_BASELINE[key])
            }
        }.toList()
        failIfAny("Public composables above $MAX_COMPOSABLE_PARAMETERS parameters must be split", offenders)
    }

    // ---------- Helpers ----------

    private fun productionClasses(): JavaClasses = imported ?: synchronized(this) {
        imported ?: ClassFileImporter().importPaths(classDirs().map { it.toPath() }).also { imported = it }
    }

    /** Debug classes of every Android module and main classes of every pure Kotlin module. */
    private fun classDirs(): List<File> = buildFiles()
        .map { it.parentFile }
        .flatMap { module -> CLASS_OUTPUTS.asSequence().map { File(module, it) } }
        .filter(File::isDirectory)
        .toList()

    private fun projectRoot(): File =
        generateSequence(File(System.getProperty("user.dir") ?: ".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: File(".").absoluteFile

    private fun buildFiles(): Sequence<File> = projectRoot().walkTopDown()
        .onEnter { it.name !in SKIPPED_DIRS }
        .filter { it.isFile && it.name == "build.gradle.kts" && it.parentFile != projectRoot() }

    private fun productionKotlinFiles(): Sequence<File> = projectRoot().walkTopDown()
        .onEnter { it.name !in SKIPPED_DIRS }
        .filter { it.isFile && it.extension == "kt" && "${File.separator}src${File.separator}main${File.separator}" in it.path }

    private fun parameterCount(lines: List<String>, start: Int): Int {
        val signature = StringBuilder()
        var depth = 0
        loop@ for (index in start until lines.size) {
            for (char in lines[index]) {
                if (char == '(') depth++
                if (depth > 0) signature.append(char)
                if (char == ')') { depth--; if (depth == 0) break@loop }
            }
            signature.append('\n')
        }
        val params = signature.toString().removePrefix("(").removeSuffix(")")
        if (params.isBlank()) return 0
        var nested = 0
        var commas = 0
        params.forEach { char ->
            when (char) {
                '(', '<', '[' -> nested++
                ')', '>', ']' -> if (nested > 0) nested--
                ',' -> if (nested == 0) commas++
            }
        }
        return commas + 1 - if (params.trimEnd().endsWith(",")) 1 else 0
    }

    private fun budgetViolation(key: String, size: Int, limit: Int, baseline: Int?): String? = when {
        size <= limit -> null
        baseline != null && size <= baseline -> null
        baseline != null -> "$key: $size exceeds baseline $baseline"
        else -> "$key: $size"
    }

    private fun location(file: File, index: Int, line: String) =
        "${file.relativeTo(projectRoot()).invariantSeparatorsPath}:${index + 1}: $line"

    private fun failIfAny(message: String, offenders: List<String>) {
        if (offenders.isNotEmpty()) fail("$message:\n${offenders.joinToString("\n")}")
    }

    private companion object {
        const val BASE = "dev.qtremors.osyster"
        const val MAX_FILE_LINES = 700
        const val MAX_VIEWMODEL_LINES = 500
        const val MAX_COMPOSABLE_PARAMETERS = 15
        val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".idea", ".kotlin")
        // Where AGP and the Kotlin JVM plugin put compiled classes. Check after AGP upgrades.
        val CLASS_OUTPUTS = listOf(
            "build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes",
            "build/tmp/kotlin-classes/debug",
            "build/classes/kotlin/main"
        )
        val PUBLIC_FUNCTION = Regex("""^(?:public\s+)?fun\s+([A-Za-z0-9_]+)\s*\(.*""")
        @Volatile var imported: JavaClasses? = null

        /** Debt recorded at adoption. These are ceilings: lower them as files are split. */
        val LARGE_FILE_BASELINE = mapOf<String, Int>(
            // "feature/x/src/main/java/dev/qtremors/osyster/feature/x/BigScreen.kt" to 820,
        )
        val LARGE_VIEWMODEL_BASELINE = mapOf<String, Int>()
        val COMPOSABLE_PARAMETER_BASELINE = mapOf<String, Int>()
    }
}
