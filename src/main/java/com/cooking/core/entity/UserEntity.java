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
 * 用户表
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
@TableName("tbl_user")
@Accessors(chain = true)
public class UserEntity extends BaseEntity {

    public static final Long super_admin_id = 1L;

    /**
     * 用户名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 用户账户
     */
    @TableField("user_code")
    private String userCode;

    /**
     * 用户密码
     */
    @TableField("user_pass")
    private String userPass;

    /**
     * 用户类型：1=普通用户，2=管理员用户
     */
    @TableField("type")
    private Integer type;

    /**
     * 用户状态：1=正常，2=待审核，3=禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 用户年龄
     */
    @TableField("age")
    private Integer age;

    /**
     * 用户性别
     */
    @TableField("gender")
    private Integer gender;

    /**
     * 用户身高
     */
    @TableField("stature")
    private Integer stature;

    /**
     * 用户体重，单位KG
     */
    @TableField("weight")
    private Integer weight;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;



}
