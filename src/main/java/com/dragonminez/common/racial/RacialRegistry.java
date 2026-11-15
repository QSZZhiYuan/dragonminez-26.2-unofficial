package com.dragonminez.common.racial;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RacialRegistry {
	private static final Map<String, RacialAbility> ABILITIES = new HashMap<>();

	private RacialRegistry() {
	}

	public static void register(RacialAbility ability) {
		ABILITIES.put(ability.id(), ability);
	}

	public static Optional<RacialAbility> get(String id) {
		return Optional.ofNullable(id).map(ABILITIES::get);
	}

	public static Optional<RacialAbility> registeredForPlayer(StatsData data) {
        if (data == null || data.getCharacter() == null) return Optional.empty();
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(data.getCharacter().getRace());
		if (config == null) return Optional.empty();
		return get(config.getRacialSkill());
	}
    public static Optional<RacialAbility> forPlayer(StatsData data) {
        return registeredForPlayer(data).filter(ability -> isEnabled(ability.id()));
    }

    public static boolean isEnabled(String id) {
        var server = ConfigManager.getServerConfig();
        if (server == null) return false;
        var config = server.getRacialSkills();
        if (!Boolean.TRUE.equals(config.getEnableRacialSkills())) return false;
        return switch (id) {
            case "saiyan" -> Boolean.TRUE.equals(config.getSaiyan().getEnabled());
            case "human" -> Boolean.TRUE.equals(config.getHuman().getEnabled());
            case "namekian" -> Boolean.TRUE.equals(config.getNamekian().getEnabled());
            case "majin" -> Boolean.TRUE.equals(config.getMajin().getEnabled());
            case "frostdemon" -> Boolean.TRUE.equals(config.getFrostdemon().getEnabled());
            case "bioandroid" -> Boolean.TRUE.equals(config.getBioandroid().getEnabled());
            case "glind" -> Boolean.TRUE.equals(config.getGlind().getEnabled());
            default -> true;
        };
    }
}

