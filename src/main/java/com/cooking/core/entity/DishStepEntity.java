package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;


/**
 * <p>
 * 菜品制作步骤表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@TableName("tbl_dish_step")
public class DishStepEntity extends BaseEntity {

    /**
     * 菜谱ID
     */
    @TableField("dish_id")
    private Long dishId;

    /**
     * 步骤描述
     */
    @TableField("step_describe")
    private String stepDescribe;

    /**
     * 步骤图示，以JSON数组格式存储，如：["图片1","图片2","图片3"]
     */
    @TableField("step_images")
    private String stepImages;

    /**
     * 步骤序号
     */
    @TableField("sort")
    private Integer sort;
}
