package eu.apps4net.parrotApp.configurations;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Provides application-wide language and translation services.
 * Parses {@code multiLanguage.xml} and exposes translated strings for both
 * the UI language and the configurable actions language.
 */
public class Language {

	/** The currently active UI language code (e.g. {@code "en"}, {@code "el"}). */
	private static String language = "en";

	/** Cached translations for the active UI language. */
	private static HashMap<String, String> languageStrings;

	/** Cached translations for the configured actions language. */
	private static HashMap<String, String> actionStrings;

	/**
	 * Parses the multiLanguage XML stream and returns a map of translation keys to
	 * their values for the requested language code.
	 * The factory is hardened against XXE injection (OWASP A05).
	 *
	 * @param inputStream the XML input stream to parse
	 * @param language    the ISO language code to extract translations for
	 * @return a map of translation key → translated text
	 * @throws Exception if parsing fails
	 */
	private static HashMap<String, String> parseXML(InputStream inputStream, String language) throws Exception {
		HashMap<String, String> translations = new HashMap<>();

		// Harden against XXE injection (OWASP A05 – Security Misconfiguration)
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(inputStream);

		Element root = document.getDocumentElement();
		NodeList entryNodes = root.getElementsByTagName("entry");

		for (int i = 0; i < entryNodes.getLength(); i++) {
			Node entryNode = entryNodes.item(i);
			if (entryNode.getNodeType() == Node.ELEMENT_NODE) {
				Element entryElement = (Element) entryNode;
				String entryId = entryElement.getAttribute("id");

				NodeList languageNodes = entryElement.getElementsByTagName("language");
				HashMap<String, String> entryTranslations = new HashMap<>();

				for (int j = 0; j < languageNodes.getLength(); j++) {
					Node languageNode = languageNodes.item(j);
					if (languageNode.getNodeType() == Node.ELEMENT_NODE) {
						Element languageElement = (Element) languageNode;
						String languageCode = languageElement.getAttribute("code");
						String translation = languageElement.getTextContent();
						entryTranslations.put(languageCode, translation);
					}
				}

				String translatedText = entryTranslations.getOrDefault(language, entryTranslations.get("en"));
				translations.put(entryId, translatedText);
			}
		}

		return translations;
	}

	/**
	 * Loads and caches translations for the given language code from the
	 * bundled {@code multiLanguage.xml} resource.
	 *
	 * @param language the ISO language code to load
	 * @return a map of translation key → translated text
	 * @throws Exception if the XML resource cannot be parsed
	 */
	public static HashMap<String, String> getTranslations(String language) throws Exception {
		InputStream inputStream = Language.class.getResourceAsStream("/multiLanguage.xml");
		languageStrings = parseXML(inputStream, language);
		return languageStrings;
	}

	/**
	 * Returns the currently active UI language code.
	 *
	 * @return the active language code
	 */
	public static String getLanguage() {
		return language;
	}

	/**
	 * Sets the active UI language and reloads the cached translation strings.
	 *
	 * @param language the ISO language code to activate
	 * @throws RuntimeException if the XML resource cannot be parsed
	 */
	public static void setLanguage(String language) {
		Language.language = language;
		InputStream inputStream = Language.class.getResourceAsStream("/multiLanguage.xml");
		try {
			languageStrings = parseXML(inputStream, language);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets the actions language and reloads the cached action translation strings.
	 *
	 * @param language the ISO language code to use for action strings
	 * @throws RuntimeException if the XML resource cannot be parsed
	 */
	public static void setActionsLanguage(String language) {
		InputStream inputStream = Language.class.getResourceAsStream("/multiLanguage.xml");
		try {
			actionStrings = parseXML(inputStream, language);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the UI translation for the given key.
	 *
	 * @param key the translation key
	 * @return the translated string, or {@code null} if the key is not found
	 */
	public static String getString(String key) {
		return languageStrings.get(key);
	}

	/**
	 * Returns the actions-language translation for the given key.
	 *
	 * @param key the translation key
	 * @return the translated string, or {@code null} if the key is not found
	 */
	public static String getActionString(String key) {
		return actionStrings.get(key);
	}
}
