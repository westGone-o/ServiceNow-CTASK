//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.servicenow.ctaskcreator.service;

import com.servicenow.ctaskcreator.model.AppSettings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SettingsManager {
    private static final String SETTINGS_FILE = "app-settings.properties";

    public static void saveSettings(AppSettings settings) {
        Properties props = new Properties();
        props.setProperty("snow.id", settings.getSnowId() != null ? settings.getSnowId() : "");
        props.setProperty("snow.password", settings.getSnowPassword() != null ? settings.getSnowPassword() : "");
        props.setProperty("excel.path", settings.getExcelPath() != null ? settings.getExcelPath() : "");
        props.setProperty("assignment.group", settings.getAssignmentGroup() != null ? settings.getAssignmentGroup() : "");
        props.setProperty("assigned.person", settings.getAssignedPerson() != null ? settings.getAssignedPerson() : "");

        try (FileOutputStream out = new FileOutputStream("app-settings.properties")) {
            props.store(out, "Snow Task Creator Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static AppSettings loadSettings() {
        AppSettings settings = new AppSettings();
        File file = new File("app-settings.properties");
        if (!file.exists()) {
            return settings;
        } else {
            Properties props = new Properties();

            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
                settings.setSnowId(props.getProperty("snow.id", ""));
                settings.setSnowPassword(props.getProperty("snow.password", ""));
                settings.setExcelPath(props.getProperty("excel.path", ""));
                settings.setAssignmentGroup(props.getProperty("assignment.group", ""));
                settings.setAssignedPerson(props.getProperty("assigned.person", ""));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return settings;
        }
    }
}
