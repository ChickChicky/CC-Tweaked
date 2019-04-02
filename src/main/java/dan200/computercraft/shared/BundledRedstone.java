/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import com.google.common.base.Preconditions;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BundledRedstone
{
    private static final Set<IBundledRedstoneProvider> providers = new LinkedHashSet<>();

    private BundledRedstone() {}

    public static synchronized void register( @Nonnull IBundledRedstoneProvider provider )
    {
        Preconditions.checkNotNull( provider, "provider cannot be null" );
        providers.add( provider );
    }

    public static int getDefaultOutput( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side )
    {
        return World.isValid( pos ) ? DefaultBundledRedstoneProvider.getDefaultBundledRedstoneOutput( world, pos, side ) : -1;
    }

    private static int getUnmaskedOutput( World world, BlockPos pos, EnumFacing side )
    {
        if( !World.isValid( pos ) ) return -1;

        // Try the providers in order:
        int combinedSignal = -1;
        for( IBundledRedstoneProvider bundledRedstoneProvider : providers )
        {
            try
            {
                int signal = bundledRedstoneProvider.getBundledRedstoneOutput( world, pos, side );
                if( signal >= 0 )
                {
                    combinedSignal = combinedSignal < 0 ? signal & 0xffff : combinedSignal | (signal & 0xffff);
                }
            }
            catch( Exception e )
            {
                ComputerCraft.log.error( "Bundled redstone provider " + bundledRedstoneProvider + " errored.", e );
            }
        }

        return combinedSignal;
    }

    public static int getOutput( World world, BlockPos pos, EnumFacing side )
    {
        int signal = getUnmaskedOutput( world, pos, side );
        return signal >= 0 ? signal : 0;
    }
}
