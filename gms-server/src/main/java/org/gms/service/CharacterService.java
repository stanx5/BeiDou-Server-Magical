package org.gms.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gms.client.*;
import org.gms.client.Character;
import org.gms.client.inventory.Inventory;
import org.gms.client.inventory.InventoryType;
import org.gms.client.inventory.Item;
import org.gms.client.inventory.ItemFactory;
import org.gms.client.inventory.Pet;
import org.gms.client.keybind.KeyBinding;
import org.gms.client.processor.npc.FredrickProcessor;
import org.gms.config.GameConfig;
import org.gms.constants.game.GameConstants;
import org.gms.constants.id.MapId;
import org.gms.constants.string.ExtendType;
import org.gms.dao.entity.*;
import org.gms.dao.mapper.*;
import org.gms.exception.BizException;
import org.gms.model.dto.*;
import org.gms.model.pojo.SkillEntry;
import org.gms.util.PacketCreator;
import org.gms.net.server.PlayerCoolDownValueHolder;
import org.gms.net.server.Server;
import org.gms.net.server.channel.Channel;
import org.gms.net.server.guild.Guild;
import org.gms.net.server.guild.GuildCharacter;
import org.gms.net.server.world.Messenger;
import org.gms.net.server.world.Party;
import org.gms.net.server.world.PartyCharacter;
import org.gms.net.server.world.World;
import org.gms.server.CashShop;
import org.gms.server.Storage;
import org.gms.server.events.Events;
import org.gms.server.life.MobSkill;
import org.gms.server.life.MobSkillFactory;
import org.gms.server.life.MobSkillType;
import org.gms.server.maps.SavedLocation;
import org.gms.server.maps.SavedLocationType;
import org.gms.util.BasePageUtil;
import org.gms.util.NumberTool;
import org.apache.logging.log4j.message.MapMessage;
import org.gms.server.logging.AuditLogger;
import org.gms.server.logging.LogAction;
import org.gms.server.logging.LogModule;
import org.gms.util.Pair;
import org.gms.util.RequireUtil;
import org.gms.util.ExtendUtil;
import org.gms.util.I18nUtil;
import org.gms.server.maps.MapManager;
import org.gms.server.maps.MapleMap;
import org.gms.server.maps.Portal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mybatisflex.core.query.QueryMethods.dateDiff;
import static com.mybatisflex.core.query.QueryMethods.now;
import static org.gms.dao.entity.table.AccountsDOTableDef.ACCOUNTS_DO;
import static org.gms.dao.entity.table.AreaInfoDOTableDef.AREA_INFO_DO;
import static org.gms.dao.entity.table.BbsRepliesDOTableDef.BBS_REPLIES_DO;
import static org.gms.dao.entity.table.BbsThreadsDOTableDef.BBS_THREADS_DO;
import static org.gms.dao.entity.table.BuddiesDOTableDef.BUDDIES_DO;
import static org.gms.dao.entity.table.CharactersDOTableDef.CHARACTERS_DO;
import static org.gms.dao.entity.table.CooldownsDOTableDef.COOLDOWNS_DO;
import static org.gms.dao.entity.table.EventstatsDOTableDef.EVENTSTATS_DO;
import static org.gms.dao.entity.table.ExtendValueDOTableDef.EXTEND_VALUE_DO;
import static org.gms.dao.entity.table.FamelogDOTableDef.FAMELOG_DO;
import static org.gms.dao.entity.table.FamilyCharacterDOTableDef.FAMILY_CHARACTER_DO;
import static org.gms.dao.entity.table.FredstorageDOTableDef.FREDSTORAGE_DO;
import static org.gms.dao.entity.table.GuildsDOTableDef.GUILDS_DO;
import static org.gms.dao.entity.table.KeymapDOTableDef.KEYMAP_DO;
import static org.gms.dao.entity.table.MonsterbookDOTableDef.MONSTERBOOK_DO;
import static org.gms.dao.entity.table.PetignoresDOTableDef.PETIGNORES_DO;
import static org.gms.dao.entity.table.PlayerdiseasesDOTableDef.PLAYERDISEASES_DO;
import static org.gms.dao.entity.table.SavedlocationsDOTableDef.SAVEDLOCATIONS_DO;
import static org.gms.dao.entity.table.ServerQueueDOTableDef.SERVER_QUEUE_DO;
import static org.gms.dao.entity.table.SkillmacrosDOTableDef.SKILLMACROS_DO;
import static org.gms.dao.entity.table.SkillsDOTableDef.SKILLS_DO;
import static org.gms.dao.entity.table.TrocklocationsDOTableDef.TROCKLOCATIONS_DO;
import static org.gms.dao.entity.table.WishlistsDOTableDef.WISHLISTS_DO;

/**
 * 角色服务类
 * 提供角色的增删改查、状态管理、数据持久化等功能
 */
@Service
@AllArgsConstructor
@Slf4j
public class CharacterService {
    private static final Map<Integer, String> characterNameCache = new ConcurrentHashMap<>();
    private final ExtendValueMapper extendValueMapper;
    private final CharactersMapper charactersMapper;
    private final SkillsMapper skillsMapper;
    private final SkillmacrosMapper skillmacrosMapper;
    private final GuildsMapper guildsMapper;
    private final BuddiesMapper buddiesMapper;
    private final BbsThreadsMapper bbsThreadsMapper;
    private final BbsRepliesMapper bbsRepliesMapper;
    private final WishlistsMapper wishlistsMapper;
    private final CooldownsMapper cooldownsMapper;
    private final PlayerdiseasesMapper playerdiseasesMapper;
    private final AreaInfoMapper areaInfoMapper;
    private final MonsterbookMapper monsterbookMapper;
    private final FamilyCharacterMapper familyCharacterMapper;
    private final FamelogMapper famelogMapper;
    private final InventoryService inventoryService;
    private final QuestService questService;
    private final FredstorageMapper fredstorageMapper;
    private final MtsService mtsService;
    private final KeymapMapper keymapMapper;
    private final SavedlocationsMapper savedlocationsMapper;
    private final TrocklocationsMapper trocklocationsMapper;
    private final EventstatsMapper eventstatsMapper;
    private final ServerQueueMapper serverQueueMapper;
    private final NameChangeService nameChangeService;
    private final WorldTransferService worldTransferService;
    private final PetignoresMapper petignoresMapper;
    private final QuickslotkeymappedMapper quickslotkeymappedMapper;
    private final ItemFactoryService itemFactoryService;
    private final AccountService accountService;

    /**
     * 根据ID查找角色实体
     * @param id 角色ID
     * @return 角色实体对象
     */
    public CharactersDO findById(int id) {
        return charactersMapper.selectOneById(id);
    }

    /**
     * 根据角色ID批量获取角色名称，并进行缓存。
     * @param characterIds 角色ID集合
     * @return Map<Integer, String> 角色ID到名称的映射
     */
    public Map<Integer, String> getChrNamesByIds(Set<Integer> characterIds) {
        if (characterIds == null || characterIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, String> names = new HashMap<>();
        Set<Integer> missingIds = new HashSet<>();

        // 1. 从缓存中获取
        for (Integer id : characterIds) {
            String name = characterNameCache.get(id);
            if (name != null) {
                names.put(id, name);
            } else {
                missingIds.add(id);
            }
        }

        if (missingIds.isEmpty()) {
            return names;
        }

        // 2. 尝试从在线玩家中获取
        for (World world : Server.getInstance().getWorlds()) {
            for (Character chr : world.getPlayerStorage().getAllCharacters()) {
                if (missingIds.contains(chr.getId())) {
                    String name = chr.getName();
                    names.put(chr.getId(), name);
                    characterNameCache.put(chr.getId(), name); // 更新缓存
                    missingIds.remove(chr.getId()); // 从待查询集合中移除
                }
            }
        }

        if (missingIds.isEmpty()) {
            return names;
        }

        // 3. 对于未在缓存和在线玩家中找到的ID，从数据库批量查询
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(CHARACTERS_DO.ID, CHARACTERS_DO.NAME)
                .where(CHARACTERS_DO.ID.in(missingIds));
        List<CharactersDO> dbCharacters = charactersMapper.selectListByQuery(queryWrapper);
        for (CharactersDO chrDO : dbCharacters) {
            names.put(chrDO.getId(), chrDO.getName());
            characterNameCache.put(chrDO.getId(), chrDO.getName()); // 更新缓存
        }

        return names;
    }

    /**
     * 更新角色信息
     * @param condition 更新条件和内容
     */
    public void update(CharactersDO condition) {
        charactersMapper.update(condition);
    }

    /**
     * 获取角色在线列表（支持分页和筛选）
     * @param request 查询请求参数
     * @return 分页的角色列表DTO
     */
    public Page<ChrOnlineListRtnDTO> getChrOnlineList(ChrOnlineListReqDTO request) {
        // 默认状态为在线
        Integer status = request.getStatus() == null ? 1 : request.getStatus();

        // 状态为1（在线）
        if (status == 1) {
            Collection<Character> chrList = Server.getInstance().getWorld(request.getWorld()).getPlayerStorage().getAllCharacters();
            return BasePageUtil.create(chrList, request)
                    .filter(chr -> {
                        // 基础条件过滤
                        boolean basicMatch = (Objects.isNull(request.getId()) || Objects.equals(chr.getId(), request.getId()))
                            && (RequireUtil.isEmpty(request.getName()) || chr.getName().contains(request.getName()))
                            && (Objects.isNull(request.getMap()) || Objects.equals(chr.getMap().getId(), request.getMap()))
                            && (Objects.isNull(request.getAccountId()) || Objects.equals(chr.getAccountId(), request.getAccountId()))
                            && (Objects.isNull(request.getChannel()) || Objects.equals(chr.getClient().getChannel(), request.getChannel()))
                            && (Objects.isNull(request.getJob()) || Objects.equals(chr.getJob().getId(), request.getJob()))
                            && (Objects.isNull(request.getPartyId()) || Objects.equals(chr.getPartyId(), request.getPartyId()))
                            && (Objects.isNull(request.getGuildId()) || Objects.equals(chr.getGuildId(), request.getGuildId()))
                            && (Objects.isNull(request.getMinLevel()) || chr.getLevel() >= request.getMinLevel())
                            && (Objects.isNull(request.getMaxLevel()) || chr.getLevel() <= request.getMaxLevel())
                            && (Objects.isNull(request.getMinOnlineTime()) || (System.currentTimeMillis() - chr.getLoginTime()) / 60000 >= request.getMinOnlineTime())
                            && (Objects.isNull(request.getMaxOnlineTime()) || (System.currentTimeMillis() - chr.getLoginTime()) / 60000 <= request.getMaxOnlineTime());
                        
                        if (!basicMatch) return false;

                        // 封禁状态过滤
                        if (request.getBanStatus() != null && request.getBanStatus() != 0) {
                            boolean isPermBan = chr.isBanned();
                            boolean isTempBan = !isPermBan && chr.getClient().getTempBanCalendar() != null && chr.getClient().getTempBanCalendar().getTimeInMillis() > System.currentTimeMillis();
                            
                            if (request.getBanStatus() == 1) return !isPermBan && !isTempBan; // 正常
                            if (request.getBanStatus() == 2) return isPermBan; // 永久
                            if (request.getBanStatus() == 3) return isTempBan; // 临时
                            if (request.getBanStatus() == 4) return isPermBan || isTempBan; // 所有封禁
                        }
                        return true;
                    })
                    .page(chr -> {
                        boolean isBanned = chr.isBanned();
                        int banStatus = 0;
                        String banReason = null;
                        String tempBanTime = null;
                        
                        if (isBanned) {
                            banStatus = 1; // 永久封禁
                            banReason = chr.getClient().getBanReason();
                        } else if (chr.getClient().getTempBanCalendar() != null && chr.getClient().getTempBanCalendar().getTimeInMillis() > System.currentTimeMillis()) {
                            isBanned = true;
                            banStatus = 2; // 临时封禁
                            banReason = chr.getClient().getBanReason();
                            tempBanTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(chr.getClient().getTempBanCalendar().getTime());
                        }
                        
                        return ChrOnlineListRtnDTO.builder()
                            .world(chr.getWorld())
                            .accountId(chr.getAccountId())
                            .accountName(chr.getClient().getAccountName())
                            .id(chr.getId())
                            .name(chr.getName())
                            .map(chr.getMap().getId())
                            .mapName(chr.getMap().getMapName())
                            .job(chr.getJob().getId())
                            .jobName(chr.getJob().getName())
                            .level(chr.getLevel())
                            .gm(chr.gmLevel())
                            .maxHp(chr.getMaxHp())
                            .maxMp(chr.getMaxMp())
                            .guildId(chr.getGuildId())
                            .guildName(chr.getGuild() != null ? chr.getGuild().getName() : null)
                            .gender(chr.getGender())
                            .partyId(chr.getPartyId())
                            .channel(chr.getClient().getChannel())
                            .fame(chr.getFame())
                            .loginTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(chr.getLoginTime())))
                            .lastLogoutTime(null) // 在线玩家登出时间为null
                            .banned(isBanned)
                            .banStatus(banStatus)
                            .banReason(banReason)
                            .tempBanTime(tempBanTime)
                            .jailTimeLeft(chr.getJailExpirationTimeLeft() / 1000)
                            .build();
                    });
        }

        // 状态为0（全部）或2（离线）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(CHARACTERS_DO.ALL_COLUMNS, 
                        ACCOUNTS_DO.NAME.as("accountName"), 
                        ACCOUNTS_DO.BANNED.as("banned"), 
                        ACCOUNTS_DO.TEMPBAN.as("tempban"),
                        ACCOUNTS_DO.BANREASON.as("banReason"),
                        GUILDS_DO.NAME.as("guildName"))
                .from(CHARACTERS_DO)
                .leftJoin(ACCOUNTS_DO).on(CHARACTERS_DO.ACCOUNTID.eq(ACCOUNTS_DO.ID))
                .leftJoin(GUILDS_DO).on(CHARACTERS_DO.GUILDID.eq(GUILDS_DO.GUILDID))
                .where(CHARACTERS_DO.WORLD.eq(request.getWorld()))
                .and(CHARACTERS_DO.ID.eq(request.getId(), Objects::nonNull))
                .and(CHARACTERS_DO.NAME.like(request.getName(), RequireUtil::isNotEmpty))
                .and(CHARACTERS_DO.MAP.eq(request.getMap(), Objects::nonNull))
                .and(CHARACTERS_DO.ACCOUNTID.eq(request.getAccountId(), Objects::nonNull))
                .and(CHARACTERS_DO.JOB.eq(request.getJob(), Objects::nonNull))
                .and(CHARACTERS_DO.PARTY.eq(request.getPartyId(), Objects::nonNull))
                .and(CHARACTERS_DO.GUILDID.eq(request.getGuildId(), Objects::nonNull))
                .and(CHARACTERS_DO.LEVEL.ge(request.getMinLevel(), Objects::nonNull))
                .and(CHARACTERS_DO.LEVEL.le(request.getMaxLevel(), Objects::nonNull));

        if (request.getBanStatus() != null && request.getBanStatus() != 0) {
            if (request.getBanStatus() == 1) { // 未封禁
                queryWrapper.and(ACCOUNTS_DO.BANNED.eq(0).or(ACCOUNTS_DO.BANNED.isNull()))
                        .and(ACCOUNTS_DO.TEMPBAN.lt(new Timestamp(System.currentTimeMillis())).or(ACCOUNTS_DO.TEMPBAN.isNull()));
            } else if (request.getBanStatus() == 2) { // 永久封禁
                queryWrapper.and(ACCOUNTS_DO.BANNED.eq(1));
            } else if (request.getBanStatus() == 3) { // 临时封禁
                queryWrapper.and(ACCOUNTS_DO.TEMPBAN.gt(new Timestamp(System.currentTimeMillis())));
            } else if (request.getBanStatus() == 4) { // 已封禁 (永久或临时)
                queryWrapper.and(
                    ACCOUNTS_DO.BANNED.eq(1)
                    .or(ACCOUNTS_DO.TEMPBAN.gt(new Timestamp(System.currentTimeMillis())))
                );
            }
        }

        if (status == 2) { // 离线
            Collection<Character> onlineChars = Server.getInstance().getWorld(request.getWorld()).getPlayerStorage().getAllCharacters();
            if (!onlineChars.isEmpty()) {
                List<Integer> onlineCharIds = onlineChars.stream().map(Character::getId).collect(Collectors.toList());
                queryWrapper.and(CHARACTERS_DO.ID.notIn(onlineCharIds));
            }
        }

        Page<CharactersDO> charPage = charactersMapper.paginateAs(new Page<>(request.getPageNo(), request.getPageSize()), queryWrapper, CharactersDO.class);

        List<ChrOnlineListRtnDTO> dtoList = charPage.getRecords().stream().map(charactersDO -> {
            String mapName = "未知地图";
            List<Channel> channels = Server.getInstance().getChannelsFromWorld(request.getWorld());
            if (!channels.isEmpty()) {
                try {
                    MapleMap map = channels.get(0).getMapFactory().getMap(charactersDO.getMap());
                    if (map != null) {
                        mapName = map.getMapName();
                    }
                } catch (Exception ignored) {
                }
            }
            
            // 优先使用关联查询出的 guildName，如果为空则尝试从内存获取
            String guildName = null;
            if (charactersDO.getExtra() != null) {
                guildName = (String) charactersDO.getExtra().get("guildName");
            }
            
            if (guildName == null && charactersDO.getGuildid() != null && charactersDO.getGuildid() > 0) {
                Guild guild = Server.getInstance().getGuild(charactersDO.getGuildid());
                if (guild != null) {
                    guildName = guild.getName();
                } else {
                    // 兜底：查数据库
                    GuildsDO g = guildsMapper.selectOneById(charactersDO.getGuildid());
                    if (g != null) {
                        guildName = g.getName();
                    }
                }
            }

            Job job = Job.getById(charactersDO.getJob());
            String jobName = (job != null) ? job.getName() : I18nUtil.getMessage("Character.job.unknown");
            
            // 检查角色是否在线，如果在线，则使用内存中的实时数据
            Character onlineChr = null;
            try {
                // 优先从请求的 World 查找
                World world = Server.getInstance().getWorld(request.getWorld());
                if (world != null) {
                    onlineChr = world.getPlayerStorage().getCharacterById(charactersDO.getId());
                }
                
                // 如果没找到，尝试遍历所有 World (防止跨服操作导致的数据不一致)
                if (onlineChr == null) {
                    for (World w : Server.getInstance().getWorlds()) {
                        onlineChr = w.getPlayerStorage().getCharacterById(charactersDO.getId());
                        if (onlineChr != null) break;
                    }
                }
            } catch (Exception e) {
                // ignore
            }

            if (onlineChr != null) {
                // 再次过滤内存数据，因为数据库查询可能包含在线玩家，但内存数据更准确
                if (request.getChannel() != null && onlineChr.getClient().getChannel() != request.getChannel()) return null;
                if (request.getMinOnlineTime() != null && (System.currentTimeMillis() - onlineChr.getLoginTime()) / 60000 < request.getMinOnlineTime()) return null;
                if (request.getMaxOnlineTime() != null && (System.currentTimeMillis() - onlineChr.getLoginTime()) / 60000 > request.getMaxOnlineTime()) return null;

                // 检查在线玩家的封禁状态
                boolean isBanned = onlineChr.isBanned();
                int banStatus = 0;
                String banReason = null;
                String tempBanTime = null;
                
                if (isBanned) {
                    banStatus = 1; // 永久封禁
                    banReason = onlineChr.getClient().getBanReason();
                } else if (onlineChr.getClient().getTempBanCalendar() != null && onlineChr.getClient().getTempBanCalendar().getTimeInMillis() > System.currentTimeMillis()) {
                    isBanned = true;
                    banStatus = 2; // 临时封禁
                    banReason = onlineChr.getClient().getBanReason();
                    tempBanTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(onlineChr.getClient().getTempBanCalendar().getTime());
                }

                return ChrOnlineListRtnDTO.builder()
                        .world(onlineChr.getWorld())
                        .accountId(onlineChr.getAccountId())
                        .accountName(onlineChr.getClient().getAccountName())
                        .id(onlineChr.getId())
                        .name(onlineChr.getName())
                        .map(onlineChr.getMap().getId())
                        .mapName(onlineChr.getMap().getMapName())
                        .job(onlineChr.getJob().getId())
                        .jobName(onlineChr.getJob().getName())
                        .level(onlineChr.getLevel())
                        .gm(onlineChr.gmLevel())
                        .maxHp(onlineChr.getMaxHp())
                        .maxMp(onlineChr.getMaxMp())
                        .guildId(onlineChr.getGuildId())
                        .guildName(onlineChr.getGuild() != null ? onlineChr.getGuild().getName() : null)
                        .gender(onlineChr.getGender())
                        .partyId(onlineChr.getPartyId())
                        .channel(onlineChr.getClient().getChannel())
                        .fame(onlineChr.getFame())
                        .loginTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(onlineChr.getLoginTime())))
                        .lastLogoutTime(null) // 在线玩家登出时间为null
                        .banned(isBanned)
                        .banStatus(banStatus)
                        .banReason(banReason)
                        .tempBanTime(tempBanTime)
                        .jailTimeLeft(onlineChr.getJailExpirationTimeLeft() / 1000)
                        .build();
            }

            // 离线玩家的数据
            // 离线玩家不应该有在线时长筛选
            if (request.getMinOnlineTime() != null || request.getMaxOnlineTime() != null) return null;
            if (request.getChannel() != null) return null; // 离线玩家没有频道

            boolean banned = false;
            int banStatus = 0;
            String banReason = null;
            String tempBanTime = null;

            // 优先使用 CharactersDO 中映射的字段
            if (charactersDO.getBanned() != null && charactersDO.getBanned() > 0) {
                banned = true;
                banStatus = 1; // 永久封禁
            }

            if (charactersDO.getTempban() != null && charactersDO.getTempban().after(new Timestamp(System.currentTimeMillis()))) {
                banned = true;
                banStatus = 2; // 临时封禁
                tempBanTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(charactersDO.getTempban());
            }
            
            // 如果 CharactersDO 中没有映射，尝试从 extra 中获取 (兼容旧逻辑)
            if (!banned && charactersDO.getExtra() != null) {
                Object bannedObj = charactersDO.getExtra().get("banned");
                if (bannedObj instanceof Boolean) {
                    if ((Boolean) bannedObj) {
                        banned = true;
                        banStatus = 1;
                    }
                } else if (bannedObj instanceof Number) {
                    if (((Number) bannedObj).intValue() > 0) {
                        banned = true;
                        banStatus = 1;
                    }
                }

                Object tempbanObj = charactersDO.getExtra().get("tempban");
                if (tempbanObj instanceof Timestamp) {
                    Timestamp tempban = (Timestamp) tempbanObj;
                    if (tempban.after(new Timestamp(System.currentTimeMillis()))) {
                        banned = true;
                        banStatus = 2;
                        tempBanTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(tempban);
                    }
                }
            }

            // 获取封禁原因 (从关联查询的 extra 中获取)
            if (charactersDO.getExtra() != null) {
                Object reason = charactersDO.getExtra().get("banReason");
                if (reason == null) {
                    reason = charactersDO.getExtra().get("banreason");
                }
                if (reason != null) {
                    banReason = reason.toString();
                }
            }

            // 如果从 extra 获取失败，尝试直接查询账号信息
            if (banReason == null && (banStatus == 1 || banStatus == 2)) {
                AccountsDO account = accountService.findById(charactersDO.getAccountid());
                if (account != null) {
                    banReason = account.getBanreason();
                }
            }

            return ChrOnlineListRtnDTO.builder()
                    .world(charactersDO.getWorld())
                    .accountId(charactersDO.getAccountid())
                    .accountName(charactersDO.getAccountName())
                    .id(charactersDO.getId())
                    .name(charactersDO.getName())
                    .map(charactersDO.getMap())
                    .mapName(mapName)
                    .job(charactersDO.getJob())
                    .jobName(jobName)
                    .level(charactersDO.getLevel())
                    .gm(charactersDO.getGm())
                    .maxHp(charactersDO.getMaxhp())
                    .maxMp(charactersDO.getMaxmp())
                    .guildId(charactersDO.getGuildid())
                    .guildName(guildName)
                    .gender(charactersDO.getGender())
                    .partyId(charactersDO.getParty())
                    .channel(-1) // 离线
                    .fame(charactersDO.getFame())
                    .loginTime(charactersDO.getCreatedate() != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(charactersDO.getCreatedate()) : null) // 离线显示创建时间
                    .lastLogoutTime(charactersDO.getLastLogoutTime() != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(charactersDO.getLastLogoutTime()) : null)
                    .banned(banned)
                    .banStatus(banStatus)
                    .banReason(banReason)
                    .tempBanTime(tempBanTime)
                    .jailTimeLeft(Math.max(0, charactersDO.getJailexpire()) / 1000)
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList()); // 过滤掉返回null的记录

        // 重新封装为 Page 对象
        Page<ChrOnlineListRtnDTO> resultPage = new Page<>();
        resultPage.setPageNumber(charPage.getPageNumber());
        resultPage.setPageSize(charPage.getPageSize());
        resultPage.setTotalRow(charPage.getTotalRow());
        resultPage.setRecords(dtoList);
        
        return resultPage;
    }

    /**
     * 更新角色详细信息
     * @param request 更新请求
     */
    public void updateCharacter(UpdateCharacterReqDTO request) {
        RequireUtil.requireNotNull(request.getId(), "Character ID cannot be null");

        // 1. 尝试获取在线玩家
        Character onlineChr = null;
        try {
            for (World world : Server.getInstance().getWorlds()) {
                onlineChr = world.getPlayerStorage().getCharacterById(request.getId());
                if (onlineChr != null) {
                    break;
                }
            }
        } catch (Exception e) {
            // ignore
        }

        if (onlineChr != null) {
            // 在线玩家：调用 Character 类中的方法进行更新
            onlineChr.updateCharacterDetails(request);
            return;
        }

        // 2. 离线玩家：直接更新数据库
        CharactersDO charactersDO = new CharactersDO();
        charactersDO.setId(request.getId());
        if (request.getName() != null) charactersDO.setName(request.getName());
        if (request.getLevel() != null) charactersDO.setLevel(request.getLevel());
        if (request.getExp() != null) charactersDO.setExp(request.getExp());
        if (request.getJob() != null) charactersDO.setJob(request.getJob());
        if (request.getStr() != null) charactersDO.setAttrStr(request.getStr());
        if (request.getDex() != null) charactersDO.setAttrDex(request.getDex());
        if (request.getIntAttr() != null) charactersDO.setAttrInt(request.getIntAttr());
        if (request.getLuk() != null) charactersDO.setAttrLuk(request.getLuk());
        if (request.getHp() != null) charactersDO.setHp(request.getHp());
        if (request.getMaxHp() != null) charactersDO.setMaxhp(request.getMaxHp());
        if (request.getMp() != null) charactersDO.setMp(request.getMp());
        if (request.getMaxMp() != null) charactersDO.setMaxmp(request.getMaxMp());
        if (request.getAp() != null) charactersDO.setAp(request.getAp());
        if (request.getSp() != null) charactersDO.setSp(request.getSp());
        if (request.getFame() != null) charactersDO.setFame(request.getFame());
        if (request.getMeso() != null) charactersDO.setMeso(request.getMeso());
        if (request.getGm() != null) charactersDO.setGm(request.getGm());
        if (request.getFace() != null) charactersDO.setFace(request.getFace());
        if (request.getHair() != null) charactersDO.setHair(request.getHair());
        if (request.getSkinColor() != null) charactersDO.setSkincolor(request.getSkinColor());
        if (request.getGender() != null) charactersDO.setGender(request.getGender());

        // 新增字段更新
        if (request.getEquipSlots() != null) charactersDO.setEquipslots(request.getEquipSlots());
        if (request.getUseSlots() != null) charactersDO.setUseslots(request.getUseSlots());
        if (request.getSetupSlots() != null) charactersDO.setSetupslots(request.getSetupSlots());
        if (request.getEtcSlots() != null) charactersDO.setEtcslots(request.getEtcSlots());
        if (request.getBuddyCapacity() != null) charactersDO.setBuddyCapacity(request.getBuddyCapacity());
        if (request.getMerchantMesos() != null) charactersDO.setMerchantmesos(request.getMerchantMesos());
        if (request.getGachaExp() != null) charactersDO.setGachaexp(request.getGachaExp());
        if (request.getMap() != null) charactersDO.setMap(request.getMap());
        if (request.getSpawnPoint() != null) charactersDO.setSpawnpoint(request.getSpawnPoint());
        if (request.getMountLevel() != null) charactersDO.setMountlevel(request.getMountLevel());
        if (request.getMountExp() != null) charactersDO.setMountexp(request.getMountExp());
        if (request.getMountTiredness() != null) charactersDO.setMounttiredness(request.getMountTiredness());

        charactersMapper.update(charactersDO);

        // 更新账号货币
        CharactersDO chr = charactersMapper.selectOneById(request.getId());
        if (chr != null) {
            AccountsDO accountsDO = new AccountsDO();
            accountsDO.setId(chr.getAccountid());
            if (request.getNxCredit() != null) accountsDO.setNxCredit(request.getNxCredit());
            if (request.getMaplePoint() != null) accountsDO.setMaplePoint(request.getMaplePoint());
            if (request.getNxPrepaid() != null) accountsDO.setNxPrepaid(request.getNxPrepaid());
            accountService.update(accountsDO);
        }
    }

    /**
     * 获取角色详情
     * @param id 角色ID
     * @return 角色详情DTO
     */
    public ChrDetailRtnDTO getCharacterDetail(Integer id) {
        RequireUtil.requireNotNull(id, "Character ID cannot be null");

        // 优先从在线玩家中查找
        try {
            for (World world : Server.getInstance().getWorlds()) {
                Character onlineChr = world.getPlayerStorage().getCharacterById(id);
                if (onlineChr != null) {
                    return ChrDetailRtnDTO.builder()
                            .id(onlineChr.getId())
                            .name(onlineChr.getName())
                            .level(onlineChr.getLevel())
                            .exp(onlineChr.getExp())
                            .job(onlineChr.getJob().getId())
                            .jobName(onlineChr.getJob().getName())
                            .str(onlineChr.getStr())
                            .dex(onlineChr.getDex())
                            .intAttr(onlineChr.getInt())
                            .luk(onlineChr.getLuk())
                            .hp(onlineChr.getHp())
                            .maxHp(onlineChr.getMaxHp())
                            .mp(onlineChr.getMp())
                            .maxMp(onlineChr.getMaxMp())
                            .ap(onlineChr.getRemainingAp())
                            .sp(Arrays.stream(onlineChr.getRemainingSps()).mapToObj(String::valueOf).collect(Collectors.joining(",")))
                            .fame(onlineChr.getFame())
                            .meso(onlineChr.getMeso())
                            .gm(onlineChr.gmLevel())
                            .face(onlineChr.getFace())
                            .hair(onlineChr.getHair())
                            .skinColor(onlineChr.getSkinColor().getId())
                            .gender(onlineChr.getGender())
                            .nxCredit(onlineChr.getCashShop().getCash(CashShop.NX_CREDIT))
                            .maplePoint(onlineChr.getCashShop().getCash(CashShop.MAPLE_POINT))
                            .nxPrepaid(onlineChr.getCashShop().getCash(CashShop.NX_PREPAID))
                            .equipSlots((int) onlineChr.getSlots(InventoryType.EQUIP.getType()))
                            .useSlots((int) onlineChr.getSlots(InventoryType.USE.getType()))
                            .setupSlots((int) onlineChr.getSlots(InventoryType.SETUP.getType()))
                            .etcSlots((int) onlineChr.getSlots(InventoryType.ETC.getType()))
                            .buddyCapacity(onlineChr.getBuddylist().getCapacity())
                            .merchantMesos(onlineChr.getMerchantMeso())
                            .gachaExp(onlineChr.getGachaExp())
                            .map(onlineChr.getMapId())
                            .spawnPoint(onlineChr.getInitialSpawnPoint())
                            .mountLevel(onlineChr.getMapleMount() != null ? onlineChr.getMapleMount().getLevel() : 1)
                            .mountExp(onlineChr.getMapleMount() != null ? onlineChr.getMapleMount().getExp() : 0)
                            .mountTiredness(onlineChr.getMapleMount() != null ? onlineChr.getMapleMount().getTiredness() : 0)
                            .build();
                }
            }
        } catch (Exception e) {
            // ignore
        }

        // 如果不在线，从数据库查找
        CharactersDO charactersDO = charactersMapper.selectOneById(id);
        RequireUtil.requireNotNull(charactersDO, I18nUtil.getExceptionMessage("Character.notFound"));

        Job job = Job.getById(charactersDO.getJob());
        String jobName = (job != null) ? job.getName() : I18nUtil.getMessage("Character.job.unknown");

        AccountsDO accountsDO = accountService.findById(charactersDO.getAccountid());

        return ChrDetailRtnDTO.builder()
                .id(charactersDO.getId())
                .name(charactersDO.getName())
                .level(charactersDO.getLevel())
                .exp(charactersDO.getExp())
                .job(charactersDO.getJob())
                .jobName(jobName)
                .str(charactersDO.getAttrStr())
                .dex(charactersDO.getAttrDex())
                .intAttr(charactersDO.getAttrInt())
                .luk(charactersDO.getAttrLuk())
                .hp(charactersDO.getHp())
                .maxHp(charactersDO.getMaxhp())
                .mp(charactersDO.getMp())
                .maxMp(charactersDO.getMaxmp())
                .ap(charactersDO.getAp())
                .sp(charactersDO.getSp())
                .fame(charactersDO.getFame())
                .meso(charactersDO.getMeso())
                .gm(charactersDO.getGm())
                .face(charactersDO.getFace())
                .hair(charactersDO.getHair())
                .skinColor(charactersDO.getSkincolor())
                .gender(charactersDO.getGender())
                .nxCredit(accountsDO != null ? accountsDO.getNxCredit() : 0)
                .maplePoint(accountsDO != null ? accountsDO.getMaplePoint() : 0)
                .nxPrepaid(accountsDO != null ? accountsDO.getNxPrepaid() : 0)
                .equipSlots(charactersDO.getEquipslots())
                .useSlots(charactersDO.getUseslots())
                .setupSlots(charactersDO.getSetupslots())
                .etcSlots(charactersDO.getEtcslots())
                .buddyCapacity(charactersDO.getBuddyCapacity())
                .merchantMesos(charactersDO.getMerchantmesos())
                .gachaExp(charactersDO.getGachaexp())
                .map(charactersDO.getMap())
                .spawnPoint(charactersDO.getSpawnpoint())
                .mountLevel(charactersDO.getMountlevel())
                .mountExp(charactersDO.getMountexp())
                .mountTiredness(charactersDO.getMounttiredness())
                .build();
    }

    /**
     * 更新角色扩展倍率
     * @param data 扩展数据
     */
    public void updateRate(ExtendValueDO data) {
        checkName(data);
        data.setExtendType(ExtendType.CHARACTER_EXTEND.getType());
        ExtendValueDO extendValueDO = ExtendUtil.getExtendValue(data.getExtendId(), data.getExtendType(), data.getExtendName());
        if (extendValueDO == null) {
            extendValueMapper.insertSelective(data);
        } else {
            data.setCreateTime(null);
            data.setUpdateTime(new Date(System.currentTimeMillis()));
            extendValueMapper.update(data);
        }

        Character character = getCharacter(data);
        character.resetPlayerRates();
        character.setWorldRates();
        character.setCouponRates();
    }

    /**
     * 重置单个倍率
     * @param data 扩展数据
     */
    public void resetRate(ExtendValueDO data) {
        checkName(data);
        extendValueMapper.deleteByQuery(QueryWrapper.create()
                .where(EXTEND_VALUE_DO.EXTEND_ID.eq(data.getExtendId()))
                .and(EXTEND_VALUE_DO.EXTEND_TYPE.eq(ExtendType.CHARACTER_EXTEND.getType()))
                .and(EXTEND_VALUE_DO.EXTEND_NAME.eq(data.getExtendName())));
        Character character = getCharacter(data);
        character.resetPlayerRates();
        character.setWorldRates();
        character.setCouponRates();
    }

    /**
     * 重置所有倍率（经验、掉落、金币）
     * @param data 扩展数据
     */
    public void resetRates(ExtendValueDO data) {
        check(data);
        extendValueMapper.deleteByQuery(QueryWrapper.create()
                .where(EXTEND_VALUE_DO.EXTEND_ID.eq(data.getExtendId()))
                .and(EXTEND_VALUE_DO.EXTEND_TYPE.eq(ExtendType.CHARACTER_EXTEND.getType()))
                .and(EXTEND_VALUE_DO.EXTEND_NAME.in("expRate", "dropRate", "mesoRate")));
        Character character = getCharacter(data);
        character.resetPlayerRates();
        character.setWorldRates();
        character.setCouponRates();
    }

    /**
     * 重置所有角色的雇佣商人状态
     */
    public void resetMerchant() {
        charactersMapper.updateAllHasMerchant(0);
    }

    /**
     * 获取各世界排名前50的玩家
     * @param worldSize 世界数量
     * @return 排名列表
     */
    public List<List<CharactersDO>> getWorldsRankPlayers(int worldSize) {
        boolean wholeServerRanking = GameConfig.getServerBoolean("use_whole_server_ranking");
        List<List<CharactersDO>> worldsRankingList = new ArrayList<>();
        if (wholeServerRanking) {
            // 全服前50
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .select(CHARACTERS_DO.NAME, CHARACTERS_DO.LEVEL, CHARACTERS_DO.WORLD)
                    .from(CHARACTERS_DO)
                    .leftJoin(ACCOUNTS_DO).on(CHARACTERS_DO.ACCOUNTID.eq(ACCOUNTS_DO.ID))
                    .where(CHARACTERS_DO.GM.lt(2))
                    .and(ACCOUNTS_DO.BANNED.eq(0).or(ACCOUNTS_DO.TEMPBAN.isNull()))
                    .and(CHARACTERS_DO.WORLD.between(0, worldSize - 1))
                    .orderBy(CHARACTERS_DO.WORLD.asc(), CHARACTERS_DO.LEVEL.desc(), CHARACTERS_DO.EXP.desc(), CHARACTERS_DO.LAST_EXP_GAIN_TIME.asc())
                    .limit(50);
            List<CharactersDO> charactersDOList = charactersMapper.selectListByQuery(queryWrapper);
            worldsRankingList.add(charactersDOList);
        } else {
            for (int i = 0; i < worldSize; i++) {
                // 每个区前50
                List<CharactersDO> charactersDOList = getWorldRankPlayers(i);
                worldsRankingList.add(charactersDOList);
            }
        }
        return worldsRankingList;
    }

    /**
     * 获取指定世界排名前50的玩家
     * @param worldId 世界ID
     * @return 排名列表
     */
    public List<CharactersDO> getWorldRankPlayers(int worldId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(CHARACTERS_DO.NAME, CHARACTERS_DO.LEVEL, CHARACTERS_DO.WORLD)
                .from(CHARACTERS_DO)
                .leftJoin(ACCOUNTS_DO).on(CHARACTERS_DO.ACCOUNTID.eq(ACCOUNTS_DO.ID))
                .where(CHARACTERS_DO.GM.lt(2))
                .and(ACCOUNTS_DO.BANNED.eq(0).or(ACCOUNTS_DO.TEMPBAN.isNull()))
                .and(CHARACTERS_DO.WORLD.eq(worldId))
                .orderBy(CHARACTERS_DO.LEVEL.desc(), CHARACTERS_DO.EXP.desc(), CHARACTERS_DO.LAST_EXP_GAIN_TIME.asc())
                .limit(50);
        return charactersMapper.selectListByQuery(queryWrapper);
    }

    /**
     * 根据名称查找角色
     * @param name 角色名
     * @return 角色实体
     */
    public CharactersDO findByName(String name) {
        List<CharactersDO> charactersDOS = charactersMapper.selectListByQuery(QueryWrapper.create().where(CHARACTERS_DO.NAME.eq(name)));
        return charactersDOS.isEmpty() ? null : charactersDOS.getFirst();
    }

    /**
     * 移除技能
     * @param skillsDO 技能实体
     */
    public void removeSkill(SkillsDO skillsDO) {
        skillsMapper.deleteByQuery(QueryWrapper.create(skillsDO));
    }

    /**
     * 删除家族
     * @param guildsDO 家族实体
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteGuild(GuildsDO guildsDO) {
        charactersMapper.updateByQuery(CharactersDO.builder().guildid(0).guildrank(5).build(), QueryWrapper.create().where(CHARACTERS_DO.GUILDID.eq(guildsDO.getGuildid().intValue())));
        guildsMapper.deleteById(guildsDO.getGuildid());
    }

    /**
     * 从数据库彻底删除角色
     * @param player 角色对象
     * @param senderAccId 发起请求的账号ID（用于安全校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCharFromDB(Character player, int senderAccId) {
        int cid = player.getId();
        if (!Server.getInstance().haveCharacterEntry(senderAccId, cid)) {    // 感谢 zera (EpiphanyMS) 指出非授权角色删除请求的关键漏洞
            throw new BizException(I18nUtil.getExceptionMessage("UNKNOWN_CHARACTER"));
        }
        int world;
        CharactersDO charactersDO = findById(cid);
        if (charactersDO != null) {
            world = charactersDO.getWorld();
            // 删除guild
            if (charactersDO.getGuildid() > 0 && Objects.equals(senderAccId, charactersDO.getAccountid())) {
                Server.getInstance().deleteGuildCharacter(new GuildCharacter(player, cid, 0, charactersDO.getName(),
                        (byte) -1, (byte) -1, 0, Optional.ofNullable(charactersDO.getGuildrank()).orElse(0),
                        Optional.ofNullable(charactersDO.getGuildid()).orElse(0), false,
                        Optional.ofNullable(charactersDO.getAllianceRank()).orElse(0)));
            }
        } else {
            world = 0;
        }
        // 删除buddies
        QueryWrapper buddiesQueryWrapper = QueryWrapper.create().where(BUDDIES_DO.CHARACTERID.eq(cid));
        List<BuddiesDO> buddiesDOS = buddiesMapper.selectListByQuery(buddiesQueryWrapper);
        buddiesDOS.forEach(buddiesDO -> {
            Character buddy = Server.getInstance().getWorld(world).getPlayerStorage().getCharacterById(buddiesDO.getBuddyid());
            if (buddy != null) {
                buddy.deleteBuddy(cid);
            }
        });
        buddiesMapper.deleteByQuery(buddiesQueryWrapper);
        // 删除bbs_threads bbs_replies
        QueryWrapper bbsThreadsQueryWrapper = QueryWrapper.create().where(BBS_THREADS_DO.POSTERCID.eq(cid));
        List<BbsThreadsDO> bbsThreadsDOS = bbsThreadsMapper.selectListByQuery(bbsThreadsQueryWrapper);
        List<Long> threadIds = bbsThreadsDOS.stream().map(BbsThreadsDO::getThreadid).toList();
        if (!threadIds.isEmpty()) {
            bbsRepliesMapper.deleteByQuery(QueryWrapper.create().where(BBS_REPLIES_DO.THREADID.in(threadIds)));
            bbsThreadsMapper.deleteByQuery(bbsThreadsQueryWrapper);
        }
        // 删除wishlists
        wishlistsMapper.deleteByQuery(QueryWrapper.create().where(WISHLISTS_DO.CHARID.eq(cid)));
        // 删除cooldowns
        cooldownsMapper.deleteByQuery(QueryWrapper.create().where(COOLDOWNS_DO.CHARID.eq(cid)));
        // 删除playerdiseases
        playerdiseasesMapper.deleteByQuery(QueryWrapper.create().where(PLAYERDISEASES_DO.CHARID.eq(cid)));
        // 删除area_info
        areaInfoMapper.deleteByQuery(QueryWrapper.create().where(AREA_INFO_DO.CHARID.eq(cid)));
        // 删除monsterbook
        monsterbookMapper.deleteByQuery(QueryWrapper.create().where(MONSTERBOOK_DO.CHARID.eq(cid)));
        // 删除characters
        charactersMapper.deleteById(cid);
        // 删除family_character
        familyCharacterMapper.deleteByQuery(QueryWrapper.create().where(FAMILY_CHARACTER_DO.CID.eq(cid)));
        // 删除famelog
        famelogMapper.deleteByQuery(QueryWrapper.create().where(FAMELOG_DO.CHARACTERID_TO.eq(cid).or(FAMELOG_DO.CHARACTERID.eq(cid))));
        // 删除背包库存
        inventoryService.deleteInventoryByCharacterId(cid);
        // 删除任务进度
        questService.deleteQuestProgressByCharacter(cid);
        // 删除fredstorage
        fredstorageMapper.deleteByQuery(QueryWrapper.create().where(FREDSTORAGE_DO.CID.eq(cid)));
        // 删除拍卖行
        mtsService.deleteMtsByCharacterId(cid);
        // 删除keymap
        keymapMapper.deleteByQuery(QueryWrapper.create().where(KEYMAP_DO.CHARACTERID.eq(cid)));
        // 删除savedlocations
        savedlocationsMapper.deleteByQuery(QueryWrapper.create().where(SAVEDLOCATIONS_DO.CHARACTERID.eq(cid)));
        // 删除trocklocations
        trocklocationsMapper.deleteByQuery(QueryWrapper.create().where(TROCKLOCATIONS_DO.CHARACTERID.eq(cid)));
        // 删除技能
        skillsMapper.deleteByQuery(QueryWrapper.create().where(SKILLS_DO.CHARACTERID.eq(cid)));
        skillmacrosMapper.deleteByQuery(QueryWrapper.create().where(SKILLMACROS_DO.CHARACTERID.eq(cid)));
        // 删除eventstats
        eventstatsMapper.deleteByQuery(QueryWrapper.create().where(EVENTSTATS_DO.CHARACTERID.eq(cid)));
        // 删除server_queue
        serverQueueMapper.deleteByQuery(QueryWrapper.create().where(SERVER_QUEUE_DO.CHARACTERID.eq(cid)));
        // 补充heaven没有删除的2张表
        nameChangeService.cancelPendingNameChange(player, false);
        worldTransferService.cancelPendingWorldTransfer(player, false);
    }

    /**
     * 保存角色数据到数据库
     * @param player 角色对象
     * @param notAutosave 是否非自动保存（用于日志区分）
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void saveCharToDB(Character player, boolean notAutosave) {
        if (!player.isLoggedIn()) {
            return;
        }
        log.info(I18nUtil.getLogMessage(notAutosave ? "Character.saveCharToDB.info1" : "Character.saveCharToDB.info2"), player.getName());
        Server.getInstance().updateCharacterEntry(player);

        CharactersDO cdo = Character.toCharactersDO(player);
        charactersMapper.update(cdo);

        // 保存背包物品
        List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
        for (InventoryType type : InventoryType.values()) {
            Inventory iv = player.getInventory(type);
            if (iv != null) {
                for (Item item : iv.list()) {
                    itemsWithType.add(new Pair<>(item, iv.getType()));
                }
            }
        }
        ItemFactory.INVENTORY.saveItems(itemsWithType, player.getId());

        // 保存商城数据
        if (player.getCashShop() != null) {
            player.getCashShop().save();
        }
        // 保存愿望单
        saveWishlist(player);
        // 保存技能
        saveSkills(player.getId(), player.getSkills());
        // 保存技能宏
        saveSkillMacros(player.getId(), player.getSkillMacros());
        // 保存按键设置
        saveKeymap(player.getId(), player.getKeymap());
        // 保存地图位置
        saveSavedLocations(player.getId(), player.getSavedLocations());
        // 保存传送石位置
        saveTrockLocations(player.getId(), player.getTrockMaps(), player.getVipTrockMaps(), player.getScriptsTrockMaps());
        // 保存好友列表
        saveBuddies(player.getId(), player.getBuddylist());
        // 保存区域信息
        saveAreaInfos(player.getId(), player.getAreaInfos());
        // 保存事件统计
        saveEventStats(player.getId(), player.getEvents());
        // 保存冷却时间
        saveCooldowns(player.getId(), player.getAllCooldowns(), player.getAllDiseases());
        // 保存宠物忽略物品
        savePetIgnores(player.getId(), player.getExcluded());
        // 保存快捷键
        if (player.getQuickSlotKeyMapped() != null) {
            saveQuickSlotKeyMap(player.getAccountId(), player.getQuickSlotKeyMapped().GetKeybindings(), player.getQuickSlotLoaded());
        }

        // 保存怪物图鉴
        if (player.getMonsterBook() != null) {
            player.getMonsterBook().saveCards(player.getId());
        }

        // [修改] 保存任务时，调用完全同步方法
        questService.syncCharacterQuests(player.getId(), new ArrayList<>(player.getQuests().values()));
    }

    /**
     * 增量保存角色的愿望单
     * @param player 角色对象
     */
    @Transactional
    public void saveWishlist(Character player) {
        if (player == null || player.getCashShop() == null) {
            return;
        }
        int characterId = player.getId();
        List<Integer> currentWishlist = player.getCashShop().getWishList();

        // 1. 从数据库加载旧的 wishlist
        List<WishlistsDO> dbWishlist = wishlistsMapper.selectListByQuery(
            QueryWrapper.create().where(WISHLISTS_DO.CHARID.eq(characterId))
        );
        Map<Integer, WishlistsDO> dbMap = dbWishlist.stream()
            .collect(Collectors.toMap(WishlistsDO::getSn, Function.identity()));

        Set<Integer> currentSnSet = new HashSet<>(currentWishlist);

        // 2. 找出需要删除的
        List<Integer> idsToDelete = dbWishlist.stream()
            .filter(dbItem -> !currentSnSet.contains(dbItem.getSn()))
            .map(WishlistsDO::getId)
            .collect(Collectors.toList());

        if (!idsToDelete.isEmpty()) {
            wishlistsMapper.deleteBatchByIds(idsToDelete);
        }

        // 3. 找出需要新增的
        List<WishlistsDO> toInsert = new ArrayList<>();
        for (Integer sn : currentWishlist) {
            if (!dbMap.containsKey(sn)) {
                toInsert.add(WishlistsDO.builder().charid(characterId).sn(sn).build());
            }
        }

        if (!toInsert.isEmpty()) {
            wishlistsMapper.insertBatch(toInsert);
        }
    }

    /**
     * 从数据库加载角色数据
     * @param cid 角色ID
     * @param client 客户端对象
     * @param channelServer 是否为频道服务器加载
     * @return 角色对象
     */
    public Character loadCharFromDB(int cid, Client client, boolean channelServer) {
        long startTime = System.currentTimeMillis(); // [日志] 开始计时
        CharactersDO charactersDO = findById(cid);
        RequireUtil.requireNotNull(charactersDO, I18nUtil.getExceptionMessage("UNKNOWN_CHARACTER"));
        Character chr = Character.fromCharactersDO(charactersDO, client);

        // [优化] 如果不是为频道服务器加载（即在角色选择界面），则只加载外观数据后直接返回
        if (!channelServer) {
//            log.info("[角色加载优化] 开始为角色选择界面轻量加载角色ID: {}", cid);

            // [优化] 只加载已装备的物品用于显示外观
            List<InventorySearchRtnDTO> equippedItems = inventoryService.getInventoryList(
                InventorySearchReqDTO.builder()
                    .characterId(cid)
                    .inventoryType(InventoryType.EQUIPPED.getType())
                    .build()
            );
            
            Inventory equipInventory = chr.getInventory(InventoryType.EQUIPPED);
            for (InventorySearchRtnDTO itemDTO : equippedItems) {
                equipInventory.addItemFromDB(itemDTO.toItem());
            }
            
//            long duration = System.currentTimeMillis() - startTime;
//            log.info("[角色加载优化] 角色ID: {} 轻量加载完成。加载物品数量: {}, 耗时: {} ms", cid, equippedItems.size(), duration);

            return chr;
        }

        MapManager mapManager = client.getChannelServer().getMapFactory();
        MapleMap mapleMap = mapManager.getMap(chr.getMapId());
        if (mapleMap == null) {
            mapleMap = mapManager.getMap(MapId.HENESYS);
        }
        chr.setMap(mapleMap);
        Portal portal = mapleMap.getPortal(chr.getInitialSpawnPoint());
        if (portal == null) {
            portal = mapleMap.getPortal(0);
            chr.setInitialSpawnPoint(0);
        }
        chr.setPosition(portal.getPosition());

        World world = Server.getInstance().getWorld(charactersDO.getWorld());
        int partyId = charactersDO.getParty();
        Party party = world.getParty(partyId);
        if (party != null) {
            PartyCharacter partyCharacter = party.getMemberById(cid);
            if (partyCharacter != null) {
                chr.setMPC(new PartyCharacter(chr));
                chr.setParty(party);
            }
        }

        int messengerId = charactersDO.getMessengerid();
        int messengerPosition = charactersDO.getMessengerposition();
        if (messengerId > 0 && messengerPosition < 4 && messengerPosition > -1) {
            Messenger messenger = world.getMessenger(messengerId);
            if (messenger != null) {
                chr.setMessenger(messenger);
                chr.setMessengerPosition(messengerPosition);
            }
        }
        chr.setLoggedIn(true);

        // [核心修复] 加载所有类型的角色物品
        for (ItemFactory factory : ItemFactory.values()) {
            // 只加载属于角色的物品 (非账号共享)，并排除特殊类型如雇佣商人和快递
            if (!factory.isAccount() && factory != ItemFactory.MERCHANT && factory != ItemFactory.DUEY) {
                List<Pair<Item, InventoryType>> items = factory.loadItems(cid, false);
                for (Pair<Item, InventoryType> itemPair : items) {
                    Inventory iv = chr.getInventory(itemPair.getRight());
                    if (iv != null) {
                        iv.addItemFromDB(itemPair.getLeft());
                    }
                }
            }
        }

        List<QuestStatus> questStatusList = questService.getQuestStatusByCharacter(cid);
        questStatusList.forEach(questStatus -> chr.getQuests().put(questStatus.getQuestID(), questStatus));

        List<SkillsDO> skillsDOList = skillsMapper.selectListByQuery(QueryWrapper.create().where(SKILLS_DO.CHARACTERID.eq(cid)));
        skillsDOList.forEach(skillsDO -> {
            Skill skill = SkillFactory.getSkill(skillsDO.getSkillid());
            if (skill != null) {
                chr.getEditableSkills().put(skill, new SkillEntry(Optional.ofNullable(skillsDO.getSkilllevel()).map(Integer::byteValue).orElse((byte) 0),
                        Optional.ofNullable(skillsDO.getMasterlevel()).map(Integer::byteValue).orElse((byte) 0), skillsDO.getExpiration()));
            }
        });

        QueryWrapper cdQueryWrapper = QueryWrapper.create().where(COOLDOWNS_DO.CHARID.eq(cid));
        List<CooldownsDO> cooldownsDOList = cooldownsMapper.selectListByQuery(cdQueryWrapper);
        cooldownsDOList.forEach(cooldownsDO -> {
            if (cooldownsDO.getSkillid() != 5221999 && cooldownsDO.getLength() + cooldownsDO.getStarttime() < System.currentTimeMillis()) {
                return;
            }
            chr.giveCoolDowns(cooldownsDO.getSkillid(), cooldownsDO.getStarttime(), cooldownsDO.getLength());
        });
        cooldownsMapper.deleteByQuery(cdQueryWrapper);

        QueryWrapper pdWrapper = QueryWrapper.create().where(PLAYERDISEASES_DO.CHARID.eq(cid));
        List<PlayerdiseasesDO> playerdiseasesDOList = playerdiseasesMapper.selectListByQuery(pdWrapper);
        Map<Disease, Pair<Long, MobSkill>> loadedDiseases = new LinkedHashMap<>();
        playerdiseasesDOList.forEach(playerdiseasesDO -> {
            Disease ordinal = Disease.ordinal(playerdiseasesDO.getDisease());
            if (Disease.NULL.equals(ordinal)) {
                return;
            }
            MobSkillType mobSkillType = MobSkillType.from(playerdiseasesDO.getMobskillid()).orElseThrow();
            MobSkill mobSkill = MobSkillFactory.getMobSkillOrThrow(mobSkillType, playerdiseasesDO.getMobskilllv());
            loadedDiseases.put(ordinal, new Pair<>(playerdiseasesDO.getLength(), mobSkill));
        });
        playerdiseasesMapper.deleteByQuery(pdWrapper);
        if (!loadedDiseases.isEmpty()) {
            Server.getInstance().getPlayerBuffStorage().addDiseasesToStorage(cid, loadedDiseases);
        }

        List<SkillmacrosDO> skillmacrosDOList = skillmacrosMapper.selectListByQuery(QueryWrapper.create().where(SKILLMACROS_DO.CHARACTERID.eq(cid)));
        skillmacrosDOList.forEach(skillmacrosDO -> chr.getSkillMacros()[skillmacrosDO.getPosition()] = new SkillMacro(
                skillmacrosDO.getSkill1(), skillmacrosDO.getSkill2(), skillmacrosDO.getSkill3(), skillmacrosDO.getName(),
                skillmacrosDO.getShout(), skillmacrosDO.getPosition()
        ));

        List<KeymapDO> keymapDOList = keymapMapper.selectListByQuery(QueryWrapper.create().where(KEYMAP_DO.CHARACTERID.eq(cid)));
        keymapDOList.forEach(keymapDO -> chr.getKeymap().put(keymapDO.getKey(), new KeyBinding(keymapDO.getType(), keymapDO.getAction())));

        List<SavedlocationsDO> savedlocationsDOList = savedlocationsMapper.selectListByQuery(QueryWrapper.create().where(SAVEDLOCATIONS_DO.CHARACTERID.eq(cid)));
        savedlocationsDOList.forEach(savedlocationsDO -> chr.getSavedLocations()[SavedLocationType.valueOf(savedlocationsDO.getLocationtype()).ordinal()]
                = new SavedLocation(savedlocationsDO.getMap(), savedlocationsDO.getPortal()));

        List<FamelogDO> famelogDOList = famelogMapper.selectListByQuery(QueryWrapper.create()
                .where(FAMELOG_DO.CHARACTERID.eq(cid)).and(dateDiff(now(), FAMELOG_DO.WHEN).lt(30)));
        long lastFameTime = 0;
        List<Integer> lastMonthFameIds = new ArrayList<>(31);
        for (FamelogDO famelogDO : famelogDOList) {
            lastFameTime = Math.max(lastFameTime, famelogDO.getWhen().getTime());
            lastMonthFameIds.add(famelogDO.getCharacteridTo());
        }
        chr.setLastfametime(lastFameTime);
        chr.setLastmonthfameids(lastMonthFameIds);

        chr.getBuddylist().loadFromDb(cid);
        Storage accountStorage = world.getAccountStorage(charactersDO.getAccountid());
        if (accountStorage == null) {
            world.loadAccountStorage(charactersDO.getAccountid());
            accountStorage = world.getAccountStorage(charactersDO.getAccountid());
        }
        chr.setStorage(accountStorage);
        chr.reapplyLocalStats();
        chr.changeHpMp(charactersDO.getHp(), charactersDO.getMp(), true);
        return chr;
    }

    public List<TrocklocationsDO> getTrockLocationByCharacter(Integer cid) {
        return trocklocationsMapper.selectListByQuery(QueryWrapper.create().where(TROCKLOCATIONS_DO.CHARACTERID.eq(cid)));
    }

    public List<AreaInfoDO> getAreaInfoByCharacter(Integer cid) {
        return areaInfoMapper.selectListByQuery(QueryWrapper.create().where(AREA_INFO_DO.CHARID.eq(cid)));
    }

    public List<EventstatsDO> getEventStatsByCharacter(Integer cid) {
        return eventstatsMapper.selectListByQuery(QueryWrapper.create().where(EVENTSTATS_DO.CHARACTERID.eq(cid)));
    }

    public List<WishlistsDO> getWishlistsByCharacter(Integer cid) {
        return wishlistsMapper.selectListByQuery(QueryWrapper.create().where(WISHLISTS_DO.CHARID.eq(cid)));
    }

    /**
     * 根据账号ID获取角色视图列表（轻量级数据，用于登录界面显示）
     * 只查询必要字段，避免全量加载
     * @param worldId 世界ID，-1时查询所有世界
     * @param accountId 账号ID
     * @return 角色实体列表，只包含视图所需字段
     */
    public List<CharactersDO> getCharactersViewByAccountId(int worldId,int accountId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(CHARACTERS_DO)
                .where(CHARACTERS_DO.ACCOUNTID.eq(accountId))
                .and(CHARACTERS_DO.WORLD.ge(worldId, id -> worldId >= 0)
                );
        return charactersMapper.selectListByQuery(queryWrapper);
    }

    public List<CharactersDO> getCharactersByAccountId(int accountId) {
        return charactersMapper.selectListByQuery(QueryWrapper.create().where(CHARACTERS_DO.ACCOUNTID.eq(accountId)));
    }

    public CharactersDO getCharacterByAccountId(int accountId) {
        List<CharactersDO> charactersDOS = charactersMapper.selectListByQuery(QueryWrapper.create().where(CHARACTERS_DO.ACCOUNTID.eq(accountId)));
        return charactersDOS.isEmpty() ? null : charactersDOS.getFirst();
    }

    private void checkName(ExtendValueDO data) {
        check(data);
        // 非法请求篡改其他字段
        if ("expRate".equals(data.getExtendName()) || "dropRate".equals(data.getExtendName()) || "mesoRate".equals(data.getExtendName())) {
            return;
        }
        throw BizException.illegalArgument();
    }

    private void check(ExtendValueDO data) {
        RequireUtil.requireNotEmpty(data.getExtendId(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_EMPTY", "extendId"));
        RequireUtil.requireNotEmpty(data.getExtendType(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_EMPTY", "extendType"));
        RequireUtil.requireNotEmpty(data.getExtendName(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_EMPTY", "extendName"));
    }

    private Character getCharacter(ExtendValueDO data) {
        for (World world : Server.getInstance().getWorlds()) {
            for (Character character : world.getPlayerStorage().getAllCharacters()) {
                if (ExtendType.isAccount(data.getExtendType()) && Objects.equals(String.valueOf(character.getAccountId()), data.getExtendId())) {
                    return character;
                }

                if (ExtendType.isCharacter(data.getExtendType()) && Objects.equals(String.valueOf(character.getId()), data.getExtendId())) {
                    return character;
                }
            }
        }
        throw BizException.illegalArgument(I18nUtil.getExceptionMessage("CharacterService.getCharacter.exception1"));
    }

    public Integer getAccountIdByName(String name) {
        CharactersDO charactersDO = findByName(name);
        return charactersDO == null ? -1 : charactersDO.getAccountid();
    }

    public Integer getIdByName(String name) {
        CharactersDO charactersDO = findByName(name);
        return charactersDO == null ? -1 : charactersDO.getId();
    }

    public String getNameById(int id) {
        CharactersDO charactersDO = findById(id);
        return charactersDO == null ? null : charactersDO.getName();
    }

    public int getMerchantNetMeso(int cid) {
        FredstorageDO fredstorageDO = fredstorageMapper.selectOneByQuery(QueryWrapper.create().select(FREDSTORAGE_DO.TIMESTAMP).where(FREDSTORAGE_DO.CID.eq(cid)));
        if (fredstorageDO != null) {
            return (int) FredrickProcessor.timestampElapsedDays(fredstorageDO.getTimestamp(), System.currentTimeMillis());
        }
        return 0;
    }

    public void addFameLog(int characterId, int characterIdTo) {
        FamelogDO famelogDO = new FamelogDO();
        famelogDO.setCharacterid(characterId);
        famelogDO.setCharacteridTo(characterIdTo);
        famelogDO.setWhen(new Timestamp(System.currentTimeMillis()));
        famelogMapper.insert(famelogDO);
    }

    public void updateGuildStatus(int id, int guildId, int guildRank, int allianceRank) {
        CharactersDO charactersDO = new CharactersDO();
        charactersDO.setId(id);
        charactersDO.setGuildid(guildId);
        charactersDO.setGuildrank(guildRank);
        charactersDO.setAllianceRank(allianceRank);
        charactersMapper.update(charactersDO);
    }

    @Transactional
    public void saveCooldowns(int charId, List<PlayerCoolDownValueHolder> cooldowns, Map<Disease, Pair<Long, MobSkill>> diseases) {
        // Cooldowns
        List<CooldownsDO> dbCooldowns = cooldownsMapper.selectListByQuery(QueryWrapper.create().where(COOLDOWNS_DO.CHARID.eq(charId)));
        Map<Integer, CooldownsDO> dbCooldownMap = dbCooldowns.stream().collect(Collectors.toMap(CooldownsDO::getSkillid, Function.identity()));
        Set<Integer> processedSkillIds = new HashSet<>();

        if (cooldowns != null) {
            for (PlayerCoolDownValueHolder cd : cooldowns) {
                processedSkillIds.add(cd.skillId);
                if (dbCooldownMap.containsKey(cd.skillId)) {
                    CooldownsDO existing = dbCooldownMap.get(cd.skillId);
                    if (existing.getStarttime() != cd.startTime || existing.getLength() != cd.length) {
                        existing.setStarttime(cd.startTime);
                        existing.setLength(cd.length);
                        cooldownsMapper.update(existing);
                    }
                } else {
                    CooldownsDO doo = new CooldownsDO();
                    doo.setCharid(charId);
                    doo.setSkillid(cd.skillId);
                    doo.setStarttime(cd.startTime);
                    doo.setLength(cd.length);
                    cooldownsMapper.insert(doo);
                }
            }
        }
        
        List<Integer> cdIdsToDelete = dbCooldowns.stream()
                .filter(cd -> !processedSkillIds.contains(cd.getSkillid()))
                .map(CooldownsDO::getId)
                .collect(Collectors.toList());
        if (!cdIdsToDelete.isEmpty()) {
            cooldownsMapper.deleteByQuery(QueryWrapper.create().where(COOLDOWNS_DO.ID.in(cdIdsToDelete)));
        }

        // Diseases
        List<PlayerdiseasesDO> dbDiseases = playerdiseasesMapper.selectListByQuery(QueryWrapper.create().where(PLAYERDISEASES_DO.CHARID.eq(charId)));
        Map<Integer, PlayerdiseasesDO> dbDiseaseMap = dbDiseases.stream().collect(Collectors.toMap(PlayerdiseasesDO::getDisease, Function.identity()));
        Set<Integer> processedDiseases = new HashSet<>();

        if (diseases != null) {
            for (Map.Entry<Disease, Pair<Long, MobSkill>> entry : diseases.entrySet()) {
                int diseaseId = entry.getKey().ordinal();
                processedDiseases.add(diseaseId);
                MobSkill ms = entry.getValue().getRight();
                long length = entry.getValue().getLeft();
                
                if (dbDiseaseMap.containsKey(diseaseId)) {
                    PlayerdiseasesDO existing = dbDiseaseMap.get(diseaseId);
                    if (existing.getMobskillid() != ms.getId().type().getId() || 
                        existing.getMobskilllv() != ms.getId().level() || 
                        existing.getLength() != length) {
                        
                        existing.setMobskillid(ms.getId().type().getId());
                        existing.setMobskilllv(ms.getId().level());
                        existing.setLength(length);
                        playerdiseasesMapper.update(existing);
                    }
                } else {
                    PlayerdiseasesDO doo = new PlayerdiseasesDO();
                    doo.setCharid(charId);
                    doo.setDisease(diseaseId);
                    doo.setMobskillid(ms.getId().type().getId());
                    doo.setMobskilllv(ms.getId().level());
                    doo.setLength(length);
                    playerdiseasesMapper.insert(doo);
                }
            }
        }

        List<Integer> pdIdsToDelete = dbDiseases.stream()
                .filter(pd -> !processedDiseases.contains(pd.getDisease()))
                .map(PlayerdiseasesDO::getId)
                .collect(Collectors.toList());
        if (!pdIdsToDelete.isEmpty()) {
            playerdiseasesMapper.deleteByQuery(QueryWrapper.create().where(PLAYERDISEASES_DO.ID.in(pdIdsToDelete)));
        }
    }

    @Transactional
    public void saveKeymap(int charId, Map<Integer, KeyBinding> keymap) {
        List<KeymapDO> dbKeymaps = keymapMapper.selectListByQuery(QueryWrapper.create().where(KEYMAP_DO.CHARACTERID.eq(charId)));
        Map<Integer, KeymapDO> dbKeymapMap = dbKeymaps.stream().collect(Collectors.toMap(KeymapDO::getKey, Function.identity()));
        Set<Integer> processedKeys = new HashSet<>();

        if (keymap != null) {
            for (Map.Entry<Integer, KeyBinding> entry : keymap.entrySet()) {
                int key = entry.getKey();
                KeyBinding kb = entry.getValue();
                processedKeys.add(key);

                if (dbKeymapMap.containsKey(key)) {
                    KeymapDO existing = dbKeymapMap.get(key);
                    if (existing.getType() != kb.getType() || existing.getAction() != kb.getAction()) {
                        existing.setType(kb.getType());
                        existing.setAction(kb.getAction());
                        keymapMapper.update(existing);
                    }
                } else {
                    KeymapDO doo = new KeymapDO();
                    doo.setCharacterid(charId);
                    doo.setKey(key);
                    doo.setType(kb.getType());
                    doo.setAction(kb.getAction());
                    keymapMapper.insert(doo);
                }
            }
        }

        List<Integer> idsToDelete = dbKeymaps.stream()
                .filter(k -> !processedKeys.contains(k.getKey()))
                .map(KeymapDO::getId)
                .collect(Collectors.toList());
        if (!idsToDelete.isEmpty()) {
            keymapMapper.deleteByQuery(QueryWrapper.create().where(KEYMAP_DO.ID.in(idsToDelete)));
        }
    }

    @Transactional
    public void saveSkillMacros(int charId, SkillMacro[] skillMacros) {
        List<SkillmacrosDO> dbMacros = skillmacrosMapper.selectListByQuery(QueryWrapper.create().where(SKILLMACROS_DO.CHARACTERID.eq(charId)));
        Map<Integer, SkillmacrosDO> dbMacroMap = dbMacros.stream().collect(Collectors.toMap(SkillmacrosDO::getPosition, Function.identity()));
        Set<Integer> processedPositions = new HashSet<>();

        if (skillMacros != null) {
            for (int i = 0; i < skillMacros.length; i++) {
                SkillMacro macro = skillMacros[i];
                if (macro != null) {
                    processedPositions.add(i);
                    String name = macro.getName() == null ? "" : macro.getName();
                    
                    if (dbMacroMap.containsKey(i)) {
                        SkillmacrosDO existing = dbMacroMap.get(i);
                        if (existing.getSkill1() != macro.getSkill1() ||
                            existing.getSkill2() != macro.getSkill2() ||
                            existing.getSkill3() != macro.getSkill3() ||
                            !Objects.equals(existing.getName(), name) ||
                            existing.getShout() != macro.getShout()) {
                            
                            existing.setSkill1(macro.getSkill1());
                            existing.setSkill2(macro.getSkill2());
                            existing.setSkill3(macro.getSkill3());
                            existing.setName(name);
                            existing.setShout(macro.getShout());
                            skillmacrosMapper.update(existing);
                        }
                    } else {
                        SkillmacrosDO doo = new SkillmacrosDO();
                        doo.setCharacterid(charId);
                        doo.setSkill1(macro.getSkill1());
                        doo.setSkill2(macro.getSkill2());
                        doo.setSkill3(macro.getSkill3());
                        doo.setName(name);
                        doo.setShout(macro.getShout());
                        doo.setPosition(i);
                        skillmacrosMapper.insert(doo);
                    }
                }
            }
        }

        List<Integer> idsToDelete = dbMacros.stream()
                .filter(m -> !processedPositions.contains(m.getPosition()))
                .map(SkillmacrosDO::getId)
                .collect(Collectors.toList());
        if (!idsToDelete.isEmpty()) {
            skillmacrosMapper.deleteByQuery(QueryWrapper.create().where(SKILLMACROS_DO.ID.in(idsToDelete)));
        }
    }

    @Transactional
    public void saveSavedLocations(int charId, SavedLocation[] savedLocations) {
        List<SavedlocationsDO> dbLocs = savedlocationsMapper.selectListByQuery(QueryWrapper.create().where(SAVEDLOCATIONS_DO.CHARACTERID.eq(charId)));
        Map<String, SavedlocationsDO> dbLocMap = dbLocs.stream().collect(Collectors.toMap(SavedlocationsDO::getLocationtype, Function.identity()));
        Set<String> processedTypes = new HashSet<>();

        if (savedLocations != null) {
            for (SavedLocationType savedLocationType : SavedLocationType.values()) {
                if (savedLocations[savedLocationType.ordinal()] != null) {
                    String typeName = savedLocationType.name();
                    processedTypes.add(typeName);
                    SavedLocation sl = savedLocations[savedLocationType.ordinal()];
                    
                    if (dbLocMap.containsKey(typeName)) {
                        SavedlocationsDO existing = dbLocMap.get(typeName);
                        if (existing.getMap() != sl.getMapId() || existing.getPortal() != sl.getPortal()) {
                            existing.setMap(sl.getMapId());
                            existing.setPortal(sl.getPortal());
                            savedlocationsMapper.update(existing);
                        }
                    } else {
                        SavedlocationsDO doo = new SavedlocationsDO();
                        doo.setCharacterid(charId);
                        doo.setLocationtype(typeName);
                        doo.setMap(sl.getMapId());
                        doo.setPortal(sl.getPortal());
                        savedlocationsMapper.insert(doo);
                    }
                }
            }
        }

        List<Integer> idsToDelete = dbLocs.stream()
                .filter(l -> !processedTypes.contains(l.getLocationtype()))
                .map(SavedlocationsDO::getId)
                .collect(Collectors.toList());
        if (!idsToDelete.isEmpty()) {
            savedlocationsMapper.deleteByQuery(QueryWrapper.create().where(SAVEDLOCATIONS_DO.ID.in(idsToDelete)));
        }
    }

    @Transactional
    public void saveTrockLocations(int charId, List<Integer> trockMaps, List<Integer> vipTrockMaps, List<Integer> scriptTrockMaps) {
        List<TrocklocationsDO> dbLocs = trocklocationsMapper.selectListByQuery(QueryWrapper.create().where(TROCKLOCATIONS_DO.CHARACTERID.eq(charId)));
        // Map key: "mapId_vip"
        Map<String, TrocklocationsDO> dbLocMap = dbLocs.stream().collect(Collectors.toMap(d -> d.getMapid() + "_" + d.getVip(), Function.identity(), (a, b) -> a));
        Set<String> processedKeys = new HashSet<>();

        if (trockMaps != null) {
            for (Integer mapId : trockMaps) {
                if (mapId != MapId.NONE) {
                    String key = mapId + "_0";
                    processedKeys.add(key);
                    if (!dbLocMap.containsKey(key)) {
                        TrocklocationsDO doo = new TrocklocationsDO();
                        doo.setCharacterid(charId);
                        doo.setMapid(mapId);
                        doo.setVip(0);
                        trocklocationsMapper.insert(doo);
                    }
                }
            }
        }

        if (vipTrockMaps != null) {
            for (Integer mapId : vipTrockMaps) {
                if (mapId != MapId.NONE) {
                    String key = mapId + "_1";
                    processedKeys.add(key);
                    if (!dbLocMap.containsKey(key)) {
                        TrocklocationsDO doo = new TrocklocationsDO();
                        doo.setCharacterid(charId);
                        doo.setMapid(mapId);
                        doo.setVip(1);
                        trocklocationsMapper.insert(doo);
                    }
                }
            }
        }

        if (scriptTrockMaps != null) {
            for (Integer mapId : scriptTrockMaps) {
                String key = mapId + "_2";
                processedKeys.add(key);
                if (!dbLocMap.containsKey(key)) {
                    TrocklocationsDO doo = new TrocklocationsDO();
                    doo.setCharacterid(charId);
                    doo.setMapid(mapId);
                    doo.setVip(2);
                    trocklocationsMapper.insert(doo);
                }
            }
        }

        List<Integer> idsToDelete = dbLocs.stream()
                .filter(d -> !processedKeys.contains(d.getMapid() + "_" + d.getVip()))
                .map(TrocklocationsDO::getTrockid)
                .collect(Collectors.toList());
        
        if (!idsToDelete.isEmpty()) {
            trocklocationsMapper.deleteByQuery(QueryWrapper.create().where(TROCKLOCATIONS_DO.TROCKID.in(idsToDelete)));
        }
    }

    @Transactional
    public void saveBuddies(int charId, BuddyList buddylist) {
        List<BuddiesDO> dbBuddies = buddiesMapper.selectListByQuery(QueryWrapper.create().where(BUDDIES_DO.CHARACTERID.eq(charId)).and(BUDDIES_DO.PENDING.eq(0)));
        Map<Integer, BuddiesDO> dbBuddyMap = dbBuddies.stream().collect(Collectors.toMap(BuddiesDO::getBuddyid, Function.identity()));
        Set<Integer> processedBuddyIds = new HashSet<>();

        if (buddylist != null && !buddylist.getBuddies().isEmpty()) {
            for (BuddylistEntry entry : buddylist.getBuddies()) {
                if (entry.isVisible()) {
                    int buddyId = entry.getCharacterId();
                    processedBuddyIds.add(buddyId);
                    String group = entry.getGroup() == null ? "" : entry.getGroup();

                    if (dbBuddyMap.containsKey(buddyId)) {
                        BuddiesDO existing = dbBuddyMap.get(buddyId);
                        if (!Objects.equals(existing.getGroup(), group)) {
                            existing.setGroup(group);
                            buddiesMapper.update(existing);
                        }
                    } else {
                        BuddiesDO doo = new BuddiesDO();
                        doo.setCharacterid(charId);
                        doo.setBuddyid(buddyId);
                        doo.setPending(0);
                        doo.setGroup(group);
                        buddiesMapper.insert(doo);
                    }
                }
            }
        }

        List<Integer> idsToDelete = dbBuddies.stream()
                .filter(b -> !processedBuddyIds.contains(b.getBuddyid()))
                .map(BuddiesDO::getId)
                .collect(Collectors.toList());
        if (!idsToDelete.isEmpty()) {
            buddiesMapper.deleteByQuery(QueryWrapper.create().where(BUDDIES_DO.ID.in(idsToDelete)));
        }
    }

    @Transactional
    public void saveAreaInfos(int charId, Map<Short, String> areaInfos) {
        List<AreaInfoDO> dbInfos = areaInfoMapper.selectListByQuery(QueryWrapper.create().where(AREA_INFO_DO.CHARID.eq(charId)));
        Map<Integer, AreaInfoDO> dbInfoMap = dbInfos.stream().collect(Collectors.toMap(AreaInfoDO::getArea, Function.identity()));
        Set<Integer> processedAreas = new HashSet<>();

        if (areaInfos != null) {
            for (Map.Entry<Short, String> entry : areaInfos.entrySet()) {
                int area = entry.getKey().intValue();
                processedAreas.add(area);
                String info = entry.getValue();

                if (dbInfoMap.containsKey(area)) {
                    AreaInfoDO existing = dbInfoMap.get(area);
                    if (!Objects.equals(existing.getInfo(), info)) {
                        existing.setInfo(info);
                        areaInfoMapper.update(existing);
                    }
                } else {
                    AreaInfoDO doo = new AreaInfoDO();
                    doo.setCharid(charId);
                    doo.setArea(area);
                    doo.setInfo(info);
                    areaInfoMapper.insert(doo);
                }
            }
        }

        List<Integer> idsToDelete = dbInfos.stream()
                .filter(a -> !processedAreas.contains(a.getArea()))
                .map(AreaInfoDO::getId)
                .collect(Collectors.toList());
        if (!idsToDelete.isEmpty()) {
            areaInfoMapper.deleteByQuery(QueryWrapper.create().where(AREA_INFO_DO.ID.in(idsToDelete)));
        }
    }

    @Transactional
    public void saveEventStats(int charId, Map<String, Events> events) {
        List<EventstatsDO> dbStats = eventstatsMapper.selectListByQuery(QueryWrapper.create().where(EVENTSTATS_DO.CHARACTERID.eq(charId)));
        Map<String, EventstatsDO> dbStatMap = dbStats.stream().collect(Collectors.toMap(EventstatsDO::getName, Function.identity()));
        Set<String> processedNames = new HashSet<>();

        if (events != null) {
            for (Map.Entry<String, Events> entry : events.entrySet()) {
                String name = entry.getKey();
                processedNames.add(name);
                int info = entry.getValue().getInfo();

                if (dbStatMap.containsKey(name)) {
                    EventstatsDO existing = dbStatMap.get(name);
                    if (!Objects.equals(existing.getInfo(), info)) {
                        existing.setInfo(info);
                        eventstatsMapper.update(existing);
                    }
                } else {
                    EventstatsDO doo = new EventstatsDO();
                    doo.setCharacterid(charId);
                    doo.setName(name);
                    doo.setInfo(info);
                    eventstatsMapper.insert(doo);
                }
            }
        }

        List<String> namesToDelete = dbStats.stream()
                .filter(e -> !processedNames.contains(e.getName()))
                .map(EventstatsDO::getName)
                .collect(Collectors.toList());
        
        if (!namesToDelete.isEmpty()) {
            eventstatsMapper.deleteByQuery(QueryWrapper.create()
                    .where(EVENTSTATS_DO.CHARACTERID.eq(charId))
                    .and(EVENTSTATS_DO.NAME.in(namesToDelete)));
        }
    }

    @Transactional
    public void saveSkills(int charId, Map<Skill, SkillEntry> skills) {
        saveSkills(charId, skills, true);
    }

    @Transactional
    public void saveSkills(int charId, Map<Skill, SkillEntry> skills, boolean deleteOthers) {
        List<SkillsDO> dbSkills = skillsMapper.selectListByQuery(QueryWrapper.create().where(SKILLS_DO.CHARACTERID.eq(charId)));
        Map<Integer, SkillsDO> dbSkillMap = dbSkills.stream().collect(Collectors.toMap(SkillsDO::getSkillid, Function.identity()));
        Set<Integer> processedSkillIds = new HashSet<>();

        if (skills != null && !skills.isEmpty()) {
            for (Map.Entry<Skill, SkillEntry> entry : skills.entrySet()) {
                int skillId = entry.getKey().getId();
                SkillEntry se = entry.getValue();
                processedSkillIds.add(skillId);

                if (dbSkillMap.containsKey(skillId)) {
                    SkillsDO existing = dbSkillMap.get(skillId);
                    if (existing.getSkilllevel() != se.skillLevel || existing.getMasterlevel() != se.masterLevel || existing.getExpiration() != se.expiration) {
                        existing.setSkilllevel((int) se.skillLevel);
                        existing.setMasterlevel((int) se.masterLevel);
                        existing.setExpiration(se.expiration);
                        skillsMapper.update(existing);
                    }
                } else {
                    SkillsDO newSkill = new SkillsDO();
                    newSkill.setCharacterid(charId);
                    newSkill.setSkillid(skillId);
                    newSkill.setSkilllevel((int) se.skillLevel);
                    newSkill.setMasterlevel((int) se.masterLevel);
                    newSkill.setExpiration(se.expiration);
                    skillsMapper.insert(newSkill);
                }
            }
        }

        if (deleteOthers) {
            List<Integer> idsToDelete = dbSkills.stream()
                    .filter(s -> !processedSkillIds.contains(s.getSkillid()))
                    .map(SkillsDO::getId)
                    .collect(Collectors.toList());

            if (!idsToDelete.isEmpty()) {
                skillsMapper.deleteByQuery(QueryWrapper.create().where(SKILLS_DO.ID.in(idsToDelete)));
            }
        }
    }

    @Transactional
    public void savePetIgnores(int charId, Map<Integer, Set<Integer>> excluded) {
        for (Map.Entry<Integer, Set<Integer>> es : excluded.entrySet()) {
            int petId = es.getKey();
            Set<Integer> currentItemIds = es.getValue();
            
            List<PetignoresDO> dbIgnores = petignoresMapper.selectListByQuery(QueryWrapper.create().where(PETIGNORES_DO.PETID.eq(petId)));
            Set<Integer> dbItemIds = dbIgnores.stream().map(PetignoresDO::getItemid).collect(Collectors.toSet());
            
            // Insert new
            List<PetignoresDO> toInsert = new ArrayList<>();
            for (Integer itemId : currentItemIds) {
                if (!dbItemIds.contains(itemId)) {
                    PetignoresDO doo = new PetignoresDO();
                    doo.setPetid(petId);
                    doo.setItemid(itemId);
                    toInsert.add(doo);
                }
            }
            if (!toInsert.isEmpty()) {
                petignoresMapper.insertBatch(toInsert);
            }
            
            // Delete removed
            List<Long> idsToDelete = dbIgnores.stream()
                    .filter(doo -> !currentItemIds.contains(doo.getItemid()))
                    .map(PetignoresDO::getId)
                    .collect(Collectors.toList());
            
            if (!idsToDelete.isEmpty()) {
                petignoresMapper.deleteByQuery(QueryWrapper.create().where(PETIGNORES_DO.ID.in(idsToDelete)));
            }
        }
    }

    @Transactional
    public void saveQuickSlotKeyMap(int accountId, byte[] quickSlotKeyMapped, byte[] quickSlotLoaded) {
        boolean bQuickslotEquals = quickSlotKeyMapped == null || (quickSlotLoaded != null && Arrays.equals(quickSlotKeyMapped, quickSlotLoaded));
        if (!bQuickslotEquals) {
            long nQuickslotKeymapped = NumberTool.BytesToLong(quickSlotKeyMapped);
            
            QuickslotkeymappedDO existing = quickslotkeymappedMapper.selectOneById(accountId);
            if (existing != null) {
                existing.setKeymap(nQuickslotKeymapped);
                quickslotkeymappedMapper.update(existing);
            } else {
                QuickslotkeymappedDO doo = new QuickslotkeymappedDO();
                doo.setAccountid(accountId);
                doo.setKeymap(nQuickslotKeymapped);
                quickslotkeymappedMapper.insert(doo);
            }
        }
    }

    /**
     * 插入新角色
     * @param chr 角色对象
     * @param recipe 创建配方
     * @return 新角色ID
     */
    @Transactional
    public int insertNewChar(Character chr, org.gms.client.creator.CharacterFactoryRecipe recipe) {
        // Character info
        CharactersDO cdo = new CharactersDO();
        cdo.setAttrStr(recipe.getStr());
        cdo.setAttrDex(recipe.getDex());
        cdo.setAttrInt(recipe.getInt());
        cdo.setAttrLuk(recipe.getLuk());
        cdo.setGm(chr.gmLevel());
        cdo.setSkincolor(chr.getSkinColor().getId());
        cdo.setGender(chr.getGender());
        cdo.setJob(chr.getJob().getId());
        cdo.setHair(chr.getHair());
        cdo.setFace(chr.getFace());
        cdo.setMap(recipe.getMap());
        cdo.setMeso(Math.abs(recipe.getMeso()));
        cdo.setSpawnpoint(0);
        cdo.setAccountid(chr.getAccountId());
        cdo.setName(chr.getName());
        cdo.setWorld(chr.getWorld());
        cdo.setHp(recipe.getMaxHp());
        cdo.setMp(recipe.getMaxMp());
        cdo.setMaxhp(recipe.getMaxHp());
        cdo.setMaxmp(recipe.getMaxMp());
        cdo.setLevel(recipe.getLevel());
        cdo.setAp(recipe.getRemainingAp());
        cdo.setExp(0);
        cdo.setGachaexp(0);
        cdo.setHpMpUsed(0);
        cdo.setFame(0);
        cdo.setFquest(0);
        cdo.setParty(0);
        cdo.setBuddyCapacity(25);
        cdo.setRank(1);
        cdo.setRankMove(0);
        cdo.setJobRank(1);
        cdo.setJobRankMove(0);
        cdo.setGuildid(0);
        cdo.setGuildrank(5);
        cdo.setMessengerid(0);
        cdo.setMessengerposition(4);
        cdo.setMountlevel(1);
        cdo.setMountexp(0);
        cdo.setMounttiredness(0);
        cdo.setOmokwins(0);
        cdo.setOmoklosses(0);
        cdo.setOmokties(0);
        cdo.setMatchcardwins(0);
        cdo.setMatchcardlosses(0);
        cdo.setMatchcardties(0);
        cdo.setMerchantmesos(0);
        cdo.setHasmerchant(false);
        cdo.setEquipslots(24);
        cdo.setUseslots(24);
        cdo.setSetupslots(24);
        cdo.setEtcslots(24);
        cdo.setFamilyId(-1);
        cdo.setMonsterbookcover(0);
        cdo.setAllianceRank(5);
        cdo.setVanquisherStage(0);
        cdo.setAriantPoints(0);
        cdo.setDojoPoints(0);
        cdo.setLastDojoStage(0);
        cdo.setFinishedDojoTutorial(0);
        cdo.setVanquisherKills(0);
        cdo.setSummonValue(0L);
        cdo.setPartnerId(0);
        cdo.setMarriageItemId(0);
        cdo.setReborns(0);
        cdo.setPqpoints(0);
        cdo.setDataString("");
        cdo.setLastLogoutTime(new Timestamp(System.currentTimeMillis()));
        cdo.setLastExpGainTime(new Timestamp(System.currentTimeMillis()));
        cdo.setPartySearch(true);
        cdo.setJailexpire(0L);
        cdo.setCreatedate(new Timestamp(System.currentTimeMillis()));

        StringBuilder sps = new StringBuilder();
        for (int j : chr.getRemainingSps()) {
            sps.append(j);
            sps.append(",");
        }
        String sp = sps.toString();
        cdo.setSp(sp.substring(0, sp.length() - 1));

        charactersMapper.insert(cdo);
        int newId = cdo.getId();
        chr.setId(newId);

        // Key config
        int[] selectedKey;
        int[] selectedType;
        int[] selectedAction;

        boolean useCustomKeySet = GameConfig.getServerBoolean("use_custom_keyset");
        selectedKey = GameConstants.getCustomKey(useCustomKeySet);
        selectedType = GameConstants.getCustomType(useCustomKeySet);
        selectedAction = GameConstants.getCustomAction(useCustomKeySet);

        List<KeymapDO> keymapList = new ArrayList<>();
        for (int i = 0; i < selectedKey.length; i++) {
            KeymapDO keymapDO = new KeymapDO();
            keymapDO.setCharacterid(newId);
            keymapDO.setKey(selectedKey[i]);
            keymapDO.setType(selectedType[i]);
            keymapDO.setAction(selectedAction[i]);
            keymapList.add(keymapDO);
        }
        if (!keymapList.isEmpty()) {
            keymapMapper.insertBatch(keymapList);
        }

        // Quickslot key config
        if (chr.getQuickSlotKeyMapped() != null) {
            saveQuickSlotKeyMap(chr.getAccountId(), chr.getQuickSlotKeyMapped().GetKeybindings(), chr.getQuickSlotLoaded());
        }

        // Items
        List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
        for (InventoryType type : InventoryType.values()) {
            Inventory iv = chr.getInventory(type);
            if (iv != null) {
                for (Item item : iv.list()) {
                    itemsWithType.add(new Pair<>(item, iv.getType()));
                }
            }
        }
        ItemFactory.INVENTORY.saveItems(itemsWithType, newId);

        // Skills
        if (!chr.getSkills().isEmpty()) {
            saveSkills(newId, chr.getSkills());
        }

        return newId;
    }

    public void deleteWishlistsByCharacter(Integer cid) {
        wishlistsMapper.deleteByQuery(QueryWrapper.create().where(WISHLISTS_DO.CHARID.eq(cid)));
    }

    public void batchInsertWishlists(List<WishlistsDO> wishlists) {
        if (!wishlists.isEmpty()) {
            wishlistsMapper.insertBatch(wishlists);
        }
    }

    /**
     * 断开玩家连接
     * @param request 断开请求
     */
    public void disconnect(DisconnectReqDTO request) {
        List<Integer> ids = request.getIds();
        if (request.isAll()) {
            // 全服断开
            for (World world : Server.getInstance().getWorlds()) {
                for (Character chr : world.getPlayerStorage().getAllCharacters()) {
                    if (chr != null && chr.getClient() != null) {
                        chr.getClient().disconnect(false, false);
                    }
                }
            }
        } else if (ids != null && !ids.isEmpty()) {
            // 批量断开
            for (Integer id : ids) {
                for (World world : Server.getInstance().getWorlds()) {
                    Character chr = world.getPlayerStorage().getCharacterById(id);
                    if (chr != null && chr.getClient() != null) {
                        chr.getClient().disconnect(false, false);
                        break; // 找到后跳出内层循环
                    }
                }
            }
        }
    }

    /**
     * 封禁玩家
     * @param request 封禁请求
     */
    public void ban(BanPlayerReqDTO request) {
        List<Integer> ids = request.getIds();
        String reason = request.getReason();
        Integer duration = request.getDuration();
        Long banUntil = request.getBanUntil();
        boolean banIp = request.isBanIp();
        boolean banMac = request.isBanMac();
        boolean banHwid = request.isBanHwid();
        boolean notify = request.isNotify();
        String notifyContent = request.getNotifyContent();

        // 计算封禁截止时间
        Timestamp tempBan = null;
        boolean isTempBan = false;
        if (banUntil != null && banUntil > 0) {
            tempBan = new Timestamp(banUntil);
            isTempBan = true;
        } else if (duration != null && duration > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, duration);
            tempBan = new Timestamp(cal.getTimeInMillis());
            isTempBan = true;
        } else {
            // 永久封禁，tempban设置为null，避免解封后因tempban未过期导致无法登录
            tempBan = null;
            isTempBan = false;
        }

        List<Integer> targetIds = new ArrayList<>();
        if (request.isAll()) {
            // 全服封禁
            for (World world : Server.getInstance().getWorlds()) {
                for (Character chr : world.getPlayerStorage().getAllCharacters()) {
                    targetIds.add(chr.getId());
                }
            }
        } else if (ids != null) {
            targetIds.addAll(ids);
        }

        for (Integer charId : targetIds) {
            // 1. 获取角色信息（在线或离线）
            Pair<Character, CharactersDO> pair = getOnlineOrOfflineCharacter(charId);
            Character onlineChr = pair.getLeft();
            CharactersDO offlineChr = pair.getRight();

            if (onlineChr == null && offlineChr == null) continue;

            int accountId = (onlineChr != null) ? onlineChr.getAccountId() : offlineChr.getAccountid();
            if (accountId == -1) continue;

            // 2. 封禁账号
            // 优化封号逻辑，仅临时封禁，则不能永久封禁账号 (banned = false)
            AccountsDO accountUpdate = AccountsDO.builder()
                    .id(accountId)
                    .banned(!isTempBan) 
                    .banreason(reason)
                    .tempban(tempBan)
                    .build();
            accountService.update(accountUpdate);

            // 3. 处理在线玩家的额外操作
            if (onlineChr != null) {
                onlineChr.setBanned(true); // Always set true in memory to indicate active ban
                Client c = onlineChr.getClient();
                
                if (banIp) {
                    if (request.getIps() != null && !request.getIps().isEmpty()) {
                        for (String ip : request.getIps()) {
                            accountService.banIp(ip, accountId);
                        }
                    } else {
                        accountService.banIp(c.getRemoteAddress(), accountId);
                    }
                }
                if (banMac) {
                    if (request.getMacs() != null && !request.getMacs().isEmpty()) {
                        accountService.banMacs(new HashSet<>(request.getMacs()), accountId);
                    } else {
                        accountService.banMacs(c.getMacs(), accountId);
                    }
                }
                if (banHwid) {
                    if (c.getHwid() != null) {
                        accountService.banHwid(c.getHwid().hwid());
                    }
                }
                
                c.disconnect(false, false);
            } else {
                // 离线玩家无法获取IP/MAC/HWID，只能封禁账号
                // 如果需要支持离线封禁IP/MAC，需要从数据库读取历史记录（如果有的话）
                // 目前仅支持在线封禁连带IP/MAC
                if (banIp || banMac || banHwid) {
                    AccountsDO account = accountService.findById(accountId);
                    if (account != null) {
                        if (banIp) {
                            if (request.getIps() != null && !request.getIps().isEmpty()) {
                                for (String ip : request.getIps()) {
                                    accountService.banIp(ip, accountId);
                                }
                            } else if (account.getIp() != null && !account.getIp().isEmpty()) {
                                String[] ips = account.getIp().split(",");
                                for (String ip : ips) {
                                    if (!ip.trim().isEmpty()) {
                                        accountService.banIp(ip.trim(), accountId);
                                    }
                                }
                            }
                        }
                        if (banMac) {
                            if (request.getMacs() != null && !request.getMacs().isEmpty()) {
                                accountService.banMacs(new HashSet<>(request.getMacs()), accountId);
                            } else if (account.getMacs() != null && !account.getMacs().isEmpty()) {
                                String[] macs = account.getMacs().split(",");
                                Set<String> macSet = new HashSet<>();
                                for (String mac : macs) {
                                    if (!mac.trim().isEmpty()) {
                                        macSet.add(mac.trim());
                                    }
                                }
                                if (!macSet.isEmpty()) {
                                    accountService.banMacs(macSet, accountId);
                                }
                            }
                        }
                        if (banHwid && account.getHwid() != null && !account.getHwid().isEmpty()) {
                            accountService.banHwid(account.getHwid());
                        }
                    }
                }
            }
            
            // 4. 全服通知 (单人)
            if (notify) {
                String name = (onlineChr != null) ? onlineChr.getName() : (offlineChr != null ? offlineChr.getName() : "未知");
                String msg;
                if (notifyContent != null && !notifyContent.isEmpty()) {
                    reason = notifyContent;
                }
                msg = I18nUtil.getMessage("Character.ban.notice", name, reason);
                int worldId = (onlineChr != null) ? onlineChr.getWorld() : (offlineChr != null ? offlineChr.getWorld() : 0);
                Server.getInstance().broadcastMessage(worldId, PacketCreator.sendYellowTip(msg));
            }
        }
    }

    /**
     * 获取封禁信息
     * @param charId 角色ID
     * @return 封禁信息DTO
     */
    public BanInfoRtnDTO getBanInfo(Integer charId) {
        Pair<Character, CharactersDO> pair = getOnlineOrOfflineCharacter(charId);
        Character onlineChr = pair.getLeft();
        CharactersDO offlineChr = pair.getRight();

        if (onlineChr == null && offlineChr == null) return BanInfoRtnDTO.builder().build();

        int accountId = (onlineChr != null) ? onlineChr.getAccountId() : offlineChr.getAccountid();
        if (accountId == -1) return BanInfoRtnDTO.builder().build();

        AccountsDO account = accountService.findById(accountId);
        if (account == null) return BanInfoRtnDTO.builder().build();

        List<String> ips = new ArrayList<>();
        if (account.getIp() != null && !account.getIp().isEmpty()) {
            ips = Arrays.stream(account.getIp().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        List<String> macs = new ArrayList<>();
        if (account.getMacs() != null && !account.getMacs().isEmpty()) {
            macs = Arrays.stream(account.getMacs().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return BanInfoRtnDTO.builder()
                .ips(ips)
                .macs(macs)
                .hwid(account.getHwid())
                .build();
    }
    
    /**
     * 解封玩家
     * @param request 解封请求
     */
    public void unban(ChrIdDTO request) {
        Integer charId = request.getId();
        Pair<Character, CharactersDO> pair = getOnlineOrOfflineCharacter(charId);
        Character onlineChr = pair.getLeft();
        CharactersDO offlineChr = pair.getRight();

        if (onlineChr == null && offlineChr == null) return;

        int accountId = (onlineChr != null) ? onlineChr.getAccountId() : offlineChr.getAccountid();
        if (accountId == -1) return;
        
        accountService.unbanAccount(accountId, request.isUnbanIp(), request.isUnbanMac(), request.isUnbanHwid(), request.getIps(), request.getMacs());
        
        if (onlineChr != null) {
            onlineChr.setBanned(false);
        }
    }

    /**
     * 抓捕玩家服刑（支持在线/离线）
     */
    public void imprison(ImprisonReqDTO request) {
        int charId = request.getPlayerId();
        long addMs = TimeUnit.MINUTES.toMillis(request.getMinutes());
        Pair<Character, CharactersDO> pair = getOnlineOrOfflineCharacter(charId);
        Character onlineChr = pair.getLeft();
        CharactersDO chrDO = pair.getRight();

        if (onlineChr == null && chrDO == null) return;

        String charName = onlineChr != null ? onlineChr.getName() : chrDO.getName();

        if (onlineChr != null) {
            onlineChr.imprison(request.getMinutes());
        } else {
            long cur = chrDO.getJailexpire() != null ? chrDO.getJailexpire() : 0L;
            long newVal = (cur <= 0) ? addMs : cur + addMs;
            CharactersDO update = new CharactersDO();
            update.setId(charId);
            update.setJailexpire(newVal);
            charactersMapper.update(update);
        }

        MapMessage logData = new MapMessage()
                .with("charId", charId)
                .with("charName", charName)
                .with("minutes", request.getMinutes())
                .with("online", onlineChr != null);
        AuditLogger.info(LogModule.CHARACTER, LogAction.CHARACTER_JAIL, logData);
    }

    /**
     * 释放服刑玩家（支持在线/离线）
     */
    public void releaseJail(ChrIdDTO request) {
        int charId = request.getId();
        Pair<Character, CharactersDO> pair = getOnlineOrOfflineCharacter(charId);
        Character onlineChr = pair.getLeft();
        CharactersDO chrDO = pair.getRight();

        if (onlineChr == null && chrDO == null) return;

        String charName = onlineChr != null ? onlineChr.getName() : chrDO.getName();

        if (onlineChr != null) {
            onlineChr.releaseFromJail();  // 封装：清除时钟+服刑状态+送回入狱前地图+提示
        } else {
            CharactersDO update = new CharactersDO();
            update.setId(charId);
            update.setJailexpire(0L);
            charactersMapper.update(update);
        }

        MapMessage logData = new MapMessage()
                .with("charId", charId)
                .with("charName", charName)
                .with("online", onlineChr != null);
        AuditLogger.info(LogModule.CHARACTER, LogAction.CHARACTER_UNJAIL, logData);
    }

    /**
     * 根据ID查找角色（优先查找在线，其次查找数据库）
     *
     * @param charId 角色ID
     * @return Pair: Left为在线角色对象，Right为数据库实体对象。两者互斥或Right为Left的数据库映射（视具体实现而定，这里设计为互斥，若在线则Right为null，若离线则Left为null）
     */
    private Pair<Character, CharactersDO> getOnlineOrOfflineCharacter(int charId) {
        // 1. 尝试获取在线玩家
        for (World world : Server.getInstance().getWorlds()) {
            Character onlineChr = world.getPlayerStorage().getCharacterById(charId);
            if (onlineChr != null) {
                return new Pair<>(onlineChr, null);
            }
        }
        // 2. 获取离线玩家
        CharactersDO offlineChr = charactersMapper.selectOneById(charId);
        return new Pair<>(null, offlineChr);
    }
}
