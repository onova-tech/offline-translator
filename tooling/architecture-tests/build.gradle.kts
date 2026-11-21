plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Konsist reads the sources of every module, so any source change must re-run these tests.
    inputs.files(
        fileTree(rootDir) {
            include("**/src/**/*.kt", "**/src/**/strings.xml")
            exclude("**/build/**")
        },
    )
}
