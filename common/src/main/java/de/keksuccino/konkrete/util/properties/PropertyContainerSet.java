package de.keksuccino.konkrete.util.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ordered collection of serialized property containers with a shared set type. Instances are not thread-safe.
 */
public class PropertyContainerSet {

    private final List<PropertyContainer> containers = new ArrayList<>();
    private String type;

    /**
     * Creates an empty property-container set.
     *
     * @param type set type
     */
    public PropertyContainerSet(@NotNull String type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Appends a container.
     *
     * @param container container to append
     */
    public void putContainer(@NotNull PropertyContainer container) {
        this.containers.add(Objects.requireNonNull(container, "container"));
    }

    /**
     * Returns an unmodifiable view of all containers.
     *
     * @return container view
     */
    @NotNull
    public List<PropertyContainer> getContainers() {
        return Collections.unmodifiableList(this.containers);
    }

    /**
     * Returns containers matching a type.
     *
     * @param type requested type
     * @return matching container snapshot
     */
    @NotNull
    public List<PropertyContainer> getContainersOfType(@NotNull String type) {
        String checkedType = Objects.requireNonNull(type, "type");
        List<PropertyContainer> matches = new ArrayList<>();
        for (PropertyContainer container : this.containers) {
            if (container.getType().equals(checkedType)) matches.add(container);
        }
        return matches;
    }

    /**
     * Returns the first container matching a type.
     *
     * @param type requested type
     * @return first match, or {@code null}
     */
    @Nullable
    public PropertyContainer getFirstContainerOfType(@NotNull String type) {
        String checkedType = Objects.requireNonNull(type, "type");
        for (PropertyContainer container : this.containers) {
            if (container.getType().equals(checkedType)) return container;
        }
        return null;
    }

    /**
     * Returns the set type.
     *
     * @return set type
     */
    @NotNull
    public String getType() {
        return this.type;
    }

    /**
     * Changes the set type.
     *
     * @param type new type
     */
    public void setType(@NotNull String type) {
        this.type = Objects.requireNonNull(type, "type");
    }

}
