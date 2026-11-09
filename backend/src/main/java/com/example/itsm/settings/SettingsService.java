package com.example.itsm.settings;

import com.example.itsm.incident.assignment.AssignmentStrategyType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    public static final String ASSIGNMENT_STRATEGY = "assignment.strategy";
    public static final String AUTO_ASSIGN = "assignment.auto-assign";

    private final SystemSettingRepository settingRepository;

    public SettingsService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        return new SettingsResponse(assignmentStrategy(), autoAssign());
    }

    @Transactional(readOnly = true)
    public AssignmentStrategyType assignmentStrategy() {
        String value = value(ASSIGNMENT_STRATEGY, AssignmentStrategyType.SKILL_BASED.name());
        try {
            return AssignmentStrategyType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return AssignmentStrategyType.SKILL_BASED;
        }
    }

    @Transactional(readOnly = true)
    public boolean autoAssign() {
        return Boolean.parseBoolean(value(AUTO_ASSIGN, "true"));
    }

    @Transactional
    public SettingsResponse update(SettingsRequest request) {
        if (request.assignmentStrategy() != null) {
            set(ASSIGNMENT_STRATEGY, request.assignmentStrategy().name());
        }
        if (request.autoAssign() != null) {
            set(AUTO_ASSIGN, request.autoAssign().toString());
        }
        return getSettings();
    }

    @Transactional
    public void set(String key, String value) {
        SystemSetting setting = settingRepository.findById(key)
                .orElseGet(() -> new SystemSetting(key, value));
        setting.setValue(value);
        settingRepository.save(setting);
    }

    private String value(String key, String fallback) {
        return settingRepository.findById(key)
                .map(SystemSetting::getValue)
                .orElse(fallback);
    }
}
