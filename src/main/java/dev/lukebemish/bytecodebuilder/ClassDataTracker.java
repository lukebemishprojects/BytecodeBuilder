package dev.lukebemish.bytecodebuilder;

import java.util.ArrayList;
import java.util.List;

public final class ClassDataTracker {
    final List<Object> data = new ArrayList<>();
    
    ClassDataTracker() {}
    
    public Constant dataConstant(Descriptor descriptor, Object value) {
        var idx = data.size();
        data.add(value);
        return Constant.classDataAt(descriptor, Constant.of(idx));
    }
}
