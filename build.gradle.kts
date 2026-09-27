import org.gradle.api.publish.maven.MavenPublication

plugins {
    java
    id("io.quarkus")
    id("checkstyle")
    id("org.owasp.dependencycheck") version "12.2.2"
    id("org.cyclonedx.bom") version "3.2.4"
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

group = findProperty("group") as String
version = findProperty("version") as String

repositories {
    // 1) Maven Local — los developers publican aqui con `./gradlew publishToMavenLocal`
    //    desde nova-java-api-standard y nova-java-api-standard-quarkus-extension
    //    para desarrollo offline (sin necesidad de token de GitHub Packages).
    mavenLocal()
    // 2) Maven Central — deps de Quarkus (quarkus-rest, quarkus-arc, junit, etc.).
    mavenCentral()
    // 3) GitHub Packages (Nova Platform). GitHub Packages requiere auth incluso para
    //    paquetes public (modelo scoped al repo dueno). En CI se inyecta
    //    NOVA_PACKAGES_READ_TOKEN (con scope read:packages) o NOVA_RELEASE_PAT
    //    como fallback via reusable-build-gradle.yml.
    maven {
        name = "GitHubPackages-Nova-QuarkusExtension"
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-10-api-standard-quarkus-extension")
        val token = System.getenv("NOVA_PACKAGES_READ_TOKEN")
            ?: System.getenv("GITHUB_TOKEN")
        if (!token.isNullOrBlank()) {
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "x-access-token"
                password = token
            }
        }
    }
    // Repo del Quarkus extension con artifactId corto: pe.edu.nova.java.starters:nova-quarkus-api-ext
    // (artifactId largo original 'nova-java-api-standard-quarkus-extension' producia paquetes
    // fantasma en GH Packages con maven-publish de Gradle; ver doc 07 seccion causa raiz).
    maven {
        name = "GitHubPackages-Nova-ApiStandard"
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-01-api-standard")
        val token = System.getenv("NOVA_PACKAGES_READ_TOKEN")
            ?: System.getenv("GITHUB_TOKEN")
        if (!token.isNullOrBlank()) {
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "x-access-token"
                password = token
            }
        }
    }
}

dependencies {
    // Quarkus BOM: alinea todas las versiones de extensiones Quarkus
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

    // Quarkus core: REST + JSON. Importante usar la extension combinada
    // quarkus-rest-jackson (NO quarkus-rest + quarkus-jackson por separado —
    // esa combinacion no registra el JSON provider en runtime, y las respuestas
    // se serializan como toString() del record en vez de JSON).
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-arc")

    // Nova Quarkus extension (publicada en GitHub Packages).
    // Esta extension aporta el ExceptionMapper generico y el ObjectMapperCustomizer
    // para que las respuestas se serialicen como ApiResponse<T> segun el contrato
    // de nova-api-standard. Sin esto, las excepciones no controladas retornarian
    // un JSON vacio y el timestamp de ApiMetadata se serializaria como epoch ms.
    implementation("pe.edu.nova.java.starters:nova-quarkus-api-ext:1.0.1")

    // Tests
    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.rest-assured:rest-assured")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

dependencyCheck {
    // Mismo patron que el extension: NVD_API_KEY / NOVA_OWASP_FAIL_ON_CVSS
    // inyectados por reusable-owasp-check.yml. Local sin env: never fail + sin key.
    failBuildOnCVSS = (System.getenv("NOVA_OWASP_FAIL_ON_CVSS") ?: "11").toFloat()
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
    skipConfigurations = listOf("testCompileClasspath", "testRuntimeClasspath")
    formats = listOf("HTML", "JSON")
    // CRITICO: reusable-owasp-check.yml descarga un mirror NVD pre-construido
    // (~119MB) desde ahincho/nova-shared-02-pipelines (releases/tag/nvd-mirror), reconstruido
    // diario por nvd-mirror-update.yml. Con autoUpdate=true (default), el plugin
    // IGNORA ese mirror y dispara un full sync contra NVD (366k records), que
    // tarda 5-15 min CON key y 18+ min SIN key (rate-limited HTTP 429). El
    // mirror + autoUpdate=false da el mismo nivel de deteccion (<24h staleness)
    // en <1 min. Documentado en doc 07 §8 (Causa raiz del OWASP lento).
    autoUpdate = false
    data.directory = System.getenv("NOVA_OWASP_DATA_DIR")
        ?: "${System.getProperty("user.home")}/.dependency-check-data"
}

checkstyle {
    // El CI ejecuta ./gradlew checkstyleMain via reusable-build-gradle.yml,
    // asi que la task DEBE existir. Usamos la config por default del plugin
    // (sun_checks.xml) ya que esta instancia es solo un ejemplo.
    toolVersion = "10.12.0"
}