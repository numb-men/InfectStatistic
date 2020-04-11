class TestCmdCmd extends InfectStatisticTestBase {
    void testCmdOut1() {
        argsInjectTestTwoScriptWithClosure("cmd -in $BASE_CMD_DIR\\cmd1.txt") {
            assertFileEquals("$BASE_RESULT_DIR\\cmdOut1\\ListOut1.txt",
                    "$BASE_OUT_DIR\\cmdOut1\\ListOut1.txt")
        }
    }

    void testCmdOut2() {
        argsInjectTestTwoScriptWithClosure("cmd -in $BASE_CMD_DIR\\cmd2.txt") {
            for (def i in 2..10) {
                assertFileEquals("$BASE_RESULT_DIR\\cmdOut2\\ListOut${i}.txt",
                        "$BASE_OUT_DIR\\cmdOut2\\ListOut${i}.txt")
            }
        }
    }

    void testCmdOut3() {
        argsInjectTestTwoScriptWithClosure("cmd -in $BASE_CMD_DIR\\cmd3.txt") {
            for (def i in 1..6) {
                assertFileEquals("$BASE_RESULT_DIR\\cmdOut3\\IncStatOut${i}.txt",
                        "$BASE_OUT_DIR\\cmdOut3\\IncStatOut${i}.txt")
            }
        }
    }

    void testCmd4() {
        argsInjectTestTwoScriptWithClosure("cmd -in $BASE_CMD_DIR\\cmd4.txt") {
            assertFileEquals("$BASE_RESULT_DIR\\cmdOut1\\ListOut1.txt",
                    "$BASE_OUT_DIR\\cmdOut1\\ListOut1.txt")
            for (def i in 2..10) {
                assertFileEquals("$BASE_RESULT_DIR\\cmdOut2\\ListOut${i}.txt",
                        "$BASE_OUT_DIR\\cmdOut2\\ListOut${i}.txt")
            }
            for (def i in 1..6) {
                assertFileEquals("$BASE_RESULT_DIR\\cmdOut3\\IncStatOut${i}.txt",
                        "$BASE_OUT_DIR\\cmdOut3\\IncStatOut${i}.txt")
            }
        }
    }
}
