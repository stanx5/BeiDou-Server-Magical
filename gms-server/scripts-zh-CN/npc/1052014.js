/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * @author: Ronan
 * @npc: Vending Machine
 * @map: 193000000 - Premium Road - Kerning City Internet Cafe
 * @func: Cafe PQ Rewarder
 */

/* 全局状态变量，用于跟踪NPC对话状态 */
var status;

/**
 * 奖励配置数据结构
 * @typedef {Object} TierConfig 层级配置
 * @property {string} name 层级显示名称
 * @property {Array<{id: number, qty: number}>} rewards 奖励物品列表
 * @property {number} [basePoints] 基础点数 (适用层级起始点)
 * @property {number} [pointsPerStep] 点数跨度 (每个层级需要的点数区间)
 * @property {number} [minPoints] 最低点数 (仅最高层使用)
 */

/**
 * 奖励系统核心配置
 * @type {{
 *  tiers: TierConfig[],
 *  coinItemId: number,
 *  eraserBaseId: number,
 *  maxTickets: number
 * }}
 */
const rewardConfig = {
    // 硬币道具ID
    coinItemId: 4001158,    // 女神的羽毛
    // 橡皮擦基础ID（4001009开始连续6个）
    eraserBaseId: 4001009,
    // 最大橡皮擦种类数量
    maxTickets: 6,
    // 层级配置数组（从低到高排序）
    tiers: [
        {
            name: "第1层",
            rewards: [
                //{ id: 物品ID, qty: 数量 },
                { id: 1302021, qty: 1 },  // 木制剑
                { id: 1302024, qty: 1 },  // 铁制剑
                { id: 1302033, qty: 1 },  // 钢制剑
                { id: 1082150, qty: 1 },  // 红色手套
                { id: 1002419, qty: 1 },  // 蓝色头巾
                { id: 2022053, qty: 20 }, // 红色药水
                { id: 2022054, qty: 20 }, // 蓝色药水
                { id: 2020032, qty: 20 }, // 魔法石
                { id: 2022057, qty: 20 }, // 超级药水
                { id: 2022096, qty: 20 }, // 幸运卷轴
                { id: 2022097, qty: 25 }, // 保护卷轴
                { id: 2022192, qty: 25 }, // 高级矿石
                { id: 2020030, qty: 25 }, // 星尘
                { id: 2010005, qty: 50 }, // 苹果
                { id: 2022041, qty: 50 }, // 魔法粉末
                { id: 2030000, qty: 12 }, // 回城卷轴
                { id: 2040100, qty: 1 },  // 头盔防御卷轴
                { id: 2040004, qty: 1 },  // 上衣防御卷轴
                { id: 2040207, qty: 1 },  // 手套攻击卷轴
                { id: 2048004, qty: 1 },  // 鞋子跳跃卷轴
                { id: 4031203, qty: 3 },  // 金属板材
                { id: 4000021, qty: 4 },   // 木柴
                { id: 4003005, qty: 2 },   // 银矿石
                { id: 4003000, qty: 2 },   // 铜矿石
                { id: 4003001, qty: 1 },   // 钢铁矿石
                { id: 4010000, qty: 2 },   // 红宝石原石
                { id: 4010001, qty: 2 },   // 蓝宝石原石
                { id: 4010002, qty: 2 },   // 绿宝石原石
                { id: 4010005, qty: 2 },   // 钻石原石
                { id: 4020004, qty: 2 }    // 水晶碎片
            ],
            basePoints: 0,    // 最低层级起始点
            maxPoints: 10       // 最大点数上限
        },
        {
            name: "第2层",
            rewards: [
                { id: 1022073, qty: 1 },  // 黄金耳环
                { id: 1012098, qty: 1 },  // 骷髅面具
                { id: 1012101, qty: 1 },  // 猫耳头饰
                { id: 1012102, qty: 1 },  // 兔耳头饰
                { id: 1012103, qty: 1 },  // 天使光环
                { id: 2022055, qty: 40 }, // 体力药水
                { id: 2022056, qty: 40 }, // 魔力药水
                { id: 2022103, qty: 40 }, // 攻击药水
                { id: 2020029, qty: 40 }, // 魔力结晶
                { id: 2020032, qty: 60 }, // 高级魔法石
                { id: 2020031, qty: 60 }, // 魔力精髓
                { id: 2022191, qty: 60 }, // 时间碎片
                { id: 2022016, qty: 60 }, // 祝福卷轴
                { id: 2043300, qty: 1 },  // 短剑攻击卷轴
                { id: 2043110, qty: 1 },  // 弓攻击卷轴
                { id: 2043800, qty: 1 },  // 拳套攻击卷轴
                { id: 2041001, qty: 1 },  // 盾牌防御卷轴
                { id: 2040903, qty: 1 },  // 披风魔力卷轴
                { id: 4031203, qty: 4 },  // 高级金属板
                { id: 4000021, qty: 6 },   // 橡树木材
                { id: 4003005, qty: 7 },   // 黄金矿石
                { id: 4003000, qty: 5 },   // 青铜矿石
                { id: 4003001, qty: 2 },   // 白金矿石
                { id: 4010000, qty: 4 },   // 完美红宝石
                { id: 4010001, qty: 4 },   // 完美蓝宝石
                { id: 4010003, qty: 3 },   // 星形绿宝石
                { id: 4010004, qty: 3 },   // 心形钻石
                { id: 4020004, qty: 4 },   // 魔力水晶
                { id: 3010004, qty: 1 },   // 战士勋章
                { id: 3010005, qty: 1 }    // 法师勋章
            ],
            basePoints: 20,      // 起始点数
            pointsPerStep: 10    // 每个层级跨度
        },
        {
            name: "第3层",
            rewards: [
                { id: 1302058, qty: 1 },  // 龙鳞剑
                { id: 1372008, qty: 1 },  // 魔导师法杖
                { id: 1422030, qty: 1 },  // 圣光长矛
                { id: 1422031, qty: 1 },  // 暗影长矛
                { id: 1022082, qty: 1 },  // 龙纹面具
                { id: 2022279, qty: 65 }, // 超级体力药水
                { id: 2022120, qty: 40 }, // 宗师魔力药水
                { id: 2001001, qty: 40 }, // 狼牙
                { id: 2001002, qty: 40 }, // 恶魔角
                { id: 2022071, qty: 25 }, // 幸运祝福卷轴
                { id: 2022189, qty: 25 }, // 时间扭曲卷轴
                { id: 2040914, qty: 1 },  // 短剑暴击卷轴
                { id: 2041001, qty: 1 },  // 盾牌生命卷轴
                { id: 2041041, qty: 1 },  // 头盔魔力卷轴
                { id: 2041308, qty: 1 },  // 耳环智力卷轴
                { id: 4031203, qty: 10 },  // 精炼金属板
                { id: 4000030, qty: 7 },   // 魔法木柴
                { id: 4003005, qty: 10 },  // 秘银矿石
                { id: 4003000, qty: 8 },   // 精金矿石
                { id: 4010004, qty: 5 },   // 星光红宝石
                { id: 4010006, qty: 5 },   // 月光蓝宝石
                { id: 4020000, qty: 5 },   // 火焰水晶
                { id: 4020006, qty: 5 },   // 冰霜水晶
                { id: 3010002, qty: 1 },   // 龙族勋章
                { id: 3010003, qty: 1 }    // 精灵勋章
            ],
            basePoints: 30,     // 起始点数
            pointsPerStep: 10    // 每个层级跨度
        },
        {
            name: "第4层",
            rewards: [
                { id: 1332029, qty: 1 },  // 暗影双刀
                { id: 1472027, qty: 1 },  // 炽炎拳套
                { id: 1462032, qty: 1 },  // 雷鸣长弓
                { id: 1492019, qty: 1 },  // 圣光弩
                { id: 2022045, qty: 45 }, // 神圣药水
                { id: 2022048, qty: 40 }, // 宗师体力药水
                { id: 2022094, qty: 25 }, // 神圣祝福卷轴
                { id: 2022123, qty: 20 }, // 时空扭曲卷轴
                { id: 2022058, qty: 60 }, // 超级魔力药水
                { id: 2041304, qty: 1 },  // 项链力量卷轴
                { id: 2041019, qty: 1 },  // 上衣生命卷轴
                { id: 2040826, qty: 1 },  // 手套敏捷卷轴
                { id: 2040758, qty: 1 },  // 鞋子速度卷轴
                { id: 4000030, qty: 10 },  // 龙鳞
                { id: 4003005, qty: 10 },  // 奥利哈钢
                { id: 4003000, qty: 20 },  // 星辰碎片
                { id: 4010007, qty: 5 },   // 永恒钻石
                { id: 4011003, qty: 1 },   // 贤者之石
                { id: 4021003, qty: 1 },   // 混沌水晶
                { id: 3010016, qty: 1 },   // 暗影勋章
                { id: 3010017, qty: 1 }    // 光明勋章
            ],
            basePoints: 40,     // 起始点数
            pointsPerStep: 10    // 每个层级跨度
        },
        {
            name: "第5层",
            rewards: [
                { id: 1382015, qty: 1 },  // 冰霜法杖
                { id: 1382016, qty: 1 },  // 烈焰法杖
                { id: 1442044, qty: 1 },  // 圣光巨斧
                { id: 1382035, qty: 1 },  // 暗影法杖
                { id: 2022310, qty: 20 }, // 时空水晶
                { id: 2022068, qty: 40 }, // 宗师生命药水
                { id: 2022069, qty: 40 }, // 宗师魔力药水
                { id: 2022190, qty: 30 }, // 神圣祝福卷轴
                { id: 2022047, qty: 30 }, // 超级恢复药水
                { id: 2040727, qty: 1 },  // 传说头盔卷轴
                { id: 2040924, qty: 1 },  // 传说武器卷轴
                { id: 2040501, qty: 1 },  // 传说鞋子卷轴
                { id: 4000030, qty: 20 },  // 凤凰羽毛
                { id: 4003005, qty: 20 },  // 星辰精华
                { id: 4003000, qty: 25 },  // 月光精华
                { id: 4011003, qty: 3 },   // 永恒之火
                { id: 4011006, qty: 2 },   // 冰霜之心
                { id: 4021004, qty: 3 },   // 雷电核心
                { id: 3010099, qty: 1 }    // 传奇勋章
            ],
            basePoints: 50,     // 起始点数
            pointsPerStep: 10    // 每个层级跨度
        },
        {
            name: "第6层",
            rewards: [
                { id: 1442046, qty: 1 },  // 龙之怒
                { id: 1432018, qty: 1 },  // 朱雀弓
                { id: 1102146, qty: 1 },  // 圣光铠甲
                { id: 1102145, qty: 1 },  // 暗影战甲
                { id: 2022094, qty: 35 }, // 神圣药水
                { id: 2022544, qty: 15 }, // 终极魔力秘药
                { id: 2022123, qty: 20 }, // 宗师卷轴
                { id: 2022310, qty: 20 }, // 时空水晶
                { id: 2040727, qty: 1 },  // 传说头盔卷轴
                { id: 2041058, qty: 1 },  // 神话武器卷轴
                { id: 2040817, qty: 1 },  // 不朽手套卷轴
                { id: 4000030, qty: 30 },  // 龙鳞
                { id: 4003005, qty: 30 },  // 奥利哈钢
                { id: 4003000, qty: 30 },  // 秘银矿石
                { id: 4011007, qty: 1 },   // 贤者之石
                { id: 4021009, qty: 1 },   // 混沌水晶
                { id: 4011008, qty: 3 },   // 永恒之火
                { id: 3010098, qty: 1 }    // 至尊VIP勋章
            ],
            minPoints: 60  // 需要至少46点
        }
    ]
};

/* 玩家当前提交的橡皮擦数量数组 */
var tickets = new Array(rewardConfig.maxTickets).fill(0);
/* 玩家当前提交的硬币数量 */
var coins = 0;
/* 临时存储当前操作信息 */
var currentInteraction = {
    itemQty: 0,    // 当前物品可用数量
    selection: -1,  // 当前选择的物品索引
    advance: true   // 对话流程控制标志
};

/**
 * 启动NPC交互
 */
function start() {
    status = -1;  // 初始化对话状态
    action(1, 0, 0);  // 开始对话流程
}

/**
 * NPC对话处理器
 * @param {number} mode 操作模式（1=前进，-1=后退）
 * @param {number} type 事件类型
 * @param {number} selection 玩家选择
 */
function action(mode, type, selection) {
    console.log(`action(${mode}, ${type}, ${selection})`);
    // 取消操作时结束对话
    if (mode === -1 || (mode === 0 && type > 0)) {
        cm.dispose();
        return;
    }

    if (mode === 1 && type === 0 && selection === -1) status = 0;   //回到选择界面
    // 处理对话流程状态
    if (mode === 1 && currentInteraction.advance) {
        status++;  // 前进到下一状态
    } else {
        status--;  // 返回上一状态
    }
    currentInteraction.advance = true;  // 重置流程控制
    try {
        handleDialogStates(selection);  // 处理各状态逻辑
    } catch (e) {
        throw (e);
        cm.sendOk(`发生系统错误，请联系管理员。\r\n错误代码：${e.id}\r\n错误内容：${e.message}`);
        cm.dispose();
    }
}

/**
 * 处理不同对话状态
 */
function handleDialogStates(selection) {
    switch (status) {
        case 0:  // 显示基础介绍
            showStartInterface();
            break;
        case 1:  // 初始状态：显示主界面
            showMainInterface();
            break;
        case 2:  // 选择操作处理
            processPlayerSelection(selection);
            break;
        case 3:  // 处理数量输入
            handleQuantityInput(selection);
            break;
        default:  // 结束对话
            cm.dispose();
    }
}

function showStartInterface() {
    let message = "你好，这是互联网咖啡厅的自动贩卖机。\r\n" +
        "放置你在任务中获得的#r橡皮擦#k或#r#t" + rewardConfig.coinItemId + "##k来兑换奖品。\r\n" +
        "你可以放置#b任意数量的橡皮擦#k，但请注意放置#r不同的橡皮擦#k和#r更多数量的任何一种#k会提高奖励的可能性！";
    cm.sendNext(message);
}
/**
 * 显示主操作界面
 */
function showMainInterface() {
    const hasCoin = cm.haveItem(rewardConfig.coinItemId);  // 检查硬币持有状态
    currentTier = calculateRewardTier();  // 计算当前层级

    // 构建界面文本
    let message = currentTier >= 0 ?
        `当前可领取： #r${rewardConfig.tiers[currentTier].name}#k 奖励\r\n已提交物品：` :
        "您尚未提交任何橡皮擦！\r\n请选择要提交的物品：\r\n";

    // 构建物品列表
    message += "\r\n" + buildItemList(hasCoin);
    message += `\r\n#r#L${getRewardOptionIndex(hasCoin)}#领取${currentTier >= 0  ? `【 #b${rewardConfig.tiers[currentTier].name}#r 】` : ''}奖励！#k#l`;

    cm.sendSimple(message);  // 发送界面给玩家
}

/**
 * 处理玩家选择
 * @param {number} selection 玩家选择的选项索引
 */
function processPlayerSelection(selection) {
    const rewardIndex = getRewardOptionIndex(cm.haveItem(rewardConfig.coinItemId));

    if (selection === rewardIndex) {  // 选择领取奖励
        handleRewardClaim();
    } else {  // 选择提交物品
        setupItemSubmission(selection);
    }
}

/**
 * 处理奖励领取
 */
function handleRewardClaim() {
    if (currentTier < 0) {  // 无有效提交
        cm.sendPrev("请先提交至少一个橡皮擦！");
        currentInteraction.advance = false;
        return;
    }

    if (distributeReward()) {  // 成功发放奖励
        cm.dispose();
    } else {  // 背包空间不足
        cm.sendOk("请清理背包空间后重试。");
        currentInteraction.advance = false;
    }
}

/**
 * 设置物品提交流程
 * @param {number} selection 选择的物品索引
 */
function setupItemSubmission(selection) {
    const itemId = selection < rewardConfig.maxTickets ?
        rewardConfig.eraserBaseId + selection :  // 橡皮擦类物品
        rewardConfig.coinItemId;  // 硬币

    currentInteraction.itemQty = cm.getItemQuantity(itemId);
    currentInteraction.selection = selection;

    if (currentInteraction.itemQty > 0) {
        cm.sendGetNumber(`请输入要提交的\r\n${getItemShowInfo(itemId)} 数量 (当前持有：#r${currentInteraction.itemQty}#k)：`,1,1,currentInteraction.itemQty);
    } else {
        cm.sendPrev(`您没有可提交的\r\n${getItemShowInfo(itemId)}！`);
        currentInteraction.advance = false;
    }
}

/**
 * 处理玩家输入的数量
 * @param {string} input 玩家输入的文本
 */
function handleQuantityInput(input) {
    try {
        const quantity = parseInputQuantity(input);
        if (quantity > 0) {
            updatePlayerSubmission(quantity);  // 更新提交数据
            cm.sendPrev("提交成功！点击返回查看最新状态。");
        } else {
            currentInteraction.advance = false;
        }
    } catch (error) {
        handleInputError(error);  // 处理输入错误
    }
    status = 2;  // 返回主界面
}

/**
 * 解析玩家输入的数量
 * @param {string} input
 * @returns {number}
 */
function parseInputQuantity(input) {
    const quantity = parseInt(input);
    if (isNaN(quantity)) {
        cm.sendOk("请输入有效数字！");
    } else if (quantity <= 0) {
        cm.sendOk("输入的数量不能 ≤ 0！");
    } else if (quantity > 0 && quantity > currentInteraction.itemQty){
        cm.sendOk(`输入的数量#b ${quantity} #k超出持有量（当前：#r${currentInteraction.itemQty}#k）！`);
    } else {
        return quantity;
    }
    return 0;
}

/**
 * 更新玩家提交数据
 * @param {number} quantity
 */
function updatePlayerSubmission(quantity) {
    if (currentInteraction.selection < rewardConfig.maxTickets) {
        tickets[currentInteraction.selection] = quantity;
    } else {
        coins = quantity;
    }
}

/**
 * 处理输入错误
 * @param {string} error
 */
function handleInputError(error) {
    cm.sendPrev(`输入错误：${error}`);
    currentInteraction.advance = false;
}

/**
 * 构建物品列表文本
 * @param {boolean} hasCoin 是否显示硬币选项
 * @returns {string}
 */
function buildItemList(hasCoin) {
    let list = "";

    // 橡皮擦列表
    for (let i = 0; i < rewardConfig.maxTickets; i++) {
        id = rewardConfig.eraserBaseId + i;
        currentInteraction.itemQty = cm.getItemQuantity(id);
        list += `#L${i}# ${getItemShowInfo(id)} ${tickets[i] ? `  (#r已提交：${tickets[i]} 个#k)` : ''}${currentInteraction.itemQty > 0 ? `\r\n(持有：#b${currentInteraction.itemQty}#k)#l\r\n` : '#l\r\n'}\r\n`;
    }

    // 硬币选项
    if (hasCoin) {
        list += `#L${rewardConfig.maxTickets}# ${getItemShowInfo(rewardConfig.coinItemId)}#l ${coins ? `已提交：${coins}个` : ''}\r\n`;
    }

    return list;
}

/**
 * 计算当前奖励层级
 * @returns {number} 层级索引（-1表示无效）
 */
function calculateRewardTier() {
    const points = calculateTotalPoints();
    if (points <= 0) return -1;

    // 从最高层开始检查
    for (let i = rewardConfig.tiers.length - 1; i >= 0; i--) {
        const tier = rewardConfig.tiers[i];

        if (i === 0 && points > 0) return 0;
        if (i === rewardConfig.tiers.length - 1 && points >= tier.minPoints) return i;
        if (points >= tier.basePoints && points < (tier.basePoints + tier.pointsPerStep)) {
            return i;
        }
    }
    return -1;
}

/**
 * 计算总点数
 * @returns {number}
 */
function calculateTotalPoints() {
    let points = 0;

    // 计算橡皮擦点数
    tickets.forEach((qty, index) => {
        if (qty > 0) {
            points += 6 + (qty - 1) * getEraserMultiplier(index);
        }
    });

    // 计算硬币点数（每个硬币0.46点）
    points += Math.ceil(coins * 0.46);

    return points;
}

/**
 * 获取橡皮擦的倍率
 * @param {number} index
 * @returns {number}
 */
function getEraserMultiplier(index) {
    // 根据物品类型决定倍率
    return (index === 1 || index === 3) ? 3 : 1;
}

/**
 * 发放奖励
 * @returns {boolean} 是否成功
 */
function distributeReward() {
    const targetTier = rewardConfig.tiers[currentTier];
    const rewards = targetTier.rewards;
    const selected = rewards[Math.floor(Math.random() * rewards.length)];

    // 检查背包空间
    if (!cm.canHold(selected.id, selected.qty)) return false;

    try {
        // 扣除提交物品
        tickets.forEach((qty, index) => {
            if (qty > 0) {
                cm.gainItem(rewardConfig.eraserBaseId + index, -qty);
            }
        });
        if (coins > 0) cm.gainItem(rewardConfig.coinItemId, -coins);

        // 发放奖励
        cm.gainItem(selected.id, selected.qty);
        resetSubmissionData();  // 重置提交数据
        return true;
    } catch (e) {
        throw (e);
        cm.sendOk(`#r奖励发放失败#k\r\n\r\n发生系统错误，请联系管理员。\r\n错误代码：${e.id}\r\n错误内容：${e.message}`);
        return false;
    }
}

/**
 * 重置玩家提交数据
 */
function resetSubmissionData() {
    tickets.fill(0);
    coins = 0;
}

/**
 * 获取奖励选项的索引
 * @param {boolean} hasCoin
 * @returns {number}
 */
function getRewardOptionIndex(hasCoin) {
    return hasCoin ? rewardConfig.maxTickets + 1 : rewardConfig.maxTickets;
}

function getItemShowInfo(id) {
    return `#i${id}#\r\n#b#t${id}##k`;
}