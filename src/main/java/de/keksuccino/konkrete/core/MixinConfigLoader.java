package de.keksuccino.konkrete.core;

import org.spongepowered.asm.mixin.Mixins;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;

public class MixinConfigLoader {

    public static void loadConfigs() {
        try {

            Enumeration<URL> resources = MixinConfigLoader.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            for (URL url : Collections.list(resources)) {
                try {
                    InputStream in = url.openStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line = reader.readLine();
                    while (line != null) {
                        if (line.startsWith("loadMixinConfigInKonkrete:")) {
                            String mixinConfig = line.split("[:]", 2)[1];
                            if (mixinConfig.startsWith(" ")) {
                                mixinConfig = mixinConfig.substring(1);
                                System.out.println("[KONKRETE] Loading Mixin Config: " + mixinConfig);
                                Mixins.addConfiguration(mixinConfig);
                            }
                        }
                        line = reader.readLine();
                    }
                    reader.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
