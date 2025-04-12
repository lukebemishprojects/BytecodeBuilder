package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MethodContext {
    private final List<Consumer<MethodVisitor>> methodVisitors = new ArrayList<>();
    
    private MethodContext() {}
    
    public MethodContext asm(Consumer<MethodVisitor> consumer) {
        methodVisitors.add(consumer);
        return this;
    }

    public MethodContext code(Consumer<CodeContext> consumer, int maxStack, int maxLocal) {
        var codeContext = CodeContext.create(consumer);
        methodVisitors.add(mv -> {
            mv.visitCode();
            codeContext.apply(mv);
            mv.visitMaxs(maxStack, maxLocal);
        });
        return this;
    }

    public MethodContext code(Consumer<CodeContext> consumer) {
        return code(consumer, 0, 0);
    }

    public void apply(MethodVisitor methodVisitor) {
        for (Consumer<MethodVisitor> consumer : methodVisitors) {
            consumer.accept(methodVisitor);
        }
    }
    
    public static MethodContext create(Consumer<MethodContext> consumer) {
        var methodContext = new MethodContext();
        consumer.accept(methodContext);
        return methodContext;
    }
    
    public static MethodContext create() {
        return new MethodContext();
    }
}
