package eu.apps4net.parrotApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.Setting;

/**
 * Spring Data JPA repository for {@link Setting} entities.
 */
@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {

	/**
	 * Finds a setting by its unique name.
	 *
	 * @param name the setting name
	 * @return the matching {@link Setting}, or {@code null} if not found
	 */
	Setting getBySettingName(String name);
}
