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
     * 烹饪小贴士
     */
    @TableField("tips")
    private String tips;

    /**
     * 预计用时
     */
    @TableField("take_times")
    private String takeTimes;

    /**
     * 来源：1=手动添加，2=AI生成
     */
    @TableField("source_type")
    private Integer sourceType;

    /**
     * 检查状态：1=未经人工检验，2=经过人工检验
     */
    @TableField("check_status")
    private Integer checkStatus;



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
