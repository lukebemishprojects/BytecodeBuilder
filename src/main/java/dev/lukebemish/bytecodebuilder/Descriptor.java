package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public final class Descriptor {
    public static final Descriptor VOID = new Descriptor(Type.VOID_TYPE);
    public static final Descriptor BOOLEAN = new Descriptor(Type.BOOLEAN_TYPE);
    public static final Descriptor BYTE = new Descriptor(Type.BYTE_TYPE);
    public static final Descriptor CHAR = new Descriptor(Type.CHAR_TYPE);
    public static final Descriptor SHORT = new Descriptor(Type.SHORT_TYPE);
    public static final Descriptor INT = new Descriptor(Type.INT_TYPE);
    public static final Descriptor LONG = new Descriptor(Type.LONG_TYPE);
    public static final Descriptor FLOAT = new Descriptor(Type.FLOAT_TYPE);
    public static final Descriptor DOUBLE = new Descriptor(Type.DOUBLE_TYPE);
    public static final Descriptor OBJECT = of(Object.class);
    public static final Descriptor METHOD_HANDLE = of(MethodHandle.class);
    public static final Descriptor METHOD_TYPE = of(MethodType.class);
    public static final Descriptor CLASS = of(Class.class);

    private final Type type;

    private Descriptor(Type type) {
        this.type = type;
    }
    
    public Type asmType() {
        return type;
    }
    
    public String descriptor() {
        return type.getDescriptor();
    }
    
    public String internalName() {
        if (type.getSort() != Type.ARRAY && type.getSort() != Type.OBJECT) {
            throw new IllegalStateException("Descriptor is not an object or array type");
        }
        return type.getInternalName();
    }
    
    public int size() {
        return type.getSize();
    }
    
    public int opcodeArrayLoad() {
        return type.getOpcode(Opcodes.IALOAD);
    }
    
    public int opcodeArrayStore() {
        return type.getOpcode(Opcodes.IASTORE);
    }
    
    public int opcodeReturn() {
        return type.getOpcode(Opcodes.IRETURN);
    }
    
    public int opcodeLoad() {
        return type.getOpcode(Opcodes.ILOAD);
    }
    
    public int opcodeStore() {
        return type.getOpcode(Opcodes.ISTORE);
    }
    
    public static Descriptor of(String descriptor) {
        return new Descriptor(Type.getType(descriptor));
    }
    
    public static Descriptor of(Type type) {
        return new Descriptor(type);
    }
    
    public static Descriptor of(Class<?> clazz) {
        return new Descriptor(Type.getType(clazz));
    }
    
    public static Descriptor of(MethodType methodType) {
        Type[] argumentTypes = new Type[methodType.parameterCount()];
        for (int i = 0; i < methodType.parameterCount(); i++) {
            argumentTypes[i] = Type.getType(methodType.parameterType(i));
        }
        return new Descriptor(Type.getMethodType(Type.getType(methodType.returnType()), argumentTypes));
    }
}

