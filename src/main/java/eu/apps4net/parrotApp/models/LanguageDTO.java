package eu.apps4net.parrotApp.models;

public class LanguageDTO {
	private String text;
	private String translation;

	public LanguageDTO(String text, String translation) {
		this.text = text;
		this.translation = translation;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTranslation() {
		return translation;
	}

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
