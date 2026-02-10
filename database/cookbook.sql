/*
 Navicat Premium Data Transfer

 Source Server         : 本机
 Source Server Type    : MySQL
 Source Server Version : 50710 (5.7.10)
 Source Host           : localhost:3306
 Source Schema         : cookbook

 Target Server Type    : MySQL
 Target Server Version : 50710 (5.7.10)
 File Encoding         : 65001

 Date: 10/02/2026 18:39:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tbl_dish
-- ----------------------------
DROP TABLE IF EXISTS `tbl_dish`;
CREATE TABLE `tbl_dish`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜品名称',
  `img_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜品图片',
  `view_count` bigint(20) NOT NULL COMMENT '菜品浏览量',
  `active_val` int(11) NOT NULL COMMENT '菜谱活跃值',
  `popular_val` int(11) NOT NULL COMMENT '菜谱人气值',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_dish_appraises
-- ----------------------------
DROP TABLE IF EXISTS `tbl_dish_appraises`;
CREATE TABLE `tbl_dish_appraises`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `user_id` bigint(20) NOT NULL COMMENT '评价用户ID',
  `manipulation_score` int(11) NOT NULL COMMENT '操作性评分',
  `equal_score` int(11) NOT NULL COMMENT '匹配度评分',
  `satisfaction_score` int(11) NOT NULL COMMENT '满意度评分',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品评价表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_dish_comment
-- ----------------------------
DROP TABLE IF EXISTS `tbl_dish_comment`;
CREATE TABLE `tbl_dish_comment`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `user_id` bigint(20) NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint(20) NOT NULL COMMENT '上级评论ID，默认0表示最顶级评论',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论内容',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_dish_lable_rel
-- ----------------------------
DROP TABLE IF EXISTS `tbl_dish_lable_rel`;
CREATE TABLE `tbl_dish_lable_rel`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dish_id` bigint(255) NOT NULL COMMENT '菜品ID',
  `lable_id` bigint(20) NOT NULL COMMENT '标签ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_dish_material
-- ----------------------------
DROP TABLE IF EXISTS `tbl_dish_material`;
CREATE TABLE `tbl_dish_material`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `material_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '食材名称',
  `dosage` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '食材用量',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品食材表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_dish_step
-- ----------------------------
DROP TABLE IF EXISTS `tbl_dish_step`;
CREATE TABLE `tbl_dish_step`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜谱ID',
  `step_describe` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '步骤描述',
  `step_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '步骤图示',
  `sort` int(11) NOT NULL COMMENT '步骤序号',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品制作步骤表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_label
-- ----------------------------
DROP TABLE IF EXISTS `tbl_label`;
CREATE TABLE `tbl_label`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `lable_name` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签名',
  `type` int(11) NOT NULL COMMENT '标签类型：1=用户标签，2=菜品标签',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_user` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `update_user` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  `deleted` bit(1) NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_repository
-- ----------------------------
DROP TABLE IF EXISTS `tbl_repository`;
CREATE TABLE `tbl_repository`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件地址',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_user` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `update_user` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  `deleted` bit(1) NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_system_params
-- ----------------------------
DROP TABLE IF EXISTS `tbl_system_params`;
CREATE TABLE `tbl_system_params`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `param_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参数名',
  `param_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '参数值',
  `create_user` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `update_user` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  `deleted` bit(1) NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统参数表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_user
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user`;
CREATE TABLE `tbl_user`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `user_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账户',
  `user_pass` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户密码',
  `type` int(11) NOT NULL COMMENT '用户类型：1=普通用户，2=管理员用户',
  `age` int(11) NULL DEFAULT NULL COMMENT '用户年龄',
  `gender` int(11) NULL DEFAULT NULL COMMENT '用户性别',
  `stature` int(11) NULL DEFAULT NULL COMMENT '用户身高',
  `weight` int(11) NULL DEFAULT NULL COMMENT '用户体重，单位KG',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_user_diet_record
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_diet_record`;
CREATE TABLE `tbl_user_diet_record`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `diet_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日期',
  `diet_order` int(11) NOT NULL COMMENT '餐次：1=早餐，2=午餐，3=晚餐',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户饮食记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_user_dish_collect
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_dish_collect`;
CREATE TABLE `tbl_user_dish_collect`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户菜品收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_user_label_rel
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_label_rel`;
CREATE TABLE `tbl_user_label_rel`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(255) NOT NULL COMMENT '用户ID',
  `lable_id` bigint(20) NOT NULL COMMENT '标签ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_user_nutrition
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_nutrition`;
CREATE TABLE `tbl_user_nutrition`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '营养名称',
  `aim_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标值',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户营养标准表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_user_share
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_share`;
CREATE TABLE `tbl_user_share`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `dish_img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜品图片',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享描述',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户菜品分享表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_user_share_comment
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_share_comment`;
CREATE TABLE `tbl_user_share_comment`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_share_id` bigint(20) NOT NULL COMMENT '用户分享ID',
  `user_id` bigint(20) NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint(20) NOT NULL COMMENT '上级评论ID，默认0表示最顶级评论',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论内容',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户分享评论表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
