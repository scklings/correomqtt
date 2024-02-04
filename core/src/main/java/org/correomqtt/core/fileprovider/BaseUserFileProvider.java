package org.correomqtt.core.fileprovider;

import org.correomqtt.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseUserFileProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseUserFileProvider.class);

    private static final String OPERATING_SYSTEM = System.getProperty("os.name").toLowerCase();
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String MAC_APP_FOLDER_NAME = "CorreoMqtt";
    private static final String WIN_APP_FOLDER_NAME = MAC_APP_FOLDER_NAME;
    private static final String LIN_APP_FOLDER_NAME = ".correomqtt";

    private static final String LOG_FOLDER_NAME = "logs";

    private static final String SCRIPT_FOLDER_NAME = "scripts";

    protected static final String SCRIPT_LOG_FOLDER_NAME = SCRIPT_FOLDER_NAME + File.separator + "logs";

    protected static final String SCRIPT_EXECUTIONS_FOLDER_NAME = SCRIPT_FOLDER_NAME + File.separator + "executions";
    private final Map<String, String> cache = new HashMap<>();
    protected final EventBus eventBus;
    private File file;
    private String targetDirectoryPathCache;

    protected BaseUserFileProvider(EventBus eventBus){

        this.eventBus = eventBus;
    }

    protected File getFile() {
        return file;
    }

    protected void prepareFile(String hookFile) throws IOException {
        prepareFile(null, hookFile);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void prepareFile(String id, String filename) throws IOException {

        String targetDirectoryPath = getTargetDirectoryPath();

        if (!new File(targetDirectoryPath).exists() && !new File(targetDirectoryPath).mkdir()) {
            eventBus.fire(new DirectoryCanNotBeCreatedEvent(targetDirectoryPath));
        }

        File targetFile;
        if (id == null) {
            targetFile = new File(targetDirectoryPath + File.separator + filename);
        } else {
            targetFile = new File(targetDirectoryPath + File.separator + id + "_" + filename);
        }

        if (!targetFile.exists()) {
            try (InputStream inputStream = BaseUserFileProvider.class.getResourceAsStream(filename)) {
                if (inputStream != null) {
                    byte[] buffer = new byte[inputStream.available()];
                    if (inputStream.read(buffer) > 0) {
                        try (OutputStream outStream = new FileOutputStream(targetFile)) {
                            outStream.write(buffer);
                        }
                    }
                } else {
                    LOGGER.warn("Can not read file {}", filename);
                }
            }
        }

        this.file = targetFile;
    }

    public String getTargetDirectoryPath() {

        if (targetDirectoryPathCache != null) {
            return targetDirectoryPathCache;
        }

        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                eventBus.fire(new WindowsAppDataNullEvent());
            } else {
                targetDirectoryPathCache = appData + File.separator + WIN_APP_FOLDER_NAME;
            }
        } else if (isMacOS()) {

            if (USER_HOME == null) {
                eventBus.fire(new UserHomeNull());
            } else {
                targetDirectoryPathCache = USER_HOME + File.separator + "Library" + File.separator + "Application Support" + File.separator + MAC_APP_FOLDER_NAME;
            }
        } else if (isLinux()) {

            if (USER_HOME == null) {
                eventBus.fire(new UserHomeNull());
            } else {
                targetDirectoryPathCache = USER_HOME + File.separator + LIN_APP_FOLDER_NAME;
            }
        } else {
            LOGGER.warn("User directory can not be found. Using working directory.");
            targetDirectoryPathCache = USER_DIR;
        }

        return targetDirectoryPathCache;
    }

    public boolean isWindows() {
        return OPERATING_SYSTEM.startsWith("windows");
    }

    public boolean isMacOS() {
        return OPERATING_SYSTEM.contains("mac os");
    }

    public boolean isLinux() {
        return OPERATING_SYSTEM.contains("linux")
                || OPERATING_SYSTEM.contains("mpe/ix")
                || OPERATING_SYSTEM.contains("freebsd")
                || OPERATING_SYSTEM.contains("irix")
                || OPERATING_SYSTEM.contains("digital unix")
                || OPERATING_SYSTEM.contains("unix");
    }

    protected void saveToUserDirectory(String filename, String content) {

        String targetDirectoryPath = getTargetDirectoryPath();
        if (!new File(targetDirectoryPath).exists() && !new File(targetDirectoryPath).mkdir()) {
            eventBus.fire(new DirectoryCanNotBeCreatedEvent(targetDirectoryPath));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetDirectoryPath + File.separator + filename))) {
            writer.write(content);
        } catch (IOException e) {
            LOGGER.warn("Error writing file {}", filename, e);
        }
    }

    public String getLogDirectory() {
        return getFromCache(LOG_FOLDER_NAME);
    }

    protected String getFromCache(String dir) {
        return getFromCache(dir, true);
    }

    protected String getFromCache(String dir, boolean autocreate) {
        String path = getTargetDirectoryPath() + File.separator + dir;

        if (!autocreate)
            return path;

        return cache.computeIfAbsent(dir, d -> {
            if (!new File(path).exists() && !new File(path).mkdirs()) {
                eventBus.fire(new DirectoryCanNotBeCreatedEvent(path));
                throw new IllegalStateException("Can not create directory: " + path);
            }
            return path;
        });
    }

    public String getScriptLogDirectory(String filename) {
        return getScriptLogDirectory(filename, true);
    }

    public String getScriptLogDirectory(String filename, boolean autocreate) {
        return getFromCache(SCRIPT_LOG_FOLDER_NAME + File.separator + filename, autocreate);
    }

    public String getScriptExecutionsDirectory(String filename) {
        return getScriptExecutionsDirectory(filename, true);
    }

    public String getScriptExecutionsDirectory(String filename, boolean autocreate) {
        return getFromCache(SCRIPT_EXECUTIONS_FOLDER_NAME + File.separator + filename, autocreate);
    }
}