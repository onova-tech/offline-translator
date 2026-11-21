import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/** Checks resolved runtime libraries and declared module edges against the constitution's rules. */
abstract class VerifyNetworkGuardTask : DefaultTask() {
    @get:Input abstract val modulePath: Property<String>

    @get:Input abstract val networkAllowed: Property<Boolean>

    @get:Input abstract val bannedCoordinatePrefixes: ListProperty<String>

    @get:Input abstract val resolvedCoordinates: ListProperty<String>

    @get:Input abstract val projectDependencies: ListProperty<String>

    @TaskAction
    fun verify() {
        val violations = bannedLibraryViolations() + moduleEdgeViolations()
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Network guard failed for ${modulePath.get()} (constitution Principle I):\n" +
                    violations.joinToString(separator = "\n") { "  - $it" },
            )
        }
    }

    private fun bannedLibraryViolations(): List<String> {
        if (networkAllowed.get()) return emptyList()
        val banned = bannedCoordinatePrefixes.get()
        return resolvedCoordinates
            .get()
            .distinct()
            .filter { coordinate -> banned.any { coordinate.startsWith(it) } }
            .map { "network-capable library on the runtime classpath: $it" }
    }

    private fun moduleEdgeViolations(): List<String> {
        val path = modulePath.get()
        return projectDependencies.get().mapNotNull { dependency ->
            when {
                path == DOMAIN_MODULE -> {
                    "$DOMAIN_MODULE must not depend on other modules (found $dependency)"
                }

                dependency in APP_ONLY_MODULES && path != APP_MODULE -> {
                    "only $APP_MODULE may depend on $dependency"
                }

                path.startsWith(FEATURE_PREFIX) && dependency.startsWith(FEATURE_PREFIX) -> {
                    "feature modules must not depend on each other ($path -> $dependency)"
                }

                else -> {
                    null
                }
            }
        }
    }

    private companion object {
        const val APP_MODULE = ":androidApp"
        const val DOMAIN_MODULE = ":core:domain"
        const val FEATURE_PREFIX = ":feature:"

        /** The language packs and the engines, bound to their ports in the app only (specs 012, 081, 082). */
        val APP_ONLY_MODULES = setOf(":core:content", ":core:translation", ":core:speech")
    }
}
