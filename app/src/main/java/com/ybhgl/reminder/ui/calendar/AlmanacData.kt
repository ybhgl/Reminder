package com.ybhgl.reminder.ui.calendar

import com.tyme.culture.Taboo
import com.tyme.solar.SolarDay
import com.tyme.sixtycycle.EarthBranch
import com.tyme.sixtycycle.SixtyCycle
import com.ybhgl.reminder.util.CalendarUtil
import java.time.LocalDate

/**
 * 黄历详情页数据层：tyme4kt 能直接给出的字段（宜忌/吉神/五行/值神/彭祖/胎神/冲煞）在此聚合，
 * 库缺失的三块（财神五方位、十二值时时辰吉凶、术语白话与择日分类）以标准口诀/常量表补齐。
 * 术语全集与 tyme4kt dayTaboo 数据集实际出现的 113 项逐一对齐（探查单测核定）。
 */

/** 黄道六值神索引（十二值神序：青龙0 明堂1 天刑2 朱雀3 金匮4 天宝5 白虎6 玉堂7 天牢8 玄武9 司命10 勾陈11） */
private val HUANGDAO_INDICES = setOf(0, 1, 4, 5, 7, 10)

/** 十二值神名（固定序列，自青龙起顺行） */
private val TWELVE_VALUE_GODS =
    listOf("青龙", "明堂", "天刑", "朱雀", "金匮", "天宝", "白虎", "玉堂", "天牢", "玄武", "司命", "勾陈")

/**
 * 十二值时起例：按日支定青龙所居时辰支序（子午起子、丑未起卯、寅申起辰、卯酉起午、辰戌起申、巳亥起戌）。
 */
private val HOUR_LUCK_START = intArrayOf(0, 3, 4, 6, 8, 10, 0, 3, 4, 6, 8, 10)

/** 时辰钟点段（自夜半子时起排） */
private val HOUR_TIME_RANGES = listOf(
    "23-1", "1-3", "3-5", "5-7", "7-9", "9-11", "11-13", "13-15", "15-17", "17-19", "19-21", "21-23"
)

/**
 * 财神/喜神/福神/阳贵/阴贵方位（按日干查，协纪辨方书口诀）：
 * 甲戊庚牛羊、乙丙鼠猴、六辛逢马虎、丁己鸡位乡、壬兔癸蛇。
 */
private val GOD_DIRECTIONS = arrayOf(
    //        财神    喜神    福神    阳贵    阴贵
    arrayOf("东北", "西北", "西南", "东北", "西南"), // 甲
    arrayOf("东北", "西南", "正北", "西南", "正北"), // 乙
    arrayOf("正西", "正南", "正西", "正西", "东北"), // 丙
    arrayOf("正西", "正南", "正东", "西北", "正西"), // 丁
    arrayOf("正北", "东南", "正北", "东北", "西南"), // 戊
    arrayOf("正北", "东北", "正南", "正北", "西南"), // 己
    arrayOf("东北", "西北", "西南", "东北", "西南"), // 庚
    arrayOf("正南", "正南", "正东", "正东", "正南"), // 辛
    arrayOf("东南", "东南", "正北", "正东", "东南"), // 壬
    arrayOf("东南", "东北", "正南", "东南", "正北")  // 癸
)

/** 术语分类 */
enum class TermCategory(val label: String) {
    MARRIAGE("婚嫁喜事"),
    HOME("生活居家"),
    BUILD("营造修造"),
    BUSINESS("工作经营"),
    RITUAL("祭祀祈福"),
    FUNERAL("丧葬白事"),
    SOCIAL("出行交际"),
    OTHER("其他事项")
}

/** 元规则项（非具体行事），不进择日分类网格 */
private val META_TERMS = setOf("诸事不宜", "馀事勿取")

/** 术语白话解释（key 全集=日级宜忌数据集实际出现的 113 个术语） */private val TERM_EXPLAIN: Map<String, String> = mapOf(
    "祭祀" to "祠祭神祀、向神明供献祭品的仪式。",
    "祈福" to "祈求神明、祖先降福，许愿酬神。",
    "求嗣" to "向神明请求保佑子嗣、祈愿怀孕得子。",
    "开光" to "佛像神像落成后举行点眼通灵仪式，赋予灵性。",
    "塑绘" to "塑造神佛像、绘制宗教画壁。",
    "斋醮" to "设坛诵经、斋戒祭祀祈福的宗教法会。",
    "沐浴" to "洁净身心洗浴，古人也指斋戒前的沐浴净身。",
    "酬神" to "酬谢神明庇佑，偿还许下的祭愿。",
    "造庙" to "建造祠堂、庙宇、神龛。",
    "出火" to "迁移移动神佛像、神主牌位。",
    "谢土" to "建筑完工后祭谢土神，安养地基之神。",
    "普渡" to "普度亡魂、中元祭祀法会。",
    "解除" to "消除灾殃、脱去旧秽的祭祀除厄仪式。",
    "嫁娶" to "男女结为夫妻、举办婚礼。古时「嫁」谓女子嫁到夫家，「娶」谓男子亲迎成婚。",
    "纳采" to "男方备礼到女方家求亲，六礼之首。",
    "问名" to "问女方姓名生辰以合八字，婚事六礼之一。",
    "订盟" to "订立婚约、交换信物，确定姻亲关系。",
    "纳婿" to "招婿入门，男方入赘女家。",
    "归宁" to "婚后妻子回娘家省亲。",
    "安床" to "安置婚床、睡床，铺设床帐，多与婚育祈吉相关。",
    "合帐" to "制作婚帐床帐，嫁妆制备仪式之一。",
    "冠笄" to "男子加冠、女子插簪的成年礼。",
    "进人口" to "收养子女、认干亲，或家中增雇人丁。",
    "裁衣" to "裁剪衣料、缝制新衣。",
    "雕刻" to "雕刻佛像、神物、器具纹饰。",
    "修门" to "修缮门户门框。",
    "修坟" to "修整祖先坟墓。",
    "启钻" to "开棺捡骨（二次葬起灵移坟）。",
    "破土" to "阴宅（墓地）开土安葬的掘土，与动土相对。",
    "安葬" to "埋葬亡者、举行入土仪式。",
    "立碑" to "为亡者竖立碑石。",
    "成服" to "丧礼中穿戴孝服。",
    "除服" to "丧期结束脱去孝服、除去祭服。",
    "开生坟" to "生前预修坟墓（寿坟）。",
    "合寿木" to "预先制作寿材棺木。",
    "入殓" to "将亡者遗体移入棺木。",
    "移柩" to "移动灵柩、出殡抬棺。",
    "行丧" to "举行丧礼、出殡送丧。",
    "断蚁" to "断除白蚁虫患、治鼠穴。",
    "归岫" to "安葬后魂归山岫、神主回宅之祭。",
    "入宅" to "迁入新建成的宅屋居住。",
    "安香" to "安放神位、香火，设置家中神龛。",
    "安门" to "安装门框门扇，定向纳吉。",
    "修造" to "室内外的装修营造、修缮住宅。",
    "起基" to "建筑奠基定槽，房屋立柱前的根基工序。",
    "动土" to "阳宅（住宅）破土兴工，开始建筑施工。",
    "上梁" to "装上房屋正梁，传统营造中的大事。",
    "竖柱" to "竖起房屋立柱。",
    "拆卸" to "拆除旧屋构件、拆毁建筑。",
    "破屋" to "拆毁旧屋。",
    "坏垣" to "拆除毁坏坍坏的围墙。",
    "补垣" to "修补破损的矮墙。",
    "作灶" to "砌筑灶台、安装炉灶。",
    "开柱眼" to "柱上凿卯眼，以便连接梁枋。",
    "开厕" to "开挖修造厕所。",
    "造仓" to "建造仓库粮仓。",
    "塞穴" to "修补墙洞鼠穴、封堵漏洞。",
    "平治道涂" to "修整道路，使之平坦畅通。",
    "造桥" to "修造桥梁。",
    "筑堤" to "修筑堤坝防水。",
    "开池" to "开挖池塘。",
    "伐木" to "砍伐林木取材。",
    "开渠" to "开挖水渠沟洫。",
    "掘井" to "凿井取水。",
    "扫舍" to "清扫房屋、洒扫除尘。",
    "放水" to "排放池田积水。",
    "合脊" to "屋顶合拢屋脊。",
    "造畜稠" to "建造牲畜圈栏围栏。",
    "定磉" to "固定柱下石墩（柱础）。",
    "作梁" to "砍伐加工屋梁木料。",
    "修饰垣墙" to "粉刷修饰围墙与房屋外观。",
    "架马" to "架设屋梁、立柱前的放样安架工序。",
    "开市" to "商铺开业、集市开市营业。",
    "挂匾" to "悬挂店招牌匾。",
    "纳财" to "进财收账、入库财物，获得收益。",
    "开仓" to "打开仓库出货、粜粮收粮。",
    "置产" to "购置产业、田宅房产。",
    "出货财" to "出货卖物、借贷财物给人。",
    "安机械" to "安装机械、设备器械。",
    "安碓磑" to "安装舂米磨面的碓磨器具。",
    "造车器" to "制造车辆器械。",
    "造船" to "打造船只。",
    "经络" to "安织机经线，或疏通管线、经络调理。",
    "割蜜" to "采割蜂蜜。",
    "栽种" to "种植谷物果蔬树木。",
    "取渔" to "捕鱼捞虾。",
    "结网" to "编结渔猎用网。",
    "牧养" to "饲养畜禽。",
    "习艺" to "学习技艺、拜师学艺。",
    "入学" to "进学读书、入学拜师。",
    "理发" to "修剪头发、梳理仪容。",
    "整手足甲" to "修剪手脚指甲。",
    "探病" to "探望病人（传统认为病气相侵，宜择日）。",
    "乘船" to "乘船出行。",
    "针灸" to "以针刺艾灸治病。",
    "出行" to "出门远行、旅行出差。",
    "移徙" to "搬迁住宅、迁居留所，泛指搬家移居。",
    "分居" to "分居立户、分家各立门户。",
    "纳畜" to "买入饲养牲畜六畜。",
    "捕捉" to "擒拿盗贼、捕捉害虫野兽。",
    "畋猎" to "打猎围猎。",
    "教牛马" to "驯教牛马牲口使役。",
    "会亲友" to "拜访会见亲戚朋友。",
    "赴任" to "前往就任新职。",
    "求医" to "寻医问诊、求治疾病。",
    "治病" to "接受治疗、服药针灸祛病。",
    "词讼" to "打官司、提起诉讼辩讼。",
    "盖屋" to "建造屋顶、加盖房屋。",
    "雇佣" to "雇佣员工、仆人匠人。",
    "交易" to "买卖交易、签署购销。",
    "立券" to "订立契约、票据文书。",
    "诸事不宜" to "该日诸事皆非所宜，不宜兴办大事。",
    "馀事勿取" to "除少数宜事外，其余诸事都不宜操办。"
)

/** 术语分类表（未收录术语归 OTHER） */
private val TERM_CATEGORY: Map<String, TermCategory> = buildMap {
    fun putAll(category: TermCategory, vararg terms: String) = terms.forEach { put(it, category) }
    putAll(
        TermCategory.MARRIAGE,
        "嫁娶", "纳采", "问名", "订盟", "纳婿", "归宁", "安床", "合帐", "冠笄", "进人口", "裁衣"
    )
    putAll(
        TermCategory.HOME,
        "入宅", "移徙", "分居", "安香", "安门", "扫舍", "塞穴", "补垣", "坏垣", "修饰垣墙",
        "整手足甲", "剃头", "理发", "沐浴", "作灶", "平治道涂"
    )
    putAll(
        TermCategory.BUILD,
        "修造", "动土", "起基", "上梁", "竖柱", "架马", "作梁", "伐木", "开柱眼", "定磉",
        "盖屋", "合脊", "破屋", "拆卸", "造屋", "造桥", "筑堤", "开渠", "掘井", "开池",
        "开厕", "放水", "修门"
    )
    putAll(
        TermCategory.BUSINESS,
        "开市", "挂匾", "纳财", "开仓", "置产", "出货财", "安机械", "造车器", "经络",
        "造船", "割蜜", "栽种", "取渔", "结网", "牧养", "安碓磑", "造仓", "造畜稠",
        "交易", "立券", "纳畜", "赴任", "雇佣", "习艺", "入学", "教牛马", "捕捉", "畋猎"
    )
    putAll(
        TermCategory.RITUAL,
        "祭祀", "祈福", "求嗣", "开光", "塑绘", "斋醮", "酬神", "造庙", "出火", "谢土",
        "普渡", "解除", "雕刻"
    )
    putAll(
        TermCategory.FUNERAL,
        "安葬", "破土", "修坟", "启钻", "立碑", "成服", "除服", "开生坟", "合寿木",
        "入殓", "移柩", "行丧", "断蚁", "归岫"
    )
    putAll(
        TermCategory.SOCIAL,
        "出行", "会亲友", "乘船", "渡水", "求医", "治病", "针灸", "探病", "见贵", "词讼"
    )
}

/** 单个时辰的吉凶（十二值时口径） */
data class HourLuck(
    /** 时柱干支，如"丙子" */
    val ganzhi: String,
    /** 地支序 0=子..11=亥（自夜半子时起排） */
    val branchIndex: Int,
    /** 钟点段文本，如"23-1" */
    val timeRange: String,
    /** 值神名，如"青龙" */
    val god: String,
    /** 吉=黄道值神，凶=黑道值神 */
    val lucky: Boolean
)

/** 某日黄历完整信息（黄历详情页数据聚合，纯 JVM 可测） */
data class FullAlmanac(
    val date: LocalDate,
    /** 大字农历日，如"十三" */
    val lunarDayName: String,
    /** 农历月，如"九月" */
    val lunarMonthName: String,
    /** 干支三柱（立春分界），如"丙午年 戊戌月 丙午日" */
    val ganzhiLine: String,
    /** 日干支，如"丙午" */
    val dayGanzhi: String,
    val recommends: List<String>,
    val avoids: List<String>,
    /** 吉神宜趋（日值神中属性为吉者） */
    val auspiciousGods: List<String>,
    /** 凶神宜忌（日值神中属性为凶者） */
    val inauspiciousGods: List<String>,
    /** 纳音五行，如"天河水" */
    val sound: String,
    /** 十二值位，如"满执位" */
    val duty: String,
    /** 日值神，如"白虎" */
    val dayGod: String,
    /** 黄道/黑道 */
    val ecliptic: String,
    /** 彭祖百忌·干，如"丙不修灶" */
    val pengZuStem: String,
    /** 彭祖百忌·支，如"午不苫盖" */
    val pengZuBranch: String,
    /** 胎神，如"厨灶碓外西南" */
    val fetus: String,
    /** 冲（生肖），如"冲鼠" */
    val clashZodiac: String,
    /** 冲的干支，如"庚子" */
    val clashGanzhi: String,
    /** 煞方，如"煞北" */
    val shaDirection: String,
    val caiShen: String,
    val xiShen: String,
    val fuShen: String,
    val yangGui: String,
    val yinGui: String,
    /** 十二时辰吉凶（自夜半子时起排） */
    val hours: List<HourLuck>,
    /** 距下一个节气天数（≥1，自次日起算） */
    val daysToNextTerm: Int,
    /** 下一个节气名 */
    val nextTermName: String
)

/** 干支序+地支序 → 六十甲子索引（同奇偶才相配，配对唯一） */
private fun sixtyIndex(stemIndex: Int, branchIndex: Int): Int {
    for (i in 0 until 60) {
        if (i % 10 == stemIndex && i % 12 == branchIndex) return i
    }
    error("干支不配: stem=$stemIndex branch=$branchIndex")
}

/** 某日完整黄历 */
fun fullAlmanac(date: LocalDate): FullAlmanac {
    val solar = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth)
    val scd = solar.getSixtyCycleDay()
    val daySixty = scd.getSixtyCycle()
    val dayName = daySixty.getName()
    val branchIndex = daySixty.getEarthBranch().getIndex()
    val stemIndex = daySixty.getHeavenStem().getIndex()

    val gods = scd.getGods().map { Pair(it.getName(), it.getLuck().getName() == "吉") }
    val lunar = solar.getLunarDay()

    // 冲干支：支取对冲（+6），干随六十循环同位数（+6 后 %10）
    val clashBranch = (branchIndex + 6) % 12
    val clashStem = (daySixty.getIndex() + 6) % 10
    val clashGanzhi = SixtyCycle.fromIndex(sixtyIndex(clashStem, clashBranch)).getName()

    // 十二值时：青龙居当日时辰支序=HOUR_LUCK_START[日支序]，自青龙顺行；
    // 时柱按五鼠遁：时干序=(日干序%5)*2+时支序
    val start = HOUR_LUCK_START[branchIndex]
    val hours = (0 until 12).map { i ->
        val godIdx = (i - start + 12) % 12
        HourLuck(
            ganzhi = SixtyCycle.fromIndex(sixtyIndex(((stemIndex % 5) * 2 + i) % 10, i)).getName(),
            branchIndex = i,
            timeRange = HOUR_TIME_RANGES[i],
            god = TWELVE_VALUE_GODS[godIdx],
            lucky = godIdx in HUANGDAO_INDICES
        )
    }

    // 下一节气：自次日向后找首个交节日（≤31 天必命中）
    var cursor = solar.next(1)
    var days = 1
    while (true) {
        val term = cursor.getTerm()
        if (term != null && term.getSolarDay() == cursor) break
        cursor = cursor.next(1)
        days++
    }

    return FullAlmanac(
        date = date,
        lunarDayName = lunar.getName(),
        lunarMonthName = CalendarUtil.getMappedMonthName(lunar.getLunarMonth()!!.getName()),
        ganzhiLine = "${scd.getYear().getName()}年 ${scd.getMonth().getName()}月 ${dayName}日",
        dayGanzhi = dayName,
        recommends = scd.getRecommends().map { it.getName() },
        avoids = scd.getAvoids().map { it.getName() },
        auspiciousGods = gods.filter { it.second }.map { it.first },
        inauspiciousGods = gods.filter { !it.second }.map { it.first },
        sound = daySixty.getSound().getName(),
        duty = scd.getDuty().getName(),
        dayGod = scd.getTwelveStar().getName(),
        ecliptic = scd.getTwelveStar().getEcliptic().getName(),
        pengZuStem = daySixty.getPengZu().getPengZuHeavenStem().getName(),
        pengZuBranch = daySixty.getPengZu().getPengZuEarthBranch().getName(),
        fetus = scd.getFetusDay().getName(),
        clashZodiac = "冲${EarthBranch.fromIndex(clashBranch).getZodiac().getName()}",
        clashGanzhi = clashGanzhi,
        shaDirection = "煞${EarthBranch.fromIndex(branchIndex).getOminous().getName()}",
        caiShen = GOD_DIRECTIONS[stemIndex % 10][0],
        xiShen = GOD_DIRECTIONS[stemIndex % 10][1],
        fuShen = GOD_DIRECTIONS[stemIndex % 10][2],
        yangGui = GOD_DIRECTIONS[stemIndex % 10][3],
        yinGui = GOD_DIRECTIONS[stemIndex % 10][4],
        hours = hours,
        daysToNextTerm = days,
        nextTermName = cursor.getTerm()!!.getName()
    )
}

/** 当前时刻在当日十二时辰列中的索引（23 点后归夜半子时段） */
fun currentHourIndex(hour: Int): Int = ((hour + 1) / 2) % 12

/** 术语白话解释，未收录返回 null */
fun tabooExplanation(term: String): String? = TERM_EXPLAIN[term]

/** 术语所属择日分类 */
fun tabooCategory(term: String): TermCategory = TERM_CATEGORY[term] ?: TermCategory.OTHER

/**
 * 择日页两区事件全集：宜区=数据集中曾出现在「宜」的术语，忌区=曾出现在「忌」的术语。
 * 直接遍历 (月支 12 × 日干支 60) 全部组合用库静态 API 求并集（约 720 次查表，首次访问即缓存），
 * 免维护硬编码清单、与库数据零漂移。同一术语可两区都出现，分别对应"为它找吉日"与"为它找忌日"。
 */
private val RECOMMEND_UNION: Set<String> by lazy { tabooUnion(recommended = true) }
private val AVOID_UNION: Set<String> by lazy { tabooUnion(recommended = false) }

private fun tabooUnion(recommended: Boolean): Set<String> {
    val out = sortedSetOf<String>()
    for (m in 0 until 12) {
        val month = SixtyCycle.fromIndex(m)
        for (d in 0 until 60) {
            val list = if (recommended) {
                Taboo.getDayRecommends(month, SixtyCycle.fromIndex(d))
            } else {
                Taboo.getDayAvoids(month, SixtyCycle.fromIndex(d))
            }
            list.forEach { out.add(it.getName()) }
        }
    }
    return out
}

/** 择日页某区（宜/忌）的事件术语列表（元规则项不进网格，按分类排序） */
fun zejiTerms(recommended: Boolean): List<String> {
    val union = if (recommended) RECOMMEND_UNION else AVOID_UNION
    return (union - META_TERMS).sortedBy { tabooCategory(it) }
}

/** 择日扫描：[from, to] 内命中指定术语（宜=出现在该日宜列表）的日期 */
fun zejiDates(term: String, recommended: Boolean, from: LocalDate, to: LocalDate): List<LocalDate> {
    val out = mutableListOf<LocalDate>()
    var d = from
    while (!d.isAfter(to)) {
        val scd = SolarDay.fromYmd(d.year, d.monthValue, d.dayOfMonth).getSixtyCycleDay()
        val list = if (recommended) scd.getRecommends() else scd.getAvoids()
        if (list.any { it.getName() == term }) out.add(d)
        d = d.plusDays(1)
    }
    return out
}
