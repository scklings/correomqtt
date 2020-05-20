package org.correomqtt.business.services;

import org.correomqtt.business.dispatcher.ConfigDispatcher;
import org.correomqtt.business.model.*;
import org.correomqtt.business.utils.ConnectionHolder;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.correomqtt.gui.theme.light.LightThemeProvider;
import org.correomqtt.gui.theme.ThemeProvider;
import org.correomqtt.plugin.manager.PluginManager;
import org.correomqtt.plugin.spi.ThemeProviderHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//TODO check invalid configs

public class SettingsService extends BaseUserFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsService.class);

    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String CSS_FILE_NAME = "style.css";
    private static final String EX_MSG_PREPARE_CONFIG = "Exception preparing config file.";
    private static final String EX_MSG_WRITE_CONFIG = "Exception writing config file.";

    private ThemeProvider activeThemeProvider;

    private static SettingsService instance = null;

    private ConfigDTO configDTO;

    private SettingsService() {

        try {
            prepareFile(CONFIG_FILE_NAME);
        } catch (InvalidPathException e) {
            LOGGER.error(EX_MSG_PREPARE_CONFIG, e);
            ConfigDispatcher.getInstance().onInvalidPath();
        } catch (FileAlreadyExistsException e) {
            LOGGER.error(EX_MSG_PREPARE_CONFIG, e);
            ConfigDispatcher.getInstance().onFileAlreadyExists();
        } catch (DirectoryNotEmptyException e) {
            LOGGER.error(EX_MSG_PREPARE_CONFIG, e);
            ConfigDispatcher.getInstance().onConfigDirectoryEmpty();
        } catch (SecurityException | AccessDeniedException e) {
            LOGGER.error(EX_MSG_PREPARE_CONFIG, e);
            ConfigDispatcher.getInstance().onConfigDirectoryNotAccessible();
        } catch (UnsupportedOperationException | IOException e) {
            LOGGER.error(EX_MSG_PREPARE_CONFIG, e);
            ConfigDispatcher.getInstance().onConfigPrepareFailure();
        }

        try {
            configDTO = new ObjectMapper().readValue(getFile(), ConfigDTO.class);
        } catch (IOException e) {
            LOGGER.error("Exception parsing config file.", e);
            ConfigDispatcher.getInstance().onInvalidJsonFormat();
        }
    }

    public static synchronized SettingsService getInstance() {
        if (instance == null) {
            instance = new SettingsService();
            return instance;
        } else {
            return instance;
        }
    }

    private ThemeProvider getActiveTheme() {
        if(activeThemeProvider == null) {
            String activeThemeName = configDTO.getThemesSettings().getActiveTheme().getName();
            ArrayList<ThemeProvider> themes = new ArrayList<>(PluginManager.getInstance().getExtensions(ThemeProviderHook.class));
            activeThemeProvider = themes.stream().filter(t -> t.getName().equals(activeThemeName)).findFirst().orElse(new LightThemeProvider());
        }
        return activeThemeProvider;
    }

    public List<ConnectionConfigDTO> getConnectionConfigs() {
        return configDTO.getConnections();
    }

    public SettingsDTO getSettings() {
        return configDTO.getSettings();
    }

    public ThemeSettingsDTO getThemeSettings() {
        return configDTO.getThemesSettings();
    }

    public void saveSettings() {
        this.activeThemeProvider = null;
        saveDTO();
        saveToUserDirectory(CSS_FILE_NAME, getActiveTheme().getCss());
        ConfigDispatcher.getInstance().onSettingsUpdated();
    }

    public void saveConnections(List<ConnectionConfigDTO> connections) {
        configDTO.setConnections(connections);
        saveDTO();
        ConnectionHolder.getInstance().refresh();
        ConfigDispatcher.getInstance().onConnectionsUpdated();
    }

    private void saveDTO() {

        try {
            new ObjectMapper().writeValue(getFile(), configDTO);
        } catch (FileNotFoundException e) {
            LOGGER.error(EX_MSG_WRITE_CONFIG, e);
            ConfigDispatcher.getInstance().onConfigDirectoryEmpty();
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error(EX_MSG_WRITE_CONFIG, e);
            ConfigDispatcher.getInstance().onInvalidJsonFormat();
        } catch (IOException e) {
            LOGGER.error(EX_MSG_WRITE_CONFIG, e);
            ConfigDispatcher.getInstance().onSavingFailed();
        }
    }

    public String getCssPath() {
        File cssFile = new File(getTargetDirectoryPath() + File.separator + CSS_FILE_NAME);
        if(!cssFile.exists()) {
            saveToUserDirectory(CSS_FILE_NAME, getActiveTheme().getCss());
        }
        if (cssFile.exists()) {
            return cssFile.toURI().toString();
        } else {
            return null;
        }
    }

    public String getLogPath() {
        return getTargetDirectoryPath() + File.separator;
    }

    public String getIconModeCssClass() {
        return configDTO.getThemesSettings().getActiveTheme().getIconMode().toString();
    }
}