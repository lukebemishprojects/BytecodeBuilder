package dev.lukebemish.bytecodebuilder.runtime;

import dev.lukebemish.bytecodebuilder.ClassContext;
import dev.lukebemish.bytecodebuilder.Descriptor;
import dev.lukebemish.bytecodebuilder.FieldOperation;
import dev.lukebemish.bytecodebuilder.MethodOperation;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Set;

public final class FlexibleLambdaMetafactory {
    private FlexibleLambdaMetafactory() {}

    public static CallSite metafactory(MethodHandles.Lookup caller,
                                       String interfaceMethodName,
                                       MethodType factoryType,
                                       MethodType samMethodType,
                                       MethodHandle implementation,
                                       MethodType dynamicMethodType) throws LambdaConversionException {
        var samType = factoryType.returnType();
        var isInterface = samType.isInterface();

        Descriptor target = Descriptor.of(Type.getObjectType(Type.getType(caller.lookupClass()).getInternalName() + "$$FlexibleLambdaMetafactory$" + interfaceMethodName));
        Descriptor toImplement = Descriptor.of(samType);

        var ctorType = factoryType.changeReturnType(void.class);

        var capturedArity = ctorType.parameterCount();

        var handleType = implementation.type();
        // We get in types of sam arg type, need to convert them to functional type, and then convert _that_ to the implementation type
        // Similarly, we get _out_ types of the implementation type, need to convert them to the functional type, and then to the sam type

        // First conversion: impl -> functional
        handleType = handleType.changeReturnType(dynamicMethodType.returnType());
        for (var i = 0; i < dynamicMethodType.parameterCount(); i++) {
            var argType = dynamicMethodType.parameterType(i);
            handleType = handleType.changeParameterType(i + capturedArity, argType);
        }
        implementation = implementation.asType(handleType);

        // Second conversion: functional -> sam
        handleType = handleType.changeReturnType(samMethodType.returnType());
        for (var i = 0; i < samMethodType.parameterCount(); i++) {
            var argType = samMethodType.parameterType(i);
            handleType = handleType.changeParameterType(i + capturedArity, argType);
        }
        implementation = implementation.asType(handleType);
        var finalImplementation = implementation;

        var doStaticInit = capturedArity == 0;

        try {
            var hiddenLookup = ClassContext.hidden(
                caller,
                false,
                Set.of(MethodHandles.Lookup.ClassOption.NESTMATE),
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS,
                Opcodes.V21,
                Opcodes.ACC_FINAL,
                target,
                isInterface ? Descriptor.OBJECT : toImplement,
                isInterface ? List.of(toImplement) : List.of(),
                null,
                (context, tracker) -> {
                    for (int i = 0; i < factoryType.parameterCount(); i++) {
                        context.field("arg$" + i, Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, Descriptor.of(factoryType.parameterType(i)), null, null, field -> {
                        });
                    }

                    if (doStaticInit) {
                        context.field("$INSTANCE", Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, toImplement, null, null, field -> {
                        });
                    }

                    context.constructor(
                        Opcodes.ACC_PRIVATE,
                        Descriptor.of(ctorType),
                        null,
                        method -> method.code(code -> {
                            for (int i = 0; i < ctorType.parameterCount(); i++) {
                                var argType = Descriptor.of(ctorType.parameterType(i));
                                code.loadThis();
                                code.load(argType, i + 1);
                                code.field(FieldOperation.PUTFIELD, target, "arg$" + i, argType);
                            }
                            code.loadThis();
                            code.method(MethodOperation.INVOKESPECIAL, isInterface ? Descriptor.OBJECT : toImplement, "<init>", Descriptor.of(MethodType.methodType(void.class)), false);
                            code.returnValue(Descriptor.VOID);
                        })
                    );

                    context.method(
                        interfaceMethodName,
                        Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC,
                        Descriptor.of(samMethodType),
                        null,
                        null,
                        method -> method.code(code -> {
                            code.constant(tracker.dataConstant(Descriptor.METHOD_HANDLE, finalImplementation));

                            for (int i = 0; i < ctorType.parameterCount(); i++) {
                                code.loadThis();
                                code.field(FieldOperation.GETFIELD, target, "arg$" + i, Descriptor.of(factoryType.parameterType(i)));
                            }

                            var lvIndex = 0;
                            for (var i = 0; i < samMethodType.parameterCount(); i++) {
                                var samArgType = samMethodType.parameterType(i);
                                code.load(Descriptor.of(samArgType), lvIndex + 1);
                                lvIndex += Descriptor.of(samArgType).size();
                            }

                            // We have all the arguments of the implementation present now

                            code.method(
                                MethodOperation.INVOKEVIRTUAL,
                                Descriptor.METHOD_HANDLE,
                                "invokeExact",
                                Descriptor.of(finalImplementation.type()),
                                false
                            );

                            // Now just return
                            code.returnValue(Descriptor.of(finalImplementation.type().returnType()));
                        })
                    );

                    if (doStaticInit) {
                        context.method(
                            "<clinit>",
                            Opcodes.ACC_STATIC,
                            Descriptor.of(MethodType.methodType(void.class)),
                            null,
                            null,
                            method -> method.code(code -> {
                                code.newInstance(target, Descriptor.of(MethodType.methodType(void.class)));
                                code.field(FieldOperation.PUTSTATIC, target, "$INSTANCE", toImplement);
                                code.returnValue(Descriptor.VOID);
                            })
                        );
                    }
                }
            );

            MethodHandle handle;
            if (doStaticInit) {
                handle = hiddenLookup.findStaticGetter(hiddenLookup.lookupClass(), "$INSTANCE", samType);
            } else {
                handle = hiddenLookup.findConstructor(hiddenLookup.lookupClass(), ctorType);
            }
            handle = handle.asType(factoryType);
            return new ConstantCallSite(handle);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            throw new LambdaConversionException(e);
        }
    }
}
