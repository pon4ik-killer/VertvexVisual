// SettingsManager.java
package dev.sxmurxy.vertexvisual.settings;

import java.util.HashMap;
import java.util.Map;

public class SettingsManager {
    private static final SettingsManager INSTANCE = new SettingsManager();

    private final Map<String, Boolean> booleanSettings = new HashMap<>();
    private final Map<String, Integer> intSettings = new HashMap<>();
    private final Map<String, String> stringSettings = new HashMap<>();

    // Ключи настроек
    public static final String REMOVE_EXTERNAL_GLOW = "remove_external_glow";
    public static final String HIDE_UID = "hide_uid";
    public static final String STREAMER_MODE = "streamer_mode";
    public static final String DISABLE_ALL_MODS = "disable_all_mods";

    private SettingsManager() {
        // Значения по умолчанию
        booleanSettings.put(REMOVE_EXTERNAL_GLOW, false);
        booleanSettings.put(HIDE_UID, false);
        booleanSettings.put(STREAMER_MODE, false);
        booleanSettings.put(DISABLE_ALL_MODS, false);
    }

    public static SettingsManager getInstance() {
        return INSTANCE;
    }

    public boolean getBoolean(String key) {
        return booleanSettings.getOrDefault(key, false);
    }

    public void setBoolean(String key, boolean value) {
        booleanSettings.put(key, value);
        applySetting(key, value);
    }

    public void toggleBoolean(String key) {
        boolean current = getBoolean(key);
        setBoolean(key, !current);
    }

    private void applySetting(String key, boolean value) {
        switch (key) {
            case REMOVE_EXTERNAL_GLOW:
                // Логика отключения внешнего свечения
                break;
            case HIDE_UID:
                // Логика скрытия UID
                break;
            case STREAMER_MODE:
                // Активация режима стримера (может включать несколько настроек сразу)
                if (value) {
                    setBoolean(HIDE_UID, true);
                }
                break;
            case DISABLE_ALL_MODS:
                // Отключение всех модов
                if (value) {
                    // Здесь можно добавить логику отключения функциональности модов
                }
                break;
        }
    }

    // Получить все настройки для отображения в меню
    public Map<String, Boolean> getAllBooleanSettings() {
        return new HashMap<>(booleanSettings);
    }
}