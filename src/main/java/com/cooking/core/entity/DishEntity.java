package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 菜品表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@TableName("tbl_dish")
@Accessors(chain = true)
public class DishEntity extends BaseEntity {

    /**
     * 菜品名称
     */
    @TableField("name")
    private String name;

    /**
     * 菜品图片
     */
    @TableField("img_path")
    private String imgPath;

    /**
     * 菜品浏览量
     */
    @TableField("view_count")
    private Long viewCount;

    /**
     * 菜谱活跃值
     */
    @TableField("active_val")
    private Integer activeVal;

    /**
     * 菜谱人气值
     */
    @TableField("popular_val")
    private Integer popularVal;


}
