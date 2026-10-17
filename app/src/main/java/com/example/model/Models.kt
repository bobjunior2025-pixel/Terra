package com.example.model

data class ResourceYield(
    val food: Int = 0,
    val wood: Int = 0,
    val stone: Int = 0,
    val gold: Int = 0,
    val science: Int = 0
)

enum class TerrainType(
    val displayName: String,
    val baseYield: ResourceYield,
    val movementCost: Int,
    val defenseBonus: Float,
    val isPassable: Boolean = true
) {
    PLAINS("Campos Férteis", ResourceYield(food = 2, gold = 1), 1, 1.0f),
    FARMLAND_WHEAT("Plantação de Trigo", ResourceYield(food = 4, gold = 1), 1, 1.0f),
    FARMLAND_CORN("Milharal Dourado", ResourceYield(food = 4, gold = 1), 1, 1.0f),
    ORCHARD_APPLE("Pomar de Maçãs", ResourceYield(food = 5, gold = 2), 1, 1.05f),
    FOREST_PINE("Bosque de Pinheiros", ResourceYield(wood = 3, food = 1), 2, 1.25f),
    FOREST_OAK("Floresta de Carvalhos", ResourceYield(wood = 3, food = 2), 2, 1.20f),
    PASTURE_HORSES("Pastagem Selvagem", ResourceYield(food = 2, gold = 3), 1, 1.10f),
    MOUNTAIN("Cordilheira Rochosa", ResourceYield(stone = 3, gold = 1), 3, 1.40f, isPassable = false),
    MOUNTAIN_SNOW("Picos Nevados", ResourceYield(stone = 2), 3, 1.30f, isPassable = false),
    MOUNTAIN_GOLD("Mina de Ouro & Gemas", ResourceYield(stone = 2, gold = 5), 3, 1.35f, isPassable = false),
    WATER_RIVER("Rio Navegável", ResourceYield(food = 2, gold = 2), 2, 0.90f),
    WATER_OCEAN("Águas Costeiras", ResourceYield(food = 3, gold = 2), 2, 0.85f),
    SPICE_GROVE("Especiarias Raras", ResourceYield(gold = 4, science = 2), 1, 1.0f)
}

enum class FactionId(val defaultName: String, val colorHex: Long) {
    PLAYER("Império de Eldoria", 0xFF8B5CF6), // Royal Purple / Violet (like the screenshot borders)
    CRIMSON("Legião Carmesim", 0xFFEF4444),  // Crimson Red
    VERDANT("Aliança Esmeralda", 0xFF10B981), // Emerald Green
    AZURE("Dinastia Celeste", 0xFF3B82F6),   // Azure Blue
    BARBARIAN("Bandos Selvagens", 0xFF64748B) // Neutral Gray
}

data class Faction(
    val id: FactionId,
    val name: String,
    val colorHex: Long,
    val isHuman: Boolean,
    val food: Int = 100,
    val wood: Int = 80,
    val stone: Int = 60,
    val gold: Int = 120,
    val science: Int = 0,
    val researchedTechs: Set<String> = emptySet(),
    val currentResearchId: String? = "AGRICULTURE",
    val researchProgress: Int = 0,
    val isDefeated: Boolean = false
) {
    fun canAfford(foodCost: Int = 0, woodCost: Int = 0, stoneCost: Int = 0, goldCost: Int = 0): Boolean {
        return food >= foodCost && wood >= woodCost && stone >= stoneCost && gold >= goldCost
    }

    fun deduct(foodCost: Int = 0, woodCost: Int = 0, stoneCost: Int = 0, goldCost: Int = 0): Faction {
        return copy(
            food = food - foodCost,
            wood = wood - woodCost,
            stone = stone - stoneCost,
            gold = gold - goldCost
        )
    }

    fun addResources(gain: ResourceYield): Faction {
        return copy(
            food = food + gain.food,
            wood = wood + gain.wood,
            stone = stone + gain.stone,
            gold = gold + gain.gold,
            science = science + gain.science
        )
    }
}

enum class UnitType(
    val displayName: String,
    val description: String,
    val foodCost: Int,
    val woodCost: Int,
    val stoneCost: Int,
    val goldCost: Int,
    val maxTroops: Int,
    val attackPower: Int,
    val defensePower: Int,
    val maxMovement: Int,
    val sightRange: Int,
    val attackRange: Int,
    val requiredTechId: String? = null
) {
    SCOUT(
        "Batedor Rápido",
        "Ágil e com grande raio de visão. Perfeito para explorar e coletar relíquias.",
        foodCost = 25, woodCost = 0, stoneCost = 0, goldCost = 15,
        maxTroops = 15, attackPower = 12, defensePower = 10,
        maxMovement = 3, sightRange = 3, attackRange = 1,
        requiredTechId = null
    ),
    WARRIOR(
        "Infantaria Espadachim",
        "Tropa básica resistente e leal. Excelente defesa em florestas e colinas.",
        foodCost = 35, woodCost = 20, stoneCost = 0, goldCost = 20,
        maxTroops = 25, attackPower = 24, defensePower = 22,
        maxMovement = 2, sightRange = 2, attackRange = 1,
        requiredTechId = null
    ),
    ARCHER(
        "Pelotão de Arqueiros",
        "Ataque à distância (2 casas). Ataca sem sofrer contra-ataque corpo a corpo.",
        foodCost = 30, woodCost = 30, stoneCost = 0, goldCost = 25,
        maxTroops = 20, attackPower = 26, defensePower = 12,
        maxMovement = 2, sightRange = 2, attackRange = 2,
        requiredTechId = "ARCHERY"
    ),
    KNIGHT(
        "Cavalaria Real",
        "Unidade veloz e demolidora. Causa dano crítico de investida em campo aberto.",
        foodCost = 50, woodCost = 15, stoneCost = 25, goldCost = 45,
        maxTroops = 20, attackPower = 36, defensePower = 28,
        maxMovement = 3, sightRange = 2, attackRange = 1,
        requiredTechId = "CAVALRY"
    ),
    CATAPULT(
        "Catapulta de Cerco",
        "Artilharia pesada de longo alcance. Bônus massivo contra cidades fortificadas.",
        foodCost = 30, woodCost = 50, stoneCost = 40, goldCost = 50,
        maxTroops = 10, attackPower = 48, defensePower = 10,
        maxMovement = 1, sightRange = 2, attackRange = 2,
        requiredTechId = "SIEGECRAFT"
    ),
    SETTLER(
        "Comitiva de Colonos",
        "Funda um novo assentamento para expandir as fronteiras e reivindicar recursos.",
        foodCost = 60, woodCost = 40, stoneCost = 10, goldCost = 35,
        maxTroops = 10, attackPower = 6, defensePower = 10,
        maxMovement = 2, sightRange = 2, attackRange = 1,
        requiredTechId = null
    )
}

data class GameUnit(
    val id: String,
    val type: UnitType,
    val factionId: FactionId,
    val troops: Int = type.maxTroops,
    val maxTroops: Int = type.maxTroops,
    val movementRemaining: Int = type.maxMovement,
    val hasAttacked: Boolean = false,
    val isFortified: Boolean = false
) {
    val isAlive: Boolean get() = troops > 0
    val strengthRatio: Float get() = (troops.toFloat() / maxTroops.toFloat()).coerceIn(0.1f, 1.0f)
    val effectiveAttack: Int get() = (type.attackPower * strengthRatio * (if (isFortified) 1.1f else 1.0f)).toInt().coerceAtLeast(1)
    val effectiveDefense: Int get() = (type.defensePower * strengthRatio * (if (isFortified) 1.35f else 1.0f)).toInt().coerceAtLeast(1)

    fun resetTurn(): GameUnit {
        return copy(
            movementRemaining = type.maxMovement,
            hasAttacked = false
        )
    }

    fun heal(amount: Int): GameUnit {
        return copy(troops = (troops + amount).coerceAtMost(maxTroops))
    }
}

enum class BuildingType(
    val displayName: String,
    val description: String,
    val foodCost: Int,
    val woodCost: Int,
    val stoneCost: Int,
    val goldCost: Int,
    val yieldBonus: ResourceYield,
    val isCity: Boolean = false,
    val requiredTechId: String? = null
) {
    CAPITAL_CASTLE(
        "Castelo Imperial",
        "Coração da civilização. Muralhas imponentes, recruta todas as unidades e governa a região.",
        foodCost = 0, woodCost = 0, stoneCost = 0, goldCost = 0,
        yieldBonus = ResourceYield(food = 4, wood = 3, stone = 3, gold = 5, science = 3),
        isCity = true
    ),
    TOWN(
        "Vila / Assentamento",
        "Centro regional que projeta fronteiras e coleta tributos dos arredores.",
        foodCost = 60, woodCost = 40, stoneCost = 20, goldCost = 30,
        yieldBonus = ResourceYield(food = 2, wood = 2, stone = 1, gold = 3, science = 1),
        isCity = true
    ),
    FARMSTEAD(
        "Celeiro Agrícola",
        "Aumenta a safra e estoca grãos para alimentar o crescimento da população.",
        foodCost = 10, woodCost = 25, stoneCost = 10, goldCost = 15,
        yieldBonus = ResourceYield(food = 4, gold = 1),
        requiredTechId = "AGRICULTURE"
    ),
    LUMBER_MILL(
        "Serraria Imperial",
        "Moinho de corte florestal para acelerar a produção de madeira para frotas e arcos.",
        foodCost = 15, woodCost = 10, stoneCost = 15, goldCost = 20,
        yieldBonus = ResourceYield(wood = 4),
        requiredTechId = "FORESTRY"
    ),
    QUARRY(
        "Pedreira & Forja",
        "Extração contínua de rochas, metais nobres e pedras de cantaria.",
        foodCost = 15, woodCost = 20, stoneCost = 10, goldCost = 25,
        yieldBonus = ResourceYield(stone = 4, gold = 2),
        requiredTechId = "MASONRY"
    ),
    WATCHTOWER(
        "Torre de Vigia",
        "Fortificação alta que concede vigilância distante e bônus de defesa territorial.",
        foodCost = 10, woodCost = 30, stoneCost = 25, goldCost = 20,
        yieldBonus = ResourceYield(gold = 1),
        requiredTechId = "MASONRY"
    ),
    MARKET(
        "Feira de Comércio",
        "Ponto de encontro de caravanas, mercadores de especiarias e ourives.",
        foodCost = 20, woodCost = 30, stoneCost = 10, goldCost = 30,
        yieldBonus = ResourceYield(gold = 6),
        requiredTechId = "COMMERCE"
    ),
    ACADEMY(
        "Academia de Sábios",
        "Arquivo de pergaminhos e astrônomos gerando avanço científico veloz.",
        foodCost = 20, woodCost = 30, stoneCost = 30, goldCost = 40,
        yieldBonus = ResourceYield(science = 5),
        requiredTechId = "SCHOLARSHIP"
    )
}

data class Building(
    val type: BuildingType,
    val factionId: FactionId,
    val level: Int = 1
)

enum class Season(
    val displayName: String,
    val icon: String,
    val themeColorHex: Long,
    val description: String,
    val foodMultiplier: Float,
    val goldMultiplier: Float,
    val winterCostPenalty: Boolean = false
) {
    SPRING("Primavera", "🌸", 0xFFF472B6, "Estação de florescer: +30% na colheita e crescimento de tropas.", 1.30f, 1.0f),
    SUMMER("Verão", "☀️", 0xFFFBBF24, "Dias longos e quentes: +25% de ouro em mercados e expedições.", 1.10f, 1.25f),
    AUTUMN("Outono", "🍂", 0xFFFB923C, "Festa da colheita abundante: +40% na extração de madeira e grãos.", 1.40f, 1.10f),
    WINTER("Inverno", "❄️", 0xFF38BDF8, "Frio congelante: suprimento militar custa +25% e rios congelam.", 0.85f, 0.90f, winterCostPenalty = true);

    fun next(): Season = when (this) {
        SPRING -> SUMMER
        SUMMER -> AUTUMN
        AUTUMN -> WINTER
        WINTER -> SPRING
    }
}

data class Technology(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val scienceCost: Int,
    val prerequisiteId: String? = null
)

object TechRegistry {
    val ALL_TECHS = listOf(
        Technology("AGRICULTURE", "Agricultura", "🌾", "Desbloqueia Celeiros Agrícolas e aumenta rendimento de plantações.", 25),
        Technology("SCOUTING", "Exploração", "🧭", "Desbloqueia Batedores e concede visão estendida.", 25),
        Technology("FORESTRY", "Silvicultura", "🌲", "Desbloqueia Serrarias e acelera coleta de madeira.", 45, "AGRICULTURE"),
        Technology("ARCHERY", "Arquearia", "🏹", "Desbloqueia Pelotões de Arqueiros para combate à distância.", 50, "SCOUTING"),
        Technology("MASONRY", "Alvenaria", "🧱", "Desbloqueia Pedreiras e Torres de Vigia fortificadas.", 50, "AGRICULTURE"),
        Technology("CAVALRY", "Hipologia / Cavalaria", "🐎", "Desbloqueia a temida Cavalaria Real de investida.", 80, "ARCHERY"),
        Technology("COMMERCE", "Rotas Comerciais", "🪙", "Desbloqueia Feiras de Comércio e caravanas lucrativas.", 75, "MASONRY"),
        Technology("SCHOLARSHIP", "Erudição & Ciência", "📜", "Desbloqueia Academias de Sábios (+5 Ciência/turno).", 100, "COMMERCE"),
        Technology("SIEGECRAFT", "Engenharia de Cerco", "⚔️", "Desbloqueia Catapultas devastadoras contra cidades.", 120, "MASONRY")
    )

    fun getTech(id: String): Technology? = ALL_TECHS.find { it.id == id }
}

data class GameTile(
    val x: Int,
    val y: Int,
    val terrain: TerrainType,
    val ownerFaction: FactionId? = null,
    val building: Building? = null,
    val unit: GameUnit? = null,
    val isExplored: Boolean = false,
    val hasAncientRuins: Boolean = false
) {
    val hasUnit: Boolean get() = unit != null
    val hasCity: Boolean get() = building?.type?.isCity == true
}

data class BattleEvent(
    val turn: Int,
    val message: String,
    val isPlayerInvolved: Boolean = true,
    val isVictory: Boolean = true
)
