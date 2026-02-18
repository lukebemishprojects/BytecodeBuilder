package dev.lukebemish.bytecodebuilder;

import org.jspecify.annotations.Nullable;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.FieldBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.instruction.OperatorInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class BackendClassFile {

    public static ClassContextClassFile createClass(Consumer<? super ClassContextClassFile> consumer) {
        var context = new ClassContextClassFile();
        consumer.accept(context);
        return context;
    }

    public static ClassContextClassFile createClass() {
        return new ClassContextClassFile();
    }

    public static MethodContextClassFile createMethod(Consumer<? super MethodContextClassFile> consumer) {
        var context = new MethodContextClassFile();
        consumer.accept(context);
        return context;
    }

    public static MethodContextClassFile createMethod() {
        return new MethodContextClassFile();
    }

    public static FieldContextClassFile createField(Consumer<? super FieldContextClassFile> consumer) {
        var context = new FieldContextClassFile();
        consumer.accept(context);
        return context;
    }

    public static FieldContextClassFile createField() {
        return new FieldContextClassFile();
    }

    public static CodeContextClassFile createCode(Consumer<? super CodeContextClassFile> consumer) {
        var context = new CodeContextClassFile();
        consumer.accept(context);
        return context;
    }

    public static CodeContextClassFile createCode() {
        return new CodeContextClassFile();
    }

    public static final class ClassContextClassFile extends ImplClassContext<ClassContextClassFile, FieldContextClassFile, MethodContextClassFile> {
        private final List<Consumer<ClassBuilder>> classVisitors = new ArrayList<>();

        private ClassContextClassFile() {}

        @Override
        public ClassContextClassFile constructor(int access, MethodTypeDesc descriptor, @Nullable Collection<ClassDesc> exceptions, Consumer<? super MethodContextClassFile> remainder) {
            return method("<init>", access, descriptor, null, exceptions, remainder);
        }

        @Override
        public ClassContextClassFile method(String name, int access, MethodTypeDesc descriptor, @Nullable MethodSignature signature, @Nullable Collection<ClassDesc> exceptions, Consumer<? super MethodContextClassFile> remainder) {
            var methodContext = createMethod(remainder);
            classVisitors.add(b -> b.withMethod(name, descriptor, access, m -> {
                if (signature != null) {
                    m.with(SignatureAttribute.of(java.lang.classfile.Signature.parseFrom(signature.signature())));
                }
                if (exceptions != null) {
                    m.with(ExceptionsAttribute.ofSymbols(exceptions.stream().toList()));
                }
                methodContext.apply(m);
            }));
            return this;
        }

        @Override
        public ClassContextClassFile field(String name, int access, ClassDesc descriptor, @Nullable Signature signature, @Nullable ConstantDesc constant, Consumer<? super FieldContextClassFile> remainder) {
            var fieldContext = createField(remainder);
            classVisitors.add(b -> b.withField(name, descriptor, f -> {
                f.withFlags(access);
                if (signature != null) {
                    f.with(SignatureAttribute.of(java.lang.classfile.Signature.parseFrom(signature.signature())));
                }
                if (constant != null) {
                    f.with(ConstantValueAttribute.of(constant));
                }
                fieldContext.apply(f);
            }));
            return this;
        }

        @Override
        public byte[] build(int version, int access, ClassDesc name, ClassDesc superName, @Nullable Collection<ClassDesc> interfaces, @Nullable ClassSignature signature) {
            var classFile = ClassFile.of();
            return classFile.build(name, cl -> {
                cl.withVersion(version, 0);
                cl.withSuperclass(superName);
                if (interfaces != null) {
                    cl.withInterfaceSymbols(interfaces.stream().toList());
                }
                if (signature != null) {
                    cl.with(SignatureAttribute.of(java.lang.classfile.Signature.parseFrom(signature.signature())));
                }
                apply(cl);
            });
        }

        public ClassContextClassFile builder(Consumer<ClassBuilder> visitor) {
            classVisitors.add(visitor);
            return this;
        }

        public void apply(ClassBuilder classBuilder) {
            for (var visitor : classVisitors) {
                visitor.accept(classBuilder);
            }
        }
    }

    public static final class MethodContextClassFile extends ImplMethodContext<MethodContextClassFile, CodeContextClassFile> {
        private final List<Consumer<MethodBuilder>> methodVisitors = new ArrayList<>();

        private MethodContextClassFile() {}

        public MethodContextClassFile builder(Consumer<MethodBuilder> visitor) {
            methodVisitors.add(visitor);
            return this;
        }

        public void apply(MethodBuilder methodBuilder) {
            for (var visitor : methodVisitors) {
                visitor.accept(methodBuilder);
            }
        }

        @Override
        public MethodContextClassFile code(Consumer<? super CodeContextClassFile> consumer) {
            var codeContext = createCode(consumer);
            methodVisitors.add(b -> b.withCode(codeContext::apply));
            return this;
        }
    }

    public static final class FieldContextClassFile extends ImplFieldContext<FieldContextClassFile> {
        private final List<Consumer<FieldBuilder>> fieldVisitors = new ArrayList<>();

        private FieldContextClassFile() {}

        public FieldContextClassFile builder(Consumer<FieldBuilder> visitor) {
            fieldVisitors.add(visitor);
            return this;
        }

        public void apply(FieldBuilder fieldBuilder) {
            for (var visitor : fieldVisitors) {
                visitor.accept(fieldBuilder);
            }
        }
    }

    public static final class CodeContextClassFile extends ImplCodeContext<CodeContextClassFile> {
        private final List<Consumer<CodeBuilder>> codeVisitors = new ArrayList<>();

        private CodeContextClassFile() {}

        public CodeContextClassFile builder(Consumer<CodeBuilder> visitor) {
            codeVisitors.add(visitor);
            return this;
        }

        public void apply(CodeBuilder codeBuilder) {
            for (var visitor : codeVisitors) {
                visitor.accept(codeBuilder);
            }
        }

        public CodeContextClassFile instruction(Opcode opcode) {
            codeVisitors.add(b -> b.accept(OperatorInstruction.of(opcode)));
            return this;
        }

        @Override
        public CodeContextClassFile constant(ConstantDesc constant) {
            codeVisitors.add(b -> b.loadConstant(constant));
            return this;
        }

        @Override
        public CodeContextClassFile load(ClassDesc descriptor, int index) {
            codeVisitors.add(b -> b.loadLocal(
                TypeKind.from(descriptor), index
            ));
            return this;
        }

        @Override
        public CodeContextClassFile store(ClassDesc descriptor, int index) {
            codeVisitors.add(b -> b.storeLocal(
                TypeKind.from(descriptor), index
            ));
            return this;
        }

        @Override
        public CodeContextClassFile newArray(ClassDesc descriptor) {
            codeVisitors.add(b -> {
                if (descriptor.isPrimitive()) {
                    b.newarray(TypeKind.from(descriptor));
                } else {
                    b.anewarray(descriptor);
                }
            });
            return this;
        }

        @Override
        public CodeContextClassFile instanceOf(ClassDesc descriptor) {
            codeVisitors.add(b -> b.instanceOf(descriptor));
            return this;
        }

        @Override
        public CodeContextClassFile checkCast(ClassDesc descriptor) {
            codeVisitors.add(b -> b.checkcast(descriptor));
            return this;
        }

        @Override
        public CodeContextClassFile returnValue(ClassDesc descriptor) {
            codeVisitors.add(b -> b.return_(TypeKind.from(descriptor)));
            return this;
        }

        @Override
        public CodeContextClassFile field(DirectMethodHandleDesc.Kind operation, ClassDesc owner, String name, ClassDesc descriptor) {
            codeVisitors.add(b -> b.fieldAccess(switch (operation) {
                case STATIC_GETTER -> Opcode.GETSTATIC;
                case STATIC_SETTER -> Opcode.PUTSTATIC;
                case GETTER -> Opcode.GETFIELD;
                case SETTER -> Opcode.PUTFIELD;
                default -> throw new IllegalArgumentException("Invalid field operation: " + operation);
            }, owner, name, descriptor));
            return this;
        }

        @Override
        public CodeContextClassFile method(DirectMethodHandleDesc.Kind operation, ClassDesc owner, String name, MethodTypeDesc descriptor) {
            if (operation == DirectMethodHandleDesc.Kind.CONSTRUCTOR) {
                if (!"<init>".equals(name)) {
                    throw new IllegalArgumentException("CONSTRUCTOR must be used with <init> method");
                }
                return newInstance(owner, descriptor);
            }
            codeVisitors.add(b -> b.invoke(switch (operation) {
                case STATIC, INTERFACE_STATIC -> Opcode.INVOKESTATIC;
                case VIRTUAL -> Opcode.INVOKEVIRTUAL;
                case INTERFACE_VIRTUAL -> Opcode.INVOKEINTERFACE;
                case SPECIAL, INTERFACE_SPECIAL -> Opcode.INVOKESPECIAL;
                default -> throw new IllegalArgumentException("Invalid method operation: " + operation);
            }, owner, name, descriptor, operation.isInterface));
            return this;
        }

        @Override
        public CodeContextClassFile newInstance(ClassDesc owner, MethodTypeDesc constructorDescriptor) {
            codeVisitors.add(b -> {
                b.new_(owner);
                b.dup();
                b.invoke(Opcode.INVOKESPECIAL, owner, "<init>", constructorDescriptor, false);
            });
            return this;
        }

        @Override
        public CodeContextClassFile invokeDynamic(String name, MethodTypeDesc descriptor, DirectMethodHandleDesc bootstrap, Collection<ConstantDesc> bootstrapArguments) {
            codeVisitors.add(b -> {
                var args = bootstrapArguments.toArray(ConstantDesc[]::new);
                b.invokedynamic(DynamicCallSiteDesc.of(
                    bootstrap, name, descriptor, args
                ));
            });
            return this;
        }

        public CodeContextClassFile jump(Opcode instruction, Consumer<? super CodeContextClassFile> skip) {
            codeVisitors.add(b -> {
                Label label = b.newLabel();
                b.branch(instruction, label);

                BackendClassFile.CodeContextClassFile skipContext = new BackendClassFile.CodeContextClassFile();
                skip.accept(skipContext);
                skipContext.apply(b);

                b.labelBinding(label);
            });
            return this;
        }

        @Override
        public CodeContextClassFile loadThis() {
            codeVisitors.add(b -> b.loadLocal(TypeKind.REFERENCE, 0));
            return this;
        }
    }
}
