package de.keksuccino.konkrete.json;

import java.io.File;
import java.net.URL;
import java.util.List;

public class JsonUtils {

    public static List<String> getJsonValueByPath(String jsonString, String jsonParsingPath) {
        return JaywayJsonPathUtils.getJsonValueByPath(jsonString, jsonParsingPath);
    }

    public static List<String> getJsonValueByPath(File jsonFile, String jsonParsingPath) {
        return JaywayJsonPathUtils.getJsonValueByPath(jsonFile, jsonParsingPath);
    }

    public static List<String> getJsonValueByPath(URL jsonWebURL, String jsonParsingPath) {
        return JaywayJsonPathUtils.getJsonValueByPath(jsonWebURL, jsonParsingPath);
    }

}
