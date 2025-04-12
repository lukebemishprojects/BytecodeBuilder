package dev.lukebemish.bytecodebuilder;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ClassContext {
    private final List<Consumer<ClassVisitor>> classVisitors = new ArrayList<>();
    
    private ClassContext() {}
    
    public ClassContext asm(Consumer<ClassVisitor> consumer) {
        classVisitors.add(consumer);
        return this;
    }
    
    public ClassContext constructor(int access, Descriptor descriptor, @Nullable Collection<Descriptor> exceptions, Consumer<MethodContext> remainder) {
        return method("<init>", access, descriptor, null, exceptions, remainder);
    }
    
    public ClassContext method(String name, int access, Descriptor descriptor, @Nullable MethodSignature signature, @Nullable Collection<Descriptor> exceptions, Consumer<MethodContext> remainder) {
        var exceptionsNames = new String[exceptions == null ? 0 : exceptions.size()];
        if (exceptions != null) {
            int i = 0;
            for (var exception : exceptions) {
                exceptionsNames[i++] = exception.internalName();
            }
        }
        
        var methodContext = MethodContext.create(remainder);
        
        classVisitors.add(cv -> {
            var mv = cv.visitMethod(access, name, descriptor.descriptor(), signature == null ? null : signature.signature(), exceptionsNames);
            methodContext.apply(mv);
            mv.visitEnd();
        });
        return this;
    }
    
    public ClassContext field(String name, int access, Descriptor descriptor, @Nullable Signature signature, @Nullable Constant constant, Consumer<FieldContext> remainder) {
        final Object constValue;
        if (constant != null) {
            if ((access & Opcodes.ACC_STATIC) == 0 || (access & Opcodes.ACC_FINAL) == 0) {
                throw new IllegalArgumentException("Constant value can only be set for static final fields");
            }
            constValue = constant.value();
            switch (constValue) {
                case Integer ignored -> {}
                case Long ignored -> {}
                case Float ignored -> {}
                case Double ignored -> {}
                case String ignored -> {}
                default -> throw new IllegalArgumentException("Constant value must be a primitive or string for a field initializer, but was "+constant.value());
            }
        } else {
            constValue = null;
        }
        
        var fieldContext = FieldContext.create(remainder);
        
        classVisitors.add(cv -> {
            var fv = cv.visitField(access, name, descriptor.descriptor(), signature == null ? null : signature.signature(), constValue);
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

    public byte[] build(int flags, int version, int access, Descriptor name, Descriptor superName, @Nullable Collection<Descriptor> interfaces, @Nullable ClassSignature signature) {
        var cv = new ClassWriter(flags);
        cv.visit(version, access, name.internalName(), signature == null ? null : signature.signature(), superName.internalName(), interfaces == null ? null : interfaces.stream().map(Descriptor::internalName).toArray(String[]::new));
        apply(cv);
        cv.visitEnd();
        return cv.toByteArray();
    }
    
    public static ClassContext create(Consumer<ClassContext> consumer) {
        var context = new ClassContext();
        consumer.accept(context);
        return context;
    }
    
    public static ClassContext create() {
        return new ClassContext();
    }

    public static MethodHandles.Lookup hidden(MethodHandles.Lookup lookup, boolean initialize, Set<MethodHandles.Lookup.ClassOption> options, int flags, int version, int access, Descriptor name, Descriptor superName, @Nullable Collection<Descriptor> interfaces, @Nullable ClassSignature signature, BiConsumer<ClassContext, ClassDataTracker> consumer) throws IllegalAccessException {
        var context = new ClassContext();
        var tracker = new ClassDataTracker();
        consumer.accept(context, tracker);
        
        var bytes = context.build(flags, version, access, name, superName, interfaces, signature);
        
        if (tracker.data.isEmpty()) {
            return lookup.defineHiddenClass(bytes, initialize, options.toArray(MethodHandles.Lookup.ClassOption[]::new));
        } else {
            return lookup.defineHiddenClassWithClassData(bytes, List.copyOf(tracker.data), initialize, options.toArray(MethodHandles.Lookup.ClassOption[]::new));
        }
    }
}
