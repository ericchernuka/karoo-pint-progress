
pluginManagement { repositories { google { content { includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google()
        mavenCentral() } }
rootProject.name = "karoo-pint-progress"
include(":lib", ":pint")
include(":calorie-source")
project(":calorie-source").projectDir = file("tools/karoo-calorie-source")
