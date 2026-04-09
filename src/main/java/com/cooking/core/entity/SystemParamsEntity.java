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
 * 系统参数表
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
@TableName("tbl_system_params")
public class SystemParamsEntity extends BaseEntity {

    /**
     * 参数名
     */
    @TableField("param_name")
    private String paramName;

    /**
     * 参数值
     */
    @TableField("param_value")
    private String paramValue;
}
