package dev.lukebemish.bytecodebuilder;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * The contents of this class rely on ASM being present at runtime. Note that simply depending on BytecodeBuilder does
 * not guarantee this; the rest of BytecodeBuilder's API does not require ASM on Java 24+ (and dependencies or a lack
 * thereof are published reflecting this).
 */
public final class BackendASM {
    private BackendASM() {}

    public static ClassContextASM createClass(Consumer<? super ClassContextASM> consumer) {
        var classContext = new ClassContextASM();
        consumer.accept(classContext);
        return classContext;
    }

    public static ClassContextASM createClass() {
        return new ClassContextASM();
    }

    public static FieldContextASM createField(Consumer<? super FieldContextASM> consumer) {
        var fieldContext = new FieldContextASM();
        consumer.accept(fieldContext);
        return fieldContext;
    }

    public static FieldContextASM createField() {
        return new FieldContextASM();
    }

    public static MethodContextASM createMethod(Consumer<? super MethodContextASM> consumer) {
        var methodContext = new MethodContextASM();
        consumer.accept(methodContext);
        return methodContext;
    }

    public static MethodContextASM createMethod() {
        return new MethodContextASM();
    }

    public static CodeContextASM createCode(Consumer<? super CodeContextASM> consumer) {
        var codeContext = new CodeContextASM();
        consumer.accept(codeContext);
        return codeContext;
    }

    public static CodeContextASM createCode() {
        return new CodeContextASM();
    }

    public static final class ClassContextASM extends ClassContext<ClassContextASM, FieldContextASM, MethodContextASM> {
        private final List<Consumer<ClassVisitor>> classVisitors = new ArrayList<>();

        private ClassContextASM() {}

        public ClassContextASM asm(Consumer<ClassVisitor> consumer) {
            classVisitors.add(consumer);
            return this;
        }

        @Override
        public ClassContextASM constructor(int access, MethodTypeDesc descriptor, @Nullable Collection<ClassDesc> exceptions, Consumer<? super MethodContextASM> remainder) {
            return method("<init>", access, descriptor, null, exceptions, remainder);
        }

        @Override
        public ClassContextASM method(String name, int access, MethodTypeDesc descriptor, @Nullable MethodSignature signature, @Nullable Collection<ClassDesc> exceptions, Consumer<? super MethodContextASM> remainder) {
            var exceptionsNames = new String[exceptions == null ? 0 : exceptions.size()];
            if (exceptions != null) {
                int i = 0;
                for (var exception : exceptions) {
                    exceptionsNames[i++] = ConstantsASM.toAsm(exception).getInternalName();
                }
            }

            var methodContext = createMethod(remainder);

            classVisitors.add(cv -> {
                var mv = cv.visitMethod(access, name, descriptor.descriptorString(), signature == null ? null : signature.signature(), exceptionsNames);
                methodContext.apply(mv);
                mv.visitEnd();
            });
            return this;
        }

        @Override
        public ClassContextASM field(String name, int access, ClassDesc descriptor, @Nullable Signature signature, @Nullable ConstantDesc constant, Consumer<? super FieldContextASM> remainder) {
            final Object constValue;
            if (constant != null) {
                if ((access & Opcodes.ACC_STATIC) == 0 || (access & Opcodes.ACC_FINAL) == 0) {
                    throw new IllegalArgumentException("Constant value can only be set for static final fields");
                }
                constValue = ConstantsASM.toAsm(constant);
                switch (constValue) {
                    case Integer ignored -> {}
                    case Long ignored -> {}
                    case Float ignored -> {}
                    case Double ignored -> {}
                    case String ignored -> {}
                    default -> throw new IllegalArgumentException("Constant value must be a primitive or string for a field initializer, but was "+constValue);
                }
            } else {
                constValue = null;
            }

            var fieldContext = createField(remainder);

            classVisitors.add(cv -> {
                var fv = cv.visitField(access, name, descriptor.descriptorString(), signature == null ? null : signature.signature(), constValue);
                fieldContext.apply(fv);
                fv.visitEnd();
            });
            return this;
        }

        public void apply(ClassVisitor classVisitor) {
            for (Consumer<ClassVisitor> consumer : classVisitors) {
                consumer.accept(classVisitor);
            }
        }

        @Override
        public byte[] build(int version, int access, ClassDesc name, ClassDesc superName, @Nullable Collection<ClassDesc> interfaces, @Nullable ClassSignature signature) {
            var cv = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cv.visit(version, access, ConstantsASM.toAsm(name).getInternalName(), signature == null ? null : signature.signature(), ConstantsASM.toAsm(superName).getInternalName(), interfaces == null ? null : interfaces.stream().map(ConstantsASM::toAsm).map(Type::getInternalName).toArray(String[]::new));
            apply(cv);
            cv.visitEnd();
            return cv.toByteArray();
        }
    }

    public static final class FieldContextASM extends FieldContext<FieldContextASM> {
        private final List<Consumer<FieldVisitor>> classVisitors = new ArrayList<>();

        private FieldContextASM() {}

        public FieldContextASM asm(Consumer<FieldVisitor> consumer) {
            classVisitors.add(consumer);
            return this;
        }

        public void apply(FieldVisitor fieldVisitor) {
            for (Consumer<FieldVisitor> consumer : classVisitors) {
                consumer.accept(fieldVisitor);
            }
        }
    }

    public static final class MethodContextASM extends MethodContext<MethodContextASM, CodeContextASM> {
        private final List<Consumer<MethodVisitor>> methodVisitors = new ArrayList<>();

        private MethodContextASM() {}

        public MethodContextASM asm(Consumer<MethodVisitor> consumer) {
            methodVisitors.add(consumer);
            return this;
        }

        public MethodContextASM code(Consumer<? super CodeContextASM> consumer, int maxStack, int maxLocal) {
            var codeContext = createCode(consumer);
            methodVisitors.add(mv -> {
                mv.visitCode();
                codeContext.apply(mv);
                mv.visitMaxs(maxStack, maxLocal);
            });
            return this;
        }

        @Override
        public MethodContextASM code(Consumer<? super CodeContextASM> consumer) {
            return code(consumer, 0, 0);
        }

        public void apply(MethodVisitor methodVisitor) {
            for (Consumer<MethodVisitor> consumer : methodVisitors) {
                consumer.accept(methodVisitor);
            }
        }
    }

    public static final class CodeContextASM extends CodeContext<CodeContextASM> {
        private final List<Consumer<MethodVisitor>> codeVisitors = new ArrayList<>();

        private CodeContextASM() {}

        public CodeContextASM asm(Consumer<MethodVisitor> consumer) {
            codeVisitors.add(consumer);
            return this;
        }

        public CodeContextASM instruction(int opcode) {
            codeVisitors.add(mv -> mv.visitInsn(opcode));
            return this;
        }

        @Override
        public CodeContextASM constant(ConstantDesc constant) {
            codeVisitors.add(mv -> {
                switch (constant) {
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
                    default -> mv.visitLdcInsn(ConstantsASM.toAsm(constant));
                }
            });
            return this;
        }

        @Override
        public CodeContextASM load(ClassDesc descriptor, int index) {
            codeVisitors.add(mv -> mv.visitVarInsn(ConstantsASM.toAsm(descriptor).getOpcode(Opcodes.ILOAD), index));
            return this;
        }

        @Override
        public CodeContextASM store(ClassDesc descriptor, int index) {
            codeVisitors.add(mv -> mv.visitVarInsn(ConstantsASM.toAsm(descriptor).getOpcode(Opcodes.ISTORE), index));
            return this;
        }

        @Override
        public CodeContextASM newArray(ClassDesc descriptor) {
            codeVisitors.add(mv -> {
                if (ConstantsASM.toAsm(descriptor).getSort() <= Type.DOUBLE) {
                    // primitive type
                    mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN + ConstantsASM.toAsm(descriptor).getSort() - Type.BOOLEAN);
                } else {
                    mv.visitTypeInsn(Opcodes.ANEWARRAY, ConstantsASM.toAsm(descriptor).getInternalName());
                }
            });
            return this;
        }

        @Override
        public CodeContextASM instanceOf(ClassDesc descriptor) {
            codeVisitors.add(mv -> mv.visitTypeInsn(Opcodes.INSTANCEOF, ConstantsASM.toAsm(descriptor).getInternalName()));
            return this;
        }

        @Override
        public CodeContextASM checkCast(ClassDesc descriptor) {
            codeVisitors.add(mv -> mv.visitTypeInsn(Opcodes.CHECKCAST, ConstantsASM.toAsm(descriptor).getInternalName()));
            return this;
        }

        @Override
        public CodeContextASM returnValue(ClassDesc descriptor) {
            if (descriptor.descriptorString().equals("V")) {
                codeVisitors.add(mv -> mv.visitInsn(Opcodes.RETURN));
                return this;
            }
            codeVisitors.add(mv -> mv.visitInsn(ConstantsASM.toAsm(descriptor).getOpcode(Opcodes.IRETURN)));
            return this;
        }

        @Override
        public CodeContextASM field(DirectMethodHandleDesc.Kind operation, ClassDesc owner, String name, ClassDesc descriptor) {
            codeVisitors.add(mv -> mv.visitFieldInsn(switch (operation) {
                case STATIC_GETTER -> Opcodes.GETSTATIC;
                case STATIC_SETTER -> Opcodes.PUTSTATIC;
                case GETTER -> Opcodes.GETFIELD;
                case SETTER -> Opcodes.PUTFIELD;
                default -> throw new IllegalArgumentException("Invalid field operation: " + operation);
            }, ConstantsASM.toAsm(owner).getInternalName(), name, descriptor.descriptorString()));
            return this;
        }

        @Override
        public CodeContextASM method(DirectMethodHandleDesc.Kind operation, ClassDesc owner, String name, MethodTypeDesc descriptor) {
            if (operation == DirectMethodHandleDesc.Kind.CONSTRUCTOR) {
                if (!"<init>".equals(name)) {
                    throw new IllegalArgumentException("CONSTRUCTOR must be used with <init> method");
                }
                return newInstance(owner, descriptor);
            }
            codeVisitors.add(mv -> mv.visitMethodInsn(switch (operation) {
                case STATIC, INTERFACE_STATIC -> Opcodes.INVOKESTATIC;
                case VIRTUAL -> Opcodes.INVOKEVIRTUAL;
                case INTERFACE_VIRTUAL -> Opcodes.INVOKEINTERFACE;
                case SPECIAL, INTERFACE_SPECIAL -> Opcodes.INVOKESPECIAL;
                default -> throw new IllegalArgumentException("Invalid method operation: " + operation);
            }, ConstantsASM.toAsm(owner).getInternalName(), name, descriptor.descriptorString(), operation.isInterface));
            return this;
        }

        @Override
        public CodeContextASM newInstance(ClassDesc owner, MethodTypeDesc constructorDescriptor) {
            codeVisitors.add(mv -> {
                mv.visitTypeInsn(Opcodes.NEW, ConstantsASM.toAsm(owner).getInternalName());
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ConstantsASM.toAsm(owner).getInternalName(), "<init>", constructorDescriptor.descriptorString(), false);
            });
            return this;
        }

        @Override
        public CodeContextASM invokeDynamic(String name, MethodTypeDesc descriptor, DirectMethodHandleDesc bootstrap, Collection<ConstantDesc> bootstrapArguments) {
            codeVisitors.add(mv -> mv.visitInvokeDynamicInsn(name, descriptor.descriptorString(), ConstantsASM.toAsm(bootstrap), bootstrapArguments.stream().map(ConstantsASM::toAsm).toArray()));
            return this;
        }

        public CodeContextASM jump(int instruction, Consumer<? super CodeContextASM> skip) {
            codeVisitors.add(mv -> {
                Label label = new Label();
                mv.visitJumpInsn(instruction, label);

                CodeContextASM skipContext = new CodeContextASM();
                skip.accept(skipContext);
                skipContext.apply(mv);

                mv.visitLabel(label);
            });
            return this;
        }

        @Override
        public CodeContextASM loadThis() {
            codeVisitors.add(mv -> mv.visitVarInsn(Opcodes.ALOAD, 0));
            return this;
        }

        public void apply(MethodVisitor methodVisitor) {
            for (Consumer<MethodVisitor> consumer : codeVisitors) {
                consumer.accept(methodVisitor);
            }
        }
    }

    public static final class ConstantsASM {
        private ConstantsASM() {}

        public static ConstantDesc from(Type type) {
            if (type.getSort() == Type.METHOD) {
                return MethodTypeDesc.ofDescriptor(type.getDescriptor());
            }
            return fromClass(type);
        }

        public static ClassDesc fromClass(Type type) {
            if (type.getSort() == Type.METHOD) {
                throw new IllegalArgumentException("Method types cannot be converted to ClassDesc");
            }
            return ClassDesc.ofDescriptor(type.getDescriptor());
        }

        public static DirectMethodHandleDesc from(Handle handle) {
            return MethodHandleDesc.of(
                DirectMethodHandleDesc.Kind.valueOf(handle.getTag(), handle.isInterface()),
                fromClass(Type.getObjectType(handle.getOwner())),
                handle.getName(),
                handle.getDesc()
            );
        }

        public static DynamicConstantDesc<?> from(ConstantDynamic constantDynamic) {
            var args = new ConstantDesc[constantDynamic.getBootstrapMethodArgumentCount()];
            for (int i = 0; i < args.length; i++) {
                var arg = constantDynamic.getBootstrapMethodArgument(i);
                args[i] = switch (arg) {
                    case Type type -> from(type);
                    case Handle handle -> from(handle);
                    case ConstantDynamic nested -> from(nested);
                    case ConstantDesc desc -> desc;
                    default -> throw new IllegalArgumentException("Unexpected bootstrap method argument type: " + arg.getClass());
                };
            }
            return DynamicConstantDesc.ofNamed(
                from(constantDynamic.getBootstrapMethod()),
                constantDynamic.getName(),
                fromClass(Type.getType(constantDynamic.getDescriptor())),
                args
            );
        }

        public static Type toAsm(ClassDesc classDesc) {
            return Type.getType(classDesc.descriptorString());
        }

        public static Type toAsm(MethodTypeDesc methodTypeDesc) {
            return Type.getMethodType(methodTypeDesc.descriptorString());
        }

        public static Handle toAsm(DirectMethodHandleDesc methodHandleDesc) {
            return new Handle(
                methodHandleDesc.kind().refKind,
                Type.getType(methodHandleDesc.owner().descriptorString()).getInternalName(),
                methodHandleDesc.methodName(),
                methodHandleDesc.lookupDescriptor(),
                methodHandleDesc.kind().isInterface
            );
        }

        public static ConstantDynamic toAsm(DynamicConstantDesc<?> dynamicConstantDesc) {
            Object[] args = dynamicConstantDesc.bootstrapArgsList().stream()
                .map(ConstantsASM::toAsm)
                .toArray(Object[]::new);
            return new ConstantDynamic(
                dynamicConstantDesc.constantName(),
                dynamicConstantDesc.constantType().descriptorString(),
                toAsm(dynamicConstantDesc.bootstrapMethod()),
                args
            );
        }

        public static Object toAsm(ConstantDesc constantDesc) {
            return switch (constantDesc) {
                case DynamicConstantDesc<?> dynamicConstantDesc -> toAsm(dynamicConstantDesc);
                case MethodTypeDesc methodTypeDesc -> toAsm(methodTypeDesc);
                case DirectMethodHandleDesc methodHandleDesc -> toAsm(methodHandleDesc);
                case ClassDesc classDesc -> toAsm(classDesc);
                default -> constantDesc;
            };
        }
    }
}
