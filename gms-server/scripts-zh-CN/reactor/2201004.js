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
function act() {
    let MapObj = rm.getMap();
    let BossMapId = MapObj.getId();
    let exitMapId = BossMapId - 1;
    rm.summonBoss(8500000, -410, -400,"Bgm09/TimeAttack","由于<时空裂痕的碎片D>填补了时空的裂痕，帕普拉图斯出现了！");
    rm.getMap(exitMapId).setReactorState(); //通知本源入口关闭
}