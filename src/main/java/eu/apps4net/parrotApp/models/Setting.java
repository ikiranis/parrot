package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;

/**
 * JPA entity representing an application configuration setting.
 * Each record stores a named key-value pair persisted in the {@code setting} table.
 */
@Entity(name = "Setting")
@Table(name = "setting")
public class Setting {

	/** Auto-generated primary key. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Unique name that identifies the setting. */
	@Column(name = "setting_name", nullable = false)
	private String settingName;

	/** The value stored for this setting. */
	@Column(name = "setting_value")
	private String settingValue;

	/** Required no-arg constructor for JPA. */
	public Setting() {
	}

	/**
	 * Constructs a new {@code Setting} with the given name and value.
	 *
	 * @param settingName  the unique setting name
	 * @param settingValue the initial value
	 */
	public Setting(String settingName, String settingValue) {
		this.settingName = settingName;
		this.settingValue = settingValue;
	}

	/**
	 * Returns the primary key.
	 *
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the primary key.
	 *
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Returns the setting name.
	 *
	 * @return the settingName
	 */
	public String getSettingName() {
		return settingName;
	}

	/**
	 * Sets the setting name.
	 *
	 * @param settingName the settingName to set
	 */
	public void setSettingName(String settingName) {
		this.settingName = settingName;
	}

	/**
	 * Returns the setting value.
	 *
	 * @return the settingValue
	 */
	public String getSettingValue() {
		return settingValue;
	}

	/**
	 * Sets the setting value.
	 *
	 * @param settingValue the settingValue to set
	 */
	public void setSettingValue(String settingValue) {
		this.settingValue = settingValue;
	}
}
