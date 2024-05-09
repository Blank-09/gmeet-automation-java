package com.automation.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesUtil {

    private static final String PROPERTIES_FILE_PATH = "./src/test/resources/config.properties";

    private Properties properties;
    private String propertiesFilePath;

    public PropertiesUtil() {
        this(PROPERTIES_FILE_PATH);
    }

    public PropertiesUtil(String propertiesFilePath) {
        this.propertiesFilePath = propertiesFilePath;
        this.properties = new Properties();

        try (FileInputStream inputStream = new FileInputStream(propertiesFilePath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) throws IOException {
        properties.setProperty(key, value);
        try (FileOutputStream outputStream = new FileOutputStream(propertiesFilePath)) {
            properties.store(outputStream, null);
        }
    }
}
