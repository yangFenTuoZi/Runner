# Runner

本APP受到[ShizukuRunner](https://github.com/WuDi-ZhanShen/ShizukuRunner)的启发而写
目前仍在开发，大部分功能不全，请等后续  
如果喜欢这个项目的话请多多为我Star吧

## 下载

[Github](https://github.com/yangFenTuoZi/Runner/releases)

## 计划

**开发大致按以下顺序进行 此表随时变动 仅供参考**

**暂并不考虑支持其他语言 如有需要 请提 [PR](https://github.com/yangFenTuoZi/Runner/pulls)**

### 功能

- [x] 命令执行实时输出
- [ ] ~~分离终端扩展包~~
- [ ] ~~终端bash, busybox~~
> 过于冗余，考虑手动外置代替
- [x] 允许进程后台运行
- [ ] ~~允许执行前降低权限（可能仅 Root 可用）~~
> 无意义，限制 SELinux 上下文更有意义，但考虑到 system_server 只观察 UID，考虑后续加回
- [x] 支持强杀进程
- [x] 支持杀所有子进程
- [x] 增加进程管理
- [ ] ~~支持自定义环境变量~~
> 无意义，请自行 export
- [ ] 开机自启动执行命令
- [x] App数据备份与还原
- [ ] 终端
> Android Terminal Emulator 架构过老，考虑重写
- ~~[ ] 终端侧载模块（类似 dpkg ）~~
> 无意义，重复造轮子
- ~~[ ] 外部CLI~~
> 无意义，但考虑使用意图框架借助 app 桥接 service 实现

### 语言

- [x] 中文
- [x] 英文

## 反馈注意事项

1. 反馈请说明设备信息(系统版本、UI版本、权限等)
2. 反馈请附日志

## 特别鸣谢

- [ShizukuRunner](https://github.com/WuDi-ZhanShen/ShizukuRunner)
- [Shizuku](https://github.com/RikkaApps/Shizuku)
