package xyz.wagyourtail.jvmdg.j15.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Math {

    @Stub(javaVersion = Opcodes.V15, ref = @Ref("Ljava/lang/Math;"))
    public static int absExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("Overflow to represent absolute value of Integer.MIN_VALUE");
        }
        return Math.abs(a);
    }

    @Stub(javaVersion = Opcodes.V15, ref = @Ref("Ljava/lang/Math;"))
    public static long absExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("Overflow to represent absolute value of Long.MIN_VALUE");
        }
        return Math.abs(a);
    }

}