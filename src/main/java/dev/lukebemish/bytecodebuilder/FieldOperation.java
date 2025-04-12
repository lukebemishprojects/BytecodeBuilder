package dev.lukebemish.bytecodebuilder;

import org.objectweb.asm.Opcodes;

public enum FieldOperation {
    GETSTATIC(Opcodes.GETSTATIC, Opcodes.H_GETSTATIC),
    PUTSTATIC(Opcodes.PUTSTATIC, Opcodes.H_PUTSTATIC),
    GETFIELD(Opcodes.GETFIELD, Opcodes.H_GETFIELD),
    PUTFIELD(Opcodes.PUTFIELD, Opcodes.H_PUTFIELD);
        
    
    final int opcode;
    final int hOpcode;

    FieldOperation(int opcode, int hOpcode) {
        this.opcode = opcode;
        this.hOpcode = hOpcode;
    }
}
