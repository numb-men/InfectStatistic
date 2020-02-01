class TestBase extends GroovyTestCase {
    static String result

    void setUp() {
        result = ""
    }

    static void runShellScript(script) {
        result = "$script".execute().text
    }

    static void evaluateScript(scriptFileName) {
        result = "groovy $scriptFileName".execute().text
    }

    static def runInGroovyShell(scriptFileName) {
        new GroovyShell().evaluate(new File(scriptFileName).text)
    }

    static void runGradle(tasks) {
        result = "gradle -q $tasks".execute().text
    }

    static void assertResultEquals(expected) {
        assertEquals(expected, result)
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
