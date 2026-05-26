package eu.apps4net.parrotApp.models;

/**
 * Data Transfer Object that pairs a translation key with its translated value
 * for a specific language, as returned by the language API.
 */
public class LanguageDTO {

	/** The translation key (identifier string). */
	private String text;

	/** The translated text for the active language. */
	private String translation;

	/**
	 * Constructs a new {@code LanguageDTO}.
	 *
	 * @param text        the translation key
	 * @param translation the translated text
	 */
	public LanguageDTO(String text, String translation) {
		this.text = text;
		this.translation = translation;
	}

	/**
	 * Returns the translation key.
	 *
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the translation key.
	 *
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Returns the translated text.
	 *
	 * @return the translation
	 */
	public String getTranslation() {
		return translation;
	}

	/**
	 * Sets the translated text.
	 *
	 * @param translation the translation to set
	 */
	public void setTranslation(String translation) {
		this.translation = translation;
	}

	@Override
	public String toString() {
		return "LanguageDTO{" +
				"text='" + text + '\'' +
				", translation='" + translation + '\'' +
				'}';
	}
}
