// 活动配置（新增）
const ACTIVITY_NAME = "夏日动漫皮肤庆典";
const ACTIVITY_ID = "SUMMER20251001";
const MAX_CHANGES = 0; // 最大更换次数，null或<=0表示无限制
// 活动时间配置（新增）
const EVENT_START_STR = "2025-10-01 00:00:00";
const EVENT_END_STR = "2025-10-07 23:59:59";

// 新人体验期配置（新增）
const NEWBIE_TRIAL_ENABLED = true; // 是否启用新人体验期
const NEWBIE_TRIAL_MINUTES = 1 * 24 * 60; // 新人体验期时长（分钟）
const NEWBIE_STORAGE_KEY = "动漫皮肤新人体验记录"; // 新人体验记录保存key

// 有效期设置（单位：分钟）
const SKIN_VALID_MINUTES = 7 * 24 * 60; // 7天
const STORAGE_KEY = "动漫皮肤领取记录"; //扩展记录保存key

// 皮肤分类数据
const skinData = {
    "Fate系列": {
        "1009007": "白贞德",
        "1009026": "黑saber",
        "1009027": "黑saber2",
        "1009082": "泳装枪手",
        "1009085": "远坂凛"
    },
    "东方Project": {
        "1009015": "东方 琪露诺",
        "1009042": "蕾米莉亚",
        "1009044": "铃仙",
        "1009078": "妖梦"
    },
    "独自升级": {
        "1009016": "独自升级",
        "1009017": "独自升级程小雨"
    },
    "海贼王": {
        "1009004": "艾尼路",
        "1009005": "艾斯",
        "1009025": "海贼王大和",
        "1009029": "红发香克斯",
        "1009050": "尼卡定制",
        "1009051": "尼卡路飞",
        "1009064": "四皇路飞",
        "1009054": "女帝",
        "1009073": "小南"
    },
    "火影忍者": {
        "1009006": "八门夜凯",
        "1009011": "雏田",
        "1009013": "迪达拉",
        "1009021": "纲手",
        "1009031": "火影四代目（永带妹）",
        "1009036": "卡卡西",
        "1009046": "鸣人小新",
        "1009047": "鸣人发光九尾模式",
        "1009075": "蝎",
        "1009083": "宇智波鼬",
        "1009086": "止水",
        "1009088": "佐助"
    },
    "绝区零": {
        "1009002": "Q版星见雅",
        "1009076": "星见雅"
    },
    "蜡笔小新": {
        "1009038": "恐龙小新",
        "1009039": "蜡笔小新 黑道",
        "1009040": "蜡笔小新",
        "1009052": "尼卡小新",
        "1009053": "尼卡小新绝版",
        "1009055": "骑车小新",
        "1009062": "睡衣小新"
    },
    "龙珠": {
        "1009019": "弗利沙",
        "1009048": "魔人布欧",
        "1009087": "自在极意 白悟空"
    },
    "犬夜叉": {
        "1009034": "桔梗X犬夜叉",
        "1009057": "杀生丸"
    },
    "拳皇": {
        "1009008": "不知火舞",
        "1009009": "草薙京"
    },
    "数码宝贝": {
        "1009014": "帝皇龙甲兽",
        "1009065": "天女兽"
    },
    "一拳超人": {
        "1009079": "一拳超人",
        "1009080": "一拳超人1"
    },
    "咒术回战": {
        "1009012": "纯爱战神",
        "1009049": "墨镜五条悟",
        "1009070": "五条悟合体",
        "1009071": "夏油杰"
    },
    "#e#b更多角色#k#n": {
        "1009000": "ALN4",
        "1009001": "新世纪福音战士 - EVA明日香",
        "1009003": "怪博士与机器娃娃 - 阿拉蕾",
        "1009010": "主播女孩重度依赖 - 超天酱",
        "1009018": "粉红幻影大剑 全残影",
        "1009020": "葬送的芙莉莲 - 芙莉莲",
        "1009022": "高达系列 - 高达",
        "1009023": "光能使者 - 光能使者阿祖",
        "1009024": "海绵宝宝 - 海绵宝宝",
        "1009028": "死神 - 黑崎一护",
        "1009030": "胜利女神：妮姬 - 红莲暗影",
        "1009032": "家庭教师 - 家庭教师",
        "1009033": "宝可梦 - 杰尼龟",
        "1009035": "Keroro军曹 - 军曹",
        "1009037": "瞌睡兔",
        "1009041": "原神 - 雷电将军",
        "1009043": "废渊战鬼 - 莉央完成",
        "1009045": "通灵王 - 麻仓叶",
        "1009056": "千与千寻 - 千寻",
        "1009058": "蛇女",
        "1009059": "圣斗士星矢 - 圣斗士白羊座",
        "1009060": "吴彦祖",
        "1009061": "美少女战士 - 水兵月",
        "1009063": "死灵姐姐",
        "1009066": "天使法",
        "1009067": "天使枪",
        "1009068": "小林家的龙女仆 - 托尔龙女仆",
        "1009069": "威爾",
        "1009072": "小恶魔",
        "1009077": "崩坏：星穹铁道 - 星穹",
        "1009081": "泳衣",
        "1009074": "魔卡少女 - 小樱",
        "1009084": "魔法少女小圆 - 圆神"
    }
}

const EVENT_START = parseDateTime(EVENT_START_STR);
const EVENT_END = parseDateTime(EVENT_END_STR);

let player;         // 玩家角色对象
let category;       // 当前选择的皮肤分类
let equippedSlot;   // 已装备槽位
let InventoryType;  // 背包类型
let InventoryManipulator; // 背包操作器
let isNewACTIVITY;      //是否为新参加活动

function start() {
    // 初始化必要对象
    if (!player) {
        player = cm.getPlayer();
        InventoryType = Java.type("org.gms.client.inventory.InventoryType");
        InventoryManipulator = Java.type("org.gms.client.inventory.manipulator.InventoryManipulator");
    }
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === 1) {
        checkSkinStatus(); // 玩家点击NPC进入对话
    } else {
        level(); // 玩家关闭对话框
    }
}

function level() {
    cm.dispose(); // 结束脚本
}

// 检查皮肤领取状态
function checkSkinStatus() {
    const storage = getSkinStorage();
    const newbieStorage = getNewbieStorage();
    const currentTime = Date.now();

    // 检查新人体验期资格（新增）- 优先级最高
    if (NEWBIE_TRIAL_ENABLED && isNewbieEligible(newbieStorage, storage)) {
        showNewbieTrialOffer(newbieStorage);
        return;
    }

    // 检查活动时间（只有正式活动受时间限制）
    if (currentTime < EVENT_START) {
        const startTime = new Date(EVENT_START).toLocaleString();
        const endTime = new Date(EVENT_END).toLocaleString();
        cm.sendOkLevel("",
            `【${ACTIVITY_NAME}】\r\n活动尚未开始！\r\n开始时间：${startTime}\r\n结束时间：${endTime}`
        );
        return;
    }

    if (currentTime > EVENT_END) {
        const startTime = new Date(EVENT_START).toLocaleString();
        const endTime = new Date(EVENT_END).toLocaleString();
        cm.sendOkLevel("",
            `【${ACTIVITY_NAME}】\r\n活动已结束！\r\n开始时间：${startTime}\r\n结束时间：${endTime}`
        );
        return;
    }

    // 格式化活动时间显示
    const changeLimit = (MAX_CHANGES === null || MAX_CHANGES <= 0) ? "无限制" : `${MAX_CHANGES}次`;

    // 未领取或已过期
    if (!storage) {
        isNewACTIVITY = true;
        const validDisplay = formatMinutes(SKIN_VALID_MINUTES);
        cm.sendNextLevel("showCategorySelection",
            `#e#r【${ACTIVITY_NAME}】#n#k\r\n
    你好 #e#b#h ##n#k！ 欢迎领取限量动漫皮肤！\r\n
    #b活动时间：#k${EVENT_START_STR}\r\n
    \t\t\t\t\t${EVENT_END_STR}\r\n
    #b更换限制：#k${changeLimit}\r\n
    #b领取规则：#k\r\n
    * 每个账号限领一次\r\n
    * 有效期: #r${validDisplay}#k\r\n
    * 有效期内可随时更换其他皮肤\r\n
    * 本次活动将会#r#e回收#n#k以往活动的时限皮肤\r\n
    * #e#r如果领取后客户端闪退，请更新游戏补丁#n#k\r\n\r\n
    准备好领取你的专属皮肤了吗？`
        );
    }
    // 已领取且在有效期内
    else if (currentTime <= storage.expire) {
        const remainingTime = (storage.expire - currentTime) / 60000;
        const remainingDisplay = formatMinutes(remainingTime);
        if (remainingTime <= 5) {
            cm.sendOkLevel("","皮肤有效期已时间小于5分钟，无法进行更换了。");
            return;
        }
        // 检查更换次数限制（新增）
        const changesLeft = (MAX_CHANGES === null || MAX_CHANGES <= 0) ?
            "无限制" :
            `${Math.max(0, MAX_CHANGES - (storage.changes || 0))}次`;

        cm.sendNextLevel("showCategorySelection",
            `#e#d【${ACTIVITY_NAME}】#n#k\r\n
    欢迎回来 #e#b#h ##n#k！\r\n
    你当前拥有 #b动漫皮肤兑换权限#k\r\n
    已更换次数: #r${storage.changes || 0}次#k (剩余: ${changesLeft})\r\n
    剩余有效期: #r${remainingDisplay}#k\r\n\r\n
    你可以在有效期内随时更换其他皮肤，保持相同有效期。`
        );
    }
    // 已过期
    else {
        cm.sendOkLevel("", "你的皮肤领取资格已过期，无法再次领取。");
    }
}

// 检查新人体验期资格（新增）- 修复判断逻辑
function isNewbieEligible(newbieStorage, storage) {
    if (!NEWBIE_TRIAL_ENABLED) return false;

    const currentTime = Date.now();

    // 如果已经有正式活动皮肤记录，则不能体验
    if (storage && storage.activityId === ACTIVITY_ID) {
        return false;
    }

    // 检查体验期状态
    if (newbieStorage) {
        if (newbieStorage.activityId === "NEWBIE_SKIPPED") {
            return false; // 已跳过体验期
        }
        if (newbieStorage.activityId === "NEWBIE_TRIAL") {
            return currentTime <= newbieStorage.expire; // 体验期进行中
        }
    }

    // 全新玩家：从未参加过正式活动且没有体验期记录
    return !storage;
}

// 显示新人体验期提示（新增）
function showNewbieTrialOffer(newbieStorage) {
    const currentTime = Date.now();
    const trialDisplay = formatMinutes(NEWBIE_TRIAL_MINUTES);

    if (newbieStorage && newbieStorage.activityId === "NEWBIE_TRIAL" && currentTime <= newbieStorage.expire) {
        // 体验期进行中
        const remainingTime = (newbieStorage.expire - currentTime) / 60000;
        const remainingDisplay = formatMinutes(remainingTime);

        cm.sendNextLevel("showCategorySelection",
            `#e#b【新人专属体验期】#n#k\r\n
    欢迎 #e#b#h ##n#k！你正在享受新人体验期！\r\n
    #b体验特权：#k\r\n
    * 免费试用所有动漫皮肤\r\n
    * 剩余体验时间: #r${remainingDisplay}#k\r\n
    * 体验期结束后可参加正式活动\r\n\r\n
    请选择你喜欢的皮肤开始体验吧！`
        );
    } else {
        // 新人有资格体验
        cm.sendNextSelectLevel("handleNewbieTrialChoice",
            `#e#g【新人专属体验期】#n#k\r\n
    欢迎来到动漫世界，亲爱的 #e#b#h ##n#k！\r\n
    作为新角色，你可以享受 #r${trialDisplay}#k 的专属体验期！\r\n\r\n
    #b体验特权：#k\r\n
    * 免费试用所有动漫皮肤\r\n
    * 无限制更换次数\r\n
    * 体验期结束后可参加正式活动\r\n\r\n
    是否立即开始体验？\r\n
    #L0# 好的，开始体验！#l\r\n
    #L1# 跳过体验，直接参加正式活动#l`
        );
    }
}

// 处理新人体验期选择（新增）
function levelhandleNewbieTrialChoice(selection) {
    if (selection === 0) {
        // 开始体验期
        startNewbieTrial();
    } else if (selection === 1) {
        // 跳过体验期，直接进入正式活动
        skipNewbieTrial();
    } else {
        cm.sendOkLevel("", "选择无效，请重新操作。");
    }
}

// 开始新人体验期（新增）
function startNewbieTrial() {
    const currentTime = Date.now();
    const expireTime = currentTime + (NEWBIE_TRIAL_MINUTES * 60000);

    const newbieStorage = {
        activityId: "NEWBIE_TRIAL",
        startTime: currentTime,
        expire: expireTime,
        skinId: null,
        changes: 0
    };

    cm.saveOrUpdateCharacterExtendValue(NEWBIE_STORAGE_KEY, JSON.stringify(newbieStorage));

    const trialDisplay = formatMinutes(NEWBIE_TRIAL_MINUTES);
    cm.sendNextLevel("showCategorySelection",
        `#e#g【体验期开始】#n#k\r\n
    恭喜！你已成功开启 #r${trialDisplay}#k 的新人体验期！\r\n
    现在你可以自由选择喜欢的皮肤进行体验。\r\n\r\n
    体验期结束后记得来参加正式活动哦！`
    );
}

// 跳过新人体验期（新增）
function skipNewbieTrial() {
    const currentTime = Date.now();
    const newbieStorage = {
        activityId: "NEWBIE_SKIPPED",
        skipTime: currentTime
    };

    cm.saveOrUpdateCharacterExtendValue(NEWBIE_STORAGE_KEY, JSON.stringify(newbieStorage));

    // 检查是否在活动时间内
    if (currentTime < EVENT_START || currentTime > EVENT_END) {
        cm.sendOkLevel("",
            "你已跳过体验期，但当前不在活动时间内。\r\n\r\n" +
            `活动时间：${EVENT_START_STR} 至 ${EVENT_END_STR}`
        );
        return;
    }

    // 进入正式活动流程
    isNewACTIVITY = true;
    const validDisplay = formatMinutes(SKIN_VALID_MINUTES);
    const changeLimit = (MAX_CHANGES === null || MAX_CHANGES <= 0) ? "无限制" : `${MAX_CHANGES}次`;

    cm.sendNextLevel("showCategorySelection",
        `#e#r【${ACTIVITY_NAME}】#n#k\r\n
    你好 #e#b#h ##n#k！ 欢迎领取限量动漫皮肤！\r\n
    #b活动时间：#k${EVENT_START_STR}\r\n
    \t\t\t\t\t${EVENT_END_STR}\r\n
    #b更换限制：#k${changeLimit}\r\n
    #b领取规则：#k\r\n
    * 每个账号限领一次\r\n
    * 有效期: #r${validDisplay}#k\r\n
    * 有效期内可随时更换其他皮肤\r\n
    * 本次活动将会#r#e回收#n#k以往活动的时限皮肤\r\n\r\n
    准备好领取你的专属皮肤了吗？`
    );
}

// 获取皮肤存储信息
function getSkinStorage() {
    const storageJson = cm.getCharacterExtendValue(STORAGE_KEY);
    try {
        if (!storageJson) return null;

        const storage = JSON.parse(storageJson);

        // 检查活动ID是否匹配（新增）
        if (storage.activityId !== ACTIVITY_ID) {
            cm.saveOrUpdateCharacterExtendValue(STORAGE_KEY, ""); // 清空不匹配的记录
            return null;
        }

        return storage;
    } catch (e) {
        return null; // JSON解析失败返回null
    }
}

// 获取新人体验期存储信息（新增）
function getNewbieStorage() {
    const storageJson = cm.getCharacterExtendValue(NEWBIE_STORAGE_KEY);
    try {
        if (!storageJson) return null;
        return JSON.parse(storageJson);
    } catch (e) {
        return null;
    }
}

// 更新皮肤存储信息
function updateSkinStorage(skinId, isNew = false) {
    const currentTime = Date.now();
    let storage = getSkinStorage() || {};

    if (isNew || !storage.activityId) {
        // 新领取或活动ID不匹配：设置新记录
        storage = {
            activityId: ACTIVITY_ID,
            activityName: ACTIVITY_NAME,
            skinId: skinId,
            expire: currentTime + (SKIN_VALID_MINUTES * 60000),
            changes: 0,
            maxChanges: MAX_CHANGES
        };
    } else {
        // 更换：增加更换次数
        storage.skinId = skinId;
        storage.changes = (storage.changes || 0) + 1;
    }

    cm.saveOrUpdateCharacterExtendValue(STORAGE_KEY, JSON.stringify(storage));
    return storage;
}

// 更新体验期存储信息（新增）
function updateNewbieStorage(skinId) {
    const currentTime = Date.now();
    let newbieStorage = getNewbieStorage() || {};

    if (newbieStorage.activityId === "NEWBIE_TRIAL") {
        newbieStorage.skinId = skinId;
        newbieStorage.changes = (newbieStorage.changes || 0) + 1;
        cm.saveOrUpdateCharacterExtendValue(NEWBIE_STORAGE_KEY, JSON.stringify(newbieStorage));
    }

    return newbieStorage;
}

// 显示皮肤分类选择
function levelshowCategorySelection() {
    const categories = Object.keys(skinData);
    let categoryList = "";

    categories.forEach((cat, index) => {
        categoryList += `#L${index}# ${cat} #l\r\n`;
    });

    // 检查是否为体验期
    const newbieStorage = getNewbieStorage();
    const isTrial = newbieStorage && newbieStorage.activityId === "NEWBIE_TRIAL";

    const title = isTrial ? "请选择要体验的动漫分类：" : "请选择动漫分类：";

    cm.sendNextSelectLevel("handleCategorySelection",`${title}\r\n${categoryList}`);
}

// 处理分类选择
function levelhandleCategorySelection(selection) {
    const categories = Object.keys(skinData);

    if (selection >= 0 && selection < categories.length) {
        showSkinSelection(categories[selection]);
    } else {
        cm.sendOkLevel("", "选择无效，请重新操作。");
    }
}

// 显示具体皮肤选择
function showSkinSelection(select) {
    category = select;
    const skins = skinData[category];
    let skinList = "";
    let index = 0;

    for (const [skinId, skinName] of Object.entries(skins)) {
        skinList += `#L${index}##i${skinId}# #b${skinName}#k #l\r\n`;
        index++;
    }

    // 检查是否为体验期
    const newbieStorage = getNewbieStorage();
    const isTrial = newbieStorage && newbieStorage.activityId === "NEWBIE_TRIAL";

    const title = isTrial ?
        `请选择要体验的 #r${category}#k 分类皮肤：` :
        `请选择 #r${category}#k 分类中的皮肤：`;

    cm.sendNextSelectLevel("handleSkinSelection",`${title}\r\n${skinList}`);
}

// 处理皮肤选择
function levelhandleSkinSelection(selection) {
    if (!category) {
        cm.sendOkLevel("", "分类选择错误，请重新开始。");
        return;
    }

    const skins = skinData[category];
    const skinEntries = Object.entries(skins);

    if (selection >= 0 && selection < skinEntries.length) {
        const [skinIdStr, skinName] = skinEntries[selection];
        const skinId = parseInt(skinIdStr);
        const storage = getSkinStorage();
        const newbieStorage = getNewbieStorage();
        const currentTime = Date.now();

        // 检查是否为体验期
        const isTrial = newbieStorage && newbieStorage.activityId === "NEWBIE_TRIAL" && currentTime <= newbieStorage.expire;

        if (isTrial) {
            // 体验期逻辑 - 回收所有带期限皮肤并发放新皮肤
            removeAllTimedSkins(); // 回收所有带期限皮肤
            grantTrialSkin(skinId, skinName, newbieStorage);
        } else {
            // 正式活动逻辑
            // 检查有效期
            if (storage && currentTime > storage.expire) {
                cm.sendOkLevel("", "你的皮肤领取资格已过期，无法更换。");
                return;
            }

            // 检查更换次数限制（新增）
            if (MAX_CHANGES > 0 && storage && storage.changes >= MAX_CHANGES) {
                cm.sendOkLevel("", `你已经达到了最大更换次数(${MAX_CHANGES}次)，无法继续更换皮肤。`);
                return;
            }

            const isNew = !storage || currentTime > storage.expire;
            const expireTime = isNew
                ? currentTime + (SKIN_VALID_MINUTES * 60000)
                : storage.expire;

            // 移除旧皮肤
            let success = removeOldSkin(storage?.skinId);
            if (success === "success") {
                // 先尝试发放皮肤
                const grantSuccess = grantSkin(skinId, skinName, expireTime);
                if (grantSuccess) {
                    // 发放成功后再更新记录
                    updateSkinStorage(skinId, isNew);
                }
            } else {
                cm.sendOkLevel("", success);
            }
        }
    } else {
        cm.sendOkLevel("", "选择无效，请重新操作。");
    }
}

// 发放体验期皮肤（新增）- 修复回收逻辑
function grantTrialSkin(skinId, skinName, newbieStorage) {
    const currentTime = Date.now();
    const remainingMinutes = (newbieStorage.expire - currentTime) / 60000;

    // 检查体验期是否已结束
    if (remainingMinutes <= 0) {
        cm.sendOkLevel("", "你的体验期已结束，请参加正式活动。");
        return false;
    }

    const remainingDisplay = formatMinutes(remainingMinutes);

    // 发放体验皮肤
    const success = player.gainEquip(
        skinId,
        0, 0, 0, 0,   // str, dex, int, luk
        0, 0, 0,       // hp, mp, pAtk
        0, 0, 0,       // mAtk, pDef, mDef
        0, 0, 0,       // acc, avoid, hands
        0, 0, 0,       // speed, jump, upgradeSlot
        Math.round(remainingMinutes)  // expireTime (分钟)
    );

    if (success) {
        // 更新体验期记录
        updateNewbieStorage(skinId);

        // 自动装备皮肤
        if (equippedSlot) {
            let items = player.getInventory(InventoryType.EQUIP).listById(skinId);
            const expiredItem = items.find(item => item.getExpiration() > 0);
            if (expiredItem) InventoryManipulator.equip(player.getClient(), expiredItem.getPosition(), equippedSlot);
        }

        cm.sendOkLevel("",
            `#e#g[体验成功]#n#k\r\n
    你正在体验 #r${skinName}#k 皮肤！\r\n
    体验期剩余: #b${remainingDisplay}#k\r\n
    已更换次数: #r${newbieStorage.changes || 0}次#k\r\n\r\n
    皮肤${equippedSlot ? "已经给你装备好了" : "已发送到背包，请查收"}！\r\n
    体验期内可随时回来更换其他皮肤。`
        );
    } else {
        cm.sendOkLevel("", "皮肤体验失败，请稍后再试。");
    }
    return success;
}

// 发放皮肤
function grantSkin(skinId, skinName, expireTimestamp) {
    const currentTime = Date.now();

    // 计算剩余时间（分钟）并向下取整（新增需求）
    let remainingMinutes = (expireTimestamp - currentTime) / 60000;
    remainingMinutes = Math.floor(Math.max(0, remainingMinutes));

    // 检查剩余时间是否有效（新增需求）
    if (remainingMinutes <= 0) {
        cm.sendOkLevel("", "你的皮肤领取资格已过期，无法发放皮肤。");
        return false;
    }
    const remainingDisplay = formatMinutes(remainingMinutes);

    if (isNewACTIVITY) removeEquippedCap();//如果是参加新活动则回收以往活动的时限皮肤

    // 发放新皮肤（无属性加成）
    const success = player.gainEquip(//返回Boolen
        skinId,
        0, 0, 0, 0,   // str, dex, int, luk
        0, 0, 0,       // hp, mp, pAtk
        0, 0, 0,       // mAtk, pDef, mDef
        0, 0, 0,       // acc, avoid, hands
        0, 0, 0,       // speed, jump, upgradeSlot
        Math.round(remainingMinutes)  // expireTime (分钟)
    );

    if (success) {
        // 如果之前有装备皮肤，自动装备新皮肤
        if (equippedSlot) {
            let items = player.getInventory(InventoryType.EQUIP).listById(skinId);   //发放装备后需要重新获取所在格子，以免错位导致客户端闪退
            const expiredItem = items.find(item => item.getExpiration() > 0); // 查找第一个带期限的皮肤
            if (expiredItem) InventoryManipulator.equip(player.getClient(), expiredItem.getPosition(), equippedSlot);//将皮肤穿上
        }

        cm.sendOkLevel("",
            `#e#g[领取成功]#n#k\r\n
    恭喜获得 #r${skinName}#k 皮肤！\r\n
    有效期剩余: #b${remainingDisplay}#k\r\n\r\n
    皮肤${equippedSlot ? "已经给你装备好了" : "已发送到背包，请查收"}！\r\n
    有效期内可随时回来更换其他皮肤。`
        );
    } else {
        cm.sendOkLevel("", "皮肤发放失败，请稍后再试。");
    }
    return success;
}

// 移除旧皮肤
function removeOldSkin(oldSkinId) {
    // 没有旧皮肤时直接返回成功（新增）
    if (!oldSkinId) return "success";
    // 1. 检查已装备的皮肤
    const equipped = player.getInventory(InventoryType.EQUIPPED); //已装备
    const equipInventory = player.getInventory(InventoryType.EQUIP); //装备栏
    const equippedItem = equipped.listById(oldSkinId)?.[0];
    let skinCount = 0;

    // 处理已装备的皮肤
    if (equippedItem && equippedItem?.getExpiration() > 0) { //已装备存在目标ID装备且为时限
        if (equipInventory.isFull()) { //判断背包是否已满
            return "背包装备栏已满，无法更换皮肤。";
        } else {
            equippedSlot = equippedItem.getPosition(); //记录装备槽位类型
            InventoryManipulator.removeFromSlot(player.getClient(), InventoryType.EQUIPPED, equippedSlot, 1, true); //直接移除已装备的皮肤
            skinCount += 1;
        }
    }

    // 2. 检查背包中的皮肤
    const equipItems = equipInventory.listById(oldSkinId); //获取指定ID的皮肤Item列表
    // 处理背包中的皮肤
    if (equipItems.length >= 1) {
        equipItems.forEach(equipItem => { //遍历并删除符合条件的装备
            if (equipItem.getExpiration() > 0) {
                InventoryManipulator.removeFromSlot(player.getClient(), InventoryType.EQUIP, equipItem.getPosition(), 1, true); //移除背包里所有包含时限的同款皮肤
                skinCount += 1;
            }
        })
    }

    // 移除皮肤
    if (skinCount > 0) {
        return "success";
    } else {
        return "你没有时限皮肤可以更换。";
    }
}

// 回收所有带期限皮肤（新增）- 用于体验期
function removeAllTimedSkins() {
    const equipped = player.getInventory(InventoryType.EQUIPPED);
    const equipInventory = player.getInventory(InventoryType.EQUIP);
    let removedCount = 0;

    // 获取所有皮肤ID
    const allSkinIds = [];
    for (const category in skinData) {
        for (const skinId in skinData[category]) {
            allSkinIds.push(parseInt(skinId));
        }
    }

    // 检查已装备的皮肤
    for (const skinId of allSkinIds) {
        const equippedItems = equipped.listById(skinId);
        for (const item of equippedItems) {
            if (item.getExpiration() > 0) {
                equippedSlot = item.getPosition(); // 记录装备槽位
                InventoryManipulator.removeFromSlot(player.getClient(), InventoryType.EQUIPPED, item.getPosition(), 1, true);
                removedCount++;
            }
        }
    }

    // 检查背包中的皮肤
    for (const skinId of allSkinIds) {
        const equipItems = equipInventory.listById(skinId);
        for (const item of equipItems) {
            if (item.getExpiration() > 0) {
                InventoryManipulator.removeFromSlot(player.getClient(), InventoryType.EQUIP, item.getPosition(), 1, true);
                removedCount++;
            }
        }
    }

    return removedCount;
}

// 格式化分钟数为易读格式
function formatMinutes(minutes) {
    minutes = Math.floor(minutes);

    const days = Math.floor(minutes / 1440);
    const hours = Math.floor((minutes % 1440) / 60);
    const mins = minutes % 60;

    let result = "";
    if (days > 0) result += `${days}天`;
    if (hours > 0) result += `${result ? " " : ""} ${hours}小时`;
    if (mins > 0) result += `${result ? " " : ""} ${mins}分`;

    return result || "0分";
}

// 时间字符串解析函数（新增）
function parseDateTime(dateTimeStr) {
    const [datePart, timePart] = dateTimeStr.split(" ");
    const [year, month, day] = datePart.split("-").map(Number);
    const [hours, minutes, seconds] = timePart.split(":").map(Number);

    // 注意：JavaScript的Date对象月份是从0开始的（0=1月）
    return new Date(year, month - 1, day, hours, minutes, seconds).getTime();
}

function removeEquippedCap() {
    const equipped = player.getInventory(InventoryType.EQUIPPED);
    const equipInventory = player.getInventory(InventoryType.EQUIP);
    let removedCount = 0;

    // 获取所有皮肤ID
    const allSkinIds = [];
    for (const category in skinData) {
        for (const skinId in skinData[category]) {
            allSkinIds.push(parseInt(skinId));
        }
    }

    // 检查已装备的皮肤
    for (const skinId of allSkinIds) {
        const equippedItems = equipped.listById(skinId);
        for (const item of equippedItems) {
            if (item.getExpiration() > 0) {
                InventoryManipulator.removeFromSlot(player.getClient(), InventoryType.EQUIPPED, item.getPosition(), 1, true);
                removedCount++;
            }
        }
    }

    // 检查背包中的皮肤
    for (const skinId of allSkinIds) {
        const equipItems = equipInventory.listById(skinId);
        for (const item of equipItems) {
            if (item.getExpiration() > 0) {
                InventoryManipulator.removeFromSlot(player.getClient(), InventoryType.EQUIP, item.getPosition(), 1, true);
                removedCount++;
            }
        }
    }

    if (removedCount > 0) {
        cm.getPlayer().dropMessage(5, `已回收 ${removedCount} 个过期皮肤。`);
    }
}