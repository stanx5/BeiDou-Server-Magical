package org.gms.cheatsystem;

import lombok.Getter;
import lombok.Setter;
import org.gms.client.inventory.Pet;
import org.gms.client.Character;
import org.gms.config.GameConfig;
import org.gms.server.maps.MapItem;
import org.gms.server.maps.MapObject;
import org.gms.server.maps.MapObjectType;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class itemVac extends basic {
    private Character player;
    /** 宠吸功能总开关 */
    private boolean enable = true;
    /** 是否允许在事件地图中使用宠吸功能，false=当检测到事件地图出现BOSS则关闭宠吸 */
    private boolean ALLOW_IN_EVENT = false;
    /** 是否在界面上展示参数提示信息 */
    private boolean SHOW_PARAMS = true;
    /** 最大拾取半径限制 */
    private double MAX_RADIUS = 15000;
    /** 当前拾取半径，默认无穷大表示无限制 */
    private double radius = Double.POSITIVE_INFINITY;
    /** 上一次执行拾取操作的时间戳 */
    private long pickupTime = currentServerTime();
    /** 拾取状态标志，true表示正在拾取中，避免重复操作 */
    private boolean pickuping = false;
    /** 最小拾取间隔时间（毫秒） */
    private int MIN_INTERVAL = 200;
    /** 最大拾取间隔时间（毫秒） */
    private int MAX_INTERVAL = 5 * 1000;
    /** 是否自动计算拾取范围和间隔 */
    private boolean AUTO_CALC = true;
    /** 当前使用的拾取间隔时间（毫秒） */
    private int sleep = MAX_INTERVAL;
    /** 宠吸功能支持的最高宠物等级 */
    private int MAX_LEVEL = 20;
    /** 击杀BOSS时间 */
    private long killBossTime = -1;

    public itemVac(Character player) {
        this.player = player;
    }

    public itemVac(Character player, double radius, int sleep) {
        this.player = player;
        this.radius = radius;
        this.sleep = sleep;
    }

    /**
     * 设置拾取半径和拾取间隔
     * @param radius 拾取半径
     * @param sleep 拾取间隔
     */
    public void setParam(double radius,int sleep) {
        this.radius = radius;
        this.sleep = sleep;
    }
    public boolean updatePetVacParam() {
        // 读取配置参数（集中管理）
        enable = GameConfig.getServerBoolean("cheat_pet_itemvac_switch");
        ALLOW_IN_EVENT = GameConfig.getServerBoolean("cheat_pet_itemvac_allow_in_event");
        SHOW_PARAMS = GameConfig.getServerBoolean("cheat_pet_itemvac_show_params");

        // ==== 控制逻辑 ====
        if (!enable) {
            resetValues();
            return false;
        }

        MAX_LEVEL = GameConfig.getServerInt("cheat_pet_itemvac_max_level");
        MAX_RADIUS = GameConfig.getServerDouble("cheat_pet_itemvac_radius_max");
        MIN_INTERVAL = Math.max(GameConfig.getServerInt("cheat_pet_itemvac_sleep_min"), 200);
        MAX_INTERVAL = GameConfig.getServerInt("cheat_pet_itemvac_sleep_max");
        AUTO_CALC = GameConfig.getServerBoolean("cheat_pet_itemvac_radius_auto");

        // ==== 控制逻辑 ====
        if (!enable || player == null || !player.isLoggedInWorld()) {
            resetValues();
            return false;
        }

        // ==== 自动计算开关处理 ====
        if (!AUTO_CALC) {
            setMaxValues();
            return true;
        }

        // ==== 宠物状态检查 ====
        Pet pet = getValidPet();
        if (pet == null) {
            resetValues();
            return false;
        }

        // ==== 核心公式计算 ====
        calculateParams(pet);
        return true;
    }

    private void calculateParams(Pet pet) {
        // 参数预处理（保持原有缩放逻辑）
        MAX_RADIUS *= (MAX_RADIUS <= 1000) ? 100 : (MAX_RADIUS <= 10000) ? 10 : 1;

        final int petLevel = Math.min(pet.getLevel(), MAX_LEVEL);
        final double levelProgress = petLevel / (double) MAX_LEVEL;
        final double fullness = pet.getFullness() / 100.0;  //饱食度百分比
        final int tameness = pet.getTameness(); //亲密度

        // ==== 修正关键系数 ====
        // 半径系数：0.6 → 1.4（饱食度越高越大）
        final double radiusFactor = fullness;
        // 间隔系数：0.6 → 1.4（饱食度越高越大）
        final double intervalFactor = fullness; // 原1.4-0.8改为0.6+0.8

        // ==== 半径计算（保持不变） ====
        double baseRadius = MAX_RADIUS * levelProgress;
        this.radius = Math.min(baseRadius, MAX_RADIUS) * radiusFactor;

        // ==== 间隔计算（修正逻辑） ====
        // 饱食度越高 → intervalFactor越大 → 减少量越多 → 最终间隔越小
        double intervalReduction = (int) Math.max(MAX_INTERVAL - (tameness * intervalFactor),MIN_INTERVAL); // 使用修正后的系数
        this.sleep = (int) Math.max(MIN_INTERVAL, intervalReduction);
    }

    // ==== 辅助方法 ====
    private Pet getValidPet() {
        Pet pet = player.getPet(0);
        return (pet != null && pet.getLevel() > 0) ? pet : null;
    }

    private void resetValues() {
        this.radius = 0;
        this.sleep = 0;
    }

    private void setMaxValues() {
        this.radius = MAX_RADIUS;
        this.sleep = MAX_INTERVAL;
    }

    /**
     * 人物范围吸物
     */
    public void pickupItem() {
        pickupItem((byte) -1);
    }

    public void pickupItem(byte petIndex,boolean update) {
        if (update) {
            updatePetVacParam();
        }
        pickupItem(petIndex);
    }
    /**
     * 范围吸物
     *
     * @param {byte} petIndex -1:玩家，0~3: 携带的宠物
     */
    public void pickupItem(byte petIndex) {
        // 检查角色是否为空，是否在线，是否拾取中，拾取范围是否小于0，拾取冷却时间(防止频繁调用)
        if (pickuping || radius <= 0 || currentServerTime() - pickupTime < sleep || player == null || !player.isLoggedInWorld()) return;
        Point Pos = null;
        if (petIndex >= 0) {
            Pet pet = player.getPet(petIndex);
            if (pet != null ) Pos = player.getPet(petIndex).getPos();   //指定索引的宠物存在则使用该宠物的坐标
        } else {
            Pos = player.getPosition();   //获取玩家坐标
            if (petIndex != -1) {
                petIndex = -1;
            }
        }
        pickupItem(Pos,radius,sleep,petIndex);
    }

    public void pickupItem(Point Pos, double radius, int sleep, byte petIndex) {
        // 检查角色是否为空，是否在线，是否拾取中，拾取范围是否小于0，拾取冷却时间(防止频繁调用)
        if (player == null || Pos == null || !player.isLoggedInWorld() || pickuping || radius <= 0 || currentServerTime() - pickupTime < sleep) {
            if (player != null && player.isLoggedInWorld()) player.enableActions();
            return;
        }
        int Stance = player.getStance();    //获取角色姿态，14~17 = 上下爬绳子、梯子；20 = 坐下
        if (Stance >= 14 && Stance <= 17 || Stance == 20) {//爬绳和坐下不拾取
            return;
        }
        // 检测条件并记录时间
        if (!ALLOW_IN_EVENT && player.getEventInstance() != null && player.getMap().countBosses() > 0 && player.getMap().getPlayers().size() > 1) {
            // 条件满足：不允许在事件中使用、角色在事件中、BOSS数量>0、地图人数>1，记录当前时间
            killBossTime = currentServerTime();
        } else if (player.getMap().getPlayers().size() == 1) {
            // 地图人数=1时，重置时间为-1
            killBossTime = -1;
        } else if (player.getMap().getPlayers().size() > 1 && player.getMap().countBosses() == 0) {
            // 地图人数>1且BOSS数量=0时
            if (killBossTime != -1) {
                if (currentServerTime() - killBossTime < 30000) { // 30秒 = 30000毫秒
                    // 时间差小于30秒，不进行捡取操作
                    return;
                } else {
                    // 时间差超过30秒，重置时间为-1
                    killBossTime = -1;
                }
            }
        } else if (killBossTime != -1) {
            killBossTime = -1;
        }

        pickuping = true;

        List<MapObject> items = player.getMap().getMapObjectsInRange(
                Pos,    //基点坐标
                radius, //拾取半径
                Collections.singletonList(MapObjectType.ITEM) // 优化为单例列表
        );

        // 预计算玩家ID和过滤列表(减少循环内重复调用)
        Set<Integer> excludedItems = player.getExcludedItems();         //获取过滤列表
        boolean hasExclusions = !excludedItems.isEmpty();               //过滤列表不为空
        boolean ignoreItems = player.isEquippedPetItemIgnore();         //检查是否启用道具过滤
        boolean isEquippedMesoMagnet = player.isEquippedMesoMagnet();   //装备了宠物磁铁
        boolean isEquippedItemPouch = player.isEquippedItemPouch();     //装备了宠物捡取袋
        boolean isEquippedPetItemScales = player.isEquippedPetItemScales();     //装备了魔法天平

        // 遍历所有可拾取物品
        for (MapObject item : items) {
            try {
                if (!player.isLoggedInWorld()) return;  //不在线直接返回
                MapItem mapItem = (MapItem) item;
                boolean shouldPickup = true;

                // 检查是否为玩家自己丢弃的物品(不拾取)
                if (mapItem.isPlayerDrop()) { //玩家丢弃的一概不拾取
                    shouldPickup = false;
                } else if (petIndex >= 0 && player.getPet(petIndex) != null) {//如果是宠物并且已召唤
                    //判断是否装备了特定宠物装备和是否在过滤列表里
                    if (mapItem.getMeso() > 0) {
                        shouldPickup = isEquippedMesoMagnet &&  (!ignoreItems || !hasExclusions || !excludedItems.contains(Integer.MAX_VALUE));
                    } else {
                        shouldPickup = isEquippedItemPouch && (!ignoreItems || !hasExclusions || !excludedItems.contains(mapItem.getItemId()));
                    }
                }
                if (shouldPickup && player.isLoggedInWorld()) { //再次判定角色是否在线
                    if ((mapItem.canBePickedBy(player) || isEquippedPetItemScales) && currentServerTime() - mapItem.getDropTime() > 1000) { //如果拥有拾取权 或者 装备了魔法天平 并且掉落时间超过1000ms，避免未落地先拾取
                        player.pickupItem(item, petIndex, false);  //执行拾取
                    }
                }
            } catch (NullPointerException | ClassCastException ignored) {}
        }
        pickuping = false;
        pickupTime = currentServerTime();
    }
}
