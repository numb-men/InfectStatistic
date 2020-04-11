import org.junit.Ignore

@Ignore
class InfectStatisticTestBase extends TestBase {

    final static String CLASSPATH = 'D:\\code\\groovy\\InfectStatistic\\out\\classes\\groovy\\main;' +
            'D:\\code\\groovy\\InfectStatistic\\out\\classes\\groovy\\test;'

    final static String WORK_DIR = 'D:\\code\\groovy\\InfectStatistic\\src\\'

    protected final static String BASE_CMD_DIR = 'D:\\code\\groovy\\InfectStatistic\\cmd'
    protected final static String BASE_OUT_DIR = 'D:\\code\\groovy\\InfectStatistic\\output'
    protected final static String BASE_RESULT_DIR = 'D:\\code\\groovy\\InfectStatistic\\result'

    protected final static String DEFAULT_LOG_ARG = '-log D:\\code\\groovy\\InfectStatistic\\log'

    final static String SCRIPT = 'InfectStatistic.groovy'
    final static String SCRIPT_BAD_ONE = 'InfectStatisticBad.groovy'

    protected final static LINEFEED = '\r\n'

    void setUp() {
        super.setUp()
        Main.instance.clear()
    }

    static void simpleTestTwoScript(argStr, expected) {
        evaluateScript SCRIPT, argStr, CLASSPATH, WORK_DIR
        assertResultEquals expected

        evaluateScript SCRIPT_BAD_ONE, argStr, CLASSPATH, WORK_DIR
        assertResultEqualsToo()
    }

    static void withOutputFileTestTwoScript(argStr, outputFileName, expectedResultFileName = outputFileName) {
        evaluateScript SCRIPT, argStr, CLASSPATH, WORK_DIR
        assertFileEquals("$BASE_OUT_DIR\\$outputFileName",
                "$BASE_RESULT_DIR\\$expectedResultFileName")

        evaluateScript SCRIPT_BAD_ONE, argStr, CLASSPATH, WORK_DIR
        assertFileEquals("$BASE_OUT_DIR\\$outputFileName",
                "$BASE_RESULT_DIR\\$expectedResultFileName")
    }

    /**
     * 重定向控制台输出到字符串
     * @param closure
     */
    static void redirectSystemOut(Closure closure) {
        PrintStream old = System.out
        ByteArrayOutputStream bs = new ByteArrayOutputStream()
        PrintStream ps = new PrintStream(bs)
        System.setOut(ps)
        closure.call(bs)
        // 还原
        System.setOut(old)
    }

    // 在expected后添加换行——根据系统
    static void argsInjectTestTwoScriptWithLineFeed(argStr, expected) {
        argsInjectTestTwoScript(argStr, "$expected$LINEFEED")
    }

    // 注入命令行参数，而不是shell建立新进程
    static void argsInjectTestTwoScript(argStr, expected) {

        InfectStatBad.extend()
        CmdExtend.extend()
        Main.instance.useLogsHandler(InfectStatBad.&handleLogs)
        redirectSystemOut { ByteArrayOutputStream bs ->
            Main.instance.run(argStr)
            result = bs.toString()
        }
        assertResultEquals expected

        Main.instance.clear()
        InfectStat.extend()
        CmdExtend.extend()
        Main.instance.useLogsHandler(InfectStat.&handleLogs)
        redirectSystemOut { ByteArrayOutputStream bs ->
            Main.instance.run(argStr)
            result = bs.toString()
        }
        assertResultEqualsToo()
    }

    static void argsInjectTestTwoScriptWithOutFile(argStr, outputFileName, expectedResultFileName = outputFileName) {
        InfectStatBad.extend()
        CmdExtend.extend()
        Main.instance.useLogsHandler(InfectStatBad.&handleLogs)
        Main.instance.run(argStr)
        assertFileEquals("$BASE_OUT_DIR\\$outputFileName",
                "$BASE_RESULT_DIR\\$expectedResultFileName")

        Main.instance.clear()
        InfectStat.extend()
        CmdExtend.extend()
        Main.instance.useLogsHandler(InfectStat.&handleLogs)
        Main.instance.run(argStr)
        assertFileEquals("$BASE_OUT_DIR\\$outputFileName",
                "$BASE_RESULT_DIR\\$expectedResultFileName")
    }

    static void argsInjectTestTwoScriptWithClosure(argStr, Closure closure) {
        InfectStatBad.extend()
        CmdExtend.extend()
        Main.instance.useLogsHandler(InfectStatBad.&handleLogs)
        Main.instance.run(argStr)
        closure.call()

        Main.instance.clear()
        InfectStat.extend()
        CmdExtend.extend()
        Main.instance.useLogsHandler(InfectStat.&handleLogs)
        Main.instance.run(argStr)
        closure.call()
    }
}
