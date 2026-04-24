package eu.apps4net.parrotApp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.Setting;
import eu.apps4net.parrotApp.repositories.SettingRepository;

import java.util.List;

@Service
public class SettingService {

	private final SettingRepository settingRepository;

	@Autowired
	public SettingService(SettingRepository settingRepository) {
		this.settingRepository = settingRepository;
	}

	/**
	 * Sets default settings if there are no settings in the database.
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
	 * Returns the upload directory.
	 *
	 * @return the upload directory
	 */
	public String getUploadDir() {
		return settingRepository.getBySettingName("uploadDir").getSettingValue();
	}

	public String getDefaultActionsLanguage() {
		return settingRepository.getBySettingName("defaultActionsLanguage").getSettingValue();
	}

	/**
	 * Get all settings.
	 *
	 * @return List<Setting>
	 */
	public List<Setting> getSettings() {
		return settingRepository.findAll();
	}

	/**
	 * Get a setting by id
	 *
	 * @param id
	 * @return Setting
	 */
	public Setting getSetting(Long id) {
		return settingRepository.findById(id).orElse(null);
	}

	/**
	 * Update a setting
	 *
	 * @param setting
	 */
	public void updateSetting(Setting setting) {
		settingRepository.save(setting);
	}

	public Boolean fieldValidation(String settingName, String settingValue) {
		return true;
	}
}
