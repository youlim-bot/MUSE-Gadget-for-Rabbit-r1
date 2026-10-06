package dev.cameronpak.muser1

/** Private, deterministic life simulation. Progress is deliberately not a visible checklist. */
data class PetState(
    val food: Double = 80.0, val mood: Double = 80.0,
    val energy: Double = 80.0, val clean: Double = 80.0,
    val xp: Int = 0, val bond: Int = 0, // Legacy values retained for migration, never growth gates.
    val sleeping: Boolean = false, val updatedAt: Long,
    val rewardAt: Long = 0L, val hatched: Boolean = false,
    val incubationMs: Long = 0L, val eggCare: Int = 0, val eggCareAt: Long = 0L,
    val seed: Long = 0L, val ageMs: Long = 0L,
    val health: Double = 100.0, val starvingMs: Long = 0L,
    val lonelyMs: Long = 0L, val warmth: Double = 50.0,
    val curiosity: Double = 0.0, val vigor: Double = 0.0,
    val dietMask: Int = 0, val sweets: Int = 0,
    val meals: Int = 0, val fedAt: Long = 0L, val playedAt: Long = 0L,
    val washedAt: Long = 0L, val medicineAt: Long = 0L,
    val sleepMs: Long = 0L, val form: PetForm = PetForm.UNDECIDED,
    val dead: Boolean = false, val diedAt: Long = 0L
) {
    val hatchDuration: Long get() = 48 * HOUR + Math.floorMod(seed, 24 * HOUR)
    val hatchReady: Boolean get() = !hatched && incubationMs >= hatchDuration
    val level: Int get() = when {
        !hatched -> 0
        ageMs < DAY -> 1
        ageMs < 4 * DAY -> 2
        ageMs < 9 * DAY -> 3
        ageMs < 21 * DAY -> 4
        else -> 5
    }
    val temperament: PetTemperament get() = when {
        warmth < 23 -> PetTemperament.GRUMPY
        warmth < 40 -> PetTemperament.SHY
        warmth > 73 -> PetTemperament.AFFECTIONATE
        curiosity > vigor + 10 -> PetTemperament.CURIOUS
        vigor > curiosity + 10 -> PetTemperament.PLAYFUL
        else -> PetTemperament.CALM
    }
    val ill: Boolean get() = hatched && !dead && health < 45
    // A ready egg meets its owner before newborn needs begin; no unseen newborn death.
    fun hatch(now: Long): PetState = if(!hatchReady) this else copy(hatched=true,
        updatedAt=maxOf(now,updatedAt),sleeping=false,ageMs=0,food=80.0,mood=80.0,
        health=100.0,energy=80.0,clean=80.0,starvingMs=0,lonelyMs=0)

    /** Fixed five-minute steps preserve fractional time across saves and render ticks. */
    fun advance(now: Long): PetState {
        if(now <= updatedAt || dead) return this
        if(!hatched) return copy(incubationMs=(incubationMs+(now-updatedAt)).coerceAtMost(hatchDuration),updatedAt=now)
        var s=this
        while(now-s.updatedAt >= STEP && !s.dead) {
            val h=STEP.toDouble()/HOUR
            val nextFood=(s.food-h*if(s.sleeping)2.0 else 3.0).coerceAtLeast(0.0)
            val starvation=if(nextFood<=0) s.starvingMs+STEP else 0L
            val loneliness=s.lonelyMs+STEP
            val nextClean=(s.clean-h*1.1).coerceAtLeast(0.0)
            val harm=(if(starvation>=6*HOUR)3.0 else 0.0)+(if(nextClean<10)0.65 else 0.0)
            val heal=if(nextFood>35 && nextClean>40 && starvation==0L)0.65 else 0.0
            val nextHealth=(s.health+h*(heal-harm)).coerceIn(0.0,100.0)
            val age=s.ageMs+STEP
            val sleepingFor=if(s.sleeping)s.sleepMs+STEP else 0L
            s=s.copy(food=nextFood,clean=nextClean,health=nextHealth,
                mood=(s.mood-h*if(s.sleeping)0.55 else 1.6).coerceAtLeast(0.0),
                energy=(s.energy+h*if(s.sleeping)12 else -2).coerceIn(0.0,100.0),
                warmth=(s.warmth-if(loneliness>12*HOUR)h*.75 else 0.0).coerceAtLeast(0.0),
                lonelyMs=loneliness,starvingMs=starvation,ageMs=age,updatedAt=s.updatedAt+STEP,
                sleepMs=sleepingFor,sleeping=s.sleeping && sleepingFor<8*HOUR,
                dead=nextHealth<=0,diedAt=if(nextHealth<=0)s.updatedAt+STEP else 0)
            if(s.level>=4 && s.form==PetForm.UNDECIDED && !s.dead) s=s.copy(form=s.chooseForm())
        }
        return s
    }
    private fun chooseForm(): PetForm = when {
        warmth<30 -> PetForm.WILD
        warmth>70 && Integer.bitCount(dietMask)>=3 -> PetForm.COMPANION
        curiosity>vigor+8 -> PetForm.EXPLORER
        vigor>curiosity+8 -> PetForm.SPRINTER
        else -> listOf(PetForm.DREAMER,PetForm.EXPLORER,PetForm.COMPANION)[Math.floorMod(seed,3L).toInt()]
    }
    private fun nurture(now:Long):PetState {
        if(eggCareAt!=0L && now-eggCareAt<15*60_000L)return this
        return copy(eggCare=(eggCare+1).coerceAtMost(1000),eggCareAt=now,
            warmth=(warmth+1).coerceAtMost(85.0),bond=(bond+1).coerceAtMost(100))
    }
    fun agentInteraction(now: Long): PetState {
        val s=advance(now)
        if(s.dead)return s
        if(!s.hatched)return s.nurture(now)
        if(s.rewardAt!=0L && now-s.rewardAt<30*60_000L)return s
        return s.copy(mood=(s.mood+7).coerceAtMost(100.0),warmth=(s.warmth+4).coerceAtMost(100.0),
            curiosity=s.curiosity+1,lonelyMs=0,rewardAt=now,bond=(s.bond+1).coerceAtMost(100))
    }
    fun feed(meal:PetFood,now:Long):PetState {
        val s=advance(now)
        if(!s.hatched || s.dead || s.sleeping || s.food>85 || (s.fedAt!=0L && now-s.fedAt<30*60_000L))return s
        val sweet=meal==PetFood.CAKE
        return s.copy(food=(s.food+meal.fill).coerceAtMost(100.0),mood=(s.mood+if(sweet)12 else 3).coerceAtMost(100.0),
            health=(s.health+if(sweet)-5.0 else meal.health).coerceIn(1.0,100.0),
            dietMask=s.dietMask or (1 shl meal.ordinal),meals=s.meals+1,sweets=s.sweets+if(sweet)1 else 0,
            vigor=s.vigor+if(meal==PetFood.FISH || meal==PetFood.VEGETABLES)1 else 0,
            warmth=(s.warmth+1).coerceAtMost(100.0),fedAt=now)
    }
    fun play(game:PetGame,won:Boolean,now:Long):PetState {
        val s=advance(now)
        if(!s.hatched || s.dead || s.sleeping || s.energy<10 || (s.playedAt!=0L && now-s.playedAt<30*60_000L))return s
        val bonus=if(won)3.0 else 1.0
        return s.copy(mood=(s.mood+if(won)20 else 8).coerceAtMost(100.0),energy=(s.energy-6).coerceAtLeast(0.0),
            warmth=(s.warmth+if(won)8 else 4).coerceAtMost(100.0),lonelyMs=0,playedAt=now,
            curiosity=s.curiosity+if(game==PetGame.MEMORY)bonus else 0.0,
            vigor=s.vigor+if(game!=PetGame.MEMORY)bonus else 0.0)
    }
    fun medicine(now:Long):PetState {
        val s=advance(now)
        if(!s.ill || s.sleeping || (s.medicineAt!=0L && now-s.medicineAt<6*HOUR))return s
        return s.copy(health=(s.health+20).coerceAtMost(100.0),medicineAt=now)
    }
    fun care(action: PetAction, now: Long): PetState {
        val s=advance(now)
        if(s.dead)return s
        if(!s.hatched)return s.nurture(now)
        if(action==PetAction.SLEEP)return s.copy(sleeping=!s.sleeping,sleepMs=0)
        if(s.sleeping)return s
        return when(action) {
            PetAction.FEED -> s.feed(PetFood.PORRIDGE,now)
            PetAction.PLAY -> s.play(PetGame.STARS,true,now)
            PetAction.CLEAN -> if(s.washedAt!=0L && now-s.washedAt<30*60_000L)s else s.copy(clean=100.0,washedAt=now)
            else -> s
        }
    }
    companion object {
        const val HOUR=3_600_000L
        const val DAY=24*HOUR
        const val STEP=300_000L
    }
}
enum class PetAction { FEED, PLAY, CLEAN, SLEEP }
enum class PetFood(val fill:Double,val health:Double) {
    MILK(18.0,2.0), PORRIDGE(30.0,3.0), BERRIES(15.0,2.0),
    FISH(32.0,4.0), VEGETABLES(24.0,5.0), CAKE(12.0,-5.0)
}
enum class PetGame { STARS, MEMORY, RHYTHM }
enum class PetForm { UNDECIDED, COMPANION, EXPLORER, SPRINTER, DREAMER, WILD }
enum class PetTemperament { CALM, AFFECTIONATE, CURIOUS, PLAYFUL, SHY, GRUMPY }
