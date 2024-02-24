package de.keksuccino.konkrete.sound;

import de.keksuccino.konkrete.platform.Services;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is outdated and doesn't work really well. You should not use it anymore.<br>
 * There's currently no replacement for this class.
 */
@Deprecated(forRemoval = true)
public class SoundHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	private static Map<String, Clip> sounds = new HashMap<>();
	private static boolean init = false;
	private static volatile boolean volumeHandling = true;
	private static List<String> unsupportedFormatAudios = new ArrayList<>();

	public static void init() {
		if (!Services.PLATFORM.isOnClient()) {
			LOGGER.error("[KONKRETE] Tried to initialize SoundHandler server-side!", new IllegalStateException("Cannot initialize SoundHandler on server."));
			return;
		}
		if (!init) {
			//Observation thread to check if Minecraft's master volume was changed and set the new volume to all registered sounds
			new Thread(() -> {
				float lastMaster = 0.0F;
				while (volumeHandling) {
					try {
						if ((Minecraft.getInstance() != null) && (Minecraft.getInstance().options != null)) {
							float currentMaster = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
							if (lastMaster != currentMaster) {
								SoundHandler.updateVolume();
							}
							lastMaster = currentMaster;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();

			init = true;
		}
	}

	public static void registerSound(String key, String path) {
		if (unsupportedFormatAudios.contains(key)) {
			return;
		}
		if (!Services.PLATFORM.isOnClient()) {
			LOGGER.error("[KONKRETE] Tried to register sound server-side!", new IllegalStateException("Cannot registers sounds to SoundHandler on server."));
			return;
		}
		if (!sounds.containsKey(key)) {
			AudioInputStream in = null;
			try {
				Clip c = AudioSystem.getClip();
				in = AudioSystem.getAudioInputStream(new File(path));
				c.open(in);
				in.close();
				sounds.put(key, c);
				setVolume(key, getMinecraftMasterVolume());
			} catch (IllegalArgumentException e) {
				LOGGER.error("[KONKRETE] Unable to register sound! Format not supported or invalid!");
				printErrorLog(e, key, 0, "registering audio");
				unsupportedFormatAudios.add(key);
				if (in != null) {
					try {
						in.close();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
				sounds.remove(key);
			} catch (Exception e) {
				e.printStackTrace();
				if (in != null) {
					try {
						in.close();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
				sounds.remove(key);
			}
		}
	}

	public static void unregisterSound(String key) {
		if (sounds.containsKey(key)) {
			sounds.get(key).stop();
			sounds.get(key).close();
			sounds.remove(key);
		}
	}

	public static void playSound(String key) {
		if (sounds.containsKey(key)) {
			sounds.get(key).start();
		}
	}

	public static void stopSound(String key) {
		if (sounds.containsKey(key)) {
			sounds.get(key).stop();
		}
	}

	public static void resetSound(String key) {
		if (sounds.containsKey(key)) {
			sounds.get(key).setMicrosecondPosition(0);
		}
	}

	public static boolean soundExists(String key) {
		return sounds.containsKey(key);
	}

	public static void setLooped(String key, boolean looped) {
		if (sounds.containsKey(key)) {
			Clip c = sounds.get(key);
			if (looped) {
				c.setLoopPoints(0, -1);
				c.loop(-1);
			} else {
				c.loop(0);
			}
		}
	}

	public static boolean isPlaying(String key) {
		return (sounds.containsKey(key) && sounds.get(key).isRunning());
	}

	public static void updateVolume() {
		for (String s : sounds.keySet()) {
			setVolume(s, getMinecraftMasterVolume());
		}
	}

	private static void setVolume(String key, int percentage) {
		if (!volumeHandling) {
			return;
		}
		try {
			if (sounds.containsKey(key)) {
				Clip clip = sounds.get(key);
				if (clip.isOpen()) {
					if (clip.isControlSupported(Type.MASTER_GAIN)) {
						FloatControl f = ((FloatControl)sounds.get(key).getControl(Type.MASTER_GAIN));
						int gain = (int) ((int) f.getMinimum() + ((f.getMaximum() - f.getMinimum()) / 100 * percentage));
						f.setValue(gain);
					} else {
						volumeHandling = false;
						LOGGER.error("[KONKRETE] Unable to set sound volume! MASTER_GAIN control not supported! Disabling volume handling!");
						printErrorLog(new Throwable(), key, percentage, "setting volume");
					}
				} else {
					LOGGER.error("[KONKRETE] Unable to set sound volume! Clip not open: " + key);
				}
			}
		} catch (Exception e) {
			volumeHandling = false;
			LOGGER.error("[KONKRETE] Unable to set sound volume! Disabling volume handling!");
			printErrorLog(e, key, percentage, "setting volume");
		}
	}

	private static void printErrorLog(Throwable throwable, String audioKey, int audioVolume, String action) {
		CrashReport c = CrashReport.forThrowable(throwable, "Exception in Konkrete while " + action);
		CrashReportCategory cat = c.addCategory("Audio Information");
		cat.setDetail("Key", audioKey);
		cat.setDetail("Volume", audioVolume);
		LOGGER.error(c.getFriendlyReport());
	}

	private static int getMinecraftMasterVolume() {
		return (int)(Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER) * 100);
	}

}
