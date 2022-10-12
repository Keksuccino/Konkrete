//TODO übernehmen
package de.keksuccino.konkrete.json;

import com.google.gson.JsonArray;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.json.jsonpath.JsonPath;
import de.keksuccino.konkrete.web.WebUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

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
