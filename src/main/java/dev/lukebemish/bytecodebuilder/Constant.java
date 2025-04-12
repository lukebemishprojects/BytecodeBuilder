package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Constant {
    private final Object value;

    private Constant(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }
    
    public Constant dynamicCast(Descriptor target) {
        return dynamic(
                "cast",
                target,
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "nullConstant",
                        Descriptor.of(MethodType.methodType(
                                Object.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class,
                                Object.class
                        )),
                        false
                ),
                List.of(this)
        );
    }

    public static Constant dynamic(String name, Descriptor descriptor, ConstantHandle.ConstantMethodHandle bootstrap, Collection<Constant> arguments) {
        return new Constant(new ConstantDynamic(name, descriptor.descriptor(), bootstrap.asmHandle(), arguments.stream().map(Constant::value).toArray()));
    }

    public static Constant classData(Descriptor descriptor) {
        return dynamic(
                "_",
                descriptor,
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(MethodHandles.class),
                        "classData",
                        Descriptor.of(MethodType.methodType(
                                Object.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class
                        )),
                        false
                ),
                List.of()
        );
    }

    public static Constant classDataAt(Descriptor descriptor, Constant index) {
        return dynamic(
                "_",
                descriptor,
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(MethodHandles.class),
                        "classDataAt",
                        Descriptor.of(MethodType.methodType(
                                Object.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class,
                                int.class
                        )),
                        false
                ),
                List.of(index)
        );
    }

    public static Constant nullConstant(Descriptor descriptor) {
        return dynamic(
                "null",
                descriptor,
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "nullConstant",
                        Descriptor.of(MethodType.methodType(
                                Object.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class
                        )),
                        false
                ),
                List.of()
        );
    }

    public static Constant enumConstant(Descriptor descriptor, String name) {
        return dynamic(
                name,
                descriptor,
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "enumConstant",
                        Descriptor.of(MethodType.methodType(
                                Enum.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class
                        )),
                        false
                ),
                List.of()
        );
    }

    public static Constant staticFinalConstant(Constant owner, Descriptor descriptor, String name) {
        return dynamic(
                name,
                descriptor,
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "getStaticFinal",
                        Descriptor.of(MethodType.methodType(
                                Enum.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class,
                                Class.class
                        )),
                        false
                ),
                List.of(owner)
        );
    }
    
    public static Constant invokeConstant(Descriptor descriptor, Constant handle, Collection<Constant> arguments) {
        var fullArgs = new ArrayList<Constant>();
        fullArgs.add(handle);
        fullArgs.addAll(arguments);
        return dynamic(
                "invoke",
                descriptor,
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "invoke",
                        Descriptor.of(MethodType.methodType(
                                Enum.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class,
                                MethodHandle.class,
                                Object[].class
                        )),
                        false
                ),
                fullArgs
        );
    }
    
    public static Constant fieldVarHandle(Constant owner, Constant fieldType, String name) {
        return dynamic(
                name,
                Descriptor.of(VarHandle.class),
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "fieldVarHandle",
                        Descriptor.of(MethodType.methodType(
                                VarHandle.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class,
                                Class.class,
                                Class.class
                        )),
                        false
                ),
                List.of(owner, fieldType)
        );
    }
    
    public static Constant staticFieldVarHandle(Constant owner, Constant fieldType, String name) {
        return dynamic(
                name,
                Descriptor.of(VarHandle.class),
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "staticFieldVarHandle",
                        Descriptor.of(MethodType.methodType(
                                VarHandle.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class,
                                Class.class
                        )),
                        false
                ),
                List.of(owner, fieldType)
        );
    }
    
    public static Constant arrayVarHandle(Constant arrayType) {
        return dynamic(
                "arrayVarHandle",
                Descriptor.of(VarHandle.class),
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "arrayVarHandle",
                        Descriptor.of(MethodType.methodType(
                                VarHandle.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class
                        )),
                        false
                ),
                List.of(arrayType)
        );
    }

    public static Constant of(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            // We use a primitive class dynamic
            return primitiveWrapperClass(clazz.descriptorString());
        }
        
        return new Constant(Type.getType(clazz));
    }

    private static Constant primitiveWrapperClass(String descriptor) {
        return dynamic(
                descriptor,
                Descriptor.of(Class.class),
                ConstantHandle.of(
                        MethodOperation.INVOKESTATIC,
                        Descriptor.of(ConstantBootstraps.class),
                        "primitiveClass",
                        Descriptor.of(MethodType.methodType(
                                Class.class,
                                MethodHandles.Lookup.class,
                                String.class,
                                Class.class
                        )),
                        false
                ),
                List.of()
        );
    }

    public static Constant of(Descriptor descriptor) {
        return of(descriptor.asmType());
    }
    
    public static Constant of(MethodType methodType) {
        Type[] argumentTypes = new Type[methodType.parameterCount()];
        for (int i = 0; i < methodType.parameterCount(); i++) {
            argumentTypes[i] = Type.getType(methodType.parameterType(i));
        }
        return new Constant(Type.getMethodType(Type.getType(methodType.returnType()), argumentTypes));
    }
    
    public static Constant of(ConstantHandle constantHandle) {
        return new Constant(constantHandle.asmHandle());
    }
    
    public static Constant of(ConstantDynamic constantDynamic) {
        return new Constant(constantDynamic);
    }
    
    public static Constant of(Handle handle) {
        return new Constant(handle);
    }
    
    public static Constant of(Type type) {
        if (type.getSort() <= Type.DOUBLE) {
            // We use a primitive class dynamic
            return primitiveWrapperClass(type.getDescriptor());
        }
        
        return new Constant(type);
    }
    
    public static Constant of(String value) {
        return new Constant(value);
    }
    
    public static Constant of(int value) {
        return new Constant(value);
    }
    
    public static Constant of(long value) {
        return new Constant(value);
    }
    
    public static Constant of(float value) {
        return new Constant(value);
    }
    
    public static Constant of(double value) {
        return new Constant(value);
    }
    
    public static Constant of(boolean value) {
        return new Constant(value ? 1 : 0);
    }
    
    public static Constant of(byte value) {
        return new Constant((int) value);
    }
    
    public static Constant of(short value) {
        return new Constant((int) value);
    }
    
    public static Constant of(char value) {
        return new Constant((int) value);
    }
}
