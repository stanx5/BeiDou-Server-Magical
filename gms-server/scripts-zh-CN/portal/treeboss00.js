/* 克雷塞尔 */

var EventLevel = 1;

function enter(pi) {
    if (!pi.haveItem(4031942, 1) && !pi.haveItem(4000385, 1)) {
        pi.playerMessage(5, "你还不能进入这个地方。");
        //pi.playerMessage(5, "你必须拥有心灯和扳手才能进入这个地方。");
        return false;
    } else {
        var em = pi.getEventManager("Ulu_KrexelPQ");

        if (pi.getParty() == null) {
            pi.playerMessage(5, "只有队长才能申请进入。");
            return false;
        } else if (!pi.isLeader()) {
            pi.playerMessage(5, "你的队长必须进入后才能开始战斗。");
            return false;
        } else {
            var eli = em.getEligibleParty(pi.getParty());
            if (eli.size() > 0) {
                if (!em.startInstance(pi.getParty(), pi.getPlayer().getMap(), EventLevel)) {
                    pi.playerMessage(5, "里面的战斗已经开始了，所以你还不能进入这个地方。");
                    return false;
                }
            } else { //this should never appear
                pi.playerMessage(5, "你还不能开始这场战斗，请召集队员后再来尝试。" + eli.size());
                return false;
            }
            pi.playPortalSound();
            return true;
        }
    }
}
