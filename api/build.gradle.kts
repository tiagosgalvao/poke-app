plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.poke"
version = "0.0.1-SNAPSHOT"
description = "Pokemon API"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

val springdocVersion = "3.1.1"
val archunitVersion = "1.5.0"
val wiremockVersion = "3.13.2"
val mapstructVersion = "1.6.3"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.mapstruct:mapstruct:$mapstructVersion")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-cache-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
	testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
	enabled = false
}

tasks.bootJar {
	archiveFileName = "poke-api.jar"
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

val coverageExclusions = listOf("com/poke/PokeApiApplication.class", "**/*MapperImpl.class")

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	classDirectories.setFrom(files(classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } }))
	reports {
		xml.required = true
		html.required = true
	}
}

val minimumLineCoverage = "0.95".toBigDecimal()
val minimumBranchCoverage = "0.90".toBigDecimal()

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	classDirectories.setFrom(files(classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } }))
	violationRules {
		rule {
			limit {
				counter = "LINE"
				minimum = minimumLineCoverage
			}
			limit {
				counter = "BRANCH"
				minimum = minimumBranchCoverage
			}
		}
	}
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification)
}
