-- 将扩展表的extend_value字段长度改为TEXT，使其可以储存更长的JSON结构数据。
ALTER TABLE `extend_value` MODIFY `extend_value` TEXT;

-- 游戏配置表
INSERT INTO `game_config`(`config_type`, `config_sub_type`, `config_clazz`, `config_code`, `config_value`, `config_desc`)
VALUES
    ('server', 'CashShop', 'java.lang.Boolean', 'use_pet_equip_permanent', 'true', 'use_pet_equip_permanent');

-- 多语言资源表
INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`)
VALUES
    ('zh-CN', 'game_config', 'use_pet_equip_permanent', '商城是否允许将可升级次数>0的宠物装备时效设为永久。'),
    ('en-US', 'game_config', 'use_pet_equip_permanent', 'Does the mall allow the expiration date of pet equipment that can be upgraded>0 times to be set permanently.');

-- 1. 修改game_config表中指定参数的config_sub_type为CashShop
UPDATE `game_config`
SET `config_sub_type` = 'CashShop'
WHERE `config_code` IN (
                        'use_supply_rate_coupons',
                        'allow_cash_shop_name_change',
                        'allow_cash_shop_world_transfer',
                        'name_change_cooldown',
                        'world_transfer_cooldown'
    );

-- 2. 修改多语言资源表中的语言内容
-- 更新中文内容
UPDATE `lang_resources`
SET `lang_value` = '商城是否允许出售双倍卡、三倍卡等倍率卡'
WHERE `lang_type` = 'zh-CN' AND `lang_base` = 'game_config' AND `lang_code` = 'use_supply_rate_coupons';

UPDATE `lang_resources`
SET `lang_value` = '解析wz时支持用逗号加点分隔的数字，如12,345.67能解析成12345.67；如遇到飞镖无法充值的情况，将该参数设为false关闭即可。'
WHERE `lang_type` = 'zh-CN' AND `lang_base` = 'game_config' AND `lang_code` = 'use_unit_price_with_comma';

UPDATE `lang_resources`
SET `lang_value` = '玩家达到转职等级而不转职，最多只能获取到99%的经验值。'
WHERE `lang_type` = 'zh-CN' AND `lang_base` = 'game_config' AND `lang_code` = 'use_enforce_job_level_range';

UPDATE `lang_resources`
SET `lang_value` = '何时应用改名卡，true：在玩家角色重连后进行，false：等待服务器重启时以应用改名'
WHERE `lang_type` = 'zh-CN' AND `lang_base` = 'game_config' AND `lang_code` = 'instant_name_change';

-- 更新英文内容（根据中文内容翻译）
UPDATE `lang_resources`
SET `lang_value` = 'Whether the mall allows the sale of multiplier cards such as double cards, triple cards, etc.'
WHERE `lang_type` = 'en-US' AND `lang_base` = 'game_config' AND `lang_code` = 'use_supply_rate_coupons';

UPDATE `lang_resources`
SET `lang_value` = 'When parsing wz, support numbers separated by commas and dots, such as 12,345.67 can be parsed into 12345.67; if darts cannot be recharged, set this parameter to false to turn it off.'
WHERE `lang_type` = 'en-US' AND `lang_base` = 'game_config' AND `lang_code` = 'use_unit_price_with_comma';

UPDATE `lang_resources`
SET `lang_value` = 'If a player reaches the job change level but does not change jobs, they can only obtain up to 99% of the experience value.'
WHERE `lang_type` = 'en-US' AND `lang_base` = 'game_config' AND `lang_code` = 'use_enforce_job_level_range';

UPDATE `lang_resources`
SET `lang_value` = 'When to apply the name change card, true: applied after the player character reconnects, false: wait for server restart to apply the name change.'
WHERE `lang_type` = 'en-US' AND `lang_base` = 'game_config' AND `lang_code` = 'instant_name_change';