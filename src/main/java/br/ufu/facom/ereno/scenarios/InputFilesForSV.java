package br.ufu.facom.ereno.scenarios;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class InputFilesForSV {
    private static final Properties config = new Properties();

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        try (InputStream input = InputFilesForSV.class.getClassLoader().getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException("config.properties not found in classpath");
            }
            config.load(input);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    /**
     * Gets all electrical source files using parallel stream for better performance
     *
     * @return Array of file paths
     */
    public static String[] getElectricalSourceFiles() {
        String rootDir = config.getProperty("root.directory");
        String fileExtension = config.getProperty("file.extension", ".out");
        int maxDepth = Integer.parseInt(config.getProperty("max.depth", "2147483647"));

        if (rootDir == null || rootDir.trim().isEmpty()) {
            throw new RuntimeException("root.directory not specified in config.properties");
        }

        // Using ConcurrentLinkedQueue for thread-safe collection in parallel stream
        ConcurrentLinkedQueue<String> fileQueue = new ConcurrentLinkedQueue<>();

        try (Stream<Path> pathStream = Files.walk(Paths.get(rootDir), maxDepth)) {
            pathStream.parallel()  // Enable parallel processing
                    .filter(Files::isRegularFile).filter(p -> p.toString().toLowerCase().endsWith(fileExtension.toLowerCase())).forEach(p -> fileQueue.add(p.toString()));
        } catch (IOException e) {
            throw new RuntimeException("Error reading files from directory: " + rootDir, e);
        }

        // Convert to array and sort (still needed as parallel processing doesn't guarantee order)
        return fileQueue.stream().sorted().toArray(String[]::new);
    }

    // Helper method to get configuration
    public static String getConfig(String key) {
        return config.getProperty(key);
    }

    // Helper method to get configuration with default value
    public static String getConfig(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
}