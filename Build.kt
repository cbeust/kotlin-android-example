import com.beust.kobalt.*
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.kotlin.*

val repos = repos()


val p = kotlinProject {

    name = "kotlin-android-example"
    group = "com.example"
    artifactId = name
    version = "0.1"

    sourceDirectories {
            path("src/main/kotlin")
            path("src/main/resources")
    }

    sourceDirectoriesTest {
            path("src/test/kotlin")
            path("src/test/resources")
    }

    dependencies {
//        compile("com.beust:jcommander:1.48")
    }

    dependenciesTest {
//        compile("org.testng:testng:6.9.5")

    }
}

val packProject = assemble(p) {
    jar {
    }
}
