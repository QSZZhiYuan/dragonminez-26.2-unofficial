package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.network.C2S.SetFlightSpeedLimitC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class FlightSpeedNode extends AbstractRadialNode {
	private int current(StatsData stats) { return stats.getResources().getFlightSpeedLimit(); }
	@Override public Component label(StatsData stats) { return Component.translatable("gui.action.dragonminez.flightspeed"); }
	@Override public Identifier icon(StatsData stats) { return null; }
	@Override public String faceText(StatsData stats) { return current(stats) + "%"; }
	@Override public boolean visible(StatsData stats) { return stats.getSkills().getSkillLevel("fly") > 0 && stats.getSkills().getSkillLevel("kicontrol") > 0; }
	@Override public boolean active(StatsData stats) { return current(stats) < 100; }
	@Override public int labelColor(StatsData stats) { return active(stats) ? GREEN : 0xFFFFFF; }

	@Override
	public void onSelect(StatsData stats) {
		NetworkHandler.sendToServer(new SetFlightSpeedLimitC2S(100));
		playToggle(false);
	}

	public List<RadialNode> buildOptions() {
		List<RadialNode> options = new ArrayList<>();
		for (int value = 100; value >= 5; value -= 5) options.add(new FlightSpeedLimitOptionNode(value));
		return options;
	}

	public void adjustByWheel(StatsData stats, double delta) {
		if (delta == 0) return;
		int next = Math.max(5, Math.min(100, current(stats) + (delta > 0 ? 5 : -5)));
		if (next != current(stats)) NetworkHandler.sendToServer(new SetFlightSpeedLimitC2S(next));
	}
}
