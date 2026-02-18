package dev.lukebemish.bytecodebuilder;

import java.util.function.Consumer;

public abstract sealed class FieldContext<T extends FieldContext<T>> permits ImplFieldContext, BackendASM.FieldContextASM {
    protected FieldContext() {}

    public static FieldContext<?> create(Consumer<? super FieldContext<?>> consumer) {
        return ImplFieldContext.create(consumer);
    }

    public static FieldContext<?> create() {
        return ImplFieldContext.create();
    }
}
