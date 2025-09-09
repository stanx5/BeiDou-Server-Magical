/**
 * 藏经阁七层 少林妖僧
 * @type {boolean}
 */
// 事件实例化变量
var isPq = true; // 是否为PQ（Party Quest）类型事件。
var minPlayers = 3, maxPlayers = 6; // 该事件实例允许的队伍成员数量范围。
var minLevel = 120, maxLevel = 255;     // 合格队伍成员的等级范围。
var entryMap = 702060000;               // 事件启动时玩家进入的初始地图。
var exitMap = 702070400;                // 玩家未能完成事件时被传送至此地图。
var recruitMap = exitMap;             // 玩家必须在此地图上才能开始此事件。
var clearMap = entryMap;               // 玩家成功完成事件后被传送至此地图。

var minMapId = entryMap;               // 事件发生在此地图ID区间内。若玩家超出此范围则立即从事件中移除。
var maxMapId = entryMap;

var eventTime = 30;              // 事件的最大允许时间，以分钟计。

const maxLobbies = 1;       // 并发活跃大厅的最大数量。

var BossID = 9600025;                   // 待生成的BOSS
var endBossID = 9600025;                // 检测到该BOSS死亡则结束事件，不设置时使用生成的BossID
var PosX = 351 , PosY = 580;           // 生成的坐标
var Difficulty = true;                  // 根据传入的难度值以倍数方式提高怪物的各项指标，在BossID有效的情况下。

// 掉落配置JSON
var lootConfig = {"loot": [
        {"id":4021004,"name":"蛋白石","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4021003,"name":"祖母绿","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4021002,"name":"海蓝宝石","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4021001,"name":"紫水晶","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4021000,"name":"石榴石","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4021008,"name":"黑水晶","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4021007,"name":"钻石","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4021006,"name":"黄晶","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4021005,"name":"蓝宝石","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":2040705,"name":"鞋子跳跃卷轴10%","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":2040708,"name":"鞋子速度卷轴10%","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":4310015,"name":"斗神证物","qtyMin":1,"qtyMax":1,"chance":2.5},
        {"id":4001002,"name":"小说书","qtyMin":1,"qtyMax":1,"chance":2.5},
        {"id":2040502,"name":"全身铠甲敏捷卷轴10%","qtyMin":1,"qtyMax":1,"chance":10},
        {"id":2022468,"name":"心跳箱子","qtyMin":1,"qtyMax":1,"chance":20},
        {"id":2022468,"name":"心跳箱子","qtyMin":1,"qtyMax":1,"chance":20},
        {"id":2022468,"name":"心跳箱子","qtyMin":1,"qtyMax":1,"chance":20},
        {"id":2022468,"name":"心跳箱子","qtyMin":1,"qtyMax":1,"chance":20},
        {"id":2022468,"name":"心跳箱子","qtyMin":1,"qtyMax":1,"chance":20}
    ]
};

const GameConfig = Java.type('org.gms.config.GameConfig');
minPlayers = GameConfig.getServerBoolean("use_enable_solo_expeditions") ? 1 : minPlayers;  //如果解除远征队人数限制，则最低人数改为1人
if(GameConfig.getServerBoolean("use_enable_party_level_limit_lift")) {  //如果解除远征队等级限制，则最低1级，最高999级。
    minLevel = 1 , maxLevel = 999;
}
/**
 * 初始化事件，设置事件要求。
 */
function init() {
    setEventRequirements();
}

/**
 * 获取最大并发活跃大厅的数量。
 * @returns {number} 最大活跃大厅数量。
 */
function getMaxLobbies() {
    return maxLobbies;
}

/**
 * 设置并显示事件的要求信息。
 */
function setEventRequirements() {
    var reqStr = "";

    reqStr += "\r\n   组队人数: ";
    if (maxPlayers - minPlayers >= 1) {
        reqStr += minPlayers + " ~ " + maxPlayers;
    } else {
        reqStr += minPlayers;
    }

    reqStr += "\r\n   等级要求: ";
    if (maxLevel - minLevel >= 1) {
        reqStr += minLevel + " ~ " + maxLevel;
    } else {
        reqStr += minLevel;
    }

    reqStr += "\r\n   时间限制: ";
    reqStr += eventTime + " 分钟";

    em.setProperty("party", reqStr);
}

/**
 * 设置仅在事件实例中存在的物品，并在事件结束时从库存中移除这些物品。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function setEventExclusives(eim) {
    var itemSet = [];
    if (itemSet.length > 0)  eim.setExclusiveItems(itemSet);
}

/**
 * 设置所有可能的奖励，随机给予玩家作为事件结束时的奖品。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function setEventRewards(eim) {
    var itemSet, itemQty, evLevel, expStages;

    evLevel = 1;    // 清晰PQ奖励
    itemSet = []; // 奖励物品列表，这里填入物品ID
    itemQty = []; // 每个物品的数量，这里填入每个ID对应的数量
    if (itemSet.length > 0 && itemQty.length > 0)  eim.setEventRewards(evLevel, itemSet, itemQty);

    expStages = [];    // 完成阶段时给予的经验值奖励
    if (expStages.length > 0)  eim.setEventClearStageExp(expStages);
}

/**
 * 从给定的队伍中选择符合资格的团队尝试此事件。
 * @param {Party} party - 队伍对象。
 * @returns {PartyCharacter[]} 符合条件的队伍成员数组。
 */
function getEligibleParty(party) {
    var eligible = [];
    var hasLeader = false;

    if (party.size() > 0) {
        var partyList = party.toArray();

        for (var i = 0; i < party.size(); i++) {
            var ch = partyList[i];

            if (ch.getMapId() === recruitMap && ch.getLevel() >= minLevel && ch.getLevel() <= maxLevel) {
                if (ch.isLeader()) {
                    hasLeader = true;
                }
                eligible.push(ch);
            }
        }
    }

    if (!(hasLeader && eligible.length >= minPlayers && eligible.length <= maxPlayers)) {
        eligible = [];
    }
    return Java.to(eligible, Java.type('org.gms.net.server.world.PartyCharacter[]'));
}

/**
 * 设置事件实例。
 * @param {number} level - 事件级别。
 * @param {number} lobbyid - 大厅ID。
 * @returns {EventInstanceManager} 事件实例管理器。
 */
function setup(level, lobbyid) {
    let eim = em.newInstance(em.getName() + lobbyid);
    eim.setProperty("level", level);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);

    /**
     * 以下为自定义召唤指定Boss，如BossID为null或者不存在则使用默认的地图生怪。
     * @type {any}
     */
    if(BossID != null && BossID > 0) {
        const LifeFactory = Java.type('org.gms.server.life.LifeFactory');
        let mob = LifeFactory.getMonster(BossID);
        if(mob != null) {
            let map = eim.getMapInstance(entryMap);
            map.killAllMonsters();      // 杀死原本地图上的所有怪物
            map.clearDrops();           // 清理掉落的物品
            if(Difficulty) {
                    level = level <= 0 ? 1 : level; //最小难度为1
                let stats = mob.getStats();
                let HpMax = mob.getMaxHp() * level;
                    HpMax = HpMax > 2147483647 ? 2147483647 : HpMax;
                let MpMax = mob.getMaxMp() * level;
                    MpMax = MpMax > 2147483647 ? 2147483647 : MpMax;
                mob.setStartingHp(HpMax);
                mob.setMp(MpMax);
                //以下属性似乎没有作用，先留着
                stats.setPADamage(stats.getPADamage() * level);
                stats.setPDDamage(stats.getPDDamage() * level);
                stats.setMADamage(stats.getMADamage() * level);
                stats.setMDDamage(stats.getMDDamage() * level);
                mob.setStats(stats);
            }
            map.spawnMonsterOnGroundBelow(mob, new java.awt.Point(PosX, PosY));     //生成指定怪物在指定坐标
        }
    }

    return eim;
}

/**
 * 事件实例初始化完毕且所有玩家分配完成后，但在玩家进入之前触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function afterSetup(eim) {}

/**
 * 定义事件内部允许重生的地图。此函数应在末尾创建一个新的任务，在指定的重生率后再次调用自身。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function respawnStages(eim) {
    // eim.getMapInstance(entryMap).instanceMapRespawn();
    // eim.getMapInstance(MapID).instanceMapRespawn();
    // eim.schedule("respawnStages", 15 * 1000);    //多少毫秒调用一次自身
}

/**
 * 将玩家传送到事件地图等操作。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function playerEntry(eim, player) {
    var map = eim.getMapInstance(entryMap);
    player.changeMap(map, map.getPortal(0));
}

/**
 * 当事件超时而未完成时触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function scheduledTimeout(eim) {
    end(eim);
}

/**
 * 在玩家即将注销前对其进行某些操作。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function playerUnregistered(eim, player) {}

/**
 * 在解散事件实例前对玩家进行某些操作。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, 0);
}

/**
 * 在玩家离开队伍前对其进行某些操作。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function playerLeft(eim, player) {
    if (!eim.isEventCleared()) {
        playerExit(eim, player);
    }
}

/**
 * 当玩家更换地图时根据mapid执行的操作。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 * @param {number} mapid - 新的地图ID。
 */
function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            end(eim);
        } else {
            eim.unregisterPlayer(player);
        }
    }
}

/**
 * 如果队伍领袖变更时执行的操作。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} leader - 新的队伍领袖。
 */
function changedLeader(eim, leader) {
    var mapid = leader.getMapId();
    if (!eim.isEventCleared() && (mapid < minMapId || mapid > maxMapId)) {
        end(eim);
    }
}

/**
 * 当玩家死亡时触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function playerDead(eim, player) {}

/**
 * 当玩家复活时触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function playerRevive(eim, player) {
    if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

/**
 * 当玩家断开连接时触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function playerDisconnected(eim, player) {
    if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

/**
 * 当玩家离开队伍时触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function leftParty(eim, player) {
    if (eim.isEventTeamLackingNow(false, minPlayers, player)) {
        end(eim);
    } else {
        playerLeft(eim, player);
    }
}

/**
 * 当队伍解散时触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function disbandParty(eim) {
    if (!eim.isEventCleared()) {
        end(eim);
    }
}

/**
 * 计算怪物价值。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {number} mobId - 怪物ID。
 * @returns {number} 怪物的价值。
 */
function monsterValue(eim, mobId) {
    return 1;
}

/**
 * 结束事件。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        playerExit(eim, party.get(i));
    }
    eim.dispose();
}

/**
 * 随机给予玩家一个奖励。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 * @param {Character} player - 玩家角色。
 */
function giveRandomEventReward(eim, player) {
    eim.giveEventReward(player);
}

/**
 * 当队伍成功完成事件实例时触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function clearPQ(eim) {
    // eim.showClearEffect();   //展示通过的图片
    eim.stopEventTimer();
    eim.setEventCleared();
    eim.startEventTimer(2 * 60000);  //2分钟后强制清场
}

/**
 * 当敌对怪物死亡时触发。
 * @param {Monster} mob - 死亡的怪物。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function monsterKilled(mob, eim) {
    try {
        if (eim.isEventCleared()) {
            return;
        }
        let id = mob.getId();
        if((endBossID && id === endBossID) || (!endBossID && id === BossID)) {
            var mapObj = mob.getMap();
            var dropper = eim.getPlayers().get(0);

            // 创建两个列表：一个用于物品ID，一个用于数量
            var itemIds = [];
            var quantities = [];

            // 处理每个掉落项
            lootConfig.loot.forEach(item => {
                // 计算概率（chance * 10000，然后与1000000比较）
                const chanceValue = item.chance * 10000;
                if (Math.random() * 1000000 < chanceValue) {
                    // 计算掉落数量（qtyMin~qtyMax之间的随机数）
                    const dropCount = Math.floor(Math.random() * (item.qtyMax - item.qtyMin + 1)) + item.qtyMin;

                    // 将物品ID和数量添加到对应的列表中（保持重复ID）
                    itemIds.push(item.id);
                    quantities.push(dropCount);
                }
            });
            mapObj.spawnItemDropList(itemIds, quantities, mob, dropper, mob.getPosition(), true, false);
            clearPQ(eim);
        }
    } catch (err) {
        console.error(err);
    } // PQ not started yet
}

/**
 * 当所有怪物死亡时触发。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function allMonstersDead(eim) {
    // clearPQ(eim);
}

/**
 * 结束正在进行的任务调度。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function cancelSchedule(eim) {}

/**
 * 结束事件实例。
 * @param {EventInstanceManager} eim - 事件实例管理器。
 */
function dispose(eim) {}