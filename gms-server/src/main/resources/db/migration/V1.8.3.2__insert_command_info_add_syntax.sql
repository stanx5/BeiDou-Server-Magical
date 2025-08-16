-- 插入第一条数据（syntax = 'dropmeso'）
INSERT INTO command_info (syntax, level, enabled, clazz, default_level)
SELECT 'dropmeso', 0, 1, 'DropMesoCommand', 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM command_info WHERE syntax = 'dropmeso'
);

-- 插入第二条数据（syntax = '丢金币'）
INSERT INTO command_info (syntax, level, enabled, clazz, default_level)
SELECT '丢金币', 0, 1, 'DropMesoCommand', 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM command_info WHERE syntax = '丢金币'
);

-- 丢金币次数上限
INSERT INTO `game_config`(`config_type`, `config_sub_type`, `config_clazz`, `config_code`, `config_value`, `config_desc`, `update_time`)
SELECT 'server', 'Game Mechanics', 'java.lang.Integer', 'command_dropmeso_maxcount', '200', 'command_dropmeso_maxcount', '2025-04-02 16:56:57'
    WHERE NOT EXISTS (
    SELECT 1 FROM `game_config` WHERE `config_code` = 'command_dropmeso_maxcount'
);

-- 中文内容
INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`, `lang_extend`)
SELECT 'zh-CN', 'game_config', 'command_dropmeso_maxcount', '使用指令 @丢金币 或 @dropmeso 最多允许丢出多少次，上限太高会造成卡顿，设0关闭该指令使用', NULL
    WHERE NOT EXISTS (
    SELECT 1 FROM `lang_resources` WHERE `lang_type` = 'zh-CN' AND `lang_code` = 'command_dropmeso_maxcount'
);

-- 英文内容
INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`, `lang_extend`)
SELECT 'en-US', 'game_config', 'command_dropmeso_maxcount', 'What is the maximum number of times the @dropmeso command can be executed? A too high upper limit may cause lag', NULL
    WHERE NOT EXISTS (
    SELECT 1 FROM `lang_resources` WHERE `lang_type` = 'en-US' AND `lang_code` = 'command_dropmeso_maxcount'
);

-- 丢金币指令使用间隔
INSERT INTO `game_config`(`config_type`, `config_sub_type`, `config_clazz`, `config_code`, `config_value`, `config_desc`, `update_time`)
SELECT 'server', 'Game Mechanics', 'java.lang.Integer', 'command_dropmeso_useinterval', '5', 'command_dropmeso_useinterval', '2025-04-02 16:56:57'
    WHERE NOT EXISTS (
    SELECT 1 FROM `game_config` WHERE `config_code` = 'command_dropmeso_useinterval'
);

-- 中文内容
INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`, `lang_extend`)
SELECT 'zh-CN', 'game_config', 'command_dropmeso_useinterval', '指令 @丢金币 或 @dropmeso 的使用间隔，单位（秒），设0为不限制', NULL
    WHERE NOT EXISTS (
    SELECT 1 FROM `lang_resources` WHERE `lang_type` = 'zh-CN' AND `lang_code` = 'command_dropmeso_useinterval'
);

-- 英文内容
INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`, `lang_extend`)
SELECT 'en-US', 'game_config', 'command_dropmeso_useinterval', 'The usage interval of instruction @dropmeso, in seconds', NULL
    WHERE NOT EXISTS (
    SELECT 1 FROM `lang_resources` WHERE `lang_type` = 'en-US' AND `lang_code` = 'command_dropmeso_useinterval'
);