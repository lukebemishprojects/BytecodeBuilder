package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.Opcodes;

public enum MethodOperation {
    INVOKEINTERFACE(Opcodes.INVOKEINTERFACE, Opcodes.H_INVOKEINTERFACE),
    INVOKEVIRTUAL(Opcodes.INVOKEVIRTUAL, Opcodes.H_INVOKEVIRTUAL),
    INVOKESPECIAL(Opcodes.INVOKESPECIAL, Opcodes.H_INVOKESPECIAL),
    INVOKESTATIC(Opcodes.INVOKESTATIC, Opcodes.H_INVOKESTATIC),
    NEWINVOKESPECIAL(-1, Opcodes.H_NEWINVOKESPECIAL);
    
    final int opcode;
    final int hOpcode;

    MethodOperation(int opcode, int hOpcode) {
        this.opcode = opcode;
        this.hOpcode = hOpcode;
    }
}
