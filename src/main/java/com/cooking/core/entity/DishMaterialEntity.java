package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;



/**
 * <p>
 * 菜品食材表
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
@TableName("tbl_dish_material")
public class DishMaterialEntity extends BaseEntity {

    /**
     * 菜品ID
     */
    @TableField("dish_id")
    private Long dishId;

    /**
     * 食材名称
     */
    @TableField("material_name")
    private String materialName;

    /**
     * 食材用量
     */
    @TableField("dosage")
    private String dosage;

    /**
     * 备注
     */
    @TableField("deal")
    private String deal;

}
