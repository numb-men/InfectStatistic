class TestCmdIncStat extends InfectStatisticTestBase {

    void testIncStatOut1() {
        argsInjectTestTwoScriptWithOutFile("incStat -out $BASE_OUT_DIR\\IncStatOut1.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-22", "IncStatOut1.txt")
    }

    void testIncStatOut2() {
        argsInjectTestTwoScriptWithOutFile("incStat -out $BASE_OUT_DIR\\IncStatOut2.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-23", "IncStatOut2.txt")
    }

    void testIncStatOut3() {
        argsInjectTestTwoScriptWithOutFile("incStat -out $BASE_OUT_DIR\\IncStatOut3.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-27", "IncStatOut3.txt")
    }

    void testIncStatOut4() {
        argsInjectTestTwoScriptWithOutFile("incStat -out $BASE_OUT_DIR\\IncStatOut4.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-27 -sp", "IncStatOut4.txt")
    }

    void testIncStatOut5() {
        argsInjectTestTwoScriptWithOutFile("incStat -out $BASE_OUT_DIR\\IncStatOut5.txt $DEFAULT_LOG_ARG " +
                "-date 2020-01-25 -sp", "IncStatOut5.txt")
    }

    void testIncStatOut6() {
        argsInjectTestTwoScriptWithOutFile("incStat -out $BASE_OUT_DIR\\IncStatOut6.txt $DEFAULT_LOG_ARG " +
                "-type cure dead -province 河北 福建 湖北 -sp", "IncStatOut6.txt")
    }

    void testIncStatOutByCmd1() {
        withOutputFileTestTwoScript("incStat -out $BASE_OUT_DIR\\IncStatOutByCmd1.txt $DEFAULT_LOG_ARG " +
                "-type cure dead -province 河北 福建 湖北 -sp", "IncStatOutByCmd1.txt")
    }
}
