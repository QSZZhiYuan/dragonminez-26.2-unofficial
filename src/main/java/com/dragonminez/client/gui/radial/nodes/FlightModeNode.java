package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.FlightModeC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** X-menu action for switching between exploration and combat flight. */
public final class FlightModeNode extends AbstractRadialNode {
	@Override
	public Component label(StatsData stats) {
		String modeKey = stats.getStatus().getFlightMode() == Status.FLIGHT_COMBAT
				? "gui.dragonminez.radial.flight_mode.combat"
				: "gui.dragonminez.radial.flight_mode.search";
		return Component.translatable("gui.dragonminez.radial.flight_mode", Component.translatable(modeKey));
	}

	@Override
	public Identifier icon(StatsData stats) {
		return icon("fly");
	}

	@Override
	public boolean visible(StatsData stats) {
		return stats.getSkills().getSkillLevel("fly") > 0
				&& stats.getSkills().getSkillLevel("kicontrol") > 0;
	}

	@Override
	public boolean active(StatsData stats) {
		return stats.getStatus().getFlightMode() == Status.FLIGHT_COMBAT;
	}

	@Override
	public void onSelect(StatsData stats) {
		NetworkHandler.sendToServer(new FlightModeC2S());
		playClick();
	}
}
