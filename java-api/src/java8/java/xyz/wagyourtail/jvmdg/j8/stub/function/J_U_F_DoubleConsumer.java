package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.stub.Stub;

@J_L_FunctionalInterface
@Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/DoubleConsumer"))
public interface J_U_F_DoubleConsumer {

    void accept(double value);

    default J_U_F_DoubleConsumer andThen(J_U_F_DoubleConsumer after) {
        return (double t) -> { accept(t); after.accept(t); };
    }

}