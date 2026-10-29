package com.itsm.itsm.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A simple key/value setting stored in the database, e.g.
// "assignment.strategy" = "SKILL". Lets the admin change behaviour without code changes.
@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    private String settingKey;

    private String settingValue;

    protected AppSetting() {
    }

    public AppSetting(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
}
