package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;


/**
 * <p>
 * 菜品标签关联表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@TableName("tbl_dish_label_rel")
public class DishLabelRelEntity extends BaseEntity {

    /**
     * 菜品ID
     */
    @TableField("dish_id")
    private Long dishId;

    /**
     * 标签ID
     */
    @TableField("label_id")
    private Long labelId;

    /**
     * 标签名
     */
    @TableField(exist = false)
    private String labelName;


}
