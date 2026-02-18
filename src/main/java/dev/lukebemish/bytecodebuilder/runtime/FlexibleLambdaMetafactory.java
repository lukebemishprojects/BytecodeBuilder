package dev.lukebemish.bytecodebuilder.runtime;

import dev.lukebemish.bytecodebuilder.ClassContext;
import dev.lukebemish.bytecodebuilder.Constants;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
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

        ClassDesc target = ClassDesc.of(caller.lookupClass().getName() + "$$FlexibleLambdaMetafactory$" + interfaceMethodName);
        ClassDesc toImplement = Constants.from(samType);

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
                65,
                Modifier.FINAL,
                target,
                isInterface ? ConstantDescs.CD_Object : toImplement,
                isInterface ? List.of(toImplement) : List.of(),
                null,
                (context, tracker) -> {
                    for (int i = 0; i < factoryType.parameterCount(); i++) {
                        context.field("arg$" + i, Modifier.PRIVATE | Modifier.FINAL, Constants.from(factoryType.parameterType(i)), null, null, field -> {
                        });
                    }

                    if (doStaticInit) {
                        context.field("$INSTANCE", Modifier.STATIC | Modifier.PRIVATE | Modifier.FINAL, toImplement, null, null, field -> {
                        });
                    }

                    context.constructor(
                        Modifier.PRIVATE,
                        Constants.from(ctorType),
                        null,
                        method -> method.code(code -> {
                            for (int i = 0; i < ctorType.parameterCount(); i++) {
                                var argType = Constants.from(ctorType.parameterType(i));
                                code.loadThis();
                                code.load(argType, i + 1);
                                code.field(DirectMethodHandleDesc.Kind.SETTER, target, "arg$" + i, argType);
                            }
                            code.loadThis();
                            code.method(DirectMethodHandleDesc.Kind.SPECIAL, isInterface ? ConstantDescs.CD_Object : toImplement, "<init>", Constants.from(MethodType.methodType(void.class)));
                            code.returnValue(ConstantDescs.CD_void);
                        })
                    );

                    context.method(
                        interfaceMethodName,
                        Modifier.FINAL | Modifier.PUBLIC,
                        Constants.from(samMethodType),
                        null,
                        null,
                        method -> method.code(code -> {
                            code.constant(tracker.dataConstant(ConstantDescs.CD_MethodHandle, finalImplementation));

                            for (int i = 0; i < ctorType.parameterCount(); i++) {
                                code.loadThis();
                                code.field(DirectMethodHandleDesc.Kind.GETTER, target, "arg$" + i, Constants.from(factoryType.parameterType(i)));
                            }

                            var lvIndex = 0;
                            for (var i = 0; i < samMethodType.parameterCount(); i++) {
                                var samArgType = samMethodType.parameterType(i);
                                code.load(Constants.from(samArgType), lvIndex + 1);
                                lvIndex += Constants.sizeOf(Constants.from(samArgType));
                            }

                            // We have all the arguments of the implementation present now

                            code.method(
                                DirectMethodHandleDesc.Kind.VIRTUAL,
                                ConstantDescs.CD_MethodHandle,
                                "invokeExact",
                                Constants.from(finalImplementation.type())
                            );

                            // Now just return
                            code.returnValue(Constants.from(finalImplementation.type().returnType()));
                        })
                    );

                    if (doStaticInit) {
                        context.method(
                            "<clinit>",
                            Modifier.STATIC,
                            Constants.from(MethodType.methodType(void.class)),
                            null,
                            null,
                            method -> method.code(code -> {
                                code.newInstance(target, Constants.from(MethodType.methodType(void.class)));
                                code.field(DirectMethodHandleDesc.Kind.STATIC_SETTER, target, "$INSTANCE", toImplement);
                                code.returnValue(ConstantDescs.CD_void);
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
