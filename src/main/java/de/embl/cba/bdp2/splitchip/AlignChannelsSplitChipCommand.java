package de.embl.cba.bdp2.splitchip;

import de.embl.cba.bdp2.process.AbstractProcessingCommand;
import de.embl.cba.bdp2.utils.Utils;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataProcessor2>" + AbstractProcessingCommand.COMMAND_PROCESS_PATH + AlignChannelsSplitChipCommand.COMMAND_FULL_NAME )
public class AlignChannelsSplitChipCommand< R extends RealType< R > & NativeType< R > > extends AbstractProcessingCommand< R >
{
    public static final String COMMAND_NAME = "Align Channels Split Chip...";
    public static final String COMMAND_FULL_NAME = Utils.COMMAND_BDP_PREFIX + COMMAND_NAME;

    @Parameter(label = "Regions [ minX, minY, sizeX, sizeY, channel; minX, ... ]")
    public String intervalsString = "896, 46, 1000, 1000, 0; 22, 643, 1000, 1000, 0";

    public void run()
    {
        process();
        handleOutputImage( true, false );
    }

    public void process()
    {
        final SplitViewMerger merger = new SplitViewMerger();
        addIntervals( merger );
        outputImage = merger.mergeIntervalsXYC( inputImage );
    }

    private void addIntervals( SplitViewMerger merger )
    {
        final List< long[] > longs = Utils.delimitedStringToLongs( intervalsString );

        for ( long[] interval : longs )
        {
            merger.addIntervalXYC( interval );
        }
    }
}