/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.Config;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.command.arguments.ArgumentSerializers;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.recipe.ComputerUpgradeRecipe;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.crafting.RecipeSerializers;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ComputerCraftProxyCommon
{
    @SubscribeEvent
    public static void init( FMLCommonSetupEvent event )
    {
        NetworkHandler.setup();
        Containers.setup();

        registerProviders();

        RecipeSerializers.register( ColourableRecipe.SERIALIZER );
        RecipeSerializers.register( ComputerUpgradeRecipe.SERIALIZER );
        RecipeSerializers.register( PocketComputerUpgradeRecipe.SERIALIZER );
        RecipeSerializers.register( DiskRecipe.SERIALIZER );
        RecipeSerializers.register( PrintoutRecipe.SERIALIZER );
        RecipeSerializers.register( TurtleRecipe.SERIALIZER );
        RecipeSerializers.register( TurtleUpgradeRecipe.SERIALIZER );
        RecipeSerializers.register( ImpostorShapelessRecipe.SERIALIZER );
        RecipeSerializers.register( ImpostorRecipe.SERIALIZER );

        ArgumentSerializers.register();

        // if( Loader.isModLoaded( ModCharset.MODID ) ) IntegrationCharset.register();
    }

    private static void registerProviders()
    {
        // Register peripheral providers
        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) -> {
            TileEntity tile = world.getTileEntity( pos );
            return tile instanceof IPeripheralTile ? ((IPeripheralTile) tile).getPeripheral( side ) : null;
        } );

        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) -> {
            TileEntity tile = world.getTileEntity( pos );
            return ComputerCraft.enableCommandBlock && tile instanceof TileEntityCommandBlock ? new CommandBlockPeripheral( (TileEntityCommandBlock) tile ) : null;
        } );

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider( new DefaultBundledRedstoneProvider() );

        // Register media providers
        ComputerCraftAPI.registerMediaProvider( stack -> {
            Item item = stack.getItem();
            if( item instanceof IMedia ) return (IMedia) item;
            if( item instanceof ItemRecord ) return RecordMedia.INSTANCE;
            return null;
        } );

        // Register network providers
        CapabilityWiredElement.register();
    }

    @Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
    public static final class ForgeHandlers
    {
        private ForgeHandlers()
        {
        }

        /*
        @SubscribeEvent
        public static void onConnectionOpened( FMLNetworkEvent.ClientConnectedToServerEvent event )
        {
            ComputerCraft.clientComputerRegistry.reset();
        }

        @SubscribeEvent
        public static void onConnectionClosed( FMLNetworkEvent.ClientDisconnectionFromServerEvent event )
        {
            ComputerCraft.clientComputerRegistry.reset();
        }
        */

        @SubscribeEvent
        public static void onClientTick( TickEvent.ClientTickEvent event )
        {
            if( event.phase == TickEvent.Phase.START )
            {
                ComputerCraft.clientComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public static void onServerTick( TickEvent.ServerTickEvent event )
        {
            if( event.phase == TickEvent.Phase.START )
            {
                MainThread.executePendingTasks();
                ComputerCraft.serverComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public static void onConfigChanged( ConfigChangedEvent.OnConfigChangedEvent event )
        {
            if( event.getModID().equals( ComputerCraft.MOD_ID ) ) Config.sync();
        }

        @SubscribeEvent
        public static void onContainerOpen( PlayerContainerEvent.Open event )
        {
            // If we're opening a computer container then broadcast the terminal state
            Container container = event.getContainer();
            if( container instanceof IContainerComputer )
            {
                IComputer computer = ((IContainerComputer) container).getComputer();
                if( computer instanceof ServerComputer )
                {
                    ((ServerComputer) computer).sendTerminalState( event.getEntityPlayer() );
                }
            }
        }

        @SubscribeEvent
        public static void onServerStarting( FMLServerStartingEvent event )
        {
            CommandComputerCraft.register( event.getCommandDispatcher() );
        }

        @SubscribeEvent
        public static void onServerStarted( FMLServerStartedEvent event )
        {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            Tracking.reset();
        }

        @SubscribeEvent
        public static void onServerStopped( FMLServerStoppedEvent event )
        {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            Tracking.reset();
        }
    }
}
