package eu.apps4net.parrotApp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.configurations.Language;
import eu.apps4net.parrotApp.exceptions.ProcessingErrorException;
import eu.apps4net.parrotApp.models.Setting;
import eu.apps4net.parrotApp.services.SettingService;
import eu.apps4net.parrotApp.utilities.ReflectionUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/setting")
public class SettingController {
	private final SettingService settingService;

	@Autowired
	public SettingController(SettingService settingService) {
		this.settingService = settingService;
	}

	/**
	 * Get all settings.
	 *
	 * @return List<Setting>
	 */
	@GetMapping(path = "all")
	public List<Setting> getSettings() {
		return settingService.getSettings();
	}

	/**
	 * Get a setting by id.
	 *
	 * @param id
	 * @return Setting
	 */
	@GetMapping(path = "{id}")
	public Setting getSetting(@PathVariable("id") Long id) {
		return settingService.getSetting(id);
	}

	/**
	 * Edit a setting
	 *
	 * @param request
	 */
	@PutMapping
	public void updateSetting(@RequestBody Map<String, String> request) {
		Setting setting = settingService.getSetting(Long.parseLong(request.get("id")));

		// Validate fields
		if (!settingService.fieldValidation(request.get("settingName"), request.get("settingValue"))) {
			throw new ProcessingErrorException(Language.getString("Invalid setting value"));
		}

		// Update the selected fields, with the values from the request
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
