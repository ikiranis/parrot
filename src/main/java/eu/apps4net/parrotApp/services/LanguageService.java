package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.configurations.Language;
import eu.apps4net.parrotApp.models.LanguageDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that provides translation data for the application UI.
 * Delegates XML parsing to {@link Language} and maps the result to
 * {@link LanguageDTO} instances.
 */
@Service
public class LanguageService {

	/**
	 * Returns all translation entries for the specified language code as a list
	 * of {@link LanguageDTO} key-value pairs.
	 *
	 * @param language the ISO language code to load (e.g. {@code "en"}, {@code "el"})
	 * @return list of translation pairs
	 * @throws Exception if the multiLanguage XML resource cannot be parsed
	 */
	public List<LanguageDTO> getTranslations(String language) throws Exception {
		List<LanguageDTO> translations = new ArrayList<>();

		Language.getTranslations(language).forEach((key, value) -> {
			translations.add(new LanguageDTO(key, value));
		});

		return translations;
	}
}
