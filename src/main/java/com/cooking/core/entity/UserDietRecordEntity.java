package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 用户饮食记录表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@Accessors(chain = true)
@TableName("tbl_user_diet_record")
public class UserDietRecordEntity extends BaseEntity {

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
     * 日期，格式：yyyy-MM-dd
     */
    @TableField("diet_date")
    private String dietDate;

    /**
     * 餐次：1=早餐，2=午餐，3=晚餐
     */
    @TableField("diet_order")
    private Integer dietOrder;

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
