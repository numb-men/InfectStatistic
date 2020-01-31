// groovy TestLibCmd list -date 2020-01-22 -out D:/output.txt -province 福建 浙江

testCmdArgs1(new CmdArgs(args))
testCmdArgs1(new CmdArgs('groovy Lib list -date 2020-01-22 -out D:/output.txt -province 福建 浙江', 'groovy Lib'))
testCmdArgs1(new CmdArgs('list -date 2020-01-22 -out D:/output.txt -province 福建 浙江'))

CmdArgs.eachLine('../case/testCmd1.txt', '$ java InfectStatistic') {
    CmdArgs cmdArgs ->
        if (cmdArgs.cmd == 'list') testCmdArgs1(cmdArgs)
        else testCmdArgs2(cmdArgs)
}

CmdArgs.eachLine('../case/testCmd2.txt') {
    CmdArgs cmdArgs ->
        if (cmdArgs.cmd == 'list') testCmdArgs1(cmdArgs)
        else testCmdArgs2(cmdArgs)
}

println '---- test ok ----'

static def testCmdArgs1(CmdArgs cmdArgs) {
    assert cmdArgs.cmd == 'list'
    assert cmdArgs.argVal('date') == '2020-01-22'
    assert cmdArgs.argVal('type') == null
    assert cmdArgs.argVals('province') == ['福建', '浙江']
    assert cmdArgs.has('date')
    assert !cmdArgs.has('sp')
}

static def testCmdArgs2(CmdArgs cmdArgs) {
    assert cmdArgs.cmd == 'incStat'
    assert cmdArgs.argVal('date') == '2020-01-23'
    assert cmdArgs.argVal('type') == null
    assert cmdArgs.argVal('out') == 'D:/incStatOutput.txt'
    assert !cmdArgs.has('province')
    assert cmdArgs.has('sp')
}

