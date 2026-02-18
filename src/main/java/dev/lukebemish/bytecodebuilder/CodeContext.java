package dev.lukebemish.bytecodebuilder;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Collection;
import java.util.function.Consumer;

public abstract sealed class CodeContext<T extends CodeContext<T>> permits ImplCodeContext, BackendASM.CodeContextASM {
    protected CodeContext() {}

    public abstract CodeContext<T> constant(ConstantDesc constant);

    public abstract CodeContext<T> load(ClassDesc descriptor, int index);

    public abstract CodeContext<T> store(ClassDesc descriptor, int index);

    public abstract CodeContext<T> newArray(ClassDesc descriptor);

    public abstract CodeContext<T> instanceOf(ClassDesc descriptor);

    public abstract CodeContext<T> checkCast(ClassDesc descriptor);

    public abstract CodeContext<T> returnValue(ClassDesc descriptor);

    public abstract CodeContext<T> field(DirectMethodHandleDesc.Kind operation, ClassDesc owner, String name, ClassDesc descriptor);

    public abstract CodeContext<T> method(DirectMethodHandleDesc.Kind operation, ClassDesc owner, String name, MethodTypeDesc descriptor);

    public abstract CodeContext<T> newInstance(ClassDesc owner, MethodTypeDesc constructorDescriptor);

    public abstract CodeContext<T> invokeDynamic(String name, MethodTypeDesc descriptor, DirectMethodHandleDesc bootstrap, Collection<ConstantDesc> bootstrapArguments);

    public abstract CodeContext<T> loadThis();

    public static CodeContext<?> create(Consumer<? super CodeContext<?>> consumer) {
        return ImplCodeContext.create(consumer);
    }

    public static CodeContext<?> create() {
        return ImplCodeContext.create();
    }
}
