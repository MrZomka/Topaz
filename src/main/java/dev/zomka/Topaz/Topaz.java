package dev.zomka.Topaz;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static net.kyori.adventure.text.Component.text;

@Plugin(
        id = "topaz",
        name = "Topaz",
        version = dev.zomka.Topaz.BuildConstants.VERSION,
        description = "A simple Anti-VPN that doesn't depend on it's own weird, unknown API. Inspired by egg82/Laarryy's Anti-VPN plugin.",
        authors = {"Zomka"}
)
public class Topaz {

    @Inject
    private Logger logger;
    private Toml config;

    private Toml loadConfig(Path path) {
        File folder = path.toFile();
        File file = new File(folder, "config.toml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            try (InputStream input = getClass().getResourceAsStream("/" + file.getName())) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                return null;
            }
        } return new Toml().read(file);
    }

    @Inject
    public void configLoader(@DataDirectory final Path folder) throws IOException {
        config = loadConfig(folder);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        logger.info("Startup successful.");
    }

    @Subscribe
    public void onLoginEvent(LoginEvent e) throws IOException {
        Toml messages = config.getTable("Messages");
        try {
            Toml options = config.getTable("Options");
            URL url = new URL("http://check.getipintel.net/check.php?ip=" + e.getPlayer().getRemoteAddress().getHostString() + "&contact=" + options.getString("email"));
            Scanner sc = new Scanner(url.openStream());
            StringBuffer sb = new StringBuffer();
            while (sc.hasNext()) {
                sb.append(sc.next());
            }
            //Retrieving the String from the String Buffer object
            String result = sb.toString();
            double number = Double.parseDouble(result);
            if (number > 0.99) {
                e.setResult(ResultedEvent.ComponentResult.denied(text(messages.getString("usingVPN"))));
                logger.warn(e.getPlayer().getUsername() + " (" + e.getPlayer().getUniqueId() + ") failed the IP quality score check! " + result + " (" + e.getPlayer().getRemoteAddress().getHostString() + ")");
            }
        } catch (IOException ex) {
            logger.error("Something went wrong! Make sure you put your correct email in the config file and have enough API requests for today!");
            ex.printStackTrace();
            e.setResult(ResultedEvent.ComponentResult.denied(text((messages.getString("errorKick")))));
        }
    }
}
