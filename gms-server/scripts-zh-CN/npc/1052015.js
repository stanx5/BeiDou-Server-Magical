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
 * @npc: Billy
 * @map: 193000000 - Premium Road - Kerning City Internet Cafe
 * @func: Cafe PQ Reward Announcer
 */

/* 全局状态变量，用于跟踪NPC对话状态 */
var status;

/**
 * 奖励层级配置
 * @typedef {Object} TierConfig 层级配置
 * @property {string} name 层级显示名称
 * @property {Array<{id: number, qty: number}>} rewards 奖励物品列表
 */

/**
 * 奖励系统核心配置
 * @type {TierConfig[]}
 */
const rewardTiers = [
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
];

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
    // 取消操作时结束对话
    if (mode === -1) {
        cm.dispose();
        return;
    }

    // 处理对话流程状态
    if (mode === 1) {
        status++;  // 前进到下一状态
    } else if (mode === 0 && status === 0) {
        cm.dispose();
        return;
    } else {
        status--;  // 返回上一状态
    }

    try {
        handleDialogStates(selection);  // 处理各状态逻辑
    } catch (e) {
        console.error("NPC交互错误: " + e);
        cm.sendOk("发生系统错误，请联系管理员。");
        cm.dispose();
    }
}

/**
 * 处理不同对话状态
 * @param {number} selection 玩家选择的选项索引
 */
function handleDialogStates(selection) {
    switch (status) {
        case 0:  // 初始状态：显示层级选择界面
            showTierSelection();
            break;
        case 1:  // 显示选定层级的奖励列表
            showTierRewards(selection);
            break;
        case 2:  // 结束对话
            cm.dispose();
            break;
    }
}

/**
 * 显示层级选择界面
 */
function showTierSelection() {
    // 构建层级选择菜单
    let menu = "这里是#b网咖派对任务#k，奖励玩家与票类似的#b怪物橡皮#k，可以用在自动售货机上兑换奖品。通过选择不同的#r层级#k，进一步获得更好的奖品。\r\n\r\n";
    menu += "请选择要查看的奖励层级:\r\n\r\n";

    // 添加每个层级选项
    rewardTiers.forEach((tier, index) => {
        menu += `#L${index}##b${tier.name}奖励#k#l\r\n\r\n`;
    });

    cm.sendSimple(menu);
}

/**
 * 显示指定层级的奖励列表
 * @param {number} tierIndex 层级索引
 */
function showTierRewards(tierIndex) {
    // 验证索引有效性
    if (tierIndex < 0 || tierIndex >= rewardTiers.length) {
        cm.sendOk("无效的层级选择。");
        cm.dispose();
        return;
    }

    const tier = rewardTiers[tierIndex];
    let itemList = `以下物品可在#b${tier.name}#k获得:\r\n\r\n`;

    // 遍历并添加该层级的所有奖励物品
    tier.rewards.forEach((item, index) => {
        itemList += `#L${index}# #i${item.id}#\r\n  #t${item.id}#`;
        if (item.qty > 1) {
            itemList += `#b × ${item.qty}#k`;
        }
        itemList += "#l\r\n\r\n";
    });

    cm.sendPrev(itemList);
}