package dev.zomka.Topaz;
import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

@Plugin(
        id = "topaz",
        name = "Topaz",
        version = dev.zomka.Topaz.BuildConstants.VERSION,
        description = "A simple Anti-VPN that doesn't depend on it's own weird, unknown API. Inspired by egg82/Laarryy's Anti-VPN plugin.",
        authors = {"Zomka"}
)
public class Topaz {
    private final ProxyServer proxy;
    private final List<String> allowedIPs;
    private final List<String> blockedIPs;
    @Inject
    private Logger logger;
    private Toml config;

    @Inject @DataDirectory
    private Path configFolder;

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
        }
        return new Toml().read(file);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void configLoader(Path folder) throws IOException {
        config = loadConfig(folder);
        assert config != null;
        Toml options = config.getTable("Options");
        double configVersion = options.getDouble("configVersion");
        if (configVersion != 1.1) {
            logger.error("Your config is outdated! Your current config was backed up and a new one was generated!");
            Files.move(folder.resolve("config.toml"), folder.resolve("config.toml.backup"), StandardCopyOption.REPLACE_EXISTING);
            File file = new File(folder.toFile(), "config.toml");
            if (!file.exists()) {
                try (InputStream input = getClass().getResourceAsStream("/" + file.getName())) {
                    if (input != null) {
                        Files.copy(input, file.toPath());
                    } else {
                        file.createNewFile();
                    }
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            configLoader(configFolder);
        } catch (Exception e) {
            logger.error("Failed to load config!", e);
            return;
        }
        logger.info("Startup successful.");
        proxy.getScheduler().buildTask(this, () -> {
            allowedIPs.clear();
            blockedIPs.clear();
            logger.info("Cache cleared!");}
        ).delay(0, TimeUnit.SECONDS)
                .repeat(config.getTable("Options").getLong("cacheClearInterval").intValue(), TimeUnit.SECONDS)
                .schedule();
    }

    @Inject
    public Topaz(ProxyServer proxy) {
        this.proxy = proxy;
        this.allowedIPs = new ArrayList<>();
        this.blockedIPs = new ArrayList<>();

        // Register reload command
        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta meta = commandManager.metaBuilder("topazreload").build();
        commandManager.register(meta, new ReloadCommand());
    }

    @Subscribe
    public void onLoginEvent(LoginEvent e) {
        Toml messages = config.getTable("Messages");
        if (e.getPlayer().hasPermission("topaz.bypass") || allowedIPs.contains(e.getPlayer().getRemoteAddress().getHostString())) {
            return;
        }
        Toml options = config.getTable("Options");
        if (blockedIPs.contains(e.getPlayer().getRemoteAddress().getHostString())) {
            e.setResult(ResultedEvent.ComponentResult.denied(text(messages.getString("usingVPN"))));
            logger.warn(e.getPlayer().getUsername() + " (" + e.getPlayer().getUniqueId() + ") failed the IP quality score check! Cached blocked IP! (" + e.getPlayer().getRemoteAddress().getHostString() + ")");
            return;
        }
        try {
            URL url = new URL(options.getString("subdomain") + "?ip=" + e.getPlayer().getRemoteAddress().getHostString() + "&contact=" + options.getString("email"));
            Scanner sc = new Scanner(url.openStream());
            StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) {
                sb.append(sc.next());
            }
            String result = sb.toString();
            double number = Double.parseDouble(result);
            if (number > 0.99) {
                blockedIPs.add(e.getPlayer().getRemoteAddress().getHostString());
                e.setResult(ResultedEvent.ComponentResult.denied(text(messages.getString("usingVPN"))));
                logger.warn(e.getPlayer().getUsername() + " (" + e.getPlayer().getUniqueId() + ") failed the IP quality score check! " + result + " (" + e.getPlayer().getRemoteAddress().getHostString() + ")");
            } else {
                allowedIPs.add(e.getPlayer().getRemoteAddress().getHostString());
            }
        } catch (IOException ex) {
            logger.error("Something went wrong! Make sure you put your correct email in the config file and have enough API requests for today!");
            ex.printStackTrace();
            e.setResult(ResultedEvent.ComponentResult.denied(text((messages.getString("errorKick")))));
        }
    }

    public final class ReloadCommand implements SimpleCommand {

        @Override
        public void execute(final Invocation invocation) {
            CommandSource source = invocation.source();
            try {
                configLoader(configFolder);
                allowedIPs.clear();
                blockedIPs.clear();
                source.sendMessage(text("Reloaded config!"));
            } catch (IOException e) {
                source.sendMessage(text("Failed to reload config because of " + e.getMessage()));
                e.printStackTrace();
            }
        }

        @Override
        public boolean hasPermission(final Invocation invocation) {
            return invocation.source().hasPermission("topaz.reload");
        }
    }
}