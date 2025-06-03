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
    private boolean enable = true;          //宠吸开关
    private double MAX_RADIUS = 15000;
    private double radius = Double.POSITIVE_INFINITY;   //默认拾取半径为无穷大
    private long pickupTime = currentServerTime();      //上一次拾取时间
    private boolean pickuping = false;                  //拾取中，如果为true，即使无冷却也会直接放弃操作
    private int MIN_INTERVAL = 500;                     //最低拾取间隔 ms
    private int MAX_INTERVAL = 5 * 1000;                //最高拾取间隔 ms
    private boolean AUTO_CALC = true;                   //自动计算拾取范围和拾取间隔
    private int sleep = MAX_INTERVAL;                   //拾取间隔 ms
    private int MAX_LEVEL = 20;                         //最高计算宠物等级

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
//        // 读取配置参数（集中管理）
//        enable = GameConfig.getServerBoolean("cheat_pet_itemvac_switch");
//
//        // ==== 控制逻辑 ====
//        if (!enable) {
//            resetValues();
//            return false;
//        }
//
//        MAX_RADIUS = GameConfig.getServerDouble("cheat_pet_itemvac_radius_max");
//        MIN_INTERVAL = Math.max(GameConfig.getServerInt("cheat_pet_itemvac_sleep_min"), 200);
//        MAX_INTERVAL = GameConfig.getServerInt("cheat_pet_itemvac_sleep_max");
//        AUTO_CALC = GameConfig.getServerBoolean("cheat_pet_itemvac_radius_auto");

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

    public boolean updatePetVacParam_bak2() {
        enable = GameConfig.getServerBoolean("cheat_pet_itemvac_switch");
        // ===== 控制开关检查 =====
        if (!enable || player == null || !player.isLoggedInWorld()) {
            this.radius = 0;
            this.sleep = 0;
            return false;
        }
        MAX_RADIUS = GameConfig.getServerDouble("cheat_pet_itemvac_radius_max");
        MIN_INTERVAL = Math.max(GameConfig.getServerInt("cheat_pet_itemvac_sleep_min"), 200);
        MAX_INTERVAL = GameConfig.getServerInt("cheat_pet_itemvac_sleep_max");
        AUTO_CALC = GameConfig.getServerBoolean("cheat_pet_itemvac_radius_auto");

        // 三元运算符（简洁高效，全版本兼容）
        MAX_RADIUS *= (MAX_RADIUS <= 1000) ? 100 : (MAX_RADIUS <= 10000) ? 10 : 1;

        // ===== 自动计算开关处理 =====
        if (!AUTO_CALC) {
            this.radius = MAX_RADIUS;
            this.sleep = MAX_INTERVAL;
            return true;
        }

        Pet pet = player.getPet(0);
        if (pet == null) {
            this.radius = 0;
            this.sleep = 0;
            return false;
        }

        // ===== 公式计算部分 =====
        // 1. 计算饱食度系数（0.5-1.5线性变化）
        final double fullnessFactor = 0.5 + pet.getFullness() / 100.0;

        // 2. 半径计算公式（等级影响+饱食度修正）
        final double levelProgress = Math.min((double)pet.getLevel() / MAX_LEVEL, 1.0);
        this.radius = Math.min(
                MAX_RADIUS *
                        Math.sqrt(levelProgress) * // 平方根曲线平滑增长
                        fullnessFactor,
                MAX_RADIUS
        );

        // 3. 间隔计算公式（亲密度影响+饱食度修正）
        final double tamenessRatio = pet.getTameness() / 100.0;
        this.sleep = (int) Math.max(
                MIN_INTERVAL,
                Math.min(
                        MAX_INTERVAL -
                                (MAX_INTERVAL - MIN_INTERVAL) *
                                        Math.pow(tamenessRatio, 0.7) * // 幂函数曲线
                                        (1.5 - fullnessFactor), // 反向影响
                        MAX_INTERVAL
                )
        );

        return true;
    }
    /**
     * 重新计算并更新主宠的拾取范围和拾取间隔
     */
    public void updatePetVacParam_bak() {
        this.enable = GameConfig.getServerBoolean("cheat_pet_itemvac_switch");
        if (!this.enable || player == null || !player.isLoggedInWorld()) {
            this.radius = 0;
            this.sleep = 0;
            return;
        }

        this.MAX_RADIUS = GameConfig.getServerDouble("cheat_pet_itemvac_radius_max");
        this.MIN_INTERVAL = Math.max(GameConfig.getServerInt("cheat_pet_itemvac_sleep_min"), 200);
        this.MAX_INTERVAL = GameConfig.getServerInt("cheat_pet_itemvac_sleep_max");


        Pet pet = player.getPet(0);
        if (pet != null) {  //存在主宠
            double per = pet.getFullness() / 100.0;
            this.radius = GameConfig.getServerBoolean("cheat_pet_itemvac_radius_auto") ? pet.getLevel() * pet.getTameness() * per : this.MAX_RADIUS;
            this.sleep = (int) (MAX_INTERVAL - pet.getTameness() / pet.getLevel() * 20 * per);
            if (this.radius > MAX_RADIUS) radius = MAX_RADIUS;
            if (this.sleep < MIN_INTERVAL) this.sleep = MIN_INTERVAL;
        }
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

        pickuping = true;

        List<MapObject> items = player.getMap().getMapObjectsInRange(
                Pos,    //基点坐标
                radius, //拾取半径
                Collections.singletonList(MapObjectType.ITEM) // 优化为单例列表
        );

        // 预计算玩家ID和过滤列表(减少循环内重复调用)
        int playerId = player.getId();
        Set<Integer> excludedItems = player.getExcludedItems();         //获取过滤列表
        boolean hasExclusions = !excludedItems.isEmpty();               //过滤列表不为空
        boolean ignoreItems = player.isEquippedPetItemIgnore();         //检查是否启用道具过滤
        boolean isEquippedMesoMagnet = player.isEquippedMesoMagnet();   //装备了宠物磁铁
        boolean isEquippedItemPouch = player.isEquippedItemPouch();     //装备了宠物捡取袋

        // 遍历所有可拾取物品
        for (MapObject item : items) {
            try {
                if (!player.isLoggedInWorld()) return;  //不在线直接返回
                MapItem mapItem = (MapItem) item;
                boolean shouldPickup = true;

                // 检查是否为玩家自己丢弃的物品(不拾取)
                if (mapItem.isPlayerDrop()) { //玩家丢弃的一概不拾取 // && mapItem.getOwnerId() == playerId) {
                    shouldPickup = false;
                } else if (petIndex >= 0 && player.getPet(petIndex) != null) {//如果是宠物并且已召唤
                    //判断是否装备了特定宠物装备和是否在过滤列表里
                    if (mapItem.getMeso() > 0) {
                        shouldPickup = isEquippedMesoMagnet && (!ignoreItems || !hasExclusions || !excludedItems.contains(Integer.MAX_VALUE));
                    } else {
                        shouldPickup = isEquippedItemPouch && (!ignoreItems || !hasExclusions || !excludedItems.contains(mapItem.getItemId()));
                    }
                }
                if (shouldPickup && player.isLoggedInWorld()) { //再此判定角色是否在线
                    player.pickupItem(item, petIndex,false);  //执行拾取
                }
            } catch (NullPointerException | ClassCastException ignored) {}
        }
        pickuping = false;
        pickupTime = currentServerTime();
    }

}
