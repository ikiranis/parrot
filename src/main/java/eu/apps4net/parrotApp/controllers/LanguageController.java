package eu.apps4net.parrotApp.controllers;

import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.configurations.Language;
import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.models.LanguageDTO;
import eu.apps4net.parrotApp.services.LanguageService;

import java.util.List;
import java.util.Map;

/**
 * REST controller for language and translation management.
 * Exposes endpoints to retrieve translation strings and change the active UI language.
 */
@RestController
@RequestMapping("api/languages")
public class LanguageController {

	/** The language service used to load translations. */
	private final LanguageService languageService;

	/**
	 * Constructs a new {@code LanguageController}.
	 *
	 * @param languageService the language service
	 */
	public LanguageController(LanguageService languageService) {
		this.languageService = languageService;
	}

	/**
	 * Returns all translation entries for the requested language code.
	 *
	 * @param language the ISO language code (e.g. {@code "en"}, {@code "el"})
	 * @return list of {@link LanguageDTO} translation pairs
	 * @throws NotFoundException if translations cannot be loaded
	 */
	@GetMapping(path = "all/{language}")
	public List<LanguageDTO> getLanguages(@PathVariable("language") String language) {
		try {
			return languageService.getTranslations(language);
		} catch (Exception e) {
			throw new NotFoundException(Language.getString("Languages not found"));
		}
	}

	/**
	 * Sets the active UI language.
	 *
	 * @param request JSON body containing a {@code "language"} key with the ISO language code
	 */
	@PostMapping()
	public void setLanguage(@RequestBody Map<String, String> request) {
		Language.setLanguage(request.get("language"));
	}
}
