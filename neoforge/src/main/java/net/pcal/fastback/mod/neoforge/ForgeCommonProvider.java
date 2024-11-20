package net.pcal.fastback.mod.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.ModLoadingContext;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.LifecycleListener;
import net.pcal.fastback.mod.MinecraftProvider;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.Commands.createBackupCommand;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.mod.MinecraftProvider.messageToText;

/**
 * @author pcal
 * @since 0.16.0
 */
class ForgeCommonProvider implements MinecraftProvider {

    static final String MOD_ID = "fastback";
    private MinecraftServer logicalServer;
    private LifecycleListener lifecycleListener = null;
    private Runnable autoSaveListener;
    private boolean isWorldSaveEnabled;

    ForgeCommonProvider() {
        final IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        modEventBus.addListener(this::onDedicatedServerStartupEvent);
        NeoForge.EVENT_BUS.addListener(this::onServerStartupEvent);
        NeoForge.EVENT_BUS.addListener(this::onServerStoppingEvent);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommandEvent);
    }


    // ======================================================================
    // Forge Event handlers

    private void onDedicatedServerStartupEvent(FMLDedicatedServerSetupEvent event) {
        this.onInitialize();
    }

    private void onServerStartupEvent(ServerStartedEvent event) {
        this.logicalServer = event.getServer();
        requireNonNull(this.lifecycleListener).onWorldStart();
    }

    private void onServerStoppingEvent(ServerStoppingEvent event) {
        requireNonNull(this.lifecycleListener).onWorldStop();
        this.logicalServer = null;
    }

    private void onRegisterCommandEvent(RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        final LiteralArgumentBuilder<CommandSourceStack> backupCommand =
                createBackupCommand(permName -> x -> true);
        commandDispatcher.register(backupCommand);
    }

    /**
     TODO This one isn't it.  We need to hear about it when an autosaves (and only autosaves) are completed.
     Might have to delve into Forge mixins to do this.
     private void onLevelSaveEvent(LevelEvent.Save event) {
     provider.onAutoSaveComplete();
     }
     **/


    // ======================================================================
    // Protected

    /**
     * This is the key initialization routine.  Registers the logger, the frameworkprovider and the commands
     * where the rest of the mod can get at them.
     */
    void onInitialize() {
        SystemLogger.Singleton.register(new Slf4jSystemLogger(LoggerFactory.getLogger(MOD_ID)));
        this.lifecycleListener = MinecraftProvider.register(this);
        syslog().debug("registered backup command");
        this.lifecycleListener.onInitialize();
        SshHacks.ensureSshSessionFactoryIsAvailable();
        syslog().info("Fastback initialized");
        syslog().warn("------------------------------------------------------------------------------------");
        syslog().warn("Thanks for trying the new Forge version of Fastback.  For help, go to:");
        syslog().warn("https://pcal43.github.io/fastback/");
        syslog().warn("Please note that this is an alpha release.  A list of known issues is available here:");
        syslog().warn("https://github.com/pcal43/fastback/issues?q=is%3Aissue+is%3Aopen+label%3Aforge");
        syslog().warn("------------------------------------------------------------------------------------");
    }


    // ======================================================================
    // Fastback MinecraftProvider implementation

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public void setHudText(UserMessage userMessage) {
    }

    @Override
    public void clearHudText() {
    }

    @Override
    public void setMessageScreenText(UserMessage userMessage) {
    }

    void renderOverlayText(GuiGraphics drawContext) {
    }

    @Override
    public String getModVersion() {
        return "0.15.3+1.20.1-alpha"; //FIXME
    }

    //FIXME!!
    void onAutoSaveComplete() {
        syslog().debug("onAutoSaveComplete");
        this.autoSaveListener.run();
    }

    @Override
    public Path getWorldDirectory() {
        if (this.logicalServer == null) throw new IllegalStateException("minecraftServer is null");
        return logicalServer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
    }

    @Override
    public void setWorldSaveEnabled(boolean enabled) {
        for (ServerLevel world : logicalServer.getAllLevels()) {
            world.noSave = !enabled;
        }
    }

    @Override
    public void saveWorld() {
        if (this.logicalServer == null) throw new IllegalStateException();
        this.logicalServer.saveEverything(false, true, true); // suppressLogs, flush, force
    }

    @Override
    public void sendBroadcast(UserMessage userMessage) {
        if (this.logicalServer != null && this.logicalServer.isDedicatedServer()) {
            logicalServer.getPlayerList().broadcastSystemMessage(messageToText(userMessage), false);
        }
    }

    @Override
    public void setAutoSaveListener(Runnable runnable) {
        this.autoSaveListener = requireNonNull(runnable);
    }

    @Override
    public Path getSavesDir() {
        if (this.isClient()) {
            return logicalServer.getServerDirectory().resolve("saves");
        } else {
            return null;
        }
    }

    @Override
    public String getWorldName() {
        return this.logicalServer.getWorldData().getLevelName();
    }

    /**
     * Add extra properties that will be stored in .fastback/backup.properties.
     */
    @Override
    public void addBackupProperties(Map<String, String> props) {
        props.put("fastback-version", this.getModVersion());
        if (this.logicalServer != null) {
            props.put("minecraft-version", logicalServer.getServerVersion());
            props.put("minecraft-game-mode", String.valueOf(logicalServer.getWorldData().getGameType()));
            props.put("minecraft-level-name", logicalServer.getWorldData().getLevelName());
        }
    }

    /**
     * @return paths to the files and directories that should be backed up when config-backup is enabled.
     */
    @Override
    public Collection<Path> getModsBackupPaths() {
        final List<Path> out = new ArrayList<>();
        /**
         final FabricLoader fl = FabricLoader.getInstance();
         final Path gameDir = fl.getGameDir();
         out.add(gameDir.resolve("options.txt´"));
         out.add(gameDir.resolve("mods"));
         out.add(gameDir.resolve("config"));
         out.add(gameDir.resolve("resourcepacks"));
         **/
        return out;
    }

}
