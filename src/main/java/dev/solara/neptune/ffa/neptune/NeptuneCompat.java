package dev.solara.neptune.ffa.neptune;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.arena.IArena;
import dev.lrxh.api.kit.IKit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class NeptuneCompat {
    private NeptuneCompat() {
    }

    public static List<IKit> allKits(NeptuneAPI neptune) {
        return invokeIterable(neptune.getKitService(), "getAllKits", IKit.class);
    }

    public static List<IArena> allArenas(NeptuneAPI neptune) {
        return invokeIterable(neptune.getArenaService(), "getAllArenas", IArena.class);
    }

    private static <T> List<T> invokeIterable(Object service, String methodName, Class<T> type) {
        List<T> result = new ArrayList<>();
        Object value = invoke(service, methodName);
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (type.isInstance(item)) {
                    result.add(type.cast(item));
                }
            }
        } else if (value != null && value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            for (Object item : array) {
                if (type.isInstance(item)) {
                    result.add(type.cast(item));
                }
            }
        } else if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (type.isInstance(item)) {
                    result.add(type.cast(item));
                }
            }
        }
        return result;
    }

    private static Object invoke(Object target, String methodName) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                try {
                    return method.invoke(target);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Could not call Neptune method " + methodName, exception);
                }
            }
        }
        throw new IllegalStateException("Neptune method not found: " + methodName);
    }
}
