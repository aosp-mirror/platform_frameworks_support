buildscript {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }

    dependencies {
        classpath(gradleApi())
        classpath("org.apache.maven:maven-model:3.5.4")
        classpath("org.apache.maven:maven-model-builder:3.5.4")
    }
}
