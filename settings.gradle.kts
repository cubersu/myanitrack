pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("""com\.android.*""")
                includeGroupByRegex("""com\.google.*""")
                includeGroupByRegex("""androidx.*""")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyAniTrack"

include(":app")

include(":core:common")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:ui")

include(":feature:auth")
include(":feature:mylist")
include(":feature:details")
include(":feature:browse")
include(":feature:calendar")
include(":feature:news")
include(":feature:profile")
include(":feature:forum")
include(":feature:messaging")
include(":feature:settings")
