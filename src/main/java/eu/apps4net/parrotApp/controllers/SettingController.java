package eu.apps4net.parrotApp.controllers;

import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.configurations.Language;
import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.exceptions.ProcessingErrorException;
import eu.apps4net.parrotApp.models.Setting;
import eu.apps4net.parrotApp.services.SettingService;
import eu.apps4net.parrotApp.utilities.ReflectionUtils;

import java.util.List;
import java.util.Map;

/**
 * REST controller for application settings management.
 * Exposes CRUD endpoints for {@link Setting} records.
 */
@RestController
@RequestMapping("api/settings")
public class SettingController {

	/** The settings service used to retrieve and persist settings. */
	private final SettingService settingService;

	/**
	 * Constructs a new {@code SettingController}.
	 *
	 * @param settingService the settings service
	 */
	public SettingController(SettingService settingService) {
		this.settingService = settingService;
	}

	/**
	 * Returns all application settings.
	 *
	 * @return list of all {@link Setting} records
	 */
	@GetMapping(path = "all")
	public List<Setting> getSettings() {
		return settingService.getSettings();
	}

	/**
	 * Returns the setting with the given identifier.
	 *
	 * @param id the setting identifier
	 * @return the matching {@link Setting}
	 * @throws NotFoundException if no setting exists with the given id
	 */
	@GetMapping(path = "{id}")
	public Setting getSetting(@PathVariable("id") Long id) {
		return settingService.getSetting(id)
				.orElseThrow(() -> new NotFoundException("Setting not found: " + id));
	}

	/**
	 * Updates the value of an existing setting.
	 *
	 * @param request JSON body containing {@code id}, {@code settingName}, and {@code settingValue}
	 * @throws NotFoundException       if the setting id does not exist
	 * @throws ProcessingErrorException if the new value fails validation or a reflection error occurs
	 */
	@PutMapping
	public void updateSetting(@RequestBody Map<String, String> request) {
		Setting setting = settingService.getSetting(Long.parseLong(request.get("id")))
				.orElseThrow(() -> new NotFoundException("Setting not found"));

		if (!settingService.fieldValidation(request.get("settingName"), request.get("settingValue"))) {
			throw new ProcessingErrorException(Language.getString("Invalid setting value"));
		}

		String[] selectedFields = {"settingValue"};

		try {
			ReflectionUtils.updateFields(setting, request, selectedFields);
		} catch (IllegalAccessException e) {
			throw new ProcessingErrorException(e.getMessage());
		}

		if (request.get("settingName").equals("defaultActionsLanguage")) {
			Language.setActionsLanguage(request.get("settingValue"));
		}

		settingService.updateSetting(setting);
	}
}
