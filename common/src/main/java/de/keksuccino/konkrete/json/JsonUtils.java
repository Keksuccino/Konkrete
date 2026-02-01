package de.keksuccino.konkrete.json;

import de.keksuccino.konkrete.Konkrete;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    public static List<String> getJsonValueByPath(String jsonString, String jsonParsingPath) {
        if (Konkrete.JSON_PATH_LIBRARY_LOADED) {
            return JaywayJsonPathUtils.getJsonValueByPath(jsonString, jsonParsingPath);
        }
        return new ArrayList<>();
    }

    public static List<String> getJsonValueByPath(File jsonFile, String jsonParsingPath) {
        if (Konkrete.JSON_PATH_LIBRARY_LOADED) {
            return JaywayJsonPathUtils.getJsonValueByPath(jsonFile, jsonParsingPath);
        }
        return new ArrayList<>();
    }

    public static List<String> getJsonValueByPath(URL jsonWebURL, String jsonParsingPath) {
        if (Konkrete.JSON_PATH_LIBRARY_LOADED) {
            return JaywayJsonPathUtils.getJsonValueByPath(jsonWebURL, jsonParsingPath);
        }
        return new ArrayList<>();
    }

}
