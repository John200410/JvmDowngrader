package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.annotations.ApiStatus
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.compile.PathDowngrader
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.util.*
import java.nio.file.FileSystem
import kotlin.io.path.*

abstract class DowngradeFiles : ConventionTask() {
    private val jvmdg by lazy {
        project.extensions.getByType(JVMDowngraderExtension::class.java)
    }

    @get:Input
    @get:Optional
    var downgradeTo by FinalizeOnRead(JavaVersion.VERSION_1_8)

    @get:InputFiles
    var toDowngrade: FileCollection by FinalizeOnRead(MustSet())

    @get:InputFiles
    var classpath: FileCollection by FinalizeOnRead(LazyMutable {
        project.extensions.getByType(SourceSetContainer::class.java).getByName("main").runtimeClasspath
    })

    /**
     * this is the output, gradle just doesn't have a
     * \@OutputDirectoriesAndFiles
     */
    @get:Internal
    val outputCollection: FileCollection by lazy {
        project.files(toDowngrade.map { temporaryDir.resolve(it.name) })
    }

    @get:Input
    @get:Optional
    @get:ApiStatus.Experimental
    abstract val debugSkipStubs: ListProperty<Int>

    @get:Input
    @get:Optional
    @get:ApiStatus.Experimental
    abstract val debugPrint: Property<Boolean>

    init {
        debugSkipStubs.convention(mutableListOf())
        debugPrint.convention(false)
    }

    @TaskAction
    fun doDowngrade() {
        var toDowngrade = toDowngrade.files.map { it.toPath() }.filter { it.exists() }
        val classpath = classpath.files

        val fileSystems = mutableSetOf<FileSystem>()

        Flags.api = jvmdg.apiJar
        Flags.printDebug = debugPrint.get()
        Flags.debugSkipStubs = debugSkipStubs.get().toSet()

        try {

            outputs.files.forEach { it.deleteRecursively() }

            val downgraded = toDowngrade.map { temporaryDir.resolve(it.name) }.map { if (it.extension == "jar" || it.extension == "zip") {
                val fs = Utils.openZipFileSystem(it.toPath(), mapOf("create" to "true"))
                fileSystems.add(fs)
                fs.getPath("/")
            } else it.toPath() }

            toDowngrade = toDowngrade.map { if (it.isDirectory()) it else run {
                val fs = Utils.openZipFileSystem(it, mapOf())
                fileSystems.add(fs)
                fs.getPath("/")
            } }

            PathDowngrader.downgradePaths(downgradeTo.toOpcode(), toDowngrade, downgraded, classpath)
        } finally {
            fileSystems.forEach { it.close() }
        }
    }


}
