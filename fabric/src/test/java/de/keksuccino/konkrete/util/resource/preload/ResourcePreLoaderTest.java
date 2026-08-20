package de.keksuccino.konkrete.util.resource.preload;

import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.ResourceSource;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ResourceLock("ResourceRuntime preload configuration")
class ResourcePreLoaderTest {

    @Test
    void customSourceTypeParsesPreloadsPersistsAndUnregisters() {
        AtomicInteger loads = new AtomicInteger();
        AtomicReference<String> persisted = new AtomicReference<>("");
        ResourceRuntime.setPreLoadConfiguration(persisted::get, persisted::set);

        try (ResourceRuntime.Registration registration = ResourcePreLoader.registerSourceType("[group]", GroupSource.class, GroupSource::new, (source, timeout) -> loads.incrementAndGet())) {
            GroupSource source = assertInstanceOf(GroupSource.class, ResourcePreLoader.buildSourceFromString("[group]images"));
            String serialized = ResourcePreLoader.addResourceSource(source, null, true);

            assertEquals("[group]images%!source_end!%", serialized);
            assertEquals(serialized, persisted.get());
            assertEquals(List.of("[group]images"), ResourcePreLoader.getRegisteredResourceSources(null).stream().map(ResourceSource::getSerializationSource).toList());

            ResourcePreLoader.preLoadAll(0L);
            assertEquals(1, loads.get());
        } finally {
            ResourceRuntime.setPreLoadConfiguration(() -> "", ignored -> {});
        }
    }

    private static final class GroupSource extends ResourceSource {

        private GroupSource(@NotNull String serialized) {
            this.sourceType = ResourceSourceType.LOCAL;
            this.resourceSourceWithoutPrefix = serialized.substring("[group]".length());
            this.serializationSourceWithoutPrefix = this.resourceSourceWithoutPrefix;
        }

        @Override
        public @NotNull String getSourceWithPrefix() {
            return this.getSerializationSource();
        }

        @Override
        public @NotNull String getSerializationSource() {
            return "[group]" + this.resourceSourceWithoutPrefix;
        }

    }

}
