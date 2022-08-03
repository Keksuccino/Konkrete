package de.keksuccino.konkrete.json.jsonpath.internal;

import de.keksuccino.konkrete.json.jsonpath.Configuration;
import de.keksuccino.konkrete.json.jsonpath.DocumentContext;
import de.keksuccino.konkrete.json.jsonpath.ParseContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static de.keksuccino.konkrete.json.jsonpath.internal.Utils.notEmpty;
import static de.keksuccino.konkrete.json.jsonpath.internal.Utils.notNull;

public class ParseContextImpl implements ParseContext {

    private final Configuration configuration;

    public ParseContextImpl() {
        this(Configuration.defaultConfiguration());
    }

    public ParseContextImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public DocumentContext parse(Object json) {
        Utils.notNull(json, "json object can not be null");
        return new JsonContext(json, configuration);
    }

    @Override
    public DocumentContext parse(String json) {
        Utils.notEmpty(json, "json string can not be null or empty");
        Object obj = configuration.jsonProvider().parse(json);
        return new JsonContext(obj, configuration);
    }

    @Override
    public DocumentContext parseUtf8(byte[] json) {
        Utils.notEmpty(json, "json bytes can not be null or empty");
        Object obj = configuration.jsonProvider().parse(json);
        return new JsonContext(obj, configuration);
    }

    @Override
    public DocumentContext parse(InputStream json) {
        return parse(json, "UTF-8");
    }

    @Override
    public DocumentContext parse(InputStream json, String charset) {
        Utils.notNull(json, "json input stream can not be null");
        Utils.notNull(charset, "charset can not be null");
        try {
            Object obj = configuration.jsonProvider().parse(json, charset);
            return new JsonContext(obj, configuration);
        } finally {
            Utils.closeQuietly(json);
        }
    }

    @Override
    public DocumentContext parse(File json) throws IOException {
        Utils.notNull(json, "json file can not be null");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(json);
            return parse(fis);
        } finally {
            Utils.closeQuietly(fis);
        }
    }

    @Override
    @Deprecated
    public DocumentContext parse(URL url) throws IOException {
        Utils.notNull(url, "url can not be null");
        InputStream fis = null;
        try {
            fis = url.openStream();
            return parse(fis);
        } finally {
            Utils.closeQuietly(fis);
        }
    }

}
