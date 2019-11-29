plugins {
    scala
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

group = "zhi.yest"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.1")
    implementation("org.scala-lang:scala-reflect:2.13.1")

    implementation("com.typesafe.akka:akka-actor_2.13:2.6.0")
    implementation("com.typesafe.akka:akka-stream_2.13:2.6.0")
    implementation("com.typesafe.akka:akka-http_2.13:10.1.10")
    implementation("com.typesafe.akka:akka-stream-kafka_2.13:1.1.0")

    // TODO: remove and replace by akka streams
    implementation("org.apache.httpcomponents:httpclient:4.5.10")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("commons-io:commons-io:2.6")
}

tasks {
    shadowJar {
        manifest {
            attributes("Main-Class" to "zhi.yest.Main")
        }
    }
    build {
        dependsOn(shadowJar)
    }
}