import org.gradle.internal.classpath.Instrumented.systemProperty
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("org.springframework.boot") version "3.1.4"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.spring") version "1.9.10"
    id("dev.hilla") version "2.3.2"
}

group = "com.ailegorreta"
version = "2.0.0"
description = "UI for all ACME company operation."
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }

    maven { url = uri("https://maven.vaadin.com/vaadin-prereleases/") }
    maven {url = uri("https://maven.vaadin.com/vaadin-addons") }

    maven {
        name = "GitHubPackages"
        url = uri(
                ("https://maven.pkg.github.com/" +
                        project.findProperty("registryPackageUrl") as String?) ?: System.getenv("URL_PACKAGE")
                        ?: "rlegorreta/ailegorreta-kit"
        )
        credentials {
            username = project.findProperty("registryUsername") as String? ?:
                    System.getenv("USERNAME") ?:
                            "rlegorreta"
            password = project.findProperty("registryToken") as String? ?: System.getenv("TOKEN")
        }
    }
}

hilla {
   productionMode = true
}

extra["springCloudVersion"] = "2022.0.4"
extra["testcontainersVersion"] = "1.17.3"
extra["otelVersion"] = "1.26.0"
extra["vok-framework-vokdbVersion"] = "0.16.0"
extra["ailegorreta-kit-version"] = "2.0.0"
extra["coroutines-version"] = "1.7.3"
extra["alfresco-opencmis-extension-version"] = "0.7"
extra["chemistry-opencmis-version"] = "1.1.0"
extra["zeebe-version"] = "8.1.14"
extra["zeebe-client-java-version"] = "8.1.6"
extra["pdf-viewer-version"] = "2.5.3"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutines-version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${property("coroutines-version")}")
    
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client") {
        exclude(group = "org.springframework.cloud", module = "spring-cloud-starter-ribbon")
        exclude(group ="com.netflix.ribbon", module = "ribbon-eureka")
    }
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka-streams")

    implementation("dev.hilla:hilla-spring-boot-starter")
    implementation("org.parttio:line-awesome:1.1.0")
    implementation("org.vaadin.artur:a-vaadin-helper:1.9.0")
    // implementation("org.vaadin.artur.exampledata:exampledata:3.4.0")
    /* VOK : Vaadin on Kotlin:
         Use this dependency 'just' for the Karibu-DSL only:
           implementation("com.github.mvysny.karibudsl:karibu-dsl-v10:${property(vok.karibu-DSL.version")})
         Use this dependency for VOK:DB library (it includes Karibu-DSL for example when using VOK filtering on Grids.
         This dependency and the Kalibu-DSL are mutual exclusive
     */
    implementation("eu.vaadinonkotlin:vok-framework-vokdb:${property("vok-framework-vokdbVersion")}")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
 
    implementation("org.springframework.boot:spring-boot-starter-graphql")

    implementation("org.springframework.boot:spring-boot-starter-rsocket")
    // ^ Spring RSocket for GraphQL Subscription
    
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    /* An HTML sanitizer to protect from HTML that can harm and compromiseVaadin */
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20200713.1")

    /* Vaadin addon for PDF viewer */
    implementation("org.vaadin.addons.componentfactory:vcf-pdf-viewer:${property("pdf-viewer-version")}")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")

    /* Push notification */
    implementation("nl.martijndwars:web-push:5.1.1") {
        exclude(group = "commons-logging", module = "commons-logging")
        // ^ avoid various versions fo logging see: https://stackoverflow.com/questions/76706612/standard-commons-logging-discovery-in-action-with-spring-jcl
    }
    implementation("org.bouncycastle:bcprov-jdk15on:1.68")

    /* Alfresco */
    implementation("org.alfresco.cmis.client:alfresco-opencmis-extension:${property("alfresco-opencmis-extension-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-client-api:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-client-impl:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-client-bindings:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-commons-api:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-commons-impl:${property("chemistry-opencmis-version")}")
    implementation("org.apache.chemistry.opencmis:chemistry-opencmis-osgi-client:${property("chemistry-opencmis-version")}")

    /* Camunda */
    implementation("io.camunda:spring-zeebe-starter:${property("zeebe-version")}")
    implementation("io.camunda:zeebe-client-java:${property("zeebe-client-java-version")}")

    implementation("com.ailegorreta:ailegorreta-kit-commons-utils:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-client-security:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-client-components:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-client-navigation:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-client-bpm:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-commons-event:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-client-dataproviders:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:ailegorreta-kit-commons-cmis:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:third-party-vaadin-litelement-ckeditor:${property("ailegorreta-kit-version")}")
    implementation("com.ailegorreta:third-party-org-vaadin-addon-visjs-network:${property("ailegorreta-kit-version")}")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.junit.vintage:junit-vintage-engine")
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("dev.hilla:hilla-bom:${property("hillaVersion")}")
    }
}

tasks.named<BootBuildImage>("bootBuildImage") {
    environment.set(environment.get() + mapOf("BP_JVM_VERSION" to "17.*"))
    imageName.set("ailegorreta/${project.name}")
    docker {
        publishRegistry {
            username.set(project.findProperty("registryUsername").toString())
            password.set(project.findProperty("registryToken").toString())
            url.set(project.findProperty("registryUrl").toString())
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
    }
}
