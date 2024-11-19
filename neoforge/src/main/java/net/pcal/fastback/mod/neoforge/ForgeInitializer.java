package net.pcal.fastback.mod.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.reflect.InvocationTargetException;

/**
 * @author pcal
 * @since 0.16.0
 */
@Mod("fastback")
final public class ForgeInitializer {

    public ForgeInitializer(ModContainer container) {
        try {
            if (FMLEnvironment.dist.isDedicatedServer()) {
                new ForgeCommonProvider(container);
            } else if (FMLEnvironment.dist.isClient()) {
                // Forge yells at us if we touch any client classes in a server.  So,
                Class.forName("net.pcal.fastback.mod.neoforge.ForgeClientProvider").getConstructor(ModContainer.class).newInstance(container);
            } else {
                throw new IllegalStateException("where am i?  server or client?");
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
