package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 用户营养标准表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@Accessors(chain = true)
@TableName("tbl_user_nutrition")
public class UserNutritionEntity extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 营养名称
     */
    @TableField("name")
    private String name;

    /**
     * 目标值
     */
    @TableField("aim_value")
    private String aimValue;
}
