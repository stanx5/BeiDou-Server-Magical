function enter(pi) {
    player = pi.getPlayer();
    level = player.getLevel();
    if (level < 70) {
        player.updateHp(player.getHp() * 0.1);
        player.dropMessage(5,"被一股未知的力量反弹回来，并且受到了严重的内伤，你感觉自己很弱鸡。");
        return false;
    } else if (player.getQuestStatus(4526) === 0) {
        player.dropMessage(5,"一股未知的力量阻止你进入。");
        return false;
    }
    pi.playPortalSound();
    pi.warp(541020000, 0);
    return true;
}