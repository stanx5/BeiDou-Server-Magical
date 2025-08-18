// 有效期设置（单位：分钟）
const SKIN_VALID_MINUTES = 4320; // 3天
const STORAGE_KEY = "动漫皮肤领取记录";

// 皮肤分类数据
const skinData = {
    "火影忍者": {
        1009011: "雏田",
        1009036: "卡卡西",
        1009046: "鸣人小新",
        1009047: "鸣人发光九尾模式",
        1009083: "宇智波鼬",
        1009086: "止水",
        1009088: "佐助",
        1009073: "小南",
        1009075: "蝎",
        1009013: "迪达拉",
        1009031: "火影四代目"
    },
    "海贼王": {
        1009005: "艾斯",
        1009025: "海贼王大和",
        1009029: "红发香克斯",
        1009054: "女帝",
        1009050: "尼卡定制",
        1009051: "尼卡路飞",
        1009064: "四皇路飞"
    },
    "经典动漫": {
        1009003: "阿拉蕾",
        1009008: "不知火舞",
        1009009: "草薙京",
        1009020: "芙莉莲",
        1009024: "海绵宝宝",
        1009045: "麻仓叶",
        1009057: "杀生丸",
        1009079: "一拳超人"
    },
    "蜡笔小新系列": {
        1009038: "恐龙小新",
        1009039: "蜡笔小新 黑道",
        1009040: "蜡笔小新",
        1009052: "尼卡小新",
        1009055: "骑车小新",
        1009062: "睡衣小新"
    },
    "游戏角色": {
        1009001: "EVA明日香",
        1009002: "Q版星见雅",
        1009010: "超天酱",
        1009041: "雷电将军",
        1009076: "星见雅",
        1009077: "星穹",
        1009084: "圆神"
    },
    "其他角色": {
        1009007: "白贞德",
        1009012: "纯爱战神",
        1009014: "帝皇龙甲兽",
        1009015: "东方 琪露诺",
        1009018: "粉红幻影大剑",
        1009019: "弗利沙",
        1009022: "高达",
        1009030: "红莲暗影",
        1009049: "墨镜五条悟",
        1009053: "尼卡小新绝版",
        1009070: "五条悟合体",
        1009071: "夏油杰",
        1009078: "妖梦",
        1009085: "远坂凛"
    }
};

let player;         //玩家角色对象
let category;       //皮肤索引
let equipSlot;      //脱下装备占用格子
let equippedSlot;   //已装备槽位
let InventoryType;
let InventoryManipulator;

function start() {
    if (!player) player = cm.getPlayer();
    if (!InventoryType) InventoryType = Java.type("org.gms.client.inventory.InventoryType");
    if (!InventoryManipulator) InventoryManipulator = Java.type("org.gms.client.inventory.manipulator.InventoryManipulator");

    // test();
    // return;
    action(1, 0, 0);
}

function level() {
    cm.dispose();
}

function action(mode) {
    if (mode === 1) {
        checkSkinStatus();
    } else {
        level();
    }
}


// 检查皮肤状态
function checkSkinStatus() {
    const storage = getSkinStorage();
    const currentTime = Date.now();

    if (!storage) {
        // 未领取或已过期
        const validDisplay = formatMinutes(SKIN_VALID_MINUTES);
        cm.sendNextLevel("showCategorySelection",
    `#e#r[动漫皮肤福利]#n#k\r\n
    你好 #h #！你可以在这里领取一款限量动漫皮肤！\r\n
    #b领取规则：#k\r\n
    * 每个账号限领一次\r\n
    * 有效期: #r${validDisplay}#k\r\n
    * 有效期内可随时更换其他皮肤\r\n
    * #e#r如果领取后客户端闪退，请更新游戏补丁#n#k\r\n\r\n
    准备好领取你的专属皮肤了吗？`
        );
    } else if (currentTime <= storage.expire) {
        // 已领取且在有效期内
        const remainingTime = (storage.expire - currentTime) / 60000;
        const remainingDisplay = formatMinutes(remainingTime);

        cm.sendNextLevel("showCategorySelection",
    `#e#d[皮肤更换中心]#n#k\r\n
    欢迎回来 #h #！\r\n
    你当前拥有 #b动漫皮肤兑换权限#k\r\n
    已更换次数: #r${storage.changes || 0}次#k\r\n
    剩余有效期: #r${remainingDisplay}#k\r\n\r\n
    你可以在有效期内随时更换其他皮肤，保持相同有效期。`
        );
    } else {
        cm.sendOkLevel("","你已经领取过了，无法再次领取。");
    }
}

// 获取或初始化皮肤存储
function getSkinStorage() {
    const storageJson = cm.getCharacterExtendValue(STORAGE_KEY);

    try {
        return storageJson ? JSON.parse(storageJson) : null;
    } catch (e) {
        return null;
    }
}

// 更新皮肤存储
function updateSkinStorage(skinId, isNew = false) {
    const currentTime = Date.now();
    let storage = getSkinStorage() || {};

    if (isNew) {
        // 新领取：设置新有效期
        storage = {
            skinId,
            expire: currentTime + (SKIN_VALID_MINUTES * 60000),
            changes: 0
        };
    } else {
        // 更换：保留原有效期，增加更换次数
        storage.skinId = skinId;
        storage.changes = (storage.changes || 0) + 1;
    }

    cm.saveOrUpdateCharacterExtendValue(STORAGE_KEY, JSON.stringify(storage));
    return storage;
}

// 显示分类选择
function levelshowCategorySelection() {
    const categories = Object.keys(skinData);
    let categoryList = "";

    categories.forEach((category, index) => {
        categoryList += `#L${index}# ${category} #l\r\n`;
    });

    cm.sendNextSelectLevel("handleCategorySelection",`请选择你喜欢的动漫分类：\r\n${categoryList}`);
}

// 处理分类选择
function levelhandleCategorySelection(selection) {
    const categories = Object.keys(skinData);

    if (selection >= 0 && selection < categories.length) {
        showSkinSelection(categories[selection]);
    } else {
        cm.sendOkLevel("","无效的选择，请重新开始。");
    }
}

// 显示皮肤选择
function showSkinSelection(select) {
    category = select;
    const skins = skinData[category];
    let skinList = "";
    let index = 0;

    for (const [skinId, skinName] of Object.entries(skins)) {
        skinList += `#L${index}##i${skinId}# #b${skinName}#k #l\r\n`;
        index++;
    }

    cm.sendNextSelectLevel("handleSkinSelection",`请选择 #r${category}#k 分类中的皮肤：\r\n${skinList}`);
}

// 处理皮肤选择
function levelhandleSkinSelection(selection) {
    if (!category) {
        cm.sendOkLevel("","选项错误，请重新选择。");
        return;
    }
    const skins = skinData[category];
    const skinEntries = Object.entries(skins);

    if (selection >= 0 && selection < skinEntries.length) {
        const [skinIdStr, skinName] = skinEntries[selection];
        const skinId = parseInt(skinIdStr);

        const storage = getSkinStorage();
        const isNew = !storage || Date.now() > storage.expire;

        // 移除旧皮肤（如果有）
        if (storage?.skinId) {
            removeOldSkin(storage.skinId);
        }

        // 更新存储并发放新皮肤
        const newStorage = updateSkinStorage(skinId, isNew);
        grantSkin(skinId, skinName, newStorage.expire);
    } else {
        cm.sendOkLevel("","无效的选择，请重新开始。");
    }
}

// 发放皮肤
function grantSkin(skinId, skinName, expireTimestamp) {
    const currentTime = Date.now();

    // 计算剩余时间（分钟）
    const remainingMinutes = Math.max(0, (expireTimestamp - currentTime) / 60000);
    const remainingDisplay = formatMinutes(remainingMinutes);

    // 发放新皮肤（无属性加成）
    state = player.gainEquip(
        skinId,
        0, 0, 0, 0,   // str, dex, int, luk, hp
        0, 0, 0,         // mp, pAtk, mAtk
        0, 0, 0,         // pDef, mDef, acc
        0, 0, 0,         // avoid, hands, speed
        0, 0, 0,              // jump, upgradeSlot
        Math.round(remainingMinutes)  // expireTime (分钟)
    );
    if (state) {
        if (equippedSlot && equipSlot) {
            console.log();
            InventoryManipulator.equip(player.getClient(), equipSlot, equippedSlot);    //从背包装备栏装备到已装备槽位里
        }
        cm.sendOkLevel("",
    `#e#g[领取成功]#n#k\r\n
    恭喜你获得了 #r${skinName}#k 皮肤！\r\n
    有效期剩余: #b${remainingDisplay}#k\r\n\r\n
    皮肤已发送到你的背包，请查收！\r\n
    在有效期内可随时回来更换其他皮肤。`
        );
        return remainingMinutes;
    } else {
        cm.sendOkLevel("","皮肤发放失败");
    }
}

// 移除旧皮肤
function removeOldSkin(oldSkinId) {
    try {
        //检查身上装备
        const equipped = player.getInventory(InventoryType.EQUIPPED);
        const equippedItems = equipped.listById(oldSkinId);
        //检查背包装备栏
        const equipInventory = player.getInventory(InventoryType.EQUIP);
        const equipItems = equipInventory.listById(oldSkinId);
        //获取背包装备栏空格子
        const gridisFull = equipInventory.isFull(); //检查格子是否已满
        const gridCount = equipInventory.getSlotLimit();    //总格子数

        if (equippedItems.length >= 1) {
            if (gridisFull) {
                cm.sendOkLevel("","背包装备栏已满，无法更换皮肤。")
            } else {
                for (i = 1; i < gridCount; i++) {
                    if (!equipInventory.getItem(i)) {//检测item是否为null
                        equipSlot = i;  //记录哪个格子为空，用于存放脱下的装备
                        break;
                    }
                }
                equippedSlot = equippedItems[0].getPosition();  // 装备在已装备的哪个槽位
                InventoryManipulator.unequip(player.getClient(),equippedSlot, equipSlot);    //脱下装备到背包装备栏
            }
        } else if (equipItems.length >= 1) {
            equipSlot = equipItems[0].getPosition();        //记录装备在背包哪个格子里
        } else {
            player.dropMessage(5, `你没有任何皮肤，无法进行更换。`);
            return false;
        }
        InventoryManipulator.removeFromSlot(player.getClient(),InventoryType.EQUIP,equipSlot,1,true);   //移除背包里的装备
        return true;
    } catch (e) {
        player.dropMessage(5, "移除旧皮肤时出错");
        throw new RuntimeException(e);
    }
}

// 格式化分钟数为易读格式
function formatMinutes(minutes) {
    minutes = Math.floor(minutes);

    if (minutes >= 1440) {
        const days = Math.floor(minutes / 1440);
        const hours = Math.floor((minutes % 1440) / 60);
        return `${days}天 ${hours > 0 ? hours + "小时 " : ""}`;
    }

    if (minutes >= 60) {
        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;
        return `${hours}小时 ${mins > 0 ? mins + "分钟" : ""}`;
    }

    return `${minutes}分钟`;
}