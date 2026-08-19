package de.keksuccino.konkrete.mixin.support.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Enumerates vanilla option instances without exposing the private {@code Options.FieldAccess} type.
 *
 * Loader access wideners cannot make that type visible to the standalone common Java compilation.
 * Runtime discovery deliberately relies on stable Java types and method shape rather than mapped names,
 * so the bridge also works after Fabric remaps Minecraft symbols.
 */
public final class OptionsFieldAccessBridge {

    private OptionsFieldAccessBridge() {
    }

    public static void collect(@NotNull Options options, @NotNull BiConsumer<String, OptionInstance<?>> collector) {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(collector, "collector");
        Method processOptions = findProcessOptionsMethod();
        Class<?> fieldAccessType = processOptions.getParameterTypes()[0];
        Object fieldAccess = Proxy.newProxyInstance(Options.class.getClassLoader(), new Class<?>[]{fieldAccessType}, (proxy, method, arguments) -> invokeFieldAccess(proxy, method, arguments, collector));
        try {
            processOptions.setAccessible(true);
            processOptions.invoke(options, fieldAccess);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Vanilla option enumeration failed", cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not invoke vanilla option enumeration", exception);
        }
    }

    private static Method findProcessOptionsMethod() {
        return Arrays.stream(Options.class.getDeclaredMethods()).filter(method -> method.getReturnType() == void.class && method.getParameterCount() == 1 && isFieldAccessType(method.getParameterTypes()[0])).findFirst().orElseThrow(() -> new IllegalStateException("Could not locate vanilla option enumeration method"));
    }

    private static boolean isFieldAccessType(Class<?> type) {
        if (!type.isInterface()) return false;
        return Arrays.stream(type.getMethods()).anyMatch(method -> method.getReturnType() == void.class && method.getParameterCount() == 2 && method.getParameterTypes()[0] == String.class && method.getParameterTypes()[1] == OptionInstance.class);
    }

    private static Object invokeFieldAccess(Object proxy, Method method, Object[] arguments, BiConsumer<String, OptionInstance<?>> collector) {
        if (method.getDeclaringClass() == Object.class) return invokeObjectMethod(proxy, method, arguments);
        if (arguments != null && arguments.length == 2 && arguments[0] instanceof String name && arguments[1] instanceof OptionInstance<?> option) {
            collector.accept(name, option);
            return null;
        }
        if (arguments != null && arguments.length >= 2) return arguments[1];
        throw new IllegalStateException("Unexpected vanilla option field-access method shape: " + method);
    }

    private static Object invokeObjectMethod(Object proxy, Method method, Object[] arguments) {
        return switch (method.getName()) {
            case "toString" -> "KonkreteOptionsFieldAccess";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (arguments == null ? null : arguments[0]);
            default -> throw new IllegalStateException("Unexpected Object method: " + method);
        };
    }

}
