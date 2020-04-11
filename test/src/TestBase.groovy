import org.junit.Ignore

@Ignore
class TestBase extends GroovyTestCase {

    final static String CHARSET = 'GBK'

    static String result

    static String lastExpected

    void setUp() {
        result = ""
        lastExpected = ""
    }

    static void runShell(String sh, String workDir = null) {
        // 设置工作目录，默认和当前进程一致
        Process process = workDir ? sh.execute(null, new File(workDir)) : sh.execute()
        String errText = process.err.getText(CHARSET)
        // 错误则抛出异常，正确则返回程序控制台输出
        if (errText) throw new RuntimeException(errText)
        else result = process.in.getText(CHARSET)
    }

    static void runShellScript(script, String workDir = null) {
        runShell("$script", workDir)
    }

    static void evaluateScript(String scriptFileName, String argStr, String classPath, String workDir = null) {
        runShell("groovy -cp $classPath $scriptFileName $argStr", workDir)
    }

    static def runInGroovyShell(scriptFileName) {
        new GroovyShell().evaluate(new File(scriptFileName).text)
    }

    static void runGradle(tasks) {
        runShell("gradle -q $tasks")
    }

    static void assertResultEquals(expected) {
        assertEquals(expected, result)
        lastExpected = expected
    }

    static void assertResultEqualsToo() {
        assertEquals(lastExpected, result)
    }

    static void assertFileEquals(outputFileName, expectedResultFileName) {
        assertEquals(new File(expectedResultFileName).text, new File(outputFileName).text)
    }

    static void assertResultMatchesFileContent(contentFile) {
        assertEquals(new File(contentFile).text, result)
    }

    static void assertResultStartsWith(expected) {
        assertTrue result.startsWith(expected)
    }

    static void assertResultContains(expected) {
        assertTrue result.replace('\n', '').contains(expected.replace('\n', ''))
    }
}
