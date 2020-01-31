import groovy.transform.Immutable
import org.junit.platform.commons.util.StringUtils

import java.text.SimpleDateFormat

/**
 * Lib.groovy：公用的类
 * @author hengyumo* @since 2020-01-31
 */

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

        List<Log> logSorted = logs.sort { log1, log2 ->
            log1.date - log2.date
        }
        new Logs(logs: logSorted)
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
class ProvinceDailyLog {
    // 感染人数、疑似患者人数、治愈人数、死亡人数
    int ip, sp, cure, dead
    // 相比昨日增加的：感染人数、疑似患者人数、治愈人数、死亡人数
    int iip, isp, icure, idead
    // 今日排除的疑似患者和确诊的感染患者
    int csp, esp

    /**
     * 继承之前的数据来构造
     * @param ip
     * @param sp
     * @param dead
     * @param cure
     */
    ProvinceDailyLog(ip, sp, dead, cure) {
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
    def logs = new HashMap<Date, ProvinceDailyLog>()
    // 该省现今的日志
    ProvinceDailyLog nowLog

    // 排序好的日期
    def getDateSorted() {
        logs.keySet().sort()
    }
}

/**
 * 所有省的日志缓存，单例
 */
@Singleton
class ProvinceMap {
    def map = new HashMap<String, ProvinceLogs>()

    Date latest

    /**
     * 获取某省某天的日志，若未找到则会新创建一个
     * @param name
     * @param date
     * @return
     */
    ProvinceDailyLog getOrCreateProvinceDailyLog(String name, Date date) {
        if (map.containsKey(name)) {
            def provinceLogs = map.get(name)
            if (provinceLogs.logs.containsKey(date)) {
                provinceLogs.logs.get(date)
            } else {
                def preProvinceDailyLog = provinceLogs.nowLog
                def newProvinceDailyLog = null
                // 继承前一天的数量
                preProvinceDailyLog.with {
                    newProvinceDailyLog = new ProvinceDailyLog(ip, sp, dead, cure)
                }
                if (date > latest) {
                    latest = date
                }
                provinceLogs.nowLog = newProvinceDailyLog
                provinceLogs.logs.put(date, newProvinceDailyLog)
                newProvinceDailyLog
            }
        } else {
            def provinceLogs = new ProvinceLogs()
            def newProvinceDailyLog = new ProvinceDailyLog()
            provinceLogs.logs.put(date, newProvinceDailyLog)
            if (date > latest) {
                latest = date
            }
            provinceLogs.nowLog = newProvinceDailyLog
            map.put(name, provinceLogs)
            newProvinceDailyLog
        }
    }

    /**
     * 获取某省某天的日志，若不存在返回空
     * @param name
     * @param date
     * @return
     */
    ProvinceDailyLog getProvinceDailyLog(String name, Date date) {
        if (map.containsKey(name)) {
            def provinceLogs = map.get(name)
            if (provinceLogs.logs.containsKey(date)) {
                provinceLogs.logs.get(date)
            }
        }
        null
    }

    ProvinceDailyLog getProvinceDailyLogLatest(String name) {
        map.get(name)?.nowLog
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

    boolean hasHandledLog = false

    Command addCommand(name) {
        def cmd = new Command(name: name)
        cmds.put(name, cmd)
        cmd
    }

    void useLogsHandler(Closure logsHandler) {
        logsHandler.call()
        hasHandledLog = true
        this
    }

    void run(String[] args) {
        if (!hasHandledLog) {
            throw new Exception('日志未处理')
        }
        try {
            CmdArgs cmdArgs = args.size() > 1 ?
                    new CmdArgs(args) : new CmdArgs(args[0])
            Command command = cmds.get(cmdArgs.cmd)
            if (command == null) {
                println "不支持的命令：${cmdArgs.cmd}"
            } else {
                command.runWith(cmdArgs)
            }
        } catch (Exception e) {
            println e.message
        }
    }
}


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
            throw new Exception("文件：$inputName 不存在")
        }
    }

    return [output, date, type, province, sp, input]
}

// 添加list命令
Main.instance.addCommand('list').whenRun { CmdArgs cmdArgs ->
    def (File output, Date date, List type, List province) = parseCmdArgs(cmdArgs)
    if (!output) {
        throw new Exception('请传入输出文件路径')
    }
    def strGenerated = ''
    def gStringMap = [
            ip  : "感染患者${-> provinceLogAtDate.ip}人",
            sp  : "疑似患者${-> provinceLogAtDate.sp}人",
            cure: "治愈${-> provinceLogAtDate.cure}人",
            dead: "死亡${-> provinceLogAtDate.dead}人"
    ]
    def templateString = "${-> provinceName}"
    if (!type) {
        templateString = " " + gStringMap.values().join(' ')
    } else {
        for (def t in type) {
            templateString += " " + gStringMap."$t"
        }
    }
    ProvinceMap.instance.each { String provinceName, ProvinceLogs provinceLogs ->
        if (province && !(provinceName in province)) {
            return
        }
        def provinceLogAtDate = date ? provinceLogs.nowLog : provinceLogs.logs.get(date)
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
                throw new Exception('日期超出日志范围')
            }
        }
        println templateString
        strGenerated += templateString + '\n'
    }
    println strGenerated
    output << strGenerated
}
