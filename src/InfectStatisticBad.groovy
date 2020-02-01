import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * InfectStatistic.groovy：耦合度过高的日志处理类、Main调用
 * @author hengyumo* @since 2020-01-31
 */

/**
 * 枚举命令类型和其对应正则
 */
enum LogRegex {
    IIP(~/(\S+) 新增 感染患者 (\d+)人/),
    ISP(~/(\S+) 新增 疑似患者 (\d+)人/),
    FIP(~/(\S+) 感染患者 流入 (\S+) (\d+)人/),
    FSP(~/(\S+) 疑似患者 流入 (\S+) (\d+)人/),
    DEAD(~/(\S+) 死亡 (\d+)人/),
    CURE(~/(\S+) 治愈 (\d+)人/),
    CSP(~/(\S+) 疑似患者 确诊感染 (\d+)人/),
    ESP(~/(\S+) 排除 疑似患者 (\d+)人/)
    ;

    Pattern reg

    LogRegex(Pattern aReg) {
        this.reg = aReg
    }

    /**
     * 根据该行的字符串匹配对应的枚举
     */
    static match(String line) {
        for (def logRex in values()) {
            def matcher = line =~ logRex.reg
            if (matcher) return [logRex, matcher]
        }
        null
    }
}

class InfectStatBad {

    /**
     * 扩展ProvinceDailyLog，增加处理对应日志的方法
     */
    static def extend() {
        DailyLog.metaClass {

            withIip = { iip ->
                delegate.iip += iip
                delegate.ip += iip
            }

            withIsp = { isp ->
                delegate.isp += isp
                delegate.sp += isp
            }

            withFip = { DailyLog p, fip ->
                delegate.iip -= fip
                delegate.ip -= fip
                p.iip += fip
                p.ip += fip
            }

            withFsp = { DailyLog p, fsp ->
                delegate.isp -= fsp
                delegate.sp -= fsp
                p.isp += fsp
                p.sp += fsp
            }

            withDead = { dead ->
                delegate.ip -= dead
                delegate.dead += dead
                delegate.idead += dead
            }

            withCure = { cure ->
                delegate.ip -= cure
                delegate.cure += cure
                delegate.icure += cure
            }

            withCsp = { csp ->
                delegate.csp += csp
                delegate.sp -= csp
                delegate.isp -= csp
                delegate.iip += csp
                delegate.ip += csp
            }

            withEsp = { esp ->
                delegate.esp += esp
                delegate.sp -= esp
                delegate.isp -= esp
            }
        }
    }

    /**
     * 处理日志
     * @return
     */
    static def handleLogs(String logsPath) {
        def logs = Logs.readFrom(logsPath)

        logs.eachLogLine { date, line ->
            //noinspection GroovyAssignabilityCheck
            def (LogRegex logRex, Matcher matcher) = LogRegex.match(line)
            // 科里化闭包
            def provinceDailyLog = { String name ->
                LogCache.instance.getOrCreateDailyLog(name, date)
            }
            DailyLog nationwideDailyLog = LogCache.instance.getOrCreateNationWideDailyLog(date)
            def group = { matcher.group(it) }
            def groupI = { matcher.group(it) as int }

            /**
             * 使用switch 方式并不灵活
             */
            switch (logRex) {
                case LogRegex.IIP:
                    int iip = groupI(2)
                    provinceDailyLog(group(1)).withIip(iip)
                    nationwideDailyLog.withIip(iip)
                    break
                case LogRegex.ISP:
                    int isp = groupI(2)
                    provinceDailyLog(group(1)).withIsp(isp)
                    nationwideDailyLog.withIsp(isp)
                    break
                case LogRegex.FIP:
                    provinceDailyLog(group(1)).withFip(
                            provinceDailyLog(group(2)), groupI(3))
                    break
                case LogRegex.FSP:
                    provinceDailyLog(group(1)).withFsp(
                            provinceDailyLog(group(2)), groupI(3))
                    break
                case LogRegex.DEAD:
                    int dead = groupI(2)
                    provinceDailyLog(group(1)).withDead(dead)
                    nationwideDailyLog.withDead(dead)
                    break
                case LogRegex.CURE:
                    int cure = groupI(2)
                    provinceDailyLog(group(1)).withCure(cure)
                    nationwideDailyLog.withCure(cure)
                    break
                case LogRegex.CSP:
                    int csp = groupI(2)
                    provinceDailyLog(group(1)).withCsp(csp)
                    nationwideDailyLog.withCsp(csp)
                    break
                case LogRegex.ESP:
                    int esp = groupI(2)
                    provinceDailyLog(group(1)).withEsp(esp)
                    nationwideDailyLog.withEsp(esp)
                    break
                default:
                    println "不支持的日志格式： $line"
            }
        }
    }
}

// --------------- 主程序 -------------------

InfectStatBad.extend()
Main.instance.useLogsHandler('../log', InfectStatBad.&handleLogs)
Main.instance.run(args)
