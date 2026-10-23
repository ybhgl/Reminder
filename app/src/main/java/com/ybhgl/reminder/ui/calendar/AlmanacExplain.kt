package com.ybhgl.reminder.ui.calendar

/**
 * 黄历信息格的白话解释。
 *
 * 只解释「内容」：库/口诀能推出来的（星宿、纳音、冲煞、胎神）用模板拼，
 * 需要民俗知识补充的（值神、建除、方位神、神煞）才建表。
 * 文案统一用「传统上/旧时说法」限定，是民俗介绍，不作为行事建议。
 */

/** 十二值神（黄黑道由库直给，释义只讲神格与宜忌倾向） */
internal val DAY_GOD_EXPLAIN: Map<String, String> = mapOf(
    "青龙" to "十二值神之一，黄道吉神。主喜庆、财帛与贵人相助，传统上宜嫁娶、开市、出行。",
    "明堂" to "十二值神之一，黄道吉神。主文教、集会与公开事务，传统上宜上官、议事、庆典。",
    "天刑" to "十二值神之一，黑道凶神。主刑罚、官非与是非，传统上宜守成静处，忌诉讼、远行。",
    "朱雀" to "十二值神之一，黑道凶神。主口舌、文书纠纷，传统上宜谨言慎行，忌签约、争讼。",
    "金匮" to "十二值神之一，黄道吉神。主财帛、库藏，传统上宜纳财、开市、嫁娶。",
    "天德" to "十二值神之一，黄道吉神。主施恩、赦罪与解厄，传统上诸事皆宜。",
    "白虎" to "十二值神之一，黑道凶神。主伤损、凶险，传统上宜静守，忌动土、远行。",
    "玉堂" to "十二值神之一，黄道吉神。主文书、喜庆与仕进，传统上宜会客、签约、赴任。",
    "天牢" to "十二值神之一，黑道凶神。主拘束、困厄，传统上宜守成，忌诉讼、出行。",
    "玄武" to "十二值神之一，黑道凶神。主盗贼、暗昧之事，传统上宜防盗谨守，忌远行、签约。",
    "司命" to "十二值神之一，黄道吉神。主寿算、福禄，传统上宜祈福、祭祀、求医。",
    "勾陈" to "十二值神之一，黑道凶神。主田土、纠葛，传统上宜守成，忌动土、诉讼。"
)

/** 建除十二神：只讲该字的取义与民俗倾向，不展开逐项宜忌 */
internal val DUTY_EXPLAIN: Map<String, String> = mapOf(
    "建" to "建始之义，象征万物初生、气象更新。传统上宜出行、上任、会友，忌动土、开仓。",
    "除" to "除旧布新之义，象征清除旧秽、去故迎新。传统上适合扫舍、疗病、解除旧约一类的事。",
    "满" to "圆满丰盈之义，象征收获与充盛。传统上适合祈福、开市、纳财一类的事。",
    "平" to "平常均平之义，象征平稳少起伏。传统上适合修整、涂泥等日常事务，不宜大动作。",
    "定" to "安定确定之义，象征安稳与定盟。传统上适合嫁娶、安床、订约一类的事。",
    "执" to "执持操守之义，象征坚守与执行。传统上适合修造、捕捉、处理既定事务。",
    "破" to "破败冲破之义，象征破损与分离。传统上宜破旧拆解之事，忌嫁娶、开市等喜庆大事。",
    "危" to "危险高峻之义，象征风险与高处。传统上宜安床、静守，忌登高、冒险、远行。",
    "成" to "成就完成之义，象征诸事可成。传统上适合嫁娶、开市、入学、修造一类的事。",
    "收" to "收获收敛之义，象征收纳与进账。传统上适合纳财、收账、入仓一类的事。",
    "开" to "开通开启之义，象征开张与舒展。传统上适合开市、出行、求职，忌安葬、闭藏之事。",
    "闭" to "闭塞收藏之义，象征关闭与收束。传统上适合安葬、筑堤、补垣，忌开市、远行。"
)

/** 五方位神：只解释神名，不解释方位吉凶 */
internal val DIRECTION_GOD_EXPLAIN: Map<String, String> = mapOf(
    "财神" to "民间信仰中主掌财源、财帛的神明。黄历按日干推算出它当日所在的方位。",
    "喜神" to "民间信仰中主掌喜庆、姻缘与和合的神明，方位随日干变化。",
    "福神" to "民间信仰中主掌福禄、平安的神明，方位随日干变化。",
    "阳贵" to "天乙贵人分昼夜两位，阳贵又称昼贵。传统上认为白天办事、求人相助时取阳贵方位。",
    "阴贵" to "天乙贵人分昼夜两位，阴贵又称夜贵。传统上认为夜间或阴柔之事取阴贵方位。"
)

/** 四象方：库只给单字方位，转成「东方青龙」这类完整称谓 */
private val XIU_ZONE_SYMBOL: Map<String, String> = mapOf(
    "东" to "东方青龙",
    "南" to "南方朱雀",
    "西" to "西方白虎",
    "北" to "北方玄武"
)

/** 值神：未收录时退回「十二值神 + 黄黑道」的保守说明 */
fun dayGodExplanation(dayGod: String, ecliptic: String): String =
    DAY_GOD_EXPLAIN[dayGod] ?: "十二值神之一，今日为${ecliptic}日。"

/** 建除：未收录时退回建除十二神总述 */
fun dutyExplanation(duty: String): String =
    DUTY_EXPLAIN[duty] ?: "建除十二神之一，古以当日值神论宜忌取舍。"

/** 方位神：未收录时退回「按日干推算方位」的总述 */
fun directionGodExplanation(label: String): String =
    DIRECTION_GOD_EXPLAIN[label] ?: "黄历按日干推算的当日${label}方位。"

/** 星宿：宿名/七曜/禽名/四象方/吉凶由数据层拆好，直接拼句 */
fun xiuExplanation(
    name: String,
    sevenStar: String,
    animal: String,
    zone: String,
    luck: String
): String {
    val zoneName = XIU_ZONE_SYMBOL[zone] ?: "${zone}方"
    return "二十八宿中的${name}宿，属${zoneName}七宿，七曜为${sevenStar}，禽名为${animal}。" +
        "二十八宿是古人沿黄道划分的星区，黄历按当日值宿论吉凶，今日为${luck}宿。"
}

/** 纳音：末字即五行，解释「纳音」这一概念而不逐条背三十个名称 */
fun soundExplanation(sound: String, dayGanzhi: String): String =
    "「$sound」是六十甲子纳音之一，五行属${sound.takeLast(1)}。" +
        "纳音把六十甲子两两相配，各附一个五行意象；今日${dayGanzhi}日的纳音即此。"

/** 冲煞：由日干支、对冲干支、煞方直接拼句 */
fun clashExplanation(
    dayGanzhi: String,
    clashZodiac: String,
    clashGanzhi: String,
    shaDirection: String
): String {
    val zodiac = clashZodiac.removePrefix("冲")
    val sha = shaDirection.removePrefix("煞")
    return "今日${dayGanzhi}日，与属${zodiac}者相冲（${clashGanzhi}），煞方在${sha}。" +
        "传统认为属${zodiac}者此日宜避开重大事项，煞方忌动土修造。"
}

/** 胎神：库格式为「占门床 外正南」（器具 + 方位），拆开拼成可读句 */
fun fetusExplanation(fetus: String): String {
    val parts = fetus.trim().split(' ').filter { it.isNotBlank() }
    val what = parts.firstOrNull().orEmpty()
    val where = parts.getOrNull(1)?.let(::fetusPlaceText)
    val whereText = where?.let { "在$it" } ?: "在当日方位"
    val spotText = if (what.isNotBlank()) "的${what}一带" else "一带"
    return "胎神是旧时民俗中护佑胎儿的方位神。今日胎神${whereText}${spotText}，" +
        "传统上孕妇宜避开在该处动土、敲打或搬移家具。"
}

/** 胎神方位：「外正南」→「室外正南方」，「房内中」→「室内中央」 */
private fun fetusPlaceText(raw: String): String {
    val (scope, dir) = when {
        raw.startsWith("房内") -> "室内" to raw.removePrefix("房内")
        raw.startsWith("外") -> "室外" to raw.removePrefix("外")
        else -> "" to raw
    }
    val dirText = when (dir) {
        "正东" -> "正东方"
        "正南" -> "正南方"
        "正西" -> "正西方"
        "正北" -> "正北方"
        "东南" -> "东南方"
        "西南" -> "西南方"
        "东北" -> "东北方"
        "西北" -> "西北方"
        "中" -> "中央"
        else -> dir
    }
    return scope + dirText
}
