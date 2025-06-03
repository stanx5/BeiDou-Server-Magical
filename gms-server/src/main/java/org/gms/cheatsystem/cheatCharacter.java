package org.gms.cheatsystem;

import lombok.Getter;
import org.gms.client.Character;


/**
 * 内置作弊类
 */

public class cheatCharacter extends basic {

    private Character player;
    @Getter
    private final itemVac itemvac;
    public cheatCharacter(Character player) {
        this.player = player;
        itemvac = new itemVac(player);
    }
}
