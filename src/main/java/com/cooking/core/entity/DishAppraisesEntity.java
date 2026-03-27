package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;


/**
 * <p>
 * 菜品评价表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@TableName("tbl_dish_appraises")
public class DishAppraisesEntity extends BaseEntity {

    /**
     * 菜品ID
     */
    @TableField("dish_id")
    private Long dishId;

    /**
     * 评价用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 操作性评分
     */
    @TableField("manipulation_score")
    private Integer manipulationScore;

    /**
     * 匹配度评分
     */
    @TableField("equal_score")
    private Integer equalScore;

    /**
     * 满意度评分
     */
    @TableField("satisfaction_score")
    private Integer satisfactionScore;
}
