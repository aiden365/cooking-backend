package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 营养表
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tbl_nutrition")
public class NutritionEntity extends BaseEntity {

    /**
     * 营养名称
     */
    @TableField("name")
    private String name;

    /**
     * 默认值
     */
    @TableField("default_value")
    private String defaultValue;
}
