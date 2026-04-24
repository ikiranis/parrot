package eu.apps4net.parrotApp.configurations;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import eu.apps4net.parrotApp.services.SettingService;

@Configuration
public class SettingConfiguration {

	private final SettingService settingService;

	@Autowired
	public SettingConfiguration(SettingService settingService) {
		this.settingService = settingService;
	}

	@Bean
	CommandLineRunner commandLineRunner() {
		return args -> {
			// Hack this to increase id sequence by 1
			System.setProperty("derby.language.sequence.preallocator", "1");
			System.setProperty("prism.order", "sw");

			settingService.setDefaultSettings();
		};
	}

	/**
	 * Accept max file upload size
	 *
	 * @return
	 */
	@Bean
	public MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofBytes(1000000000L));
		factory.setMaxRequestSize(DataSize.ofBytes(1000000000L));
		return factory.createMultipartConfig();
	}
}
