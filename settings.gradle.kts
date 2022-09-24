rootProject.name = "dev.limebeck.ktor-native-test"
pluginManagement {
    val kotlin_version: String by settings
    plugins {
        kotlin("multiplatform") version kotlin_version
    }
}
