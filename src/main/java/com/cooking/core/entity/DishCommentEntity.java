package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.util.List;


/**
 * <p>
 * 菜品评论表
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
@TableName("tbl_dish_comment")
public class DishCommentEntity extends BaseEntity {

    /**
     * 菜品ID
     */
    @TableField("dish_id")
    private Long dishId;

    /**
     * 评论用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 上级评论ID，默认0表示最顶级评论
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 评论内容
     */
    @TableField("content")
    private String content;

    /**
     * 评论用户ID
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 子评论列表
     */
    @TableField(exist = false)
    private List<DishCommentEntity> childCommentList;

}
