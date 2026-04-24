package eu.apps4net.parrotApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import eu.apps4net.parrotApp.configurations.Language;
import eu.apps4net.parrotApp.services.SettingService;

@SpringBootApplication
public class ParrotApp {
    private static SettingService settingService;

    public ParrotApp(SettingService settingService) {
        ParrotApp.settingService = settingService;
    }

    public static void main(String[] args)
    {
        SpringApplication.run(ParrotApp.class, args);

        Language.setActionsLanguage(settingService.getDefaultActionsLanguage());

        System.out.println("Run app on http://localhost:9999");

    }

}
