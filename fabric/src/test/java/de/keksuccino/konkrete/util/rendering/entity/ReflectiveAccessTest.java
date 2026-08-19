package de.keksuccino.konkrete.util.rendering.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectiveAccessTest {

    @Test
    void constructsAndInvokesTheExactOverload() throws Exception {
        Class<?> targetClass = ReflectiveAccess.loadClass(FakeOptionalWidget.class.getName(), FakeOptionalWidget.class.getClassLoader());
        Constructor<?> constructor = ReflectiveAccess.requireConstructor(targetClass, int.class);
        Method stringMethod = ReflectiveAccess.requireMethod(targetClass, "describe", String.class);
        Method numberMethod = ReflectiveAccess.requireMethod(targetClass, "describe", int.class);

        Object target = ReflectiveAccess.construct(constructor, 7);

        assertEquals("prefix-7:value", ReflectiveAccess.invoke(stringMethod, target, "value"));
        assertEquals("prefix-7:12", ReflectiveAccess.invoke(numberMethod, target, 12));
    }

    @Test
    void rejectsMissingContractMembersDuringResolution() {
        assertThrows(ClassNotFoundException.class, () -> ReflectiveAccess.loadClass("example.missing.OptionalWidget", FakeOptionalWidget.class.getClassLoader()));
        assertThrows(NoSuchMethodException.class, () -> ReflectiveAccess.requireMethod(FakeOptionalWidget.class, "missing", boolean.class));
        assertThrows(NoSuchMethodException.class, () -> ReflectiveAccess.requireConstructor(FakeOptionalWidget.class, String.class));
    }

    @Test
    void preservesRuntimeFailuresThrownByOptionalCode() throws Exception {
        FakeOptionalWidget target = new FakeOptionalWidget(1);
        Method method = ReflectiveAccess.requireMethod(FakeOptionalWidget.class, "failRuntime");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> ReflectiveAccess.invoke(method, target));

        assertEquals("runtime failure", failure.getMessage());
    }

    @Test
    void preservesErrorsThrownByOptionalCode() throws Exception {
        FakeOptionalWidget target = new FakeOptionalWidget(1);
        Method method = ReflectiveAccess.requireMethod(FakeOptionalWidget.class, "failError");

        AssertionError failure = assertThrows(AssertionError.class, () -> ReflectiveAccess.invoke(method, target));

        assertEquals("error failure", failure.getMessage());
    }

    @Test
    void wrapsCheckedFailuresWithTheOriginalCause() throws Exception {
        FakeOptionalWidget target = new FakeOptionalWidget(1);
        Method method = ReflectiveAccess.requireMethod(FakeOptionalWidget.class, "failChecked");

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> ReflectiveAccess.invoke(method, target));

        assertInstanceOf(IOException.class, failure.getCause());
        assertEquals("checked failure", failure.getCause().getMessage());
    }

    @Test
    void constructorFailuresUseTheSamePropagationRules() throws Exception {
        Constructor<?> constructor = ReflectiveAccess.requireConstructor(FailingConstructor.class, IllegalStateException.class);
        IllegalStateException expected = new IllegalStateException("constructor failure");

        IllegalStateException actual = assertThrows(IllegalStateException.class, () -> ReflectiveAccess.construct(constructor, expected));

        assertSame(expected, actual);
    }

    public static final class FakeOptionalWidget {

        private final String prefix;

        public FakeOptionalWidget(int value) {
            this.prefix = "prefix-" + value;
        }

        public String describe(String value) {
            return this.prefix + ":" + value;
        }

        public String describe(int value) {
            return this.prefix + ":" + value;
        }

        public void failRuntime() {
            throw new IllegalArgumentException("runtime failure");
        }

        public void failError() {
            throw new AssertionError("error failure");
        }

        public void failChecked() throws IOException {
            throw new IOException("checked failure");
        }
    }

    public static final class FailingConstructor {

        public FailingConstructor(IllegalStateException failure) {
            throw failure;
        }
    }

}
