plugins {
    alias(libs.plugins.allure)
}

dependencies {
    testImplementation(project(":core"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.property)

    testImplementation(libs.wiremock)

    testImplementation(platform(libs.allure.bom))
    testImplementation(libs.allure.junit5)
    testImplementation(libs.allure.rest.assured)

    testRuntimeOnly(libs.junit.platform.launcher)
}

allure {
    version.set(libs.versions.allure.get())
    adapter.autoconfigure.set(true)
    adapter.aspectjWeaver.set(true)
}
