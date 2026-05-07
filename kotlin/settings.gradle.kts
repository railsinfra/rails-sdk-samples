rootProject.name = "rails-kotlin-sdk-sample"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
}

includeBuild("../../rails-sdks/rails-kotlin") {
    dependencySubstitution {
        substitute(module("com.railsinfra:rails-kotlin")).using(project(":rails-kotlin"))
    }
}
