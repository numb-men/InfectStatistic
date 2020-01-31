import groovy.transform.Immutable
import org.junit.platform.commons.util.StringUtils

import java.text.SimpleDateFormat

/**
 * Lib.groovy：公用的类
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
     * 默认全部为0
     */
    ProvinceDailyLog() {
        ip = sp = cure = dead = iip = isp = icure = idead = csp = esp = 0
    }

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
        iip = isp = icure = idead = csp = esp = 0
    }
}

/**
 * 某省的所有日志
 */
class ProvinceLogs {
    // 该省的日志列表
    def logs = new HashMap<Date, ProvinceDailyLog>()
    // 该省现今的日志
    Date now
    ProvinceDailyLog nowLog
}

/**
 * 所有省的日志缓存，单例
 */
@Singleton
class ProvinceMap {
    def map = new HashMap<String, ProvinceLogs>()

    /**
     * 获取某省某天的日志，若未找到则会新创建一个
     * @param name
     * @param date
     * @return
     */
    ProvinceDailyLog getOrCreateProvinceLogs(String name, Date date) {
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
                provinceLogs.now = date
                provinceLogs.nowLog = newProvinceDailyLog
                provinceLogs.logs.put(date, newProvinceDailyLog)
                newProvinceDailyLog
            }
        } else {
            def provinceLogs = new ProvinceLogs()
            def newProvinceDailyLog = new ProvinceDailyLog()
            provinceLogs.logs.put(date, newProvinceDailyLog)
            provinceLogs.now = date
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
    ProvinceDailyLog getProvinceLogs(String name, Date date) {
        if (map.containsKey(name)) {
            def provinceLogs = map.get(name)
            if (provinceLogs.logs.containsKey(date)) {
                provinceLogs.logs.get(date)
            }
        }
        null
    }
}

class Command {
    String name
    Closure runFunc

    Command whenRun(Closure aRunFunc) {
        runFunc = aRunFunc
        this
    }

    void runWith(CmdArgs cmdArgs) {
        runFunc.call(cmdArgs)
    }
}

Main.instance.addCommand('list').whenRun { CmdArgs cmdArgs ->
    if (!cmdArgs.has('out')) {
        println '请设置输出路径'
    } else if (cmdArgs.has('date')) {

    } else if (cmdArgs.has('type')) {

    } else if (cmdArgs.has('province')) {

    }
}

@Singleton
class Main {
    CmdArgs cmdArgs
    Map cmds = new HashMap<String, Command>(3)

    Command addCommand(name) {
        def cmd = new Command(name: name)
        cmds.put(name, cmd)
        cmd
    }

    void withArgs(String[] args) {
        cmdArgs = new CmdArgs(args)
    }

    void useStatClosure(Closure statClosure) {

    }

    void run() {
        switch (cmdArgs.cmd) {
            case 'list':
                // -out -date -type -province
                break
            case 'incStat':
                break
            case 'cmd':
                break
            default:
                println "不支持的命令：${cmdArgs.cmd}"
        }
    }
}

