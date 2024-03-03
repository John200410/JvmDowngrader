package xyz.wagyourtail.jvmdg.runtime;

import org.objectweb.asm.*;
import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassDowngradingAgent implements ClassFileTransformer {
    public static final MethodHandle defineClass;
    private static final Logger LOGGER = Logger.getLogger("JVMDowngrader/Agent");
    public static final boolean DUMP_CLASSES = Boolean.parseBoolean(System.getProperty("jvmdg.dump", "false"));

    static {
        LOGGER.setLevel(Boolean.parseBoolean(System.getProperty("jvmdg.log", "false")) ? Level.ALL : Level.OFF);
    }

    static {
        Method md;
        try {
            md = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            defineClass = Utils.getImplLookup().unreflect(md);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int currentVersion;

    static {
        String version = System.getProperty("java.class.version");
        if (version != null) {
            try {
                currentVersion = Integer.parseInt(version.split("\\.")[0]);
            } catch (NumberFormatException e) {
                throw new UnsupportedOperationException("Unable to determine current class version");
            }
        } else {
            throw new UnsupportedOperationException("Unable to determine current class version");
        }
    }

    public byte[] retransformCodeSource(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("<init>")) {
                    return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            visitInsn(Opcodes.ACONST_NULL);
                            visitVarInsn(Opcodes.ASTORE, 2);
                        }
                    };
                } else {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    @Override
    public byte[] transform(final ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
        try {
            if (className != null && className.equals("java/security/CodeSource")) {
                return retransformCodeSource(bytes);
            }
            // check magic
            if (bytes[0] != (byte) 0xCA || bytes[1] != (byte) 0xFE || bytes[2] != (byte) 0xBA ||
                    bytes[3] != (byte) 0xBE) {
                throw new IllegalClassFormatException(className);
            }
            // ignore minor version
            // get major version
            int version = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
            if (version <= currentVersion) {
                // already at or below the target version
            LOGGER.finer("Ignoring " + className + " as it is already at or below the target version");
                return null;
            }
            LOGGER.fine("Transforming " + className + " from " + version + " to " + currentVersion);
//        if (loader instanceof DowngradingClassLoader) return bytes; // already handled
            Map<String, byte[]> outputs = ClassDowngrader.currentVersionDowngrader.downgrade(new AtomicReference<>(className), bytes, true, new Function<String, byte[]>() {
                @Override
                public byte[] apply(String s) {
                    try {
                        URL url = loader.getResource(s + ".class");
                        if (url == null) return null;
                        return Utils.readAllBytes(url.openStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            LOGGER.fine("transform size: " + (outputs == null ? null : outputs.size()));
            if (outputs == null || outputs.isEmpty()) return bytes;
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                LOGGER.fine("Loading " + entry.getKey() + " into " + loader);
                if (DUMP_CLASSES) {
                    ClassDowngrader.currentVersionDowngrader.writeBytesToDebug(entry.getKey(), entry.getValue());
                }
                if (entry.getKey().equals(className)) {
                    bytes = entry.getValue();
                    continue;
                }
                try {
                    defineClass.bindTo(loader).invoke(entry.getKey().replace('/', '.'), entry.getValue(), 0, entry.getValue().length);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
            return bytes;
        } catch (Throwable t) {
            System.err.println("Failed to transform " + className);
            t.printStackTrace(System.err);
            System.err.flush();
            System.exit(42);
            return null;
        }
    }

}
