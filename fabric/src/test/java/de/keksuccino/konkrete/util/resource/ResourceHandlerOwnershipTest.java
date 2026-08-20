package de.keksuccino.konkrete.util.resource;

import de.keksuccino.konkrete.util.file.type.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceHandlerOwnershipTest {

    @Test
    void rejectedDuplicateRegistrationIsClosedWithoutReplacingOwner() {
        TestHandler handler = new TestHandler();
        TestResource owner = new TestResource();
        TestResource duplicate = new TestResource();

        handler.registerIfKeyAbsent("key", owner);
        handler.registerIfKeyAbsent("key", duplicate);

        assertSame(owner, handler.getIfRegistered("key"));
        assertFalse(owner.closed);
        assertTrue(duplicate.closed);
    }

    @Test
    void registeringTheCurrentOwnerAgainIsIdempotent() {
        TestHandler handler = new TestHandler();
        TestResource owner = new TestResource();

        handler.registerIfKeyAbsent("key", owner);
        handler.registerIfKeyAbsent("key", owner);

        assertSame(owner, handler.getIfRegistered("key"));
        assertFalse(owner.closed);
    }

    @Test
    void resourceSourceReleaseCanonicalizesAnUnprefixedIdentifier() {
        TestHandler handler = new TestHandler();
        TestResource resource = new TestResource();
        handler.registerIfKeyAbsent("[source:location]example:textures/test.png", resource);

        handler.release("example:textures/test.png", true);

        assertFalse(handler.hasResource("[source:location]example:textures/test.png"));
        assertTrue(resource.closed);
    }

    private static final class TestHandler extends ResourceHandler<TestResource, FileType<TestResource>> {

        @Override
        public @NotNull List<FileType<TestResource>> getAllowedFileTypes() {
            return List.of();
        }

        @Override
        public @Nullable FileType<TestResource> getFallbackFileType() {
            return null;
        }

    }

    private static final class TestResource implements Resource {

        private boolean closed;

        @Override
        public @Nullable InputStream open() {
            return null;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public boolean isLoadingCompleted() {
            return true;
        }

        @Override
        public boolean isLoadingFailed() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            this.closed = true;
        }

    }

}
