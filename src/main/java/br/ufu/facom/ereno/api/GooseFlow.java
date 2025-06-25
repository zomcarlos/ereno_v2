package br.ufu.facom.ereno.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class GooseFlow {
    // Configuration parameters
    public static String goID;
    public static int numberOfMessages;
    public static String ethSrc;
    public static String ethDst;
    public static String ethType;
    public static String gooseAppid;
    public static String TPID;
    public static boolean ndsCom;
    public static boolean test;
    public static boolean cbstatus;

    private static final String CONFIG_FILE = "params.properties";
    private static final Properties props = new Properties();

    static {
        loadConfigs();
    }

    /**
     * Loads configuration from params.properties file
     */
    public static void loadConfigs() {
        try (InputStream input = GooseFlow.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new RuntimeException(CONFIG_FILE + " not found in classpath");
            }

            props.load(input);

            // Map properties to static fields
            goID = props.getProperty("goose.flow.goID");
            numberOfMessages = Integer.parseInt(props.getProperty("goose.flow.numberOfMessages", "0"));
            ethSrc = props.getProperty("goose.flow.ethSrc");
            ethDst = props.getProperty("goose.flow.ethDst");
            ethType = props.getProperty("goose.flow.ethType");
            gooseAppid = props.getProperty("goose.flow.gooseAppid");
            TPID = props.getProperty("goose.flow.TPID");
            ndsCom = Boolean.parseBoolean(props.getProperty("goose.flow.ndsCom", "false"));
            test = Boolean.parseBoolean(props.getProperty("goose.flow.test", "false"));
            cbstatus = Boolean.parseBoolean(props.getProperty("goose.flow.cbstatus", "false"));

        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration from " + CONFIG_FILE, e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format in configuration", e);
        }
    }

    /**
     * Saves current configuration to params.properties file
     */
    public static void saveConfigs() {
        try {
            // Update properties with current values
            props.setProperty("goose.flow.goID", goID != null ? goID : "");
            props.setProperty("goose.flow.numberOfMessages", String.valueOf(numberOfMessages));
            props.setProperty("goose.flow.ethSrc", ethSrc != null ? ethSrc : "");
            props.setProperty("goose.flow.ethDst", ethDst != null ? ethDst : "");
            props.setProperty("goose.flow.ethType", ethType != null ? ethType : "");
            props.setProperty("goose.flow.gooseAppid", gooseAppid != null ? gooseAppid : "");
            props.setProperty("goose.flow.TPID", TPID != null ? TPID : "");
            props.setProperty("goose.flow.ndsCom", String.valueOf(ndsCom));
            props.setProperty("goose.flow.test", String.valueOf(test));
            props.setProperty("goose.flow.cbstatus", String.valueOf(cbstatus));

            // Get the path to the properties file in the same directory as config.properties
            Path configPath = Paths.get(System.getProperty("user.dir"),
                    "src", "main", "resources", CONFIG_FILE);

            // Write the properties to file
            try (var writer = Files.newBufferedWriter(configPath)) {
                props.store(writer, "Goose Flow Configuration");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration to " + CONFIG_FILE, e);
        }
    }
}