package dev.lukebemish.bytecodebuilder;

import java.lang.constant.ClassDesc;
import java.lang.constant.DynamicConstantDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ClassDataTracker {
    sealed interface DataValue {
        Object resolve();

        record Direct(Object object) implements DataValue {
            @Override
            public Object resolve() {
                return object;
            }
        }
        record Lazy(Supplier<Object> object) implements DataValue {
            @Override
            public Object resolve() {
                return object.get();
            }
        }
    }

    final List<DataValue> data = new ArrayList<>();

    ClassDataTracker() {}

    public DynamicConstantDesc<?> dataConstant(ClassDesc descriptor, Object value) {
        var idx = data.size();
        data.add(new DataValue.Direct(value));
        return Constants.classDataAt(descriptor, idx);
    }

    public DynamicConstantDesc<?> delayedDataConstant(ClassDesc descriptor, Supplier<Object> value) {
        var idx = data.size();
        data.add(new DataValue.Lazy(value));
        return Constants.classDataAt(descriptor, idx);
    }
}
