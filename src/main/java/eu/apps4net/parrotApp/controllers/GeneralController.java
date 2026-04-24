package eu.apps4net.parrotApp.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/general")
public class GeneralController {

	@GetMapping(path = "appAlive")
	public boolean getAppAlive() {
		return true;
	}
}
