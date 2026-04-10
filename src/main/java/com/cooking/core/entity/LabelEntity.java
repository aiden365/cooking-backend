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
 * 用户标签表
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
@TableName("tbl_label")
@Accessors(chain = true)
public class LabelEntity extends BaseEntity {

    /**
     * 标签名
     */
    @TableField("label_name")
    private String labelName;

    /**
     * 标签类型：1=用户标签，2=菜品标签
     */
    @TableField("type")
    private Integer type;
}
