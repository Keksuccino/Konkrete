package de.keksuccino.konkrete.json.minidev.json.reader;

import java.io.IOException;

import de.keksuccino.konkrete.json.minidev.json.JSONStyle;

public interface JsonWriterI<T> {
	public <E extends T> void writeJSONString(E value, Appendable out, JSONStyle compression) throws IOException;
}
