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
- **构建工具:** Maven 请直接使用系统的maven进行构建任务，不要使用项目中的mvnw（我已移除了项目中的mvnw.cmd）

## 3. 代码要求
1. 尽可能的不要在循环中查询数据库

## 4. 目录结构与分层规范
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
|  |     └── template                               # 模板文件，如：提示词模板等
├── pom.xml                                         # maven项目配置文件     
```
## 5. 开发路线图 (Project Roadmap)
本项目采用模块化、渐进式的敏捷开发策略。请 AI 助手在每次回答前，先读取此处的勾选状态，严格将注意力集中在(当前进行)的阶段。

- [x] **阶段 0: 基础设施搭建**
    - [x] Spring Boot + MyBatis-Plus 框架搭建 (已完成)
    - [x] 各数据库表的查询代码生成，方便后续开发 (已完成)
    - [ ] 通用文件上传接口实现（已完成）
- [x] **阶段 1: 用户模块 (User Module)**
    - [x] 数据库 user 表设计与 Entity 生成 (已完成)
    - [x] 登录/注册接口开发 (包含密码加密) (已完成)
    - [x] 用户基础信息 CRUD (已完成)
    - [x] 用户饮食记录管理接口开发 (已完成)
    - [x] 用户菜品的收藏接口开发 (已完成)
    - [x] 用户菜品分享相关接口开发 (已完成)
    - [x] 用户菜品分享评论相关接口开发 (已完成)
    - [x] 用户营养目标相关接口开发 (已完成)
    - [x] 用户注册接口实现 (已完成)
    - [x] 用户统计信息接口开发 (已完成)
    - [x] 保存用户标签接口开发 (已完成)
    - [x] 生成用户个性化菜谱接口开发 (已完成)
    - [x] 删除用户接口开发 (已完成)
    - [x] 完成用户菜谱收藏接口开发 (已完成)
    - [ ] 获取用户饮食记录接口开发 (当前进行)
- [x] **阶段 2: 菜谱模块 (Recipe Module)**
    - [x] 菜谱表、食材表设计 (已完成)
    - [x] 菜谱的 CRUD 与分页查询 (已完成)
    - [x] 复杂条件搜索 (按分类、热度等) (已完成)
    - [x] 菜品的评分接口开发 (已完成)
    - [x] 菜品食材管理接口开发 (已完成)
    - [x] 菜品制作步骤管理接口开发 (已完成)
    - [x] 保存菜品标签接口开发 (已完成)
    - [x] 保存菜品信息接口开发 (已完成)
    - [x] 获取菜品标签接口开发 (已完成)
    - [x] 删除菜谱接口开发 (已完成)
    - [x] 获取菜谱得分列表接口开发 (已完成)
    - [x] 获取菜谱用户评价接口开发 (已完成)
- [x] **阶段 3: 标签模块 (Lable Module)**
    - [x] 标签模块接口开发 (已完成)
- [x] **阶段 4: 知识库模块 (Lable Module)**
  - [x] 保存知识库接口开发 (已完成)
  - [x] 删除知识库接口开发 (已完成)
- [x] **阶段 5: 系统首页 (Lable Module)**
  - [x] 完成系统首页的数据统计接口开发 (已完成)
  - [x] 完成系统首页的菜谱审核统计接口开发 (已完成)
  - [x] 完成系统首页的菜谱动态统计接口开发 (已完成)
- [x] **阶段 6: 系统管理 (Lable Module)**
  - [x] 完成系统营养相关接口开发 (已完成)
- [x] **阶段 7: AI 对接模块 (AI Chat Module)**
    - [x] 集成 Spring AI 与 Qwen (已完成)
    - [x] 菜谱智能生成接口开发 (已完成)
    - [x] 菜谱智能生成添加RAG功能 (已完成)

## 6. 当前执行任务 (Current Task)
> **开发者指令：**
> 现在，让我们开始`获取用户饮食记录接口开发`吧，请按照以下要求完成该任务：
> 1: 在src/main/java/com/cooking/api/UserDietRecordApi.java中实现。 完成detail接口。
> 2: 接口接收 date 为参数，用户获取用户指定某一天的饮食记录，格式为：yyyy-MM-dd
> 3: 要求接口返回结构如下
> 4: 其中 key 对应餐次(UserDietRecordEntity.dietOrder), label 对应餐次名称， dishes 对应该餐次下的菜谱列表。
> 5: 菜谱列表包含 id, dishId, name, dishImg, tags, shareCount, collectCount 七个字段，
> 6: 其中 id 饮食记录id dishId 菜谱id dishImg 即菜谱的图片， tags 为标签列表，shareCount 即菜谱分享数量（对应 UserShareEntity） collectCount 即菜谱分享数量（对应 UserDishCollectEntity）

```json5

//用户菜谱收藏接口响应示例

[
  {
    key: 'breakfast',
    label: '早餐',
    dishes: [{
      id: 301,
      dishId: 16,
      name: '红烧排骨',
      dishImg: "/UploadFile/dish.img",
      tags: ['家常菜', '零厨艺'],
      shareCount: 2,
      collectCount: 5,
    },],
  },
  {
    key: 'lunch',
    label: '午餐',
    dishes: [
      {
        id: 301,
        dishId: 16,
        name: '红烧排骨',
        dishImg: "/UploadFile/dish.img",
        tags: ['家常菜', '零厨艺'],
        shareCount: 2,
        collectCount: 5,
      },
      {
        id: 302,
        dishId: 17,
        name: '麻婆豆腐',
        dishImg: "/UploadFile/dish.img",
        tags: ['家常菜', '用料少'],
        shareCount: 2,
        collectCount: 5,
      },
    ],
  },
  {
    key: 'dinner',
    label: '晚餐',
    dishes: [
      {
        id: 303,
        dishId: 18,
        name: '干锅花菜',
        dishImg: "/UploadFile/dish.img",
        tags: ['家常菜', '零厨艺'],
        shareCount: 2,
        collectCount: 5,
        
      },
    ],
  },
]

```