package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract sealed class Signature {
    private static final class SimpleSignature extends Signature {
        private final String signature;

        public SimpleSignature(String signature) {
            this.signature = signature;
        }

        public String signature() {
            return signature;
        }
    }
    
    private static sealed abstract class ClassSignatureLike extends Signature {
        abstract protected String partial();

        @Override
        public String signature() {
            return "L" + partial() + ";";
        }

        @Override
        public Signature inner(String name, Collection<TypeArgument> typeArguments) {
            return new InnerClassSignature(name, this, Optional.of(typeArguments.stream().toList()));
        }

        @Override
        public Signature inner(String name) {
            return new InnerClassSignature(name, this, Optional.empty());
        }
    }
    
    private static final class ClassSignature extends ClassSignatureLike {
        private final String classInternalName;
        private final Optional<List<TypeArgument>> typeArguments;

        private ClassSignature(String classInternalName, Optional<List<TypeArgument>> typeArguments) {
            this.classInternalName = classInternalName;
            this.typeArguments = typeArguments;
        }

        @Override
        public String partial() {
            return classInternalName + typeArguments.map(l -> "<"+l.stream().map(arg -> arg.typeArgument).collect(Collectors.joining())+">").orElse("");
        }
    }
    
    private static final class InnerClassSignature extends ClassSignatureLike {
        private final String innerName;
        private final ClassSignatureLike parent;
        private final Optional<List<TypeArgument>> typeArguments;

        private InnerClassSignature(String innerName, ClassSignatureLike parent, Optional<List<TypeArgument>> typeArguments) {
            this.innerName = innerName;
            this.parent = parent;
            this.typeArguments = typeArguments;
        }

        @Override
        public String partial() {
            return parent.partial() + "." + innerName + typeArguments.map(l -> "<"+l.stream().map(arg -> arg.typeArgument).collect(Collectors.joining())+">").orElse("");
        }
    }

    public Signature inner(String name) {
        throw new UnsupportedOperationException("Cannot make an inner class signature of "+signature());
    }
    public Signature inner(String name, TypeArgument... typeArguments) {
        return inner(name, List.of(typeArguments));
    }
    public Signature inner(String name, Collection<TypeArgument> typeArguments) {
        throw new UnsupportedOperationException("Cannot make an inner class signature of "+signature());
    }
    public Signature array() {
        return new SimpleSignature("["+this.signature());
    }
    
    abstract public String signature();

    public static Signature classType(String name) {
        return new ClassSignature(name, Optional.empty());
    }

    public static Signature classType(String name, TypeArgument... typeArguments) {
        return classType(name, List.of(typeArguments));
    }

    public static Signature classType(String name, Collection<TypeArgument> typeArguments) {
        return new ClassSignature(name, Optional.of(typeArguments.stream().toList()));
    }

    public static Signature classType(Type type) {
        if (type.getSort() <= 8 || type.getSort() == Type.ARRAY) { // Type.DOUBLE
            return new SimpleSignature(type.getDescriptor());
        }
        
        if (type.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException("Type "+type+" is not an object type");
        }
        var name = type.getInternalName();
        return new ClassSignature(name, Optional.empty());
    }

    public static Signature classType(Type type, TypeArgument... typeArguments) {
        return classType(type, List.of(typeArguments));
    }

    public static Signature classType(Type type, Collection<TypeArgument> typeArguments) {
        if (type.getSort() <= 8) { // Type.DOUBLE
            return new SimpleSignature(type.getDescriptor());
        }
        
        if (type.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException("Type "+type+" is not an object type");
        }
        var name = type.getInternalName();
        return new ClassSignature(name, Optional.of(typeArguments.stream().toList()));
    }

    public static Signature classType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return new SimpleSignature(clazz.descriptorString());
        }
        
        var type = Type.getType(clazz);
        if (type.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException("Type "+type+" is not an object type");
        }
        var name = type.getInternalName();
        return new ClassSignature(name, Optional.empty());
    }

    public static Signature classType(Class<?> clazz, TypeArgument... typeArguments) {
        return classType(clazz, List.of(typeArguments));
    }

    public static Signature classType(Class<?> clazz, Collection<TypeArgument> typeArguments) {
        if (clazz.isPrimitive()) {
            return new SimpleSignature(clazz.descriptorString());
        }
        
        var type = Type.getType(clazz);
        if (type.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException("Type "+type+" is not an object type");
        }
        var name = type.getInternalName();
        return new ClassSignature(name, Optional.of(typeArguments.stream().toList()));
    }

    public static Signature typeVariable(String name) {
        return new SimpleSignature("T"+name+";");
    }
    
    public static final class TypeArgument {
        private final String typeArgument;

        private TypeArgument(String typeArgument) {
            this.typeArgument = typeArgument;
        }
    }
    
    public static TypeArgument wildcard() {
        return new TypeArgument("*");
    }
    
    public static TypeArgument extendsBound(Signature signature) {
        return new TypeArgument("+"+signature.signature());
    }
    
    public static TypeArgument superBound(Signature signature) {
        return new TypeArgument("-"+signature.signature());
    }
}
