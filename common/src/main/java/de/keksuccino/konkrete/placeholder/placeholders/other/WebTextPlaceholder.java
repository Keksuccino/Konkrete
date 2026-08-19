package de.keksuccino.konkrete.placeholder.placeholders.other;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.util.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.*;

/** Asynchronously downloads text with bounded stale-while-refresh caching. */
public class WebTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final WebTextPlaceholderCache CACHE = new WebTextPlaceholderCache(task -> Thread.ofVirtual().name("Konkrete-WebTextPlaceholder-WebLoader").start(task), link -> WebUtils.isValidUrl(link) ? WebTextPlaceholderCache.LoadResult.valid(WebUtils.getPlainTextContentOfPage(new URL(link))) : WebTextPlaceholderCache.LoadResult.invalid());

    /** Creates the {@code webtext} placeholder. */
    public WebTextPlaceholder() {
        super("webtext");
    }

    /** Cancels current web loads and clears cached text and invalid-link decisions. */
    public static void reloadCache() {
        CACHE.reload();
        LOGGER.info("[KONKRETE] WebTextPlaceholder cache successfully cleared!");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String link = dps.values.get("link");
        if (link != null) {
            link = StringUtils.convertFormatCodes(link, "§", "&");
            WebTextPlaceholderCache.Lookup lookup = CACHE.getOrLoad(dps.placeholderString, link);
            if (lookup.status() == WebTextPlaceholderCache.Status.INVALID) return null;
            if (lookup.status() == WebTextPlaceholderCache.Status.LOADING) return "";
            if (!lookup.lines().isEmpty()) return lookup.lines().get(0);
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("link");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.webtext");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.webtext.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("link", "https://example.com/text.txt");
        return dps;
    }

}
