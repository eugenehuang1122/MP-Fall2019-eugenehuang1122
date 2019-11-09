import java.util.function.BiConsumer

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}
plugins {
    id("com.android.application")
    id("checkstyle")
    id("edu.illinois.cs.cs125.gradlegrader") version "1.0.5"
    id("edu.illinois.cs.cs125.empire") version "1.0.4"
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.media:media:1.1.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.android.volley:volley:1.1.1")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.google.android.gms:play-services-maps:17.0.0")
    implementation("com.google.android.gms:play-services-location:17.0.0")
    implementation("com.google.firebase:firebase-core:17.2.0")
    implementation("com.google.firebase:firebase-auth:19.0.0")
    implementation("com.firebaseui:firebase-ui-auth:4.3.1")
    implementation("com.neovisionaries:nv-websocket-client:2.9")
    implementation("nz.ac.waikato.cms.weka:weka-stable:3.8.3") {
        exclude(module = "java-cup-11b-runtime")
    }
    testImplementation("junit:junit:4.12")
    testImplementation("org.robolectric:robolectric:4.3")
    testImplementation("androidx.test:core:1.2.0")
    testImplementation("org.powermock:powermock-module-junit4:2.0.2")
    testImplementation("org.powermock:powermock-module-junit4-rule:2.0.2")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.2")
    testImplementation("org.powermock:powermock-classloading-xstream:2.0.2")
    testImplementation("com.github.cs125-illinois:gradlegrader:1.0.5")
    testImplementation("com.github.cs125-illinois:robolectricsecurity:1.1.1")

    testAnnotationProcessor("com.google.auto.service:auto-service:1.0-rc4")
}
android {
    compileSdkVersion(28)
    buildToolsVersion("29.0.2")
    defaultConfig {
        applicationId = "edu.illinois.cs.cs125.fall2019.mp"
        minSdkVersion(24)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
gradlegrader {
    assignment = "Fall2019.MP"
    checkpoint {
        yamlFile = rootProject.file("grade.yaml")
        configureTests(BiConsumer { checkpoint, test ->
            if (checkpoint !in setOf("0", "1", "2", "3", "4", "5")) error("Cannot grade unknown checkpoint '$checkpoint'")
            test.setTestNameIncludePatterns(listOf("Checkpoint${checkpoint}Test"))
            test.filter.isFailOnNoMatchingTests = false
        })
    }
    checkstyle {
        points = 10
        configFile = rootProject.file("config/checkstyle.xml")
    }
    identification {
        txtFile = rootProject.file("email.txt")
        validate = Spec { it.endsWith("@illinois.edu") }
    }
    reporting {
        post {
            endpoint = "https://cs125-cloud.cs.illinois.edu/gradlegrader"
        }
        printPretty {
            title = "Grade Summary"
            notes = "On checkpoints with an early deadline, the maximum local score is 90/100. " +
                    "10 points will be provided during official grading if you submitted code " +
                    "that earns at least 40 points by 8 PM on the early deadline day."
        }
    }
    vcs {
        git = true
        requireCommit = true
    }
}
eMPire {
    excludedSrcPath = "edu/illinois/cs/cs125/fall2019/mp"
    studentConfig = rootProject.file("grade.yaml")
    studentCompileTasks("compileDebugJavaWithJavac", "compileReleaseJavaWithJavac")
    segments {
        register("tvc") {
            addJars("tvc")
            removeClasses("TargetVisitChecker")
        }
        register("ad") {
            addJars("ad")
            removeClasses("AreaDivider")
        }
        register("cgb") {
            addAars("inject-cgb")
            injector("MainActivity", "onCreate", "CreateGameInjector")
        }
        register("lgci") {
            addAars("inject-lgci")
            injector("NewGameActivity", "onCreate", "LocalGameConfigInjector")
        }
        register("ga1") {
            addAars("dt")
            chimera("chimera-ga1.jar", "GameActivity", "addLine")
        }
        register("gl") {
            addAars("gl")
            removeClasses("MainActivity", "LaunchActivity")
            manifestEditor("manifest-la.jar", "LaunchManifestEditor")
        }
        register("sgc") {
            addAars("sgc")
            removeClasses("NewGameActivity", "Invitee")
        }
        register("llr") {
            addJars("llr")
            removeClasses("LineCrossDetector", "Target")
        }
        register("gsc") {
            addJars("gsc")
            removeClasses("TargetVisitChecker", "TargetGame", "AreaGame")
        }
        register("ga4") {
            addAars("gi")
            chimera("chimera-ga4.jar", "GameActivity", "updateRandomWalkDetection",
                    "camo-ga4.jar", "CallScraperKt", "createCamo")
        }
        register("rwd") {
            addAars("rwd")
            removeClasses("RandomWalkDetector", "Game", "GameActivity")
        }
    }
    checkpoints {
        register("0") {
            segments()
        }
        register("1") {
            segments("tvc")
        }
        register("2") {
            segments("tvc", "ad", "cgb", "lgci", "ga1")
        }
        register("3") {
            segments("tvc", "ad", "lgci", "ga1", "gl")
        }
        register("4") {
            segments("tvc", "ad", "gl", "sgc", "llr")
        }
        register("5") {
            segments("ad", "gl", "sgc", "llr", "gsc", "ga4")
        }
        register("demo") {
            segments("ad", "gl", "sgc", "llr", "gsc", "rwd")
        }
    }
}

apply(plugin = "com.google.gms.google-services")
