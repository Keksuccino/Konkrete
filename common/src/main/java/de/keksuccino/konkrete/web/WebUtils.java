package de.keksuccino.konkrete.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import de.keksuccino.konkrete.input.CharacterFilter;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebUtils {

	private static final Logger LOGGER = LogManager.getLogger();
	
	public static boolean isValidUrl(String url) {
		if ((url == null) || (!url.startsWith("http://") && !url.startsWith("https://"))) {
			return false;
		}
		try {
			URL u = new URL(url);
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			c.addRequestProperty("User-Agent", "Mozilla/4.0");
			c.setRequestMethod("HEAD");
			int r = c.getResponseCode();
			if (r == 200) {
				return true;
			}
		} catch (Exception ignore) {
			try {
				URL u = new URL(url);
				HttpURLConnection c = (HttpURLConnection) u.openConnection();
				c.addRequestProperty("User-Agent", "Mozilla/4.0");
				int r = c.getResponseCode();
				if (r == 200) {
					return true;
				}
			} catch (Exception ignore2) {
			}
		}
		return false;
	}

	public static List<String> getPlainTextContentOfPage(URL webLink) {
		List<String> l = new ArrayList<>();
		BufferedReader r = null;
		InputStream in = null;
		InputStreamReader reader = null;
		try {
			in = webLink.openStream();
			reader = new InputStreamReader(in, StandardCharsets.UTF_8);
			r = new BufferedReader(reader);
			String s = r.readLine();
			while(s != null) {
				l.add(s);
				s = r.readLine();
			}
		} catch (Exception ex) {
			LOGGER.error("[KONKRETE] Failed to get plain text content of URL: " + webLink.toString(), ex);
			l.clear();
		}
		IOUtils.closeQuietly(r);
		IOUtils.closeQuietly(in);
		IOUtils.closeQuietly(reader);
		return l;
	}
	
	public static String filterURL(String url) {
		if (url == null) {
			return null;
		}
		CharacterFilter f = new CharacterFilter();
		f.addAllowedCharacters("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
				"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r","s", "t", "u", "v", "w", "x", "y", "z",
				"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
				"-", ".", "_", "~", ":", "/", "?", "#", "[", "]", "@", "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "%", "=");
		return f.filterForAllowedChars(url);
	}

}
