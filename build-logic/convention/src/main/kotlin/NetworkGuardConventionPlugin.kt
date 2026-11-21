import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/**
 * Constitution Principle I enforcement: fails `check` when a module pulls in a network-capable
 * library or depends on a module it must not see. Libraries that arrive only through a
 * network-allowed module (Play's asset delivery through `:core:content`) are that module's
 * business, so the traversal stops there. The Android manifest check is added only when the
 * module is an Android application.
 */
class NetworkGuardConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            val allowedModules = NETWORK_ALLOWED_MODULES
            val guard =
                tasks.register("verifyNetworkGuard", VerifyNetworkGuardTask::class.java) {
                    group = "verification"
                    description = "Fails on network-capable dependencies or forbidden module edges (Principle I)."
                    modulePath.set(target.path)
                    networkAllowed.set(target.path in NETWORK_ALLOWED_MODULES)
                    bannedCoordinatePrefixes.set(BANNED_COORDINATE_PREFIXES)
                    projectDependencies.set(target.provider { target.declaredProjectDependencies() })
                    target.configurations
                        .filter { it.isCanBeResolved && it.isProductionRuntimeClasspath() }
                        .forEach { configuration ->
                            resolvedCoordinates.addAll(
                                configuration.incoming.resolutionResult.rootComponent
                                    .map { root -> collectModuleCoordinates(root, allowedModules) },
                            )
                        }
                }
            tasks.matching { it.name == "check" }.configureEach { dependsOn(guard) }

            pluginManager.withPlugin("com.android.application") {
                AndroidManifestGuard.configure(target, target.layout.projectDirectory.file(PERMISSION_ALLOWLIST))
            }
        }

    private fun Project.declaredProjectDependencies(): List<String> =
        configurations
            .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
            .map { it.path }
            .filter { it != path }
            .distinct()
            .sorted()

    internal companion object {
        /** The one module allowed to use the network (spec 081): Google Play's language packs. */
        val NETWORK_ALLOWED_MODULES = setOf(":core:content")

        val BANNED_COORDINATE_PREFIXES =
            listOf(
                "com.squareup.okhttp3:",
                "com.squareup.okhttp:",
                "com.squareup.retrofit2:",
                "io.ktor:ktor-client",
                "com.android.volley:",
                "com.google.firebase:",
                "com.google.android.gms:",
                "com.google.mlkit:",
            )

        /** Reviewed install-time permissions of the app, one per line with its reason (spec 002 R9). */
        const val PERMISSION_ALLOWLIST = "permissions.allowlist"
    }
}

/** Runtime classpaths of shipped code (`runtimeClasspath`, `debugRuntimeClasspath`, …), excluding tests. */
private fun Configuration.isProductionRuntimeClasspath(): Boolean =
    (name == "runtimeClasspath" || name.endsWith("RuntimeClasspath")) && !name.contains("Test")

private fun collectModuleCoordinates(
    root: ResolvedComponentResult,
    networkAllowedModules: Set<String>,
): List<String> {
    val visited = mutableSetOf<ResolvedComponentResult>()
    val pending = ArrayDeque(listOf(root))
    val coordinates = sortedSetOf<String>()
    while (pending.isNotEmpty()) {
        val component = pending.removeFirst()
        if (!visited.add(component)) continue
        (component.id as? ModuleComponentIdentifier)?.let { coordinates += "${it.group}:${it.module}" }
        component.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .map { it.selected }
            .filterNot { it.isProjectIn(networkAllowedModules) }
            .forEach { pending += it }
    }
    return coordinates.toList()
}

private fun ResolvedComponentResult.isProjectIn(paths: Set<String>): Boolean =
    (id as? ProjectComponentIdentifier)?.projectPath in paths
