// Test configuration for all modules
tasks.withType<Test> {
    useJUnitPlatform()
    
    // Set heap size for tests
    maxHeapSize = "1G"
    
    // Fail fast
    failFast = true
    
    // Test logging
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    
    // Exclude integration tests in CI environment
    if (System.getenv("CI") == "true") {
        exclude("**/*IntegrationTest*")
        exclude("**/*IT.class")
    }
    
    // Set test properties
    systemProperty("spring.profiles.active", "test")
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
}