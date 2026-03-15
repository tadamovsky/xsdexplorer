package com.xsdexplorer;

import java.io.IOException;
import java.time.Instant;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import com.xsdexplorer.uihelpers.Utils;

/**
 * Checks for new versions of XSD Explorer from GitHub releases.
 * Performs version check at most once per day using system preferences.
 */
public class VersionChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/tadamovsky/xsdexplorer/releases/latest";
    private static final String PREFS_KEY_LAST_CHECK = "last_version_check";
    private static final String PREFS_ENABLE_STARTUP_CHECK = "enable_startup_version_check";

    private static final long CHECK_INTERVAL_HOURS = 24;
    private static final Pattern VERSION_PATTERN = Pattern.compile("v(\\d+\\.\\d+\\.\\d+)");
    
    
    /**
     * Check for new version in background thread.
     * Returns true if a new version is available, false otherwise.
     */
    public void checkForNewVersionAsync(VersionCheckCallback callback) {
        if (shouldCheck()) {
            Thread checkThread = new Thread(() -> {
                try {
                        String latestVersion = fetchLatestVersion();
                        if (latestVersion != null && isNewerVersion(latestVersion)) {
                            callback.onNewVersionAvailable(latestVersion);
                        }
                        updateLastCheckTime();
                } catch (Exception e) {
                    System.err.println("Failed to check for new version: " + e.getMessage());
                }
            });
            checkThread.setDaemon(true);
            checkThread.start();
        }
    }
    
    /**
     * Check if enough time has passed since last version check.
     */
    private boolean shouldCheck() {
        Preferences prefs = Utils.getPreferences();
        boolean enabled = prefs.getBoolean(PREFS_ENABLE_STARTUP_CHECK, true);
        if (!enabled) {
            return false;
        }
        long lastCheck = prefs.getLong(PREFS_KEY_LAST_CHECK, 0);
        long currentTime = Instant.now().getEpochSecond();
        long hoursSinceLastCheck = (currentTime - lastCheck) / 3600;
        return hoursSinceLastCheck >= CHECK_INTERVAL_HOURS;
    }
    
    /**
     * Update the timestamp of last version check.
     */
    private void updateLastCheckTime() {
        Preferences prefs = Utils.getPreferences();
        prefs.putLong(PREFS_KEY_LAST_CHECK, Instant.now().getEpochSecond());
    }
    
    public static void setStartupCheckEnabled(boolean enabled) {
        Preferences prefs = Utils.getPreferences();
        prefs.putBoolean(PREFS_ENABLE_STARTUP_CHECK, enabled);
    }
    
    public static boolean isStartupCheckEnabled() {
        Preferences prefs = Utils.getPreferences();
        return prefs.getBoolean(PREFS_ENABLE_STARTUP_CHECK, true);
    }
    
    /**
     * Fetch the latest release version from GitHub API.
     */
    private String fetchLatestVersion() throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(GITHUB_API_URL);
            request.setHeader("User-Agent", "XsdExplorer");
            request.setHeader("Accept", "application/vnd.github.v3+json");
            
            return httpClient.execute(request, response -> {
                if (response.getCode() == 200) {
                    String body = new String(response.getEntity().getContent().readAllBytes());
                    //System.out.println("GitHub API response: " + body);
                    return extractVersionFromJson(body);
                }
                return null;
            });
        }
    }
    
    /**
     * Extract version tag from GitHub API JSON response.
     */
    private String extractVersionFromJson(String jsonResponse) {
        // Simple regex to extract tag_name value
        Pattern tagPattern = Pattern.compile("\"tag_name\"\s*:\s*\"([^\"]+)\"");
        Matcher matcher = tagPattern.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Compare versions in format v1.0.0.
     * Returns true if latestVersion is newer than currentVersion.
     */
    private boolean isNewerVersion(String latestVersion) {
        try {
            String currentVersionNum = Utils.getVersion();
            String latestVersionNum = extractVersionNumber(latestVersion);
            
            if (currentVersionNum == null || latestVersionNum == null) {
                return false;
            }
            
            return compareVersions(latestVersionNum, currentVersionNum) > 0;
        } catch (Exception e) {
            System.err.println("Failed to compare versions: "+ e);
            return false;
        }
    }
    
    /**
     * Extract version number from version string (e.g., "v1.0.2" -> "1.0.2").
     */
    private String extractVersionNumber(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Compare two semantic versions.
     * Returns positive if v1 > v2, negative if v1 < v2, 0 if equal.
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }
    
    /**
     * Callback interface for version check results.
     */
    public interface VersionCheckCallback {
        void onNewVersionAvailable(String newVersion);
    }
    
    @SuppressWarnings("resource")
    public static void main(String[] args) {
        VersionChecker checker = new VersionChecker();
        checker.checkForNewVersionAsync(newVersion -> {
            System.out.println("New version available: " + newVersion);
        });
        new java.util.Scanner(System.in).nextLine();
    }
}