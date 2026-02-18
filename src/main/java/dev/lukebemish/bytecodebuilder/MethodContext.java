package dev.lukebemish.bytecodebuilder;

import java.util.function.Consumer;

public abstract sealed class MethodContext<T extends MethodContext<T, C>, C extends CodeContext<C>> permits ImplMethodContext, BackendASM.MethodContextASM {
    public abstract MethodContext<T, C> code(Consumer<? super C> consumer);

    public static MethodContext<?, ?> create(Consumer<? super MethodContext<?, ?>> consumer) {
        return ImplMethodContext.create(consumer);
    }

    public static MethodContext<?, ?> create() {
        return ImplMethodContext.create();
    }
}
