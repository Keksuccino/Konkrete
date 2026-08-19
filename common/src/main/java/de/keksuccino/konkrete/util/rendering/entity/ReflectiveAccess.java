package de.keksuccino.konkrete.util.rendering.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Centralizes reflective optional-dependency access and preserves the original failure when invoked code throws.
 */
final class ReflectiveAccess {

    private ReflectiveAccess() {
    }

    static Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        return Class.forName(className, false, classLoader);
    }

    static Constructor<?> requireConstructor(Class<?> type, Class<?>... parameterTypes) throws NoSuchMethodException {
        return type.getConstructor(parameterTypes);
    }

    static Method requireMethod(Class<?> type, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        return type.getMethod(methodName, parameterTypes);
    }

    static Object construct(Constructor<?> constructor, Object... arguments) {
        try {
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException ex) {
            throw propagateTargetFailure("constructor " + constructor.getDeclaringClass().getName(), ex.getCause());
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            throw new IllegalStateException("Failed to invoke constructor " + constructor.getDeclaringClass().getName(), ex);
        }
    }

    static Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException ex) {
            throw propagateTargetFailure("method " + method.getDeclaringClass().getName() + "." + method.getName(), ex.getCause());
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            throw new IllegalStateException("Failed to invoke method " + method.getDeclaringClass().getName() + "." + method.getName(), ex);
        }
    }

    private static RuntimeException propagateTargetFailure(String operation, Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) return runtimeException;
        if (failure instanceof Error error) throw error;
        return new IllegalStateException("Optional dependency " + operation + " failed", failure);
    }

}
