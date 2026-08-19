package de.keksuccino.konkrete.util.minecraftuser.v2;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/** A Minecraft profile identifier and player name. */
public class UserProfile {

    /** Compact or dashed UUID text returned by the profile service. */
    protected String id;
    /** Player name returned by the profile service. */
    protected String name;

    /** Returns the uuid. */
    @Nullable
    public UUID getUUID() {
        if (this.id == null) return null;
        return UUID.fromString(this.id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    /** Returns the name. */
    @Nullable
    public String getName() {
        return this.name;
    }

}
