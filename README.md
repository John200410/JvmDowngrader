# JvmDowngrader

downgrades modern java bytecode to older versions. at either compile or runtime.

## Gradle Plugin

This downgrades the output of a jar task using another task.
Note that certain things like reflection and dynamic class definition downgrading will not work without runtime downgrading.
dynamic class definitions being things like `MethodHandles$Lookup#defineClass` and classloader shenanigans.

add my maven in `settings.gradle`:
```gradle
pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = "https://maven.wagyourtail.xyz/snapshots"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

in `build.gradle`:
```gradle
// add the plugin
plugins {
    id 'xyz.wagyourtail.jvmdowngrader' version '0.0.1-SNAPSHOT'
}
```

This will create a default downgrade task for `jar` (or `shadowJar` if present) called `downgradeJar` that will downgrade the output to java 8 by default.
as well as a `shadeDowngradedApi` to then insert the required classes for not having a runtime dependency on the api jar.

you can change the downgrade version by doing:
```gradle
downgradeJar {
    downgradeVersion = JavaVersion.VERSION_1_11
    archiveClassifier = "downgraded-11"
}

shadeDowngradedApi {
    downgradeVersion = JavaVersion.VERSION_1_11
    archiveClassifier = "downgraded-11-shaded"
}
```

Optionally, you can also depend on the sahdeDowngradedApi task when running build.
```gradle
assemble.dependsOn shadeDowngradedApi
```

you can create a custom task by doing:
```gradle
task customDowngrade(type: xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar) {
    inputFile = tasks.jar.archiveFile
    downgradeTo = JavaVersion.VERSION_1_8 // default
    sourceSet = sourceSets.main // default
    archiveClassifier = "downgraded-8"
}

task customShadeDowngradedApi(type: xyz.wagyourtail.jvmdg.gradle.task.ShadeDowngradedApi) {
    inputFile = customDowngrade.archiveFile
    downgradeTo = JavaVersion.VERSION_1_8 // default
    sourceSet = sourceSets.main // default
    shadePath = "${archiveBaseName}/jvmdg/api" // default
    archiveClassifier = "downgraded-8-shaded"
}
```

## "Compile" Time Downgrading

Shading the required api is currently only supported in the gradle plugin. 
So this isn't recommended outside of debugging.

### Zip Downgrading

Downgrades the contents of a zip file to an older version.

ex. `java -cp JvmDowngrader-all.jar xyz.wagyourtail.jvmdg.compile.ZipDowngrader 52 input.jar output.jar classpath.jar;classpath2.jar`

### Path Downgrading

Downgrade the contents of 1 or more directory trees to an older version.

ex. `java -cp JvmDowngrader-all.jar xyz.wagyourtail.jvmdg.compile.PathDowngrader 52 build/classes/java/main;build/classes/java/api build/downgraded/java/main;build/classes/java/api classpath.jar;classpath2.jar`

## Runtime Downgrading

This is basically only here so I can take funny screenshots of minecraft running on java 8.
I recommend the agent method, as it's most reliable.

### Agent Downgrading
Uses the java agent to downgrade at runtime.

ex. `java -javaagent:JvmDowngrader-all.jar -jar myapp.jar`

### Bootstrap Downgrading
Uses the bootstrap main class

ex. `java -jar JvmDowngrader-all.jar myapp.jar;classpath.jar;classpath2.jar mainclass args`

### inspired by

https://github.com/Chocohead/Not-So-New and https://github.com/luontola/retrolambda
