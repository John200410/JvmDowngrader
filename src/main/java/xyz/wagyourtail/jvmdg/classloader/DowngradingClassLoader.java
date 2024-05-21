package xyz.wagyourtail.jvmdg.classloader;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DowngradingClassLoader extends ClassLoader {
    private final ClassDowngrader currentVersionDowngrader;
    private final List<ClassLoader> delegates = new ArrayList<>();

    public DowngradingClassLoader(ClassDowngrader downgrader) {
        super();
        if (downgrader.target != Utils.getCurrentClassVersion()) {
            this.currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(downgrader.flags);
        } else {
            this.currentVersionDowngrader = downgrader;
        }
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, ClassLoader parent) {
        super(parent);
        if (downgrader.target != Utils.getCurrentClassVersion()) {
            this.currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(downgrader.flags);
        } else {
            this.currentVersionDowngrader = downgrader;
        }
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, URL[] urls, ClassLoader parent) {
        this(downgrader, parent);
        delegates.add(new URLClassLoader(urls, getParent()));
    }

    public DowngradingClassLoader(ClassDowngrader downgrader, URL[] urls) {
        this(downgrader);
        delegates.add(new URLClassLoader(urls, getParent()));
    }

    public void addDelegate(ClassLoader loader) {
        delegates.add(loader);
    }

    public void addDelegate(URL[] urls) {
        delegates.add(new URLClassLoader(urls, getParent()));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String internalName = name.replace('.', '/');
        String path = internalName + ".class";
        URL url = findResource(path);
        if (url == null) {
            return super.findClass(name);
        }
        byte[] bytes = null;
        try {
            bytes = Utils.readAllBytes(url.openStream());
            Map<String, byte[]> outputs = currentVersionDowngrader.downgrade(new AtomicReference<>(internalName), bytes, true, new Function<String, byte[]>() {
                @Override
                public byte[] apply(String s) {
                    try {
                        URL url = findResource(s + ".class");
                        if (url == null) return null;
                        return Utils.readAllBytes(url.openStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            if (outputs == null) {
                // doesn't need downgrading
                return defineClass(name, bytes, 0, bytes.length);
            }
            for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                if (entry.getKey().equals(internalName)) continue; // skip the main class (load later and returned)
                String extraName = entry.getKey().replace('/', '.');
                byte[] extraBytes = entry.getValue();
                try {
                    defineClass(extraName, extraBytes, 0, extraBytes.length);
                } catch (ClassFormatError e) {
                    currentVersionDowngrader.writeBytesToDebug(extraName, bytes);
                    throw e;
                }
            }
            try {
                bytes = outputs.get(internalName);
                if (bytes == null) {
                    throw new ClassNotFoundException("removed by downgrader: " + name);
                }
                return defineClass(name, bytes, 0, bytes.length);
            } catch (ClassFormatError e) {
                currentVersionDowngrader.writeBytesToDebug(name, bytes);
//                System.err.println("Failed to load class " + name + " with downgraded bytes, writing to debug folder.");
//                throw e;
                throw new ClassNotFoundException(name, e);
            }
        } catch (ClassFormatError e) {
            currentVersionDowngrader.writeBytesToDebug(name, bytes);
//           System.err.println("Failed to load class " + name + " with original bytes, writing to debug folder.");
//           throw e;
            throw new ClassNotFoundException(name, e);
        } catch (Throwable e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    protected URL findResource(String name) {
        for (ClassLoader delegate : delegates) {
            URL resource = delegate.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Vector<URL> vector = new Vector<>();
        for (ClassLoader delegate : delegates) {
            Enumeration<URL> enumeration = delegate.getResources(name);
            while (enumeration.hasMoreElements()) {
                vector.add(enumeration.nextElement());
            }
        }
        return vector.elements();
    }

}
