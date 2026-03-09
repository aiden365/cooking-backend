# 智能菜谱系统 (Intelligent Recipe System) - 后端开发指南

## 1. 项目概述
本项目是“智能菜谱系统”的后端服务，主要为前端提供API支撑。
核心业务包括：用户注册与登录、用户管理、用户标签管理，菜谱管理、菜品标签管理、菜谱评价、用户饮食记录管理，用户菜品分享、用户菜品收藏，健康标签管理、营养标准管理、系统首页数据统计、知识库管理及系统参数维护等模块
## 2. 技术栈与框架约束
在生成、修改代码或编写 SQL 时，请严格遵守以下技术栈规范：
- **核心框架:** Spring Boot 4.x (Java 21+)
- **持久层框架:** MyBatis-Plus
- **数据库:** MySQL 8.x
- **AI 接入方案:** Spring AI 结合本地/远端部署的 Ollama (调用 Qwen 通义千问模型)
- **构建工具:** Maven

## 3. 目录结构与分层规范
本项目采用标准的 Controller-Service-DAO 三层架构，并严格区分数据传输对象。请将生成的代码放入正确的包路径下：

```text
P:\workspace\projects\cooking\cooking-backend       # 项目根目录
├── database                                        # 数据库相关文档                    
|  └── cookbook.sql                                 # 数据库脚本                           
├── src                                             # 项目源代码
|  ├── main                                         # 项目主代码
|  |  ├── java                                      # 项目主代码
|  |  |  └── com                                    # 项目主代码包
|  |  |     └── cooking                             # 项目主代码包名
|  |  |        ├── api                              # Spring Controller接口目录
|  |  |        ├── base                             # 基础类和公共类目录
|  |  |        ├── config                           # Spring及其他框架配置类目录   
|  |  |        ├── dto                              # 数据对象存放目录
|  |  |        ├── enums                            # 系统枚举类目录
|  |  |        ├── exceptions                       # 系统异常类目录
|  |  |        ├── core                             # 项目核心业务类目录
|  |  |        |  ├── entity                        # 数据库表实体类目录
|  |  |        |  ├── mapper                        # Mybatis-Plus Mapper接口目录
|  |  |        |  └── service                       # 项目核心业务类目录
|  |  |        |     ├── impl                       # 项目核心业务类实现类目录 
|  |  |        └── utils                            # 项目工具类目录
|  |  └── resources                                 # 项目资源文件 
|  |     ├── application.properties                 # 项目配置文件
|  |     └── mapper                                 # Mybatis-Plus Mapper接口对应的XML文件目录
├── pom.xml                                         # maven项目配置文件     
```
## 4. 开发路线图 (Project Roadmap)
本项目采用模块化、渐进式的敏捷开发策略。请 AI 助手在每次回答前，先读取此处的勾选状态，严格将注意力集中在(当前进行)的阶段。

- [x] **阶段 0: 基础设施搭建**
    - [x] Spring Boot + MyBatis-Plus 框架搭建 (已完成)
    - [x] 各数据库表的查询代码生成，方便后续开发 (已完成)
- [x] **阶段 1: 用户模块 (User Module)**
    - [x] 数据库 user 表设计与 Entity 生成 (已完成)
    - [x] 登录/注册接口开发 (包含密码加密) (已完成)
    - [x] 用户基础信息 CRUD (已完成)
    - [x] 用户饮食记录管理接口开发 (已完成)
    - [x] 用户菜品的收藏接口开发 (已完成)
    - [x] 用户菜品分享相关接口开发 (已完成)
    - [x] 用户菜品分享评论相关接口开发 (已完成)
    - [x] 用户营养目标相关接口开发 (当前进行)
- [x] **阶段 2: 菜谱模块 (Recipe Module)**
    - [x] 菜谱表、食材表设计 (已完成)
    - [x] 菜谱的 CRUD 与分页查询 (已完成)
    - [x] 复杂条件搜索 (按分类、热度等) (已完成)
    - [x] 菜品的评分接口开发 (已完成)
    - [x] 菜品食材管理接口开发 (已完成)
    - [x] 菜品制作步骤管理接口开发 (已完成)
- [x] **阶段 3: 标签模块 (Lable Module)**
    - [x] 标签模块接口开发 (已完成)
- [ ] **阶段 3: AI 对接模块 (AI Chat Module)**
    - [x] 集成 Spring AI 与 Qwen (已完成)
    - [ ] 菜谱智能生成接口开发 (当前进行)

## 5. 当前执行任务 (Current Task)
> **开发者指令：** 请根据以下具体要求，为我生成代码。
> 在`DishApi.java`中完成菜谱智能生成接口开发接口开发。该功能主要是让用户设置自身每日的营养目标，为菜品的推荐提供依据。
> 对于添加接口和修改接口可以合并为一个save接口，接口接收id、用户id、营养元素名称、目标值参数。添加时请根据系统参数（SystemParamEnum.user_nutrition_limit）中配置的数量进行限制
> 系统参数配置可通过SystemParamsService.findByName方法获取