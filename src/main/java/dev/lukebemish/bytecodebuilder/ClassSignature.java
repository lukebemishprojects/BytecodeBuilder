package dev.lukebemish.bytecodebuilder;

public final class ClassSignature {
    private final String signature;

    private ClassSignature(String signature) {
        this.signature = signature;
    }

    public String signature() {
        return signature;
    }

    public static final class Builder {
        private final StringBuilder signature = new StringBuilder();

        public Builder typeParameter(String name, Signature classBound, Signature... interfaceBounds) {
            signature.append(name);
            signature.append(':').append(classBound.signature());
            for (Signature interfaceBound : interfaceBounds) {
                signature.append(':').append(interfaceBound.signature());
            }
            return this;
        }

        public ClassSignature build(Signature superClass, Signature... interfaces) {
            var full = new StringBuilder();
            if (!signature.isEmpty()) {
                full.append('<').append(signature).append('>');
            }
            signature.append(superClass.signature());
            for (Signature interfaceSignature : interfaces) {
                full.append(interfaceSignature.signature());
            }
            return new ClassSignature(full.toString());
        }
    }

    public Builder create() {
        return new Builder();
    }
}
