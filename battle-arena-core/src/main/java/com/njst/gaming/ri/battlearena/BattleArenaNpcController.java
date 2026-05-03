package com.njst.gaming.ri.battlearena;

import java.util.List;

public interface BattleArenaNpcController {
    BattleArenaPlayerInput decide(BattleArenaPlayerState self,
                                  List<BattleArenaPlayerState> players,
                                  int tick);
}
