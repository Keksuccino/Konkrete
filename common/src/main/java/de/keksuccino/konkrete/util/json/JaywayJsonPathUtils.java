package de.keksuccino.konkrete.util.json;

import com.google.gson.JsonArray;
import com.jayway.jsonpath.JsonPath;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.file.FileUtils;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Evaluates Jayway JSONPath expressions and flattens results to strings. */
public class JaywayJsonPathUtils {

    /**
     * Evaluates {@code jsonParsingPath} against JSON text and returns scalar/array values in encounter order.
     * Parse, path, and conversion failures are printed and produce an empty or partially collected mutable list.
     */
    public static List<String> getJsonValueByPath(String jsonString, String jsonParsingPath) {
        List<String> l = new ArrayList<>();
        try {
            Object j = JsonPath.read(jsonString, jsonParsingPath);
            if (j instanceof JsonArray) {
                ((JsonArray)j).forEach((element) -> {
                    String value = element.toString();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1);
                        value = new StringBuilder(new StringBuilder(value).reverse().substring(1)).reverse().toString();
                    }
                    l.add(value);
                });
            } else {
                String value = j.toString();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1);
                    value = new StringBuilder(new StringBuilder(value).reverse().substring(1)).reverse().toString();
                }
                l.add(value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return l;
    }

    /** Reads an existing JSON file as UTF-8 lines and delegates evaluation; invalid files produce an empty list. */
    public static List<String> getJsonValueByPath(File jsonFile, String jsonParsingPath) {
        if ((jsonFile != null) && jsonFile.isFile()) {
            List<String> lines = FileUtils.getFileLines(jsonFile);
            String json = "";
            for (String s : lines) {
                json += s;
            }
            return getJsonValueByPath(json, jsonParsingPath);
        }
        return new ArrayList<>();
    }

    /** Fetches a valid web URL and delegates evaluation; invalid URLs and fetch failures produce an empty list. */
    public static List<String> getJsonValueByPath(URL jsonWebURL, String jsonParsingPath) {
        if (jsonWebURL != null) {
            try {
                if (WebUtils.isValidUrl(jsonWebURL.toString())) {
                    List<String> lines = WebUtils.getPlainTextContentOfPage(jsonWebURL);
                    String json = "";
                    for (String s : lines) {
                        json += s;
                    }
                    return getJsonValueByPath(json, jsonParsingPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

}
