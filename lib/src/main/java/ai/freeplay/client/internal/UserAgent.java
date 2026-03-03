package ai.freeplay.client.internal;

public class UserAgent {

    public static String getUserAgent() {
        String sdkName = "Freeplay";
        Package packageInfo = UserAgent.class.getPackage();
        // Get the implementation version
        String sdkVersion = packageInfo.getImplementationVersion();

        String language = "JVM";
        String languageVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");

        return String.format("%s/%s (%s/%s; %s/%s)", sdkName, sdkVersion, language, languageVersion, osName, osVersion);
    }
}
