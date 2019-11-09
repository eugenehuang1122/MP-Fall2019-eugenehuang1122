buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        @Suppress("GradleDependency") // Version 4.3.1 produces a Gradle warning during build
        classpath("com.google.gms:google-services:4.2.0")
    }
}
allprojects {
    repositories {
        google()
        jcenter()
    }
}
tasks.register<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}
