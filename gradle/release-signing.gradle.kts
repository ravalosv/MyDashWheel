// How release builds are signed, for both :app and :companion (docs/harness/reports/release-signing-local-plan.md).
//
// CI exports the four KEYSTORE_* variables and wins. A local build has none, so it reads
// keystore.properties at the repository root instead — gitignored, along with the .keystore,
// because this repository is public. Neither present: the module falls back to the debug key,
// as it always did, and the release APK simply can never update a properly signed install.
import java.util.Properties

// keys/ holds both files, gitignored: the public repository must never carry them.
val signingPropertiesFile = rootProject.file("keys/keystore.properties")
val signingProperties = signingPropertiesFile
    .takeIf { it.exists() }
    ?.let { file ->
        Properties().apply { file.inputStream().use { load(it) } }
    }

// Environment first (CI), then the local file; null means "not configured here".
fun signingValue(propertyName: String, envName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: signingProperties?.getProperty(propertyName)?.takeIf { it.isNotBlank() }

// A relative path in keystore.properties is relative to the repository root, so the file
// keeps working on any machine; an absolute one is taken as it is.
val configuredStore = signingValue("storeFile", "KEYSTORE_FILE")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }

if (configuredStore == null && signingProperties != null) {
    logger.warn("keys/keystore.properties is set but its storeFile does not exist; signing with the debug key.")
}

extra["releaseKeystoreFile"] = configuredStore
extra["releaseStorePassword"] = signingValue("storePassword", "KEYSTORE_PASSWORD")
extra["releaseKeyAlias"] = signingValue("keyAlias", "KEY_ALIAS")
extra["releaseKeyPassword"] = signingValue("keyPassword", "KEY_PASSWORD")

// A debug-signed release can never be updated in place by a later properly signed one, so CI
// must stop rather than publish one. Local builds are allowed to fall back, to try R8.
// Registered once, from whichever module applies this script first.
if (!rootProject.extra.has("releaseSigningGuard")) {
    rootProject.extra["releaseSigningGuard"] = true
    gradle.taskGraph.whenReady {
        val buildsRelease = allTasks.any { it.name.contains("Release") }
        if (buildsRelease && configuredStore == null) {
            if (System.getenv("CI") == "true") {
                throw GradleException(
                    "CI release build without a keystore: set the KEYSTORE_BASE64 / " +
                        "KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD secrets."
                )
            }
            // Said out loud: a release signed with the debug key looks fine until the
            // moment it has to update an installed app, and then it cannot.
            logger.warn(
                "Release signed with the DEBUG key: no $signingPropertiesFile and no KEYSTORE_FILE."
            )
        }
    }
}
