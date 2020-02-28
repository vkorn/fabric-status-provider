package dev.lambdacraft.status_provider;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.loader.api.FabricLoader;

public class StatusMain implements ModInitializer {
    public static Properties props;
    public static ServerStatus status;
    public static final Logger LOG = LogManager.getLogger();

    public void loadProps() {
        Path configPath = Paths.get(
            FabricLoader.getInstance().getConfigDirectory().toString(),
            "fabric_status_provider.properties"
        );
        Properties props = new Properties();
        Boolean writeExample = false;
        try {
            props.load(new FileInputStream(configPath.toString()));
        } catch (FileNotFoundException e) {
            props.setProperty("port", "8080");
            props.setProperty("secret", "");
            writeExample = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (writeExample) {
            try {
                props.store(
                    new FileWriter(configPath.toString()),
                    "Example default config for Status Provider"
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        StatusMain.props = props;
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        loadProps();

        ServerStartCallback.EVENT.register(server -> {
            int port;
            try {
                port = Integer.parseInt(StatusMain.props.getProperty("port"));
            } catch (NumberFormatException e) {
                LOG.error("[Status Provider] Port needs to be a number! Status Provider cannot run!");
                return;
            }

            try {
                status = new ServerStatus(server, port);
            } catch (Exception ex) {
                LOG.error("Failed to start Status server:\n" + ex.getMessage());
            }
        });
    }
}
