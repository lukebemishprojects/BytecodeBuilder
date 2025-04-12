package dev.lukebemish.bytecodebuilder;

import java.util.Collection;

public final class MethodSignature {
    private final String signature;
    
    private MethodSignature(String signature) {
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

        public MethodSignature build(Signature returnType, Signature... parameters) {
            var full = new StringBuilder();
            if (!signature.isEmpty()) {
                full.append('<').append(signature).append('>');
            }
            full.append('(');
            for (Signature parameter : parameters) {
                full.append(parameter.signature());
            }
            full.append(')');
            full.append(returnType.signature());
            return new MethodSignature(full.toString());
        }



        public MethodSignature build(Signature returnType, Collection<Signature> throwsSignatures, Signature... parameters) {
            var full = new StringBuilder();
            if (!signature.isEmpty()) {
                full.append('<').append(signature).append('>');
            }
            full.append('(');
            for (Signature parameter : parameters) {
                full.append(parameter.signature());
            }
            full.append(')');
            full.append(returnType.signature());
            for (Signature throwsSignature : throwsSignatures) {
                full.append('^').append(throwsSignature.signature());
            }
            return new MethodSignature(full.toString());
        }
    }
    
    public Builder create() {
        return new Builder();
    }
}
