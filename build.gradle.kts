plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.141'
}

group = 'com.darkiiraze'
version = '1.0.0'

neoForge {
    version = '21.1.219'
    
    parchment {
        mappingsVersion = '2024.11.10'
        minecraftVersion = '1.21'
    }

    accessTransformers {
        file('src/main/resources/META-INF/accesstransformer.cfg')
    }

    mods {
        createvoicelink {
            sourceSet sourceSets.main
        }
    }
}

repositories {
    mavenCentral()
    maven {
        url 'https://maven.createmod.net'
        content {
            includeGroup 'com.simibubi.create'
            includeGroup 'net.createmod'
            includeGroup 'dev.engine-room.flywheel'
        }
    }
    maven { url 'https://mvn.devos.one/snapshots' }
    maven { url 'https://maven.ryanhcode.dev' }
    maven { url 'https://maven.maxhenkel.de/repository/public' }
    maven { url 'https://cursemaven.com' }
}

dependencies {
    // Create Mod
    compileOnly("com.simibubi.create:create-1.21.1:6.0.10") { transitive = false }
    compileOnly("net.createmod.ponder:ponder-neoforge-1.21.1:1.0.81") { transitive = false }
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1:1.0.6") { transitive = false }
    compileOnly("com.tterrag.registrate:Registrate:MC1.21-1.3.0+67") { transitive = false }
    compileOnly("net.createmod:catnip-neoforge-1.21.1:2.0.1") { transitive = false }
    
    // Local runtime for testing
    localRuntime("com.simibubi.create:create-1.21.1:6.0.10")
    localRuntime("net.createmod.ponder:ponder-neoforge-1.21.1:1.0.81")
    localRuntime("dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1:1.0.6")
    localRuntime("com.tterrag.registrate:Registrate:MC1.21-1.3.0+67")
    localRuntime("net.createmod:catnip-neoforge-1.21.1:2.0.1")
    
    // Simple Voice Chat API
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.5.2")
    localRuntime("curse.maven:simple-voice-chat-416089:5841627")
    
    // Vosk STT
    implementation("com.alphacephei:vosk:0.3.45")
    
    // Jar-in-jar for Vosk
    jarJar("com.alphacephei:vosk:0.3.45") {
        version {
            strictly '[0.3.45,0.4.0)'
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = 21
}
