package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.stub.Stub;

@J_L_FunctionalInterface
@Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/IntUnaryOperator"))
public interface J_U_F_IntUnaryOperator {

    int applyAsInt(int operand);

    default J_U_F_IntUnaryOperator compose(J_U_F_IntUnaryOperator before) {
        return (int v) -> applyAsInt(before.applyAsInt(v));
    }

    default J_U_F_IntUnaryOperator andThen(J_U_F_IntUnaryOperator after) {
        return (int t) -> after.applyAsInt(applyAsInt(t));
    }

    static J_U_F_IntUnaryOperator identity() {
        return t -> t;
    }

}