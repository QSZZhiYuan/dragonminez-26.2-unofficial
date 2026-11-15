package com.dragonminez;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Runs pure regressions independently of the port's unfinished FML JUnit bootstrap. */
public final class UiRegressionRunner {
    public static void main(String[] args) throws java.io.IOException {
        net.neoforged.fml.loading.FMLPaths.loadAbsolutePaths(
                java.nio.file.Files.createTempDirectory("dmz-ui-regression-"));
        var request = LauncherDiscoveryRequestBuilder.request().selectors(
                DiscoverySelectors.selectClass("com.dragonminez.server.world.dimension.KaioPlanetProtectionTest"),
                DiscoverySelectors.selectClass("com.dragonminez.common.config.RaceConfigSyncTest"),
                DiscoverySelectors.selectClass("com.dragonminez.common.stats.StatsLimitsTest"),
                DiscoverySelectors.selectClass("com.dragonminez.common.util.TransformationRequirementsTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.gui.hud.HudReleaseIndicatorTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.render.EntityPreviewLayoutTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.render.layer.SkinLayerSubmissionTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.render.util.RenderPipelineInitializationTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.render.util.KiWaveGeometryTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.render.util.GeoAttachmentRendererTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.render.util.AuraPoseTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.render.effects.KiChargeVisualStateTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.render.hair.HairSubmissionTest"),
                DiscoverySelectors.selectClass("com.dragonminez.client.animation.CombatAnimationResolverTest")).build();
        var summary = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(summary);
        launcher.execute(request);
        var output = new java.io.PrintWriter(System.out, true);
        summary.getSummary().printTo(output);
        summary.getSummary().printFailuresTo(output);
        if (summary.getSummary().getTestsFailedCount() > 0 || summary.getSummary().getTestsFoundCount() == 0
                || summary.getSummary().getTestsSucceededCount() != summary.getSummary().getTestsFoundCount()) {
            throw new AssertionError("UI regression tests failed");
        }
    }
}
