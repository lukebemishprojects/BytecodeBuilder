package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class CodeContext {
    private final List<Consumer<MethodVisitor>> codeVisitors = new ArrayList<>();
    
    private CodeContext() {}
    
    public CodeContext asm(Consumer<MethodVisitor> consumer) {
        codeVisitors.add(consumer);
        return this;
    }
    
    public CodeContext instruction(int opcode) {
        codeVisitors.add(mv -> mv.visitInsn(opcode));
        return this;
    }
    
    public CodeContext constant(Constant constant) {
        codeVisitors.add(mv -> {
            switch (constant.value()) {
                case Integer i -> {
                    int iInt = i;
                    if (iInt == -1) {
                        mv.visitInsn(Opcodes.ICONST_M1);
                    } else if (0 <= iInt && iInt <= 5) {
                        mv.visitInsn(Opcodes.ICONST_0 + iInt);
                    } else if (iInt >= Byte.MIN_VALUE && iInt <= Byte.MAX_VALUE) {
                        mv.visitIntInsn(Opcodes.BIPUSH, iInt);
                    } else if (iInt >= Short.MIN_VALUE && iInt <= Short.MAX_VALUE) {
                        mv.visitIntInsn(Opcodes.SIPUSH, iInt);
                    } else {
                        mv.visitLdcInsn(i);
                    }
                }
                case Long l -> {
                    long lLong = l;
                    if (lLong == 0L) {
                        mv.visitInsn(Opcodes.LCONST_0);
                    } else if (lLong == 1L) {
                        mv.visitInsn(Opcodes.LCONST_1);
                    } else {
                        mv.visitLdcInsn(l);
                    }
                }
                case Float f -> {
                    float fFloat = f;
                    if (fFloat == 0f) {
                        mv.visitInsn(Opcodes.FCONST_0);
                    } else if (fFloat == 1f) {
                        mv.visitInsn(Opcodes.FCONST_1);
                    } else if (fFloat == 2f) {
                        mv.visitInsn(Opcodes.FCONST_2);
                    } else {
                        mv.visitLdcInsn(f);
                    }
                }
                case Double d -> {
                    double dDouble = d;
                    if (dDouble == 0d) {
                        mv.visitInsn(Opcodes.DCONST_0);
                    } else if (dDouble == 1d) {
                        mv.visitInsn(Opcodes.DCONST_1);
                    } else {
                        mv.visitLdcInsn(d);
                    }
                }
                default -> mv.visitLdcInsn(constant.value());
            }
        });
        return this;
    }
    
    public CodeContext load(Descriptor descriptor, int index) {
        codeVisitors.add(mv -> mv.visitVarInsn(descriptor.opcodeLoad(), index));
        return this;
    }
    
    public CodeContext store(Descriptor descriptor, int index) {
        codeVisitors.add(mv -> mv.visitVarInsn(descriptor.opcodeStore(), index));
        return this;
    }
    
    public CodeContext newArray(Descriptor descriptor) {
        codeVisitors.add(mv -> {
            if (descriptor.asmType().getSort() <= Type.DOUBLE) {
                // primitive type
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN + descriptor.asmType().getSort() - Type.BOOLEAN);
            } else {
                mv.visitTypeInsn(Opcodes.ANEWARRAY, descriptor.internalName());
            }
        });
        return this;
    }
    
    public CodeContext instanceOf(Descriptor descriptor) {
        codeVisitors.add(mv -> mv.visitTypeInsn(Opcodes.INSTANCEOF, descriptor.internalName()));
        return this;
    }
    
    public CodeContext checkCast(Descriptor descriptor) {
        codeVisitors.add(mv -> mv.visitTypeInsn(Opcodes.CHECKCAST, descriptor.internalName()));
        return this;
    }
    
    public CodeContext returnValue(Descriptor descriptor) {
        codeVisitors.add(mv -> mv.visitInsn(descriptor.opcodeReturn()));
        return this;
    }
    
    public CodeContext field(FieldOperation operation, Descriptor owner, String name, Descriptor descriptor) {
        codeVisitors.add(mv -> mv.visitFieldInsn(operation.opcode, owner.internalName(), name, descriptor.descriptor()));
        return this;
    }
    
    public CodeContext method(MethodOperation operation, Descriptor owner, String name, Descriptor descriptor, boolean isInterface) {
        if (operation == MethodOperation.NEWINVOKESPECIAL) {
            if (!"<init>".equals(name)) {
                throw new IllegalArgumentException("NEWINVOKESPECIAL must be used with <init> method");
            }
            return newInstance(owner, descriptor);
        }
        codeVisitors.add(mv -> mv.visitMethodInsn(operation.opcode, owner.internalName(), name, descriptor.descriptor(), isInterface));
        return this;
    }
    
    public CodeContext newInstance(Descriptor owner, Descriptor constructorDescriptor) {
        codeVisitors.add(mv -> {
            mv.visitTypeInsn(Opcodes.NEW, owner.internalName());
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner.internalName(), "<init>", constructorDescriptor.descriptor(), false);
        });
        return this;
    }
    
    public CodeContext invokeDynamic(String name, Descriptor descriptor, ConstantHandle.ConstantMethodHandle bootstrap, Collection<Constant> bootstrapArguments) {
        codeVisitors.add(mv -> mv.visitInvokeDynamicInsn(name, descriptor.descriptor(), bootstrap.asmHandle(), bootstrapArguments.stream().map(Constant::value).toArray()));
        return this;
    }
    
    public CodeContext jump(int instruction, Consumer<CodeContext> skip) {
        codeVisitors.add(mv -> {
            Label label = new Label();
            mv.visitJumpInsn(instruction, label);
            
            CodeContext skipContext = new CodeContext();
            skip.accept(skipContext);
            skipContext.apply(mv);
            
            mv.visitLabel(label);
        });
        return this;
    }

    public CodeContext loadThis() {
        codeVisitors.add(mv -> mv.visitVarInsn(Opcodes.ALOAD, 0));
        return this;
    }
    
    public void apply(MethodVisitor methodVisitor) {
        for (Consumer<MethodVisitor> consumer : codeVisitors) {
            consumer.accept(methodVisitor);
        }
    }
    
    public static CodeContext create(Consumer<CodeContext> consumer) {
        var codeContext = new CodeContext();
        consumer.accept(codeContext);
        return codeContext;
    }
    
    public static CodeContext create() {
        return new CodeContext();
    }
}
