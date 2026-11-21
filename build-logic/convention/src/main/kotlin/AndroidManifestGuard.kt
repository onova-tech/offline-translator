import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Adds one manifest-permission check per application variant and wires it into `check`. */
internal object AndroidManifestGuard {
    fun configure(
        project: Project,
        allowlist: RegularFile,
    ) {
        val components = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        components.onVariants { variant ->
            val taskName = "verify${variant.name.replaceFirstChar(Char::uppercaseChar)}ManifestPermissions"
            val verify =
                project.tasks.register(taskName, VerifyManifestPermissionsTask::class.java) {
                    group = "verification"
                    description =
                        "Fails if the ${variant.name} merged manifest requests a permission not in the allowlist."
                    mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                    this.allowlist.set(allowlist)
                }
            project.tasks.named("check").configure { dependsOn(verify) }
        }
    }
}

/**
 * Every permission in the merged manifest, including those merged in by libraries, must be listed
 * in the app's allowlist with a reason, so a new permission is always a reviewed decision
 * (constitution Principles I and IX).
 */
abstract class VerifyManifestPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val allowlist: RegularFileProperty

    @TaskAction
    fun verify() {
        val violations =
            PermissionAllowlist.violations(
                manifestXml = mergedManifest.get().asFile.readText(),
                allowlistText = allowlist.get().asFile.readText(),
            )
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Merged manifest requests permissions missing from ${allowlist.get().asFile.name} " +
                    "(Principles I and IX): $violations",
            )
        }
    }
}

/** Pure parsing and matching, kept apart from Gradle so it can be unit-tested. */
internal object PermissionAllowlist {
    private val USES_PERMISSION = Regex("""<uses-permission(?:-sdk-23)?\b[^>]*android:name="([^"]+)"""")

    /** One permission per line; `#` starts a comment (the reason). Blank lines are ignored. */
    fun parse(allowlistText: String): Set<String> =
        allowlistText
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    fun requested(manifestXml: String): Set<String> =
        USES_PERMISSION
            .findAll(manifestXml)
            .map { it.groupValues[1] }
            .toSet()

    fun violations(
        manifestXml: String,
        allowlistText: String,
    ): List<String> = (requested(manifestXml) - parse(allowlistText)).sorted()
}
