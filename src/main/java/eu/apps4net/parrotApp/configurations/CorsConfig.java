package eu.apps4net.parrotApp.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS configuration for the application.
 * Permits all origins, methods, and headers to support the Vue 3 frontend
 * running on a separate dev port.
 */
@Configuration
public class CorsConfig extends CorsConfiguration {

	/**
	 * Registers a {@link CorsFilter} that allows all cross-origin requests.
	 *
	 * @return configured {@link CorsFilter} bean
	 */
	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfig config = new CorsConfig();

		config.addAllowedOrigin("*");
		config.addAllowedMethod("*");
		config.addAllowedHeader("*");

		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
}
