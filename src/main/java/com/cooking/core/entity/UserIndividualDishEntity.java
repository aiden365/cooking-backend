package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

/**
 * <p>
 * 用户个性化菜谱
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@TableName("tbl_user_individual_dish")
public class UserIndividualDishEntity extends BaseEntity {

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
     * 菜谱内容
     */
    @TableField("content")
    private String content;

    @TableField(exist = false)
    private String dishName;

    @TableField(exist = false)
    private String dishImg;
}
