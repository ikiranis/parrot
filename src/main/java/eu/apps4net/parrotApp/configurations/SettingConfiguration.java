package eu.apps4net.parrotApp.configurations;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import eu.apps4net.parrotApp.services.SettingService;

/**
 * Application startup configuration.
 * Initialises default settings and configures the multipart file-upload limits.
 */
@Configuration
public class SettingConfiguration {

	/** The settings service used to seed default values on first run. */
	private final SettingService settingService;

	/**
	 * Constructs a new {@code SettingConfiguration} with the given service.
	 *
	 * @param settingService the settings service
	 */
	@Autowired
	public SettingConfiguration(SettingService settingService) {
		this.settingService = settingService;
	}

	/**
	 * Runs startup tasks after the application context is ready.
	 * Sets Derby sequence pre-allocation and seeds default settings.
	 *
	 * @return a {@link CommandLineRunner} that performs initialisation
	 */
	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {
			// Hack this to increase id sequence by 1
			System.setProperty("derby.language.sequence.preallocator", "1");
			System.setProperty("prism.order", "sw");

			settingService.setDefaultSettings();
		};
	}

	/**
	 * Configures the maximum allowed file and request upload sizes.
	 *
	 * @return a {@link MultipartConfigElement} with 1 GB limits
	 */
	@Bean
	public MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofBytes(1000000000L));
		factory.setMaxRequestSize(DataSize.ofBytes(1000000000L));
		return factory.createMultipartConfig();
	}
}
