package com.cooking.enums;

public enum SystemParamEnum {

    user_nutrition_limit("用户营养目标限制", "3"),
    ;
    public String description;
    public String defaultValue;

    SystemParamEnum(String description, String defaultValue) {
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public static SystemParamEnum getByName(String name) {
        for (SystemParamEnum item : SystemParamEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
