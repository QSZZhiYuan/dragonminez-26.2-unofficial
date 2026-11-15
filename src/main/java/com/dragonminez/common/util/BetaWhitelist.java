package com.dragonminez.common.util;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraft.util.RandomSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Beta-tester whitelist, fetched from GitHub with a bundled fallback.
 *
 * <h2>26.2 / robustness notes</h2>
 * The previous implementation used {@code new URL(...).openStream()}, which has <b>no timeout at
 * all</b> — the connection inherited the OS default, and on a machine that blackholes the request
 * the call blocked for ~21 seconds before failing. The fetch is already asynchronous, but the log
 * line still appeared 21 s late and a second fetch fired on world join.
 *
 * <p>Two changes:
 * <ul>
 *   <li>explicit {@code setConnectTimeout} / {@code setReadTimeout} (3 s each), so an unreachable
 *       host fails fast instead of stalling;</li>
 *   <li>a one-shot guard so the list is fetched at most once per JVM unless {@link #reload()} is
 *       called explicitly — that removes the duplicate fetch on world join.</li>
 * </ul>
 * The fallback list is used whenever the fetch fails, and the whitelist only gates beta cosmetics,
 * so a failed fetch is never fatal.
 */
public class BetaWhitelist {
	public static final String DEV_USERNAME = "Dev";
	public static final String WHITELIST_URL = "https://raw.githubusercontent.com/DragonMineZ/.github/refs/heads/main/allowed_betatesters.txt";

	/** Fail fast: an unreachable host must not stall startup. */
	private static final int CONNECT_TIMEOUT_MS = 3000;
	private static final int READ_TIMEOUT_MS = 3000;

	private static final List<String> FALLBACK_LIST = Arrays.asList("Dev", "ImYuseix", "ezShokkoh");
	private static volatile List<String> activeList = new ArrayList<>(FALLBACK_LIST);
	private static volatile boolean fetchStarted = false;

	/** Fetches the remote list once per JVM. Safe to call repeatedly. */
	public static void reload() {
		if (fetchStarted) return;
		fetchStarted = true;

		LogUtil.info(Env.SERVER, "Fetching remote whitelist from GitHub ({}ms timeout)...", CONNECT_TIMEOUT_MS);
		CompletableFuture.runAsync(BetaWhitelist::fetch);
	}

	/** Forces a re-fetch; used by explicit config reloads. */
	public static void forceReload() {
		fetchStarted = false;
		reload();
	}

	private static void fetch() {
		try {
			URLConnection connection = new URL(WHITELIST_URL).openConnection();
			connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
			connection.setReadTimeout(READ_TIMEOUT_MS);

			List<String> downloadedList = new ArrayList<>();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = in.readLine()) != null) {
					String name = line.trim();
					if (!name.isEmpty()) downloadedList.add(name.toLowerCase());
				}
			}

			if (!downloadedList.isEmpty()) {
				activeList = downloadedList;
				LogUtil.info(Env.SERVER, "Whitelist updated! Loaded {} users.", activeList.size());
			}
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Failed to fetch remote whitelist. Using fallback list. Error: " + e.getMessage());
		}
	}

	public static boolean isAllowed(String username) {
		return activeList.contains(username.toLowerCase());
	}

	public static List<String> getSkinCandidates() {
		List<String> candidates = new ArrayList<>(activeList.size());
		for (String name : activeList) if (!name.equalsIgnoreCase(DEV_USERNAME)) candidates.add(name);
		return candidates;
	}

	public static String getRandomBetatester(RandomSource random) {
		List<String> candidates = getSkinCandidates();
		if (candidates.isEmpty()) return "";
		return candidates.get(random.nextInt(candidates.size()));
	}
}
