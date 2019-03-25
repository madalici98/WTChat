package WTServer.data;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private Properties configFile;

    public Config(String file) throws IOException {

        configFile = new Properties();
        configFile.load(new FileInputStream(file));
    }

    public String getProperty(String propName) {

        return configFile.getProperty(propName);
    }
}
