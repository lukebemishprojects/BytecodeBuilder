package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.Handle;

public abstract sealed class ConstantHandle {
    public static final class ConstantMethodHandle extends ConstantHandle {
        private final MethodOperation operation;
        private final Descriptor owner;
        private final String name;
        private final Descriptor descriptor;
        private final boolean isInterface;

        private ConstantMethodHandle(MethodOperation operation, Descriptor owner, String name, Descriptor descriptor, boolean isInterface) {
            this.operation = operation;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
            this.isInterface = isInterface;
        }

        @Override
        public Handle asmHandle() {
            return new Handle(operation.hOpcode, owner.internalName(), name, descriptor.descriptor(), isInterface);
        }
    }
    
    public static final class ConstantFieldHandle extends ConstantHandle {
        private final FieldOperation operation;
        private final Descriptor owner;
        private final String name;
        private final Descriptor descriptor;

        private ConstantFieldHandle(FieldOperation operation, Descriptor owner, String name, Descriptor descriptor) {
            this.operation = operation;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public Handle asmHandle() {
            return new Handle(operation.hOpcode, owner.internalName(), name, descriptor.descriptor(), false);
        }
    }
    
    public abstract Handle asmHandle();
    
    public static ConstantMethodHandle of(MethodOperation operation, Descriptor owner, String name, Descriptor descriptor, boolean isInterface) {
        return new ConstantMethodHandle(operation, owner, name, descriptor, isInterface);
    }
    
    public static ConstantFieldHandle of(FieldOperation operation, Descriptor owner, String name, Descriptor descriptor) {
        return new ConstantFieldHandle(operation, owner, name, descriptor);
    }
}
