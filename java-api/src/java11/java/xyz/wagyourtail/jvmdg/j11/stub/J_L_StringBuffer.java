package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_StringBuffer {

    @Stub(javaVersion = Opcodes.V11)
    public static int compareTo(StringBuffer sb, StringBuffer other) {
        synchronized (sb) {
            return J_L_CharSequence.compare(sb, other);
        }
    }

}