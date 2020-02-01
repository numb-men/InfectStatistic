import groovy.transform.Immutable
import org.junit.platform.commons.util.StringUtils

import java.text.SimpleDateFormat

/**
 * Lib.groovy：公用的类
 * @author hengyumo* @since 2020-01-31
 */

class StatException extends Exception {}

/**
 * 解析命令行参数
 */
class CmdArgs {
    String[] args

    /**
     * 传入命令行参数数组构造
     * @param args
     */
    CmdArgs(String[] args) {
        this.args = args
    }

    /**
     * 传入命令构造，可以设置无用的前缀，如`groovy Lib`
     * @param argsStr
     * @param noUseInStart
     */
    CmdArgs(String argsStr, String noUseInStart = '') {
        this.args = (argsStr - noUseInStart).tokenize(' ') as String[]
    }

    /**
     * 遍历文件的命令，调用闭包
     * @param fileName
     * @param noUseInStart
     * @param closure{ String line -> }
     */
    static void eachLine(String fileName, String noUseInStart = '', Closure closure) {
        new File(fileName).eachLine { line ->
            if (StringUtils.isNotBlank(line)) {
                closure.call(new CmdArgs(line, noUseInStart))
            }
        }
    }

    /**
     * 获取命令
     * @return
     */
    String getCmd() {
        args[0]
    }

    /**
     * 获取某个命令行参数的值，多个值只会返回第一个
     * @param key
     * @return
     */
    String argVal(String key) {
        key = '-' + key
        for (int i = 0; i < args.length; i++) {
            if (args[i] == key) {
                for (int j = i + 1; j < args.length; j++) {
                    if (!args[j].startsWith('-'))
                        return args[j]
                    else break
                }
            }
        }
        null
    }

    /**
     * 获取某个命令行参数的值，返回列表
     * @param key
     * @return
     */
    def argVals(String key) {
        key = '-' + key
        for (int i = 0; i < args.length; i++) {
            if (args[i] == key) {
                def value = []
                for (int j = i + 1; j < args.length; j++) {
                    if (!args[j].startsWith('-'))
                        value << args[j]
                    else break
                }
                return value
            }
        }
        null
    }

    /**
     * 判断该命令是否有对应的参数
     * @param key
     * @return
     */
    boolean has(String key) {
        args.any { it == '-' + key }
    }
}

/**
 * 日志类，不可变
 */
@Immutable
class Log {
    Date date
    String text
}

/**
 * 日志缓存、读取
 */
class Logs {

    List<Log> logs

    /**
     * 从某文件夹路径读取所有日志
     * @param path
     * @return
     */
    static Logs readFrom(String path) {
        def logFiles = new File(path)
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def logs = new ArrayList<Log>()
        logFiles.eachFile { file ->
            Date date = dateFormat.parse(file.name - '.log.txt')
            logs << new Log(date: date, text: file.text)
        }

        new Logs(logs: logs.sort())
    }

    /**
     * 遍历所有日志的所有行，执行传入的闭包
     * @param closure{ date, line -> }
     */
    void eachLogLine(Closure closure) {
        logs.forEach { log ->
            log.text.eachLine { line ->
                if (StringUtils.isNotBlank(line) && !line.startsWith("//")) {
                    closure.call(log.date, line)
                }
            }
        }
    }
}

/**
 * 某省的某天日志数据
 */
class DailyLog {
    // 感染人数、疑似患者人数、治愈人数、死亡人数
    int ip, sp, cure, dead
    // 相比昨日增加的：感染人数、疑似患者人数、治愈人数、死亡人数
    int iip, isp, icure, idead
    // 今日排除的疑似患者和确诊的感染患者
    int csp, esp

    DailyLog() {}

    /**
     * 继承之前的数据来构造
     * @param ip
     * @param sp
     * @param dead
     * @param cure
     */
    DailyLog(ip, sp, dead, cure) {
        this.ip = ip
        this.sp = sp
        this.dead = dead
        this.cure = cure
    }
}

/**
 * 某省的所有日志
 */
class ProvinceLogs {
    // 该省的日志列表
    def logs = new HashMap<Date, DailyLog>()
    // 该省现今的日志
    DailyLog latestLog

    // 排序好的日期
    def getDateSorted() {
        logs.keySet().sort()
    }
}

/**
 * 所有省的日志缓存，单例
 */
@Singleton
class LogCache {
    def map = new HashMap<String, ProvinceLogs>()

    Date latest

    final static String NATIONWIDE = '全国'

    /**
     * 获取某省某天的日志，若未找到则会新创建一个
     * @param name
     * @param date
     * @return
     */
    DailyLog getOrCreateDailyLog(String name, Date date) {
        if (map.containsKey(name)) {
            def provinceLogs = map.get(name)
            if (provinceLogs.logs.containsKey(date)) {
                provinceLogs.logs.get(date)
            } else {
                def preProvinceDailyLog = provinceLogs.latestLog
                def newProvinceDailyLog = null
                // 继承前一天的数量
                preProvinceDailyLog.with {
                    newProvinceDailyLog = new DailyLog(ip, sp, dead, cure)
                }
                if (date > latest) {
                    latest = date
                }
                provinceLogs.latestLog = newProvinceDailyLog
                provinceLogs.logs.put(date, newProvinceDailyLog)
                newProvinceDailyLog
            }
        } else {
            def provinceLogs = new ProvinceLogs()
            def newProvinceDailyLog = new DailyLog()
            provinceLogs.logs.put(date, newProvinceDailyLog)
            if (date > latest) {
                latest = date
            }
            provinceLogs.latestLog = newProvinceDailyLog
            map.put(name, provinceLogs)
            newProvinceDailyLog
        }
    }

    /**
     * 获取全国某天的日志，不存在会先创建
     * @param date
     * @return
     */
    DailyLog getOrCreateNationWideDailyLog(Date date) {
        getOrCreateDailyLog(NATIONWIDE, date)
    }

    /**
     * 获取某省某天的日志，若不存在返回空
     * @param name
     * @param date
     * @return
     */
    DailyLog getDailyLog(String name, Date date) {
        if (map.containsKey(name)) {
            def provinceLogs = map.get(name)
            if (provinceLogs.logs.containsKey(date)) {
                provinceLogs.logs.get(date)
            }
        }
        null
    }

    /**
     * 获取全国某天的日志，不存在返回null
     * @param date
     * @return
     */
    DailyLog getNationWideDailyLog(Date date) {
        getDailyLog(NATIONWIDE, date)
    }

    DailyLog getDailyLogLatest(String name) {
        map.get(name)?.latestLog
    }
}

/**
 * 不同种类的命令
 */
class Command {
    String name
    Closure runFunc

    /**
     * 为命令设置运行闭包
     * @param aRunFunc{ cmdArgs -> }* @return this
     */
    Command whenRun(Closure aRunFunc) {
        runFunc = aRunFunc
        this
    }

    /**
     * 使用传入的cmdArgs运行该种命令
     * @param cmdArgs
     */
    void runWith(CmdArgs cmdArgs) {
        runFunc.call(cmdArgs)
    }
}

/**
 * 核心程序
 */
@Singleton
class Main {
    Map cmds = new HashMap<String, Command>(3)

    // 是否已经处理好日志
    boolean hasHandledLog = false

    /**
     * 添加命令：链式构造
     * @param name
     * @return
     */
    Command addCommand(name) {
        def cmd = new Command(name: name)
        cmds.put(name, cmd)
        cmd
    }

    /**
     * 使用某个LogsHandle
     * @param logsPath
     * @param logsHandler
     */
    void useLogsHandler(String logsPath, Closure logsHandler) {
        logsHandler.call(logsPath)
        hasHandledLog = true
        this
    }

    /**
     * 传入构造好的cmdArgs运行
     * @param cmdArgs
     */
    void run(CmdArgs cmdArgs) {
        if (!hasHandledLog) {
            throw new Exception('日志未处理')
        }
        try {
            Command command = cmds.get(cmdArgs.cmd)
            if (command == null) {
                println "不支持的命令：${cmdArgs.cmd}"
            } else {
                command.runWith(cmdArgs)
            }
        } catch (StatException e) {
            println e.message
        }
    }

    /**
     * 传入命令行命令数组运行
     * @param args
     */
    void run(String[] args) {
        run(new CmdArgs(args))
    }

    /**
     * 传入命令行命令字符串运行，可以设置去除命令开头的无效字符串
     * @param argStr
     * @param noUseInStart
     */
    void run(String argStr, String noUseInStart = '') {
        run(new CmdArgs(argStr, noUseInStart))
    }
}

/**
 * 扩展程序支持的cmd、解耦
 */
class CmdExtend {
    /**
     * 集中解析命令行参数
     * @param cmdArgs
     * @return [output, date, type, province, sp, input]
     */
    static def parseCmdArgs(CmdArgs cmdArgs) {
        // 所有被支持的参数
        def output, date, type, province, sp, input

        output = date = type = province = sp = input = null

        if (cmdArgs.has('out')) {
            output = new File(cmdArgs.argVal('out'))
        }

        if (cmdArgs.has('date')) {
            date = new SimpleDateFormat('yyyy-MM-dd').parse(cmdArgs.argVal('date'))
        }

        if (cmdArgs.has('type')) {
            type = cmdArgs.argVals('type')
        }

        if (cmdArgs.has('province')) {
            province = cmdArgs.argVals('province')
        }

        if (cmdArgs.has('sp')) {
            sp = true
        }

        if (cmdArgs.has('in')) {
            def inputName = cmdArgs.argVal('in')
            input = new File(inputName)
            if (!input.exists()) {
                throw new StatException("文件：$inputName 不存在")
            }
        }

        return [output, date, type, province, sp, input]
    }

    /**
     * 扩展命令
     */
    static void extend() {
        // 添加list命令
        Main.instance.addCommand('list').whenRun { CmdArgs cmdArgs ->
            def (File output, Date date, List type, List province) = parseCmdArgs(cmdArgs)
            if (!output) {
                throw new StatException('请传入输出文件路径')
            }
            def strGenerated = ''
            def provinceLogAtDate = null
            def provinceName = ''
            // 利用闭包惰性求值
            def gStringMap = [
                    ip  : " 感染患者${-> provinceLogAtDate?.ip}人",
                    sp  : " 疑似患者${-> provinceLogAtDate.sp}人",
                    cure: " 治愈${-> provinceLogAtDate.cure}人",
                    dead: " 死亡${-> provinceLogAtDate.dead}人"
            ]

            // 将全国移到第一个，其它省按照字母排序
            def pNames = LogCache.instance.map.keySet().sort()
            pNames.swap(0, pNames.indexOf(LogCache.NATIONWIDE))

            pNames.each { String pName ->
                // 如果设置了province选项，则只列出提供的省的数据
                if (province && !(pName in province)) {
                    return
                }
                ProvinceLogs provinceLogs = LogCache.instance.map.get(pName)
                provinceName = pName
                // 未设置date则默认取提供日志的最新的一天
                provinceLogAtDate = date ? provinceLogs.latestLog : provinceLogs.logs.get(date)
                if (provinceLogAtDate == null) {
                    def dates = provinceLogs.dateSorted
                    // 日期缺失则默认取离其最近的前一天
                    for (int i = 0; i < dates.size(); i++) {
                        if (dates[i] < date && (i < dates.size() - 1 && dates[i + 1] > date)) {
                            provinceLogAtDate = provinceLogs.logs.get(dates[i])
                            provinceLogs.logs.put(date, provinceLogAtDate)
                        }
                    }
                    // 日期超出了提供的日志的最后一天
                    if (provinceLogAtDate == null) {
                        throw new StatException('日期超出日志范围')
                    }
                }
                def templateString = "${-> provinceName}"
                if (!type) {
                    templateString += gStringMap.values().join()
                } else {
                    for (def t in type) {
                        templateString += gStringMap."$t"
                    }
                }
                println templateString
                strGenerated += templateString + '\n'
            }
            output.withWriter('utf-8') { writer ->
                writer.write(strGenerated)
            }
        }
    }
}
