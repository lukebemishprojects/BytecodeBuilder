package dev.lukebemish.bytecodebuilder.runtime;

import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class Coercion {
    private Coercion() {}

    @SuppressWarnings("unchecked")
    public static <T> T coerce(MethodHandle handle, Class<T> targetSamClass) throws LambdaConversionException {
        var method = findAbstractMethod(targetSamClass);
        var samMethodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());

        var callsite = FlexibleLambdaMetafactory.metafactory(
            MethodHandles.lookup(),
            method.getName(),
            MethodType.methodType(targetSamClass),
            samMethodType,
            handle,
            samMethodType
        );

        try {
            return (T) callsite.dynamicInvoker().invoke();
        } catch (Throwable e) {
            throw new LambdaConversionException(e);
        }
    }

    public static <F> F coerceCapturing(MethodHandle handle, Class<?> targetSamClass, Class<F> targetFactoryClass) throws LambdaConversionException {
        var factoryHandle = coerceCapturing(handle, targetSamClass);
        return coerce(factoryHandle, targetFactoryClass);
    }

    public static MethodHandle coerceCapturing(MethodHandle handle, Class<?> targetSamClass) throws LambdaConversionException {
        var method = findAbstractMethod(targetSamClass);
        var samMethodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        var capturedArgArity = handle.type().parameterCount() - samMethodType.parameterCount();

        var ctorArgs = new Class[capturedArgArity];
        for (var i = 0; i < capturedArgArity; i++) {
            ctorArgs[i] = handle.type().parameterType(i);
        }

        var targetType = MethodType.methodType(targetSamClass, ctorArgs);

        var callsite = FlexibleLambdaMetafactory.metafactory(
            MethodHandles.lookup(),
            method.getName(),
            targetType,
            samMethodType,
            handle,
            samMethodType
        );

        return callsite.dynamicInvoker();
    }

    private static Method findAbstractMethod(Class<?> targetSamClass) {
        AtomicReference<Method> found = new AtomicReference<>();
        Set<String> checked = new HashSet<>();
        Consumer<Class<?>> addAbstractMethods = new Consumer<>() {
            @Override
            public void accept(Class<?> cls) {
                for (var method : cls.getDeclaredMethods()) {
                    if (method.accessFlags().contains(AccessFlag.STATIC)) {
                        continue;
                    }
                    var tag = method.getName() + MethodType.methodType(method.getReturnType(), method.getParameterTypes()).descriptorString();
                    if (checked.contains(tag)) {
                        continue;
                    }
                    checked.add(tag);
                    if (method.accessFlags().contains(AccessFlag.ABSTRACT)) {
                        var existing = found.getAndSet(method);
                        if (existing != null) {
                            throw new IllegalArgumentException("Found two abstract methods in " + cls.getName() + ": " + existing + " and " + method);
                        }
                    }
                }
                for (var iface : cls.getInterfaces()) {
                    this.accept(iface);
                }
                var superClass = cls.getSuperclass();
                if (superClass != null && superClass != Object.class) {
                    this.accept(superClass);
                }
            }
        };
        addAbstractMethods.accept(targetSamClass);
        var method = found.get();
        if (method == null) {
            throw new IllegalArgumentException("No abstract method found in " + targetSamClass.getName());
        }
        return method;
    }
}
