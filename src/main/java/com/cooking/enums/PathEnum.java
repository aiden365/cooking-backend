package com.cooking.enums;

import lombok.Getter;

/**
 * 文件路径枚举
 */
@Getter
public enum PathEnum {

    dish_img_path("/UploadFile/Dish", "菜品相关文件存放路径"),
    user_share_path("/UploadFile/Share", "用户分享相关文件存放路径"),
    repo_path("/UploadFile/Repo", "知识库相关文件存放路径"),
    ;
    private String value;
    private String name;

    PathEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }
}
