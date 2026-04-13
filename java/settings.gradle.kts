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

includeBuild("../../rails-sdks/sdks/rails-java") {
    dependencySubstitution {
        substitute(module("com.rails.api:rails-java")).using(project(":rails-java"))
    }
}
