package dev.lukebemish.bytecodebuilder;

import java.lang.constant.ClassDesc;
import java.lang.constant.DynamicConstantDesc;
import java.util.ArrayList;
import java.util.List;

public final class ClassDataTracker {
    final List<Object> data = new ArrayList<>();

    ClassDataTracker() {}

    public DynamicConstantDesc<?> dataConstant(ClassDesc descriptor, Object value) {
        var idx = data.size();
        data.add(value);
        return Constants.classDataAt(descriptor, idx);
    }
}
