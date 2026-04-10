package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;


/**
 * <p>
 * 用户菜品分享表
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@TableName("tbl_user_share")
public class UserShareEntity extends BaseEntity {

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
     * 菜品图片
     */
    @TableField("dish_img")
    private String dishImg;

    /**
     * 分享描述
     */
    @TableField("description")
    private String description;

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
