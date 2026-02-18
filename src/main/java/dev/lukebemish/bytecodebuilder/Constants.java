package dev.lukebemish.bytecodebuilder;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public final class Constants {
    private Constants() {}

    public static DynamicConstantDesc<?> cast(ConstantDesc value, ClassDesc type) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_EXPLICIT_CAST,
            ConstantDescs.DEFAULT_NAME,
            type,
            value
        );
    }

    public static DynamicConstantDesc<?> classData(ClassDesc descriptor) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_CLASS_DATA,
            ConstantDescs.DEFAULT_NAME,
            descriptor
        );
    }

    public static DynamicConstantDesc<?> classDataAt(ClassDesc descriptor, ConstantDesc index) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_CLASS_DATA_AT,
            ConstantDescs.DEFAULT_NAME,
            descriptor,
            index
        );
    }

    public static DynamicConstantDesc<?> nullConstant(ClassDesc descriptor) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_NULL_CONSTANT,
            ConstantDescs.DEFAULT_NAME,
            descriptor
        );
    }

    public static DynamicConstantDesc<?> enumConstant(ClassDesc type, String name) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_ENUM_CONSTANT,
            ConstantDescs.DEFAULT_NAME,
            type,
            name
        );
    }

    public static DynamicConstantDesc<?> staticFinalConstant(ClassDesc type, String name, ClassDesc descriptor) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_GET_STATIC_FINAL,
            name,
            descriptor,
            type
        );
    }

    public static DynamicConstantDesc<?> invokeConstant(ClassDesc descriptor, ConstantDesc handle, ConstantDesc... args) {
        var allArgs = new ConstantDesc[args.length + 1];
        allArgs[0] = handle;
        System.arraycopy(args, 0, allArgs, 1, args.length);
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_INVOKE,
            ConstantDescs.DEFAULT_NAME,
            descriptor,
            allArgs
        );
    }

    public static DynamicConstantDesc<?> fieldVarHandle(ClassDesc owner, String name, ClassDesc descriptor) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_VARHANDLE_FIELD,
            name,
            ConstantDescs.CD_VarHandle,
            owner,
            descriptor
        );
    }

    public static DynamicConstantDesc<?> staticFieldVarHandle(ClassDesc owner, String name, ClassDesc descriptor) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_VARHANDLE_STATIC_FIELD,
            name,
            ConstantDescs.CD_VarHandle,
            owner,
            descriptor
        );
    }

    public static DynamicConstantDesc<?> arrayVarHandle(ClassDesc descriptor) {
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_VARHANDLE_ARRAY,
            ConstantDescs.DEFAULT_NAME,
            ConstantDescs.CD_VarHandle,
            descriptor
        );
    }

    public static ConstantDesc from(Constable value) {
        return value.describeConstable().orElseThrow();
    }

    public static ClassDesc from(Class<?> clazz) {
        return clazz.describeConstable().orElseThrow();
    }

    public static MethodHandleDesc from(MethodHandle handle) {
        return handle.describeConstable().orElseThrow();
    }

    public static MethodTypeDesc from(MethodType type) {
        return type.describeConstable().orElseThrow();
    }

    public static int sizeOf(ClassDesc descriptor) {
        return switch (descriptor.descriptorString()) {
            case "J", "D" -> 2;
            case "V" -> 0;
            default -> 1;
        };
    }
}
