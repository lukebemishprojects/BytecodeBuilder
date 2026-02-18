package dev.lukebemish.bytecodebuilder;

import org.jspecify.annotations.Nullable;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract sealed class ClassContext<T extends ClassContext<T, F, M>, F extends FieldContext<F>, M extends MethodContext<M, ?>> permits ImplClassContext, BackendASM.ClassContextASM {
    protected ClassContext() {}

    public abstract ClassContext<T, F, M> constructor(int access, MethodTypeDesc descriptor, @Nullable Collection<ClassDesc> exceptions, Consumer<? super M> remainder);

    public abstract ClassContext<T, F, M> method(String name, int access, MethodTypeDesc descriptor, @Nullable MethodSignature signature, @Nullable Collection<ClassDesc> exceptions, Consumer<? super M> remainder);

    public abstract ClassContext<T, F, M> field(String name, int access, ClassDesc descriptor, @Nullable Signature signature, @Nullable ConstantDesc constant, Consumer<? super F> remainder);

    public abstract byte[] build(int version, int access, ClassDesc name, ClassDesc superName, @Nullable Collection<ClassDesc> interfaces, @Nullable ClassSignature signature);

    public static ClassContext<?, ?, ?> create(Consumer<? super ClassContext<?, ?, ?>> consumer) {
        return ImplClassContext.create(consumer);
    }

    public static ClassContext<?, ?, ?> create() {
        return ImplClassContext.create();
    }

    public static MethodHandles.Lookup hidden(MethodHandles.Lookup lookup, boolean initialize, Set<MethodHandles.Lookup.ClassOption> options, int version, int access, ClassDesc name, ClassDesc superName, @Nullable Collection<ClassDesc> interfaces, @Nullable ClassSignature signature, BiConsumer<? super ClassContext<?, ?, ?>, ClassDataTracker> consumer) throws IllegalAccessException {
        var context = create();
        var tracker = new ClassDataTracker();
        consumer.accept(context, tracker);

        var bytes = context.build(version, access, name, superName, interfaces, signature);

        if (tracker.data.isEmpty()) {
            return lookup.defineHiddenClass(bytes, initialize, options.toArray(MethodHandles.Lookup.ClassOption[]::new));
        } else {
            return lookup.defineHiddenClassWithClassData(bytes, List.copyOf(tracker.data), initialize, options.toArray(MethodHandles.Lookup.ClassOption[]::new));
        }
    }
}
