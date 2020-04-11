import groovy.transform.Immutable
import org.junit.platform.commons.util.StringUtils

import java.text.Collator
import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * Lib.groovy：公用的类
 * @author hengyumo* @since 2020-01-31
 */

/**
 * 由用户错误操作造成的异常
 */
class StatException extends Exception {
    StatException(String msg) { super(msg) }
}

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
        if (args.size() == 0) {
            throw new StatException('请传入参数')
        }
        if (args[0].startsWith('-')) {
            throw new StatException('请传入命令/命令应排在参数之前')
        }
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
 * 日志文本缓存
 */
class Logs {

    static List<Log> logs

    /**
     * 从某文件夹路径读取所有日志
     * @param path
     * @return
     */
    static List<Log> readFrom(String path) {
        def newLogs = new ArrayList<Log>()
        def logFiles = new File(path)
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        logFiles.eachFile { file ->
            Date date = dateFormat.parse(file.name - '.log.txt')
            newLogs << new Log(date: date, text: file.text)
        }
        newLogs.sort { log1, log2 ->
            log1.date <=> log2.date
        }
        logs = newLogs
    }

    /**
     * 遍历所有日志的所有行，执行传入的闭包
     * @param closure{ date, line -> }
     */
    static void eachLogLine(Closure closure) {
        for (def log in logs) {
            log.text.eachLine { line ->
                if (StringUtils.isNotBlank(line) && !line.startsWith("//")) {
                    closure.call(log.date, line)
                }
                null
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

    def clear() {
        map = new HashMap<String, ProvinceLogs>()
        latest = null
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

    // todo 目前只有三个命令
    Map cmds = new HashMap<String, Command>(3)

    Closure logsHandler

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

    void clear() {
        LogCache.instance.clear()
        cmds = new HashMap<String, Command>(3)
    }

    /**
     * 使用某个LogsHandle，后边在运行时需要传入-log参数
     * @param logsHandler
     */
    void useLogsHandler(Closure logsHandler) {
        this.logsHandler = logsHandler
        this
    }

    /**
     * 传入构造好的cmdArgs运行
     * @param cmdArgs
     */
    void run(CmdArgs cmdArgs) {
        try {
            Command command = cmds.get(cmdArgs.cmd)
            if (command == null) {
                throw new StatException("不支持的命令：${cmdArgs.cmd}")
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

    final static String NOTICE = '// 该文档并非真实数据，仅供测试使用'

    final static PROVINCE_LIST = ["安徽", "北京", "重庆", "福建", "甘肃", "广东",
                                  "广西", "贵州", "海南", "河北", "河南", "黑龙江", "湖北",
                                  "湖南", "吉林", "江苏", "江西", "辽宁", "内蒙古", "宁夏",
                                  "青海", "山东", "山西", "陕西", "上海", "四川",
                                  "天津", "西藏", "新疆", "云南", "浙江",]
    /**
     * 集中解析命令行参数
     * @param cmdArgs
     * @return [logsPath, output, date, type, province, sp, input]
     */
    static def parseCmdArgs(CmdArgs cmdArgs, Boolean needLogs=true) {
        // 所有被支持的参数
        def logsPath, output, date, type, province, sp, input

        logsPath = output = date = type = province = sp = input = null
        // 读取日志
        logsPath = cmdArgs.argVal('log')

        if (needLogs) {
            if (!logsPath) {
                throw new StatException('请设置-log 目录位置')
            }
            def logsDir = new File(logsPath)
            if (!logsDir.exists()) {
                throw new StatException('日志目录不存在')
            } else {
                if (logsDir.isFile()) {
                    throw new StatException('日志目录应该要是目录')
                } else {
                    String[] logNames = logsDir.list()
                    if (logNames.length == 0) {
                        throw new StatException('日志目录不能为空')
                    } else {
                        final Pattern logNamePattern = ~/\d{4}-\d{2}-\d{2}\.log\.txt/
                        for (String logName in logNames) {
                            // 绝对匹配
                            if (!(logName ==~ logNamePattern)) {
                                throw new StatException("$logName 不符合日志文件命名要求")
                            }
                        }
                    }
                }
            }

            Main.instance.logsHandler.call(logsPath)
        }

        if (cmdArgs.has('out')) {
            output = new File(cmdArgs.argVal('out'))
        }

        if (cmdArgs.has('date')) {
            date = new SimpleDateFormat('yyyy-MM-dd').parse(cmdArgs.argVal('date'))
            if (date < Logs.logs[0].date || date > Logs.logs[-1].date) {
                throw new StatException('日期超出日志范围')
            }
        }

        if (cmdArgs.has('type')) {
            type = cmdArgs.argVals('type')
            if (type != null && type.size() == 0) {
                throw new StatException('-type后至少需要跟着一个参数值')
            }
        }

        if (cmdArgs.has('province')) {
            province = cmdArgs.argVals('province')
            if (province != null && province.size() == 0) {
                throw new StatException('-province后至少需要跟着一个参数值')
            }
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

        return [logsPath, output, date, type, province, sp, input]
    }

    static void eachPName(province, date, Closure closure) {
        // 将全国移到第一个，其它省按照汉字拼音排序
        def keys = new HashSet<String>(LogCache.instance.map.keySet())
        if (province) {
            keys.addAll(province)
        }
//        Comparator<String> comparator = Collator.getInstance(Locale.CHINA)
        def pNames = keys.sort { name1, name2 ->
//            comparator.compare(name1, name2)
            // 防止重庆被排序为zhongqing，使用预先声明好的排序
            PROVINCE_LIST.indexOf(name1) - PROVINCE_LIST.indexOf(name2)
        }
        int nationIndex = pNames.indexOf(LogCache.NATIONWIDE)
        for (int i = nationIndex; i > 0; i--) {
            pNames[i] = pNames[i - 1]
        }
        pNames[0] = LogCache.NATIONWIDE

        pNames.each { String pName ->
            // 如果设置了province选项，则只列出提供的省的数据
            if (province && !(pName in province)) {
                return
            }
            ProvinceLogs provinceLogs = LogCache.instance.map.get(pName)
            DailyLog provinceLogAtDate
            // 该省日志未记录过，则默认都为0
            if (provinceLogs == null) {
                provinceLogAtDate = new DailyLog()
            } else {
                // 未设置date则默认取提供日志的最新的一天
                provinceLogAtDate = date ? provinceLogs.logs.get(date) : provinceLogs.latestLog
                if (provinceLogAtDate == null) {
                    def dates = provinceLogs.dateSorted
                    // 日期缺失则默认取离其最近的前一天
                    for (int i = 0; i < dates.size(); i++) {
                        if (dates[i] < date && (i < dates.size() - 1 && dates[i + 1] > date)) {
                            def latestLog = provinceLogs.logs.get(dates[i])
                            latestLog.with {
                                provinceLogAtDate = new DailyLog(ip, sp, dead, cure)
                            }
                        }
                    }
                }
            }
            closure.call(pName, provinceLogAtDate)
        }
    }

    /**
     * 扩展命令
     */
    static void extend() {
        // 添加list命令
        Main.instance.addCommand('list').whenRun { CmdArgs cmdArgs ->
            def (String logsPath, File output, Date date, List type, List province) = parseCmdArgs(cmdArgs)
            if (!output) {
                throw new StatException('请传入输出文件路径')
            }
            def strGenerated = ''
            def provinceLogAtDate = null
            def provinceName = ''
            // 利用闭包惰性求值
            def gStringMap = [
                    ip  : " 感染患者${-> provinceLogAtDate.ip}人",
                    sp  : " 疑似患者${-> provinceLogAtDate.sp}人",
                    cure: " 治愈${-> provinceLogAtDate.cure}人",
                    dead: " 死亡${-> provinceLogAtDate.dead}人"
            ]

            eachPName(province, date) { String pName, DailyLog pLogAtDate ->
                provinceName = pName
                provinceLogAtDate = pLogAtDate

                def templateString = "${-> provinceName}"
                if (!type) {
                    templateString += gStringMap.values().join()
                } else {
                    for (def t in type) {
                        if (!gStringMap.containsKey(t)) {
                            throw new StatException("不存在的type：$t")
                        }
                        templateString += gStringMap."$t"
                    }
                }
                strGenerated += templateString + '\n'
            }
            strGenerated += NOTICE
            output.withWriter('utf-8') { writer ->
                writer.write(strGenerated)
            }
        }

        // 添加incStat命令
        Main.instance.addCommand('incStat').whenRun { CmdArgs cmdArgs ->
            def (String logsPath, File output, Date date, List type, List province, Boolean sp) = parseCmdArgs(cmdArgs)
            if (!output) {
                throw new StatException('请传入输出文件路径')
            }
            def strGenerated = ''
            def provinceLogAtDate = null
            def provinceName = ''
            // 利用闭包惰性求值
            def gStringMap = [
                    ip  : " 新增感染患者${-> provinceLogAtDate.iip}人",
                    sp  : " 新增疑似患者${-> provinceLogAtDate.isp}人",
                    cure: " 新增治愈${-> provinceLogAtDate.icure}人",
                    dead: " 新增死亡${-> provinceLogAtDate.idead}人"
            ]
            def spGString = " 疑似患者确诊感染${-> provinceLogAtDate.csp}人 排除疑似患者${-> provinceLogAtDate.esp}人"

            eachPName(province, date) { String pName, DailyLog pLogAtDate ->
                provinceName = pName
                provinceLogAtDate = pLogAtDate

                def templateString = "${-> provinceName}"
                if (!type) {
                    templateString += gStringMap.values().join()
                } else {
                    for (def t in type) {
                        if (!gStringMap.containsKey(t)) {
                            throw new StatException("不存在的type：$t")
                        }
                        templateString += gStringMap."$t"
                    }
                }
                strGenerated += templateString
                if (sp) {
                    strGenerated += spGString
                }
                strGenerated += '\n'
            }
            strGenerated += NOTICE
            output.withWriter('utf-8') { writer ->
                writer.write(strGenerated)
            }
        }

        // 添加cmd命令
        Main.instance.addCommand('cmd').whenRun { CmdArgs cmdArgs ->
            File input = parseCmdArgs(cmdArgs, false)[-1]
            if (!input) {
                throw new StatException('请传入命令文件路径')
            }
            input.eachLine { line ->
                if (StringUtils.isNotBlank(line) && !line.startsWith('//')) {
                    LogCache.instance.clear()
                    Main.instance.run(line)
                }
            }
        }
    }
}
