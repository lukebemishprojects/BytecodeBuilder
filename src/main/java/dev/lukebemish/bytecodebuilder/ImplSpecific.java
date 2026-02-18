package dev.lukebemish.bytecodebuilder;

import java.util.function.Consumer;

non-sealed abstract class ImplClassContext<T extends ImplClassContext<T, F, M>, F extends FieldContext<F>, M extends ImplMethodContext<M, ?>> extends ClassContext<T, F, M> {
    public static ClassContext<?, ?, ?> create(Consumer<? super ClassContext<?, ?, ?>> consumer) {
        return BackendASM.createClass(consumer);
    }

    public static ClassContext<?, ?, ?> create() {
        return BackendASM.createClass();
    }
}

non-sealed abstract class ImplMethodContext<T extends ImplMethodContext<T, C>, C extends ImplCodeContext<C>> extends MethodContext<T, C> {
    public static MethodContext<?, ?> create(Consumer<? super MethodContext<?, ?>> consumer) {
        return BackendASM.createMethod(consumer);
    }

    public static MethodContext<?, ?> create() {
        return BackendASM.createMethod();
    }
}

non-sealed abstract class ImplFieldContext<T extends ImplFieldContext<T>> extends FieldContext<T> {
    public static FieldContext<?> create(Consumer<? super FieldContext<?>> consumer) {
        return BackendASM.createField(consumer);
    }

    public static FieldContext<?> create() {
        return BackendASM.createField();
    }
}

non-sealed abstract class ImplCodeContext<T extends ImplCodeContext<T>> extends CodeContext<T> {
    public static CodeContext<?> create(Consumer<? super CodeContext<?>> consumer) {
        return BackendASM.createCode(consumer);
    }

    public static CodeContext<?> create() {
        return BackendASM.createCode();
    }
}
