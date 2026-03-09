package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 用户菜品收藏表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@Accessors(chain = true)
@TableName("tbl_user_dish_collect")
public class UserDishCollectEntity extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 菜品ID
     */
    @TableField("dish_id")
    private Long dishId;

    /**
     * 用户名
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 菜品名
     */
    @TableField(exist = false)
    private String dishName;
}
