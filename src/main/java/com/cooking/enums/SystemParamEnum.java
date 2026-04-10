package com.cooking.enums;

import lombok.Getter;

@Getter
public enum SystemParamEnum {

    maxUserLabels("用户标签最大数量", "3", "设置单个用户可绑定的标签数量上限。"),
    maxDishLabels("菜谱标签最大数量", "3", "设置单个菜品可绑定的标签数量上限。"),
    maxNutritionTargets("营养目标最大数量", "3", "设置单个用户可配置的营养目标数量上限。"),
    maxUserLoginCount("用户登录次数限制", "5", "设置用户最大尝试登录次数配置。"),

    ;
    public String name;
    public String value;
    public String desc;

    SystemParamEnum(String name, String value, String desc) {
        this.name = name;
        this.value = value;
        this.desc = desc;

    }

    public static SystemParamEnum getByName(String name) {
        for (SystemParamEnum item : SystemParamEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }

}
