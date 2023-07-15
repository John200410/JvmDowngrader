package xyz.wagyourtail.jvmdg.j12.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Map;

public class J_N_F_FileSystems {

    // fix env bug in older java

    @Stub(opcVers = Opcodes.V12, ref = @Ref("java/nio/file/FileSystems"))
    public static FileSystem newFileSystem(URI uri, Map<String, Object> env) throws IOException {
        env.replaceAll((k, v) -> {
            if (v instanceof Boolean && !k.equals("useTempFile")) {
                return v.toString();
            }
            return v;
        });
        return FileSystems.newFileSystem(uri, env);
    }

    @Stub(opcVers = Opcodes.V12, ref = @Ref("java/nio/file/FileSystems"))
    public static FileSystem newFileSystem(URI uri, Map<String, Object> env, ClassLoader loader) throws IOException {
        env.replaceAll((k, v) -> {
            if (v instanceof Boolean && !k.equals("useTempFile")) {
                return v.toString();
            }
            return v;
        });
        return FileSystems.newFileSystem(uri, env, loader);
    }

}