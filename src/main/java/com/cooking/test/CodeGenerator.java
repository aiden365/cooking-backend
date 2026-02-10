package com.cooking.test;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.cooking.base.BaseController;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseService;
import com.cooking.base.BaseServiceImpl;

public class CodeGenerator {

    static String url = "jdbc:mysql://192.168.19.119:3308/cookbook?autoReconnect=true&allowPublicKeyRetrieval=true&useAffectedRows=true&useUnicode=true&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF8";
    static String username = "root";
    static String password = "123456";

    public static void main(String[] args) {

        // 使用 FastAutoGenerator 快速配置代码生成器
        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author("aiden") // 设置作者
                            .outputDir("E:\\workspace\\java\\cooking-backend\\src\\main\\java")
                            .disableOpenDir()

                            ;
                })
                .packageConfig(builder -> {
                    builder.parent("com.cooking.core") // 设置父包名
                            .entity("entity") // 设置实体类包名
                            .mapper("mapper") // 设置 Mapper 接口包名
                            .service("service") // 设置 Service 接口包名
                            .serviceImpl("service.impl") // 设置 Service 实现类包名
                            .controller("api")
                            .xml("mapper.xml"); // 设置 Mapper XML 文件包名
                })
                .strategyConfig(builder -> {
                    builder.addInclude("tbl_user") // 设置需要生成的表名
                            .addTablePrefix("tbl_")
                            .entityBuilder()
                            .superClass(BaseEntity.class)
                            .disableSerialVersionUID()
                            .enableChainModel()
                            .enableLombok()
                            .enableRemoveIsPrefix()
                            .enableTableFieldAnnotation()
                            .enableActiveRecord()
                            .versionColumnName("version")
                            .logicDeleteColumnName("deleted")
                            .naming(NamingStrategy.no_change)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .addSuperEntityColumns("id", "create_user", "created_time", "update_user", "updated_time")
                            .addIgnoreColumns("xxx")
                            /*.addTableFills(new Column("create_time", FieldFill.INSERT))
                            .addTableFills(new Property("updateTime", FieldFill.INSERT_UPDATE))*/
                            .idType(IdType.AUTO)
                            .formatFileName("%sEntity")


                            .controllerBuilder()
                            .superClass(BaseController.class)
                            .enableRestStyle()
                            .formatFileName("%sApi")

                            .serviceBuilder()
                            .superServiceClass(BaseService.class)
                            .superServiceImplClass(BaseServiceImpl.class)
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")


                            .mapperBuilder()
                            .superClass(BaseMapper.class)
                            /*.enableMapperAnnotation()*/
                            /*.enableBaseResultMap()*/
                            /*.enableBaseColumnList()*/
                            .formatMapperFileName("%sMapper")
                            .formatXmlFileName("%sXml")

                            ;

                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用 Freemarker 模板引擎
                .execute(); // 执行生成
    }
}
