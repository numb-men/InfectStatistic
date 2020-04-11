// Main 冒烟兼集成测试
InfectStatBad.extend()

CmdExtend.extend()

// 兼容gradle和idea调试
Main.instance.useLogsHandler(InfectStatBad.&handleLogs)

def outputPath = 'D:\\code\\groovy\\InfectStatistic\\output\\IntegrationMainOutput.txt'
def logsPath = 'D:\\code\\groovy\\InfectStatistic\\log'

Main.instance.run("list -date 2020-01-22 -log $logsPath -out $outputPath -province 福建 浙江")

println new File(outputPath).text

Main.instance.run(["list", "-date", "2020-01-22", "-log", logsPath,
                   "-out", outputPath, "-province", "福建", "浙江"] as String[])

println new File(outputPath).text
