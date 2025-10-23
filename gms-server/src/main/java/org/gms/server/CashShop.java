/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

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
package org.gms.server;

import lombok.Getter;
import net.jcip.annotations.GuardedBy;
import org.gms.client.inventory.Equip;
import org.gms.client.inventory.InventoryType;
import org.gms.client.inventory.Item;
import org.gms.client.inventory.ItemFactory;
import org.gms.config.GameConfig;
import org.gms.constants.id.ItemId;
import org.gms.constants.inventory.ItemConstants;
import org.gms.dao.entity.AccountsDO;
import org.gms.dao.entity.ModifiedCashItemDO;
import org.gms.dao.entity.WishlistsDO;
import org.gms.manager.ServerManager;
import org.gms.model.pojo.CashCategory;
import org.gms.net.server.Server;
import org.gms.provider.Data;
import org.gms.provider.DataProvider;
import org.gms.provider.DataProviderFactory;
import org.gms.provider.DataTool;
import org.gms.provider.wz.WZFiles;
import org.gms.service.AccountService;
import org.gms.service.CashShopService;
import org.gms.service.CharacterService;
import org.gms.util.DatabaseConnection;
import org.gms.util.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * @author Flav
 * @author Ponk
 */
public class CashShop {
    public static final int NX_CREDIT = 1;
    public static final int MAPLE_POINT = 2;
    public static final int NX_PREPAID = 4;

    private final int accountId;
    private final int characterId;
    private int nxCredit;
    private int maplePoint;
    private int nxPrepaid;
    private boolean opened;
    private ItemFactory factory;
    private final List<Item> inventory = new ArrayList<>();
    private final List<Integer> wishList = new ArrayList<>();
    private int notes = 0;
    private final Lock lock = new ReentrantLock();
    private static final AccountService accountService = ServerManager.getApplicationContext().getBean(AccountService.class);
    private static final CharacterService characterService = ServerManager.getApplicationContext().getBean(CharacterService.class);

    public CashShop(int accountId, int characterId, int jobType) {
        this.accountId = accountId;
        this.characterId = characterId;

        if (!GameConfig.getServerBoolean("use_joint_cash_shop_inventory")) {
            switch (jobType) {
                case 0:
                    factory = ItemFactory.CASH_EXPLORER;
                    break;
                case 1:
                    factory = ItemFactory.CASH_CYGNUS;
                    break;
                case 2:
                    factory = ItemFactory.CASH_ARAN;
                    break;
            }
        } else {
            factory = ItemFactory.CASH_OVERALL;
        }

        AccountsDO accountsDO = accountService.findById(accountId);
        this.nxCredit = Optional.ofNullable(accountsDO.getNxCredit()).orElse(0);
        this.maplePoint = Optional.ofNullable(accountsDO.getMaplePoint()).orElse(0);
        this.nxPrepaid = Optional.ofNullable(accountsDO.getNxPrepaid()).orElse(0);

        try {
            for (Pair<Item, InventoryType> item : factory.loadItems(accountId, false)) {
                inventory.add(item.getLeft());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        List<WishlistsDO> wishlistsDOList = characterService.getWishlistsByCharacter(characterId);
        wishlistsDOList.forEach(wishlistsDO -> wishList.add(wishlistsDO.getSn()));
    }

    public static class CashItemFactory {
        @Getter
        private static volatile Map<Integer, ModifiedCashItemDO> items = new HashMap<>();
        private static volatile Map<Integer, List<Integer>> packages = new HashMap<>();
        @Getter
        private static final List<CashCategory> cashCategories = new ArrayList<>();
        @Getter
        private static final Map<Integer, ModifiedCashItemDO> modifiedCashItems = new HashMap<>();  //控制台商城管理存放缓存的商品
        @Getter
        private static final Map<Integer, ModifiedCashItemDO> discontinuedCashItems = new HashMap<>();  //已下架的商品
        @Getter
        private static final Map<Integer, ModifiedCashItemDO> permanentCashItems = new HashMap<>(); //永久的商品

        public static void loadAllCashItems() {
            DataProvider etc = DataProviderFactory.getDataProvider(WZFiles.ETC);

            Map<Integer, ModifiedCashItemDO> loadedItems = new HashMap<>();
            for (Data item : etc.getData("Commodity.img").getChildren()) {
                int sn = DataTool.getIntConvert("SN", item);
                int itemId = DataTool.getIntConvert("ItemId", item);
                int price = DataTool.getIntConvert("Price", item, 0);
                long period = DataTool.getIntConvert("Period", item, 1);
                short count = (short) DataTool.getIntConvert("Count", item, 1);
                int onSale = DataTool.getIntConvert("OnSale", item, 0);
                Integer priority = DataTool.getInteger("Priority", item);
                // 我猜的
                Integer bonus = DataTool.getInteger("Bonus", item);
                Integer maplePoint = DataTool.getInteger("MaplePoint", item);
                Integer meso = DataTool.getInteger("Meso", item);
                Integer forPremiumUser = DataTool.getInteger("ForPremiumUser", item);
                Integer gender = DataTool.getInteger("Gender", item);
                Integer clz = DataTool.getInteger("Class", item);
                // limit一堆问号，是有问题还是就是这样？暂不解析这个字段
//                Integer limit = DataTool.getInteger("Limit", item);
                Integer pbCash = DataTool.getInteger("PbCash", item);
                Integer pbPoint = DataTool.getInteger("PbPoint", item);
                Integer pbGift = DataTool.getInteger("PbGift", item);
                Integer packageSN = DataTool.getInteger("PackageSN", item);
                loadedItems.put(sn, ModifiedCashItemDO.builder()
                        .sn(sn)
                        .itemId(itemId)
                        .count(count)
                        .price(price)
                        .bonus(bonus)
                        .priority(priority)
                        .period(period == 0 ? 90 : period)
                        .maplePoint(maplePoint)
                        .meso(meso)
                        .forPremiumUser(forPremiumUser)
                        .commodityGender(gender)
                        .onSale(onSale)
                        .clz(clz)
//                        .limit(limit)
                        .pbCash(pbCash)
                        .pbPoint(pbPoint)
                        .pbGift(pbGift)
                        .packageSn(packageSN)
                        .build());
            }
            CashItemFactory.items = loadedItems;

            Map<Integer, List<Integer>> loadedPackages = new HashMap<>();
            for (Data cashPackage : etc.getData("CashPackage.img").getChildren()) {
                List<Integer> cPackage = new ArrayList<>();

                for (Data item : cashPackage.getChildByPath("SN").getChildren()) {
                    cPackage.add(Integer.parseInt(item.getData().toString()));
                }

                loadedPackages.put(Integer.parseInt(cashPackage.getName()), cPackage);
            }
            CashItemFactory.packages = loadedPackages;

            loadCashCategories();
            loadAllModifiedCashItems();
        }


        /**
         * 加载所有修改过的现金商品数据
         * 从数据库中获取所有经过修改的现金商品信息，并存储到modifiedCashItems映射中
         * 这些商品信息会覆盖WZ文件中的默认商品数据
         */
        public static void loadAllModifiedCashItems() {
            // 清空现有的修改商品缓存
            modifiedCashItems.clear();
            // 获取CashShopService实例并加载所有修改过的现金商品
            CashShopService cashShopService = ServerManager.getApplicationContext().getBean(CashShopService.class);
            cashShopService.loadAllModifiedCashItems().forEach(modifiedCashItemDO -> modifiedCashItems.put(modifiedCashItemDO.getSn(), modifiedCashItemDO));
        }

        /**
         * 加载商城分类数据
         * 从数据库中获取所有商城商品分类信息，并添加到cashCategories列表中
         * 用于构建商城界面的商品分类展示
         */
        private static void loadCashCategories() {
            // 注意：此处原代码有误，应该是清空cashCategories而不是modifiedCashItems
            cashCategories.clear();
            // 获取CashShopService实例并加载所有商城分类
            CashShopService cashShopService = ServerManager.getApplicationContext().getBean(CashShopService.class);
            cashCategories.addAll(cashShopService.getAllCategoryList());
        }

        public static Optional<ModifiedCashItemDO> getRandomCashItem() {
            if (items.isEmpty()) {
                return Optional.empty();
            }

            List<ModifiedCashItemDO> itemPool = items.values().stream()
                    .filter(ModifiedCashItemDO::isSelling)
                    .filter(cashItem -> !ItemId.isCashPackage(cashItem.getItemId()))
                    .toList();
            return Optional.of(getRandomItem(itemPool));
        }

        private static ModifiedCashItemDO getRandomItem(List<ModifiedCashItemDO> items) {
            return items.get(new Random().nextInt(items.size()));
        }

        /**
         * 处理倍率卡商品的上架/下架状态<br>
         * <br>
         * @param sale true表示上架倍率卡商品，false表示下架倍率卡商品<br>
         * <br>
         * 当sale为false时，将所有倍率卡商品标记为下架状态并添加到已下架商品列表中<br>
         * 当sale为true时，从已下架商品列表中移除所有倍率卡商品
         */
        public static void processRateCouponItems(boolean sale) {
            if (!sale) { // 如果状态为下架，则将倍率卡商品标记为下架并添加到 discontinuedCashItems 列表中
                // 遍历所有商品，筛选出倍率卡商品并将其设置为下架状态
                items.values().stream()
                        .filter(item -> ItemConstants.isRateCoupon(item.getItemId())) // 筛选倍率卡商品
                        .forEach(item -> {
                            item.setOnSale(0); // 设置为下架状态（0表示下架）
                            discontinuedCashItems.put(item.getSn(), item); // 添加到已下架商品列表中
                        });
            } else {
                // 如果状态为上架，则将所有倍率卡商品设置为上架状态，然后从已下架商品列表中移除
                discontinuedCashItems.values().stream()
                        .filter(item -> ItemConstants.isRateCoupon(item.getItemId()))
                        .forEach(item -> item.setOnSale(1));
                discontinuedCashItems.values().removeIf(item -> ItemConstants.isRateCoupon(item.getItemId()));
            }
        }

        /**
         * 处理宠物装备商品的永久化设置<br>
         * <br>
         * @param makePermanent true表示将符合条件的宠物装备设置为永久，false表示取消永久设置
         * <br>
         * 该方法根据makePermanent参数决定是否将宠物装备设置为永久有效：<br>
         * 当makePermanent为true时，筛选出所有宠物装备商品中符合以下条件的商品：<br>
         *   1. 是宠物装备（ItemConstants.isPetEquip判断）<br>
         *   2. 属于装备类型（InventoryType.EQUIP）<br>
         *   3. 具有升级槽位（getUpgradeSlots() > 0）<br>
         * 对于符合条件的商品，将其有效期设为永久（period=0）并加入永久商品列表<br>
         * <br>
         * 当makePermanent为false时，从永久商品列表中移除所有宠物装备商品<br>
         */
        public static void processPetEquipItems(boolean makePermanent) {
            if (makePermanent) {
                items.values().stream()
                        .filter(item -> ItemConstants.isPetEquip(item.getItemId())) //宠物装备
                        .filter(item -> {
                            Item cItem = item.toItem();
                            return cItem.getInventoryType().equals(InventoryType.EQUIP) && ((Equip) cItem).getUpgradeSlots() > 0;
                        })
                        .forEach(item -> {
                            ModifiedCashItemDO clonedItem = item.clone();// 创建副本，不修改原始数据
                            clonedItem.setPeriod(0L);
                            permanentCashItems.put(clonedItem.getSn(), clonedItem);
                        });
            } else {
                permanentCashItems.values().removeIf(item -> (ItemConstants.isPetEquip(item.getItemId()) && item.toItem().getInventoryType().equals(InventoryType.EQUIP) && ((Equip) item.toItem()).getUpgradeSlots() > 0));
            }
        }

        public static ModifiedCashItemDO getItem(int sn) {
            ModifiedCashItemDO cashItemDO = items.get(sn);
            if(cashItemDO == null) {
                return null;
            }
            ModifiedCashItemDO dbItemDO = modifiedCashItems.get(sn);
            ModifiedCashItemDO returnDo = cashItemDO.clone();
            if (dbItemDO != null) {
                returnDo.setItemId(Optional.ofNullable(dbItemDO.getItemId()).orElse(cashItemDO.getItemId()));
                returnDo.setPrice(Optional.ofNullable(dbItemDO.getPrice()).orElse(cashItemDO.getPrice()));
                returnDo.setPeriod(Optional.ofNullable(dbItemDO.getPeriod()).orElse(cashItemDO.getPeriod()));
                returnDo.setPriority(Optional.ofNullable(dbItemDO.getPriority()).orElse(cashItemDO.getPriority()));
                returnDo.setCount(Optional.ofNullable(dbItemDO.getCount()).orElse(cashItemDO.getCount()));
                returnDo.setOnSale(Optional.ofNullable(dbItemDO.getOnSale()).orElse(cashItemDO.getOnSale()));
                returnDo.setBonus(Optional.ofNullable(dbItemDO.getBonus()).orElse(cashItemDO.getBonus()));
                returnDo.setMaplePoint(Optional.ofNullable(dbItemDO.getMaplePoint()).orElse(cashItemDO.getMaplePoint()));
                returnDo.setMeso(Optional.ofNullable(dbItemDO.getMeso()).orElse(cashItemDO.getMeso()));
                returnDo.setForPremiumUser(Optional.ofNullable(dbItemDO.getForPremiumUser()).orElse(cashItemDO.getForPremiumUser()));
                returnDo.setCommodityGender(Optional.ofNullable(dbItemDO.getCommodityGender()).orElse(cashItemDO.getCommodityGender()));
                returnDo.setClz(Optional.ofNullable(dbItemDO.getClz()).orElse(cashItemDO.getClz()));
                returnDo.setLimit(Optional.ofNullable(dbItemDO.getLimit()).orElse(cashItemDO.getLimit()));
                returnDo.setPbCash(Optional.ofNullable(dbItemDO.getPbCash()).orElse(cashItemDO.getPbCash()));
                returnDo.setPbPoint(Optional.ofNullable(dbItemDO.getPbPoint()).orElse(cashItemDO.getPbPoint()));
                returnDo.setPbGift(Optional.ofNullable(dbItemDO.getPbGift()).orElse(cashItemDO.getPbGift()));
                returnDo.setPackageSn(Optional.ofNullable(dbItemDO.getPackageSn()).orElse(cashItemDO.getPackageSn()));
            }
            return returnDo;
        }

        public static ModifiedCashItemDO getWzItem(int sn) {
            return items.get(sn);
        }

        public static List<Item> getPackage(int itemId) {
            List<Item> cashPackage = new ArrayList<>();

            for (int sn : packages.get(itemId)) {
                cashPackage.add(getItem(sn).toItem());
            }

            return cashPackage;
        }

        public static boolean isPackage(int itemId) {
            return packages.containsKey(itemId);
        }

    }

    public record CashShopSurpriseResult(Item usedCashShopSurprise, Item reward) {
    }

    public String getCashName(int type) {
        return switch (type) {
            case NX_CREDIT -> "点券";
            case MAPLE_POINT -> "抵用券";
            case NX_PREPAID -> "信用点";
            default -> "未知:" + type;
        };
    }
    public int getCash(int type) {
        return switch (type) {
            case NX_CREDIT -> nxCredit;
            case MAPLE_POINT -> maplePoint;
            case NX_PREPAID -> nxPrepaid;
            default -> 0;
        };
    }

    public void gainCash(int type, int cash) {
        switch (type) {
            case NX_CREDIT -> nxCredit += cash;
            case MAPLE_POINT -> maplePoint += cash;
            case NX_PREPAID -> nxPrepaid += cash;
        }
    }

    public void gainCash(int type, ModifiedCashItemDO buyItem, int world) {
        gainCash(type, -buyItem.getPrice());
        if (!GameConfig.getServerBoolean("use_enforce_item_suggestion")) {
            Server.getInstance().getWorld(world).addCashItemBought(buyItem.getSn());
        }
    }

    public boolean isOpened() {
        return opened;
    }

    public void open(boolean b) {
        opened = b;
    }

    public List<Item> getInventory() {
        lock.lock();
        try {
            return Collections.unmodifiableList(inventory);
        } finally {
            lock.unlock();
        }
    }

    public Item findByCashId(int cashId) {
        boolean isRing;
        Equip equip = null;
        for (Item item : getInventory()) {
            if (item.getInventoryType().equals(InventoryType.EQUIP)) {
                equip = (Equip) item;
                isRing = equip.getRingId() > -1;
            } else {
                isRing = false;
            }

            if ((item.getPetId() > -1 ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId()) == cashId) {
                return item;
            }
        }

        return null;
    }

    public void addToInventory(Item item) {
        lock.lock();
        try {
            inventory.add(item);
        } finally {
            lock.unlock();
        }
    }

    public void removeFromInventory(Item item) {
        lock.lock();
        try {
            inventory.remove(item);
        } finally {
            lock.unlock();
        }
    }

    public List<Integer> getWishList() {
        return wishList;
    }

    public void clearWishList() {
        wishList.clear();
    }

    public void addToWishList(int sn) {
        wishList.add(sn);
    }

    public void gift(int recipient, String from, String message, int sn) {
        gift(recipient, from, message, sn, -1);
    }

    public void gift(int recipient, String from, String message, int sn, int ringid) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO `gifts` VALUES (DEFAULT, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, recipient);
            ps.setString(2, from);
            ps.setString(3, message);
            ps.setInt(4, sn);
            ps.setInt(5, ringid);
            ps.executeUpdate();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    public List<Pair<Item, String>> loadGifts() {
        List<Pair<Item, String>> gifts = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection()) {

            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM `gifts` WHERE `to` = ?")) {
                ps.setInt(1, characterId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        notes++;
                        ModifiedCashItemDO cItem = CashItemFactory.getItem(rs.getInt("sn"));
                        Item item = cItem.toItem();
                        Equip equip = null;
                        item.setGiftFrom(rs.getString("from"));
                        if (item.getInventoryType().equals(InventoryType.EQUIP)) {
                            equip = (Equip) item;
                            equip.setRingId(rs.getInt("ringid"));
                            gifts.add(new Pair<>(equip, rs.getString("message")));
                        } else {
                            gifts.add(new Pair<>(item, rs.getString("message")));
                        }

                        if (CashItemFactory.isPackage(cItem.getItemId())) { //Packages never contains a ring
                            for (Item packageItem : CashItemFactory.getPackage(cItem.getItemId())) {
                                packageItem.setGiftFrom(rs.getString("from"));
                                addToInventory(packageItem);
                            }
                        } else {
                            addToInventory(equip == null ? item : equip);
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM `gifts` WHERE `to` = ?")) {
                ps.setInt(1, characterId);
                ps.executeUpdate();
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return gifts;
    }

    public int getAvailableNotes() {
        return notes;
    }

    public void decreaseNotes() {
        notes--;
    }

    public void save(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `nxCredit` = ?, `maplePoint` = ?, `nxPrepaid` = ? WHERE `id` = ?")) {
            ps.setInt(1, nxCredit);
            ps.setInt(2, maplePoint);
            ps.setInt(3, nxPrepaid);
            ps.setInt(4, accountId);
            ps.executeUpdate();
        }

        List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();

        List<Item> inv = getInventory();
        for (Item item : inv) {
            itemsWithType.add(new Pair<>(item, item.getInventoryType()));
        }

        factory.saveItems(itemsWithType, accountId, con);

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM `wishlists` WHERE `charid` = ?")) {
            ps.setInt(1, characterId);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = con.prepareStatement("INSERT INTO `wishlists` VALUES (DEFAULT, ?, ?)")) {
            ps.setInt(1, characterId);

            for (int sn : wishList) {
                // TODO: batch insert
                ps.setInt(2, sn);
                ps.executeUpdate();
            }
        }
    }

    public Optional<CashShopSurpriseResult> openCashShopSurprise(long cashId) {
        lock.lock();
        try {
            Optional<Item> maybeCashShopSurprise = getItemByCashId(cashId);
            if (maybeCashShopSurprise.isEmpty() ||
                    maybeCashShopSurprise.get().getItemId() != ItemId.CASH_SHOP_SURPRISE) {
                return Optional.empty();
            }

            Item cashShopSurprise = maybeCashShopSurprise.get();
            if (cashShopSurprise.getQuantity() <= 0) {
                return Optional.empty();
            }

            if (getItemsSize() >= 100) {
                return Optional.empty();
            }

            Optional<ModifiedCashItemDO> cashItemReward = CashItemFactory.getRandomCashItem();
            if (cashItemReward.isEmpty()) {
                return Optional.empty();
            }

            short newQuantity = (short) (cashShopSurprise.getQuantity() - 1);
            cashShopSurprise.setQuantity(newQuantity);
            if (newQuantity <= 0) {
                removeFromInventory(cashShopSurprise);
            }

            Item itemReward = cashItemReward.get().toItem();
            addToInventory(itemReward);

            return Optional.of(new CashShopSurpriseResult(cashShopSurprise, itemReward));
        } finally {
            lock.unlock();
        }
    }

    @GuardedBy("lock")
    private Optional<Item> getItemByCashId(long cashId) {
        return inventory.stream()
                .filter(item -> item.getCashId() == cashId)
                .findAny();
    }

    public int getItemsSize() {
        lock.lock();
        try {
            return inventory.size();
        } finally {
            lock.unlock();
        }
    }

    public static Item generateCouponItem(int itemId, short quantity) {
        return ModifiedCashItemDO.builder()
                .sn(77777777)
                .itemId(itemId)
                .price(777)
                .period(ItemConstants.isPet(itemId) ? 30L : 0L)
                .count(quantity)
                .onSale(1)
                .priority(0)
                .build()
                .toItem();
    }
}
