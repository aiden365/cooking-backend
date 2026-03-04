package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 菜品标签关联表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@Accessors(chain = true)
@TableName("tbl_dish_lable_rel")
public class DishLableRelEntity extends BaseEntity {

    /**
     * 菜品ID
     */
    @TableField("dish_id")
    private Long dishId;

    /**
     * 标签ID
     */
    @TableField("lable_id")
    private Long lableId;
}
