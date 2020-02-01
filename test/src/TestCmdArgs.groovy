/**
 * 测试CmdArgs类
 */
class TestCmdArgs extends TestBase {

    static def assert1(CmdArgs cmdArgs) {
        assertTrue cmdArgs.cmd == 'list'
        assertTrue cmdArgs.argVal('date') == '2020-01-22'
        assertTrue cmdArgs.argVal('type') == null
        assertTrue cmdArgs.argVals('province') == ['福建', '浙江']
        assertTrue cmdArgs.has('date')
        assertTrue !cmdArgs.has('sp')
    }

    static def assert2(CmdArgs cmdArgs) {
        assertTrue cmdArgs.cmd == 'incStat'
        assertTrue cmdArgs.argVal('date') == '2020-01-23'
        assertTrue cmdArgs.argVal('type') == null
        assertTrue cmdArgs.argVal('out') == 'D:/incStatOutput.txt'
        assertTrue !cmdArgs.has('province')
        assertTrue cmdArgs.has('sp')
    }

    void testWithArgsArray() {
        assert1(new CmdArgs('list', '-date', '2020-01-22', '-out', 'D:/output.txt', '-province', '福建', '浙江'))
    }

    void testWithArgStr() {
        assert1(new CmdArgs('groovy Lib list -date 2020-01-22 -out D:/output.txt -province 福建 浙江', 'groovy Lib'))
        assert1(new CmdArgs('list -date 2020-01-22 -out D:/output.txt -province 福建 浙江'))
    }

    void testFileEachLine() {

        // gradle 调用和命令行调用时user.dir不同
        def userDir = System.getProperty('user.dir')
        //println userDir

        CmdArgs.eachLine("$userDir/test/case/testLibCmdArgs1.txt", '$ java InfectStatistic') {
            CmdArgs cmdArgs ->
                if (cmdArgs.cmd == 'list') assert1(cmdArgs)
                else assert2(cmdArgs)
        }

        CmdArgs.eachLine("$userDir/test/case/testLibCmdArgs2.txt") {
            CmdArgs cmdArgs ->
                if (cmdArgs.cmd == 'list') assert1(cmdArgs)
                else assert2(cmdArgs)
        }
    }

    /**
     * 在命令行下运行测试
     * @param args
     */
    static void main(String[] args) {
        // groovy -cp ../../out/classes/groovy/main;../../out/classes/groovy/test;
        //              TestLibCmdArgs list -date 2020-01-22 -out D:/output.txt -province 福建 浙江

        assert1(new CmdArgs(args))
    }
}
