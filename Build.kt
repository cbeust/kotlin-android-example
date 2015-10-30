import com.beust.kobalt.*
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.kotlin.*
import com.beust.kobalt.plugin.android.*

val r = repos(file("/Users/beust/adt-bundle-mac-x86_64-20140702/sdk/extras/android/m2repository"))

val p = kotlinProject {
    name = "kotlin-android-example"
    group = "com.beust"
    artifactId = name
    version = "0.1"

    sourceDirectories {
            path("app/src/main/kotlin")
            path("app/src/main/resources")
    }

    dependencies {
        compile("com.android.support:appcompat-v7:aar:22.2.1",
                "com.google.code.gson:gson:2.4",
                "com.squareup.retrofit:retrofit:1.9.0",
                "io.reactivex:rxandroid:0.24.0"
        )
    }

    dependenciesTest {
//        compile("org.testng:testng:6.9.5")

    }

    assemble {
        jar {
        }
    }

    android {
        applicationId = "com.beust.example"
    }
}

