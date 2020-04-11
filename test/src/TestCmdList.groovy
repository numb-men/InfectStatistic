/**
 * 测试命令list
 */
class TestCmdList extends InfectStatisticTestBase {

    void testNoneArgs() {
        argsInjectTestTwoScriptWithLineFeed("", "请传入参数")
    }

    void testErrorCommand() {
        // 几十毫秒
        argsInjectTestTwoScriptWithLineFeed("-xxx xx", "请传入命令/命令应排在参数之前")
    }

    void testInvalidCommand() {
        argsInjectTestTwoScriptWithLineFeed("xxx -date xx", "不支持的命令：xxx")
    }

    void testNoneOut() {
        argsInjectTestTwoScriptWithLineFeed("list $DEFAULT_LOG_ARG", "请传入输出文件路径")
    }

    void testListOut1() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut1.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22", "ListOut1.txt")
    }

    void testListOut2() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut2.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22 -province 福建 河北", "ListOut2.txt")
    }

    void testListOut3() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut3.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22 -type ip", "ListOut3.txt")
    }

    void testListOut4() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut4.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22 -type cure dead", "ListOut4.txt")
    }

    void testListOut5() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut5.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22 -type cure dead ip", "ListOut5.txt")
    }

    void testListOut6() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut6.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-23", "ListOut6.txt")
    }

    void testListOut7() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut7.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-23 -type cure dead ip -province 全国 浙江 福建", "ListOut7.txt")
    }

    void testListOut8() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut8.txt $DEFAULT_LOG_ARG",
                "ListOut8.txt")
    }

    void testListOut9() {
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut9.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-27 -type dead -province 福建", "ListOut9.txt")
    }

    void testListOut10() {
        // 日期缺失则默认取离其最近的前一天
        argsInjectTestTwoScriptWithOutFile("list -out $BASE_OUT_DIR\\ListOut10.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-25", "ListOut10.txt")
    }

    void testListDateOutSide() {
        argsInjectTestTwoScriptWithLineFeed("list -out $BASE_OUT_DIR\\ListOut6.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-28", "日期超出日志范围")

        argsInjectTestTwoScriptWithLineFeed("list -out $BASE_OUT_DIR\\ListOut6.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-20", "日期超出日志范围")
    }

    void testListNoneType() {
        argsInjectTestTwoScriptWithLineFeed("list -out $BASE_OUT_DIR\\ListOut6.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22 -type", "-type后至少需要跟着一个参数值")
    }

    void testListNoneProvince() {
        argsInjectTestTwoScriptWithLineFeed("list -out $BASE_OUT_DIR\\ListOut6.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22 -province", "-province后至少需要跟着一个参数值")
    }

    void testListErrorType() {
        argsInjectTestTwoScriptWithLineFeed("list -out $BASE_OUT_DIR\\ListOut6.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22 -type ip xxxx", "不存在的type：xxxx")
    }

    void testListOutByCmd1() {
        withOutputFileTestTwoScript("list -out $BASE_OUT_DIR\\ListOutByCmd1.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22", "ListOutByCmd1.txt")
    }
}
