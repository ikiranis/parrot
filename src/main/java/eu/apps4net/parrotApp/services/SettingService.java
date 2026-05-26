package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.Setting;
import eu.apps4net.parrotApp.repositories.SettingRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for application settings.
 * Provides CRUD operations and validation for {@link Setting} records.
 */
@Service
public class SettingService {

	/** Repository used to persist and retrieve settings. */
	private final SettingRepository settingRepository;

	/**
	 * Constructs a new {@code SettingService}.
	 *
	 * @param settingRepository the settings repository
	 */
	public SettingService(SettingRepository settingRepository) {
		this.settingRepository = settingRepository;
	}

	/**
	 * Seeds default settings if they are absent from the database.
	 * Ensures {@code uploadDir} and {@code defaultActionsLanguage} always exist.
	 */
	public void setDefaultSettings() {
		List<Setting> settings = settingRepository.findAll();

		if (settings.isEmpty() || settingRepository.getBySettingName("uploadDir") == null) {
			settingRepository.save(new Setting("uploadDir", "uploads"));
		}

		if (settings.isEmpty() || settingRepository.getBySettingName("defaultActionsLanguage") == null) {
			settingRepository.save(new Setting("defaultActionsLanguage", "en"));
		}
	}

	/**
	 * Returns the configured upload directory path.
	 *
	 * @return the upload directory value
	 */
	public String getUploadDir() {
		return settingRepository.getBySettingName("uploadDir").getSettingValue();
	}

	/**
	 * Returns the configured default actions language code.
	 *
	 * @return the actions language code (e.g. {@code "en"})
	 */
	public String getDefaultActionsLanguage() {
		return settingRepository.getBySettingName("defaultActionsLanguage").getSettingValue();
	}

	/**
	 * Returns all application settings.
	 *
	 * @return list of all {@link Setting} records
	 */
	public List<Setting> getSettings() {
		return settingRepository.findAll();
	}

	/**
	 * Finds a setting by its primary key.
	 *
	 * @param id the setting identifier
	 * @return an {@link Optional} containing the {@link Setting}, or empty if not found
	 */
	public Optional<Setting> getSetting(Long id) {
		return settingRepository.findById(id);
	}

	/**
	 * Persists an updated setting.
	 *
	 * @param setting the setting to save
	 */
	public void updateSetting(Setting setting) {
		settingRepository.save(setting);
	}

	/**
	 * Validates the proposed value for a given setting name.
	 * Override this method to add per-field business rules.
	 *
	 * @param settingName  the name of the setting to validate
	 * @param settingValue the proposed new value
	 * @return {@code true} if the value is acceptable
	 */
	public Boolean fieldValidation(String settingName, String settingValue) {
		return true;
	}
}
