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
     * @param aFunc{ date, group -> }* @return
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
        println "不支持的日志格式： $line"
    }
}

class InfectStat {

    static def pdLog(name, date) {
        ProvinceMap.instance.getOrCreateProvinceDailyLog(name, date)
    }

    /**
     * 扩展日志类型
     * @return
     */
    static def extend() {
        LogsHandle.instance.addHandle('IIP')
                .withReg(~/(\S+) 新增 感染患者 (\d+)人/)
                .withHandle({ Date date, group ->
                    ProvinceDailyLog p = pdLog(group[1], date)
                    int iip = group[2] as int
                    p.iip += iip
                    p.ip += iip
                })

        LogsHandle.instance.addHandle('ISP')
                .withReg(~/(\S+) 新增 疑似患者 (\d+)人/)
                .withHandle({ date, group ->
                    ProvinceDailyLog p = pdLog(group[1], date)
                    int isp = group[2] as int
                    p.isp += isp
                    p.sp += isp
                })

        LogsHandle.instance.addHandle('FIP')
                .withReg(~/(\S+) 感染患者 流入 (\S+) (\d+)人/)
                .withHandle({ date, group ->
                    ProvinceDailyLog p1 = pdLog(group[1], date)
                    ProvinceDailyLog p2 = pdLog(group[2], date)
                    int fip = group[3] as int
                    p1.iip -= fip
                    p1.ip -= fip
                    p2.iip += fip
                    p2.ip += fip
                })

        LogsHandle.instance.addHandle('FSP')
                .withReg(~/(\S+) 疑似患者 流入 (\S+) (\d+)人/)
                .withHandle({ date, group ->
                    ProvinceDailyLog p1 = pdLog(group[1], date)
                    ProvinceDailyLog p2 = pdLog(group[2], date)
                    int fsp = group[3] as int
                    p1.isp -= fsp
                    p1.sp -= fsp
                    p2.isp += fsp
                    p2.sp += fsp
                })

        LogsHandle.instance.addHandle('DEAD')
                .withReg(~/(\S+) 死亡 (\d+)人/)
                .withHandle({ date, group ->
                    ProvinceDailyLog p = pdLog(group[1], date)
                    int dead = group[2] as int
                    p.ip -= dead
                    p.dead += dead
                    p.idead += dead
                })

        LogsHandle.instance.addHandle('CURE')
                .withReg(~/(\S+) 治愈 (\d+)人/)
                .withHandle({ date, group ->
                    ProvinceDailyLog p = pdLog(group[1], date)
                    int cure = group[2] as int
                    p.ip -= cure
                    p.cure += cure
                    p.icure += cure
                })

        LogsHandle.instance.addHandle('CSP')
                .withReg(~/(\S+) 疑似患者 确诊感染 (\d+)人/)
                .withHandle({ date, group ->
                    ProvinceDailyLog p = pdLog(group[1], date)
                    int csp = group[2] as int
                    p.csp += csp
                    p.sp -= csp
                    p.isp -= csp
                    p.iip += csp
                    p.ip += csp
                })

        LogsHandle.instance.addHandle('ESP')
                .withReg(~/(\S+) 排除 疑似患者 (\d+)人/)
                .withHandle({ date, group ->
                    ProvinceDailyLog p = pdLog(group[1], date)
                    int esp = group[2] as int
                    p.esp += esp
                    p.sp -= esp
                    p.isp -= esp
                })

    }

    /**
     * 处理日志
     * @return
     */
    static def handleLogs() {
        def logs = Logs.readFrom('../log')

        logs.eachLogLine { date, line ->
            LogsHandle.instance.handle(date, line)
        }
    }

}

// --------------- 主程序 -------------------

InfectStat.extend()
Main.instance.useLogsHandler(InfectStat.&handleLogs)
Main.instance.run(args)
