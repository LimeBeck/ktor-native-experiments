val ktor_version: String by project
val kotlin_version: String by project

plugins {
    application
    kotlin("multiplatform")
    id("com.github.ben-manes.versions").version("0.42.0")
}

application {
    mainClass.set("MainKt")
}

val kotlinCoroutinesVersion: String by project
val okioVersion: String by project

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven { //Provide your git credentials here to access library
        url = uri("https://maven.pkg.github.com/LimeBeck/ko-te")
        credentials {
            username = "limebeck"
            password = "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"
        }
    }
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:$okioVersion")
                implementation("dev.limebeck:ko-te:0.2.0")
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-resources:$ktor_version")
                implementation("io.ktor:ktor-server-status-pages:$ktor_version")
                implementation("io.ktor:ktor-server-cio:$ktor_version")
                //implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
                //implementation("io.ktor:ktor-server-data-conversion:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core"){
                    version {
                        strictly(kotlinCoroutinesVersion)
                    }
                }
                implementation("com.benasher44:uuid:0.4.0")
            }
        }
        val nativeTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}