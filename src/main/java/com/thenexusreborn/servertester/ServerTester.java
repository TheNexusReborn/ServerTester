package com.thenexusreborn.servertester;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public final class ServerTester {
    public static void main(String[] args) {
        Path propertiesFile = Path.of("tester.properties");
        if (!Files.exists(propertiesFile)) {
            try {
                Files.createDirectories(propertiesFile.toAbsolutePath().getParent());
            } catch (IOException e) {
                System.err.println("Could not create directories for the tester.properties file.");
                return;
            }
            
            try (InputStream resourceStream = ServerTester.class.getResourceAsStream("tester.properties")) {
                if (resourceStream == null) {
                    System.err.println("Could not find a default tester.properties file. This is a bug");
                    return;
                }
                
                Files.copy(resourceStream, propertiesFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Created a default tester.properties file. Please change as needed");
            } catch (IOException e) {
                System.err.println("Error while copying the default properties file");
                e.printStackTrace();
            }
        }
        
        Properties properties = new Properties();
        try {
            properties.load(Files.newInputStream(propertiesFile));
            System.out.println("Loaded settings from the tester.properties file");
        } catch (IOException e) {
            System.err.println("Could not load properties from the tester.properties file");
            e.printStackTrace();
            return;
        }
        
        Path serverDir = Path.of(properties.getProperty("serverDir").replace("{sep}", File.separator).replace("{user.home}", System.getProperty("user.home")));
        Path nexusCoreDir = Path.of(properties.getProperty("nexusCoreDir").replace("{sep}", File.separator).replace("{user.home}", System.getProperty("user.home")));
        Path nexusHubDir = Path.of(properties.getProperty("nexusHubDir").replace("{sep}", File.separator).replace("{user.home}", System.getProperty("user.home")));
        Path nexusMapsDir = Path.of(properties.getProperty("nexusMapsDir").replace("{sep}", File.separator).replace("{user.home}", System.getProperty("user.home")));
        Path nexusSGDir = Path.of(properties.getProperty("nexusSGDir").replace("{sep}", File.separator).replace("{user.home}", System.getProperty("user.home")));
        Path starChatDir = Path.of(properties.getProperty("starChatDir").replace("{sep}", File.separator).replace("{user.home}", System.getProperty("user.home")));
        Path starCoreDir = Path.of(properties.getProperty("starCoreDir").replace("{sep}", File.separator).replace("{user.home}", System.getProperty("user.home")));
        Path starItemsDir = Path.of(properties.getProperty("starItemsDir").replace("{sep}", File.separator).replace("{user.home}", System.getProperty("user.home")));
        
        Path pluginsDir = FileSystems.getDefault().getPath(serverDir.toAbsolutePath().toString(), "plugins");
        Path oldNexusCoreFile = getPluginFile(pluginsDir, "NexusCore");
        Path oldNexusHubFile = getPluginFile(pluginsDir, "NexusHub");
        Path oldNexusMapsFile = getPluginFile(pluginsDir, "NexusMaps");
        Path oldNexusSGFile = getPluginFile(pluginsDir, "NexusSurvivalGames");
        Path oldStarChatFile = getPluginFile(pluginsDir, "StarChat");
        Path oldStarCoreFile = getPluginFile(pluginsDir, "StarCore");
        Path oldStarItemsFile = getPluginFile(pluginsDir, "StarItems");
        
        try {
            deleteIfExists(oldNexusCoreFile);
            deleteIfExists(oldNexusHubFile);
            deleteIfExists(oldNexusMapsFile);
            deleteIfExists(oldNexusSGFile);
            deleteIfExists(oldStarChatFile);
            deleteIfExists(oldStarCoreFile);
            deleteIfExists(oldStarItemsFile);
        } catch (Exception e) {
            System.err.println("Error while deleting the old files");
            e.printStackTrace();
            return;
        }
        
        Path newNexusCoreFile = getPluginFile(nexusCoreDir, "NexusCore");
        Path newNexusHubFile = getPluginFile(nexusHubDir, "NexusHub");
        Path newNexusMapsFile = getPluginFile(nexusMapsDir, "NexusMaps");
        Path newNexusSGFile = getPluginFile(nexusSGDir, "NexusSurvivalGames");
        Path newStarChatFile = getPluginFile(starChatDir, "StarChat");
        Path newStarCoreFile = getPluginFile(starCoreDir, "StarCore");
        Path newStarItemsFile = getPluginFile(starItemsDir, "StarItems");
        
        try {
            Files.copy(newNexusCoreFile, newPluginPath(pluginsDir, newNexusCoreFile), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newNexusHubFile, newPluginPath(pluginsDir, newNexusHubFile), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newNexusMapsFile, newPluginPath(pluginsDir, newNexusMapsFile), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newNexusSGFile, newPluginPath(pluginsDir, newNexusSGFile), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newStarChatFile, newPluginPath(pluginsDir, newStarChatFile), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newStarCoreFile, newPluginPath(pluginsDir, newStarCoreFile), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newStarItemsFile, newPluginPath(pluginsDir, newStarItemsFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Error while copying new files");
            e.printStackTrace();
            return;
        }
        
        String serverJarName = properties.getProperty("serverJarFile");
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xmx4G", "--enable-native-access=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", serverJarName, "nogui");
        processBuilder.directory(serverDir.toFile());
        processBuilder.inheritIO();
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            System.out.println("Server exited with exit code: " + exitCode);
        } catch (Exception e) {
            System.err.println("Error while trying to run the server");
            e.printStackTrace();
        }
    }
    
    public static void deleteIfExists(Path path) throws IOException {
        if (path == null) {
            return;
        }
        
        Files.deleteIfExists(path);
    }
    
    public static Path newPluginPath(Path destDir, Path file) {
        return FileSystems.getDefault().getPath(destDir.toAbsolutePath().toString(), file.getFileName().toString());
    }
    
    public static Path getPluginFile(Path dir, String name) {
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> results = stream.filter(p -> !Files.isDirectory(p)).filter(p -> {
                String fileName = p.getFileName().toString();
                int extensionIndex = fileName.lastIndexOf('.');
                if (extensionIndex <= 0) {
                    return false;
                }
                
                String extension = fileName.substring(extensionIndex);
                
                if (!extension.equalsIgnoreCase(".jar")) {
                    return false;
                }
                
                if (!fileName.startsWith(name)) {
                    return false;
                }
                
                if (fileName.contains("javadoc")) {
                    return false;
                }
                
                return !fileName.contains("sources");
            }).toList();
            
            if (!results.isEmpty()) {
                return results.getFirst();
            }
        } catch (IOException e) {
            System.err.println("Error while trying to find the plugin file " + name + " in " + dir.toAbsolutePath());
        }
        
        return null;
    }
}