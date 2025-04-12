package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.FieldVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FieldContext {
    private final List<Consumer<FieldVisitor>> classVisitors = new ArrayList<>();
    
    private FieldContext() {}
    
    public FieldContext asm(Consumer<FieldVisitor> consumer) {
        classVisitors.add(consumer);
        return this;
    }
    
    public void apply(FieldVisitor fieldVisitor) {
        for (Consumer<FieldVisitor> consumer : classVisitors) {
            consumer.accept(fieldVisitor);
        }
    }
    
    public static FieldContext create(Consumer<FieldContext> consumer) {
        var fieldContext = new FieldContext();
        consumer.accept(fieldContext);
        return fieldContext;
    }
    
    public static FieldContext create() {
        return new FieldContext();
    }
}
