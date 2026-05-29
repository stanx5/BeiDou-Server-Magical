/* 
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any otheer version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; witout even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.


 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gms.client;

import lombok.Getter;
import lombok.Setter;
import org.gms.client.cheatsystem.core.CheatManager;
import org.gms.client.cheatsystem.manager.CheatModuleManager;
import org.gms.client.cheatsystem.plugin.ItemVacPlugin;
import org.gms.client.autoban.AutobanManager;
import org.gms.client.creator.CharacterFactoryRecipe;
import org.gms.client.inventory.*;
import org.gms.client.inventory.Equip.StatUpgrade;
import org.gms.client.inventory.manipulator.InventoryManipulator;
import org.gms.client.keybind.KeyBinding;
import org.gms.client.keybind.QuickslotBinding;
import org.gms.client.processor.action.PetAutopotProcessor;
import org.gms.client.processor.npc.FredrickProcessor;
import org.gms.config.GameConfig;
import org.gms.constants.game.DelayedQuestUpdate;
import org.gms.constants.game.ExpTable;
import org.gms.constants.game.GameConstants;
import org.gms.constants.id.ItemId;
import org.gms.constants.id.MapId;
import org.gms.constants.id.MobId;
import org.gms.constants.inventory.ItemConstants;
import org.gms.constants.net.ServerConstants;
import org.gms.constants.skills.*;
import org.gms.constants.string.ExtendType;
import org.gms.dao.entity.*;
import org.gms.exception.NotEnabledException;
import org.gms.manager.ServerManager;
import org.gms.model.dto.InventorySearchReqDTO;
import org.gms.model.dto.InventorySearchRtnDTO;
import org.gms.model.dto.UpdateCharacterReqDTO;
import org.gms.model.pojo.NewYearCardRecord;
import org.gms.model.pojo.SkillEntry;
import org.gms.net.packet.Packet;
import org.gms.net.server.PlayerBuffValueHolder;
import org.gms.net.server.PlayerCoolDownValueHolder;
import org.gms.net.server.Server;
import org.gms.net.server.coordinator.world.InviteCoordinator;
import org.gms.net.server.guild.Alliance;
import org.gms.net.server.guild.Guild;
import org.gms.net.server.guild.GuildCharacter;
import org.gms.net.server.guild.GuildPackets;
import org.gms.net.server.services.task.world.CharacterSaveService;
import org.gms.net.server.services.type.WorldServices;
import org.gms.net.server.world.*;
import org.gms.scripting.AbstractPlayerInteraction;
import org.gms.scripting.event.EventInstanceManager;
import org.gms.scripting.item.ItemScriptManager;
import org.gms.server.*;
import org.gms.server.ExpLogger.ExpLogRecord;
import org.gms.server.ItemInformationProvider.ScriptedItem;
import org.gms.server.events.Events;
import org.gms.server.events.RescueGaga;
import org.gms.server.events.gm.Fitness;
import org.gms.server.events.gm.Ola;
import org.gms.server.life.*;
import org.gms.server.maps.*;
import org.gms.server.maps.MiniGame.MiniGameResult;
import org.gms.server.minigame.RockPaperScissor;
import org.gms.server.partyquest.AriantColiseum;
import org.gms.server.partyquest.MonsterCarnival;
import org.gms.server.partyquest.MonsterCarnivalParty;
import org.gms.server.partyquest.PartyQuest;
import org.gms.server.quest.Quest;
import org.gms.service.*;
import org.gms.util.*;
import org.gms.util.packets.WeddingPackets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.*;

@Getter
public class Character extends AbstractCharacterObject {
    private static final Logger log = LoggerFactory.getLogger(Character.class);

    @Setter
    private int world;
    @Getter
    @Setter
    private int id;
    @Getter
    @Setter
    private int accountId;
    @Getter
    @Setter
    private int level;
    @Getter
    @Setter
    private int rank;
    @Getter
    @Setter
    private int rankMove;
    @Getter
    @Setter
    private int jobRank;
    @Getter
    @Setter
    private int jobRankMove;
    @Setter
    @Getter
    private int gender;
    @Setter
    @Getter
    private int hair;
    @Setter
    @Getter
    private int face;
    @Setter
    @Getter
    private int fame;
    @Getter
    @Setter
    private int questFame;
    @Getter
    @Setter
    private int initialSpawnPoint;
    @Setter
    private int mapId;
    @Getter
    private int currentPage;
    @Getter
    private int currentType = 0;
    @Getter
    private int currentTab = 1;
    @Setter
    @Getter
    private int itemEffect;
    @Setter
    @Getter
    private int guildId;
    @Setter
    @Getter
    private int guildRank;
    @Setter
    @Getter
    private int allianceRank;
    @Setter
    @Getter
    private int messengerPosition = 4;
    private int slots = 0;
    @Getter
    @Setter
    private int energyBar;
    private int gmLevel;
    @Getter
    private int ci = 0;
    @Getter
    private FamilyEntry familyEntry;
    @Setter
    @Getter
    private int familyId;
    @Setter
    private int bookCover;
    @Setter
    @Getter
    private int battleshipHp = 0;
    @Getter
    private int mesosTraded = 0;
    @Getter
    private int possibleReports = 10;
    @Getter
    @Setter
    private int ariantPoints;
    @Setter
    @Getter
    private int dojoPoints;
    @Getter
    @Setter
    private int vanquisherStage;
    @Setter
    @Getter
    private int dojoStage;
    @Getter
    private int dojoEnergy;
    @Getter
    @Setter
    private int vanquisherKills;
    private float expRate = 1;
    @Getter
    private float mesoRate = 1;
    @Getter
    private float dropRate = 1;
    private int expCoupon = 1, mesoCoupon = 1, dropCoupon = 1;
    @Getter
    @Setter
    private int omokwins;
    @Getter
    @Setter
    private int omokties;
    @Getter
    @Setter
    private int omoklosses;
    @Getter
    @Setter
    private int matchcardwins;
    @Getter
    @Setter
    private int matchcardties;
    @Getter
    @Setter
    private int matchcardlosses;
    @Getter
    @Setter
    private int owlSearch;
    @Setter
    @Getter
    private long lastfametime;
    @Setter
    @Getter
    private long lastUsedCashItem;
    private long lastExpression = 0;
    private long lastMonsterDamageTime = 0;  // 记录最后一次对怪物造成伤害的时间戳，用于判断是否是组队活跃成员
    @Setter
    private long jailExpiration = -1;  // 剩余服刑时间(毫秒)，0或负数表示未服刑
    private transient int localstr, localdex, localluk, localint_, localmagic, localwatk;
    private transient int equipmaxhp, equipmaxmp, equipstr, equipdex, equipluk, equipint_, equipmagic, equipwatk, localchairhp, localchairmp;
    private int localchairrate;
    @Getter
    private boolean hidden;
    private boolean equipchanged = true, berserk, hasMerchant, hasSandboxItem = false, whiteChat = false;
    @Setter
    private boolean canRecvPartySearchInvite = true;
    @Getter
    private boolean equippedMesoMagnet = false;
    @Getter
    private boolean equippedItemPouch = false;
    @Getter
    private boolean equippedPetItemIgnore = false;
    @Getter
    private boolean equippedPetItemScales = false;
    private boolean usedSafetyCharm = false;
    @Getter
    @Setter
    private int linkedLevel = 0;
    @Getter
    @Setter
    private String linkedName = null;
    @Getter
    @Setter
    private boolean finishedDojoTutorial;
    private boolean usedStorage = false;
    @Getter
    @Setter
    private String name;
    private String chalktext;
    private String commandtext;
    @Setter
    private String dataString = "";
    @Getter
    @Setter
    private String search = null;
    private final AtomicBoolean mapTransitioning = new AtomicBoolean(true);  // player client is currently trying to change maps or log in the game map //玩家客户端当前正在尝试更改地图或登录游戏地图
    private final AtomicBoolean awayFromWorld = new AtomicBoolean(true);  // player is online, but on cash shop or mts
    private final AtomicInteger exp = new AtomicInteger();
    private final AtomicInteger gachaExp = new AtomicInteger();
    private final AtomicInteger meso = new AtomicInteger();
    private final AtomicInteger chair = new AtomicInteger(-1);
    private long totalExpGained = 0;
    private int merchantmeso;
    @Getter
    @Setter
    private BuddyList buddylist;
    private EventInstanceManager eventInstance = null;
    @Setter
    @Getter
    private HiredMerchant hiredMerchant = null;
    @Getter
    @Setter
    private Client client;
    private GuildCharacter mgc = null;
    private PartyCharacter mpc = null;
    private Inventory[] inventory;
    @Setter
    @Getter
    private Job job = Job.BEGINNER;
    @Getter
    @Setter
    private Messenger messenger = null;
    @Getter
    @Setter
    private MiniGame miniGame;
    @Getter
    private RockPaperScissor rps;
    @Getter
    @Setter
    private Mount mapleMount;
    private Party party;
    private final Pet[] pets = new Pet[3];
    @Getter
    @Setter
    private PlayerShop playerShop = null;
    @Getter
    @Setter
    private Shop shop = null;
    @Getter
    @Setter
    private SkinColor skinColor = SkinColor.NORMAL;
    @Getter
    @Setter
    private Storage storage = null;
    @Getter
    @Setter
    private Trade trade = null;
    @Getter
    @Setter
    private MonsterBook monsterBook;
    @Getter
    @Setter
    private CashShop cashShop;
    private final Set<NewYearCardRecord> newyears = new LinkedHashSet<>();
    @Getter
    private final SavedLocation[] savedLocations;
    @Getter
    private final SkillMacro[] skillMacros = new SkillMacro[5];
    @Setter
    @Getter
    private List<Integer> lastmonthfameids;
    private final List<WeakReference<MapleMap>> lastVisitedMaps = new LinkedList<>();
    private WeakReference<MapleMap> ownedMap = new WeakReference<>(null);
    @Getter
    private final Map<Short, QuestStatus> quests;
    private final Set<Monster> controlled = new LinkedHashSet<>();
    private final Map<Integer, String> entered = new LinkedHashMap<>();
    private final Set<MapObject> visibleMapObjects = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Skill, SkillEntry> skills = new LinkedHashMap<>();
    private final Map<Integer, Integer> activeCoupons = new LinkedHashMap<>();
    private final Map<Integer, Integer> activeCouponRates = new LinkedHashMap<>();
    private final EnumMap<BuffStat, BuffStatValueHolder> effects = new EnumMap<>(BuffStat.class);
    private final Map<BuffStat, Byte> buffEffectsCount = new LinkedHashMap<>();
    private final Map<Disease, Long> diseaseExpires = new LinkedHashMap<>();
    private final Map<Integer, Map<BuffStat, BuffStatValueHolder>> buffEffects = new LinkedHashMap<>(); // non-overriding buffs thanks to Ronan
    private final Map<Integer, Long> buffExpires = new LinkedHashMap<>();
    @Getter
    private final Map<Integer, KeyBinding> keymap = new LinkedHashMap<>();
    private final Map<Integer, Summon> summons = new LinkedHashMap<>();
    private final Map<Integer, CooldownValueHolder> coolDowns = new LinkedHashMap<>();
    private final EnumMap<Disease, Pair<DiseaseValueHolder, MobSkill>> diseases = new EnumMap<>(Disease.class);
    @Getter
    @Setter
    private byte[] quickSlotLoaded;
    @Setter
    private QuickslotBinding quickSlotKeyMapped;
    private Door pdoor = null;
    private Map<Quest, Long> questExpirations = new LinkedHashMap<>();
    private ScheduledFuture<?> dragonBloodSchedule;
    private ScheduledFuture<?> hpDecreaseTask;
    private ScheduledFuture<?> beholderHealingSchedule, beholderBuffSchedule, berserkSchedule;
    private ScheduledFuture<?> skillCooldownTask = null;
    private ScheduledFuture<?> buffExpireTask = null;
    private ScheduledFuture<?> itemExpireTask = null;
    private ScheduledFuture<?> diseaseExpireTask = null;
    private ScheduledFuture<?> questExpireTask = null;
    private ScheduledFuture<?> recoveryTask = null;
    private ScheduledFuture<?> extraRecoveryTask = null;
    private ScheduledFuture<?> chairRecoveryTask = null;
    private ScheduledFuture<?> pendantOfSpirit = null; //1122017
    private ScheduledFuture<?> cpqSchedule = null;
    private ScheduledFuture<?> FamilyBuffTimer = null;
    /**
     * 自动吸物定时器
     */
    private final Lock chrLock = new ReentrantLock(true);
    private final Lock evtLock = new ReentrantLock(true);
    private final Lock petLock = new ReentrantLock(true);
    private final Lock prtLock = new ReentrantLock();
    private final Lock cpnLock = new ReentrantLock();
    private final Map<Integer, Set<Integer>> excluded = new LinkedHashMap<>();
    private final Set<Integer> excludedItems = new LinkedHashSet<>();
    @Getter
    private final Set<Integer> disabledPartySearchInvites = new LinkedHashSet<>();
    private long portaldelay = 0;
    @Getter
    @Setter
    private long lastCombo = 0;
    private short combocounter = 0;
    @Getter
    private final List<String> blockedPortals = new ArrayList<>();
    private final Map<Short, String> area_info = new LinkedHashMap<>();
    private AutobanManager autoBan;
    @Getter
    @Setter
    private boolean banned = false;
    private boolean blockCashShop = false;
    private boolean allowExpGain = true;
    private byte pendantExp = 0, doorSlot = -1;
    private final List<Integer> trockmaps = new ArrayList<>();
    private final List<Integer> viptrockmaps = new ArrayList<>();
    private final List<Integer> scriptTrockMaps = new ArrayList<>();
    @Getter
    private Map<String, Events> events = new LinkedHashMap<>();
    @Setter
    @Getter
    private PartyQuest partyQuest = null;
    private final List<Pair<DelayedQuestUpdate, Object[]>> npcUpdateQuests = new LinkedList<>();
    @Setter
    @Getter
    private Dragon dragon = null;
    @Setter
    private Ring marriageRing;
    @Setter
    @Getter
    private int marriageItemId = -1;
    @Setter
    @Getter
    private int partnerId = -1;
    private final List<Ring> crushRings = new ArrayList<>();
    private final List<Ring> friendshipRings = new ArrayList<>();
    @Getter
    @Setter
    private boolean loggedIn = false;
    @Getter
    private boolean useCS;  //chaos scroll upon crafting item.
    private long npcCd;
    private int newWarpMap = -1;
    private boolean canWarpMap = true;  //only one "warp" must be used per call, and this will define the right one.
    private int canWarpCounter = 0;     //counts how many times "inner warps" have been called.
    private byte extraHpRec = 0, extraMpRec = 0;
    private short extraRecInterval;
    @Setter
    @Getter
    private int targetHpBarHash = 0;
    @Setter
    @Getter
    private long targetHpBarTime = 0;
    private long nextWarningTime = 0;
    private int banishMap = -1;
    private int banishSp = -1;
    private long banishTime = 0;
    @Setter
    private long lastExpGainTime;
    private boolean pendingNameChange; //only used to change name on logout, not to be relied upon elsewhere
    @Getter
    @Setter
    private long loginTime;
    @Setter
    @Getter
    private boolean chasing = false;
    private float mobExpRate = -1;

    @Getter
    private boolean familyBuff = false;
    private boolean familyParty = false;

    // 获取 FamilyExp 的值
    @Getter
    private float familyExp = 1;
    @Getter
    private float familyDrop = 1;
    /**
     * 内置辅助管理器
     */
    @Getter
    private CheatManager cheatManager;
    
    private static final CharacterService characterService = ServerManager.getApplicationContext().getBean(CharacterService.class);
    private static final NameChangeService nameChangeService = ServerManager.getApplicationContext().getBean(NameChangeService.class);
    private static final WorldTransferService worldTransferService = ServerManager.getApplicationContext().getBean(WorldTransferService.class);
    private static final AccountService accountService = ServerManager.getApplicationContext().getBean(AccountService.class);
    private static final HpMpAlertService hpMpAlertService = ServerManager.getApplicationContext().getBean(HpMpAlertService.class);
    private static final InventoryService inventoryService = ServerManager.getApplicationContext().getBean(InventoryService.class);
    private static final QuestService questService = ServerManager.getApplicationContext().getBean(QuestService.class);
    private static final TraceabilityService traceabilityService = ServerManager.getApplicationContext().getBean(TraceabilityService.class);
    private static final ItemRecoveryService itemRecoveryaservic = ServerManager.getApplicationContext().getBean(ItemRecoveryService.class);

    private Character() {
        super.setListener(new CharacterListener(this));
        useCS = false;
        setStance(0);
        inventory = new Inventory[InventoryType.values().length];
        savedLocations = new SavedLocation[SavedLocationType.values().length];

        for (InventoryType type : InventoryType.values()) {
            byte b = 24;
            if (type == InventoryType.CASH) {
                b = 96;
            }
            inventory[type.ordinal()] = new Inventory(this, type, b);
        }
        inventory[InventoryType.CANHOLD.ordinal()] = new InventoryProof(this);

        for (int i = 0; i < SavedLocationType.values().length; i++) {
            savedLocations[i] = null;
        }
        quests = new LinkedHashMap<>();
        setPosition(new Point(0, 0));
    }

    public Job getJobStyle(byte opt) {
        return Job.getJobStyleInternal(this.getJob().getId(), opt);
    }

    public Job getJobStyle() {
        return getJobStyle((byte) ((this.getStr() > this.getDex()) ? 0x80 : 0x40));
    }

    public static Character getDefault(Client c) {
        Character ret = new Character();
        ret.client = c;
        ret.setGMLevel(0);
        ret.hp = 50;
        ret.setMaxHp(50);
        ret.mp = 5;
        ret.setMaxMp(5);
        ret.attrStr = 12;
        ret.attrDex = 5;
        ret.attrInt = 4;
        ret.attrLuk = 4;
        ret.map = null;
        ret.job = Job.BEGINNER;
        ret.level = 1;
        ret.accountId = c.getAccID();
        ret.buddylist = new BuddyList(20);
        ret.mapleMount = null;
        ret.getInventory(InventoryType.EQUIP).setSlotLimit(24);
        ret.getInventory(InventoryType.USE).setSlotLimit(24);
        ret.getInventory(InventoryType.SETUP).setSlotLimit(24);
        ret.getInventory(InventoryType.ETC).setSlotLimit(24);

        // Select a keybinding method
        boolean useCustomKeySet = GameConfig.getServerBoolean("use_custom_keyset");
        int[] selectedKey = GameConstants.getCustomKey(useCustomKeySet);
        int[] selectedType = GameConstants.getCustomType(useCustomKeySet);
        int[] selectedAction = GameConstants.getCustomAction(useCustomKeySet);

        for (int i = 0; i < selectedKey.length; i++) {
            ret.keymap.put(selectedKey[i], new KeyBinding(selectedType[i], selectedAction[i]));
        }

        //to fix the map 0 lol
        for (int i = 0; i < 5; i++) {
            ret.trockmaps.add(MapId.NONE);
        }
        for (int i = 0; i < 10; i++) {
            ret.viptrockmaps.add(MapId.NONE);
        }

        return ret;
    }
    
    /**
     * 初始化内置辅助管理器
     */
    public void initCheatManager() {
        if (this.getId() != 0 && cheatManager == null) {
            cheatManager = CheatModuleManager.getInstance().createCheatManager(this);
            CheatModuleManager.getInstance().registerAllPlugins(this);
        }
    }

    public boolean isLoggedInWorld() {
        return this.isLoggedIn() && !this.isAwayFromWorld();
    }

    public boolean isAwayFromWorld() {
        return awayFromWorld.get();
    }

    public void setEnteredChannelWorld() {
        awayFromWorld.set(false);
        client.getChannelServer().removePlayerAway(id);

        if (canRecvPartySearchInvite) {
            this.getWorldServer().getPartySearchCoordinator().attachPlayer(this);
        }
    }

    public void setAwayFromChannelWorld() {
        setAwayFromChannelWorld(false);
    }

    public void setDisconnectedFromChannelWorld() {
        setAwayFromChannelWorld(true);
    }

    private void setAwayFromChannelWorld(boolean disconnect) {
        awayFromWorld.set(true);

        if (!disconnect) {
            client.getChannelServer().insertPlayerAway(id);
        } else {
            client.getChannelServer().removePlayerAway(id);
        }
    }

    public void updatePartySearchAvailability(boolean pSearchAvailable) {
        if (pSearchAvailable) {
            if (canRecvPartySearchInvite && getParty() == null) {
                this.getWorldServer().getPartySearchCoordinator().attachPlayer(this);
            }
        } else {
            if (canRecvPartySearchInvite) {
                this.getWorldServer().getPartySearchCoordinator().detachPlayer(this);
            }
        }
    }

    public boolean toggleRecvPartySearchInvite() {
        canRecvPartySearchInvite = !canRecvPartySearchInvite;

        if (canRecvPartySearchInvite) {
            updatePartySearchAvailability(getParty() == null);
        } else {
            this.getWorldServer().getPartySearchCoordinator().detachPlayer(this);
        }

        return canRecvPartySearchInvite;
    }

    public boolean isRecvPartySearchInviteEnabled() {
        return canRecvPartySearchInvite;
    }

    public void setSessionTransitionState() {
        client.setCharacterOnSessionTransitionState(this.getId());
    }

    public void setCS(boolean cs) {
        useCS = cs;
    }

    public long getNpcCooldown() {
        return npcCd;
    }

    public void setNpcCooldown(long d) {
        npcCd = d;
    }
    
    public long getLastMonsterDamageTime() {
        return lastMonsterDamageTime;
    }
    
    public void setLastMonsterDamageTime(long time) {
        this.lastMonsterDamageTime = time;
    }

    public void addCooldown(int skillId, long startTime, long length) {
        effLock.lock();
        chrLock.lock();
        try {
            this.coolDowns.put(skillId, new CooldownValueHolder(skillId, startTime, length));
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public Ring getRingById(int id) {
        Optional<Ring> ringOptional = getCrushRings().stream().filter(ring -> ring.getRingId() == id).findFirst();
        if (ringOptional.isPresent()) {
            return ringOptional.get();
        }
        ringOptional = getFriendshipRings().stream().filter(ring -> ring.getRingId() == id).findFirst();
        if (ringOptional.isPresent()) {
            return ringOptional.get();
        }
        if (marriageRing != null && marriageRing.getRingId() == id) {
            return marriageRing;
        }
        return null;
    }

    public int getRelationshipId() {
        return getWorldServer().getRelationshipId(id);
    }

    public boolean isMarried() {
        return marriageRing != null && partnerId > 0;
    }

    public boolean hasJustMarried() {
        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            String prop = eim.getProperty("groomId");

            if (prop != null) {
                return (Integer.parseInt(prop) == id || eim.getIntProperty("brideId") == id) &&
                        (mapId == MapId.CHAPEL_WEDDING_ALTAR || mapId == MapId.CATHEDRAL_WEDDING_ALTAR);
            }
        }

        return false;
    }

    public int addDojoPointsByMap(int mapId) {
        int pts = 0;
        if (dojoPoints < 17000) {
            pts = 1 + ((mapId - 1) / 100 % 100) / 6;
            if (!MapId.isPartyDojo(this.getMapId())) {
                pts++;
            }
            this.dojoPoints += pts;
        }
        return pts;
    }

    public void addMesosTraded(int gain) {
        this.mesosTraded += gain;
    }

    public void addPet(Pet pet) {
        petLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (pets[i] == null) {
                    pets[i] = pet;
                    if (i == 0) {
                        startCheatItemVac();
                    }
                    return;
                }
            }
        } finally {
            petLock.unlock();
        }
    }

    public void addSummon(int id, Summon summon) {
        summons.put(id, summon);

        if (summon.isPuppet()) {
            map.addPlayerPuppet(this);
        }
    }

    public void addVisibleMapObject(MapObject mo) {
        visibleMapObjects.add(mo);
    }

    public void ban(String reason) {
        accountService.ban(this, reason);
    }

    public static boolean ban(String id, String reason, boolean accountId) {
        try {
            accountService.ban(id, reason, accountId);
            return true;
        } catch (Exception ex) {
            log.error(I18nUtil.getLogMessage("Character.ban.error1"), id, ex);
        }
        return false;
    }

    public int calculateMaxBaseDamage(int watk, WeaponType weapon) {
        int mainstat, secondarystat;
        if (getJob().isA(Job.THIEF) && weapon == WeaponType.DAGGER_OTHER) {
            weapon = WeaponType.DAGGER_THIEVES;
        }

        if (weapon == WeaponType.BOW || weapon == WeaponType.CROSSBOW || weapon == WeaponType.GUN) {
            mainstat = localdex;
            secondarystat = localstr;
        } else if (weapon == WeaponType.CLAW || weapon == WeaponType.DAGGER_THIEVES) {
            mainstat = localluk;
            secondarystat = localdex + localstr;
        } else {
            mainstat = localstr;
            secondarystat = localdex;
        }
        return (int) Math.ceil(((weapon.getMaxDamageMultiplier() * mainstat + secondarystat) / 100.0) * watk);
    }

    public int calculateMaxBaseDamage(int watk) {
        int maxbasedamage;
        Item weapon_item = getInventory(InventoryType.EQUIPPED).getItem((short) -11);
        if (weapon_item != null) {
            maxbasedamage = calculateMaxBaseDamage(watk, ItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId()));
        } else {
            if (job.isA(Job.PIRATE) || job.isA(Job.THUNDERBREAKER1)) {
                double weapMulti = 3;
                if (job.getId() % 100 != 0) {
                    weapMulti = 4.2;
                }

                int attack = (int) Math.min(Math.floor((2D * getLevel() + 31) / 3), 31);
                maxbasedamage = (int) Math.ceil((localstr * weapMulti + localdex) * attack / 100.0);
            } else {
                maxbasedamage = 1;
            }
        }
        return maxbasedamage;
    }

    public int calculateMaxBaseMagicDamage(int matk) {
        int maxbasedamage = matk;
        int totalint = getTotalInt();

        if (totalint > 2000) {
            maxbasedamage -= 2000;
            maxbasedamage += (int) ((0.09033024267 * totalint) + 3823.8038);
        } else {
            maxbasedamage -= totalint;

            if (totalint > 1700) {
                maxbasedamage += (int) (0.1996049769 * Math.pow(totalint, 1.300631341));
            } else {
                maxbasedamage += (int) (0.1996049769 * Math.pow(totalint, 1.290631341));
            }
        }

        return (maxbasedamage * 107) / 100;
    }

    /**
     * [新增] 计算并返回所有被动技能提供的暴击伤害总和。
     * <p>
     * 此方法会遍历角色所有已学会的技能，筛选出其中的被动技能，
     * 并累加它们提供的 `criticalDamage` 值。
     * <p>
     * 被动技能的判断标准 (启发式规则):
     * 1. 角色已学习该技能 (skillLevel > 0)。
     * 2. 该技能效果没有冷却时间 (cooldown == 0)。
     * 3. 该技能效果没有MP消耗 (mpCon == 0)。
     * 4. 该技能效果没有持续时间 (duration <= 0)，以区别于主动Buff。
     *
     * @return 所有被动技能提供的暴击伤害百分比总和。
     */
    public int getPassiveCriticalDamageBonus() {
        int passiveBonus = 0;
        // 遍历角色所有已学习的技能
        for (Map.Entry<Skill, SkillEntry> entry : getSkills().entrySet()) {
            Skill skill = entry.getKey();
            SkillEntry skillEntry = entry.getValue();

            // 确保技能有效且已学习
            if (skill == null || skillEntry.skillLevel <= 0) {
                continue;
            }

            StatEffect effect = skill.getEffect(skillEntry.skillLevel);
            if (effect == null) {
                continue;
            }

            // 应用启发式规则判断是否为被动技能
            // 规则：有概率、无冷却、无HP/MP消耗、无持续时间
            boolean isPassive = skill.getSkillType() == -1 && effect.getProp() < 1 && effect.getDamage() > 100 && effect.getCooldown() == 0 && effect.getHpCon() == 0  && effect.getMpCon() == 0 && effect.getDuration() <= 0;
            if (isPassive) {
//                System.out.println("Skill: " + SkillFactory.getSkillName(skill.getId()) + "("+skill.getId()+"), 概率: " + effect.getProp() + ", 加成: " + effect.getDamage() + ", type: " + skill.getSkillType());
                // 累加被动技能提供的暴击伤害
                passiveBonus += effect.getDamage();
            }
        }
        return passiveBonus;
    }

    /**
     * [重构] 获取总暴击伤害加成（被动技能 + 主动Buff）。
     * <p>
     * 此方法整合了来自永久生效的被动技能和临时生效的主动Buff的所有暴击伤害加成，
     * 提供了更清晰和准确的计算逻辑。
     *
     * @return 角色当前的总暴击伤害加成百分比。
     */
    public int getTotalCriticalBonus() {
        // 1. 获取所有被动技能提供的基础暴击伤害加成
        int totalBonus = getPassiveCriticalDamageBonus();

        // 2. 累加所有当前生效的主动Buff提供的暴击伤害
        // 特殊处理“火眼晶晶”，其暴击伤害值存储在buff value的低8位
        Integer sharpEyesValue = getBuffedValue(BuffStat.SHARP_EYES);
        if (sharpEyesValue != null) {
            totalBonus += sharpEyesValue & 0xFF;
        }

        // 遍历其他Buff，查找除了火眼之外的暴击伤害来源
        List<PlayerBuffValueHolder> allBuffs = getAllBuffs();
        for (PlayerBuffValueHolder buff : allBuffs) {
            if (buff.effect == null) {
                continue;
            }
            // 跳过已经处理过的“火眼晶晶”
            if (buff.effect.isSkill() && (buff.effect.getSourceId() == Bowmaster.SHARP_EYES || buff.effect.getSourceId() == Marksman.SHARP_EYES)) {
                continue;
            }
            // 累加其他buff提供的暴击伤害
            totalBonus += buff.effect.getCriticalDamage();
        }

        return totalBonus;
    }

    /**
     * 获取物理职业的武器熟练度。
     * <p>
     * 该方法会遍历角色所有已学会的技能，查找适用于当前装备武器的“精准”类被动技能，
     * 并返回其中最高的熟练度值。
     *
     * @return 最高的适用武器熟练度值（以小数形式，例如 0.6 表示 60%），如果没有则返回 0.0。
     */
    public double getWeaponMastery() {
        Item weapon = getInventory(InventoryType.EQUIPPED).getItem((short) -11);
        if (weapon == null) {
            return 0.0;
        }
        WeaponType weaponType = ItemInformationProvider.getInstance().getWeaponType(weapon.getItemId());
        double maxMastery = 1.0;

        for (Map.Entry<Skill, SkillEntry> skillEntry : getSkills().entrySet()) {
            List<WeaponType> applicableWeapons = SkillFactory.getMasterySkillWeaponTypes(skillEntry.getKey().getId());
            if (applicableWeapons != null && applicableWeapons.contains(weaponType) && skillEntry.getValue().skillLevel > 0) {
                double currentMastery = skillEntry.getKey().getEffect(skillEntry.getValue().skillLevel).getMastery();
                if (currentMastery > maxMastery) {
                    maxMastery = currentMastery;
                }
            }
        }
        return maxMastery;
    }

    /**
     * 获取特定魔法技能的熟练度。
     * <p>
     * 对于法系职业，熟练度是每个攻击技能的固有属性。
     *
     * @param skill      要查询的技能对象。
     * @param skillLevel 该技能的等级。
     * @return 该技能的熟练度值（以小数形式），如果技能或等级无效则返回 0.0。
     */
    public double getMagicMastery(Skill skill, int skillLevel) {
        if (skill == null || skillLevel <= 0) {
            return 0.0;
        }
        return skill.getEffect(skillLevel).getMastery();
    }

    public void setCombo(short count) {
        if (count < combocounter) {
            cancelEffectFromBuffStat(BuffStat.ARAN_COMBO);
        }
        combocounter = (short) Math.min(30000, count);
        if (count > 0) {
            sendPacket(PacketCreator.showCombo(combocounter));
        }
    }
    /**
     * 计算物理职业的最大攻击力。
     * <p>
     * 公式: (主属性 * 武器系数 + 副属性) / 100 * 装备攻击力
     *
     * @param watk 装备攻击力
     * @param stance 攻击姿势/动作 <br>32 趴下 <br>标拳 / 海盗枪 / 弓 / 弩 <=20 近战挥 ，>20 远程
     * @param direction 攻击方向 <br>-128 左边 <br>0 右边
     * @return 最大攻击力
     */
    /**
     * 计算物理职业的最大攻击力。
     * <p>
     * 公式: (主属性 * 武器系数 + 副属性) / 100 * 装备攻击力
     *
     * @param watk 装备攻击力
     * @return 最大攻击力
     */
    public double calculatePhysicalMaxBaseAttack(int watk) {
        Item weaponItem = getInventory(InventoryType.EQUIPPED).getItem((short) -11);// 获取角色装备栏中武器格子(-11)的物品
        if (weaponItem == null) {
            return 1;// 如果没有装备武器，返回基础攻击力1
        }
        WeaponType weaponType = ItemInformationProvider.getInstance().getWeaponType(weaponItem.getItemId());// 根据武器物品ID获取武器类型

        // [修正] 如果是飞侠职业使用短刀，则切换为飞侠专用的短刀类型
        if (getJob().isA(Job.THIEF) && weaponType == WeaponType.DAGGER_OTHER) {
            weaponType = WeaponType.DAGGER_THIEVES;
        }

        int mainStat, secondaryStat;// 定义主属性和副属性变量
        double weaponCoefficient = weaponType.getMaxDamageMultiplier();// 获取武器的最大伤害倍数系数

        // 根据武器类型确定主属性和副属性的计算方式
        if (weaponType == WeaponType.BOW || weaponType == WeaponType.CROSSBOW || weaponType == WeaponType.GUN) {
            // 弓、弩、枪类武器：主属性为敏捷，副属性为力量
            mainStat = getTotalDex();       // 主属性：敏捷
            secondaryStat = getTotalStr();   // 副属性：力量
        } else if (weaponType == WeaponType.CLAW || weaponType == WeaponType.DAGGER_THIEVES) {
            // 拳套、飞侠短刀：主属性为运气，副属性为敏捷+力量
            mainStat = getTotalLuk();                           // 主属性：运气
            secondaryStat = getTotalDex() + getTotalStr();      // 副属性：敏捷+力量
        } else {
            // 其他近战武器：主属性为力量，副属性为敏捷
            mainStat = getTotalStr();       // 主属性：力量
            secondaryStat = getTotalDex();   // 副属性：敏捷
        }

        // 计算主属性加成：主属性值 * 武器系数
        double mainStatBonus = mainStat * weaponCoefficient;
        // 返回最终计算结果：(主属性加成 + 副属性) / 100 * 武器攻击力
        return (mainStatBonus + secondaryStat) / 100.0 * watk;
    }

    /**
     * 计算物理职业的最小攻击力。
     * <p>
     * 公式: (主属性 * 0.9 * 武器熟练度 + 副属性) / 100 * 装备攻击力
     *
     * @param watk 装备攻击力
     * @return 最小攻击力
     */
    public double calculatePhysicalMinBaseAttack(int watk) {
        Item weaponItem = getInventory(InventoryType.EQUIPPED).getItem((short) -11);
        if (weaponItem == null) {
            return 1;
        }
        WeaponType weaponType = ItemInformationProvider.getInstance().getWeaponType(weaponItem.getItemId());

        // [修正] 如果是飞侠职业使用短刀，则切换为飞侠专用的短刀类型
        if (getJob().isA(Job.THIEF) && weaponType == WeaponType.DAGGER_OTHER) {
            weaponType = WeaponType.DAGGER_THIEVES;
        }

        int mainStat, secondaryStat;
        double weaponCoefficient = weaponType.getMinDamageMultiplier();
        double mastery = getWeaponMastery();

        if (weaponType == WeaponType.BOW || weaponType == WeaponType.CROSSBOW || weaponType == WeaponType.GUN) {
            mainStat = getTotalDex();
            secondaryStat = getTotalStr();
        } else if (weaponType == WeaponType.CLAW || weaponType == WeaponType.DAGGER_THIEVES) {
            mainStat = getTotalLuk();
            secondaryStat = getTotalDex() + getTotalStr();
        } else {
            mainStat = getTotalStr();
            secondaryStat = getTotalDex();
        }

        double mainStatBonus = mainStat * weaponCoefficient * 0.9 * mastery;
        return (mainStatBonus + secondaryStat) / 100.0 * watk;
    }

    /**
     * 计算法师的最大魔法伤害。
     * <p>
     * 公式: ((魔法力^2 / 1000 + 魔法力) / 30 + 智力 / 200) * 技能攻击力
     *
     * @param skill 技能对象
     * @param skillLevel 技能等级
     * @return 最大魔法伤害
     */
    public double calculateMagicMaxDamage(Skill skill, int skillLevel) {
        double magic = getTotalMagic();
        double intel = getTotalInt();
        double skillAtk = skill.getEffect(skillLevel).getMatk();
        magic = Math.min(magic,switch (skill.getId()) {
            case FPArchMage.ELQUINES , ILArchMage.IFRIT ,BlazeWizard.IFRIT -> 1999; //客户端没有针对召唤兽破功，所以需要做限制，否则伤害上限不正确
            default -> magic;
        });
        double damage = ((magic * magic / 1000.0) + magic) / 30.0 + (intel / 50.0);
        return damage * skillAtk;
    }

    /**
     * 计算法师的最小魔法伤害。
     * <p>
     * 公式: ((魔法力^2 / 1000 + 魔法力 * 熟练度 * 0.9) / 30 + 智力 / 200) * 技能攻击力
     *
     * @param skill 技能对象
     * @param skillLevel 技能等级
     * @return 最小魔法伤害
     */
    public double calculateMagicMinDamage(Skill skill, int skillLevel) {
        double magic = getTotalMagic();
        double intel = getTotalInt();
        double mastery = getMagicMastery(skill, skillLevel);
        double skillAtk = skill.getEffect(skillLevel).getMatk();

        double damage = ((magic * magic / 1000.0) + (magic * mastery * 0.9)) / 30.0 + (intel / 200.0);
        return damage * skillAtk;
    }

    public short getCombo() {
        return combocounter;
    }

    public boolean cannotEnterCashShop() {
        return blockCashShop;
    }

    public void toggleBlockCashShop() {
        blockCashShop = !blockCashShop;
    }

    public void toggleExpGain() {
        allowExpGain = !allowExpGain;
    }

    public void newClient(Client c) {
        this.loggedIn = true;
        c.setAccountName(this.client.getAccountName());//No null's for accountName
        this.setClient(c);
        this.map = c.getChannelServer().getMapFactory().getMap(getMapId());
        Portal portal = map.findClosestPlayerSpawnpoint(getPosition());
        if (portal == null) {
            portal = map.getPortal(0);
        }
        this.setPosition(portal.getPosition());
        this.initialSpawnPoint = portal.getId();
        c.setMacs(this.client.getMacs());
    }

    public String getMedalText() {
        String medal = "";
        final Item medalItem = getInventory(InventoryType.EQUIPPED).getItem((short) -49);
        if (medalItem != null) {
            medal = "<" + ItemInformationProvider.getInstance().getName(medalItem.getItemId()) + "> ";
        }
        return medal;
    }

    public void hide(boolean hide, boolean login) {
        if (isGM() && hide != this.hidden) {
            if (!hide) {
                this.hidden = false;
                sendPacket(PacketCreator.getGMEffect(0x10, (byte) 0));
                List<BuffStat> dsstat = Collections.singletonList(BuffStat.DARKSIGHT);
                getMap().broadcastGMMessage(this, PacketCreator.cancelForeignBuff(id, dsstat), false);
                getMap().broadcastSpawnPlayerMapObjectMessage(this, this, false);

                for (Summon ms : this.getSummonsValues()) {
                    getMap().broadcastNONGMMessage(this, PacketCreator.spawnSummon(ms, false), false);
                }

                for (MapObject mo : this.getMap().getMonsters()) {
                    Monster m = (Monster) mo;
                    m.aggroUpdateController();
                }
            } else {
                this.hidden = true;
                sendPacket(PacketCreator.getGMEffect(0x10, (byte) 1));
                if (!login) {
                    getMap().broadcastNONGMMessage(this, PacketCreator.removePlayerFromMap(getId()), false);
                }
                List<Pair<BuffStat, Integer>> ldsstat = Collections.singletonList(new Pair<>(BuffStat.DARKSIGHT, 0));
                getMap().broadcastGMMessage(this, PacketCreator.giveForeignBuff(id, ldsstat), false);
                this.releaseControlledMonsters();
            }
            enableActions();
        }
    }

    public void hide(boolean hide) {
        hide(hide, false);
    }

    public void toggleHide(boolean login) {
        hide(!hidden, login);
    }

    public void cancelMagicDoor() {
        List<BuffStatValueHolder> mbsvhList = getAllStatups();
        for (BuffStatValueHolder mbsvh : mbsvhList) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    private void cancelPlayerBuffs(List<BuffStat> buffstats) {
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            updateLocalStats();
            sendPacket(PacketCreator.cancelBuff(buffstats));
            if (!buffstats.isEmpty()) {
                getMap().broadcastMessage(this, PacketCreator.cancelForeignBuff(getId(), buffstats), false);
            }
        }
    }

    public static boolean canCreateChar(String name) {
        String lname = name.toLowerCase();
        for (String nameTest : ServerConstants.BLOCKED_NAMES) {
            if (lname.contains(nameTest)) {
                return false;
            }
        }
        return !existName(name) && Pattern.compile("[a-zA-Z0-9\u4e00-\u9fa5]{2,12}").matcher(name).matches(); // 加入对中文编码的检测
    }

    public static boolean existName(String name) {
        try {
            if (characterService.findByName(name) != null) {
                return true;
            }
            if (!nameChangeService.getAllNameChanges().isEmpty()) {
                return true;
            }
        } catch (Exception e) {
            log.error(I18nUtil.getLogMessage("Character.ban.error2"), e);
        }
        return false;
    }

    public boolean canDoor() {
        Door door = getPlayerDoor();
        return door == null || (door.isActive() && door.getElapsedDeployTime() > 5000);
    }

    public void setHasSandboxItem() {
        hasSandboxItem = true;
    }

    public void removeSandboxItems() {  // sandbox idea thanks to Morty
        if (!hasSandboxItem) {
            return;
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (InventoryType invType : InventoryType.values()) {
            Inventory inv = this.getInventory(invType);

            inv.lockInventory();
            try {
                for (Item item : new ArrayList<>(inv.list())) {
                    if (InventoryManipulator.isSandboxItem(item)) {
                        InventoryManipulator.removeFromSlot(client, invType, item.getPosition(), item.getQuantity(), false);
                        dropMessage(5, "[" + ii.getName(item.getItemId()) + "] " + I18nUtil.getMessage("Character.removeSandboxItems.message1"));
                    }
                }
            } finally {
                inv.unlockInventory();
            }
        }

        hasSandboxItem = false;
    }

    public void changeCI(int type) {
        this.ci = type;
    }

    public void setMasteries(int jobId) {
        int[] skills = new int[]{0, 0, 0, 0};
        if (jobId == 112) {
            skills[0] = Hero.ACHILLES;
            skills[1] = Hero.MONSTER_MAGNET;
            skills[2] = Hero.BRANDISH;
        } else if (jobId == 122) {
            skills[0] = Paladin.ACHILLES;
            skills[1] = Paladin.MONSTER_MAGNET;
            skills[2] = Paladin.BLAST;
        } else if (jobId == 132) {
            skills[0] = DarkKnight.BEHOLDER;
            skills[1] = DarkKnight.ACHILLES;
            skills[2] = DarkKnight.MONSTER_MAGNET;
        } else if (jobId == 212) {
            skills[0] = FPArchMage.BIG_BANG;
            skills[1] = FPArchMage.MANA_REFLECTION;
            skills[2] = FPArchMage.PARALYZE;
        } else if (jobId == 222) {
            skills[0] = ILArchMage.BIG_BANG;
            skills[1] = ILArchMage.MANA_REFLECTION;
            skills[2] = ILArchMage.CHAIN_LIGHTNING;
        } else if (jobId == 232) {
            skills[0] = Bishop.BIG_BANG;
            skills[1] = Bishop.MANA_REFLECTION;
            skills[2] = Bishop.HOLY_SHIELD;
        } else if (jobId == 312) {
            skills[0] = Bowmaster.BOW_EXPERT;
            skills[1] = Bowmaster.HAMSTRING;
            skills[2] = Bowmaster.SHARP_EYES;
        } else if (jobId == 322) {
            skills[0] = Marksman.MARKSMAN_BOOST;
            skills[1] = Marksman.BLIND;
            skills[2] = Marksman.SHARP_EYES;
        } else if (jobId == 412) {
            skills[0] = NightLord.SHADOW_STARS;
            skills[1] = NightLord.SHADOW_SHIFTER;
            skills[2] = NightLord.VENOMOUS_STAR;
        } else if (jobId == 422) {
            skills[0] = Shadower.SHADOW_SHIFTER;
            skills[1] = Shadower.VENOMOUS_STAB;
            skills[2] = Shadower.BOOMERANG_STEP;
        } else if (jobId == 512) {
            skills[0] = Buccaneer.BARRAGE;
            skills[1] = Buccaneer.ENERGY_ORB;
            skills[2] = Buccaneer.SPEED_INFUSION;
            skills[3] = Buccaneer.DRAGON_STRIKE;
        } else if (jobId == 522) {
            skills[0] = Corsair.ELEMENTAL_BOOST;
            skills[1] = Corsair.BULLSEYE;
            skills[2] = Corsair.WRATH_OF_THE_OCTOPI;
            skills[3] = Corsair.RAPID_FIRE;
        } else if (jobId == 2112) {
            skills[0] = Aran.OVER_SWING;
            skills[1] = Aran.HIGH_MASTERY;
            skills[2] = Aran.FREEZE_STANDING;
        } else if (jobId == 2217) {
            skills[0] = Evan.MAPLE_WARRIOR;
            skills[1] = Evan.ILLUSION;
        } else if (jobId == 2218) {
            skills[0] = Evan.BLESSING_OF_THE_ONYX;
            skills[1] = Evan.BLAZE;
        }
        for (Integer skillId : skills) {
            if (skillId != 0) {
                Skill skill = SkillFactory.getSkill(skillId);
                final int skilllevel = getSkillLevel(skill);
                if (skilllevel > 0) {
                    continue;
                }

                changeSkillLevel(skill, (byte) 0, 10, -1);
            }
        }
    }

    private void broadcastChangeJob() {
        for (Character chr : map.getAllPlayers()) {
            Client chrC = chr.getClient();

            if (chrC != null) {     // propagate new job 3rd-person effects (FJ, Aran 1st strike, etc)
                this.sendDestroyData(chrC);
                this.sendSpawnData(chrC);
            }
        }

        // need to delay to ensure clientside has finished reloading character data     //需要延迟以确保客户端已完成重新加载角色数据
        TimerManager.getInstance().schedule(() -> {
            Character thisChr = Character.this;
            MapleMap map = thisChr.getMap();

            if (map != null) {
                map.broadcastMessage(thisChr, PacketCreator.showForeignEffect(thisChr.getId(), 8), false);
            }
        }, 777);
    }

    public synchronized void changeJob(Job newJob) {
        if (newJob == null) {
            return;//the fuck you doing idiot!
        }

        if (canRecvPartySearchInvite && getParty() == null) {
            this.updatePartySearchAvailability(false);
            this.job = newJob;
            this.updatePartySearchAvailability(true);
        } else {
            this.job = newJob;
        }

        int spGain = 1;
        if (GameConstants.hasSPTable(newJob)) {
            spGain += 2;
        } else {
            if (newJob.getId() % 10 == 2) {
                spGain += 2;
            }

            if (GameConfig.getServerBoolean("use_enforce_job_sp_range")) {
                spGain = getChangedJobSp(newJob);
            }
        }

        if (spGain > 0) {
            gainSp(spGain, GameConstants.getSkillBook(newJob.getId()), true);
        }

        // thanks xinyifly for finding out missing AP awards (AP Reset can be used as a compass)
        if (newJob.getId() % 100 >= 1) {
            if (this.isCygnus()) {
                gainAp(7, true);
            } else {
                if (GameConfig.getServerBoolean("use_starting_ap_4") || newJob.getId() % 10 >= 1) {
                    gainAp(5, true);
                }
            }
        } else {    // thanks Periwinks for noticing an AP shortage from lower levels
            if (GameConfig.getServerBoolean("use_starting_ap_4") && newJob.getId() % 1000 >= 1) {
                gainAp(4, true);
            }
        }

        if (!isGM()) {
            for (byte i = 1; i < 5; i++) {
                gainSlots(i, 4, true);
            }
        }

        int addhp = 0, addmp = 0;
        int job_ = job.getId() % 1000; // lame temp "fix"
        if (job_ == 100) {                      // 1st warrior
            addhp += Randomizer.rand(200, 250);
        } else if (job_ == 200) {               // 1st mage
            addmp += Randomizer.rand(100, 150);
        } else if (job_ % 100 == 0) {           // 1st others
            addhp += Randomizer.rand(100, 150);
            addmp += Randomizer.rand(25, 50);
        } else if (job_ > 0 && job_ < 200) {    // 2nd~4th warrior
            addhp += Randomizer.rand(300, 350);
        } else if (job_ < 300) {                // 2nd~4th mage
            addmp += Randomizer.rand(450, 500);
        } else {                  // 2nd~4th others
            addhp += Randomizer.rand(300, 350);
            addmp += Randomizer.rand(150, 200);
        }

        /*
        //aran perks?
        int newJobId = newJob.getId();
        if(newJobId == 2100) {          // become aran1
            addhp += 275;
            addmp += 15;
        } else if(newJobId == 2110) {   // become aran2
            addmp += 275;
        } else if(newJobId == 2111) {   // become aran3
            addhp += 275;
            addmp += 275;
        }
        */

        effLock.lock();
        statWlock.lock();
        try {
            addMaxMPMaxHP(addhp, addmp, true);
            recalcLocalStats();

            List<Pair<Stat, Integer>> statup = new ArrayList<>(7);
            statup.add(new Pair<>(Stat.HP, hp));
            statup.add(new Pair<>(Stat.MP, mp));
            statup.add(new Pair<>(Stat.MAXHP, clientMaxHp));
            statup.add(new Pair<>(Stat.MAXMP, clientMaxMp));
            statup.add(new Pair<>(Stat.AVAILABLEAP, remainingAp));
            statup.add(new Pair<>(Stat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job.getId())]));
            statup.add(new Pair<>(Stat.JOB, job.getId()));
            sendPacket(PacketCreator.updatePlayerStats(statup, true, this));
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }

        setMPC(new PartyCharacter(this));
        silentPartyUpdate();

        if (dragon != null) {
            getMap().broadcastMessage(PacketCreator.removeDragon(dragon.getObjectId()));
            dragon = null;
        }

        if (this.guildId > 0) {
            getGuild().broadcast(PacketCreator.jobMessage(0, job.getId(), name), this.getId());
        }
        Family family = getFamily();
        if (family != null) {
            family.broadcast(PacketCreator.jobMessage(1, job.getId(), name), this.getId());
        }
        setMasteries(this.job.getId());
        guildUpdate();

        broadcastChangeJob();

        if (GameConstants.hasSPTable(newJob) && newJob.getId() != 2001) {
            if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
                cancelBuffStats(BuffStat.MONSTER_RIDING);
            }
            createDragon();
        }

        if (GameConfig.getServerBoolean("use_announce_change_job")) {
            if (!this.isGM()) {
                broadcastAcquaintances(6, I18nUtil.getMessage("Character.Job.Change.message", getName(), GameConstants.ordinal(GameConstants.getJobBranch(newJob)), GameConstants.getJobName(this.job.getId())));        // thanks Vcoc for noticing job name appearing in uppercase here
            }
        }
    }

    public void broadcastAcquaintances(int type, String message) {
        broadcastAcquaintances(PacketCreator.serverNotice(type, message));
    }

    public void broadcastAcquaintances(Packet packet) {
        buddylist.broadcast(packet, getWorldServer().getPlayerStorage());
        Family family = getFamily();
        if (family != null) {
            family.broadcast(packet, id);
        }

        Guild guild = getGuild();
        if (guild != null) {
            guild.broadcast(packet, id);
        }

        /*
        if(partnerid > 0) {
            partner.sendPacket(packet); not yet implemented
        }
        */
        sendPacket(packet);
    }

    public void changeKeybinding(int key, KeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keymap.put(key, keybinding);
        } else {
            keymap.remove(key);
        }
        // [新增] 实时保存按键映射
        characterService.saveKeymap(id, keymap);
    }

    public void changeQuickslotKeybinding(byte[] aQuickslotKeyMapped) {
        this.quickSlotKeyMapped = new QuickslotBinding(aQuickslotKeyMapped);
    }

    public void broadcastStance(int newStance) {
        setStance(newStance);
        broadcastStance();
    }

    public void broadcastStance() {
        map.broadcastMessage(this, PacketCreator.movePlayer(id, this.getIdleMovement(), AbstractAnimatedMapObject.IDLE_MOVEMENT_PACKET_LENGTH), false);
    }

    public MapleMap getWarpMap(int map) {
        MapleMap warpMap;
        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else if (this.getMonsterCarnival() != null && this.getMonsterCarnival().getEventMap().getId() == map) {
            warpMap = this.getMonsterCarnival().getEventMap();
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }
        return warpMap;
    }

    // 仅用于OnUserEnter地图脚本中，当玩家在切换地图过程中需要改变地图时使用。
    public void warpAhead(int map) {
        newWarpMap = map;
    }

    private void eventChangedMap(int map) {
        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            eim.changedMap(this, map);
        }
    }

    private void eventAfterChangedMap(int map) {
        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            eim.afterChangedMap(this, map);
        }
    }

    public boolean canRecoverLastBanish() {
        return System.currentTimeMillis() - this.banishTime < MINUTES.toMillis(5);
    }

    public Pair<Integer, Integer> getLastBanishData() {
        return new Pair<>(this.banishMap, this.banishSp);
    }

    public void clearBanishPlayerData() {
        this.banishMap = -1;
        this.banishSp = -1;
        this.banishTime = 0;
    }

    public void setBanishPlayerData(int banishMap, int banishSp, long banishTime) {
        this.banishMap = banishMap;
        this.banishSp = banishSp;
        this.banishTime = banishTime;
    }

    public void changeMapBanish(int mapid, String portal, String msg) {
        if (GameConfig.getServerBoolean("use_spikes_avoid_banish")) {
            for (Item it : this.getInventory(InventoryType.EQUIPPED).list()) {
                if ((it.getFlag() & ItemConstants.SPIKES) == ItemConstants.SPIKES) {
                    return;
                }
            }
        }

        int banMap = this.getMapId();
        int banSp = this.getMap().findClosestPlayerSpawnpoint(this.getPosition()).getId();
        long banTime = System.currentTimeMillis();

        if (msg != null) {
            dropMessage(5, msg);
        }

        MapleMap map_ = getWarpMap(mapid);
        Portal portal_ = map_.getPortal(portal);
        changeMap(map_, portal_ != null ? portal_ : map_.getRandomPlayerSpawnpoint());

        setBanishPlayerData(banMap, banSp, banTime);
    }

    public void changeMap(int map) {
        changeMap(map, null);
    }


    /**
     * 玩家角色更改地图
     * @param map   地图ID
     */
    public void changeMap(int map, Object pt) {
        MapleMap warpMap;
        EventInstanceManager eim = getEventInstance();

        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = getMap(map, true);
            if (warpMap == null) return; //判断地图不存在则直接返回并发送提示消息。
        }

        Portal portal = switch (pt) {
            case null -> warpMap.getRandomPlayerSpawnpoint();
            case Integer i -> warpMap.getPortal(i);
            case String s -> warpMap.getPortal(s);
            case Portal p -> p;
            default -> warpMap.getPortal(0);
        };
        changeMap(warpMap, portal);
    }

    public void changeMap(MapleMap to) {
        changeMap(to, 0);
    }

    public void changeMap(MapleMap to, int portal) {
        changeMap(to, to.getPortal(portal));
    }

    public void changeMap(final MapleMap target, Portal pto) {
        canWarpCounter++;

        eventChangedMap(target.getId());    // player can be dropped from an event here, hence the new warping target.  //玩家可以从这里的事件中退出，因此成为新的扭曲目标。
        MapleMap to = getWarpMap(target.getId());
        if (pto == null) {
            pto = to.getPortal(0);
        }
        changeMapInternal(to, pto.getPosition(), PacketCreator.getWarpToMap(to, pto.getId(), this));
        canWarpMap = false;

        canWarpCounter--;
        if (canWarpCounter == 0) {
            canWarpMap = true;
        }

        eventAfterChangedMap(this.getMapId());
    }

    public void changeMap(final MapleMap target, final Point pos) {
        canWarpCounter++;

        eventChangedMap(target.getId());
        MapleMap to = getWarpMap(target.getId());
        changeMapInternal(to, pos, PacketCreator.getWarpToMap(to, 0x80, pos, this));
        canWarpMap = false;

        canWarpCounter--;
        if (canWarpCounter == 0) {
            canWarpMap = true;
        }

        eventAfterChangedMap(this.getMapId());
    }

    public void forceChangeMap(final MapleMap target, Portal pto) {
        // will actually enter the map given as parameter, regardless of being an eventmap or whatnot       //将实际输入作为参数给出的映射，无论是事件映射还是其他什么

        canWarpCounter++;
        eventChangedMap(MapId.NONE);

        EventInstanceManager mapEim = target.getEventInstance();
        if (mapEim != null) {
            EventInstanceManager playerEim = this.getEventInstance();
            if (playerEim != null) {
                playerEim.exitPlayer(this);
                if (playerEim.getPlayerCount() == 0) {
                    playerEim.dispose();
                }
            }

            // thanks Thora for finding an issue with players not being actually warped into the target event map (rather sent to the event starting map)
            //感谢Thora发现玩家实际上没有被扭曲到目标事件地图中（而是被发送到事件开始地图）的问题
            mapEim.registerPlayer(this, false);
        }

        if (pto == null) {
            pto = target.getPortal(0);
        }
        changeMapInternal(target, pto.getPosition(), PacketCreator.getWarpToMap(target, pto.getId(), this));
        canWarpMap = false;

        canWarpCounter--;
        if (canWarpCounter == 0) {
            canWarpMap = true;
        }

        eventAfterChangedMap(this.getMapId());
    }

    private boolean buffMapProtection() {
        int thisMapid = mapId;
        int returnMapid = client.getChannelServer().getMapFactory().getMap(thisMapid).getReturnMapId();

        effLock.lock();
        chrLock.lock();
        try {
            for (Entry<BuffStat, BuffStatValueHolder> mbs : effects.entrySet()) {
                if (mbs.getKey() == BuffStat.MAP_PROTECTION) {
                    byte value = (byte) mbs.getValue().value;

                    if (value == 1 && ((returnMapid == MapId.EL_NATH && thisMapid != MapId.ORBIS_TOWER_BOTTOM)
                            || returnMapid == MapId.INTERNET_CAFE)) {
                        return true;        //protection from cold
                    } else {
                        return value == 2 && (returnMapid == MapId.AQUARIUM || thisMapid == MapId.ORBIS_TOWER_BOTTOM);        //breathing underwater
                    }
                }
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }

        for (Item it : this.getInventory(InventoryType.EQUIPPED).list()) {
            if ((it.getFlag() & ItemConstants.COLD) == ItemConstants.COLD
                    && ((returnMapid == MapId.EL_NATH && thisMapid != MapId.ORBIS_TOWER_BOTTOM)
                    || returnMapid == MapId.INTERNET_CAFE)) {
                return true;        //protection from cold
            }
        }

        return false;
    }

    public List<Integer> getLastVisitedMapIds() {
        List<Integer> lastVisited = new ArrayList<>(5);

        petLock.lock();
        try {
            for (WeakReference<MapleMap> lv : lastVisitedMaps) {
                MapleMap lvm = lv.get();

                if (lvm != null) {
                    lastVisited.add(lvm.getId());
                }
            }
        } finally {
            petLock.unlock();
        }

        return lastVisited;
    }

    public void partyOperationUpdate(Party party, List<Character> exPartyMembers) {
        List<WeakReference<MapleMap>> mapIds;

        petLock.lock();
        try {
            mapIds = new LinkedList<>(lastVisitedMaps);
        } finally {
            petLock.unlock();
        }

        List<Character> partyMembers = new LinkedList<>();
        for (Character mc : (exPartyMembers != null) ? exPartyMembers : this.getPartyMembersOnline()) {
            if (mc.isLoggedInWorld()) {
                partyMembers.add(mc);
            }
        }

        Character partyLeaver = null;
        if (exPartyMembers != null) {
            partyMembers.remove(this);
            partyLeaver = this;
        }

        MapleMap map = this.getMap();
        List<MapItem> partyItems = null;

        int partyId = exPartyMembers != null ? -1 : this.getPartyId();
        for (WeakReference<MapleMap> mapRef : mapIds) {
            MapleMap mapObj = mapRef.get();

            if (mapObj != null) {
                List<MapItem> partyMapItems = mapObj.updatePlayerItemDropsToParty(partyId, id, partyMembers, partyLeaver);
                if (map.hashCode() == mapObj.hashCode()) {
                    partyItems = partyMapItems;
                }
            }
        }

        if (partyItems != null && exPartyMembers == null) {
            map.updatePartyItemDropsToNewcomer(this, partyItems);
        }

        updatePartyTownDoors(party, this, partyLeaver, partyMembers);
    }

    private static void addPartyPlayerDoor(Character target) {
        Door targetDoor = target.getPlayerDoor();
        if (targetDoor != null) {
            target.applyPartyDoor(targetDoor, true);
        }
    }

    private static void removePartyPlayerDoor(Party party, Character target) {
        target.removePartyDoor(party);
    }

    private static void updatePartyTownDoors(Party party, Character target, Character partyLeaver, List<Character> partyMembers) {
        if (partyLeaver != null) {
            removePartyPlayerDoor(party, target);
        } else {
            addPartyPlayerDoor(target);
        }

        Map<Integer, Door> partyDoors = null;
        if (!partyMembers.isEmpty()) {
            partyDoors = party.getDoors();

            for (Character pchr : partyMembers) {
                Door door = partyDoors.get(pchr.getId());
                if (door != null) {
                    door.updateDoorPortal(pchr);
                }
            }

            for (Door door : partyDoors.values()) {
                for (Character pchar : partyMembers) {
                    DoorObject mdo = door.getTownDoor();
                    mdo.sendDestroyData(pchar.getClient(), true);
                    pchar.removeVisibleMapObject(mdo);
                }
            }

            if (partyLeaver != null) {
                Collection<Door> leaverDoors = partyLeaver.getDoors();
                for (Door door : leaverDoors) {
                    for (Character pchar : partyMembers) {
                        DoorObject mdo = door.getTownDoor();
                        mdo.sendDestroyData(pchar.getClient(), true);
                        pchar.removeVisibleMapObject(mdo);
                    }
                }
            }

            List<Integer> histMembers = party.getMembersSortedByHistory();
            for (Integer chrid : histMembers) {
                Door door = partyDoors.get(chrid);

                if (door != null) {
                    for (Character pchar : partyMembers) {
                        DoorObject mdo = door.getTownDoor();
                        mdo.sendSpawnData(pchar.getClient());
                        pchar.addVisibleMapObject(mdo);
                    }
                }
            }
        }

        if (partyLeaver != null) {
            Collection<Door> leaverDoors = partyLeaver.getDoors();

            if (partyDoors != null) {
                for (Door door : partyDoors.values()) {
                    DoorObject mdo = door.getTownDoor();
                    mdo.sendDestroyData(partyLeaver.getClient(), true);
                    partyLeaver.removeVisibleMapObject(mdo);
                }
            }

            for (Door door : leaverDoors) {
                DoorObject mdo = door.getTownDoor();
                mdo.sendDestroyData(partyLeaver.getClient(), true);
                partyLeaver.removeVisibleMapObject(mdo);
            }

            for (Door door : leaverDoors) {
                door.updateDoorPortal(partyLeaver);

                DoorObject mdo = door.getTownDoor();
                mdo.sendSpawnData(partyLeaver.getClient());
                partyLeaver.addVisibleMapObject(mdo);
            }
        }
    }

    private Integer getVisitedMapIndex(MapleMap map) {
        int idx = 0;
        for (WeakReference<MapleMap> mapRef : lastVisitedMaps) {
            if (map.equals(mapRef.get())) {
                return idx;
            }
            idx++;
        }
        return -1;
    }

    public void visitMap(MapleMap map) {
        petLock.lock();
        try {
            int idx = getVisitedMapIndex(map);

            if (idx == -1) {
                if (lastVisitedMaps.size() == GameConfig.getServerInt("map_visited_size")) {
                    lastVisitedMaps.removeFirst();
                }
            } else {
                WeakReference<MapleMap> mapRef = lastVisitedMaps.remove(idx);
                lastVisitedMaps.add(mapRef);
                return;
            }

            lastVisitedMaps.add(new WeakReference<>(map));
        } finally {
            petLock.unlock();
        }
    }

    public void setOwnedMap(MapleMap map) {
        ownedMap = new WeakReference<>(map);
    }

    public MapleMap getOwnedMap() {
        return ownedMap.get();
    }

    public void notifyMapTransferToPartner(int mapid) {
        if (partnerId > 0) {
            final Character partner = getWorldServer().getPlayerStorage().getCharacterById(partnerId);
            if (partner != null && !partner.isAwayFromWorld()) {
                partner.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(id, mapid));
            }
        }
    }

    public void removeIncomingInvites() {
        InviteCoordinator.removePlayerIncomingInvites(id);
    }

    /**
     * 玩家更改地图 内部方法
     * @param to 目标地图对象
     * @param pos 目标位置坐标
     * @param warpPacket 传送数据包
     */
    private void changeMapInternal(MapleMap to, Point pos, Packet warpPacket) {
        if (!canWarpMap) {
            return;
        }

        // ===== 监狱传送检查 =====
        if (isJailed()) {
            // 条件①：服刑期间不在监狱 → 强制改目标为监狱地图
            if (getMapId() != MapId.JAIL) {
                MapleMap jailMap = getMap(MapId.JAIL, true);
                if (jailMap == null) return;
                to = jailMap;
                pos = jailMap.getPortal(0).getPosition();
                warpPacket = PacketCreator.getWarpToMap(to, 0, pos, this);
            }
            // 条件②：在监狱试图越狱 → 加刑10分钟 + 拦截
            else if (to.getId() != MapId.JAIL) {
                addJailDuration(TimeUnit.MINUTES.toMillis(10));
                refreshJailDisplay();
                return;
            }
        }

        if (getMap(to.getId(), true) == null) return; //判断地图不存在则直接返回并发送提示消息。

        this.mapTransitioning.set(true);

        this.unregisterChairBuff();
        this.clearBanishPlayerData();
        Trade.cancelTrade(this, Trade.TradeResult.UNSUCCESSFUL_ANOTHER_MAP);
        this.closePlayerInteractions();

        Party e = null;
        if (this.getParty() != null && this.getParty().getEnemy() != null) {
            e = this.getParty().getEnemy();
        }
        final Party k = e;

        sendPacket(warpPacket);
        map.removePlayer(this);
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            map = to;
            setPosition(pos);
            map.addPlayer(this);
            visitMap(map);

            prtLock.lock();
            try {
                if (party != null) {
                    mpc.setMapId(to.getId());
                    sendPacket(PacketCreator.updateParty(client.getChannel(), party, PartyOperation.SILENT_UPDATE, null));
                    updatePartyMemberHPInternal();
                }
            } finally {
                prtLock.unlock();
            }
            if (Character.this.getParty() != null) {
                Character.this.getParty().setEnemy(k);
            }
            silentPartyUpdateInternal(getParty());  // EIM script calls inside
        } else {    //切换地图时卡住了
            log.warn(I18nUtil.getLogMessage("Character.Map.Change.warn2"), getName(), map.getMapName(), map.getId());
            client.disconnect(true, false);     // thanks BHB for noticing a player storage stuck case here
            return;
        }

        notifyMapTransferToPartner(map.getId());

        //alas, new map has been specified when a warping was being processed...
        if (newWarpMap != -1) {
            canWarpMap = true;

            int temp = newWarpMap;
            newWarpMap = -1;
            changeMap(temp);
        } else {
            // if this event map has a gate already opened, render it
            EventInstanceManager eim = getEventInstance();
            if (eim != null) {
                eim.recoverOpenedGate(this, map.getId());
            }

            // if this map has obstacle components moving, make it do so for this client
            sendPacket(PacketCreator.environmentMoveList(map.getEnvironment().entrySet()));
        }
    }

    /**
     * 玩家角色是否处于切换地图的状态
     * @return boolean
     */
    public boolean isChangingMaps() {
        return this.mapTransitioning.get();
    }

    /**
     *  设置地图转换完成
     */
    public void setMapTransitionComplete() {
        this.mapTransitioning.set(false);
    }

    public void changePage(int page) {
        this.currentPage = page;
    }

    public void changeSkillLevel(Skill skill, byte newLevel, int newMasterlevel, long expiration) {
        if (newLevel > -1) {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            if (!GameConstants.isHiddenSkills(skill.getId())) {
                sendPacket(PacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, expiration));
            }
        } else {
            skills.remove(skill);
            sendPacket(PacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, -1)); //Shouldn't use expiration anymore :)
            characterService.removeSkill(SkillsDO.builder().skillid(skill.getId()).characterid(getId()).build());
        }
        // [新增] 实时保存技能 (增量)
        if (newLevel > -1) {
            Map<Skill, SkillEntry> singleSkill = new HashMap<>();
            singleSkill.put(skill, skills.get(skill));
            characterService.saveSkills(id, singleSkill,false);
        }
    }

    public void changeTab(int tab) {
        this.currentTab = tab;
    }

    public void changeType(int type) {
        this.currentType = type;
    }

    public void checkBerserk(final boolean isHidden) {
        if (berserkSchedule != null) {
            berserkSchedule.cancel(false);
        }
        final Character chr = this;
        if (job.equals(Job.DARKKNIGHT)) {
            Skill BerserkX = SkillFactory.getSkill(DarkKnight.BERSERK);
            final int skilllevel = getSkillLevel(BerserkX);
            if (skilllevel > 0) {
                berserk = chr.getHp() * 100 / chr.getCurrentMaxHp() < BerserkX.getEffect(skilllevel).getX();
                berserkSchedule = TimerManager.getInstance().register(() -> {
                    if (awayFromWorld.get()) {
                        return;
                    }

                    sendPacket(PacketCreator.showOwnBerserk(skilllevel, berserk));
                    if (!isHidden) {
                        getMap().broadcastMessage(Character.this, PacketCreator.showBerserk(getId(), skilllevel, berserk), false);
                    } else {
                        getMap().broadcastGMMessage(Character.this, PacketCreator.showBerserk(getId(), skilllevel, berserk), false);
                    }
                }, 5000, 3000);
            }
        }
    }

    public void checkMessenger() {
        if (messenger != null && messengerPosition < 4 && messengerPosition > -1) {
            World worldz = getWorldServer();
            worldz.silentJoinMessenger(messenger.getId(), new MessengerCharacter(this, messengerPosition), messengerPosition);
            worldz.updateMessenger(getMessenger().getId(), name, client.getChannel());
        }
    }

    public void controlMonster(Monster monster) {
        if (cpnLock.tryLock()) {
            try {
                controlled.add(monster);
            } finally {
                cpnLock.unlock();
            }
        }
    }

    public void stopControllingMonster(Monster monster) {
        if (cpnLock.tryLock()) {
            try {
                controlled.remove(monster);
            } finally {
                cpnLock.unlock();
            }
        }
    }

    public int getNumControlledMonsters() {
        cpnLock.lock();
        try {
            return controlled.size();
        } finally {
            cpnLock.unlock();
        }
    }

    public Collection<Monster> getControlledMonsters() {
        cpnLock.lock();
        try {
            return new ArrayList<>(controlled);
        } finally {
            cpnLock.unlock();
        }
    }

    public void releaseControlledMonsters() {
        Collection<Monster> controlledMonsters;

        cpnLock.lock();
        try {
            controlledMonsters = new ArrayList<>(controlled);
            controlled.clear();
        } finally {
            cpnLock.unlock();
        }

        for (Monster monster : controlledMonsters) {
            monster.aggroRedirectController();
        }
    }

    /**
     * 处理拾取后立即消耗的道具逻辑
     *
     * @param itemId 物品的唯一标识ID，应符合游戏物品ID规范（消耗品类ID以2开头）
     * @return boolean 消耗是否成功应用：
     *                 - true: 道具效果已应用/处理完成
     *                 - false: 非消耗品或无需立即使用
     * @throws NullPointerException 如果无法获取物品信息或效果对象可能抛出
     *
     * @description
     * 实现以下核心逻辑：
     * 1. 验证物品是否为可消耗类型（ID首数字为2）
     * 2. 检查物品的"拾取即用"标记
     * 3. 处理队伍道具的特殊场景：
     *    - 普通队伍道具：对同地图存活队友应用效果
     *    - 全体治疗道具：解除队友异常状态
     * 4. 处理怪物卡片收集（ID 238xxxx类型）
     *
     * @example
     * // 典型使用场景
     * if(applyConsumeOnPickup(2001000)) {
     *     removeFromInventory(item); // 消耗后移除物品
     * }
     *
     * @note
     * - 物品ID格式约定：
     *   - 第1位：物品大类（2=消耗品）
     *   - 第2-4位：物品子类（238=怪物卡片）
     * - 队伍道具效果只会影响同地图的存活队友
     */
    public boolean applyConsumeOnPickup(final int itemId) {// 判断拾取后是否立即消耗道具的方法
        if (itemId / 1000000 != 2) {// 检查物品ID是否属于消耗品类（假设ID以2开头）
            return false; // 非消耗品直接返回不处理
        }
        ItemInformationProvider ii = ItemInformationProvider.getInstance();// 获取物品信息提供器实例
        if (!ii.isConsumeOnPickup(itemId)) {// 检查该物品是否标记为"拾取后立即使用"
            return false; // 无需立即使用则返回
        }
        if (ItemConstants.isPartyItem(itemId)) {// 判断是否为队伍共享类道具
            List<Character> partyMembers = this.getPartyMembersOnSameMap();// 获取同一地图内的队伍成员列表
            if (!ItemId.isPartyAllCure(itemId)) {// 处理非全体治疗类道具
                StatEffect mse = ii.getItemEffect(itemId);// 获取道具效果对象
                if (!partyMembers.isEmpty()) {
                    for (Character mc : partyMembers) {// 遍历存活队友并施加效果
                        if (mc.isAlive()) {
                            mse.applyTo(mc);
                        }
                    }
                } else if (this.isAlive()) {
                    mse.applyTo(this);// 无队友时对自己生效
                }
            } else {
                if (!partyMembers.isEmpty()) {// 处理全体治疗类道具（如解除异常状态）
                    for (Character mc : partyMembers) {
                        mc.dispelDebuffs(); // 解除队友debuff
                    }
                } else {
                    this.dispelDebuffs(); // 无队友时解除自身
                }
            }
        } else {
            ii.getItemEffect(itemId).applyTo(this);// 非队伍道具直接对自身生效
        }

        if (itemId / 10000 == 238) {// 特殊处理怪物卡片收集（ID格式238xxxx）
            this.getMonsterBook().addCard(client, itemId); // 添加到怪物图鉴
        }
        return true; // 成功执行消耗操作
    }

    public final void pickupItem(MapObject ob) {
        pickupItem(ob, -1);
    }

    public final void pickupItem(MapObject ob, int petIndex) {
        pickupItem(ob,petIndex,true);
    }
    public final void pickupItem(MapObject ob, int petIndex,boolean showItemUnavailable) {     // 定义拾取物品方法，参数为地图对象和宠物索引
        if (ob == null) {                                               // 检查对象是否为null
            return;                                                     // 如果为null则直接返回
        }

        if (ob instanceof MapItem mapitem) {                            // 检查对象是否为MapItem类型
            if (System.currentTimeMillis() - mapitem.getDropTime() < 400 || !mapitem.canBePickedBy(this)) { // 检查物品是否刚掉落或不可拾取
                enableActions();                                        // 启用玩家动作
                return;                                                 // 返回
            }

            List<Character> mpcs = new LinkedList<>();                  // 创建队伍成员列表
            if (mapitem.getMeso() > 0 && !mapitem.isPickedUp()) {       // 如果是金币且未被拾取
                mpcs = getPartyMembersOnSameMap();                      // 获取同地图队伍成员
            }

            ScriptedItem itemScript = null;                             // 初始化脚本物品变量
            mapitem.lockItem();                                         // 锁定物品防止并发拾取
            try {
                if (mapitem.isPickedUp()) {                             // 再次检查物品是否已被拾取
                    sendPacket(PacketCreator.showItemUnavailable());    // 发送物品不可用包
                    enableActions();                                    // 启用玩家动作
                    return;                                             // 返回
                }

                boolean isPet = petIndex > -1;                          // 判断是否是宠物拾取
                final Packet pickupPacket = PacketCreator.removeItemFromMap(mapitem.getObjectId(), (isPet) ? 5 : 2, this.getId(), isPet, petIndex); // 创建拾取数据包

                Item mItem = mapitem.getItem();                         // 获取物品对象
                boolean hasSpaceInventory = true;                       // 初始化背包空间标志
                ItemInformationProvider ii = ItemInformationProvider.getInstance(); // 获取物品信息提供者
                if (ItemId.isNxCard(mapitem.getItemId()) || mapitem.getMeso() > 0 || ii.isConsumeOnPickup(mapitem.getItemId()) || (hasSpaceInventory = InventoryManipulator.checkSpace(client, mapitem.getItemId(), mItem.getQuantity(), mItem.getOwner()))) { // 检查物品类型和背包空间
                    int mapId = this.getMapId();                        // 获取当前地图ID

                    if ((MapId.isSelfLootableOnly(mapId))) {//happyville trees and guild PQ // 检查是否是特殊地图
                        if (!mapitem.isPlayerDrop() || mapitem.getDropper().getObjectId() == client.getPlayer().getObjectId()) { // 检查是否可以拾取
                            if (mapitem.getMeso() > 0) {                // 处理金币拾取
                                if (!mpcs.isEmpty()) {                  // 如果有队友
                                    int mesosamm = mapitem.getMeso() / mpcs.size(); // 计算每人分得金币
                                    for (Character partymem : mpcs) {   // 遍历队友
                                        if (partymem.isLoggedInWorld()) { // 检查是否在线
                                            partymem.gainMeso(mesosamm, true, true, false); // 分配金币
                                            traceabilityService.log(null, partymem, TraceabilityService.ActionType.INVENTORY, TraceabilityService.ActionSourceType.PLAYER_PICKUP, mapitem.getMeso(), String.format("[%d] %s 拾取 %d",this.getId(),this.getName(),mapitem.getMeso()), "金币-分配(特殊地图)");
                                         }
                                    }
                                } else {
                                    this.gainMeso(mapitem.getMeso(), true, true, false); // 自己获得全部金币
                                    traceabilityService.log(null, this, TraceabilityService.ActionType.INVENTORY, TraceabilityService.ActionSourceType.PLAYER_PICKUP, mapitem.getMeso(), null, "金币(特殊地图)");
                                }

                                this.getMap().pickItemDrop(pickupPacket, mapitem); // 从地图移除物品
                            } else if (ItemId.isNxCard(mapitem.getItemId())) { // 处理NX点券卡
                                // Add NX to account, show effect and make item disappear   //添加点券到账户，是否展示捡到点券，并移除物品
                                int nxGain = (mapitem.getItemId() == ItemId.NX_CARD_100 ? 100 : 250) * mItem.getQuantity(); //计算点券数量
                                this.getCashShop().gainCash(CashShop.NX_CREDIT, nxGain); // 增加点券

                                if (GameConfig.getServerBoolean("use_announce_nx_coupon_loot")) { // 检查是否广播
                                    showHint(I18nUtil.getMessage("Character.pickupItem.message1", nxGain, this.getCashShop().getCash(CashShop.NX_CREDIT)), 300); // 显示提示
                                    //showHint("捡到 #e#b" + nxGain + " NX#k#n (" + this.getCashShop().getCash(CashShop.NX_CREDIT) + " NX)", 300);
                                }

                                this.getMap().pickItemDrop(pickupPacket, mapitem); // 从地图移除物品
                                traceabilityService.log(mItem, this, TraceabilityService.ActionType.INVENTORY, TraceabilityService.ActionSourceType.PLAYER_PICKUP, mItem.getQuantity(), null, "NX卡(特殊地图)");
                            } else if (InventoryManipulator.addFromDrop(client, mItem, true)) { // 尝试添加物品到背包
                                this.getMap().pickItemDrop(pickupPacket, mapitem); // 从地图移除物品
                                itemRecoveryaservic.processItemPickup(mItem.getUid());
                                traceabilityService.log(mItem, this, TraceabilityService.ActionType.INVENTORY, TraceabilityService.ActionSourceType.PLAYER_PICKUP, mItem.getQuantity(), null, "物品(特殊地图)");
                            } else {
                                enableActions();                        // 启用玩家动作
                                return;                                 // 返回
                            }
                        } else {
                            sendPacket(PacketCreator.showItemUnavailable()); // 发送物品不可用包
                            enableActions();                            // 启用玩家动作
                            return;                                     // 返回
                        }
                        enableActions();                                // 启用玩家动作
                        return;                                         // 返回
                    }

                    if (!this.needQuestItem(mapitem.getQuest(), mapitem.getItemId())) { // 检查是否需要任务物品
                        if (showItemUnavailable) sendPacket(PacketCreator.showItemUnavailable()); // 发送物品不可用包
                        enableActions();                                // 启用玩家动作
                        return;                                         // 返回
                    }

                    if (mapitem.getMeso() > 0) {                        // 处理金币拾取(普通地图)
                        if (!mpcs.isEmpty()) {                          // 如果有队友
                            int mesosamm = mapitem.getMeso() / mpcs.size(); // 计算每人分得金币
                            for (Character partymem : mpcs) {           // 遍历队友
                                if (partymem.isLoggedInWorld()) {       // 检查是否在线
                                    partymem.gainMeso(mesosamm, true, true, false); // 分配金币
                                    traceabilityService.log(null, partymem, TraceabilityService.ActionType.INVENTORY, TraceabilityService.ActionSourceType.PLAYER_PICKUP, mapitem.getMeso(), String.format("[%d] %s 拾取 %d",this.getId(),this.getName(),mapitem.getMeso()), "金币-分配");
                                }
                            }
                        } else {
                            this.gainMeso(mapitem.getMeso(), true, true, false); // 自己获得全部金币
                            traceabilityService.log(null, this, TraceabilityService.ActionType.INVENTORY, TraceabilityService.ActionSourceType.PLAYER_PICKUP, mapitem.getMeso(), null, "金币");
                        }
                    } else if (mItem.getItemId() / 10000 == 243) {      // 处理脚本物品
                        ScriptedItem info = ii.getScriptedItemInfo(mItem.getItemId()); // 获取脚本信息
                        if (info != null && info.runOnPickup()) {       // 检查是否需要执行脚本
                            itemScript = info;                          // 设置脚本物品
                        } else {
                            if (!InventoryManipulator.addFromDrop(client, mItem, true)) { // 尝试添加物品到背包
                                enableActions();                        // 启用玩家动作
                                return;                                 // 返回
                            }
                        }
                    } else if (ItemId.isNxCard(mapitem.getItemId())) {  // 再次检查NX点券卡(普通地图)
                        // Add NX to account, show effect and make item disappear
                        int nxGain = (mapitem.getItemId() == ItemId.NX_CARD_100 ? 100 : 250) * mItem.getQuantity(); //计算点券数量
                        this.getCashShop().gainCash(CashShop.NX_CREDIT, nxGain); // 增加点券
                        traceabilityService.log(mItem, this, TraceabilityService.ActionType.INVENTORY, TraceabilityService.ActionSourceType.PLAYER_PICKUP, mItem.getQuantity(), null, "NX卡");
                        if (GameConfig.getServerBoolean("use_announce_nx_coupon_loot")) { // 检查是否广播
                            showHint(I18nUtil.getMessage("Character.pickupItem.message1", nxGain, this.getCashShop().getCash(CashShop.NX_CREDIT)), 300); // 显示提示
                            //showHint("捡到 #e#b" + nxGain + " NX#k#n (" + this.getCashShop().getCash(CashShop.NX_CREDIT) + " NX)", 300);
                        }
                    } else if (applyConsumeOnPickup(mItem.getItemId())) {//此段判断为处理捡取治疗道具和怪物卡加入图鉴
                    } else if (InventoryManipulator.addFromDrop(client, mItem, true)) { // 尝试添加普通物品到背包
                        itemRecoveryaservic.processItemPickup(mItem.getUid());
                        traceabilityService.log(mItem, this, TraceabilityService.ActionType.INVENTORY, TraceabilityService.ActionSourceType.PLAYER_PICKUP, mItem.getQuantity(), null, null);
                        if (mItem.getItemId() == ItemId.ARPQ_SPIRIT_JEWEL) { // 检查是否是特殊物品
                            updateAriantScore();                        // 更新分数
                        }
                    } else {
                        enableActions();                                // 启用玩家动作
                        return;                                         // 返回
                    }

                    this.getMap().pickItemDrop(pickupPacket, mapitem);  // 从地图移除物品
                } else if (!hasSpaceInventory && showItemUnavailable) {                        // 检查背包空间
                    sendPacket(PacketCreator.getInventoryFull());       // 发送背包已满包
                    sendPacket(PacketCreator.getShowInventoryFull());   // 显示背包已满提示
                }
            } finally {
                mapitem.unlockItem();                                   // 解锁物品
            }

            if (itemScript != null) {                                   // 检查是否有脚本需要执行
                ItemScriptManager ism = ItemScriptManager.getInstance(); // 获取脚本管理器
                ism.runItemScript(client, itemScript);                  // 执行物品脚本
            }
        }
        enableActions();                                                // 最后启用玩家动作
    }

    public int countItem(int itemid) {
        return inventory[ItemConstants.getInventoryType(itemid).ordinal()].countById(itemid);
    }

    public boolean canHold(int itemid) {
        return canHold(itemid, 1);
    }

    public boolean canHold(int itemid, int quantity) {
        return client.getAbstractPlayerInteraction().canHold(itemid, quantity);
    }

    public boolean canHoldUniques(List<Integer> itemids) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Integer itemid : itemids) {
            if (ii.isPickupRestricted(itemid) && this.haveItem(itemid)) {
                return false;
            }
        }

        return true;
    }

    public boolean isRidingBattleship() {
        Integer bv = getBuffedValue(BuffStat.MONSTER_RIDING);
        return bv != null && bv.equals(Corsair.BATTLE_SHIP);
    }

    public void announceBattleshipHp() {
        sendPacket(PacketCreator.skillCooldown(5221999, battleshipHp));
    }

    public void decreaseBattleshipHp(int decrease) {
        this.battleshipHp -= decrease;
        if (battleshipHp <= 0) {
            Skill battleship = SkillFactory.getSkill(Corsair.BATTLE_SHIP);
            int cooldown = battleship.getEffect(getSkillLevel(battleship)).getCooldown();
            sendPacket(PacketCreator.skillCooldown(Corsair.BATTLE_SHIP, cooldown));
            addCooldown(Corsair.BATTLE_SHIP, Server.getInstance().getCurrentTime(), SECONDS.toMillis(cooldown));
            removeCooldown(5221999);
            cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
        } else {
            announceBattleshipHp();
            addCooldown(5221999, 0, Long.MAX_VALUE);
        }
    }

    public void decreaseReports() {
        this.possibleReports--;
    }

    public void deleteGuild(int guildId) {
        characterService.deleteGuild(GuildsDO.builder().guildid((long) guildId).build());
    }

    private void nextPendingRequest(Client c) {
        CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.sendPacket(PacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), c.getPlayer().getId(), pendingBuddyRequest.getName()));
        }
    }

    private void notifyRemoteChannel(Client c, int remoteChannel, int otherCid, BuddyList.BuddyOperation operation) {
        Character player = c.getPlayer();
        if (remoteChannel != -1) {
            c.getWorldServer().buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation);
        }
    }

    public void deleteBuddy(int otherCid) {
        BuddyList bl = getBuddylist();

        if (bl.containsVisible(otherCid)) {
            notifyRemoteChannel(client, getWorldServer().find(otherCid), otherCid, BuddyList.BuddyOperation.DELETED);
        }
        bl.remove(otherCid);
        sendPacket(PacketCreator.updateBuddylist(getBuddylist().getBuddies()));
        // [新增] 实时保存好友列表
        characterService.saveBuddies(id, buddylist);
        nextPendingRequest(client);
    }

    public static boolean deleteCharFromDB(Character player, int senderAccId) {
        try {
            characterService.deleteCharFromDB(player, senderAccId);
            Server.getInstance().deleteCharacterEntry(senderAccId, player.getId());// 删除缓存,防止角色槽满后无法再次建立角色
            return true;
        } catch (Exception e) {
            log.error(I18nUtil.getLogMessage("Character.deleteCharFromDB.error1"), e);
        }
        return false;
    }

    private static void deleteQuestProgressWhereCharacterId(int cid) {
        questService.deleteQuestProgressByCharacter(cid);
    }

    private void stopChairTask() {
        chrLock.lock();
        try {
            if (chairRecoveryTask != null) {
                chairRecoveryTask.cancel(false);
                chairRecoveryTask = null;
            }
        } finally {
            chrLock.unlock();
        }
    }

    private static Pair<Integer, Pair<Integer, Integer>> getChairTaskIntervalRate(int maxhp, int maxmp) {
        /*
        此处2个参数CHAIR_EXTRA_HEAL_MULTIPLIER和CHAIR_EXTRA_HEAL_MAX_DELAY已被我删除
        1.在倍率固定的情况下，既不希望定时任务执行太快，又不希望定时任务执行太慢
        2.在固定最大时间的情况下，既不希望恢复量太大，又不希望恢复量太小
        3.关键是这2个参数又都能配置，在某些场景下，就会打破上述他自己设置的限制
        4.所以，这个参数需要个人进行复杂的计算才能配置，不能乱配，但他又放开让你都允许配置
        5.综上，这种属于既要又要，什么都要只会害了你，所以我把这2个参数都干掉了
        6.如果确实要放开允许配置，最多把CHAIR_EXTRA_HEAL_MAX_DELAY放开即可，这个参数还算有点意义，但也需要简单计算一下得到他合适的值
         */
        float toHeal = Math.max(maxhp, maxmp);
        float maxDuration = SECONDS.toMillis(21);

        int rate = 0;
        int minRegen = 1, maxRegen = 2559, midRegen = 1;
        while (minRegen < maxRegen) {
            midRegen = (int) ((minRegen + maxRegen) * 0.94);

            float procs = toHeal / midRegen;
            float newRate = maxDuration / procs;
            rate = (int) newRate;

            if (newRate < 420) {
                minRegen = (int) (1.2 * midRegen);
            } else if (newRate > 5000) {
                maxRegen = (int) (0.8 * midRegen);
            } else {
                break;
            }
        }

        float procs = maxDuration / rate;
        int hpRegen, mpRegen;
        if (maxhp > maxmp) {
            hpRegen = midRegen;
            mpRegen = (int) Math.ceil(maxmp / procs);
        } else {
            hpRegen = (int) Math.ceil(maxhp / procs);
            mpRegen = midRegen;
        }

        return new Pair<>(rate, new Pair<>(hpRegen, mpRegen));
    }

    private void updateChairHealStats() {
        statRlock.lock();
        try {
            if (localchairrate != -1) {
                return;
            }
        } finally {
            statRlock.unlock();
        }

        effLock.lock();
        statWlock.lock();
        try {
            Pair<Integer, Pair<Integer, Integer>> p = getChairTaskIntervalRate(localMaxHp, localMaxMp);

            localchairrate = p.getLeft();
            localchairhp = p.getRight().getLeft();
            localchairmp = p.getRight().getRight();
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }
    }

    private void startChairTask() {
        if (chair.get() < 0) {
            return;
        }

        int healInterval;
        effLock.lock();
        try {
            updateChairHealStats();
            healInterval = localchairrate;
        } finally {
            effLock.unlock();
        }

        chrLock.lock();
        try {
            if (chairRecoveryTask != null) {
                stopChairTask();
            }

            chairRecoveryTask = TimerManager.getInstance().register(() -> {
                updateChairHealStats();
                final int healHP = localchairhp;
                final int healMP = localchairmp;

                if (Character.this.getHp() < localMaxHp) {
                    byte recHP = (byte) (healHP / 10);

                    sendPacket(PacketCreator.showOwnRecovery(recHP));
                    getMap().broadcastMessage(Character.this, PacketCreator.showRecovery(id, recHP), false);
                } else if (Character.this.getMp() >= localMaxMp) {
                    stopChairTask();    // optimizing schedule management when player is already with full pool.
                }

                addMPHP(healHP, healMP);
            }, healInterval, healInterval);
        } finally {
            chrLock.unlock();
        }
    }

    /**
     * 启动自动吸物定时器
     */
    public void startCheatItemVac() {
        if (cheatManager == null) return;
        ItemVacPlugin itemVacPlugin = cheatManager.getPlugin("ItemVac");
        if (itemVacPlugin != null) {
            itemVacPlugin.startCheatItemVac();
        }
    }

    /**
     * 停止自动吸物定时器
     */
    public void stopCheatItemVac() {
        if (cheatManager == null) return;
        ItemVacPlugin itemVacPlugin = cheatManager.getPlugin("ItemVac");
        if (itemVacPlugin != null) {
            itemVacPlugin.stop();
        }
    }

    // 修复遗留的不完整代码块
    private void stopExtraTask() {
        chrLock.lock();
        try {
            if (extraRecoveryTask != null) {
                extraRecoveryTask.cancel(false);
                extraRecoveryTask = null;
            }
        } finally {
            chrLock.unlock();
        }
    }

    private void startExtraTask(final byte healHP, final byte healMP, final short healInterval) {
        chrLock.lock();
        try {
            startExtraTaskInternal(healHP, healMP, healInterval);
        } finally {
            chrLock.unlock();
        }
    }

    private void startExtraTaskInternal(final byte healHP, final byte healMP, final short healInterval) {
        extraRecInterval = healInterval;

        extraRecoveryTask = TimerManager.getInstance().register(() -> {
            if (getBuffSource(BuffStat.HPREC) == -1 && getBuffSource(BuffStat.MPREC) == -1) {
                stopExtraTask();
                return;
            }

            if (Character.this.getHp() < localMaxHp) {
                if (healHP > 0) {
                    sendPacket(PacketCreator.showOwnRecovery(healHP));
                    getMap().broadcastMessage(Character.this, PacketCreator.showRecovery(id, healHP), false);
                }
            }

            addMPHP(healHP, healMP);
        }, healInterval, healInterval);
    }

    public void disbandGuild() {
        if (guildId < 1 || guildRank != 1) {
            return;
        }
        try {
            Server.getInstance().disbandGuild(guildId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispel() {
        if (!(GameConfig.getServerBoolean("use_undispel_holy_shield") && this.hasActiveBuff(Bishop.HOLY_SHIELD))) {
            List<BuffStatValueHolder> mbsvhList = getAllStatups();
            for (BuffStatValueHolder mbsvh : mbsvhList) {
                if (mbsvh.effect.isSkill()) {
                    if (mbsvh.effect.getBuffSourceId() != Aran.COMBO_ABILITY) { // check discovered thanks to Croosade dev team
                        cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                    }
                }
            }
        }
    }

    public final boolean hasDisease(final Disease dis) {
        chrLock.lock();
        try {
            return diseases.containsKey(dis);
        } finally {
            chrLock.unlock();
        }
    }

    public final int getDiseasesSize() {
        chrLock.lock();
        try {
            return diseases.size();
        } finally {
            chrLock.unlock();
        }
    }

    public Map<Disease, Pair<Long, MobSkill>> getAllDiseases() {
        chrLock.lock();
        try {
            long curtime = Server.getInstance().getCurrentTime();
            Map<Disease, Pair<Long, MobSkill>> ret = new LinkedHashMap<>();

            for (Entry<Disease, Long> de : diseaseExpires.entrySet()) {
                Pair<DiseaseValueHolder, MobSkill> dee = diseases.get(de.getKey());
                DiseaseValueHolder mdvh = dee.getLeft();

                ret.put(de.getKey(), new Pair<>(mdvh.length - (curtime - mdvh.startTime), dee.getRight()));
            }

            return ret;
        } finally {
            chrLock.unlock();
        }
    }

    public void silentApplyDiseases(Map<Disease, Pair<Long, MobSkill>> diseaseMap) {
        chrLock.lock();
        try {
            long curTime = Server.getInstance().getCurrentTime();

            for (Entry<Disease, Pair<Long, MobSkill>> di : diseaseMap.entrySet()) {
                long expTime = curTime + di.getValue().getLeft();

                diseaseExpires.put(di.getKey(), expTime);
                diseases.put(di.getKey(), new Pair<>(new DiseaseValueHolder(curTime, di.getValue().getLeft()), di.getValue().getRight()));
            }
        } finally {
            chrLock.unlock();
        }
    }

    public void announceDiseases() {
        Set<Entry<Disease, Pair<DiseaseValueHolder, MobSkill>>> chrDiseases;

        chrLock.lock();
        try {
            // Poison damage visibility and diseases status visibility, extended through map transitions thanks to Ronan
            if (!this.isLoggedInWorld()) {
                return;
            }

            chrDiseases = new LinkedHashSet<>(diseases.entrySet());
        } finally {
            chrLock.unlock();
        }

        for (Entry<Disease, Pair<DiseaseValueHolder, MobSkill>> di : chrDiseases) {
            Disease disease = di.getKey();
            MobSkill skill = di.getValue().getRight();
            final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, skill.getX()));

            if (disease != Disease.SLOW) {
                map.broadcastMessage(PacketCreator.giveForeignDebuff(id, debuff, skill));
            } else {
                map.broadcastMessage(PacketCreator.giveForeignSlowDebuff(id, debuff, skill));
            }
        }
    }

    public void collectDiseases() {
        for (Character chr : map.getAllPlayers()) {
            int cid = chr.getId();

            for (Entry<Disease, Pair<Long, MobSkill>> di : chr.getAllDiseases().entrySet()) {
                Disease disease = di.getKey();
                MobSkill skill = di.getValue().getRight();
                final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, skill.getX()));

                if (disease != Disease.SLOW) {
                    this.sendPacket(PacketCreator.giveForeignDebuff(cid, debuff, skill));
                } else {
                    this.sendPacket(PacketCreator.giveForeignSlowDebuff(cid, debuff, skill));
                }
            }
        }
    }

    public void giveDebuff(final Disease disease, MobSkill skill, int time) {
        if (!hasDisease(disease) && getDiseasesSize() < 2) {
            if (!(disease == Disease.SEDUCE || disease == Disease.STUN)) {
                if (hasActiveBuff(Bishop.HOLY_SHIELD)) {
                    return;
                }
            }

            chrLock.lock();
            try {
                long curTime = Server.getInstance().getCurrentTime();
                diseaseExpires.put(disease, curTime + MILLISECONDS.toMillis(time));
                diseases.put(disease, new Pair<>(new DiseaseValueHolder(curTime, skill.getDuration()), skill));
            } finally {
                chrLock.unlock();
            }

            if (disease == Disease.SEDUCE && chair.get() < 0) {
                sitChair(-1);
            }

            final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, skill.getX()));
            sendPacket(PacketCreator.giveDebuff(debuff, skill));

            if (disease != Disease.SLOW) {
                map.broadcastMessage(this, PacketCreator.giveForeignDebuff(id, debuff, skill), false);
            } else {
                map.broadcastMessage(this, PacketCreator.giveForeignSlowDebuff(id, debuff, skill), false);
            }
        }
    }
    public void giveDebuff(final Disease disease, MobSkill skill) {
        if (!hasDisease(disease) && getDiseasesSize() < 2) {
            if (!(disease == Disease.SEDUCE || disease == Disease.STUN)) {
                if (hasActiveBuff(Bishop.HOLY_SHIELD)) {
                    return;
                }
            }

            chrLock.lock();
            try {
                long curTime = Server.getInstance().getCurrentTime();
                diseaseExpires.put(disease, curTime + skill.getDuration());
                diseases.put(disease, new Pair<>(new DiseaseValueHolder(curTime, skill.getDuration()), skill));
            } finally {
                chrLock.unlock();
            }

            if (disease == Disease.SEDUCE && chair.get() < 0) {
                sitChair(-1);
            }

            final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, skill.getX()));
            sendPacket(PacketCreator.giveDebuff(debuff, skill));

            if (disease != Disease.SLOW) {
                map.broadcastMessage(this, PacketCreator.giveForeignDebuff(id, debuff, skill), false);
            } else {
                map.broadcastMessage(this, PacketCreator.giveForeignSlowDebuff(id, debuff, skill), false);
            }
        }
    }

    public void dispelDebuff(Disease debuff) {
        if (hasDisease(debuff)) {
            long mask = debuff.getValue();
            sendPacket(PacketCreator.cancelDebuff(mask));

            if (debuff != Disease.SLOW) {
                map.broadcastMessage(this, PacketCreator.cancelForeignDebuff(id, mask), false);
            } else {
                map.broadcastMessage(this, PacketCreator.cancelForeignSlowDebuff(id), false);
            }

            chrLock.lock();
            try {
                diseases.remove(debuff);
                diseaseExpires.remove(debuff);
            } finally {
                chrLock.unlock();
            }
        }
    }

    public void dispelDebuffs() {
        dispelDebuff(Disease.CURSE);
        dispelDebuff(Disease.DARKNESS);
        dispelDebuff(Disease.POISON);
        dispelDebuff(Disease.SEAL);
        dispelDebuff(Disease.WEAKEN);
        dispelDebuff(Disease.SLOW);    // thanks Conrad for noticing ZOMBIFY isn't dispellable
    }

    public void purgeDebuffs() {
        dispelDebuff(Disease.SEDUCE);
        dispelDebuff(Disease.ZOMBIFY);
        dispelDebuff(Disease.CONFUSE);
        dispelDebuffs();
    }

    public void cancelAllDebuffs() {
        chrLock.lock();
        try {
            diseases.clear();
            diseaseExpires.clear();
        } finally {
            chrLock.unlock();
        }
    }

    public void dispelSkill(int skillid) {
        List<BuffStatValueHolder> allBuffs = getAllStatups();
        for (BuffStatValueHolder mbsvh : allBuffs) {
            if (skillid == 0) {
                if (mbsvh.effect.isSkill() && (mbsvh.effect.getSourceId() % 10000000 == 1004 || dispelSkills(mbsvh.effect.getSourceId()))) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            } else if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    private static boolean dispelSkills(int skillid) {
        return switch (skillid) {
            case DarkKnight.BEHOLDER, FPArchMage.ELQUINES, ILArchMage.IFRIT, Priest.SUMMON_DRAGON, Bishop.BAHAMUT,
                 Ranger.PUPPET, Ranger.SILVER_HAWK, Sniper.PUPPET, Sniper.GOLDEN_EAGLE, Hermit.SHADOW_PARTNER -> true;
            default -> false;
        };
    }

    public void changeFaceExpression(int emote) {
        long timeNow = Server.getInstance().getCurrentTime();
        // Client allows changing every 2 seconds. Give it a little bit of overhead for packet delays.
        if (timeNow - lastExpression > 1500) {
            lastExpression = timeNow;
            getMap().broadcastMessage(this, PacketCreator.facialExpression(this, emote), false);
        }
    }

    public void doHurtHp() {
        if (!(this.getInventory(InventoryType.EQUIPPED).findById(getMap().getHPDecProtect()) != null || buffMapProtection())) {
            addHP(-getMap().getHPDec());
        }
    }

    public void dropMessage(String message) {
        dropMessage(0, message);
    }

    /**
     * 给玩家角色发送消息
     * @param type  0=聊天窗[note]蓝色消息；1=中间弹窗；2=？；3=？；4=？；5=聊天窗红色消息；6=聊天窗黄色消息
     * @param message 消息内容
     */
    public void dropMessage(int type, String message) {
        sendPacket(PacketCreator.serverNotice(type, message));
    }

    public void enteredScript(String script, int mapid) {
        if (!entered.containsKey(mapid)) {
            entered.put(mapid, script);
        }
    }

    public void equipChanged() {
        getMap().broadcastUpdateCharLookMessage(this, this);
        equipchanged = true;
        updateLocalStats();
        if (getMessenger() != null) {
            getWorldServer().updateMessenger(getMessenger(), getName(), getWorld(), client.getChannel());
        }
    }

    public void cancelDiseaseExpireTask() {
        if (diseaseExpireTask != null) {
            diseaseExpireTask.cancel(false);
            diseaseExpireTask = null;
        }
    }

    public void diseaseExpireTask() {
        if (diseaseExpireTask == null) {
            diseaseExpireTask = TimerManager.getInstance().register(() -> {
                Set<Disease> toExpire = new LinkedHashSet<>();

                chrLock.lock();
                try {
                    long curTime = Server.getInstance().getCurrentTime();

                    for (Entry<Disease, Long> de : diseaseExpires.entrySet()) {
                        if (de.getValue() < curTime) {
                            toExpire.add(de.getKey());
                        }
                    }
                } finally {
                    chrLock.unlock();
                }

                for (Disease d : toExpire) {
                    dispelDebuff(d);
                }
            }, 1500);
        }
    }

    public void cancelBuffExpireTask() {
        if (buffExpireTask != null) {
            buffExpireTask.cancel(false);
            buffExpireTask = null;
        }
    }

    public void buffExpireTask() {
        if (buffExpireTask == null) {
            buffExpireTask = TimerManager.getInstance().register(() -> {
                Set<Entry<Integer, Long>> es;
                List<BuffStatValueHolder> toCancel = new ArrayList<>();

                effLock.lock();
                chrLock.lock();
                try {
                    es = new LinkedHashSet<>(buffExpires.entrySet());

                    long curTime = Server.getInstance().getCurrentTime();
                    for (Entry<Integer, Long> bel : es) {
                        if (curTime >= bel.getValue()) {
                            toCancel.add(buffEffects.get(bel.getKey()).entrySet().iterator().next().getValue());    //rofl
                        }
                    }
                } finally {
                    chrLock.unlock();
                    effLock.unlock();
                }

                for (BuffStatValueHolder mbsvh : toCancel) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            }, 1500);
        }
    }

    public void cancelSkillCooldownTask() {
        if (skillCooldownTask != null) {
            skillCooldownTask.cancel(false);
            skillCooldownTask = null;
        }
    }

    public void skillCooldownTask() {
        if (skillCooldownTask == null) {
            skillCooldownTask = TimerManager.getInstance().register(() -> {
                Set<Entry<Integer, CooldownValueHolder>> es;

                effLock.lock();
                chrLock.lock();
                try {
                    es = new LinkedHashSet<>(coolDowns.entrySet());
                } finally {
                    chrLock.unlock();
                    effLock.unlock();
                }

                long curTime = Server.getInstance().getCurrentTime();
                for (Entry<Integer, CooldownValueHolder> bel : es) {
                    CooldownValueHolder mcdvh = bel.getValue();
                    if (curTime >= mcdvh.startTime + mcdvh.length) {
                        removeCooldown(mcdvh.skillId);
                        sendPacket(PacketCreator.skillCooldown(mcdvh.skillId, 0));
                    }
                }
            }, 1500);
        }
    }

    public void cancelExpirationTask() {
        if (itemExpireTask != null) {
            itemExpireTask.cancel(false);
            itemExpireTask = null;
        }
    }

    public void expirationTask() {
        if (itemExpireTask == null) {
            itemExpireTask = TimerManager.getInstance().register(() -> {
                boolean deletedCoupon = false;

                long expiration, currenttime = System.currentTimeMillis();
                Set<Skill> keys = getSkills().keySet();
                for (Skill key : keys) {
                    SkillEntry skill = getSkills().get(key);
                    if (skill.expiration != -1 && skill.expiration < currenttime) {
                        changeSkillLevel(key, (byte) -1, 0, -1);
                    }
                }

                List<Item> toberemove = new ArrayList<>();
                for (Inventory inv : inventory) {
                    for (Item item : inv.list()) {
                        expiration = item.getExpiration();

                        if (expiration != -1 && (expiration < currenttime) && ((item.getFlag() & ItemConstants.LOCK) == ItemConstants.LOCK)) {
                            short lock = item.getFlag();
                            lock &= ~(ItemConstants.LOCK);
                            item.setFlag(lock); //Probably need a check, else people can make expiring items into permanent items...
                            item.setExpiration(-1);
                            forceUpdateItem(item);   //TEST :3
                        } else if (expiration != -1 && expiration < currenttime) {
                            if (!ItemConstants.isPet(item.getItemId())) {
                                sendPacket(PacketCreator.itemExpired(item.getItemId()));
                                toberemove.add(item);
                                if (ItemConstants.isRateCoupon(item.getItemId())) {
                                    deletedCoupon = true;
                                }
                            } else {
                                Pet pet = item.getPet();   // thanks Lame for noticing pets not getting despawned after expiration time
                                if (pet != null) {
                                    unEquipPet(pet, true);
                                }

                                if (ItemConstants.isExpirablePet(item.getItemId())) {
                                    sendPacket(PacketCreator.itemExpired(item.getItemId()));
                                    toberemove.add(item);
                                } else {
                                    item.setExpiration(-1);
                                    forceUpdateItem(item);
                                }
                            }
                        }
                    }

                    if (!toberemove.isEmpty()) {
                        for (Item item : toberemove) {
                            InventoryManipulator.removeFromSlot(client, inv.getType(), item.getPosition(), item.getQuantity(), true);
                        }

                        ItemInformationProvider ii = ItemInformationProvider.getInstance();
                        for (Item item : toberemove) {
                            List<Integer> toadd = new ArrayList<>();
                            Pair<Integer, String> replace = ii.getReplaceOnExpire(item.getItemId());
                            if (replace.left > 0) {
                                toadd.add(replace.left);
                                if (!replace.right.isEmpty()) {
                                    dropMessage(replace.right);
                                }
                            }
                            for (Integer itemid : toadd) {
                                InventoryManipulator.addById(client, itemid, (short) 1);
                            }
                        }

                        toberemove.clear();
                    }

                    if (deletedCoupon) {
                        updateCouponRates();
                    }
                }
            }, 60000);
        }
    }

    public void forceUpdateItem(Item item) {
        final List<ModifyInventory> mods = new LinkedList<>();
        mods.add(new ModifyInventory(3, item));
        mods.add(new ModifyInventory(0, item));
        sendPacket(PacketCreator.modifyInventory(true, mods));
    }

    /**
     * 修复在某些未知极端情况下导致经验变成负数
     */
    public void gainGachaExp() {
        long expgain = 0; // 改为long
        long currentgexp = gachaExp.get(); // 当前gachaExp是int，转long

        long expNeeded = ExpTable.getExpNeededForLevel(level);
        if (expNeeded > Integer.MAX_VALUE) {
            // 如果所需经验超过int最大值，我们将其限制为int最大值，避免后续计算溢出
            expNeeded = Integer.MAX_VALUE;
        }

        if ((currentgexp + exp.get()) >= expNeeded) {
            expgain = expNeeded - exp.get();

            int nextneed = ExpTable.getExpNeededForLevel(level + 1);
            if (nextneed < 0) {
                // 如果下一级所需经验为负数，说明可能超过int范围，我们取int最大值
                nextneed = Integer.MAX_VALUE;
            }
            if (currentgexp - expgain >= nextneed) {
                expgain += nextneed;
            }

            long newGachaExp = currentgexp - expgain;
            if (newGachaExp < 0) {
                newGachaExp = 0;
            } else if (newGachaExp > Integer.MAX_VALUE) {
                newGachaExp = Integer.MAX_VALUE;
            }
            this.gachaExp.set((int) newGachaExp);
        } else {
            expgain = this.gachaExp.getAndSet(0);
        }

        // 将expgain转换为int，确保在int范围内
        int expGainInt;
        if (expgain > Integer.MAX_VALUE) {
            expGainInt = Integer.MAX_VALUE;
        } else if (expgain < Integer.MIN_VALUE) {
            expGainInt = Integer.MIN_VALUE;
        } else {
            expGainInt = (int) expgain;
        }

        gainExp(expGainInt, false, true);
        updateSingleStat(Stat.GACHAEXP, this.gachaExp.get());
    }

    public void addGachaExp(int gain) {
        updateSingleStat(Stat.GACHAEXP, gachaExp.addAndGet(gain));
    }

    public void gainExp(int gain) {
        gainExp(gain, true, true);
    }

    public void gainExp(int gain, boolean show, boolean inChat) {
        gainExp(gain, show, inChat, true);
    }

    public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
        gainExp(gain, 0, show, inChat, white);
    }

    public void gainExp(int gain, int party, boolean show, boolean inChat, boolean white) {
        // 使用long进行中间计算防止溢出
        long longGain = gain;
        long longParty = party;

        if (hasDisease(Disease.CURSE)) {
            // 安全的乘法运算
            longGain = (long) (longGain * 0.5);
            longParty = (long) (longParty * 0.5);
        }

        // 修复溢出处理逻辑
        longGain = clampExpValue(longGain);
        longParty = clampExpValue(longParty);

        // 安全的equip计算
        long equipValue = 0;
        if (longGain > 0 && pendantExp > 0) {
            try {
                equipValue = Math.multiplyExact(longGain / 10, pendantExp);
                equipValue = Math.min(equipValue, Integer.MAX_VALUE);
            } catch (ArithmeticException e) {
                equipValue = Integer.MAX_VALUE;
            }
        }

        int safeGain = (int) longGain;
        int safeParty = (int) longParty;
        int safeEquip = (int) Math.max(equipValue, 0);

        // 记录负数经验值的警告
        if (gain < 0 || party < 0) {
            log.warn("客户端 {} 玩家 {} 等级 {} 在地图[{}]({}) 获得经验 {} 组队经验 {} 处理后经验 {} 组队经验 {}",
                    getClient().getRemoteAddress(),
                    getName(), getLevel(),
                    getMap().getMapName(),getMapId(),
                    gain, party,
                    longGain,longParty
            );
        }

        gainExpInternal(safeGain, safeEquip, safeParty, show, inChat, white);
    }

    /**
     * 安全限制经验值范围
     */
    private long clampExpValue(long expValue) {
        if (expValue < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else if (expValue > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return expValue;
    }

    public void loseExp(int loss, boolean show, boolean inChat) {
        loseExp(loss, show, inChat, true);
    }

    public void loseExp(int loss, boolean show, boolean inChat, boolean white) {
        gainExpInternal(-loss, 0, 0, show, inChat, white);
    }

    private void announceExpGain(long gain, int equip, int party, boolean inChat, boolean white) {
        gain = Math.min(gain, Integer.MAX_VALUE);
        if (gain == 0) {
            if (party == 0) {
                return;
            }

            gain = party;
            party = 0;
            white = false;
        }

        sendPacket(PacketCreator.getShowExpGain((int) gain, equip, party, inChat, white));
    }

    /**
     * 内部经验获取方法，处理角色经验增长的核心逻辑
     *
     * @param gain 基础经验 gain
     * @param equip 装备提供的额外经验
     * @param party 组队经验加成
     * @param show 是否显示经验获取效果
     * @param inChat 是否在聊天框中显示
     * @param white 是否以白色字体显示(通常用于任务经验)
     *
     * 该方法通过同步确保经验计算的线程安全性，防止并发问题导致的经验异常
     */
    private synchronized void gainExpInternal(long gain, int equip, int party, boolean show, boolean inChat, boolean white) {
        // 计算总经验 gain = 基础经验 + 装备经验 + 组队经验
        long total = gain + equip + party;
        // 防止经验总值下溢，设置最小值限制
        if (total < Integer.MIN_VALUE) total = Integer.MIN_VALUE;
        // 检查角色等级是否未达到最大等级 且 允许获得经验(活动期间可能禁用经验获取)
        if (level <= getMaxLevel() && (allowExpGain || this.getEventInstance() != null)) {
            long leftover = 0;  // 用于存储超过当前等级最大经验后的剩余经验
            long nextExp = exp.get() + total;  // 计算获得经验后的总经验数
            // 处理经验上限溢出情况
            if (nextExp > (long) Integer.MAX_VALUE) {
                // 如果超出 Integer 最大值，则只增加到最大值的部分
                total = Integer.MAX_VALUE - exp.get();
                // 如果不是最高等级，计算超出部分作为剩余经验
                if (level != getMaxLevel()) leftover = nextExp - Integer.MAX_VALUE;
            }
            // 更新角色经验并通知客户端
            updateSingleStat(Stat.EXP, exp.addAndGet((int) total));
            totalExpGained += total;  // 累计本次获得的总经验
            // 如果需要显示经验获取效果，则向客户端发送相关数据包
            if (show) announceExpGain(gain, equip, party, inChat, white);
            // 升级处理循环 - 当经验足够升级时持续升级
            while (exp.get() >= ExpTable.getExpNeededForLevel(level) && level != getMaxLevel()) {
                levelUp(true);  // 执行升级操作
                // 全服升级公告逻辑
                if (GameConfig.getServerBoolean("use_announce_global_level_up") && !isGM()) {
                    String msg = I18nUtil.getMessage("Character.levelUp.globalNotice", getName(), getMap().getMapName(), getLevel());
                    // 遍历所有在线玩家发送升级公告(排除在商城中的玩家避免弹窗骚扰)
                    for (Character player : getWorldServer().getPlayerStorage().getAllCharacters()) {
                        if (player.getCashShop().isOpened()) {
                            continue;  // 跳过在商城中的玩家
                        }
                        player.dropMessage(6, msg);  // 发送黄色聊天框消息
                    }
                    log.info(msg);  // 记录日志
                }
                // 特殊处理最高等级情况
                if (level == getMaxLevel()) {
                    // 如果经验少于升级所需经验的99%，则停止升级循环
                    if (exp.get() < (ExpTable.getExpNeededForLevel(level) * 0.99)) {
                        break;
                    } else {
                        // 否则将经验清零
                        setExp(0);
                        updateSingleStat(Stat.EXP, 0);
                        break;
                    }
                }
                // 如果启用了升级保护机制，则每次只升一级
                if (GameConfig.getServerBoolean("use_level_up_protect")) break;
            }

            // 递归处理剩余经验(当一次获得大量经验导致多级升级时)
            if (leftover > 0) {
                gainExpInternal(leftover, equip, party, false, inChat, white);
            } else {
                // 更新最后经验获取时间
                lastExpGainTime = System.currentTimeMillis();
                // 记录经验获取日志
                if (GameConfig.getServerBoolean("use_exp_gain_log")) {
                    ExpLogRecord expLogRecord = new ExpLogger.ExpLogRecord(
                            getWorldServer().getExpRate(),  // 世界经验倍率
                            expCoupon,                      // 经验券倍率
                            totalExpGained,                 // 累计获得经验
                            exp.get(),                      // 当前经验
                            new Timestamp(lastExpGainTime), // 时间戳
                            id                              // 角色ID
                    );
                    ExpLogger.putExpLogRecord(expLogRecord);
                }
                // 重置累计经验计数器
                totalExpGained = 0;
            }
        }
    }

    private Pair<Integer, Integer> applyFame(int delta) {
        petLock.lock();
        try {
            int newFame = fame + delta;
            if (newFame < -30000) {
                delta = -(30000 + fame);
            } else if (newFame > 30000) {
                delta = 30000 - fame;
            }

            fame += delta;
            return new Pair<>(fame, delta);
        } finally {
            petLock.unlock();
        }
    }

    public void gainFame(int delta) {
        gainFame(delta, null, 0);
    }

    public boolean gainFame(int delta, Character fromPlayer, int mode) {
        Pair<Integer, Integer> fameRes = applyFame(delta);
        delta = fameRes.getRight();
        if (delta != 0) {
            int thisFame = fameRes.getLeft();
            updateSingleStat(Stat.FAME, thisFame);

            if (fromPlayer != null) {
                fromPlayer.sendPacket(PacketCreator.giveFameResponse(mode, getName(), thisFame));
                sendPacket(PacketCreator.receiveFame(mode, fromPlayer.getName()));
            } else {
                sendPacket(PacketCreator.getShowFameGain(delta));
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean canHoldMeso(int gain) {  // thanks lucasziron for pointing out a need to check space availability for mesos on player transactions
        long nextMeso = (long) meso.get() + gain;
        return nextMeso <= Integer.MAX_VALUE;
    }

    public void gainMeso(int gain) {
        gainMeso(gain, true, false, true);
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
        long nextMeso;
        petLock.lock();
        try {
            nextMeso = (long) meso.get() + gain;  // thanks Thora for pointing integer overflow here
            if (nextMeso > Integer.MAX_VALUE) {
                gain -= (int) (nextMeso - Integer.MAX_VALUE);
            } else if (nextMeso < 0) {
                gain = -meso.get();
            }
            nextMeso = meso.addAndGet(gain);
        } finally {
            petLock.unlock();
        }

        if (gain != 0) {
            updateSingleStat(Stat.MESO, (int) nextMeso, enableActions);
            // [新增] 实时保存金币
            characterService.update(CharactersDO.builder().id(id).meso((int)nextMeso).build());
            if (show) {
                sendPacket(PacketCreator.getShowMesoGain(gain, inChat));
            }
        } else {
            enableActions();
        }
    }

    public void genericGuildMessage(int code) {
        this.sendPacket(GuildPackets.genericGuildMessage((byte) code));
    }

    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<>();

        effLock.lock();
        chrLock.lock();
        try {
            for (CooldownValueHolder mcdvh : coolDowns.values()) {
                ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }

        return ret;
    }

    public void updateAriantScore() {
        updateAriantScore(0);
    }

    public void updateAriantScore(int dropQty) {
        AriantColiseum arena = this.getAriantColiseum();
        if (arena != null) {
            arena.updateAriantScore(this, countItem(ItemId.ARPQ_SPIRIT_JEWEL));

            if (dropQty > 0) {
                arena.addLostShards(dropQty);
            }
        }
    }

    public Long getBuffedStarttime(BuffStat effect) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return null;
            }
            return mbsvh.startTime;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public Integer getBuffedValue(BuffStat effect) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return null;
            }
            return mbsvh.value;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public int getBuffSource(BuffStat stat) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return -1;
            }
            return mbsvh.effect.getSourceId();
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public StatEffect getBuffEffect(BuffStat stat) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return null;
            } else {
                return mbsvh.effect;
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    private List<BuffStatValueHolder> getAllStatups() {
        effLock.lock();
        chrLock.lock();
        try {
            List<BuffStatValueHolder> ret = new ArrayList<>();
            for (Map<BuffStat, BuffStatValueHolder> bel : buffEffects.values()) {
                ret.addAll(bel.values());
            }
            return ret;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {  // buff values will be stored in an arbitrary order
        effLock.lock();
        chrLock.lock();
        try {
            long curtime = Server.getInstance().getCurrentTime();

            Map<Integer, PlayerBuffValueHolder> ret = new LinkedHashMap<>();
            for (Map<BuffStat, BuffStatValueHolder> bel : buffEffects.values()) {
                for (BuffStatValueHolder mbsvh : bel.values()) {
                    int srcid = mbsvh.effect.getBuffSourceId();
                    if (!ret.containsKey(srcid)) {
                        ret.put(srcid, new PlayerBuffValueHolder((int) (curtime - mbsvh.startTime), mbsvh.effect));
                    }
                }
            }
            return new ArrayList<>(ret.values());
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public boolean hasBuffFromSourceid(int sourceid) {
        effLock.lock();
        chrLock.lock();
        try {
            return buffEffects.containsKey(sourceid);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public boolean hasActiveBuff(int sourceid) {
        LinkedList<BuffStatValueHolder> allBuffs;

        effLock.lock();
        chrLock.lock();
        try {
            allBuffs = new LinkedList<>(effects.values());
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }

        for (BuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getBuffSourceId() == sourceid) {
                return true;
            }
        }
        return false;
    }

    private List<Pair<BuffStat, Integer>> getActiveStatupsFromSourceid(int sourceid) { // already under effLock & chrLock
        List<Pair<BuffStat, Integer>> ret = new ArrayList<>();
        List<Pair<BuffStat, Integer>> singletonStatups = new ArrayList<>();
        for (Entry<BuffStat, BuffStatValueHolder> bel : buffEffects.get(sourceid).entrySet()) {
            BuffStat mbs = bel.getKey();
            BuffStatValueHolder mbsvh = effects.get(bel.getKey());

            Pair<BuffStat, Integer> p;
            if (mbsvh != null) {
                p = new Pair<>(mbs, mbsvh.value);
            } else {
                p = new Pair<>(mbs, 0);
            }

            if (!isSingletonStatup(mbs)) {   // thanks resinate, Daddy Egg for pointing out morph issues when updating it along with other statups
                ret.add(p);
            } else {
                singletonStatups.add(p);
            }
        }

        ret.sort(Comparator.comparing(Pair::getLeft));

        if (!singletonStatups.isEmpty()) {
            singletonStatups.sort(Comparator.comparing(Pair::getLeft));

            ret.addAll(singletonStatups);
        }

        return ret;
    }

    private void addItemEffectHolder(Integer sourceid, long expirationtime, Map<BuffStat, BuffStatValueHolder> statups) {
        buffEffects.put(sourceid, statups);
        buffExpires.put(sourceid, expirationtime);
    }

    private boolean removeEffectFromItemEffectHolder(Integer sourceid, BuffStat buffStat) {
        Map<BuffStat, BuffStatValueHolder> lbe = buffEffects.get(sourceid);

        if (lbe.remove(buffStat) != null) {
            buffEffectsCount.put(buffStat, (byte) (buffEffectsCount.get(buffStat) - 1));

            if (lbe.isEmpty()) {
                buffEffects.remove(sourceid);
                buffExpires.remove(sourceid);
            }

            return true;
        }

        return false;
    }

    private void removeItemEffectHolder(Integer sourceid) {
        Map<BuffStat, BuffStatValueHolder> be = buffEffects.remove(sourceid);
        if (be != null) {
            for (Entry<BuffStat, BuffStatValueHolder> bei : be.entrySet()) {
                buffEffectsCount.put(bei.getKey(), (byte) (buffEffectsCount.get(bei.getKey()) - 1));
            }
        }

        buffExpires.remove(sourceid);
    }

    private BuffStatValueHolder fetchBestEffectFromItemEffectHolder(BuffStat mbs) {
        Pair<Integer, Integer> max = new Pair<>(Integer.MIN_VALUE, 0);
        BuffStatValueHolder mbsvh = null;
        for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> bpl : buffEffects.entrySet()) {
            BuffStatValueHolder mbsvhi = bpl.getValue().get(mbs);
            if (mbsvhi != null) {
                if (!mbsvhi.effect.isActive(this)) {
                    continue;
                }

                if (mbsvhi.value > max.left) {
                    max = new Pair<>(mbsvhi.value, mbsvhi.effect.getStatups().size());
                    mbsvh = mbsvhi;
                } else if (mbsvhi.value == max.left && mbsvhi.effect.getStatups().size() > max.right) {
                    max = new Pair<>(mbsvhi.value, mbsvhi.effect.getStatups().size());
                    mbsvh = mbsvhi;
                }
            }
        }

        if (mbsvh != null) {
            effects.put(mbs, mbsvh);
        }
        return mbsvh;
    }

    private void extractBuffValue(int sourceid, BuffStat stat) {
        chrLock.lock();
        try {
            removeEffectFromItemEffectHolder(sourceid, stat);
        } finally {
            chrLock.unlock();
        }
    }

    public void debugListAllBuffs() {
        effLock.lock();
        chrLock.lock();
        try {
            log.debug("-------------------");
            log.debug("CACHED BUFF COUNT: {}", buffEffectsCount.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining(", "))
            );

            log.debug("-------------------");
            log.debug("CACHED BUFFS: {}", buffEffects.entrySet().stream()
                    .map(entry -> entry.getKey() + ": (" + entry.getValue().entrySet().stream()
                            .map(innerEntry -> innerEntry.getKey().name() + innerEntry.getValue().value)
                            .collect(Collectors.joining(", ")) + ")")
                    .collect(Collectors.joining(", "))
            );

            log.debug("-------------------");
            log.debug("IN ACTION: {}", effects.entrySet().stream()
                    .map(entry -> entry.getKey().name() + " -> " + ItemInformationProvider.getInstance().getName(entry.getValue().effect.getSourceId()))
                    .collect(Collectors.joining(", "))
            );
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void cancelAllBuffs(boolean softcancel) {
        if (softcancel) {
            effLock.lock();
            chrLock.lock();
            try {
                cancelEffectFromBuffStat(BuffStat.SUMMON);
                cancelEffectFromBuffStat(BuffStat.PUPPET);
                cancelEffectFromBuffStat(BuffStat.COMBO);

                effects.clear();

                for (Integer srcid : new ArrayList<>(buffEffects.keySet())) {
                    removeItemEffectHolder(srcid);
                }
            } finally {
                chrLock.unlock();
                effLock.unlock();
            }
        } else {
            Map<StatEffect, Long> mseBuffs = new LinkedHashMap<>();

            effLock.lock();
            chrLock.lock();
            try {
                for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> bpl : buffEffects.entrySet()) {
                    for (Entry<BuffStat, BuffStatValueHolder> mbse : bpl.getValue().entrySet()) {
                        mseBuffs.put(mbse.getValue().effect, mbse.getValue().startTime);
                    }
                }
            } finally {
                chrLock.unlock();
                effLock.unlock();
            }

            for (Entry<StatEffect, Long> mse : mseBuffs.entrySet()) {
                cancelEffect(mse.getKey(), false, mse.getValue());
            }
        }
    }

    private void dropBuffStats(List<Pair<BuffStat, BuffStatValueHolder>> effectsToCancel) {
        for (Pair<BuffStat, BuffStatValueHolder> cancelEffectCancelTasks : effectsToCancel) {
            //boolean nestedCancel = false;

            chrLock.lock();
            try {
                /*
                if (buffExpires.get(cancelEffectCancelTasks.getRight().effect.getBuffSourceId()) != null) {
                    nestedCancel = true;
                }*/

                if (cancelEffectCancelTasks.getRight().bestApplied) {
                    fetchBestEffectFromItemEffectHolder(cancelEffectCancelTasks.getLeft());
                }
            } finally {
                chrLock.unlock();
            }

            /*
            if (nestedCancel) {
                this.cancelEffect(cancelEffectCancelTasks.getRight().effect, false, -1, false);
            }*/
        }
    }

    private List<Pair<BuffStat, BuffStatValueHolder>> deregisterBuffStats(Map<BuffStat, BuffStatValueHolder> stats) {
        chrLock.lock();
        try {
            List<Pair<BuffStat, BuffStatValueHolder>> effectsToCancel = new ArrayList<>(stats.size());
            for (Entry<BuffStat, BuffStatValueHolder> stat : stats.entrySet()) {
                int sourceid = stat.getValue().effect.getBuffSourceId();

                if (!buffEffects.containsKey(sourceid)) {
                    buffExpires.remove(sourceid);
                }

                BuffStat mbs = stat.getKey();
                effectsToCancel.add(new Pair<>(mbs, stat.getValue()));

                BuffStatValueHolder mbsvh = effects.get(mbs);
                if (mbsvh != null && mbsvh.effect.getBuffSourceId() == sourceid) {
                    mbsvh.bestApplied = true;
                    effects.remove(mbs);

                    if (mbs == BuffStat.RECOVERY) {
                        if (recoveryTask != null) {
                            recoveryTask.cancel(false);
                            recoveryTask = null;
                        }
                    } else if (mbs == BuffStat.SUMMON || mbs == BuffStat.PUPPET) {
                        int summonId = mbsvh.effect.getSourceId();

                        Summon summon = summons.get(summonId);
                        if (summon != null) {
                            getMap().broadcastMessage(PacketCreator.removeSummon(summon, true), summon.getPosition());
                            getMap().removeMapObject(summon);
                            removeVisibleMapObject(summon);

                            summons.remove(summonId);
                            if (summon.isPuppet()) {
                                map.removePlayerPuppet(this);
                            } else if (summon.getSkill() == DarkKnight.BEHOLDER) {
                                if (beholderHealingSchedule != null) {
                                    beholderHealingSchedule.cancel(false);
                                    beholderHealingSchedule = null;
                                }
                                if (beholderBuffSchedule != null) {
                                    beholderBuffSchedule.cancel(false);
                                    beholderBuffSchedule = null;
                                }
                            }
                        }
                    } else if (mbs == BuffStat.DRAGONBLOOD) {
                        dragonBloodSchedule.cancel(false);
                        dragonBloodSchedule = null;
                    } else if (mbs == BuffStat.HPREC || mbs == BuffStat.MPREC) {
                        if (mbs == BuffStat.HPREC) {
                            extraHpRec = 0;
                        } else {
                            extraMpRec = 0;
                        }

                        if (extraRecoveryTask != null) {
                            extraRecoveryTask.cancel(false);
                            extraRecoveryTask = null;
                        }

                        if (extraHpRec != 0 || extraMpRec != 0) {
                            startExtraTaskInternal(extraHpRec, extraMpRec, extraRecInterval);
                        }
                    }
                }
            }

            return effectsToCancel;
        } finally {
            chrLock.unlock();
        }
    }

    public void cancelEffect(int itemId) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        cancelEffect(ii.getItemEffect(itemId), false, -1);
    }

    public boolean cancelEffect(StatEffect effect, boolean overwrite, long startTime) {
        boolean ret;

        prtLock.lock();
        effLock.lock();
        try {
            ret = cancelEffect(effect, overwrite, startTime, true);
        } finally {
            effLock.unlock();
            prtLock.unlock();
        }

        if (effect.isMagicDoor() && ret) {
            prtLock.lock();
            effLock.lock();
            try {
                if (!hasBuffFromSourceid(Priest.MYSTIC_DOOR)) {
                    Door.attemptRemoveDoor(this);
                }
            } finally {
                effLock.unlock();
                prtLock.unlock();
            }
        }

        return ret;
    }

    private static StatEffect getEffectFromBuffSource(Map<BuffStat, BuffStatValueHolder> buffSource) {
        try {
            return buffSource.entrySet().iterator().next().getValue().effect;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isUpdatingEffect(Set<StatEffect> activeEffects, StatEffect mse) {
        if (mse == null) {
            return false;
        }

        // thanks xinyifly for noticing "Speed Infusion" crashing game when updating buffs during map transition
        boolean active = mse.isActive(this);
        if (active) {
            return !activeEffects.contains(mse);
        } else {
            return activeEffects.contains(mse);
        }
    }

    public void updateActiveEffects() {
        effLock.lock();     // thanks davidlafriniere, maple006, RedHat for pointing a deadlock occurring here
        try {
            Set<BuffStat> updatedBuffs = new LinkedHashSet<>();
            Set<StatEffect> activeEffects = new LinkedHashSet<>();

            for (BuffStatValueHolder mse : effects.values()) {
                activeEffects.add(mse.effect);
            }

            for (Map<BuffStat, BuffStatValueHolder> buff : buffEffects.values()) {
                StatEffect mse = getEffectFromBuffSource(buff);
                if (isUpdatingEffect(activeEffects, mse)) {
                    for (Pair<BuffStat, Integer> p : mse.getStatups()) {
                        updatedBuffs.add(p.getLeft());
                    }
                }
            }

            for (BuffStat mbs : updatedBuffs) {
                effects.remove(mbs);
            }

            updateEffects(updatedBuffs);
        } finally {
            effLock.unlock();
        }
    }

    private void updateEffects(Set<BuffStat> removedStats) {
        effLock.lock();
        chrLock.lock();
        try {
            Set<BuffStat> retrievedStats = new LinkedHashSet<>();

            for (BuffStat mbs : removedStats) {
                fetchBestEffectFromItemEffectHolder(mbs);

                BuffStatValueHolder mbsvh = effects.get(mbs);
                if (mbsvh != null) {
                    for (Pair<BuffStat, Integer> statup : mbsvh.effect.getStatups()) {
                        retrievedStats.add(statup.getLeft());
                    }
                }
            }

            propagateBuffEffectUpdates(new LinkedHashMap<>(), retrievedStats, removedStats);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    private boolean cancelEffect(StatEffect effect, boolean overwrite, long startTime, boolean firstCancel) {
        Set<BuffStat> removedStats = new LinkedHashSet<>();
        dropBuffStats(cancelEffectInternal(effect, overwrite, startTime, removedStats));
        updateLocalStats();
        updateEffects(removedStats);

        return !removedStats.isEmpty();
    }

    private List<Pair<BuffStat, BuffStatValueHolder>> cancelEffectInternal(StatEffect effect, boolean overwrite, long startTime, Set<BuffStat> removedStats) {
        Map<BuffStat, BuffStatValueHolder> buffstats = null;
        BuffStat ombs;
        if (!overwrite) {   // is removing the source effect, meaning every effect from this srcid is being purged
            buffstats = extractCurrentBuffStats(effect);
        } else if ((ombs = getSingletonStatupFromEffect(effect)) != null) {   // removing all effects of a buff having non-shareable buff stat.
            BuffStatValueHolder mbsvh = effects.get(ombs);
            if (mbsvh != null) {
                buffstats = extractCurrentBuffStats(mbsvh.effect);
            }
        }

        if (buffstats == null) {            // all else, is dropping ALL current statups that uses same stats as the given effect
            buffstats = extractLeastRelevantStatEffectsIfFull(effect);
        }

        if (effect.isMapChair()) {
            stopChairTask();
        }

        List<Pair<BuffStat, BuffStatValueHolder>> toCancel = deregisterBuffStats(buffstats);
        if (effect.isMonsterRiding()) {
            this.getClient().getWorldServer().unregisterMountHunger(this);
            this.getMapleMount().setActive(false);
        }

        if (!overwrite) {
            removedStats.addAll(buffstats.keySet());
        }

        return toCancel;
    }

    public void cancelEffectFromBuffStat(BuffStat stat) {
        BuffStatValueHolder effect;

        effLock.lock();
        chrLock.lock();
        try {
            effect = effects.get(stat);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
        if (effect != null) {
            cancelEffect(effect.effect, false, -1);
        }
    }

    public void cancelBuffStats(BuffStat stat) {
        effLock.lock();
        try {
            List<Pair<Integer, BuffStatValueHolder>> cancelList = new LinkedList<>();

            chrLock.lock();
            try {
                for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> bel : this.buffEffects.entrySet()) {
                    BuffStatValueHolder beli = bel.getValue().get(stat);
                    if (beli != null) {
                        cancelList.add(new Pair<>(bel.getKey(), beli));
                    }
                }
            } finally {
                chrLock.unlock();
            }

            Map<BuffStat, BuffStatValueHolder> buffStatList = new LinkedHashMap<>();
            for (Pair<Integer, BuffStatValueHolder> p : cancelList) {
                buffStatList.put(stat, p.getRight());
                extractBuffValue(p.getLeft(), stat);
                dropBuffStats(deregisterBuffStats(buffStatList));
            }
        } finally {
            effLock.unlock();
        }

        cancelPlayerBuffs(Collections.singletonList(stat));
    }

    private Map<BuffStat, BuffStatValueHolder> extractCurrentBuffStats(StatEffect effect) {
        chrLock.lock();
        try {
            Map<BuffStat, BuffStatValueHolder> stats = new LinkedHashMap<>();
            Map<BuffStat, BuffStatValueHolder> buffList = buffEffects.remove(effect.getBuffSourceId());

            if (buffList != null) {
                for (Entry<BuffStat, BuffStatValueHolder> stateffect : buffList.entrySet()) {
                    stats.put(stateffect.getKey(), stateffect.getValue());
                    buffEffectsCount.put(stateffect.getKey(), (byte) (buffEffectsCount.get(stateffect.getKey()) - 1));
                }
            }

            return stats;
        } finally {
            chrLock.unlock();
        }
    }

    private Map<BuffStat, BuffStatValueHolder> extractLeastRelevantStatEffectsIfFull(StatEffect effect) {
        Map<BuffStat, BuffStatValueHolder> extractedStatBuffs = new LinkedHashMap<>();

        chrLock.lock();
        try {
            Map<BuffStat, Byte> stats = new LinkedHashMap<>();
            Map<BuffStat, BuffStatValueHolder> minStatBuffs = new LinkedHashMap<>();

            for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> mbsvhi : buffEffects.entrySet()) {
                for (Entry<BuffStat, BuffStatValueHolder> mbsvhe : mbsvhi.getValue().entrySet()) {
                    BuffStat mbs = mbsvhe.getKey();
                    Byte b = stats.get(mbs);

                    if (b != null) {
                        stats.put(mbs, (byte) (b + 1));
                        if (mbsvhe.getValue().value < minStatBuffs.get(mbs).value) {
                            minStatBuffs.put(mbs, mbsvhe.getValue());
                        }
                    } else {
                        stats.put(mbs, (byte) 1);
                        minStatBuffs.put(mbs, mbsvhe.getValue());
                    }
                }
            }

            Set<BuffStat> effectStatups = new LinkedHashSet<>();
            for (Pair<BuffStat, Integer> efstat : effect.getStatups()) {
                effectStatups.add(efstat.getLeft());
            }

            for (Entry<BuffStat, Byte> it : stats.entrySet()) {
                boolean uniqueBuff = isSingletonStatup(it.getKey());

                if (it.getValue() >= (!uniqueBuff ? GameConfig.getServerByte("max_monitored_buff_stats") : 1) && effectStatups.contains(it.getKey())) {
                    BuffStatValueHolder mbsvh = minStatBuffs.get(it.getKey());

                    Map<BuffStat, BuffStatValueHolder> lpbe = buffEffects.get(mbsvh.effect.getBuffSourceId());
                    lpbe.remove(it.getKey());
                    buffEffectsCount.put(it.getKey(), (byte) (buffEffectsCount.get(it.getKey()) - 1));

                    if (lpbe.isEmpty()) {
                        buffEffects.remove(mbsvh.effect.getBuffSourceId());
                    }
                    extractedStatBuffs.put(it.getKey(), mbsvh);
                }
            }
        } finally {
            chrLock.unlock();
        }

        return extractedStatBuffs;
    }

    private void cancelInactiveBuffStats(Set<BuffStat> retrievedStats, Set<BuffStat> removedStats) {
        List<BuffStat> inactiveStats = new LinkedList<>();
        for (BuffStat mbs : removedStats) {
            if (!retrievedStats.contains(mbs)) {
                inactiveStats.add(mbs);
            }
        }

        if (!inactiveStats.isEmpty()) {
            sendPacket(PacketCreator.cancelBuff(inactiveStats));
            getMap().broadcastMessage(this, PacketCreator.cancelForeignBuff(getId(), inactiveStats), false);
        }
    }

    private static Map<StatEffect, Integer> topologicalSortLeafStatCount(Map<BuffStat, Stack<StatEffect>> buffStack) {
        Map<StatEffect, Integer> leafBuffCount = new LinkedHashMap<>();

        for (Entry<BuffStat, Stack<StatEffect>> e : buffStack.entrySet()) {
            Stack<StatEffect> mseStack = e.getValue();
            if (mseStack.isEmpty()) {
                continue;
            }

            StatEffect mse = mseStack.peek();
            leafBuffCount.merge(mse, 1, Integer::sum);
        }

        return leafBuffCount;
    }

    private static List<StatEffect> topologicalSortRemoveLeafStats(Map<StatEffect, Set<BuffStat>> stackedBuffStats, Map<BuffStat, Stack<StatEffect>> buffStack, Map<StatEffect, Integer> leafStatCount) {
        List<StatEffect> clearedStatEffects = new LinkedList<>();
        Set<BuffStat> clearedStats = new LinkedHashSet<>();

        for (Entry<StatEffect, Integer> e : leafStatCount.entrySet()) {
            StatEffect mse = e.getKey();

            if (stackedBuffStats.get(mse).size() <= e.getValue()) {
                clearedStatEffects.add(mse);
                clearedStats.addAll(stackedBuffStats.get(mse));
            }
        }

        for (BuffStat mbs : clearedStats) {
            StatEffect mse = buffStack.get(mbs).pop();
            stackedBuffStats.get(mse).remove(mbs);
        }

        return clearedStatEffects;
    }

    private static void topologicalSortRebaseLeafStats(Map<StatEffect, Set<BuffStat>> stackedBuffStats, Map<BuffStat, Stack<StatEffect>> buffStack) {
        for (Entry<BuffStat, Stack<StatEffect>> e : buffStack.entrySet()) {
            Stack<StatEffect> mseStack = e.getValue();

            if (!mseStack.isEmpty()) {
                StatEffect mse = mseStack.pop();
                stackedBuffStats.get(mse).remove(e.getKey());
            }
        }
    }

    private static List<StatEffect> topologicalSortEffects(Map<BuffStat, List<Pair<StatEffect, Integer>>> buffEffects) {
        Map<StatEffect, Set<BuffStat>> stackedBuffStats = new LinkedHashMap<>();
        Map<BuffStat, Stack<StatEffect>> buffStack = new LinkedHashMap<>();

        for (Entry<BuffStat, List<Pair<StatEffect, Integer>>> e : buffEffects.entrySet()) {
            BuffStat mbs = e.getKey();

            Stack<StatEffect> mbsStack = new Stack<>();
            buffStack.put(mbs, mbsStack);

            for (Pair<StatEffect, Integer> emse : e.getValue()) {
                StatEffect mse = emse.getLeft();
                mbsStack.push(mse);
                Set<BuffStat> mbsStats = stackedBuffStats.computeIfAbsent(mse, k -> new LinkedHashSet<>());
                mbsStats.add(mbs);
            }
        }

        List<StatEffect> buffList = new LinkedList<>();
        // 添加最大迭代次数限制，防止无限循环
        int iterations = 0;
        int maxIterations = 10000; // 最大迭代次数设置为10000次

        while (iterations < maxIterations) {
            Map<StatEffect, Integer> leafStatCount = topologicalSortLeafStatCount(buffStack);
            if (leafStatCount.isEmpty()) {
                break;
            }

            List<StatEffect> clearedNodes = topologicalSortRemoveLeafStats(stackedBuffStats, buffStack, leafStatCount);
            if (clearedNodes.isEmpty()) {
                topologicalSortRebaseLeafStats(stackedBuffStats, buffStack);
            } else {
                buffList.addAll(clearedNodes);
            }

            iterations++;
        }

        // 如果达到最大迭代次数，记录错误日志
        if (iterations >= maxIterations) {
            log.error("拓扑排序超过最大迭代次数: {}，可能存在循环依赖或算法缺陷", maxIterations);
            // 可以选择抛出异常或者返回已处理的部分结果
        } else {
            log.debug("拓扑排序完成，共迭代{}次，处理了{}个效果", iterations, buffList.size());
        }

        return buffList;
    }

    private static List<StatEffect> sortEffectsList(Map<StatEffect, Integer> updateEffectsList) {
        Map<BuffStat, List<Pair<StatEffect, Integer>>> buffEffects = new LinkedHashMap<>();

        for (Entry<StatEffect, Integer> p : updateEffectsList.entrySet()) {
            StatEffect mse = p.getKey();

            for (Pair<BuffStat, Integer> statup : mse.getStatups()) {
                BuffStat stat = statup.getLeft();
                List<Pair<StatEffect, Integer>> statBuffs = buffEffects.computeIfAbsent(stat, k -> new ArrayList<>());
                statBuffs.add(new Pair<>(mse, statup.getRight()));
            }
        }

        for (Entry<BuffStat, List<Pair<StatEffect, Integer>>> statBuffs : buffEffects.entrySet()) {
            statBuffs.getValue().sort((o1, o2) -> o2.getRight().compareTo(o1.getRight()));
        }

        return topologicalSortEffects(buffEffects);
    }

    private List<Pair<Integer, Pair<StatEffect, Long>>> propagatePriorityBuffEffectUpdates(Set<BuffStat> retrievedStats) {
        List<Pair<Integer, Pair<StatEffect, Long>>> priorityUpdateEffects = new LinkedList<>();
        Map<BuffStatValueHolder, StatEffect> yokeStats = new LinkedHashMap<>();

        // priority buffsources: override buffstats for the client to perceive those as "currently buffed"
        Set<BuffStatValueHolder> mbsvhList = new LinkedHashSet<>(getAllStatups());

        for (BuffStatValueHolder mbsvh : mbsvhList) {
            StatEffect mse = mbsvh.effect;
            int buffSourceId = mse.getBuffSourceId();
            if (isPriorityBuffSourceId(buffSourceId) && !hasActiveBuff(buffSourceId)) {
                for (Pair<BuffStat, Integer> ps : mse.getStatups()) {
                    BuffStat mbs = ps.getLeft();
                    if (retrievedStats.contains(mbs)) {
                        BuffStatValueHolder mbsvhe = effects.get(mbs);

                        // this shouldn't even be null...
                        //if (mbsvh != null) {
                        yokeStats.put(mbsvh, mbsvhe.effect);
                        //}
                    }
                }
            }
        }

        for (Entry<BuffStatValueHolder, StatEffect> e : yokeStats.entrySet()) {
            BuffStatValueHolder mbsvhPriority = e.getKey();
            StatEffect mseActive = e.getValue();

            priorityUpdateEffects.add(new Pair<>(mseActive.getBuffSourceId(), new Pair<>(mbsvhPriority.effect, mbsvhPriority.startTime)));
        }

        return priorityUpdateEffects;
    }

    private void propagateBuffEffectUpdates(Map<Integer, Pair<StatEffect, Long>> retrievedEffects, Set<BuffStat> retrievedStats, Set<BuffStat> removedStats) {
        cancelInactiveBuffStats(retrievedStats, removedStats);
        if (retrievedStats.isEmpty()) {
            return;
        }

        Map<BuffStat, Pair<Integer, StatEffect>> maxBuffValue = new LinkedHashMap<>();
        for (BuffStat mbs : retrievedStats) {
            BuffStatValueHolder mbsvh = effects.get(mbs);
            if (mbsvh != null) {
                retrievedEffects.put(mbsvh.effect.getBuffSourceId(), new Pair<>(mbsvh.effect, mbsvh.startTime));
            }

            maxBuffValue.put(mbs, new Pair<>(Integer.MIN_VALUE, null));
        }

        Map<StatEffect, Integer> updateEffects = new LinkedHashMap<>();

        List<StatEffect> recalcMseList = new LinkedList<>();
        for (Entry<Integer, Pair<StatEffect, Long>> re : retrievedEffects.entrySet()) {
            recalcMseList.add(re.getValue().getLeft());
        }

        boolean mageJob = this.getJobStyle() == Job.MAGICIAN;
        do {
            List<StatEffect> mseList = recalcMseList;
            recalcMseList = new LinkedList<>();

            for (StatEffect mse : mseList) {
                int maxEffectiveStatup = Integer.MIN_VALUE;
                for (Pair<BuffStat, Integer> st : mse.getStatups()) {
                    BuffStat mbs = st.getLeft();

                    boolean relevantStatup = true;
                    if (mbs == BuffStat.WATK) {  // not relevant for mages
                        if (mageJob) {
                            relevantStatup = false;
                        }
                    } else if (mbs == BuffStat.MATK) { // not relevant for non-mages
                        if (!mageJob) {
                            relevantStatup = false;
                        }
                    }

                    Pair<Integer, StatEffect> mbv = maxBuffValue.get(mbs);
                    if (mbv == null) {
                        continue;
                    }

                    if (mbv.getLeft() < st.getRight()) {
                        StatEffect msbe = mbv.getRight();
                        if (msbe != null) {
                            recalcMseList.add(msbe);
                        }

                        maxBuffValue.put(mbs, new Pair<>(st.getRight(), mse));

                        if (relevantStatup) {
                            if (maxEffectiveStatup < st.getRight()) {
                                maxEffectiveStatup = st.getRight();
                            }
                        }
                    }
                }

                updateEffects.put(mse, maxEffectiveStatup);
            }
        } while (!recalcMseList.isEmpty());

        List<StatEffect> updateEffectsList = sortEffectsList(updateEffects);

        List<Pair<Integer, Pair<StatEffect, Long>>> toUpdateEffects = new LinkedList<>();
        for (StatEffect mse : updateEffectsList) {
            toUpdateEffects.add(new Pair<>(mse.getBuffSourceId(), retrievedEffects.get(mse.getBuffSourceId())));
        }

        List<Pair<BuffStat, Integer>> activeStatups = new LinkedList<>();
        for (Pair<Integer, Pair<StatEffect, Long>> lmse : toUpdateEffects) {
            Pair<StatEffect, Long> msel = lmse.getRight();
            activeStatups.addAll(getActiveStatupsFromSourceid(lmse.getLeft()));
            msel.getLeft().updateBuffEffect(this, activeStatups, msel.getRight());
            activeStatups.clear();
        }

        List<Pair<Integer, Pair<StatEffect, Long>>> priorityEffects = propagatePriorityBuffEffectUpdates(retrievedStats);
        for (Pair<Integer, Pair<StatEffect, Long>> lmse : priorityEffects) {
            Pair<StatEffect, Long> msel = lmse.getRight();
            activeStatups.addAll(getActiveStatupsFromSourceid(lmse.getLeft()));
            msel.getLeft().updateBuffEffect(this, activeStatups, msel.getRight());
            activeStatups.clear();
        }

        if (this.isRidingBattleship()) {
            List<Pair<BuffStat, Integer>> statups = new ArrayList<>(1);
            statups.add(new Pair<>(BuffStat.MONSTER_RIDING, 0));
            this.sendPacket(PacketCreator.giveBuff(ItemId.BATTLESHIP, 5221006, statups));
            this.announceBattleshipHp();
        }
    }

    private static BuffStat getSingletonStatupFromEffect(StatEffect mse) {
        for (Pair<BuffStat, Integer> mbs : mse.getStatups()) {
            if (isSingletonStatup(mbs.getLeft())) {
                return mbs.getLeft();
            }
        }

        return null;
    }

    private static boolean isSingletonStatup(BuffStat mbs) {
        return switch (mbs) {           //HPREC and MPREC are supposed to be singleton
            case COUPON_EXP1, COUPON_EXP2, COUPON_EXP3, COUPON_EXP4, COUPON_DRP1, COUPON_DRP2, COUPON_DRP3,
                 MESO_UP_BY_ITEM,
                 ITEM_UP_BY_ITEM, RESPECT_PIMMUNE, RESPECT_MIMMUNE, DEFENSE_ATT, DEFENSE_STATE, WATK, WDEF, MATK, MDEF,
                 ACC, AVOID, SPEED, JUMP -> false;
            default -> true;
        };
    }

    private static boolean isPriorityBuffSourceId(int sourceId) {
        return -ItemId.ROSE_SCENT == sourceId || -ItemId.FREESIA_SCENT == sourceId || -ItemId.LAVENDER_SCENT == sourceId;
    }

    private void addItemEffectHolderCount(BuffStat stat) {
        Byte val = buffEffectsCount.get(stat);
        if (val != null) {
            val = (byte) (val + 1);
        } else {
            val = (byte) 1;
        }

        buffEffectsCount.put(stat, val);
    }

    public void registerEffect(StatEffect effect, long starttime, long expirationtime, boolean isSilent) {
        if (effect.isDragonBlood()) {
            prepareDragonBlood(effect);
        } else if (effect.isBerserk()) {
            checkBerserk(isHidden());
        } else if (effect.isBeholder()) {
            final int beholder = DarkKnight.BEHOLDER;
            if (beholderHealingSchedule != null) {
                beholderHealingSchedule.cancel(false);
            }
            if (beholderBuffSchedule != null) {
                beholderBuffSchedule.cancel(false);
            }
            Skill bHealing = SkillFactory.getSkill(DarkKnight.AURA_OF_BEHOLDER);
            int bHealingLvl = getSkillLevel(bHealing);
            if (bHealingLvl > 0) {
                final StatEffect healEffect = bHealing.getEffect(bHealingLvl);
                int healInterval = (int) SECONDS.toMillis(healEffect.getX());
                beholderHealingSchedule = TimerManager.getInstance().register(() -> {
                    if (awayFromWorld.get()) {
                        return;
                    }

                    addHP(healEffect.getHp());
                    sendPacket(PacketCreator.showOwnBuffEffect(beholder, 2));
                    getMap().broadcastMessage(Character.this, PacketCreator.summonSkill(getId(), beholder, 5), true);
                    getMap().broadcastMessage(Character.this, PacketCreator.showOwnBuffEffect(beholder, 2), false);
                }, healInterval, healInterval);
            }
            Skill bBuff = SkillFactory.getSkill(DarkKnight.HEX_OF_BEHOLDER);
            if (getSkillLevel(bBuff) > 0) {
                final StatEffect buffEffect = bBuff.getEffect(getSkillLevel(bBuff));
                int buffInterval = (int) SECONDS.toMillis(buffEffect.getX());
                beholderBuffSchedule = TimerManager.getInstance().register(() -> {
                    if (awayFromWorld.get()) {
                        return;
                    }

                    buffEffect.applyTo(Character.this);
                    sendPacket(PacketCreator.showOwnBuffEffect(beholder, 2));
                    getMap().broadcastMessage(Character.this, PacketCreator.summonSkill(getId(), beholder, (int) (Math.random() * 3) + 6), true);
                    getMap().broadcastMessage(Character.this, PacketCreator.showBuffEffect(getId(), beholder, 2), false);
                }, buffInterval, buffInterval);
            }
        } else if (effect.isRecovery()) {
            int healInterval = (GameConfig.getServerBoolean("use_ultra_recovery")) ? 2000 : 5000;
            final byte heal = (byte) effect.getX();

            chrLock.lock();
            try {
                if (recoveryTask != null) {
                    recoveryTask.cancel(false);
                }

                recoveryTask = TimerManager.getInstance().register(() -> {
                    if (getBuffSource(BuffStat.RECOVERY) == -1) {
                        chrLock.lock();
                        try {
                            if (recoveryTask != null) {
                                recoveryTask.cancel(false);
                                recoveryTask = null;
                            }
                        } finally {
                            chrLock.unlock();
                        }

                        return;
                    }

                    addHP(heal);
                    sendPacket(PacketCreator.showOwnRecovery(heal));
                    getMap().broadcastMessage(Character.this, PacketCreator.showRecovery(id, heal), false);
                }, healInterval, healInterval);
            } finally {
                chrLock.unlock();
            }
        } else if (effect.getHpRRate() > 0 || effect.getMpRRate() > 0) {
            if (effect.getHpRRate() > 0) {
                extraHpRec = effect.getHpR();
                extraRecInterval = effect.getHpRRate();
            }

            if (effect.getMpRRate() > 0) {
                extraMpRec = effect.getMpR();
                extraRecInterval = effect.getMpRRate();
            }

            chrLock.lock();
            try {
                stopExtraTask();
                startExtraTask(extraHpRec, extraMpRec, extraRecInterval);   // HP & MP sharing the same task holder
            } finally {
                chrLock.unlock();
            }

        } else if (effect.isMapChair()) {
            startChairTask();
        }

        prtLock.lock();
        effLock.lock();
        chrLock.lock();
        try {
            Integer sourceid = effect.getBuffSourceId();
            Map<BuffStat, BuffStatValueHolder> toDeploy;
            Map<BuffStat, BuffStatValueHolder> appliedStatups = new LinkedHashMap<>();

            for (Pair<BuffStat, Integer> ps : effect.getStatups()) {
                appliedStatups.put(ps.getLeft(), new BuffStatValueHolder(effect, starttime, ps.getRight()));
            }

            boolean active = effect.isActive(this);
            if (GameConfig.getServerBoolean("use_buff_most_significant")) {
                toDeploy = new LinkedHashMap<>();
                Map<Integer, Pair<StatEffect, Long>> retrievedEffects = new LinkedHashMap<>();
                Set<BuffStat> retrievedStats = new LinkedHashSet<>();
                for (Entry<BuffStat, BuffStatValueHolder> statup : appliedStatups.entrySet()) {
                    BuffStatValueHolder mbsvh = effects.get(statup.getKey());
                    BuffStatValueHolder statMbsvh = statup.getValue();

                    if (active) {
                        if (mbsvh == null || mbsvh.value < statMbsvh.value || (mbsvh.value == statMbsvh.value && mbsvh.effect.getStatups().size() <= statMbsvh.effect.getStatups().size())) {
                            toDeploy.put(statup.getKey(), statMbsvh);
                        } else {
                            if (!isSingletonStatup(statup.getKey())) {
                                for (Pair<BuffStat, Integer> mbs : mbsvh.effect.getStatups()) {
                                    retrievedStats.add(mbs.getLeft());
                                }
                            }
                        }
                    }

                    addItemEffectHolderCount(statup.getKey());
                }

                // should also propagate update from buffs shared with priority sourceids
                Set<BuffStat> updated = appliedStatups.keySet();
                for (BuffStatValueHolder mbsvh : this.getAllStatups()) {
                    if (isPriorityBuffSourceId(mbsvh.effect.getBuffSourceId())) {
                        for (Pair<BuffStat, Integer> p : mbsvh.effect.getStatups()) {
                            if (updated.contains(p.getLeft())) {
                                retrievedStats.add(p.getLeft());
                            }
                        }
                    }
                }

                if (!isSilent) {
                    addItemEffectHolder(sourceid, expirationtime, appliedStatups);
                    effects.putAll(toDeploy);

                    if (active) {
                        retrievedEffects.put(sourceid, new Pair<>(effect, starttime));
                    }

                    propagateBuffEffectUpdates(retrievedEffects, retrievedStats, new LinkedHashSet<>());
                }
            } else {
                for (Entry<BuffStat, BuffStatValueHolder> statup : appliedStatups.entrySet()) {
                    addItemEffectHolderCount(statup.getKey());
                }

                toDeploy = (active ? appliedStatups : new LinkedHashMap<>());
            }

            addItemEffectHolder(sourceid, expirationtime, appliedStatups);
            effects.putAll(toDeploy);
        } finally {
            chrLock.unlock();
            effLock.unlock();
            prtLock.unlock();
        }

        updateLocalStats();
    }

    private static int getJobMapChair(Job job) {
        return switch (job.getId() / 1000) {
            case 0 -> Beginner.MAP_CHAIR;
            case 1 -> Noblesse.MAP_CHAIR;
            default -> Legend.MAP_CHAIR;
        };
    }

    public boolean unregisterChairBuff() {
        if (!GameConfig.getServerBoolean("use_chair_extra_heal")) {
            return false;
        }

        int skillId = getJobMapChair(job);
        int skillLv = getSkillLevel(skillId);
        if (skillLv > 0) {
            StatEffect mapChairSkill = SkillFactory.getSkill(skillId).getEffect(skillLv);
            return cancelEffect(mapChairSkill, false, -1);
        }

        return false;
    }

    public boolean registerChairBuff() {
        if (!GameConfig.getServerBoolean("use_chair_extra_heal")) {
            return false;
        }

        int skillId = getJobMapChair(job);
        int skillLv = getSkillLevel(skillId);
        if (skillLv > 0) {
            StatEffect mapChairSkill = SkillFactory.getSkill(skillId).getEffect(skillLv);
            mapChairSkill.applyTo(this);
            return true;
        }

        return false;
    }

    public int getChair() {
        return chair.get();
    }

    public String getChalkboard() {
        return this.chalktext;
    }

    public AbstractPlayerInteraction getAbstractPlayerInteraction() {
        return client.getAbstractPlayerInteraction();
    }

    private List<QuestStatus> getQuestValues() {
        synchronized (quests) {
            return new ArrayList<>(quests.values());
        }
    }

    public final List<QuestStatus> getCompletedQuests() {
        List<QuestStatus> ret = new LinkedList<>();
        for (QuestStatus qs : getQuestValues()) {
            if (qs.getStatus().equals(QuestStatus.Status.COMPLETED)) {
                ret.add(qs);
            }
        }

        return Collections.unmodifiableList(ret);
    }

    public List<Ring> getCrushRings() {
        Collections.sort(crushRings);
        return crushRings;
    }

    public Collection<Door> getDoors() {
        prtLock.lock();
        try {
            return (party != null ? Collections.unmodifiableCollection(party.getDoors().values()) : (pdoor != null ? Collections.singleton(pdoor) : new LinkedHashSet<>()));
        } finally {
            prtLock.unlock();
        }
    }

    public Door getPlayerDoor() {
        prtLock.lock();
        try {
            return pdoor;
        } finally {
            prtLock.unlock();
        }
    }

    public Door getMainTownDoor() {
        for (Door door : getDoors()) {
            if (door.getTownPortal().getId() == 0x80) {
                return door;
            }
        }

        return null;
    }

    public void applyPartyDoor(Door door, boolean partyUpdate) {
        Party chrParty;
        prtLock.lock();
        try {
            if (!partyUpdate) {
                pdoor = door;
            }

            chrParty = getParty();
            if (chrParty != null) {
                chrParty.addDoor(id, door);
            }
        } finally {
            prtLock.unlock();
        }

        silentPartyUpdateInternal(chrParty);
    }

    public Door removePartyDoor(boolean partyUpdate) {
        Door ret = null;
        Party chrParty;

        prtLock.lock();
        try {
            chrParty = getParty();
            if (chrParty != null) {
                chrParty.removeDoor(id);
            }

            if (!partyUpdate) {
                ret = pdoor;
                pdoor = null;
            }
        } finally {
            prtLock.unlock();
        }

        silentPartyUpdateInternal(chrParty);
        return ret;
    }

    private void removePartyDoor(Party formerParty) {    // player is no longer registered at this party
        formerParty.removeDoor(id);
    }

    public EventInstanceManager getEventInstance() {
        evtLock.lock();
        try {
            return eventInstance;
        } finally {
            evtLock.unlock();
        }
    }

    public Marriage getMarriageInstance() {
        return (Marriage) getEventInstance();
    }

    public void resetExcluded(int petId) {
        chrLock.lock();
        try {
            Set<Integer> petExclude = excluded.get(petId);

            if (petExclude != null) {
                petExclude.clear();
            } else {
                excluded.put(petId, new LinkedHashSet<>());
            }
        } finally {
            chrLock.unlock();
        }
    }

    public void addExcluded(int petId, int x) {
        chrLock.lock();
        try {
            excluded.get(petId).add(x);
        } finally {
            chrLock.unlock();
        }
    }

    public void commitExcludedItems() {
        Map<Integer, Set<Integer>> petExcluded = this.getExcluded();

        chrLock.lock();
        try {
            excludedItems.clear();
        } finally {
            chrLock.unlock();
        }

        for (Map.Entry<Integer, Set<Integer>> pe : petExcluded.entrySet()) {
            byte petIndex = this.getPetIndex(pe.getKey());
            if (petIndex < 0) {
                continue;
            }

            Set<Integer> exclItems = pe.getValue();
            if (!exclItems.isEmpty()) {
                sendPacket(PacketCreator.loadExceptionList(this.getId(), pe.getKey(), petIndex, new ArrayList<>(exclItems)));

                chrLock.lock();
                try {
                    excludedItems.addAll(exclItems);
                } finally {
                    chrLock.unlock();
                }
            }
        }
    }

    public void exportExcludedItems(Client c) {
        Map<Integer, Set<Integer>> petExcluded = this.getExcluded();
        for (Map.Entry<Integer, Set<Integer>> pe : petExcluded.entrySet()) {
            byte petIndex = this.getPetIndex(pe.getKey());
            if (petIndex < 0) {
                continue;
            }

            Set<Integer> exclItems = pe.getValue();
            if (!exclItems.isEmpty()) {
                c.sendPacket(PacketCreator.loadExceptionList(this.getId(), pe.getKey(), petIndex, new ArrayList<>(exclItems)));
            }
        }
    }

    public Map<Integer, Set<Integer>> getExcluded() {
        chrLock.lock();
        try {
            return Collections.unmodifiableMap(excluded);
        } finally {
            chrLock.unlock();
        }
    }

    public Set<Integer> getExcludedItems() {
        chrLock.lock();
        try {
            return Collections.unmodifiableSet(excludedItems);
        } finally {
            chrLock.unlock();
        }
    }

    public int getExp() {
        return exp.get();
    }

    public int getGachaExp() {
        return gachaExp.get();
    }

    public boolean hasNoviceExpRate() {
        return GameConfig.getServerBoolean("use_enforce_novice_exp_rate") && isBeginnerJob() && level < 11;
    }

    public float getExpRate() {
        if (hasNoviceExpRate()) {   // base exp rate 1x for early levels idea thanks to Vcoc
            return 1;
        }

        return expRate;
    }

    public float getLevelExpRate() {
        if (hasNoviceExpRate()) return 1; // 新手经验保护

        return 1f + GameConfig.getWorldFloat(getWorld(), "level_exp_rate") * level;
    }

    public float getQuickLevelExpRate() {
        if (hasNoviceExpRate()) return 1; // 新手经验保护

        int quickLv = GameConfig.getWorldInt(getWorld(), "quick_level");
        if (level >= quickLv) return 1;

        return 1f + (quickLv - level) * GameConfig.getWorldFloat(getWorld(), "quick_level_rate");
    }

    public void updateMobExpRate() {
        mobExpRate = getLevelExpRate() * getQuickLevelExpRate();
    }

    public float getMobExpRate() {
        if (mobExpRate <= 0) updateMobExpRate();
        return mobExpRate;
    }

    public int getCouponExpRate() {
        return expCoupon;
    }

    public float getRawExpRate() {
        return expRate / (expCoupon * getWorldServer().getExpRate());
    }

    public int getCouponDropRate() {
        return dropCoupon;
    }

    public float getRawDropRate() {
        return dropRate / (dropCoupon * getWorldServer().getDropRate());
    }

    public float getBossDropRate() {
        World w = getWorldServer();
        return (dropRate / w.getDropRate()) * w.getBossDropRate();
    }

    public int getCouponMesoRate() {
        return mesoCoupon;
    }

    public float getRawMesoRate() {
        return mesoRate / (mesoCoupon * getWorldServer().getMesoRate());
    }

    public float getQuestExpRate() {
        if (hasNoviceExpRate()) {
            return 1;
        }

        World w = getWorldServer();
        return w.getExpRate() * w.getQuestRate();
    }

    public float getQuestMesoRate() {
        World w = getWorldServer();
        return w.getMesoRate() * w.getQuestRate();
    }

    public float getCardRate(int itemid) {
        float rate = 100.0f;

        if (itemid == 0) {
            StatEffect mseMeso = getBuffEffect(BuffStat.MESO_UP_BY_ITEM);
            if (mseMeso != null) {
                rate += mseMeso.getCardRate(mapId, itemid);
            }
        } else {
            StatEffect mseItem = getBuffEffect(BuffStat.ITEM_UP_BY_ITEM);
            if (mseItem != null) {
                rate += mseItem.getCardRate(mapId, itemid);
            }
        }

        return rate / 100;
    }

    public float getTotalMesoRate() {
        return getCouponMesoRate() * getCardRate(0);
    }

    public float getTotalDropRate() {
        return getDropRate() * getFamilyDrop() * getCardRate(1);
    }

    public float getTotalBossDropRate() {
        return getBossDropRate() * getCardRate(1);
    }
    public Family getFamily() {
        if (familyEntry != null) {
            return familyEntry.getFamily();
        } else {
            return null;
        }
    }

    public void setFamilyEntry(FamilyEntry entry) {
        if (entry != null) {
            setFamilyId(entry.getFamily().getID());
        }
        this.familyEntry = entry;
    }

    public void setUsedStorage() {
        usedStorage = true;
    }

    public List<Ring> getFriendshipRings() {
        Collections.sort(friendshipRings);
        return friendshipRings;
    }

    public boolean isMale() {
        return getGender() == 0;
    }

    public Guild getGuild() {
        try {
            return Server.getInstance().getGuild(getGuildId(), getWorld(), this);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public Alliance getAlliance() {
        if (mgc != null) {
            try {
                return Server.getInstance().getAlliance(getGuild().getAllianceId());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }


    public static int getAccountIdByName(String name) {
        try {
            return characterService.getAccountIdByName(name);
        } catch (Exception e) {
            log.error("根据角色名 {} 查询账号ID时出错", name, e);
            return -1;
        }
    }

    public static int getIdByName(String name) {
        try {
            return characterService.getIdByName(name);
        } catch (Exception e) {
            log.error("根据角色名 {} 查询角色ID时出错", name, e);
            return -1;
        }
    }

    public static String getNameById(int id) {
        try {
            return characterService.getNameById(id);
        } catch (Exception e) {
            log.error("根据角色ID {} 查询角色名时出错", id, e);
            return null;
        }
    }

    public Inventory getInventory(InventoryType type) {
        return inventory[type.ordinal()];
    }

    public boolean haveItemWithId(int itemid, boolean checkEquipped) {
        return (inventory[ItemConstants.getInventoryType(itemid).ordinal()].findById(itemid) != null)
                || (checkEquipped && inventory[InventoryType.EQUIPPED.ordinal()].findById(itemid) != null);
    }

    public boolean haveItemEquipped(int itemid) {
        return (inventory[InventoryType.EQUIPPED.ordinal()].findById(itemid) != null);
    }

    public boolean haveWeddingRing() {
        int[] rings = {ItemId.WEDDING_RING_STAR, ItemId.WEDDING_RING_MOONSTONE, ItemId.WEDDING_RING_GOLDEN, ItemId.WEDDING_RING_SILVER};

        for (int ringid : rings) {
            if (haveItemWithId(ringid, true)) {
                return true;
            }
        }

        return false;
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int count = inventory[ItemConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            count += inventory[InventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return count;
    }

    public int getCleanItemQuantity(int itemid, boolean checkEquipped) {
        int count = inventory[ItemConstants.getInventoryType(itemid).ordinal()].countNotOwnedById(itemid);
        if (checkEquipped) {
            count += inventory[InventoryType.EQUIPPED.ordinal()].countNotOwnedById(itemid);
        }
        return count;
    }

    public int getJobType() {
        return job.getId() / 1000;
    }

    public int getFh() {
        Point pos = this.getPosition();
        pos.y -= 6;

        if (map.getFootholds().findBelow(pos) == null) {
            return 0;
        } else {
            return map.getFootholds().findBelow(pos).getY1();
        }
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapId;
    }

    public Ring getMarriageRing() {
        return partnerId > 0 ? marriageRing : null;
    }

    public int getMasterLevel(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.masterLevel;
    }

    public int getMasterLevel(Skill skill) {
        if (skills.get(skill) == null) {
            return 0;
        }
        return skills.get(skill).masterLevel;
    }

    public int getTotalStr() {
        return localstr;
    }

    public int getTotalDex() {
        return localdex;
    }

    public int getTotalInt() {
        return localint_;
    }

    public int getTotalLuk() {
        return localluk;
    }

    public int getTotalMagic() {
        return localmagic;
    }

    public int getTotalWatk() {
        return localwatk;
    }

    public int getMaxClassLevel() {
        return isCygnus() ? 120 : 200;
    }

    public int getMaxLevel() {
        if (!GameConfig.getServerBoolean("use_enforce_job_level_range") || isGmJob()) {
            return getMaxClassLevel();
        }

        return GameConstants.getJobMaxLevel(job);
    }

    public int getMeso() {
        return meso.get();
    }

    public void setMeso(int meso) {
        this.meso.set(meso);
    }

    public int getMerchantMeso() {
        return merchantmeso;
    }

    public int getMerchantNetMeso() {
        int elapsedDays = 0;
        try {
            elapsedDays = characterService.getMerchantNetMeso(id);
        } catch (Exception e) {
            log.error("获取角色 {} 的商人净收益金币时出错", name, e);
        }

        if (elapsedDays > 100) {
            elapsedDays = 100;
        }

        long netMeso = merchantmeso;
        netMeso = (netMeso * (100 - elapsedDays)) / 100;
        return (int) netMeso;
    }
    public GuildCharacter getMGC() {
        return mgc;
    }

    public void setMGC(GuildCharacter mgc) {
        this.mgc = mgc;
    }

    public PartyCharacter getMPC() {
        if (mpc == null) {
            mpc = new PartyCharacter(this);
        }
        return mpc;
    }

    public void setMPC(PartyCharacter mpc) {
        this.mpc = mpc;
    }

    public void setPlayerAggro(int mobHash) {
        setTargetHpBarHash(mobHash);
        setTargetHpBarTime(System.currentTimeMillis());
    }

    public void resetPlayerAggro() {
        if (getWorldServer().unregisterDisabledServerMessage(id)) {
            client.announceServerMessage();
        }

        setTargetHpBarHash(0);
        setTargetHpBarTime(0);
    }

    public int getMiniGamePoints(MiniGameResult type, boolean omok) {
        if (omok) {
            return switch (type) {
                case WIN -> omokwins;
                case LOSS -> omoklosses;
                default -> omokties;
            };
        } else {
            return switch (type) {
                case WIN -> matchcardwins;
                case LOSS -> matchcardlosses;
                default -> matchcardties;
            };
        }
    }

    public int getMonsterBookCover() {
        return bookCover;
    }

    public int getNoPets() {
        petLock.lock();
        try {
            int ret = 0;
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    ret++;
                }
            }
            return ret;
        } finally {
            petLock.unlock();
        }
    }

    public Party getParty() {
        prtLock.lock();
        try {
            return party;
        } finally {
            prtLock.unlock();
        }
    }

    public int getPartyId() {
        prtLock.lock();
        try {
            return (party != null ? party.getId() : -1);
        } finally {
            prtLock.unlock();
        }
    }

    public List<Character> getPartyMembersOnline() {
        List<Character> list = new LinkedList<>();

        prtLock.lock();
        try {
            if (party != null) {
                for (PartyCharacter mpc : party.getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        list.add(mc);
                    }
                }
            }
        } finally {
            prtLock.unlock();
        }

        return list;
    }

    public List<Character> getPartyMembersOnSameMap() {
        List<Character> list = new LinkedList<>();
        int thisMapHash = this.getMap().hashCode();

        prtLock.lock();
        try {
            if (party != null) {
                for (PartyCharacter mpc : party.getMembers()) {
                    Character chr = mpc.getPlayer();
                    if (chr != null) {
                        MapleMap chrMap = chr.getMap();
                        if (chrMap != null && chrMap.hashCode() == thisMapHash && chr.isLoggedInWorld()) {
                            list.add(chr);
                        }
                    }
                }
            }
        } finally {
            prtLock.unlock();
        }

        return list;
    }

    public boolean isPartyMember(Character chr) {
        return isPartyMember(chr.getId());
    }

    public boolean isPartyMember(int cid) {
        prtLock.lock();
        try {
            if (party != null) {
                return party.getMemberById(cid) != null;
            }
        } finally {
            prtLock.unlock();
        }

        return false;
    }

    public void setGMLevel(int level) {
        this.gmLevel = Math.max(Math.min(level, 6), 0);
        whiteChat = gmLevel >= 4;   // thanks ozanrijen for suggesting default white chat
    }

    public void closePartySearchInteractions() {
        this.getWorldServer().getPartySearchCoordinator().unregisterPartyLeader(this);
        if (canRecvPartySearchInvite) {
            this.getWorldServer().getPartySearchCoordinator().detachPlayer(this);
        }
    }

    public void closePlayerInteractions() {
        closeNpcShop();
        closeTrade();
        closePlayerShop();
        closeMiniGame(true);
        closeRPS();
        closeHiredMerchant(false);
        closePlayerMessenger();

        client.closePlayerScriptInteractions();
        resetPlayerAggro();
    }

    public void closeNpcShop() {
        setShop(null);
    }

    public void closeTrade() {
        Trade.cancelTrade(this, Trade.TradeResult.PARTNER_CANCEL);
    }

    public void closePlayerShop() {
        PlayerShop mps = this.getPlayerShop();
        if (mps == null) {
            return;
        }

        if (mps.isOwner(this)) {
            mps.setOpen(false);
            getWorldServer().unregisterPlayerShop(mps);

            for (PlayerShopItem mpsi : mps.getItems()) {
                if (mpsi.getBundles() >= 2) {
                    Item iItem = mpsi.getItem().copy();
                    iItem.setQuantity((short) (mpsi.getBundles() * iItem.getQuantity()));
                    InventoryManipulator.addFromDrop(this.getClient(), iItem, false);
                } else if (mpsi.isExist()) {
                    InventoryManipulator.addFromDrop(this.getClient(), mpsi.getItem(), true);
                }
            }
            mps.closeShop();
        } else {
            mps.removeVisitor(this);
        }
        this.setPlayerShop(null);
    }

    public void closeMiniGame(boolean forceClose) {
        MiniGame game = this.getMiniGame();
        if (game == null) {
            return;
        }

        if (game.isOwner(this)) {
            game.closeRoom(forceClose);
        } else {
            game.removeVisitor(forceClose, this);
        }
    }

    public void closeHiredMerchant(boolean closeMerchant) {
        HiredMerchant merchant = this.getHiredMerchant();
        if (merchant == null) {
            return;
        }

        if (closeMerchant) {
            if (merchant.isOwner(this) && merchant.getItems().isEmpty()) {
                merchant.forceClose();
            } else {
                merchant.removeVisitor(this);
                this.setHiredMerchant(null);
            }
        } else {
            if (merchant.isOwner(this)) {
                merchant.setOpen(true);
            } else {
                merchant.removeVisitor(this);
            }
            try {
                merchant.saveItems(false);
            } catch (Exception e) {
                log.error(I18nUtil.getLogMessage("Character.closeHiredMerchant.error1") + "{}", name, e);
            }
        }
    }

    public void closePlayerMessenger() {
        Messenger m = this.getMessenger();
        if (m == null) {
            return;
        }

        World w = getWorldServer();
        w.leaveMessenger(m.getId(), new MessengerCharacter(this, this.getMessengerPosition()));
        this.setMessenger(null);
        this.setMessengerPosition(4);
    }

    public Pet[] getPets() {
        petLock.lock();
        try {
            return Arrays.copyOf(pets, pets.length);
        } finally {
            petLock.unlock();
        }
    }

    public Pet getPet(int index) {
        if (index < 0) {
            return null;
        }

        petLock.lock();
        try {
            return pets[index];
        } finally {
            petLock.unlock();
        }
    }

    public byte getPetIndex(int petId) {
        petLock.lock();
        try {
            for (byte i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == petId) {
                        return i;
                    }
                }
            }
            return -1;
        } finally {
            petLock.unlock();
        }
    }

    public byte getPetIndex(Pet pet) {
        petLock.lock();
        try {
            for (byte i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == pet.getUniqueId()) {
                        return i;
                    }
                }
            }
            return -1;
        } finally {
            petLock.unlock();
        }
    }

    public final byte getQuestStatus(final int quest) {
        synchronized (quests) {
            QuestStatus mqs = quests.get((short) quest);
            if (mqs != null) {
                return (byte) mqs.getStatus().getId();
            } else {
                return 0;
            }
        }
    }

    public QuestStatus getQuest(final int quest) {
        return getQuest(Quest.getInstance(quest));
    }

    public QuestStatus getQuest(Quest quest) {
        synchronized (quests) {
            short questid = quest.getId();
            QuestStatus qs = quests.get(questid);
            if (qs == null) {
                qs = new QuestStatus(quest, QuestStatus.Status.NOT_STARTED);
                quests.put(questid, qs);
            }
            return qs;
        }
    }

    public final QuestStatus getQuestNAdd(final Quest quest) {
        synchronized (quests) {
            if (!quests.containsKey(quest.getId())) {
                final QuestStatus status = new QuestStatus(quest, QuestStatus.Status.NOT_STARTED);
                quests.put(quest.getId(), status);
                return status;
            }
            return quests.get(quest.getId());
        }
    }

    public final QuestStatus getQuestNoAdd(final Quest quest) {
        synchronized (quests) {
            return quests.get(quest.getId());
        }
    }

    public boolean needQuestItem(int questid, int itemid) {
        if (questid <= 0) { //For non quest items :3
            return true;
        }

        int amountNeeded, questStatus = this.getQuestStatus(questid);
        if (questStatus == 0) {
            amountNeeded = Quest.getInstance(questid).getStartItemAmountNeeded(itemid);
            if (amountNeeded == Integer.MIN_VALUE) {
                return false;
            }
        } else if (questStatus != 1) {
            return false;
        } else {
            amountNeeded = Quest.getInstance(questid).getCompleteItemAmountNeeded(itemid);
            if (amountNeeded == Integer.MAX_VALUE) {
                return true;
            }
        }

        return getInventory(ItemConstants.getInventoryType(itemid)).countById(itemid) < amountNeeded;
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = null;
    }

    public int peekSavedLocation(String type) {
        SavedLocation sl = savedLocations[SavedLocationType.fromString(type).ordinal()];
        if (sl == null) {
            return -1;
        }
        return sl.getMapId();
    }

    public int getSavedLocation(String type) {
        int m = peekSavedLocation(type);
        clearSavedLocation(SavedLocationType.fromString(type));

        return m;
    }

    public Map<Skill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public Map<Skill, SkillEntry> getEditableSkills() {
        return skills;
    }

    public int getSkillLevel(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.skillLevel;
    }

    public byte getSkillLevel(Skill skill) {
        if (skills.get(skill) == null) {
            return 0;
        }
        return skills.get(skill).skillLevel;
    }

    public long getSkillExpiration(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return -1;
        }
        return ret.expiration;
    }

    public long getSkillExpiration(Skill skill) {
        if (skills.get(skill) == null) {
            return -1;
        }
        return skills.get(skill).expiration;
    }

    public int getSlot() {
        return slots;
    }

    public final List<QuestStatus> getStartedQuests() {
        List<QuestStatus> ret = new LinkedList<>();
        for (QuestStatus qs : getQuestValues()) {
            if (QuestStatus.Status.STARTED.equals(qs.getStatus())) {
                ret.add(qs);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public StatEffect getStatForBuff(BuffStat effect) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return null;
            }
            return mbsvh.effect;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public Collection<Summon> getSummonsValues() {
        return summons.values();
    }

    public void clearSummons() {
        summons.clear();
    }

    public Summon getSummonByKey(int id) {
        return summons.get(id);
    }

    public boolean isSummonsEmpty() {
        return summons.isEmpty();
    }

    public boolean containsSummon(Summon summon) {
        return summons.containsValue(summon);
    }

    public MapObject[] getVisibleMapObjects() {
        return visibleMapObjects.toArray(new MapObject[visibleMapObjects.size()]);
    }

    public World getWorldServer() {
        return Server.getInstance().getWorld(world);
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        if (skillid == 5221999) {
            this.battleshipHp = (int) length;
            addCooldown(skillid, 0, length);
        } else {
            long timeNow = Server.getInstance().getCurrentTime();
            int time = (int) ((length + starttime) - timeNow);
            addCooldown(skillid, timeNow, time);
        }
    }

    public int gmLevel() {
        return gmLevel;
    }

    private void guildUpdate() {
        mgc.setLevel(level);
        mgc.setJobId(job.getId());

        if (this.guildId < 1) {
            return;
        }

        try {
            Server.getInstance().memberLevelJobUpdate(this.mgc);
            //Server.getInstance().getGuild(guildid, world, mgc).gainGP(40);
            int allianceId = getGuild().getAllianceId();
            if (allianceId > 0) {
                Server.getInstance().allianceMessage(allianceId, GuildPackets.updateAllianceJobLevel(this), getId(), -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleEnergyChargeGain() { // to get here energychargelevel has to be > 0
        Skill energycharge = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory.getSkill(Marauder.ENERGY_CHARGE);
        StatEffect ceffect;
        ceffect = energycharge.getEffect(getSkillLevel(energycharge));
        TimerManager tMan = TimerManager.getInstance();
        if (energyBar < 10000) {
            energyBar += 102;
            if (energyBar > 10000) {
                energyBar = 10000;
            }
            List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.ENERGY_CHARGE, energyBar));
            setBuffedValue(BuffStat.ENERGY_CHARGE, energyBar);
            sendPacket(PacketCreator.giveBuff(energyBar, 0, stat));
            sendPacket(PacketCreator.showOwnBuffEffect(energycharge.getId(), 2));
            getMap().broadcastPacket(this, PacketCreator.showBuffEffect(id, energycharge.getId(), 2));
            getMap().broadcastPacket(this, PacketCreator.giveForeignPirateBuff(id, energycharge.getId(),
                    ceffect.getDuration(), stat));
        }
        if (energyBar >= 10000 && energyBar < 11000) {
            energyBar = 15000;
            final Character chr = this;
            tMan.schedule(() -> {
                energyBar = 0;
                List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.ENERGY_CHARGE, energyBar));
                setBuffedValue(BuffStat.ENERGY_CHARGE, energyBar);
                sendPacket(PacketCreator.giveBuff(energyBar, 0, stat));
                getMap().broadcastPacket(chr, PacketCreator.cancelForeignFirstDebuff(id, ((long) 1) << 50));
            }, ceffect.getDuration());
        }
    }

    public void handleOrbconsume() {
        int skillid = isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
        Skill combo = SkillFactory.getSkill(skillid);
        List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.COMBO, 1));
        setBuffedValue(BuffStat.COMBO, 1);
        sendPacket(PacketCreator.giveBuff(skillid, combo.getEffect(getSkillLevel(combo)).getDuration() + (int) ((getBuffedStarttime(BuffStat.COMBO) - System.currentTimeMillis())), stat));
        getMap().broadcastMessage(this, PacketCreator.giveForeignBuff(getId(), stat), false);
    }

    public boolean hasEntered(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntered(String script, int mapId) {
        String e = entered.get(mapId);
        return script.equals(e);
    }

    public void hasGivenFame(Character to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(to.getId());
        characterService.addFameLog(getId(), to.getId());
    }

    public boolean hasMerchant() {
        return hasMerchant;
    }

    public boolean haveItem(int itemid) {
        return getItemQuantity(itemid, ItemConstants.isEquipment(itemid)) > 0;
    }

    public boolean haveCleanItem(int itemid) {
        return getCleanItemQuantity(itemid, ItemConstants.isEquipment(itemid)) > 0;
    }

    public boolean hasEmptySlot(int itemId) {
        return getInventory(ItemConstants.getInventoryType(itemId)).getNextFreeSlot() > -1;
    }

    public boolean hasEmptySlot(byte invType) {
        return getInventory(InventoryType.getByType(invType)).getNextFreeSlot() > -1;
    }

    public void increaseGuildCapacity() {
        int cost = Guild.getIncreaseGuildCost(getGuild().getCapacity());

        if (getMeso() < cost) {
            dropMessage(1, I18nUtil.getMessage("Character.increaseGuildCapacity.message1"));
            return;
        }

        if (Server.getInstance().increaseGuildCapacity(guildId)) {
            gainMeso(-cost, true, false, true);
        } else {
            dropMessage(1, I18nUtil.getMessage("Character.increaseGuildCapacity.message2"));
        }
    }

    private static String getTimeRemaining(long timeLeft) {
        int seconds = (int) Math.floor((double) timeLeft / SECONDS.toMillis(1)) % 60;
        int minutes = (int) Math.floor((double) timeLeft / MINUTES.toMillis(1)) % 60;

        return (minutes > 0 ? (String.format("%02d", minutes) + " minutes, ") : "") + String.format("%02d", seconds) + " seconds";
    }

    public boolean isBuffFrom(BuffStat stat, Skill skill) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return false;
            }
            return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public boolean isGmJob() {
        int jn = job.getJobNiche();
        return jn >= 8 && jn <= 9;
    }

    public boolean isCygnus() {
        return getJobType() == 1;
    }

    public boolean isAran() {
        return job.getId() >= 2000 && job.getId() <= 2112;
    }

    public boolean isBeginnerJob() {
        return (job.getId() == 0 || job.getId() == 1000 || job.getId() == 2000);
    }

    public boolean isGM() {
        return gmLevel > 1;
    }

    public boolean isMapObjectVisible(MapObject mo) {
        return visibleMapObjects.contains(mo);
    }

    public boolean isPartyLeader() {
        prtLock.lock();
        try {
            Party party = getParty();
            return party != null && party.getLeaderId() == getId();
        } finally {
            prtLock.unlock();
        }
    }

    public boolean isGuildLeader() {    // true on guild master or jr. master
        return guildId > 0 && guildRank < 3;
    }

    public boolean attemptCatchFish(int baitLevel) {
        return GameConfig.getServerBoolean("use_fishing_system") && MapId.isFishingArea(mapId) &&
                this.getPosition().getY() > 0 &&
                ItemConstants.isFishingChair(chair.get()) &&
                this.getWorldServer().registerFisherPlayer(this, baitLevel);
    }

    public void leaveMap() {
        releaseControlledMonsters();
        visibleMapObjects.clear();
        setChair(-1);
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }

        AriantColiseum arena = this.getAriantColiseum();
        if (arena != null) {
            arena.leaveArena(this);
        }
    }

    private int getChangedJobSp(Job newJob) {
        int curSp = getUsedSp(newJob) + getJobRemainingSp(newJob);
        int spGain = 0;
        int expectedSp = getJobLevelSp(level - 10, newJob, GameConstants.getJobBranch(newJob));
        if (curSp < expectedSp) {
            spGain += (expectedSp - curSp);
        }

        return getSpGain(spGain, curSp, newJob);
    }

    private int getUsedSp(Job job) {
        int jobId = job.getId();
        int spUsed = 0;

        for (Entry<Skill, SkillEntry> s : this.getSkills().entrySet()) {
            Skill skill = s.getKey();
            if (GameConstants.isInJobTree(skill.getId(), jobId) && !skill.isBeginnerSkill()) {
                spUsed += s.getValue().skillLevel;
            }
        }

        return spUsed;
    }

    private int getJobLevelSp(int level, Job job, int jobBranch) {
        if (Job.getJobStyleInternal(job.getId(), (byte) 0x40) == Job.MAGICIAN) {
            level += 2;  // starts earlier, level 8
        }

        return 3 * level + GameConstants.getChangeJobSpUpgrade(jobBranch);
    }

    private int getJobMaxSp(Job job) {
        int jobBranch = GameConstants.getJobBranch(job);
        int jobRange = GameConstants.getJobUpgradeLevelRange(jobBranch);
        return getJobLevelSp(jobRange, job, jobBranch);
    }

    private int getJobRemainingSp(Job job) {
        int skillBook = GameConstants.getSkillBook(job.getId());

        int ret = 0;
        for (int i = 0; i <= skillBook; i++) {
            ret += this.getRemainingSp(i);
        }

        return ret;
    }

    private int getSpGain(int spGain, Job job) {
        int curSp = getUsedSp(job) + getJobRemainingSp(job);
        return getSpGain(spGain, curSp, job);
    }

    private int getSpGain(int spGain, int curSp, Job job) {
        int maxSp = getJobMaxSp(job);
        return Math.min(spGain, maxSp - curSp);
    }

    private void levelUpGainSp() {
        if (GameConstants.getJobBranch(job) == 0) {
            return;
        }

        int spGain = GameConfig.getServerInt("level_up_sp_gain");
        if (GameConfig.getServerBoolean("use_enforce_job_sp_range") && !GameConstants.hasSPTable(job)) {
            spGain = getSpGain(spGain, job);
        }

        if (spGain > 0) {
            gainSp(spGain, GameConstants.getSkillBook(job.getId()), true);
        }
    }

    public synchronized void levelUp(boolean takeexp) {
        Skill improvingMaxHP = null;
        Skill improvingMaxMP = null;
        int improvingMaxHPLevel = 0;
        int improvingMaxMPLevel = 0;

        boolean isBeginner = isBeginnerJob();
        if (GameConfig.getServerBoolean("use_auto_assign_starters_ap") && isBeginner && level < 11) {
            effLock.lock();
            statWlock.lock();
            try {
                gainAp(5, true);

                int str = 0, dex = 0;
                if (level < 6) {
                    str += 5;
                } else {
                    str += 4;
                    dex += 1;
                }

                assignStrDexIntLuk(str, dex, 0, 0);
            } finally {
                statWlock.unlock();
                effLock.unlock();
            }
        } else {
            int remainingAp = GameConfig.getServerInt("level_up_ap_gain");

            if (isCygnus()) {
                if (level > 10) {
                    if (level <= 17) {
                        remainingAp += 2;
                    } else if (level < 77) {
                        remainingAp++;
                    }
                }
            }

            gainAp(remainingAp, true);
        }

        int addhp = 0, addmp = 0;
        if (isBeginner) {
            addhp += Randomizer.rand(12, 16);
            addmp += Randomizer.rand(10, 12);
        } else if (job.isA(Job.WARRIOR) || job.isA(Job.DAWNWARRIOR1)) {
            improvingMaxHP = isCygnus() ? SkillFactory.getSkill(DawnWarrior.MAX_HP_INCREASE) : SkillFactory.getSkill(Warrior.IMPROVED_MAXHP);
            if (job.isA(Job.CRUSADER)) {
                improvingMaxMP = SkillFactory.getSkill(1210000);
            } else if (job.isA(Job.DAWNWARRIOR2)) {
                improvingMaxMP = SkillFactory.getSkill(11110000);
            }
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            addhp += Randomizer.rand(24, 28);
            addmp += Randomizer.rand(4, 6);
        } else if (job.isA(Job.MAGICIAN) || job.isA(Job.BLAZEWIZARD1)) {
            improvingMaxMP = isCygnus() ? SkillFactory.getSkill(BlazeWizard.INCREASING_MAX_MP) : SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE);
            improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
            addhp += Randomizer.rand(10, 14);
            addmp += Randomizer.rand(22, 24);
        } else if (job.isA(Job.BOWMAN) || job.isA(Job.THIEF) || (job.getId() > 1299 && job.getId() < 1500)) {
            addhp += Randomizer.rand(20, 24);
            addmp += Randomizer.rand(14, 16);
        } else if (job.isA(Job.GM)) {
            addhp += 30000;
            addmp += 30000;
        } else if (job.isA(Job.PIRATE) || job.isA(Job.THUNDERBREAKER1)) {
            improvingMaxHP = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.IMPROVE_MAX_HP) : SkillFactory.getSkill(Brawler.IMPROVE_MAX_HP);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            addhp += Randomizer.rand(22, 28);
            addmp += Randomizer.rand(18, 23);
        } else if (job.isA(Job.ARAN1)) {
            addhp += Randomizer.rand(44, 48);
            int aids = Randomizer.rand(4, 8);
            addmp += aids + (int) Math.floor(aids * 0.1);
        }
        if (improvingMaxHPLevel > 0 && (job.isA(Job.WARRIOR) || job.isA(Job.PIRATE) || job.isA(Job.DAWNWARRIOR1) || job.isA(Job.THUNDERBREAKER1))) {
            addhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
        }
        if (improvingMaxMPLevel > 0 && (job.isA(Job.MAGICIAN) || job.isA(Job.CRUSADER) || job.isA(Job.BLAZEWIZARD1))) {
            addmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
        }

        if (GameConfig.getServerBoolean("use_randomize_hpmp_gain")) {
            if (getJobStyle() == Job.MAGICIAN) {
                addmp += localint_ / 20;
            } else {
                addmp += localint_ / 10;
            }
        }

        addMaxMPMaxHP(addhp, addmp, true);

        if (takeexp) {
            exp.addAndGet(-ExpTable.getExpNeededForLevel(level));
            if (exp.get() < 0) {
                exp.set(0);
            }
        }

        level++;
        if (level >= getMaxClassLevel()) {
            exp.set(0);

            int maxClassLevel = getMaxClassLevel();
            if (level == maxClassLevel) {
                if (!this.isGM()) {
                    if (GameConfig.getServerBoolean("playernpc_auto_deploy")) {
                        ThreadManager.getInstance().newTask(() -> PlayerNPC.spawnPlayerNPC(GameConstants.getHallOfFameMapid(job), Character.this));
                    }

                    final String names = (getMedalText() + name);
                    getWorldServer().broadcastPacket(PacketCreator.serverNotice(6, String.format(ServerConstants.LEVEL_200, names, maxClassLevel, names)));
                }
            }

            level = maxClassLevel; //To prevent levels past the maximum
        }

        levelUpGainSp();

        effLock.lock();
        statWlock.lock();
        try {
            recalcLocalStats();
            changeHpMp(localMaxHp, localMaxMp, true);

            List<Pair<Stat, Integer>> statup = new ArrayList<>(10);
            statup.add(new Pair<>(Stat.AVAILABLEAP, remainingAp));
            statup.add(new Pair<>(Stat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job.getId())]));
            statup.add(new Pair<>(Stat.HP, hp));
            statup.add(new Pair<>(Stat.MP, mp));
            statup.add(new Pair<>(Stat.EXP, exp.get()));
            statup.add(new Pair<>(Stat.LEVEL, level));
            statup.add(new Pair<>(Stat.MAXHP, clientMaxHp));
            statup.add(new Pair<>(Stat.MAXMP, clientMaxMp));
            statup.add(new Pair<>(Stat.STR, attrStr));
            statup.add(new Pair<>(Stat.DEX, attrDex));

            sendPacket(PacketCreator.updatePlayerStats(statup, true, this));
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }

        getMap().broadcastMessage(this, PacketCreator.showForeignEffect(getId(), 0), false);
        setMPC(new PartyCharacter(this));
        silentPartyUpdate();

        if (this.guildId > 0) {
            getGuild().broadcast(PacketCreator.levelUpMessage(2, level, name), this.getId());
        }

        if (level % 20 == 0) {
            if (GameConfig.getServerBoolean("use_add_slots_by_level")) {
                if (!isGM()) {
                    for (byte i = 1; i < 5; i++) {
                        gainSlots(i, 4, true);
                    }

                    this.yellowMessage(I18nUtil.getMessage("Character.levelUp.USE_ADD_SLOTS_BY_LEVEL", level));
                }
            }
            if (GameConfig.getServerBoolean("use_add_rates_by_level")) { //For the rate upgrade
                revertLastPlayerRates();
                setPlayerRates();
                this.yellowMessage(I18nUtil.getMessage("Character.levelUp.USE_ADD_RATES_BY_LEVEL", level));
            }
        }

        if (GameConfig.getServerBoolean("use_perfect_pitch") && level >= 30) {
            //milestones?
            if (InventoryManipulator.checkSpace(client, ItemId.PERFECT_PITCH, (short) 1, "")) {
                InventoryManipulator.addById(client, ItemId.PERFECT_PITCH, (short) 1, "", -1);
            }
        } else if (level == 10) {
            ThreadManager.getInstance().newTask(() -> {
                if (leaveParty()) {
                    showHint(I18nUtil.getMessage("Character.levelUp.LeaveStarterParty"));
                }
            });
        }

        guildUpdate();

        FamilyEntry familyEntry = getFamilyEntry();
        if (familyEntry != null) {
            familyEntry.giveReputationToSenior(GameConfig.getServerInt("family_rep_per_level_up"), true);
            FamilyEntry senior = familyEntry.getSenior();
            if (senior != null) { //only send the message to direct senior
                Character seniorChr = senior.getChr();
                if (seniorChr != null) {
                    seniorChr.sendPacket(PacketCreator.levelUpMessage(1, level, getName()));
                }
            }
        }

        updateMobExpRate();
    }

    public boolean leaveParty() {
        Party party;
        boolean partyLeader;

        prtLock.lock();
        try {
            party = getParty();
            partyLeader = isPartyLeader();
        } finally {
            prtLock.unlock();
        }

        if (party != null) {
            if (partyLeader) {
                party.assignNewLeader(client);
            }
            Party.leaveParty(party, client);

            return true;
        } else {
            return false;
        }
    }

    public void setPlayerRates() {
        applySavedRateOrElse("expRate", () -> this.expRate *= GameConstants.getPlayerBonusExpRate(this.level / 20));
        applySavedRateOrElse("mesoRate", () -> this.mesoRate *= GameConstants.getPlayerBonusMesoRate(this.level / 20));
        applySavedRateOrElse("dropRate", () -> this.dropRate *= GameConstants.getPlayerBonusDropRate(this.level / 20));
    }

    public void revertLastPlayerRates() {
        this.expRate /= GameConstants.getPlayerBonusExpRate((this.level - 1) / 20);
        this.mesoRate /= GameConstants.getPlayerBonusMesoRate((this.level - 1) / 20);
        this.dropRate /= GameConstants.getPlayerBonusDropRate((this.level - 1) / 20);
    }

    public void revertPlayerRates() {
        this.expRate /= GameConstants.getPlayerBonusExpRate(this.level / 20);
        this.mesoRate /= GameConstants.getPlayerBonusMesoRate(this.level / 20);
        this.dropRate /= GameConstants.getPlayerBonusDropRate(this.level / 20);
    }

    public void setWorldRates() {
        World worldz = getWorldServer();
        applySavedRateOrElse("expRate", () -> this.expRate *= worldz.getExpRate());
        applySavedRateOrElse("mesoRate", () -> this.mesoRate *= worldz.getMesoRate());
        applySavedRateOrElse("dropRate", () -> this.dropRate *= worldz.getDropRate());
    }

    public void revertWorldRates() {
        World worldz = getWorldServer();
        this.expRate /= worldz.getExpRate();
        this.mesoRate /= worldz.getMesoRate();
        this.dropRate /= worldz.getDropRate();
    }

    private void applySavedRateOrElse(String type, Runnable runnable) {
        ExtendValueDO extendValueDO = ExtendUtil.getExtendValue(String.valueOf(id), ExtendType.CHARACTER_EXTEND.getType(), type);

        if (extendValueDO == null) {
            runnable.run();
            return;
        }
        float savedRateValue = Float.parseFloat(extendValueDO.getExtendValue());
        switch (type) {
            case "expRate" -> this.expRate = savedRateValue;
            case "mesoRate" -> this.mesoRate = savedRateValue;
            case "dropRate" -> this.dropRate = savedRateValue;
        }
    }

    public void setCouponRates() {
        List<Integer> couponEffects;

        Collection<Item> cashItems = this.getInventory(InventoryType.CASH).list();
        chrLock.lock();
        try {
            setActiveCoupons(cashItems);
            couponEffects = activateCouponsEffects();
        } finally {
            chrLock.unlock();
        }

        for (Integer couponId : couponEffects) {
            commitBuffCoupon(couponId);
        }
    }

    private void revertCouponRates() {
        revertCouponsEffects();
    }

    public void updateCouponRates() {
        Inventory cashInv = this.getInventory(InventoryType.CASH);
        if (cashInv == null) {
            return;
        }

        effLock.lock();
        chrLock.lock();
        cashInv.lockInventory();
        try {
            revertCouponRates();
            setCouponRates();
        } finally {
            cashInv.unlockInventory();
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void resetPlayerRates() {
        expRate = 1;
        mesoRate = 1;
        dropRate = 1;

        expCoupon = 1;
        mesoCoupon = 1;
        dropCoupon = 1;
    }

    private int getCouponMultiplier(int couponId) {
        return activeCouponRates.get(couponId);
    }

    private void setExpCouponRate(int couponId, int couponQty) {
        this.expCoupon *= (getCouponMultiplier(couponId) * couponQty);
    }

    private void setDropCouponRate(int couponId, int couponQty) {
        this.dropCoupon *= (getCouponMultiplier(couponId) * couponQty);
        this.mesoCoupon *= (getCouponMultiplier(couponId) * couponQty);
    }

    private void revertCouponsEffects() {
        dispelBuffCoupons();

        this.expRate /= this.expCoupon;
        this.dropRate /= this.dropCoupon;
        this.mesoRate /= this.mesoCoupon;

        this.expCoupon = 1;
        this.dropCoupon = 1;
        this.mesoCoupon = 1;
    }

    private List<Integer> activateCouponsEffects() {
        List<Integer> toCommitEffect = new LinkedList<>();

        // 优先检查服务器是否允许使用倍率卡
        boolean allowExpMultiplier = GameConfig.getServerBoolean("allow_exp_multiplier_card");
        boolean allowDropMultiplier = GameConfig.getServerBoolean("allow_drop_multiplier_card");

        if (GameConfig.getServerBoolean("use_stack_coupon_rates")) {
            for (Entry<Integer, Integer> coupon : activeCoupons.entrySet()) {
                int couponId = coupon.getKey();
                int couponQty = coupon.getValue();

                // 根据是否允许使用决定是否添加效果
                if ((ItemConstants.isExpCoupon(couponId) && allowExpMultiplier) ||
                        (!ItemConstants.isExpCoupon(couponId) && allowDropMultiplier)) {
                    toCommitEffect.add(couponId);
                }

                if (ItemConstants.isExpCoupon(couponId)) {
                    setExpCouponRate(couponId, allowExpMultiplier ? couponQty : 0);
                } else {
                    setDropCouponRate(couponId, allowDropMultiplier ? couponQty : 0);
                }
            }
        } else {
            int maxExpRate = 1, maxDropRate = 1, maxExpCouponId = -1, maxDropCouponId = -1;

            for (Entry<Integer, Integer> coupon : activeCoupons.entrySet()) {
                int couponId = coupon.getKey();

                if (ItemConstants.isExpCoupon(couponId)) {
                    if (allowExpMultiplier && maxExpRate < getCouponMultiplier(couponId)) {
                        maxExpCouponId = couponId;
                        maxExpRate = getCouponMultiplier(couponId);
                    }
                } else {
                    if (allowDropMultiplier && maxDropRate < getCouponMultiplier(couponId)) {
                        maxDropCouponId = couponId;
                        maxDropRate = getCouponMultiplier(couponId);
                    }
                }
            }

            if (maxExpCouponId > -1 && allowExpMultiplier) {
                toCommitEffect.add(maxExpCouponId);
            }
            if (maxDropCouponId > -1 && allowDropMultiplier) {
                toCommitEffect.add(maxDropCouponId);
            }

            // 直接根据允许状态设置倍率
            this.expCoupon = allowExpMultiplier ? maxExpRate : 1;
            this.dropCoupon = allowDropMultiplier ? maxDropRate : 1;
            this.mesoCoupon = allowDropMultiplier ? maxDropRate : 1;
        }

        this.expRate *= this.expCoupon;
        this.dropRate *= this.dropCoupon;
        this.mesoRate *= this.mesoCoupon;

        return toCommitEffect;
    }

    private void setActiveCoupons(Collection<Item> cashItems) {
        activeCoupons.clear();
        activeCouponRates.clear();

        Map<Integer, Integer> coupons = Server.getInstance().getCouponRates();
        List<Integer> active = Server.getInstance().getActiveCoupons();

        for (Item it : cashItems) {
            if (ItemConstants.isRateCoupon(it.getItemId()) && active.contains(it.getItemId())) {
                Integer count = activeCoupons.get(it.getItemId());

                if (count != null) {
                    activeCoupons.put(it.getItemId(), count + 1);
                } else {
                    activeCoupons.put(it.getItemId(), 1);
                    activeCouponRates.put(it.getItemId(), coupons.get(it.getItemId()));
                }
            }
        }
    }

    private void commitBuffCoupon(int couponid) {
        if (!GameConfig.getServerBoolean("show_coupon_buff")) {
            return;
        }
        if (!isLoggedIn() || getCashShop().isOpened()) {
            return;
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        StatEffect mse = ii.getItemEffect(couponid);
        mse.applyTo(this);
    }

    public void dispelBuffCoupons() {
        List<BuffStatValueHolder> allBuffs = getAllStatups();

        for (BuffStatValueHolder mbsvh : allBuffs) {
            if (ItemConstants.isRateCoupon(mbsvh.effect.getSourceId())) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public Set<Integer> getActiveCoupons() {
        chrLock.lock();
        try {
            return Collections.unmodifiableSet(activeCoupons.keySet());
        } finally {
            chrLock.unlock();
        }
    }

    public void addPlayerRing(Ring ring) {
        int ringItemId = ring.getItemId();
        if (ItemId.isWeddingRing(ringItemId)) {
            this.marriageRing = ring;
        } else if (ring.getItemId() > 1112012) {
            this.friendshipRings.add(ring);
        } else {
            this.crushRings.add(ring);
        }
    }

    public static Character loadCharacterEntryFromDB(ResultSet rs, List<Item> equipped) {
        Character ret = new Character();

        try {
            ret.accountId = rs.getInt("accountid");
            ret.id = rs.getInt("id");
            ret.name = rs.getString("name");
            ret.gender = rs.getInt("gender");
            ret.skinColor = SkinColor.getById(rs.getInt("skincolor"));
            ret.face = rs.getInt("face");
            ret.hair = rs.getInt("hair");

            // skipping pets, probably unneeded here

            ret.level = rs.getInt("level");
            ret.job = Job.getById(rs.getInt("job"));
            ret.attrStr = rs.getInt("str");
            ret.attrDex = rs.getInt("dex");
            ret.attrInt = rs.getInt("int");
            ret.attrLuk = rs.getInt("luk");
            ret.hp = rs.getInt("hp");
            ret.setMaxHp(rs.getInt("maxhp"));
            ret.mp = rs.getInt("mp");
            ret.setMaxMp(rs.getInt("maxmp"));
            ret.remainingAp = rs.getInt("ap");
            ret.loadCharSkillPoints(rs.getString("sp").split(","));
            ret.exp.set(rs.getInt("exp"));
            ret.fame = rs.getInt("fame");
            ret.gachaExp.set(rs.getInt("gachaexp"));
            ret.mapId = rs.getInt("map");
            ret.initialSpawnPoint = rs.getInt("spawnpoint");
            ret.setGMLevel(rs.getInt("gm"));
            ret.world = rs.getByte("world");
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");

            if (equipped != null) {  // players can have no equipped items at all, ofc
                Inventory inv = ret.inventory[InventoryType.EQUIPPED.ordinal()];
                for (Item item : equipped) {
                    inv.addItemFromDB(item);
                }
            }
        } catch (Exception sqle) {
            sqle.printStackTrace();
        }

        return ret;
    }

    public Character generateCharacterEntry() {
        Character ret = new Character();

        ret.accountId = this.getAccountId();
        ret.id = this.getId();
        ret.name = this.getName();
        ret.gender = this.getGender();
        ret.skinColor = this.getSkinColor();
        ret.face = this.getFace();
        ret.hair = this.getHair();

        // skipping pets, probably unneeded here

        ret.level = this.getLevel();
        ret.job = this.getJob();
        ret.attrStr = this.getStr();
        ret.attrDex = this.getDex();
        ret.attrInt = this.getInt();
        ret.attrLuk = this.getLuk();
        ret.hp = this.getHp();
        ret.setMaxHp(this.getMaxHp());
        ret.mp = this.getMp();
        ret.setMaxMp(this.getMaxMp());
        ret.remainingAp = this.getRemainingAp();
        ret.setRemainingSp(this.getRemainingSps());
        ret.exp.set(this.getExp());
        ret.fame = this.getFame();
        ret.gachaExp.set(this.getGachaExp());
        ret.mapId = this.getMapId();
        ret.initialSpawnPoint = this.getInitialSpawnPoint();

        ret.inventory[InventoryType.EQUIPPED.ordinal()] = this.getInventory(InventoryType.EQUIPPED);

        ret.setGMLevel(this.gmLevel());
        ret.world = this.getWorld();
        ret.rank = this.getRank();
        ret.rankMove = this.getRankMove();
        ret.jobRank = this.getJobRank();
        ret.jobRankMove = this.getJobRankMove();

        return ret;
    }

    private void loadCharSkillPoints(String[] skillPoints) {
        int[] sps = new int[skillPoints.length];
        for (int i = 0; i < skillPoints.length; i++) {
            sps[i] = Integer.parseInt(skillPoints[i]);
        }

        setRemainingSp(sps);
    }

    public int getRemainingSp() {
        return getRemainingSp(job.getId()); //default
    }

    public void updateRemainingSp(int remainingSp) {
        updateRemainingSp(remainingSp, GameConstants.getSkillBook(job.getId()));
    }

    /**
     * 加载账号所有角色预览数据
     * @param accountId
     * @return
     */
    public static List<Character> fromCharactersViewList(int worldId,int accountId) {
        List<Character> CharsList = new ArrayList<>();
        List<CharactersDO> charactersDOList = characterService.getCharactersViewByAccountId(worldId,accountId);
        for (CharactersDO charactersDO : charactersDOList) {
            CharsList.add(fromCharactersViewDO(charactersDO));
        }
        return CharsList;
    }
    /**
     * 加载角色预览数据
     * @param charactersDO
     * @return
     */
    public static Character fromCharactersViewDO(CharactersDO charactersDO) {
        Character chr = new Character();
        chr.setId(charactersDO.getId());
        chr.setName(charactersDO.getName());
        chr.setLevel(charactersDO.getLevel());
        chr.setFame(charactersDO.getFame());
        chr.setQuestFame(charactersDO.getFquest());
        chr.setStr(charactersDO.getAttrStr());
        chr.setDex(charactersDO.getAttrDex());
        chr.setInt(charactersDO.getAttrInt());
        chr.setLuk(charactersDO.getAttrLuk());
        chr.setExp(charactersDO.getExp());
        chr.setGachaExp(charactersDO.getGachaexp());
        chr.setHp(charactersDO.getHp());
        chr.setMaxHp(charactersDO.getMaxhp());
        chr.setMp(charactersDO.getMp());
        chr.setMaxMp(charactersDO.getMaxmp());
        chr.setGMLevel(charactersDO.getGm());
        chr.setSkinColor(SkinColor.getById(charactersDO.getSkincolor()));
        chr.setGender(charactersDO.getGender());
        chr.setJob(Job.getById(charactersDO.getJob()));
        chr.setHair(charactersDO.getHair());
        chr.setFace(charactersDO.getFace());
        chr.setAccountId(charactersDO.getAccountid());
        chr.setMapId(charactersDO.getMap());
        chr.setWorld(charactersDO.getWorld());
        chr.setRank(charactersDO.getRank());
        chr.setRankMove(charactersDO.getRankMove());
        chr.setJobRank(charactersDO.getJobRank());
        chr.setJobRankMove(charactersDO.getJobRankMove());
        chr.setGuildId(charactersDO.getGuildid());
        chr.setGuildRank(charactersDO.getGuildrank());
        chr.setAllianceRank(charactersDO.getAllianceRank());
        chr.setFamilyId(charactersDO.getFamilyId());

        // 加载角色外观数据
        List<InventorySearchRtnDTO> equippedItems = inventoryService.getInventoryList(
                InventorySearchReqDTO.builder()
                        .characterId(charactersDO.getId())
                        .inventoryType(InventoryType.EQUIPPED.getType())
                        .build()
        );

        Inventory equipInventory = chr.getInventory(InventoryType.EQUIPPED);
        for (InventorySearchRtnDTO itemDTO : equippedItems) {
            equipInventory.addItemFromDB(itemDTO.toItem());
        }
        return chr;
    }
    public static Character fromCharactersDO(CharactersDO charactersDO, Client client) {
        Character chr = new Character();
        chr.setClient(client);
        chr.setId(charactersDO.getId());

        chr.setName(charactersDO.getName());
        chr.setLevel(charactersDO.getLevel());
        chr.setFame(charactersDO.getFame());
        chr.setQuestFame(charactersDO.getFquest());
        chr.setStr(charactersDO.getAttrStr());
        chr.setDex(charactersDO.getAttrDex());
        chr.setInt(charactersDO.getAttrInt());
        chr.setLuk(charactersDO.getAttrLuk());
        chr.setExp(charactersDO.getExp());
        chr.setGachaExp(charactersDO.getGachaexp());
        chr.setHp(charactersDO.getHp());
        chr.setMaxHp(charactersDO.getMaxhp());
        chr.setMp(charactersDO.getMp());
        chr.setMaxMp(charactersDO.getMaxmp());
        chr.setHpMpApUsed(charactersDO.getHpMpUsed());
        chr.setHasMerchant(charactersDO.getHasmerchant());
        chr.setRemainingAp(charactersDO.getAp());
        int[] remainingSps = new int[10];
        Arrays.fill(remainingSps, 0);
        if (!RequireUtil.isEmpty(charactersDO.getSp())) {
            String[] splits = charactersDO.getSp().split(",");
            int len = Math.min(splits.length, remainingSps.length);
            for (int i = 0; i < len; i++) {
                remainingSps[i] = Integer.parseInt(splits[i]);
            }
        }
        chr.setRemainingSp(remainingSps);
        chr.setMeso(charactersDO.getMeso());
        chr.setMerchantMeso(charactersDO.getMerchantmesos());
        chr.setGMLevel(charactersDO.getGm());
        chr.setSkinColor(SkinColor.getById(charactersDO.getSkincolor()));
        chr.setGender(charactersDO.getGender());
        chr.setJob(Job.getById(charactersDO.getJob()));
        chr.setFinishedDojoTutorial(charactersDO.getFinishedDojoTutorial() == 1);
        chr.setVanquisherKills(charactersDO.getVanquisherKills());
        chr.setOmokwins(charactersDO.getOmokwins());
        chr.setOmoklosses(charactersDO.getOmoklosses());
        chr.setOmokties(charactersDO.getOmokties());
        chr.setMatchcardwins(charactersDO.getMatchcardwins());
        chr.setMatchcardlosses(charactersDO.getMatchcardlosses());
        chr.setMatchcardties(charactersDO.getMatchcardties());
        chr.setHair(charactersDO.getHair());
        chr.setFace(charactersDO.getFace());
        chr.setAccountId(charactersDO.getAccountid());
        chr.setMapId(charactersDO.getMap());
        chr.setJailExpiration(charactersDO.getJailexpire());
        chr.setInitialSpawnPoint(charactersDO.getSpawnpoint());
        chr.setWorld(charactersDO.getWorld());
        chr.setRank(charactersDO.getRank());
        chr.setRankMove(charactersDO.getRankMove());
        chr.setJobRank(charactersDO.getJobRank());
        chr.setJobRankMove(charactersDO.getJobRankMove());
        chr.setGuildId(charactersDO.getGuildid());
        chr.setGuildRank(charactersDO.getGuildrank());
        chr.setAllianceRank(charactersDO.getAllianceRank());
        chr.setFamilyId(charactersDO.getFamilyId());
        chr.setBookCover(charactersDO.getMonsterbookcover());
        chr.setMonsterBook(new MonsterBook(charactersDO.getId()));
        chr.setVanquisherStage(charactersDO.getVanquisherStage());
        chr.setAriantPoints(charactersDO.getAriantPoints());
        chr.setDojoPoints(charactersDO.getDojoPoints());
        chr.setDojoStage(charactersDO.getLastDojoStage());
        chr.setDataString(charactersDO.getDataString());
        chr.setMGC(new GuildCharacter(chr));
        chr.setBuddylist(new BuddyList(charactersDO.getBuddyCapacity()));
        chr.setLastExpGainTime(charactersDO.getLastExpGainTime().getTime());
        chr.setCanRecvPartySearchInvite(charactersDO.getPartySearch());
        chr.getInventory(InventoryType.EQUIP).setSlotLimit(charactersDO.getEquipslots());
        chr.getInventory(InventoryType.USE).setSlotLimit(charactersDO.getUseslots());
        chr.getInventory(InventoryType.SETUP).setSlotLimit(charactersDO.getSetupslots());
        chr.getInventory(InventoryType.ETC).setSlotLimit(charactersDO.getEtcslots());
        short sandboxCheck = 0x0;
        for (InventoryType inventoryType : InventoryType.values()) {
            List<InventorySearchRtnDTO> searchRtnDTOList = inventoryService.getInventoryList(InventorySearchReqDTO.builder()
                    .characterId(charactersDO.getId())
                    .inventoryType(inventoryType.getType())
                    .build());
            for (InventorySearchRtnDTO searchRtnDTO : searchRtnDTOList) {
                sandboxCheck |= searchRtnDTO.getFlag();
                Item item = searchRtnDTO.toItem();
                chr.getInventory(inventoryType).addItemFromDB(item);
                if (item.getPetId() > -1) {
                    Pet pet = item.getPet();
                    if (pet != null && pet.isSummoned()) {
                        chr.addPet(pet);
                        chr.resetExcluded(item.getPetId());
                        inventoryService.getPetIgnoreByPetId(item.getPetId()).forEach(petignoresDO -> chr.addExcluded(petignoresDO.getPetid(), petignoresDO.getItemid()));
                    }
                    continue;
                }
                if (searchRtnDTO.isEquipment() && searchRtnDTO.getInventoryEquipment().getRingId() > -1) {
                    Ring ring = Ring.loadFromDb(searchRtnDTO.getInventoryEquipment().getRingId());
                    if (ring == null) {
                        continue;
                    }
                    if (InventoryType.EQUIPPED.equals(inventoryType)) {
                        ring.equip();
                    }
                    chr.addPlayerRing(ring);
                }
            }
        }
        chr.commitExcludedItems();
        if ((sandboxCheck & ItemConstants.SANDBOX) == ItemConstants.SANDBOX) {
            chr.setHasSandboxItem();
        }
        chr.setPartnerId(charactersDO.getPartnerId());
        chr.setMarriageItemId(charactersDO.getMarriageItemId());
        World world = Server.getInstance().getWorld(charactersDO.getWorld());
        if (charactersDO.getMarriageItemId() > 0 && charactersDO.getPartnerId() <= 0) {
            chr.setMarriageItemId(-1);
        } else if (charactersDO.getPartnerId() > 0 && world.getRelationshipId(charactersDO.getId()) <= 0) {
            chr.setMarriageItemId(-1);
            chr.setPartnerId(-1);
        }
        NewYearCardRecord.loadPlayerNewYearCards(chr);

        List<TrocklocationsDO> trocklocationsDOList = characterService.getTrockLocationByCharacter(charactersDO.getId());
        int vip = 0;
        int reg = 0;
        for (int i = 0; i < 15; i++) {
            if (i < trocklocationsDOList.size()) {
                TrocklocationsDO trocklocationsDO = trocklocationsDOList.get(i);
                if (trocklocationsDO.getVip() == 1) {
                    vip++;
                    chr.getVipTrockMaps().add(trocklocationsDO.getMapid());
                } else if (trocklocationsDO.getVip() == 2) {
                    // 跳过脚本传送点，单独处理
                } else {
                    reg++;
                    chr.getTrockMaps().add(trocklocationsDO.getMapid());
                }
                continue;
            }
            if (vip < 10) {
                chr.getVipTrockMaps().add(MapId.NONE);
            }
            if (reg < 5) {
                chr.getTrockMaps().add(MapId.NONE);
            }
        }
        // 加载脚本传送点（vip = 2）
        for (TrocklocationsDO loc : trocklocationsDOList) {
            if (loc.getVip() == 2) {
                chr.getScriptsTrockMaps().add(loc.getMapid());
            }
        }

        AccountsDO accountsDO = accountService.findById(charactersDO.getAccountid());
        if (chr.getClient() != null) {
            chr.getClient().setAccountName(accountsDO.getName());
            chr.getClient().setCharacterSlots(Optional.ofNullable(accountsDO.getCharacterslots()).map(Integer::byteValue).orElse((byte) 0));
            chr.getClient().setLanguage(accountsDO.getLanguage());
        }
        List<AreaInfoDO> areaInfoDOList = characterService.getAreaInfoByCharacter(charactersDO.getId());
        areaInfoDOList.forEach(areaInfoDO -> chr.getAreaInfos().put(Optional.ofNullable(areaInfoDO.getArea()).map(Integer::shortValue).orElse((short) 0),
                areaInfoDO.getInfo()));

        List<EventstatsDO> eventstatsDOList = characterService.getEventStatsByCharacter(charactersDO.getId());
        eventstatsDOList.forEach(eventstatsDO -> chr.getEvents().put(eventstatsDO.getName(), new RescueGaga(Optional.ofNullable(eventstatsDO.getInfo()).orElse(0))));

        chr.setCashShop(new CashShop(charactersDO.getAccountid(), charactersDO.getId(), chr.getJobType()));
        chr.setAutoBanManager(new AutobanManager(chr));

        List<CharactersDO> charactersDOList = characterService.getCharactersByAccountId(charactersDO.getAccountid());
        charactersDOList.stream()
                .filter(chrDO -> !Objects.equals(chrDO.getId(), charactersDO.getId()))
                .max(Comparator.comparing(CharactersDO::getLevel))
                .ifPresent(chrDO -> {
                    chr.setLinkedName(chrDO.getName());
                    chr.setLinkedLevel(chrDO.getLevel());
                });

        int mountId = chr.getJobType() * 10000000 + 1004;
        if (chr.getInventory(InventoryType.EQUIPPED).getItem((short) -18) != null) {
            chr.setMapleMount(new Mount(chr, chr.getInventory(InventoryType.EQUIPPED).getItem((short) -18).getItemId(), mountId));
        } else {
            chr.setMapleMount(new Mount(chr, 0, mountId));
        }
        chr.getMapleMount().setExp(charactersDO.getMountexp());
        chr.getMapleMount().setLevel(charactersDO.getMountlevel());
        chr.getMapleMount().setTiredness(charactersDO.getMounttiredness());
        chr.getMapleMount().setActive(false);
        QuickslotkeymappedDO quickSlotKeyMap = accountService.getQuickSlotKeyMap(charactersDO.getAccountid());
        if (quickSlotKeyMap != null) {
            chr.setQuickSlotLoaded(NumberTool.LongToBytes(quickSlotKeyMap.getKeymap()));
            chr.setQuickSlotKeyMapped(new QuickslotBinding(chr.getQuickSlotLoaded()));
        }
        return chr;
    }

    public static CharactersDO toCharactersDO(Character chr) {
        CharactersDO cdo = new CharactersDO();
        cdo.setId(chr.getId());
        cdo.setAccountid(chr.getAccountId());
        cdo.setWorld(chr.getWorld());
        cdo.setName(chr.getName());
        cdo.setLevel(chr.getLevel());
        cdo.setExp(Math.abs(chr.exp.get()));
        cdo.setGachaexp(Math.abs(chr.gachaExp.get()));
        cdo.setAttrStr(chr.getStr());
        cdo.setAttrDex(chr.getDex());
        cdo.setAttrLuk(chr.getLuk());
        cdo.setAttrInt(chr.getInt());
        cdo.setHp(chr.getHp());
        cdo.setMp(chr.getMp());
        cdo.setMaxhp(chr.getMaxHp());
        cdo.setMaxmp(chr.getMaxMp());
        cdo.setMeso(chr.getMeso());
        cdo.setHpMpUsed(chr.getHpMpApUsed());
        cdo.setJob(chr.getJob().getId());
        cdo.setSkincolor(chr.getSkinColor().getId());
        cdo.setGender(chr.getGender());
        cdo.setFame(chr.getFame());
        cdo.setFquest(chr.getQuestFame());
        cdo.setHair(chr.getHair());
        cdo.setFace(chr.getFace());
        cdo.setAp(chr.getRemainingAp());

        StringBuilder sps = new StringBuilder();
        for (int sp : chr.getRemainingSps()) {
            sps.append(sp);
            sps.append(",");
        }
        sps.deleteCharAt(sps.length() - 1);
        cdo.setSp(sps.toString());

        if (chr.getMap() == null || (chr.getCashShop() != null && chr.getCashShop().isOpened())) {
            cdo.setMap(chr.getMapId());
        } else {
            if (chr.getMap().getForcedReturnId() != MapId.NONE) {
                cdo.setMap(chr.getMap().getForcedReturnId());
            } else {
                cdo.setMap(chr.getHp() < 1 ? chr.getMap().getReturnMapId() : chr.getMap().getId());
            }
        }

        if (chr.getMap() == null || chr.getMap().getId() == MapId.CRIMSONWOOD_VALLEY_1 || chr.getMap().getId() == MapId.CRIMSONWOOD_VALLEY_2) {
            cdo.setSpawnpoint(0);
        } else {
            Portal closest = chr.getMap().findClosestPlayerSpawnpoint(chr.getPosition());
            cdo.setSpawnpoint(closest != null ? closest.getId() : 0);
        }

        cdo.setGm(chr.gmLevel());
        cdo.setParty(chr.getPartyId());
        cdo.setBuddyCapacity(chr.getBuddylist().getCapacity());
        cdo.setRank(chr.getRank());
        cdo.setRankMove(chr.getRankMove());
        cdo.setJobRank(chr.getJobRank());
        cdo.setJobRankMove(chr.getJobRankMove());
        cdo.setGuildid(chr.getGuildId());
        cdo.setGuildrank(chr.getGuildRank());
        cdo.setAllianceRank(chr.getAllianceRank());

        if (chr.getMessenger() == null) {
            cdo.setMessengerid(0);
            cdo.setMessengerposition(4);
        } else {
            cdo.setMessengerid(chr.getMessenger().getId());
            cdo.setMessengerposition(chr.getMessengerPosition());
        }

        if (chr.getMapleMount() == null) {
            cdo.setMountlevel(1);
            cdo.setMountexp(0);
            cdo.setMounttiredness(0);
        } else {
            cdo.setMountlevel(chr.getMapleMount().getLevel());
            cdo.setMountexp(chr.getMapleMount().getExp());
            cdo.setMounttiredness(chr.getMapleMount().getTiredness());
        }

        cdo.setOmokwins(chr.getOmokwins());
        cdo.setOmoklosses(chr.getOmoklosses());
        cdo.setOmokties(chr.getOmokties());
        cdo.setMatchcardwins(chr.getMatchcardwins());
        cdo.setMatchcardlosses(chr.getMatchcardlosses());
        cdo.setMatchcardties(chr.getMatchcardties());
        cdo.setMerchantmesos(chr.getMerchantMeso());
        cdo.setHasmerchant(chr.hasMerchant());
        cdo.setEquipslots((int) chr.getSlots(InventoryType.EQUIP.getType()));
        cdo.setUseslots((int) chr.getSlots(InventoryType.USE.getType()));
        cdo.setSetupslots((int) chr.getSlots(InventoryType.SETUP.getType()));
        cdo.setEtcslots((int) chr.getSlots(InventoryType.ETC.getType()));
        cdo.setFamilyId(chr.getFamilyId());
        cdo.setMonsterbookcover(chr.getMonsterBookCover());
        cdo.setVanquisherStage(chr.getVanquisherStage());
        cdo.setAriantPoints(chr.getAriantPoints());
        cdo.setDojoPoints(chr.getDojoPoints());
        cdo.setLastDojoStage(chr.getDojoStage());
        cdo.setFinishedDojoTutorial(chr.isFinishedDojoTutorial() ? 1 : 0);
        cdo.setVanquisherKills(chr.getVanquisherKills());
        cdo.setSummonValue(0L);
        cdo.setPartnerId(chr.getPartnerId());
        cdo.setMarriageItemId(chr.getMarriageItemId());
//        cdo.setReborns(chr.getReborns());
        cdo.setPqpoints(0);
        cdo.setDataString(chr.getDataString());
        cdo.setLastLogoutTime(new Timestamp(System.currentTimeMillis()));
        cdo.setLastExpGainTime(new Timestamp(chr.getLastExpGainTime()));
        cdo.setPartySearch(chr.isRecvPartySearchInviteEnabled());
        cdo.setJailexpire(chr.getJailExpiration());

        // 保存学院声望——确保声望变更在角色存盘时持久化
        FamilyEntry fe = chr.getFamilyEntry();
        if (fe != null) {
            fe.saveReputation();
        }

        return cdo;
    }

    public static Character loadCharFromDB(final int cid, Client client, boolean channelServer) {
        try {
            return characterService.loadCharFromDB(cid, client, channelServer);
        } catch (Exception e) {
            log.error(I18nUtil.getLogMessage("Character.loadCharFromDB.error1"), cid, e);
        }
        return null;
    }

    public void reloadQuestExpirations() {
        for (QuestStatus mqs : getStartedQuests()) {
            if (mqs.getExpirationTime() > 0) {
                questTimeLimit2(mqs.getQuest(), mqs.getExpirationTime());
            }
        }
    }

    public static String makeMapleReadable(String in) {
        return in.replace('I', 'i')
                .replace('l', 'L')
                .replace("rn", "Rn")
                .replace("vv", "Vv")
                .replace("VV", "Vv");
    }

    private static class BuffStatValueHolder {

        public StatEffect effect;
        public long startTime;
        public int value;
        public boolean bestApplied;

        public BuffStatValueHolder(StatEffect effect, long startTime, int value) {
            super();
            this.effect = effect;
            this.startTime = startTime;
            this.value = value;
            this.bestApplied = false;
        }
    }

    public static class CooldownValueHolder {

        public int skillId;
        public long startTime, length;

        public CooldownValueHolder(int skillId, long startTime, long length) {
            super();
            this.skillId = skillId;
            this.startTime = startTime;
            this.length = length;
        }
    }

    public void message(String m) {
        dropMessage(5, m);
    }

    public void yellowMessage(String m) {
        sendPacket(PacketCreator.sendYellowTip(m));
    }

    public void raiseQuestMobCount(int id) {
        // It seems nexon uses monsters that don't exist in the WZ (except string) to merge multiple mobs together for these 3 monsters.
        // We also want to run mobKilled for both since there are some quest that don't use the updated ID...
        if (id == MobId.GREEN_MUSHROOM || id == MobId.DEJECTED_GREEN_MUSHROOM) {
            raiseQuestMobCount(MobId.GREEN_MUSHROOM_QUEST);
        } else if (id == MobId.ZOMBIE_MUSHROOM || id == MobId.ANNOYED_ZOMBIE_MUSHROOM) {
            raiseQuestMobCount(MobId.ZOMBIE_MUSHROOM_QUEST);
        } else if (id == MobId.GHOST_STUMP || id == MobId.SMIRKING_GHOST_STUMP) {
            raiseQuestMobCount(MobId.GHOST_STUMP_QUEST);
        }

        int lastQuestProcessed = 0;
        try {
            synchronized (quests) {
                for (QuestStatus qs : getQuestValues()) {
                    lastQuestProcessed = qs.getQuest().getId();
                    if (qs.getStatus() != QuestStatus.Status.STARTED || qs.getQuest().canComplete(this, null)) {
                        continue;
                    }

                    if (qs.progress(id)) {
                        announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, false);
                        if (qs.getInfoNumber() > 0) {
                            announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, true);
                        }
                        // [修改] 调用单个任务更新方法，实现实时保存
                        questService.updateQuestStatus(this.id, qs);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("角色击杀怪物. 角色ID {}, 最后处理的任务: {}", this.id, lastQuestProcessed, e);
        }
    }

    public Mount mount(int id, int skillid) {
        Mount mount = mapleMount;
        mount.setItemId(id);
        mount.setSkillId(skillid);
        return mount;
    }

    private void playerDead() {
        if (this.getMap().isCPQMap()) {
            int losing = getMap().getDeathCP();
            if (getCP() < losing) {
                losing = getCP();
            }
            getMap().broadcastMessage(PacketCreator.playerDiedMessage(getName(), losing, getTeam()));
            gainCP(-losing);
            return;
        }

        cancelAllBuffs(false);
        dispelDebuffs();

        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            eim.playerKilled(this);
        }
        int[] charmID = {ItemId.SAFETY_CHARM, ItemId.EASTER_BASKET, ItemId.EASTER_CHARM};
        int possesed = 0;
        int i;
        for (i = 0; i < charmID.length; i++) {
            int quantity = getItemQuantity(charmID[i], false);
            if (quantity > 0) {
                possesed = quantity;
                break;
            }
        }
        if (possesed > 0 && !MapId.isDojo(getMapId())) {
            InventoryManipulator.removeById(client, ItemConstants.getInventoryType(charmID[i]), charmID[i], 1, true, false);
            usedSafetyCharm = true;
            message(I18nUtil.getMessage("Character.useItem.message1"));  //使用安全护符，不扣经验
        } else if (getJob() != Job.BEGINNER) { //Hmm...
            if (!FieldLimit.NO_EXP_DECREASE.check(getMap().getFieldLimit())) {  // 感谢Conrad指出缺少的FieldLimit检查
                int XPdummy = ExpTable.getExpNeededForLevel(getLevel());

                if (getMap().isTown()) {    // 感谢MindLove, SIayerMonkey, HaItsNotOver指出玩家在城镇地图上只损失1%经验
                    XPdummy /= 100;
                } else {
                    if (getLuk() < 50) {    // 感谢Taiketo, Quit, Fishanelli指出玩家经验损失是固定的，50幸运值为阈值
                        XPdummy /= 10;
                    } else {
                        XPdummy /= 20;
                    }
                }

                int curExp = getExp();
                if (curExp > XPdummy) {
                    loseExp(XPdummy, false, false);
                } else {
                    loseExp(curExp, false, false);
                }
            }
        }

        if (getBuffedValue(BuffStat.MORPH) != null) {
            cancelEffectFromBuffStat(BuffStat.MORPH);
        }

        if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
            cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
        }

        unsitChairInternal();
        enableActions();
    }

    private void unsitChairInternal() {
        int chairid = chair.get();
        if (chairid >= 0) {
            if (ItemConstants.isFishingChair(chairid)) {
                this.getWorldServer().unregisterFisherPlayer(this);
            }

            setChair(-1);
            if (unregisterChairBuff()) {
                getMap().broadcastMessage(this, PacketCreator.cancelForeignChairSkillEffect(this.getId()), false);
            }

            getMap().broadcastMessage(this, PacketCreator.showChair(this.getId(), 0), false);
        }

        sendPacket(PacketCreator.cancelChair(-1));
    }

    public void sitChair(int itemId) {
        if (this.isLoggedInWorld()) {
            if (itemId >= 1000000) {    // sit on item chair
                if (chair.get() < 0) {
                    setChair(itemId);
                    getMap().broadcastMessage(this, PacketCreator.showChair(this.getId(), itemId), false);
                }
                enableActions();
            } else if (itemId >= 0) {    // sit on map chair
                if (chair.get() < 0) {
                    setChair(itemId);
                    if (registerChairBuff()) {
                        getMap().broadcastMessage(this, PacketCreator.giveForeignChairSkillEffect(this.getId()), false);
                    }
                    sendPacket(PacketCreator.cancelChair(itemId));
                }
            } else {    // stand up
                unsitChairInternal();
            }
        }
    }

    private void setChair(int chair) {
        this.chair.set(chair);
    }

    public void respawn(int returnMap) {
        respawn(null, returnMap);    // unspecified EIM, don't force EIM unregister in this case
    }

    public void respawn(EventInstanceManager eim, int returnMap) {
        if (eim != null) {
            eim.unregisterPlayer(this);    // some event scripts uses this...
        }
        changeMap(returnMap);

        cancelAllBuffs(false);  // thanks Oblivium91 for finding out players still could revive in area and take damage before returning to town

        if (usedSafetyCharm) {  // thanks kvmba for noticing safety charm not providing 30% HP/MP
            addMPHP((int) Math.ceil(this.getClientMaxHp() * 0.3), (int) Math.ceil(this.getClientMaxMp() * 0.3));
        } else {
            updateHp(50);
        }

        setStance(0);
    }

    private void prepareDragonBlood(final StatEffect bloodEffect) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        dragonBloodSchedule = TimerManager.getInstance().register(() -> {
            if (awayFromWorld.get()) {
                return;
            }

            addHP(-bloodEffect.getX());
            sendPacket(PacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
            getMap().broadcastMessage(Character.this, PacketCreator.showBuffEffect(getId(), bloodEffect.getSourceId(), 5), false);
        }, 4000, 4000);
    }

    private void recalcEquipStats() {
        if (equipchanged) {
            equipmaxhp = 0;
            equipmaxmp = 0;
            equipdex = 0;
            equipint_ = 0;
            equipstr = 0;
            equipluk = 0;
            equipmagic = 0;
            equipwatk = 0;
            //equipspeed = 0;
            //equipjump = 0;

            for (Item item : getInventory(InventoryType.EQUIPPED)) {
                Equip equip = (Equip) item;
                equipmaxhp += equip.getHp();
                equipmaxmp += equip.getMp();
                equipdex += equip.getDex();
                equipint_ += equip.getInt();
                equipstr += equip.getStr();
                equipluk += equip.getLuk();
                equipmagic += equip.getMatk() + equip.getInt();
                equipwatk += equip.getWatk();
                //equipspeed += equip.getSpeed();
                //equipjump += equip.getJump();
            }

            equipchanged = false;
        }

        localMaxHp += equipmaxhp;
        localMaxMp += equipmaxmp;
        localdex += equipdex;
        localint_ += equipint_;
        localstr += equipstr;
        localluk += equipluk;
        localmagic += equipmagic;
        localwatk += equipwatk;
    }

    public void reapplyLocalStats() {
        effLock.lock();
        chrLock.lock();
        statWlock.lock();
        try {
            localMaxHp = getMaxHp();
            localMaxMp = getMaxMp();
            localdex = getDex();
            localint_ = getInt();
            localstr = getStr();
            localluk = getLuk();
            localmagic = localint_;
            localwatk = 0;
            localchairrate = -1;

            recalcEquipStats();

//            localmagic = Math.min(localmagic, 2000);  // 需要注释，否则无法配合破功

            Integer hbhp = getBuffedValue(BuffStat.HYPERBODYHP);
            if (hbhp != null) {
                localMaxHp += (int) ((hbhp.doubleValue() / 100) * localMaxHp);
            }
            Integer hbmp = getBuffedValue(BuffStat.HYPERBODYMP);
            if (hbmp != null) {
                localMaxMp += (int) ((hbmp.doubleValue() / 100) * localMaxMp);
            }

            localMaxHp = Math.min(30000, localMaxHp);
            localMaxMp = Math.min(30000, localMaxMp);

            StatEffect combo = getBuffEffect(BuffStat.ARAN_COMBO);
            if (combo != null) {
                localwatk += combo.getX();
            }

            if (energyBar == 15000) {
                Skill energycharge = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory.getSkill(Marauder.ENERGY_CHARGE);
                StatEffect ceffect = energycharge.getEffect(getSkillLevel(energycharge));
                localwatk += ceffect.getWatk();
            }

            Integer mwarr = getBuffedValue(BuffStat.MAPLE_WARRIOR);
            if (mwarr != null) {
                localstr += getStr() * mwarr / 100;
                localdex += getDex() * mwarr / 100;
                localint_ += getInt() * mwarr / 100;
                localluk += getLuk() * mwarr / 100;
            }
            if (job.isA(Job.BOWMAN)) {
                Skill expert = null;
                if (job.isA(Job.MARKSMAN)) {
                    expert = SkillFactory.getSkill(3220004);
                } else if (job.isA(Job.BOWMASTER)) {
                    expert = SkillFactory.getSkill(3120005);
                }
                if (expert != null) {
                    int boostLevel = getSkillLevel(expert);
                    if (boostLevel > 0) {
                        localwatk += expert.getEffect(boostLevel).getX();
                    }
                }
            }

            Integer watkbuff = getBuffedValue(BuffStat.WATK);
            if (watkbuff != null) {
                localwatk += watkbuff;
            }
            Integer matkbuff = getBuffedValue(BuffStat.MATK);
            if (matkbuff != null) {
                localmagic += matkbuff;
            }

            /*
            Integer speedbuff = getBuffedValue(BuffStat.SPEED);
            if (speedbuff != null) {
                localspeed += speedbuff.intValue();
            }
            Integer jumpbuff = getBuffedValue(BuffStat.JUMP);
            if (jumpbuff != null) {
                localjump += jumpbuff.intValue();
            }
            */

            int blessing = getSkillLevel(10000000 * getJobType() + 12);
            if (blessing > 0) {
                localwatk += blessing;
                localmagic += blessing * 2;
            }

            if (job.isA(Job.THIEF) || job.isA(Job.BOWMAN) || job.isA(Job.PIRATE) || job.isA(Job.NIGHTWALKER1) || job.isA(Job.WINDARCHER1)) {
                Item weapon_item = getInventory(InventoryType.EQUIPPED).getItem((short) -11);
                if (weapon_item != null) {
                    ItemInformationProvider ii = ItemInformationProvider.getInstance();
                    WeaponType weapon = ii.getWeaponType(weapon_item.getItemId());
                    boolean bow = weapon == WeaponType.BOW;
                    boolean crossbow = weapon == WeaponType.CROSSBOW;
                    boolean claw = weapon == WeaponType.CLAW;
                    boolean gun = weapon == WeaponType.GUN;
                    if (bow || crossbow || claw || gun) {
                        // Also calc stars into this.
                        Inventory inv = getInventory(InventoryType.USE);
                        for (short i = 1; i <= inv.getSlotLimit(); i++) {
                            Item item = inv.getItem(i);
                            if (item == null) {
                                continue;
                            }
                            if ((claw && ItemConstants.isThrowingStar(item.getItemId()))
                                    || (gun && ItemConstants.isBullet(item.getItemId()))
                                    || (bow && ItemConstants.isArrowForBow(item.getItemId()))
                                    || (crossbow && ItemConstants.isArrowForCrossBow(item.getItemId()))) {
                                if (item.getQuantity() > 0) {
                                    // Finally there!
                                    localwatk += ii.getWatkForProjectile(item.getItemId());
                                    break;
                                }
                            }
                        }
                    }
                }
                // Add throwing stars to dmg.
            }
        } finally {
            statWlock.unlock();
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public List<Pair<Stat, Integer>> recalcLocalStats() {
        effLock.lock();
        chrLock.lock();
        statWlock.lock();
        try {
            List<Pair<Stat, Integer>> hpmpupdate = new ArrayList<>(2);
            int oldlocalmaxhp = localMaxHp;
            int oldlocalmaxmp = localMaxMp;

            reapplyLocalStats();

            if (GameConfig.getServerBoolean("use_fixed_ratio_hpmp_update")) {
                if (localMaxHp != oldlocalmaxhp) {
                    Pair<Stat, Integer> hpUpdate;

                    if (transientHp == Float.NEGATIVE_INFINITY) {
                        hpUpdate = calcHpRatioUpdate(localMaxHp, oldlocalmaxhp);
                    } else {
                        hpUpdate = calcHpRatioTransient();
                    }

                    hpmpupdate.add(hpUpdate);
                }

                if (localMaxMp != oldlocalmaxmp) {
                    Pair<Stat, Integer> mpUpdate;

                    if (transientMp == Float.NEGATIVE_INFINITY) {
                        mpUpdate = calcMpRatioUpdate(localMaxMp, oldlocalmaxmp);
                    } else {
                        mpUpdate = calcMpRatioTransient();
                    }

                    hpmpupdate.add(mpUpdate);
                }
            }

            return hpmpupdate;
        } finally {
            statWlock.unlock();
            chrLock.unlock();
            effLock.unlock();
        }
    }

    private void updateLocalStats() {
        prtLock.lock();
        effLock.lock();
        statWlock.lock();
        try {
            int oldmaxhp = localMaxHp;
            List<Pair<Stat, Integer>> hpmpupdate = recalcLocalStats();
            enforceMaxHpMp();

            if (!hpmpupdate.isEmpty()) {
                sendPacket(PacketCreator.updatePlayerStats(hpmpupdate, true, this));
            }

            if (oldmaxhp != localMaxHp) {   // thanks Wh1SK3Y (Suwaidy) for pointing out a deadlock occuring related to party members HP
                updatePartyMemberHP();
            }
        } finally {
            statWlock.unlock();
            effLock.unlock();
            prtLock.unlock();
        }
    }

    public void receivePartyMemberHP() {
        prtLock.lock();
        try {
            if (party != null) {
                for (Character partychar : this.getPartyMembersOnSameMap()) {
                    sendPacket(PacketCreator.updatePartyMemberHP(partychar.getId(), partychar.getHp(), partychar.getCurrentMaxHp()));
                }
            }
        } finally {
            prtLock.unlock();
        }
    }

    public void removeAllCooldownsExcept(int id, boolean packet) {
        effLock.lock();
        chrLock.lock();
        try {
            ArrayList<CooldownValueHolder> list = new ArrayList<>(coolDowns.values());
            for (CooldownValueHolder mcvh : list) {
                if (mcvh.skillId != id) {
                    coolDowns.remove(mcvh.skillId);
                    if (packet) {
                        sendPacket(PacketCreator.skillCooldown(mcvh.skillId, 0));
                    }
                }
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void removeCooldown(int skillId) {
        effLock.lock();
        chrLock.lock();
        try {
            this.coolDowns.remove(skillId);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void removePet(Pet pet, boolean shift_left) {
        petLock.lock();
        try {
            int slot = -1;
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == pet.getUniqueId()) {
                        pets[i] = null;
                        slot = i;
                        break;
                    }
                }
            }
            if (shift_left) {
                if (slot > -1) {
                    for (int i = slot; i < 3; i++) {
                        if (i != 2) {
                            pets[i] = pets[i + 1];
                        } else {
                            pets[i] = null;
                        }
                    }
                }
            }
        } finally {
            petLock.unlock();
        }
    }

    public void removeVisibleMapObject(MapObject mo) {
        visibleMapObjects.remove(mo);
    }

    public synchronized void resetStats() {
        if (!GameConfig.getServerBoolean("use_auto_assign_starters_ap")) {
            return;
        }

        effLock.lock();
        statWlock.lock();
        try {
            int tap = remainingAp + attrStr + attrDex + attrInt + attrLuk, tsp = 1;
            int tstr = 4, tdex = 4, tint = 4, tluk = 4;

            switch (job.getId()) {
                case 100:
                case 1100:
                case 2100:
                    tstr = 35;
                    tsp += ((getLevel() - 10) * 3);
                    break;
                case 200:
                case 1200:
                    tint = 20;
                    tsp += ((getLevel() - 8) * 3);
                    break;
                case 300:
                case 1300:
                case 400:
                case 1400:
                    tdex = 25;
                    tsp += ((getLevel() - 10) * 3);
                    break;
                case 500:
                case 1500:
                    tdex = 20;
                    tsp += ((getLevel() - 10) * 3);
                    break;
            }

            tap -= tstr;
            tap -= tdex;
            tap -= tint;
            tap -= tluk;

            if (tap >= 0) {
                updateStrDexIntLukSp(tstr, tdex, tint, tluk, tap, tsp, GameConstants.getSkillBook(job.getId()));
            } else {
                log.warn("Chr {} tried to have its stats reset without enough AP available", getName());
            }
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }
    }

    public void resetBattleshipHp() {
        int bshipLevel = Math.max(getLevel() - 120, 0);  // thanks alex12 for noticing battleship HP issues for low-level players
        this.battleshipHp = 400 * getSkillLevel(SkillFactory.getSkill(Corsair.BATTLE_SHIP)) + (bshipLevel * 200);
    }

    public void resetEnteredScript() {
        entered.remove(map.getId());
    }

    public void resetEnteredScript(int mapId) {
        entered.remove(mapId);
    }

    public void resetEnteredScript(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                entered.remove(mapId);
            }
        }
    }

    public synchronized void saveCooldowns() {
        characterService.saveCooldowns(getId(), getAllCooldowns(), getAllDiseases());
    }

    public void saveGuildStatus() {
        characterService.updateGuildStatus(id, guildId, guildRank, allianceRank);
    }

    public void saveLocationOnWarp() {  // suggestion to remember the map before warp command thanks to Lei
        Portal closest = map.findClosestPortal(getPosition());
        int curMapid = getMapId();

        for (int i = 0; i < savedLocations.length; i++) {
            if (savedLocations[i] == null) {
                savedLocations[i] = new SavedLocation(curMapid, closest != null ? closest.getId() : 0);
            }
        }
    }

    public void saveLocation(String type) {
        Portal closest = map.findClosestPortal(getPosition());
        savedLocations[SavedLocationType.fromString(type).ordinal()] = new SavedLocation(getMapId(), closest != null ? closest.getId() : 0);
        // [新增] 实时保存地图位置
        characterService.saveSavedLocations(id, savedLocations);
    }

    public final boolean insertNewChar(CharacterFactoryRecipe recipe) {
        attrStr = recipe.getStr();
        attrDex = recipe.getDex();
        attrInt = recipe.getInt();
        attrLuk = recipe.getLuk();
        setMaxHp(recipe.getMaxHp());
        setMaxMp(recipe.getMaxMp());
        hp = maxHp;
        mp = maxMp;
        level = recipe.getLevel();
        remainingAp = recipe.getRemainingAp();
        remainingSp[GameConstants.getSkillBook(job.getId())] = recipe.getRemainingSp();
        mapId = recipe.getMap();
        meso.set(recipe.getMeso());

        List<Pair<Skill, Integer>> startingSkills = recipe.getStartingSkillLevel();
        for (Pair<Skill, Integer> skEntry : startingSkills) {
            Skill skill = skEntry.getLeft();
            this.changeSkillLevel(skill, skEntry.getRight().byteValue(), skill.getMaxLevel(), -1);
        }

        List<Pair<Item, InventoryType>> itemsWithType = recipe.getStartingItems();
        for (Pair<Item, InventoryType> itEntry : itemsWithType) {
            this.getInventory(itEntry.getRight()).addItem(itEntry.getLeft());
        }

        this.events.put("rescueGaga", new RescueGaga(0));

        try {
            characterService.insertNewChar(this, recipe);
            return true;
        } catch (Exception e) {
            log.error("创建角色 {} (等级: {}, 职业: {}) 时出错", name, level, job.getId(), e);
            return false;
        }
    }

    public void saveCharToDB() {
        if (GameConfig.getServerBoolean("use_autosave")) {
            Runnable r = () -> saveCharToDB(true);

            CharacterSaveService service = (CharacterSaveService) getWorldServer().getServiceAccess(WorldServices.SAVE_CHARACTER);
            service.registerSaveCharacter(this.getId(), r);
        } else {
            saveCharToDB(true);
        }
    }

    //ItemFactory saveItems and monsterbook.saveCards are the most time consuming here.
    public synchronized void saveCharToDB(boolean notAutosave) {
//        if (!loggedIn) {
//            return;
//        }
//        log.info(I18nUtil.getLogMessage(notAutosave ? "Character.saveCharToDB.info1" : "Character.saveCharToDB.info2"), name);
//        Server.getInstance().updateCharacterEntry(this);
        try {
            characterService.saveCharToDB(this, notAutosave);
        } catch (Exception e) {
            log.error("保存角色 {} (等级: {}, 职业: {}) 时出错", name, level, job.getId(), e);
        }
    }

    public void sendPolice(int greason, String reason, int duration) {
        sendPacket(PacketCreator.sendPolice(String.format("You have been blocked by the#b %s Police for %s.#k", "Cosmic", reason)));
        this.banned = true;
        TimerManager.getInstance().schedule(() -> client.disconnect(false, false), duration);
    }

    public void sendPolice(String text) {
        final String message = getName() + " received this - " + text;
        if (Server.getInstance().isGmOnline(this.getWorld())) { //Alert and log if a GM is online
            Server.getInstance().broadcastGMMessage(this.getWorld(), PacketCreator.sendYellowTip(message));
        } else { //Auto DC and log if no GM is online
            client.disconnect(false, false);
        }
        log.info(message);
        //Server.getInstance().broadcastGMMessage(0, PacketCreator.serverNotice(1, getName() + " received this - " + text));
        //sendPacket(PacketCreator.sendPolice(text));
        //this.isbanned = true;
        //TimerManager.getInstance().schedule(new Runnable() {
        //    @Override
        //    public void run() {
        //        client.disconnect(false, false);
        //    }
        //}, 6000);
    }

    public void sendKeymap() {
        sendPacket(PacketCreator.getKeymap(keymap));
    }

    public void sendQuickmap() {
        // send quickslots to user
        QuickslotBinding pQuickslotKeyMapped = this.quickSlotKeyMapped;

        if (pQuickslotKeyMapped == null) {
            pQuickslotKeyMapped = new QuickslotBinding(QuickslotBinding.DEFAULT_QUICKSLOTS);
        }

        this.sendPacket(PacketCreator.QuickslotMappedInit(pQuickslotKeyMapped));
    }

    public void sendMacros() {
        // Always send the macro packet to fix a client side bug when switching characters.
        sendPacket(PacketCreator.getMacros(skillMacros));
    }

    public void setBuddyCapacity(int capacity) {
        buddylist.setCapacity(capacity);
        sendPacket(PacketCreator.updateBuddyCapacity(capacity));
    }

    public void setBuffedValue(BuffStat effect, int value) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return;
            }
            mbsvh.value = value;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
    }

    public void setDojoEnergy(int x) {
        this.dojoEnergy = Math.min(x, 10000);
    }


    public void setEventInstance(EventInstanceManager eventInstance) {
        evtLock.lock();
        try {
            this.eventInstance = eventInstance;
        } finally {
            evtLock.unlock();
        }
    }

    public void setExp(int amount) {
        this.exp.set(amount);
    }

    public void setGachaExp(int exp) {
        this.gachaExp.set(exp);
    }

    public void finishDojoTutorial() {
        this.finishedDojoTutorial = true;
    }

    public void setGM(int level) {
        this.gmLevel = level;
    }

    public void setHasMerchant(boolean set) {
        characterService.update(CharactersDO.builder()
                .id(id)
                .hasmerchant(set)
                .build());
        hasMerchant = set;
    }

    public void addMerchantMesos(int add) {
        final int newAmount = (int) Math.min((long) merchantmeso + add, Integer.MAX_VALUE);
        setMerchantMeso(newAmount);
    }

    public void setMerchantMeso(int set) {
        characterService.update(CharactersDO.builder()
                .id(id)
                .merchantmesos(set)
                .build());
        merchantmeso = set;
    }

    public synchronized void withdrawMerchantMesos() {
        int merchantMeso = this.getMerchantNetMeso();
        int playerMeso = this.getMeso();

        if (merchantMeso > 0) {
            int possible = Integer.MAX_VALUE - playerMeso;

            if (possible > 0) {
                if (possible < merchantMeso) {
                    this.gainMeso(possible, false);
                    this.setMerchantMeso(merchantMeso - possible);
                } else {
                    this.gainMeso(merchantMeso, false);
                    this.setMerchantMeso(0);
                }
            }
        } else {
            int nextMeso = playerMeso + merchantMeso;

            if (nextMeso < 0) {
                this.gainMeso(-playerMeso, false);
                this.setMerchantMeso(merchantMeso + playerMeso);
            } else {
                this.gainMeso(merchantMeso, false);
                this.setMerchantMeso(0);
            }
        }
    }

    public void hpChangeAction(int oldHp) {
        boolean playerDied = false;
        if (hp <= 0) {
            if (oldHp > hp) {
                playerDied = true;
            }
        }

        final boolean chrDied = playerDied;
        Runnable r = () -> {
            updatePartyMemberHP();    // thanks BHB (BHB88) for detecting a deadlock case within player stats.

            if (chrDied) {
                playerDead();
            } else {
                checkBerserk(isHidden());
            }
        };
        if (map != null) {
            map.registerCharacterStatUpdate(r);
        }
    }

    private Pair<Stat, Integer> calcHpRatioUpdate(int newHp, int oldHp) {
        int delta = newHp - oldHp;
        this.hp = calcHpRatioUpdate(hp, oldHp, delta);

        hpChangeAction(Short.MIN_VALUE);
        return new Pair<>(Stat.HP, hp);
    }

    private Pair<Stat, Integer> calcMpRatioUpdate(int newMp, int oldMp) {
        int delta = newMp - oldMp;
        this.mp = calcMpRatioUpdate(mp, oldMp, delta);
        return new Pair<>(Stat.MP, mp);
    }

    private static int calcTransientRatio(float transientpoint) {
        int ret = (int) transientpoint;
        return !(ret <= 0 && transientpoint > 0.0f) ? ret : 1;
    }

    private Pair<Stat, Integer> calcHpRatioTransient() {
        this.hp = calcTransientRatio(transientHp * localMaxHp);

        hpChangeAction(Short.MIN_VALUE);
        return new Pair<>(Stat.HP, hp);
    }

    private Pair<Stat, Integer> calcMpRatioTransient() {
        this.mp = calcTransientRatio(transientMp * localMaxMp);
        return new Pair<>(Stat.MP, mp);
    }

    private int calcHpRatioUpdate(int curpoint, int maxpoint, int diffpoint) {
        int nextMax = Math.min(30000, maxpoint + diffpoint);

        float temp = curpoint * nextMax;
        int ret = (int) Math.ceil(temp / maxpoint);

        transientHp = (maxpoint > nextMax) ? ((float) curpoint) / maxpoint : ((float) ret) / nextMax;
        return ret;
    }

    private int calcMpRatioUpdate(int curpoint, int maxpoint, int diffpoint) {
        int nextMax = Math.min(30000, maxpoint + diffpoint);

        float temp = curpoint * nextMax;
        int ret = (int) Math.ceil(temp / maxpoint);

        transientMp = (maxpoint > nextMax) ? ((float) curpoint) / maxpoint : ((float) ret) / nextMax;
        return ret;
    }

    public boolean applyHpMpChange(int hpCon, int hpchange, int mpchange) {
        boolean zombify = hasDisease(Disease.ZOMBIFY);

        effLock.lock();
        statWlock.lock();
        try {
            int nextHp = hp + hpchange, nextMp = mp + mpchange;
            boolean cannotApplyHp = hpchange != 0 && nextHp <= 0 && (!zombify || hpCon > 0);
            boolean cannotApplyMp = mpchange != 0 && nextMp < 0;

            if (cannotApplyHp || cannotApplyMp) {
                if (!isGM()) {
                    return false;
                }

                if (cannotApplyHp) {
                    nextHp = 1;
                }
            }

            updateHpMp(nextHp, nextMp);
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }

        if (GameConfig.getServerBoolean("use_server_auto_pot") || GameConfig.getServerBoolean("use_compulsory_auto_pot")) {
            float autoHpAlert, autoMpAlert;
            if (GameConfig.getServerBoolean("use_server_auto_pot")) {
                autoHpAlert = hpMpAlertService.getHpAlertPer(id);
                autoMpAlert = hpMpAlertService.getMpAlertPer(id);
            } else {
                autoHpAlert = GameConfig.getServerFloat("pet_auto_hp_ratio");
                autoMpAlert = GameConfig.getServerFloat("pet_auto_mp_ratio");
            }

            if (hpchange < 0) {
                KeyBinding autoHpPot = this.getKeymap().get(91);
                if (autoHpPot != null) {
                    int autoHpItemId = autoHpPot.getAction();
                    if (((float) this.getHp()) / this.getCurrentMaxHp() <= autoHpAlert) {
                        Item autoHpItem = this.getInventory(InventoryType.USE).findById(autoHpItemId);
                        if (autoHpItem != null) {
                            PetAutopotProcessor.runAutopotAction(client, autoHpItem.getPosition(), autoHpItemId);
                        }
                    }
                }
            }

            if (mpchange < 0) {
                KeyBinding autoMpPot = this.getKeymap().get(92);
                if (autoMpPot != null) {
                    int autoMpItemId = autoMpPot.getAction();
                    if (((float) this.getMp()) / this.getCurrentMaxMp() <= autoMpAlert) {
                        Item autoMpItem = this.getInventory(InventoryType.USE).findById(autoMpItemId);
                        if (autoMpItem != null) {
                            PetAutopotProcessor.runAutopotAction(client, autoMpItem.getPosition(), autoMpItemId);
                        }
                    }
                }
            }
        } else {
            if (hpchange < 0) {
                sendPacket(PacketCreator.onNotifyHPDecByField(hpchange * -1));
            }
        }

        return true;
    }

    public void setMap(int PmapId) {
        this.mapId = PmapId;
    }

    public void setMiniGamePoints(Character visitor, int winnerslot, boolean omok) {
        if (omok) {
            if (winnerslot == 1) {
                this.omokwins++;
                visitor.omoklosses++;
            } else if (winnerslot == 2) {
                visitor.omokwins++;
                this.omoklosses++;
            } else {
                this.omokties++;
                visitor.omokties++;
            }
        } else {
            if (winnerslot == 1) {
                this.matchcardwins++;
                visitor.matchcardlosses++;
            } else if (winnerslot == 2) {
                visitor.matchcardwins++;
                this.matchcardlosses++;
            } else {
                this.matchcardties++;
                visitor.matchcardties++;
            }
        }
    }

    public void setRPS(RockPaperScissor rps) {
        this.rps = rps;
    }

    public void closeRPS() {
        RockPaperScissor rps = this.rps;
        if (rps != null) {
            rps.dispose(client);
            setRPS(null);
        }
    }

    public int getDoorSlot() {
        if (doorSlot != -1) {
            return doorSlot;
        }
        return fetchDoorSlot();
    }

    public int fetchDoorSlot() {
        prtLock.lock();
        try {
            doorSlot = (party == null) ? 0 : party.getPartyDoor(this.getId());
            return doorSlot;
        } finally {
            prtLock.unlock();
        }
    }

    public void setParty(Party p) {
        prtLock.lock();
        try {
            if (p == null) {
                this.mpc = null;
                doorSlot = -1;

                party = null;
            } else {
                party = p;
            }
        } finally {
            prtLock.unlock();
        }
    }

    public byte getSlots(int type) {
        return type == InventoryType.CASH.getType() ? 96 : inventory[type].getSlotLimit();
    }

    public boolean canGainSlots(int type, int slots) {
        slots += inventory[type].getSlotLimit();
        return slots <= 96;
    }

    public boolean gainSlots(int type, int slots) {
        return gainSlots(type, slots, true);
    }

    public boolean gainSlots(int type, int slots, boolean update) {
        int newLimit = gainSlotsInternal(type, slots);
        if (newLimit != -1) {
            this.saveCharToDB();
            if (update) {
                sendPacket(PacketCreator.updateInventorySlotLimit(type, newLimit));
            }
            return true;
        } else {
            return false;
        }
    }

    private int gainSlotsInternal(int type, int slots) {
        inventory[type].lockInventory();
        try {
            if (canGainSlots(type, slots)) {
                int newLimit = inventory[type].getSlotLimit() + slots;
                inventory[type].setSlotLimit(newLimit);
                return newLimit;
            } else {
                return -1;
            }
        } finally {
            inventory[type].unlockInventory();
        }
    }

    public int sellAllItemsFromName(byte invTypeId, String name) {
        //player decides from which inventory items should be sold.
        InventoryType type = InventoryType.getByType(invTypeId);

        Inventory inv = getInventory(type);
        inv.lockInventory();
        try {
            Item it = inv.findByName(name);
            if (it == null) {
                return (-1);
            }

            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            return (sellAllItemsFromPosition(ii, type, it.getPosition()));
        } finally {
            inv.unlockInventory();
        }
    }

    public int sellAllItemsFromPosition(ItemInformationProvider ii, InventoryType type, short pos) {
        int mesoGain = 0;

        Inventory inv = getInventory(type);
        inv.lockInventory();
        try {
            for (short i = pos; i <= inv.getSlotLimit(); i++) {
                if (inv.getItem(i) == null) {
                    continue;
                }
                mesoGain += standaloneSell(getClient(), ii, type, i, inv.getItem(i).getQuantity());
            }
        } finally {
            inv.unlockInventory();
        }

        return (mesoGain);
    }

    private int standaloneSell(Client c, ItemInformationProvider ii, InventoryType type, short slot, short quantity) {
        if (quantity == 0) {
            quantity = 1;
        }

        Inventory inv = getInventory(type);
        inv.lockInventory();
        try {
            Item item = inv.getItem(slot);
            if (item == null) { //Basic check
                return (0);
            }

            int itemid = item.getItemId();
            if (ItemConstants.isRechargeable(itemid)) {
                quantity = item.getQuantity();
            } else if (ItemId.isWeddingToken(itemid) || ItemId.isWeddingRing(itemid)) {
                return (0);
            }

            if (quantity < 0) {
                return (0);
            }
            short iQuant = item.getQuantity();

            if (quantity <= iQuant && iQuant > 0) {
                InventoryManipulator.removeFromSlot(c, type, (byte) slot, quantity, false);
                int recvMesos = ii.getPrice(itemid, quantity);
                if (recvMesos > 0) {
                    gainMeso(recvMesos, false);
                    return (recvMesos);
                }
            }

            return (0);
        } finally {
            inv.unlockInventory();
        }
    }

    private static boolean hasMergeFlag(Item item) {
        return (item.getFlag() & ItemConstants.MERGE_UNTRADEABLE) == ItemConstants.MERGE_UNTRADEABLE;
    }

    private static void setMergeFlag(Item item) {
        short flag = item.getFlag();
        flag |= ItemConstants.MERGE_UNTRADEABLE;
        flag |= ItemConstants.UNTRADEABLE;
        item.setFlag(flag);
    }

    private List<Equip> getUpgradeableEquipped() {
        List<Equip> list = new LinkedList<>();

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Item item : getInventory(InventoryType.EQUIPPED)) {
            if (ii.isUpgradeable(item.getItemId())) {
                list.add((Equip) item);
            }
        }

        return list;
    }

    private static List<Equip> getEquipsWithStat(List<Pair<Equip, Map<StatUpgrade, Short>>> equipped, StatUpgrade stat) {
        List<Equip> equippedWithStat = new LinkedList<>();

        for (Pair<Equip, Map<StatUpgrade, Short>> eq : equipped) {
            if (eq.getRight().containsKey(stat)) {
                equippedWithStat.add(eq.getLeft());
            }
        }

        return equippedWithStat;
    }

    public boolean mergeAllItemsFromName(String name) {
        InventoryType type = InventoryType.EQUIP;

        Inventory inv = getInventory(type);
        inv.lockInventory();
        try {
            Item it = inv.findByName(name);
            if (it == null) {
                return false;
            }

            Map<StatUpgrade, Float> statups = new LinkedHashMap<>();
            mergeAllItemsFromPosition(statups, it.getPosition());

            List<Pair<Equip, Map<StatUpgrade, Short>>> upgradeableEquipped = new LinkedList<>();
            Map<Equip, List<Pair<StatUpgrade, Integer>>> equipUpgrades = new LinkedHashMap<>();
            for (Equip eq : getUpgradeableEquipped()) {
                upgradeableEquipped.add(new Pair<>(eq, eq.getStats()));
                equipUpgrades.put(eq, new LinkedList<Pair<StatUpgrade, Integer>>());
            }

            /*
            for (Entry<StatUpgrade, Float> es : statups.entrySet()) {
                System.out.println(es);
            }
            */

            for (Entry<StatUpgrade, Float> e : statups.entrySet()) {
                double ev = Math.sqrt(e.getValue());

                Set<Equip> extraEquipped = new LinkedHashSet<>(equipUpgrades.keySet());
                List<Equip> statEquipped = getEquipsWithStat(upgradeableEquipped, e.getKey());
                float extraRate = (float) (0.2 * Math.random());

                if (!statEquipped.isEmpty()) {
                    float statRate = 1.0f - extraRate;

                    int statup = (int) Math.ceil((ev * statRate) / statEquipped.size());
                    for (Equip statEq : statEquipped) {
                        equipUpgrades.get(statEq).add(new Pair<>(e.getKey(), statup));
                        extraEquipped.remove(statEq);
                    }
                }

                if (!extraEquipped.isEmpty()) {
                    int statup = (int) Math.round((ev * extraRate) / extraEquipped.size());
                    if (statup > 0) {
                        for (Equip extraEq : extraEquipped) {
                            equipUpgrades.get(extraEq).add(new Pair<>(e.getKey(), statup));
                        }
                    }
                }
            }

            dropMessage(6, "EQUIPMENT MERGE operation results:");
            for (Entry<Equip, List<Pair<StatUpgrade, Integer>>> eqpUpg : equipUpgrades.entrySet()) {
                List<Pair<StatUpgrade, Integer>> eqpStatups = eqpUpg.getValue();
                if (!eqpStatups.isEmpty()) {
                    Equip eqp = eqpUpg.getKey();
                    setMergeFlag(eqp);

                    String showStr = " '" + ItemInformationProvider.getInstance().getName(eqp.getItemId()) + "': ";
                    String upgdStr = eqp.gainStats(eqpStatups).getLeft();

                    this.forceUpdateItem(eqp);

                    showStr += upgdStr;
                    dropMessage(6, showStr);
                }
            }

            return true;
        } finally {
            inv.unlockInventory();
        }
    }

    public void mergeAllItemsFromPosition(Map<StatUpgrade, Float> statUps, short pos) {
        Inventory inv = getInventory(InventoryType.EQUIP);
        inv.lockInventory();
        try {
            for (short i = pos; i <= inv.getSlotLimit(); i++) {
                standaloneMerge(statUps, getClient(), InventoryType.EQUIP, i, inv.getItem(i));
            }
        } finally {
            inv.unlockInventory();
        }
    }

    private void standaloneMerge(Map<StatUpgrade, Float> statUps, Client c, InventoryType type, short slot, Item item) {
        short quantity;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (item == null || (quantity = item.getQuantity()) < 1 || ii.isCash(item.getItemId()) || !ii.isUpgradeable(item.getItemId()) || hasMergeFlag(item)) {
            return;
        }

        Equip e = (Equip) item;
        for (Entry<StatUpgrade, Short> s : e.getStats().entrySet()) {
            Float newVal = statUps.get(s.getKey());

            float incVal = s.getValue().floatValue();
            incVal = switch (s.getKey()) {
                case incPAD, incMAD, incPDD, incMDD -> (float) Math.log(incVal);
                default -> incVal;
            };

            if (newVal != null) {
                newVal += incVal;
            } else {
                newVal = incVal;
            }

            statUps.put(s.getKey(), newVal);
        }

        InventoryManipulator.removeFromSlot(c, type, (byte) slot, quantity, false);
    }

    public void setSlot(int slotid) {
        slots = slotid;
    }


    public void shiftPetsRight() {
        petLock.lock();
        try {
            if (pets[2] == null) {
                pets[2] = pets[1];
                pets[1] = pets[0];
                pets[0] = null;
            }
        } finally {
            petLock.unlock();
        }
    }

    private long getDojoTimeLeft() {
        return client.getChannelServer().getDojoFinishTime(map.getId()) - Server.getInstance().getCurrentTime();
    }

    public void showDojoClock() {
        if (GameConstants.isDojoBossArea(map.getId())) {
            sendPacket(PacketCreator.getClock((int) (getDojoTimeLeft() / 1000)));
        }
    }

    public void showUnderLeveledInfo(Monster mob) {
        long curTime = Server.getInstance().getCurrentTime();
        if (nextWarningTime < curTime) {
            nextWarningTime = curTime + MINUTES.toMillis(1);   // show underlevel info again after 1 minute

            showHint(I18nUtil.getMessage("Character.showUnderLeveledInfo", mob.getName(), mob.getLevel()));
        }
    }

    public void showMapOwnershipInfo(Character mapOwner) {
        long curTime = Server.getInstance().getCurrentTime();
        if (nextWarningTime < curTime) {
            nextWarningTime = curTime + MINUTES.toMillis(1); // show underlevel info again after 1 minute

            String medal = "";
            Item medalItem = mapOwner.getInventory(InventoryType.EQUIPPED).getItem((short) -49);
            if (medalItem != null) {
                medal = "<" + ItemInformationProvider.getInstance().getName(medalItem.getItemId()) + "> ";
            }

            List<String> strLines = new LinkedList<>();
            strLines.add("");
            strLines.add("");
            strLines.add("");
            strLines.add(this.getClient().getChannelServer().getServerMessage().isEmpty() ? 0 : 1, "Get off my lawn!!");

            this.sendPacket(PacketCreator.getAvatarMega(mapOwner, medal, this.getClient().getChannel(), ItemId.ROARING_TIGER_MESSENGER, strLines, true));
        }
    }

    public void showHint(String msg) {
        showHint(msg, 500);
    }

    public void showHint(String msg, int length) {
        client.announceHint(msg, length);
    }

    public void silentGiveBuffs(List<Pair<Long, PlayerBuffValueHolder>> buffs) {
        for (Pair<Long, PlayerBuffValueHolder> mbsv : buffs) {
            PlayerBuffValueHolder mbsvh = mbsv.getRight();
            mbsvh.effect.silentApplyBuff(this, mbsv.getLeft());
        }
    }

    public void silentPartyUpdate() {
        silentPartyUpdateInternal(getParty());
    }

    private void silentPartyUpdateInternal(Party chrParty) {
        if (chrParty != null) {
            getWorldServer().updateParty(chrParty.getId(), PartyOperation.SILENT_UPDATE, getMPC());
        }
    }

    public boolean skillIsCooling(int skillId) {
        effLock.lock();
        chrLock.lock();
        try {
            return coolDowns.containsKey(skillId);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void runFullnessSchedule(int petSlot) {
        Pet pet = getPet(petSlot);
        if (pet == null) {
            return;
        }

        int newFullness = pet.getFullness() - PetDataFactory.getHunger(pet.getItemId());
        if (newFullness <= 5) {
            pet.setFullness(15);
            pet.saveToDb();
            unEquipPet(pet, true);
            dropMessage(6, I18nUtil.getMessage("Character.runFullnessSchedule"));
        } else {
            pet.setFullness(newFullness);
            pet.saveToDb();
            Item petz = getInventory(InventoryType.CASH).getItem(pet.getPosition());
            if (petz != null) {
                forceUpdateItem(petz);
            }
        }
    }

    public boolean runTirednessSchedule() {
        if (mapleMount != null) {
            int tiredness = mapleMount.incrementAndGetTiredness();

            this.getMap().broadcastMessage(PacketCreator.updateMount(this.getId(), mapleMount, false));
            if (tiredness > 99) {
                mapleMount.setTiredness(99);
                this.dispelSkill(this.getJobType() * 10000000 + 1004);
                this.dropMessage(6, I18nUtil.getMessage("Character.runTirednessSchedule"));
                return false;
            }
        }

        return true;
    }

    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public void startMapEffect(String msg, int itemId, int duration) {
        final MapEffect mapEffect = new MapEffect(msg, itemId);
        sendPacket(mapEffect.makeStartData());
        TimerManager.getInstance().schedule(() -> sendPacket(mapEffect.makeDestroyData()), duration);
    }

    public void unEquipAllPets() {
        for (int i = 0; i < 3; i++) {
            Pet pet = getPet(i);
            if (pet != null) {
                unEquipPet(pet, true);
            }
        }
    }

    public void unEquipPet(Pet pet, boolean shift_left) {
        unEquipPet(pet, shift_left, false);
    }

    public void unEquipPet(Pet pet, boolean shift_left, boolean hunger) {
        byte petIdx = this.getPetIndex(pet);
        Pet chrPet = this.getPet(petIdx);

        if (chrPet != null) {
            chrPet.setSummoned(false);
            chrPet.saveToDb();
        }

        this.getClient().getWorldServer().unregisterPetHunger(this, petIdx);
        getMap().broadcastMessage(this, PacketCreator.showPet(this, pet, true, hunger), true);

        removePet(pet, shift_left);
        commitExcludedItems();

        sendPacket(PacketCreator.petStatUpdate(this));
        enableActions();
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
        // [新增] 实时保存技能宏
        characterService.saveSkillMacros(id, skillMacros);
    }

    public void updatePartyMemberHP() {
        prtLock.lock();
        try {
            updatePartyMemberHPInternal();
        } finally {
            prtLock.unlock();
        }
    }

    private void updatePartyMemberHPInternal() {
        if (party != null) {
            int curmaxhp = getCurrentMaxHp();
            int curhp = getHp();
            for (Character partychar : this.getPartyMembersOnSameMap()) {
                partychar.sendPacket(PacketCreator.updatePartyMemberHP(getId(), curhp, curmaxhp));
            }
        }
    }

    public void setQuestProgress(int id, int infoNumber, String progress) {
        Quest q = Quest.getInstance(id);
        QuestStatus qs = getQuest(q);

        if (qs.getInfoNumber() == infoNumber && infoNumber > 0) {
            Quest iq = Quest.getInstance(infoNumber);
            QuestStatus iqs = getQuest(iq);
            iqs.setProgress(0, progress);
            // [修改] 调用单个任务更新方法，实现实时保存
            questService.updateQuestStatus(this.id, iqs);
        } else {
            qs.setProgress(infoNumber, progress);   // quest progress is thoroughly a string match, infoNumber is actually another questid
            // [修改] 调用单个任务更新方法，实现实时保存
            questService.updateQuestStatus(this.id, qs);
        }

        announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, false);
        if (qs.getInfoNumber() > 0) {
            announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, true);
        }
    }

    public void awardQuestPoint(int awardedPoints) {
        if (GameConfig.getServerInt("quest_point_requirement") < 1 || awardedPoints < 1) {
            return;
        }

        int delta;
        synchronized (quests) {
            questFame += awardedPoints;

            delta = questFame / GameConfig.getServerInt("quest_point_requirement");
            questFame %= GameConfig.getServerInt("quest_point_requirement");
        }

        if (delta > 0) {
            gainFame(delta);
        }
    }

    private void announceUpdateQuestInternal(Character chr, Pair<DelayedQuestUpdate, Object[]> questUpdate) {
        Object[] objs = questUpdate.getRight();

        switch (questUpdate.getLeft()) {
            case UPDATE -> sendPacket(PacketCreator.updateQuest(chr, (QuestStatus) objs[0], (Boolean) objs[1]));
            case FORFEIT -> sendPacket(PacketCreator.forfeitQuest((Short) objs[0]));
            case COMPLETE -> sendPacket(PacketCreator.completeQuest((Short) objs[0], (Long) objs[1]));
            case INFO -> {
                QuestStatus qs = (QuestStatus) objs[0];
                sendPacket(PacketCreator.updateQuestInfo(qs.getQuest().getId(), qs.getNpc()));
            }
        }
    }

    public void announceUpdateQuest(DelayedQuestUpdate questUpdateType, Object... params) {
        Pair<DelayedQuestUpdate, Object[]> p = new Pair<>(questUpdateType, params);
        Client c = this.getClient();
        if (c.getQM() != null || c.getCM() != null) {
            synchronized (npcUpdateQuests) {
                npcUpdateQuests.add(p);
            }
        } else {
            announceUpdateQuestInternal(this, p);
        }
    }

    public void flushDelayedUpdateQuests() {
        List<Pair<DelayedQuestUpdate, Object[]>> qmQuestUpdateList;

        synchronized (npcUpdateQuests) {
            qmQuestUpdateList = new ArrayList<>(npcUpdateQuests);
            npcUpdateQuests.clear();
        }

        for (Pair<DelayedQuestUpdate, Object[]> q : qmQuestUpdateList) {
            announceUpdateQuestInternal(this, q);
        }
    }

    public void updateQuestStatus(QuestStatus qs) {
        synchronized (quests) {
            quests.put(qs.getQuestID(), qs);
        }

        if (qs.getStatus().equals(QuestStatus.Status.STARTED)) {
            announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, false);
            if (qs.getInfoNumber() > 0) {
                announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, true);
            }
            announceUpdateQuest(DelayedQuestUpdate.INFO, qs);
            // [修改] 任务开始或进度更新时，调用updateQuestStatus
            // 该方法会处理插入或更新逻辑
            questService.updateQuestStatus(this.id, qs);
        } else if (qs.getStatus().equals(QuestStatus.Status.COMPLETED)) {
            Quest mquest = qs.getQuest();
            short questid = mquest.getId();
            if (!mquest.isSameDayRepeatable() && !Quest.isExploitableQuest(questid)) {
                awardQuestPoint(GameConfig.getServerInt("quest_point_per_quest_complete"));
            }
            qs.setCompleted(qs.getCompleted() + 1);

            announceUpdateQuest(DelayedQuestUpdate.COMPLETE, questid, qs.getCompletionTime());
            // [修改] 任务完成时，调用updateQuestStatus
            questService.updateQuestStatus(this.id, qs);
        } else if (qs.getStatus().equals(QuestStatus.Status.NOT_STARTED)) {
            // [修改] 任务被放弃（状态变为NOT_STARTED）时，调用deleteQuestStatus
            // 注意：原有的 announceUpdateQuest 逻辑保持不变，因为客户端需要收到更新通知
            announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, false);
            if (qs.getInfoNumber() > 0) {
                announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, true);
            }
            questService.deleteQuestStatus(this.id, qs.getQuestID());
        }
    }

    public void cancelQuestExpirationTask() {
        evtLock.lock();
        try {
            if (questExpireTask != null) {
                questExpireTask.cancel(false);
                questExpireTask = null;
            }
        } finally {
            evtLock.unlock();
        }
    }

    public void forfeitExpirableQuests() {
        evtLock.lock();
        try {
            for (Quest quest : questExpirations.keySet()) {
                quest.forfeit(this);
            }

            questExpirations.clear();
        } finally {
            evtLock.unlock();
        }
    }

    public void questExpirationTask() {
        evtLock.lock();
        try {
            if (!questExpirations.isEmpty()) {
                if (questExpireTask == null) {
                    questExpireTask = TimerManager.getInstance().register(this::runQuestExpireTask, SECONDS.toMillis(10));
                }
            }
        } finally {
            evtLock.unlock();
        }
    }

    private void runQuestExpireTask() {
        evtLock.lock();
        try {
            long timeNow = Server.getInstance().getCurrentTime();
            List<Quest> expireList = new LinkedList<>();

            for (Entry<Quest, Long> qe : questExpirations.entrySet()) {
                if (qe.getValue() <= timeNow) {
                    expireList.add(qe.getKey());
                }
            }

            if (!expireList.isEmpty()) {
                for (Quest quest : expireList) {
                    quest.expireQuest(this);
                    questExpirations.remove(quest);
                }

                if (questExpirations.isEmpty()) {
                    questExpireTask.cancel(false);
                    questExpireTask = null;
                }
            }
        } finally {
            evtLock.unlock();
        }
    }

    private void registerQuestExpire(Quest quest, long time) {
        evtLock.lock();
        try {
            if (questExpireTask == null) {
                questExpireTask = TimerManager.getInstance().register(this::runQuestExpireTask, SECONDS.toMillis(10));
            }

            questExpirations.put(quest, Server.getInstance().getCurrentTime() + time);
        } finally {
            evtLock.unlock();
        }
    }

    public void questTimeLimit(final Quest quest, int seconds) {
        registerQuestExpire(quest, SECONDS.toMillis(seconds));
        sendPacket(PacketCreator.addQuestTimeLimit(quest.getId(), (int) SECONDS.toMillis(seconds)));
    }

    public void questTimeLimit2(final Quest quest, long expires) {
        long timeLeft = expires - System.currentTimeMillis();

        if (timeLeft <= 0) {
            quest.expireQuest(this);
        } else {
            registerQuestExpire(quest, timeLeft);
        }
    }

    public void updateSingleStat(Stat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    private void updateSingleStat(Stat stat, int newval, boolean itemReaction) {
        sendPacket(PacketCreator.updatePlayerStats(Collections.singletonList(new Pair<>(stat, newval)), itemReaction, this));
    }

    public void sendPacket(Packet packet) {
        if (client != null) {
            client.sendPacket(packet);
        }
    }

    @Override
    public int getObjectId() {
        return getId();
    }

    @Override
    public MapObjectType getType() {
        return MapObjectType.PLAYER;
    }

    @Override
    public void sendDestroyData(Client client) {
        client.sendPacket(PacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(Client client) {
        if (!this.isHidden() || client.getPlayer().gmLevel() > 1) {
            client.sendPacket(PacketCreator.spawnPlayerMapObject(client, this, false));

            if (buffEffects.containsKey(getJobMapChair(job))) { // mustn't effLock, chrLock sendSpawnData
                client.sendPacket(PacketCreator.giveForeignChairSkillEffect(id));
            }
        }

        if (this.isHidden()) {
            List<Pair<BuffStat, Integer>> dsstat = Collections.singletonList(new Pair<>(BuffStat.DARKSIGHT, 0));
            getMap().broadcastGMMessage(this, PacketCreator.giveForeignBuff(getId(), dsstat), false);
        }
    }

    @Override
    public void setObjectId(int id) {
    }

    @Override
    public String toString() {
        return name;
    }

    public Set<NewYearCardRecord> getNewYearRecords() {
        return newyears;
    }

    public Set<NewYearCardRecord> getReceivedNewYearRecords() {
        Set<NewYearCardRecord> received = new LinkedHashSet<>();

        for (NewYearCardRecord nyc : newyears) {
            if (nyc.isReceiverReceivedCard()) {
                received.add(nyc);
            }
        }

        return received;
    }

    public NewYearCardRecord getNewYearRecord(int cardid) {
        for (NewYearCardRecord nyc : newyears) {
            if (nyc.getId() == cardid) {
                return nyc;
            }
        }

        return null;
    }

    public void addNewYearRecord(NewYearCardRecord newyear) {
        newyears.add(newyear);
    }

    public void removeNewYearRecord(NewYearCardRecord newyear) {
        newyears.remove(newyear);
    }

    public void portalDelay(long delay) {
        this.portaldelay = System.currentTimeMillis() + delay;
    }

    public long portalDelay() {
        return portaldelay;
    }

    public void blockPortal(String scriptName) {
        if (!blockedPortals.contains(scriptName) && scriptName != null) {
            blockedPortals.add(scriptName);
            enableActions();
        }
    }

    public void unblockPortal(String scriptName) {
        if (blockedPortals.contains(scriptName) && scriptName != null) {
            blockedPortals.remove(scriptName);
        }
    }

    public boolean containsAreaInfo(int area, String info) {
        short area_ = (short) area;
        if (area_info.containsKey(area_)) {
            return area_info.get(area_).contains(info);
        }
        return false;
    }

    public void updateAreaInfo(int area, String info) {
        area_info.put((short) area, info);
        sendPacket(PacketCreator.updateAreaInfo(area, info));
    }

    public Map<Short, String> getAreaInfos() {
        return area_info;
    }

    public void autoBan(String reason) {
        if (this.isBanned()) { //已被封禁则返回
            return;
        }

        if (this.getAutoBanManager().useAutoBanLog() && this.gmLevel() >= 4) { //高等级GM不封禁，但进行提示
            log.warn("[自动封禁] 玩家 {} GM等级 {} ，触发自动封禁，但GM等级≥4，未被封禁。",this.name,this.gmLevel());
            return;
        }

        this.ban(reason);
        sendPacket(PacketCreator.sendPolice(I18nUtil.getMessage("Character.autoBan.message1")));  //发送自动封禁提示
        this.getAutoBanManager().applyDebuffPunishment(5000); //防止被封禁玩家继续操作。
        TimerManager.getInstance().schedule(() -> client.disconnect(false, false), 5000);

        Server.getInstance().broadcastGMMessage(this.getWorld(), PacketCreator.serverNotice(6, "[自动封禁]" + this.name + " 触发自动封禁，原因：" + reason));
    }

    public void block(int reason, int days, String desc) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, days);
        accountService.update(AccountsDO.builder()
                .id(accountId)
                .banreason(desc)
                .tempban(new Timestamp(cal.getTimeInMillis()))
                .greason(reason)
                .build());
    }

    public List<Integer> getTrockMaps() {
        return trockmaps;
    }

    public List<Integer> getVipTrockMaps() {
        return viptrockmaps;
    }

    public int getTrockSize() {
        int ret = trockmaps.indexOf(MapId.NONE);
        if (ret == -1) {
            ret = 5;
        }

        return ret;
    }

    public void deleteFromTrocks(int map) {
        trockmaps.remove(Integer.valueOf(map));
        while (trockmaps.size() < 10) {
            trockmaps.add(MapId.NONE);
        }
    }

    public void addTrockMap() {
        int index = trockmaps.indexOf(MapId.NONE);
        if (index != -1) {
            trockmaps.set(index, getMapId());
        }
    }

    public boolean isTrockMap(int id) {
        int index = trockmaps.indexOf(id);
        return index != -1;
    }

    // -- Script Trock (万能传送脚本专用) --

    public List<Integer> getScriptsTrockMaps() {
        return scriptTrockMaps;
    }

    public int getScriptsTrockSize() {
        return scriptTrockMaps.size();
    }

    public void addScriptsTrockMap() {
        scriptTrockMaps.add(getMapId());
    }

    public void deleteFromScriptsTrocks(int map) {
        scriptTrockMaps.remove(Integer.valueOf(map));
    }

    public boolean isScriptsTrockMap(int id) {
        return scriptTrockMaps.contains(id);
    }

    public int getVipTrockSize() {
        int ret = viptrockmaps.indexOf(MapId.NONE);

        if (ret == -1) {
            ret = 10;
        }

        return ret;
    }

    public void deleteFromVipTrocks(int map) {
        viptrockmaps.remove(Integer.valueOf(map));
        while (viptrockmaps.size() < 10) {
            viptrockmaps.add(MapId.NONE);
        }
    }

    public void addVipTrockMap() {
        int index = viptrockmaps.indexOf(MapId.NONE);
        if (index != -1) {
            viptrockmaps.set(index, getMapId());
        }
    }

    public boolean isVipTrockMap(int id) {
        int index = viptrockmaps.indexOf(id);
        return index != -1;
    }

    public AutobanManager getAutoBanManager() {
        return autoBan;
    }

    public void setAutoBanManager(AutobanManager autoBan) {
        this.autoBan = autoBan;
    }

    public void equippedItem(Equip equip) {
        int itemid = equip.getItemId();

        if (itemid == ItemId.PENDANT_OF_THE_SPIRIT) {
            this.equipPendantOfSpirit();
        } else if (itemid == ItemId.MESO_MAGNET) {
            equippedMesoMagnet = true;
        } else if (itemid == ItemId.ITEM_POUCH) {
            equippedItemPouch = true;
        } else if (itemid == ItemId.ITEM_IGNORE) {
            equippedPetItemIgnore = true;
        } else if (itemid == ItemId.ITEM_SCALES) {
            equippedPetItemScales = true;
        }
    }

    public void unequippedItem(Equip equip) {
        int itemid = equip.getItemId();

        if (itemid == ItemId.PENDANT_OF_THE_SPIRIT) {
            this.unequipPendantOfSpirit();
        } else if (itemid == ItemId.MESO_MAGNET) {
            equippedMesoMagnet = false;
        } else if (itemid == ItemId.ITEM_POUCH) {
            equippedItemPouch = false;
        } else if (itemid == ItemId.ITEM_IGNORE) {
            equippedPetItemIgnore = false;
        } else if (itemid == ItemId.ITEM_SCALES) {
            equippedPetItemScales = false;
        }
    }

    private void equipPendantOfSpirit() {   //精灵吊坠装备时长经验计算
        if (pendantOfSpirit == null) {
            // 在启动定时器前，先加载一次离线时长（如果开启了配置）
            if (GameConfig.getServerBoolean("offline_pendant_of_spirit_enabled")) {
                String type = "精灵吊坠时长";
                ExtendValueDO extendValueDO = ExtendUtil.getExtendValue(String.valueOf(id), ExtendType.CHARACTER_EXTEND_DAILY.getType(), type);
                if (extendValueDO != null && extendValueDO.getExtendValue() != null) {
                    try {
                        byte savedExp = Byte.parseByte(extendValueDO.getExtendValue());
                        // 只有当保存的值合法且大于当前内存值时才覆盖，防止回档
                        if (savedExp > pendantExp && savedExp <= 3) {
                            pendantExp = savedExp;
                        }
                    } catch (NumberFormatException e) {
                        log.error("玩家 {} 解析精灵吊坠时长出错",getName(), e);
                    }
                }
            }

            pendantOfSpirit = TimerManager.getInstance().register(() -> {
                if (pendantExp < 3) {
                    pendantExp++;

                    // 保存新时长
                    if (GameConfig.getServerBoolean("offline_pendant_of_spirit_enabled")) {
                        String type = "精灵吊坠时长";
                        ExtendUtil.saveOrUpdateExtendValue(String.valueOf(id), ExtendType.CHARACTER_EXTEND_DAILY.getType(), type, String.valueOf(pendantExp));
                    }

                    //用于准确提示装备1小时内还是装备经过几小时
                    message(I18nUtil.getMessage(pendantExp <= 2 ? "Character.equipPendantOfSpirit.message1" : "Character.equipPendantOfSpirit.message2", pendantExp == 3 ? 2 : pendantExp, pendantExp * 10));
                } else {
                    pendantOfSpirit.cancel(false);
                }
            }, 3600000); //1 hour
        }
    }

    private void unequipPendantOfSpirit() {
        if (pendantOfSpirit != null) {
            pendantOfSpirit.cancel(false);
            pendantOfSpirit = null;
        }
        pendantExp = 0;
    }

    private Collection<Item> getUpgradeableEquipList() {
        Collection<Item> fullList = getInventory(InventoryType.EQUIPPED).list();
        if (GameConfig.getServerBoolean("use_equipment_level_up_cash")) {
            return fullList;
        }

        Collection<Item> eqpList = new LinkedHashSet<>();
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Item it : fullList) {
            if (!ii.isCash(it.getItemId())) {
                eqpList.add(it);
            }
        }

        return eqpList;
    }

    public void increaseEquipExp(int expGain) {
        if (allowExpGain) {     // thanks Vcoc for suggesting equip EXP gain conditionally
            if (expGain < 0) {
                expGain = Integer.MAX_VALUE;
            }

            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            for (Item item : getUpgradeableEquipList()) {
                Equip nEquip = (Equip) item;
                String itemName = ii.getName(nEquip.getItemId());
                if (itemName == null) {
                    continue;
                }

                nEquip.gainItemExp(client, expGain);
            }
        }
    }

    public void showAllEquipFeatures() {
        StringBuilder showMsg = new StringBuilder();

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Item item : getInventory(InventoryType.EQUIPPED).list()) {
            Equip nEquip = (Equip) item;
            String itemName = ii.getName(nEquip.getItemId());
            if (itemName == null) {
                continue;
            }

            showMsg.append(nEquip.showEquipFeatures(client));
        }

        if (!showMsg.isEmpty()) {
            this.showHint("#ePLAYER EQUIPMENTS:#n\r\n\r\n" + showMsg, 400);
        }
    }

    public void broadcastMarriageMessage() {
        Guild guild = this.getGuild();
        if (guild != null) {
            guild.broadcast(PacketCreator.marriageMessage(0, name));
        }

        Family family = this.getFamily();
        if (family != null) {
            family.broadcast(PacketCreator.marriageMessage(1, name));
        }
    }

    public void setCpqTimer(ScheduledFuture<?> timer) {
        this.cpqSchedule = timer;
    }

    public void clearCpqTimer() {
        if (cpqSchedule != null) {
            cpqSchedule.cancel(true);
        }
        cpqSchedule = null;
    }

    public final void empty(final boolean remove) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(true);
        }
        dragonBloodSchedule = null;

        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(true);
        }
        hpDecreaseTask = null;

        if (beholderHealingSchedule != null) {
            beholderHealingSchedule.cancel(true);
        }
        beholderHealingSchedule = null;

        if (beholderBuffSchedule != null) {
            beholderBuffSchedule.cancel(true);
        }
        beholderBuffSchedule = null;

        if (berserkSchedule != null) {
            berserkSchedule.cancel(true);
        }
        berserkSchedule = null;

        unregisterChairBuff();
        cancelBuffExpireTask();
        cancelDiseaseExpireTask();
        cancelSkillCooldownTask();
        cancelExpirationTask();

        if (questExpireTask != null) {
            questExpireTask.cancel(true);
        }
        questExpireTask = null;

        if (recoveryTask != null) {
            recoveryTask.cancel(true);
        }
        recoveryTask = null;

        if (extraRecoveryTask != null) {
            extraRecoveryTask.cancel(true);
        }
        extraRecoveryTask = null;

        // already done on unregisterChairBuff
        /* if (chairRecoveryTask != null) { chairRecoveryTask.cancel(true); }
        chairRecoveryTask = null; */

        if (pendantOfSpirit != null) {
            pendantOfSpirit.cancel(true);
        }
        pendantOfSpirit = null;

        clearCpqTimer();

        evtLock.lock();
        try {
            if (questExpireTask != null) {
                questExpireTask.cancel(false);
                questExpireTask = null;

                questExpirations.clear();
                questExpirations = null;
            }
        } finally {
            evtLock.unlock();
        }

        if (mapleMount != null) {
            mapleMount.empty();
            mapleMount = null;
        }
        if (remove) {
            partyQuest = null;
            events = null;
            mpc = null;
            mgc = null;
            party = null;
            FamilyEntry familyEntry = getFamilyEntry();
            if (familyEntry != null) {
                familyEntry.setCharacter(null);
                setFamilyEntry(null);
            }

            getWorldServer().registerTimedMapObject(() -> {
                client = null;  // clients still triggers handlers a few times after disconnecting
                map = null;
                setListener(null);

                // thanks Shavit for noticing a memory leak with inventories holding owner object
                for (Inventory items : inventory) {
                    items.dispose();
                }
                inventory = null;
            }, MINUTES.toMillis(5));
        }
    }

    public void logOff() {
        this.loggedIn = false;
        characterService.update(CharactersDO.builder()
                .id(id)
                .lastLogoutTime(new Timestamp(System.currentTimeMillis()))
                .build());
    }


    public long getLoggedInTime() {
        return System.currentTimeMillis() - loginTime;
    }

    public boolean getWhiteChat() {
        return isGM() && whiteChat;
    }

    public void toggleWhiteChat() {
        whiteChat = !whiteChat;
    }

    public boolean gotPartyQuestItem(String partyquestchar) {
        return dataString.contains(partyquestchar);
    }

    public void removePartyQuestItem(String letter) {
        if (gotPartyQuestItem(letter)) {
            dataString = dataString.substring(0, dataString.indexOf(letter)) + dataString.substring(dataString.indexOf(letter) + letter.length());
        }
    }

    public void setPartyQuestItemObtained(String partyquestchar) {
        if (!dataString.contains(partyquestchar)) {
            this.dataString += partyquestchar;
        }
    }

    public void createDragon() {
        dragon = new Dragon(this);
    }

    // ===================== 监狱系统 (Jail System) =====================

    public boolean isJailed() {
        return jailExpiration > 0;
    }

    public long getJailExpirationTimeLeft() {
        return Math.max(0, jailExpiration);
    }

    /**
     * [GM命令] 关入监狱 / 延长刑期。
     * 自动处理：加刑期 → 保存现场 → 传送监狱 → 客户端倒计时 → 剩余时间提示。
     */
    public void imprison(int minutes) {
        addJailDuration(TimeUnit.MINUTES.toMillis(minutes));
        if (getMapId() != MapId.JAIL) {
            saveLocation("JAIL");
            changeMap(MapId.JAIL);
        }
        refreshJailDisplay();
    }

    /**
     * [登录恢复] 上线时自动恢复服刑状态：确认在监狱、显示倒计时和剩余时间。
     */
    public void resumeJailSentence() {
        if (!isJailed()) return;
        if (getMapId() != MapId.JAIL) {
            saveLocation("JAIL");
            changeMap(MapId.JAIL);
        }
        refreshJailDisplay();
    }

    /**
     * [OnlineTimeTask] 服刑倒计时 tick（每5秒），在线期间递减，归零自动释放。
     */
    public void tickJailCountdown(long deltaMs) {
        if (!isJailed()) return;
        jailExpiration -= deltaMs;
        if (jailExpiration <= 0) {
            releaseFromJail();
        } else {
            sendPacket(PacketCreator.getClock((int) Math.max(1, jailExpiration / 1000)));
        }
    }

    /**
     * [刑满释放/!unjail] 倒计时归零或GM强制释放时调用。
     * 清除服刑状态 → 移除倒计时 → 送回入狱前地图 → 提示消息。
     */
    public void releaseFromJail() {
        removeJailTimeAndClock();
        message(I18nUtil.getMessage("JailCommand.message6"));
        int returnMap = getSavedLocation("JAIL");
        changeMap(returnMap != -1 && returnMap != MapId.NONE ? returnMap : MapId.FM_ENTRANCE);
    }

    /**
     * [GM强制释放] 仅清除服刑状态和客户端倒计时，不传送玩家。
     */
    public void cancelImprisonment() {
        removeJailTimeAndClock();
    }

    // ----- 内部方法 -----

    private void addJailDuration(long ms) {
        jailExpiration = (jailExpiration <= 0) ? ms : jailExpiration + ms;
    }

    private void removeJailTimeAndClock() {
        jailExpiration = 0;
        sendPacket(PacketCreator.removeClock());
    }

    private void refreshJailDisplay() {
        long ms = getJailExpirationTimeLeft();
        int sec = (int) Math.max(1, ms / 1000);
        sendPacket(PacketCreator.getClock(sec));
        message(I18nUtil.getMessage("JailCommand.message7",
                ms / 3600000, (ms % 3600000) / 60000, (ms % 60000) / 1000));
    }

    public boolean registerNameChange(String newName) {
        try {
            if (nameChangeService.registerNameChange(this, newName)) {
                pendingNameChange = true;
                return true;
            }
        } catch (Exception e) {
            log.error(I18nUtil.getLogMessage("Character.registerNameChange.error1"), getName(), newName, e);
        }
        return false;
    }

    public boolean cancelPendingNameChange() {
        try {
            nameChangeService.cancelPendingNameChange(this, true);
            return true;
        } catch (Exception e) {
            log.error(I18nUtil.getLogMessage("Character.cancelPendingNameChange.error1"), getName(), e);
            return false;
        }
    }

    public void doPendingNameChange() { //called on logout
        if (!pendingNameChange) {
            return;
        }
        nameChangeService.applyNameChange(getId(), getName());
    }

    public int checkWorldTransferEligibility() {
        if (getLevel() < 20) {
            return 2;
        } else if (getClient().getTempBanCalendar() != null && getClient().getTempBanCalendar().getTimeInMillis() + (int) DAYS.toMillis(30) < Calendar.getInstance().getTimeInMillis()) {
            return 3;
        } else if (isMarried()) {
            return 4;
        } else if (getGuildRank() < 2) {
            return 5;
        } else if (getFamily() != null) {
            return 8;
        } else {
            return 0;
        }
    }

    public boolean registerWorldTransfer(int newWorld) {
        try {
            return worldTransferService.registerWorldTransfer(this, newWorld);
        } catch (Exception e) {
            log.error(I18nUtil.getLogMessage("Character.registerWorldTransfer.error1"), getName(), newWorld, e);
        }
        return false;
    }

    public boolean cancelPendingWorldTransfer() {
        try {
            worldTransferService.cancelPendingWorldTransfer(this, true);
            return true;
        } catch (Exception e) {
            log.error(I18nUtil.getLogMessage("Character.cancelPendingWorldTransfer.error1"), getName(), e);
            return false;
        }
    }

    public String getLastCommandMessage() {
        return this.commandtext;
    }

    public void setLastCommandMessage(String text) {
        this.commandtext = text;
    }

    public int getRewardPoints() {
        AccountsDO accountsDO = accountService.findById(accountId);
        return accountsDO == null ? -1 : Optional.ofNullable(accountsDO.getRewardpoints()).orElse(-1);
    }

    public void setRewardPoints(int value) {
        accountService.update(AccountsDO.builder()
                .id(accountId)
                .rewardpoints(value)
                .build());
    }

    public void setReborns(int value) {
        if (!GameConfig.getServerBoolean("use_rebirth_system")) {
            yellowMessage(I18nUtil.getMessage("Character.USE_REBIRTH_SYSTEM")); //重生系统未启用
            throw new NotEnabledException();
        }

        characterService.update(CharactersDO.builder()
                .id(id)
                .reborns(value)
                .build());
    }

    public void addReborns() {
        setReborns(getReborns() + 1);
    }

    public int getReborns() {
        if (!GameConfig.getServerBoolean("use_rebirth_system")) {
            yellowMessage(I18nUtil.getMessage("Character.USE_REBIRTH_SYSTEM")); //重生系统未启用
            throw new NotEnabledException();
        }

        CharactersDO charactersDO = characterService.findById(id);
        return charactersDO == null ? 0 : Optional.ofNullable(charactersDO.getReborns()).orElse(0);
    }

    public void executeRebornAsId(int jobId) {
        executeRebornAs(Job.getById(jobId));
    }

    public void executeRebornAs(Job job) {
        if (!GameConfig.getServerBoolean("use_rebirth_system")) {
            yellowMessage(I18nUtil.getMessage("Character.USE_REBIRTH_SYSTEM")); //重生系统未启用
            throw new NotEnabledException();
        }
        if (getLevel() != getMaxClassLevel()) {
            return;
        }
        addReborns();
        changeJob(job);
        setLevel(0);
        levelUp(true);
    }

    //EVENTS
    @Getter
    private byte team = 0;
    @Setter
    @Getter
    private Fitness fitness;
    @Setter
    @Getter
    private Ola ola;
    private long snowballattack;

    public void setTeam(int team) {
        this.team = (byte) team;
    }

    public long getLastSnowballAttack() {
        return snowballattack;
    }

    public void setLastSnowballAttack(long time) {
        this.snowballattack = time;
    }

    // MCPQ

    @Setter
    @Getter
    public AriantColiseum ariantColiseum;
    @Setter
    @Getter
    private MonsterCarnival monsterCarnival;
    @Setter
    @Getter
    private MonsterCarnivalParty monsterCarnivalParty = null;

    private int cp = 0;
    private int totCP = 0;
    @Setter
    @Getter
    private int FestivalPoints;
    @Setter
    @Getter
    private boolean challenged = false;

    public void gainFestivalPoints(int gain) {
        this.FestivalPoints += gain;
    }

    public int getCP() {
        return cp;
    }

    public void gainCP(int gain) {
        if (this.getMonsterCarnival() != null) {
            if (gain > 0) {
                this.setTotalCP(this.getTotalCP() + gain);
            }
            this.setCP(this.getCP() + gain);
            if (this.getParty() != null) {
                this.getMonsterCarnival().setCP(this.getMonsterCarnival().getCP(team) + gain, team);
                if (gain > 0) {
                    this.getMonsterCarnival().setTotalCP(this.getMonsterCarnival().getTotalCP(team) + gain, team);
                }
            }
            if (this.getCP() > this.getTotalCP()) {
                this.setTotalCP(this.getCP());
            }
            sendPacket(PacketCreator.CPUpdate(false, this.getCP(), this.getTotalCP(), getTeam()));
            if (this.getParty() != null && getTeam() != -1) {
                this.getMap().broadcastMessage(PacketCreator.CPUpdate(true, this.getMonsterCarnival().getCP(team), this.getMonsterCarnival().getTotalCP(team), getTeam()));
            }
        }
    }

    public void setTotalCP(int a) {
        this.totCP = a;
    }

    public void setCP(int a) {
        this.cp = a;
    }

    public int getTotalCP() {
        return totCP;
    }

    public void resetCP() {
        this.cp = 0;
        this.totCP = 0;
        this.monsterCarnival = null;
    }

    public void gainAriantPoints(int points) {
        this.ariantPoints += points;
    }

    /**
     * 发装备，除id外都可以传null，传null取装备默认属性
     *
     * @param itemId      装备id
     * @param attStr      力量
     * @param attDex      敏捷
     * @param attInt      智力
     * @param attLuk      运气
     * @param attHp       血量
     * @param attMp       蓝量
     * @param pAtk        物理攻击
     * @param mAtk        魔法攻击
     * @param pDef        物理防御
     * @param mDef        魔法防御
     * @param acc         命中
     * @param avoid       回避
     * @param hands       攻击速度
     * @param speed       移动速度
     * @param jump        跳跃
     * @param upgradeSlot 可升级次数
     * @param expireTime  失效时间，-1为不失效 来自 @leevccc 的建议，传值则为分钟
     */
    public boolean gainEquip(int itemId, Short attStr, Short attDex, Short attInt, Short attLuk, Short attHp, Short attMp,
                          Short pAtk, Short mAtk, Short pDef, Short mDef, Short acc, Short avoid, Short hands, Short speed,
                          Short jump, Byte upgradeSlot, Long expireTime) {
        Equip baseEquip = (Equip) ItemInformationProvider.getInstance().getEquipById(itemId);
        if (!ItemConstants.getInventoryType(itemId).equals(InventoryType.EQUIP) || baseEquip == null) {
            message(I18nUtil.getMessage("AbstractPlayerInteraction.gainEquip.message1"));
            return false;
        }
        baseEquip.setQuantity((short) 1);
        if (!InventoryManipulator.checkSpace(getClient(), itemId, 1, baseEquip.getOwner())) {
            message(I18nUtil.getMessage("AbstractPlayerInteraction.gainEquip.message2", InventoryType.EQUIP.getName()));
            return false;
        }
        RequireUtil.requireNotEmptyAndThen(baseEquip, attStr, Equip::setStr);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attDex, Equip::setDex);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attInt, Equip::setInt);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attLuk, Equip::setLuk);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attHp, Equip::setHp);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attMp, Equip::setMp);
        RequireUtil.requireNotEmptyAndThen(baseEquip, pAtk, Equip::setWatk);
        RequireUtil.requireNotEmptyAndThen(baseEquip, mAtk, Equip::setMatk);
        RequireUtil.requireNotEmptyAndThen(baseEquip, pDef, Equip::setWdef);
        RequireUtil.requireNotEmptyAndThen(baseEquip, mDef, Equip::setMdef);
        RequireUtil.requireNotEmptyAndThen(baseEquip, acc, Equip::setAcc);
        RequireUtil.requireNotEmptyAndThen(baseEquip, avoid, Equip::setAvoid);
        RequireUtil.requireNotEmptyAndThen(baseEquip, hands, Equip::setHands);
        RequireUtil.requireNotEmptyAndThen(baseEquip, speed, Equip::setSpeed);
        RequireUtil.requireNotEmptyAndThen(baseEquip, jump, Equip::setJump);
        RequireUtil.requireNotEmptyAndThen(baseEquip, upgradeSlot, Equip::setUpgradeSlots);
        RequireUtil.requireNotEmptyAndThen(baseEquip, expireTime, (eq, ep) -> {
            if (ep > 0) {
                eq.setExpiration(TimeUnit.MINUTES.toMillis(ep) + System.currentTimeMillis());
            } else {
                eq.setExpiration(-1);
            }
        });
        return InventoryManipulator.addFromDrop(getClient(), baseEquip, false);
    }

    /**
     * 发装备，支持自定义强化次数、道具等级和Flag
     *
     * @param itemId      装备id
     * @param attStr      力量
     * @param attDex      敏捷
     * @param attInt      智力
     * @param attLuk      运气
     * @param attHp       血量
     * @param attMp       蓝量
     * @param pAtk        物理攻击
     * @param mAtk        魔法攻击
     * @param pDef        物理防御
     * @param mDef        魔法防御
     * @param acc         命中
     * @param avoid       回避
     * @param hands       攻击速度
     * @param speed       移动速度
     * @param jump        跳跃
     * @param upgradeSlot 可升级次数
     * @param level       已升级次数
     * @param itemLevel   道具等级
     * @param expireTime  失效时间
     * @param flag        物品标记
     */
    public boolean gainEquip(int itemId, Short attStr, Short attDex, Short attInt, Short attLuk, Short attHp, Short attMp,
                             Short pAtk, Short mAtk, Short pDef, Short mDef, Short acc, Short avoid, Short hands, Short speed,
                             Short jump, Byte upgradeSlot, Short level, Short itemLevel, Long expireTime,String owner, Short flag) {
        Equip baseEquip = (Equip) ItemInformationProvider.getInstance().getEquipById(itemId);
        if (!ItemConstants.getInventoryType(itemId).equals(InventoryType.EQUIP) || baseEquip == null) {
            message(I18nUtil.getMessage("AbstractPlayerInteraction.gainEquip.message1"));
            return false;
        }
        baseEquip.setQuantity((short) 1);
        if (!InventoryManipulator.checkSpace(getClient(), itemId, 1, baseEquip.getOwner())) {
            message(I18nUtil.getMessage("AbstractPlayerInteraction.gainEquip.message2", InventoryType.EQUIP.getName()));
            return false;
        }
        RequireUtil.requireNotEmptyAndThen(baseEquip, attStr, Equip::setStr);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attDex, Equip::setDex);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attInt, Equip::setInt);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attLuk, Equip::setLuk);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attHp, Equip::setHp);
        RequireUtil.requireNotEmptyAndThen(baseEquip, attMp, Equip::setMp);
        RequireUtil.requireNotEmptyAndThen(baseEquip, pAtk, Equip::setWatk);
        RequireUtil.requireNotEmptyAndThen(baseEquip, mAtk, Equip::setMatk);
        RequireUtil.requireNotEmptyAndThen(baseEquip, pDef, Equip::setWdef);
        RequireUtil.requireNotEmptyAndThen(baseEquip, mDef, Equip::setMdef);
        RequireUtil.requireNotEmptyAndThen(baseEquip, acc, Equip::setAcc);
        RequireUtil.requireNotEmptyAndThen(baseEquip, avoid, Equip::setAvoid);
        RequireUtil.requireNotEmptyAndThen(baseEquip, hands, Equip::setHands);
        RequireUtil.requireNotEmptyAndThen(baseEquip, speed, Equip::setSpeed);
        RequireUtil.requireNotEmptyAndThen(baseEquip, jump, Equip::setJump);
        RequireUtil.requireNotEmptyAndThen(baseEquip, upgradeSlot, Equip::setUpgradeSlots);
        RequireUtil.requireNotEmptyAndThen(baseEquip, level, Equip::setLevel);
        RequireUtil.requireNotEmptyAndThen(baseEquip, itemLevel, Equip::setItemLevel);
        RequireUtil.requireNotEmptyAndThen(baseEquip, owner, Equip::setOwner);
        RequireUtil.requireNotEmptyAndThen(baseEquip, flag, Equip::setFlag);
        RequireUtil.requireNotEmptyAndThen(baseEquip, expireTime, (eq, ep) -> {
            if (ep > 0) {
                eq.setExpiration(TimeUnit.MINUTES.toMillis(ep) + System.currentTimeMillis());
            } else {
                eq.setExpiration(-1);
            }
        });
        boolean result = InventoryManipulator.addFromDrop(getClient(), baseEquip, true);
        if (result) traceabilityService.log(baseEquip, this, TraceabilityService.ActionType.ADMIN, TraceabilityService.ActionSourceType.ADMIN_CREATE, 1, "后台发放",null);
        return result;
    }

    public void setFamilyBuff(boolean type, float exp, float drop) {
        this.familyBuff = type;
        this.familyExp = exp;
        this.familyDrop = drop;
    }

    public void startFamilyBuffTimer(int delay) {
        if (FamilyBuffTimer != null && !FamilyBuffTimer.isCancelled()) {
            FamilyBuffTimer.cancel(false);
        }
        FamilyBuffTimer = TimerManager.getInstance().schedule(() -> {
            try {
                sendPacket(PacketCreator.cancelFamilyBuff());
            } finally {
                cancelFamilyBuffTimer();
            }
        }, delay);
    }

    public void cancelFamilyBuffTimer() {
        if (FamilyBuffTimer != null && !FamilyBuffTimer.isCancelled()) {
            FamilyBuffTimer.cancel(false);
            setFamilyBuff(false, 1, 1);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    //module: 角色在线时间
    private int m_iCurrentOnlineTime = -1;//-1用于服务器重启时角色初始变量时间

    public int getCurrentOnlineTime() {
        return this.m_iCurrentOnlineTime;
    }

    public void setCurrentOnlineTime(final int iTime) {
        this.m_iCurrentOnlineTime = iTime;
    }

    public void updateOnlineTime() {
        if (m_iCurrentOnlineTime > 0) { //在线时间小于0不保存，避免刚上线立马掉线导致在线时间清0
            String strNewOnlineTime = String.valueOf(m_iCurrentOnlineTime);
            getAbstractPlayerInteraction().saveOrUpdateAccountExtendValue("每日在线时间", strNewOnlineTime, true);
        }
    }

    /**
     * 根据地图ID获取地图对象
     * @param mapid 地图ID
     * @param showMsg true = 地图不存在时弹出提示，false = 不提示
     * @return MapleMap 地图对象，如果地图不存在且showMsg为true则返回null并发送提示消息
     */
    public MapleMap getMap(int mapid, boolean showMsg) {
        MapleMap map = null;
        try {
            map = client.getChannelServer().getMapFactory().getMap(mapid);
        } catch (Exception ignored) {
        }
        if (map == null && showMsg) {
            String msg = I18nUtil.getMessage("Character.Map.Change.message1", Integer.toString(mapid));
            log.warn(I18nUtil.getLogMessage("Character.Map.Change.warn1"), getName(), getMap().getMapName(), getMapId(),
                    I18nUtil.getLogMessage("SystemRescue.info.map.message1"),
                    mapid);
            dropMessage(5, msg);                 //聊天窗红色消息提示
            dropMessage(1, msg);                 //弹窗消息
            enableActions();
        }
        return map;
    }

    /**
     * 通知客户端启用操作，解除假死
     */
    public void enableActions() {
        sendPacket(PacketCreator.enableActions());
    }

    /**
     * 从扩展表中获取账号或角色的扩展值
     * 
     * @param extendId 扩展ID（账号ID或角色ID）
     * @param extendType 扩展类型，11-账号，12-账号日清，13-账号周清；21-角色，22-角色日清，23-角色周清
     * @param extendName 扩展字段名称
     * @return 扩展值对象
     */
    public ExtendValueDO getExtendValue(String extendId, String extendType, String extendName) {
        return ExtendUtil.getExtendValue(extendId, extendType, extendName);
    }

    /**
     * 保存或更新账号或角色的扩展值到数据库
     * 
     * @param extendId 扩展ID（账号ID或角色ID）
     * @param extendType 扩展类型，11-账号，12-账号日清，13-账号周清；21-角色，22-角色日清，23-角色周清
     * @param extendName 扩展字段名称
     * @param extendValue 扩展字段值
     */
    public void saveOrUpdateExtendValue(String extendId, String extendType, String extendName, String extendValue) {
        ExtendUtil.saveOrUpdateExtendValue(extendId, extendType, extendName, extendValue);
    }

    /**
     * 从扩展表中获取账号的扩展值
     * 
     * @param accountId 账号ID
     * @param extendType 扩展类型，11-账号，12-账号日清，13-账号周清
     * @param extendName 扩展字段名称
     * @return 扩展值对象
     */
    public ExtendValueDO getAccountExtendValue(int accountId, String extendType, String extendName) {
        return getExtendValue(String.valueOf(accountId), extendType, extendName);
    }

    /**
     * 保存或更新账号的扩展值到数据库
     * 
     * @param accountId 账号ID
     * @param extendType 扩展类型，11-账号，12-账号日清，13-账号周清
     * @param extendName 扩展字段名称
     * @param extendValue 扩展字段值
     */
    public void saveOrUpdateAccountExtendValue(int accountId, String extendType, String extendName, String extendValue) {
        saveOrUpdateExtendValue(String.valueOf(accountId), extendType, extendName, extendValue);
    }

    /**
     * 从扩展表中获取角色的扩展值
     * 
     * @param characterId 角色ID
     * @param extendType 扩展类型，21-角色，22-角色日清，23-角色周清
     * @param extendName 扩展字段名称
     * @return 扩展值对象
     */
    public ExtendValueDO getCharacterExtendValue(int characterId, String extendType, String extendName) {
        return getExtendValue(String.valueOf(characterId), extendType, extendName);
    }

    /**
     * 保存或更新角色的扩展值到数据库
     * 
     * @param characterId 角色ID
     * @param extendType 扩展类型，21-角色，22-角色日清，23-角色周清
     * @param extendName 扩展字段名称
     * @param extendValue 扩展字段值
     */
    public void saveOrUpdateCharacterExtendValue(int characterId, String extendType, String extendName, String extendValue) {
        saveOrUpdateExtendValue(String.valueOf(characterId), extendType, extendName, extendValue);
    }
    public void updateCharacterDetails(UpdateCharacterReqDTO request) {
        effLock.lock();
        statWlock.lock();
        try {
            List<Pair<Stat, Integer>> stats = new ArrayList<>();
            boolean lookChanged = false;

            if (request.getName() != null && !request.getName().equals(getName())) {
                setName(request.getName());
                // 名字变更通常需要重登或特殊处理，这里仅更新内存
            }
            if (request.getLevel() != null && request.getLevel() != getLevel()) {
                setLevel(request.getLevel());
                stats.add(new Pair<>(Stat.LEVEL, request.getLevel()));
            }
            if (request.getJob() != null && request.getJob() != getJob().getId()) {
                setJob(Job.getById(request.getJob()));
                stats.add(new Pair<>(Stat.JOB, request.getJob()));
            }
            if (request.getStr() != null && request.getStr() != getStr()) {
                setStr(request.getStr());
                stats.add(new Pair<>(Stat.STR, request.getStr()));
            }
            if (request.getDex() != null && request.getDex() != getDex()) {
                setDex(request.getDex());
                stats.add(new Pair<>(Stat.DEX, request.getDex()));
            }
            if (request.getIntAttr() != null && request.getIntAttr() != getInt()) {
                setInt(request.getIntAttr());
                stats.add(new Pair<>(Stat.INT, request.getIntAttr()));
            }
            if (request.getLuk() != null && request.getLuk() != getLuk()) {
                setLuk(request.getLuk());
                stats.add(new Pair<>(Stat.LUK, request.getLuk()));
            }
            if (request.getHp() != null && request.getHp() != getHp()) {
                setHp(request.getHp());
                stats.add(new Pair<>(Stat.HP, request.getHp()));
            }
            if (request.getMaxHp() != null && request.getMaxHp() != getMaxHp()) {
                setMaxHp(request.getMaxHp());
                stats.add(new Pair<>(Stat.MAXHP, request.getMaxHp()));
            }
            if (request.getMp() != null && request.getMp() != getMp()) {
                setMp(request.getMp());
                stats.add(new Pair<>(Stat.MP, request.getMp()));
            }
            if (request.getMaxMp() != null && request.getMaxMp() != getMaxMp()) {
                setMaxMp(request.getMaxMp());
                stats.add(new Pair<>(Stat.MAXMP, request.getMaxMp()));
            }
            if (request.getAp() != null && request.getAp() != getRemainingAp()) {
                setRemainingAp(request.getAp());
                stats.add(new Pair<>(Stat.AVAILABLEAP, request.getAp()));
            }
            if (request.getSp() != null) {
                String[] sps = request.getSp().split(",");
                int[] spInts = new int[sps.length];
                boolean spChanged = false;
                int[] currentSps = getRemainingSps();

                if (spInts.length == currentSps.length) {
                    for (int i = 0; i < sps.length; i++) {
                        spInts[i] = Integer.parseInt(sps[i]);
                        if (spInts[i] != currentSps[i]) {
                            spChanged = true;
                        }
                    }
                } else {
                    // 长度不同，肯定变了（虽然理论上长度应该一致）
                    for (int i = 0; i < sps.length; i++) {
                        spInts[i] = Integer.parseInt(sps[i]);
                    }
                    spChanged = true;
                }

                if (spChanged) {
                    setRemainingSp(spInts);
                    // 发送当前职业技能书的SP更新
                    stats.add(new Pair<>(Stat.AVAILABLESP, getRemainingSp(getJob().getId())));
                }
            }
            if (request.getFame() != null && request.getFame() != getFame()) {
                setFame(request.getFame());
                stats.add(new Pair<>(Stat.FAME, request.getFame()));
            }

            if (request.getFace() != null && request.getFace() != getFace()) {
                setFace(request.getFace());
                stats.add(new Pair<>(Stat.FACE, request.getFace()));
                lookChanged = true;
            }
            if (request.getHair() != null && request.getHair() != getHair()) {
                setHair(request.getHair());
                stats.add(new Pair<>(Stat.HAIR, request.getHair()));
                lookChanged = true;
            }
            if (request.getSkinColor() != null && request.getSkinColor() != getSkinColor().getId()) {
                setSkinColor(SkinColor.getById(request.getSkinColor()));
                stats.add(new Pair<>(Stat.SKIN, request.getSkinColor()));
                lookChanged = true;
            }
            if (request.getGender() != null && request.getGender() != getGender()) {
                setGender(request.getGender());
                lookChanged = true;
            }
            if (request.getExp() != null && request.getExp() != getExp()) {
                setExp(request.getExp());
                stats.add(new Pair<>(Stat.EXP, getExp()));
            }
            if (request.getMeso() != null && request.getMeso() != getMeso()) {
                setMeso(request.getMeso());
                stats.add(new Pair<>(Stat.MESO, getMeso()));
            }
            if (request.getGm() != null && request.getGm() != gmLevel()) {
                setGMLevel(request.getGm());
                // GM等级变更可能需要更复杂的处理，但目前仅设置等级
            }
            if (request.getEquipSlots() != null) {
                gainSlots(InventoryType.EQUIP.getType(), request.getEquipSlots() - getSlots(InventoryType.EQUIP.getType()), true);
            }
            if (request.getUseSlots() != null) {
                gainSlots(InventoryType.USE.getType(), request.getUseSlots() - getSlots(InventoryType.USE.getType()), true);
            }
            if (request.getSetupSlots() != null) {
                gainSlots(InventoryType.SETUP.getType(), request.getSetupSlots() - getSlots(InventoryType.SETUP.getType()), true);
            }
            if (request.getEtcSlots() != null) {
                gainSlots(InventoryType.ETC.getType(), request.getEtcSlots() - getSlots(InventoryType.ETC.getType()), true);
            }
            if (request.getBuddyCapacity() != null && request.getBuddyCapacity() != getBuddylist().getCapacity()) {
                setBuddyCapacity(request.getBuddyCapacity());
            }
            if (request.getMerchantMesos() != null && request.getMerchantMesos() != getMerchantMeso()) {
                setMerchantMeso(request.getMerchantMesos());
            }
            if (request.getGachaExp() != null && request.getGachaExp() != getGachaExp()) {
                setGachaExp(request.getGachaExp());
                stats.add(new Pair<>(Stat.GACHAEXP, getGachaExp()));
            }
            if (!getCashShop().isOpened()) {
                if (request.getMap() != null && request.getMap() != getMapId()) {
                    setInitialSpawnPoint(request.getSpawnPoint());
                    changeMap(request.getMap(), request.getSpawnPoint());
                }
                if (request.getSpawnPoint() != null && request.getSpawnPoint() != getInitialSpawnPoint()) {
                    setInitialSpawnPoint(request.getSpawnPoint());
                    changeMap(request.getMap(), request.getSpawnPoint());
                }
            }
            if (request.getMountLevel() != null || request.getMountExp() != null || request.getMountTiredness() != null) {
                if (getMapleMount() != null) {
                    if (request.getMountLevel() != null) getMapleMount().setLevel(request.getMountLevel());
                    if (request.getMountExp() != null) getMapleMount().setExp(request.getMountExp());
                    if (request.getMountTiredness() != null) getMapleMount().setTiredness(request.getMountTiredness());
                    getMap().broadcastMessage(PacketCreator.updateMount(getId(), getMapleMount(), false));
                }
            }
            // 处理货币变更
            CashShop cs = getCashShop();
            boolean cashChanged = false;
            if (request.getNxCredit() != null && !request.getNxCredit().equals(cs.getCash(CashShop.NX_CREDIT))) {
                cs.setNxCredit(request.getNxCredit());
                cashChanged = true;
            }
            if (request.getMaplePoint() != null && !request.getMaplePoint().equals(cs.getCash(CashShop.MAPLE_POINT))) {
                cs.setMaplePoint(request.getMaplePoint());
                cashChanged = true;
            }
            if (request.getNxPrepaid() != null && !request.getNxPrepaid().equals(cs.getCash(CashShop.NX_PREPAID))) {
                cs.setNxPrepaid(request.getNxPrepaid());
                cashChanged = true;
            }

            if (cashChanged) {
                // 更新货币后，需要通知客户端。通常这会在进入商城时自动同步，
                // 但为了在游戏中实时看到（如果UI支持），可能需要一个特定的封包。
                // 此处我们先更新内存中的值，并保存。
                cs.save();
            }
            // 只有当属性确实发生变化时才重新计算和发送包
            if (!stats.isEmpty() || lookChanged) {
                recalcLocalStats();
                // updateLocalStats(); // 移除这个调用，避免重复发送不完整的包

                if (!stats.isEmpty()) {
                    sendPacket(PacketCreator.updatePlayerStats(stats, true, this));
                }

                if (lookChanged) {
                    equipChanged();
                }

//                saveCharToDB(); // 只有变化了才保存
            }

//            enableActions(); // 总是启用操作，防止客户端卡死
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }
    }
}