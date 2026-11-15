package com.dragonminez.client.gui.quest;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Story-progression toast.
 *
 * <h2>26.2 migration</h2>
 * The {@link Toast} interface changed shape completely:
 * <ul>
 *   <li>1.21: {@code Visibility render(GuiGraphics, ToastComponent, long)} — the toast drew itself and
 *       returned the visibility in one call.</li>
 *   <li>26.2: the toast is a plain state object. It reports
 *       {@link #getWantedVisibility()}, advances its own timer in
 *       {@link #update(ToastManager, long)}, and draws in
 *       {@link #extractRenderState(GuiGraphicsExtractor, Font, long)} — where the {@link Font} and the
 *       elapsed-visible time are handed in, so the {@code ToastComponent} parameter is gone.</li>
 * </ul>
 * The timer therefore moved from a local read of {@code delta} into a field that
 * {@link #update} advances each frame.
 */
public class StoryToast implements Toast {

	public enum Tone {
		INFO,
		PROGRESS,
		FAILURE,
		SUCCESS
	}

	private static final long DURATION_MS = 5000L;

	private final Component title;
	private final Component description;
	private final Tone tone;

	/** 26.2 keeps the timing state on the toast; the manager only tells us how long we were visible. */
	private Toast.Visibility wantedVisibility = Toast.Visibility.SHOW;

	public StoryToast(Component title, Component description, Tone tone) {
		this.title = title;
		this.description = description;
		this.tone = tone;
	}

	@Override
	public Toast.Visibility getWantedVisibility() {
		return this.wantedVisibility;
	}

	@Override
	public void update(ToastManager manager, long fullyVisibleForMs) {
		this.wantedVisibility = fullyVisibleForMs >= DURATION_MS ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, Font font, long fullyVisibleForMs) {
		int backgroundColor = switch (tone) {
			case INFO -> 0xCC10212B;
			case PROGRESS -> 0xCC1D2338;
			case FAILURE -> 0xCC321717;
			case SUCCESS -> 0xCC173122;
		};
		int borderColor = switch (tone) {
			case INFO -> 0xFF58B3FF;
			case PROGRESS -> 0xFF7E8BFF;
			case FAILURE -> 0xFFFF6D6D;
			case SUCCESS -> 0xFF4CDB8D;
		};

		guiGraphics.fill(0, 0, width(), height(), backgroundColor);
		guiGraphics.fill(0, 0, width(), 2, borderColor);
		guiGraphics.fill(0, height() - 1, width(), height(), 0x66000000);

		int progressWidth = (int) ((1.0f - Math.min(fullyVisibleForMs / (float) DURATION_MS, 1.0f)) * (width() - 8));
		guiGraphics.fill(4, height() - 4, 4 + progressWidth, height() - 2, borderColor);

		int textWidth = width() - (16);
		List<FormattedCharSequence> titleLines = font.split(title, textWidth);
		List<FormattedCharSequence> descriptionLines = font.split(description, textWidth);

		int textY = 6;
		int titleHeight = drawWrappedLines(guiGraphics, font, titleLines, 8, textY, 0xFFFFFFFF, 2);
		textY += titleHeight;
		if (titleHeight > 0 && !descriptionLines.isEmpty()) textY += 2;
		drawWrappedLines(guiGraphics, font, descriptionLines, 8, textY, 0xFFD8E6FF, 4);
	}

	private static int drawWrappedLines(
			GuiGraphicsExtractor guiGraphics,
			Font font,
			List<FormattedCharSequence> lines,
			int x,
			int startY,
			int color,
			int maxLines
	) {
		int count = Math.min(maxLines, lines.size());
		for (int i = 0; i < count; i++) {
			guiGraphics.text(font, lines.get(i), x, startY + (i * font.lineHeight), color, false);
		}
		return count * font.lineHeight;
	}

	@Override
	public int width() {
		return 220;
	}

	@Override
	public int height() {
		return 52;
	}
}
