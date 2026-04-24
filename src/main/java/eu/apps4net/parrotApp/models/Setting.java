package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;

@Entity(name = "Setting")
@Table(name = "setting")
public class Setting {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "setting_name", nullable = false)
	private String settingName;

	@Column(name = "setting_value")
	private String settingValue;

	public Setting() {
	}

	public Setting(String settingName, String settingValue) {
		this.settingName = settingName;
		this.settingValue = settingValue;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSettingName() {
		return settingName;
	}

	public void setSettingName(String settingName) {
		this.settingName = settingName;
	}

	public String getSettingValue() {
		return settingValue;
	}

	public void setSettingValue(String settingValue) {
		this.settingValue = settingValue;
	}
}
