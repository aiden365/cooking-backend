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
 * 菜品调料表
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@TableName("tbl_dish_flavors")
public class DishFlavorEntity extends BaseEntity {

    @TableField("dish_id")
    private Long dishId;

    @TableField("flavor_name")
    private String flavorName;

    @TableField("dosage")
    private String dosage;
}
