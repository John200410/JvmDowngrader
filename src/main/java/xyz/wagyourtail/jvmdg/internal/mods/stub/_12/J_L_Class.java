package xyz.wagyourtail.jvmdg.internal.mods.stub._12;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.lang.reflect.Array;

public class J_L_Class {

    @Stub(JavaVersion.VERSION_12)
    public static String descriptorString(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "boolean":
                    return "Z";
                case "byte":
                    return "B";
                case "char":
                    return "C";
                case "short":
                    return "S";
                case "int":
                    return "I";
                case "long":
                    return "J";
                case "float":
                    return "F";
                case "double":
                    return "D";
                case "void":
                    return "V";
                default:
                    throw new InternalError("Unknown primitive type: " + clazz.getName());
            }
        }
        if (clazz.isArray()) {
            return clazz.getName().replace('.', '/');
        }
        return "L" + clazz.getName().replace('.', '/') + ";";
    }

    @Stub(JavaVersion.VERSION_12)
    public static Class<?> componentType(Class<?> clazz) throws ClassNotFoundException {
        return clazz.getComponentType();
    }

    @Stub(JavaVersion.VERSION_12)
    public static Class<?> arrayType(Class<?> clazz) {
        return Array.newInstance(clazz, 0).getClass();
    }

}