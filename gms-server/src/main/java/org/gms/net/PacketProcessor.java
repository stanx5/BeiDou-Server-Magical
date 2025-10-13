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
package org.gms.net;

import org.gms.net.netty.LoginServer;
import org.gms.net.opcodes.RecvOpcode;
import org.gms.net.server.channel.handlers.*;
import org.gms.net.server.handlers.CustomPacketHandler;
import org.gms.net.server.handlers.KeepAliveHandler;
import org.gms.net.server.handlers.LoginRequiringNoOpHandler;
import org.gms.net.server.handlers.login.AcceptToSHandler;
import org.gms.net.server.handlers.login.AfterLoginHandler;
import org.gms.net.server.handlers.login.CharSelectedHandler;
import org.gms.net.server.handlers.login.CharSelectedWithPicHandler;
import org.gms.net.server.handlers.login.CharlistRequestHandler;
import org.gms.net.server.handlers.login.CheckCharNameHandler;
import org.gms.net.server.handlers.login.CreateCharHandler;
import org.gms.net.server.handlers.login.DeleteCharHandler;
import org.gms.net.server.handlers.login.GuestLoginHandler;
import org.gms.net.server.handlers.login.LoginPasswordHandler;
import org.gms.net.server.handlers.login.RegisterPicHandler;
import org.gms.net.server.handlers.login.RegisterPinHandler;
import org.gms.net.server.handlers.login.RelogRequestHandler;
import org.gms.net.server.handlers.login.ServerStatusRequestHandler;
import org.gms.net.server.handlers.login.ServerlistRequestHandler;
import org.gms.net.server.handlers.login.SetGenderHandler;
import org.gms.net.server.handlers.login.ViewAllCharHandler;
import org.gms.net.server.handlers.login.ViewAllCharRegisterPicHandler;
import org.gms.net.server.handlers.login.ViewAllCharSelectedHandler;
import org.gms.net.server.handlers.login.ViewAllCharSelectedWithPicHandler;
import org.gms.net.server.handlers.login.LoginInitHandler;
import org.gms.net.client.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PacketProcessor {
    private static final Logger log = LoggerFactory.getLogger(PacketProcessor.class);
    private static final Map<String, PacketProcessor> instances = new LinkedHashMap<>();

    private static ChannelDependencies channelDeps;

    private PacketHandler[] handlers;

    private PacketProcessor() {
        int maxRecvOp = 0;
        for (RecvOpcode op : RecvOpcode.values()) {
            if (op.getValue() > maxRecvOp) {
                maxRecvOp = op.getValue();
            }
        }
        handlers = new PacketHandler[maxRecvOp + 1];
    }

    public static void registerGameHandlerDependencies(ChannelDependencies channelDependencies) {
        PacketProcessor.channelDeps = channelDependencies;
    }

    public static PacketProcessor getLoginServerProcessor() {
        return getProcessor(LoginServer.WORLD_ID, LoginServer.CHANNEL_ID);
    }

    public static PacketProcessor getChannelServerProcessor(int world, int channel) {
        if (channelDeps == null) {
            throw new IllegalStateException("无法获取通道服务器处理器 - 未注册依赖关系");
        }

        return getProcessor(world, channel);
    }

    public PacketHandler getHandler(short packetId) {
        if (packetId > handlers.length) {
            return null;
        }
        return handlers[packetId];
    }

    public void registerHandler(RecvOpcode code, PacketHandler handler) {
        try {
            handlers[code.getValue()] = handler;
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("注册处理程序 {} 时出错", code.name(), e);
        }
    }

    public synchronized static PacketProcessor getProcessor(int world, int channel) {
        final String processorId = world + " " + channel;
        PacketProcessor processor = instances.get(processorId);
        if (processor == null) {
            processor = new PacketProcessor();
            processor.reset(channel);
            instances.put(processorId, processor);
        }
        return processor;
    }

    public void reset(int channel) {
        handlers = new PacketHandler[handlers.length];

        registerCommonHandlers();

        if (channel < 0) {
            registerLoginHandlers();
        } else {
            registerChannelHandlers();
        }
    }

    private void registerCommonHandlers() {
        registerHandler(RecvOpcode.PONG, new KeepAliveHandler()); // 心跳包处理 - 客户端保活
        registerHandler(RecvOpcode.CUSTOM_PACKET, new CustomPacketHandler()); // 自定义数据包处理
        registerHandler(RecvOpcode.CLIENT_START_ERROR, new ClientStartErrorHandler()); // 客户端启动错误处理
        registerHandler(RecvOpcode.CLIENT_ERROR, new ClientErrorHandler()); // 客户端一般错误处理
    }

    private void registerLoginHandlers() {
        registerHandler(RecvOpcode.ACCEPT_TOS, new AcceptToSHandler()); // 接受服务条款处理
        registerHandler(RecvOpcode.AFTER_LOGIN, new AfterLoginHandler()); // 登录后处理
        registerHandler(RecvOpcode.SERVERLIST_REREQUEST, new ServerlistRequestHandler()); // 重新请求服务器列表
        registerHandler(RecvOpcode.CHARLIST_REQUEST, new CharlistRequestHandler()); // 请求角色列表
        registerHandler(RecvOpcode.CHAR_SELECT, new CharSelectedHandler()); // 选择角色处理
        registerHandler(RecvOpcode.LOGIN_PASSWORD, new LoginPasswordHandler()); // 密码登录处理
        registerHandler(RecvOpcode.RELOG, new RelogRequestHandler()); // 重新登录请求处理
        registerHandler(RecvOpcode.SERVERLIST_REQUEST, new ServerlistRequestHandler()); // 请求服务器列表
        registerHandler(RecvOpcode.SERVERSTATUS_REQUEST, new ServerStatusRequestHandler()); // 请求服务器状态
        registerHandler(RecvOpcode.CHECK_CHAR_NAME, new CheckCharNameHandler()); // 检查角色名是否可用
        registerHandler(RecvOpcode.CREATE_CHAR, new CreateCharHandler()); // 创建角色处理
        registerHandler(RecvOpcode.DELETE_CHAR, new DeleteCharHandler()); // 删除角色处理
        registerHandler(RecvOpcode.VIEW_ALL_CHAR, new ViewAllCharHandler()); // 查看所有角色处理
        registerHandler(RecvOpcode.PICK_ALL_CHAR, new ViewAllCharSelectedHandler()); // 选择所有角色视图中的角色
        registerHandler(RecvOpcode.REGISTER_PIN, new RegisterPinHandler()); // 注册PIN码处理
        registerHandler(RecvOpcode.GUEST_LOGIN, new GuestLoginHandler()); // 游客登录处理
        registerHandler(RecvOpcode.REGISTER_PIC, new RegisterPicHandler()); // 注册PIC处理
        registerHandler(RecvOpcode.CHAR_SELECT_WITH_PIC, new CharSelectedWithPicHandler()); // 使用PIC选择角色
        registerHandler(RecvOpcode.SET_GENDER, new SetGenderHandler()); // 设置性别处理
        registerHandler(RecvOpcode.VIEW_ALL_WITH_PIC, new ViewAllCharSelectedWithPicHandler()); // 使用PIC查看所有角色
        registerHandler(RecvOpcode.VIEW_ALL_PIC_REGISTER, new ViewAllCharRegisterPicHandler()); // 为所有角色注册PIC
        registerHandler(RecvOpcode.LOGIN_INIT, new LoginInitHandler()); // 登录初始化处理
    }

    private void registerChannelHandlers() {
        registerHandler(RecvOpcode.NAME_TRANSFER, new TransferNameHandler()); // 角色名转移处理
        registerHandler(RecvOpcode.CHECK_CHAR_NAME, new TransferNameResultHandler()); // 角色名转移结果处理
        registerHandler(RecvOpcode.WORLD_TRANSFER, new TransferWorldHandler()); // 世界转移处理
        registerHandler(RecvOpcode.CHANGE_CHANNEL, new ChangeChannelHandler()); // 更换频道处理
        registerHandler(RecvOpcode.STRANGE_DATA, LoginRequiringNoOpHandler.getInstance()); // 异常数据处理
        registerHandler(RecvOpcode.GENERAL_CHAT, new GeneralChatHandler()); // 普通聊天处理
        registerHandler(RecvOpcode.WHISPER, new WhisperHandler()); // 私聊处理
        registerHandler(RecvOpcode.NPC_TALK, new NPCTalkHandler()); // NPC对话开始处理
        registerHandler(RecvOpcode.NPC_TALK_MORE, new NPCMoreTalkHandler()); // NPC继续对话处理
        registerHandler(RecvOpcode.QUEST_ACTION, new QuestActionHandler()); // 任务操作处理
        registerHandler(RecvOpcode.GRENADE_EFFECT, new GrenadeEffectHandler()); // 手雷效果处理
        registerHandler(RecvOpcode.NPC_SHOP, new NPCShopHandler()); // NPC商店处理
        registerHandler(RecvOpcode.ITEM_SORT, new InventoryMergeHandler()); // 物品整理处理
        registerHandler(RecvOpcode.ITEM_MOVE, new ItemMoveHandler()); // 物品移动处理
        registerHandler(RecvOpcode.MESO_DROP, new MesoDropHandler()); // 金币丢弃处理
        registerHandler(RecvOpcode.PLAYER_LOGGEDIN, new PlayerLoggedinHandler(channelDeps.noteService())); // 玩家登录处理
        registerHandler(RecvOpcode.CHANGE_MAP, new ChangeMapHandler()); // 地图切换处理
        registerHandler(RecvOpcode.MOVE_LIFE, new MoveLifeHandler()); // 生物移动处理
        registerHandler(RecvOpcode.CLOSE_RANGE_ATTACK, new CloseRangeDamageHandler()); // 近战攻击处理
        registerHandler(RecvOpcode.RANGED_ATTACK, new RangedAttackHandler()); // 远程攻击处理
        registerHandler(RecvOpcode.MAGIC_ATTACK, new MagicDamageHandler()); // 魔法攻击处理
        registerHandler(RecvOpcode.TAKE_DAMAGE, new TakeDamageHandler()); // 受伤处理
        registerHandler(RecvOpcode.MOVE_PLAYER, new MovePlayerHandler()); // 玩家移动处理
        registerHandler(RecvOpcode.USE_CASH_ITEM, new UseCashItemHandler(channelDeps.noteService())); // 使用商城物品处理
        registerHandler(RecvOpcode.USE_ITEM, new UseItemHandler()); // 使用物品处理
        registerHandler(RecvOpcode.USE_RETURN_SCROLL, new UseItemHandler()); // 使用回城卷轴处理
        registerHandler(RecvOpcode.USE_UPGRADE_SCROLL, new ScrollHandler()); // 使用升级卷轴处理
        registerHandler(RecvOpcode.USE_SUMMON_BAG, new UseSummonBagHandler()); // 使用召唤袋处理
        registerHandler(RecvOpcode.FACE_EXPRESSION, new FaceExpressionHandler()); // 表情处理
        registerHandler(RecvOpcode.HEAL_OVER_TIME, new HealOvertimeHandler()); // 持续治疗处理
        registerHandler(RecvOpcode.ITEM_PICKUP, new ItemPickupHandler()); // 物品拾取处理
        registerHandler(RecvOpcode.CHAR_INFO_REQUEST, new CharInfoRequestHandler()); // 请求角色信息处理
        registerHandler(RecvOpcode.SPECIAL_MOVE, new SpecialMoveHandler()); // 特殊移动处理
        registerHandler(RecvOpcode.USE_INNER_PORTAL, new InnerPortalHandler()); // 使用内部传送门处理
        registerHandler(RecvOpcode.CANCEL_BUFF, new CancelBuffHandler()); // 取消增益效果处理
        registerHandler(RecvOpcode.CANCEL_ITEM_EFFECT, new CancelItemEffectHandler()); // 取消物品效果处理
        registerHandler(RecvOpcode.PLAYER_INTERACTION, new PlayerInteractionHandler()); // 玩家交互处理
        registerHandler(RecvOpcode.RPS_ACTION, new RPSActionHandler()); // 猜拳游戏处理
        registerHandler(RecvOpcode.DISTRIBUTE_AP, new DistributeAPHandler()); // 分配能力点处理
        registerHandler(RecvOpcode.DISTRIBUTE_SP, new DistributeSPHandler()); // 分配技能点处理
        registerHandler(RecvOpcode.CHANGE_KEYMAP, new KeymapChangeHandler()); // 更改键位设置处理
        registerHandler(RecvOpcode.CHANGE_MAP_SPECIAL, new ChangeMapSpecialHandler()); // 特殊地图切换处理
        registerHandler(RecvOpcode.STORAGE, new StorageHandler()); // 仓库操作处理
        registerHandler(RecvOpcode.GIVE_FAME, new GiveFameHandler()); // 给予声望处理
        registerHandler(RecvOpcode.PARTY_OPERATION, new PartyOperationHandler()); // 组队操作处理
        registerHandler(RecvOpcode.DENY_PARTY_REQUEST, new DenyPartyRequestHandler()); // 拒绝组队请求处理
        registerHandler(RecvOpcode.MULTI_CHAT, new MultiChatHandler()); // 多人聊天处理
        registerHandler(RecvOpcode.USE_DOOR, new DoorHandler()); // 使用门处理
        registerHandler(RecvOpcode.ENTER_MTS, new EnterMTSHandler()); // 进入拍卖系统处理
        registerHandler(RecvOpcode.ENTER_CASHSHOP, new EnterCashShopHandler()); // 进入商城处理
        registerHandler(RecvOpcode.DAMAGE_SUMMON, new DamageSummonHandler()); // 召唤兽受伤处理
        registerHandler(RecvOpcode.MOVE_SUMMON, new MoveSummonHandler()); // 召唤兽移动处理
        registerHandler(RecvOpcode.SUMMON_ATTACK, new SummonDamageHandler()); // 召唤兽攻击处理
        registerHandler(RecvOpcode.BUDDYLIST_MODIFY, new BuddylistModifyHandler()); // 好友列表修改处理
        registerHandler(RecvOpcode.USE_ITEMEFFECT, new UseItemEffectHandler()); // 使用物品效果处理
        registerHandler(RecvOpcode.USE_CHAIR, new UseChairHandler()); // 使用椅子处理
        registerHandler(RecvOpcode.CANCEL_CHAIR, new CancelChairHandler()); // 取消椅子效果处理
        registerHandler(RecvOpcode.DAMAGE_REACTOR, new ReactorHitHandler()); // 反应堆受伤处理
        registerHandler(RecvOpcode.GUILD_OPERATION, new GuildOperationHandler()); // 公会操作处理
        registerHandler(RecvOpcode.DENY_GUILD_REQUEST, new DenyGuildRequestHandler()); // 拒绝公会请求处理
        registerHandler(RecvOpcode.BBS_OPERATION, new BBSOperationHandler()); // 公告板操作处理
        registerHandler(RecvOpcode.SKILL_EFFECT, new SkillEffectHandler()); // 技能效果处理
        registerHandler(RecvOpcode.MESSENGER, new MessengerHandler()); // 好友消息处理
        registerHandler(RecvOpcode.NPC_ACTION, new NPCAnimationHandler()); // NPC动作处理
        registerHandler(RecvOpcode.CHECK_CASH, new TouchingCashShopHandler()); // 检查商城处理
        registerHandler(RecvOpcode.CASHSHOP_OPERATION, new CashOperationHandler(channelDeps.noteService())); // 商城操作处理
        registerHandler(RecvOpcode.COUPON_CODE, new CouponCodeHandler()); // 优惠券代码处理
        registerHandler(RecvOpcode.SPAWN_PET, new SpawnPetHandler()); // 召唤宠物处理
        registerHandler(RecvOpcode.MOVE_PET, new MovePetHandler()); // 宠物移动处理
        registerHandler(RecvOpcode.PET_CHAT, new PetChatHandler()); // 宠物聊天处理
        registerHandler(RecvOpcode.PET_COMMAND, new PetCommandHandler()); // 宠物命令处理
        registerHandler(RecvOpcode.PET_FOOD, new PetFoodHandler()); // 宠物食物处理
        registerHandler(RecvOpcode.PET_LOOT, new PetLootHandler()); // 宠物拾取处理
        registerHandler(RecvOpcode.AUTO_AGGRO, new AutoAggroHandler()); // 自动仇恨处理
        registerHandler(RecvOpcode.MONSTER_BOMB, new MonsterBombHandler()); // 怪物炸弹处理
        registerHandler(RecvOpcode.CANCEL_DEBUFF, new CancelDebuffHandler()); // 取消负面效果处理
        registerHandler(RecvOpcode.USE_SKILL_BOOK, new SkillBookHandler()); // 使用技能书处理
        registerHandler(RecvOpcode.SKILL_MACRO, new SkillMacroHandler()); // 技能宏处理
        registerHandler(RecvOpcode.NOTE_ACTION, new NoteActionHandler(channelDeps.noteService())); // 纸条操作处理
        registerHandler(RecvOpcode.CLOSE_CHALKBOARD, new CloseChalkboardHandler()); // 关闭黑板处理
        registerHandler(RecvOpcode.USE_MOUNT_FOOD, new UseMountFoodHandler()); // 使用坐骑食物处理
        registerHandler(RecvOpcode.MTS_OPERATION, new MTSHandler()); // 拍卖系统操作处理
        registerHandler(RecvOpcode.RING_ACTION, new RingActionHandler(channelDeps.noteService())); // 戒指操作处理
        registerHandler(RecvOpcode.SPOUSE_CHAT, new SpouseChatHandler()); // 配偶聊天处理
        registerHandler(RecvOpcode.PET_AUTO_POT, new PetAutoPotHandler()); // 宠物自动喝药处理
        registerHandler(RecvOpcode.PET_EXCLUDE_ITEMS, new PetExcludeItemsHandler()); // 宠物排除物品处理
        registerHandler(RecvOpcode.OWL_ACTION, new UseOwlOfMinervaHandler()); // 使用猫头鹰之眼处理
        registerHandler(RecvOpcode.OWL_WARP, new OwlWarpHandler()); // 猫头鹰传送处理
        registerHandler(RecvOpcode.TOUCH_MONSTER_ATTACK, new TouchMonsterDamageHandler()); // 触碰怪物攻击处理
        registerHandler(RecvOpcode.TROCK_ADD_MAP, new TrockAddMapHandler()); // 增加传送石地图处理
        registerHandler(RecvOpcode.HIRED_MERCHANT_REQUEST, new HiredMerchantRequest()); // 雇佣商人请求处理
        registerHandler(RecvOpcode.MOB_BANISH_PLAYER, new MobBanishPlayerHandler()); // 怪物驱逐玩家处理
        registerHandler(RecvOpcode.MOB_DAMAGE_MOB, new MobDamageMobHandler()); // 怪物伤害怪物处理
        registerHandler(RecvOpcode.REPORT, new ReportHandler()); // 举报处理
        registerHandler(RecvOpcode.MONSTER_BOOK_COVER, new MonsterBookCoverHandler()); // 怪物手册封面处理
        registerHandler(RecvOpcode.AUTO_DISTRIBUTE_AP, new AutoAssignHandler()); // 自动分配能力点处理
        registerHandler(RecvOpcode.MAKER_SKILL, new MakerSkillHandler()); // 制作技能处理
        registerHandler(RecvOpcode.USE_TREASUER_CHEST, new UseTreasureChestHandler()); // 使用宝箱处理
        registerHandler(RecvOpcode.OPEN_FAMILY_PEDIGREE, new OpenFamilyPedigreeHandler()); // 打开家族族谱处理
        registerHandler(RecvOpcode.OPEN_FAMILY, new OpenFamilyHandler()); // 打开家族处理
        registerHandler(RecvOpcode.ADD_FAMILY, new FamilyAddHandler()); // 添加家族成员处理
        registerHandler(RecvOpcode.SEPARATE_FAMILY_BY_SENIOR, new FamilySeparateHandler()); // 长辈分离家族处理
        registerHandler(RecvOpcode.SEPARATE_FAMILY_BY_JUNIOR, new FamilySeparateHandler()); // 晚辈分离家族处理
        registerHandler(RecvOpcode.USE_FAMILY, new FamilyUseHandler()); // 使用家族功能处理
        registerHandler(RecvOpcode.CHANGE_FAMILY_MESSAGE, new FamilyPreceptsHandler()); // 更改家族训言处理
        registerHandler(RecvOpcode.FAMILY_SUMMON_RESPONSE, new FamilySummonResponseHandler()); // 家族召唤响应处理
        registerHandler(RecvOpcode.USE_HAMMER, new UseHammerHandler()); // 使用锤子处理
        registerHandler(RecvOpcode.SCRIPTED_ITEM, new ScriptedItemHandler()); // 脚本物品处理
        registerHandler(RecvOpcode.TOUCHING_REACTOR, new TouchReactorHandler()); // 触碰反应堆处理
        registerHandler(RecvOpcode.BEHOLDER, new BeholderHandler()); // 守护者处理
        registerHandler(RecvOpcode.ADMIN_COMMAND, new AdminCommandHandler()); // 管理员命令处理
        registerHandler(RecvOpcode.ADMIN_LOG, new AdminLogHandler()); // 管理员日志处理
        registerHandler(RecvOpcode.ALLIANCE_OPERATION, new AllianceOperationHandler()); // 联盟操作处理
        registerHandler(RecvOpcode.DENY_ALLIANCE_REQUEST, new DenyAllianceRequestHandler()); // 拒绝联盟请求处理
        registerHandler(RecvOpcode.USE_SOLOMON_ITEM, new UseSolomonHandler()); // 使用所罗门物品处理（既孙子兵法等经验卷轴）
        registerHandler(RecvOpcode.USE_GACHA_EXP, new UseGachaExpHandler()); // 使用转蛋经验处理
        registerHandler(RecvOpcode.NEW_YEAR_CARD_REQUEST, new NewYearCardHandler()); // 新年贺卡请求处理
        registerHandler(RecvOpcode.CASHSHOP_SURPRISE, new CashShopSurpriseHandler()); // 商城惊喜处理
        registerHandler(RecvOpcode.USE_ITEM_REWARD, new ItemRewardHandler()); // 使用物品奖励处理
        registerHandler(RecvOpcode.USE_REMOTE, new RemoteGachaponHandler()); // 远程转蛋处理
        registerHandler(RecvOpcode.ACCEPT_FAMILY, new AcceptFamilyHandler()); // 接受家族邀请处理
        registerHandler(RecvOpcode.DUEY_ACTION, new DueyHandler()); // 快递服务处理
        registerHandler(RecvOpcode.USE_DEATHITEM, new UseDeathItemHandler()); // 使用死亡物品处理
        registerHandler(RecvOpcode.PLAYER_MAP_TRANSFER, new PlayerMapTransitionHandler()); // 玩家地图传送处理
        registerHandler(RecvOpcode.USE_MAPLELIFE, new UseMapleLifeHandler()); // 使用枫叶生命处理
        registerHandler(RecvOpcode.USE_CATCH_ITEM, new UseCatchItemHandler()); // 使用捕捉物品处理
        registerHandler(RecvOpcode.FIELD_DAMAGE_MOB, new FieldDamageMobHandler()); // 场地伤害怪物处理
        registerHandler(RecvOpcode.MOB_DAMAGE_MOB_FRIENDLY, new MobDamageMobFriendlyHandler()); // 怪物友好伤害处理
        registerHandler(RecvOpcode.PARTY_SEARCH_REGISTER, new PartySearchRegisterHandler()); // 组队搜索注册处理
        registerHandler(RecvOpcode.PARTY_SEARCH_START, new PartySearchStartHandler()); // 开始组队搜索处理
        registerHandler(RecvOpcode.PARTY_SEARCH_UPDATE, new PartySearchUpdateHandler()); // 更新组队搜索处理
        registerHandler(RecvOpcode.ITEM_SORT2, new InventorySortHandler()); // 物品排序处理
        registerHandler(RecvOpcode.LEFT_KNOCKBACK, new LeftKnockbackHandler()); // 左侧击退处理
        registerHandler(RecvOpcode.SNOWBALL, new SnowballHandler()); // 雪球处理
        registerHandler(RecvOpcode.COCONUT, new CoconutHandler()); // 椰子处理
        registerHandler(RecvOpcode.ARAN_COMBO_COUNTER, new AranComboHandler()); // 阿兰连击计数处理（战神连击）
        registerHandler(RecvOpcode.CLICK_GUIDE, new ClickGuideHandler()); // 点击指南处理
        registerHandler(RecvOpcode.FREDRICK_ACTION, new FredrickHandler(channelDeps.fredrickProcessor())); // 弗雷德里克操作处理
        registerHandler(RecvOpcode.MONSTER_CARNIVAL, new MonsterCarnivalHandler()); // 怪物嘉年华处理
        registerHandler(RecvOpcode.REMOTE_STORE, new RemoteStoreHandler()); // 远程存储处理
        registerHandler(RecvOpcode.WEDDING_ACTION, new WeddingHandler()); // 婚礼操作处理
        registerHandler(RecvOpcode.WEDDING_TALK, new WeddingTalkHandler()); // 婚礼谈话处理
        registerHandler(RecvOpcode.WEDDING_TALK_MORE, new WeddingTalkMoreHandler()); // 婚礼继续谈话处理
        registerHandler(RecvOpcode.WATER_OF_LIFE, new UseWaterOfLifeHandler()); // 使用生命之水处理
        registerHandler(RecvOpcode.ADMIN_CHAT, new AdminChatHandler()); // 管理员聊天处理
        registerHandler(RecvOpcode.MOVE_DRAGON, new MoveDragonHandler()); // 龙移动处理
        registerHandler(RecvOpcode.OPEN_ITEMUI, new RaiseUIStateHandler()); // 打开物品UI处理
        registerHandler(RecvOpcode.USE_ITEMUI, new RaiseIncExpHandler()); // 使用物品UI处理
        registerHandler(RecvOpcode.CHANGE_QUICKSLOT, new QuickslotKeyMappedModifiedHandler()); // 更改快捷栏处理
        registerHandler(RecvOpcode.SET_HPMPALERT, new SetHpMpAlertHandler()); // 设置HP/MP警报处理
    }
}
