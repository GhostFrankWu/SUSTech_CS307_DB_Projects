# SUSTech_CS307_DB_Projects
## 期中project内容：选课系统
[查看pdf报告](https://github.com/GhostFrankWu/SUSTech_CS307_DB_Projects/blob/main/mid/%E6%8A%A5%E5%91%8A.pdf)
### 数据表设计包括：
- 每个表要有外键，或者有其他表的外键指向  
- 对于表之间的外键方向，不能有环  
- 除了主键自增的id之外，需要有其他unique约束的列  
- 对于每种数据，使用合适的类型存储  
- 具有易拓展性
### 效率探究包括：
- 单次链接、预编译、批处理、事务处理、关闭日志、索引加速。并进行前后比对。    
- 7 种不同配置下的 13 个数据库：Debian & Windows 两大系列 4 种操作系统，CISC & RISC 两大系列 3 种硬件架构，同系统下不同系统版本横向对比。  
- 指令集架构、操作系统、硬件配置、数据库类型的效率对比。  
- 数据库高并发和文件系统高并发的对比及拟合曲线。    
### 查询系统包括：   
- 过滤已选课程  
- 过滤已满课程  
- 过滤时间冲突课程并提示冲突课程名  
- 过滤先修不足课程（先修关系处理）  
- 多条件联合模糊查询及退选课  
- 按周次查询课程表7.密码修改、登出
- 教师显示所任课程和所带班级学生，劝退所带学生
- 管理员执行 sql 语句（非最高权限），修改任意用户密码
### 登陆界面
![m](https://github.com/GhostFrankWu/SUSTech_CS307_DB_Projects/blob/main/sc/1.png)
### 选课系统主界面
![m](https://github.com/GhostFrankWu/SUSTech_CS307_DB_Projects/blob/main/sc/2.png)
### 课程表
![m](https://github.com/GhostFrankWu/SUSTech_CS307_DB_Projects/blob/main/sc/3.png)
### 权限控制
![m](https://github.com/GhostFrankWu/SUSTech_CS307_DB_Projects/blob/main/sc/4.png)
### 更多详情请[查看pdf报告](https://github.com/GhostFrankWu/SUSTech_CS307_DB_Projects/blob/main/mid/%E6%8A%A5%E5%91%8A.pdf)


## 期末project内容：重构选课系统后端及中间件

