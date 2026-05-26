package eu.apps4net.parrotApp.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes general application health endpoints.
 */
@RestController
@RequestMapping("api/general")
public class GeneralController {

	/**
	 * Returns {@code true} to signal that the backend is reachable.
	 *
	 * @return {@code true} always
	 */
	@GetMapping(path = "appAlive")
	public boolean getAppAlive() {
		return true;
	}
}
