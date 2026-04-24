package eu.apps4net.parrotApp.configurations;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;

public class Language {
    private static String language = "en";
    private static HashMap<String, String> languageStrings;
    private static HashMap<String, String> actionStrings;

    private static HashMap<String, String> parseXML(InputStream inputStream, String language) throws Exception {
        // Create a new HashMap to store the parsed data
        HashMap<String, String> translations = new HashMap<>();

        // Create a DocumentBuilderFactory and a DocumentBuilder to parse the XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Parse the XML from the InputStream
        Document document = builder.parse(inputStream);

        // Get the root element of the XML
        Element root = document.getDocumentElement();

        // Get a list of all <entry> elements
        NodeList entryNodes = root.getElementsByTagName("entry");

        // Loop through each <entry> element
        for (int i = 0; i < entryNodes.getLength(); i++) {
            Node entryNode = entryNodes.item(i);
            if (entryNode.getNodeType() == Node.ELEMENT_NODE) {
                Element entryElement = (Element) entryNode;

                // Get the ID attribute of the <entry> element
                String entryId = entryElement.getAttribute("id");

                // Get a list of all <language> elements within this <entry> element
                NodeList languageNodes = entryElement.getElementsByTagName("language");

                // Create a HashMap to store translations for this entry
                HashMap<String, String> entryTranslations = new HashMap<>();

                // Loop through each <language> element
                for (int j = 0; j < languageNodes.getLength(); j++) {
                    Node languageNode = languageNodes.item(j);
                    if (languageNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element languageElement = (Element) languageNode;

                        // Get the language code as the key and the translation as the value
                        String languageCode = languageElement.getAttribute("code");
                        String translation = languageElement.getTextContent();

                        // Put the translation into the HashMap for this entry
                        entryTranslations.put(languageCode, translation);
                    }
                }

                // Get the translation for the specified language, or use the default language ("en") if not available
                String translatedText = entryTranslations.getOrDefault(language, entryTranslations.get("en"));

                // Put the entry's translation into the main translations HashMap
                translations.put(entryId, translatedText);
            }
        }

        return translations;
    }

    /**
     * Read XML file and put strings into languageStrings
     *
     * @param language
     */
    public static HashMap<String, String> getTranslations(String language) throws Exception {
        InputStream inputStream = Language.class.getResourceAsStream("/multiLanguage.xml");

        languageStrings = parseXML(inputStream, language);

        return languageStrings;
    }

    public static String getLanguage() {
        return language;
    }

    public static void setLanguage(String language) {
        Language.language = language;

        InputStream inputStream = Language.class.getResourceAsStream("/multiLanguage.xml");

        try {
            languageStrings = parseXML(inputStream, language);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setActionsLanguage(String language) {
        InputStream inputStream = Language.class.getResourceAsStream("/multiLanguage.xml");

        try {
            actionStrings = parseXML(inputStream, language);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getString(String key) {
        return languageStrings.get(key);
    }

    public static String getActionString(String key) {
        return actionStrings.get(key);
    }
}
