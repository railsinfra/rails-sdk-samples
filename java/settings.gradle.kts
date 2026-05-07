rootProject.name = "rails-java-sdk-sample"

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

includeBuild("../../rails-sdks/java") {
    dependencySubstitution {
        substitute(module("com.railsinfra:rails-java")).using(project(":rails-java"))
    }
}
