import java.util.regex.Pattern

/**
 * InfectStatistic.groovy：解耦的日志处理类、Main调用
 * @author hengyumo
 * @since 2020-01-31
 */

/**
 * 某种类型的日志的处理器
 */
class LogHandle {
    Pattern reg
    Closure handleFunc

    /**
     * 设置正则，链式调用
     * @param aReg
     * @return
     */
    LogHandle withReg(Pattern aReg) {
        reg = aReg
        this
    }

    /**
     * 设置闭包，链式调用
     * @param aFunc{ date, group -> }
     * @return
     */
    LogHandle withHandle(aFunc) {
        handleFunc = aFunc
        this
    }
}

/**
 * 日志总执行器
 */
@Singleton
class LogsHandle {

    /**
     * 日志执行器集合，大小默认初始化为8
     */
    def handles = new HashMap<String, LogHandle>(8)

    /**
     * 构造命令
     * @param name
     * @return
     */
    LogHandle addHandle(name) {
        LogHandle commandHandle = new LogHandle()
        handles.put(name, commandHandle)
        commandHandle
    }

    /**
     * 处理某天的某行命令
     * @param date
     * @param line
     */
    def handle(Date date, String line) {
        for (def handle in handles.values()) {
            def matcher = line =~ handle.reg
            if (matcher) {
                handle.handleFunc.call(date, matcher[0])
                return
            }
        }
        throw new StatException("不支持的日志格式： $line")
    }
}

/**
 * 高内聚，低耦合
 */
class InfectStat {

    /**
     * 方法简写：获取某省某日日志，不存在则创建
     * @param name
     * @param date
     * @return
     */
    static DailyLog pdLog(name, date) {
        LogCache.instance.getOrCreateDailyLog(name, date)
    }

    /**
     * 方法简写：获取全国某天的日志，不存在则创建
     * @param date
     * @return
     */
    static DailyLog ndLog(date) {
        LogCache.instance.getOrCreateNationWideDailyLog(date)
    }

    /**
     * 扩展日志类型
     * @return
     */
    static def extend() {
        LogsHandle.instance.addHandle('IIP')
                .withReg(~/(\S+) 新增 感染患者 (\d+)人/)
                .withHandle({ Date date, group ->
                    DailyLog p = pdLog(group[1], date)
                    DailyLog n = ndLog(date)
                    int iip = group[2] as int
                    def doIip = {
                        it.iip += iip
                        it.ip += iip
                    }
                    doIip(p)
                    doIip(n)
                })

        LogsHandle.instance.addHandle('ISP')
                .withReg(~/(\S+) 新增 疑似患者 (\d+)人/)
                .withHandle({ date, group ->
                    DailyLog p = pdLog(group[1], date)
                    DailyLog n = ndLog(date)
                    int isp = group[2] as int
                    def doIsp = {
                        it.isp += isp
                        it.sp += isp
                    }
                    doIsp(p)
                    doIsp(n)
                })

        LogsHandle.instance.addHandle('FIP')
                .withReg(~/(\S+) 感染患者 流入 (\S+) (\d+)人/)
                .withHandle({ date, group ->
                    DailyLog p1 = pdLog(group[1], date)
                    DailyLog p2 = pdLog(group[2], date)
                    int fip = group[3] as int
                    p1.iip -= fip
                    p1.ip -= fip
                    p2.iip += fip
                    p2.ip += fip
                })

        LogsHandle.instance.addHandle('FSP')
                .withReg(~/(\S+) 疑似患者 流入 (\S+) (\d+)人/)
                .withHandle({ date, group ->
                    DailyLog p1 = pdLog(group[1], date)
                    DailyLog p2 = pdLog(group[2], date)
                    int fsp = group[3] as int
                    p1.isp -= fsp
                    p1.sp -= fsp
                    p2.isp += fsp
                    p2.sp += fsp
                })

        LogsHandle.instance.addHandle('DEAD')
                .withReg(~/(\S+) 死亡 (\d+)人/)
                .withHandle({ date, group ->
                    DailyLog p = pdLog(group[1], date)
                    DailyLog n = ndLog(date)
                    int dead = group[2] as int
                    def doDead = {
                        it.ip -= dead
                        it.iip -= dead
                        it.dead += dead
                        it.idead += dead
                    }
                    doDead(p)
                    doDead(n)
                })

        LogsHandle.instance.addHandle('CURE')
                .withReg(~/(\S+) 治愈 (\d+)人/)
                .withHandle({ date, group ->
                    DailyLog p = pdLog(group[1], date)
                    DailyLog n = ndLog(date)
                    int cure = group[2] as int
                    def doCure = {
                        it.ip -= cure
                        it.iip -= cure
                        it.cure += cure
                        it.icure += cure
                    }
                    doCure(p)
                    doCure(n)
                })

        LogsHandle.instance.addHandle('CSP')
                .withReg(~/(\S+) 疑似患者 确诊感染 (\d+)人/)
                .withHandle({ date, group ->
                    DailyLog p = pdLog(group[1], date)
                    DailyLog n = ndLog(date)
                    int csp = group[2] as int
                    def doCsp = {
                        it.csp += csp
                        it.sp -= csp
                        it.isp -= csp
                        it.iip += csp
                        it.ip += csp
                    }
                    doCsp(p)
                    doCsp(n)
                })

        LogsHandle.instance.addHandle('ESP')
                .withReg(~/(\S+) 排除 疑似患者 (\d+)人/)
                .withHandle({ date, group ->
                    DailyLog p = pdLog(group[1], date)
                    DailyLog n = ndLog(date)
                    int esp = group[2] as int
                    def doEsp = {
                        it.esp += esp
                        it.sp -= esp
                        it.isp -= esp
                    }
                    doEsp(p)
                    doEsp(n)
                })
    }

    /**
     * 处理日志
     * @return
     */
    static def handleLogs(String logsPath) {
        Logs.readFrom(logsPath)
        Logs.eachLogLine { date, line ->
            LogsHandle.instance.handle(date, line)
        }
    }

}

// --------------- 主程序 -------------------

class RunMain {
    static void main(String[] args) {
        InfectStat.extend()
        CmdExtend.extend()
        Main.instance.useLogsHandler(InfectStat.&handleLogs)
        Main.instance.run(args)
    }
}

RunMain.main(args)
