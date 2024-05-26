
pluginManagement {

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            val gpr_userProvider = providers.gradleProperty("gpr.username")
            val gpr_keyProvider = providers.gradleProperty("gpr.key")

            url = uri("https://maven.pkg.github.com/justincrosbie/passkeyme-android-sdk")
            credentials {
                username = gpr_userProvider.getOrNull()
                password = gpr_keyProvider.getOrNull()
            }
        }
    }
}

rootProject.name = "Passkeyme Demo App"
include(":app")
 