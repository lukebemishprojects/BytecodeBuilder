package dev.lukebemish.bytecodebuilder.test;

import dev.lukebemish.bytecodebuilder.FlexibleLambdaMetafactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

public class TestFlexibleLambdaMetafactory {
    // TODO: proper junit testing
    public static void main(String[] args) throws Throwable {
        var lookup = MethodHandles.lookup();
        var processString = lookup.findStatic(TestFlexibleLambdaMetafactory.class, "processString", MethodType.methodType(String.class, String.class));
        var printString = lookup.findStatic(TestFlexibleLambdaMetafactory.class, "printString", MethodType.methodType(void.class, String.class, String.class));
        
        var combined = MethodHandles.filterArguments(printString, 1, processString);

        @SuppressWarnings("unchecked") Consumer<String> consumer = (Consumer<String>) FlexibleLambdaMetafactory.metafactory(
                lookup,
                "accept",
                MethodType.methodType(Consumer.class, String.class),
                MethodType.methodType(void.class, Object.class),
                combined,
                MethodType.methodType(void.class, String.class)
        ).dynamicInvoker().invokeExact("prefix");
        consumer.accept("string");
    }
    
    private static String processString(String s) {
        return "processed: " + s;
    }
    
    private static void printString(String prefix, String s) {
        System.out.println(prefix + ": " + s);
    }
}
