/**
 * 测试 -log参数的错误输入情况
 */
class TestLogArg extends InfectStatisticTestBase {

    void testNoneLog() {
        argsInjectTestTwoScriptWithLineFeed("list", "请设置-log 目录位置")
    }

    void testLogPathNotExists() {
        argsInjectTestTwoScriptWithLineFeed("list -log xxxx -out test.txt", "日志目录不存在")
    }

    void testEmptyLogsDir() {
        argsInjectTestTwoScriptWithLineFeed(
                "list -log D:\\code\\groovy\\InfectStatistic\\badLog\\emptyLogDir", "日志目录不能为空")
    }

    void testLogsDirIsFile() {
        argsInjectTestTwoScriptWithLineFeed(
                "list -log D:\\code\\groovy\\InfectStatistic\\badLog\\fileLog", "日志目录应该要是目录")
    }

    void testBadLogName() {
        argsInjectTestTwoScriptWithLineFeed(
                "list -log D:\\code\\groovy\\InfectStatistic\\badLog\\badLogName", "badLogName.log.txt 不符合日志文件命名要求")
    }

    void testBadLogLine() {
        argsInjectTestTwoScriptWithLineFeed("list -log D:\\code\\groovy\\InfectStatistic\\badLog\\badLogLine", "不支持的日志格式： a bad line")
    }
}
