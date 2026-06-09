package eu.apps4net.parrotApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import eu.apps4net.parrotApp.configurations.Language;
import eu.apps4net.parrotApp.services.SettingService;

/**
 * Main entry point for the ParrotApp Spring Boot application.
 * Bootstraps the application context and initialises the actions language.
 */
@SpringBootApplication
@EnableScheduling
public class ParrotApp {

	/** Spring-managed {@link SettingService} instance, resolved at construction time. */
	private static SettingService settingService;

	/**
	 * Constructs the application and captures the {@link SettingService} dependency
	 * so that it can be used in the static {@link #main(String[])} method.
	 *
	 * @param settingService the application settings service
	 */
	public ParrotApp(SettingService settingService) {
		ParrotApp.settingService = settingService;
	}

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments passed to the JVM
	 */
	public static void main(String[] args) {
		// Must be set before Derby initialises (first DataSource connection).
		// Prevents table-level lock escalation when concurrent threads insert large batches,
		// which would block FK constraint checks in other transactions.
		System.setProperty("derby.locks.escalationThreshold", String.valueOf(Integer.MAX_VALUE));

		SpringApplication.run(ParrotApp.class, args);

		Language.setActionsLanguage(settingService.getDefaultActionsLanguage());

		System.out.println("Run app on http://localhost:9999");
	}
}
