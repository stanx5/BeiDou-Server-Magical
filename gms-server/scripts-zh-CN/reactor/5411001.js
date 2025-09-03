function act(){
    // rm.changeMusic("Bgm09/TimeAttack");
    // rm.mapMessage(5, "如你所愿，克雷塞尔出现了.");
    rm.spawnMonster(9420520);
    rm.summonBossDelayed(9420521,4000,rm.getPosition().getX(),rm.getPosition().getY(),"Bgm09/TimeAttack","如你所愿，克雷塞尔出现了.");
}