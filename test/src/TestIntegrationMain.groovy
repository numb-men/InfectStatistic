// Main 集成测试
InfectStatBad.extend()
CmdExtend.extend()
// 兼容gradle和idea调试
Main.instance.useLogsHandler(args.size() > 0 ? args[0] : '../../log', InfectStatBad.&handleLogs)

ProvinceLogs provinceLogs
