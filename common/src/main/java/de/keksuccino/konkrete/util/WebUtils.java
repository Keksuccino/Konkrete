package de.keksuccino.konkrete.util;

import de.keksuccino.konkrete.util.file.FileUtils;
import de.keksuccino.konkrete.util.threading.KonkreteExecutors;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Provides bounded HTTP access, connectivity monitoring, and operating-system link helpers. */
public class WebUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final URI INTERNET_AVAILABILITY_ENDPOINT = URI.create("https://google.com");
    private static final int INTERNET_AVAILABILITY_TIMEOUT_MILLIS = 3000;
    private static final Duration INTERNET_AVAILABILITY_REFRESH_DELAY = Duration.ofSeconds(20L);
    private static final Duration RESOURCE_CONNECT_TIMEOUT = Duration.ofSeconds(10L);
    private static final Duration RESOURCE_READ_TIMEOUT = Duration.ofSeconds(30L);
    private static final Duration METADATA_CONNECT_TIMEOUT = Duration.ofSeconds(5L);
    private static final Duration METADATA_READ_TIMEOUT = Duration.ofSeconds(5L);
    private static final Duration METADATA_OVERALL_TIMEOUT = Duration.ofSeconds(10L);
    private static final BoundedWebResourceClient.RequestLimits METADATA_LIMITS = new BoundedWebResourceClient.RequestLimits(METADATA_CONNECT_TIMEOUT, METADATA_READ_TIMEOUT, METADATA_OVERALL_TIMEOUT, Long.MAX_VALUE);
    private static final InternetAvailabilityMonitor INTERNET_AVAILABILITY_MONITOR = new InternetAvailabilityMonitor(new HttpInternetAvailabilityProbe(INTERNET_AVAILABILITY_ENDPOINT, INTERNET_AVAILABILITY_TIMEOUT_MILLIS, INTERNET_AVAILABILITY_TIMEOUT_MILLIS, endpoint -> (HttpURLConnection) endpoint.toURL().openConnection()), () -> KonkreteExecutors.newSingleThreadScheduledExecutor("Konkrete-WebUtils-ConnectivityCheck"), INTERNET_AVAILABILITY_REFRESH_DELAY, available -> isConnectionAvailable = available);
    private static final BoundedWebResourceClient RESOURCE_CLIENT = new BoundedWebResourceClient(resourceUri -> (HttpURLConnection) resourceUri.toURL().openConnection(), KonkreteExecutors.newSingleThreadScheduledExecutor("Konkrete-WebUtils-ResourceDeadline"), System::nanoTime);

    private static volatile boolean isConnectionAvailable;

    /** Starts the asynchronous internet-availability monitor. */
    public static void init() {
        INTERNET_AVAILABILITY_MONITOR.init();
    }

    /** Stops web deadlines and connectivity monitoring, releasing their managed executors. */
    public static void shutdown() {
        try {
            RESOURCE_CLIENT.shutdown();
        } finally {
            INTERNET_AVAILABILITY_MONITOR.shutdown();
        }
    }

    /** Returns the latest asynchronously probed internet-availability value. */
    public static boolean isInternetAvailable() {
        return isConnectionAvailable;
    }

    /** Opens an HTTP(S) resource with conservative general-purpose bounds. */
    @Nullable
    public static InputStream openResourceStream(@NotNull String resourceURL) {
        return openResourceStream(resourceURL, WebResourceType.GENERAL);
    }

    /** Opens an HTTP(S) resource using the selected timeout and byte limits. */
    @Nullable
    public static InputStream openResourceStream(@NotNull String resourceURL, @NotNull WebResourceType resourceType) {
        URI resourceUri = parseHttpUri(resourceURL);
        if (resourceUri == null) return null;
        try {
            return RESOURCE_CLIENT.openResourceStream(resourceUri, Objects.requireNonNull(resourceType, "resourceType").limits());
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to open bounded web resource stream: {}", resourceURL, ex);
            return null;
        }
    }

    /** Returns the remote resource MIME type, or {@code null} when it cannot be resolved safely. */
    @Nullable
    public static String getMimeType(@NotNull String url) {
        URI resourceUri = parseHttpUri(url);
        if (resourceUri == null) return null;
        try {
            return RESOURCE_CLIENT.getMimeType(resourceUri, METADATA_LIMITS);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Returns whether the value identifies a reachable HTTP(S) resource. */
    public static boolean isValidUrl(@Nullable String url) {
        URI resourceUri = parseHttpUri(url);
        if (resourceUri == null) return false;
        try {
            return RESOURCE_CLIENT.isValidUrl(resourceUri, METADATA_LIMITS);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Reads the UTF-8 lines returned by a URL, logging failures and returning an empty list. */
    public static List<String> getPlainTextContentOfPage(@NotNull URL webLink) {
        Objects.requireNonNull(webLink, "webLink");
        try (InputStream input = openResourceStream(webLink.toString(), WebResourceType.TEXT)) {
            if (input == null) return new ArrayList<>();
            return FileUtils.readTextLinesFrom(input);
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get plain text content of URL: " + webLink, ex);
            return new ArrayList<>();
        }
    }

    /** Removes characters outside the RFC 3986 URL character set retained by the legacy API. */
    @Nullable
    public static String filterURL(@Nullable String url) {
        if (url == null) return null;
        String allowedPunctuation = "-._~:/?#[]@!$&'()*+,;%=";
        StringBuilder result = new StringBuilder(url.length());
        for (int index = 0; index < url.length(); index++) {
            char character = url.charAt(index);
            if (Character.isLetterOrDigit(character) || allowedPunctuation.indexOf(character) >= 0) result.append(character);
        }
        return result.toString();
    }

    /** Opens an HTTP(S) link with the operating system's default application. */
    public static void openWebLink(@NotNull String url) {
        Objects.requireNonNull(url, "url");
        try {
            String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            URL parsedUrl = new URL(url);
            if (Util.getPlatform() == Util.OS.OSX) Runtime.getRuntime().exec(new String[]{"open", url});
            else if (operatingSystem.contains("win")) Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            else Runtime.getRuntime().exec(new String[]{"xdg-open", parsedUrl.getProtocol().equals("file") ? url.replace("file:", "file://") : url});
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to open web link: {}", url, ex);
        }
    }

    @Nullable
    private static URI parseHttpUri(@Nullable String value) {
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) return null;
            return uri;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Defines bounded download policies for common resource consumption models. */
    public enum WebResourceType {
        /** Small textual resources. */
        TEXT(8L * 1024L * 1024L, Duration.ofMinutes(1L)),
        /** Static images. */
        IMAGE(128L * 1024L * 1024L, Duration.ofMinutes(3L)),
        /** Animated images buffered in memory. */
        BUFFERED_ANIMATED_TEXTURE(256L * 1024L * 1024L, Duration.ofMinutes(5L)),
        /** Animated archives streamed or spooled to disk. */
        STREAMED_ANIMATED_ARCHIVE(2L * 1024L * 1024L * 1024L, Duration.ofMinutes(10L)),
        /** Audio resources. */
        AUDIO(512L * 1024L * 1024L, Duration.ofMinutes(10L)),
        /** Video resources. */
        VIDEO(2L * 1024L * 1024L * 1024L, Duration.ofMinutes(10L)),
        /** General bounded resources. */
        GENERAL(256L * 1024L * 1024L, Duration.ofMinutes(5L));

        private final BoundedWebResourceClient.RequestLimits limits;

        WebResourceType(long maximumBytes, @NotNull Duration overallTimeout) {
            this.limits = new BoundedWebResourceClient.RequestLimits(RESOURCE_CONNECT_TIMEOUT, RESOURCE_READ_TIMEOUT, overallTimeout, maximumBytes);
        }

        private BoundedWebResourceClient.RequestLimits limits() {
            return this.limits;
        }
    }
}
