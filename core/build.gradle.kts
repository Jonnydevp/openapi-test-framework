plugins {
    `java-library`
}

dependencies {
    // OpenAPI parsing
    api(libs.swagger.parser)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)
    implementation(libs.jackson.kotlin)

    // HTTP execution
    api(libs.rest.assured)
    implementation(libs.rest.assured.kotlin)

    // data generation (Arb is part of the public API)
    api(libs.kotest.property)
    implementation(libs.kotest.assertions)

    // dynamic tests are part of the public API
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)

    // code generation + schema validation
    implementation(libs.kotlinpoet)
    implementation(libs.json.schema.validator)

    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.property)
    testRuntimeOnly(libs.junit.platform.launcher)
}
