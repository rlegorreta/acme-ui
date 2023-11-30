rootProject.name = "acme-ui"

pluginManagement {
    repositories {
        maven { url = uri("https://maven.vaadin.com/vaadin-prereleases") }
        gradlePluginPortal()
    }
    plugins {
        id("dev.hilla") version "2.3.2"
    }
}

buildscript {
    repositories {
        mavenCentral()
        maven { setUrl("https://maven.vaadin.com/vaadin-prereleases") }
    }
}