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

import java.util.Date;
import java.util.List;


/**
 * <p>
 * 菜品表
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
@TableName("tbl_dish")
@Accessors(chain = true)
public class DishEntity extends BaseEntity {

    /**
     * 菜品名称
     */
    @TableField("name")
    private String name;



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
     * 检查时间
     */
    @TableField("check_time")
    private Date checkTime;




    /**
     * 菜品浏览量
     */
    @TableField("view_count")
    private Long viewCount;

    /**
     * 菜谱活跃值, 菜谱收到评价会增加
     */
    @TableField("active_val")
    private Integer activeVal;

    /**
     * 菜谱人气值, 菜谱收到分享会增加
     */
    @TableField("popular_val")
    private Integer popularVal;

    /**
     * 菜谱总评分, 默认0表示未打分
     */
    @TableField("total_score")
    private Double totalScore;

    /**
     * 菜品图片
     */
    @TableField("img_path")
    private String imgPath;

    /**
     * 菜品制作视频
     */
    @TableField("video_path")
    private String videoPath;

    /**
     * 烹饪小贴士
     */
    @TableField("tips")
    private String tips;



    /**
     * 菜谱收藏量
     */
    @TableField(exist = false)
    private Long collectCount;

    /**
     * 菜谱分享量
     */
    @TableField(exist = false)
    private Long shareCount;

    /**
     * 当前用户是否已收藏
     */
    @TableField(exist = false)
    private Boolean userCollected;

    /**
     * 调料数量
     */
    @TableField(exist = false)
    private Integer flavorCount;

    /**
     * 食材数量
     */
    @TableField(exist = false)
    private Integer materialCount;

    /**
     * 制作步骤数量
     */
    @TableField(exist = false)
    private Integer stepCount;

    /**
     * 标签列表
     */
    @TableField(exist = false)
    private List<String> labelNames;

    /**
     * 调料列表
     */
    @TableField(exist = false)
    private List<DishFlavorEntity> flavorList;

    /**
     * 食材列表
     */
    @TableField(exist = false)
    private List<DishMaterialEntity> materialList;

    /**
     * 制作步骤列表
     */
    @TableField(exist = false)
    private List<DishStepEntity> stepList;
}
