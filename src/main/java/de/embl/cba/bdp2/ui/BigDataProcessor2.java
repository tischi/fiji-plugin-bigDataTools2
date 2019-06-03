package de.embl.cba.bdp2.ui;

import de.embl.cba.bdp2.Image;
import de.embl.cba.bdp2.loading.CachedCellImgReader;
import de.embl.cba.bdp2.loading.files.FileInfos;
import de.embl.cba.bdp2.logging.Logger;
import de.embl.cba.bdp2.progress.ProgressListener;
import de.embl.cba.bdp2.saving.*;
import de.embl.cba.bdp2.utils.DimensionOrder;
import de.embl.cba.bdp2.utils.Utils;
import de.embl.cba.bdp2.viewers.BdvImageViewer;
import ij.gui.GenericDialog;
import net.imglib2.*;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellRandomAccess;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class BigDataProcessor2 < R extends RealType< R > & NativeType< R >>
{

    public static ExecutorService generalThreadPool;  //General thread pool
    public static ExecutorService trackerThreadPool; // Thread pool for tracking
    public static Map<Integer, Integer> progressTracker = new ConcurrentHashMap<>();
    public static int MAX_THREAD_LIMIT = Runtime.getRuntime().availableProcessors() * 2;
    public static Map<Integer, AtomicBoolean> saveTracker = new ConcurrentHashMap<>();

    public BigDataProcessor2() {
        //TODO: have separate shutdown for the executorService. It will not shutdown when ui exeService is shut. --ashis (DONE but needs testing)
        //Ref: https://stackoverflow.com/questions/23684189/java-how-to-make-an-executorservice-running-inside-another-executorservice-not
        kickOffThreadPack( Runtime.getRuntime().availableProcessors() * 2 );
    }

    private void kickOffThreadPack( int numThreads ) {
        if ( generalThreadPool == null)
            generalThreadPool = Executors.newFixedThreadPool( numThreads );
    }

    public Image< R > openImage(
            String directory,
            String loadingScheme,
            String filterPattern )
    {
        FileInfos fileInfos =
                new FileInfos( directory, loadingScheme, filterPattern );

        final Image< R > image = CachedCellImgReader.loadImage( fileInfos );

        return image;
    }


    public Image< R > openHdf5Image(
            String directory,
            String loadingScheme,
            String filterPattern,
            String hdf5DatasetName )
    {
        FileInfos fileInfos =
				new FileInfos(
				        directory,
                        loadingScheme,
                        filterPattern,
                        hdf5DatasetName );

        final Image< R > image = CachedCellImgReader.loadImage( fileInfos );
        
        return image;
    }
    
    public BdvImageViewer showImage( Image< R > image )
    {
        final BdvImageViewer viewer = new BdvImageViewer( image );

        return viewer;
    }

    public boolean showVoxelSpacingDialog( Image< R > image )
    {
        final double[] voxelSpacing = image.getVoxelSpacing();
        final String voxelUnit = image.getVoxelUnit();
        final GenericDialog genericDialog = new GenericDialog( "Calibration" );
        genericDialog.addStringField( "Unit", voxelUnit, 12 );
        genericDialog.addNumericField( "Spacing X", voxelSpacing[ 0 ], 3 );
        genericDialog.addNumericField( "Spacing Y", voxelSpacing[ 1 ], 3 );
        genericDialog.addNumericField( "Spacing Z", voxelSpacing[ 2 ], 3 );
        genericDialog.showDialog();
        if ( genericDialog.wasCanceled() ) return false;
        image.setVoxelUnit( genericDialog.getNextString() );
        voxelSpacing[ 0 ] = genericDialog.getNextNumber();
        voxelSpacing[ 1 ] = genericDialog.getNextNumber();
        voxelSpacing[ 2 ] = genericDialog.getNextNumber();
        return true;
    }


    // Return futures from the image saver
    public static < R extends RealType< R > & NativeType< R > >
    ImgSaver saveImage(
            Image< R > image,
            SavingSettings savingSettings,
            ProgressListener progressListener )
    {
        Logger.info( "Saving: Started..." );
        int nIOThread = Math.max( 1, Math.min( savingSettings.numIOThreads, MAX_THREAD_LIMIT ));
        ExecutorService saveExecutorService = Executors.newFixedThreadPool( nIOThread );

        Logger.info( "Saving: Configuring volume reader..." );
        final CachedCellImg< R, ? > volumeCachedCellImg
                = CachedCellImgReader.getVolumeCachedCellImg( image.getFileInfos() );

        final RandomAccessibleInterval< R > volumeLoadedRAI =
                new CachedCellImgReplacer( image.getRai(), volumeCachedCellImg ).get();

        savingSettings.rai = volumeLoadedRAI;
        savingSettings.voxelSpacing = image.getVoxelSpacing();
        savingSettings.voxelUnit = image.getVoxelUnit();
        ImgSaverFactory factory = new ImgSaverFactory();

        if ( savingSettings.saveVolumes )
            Utils.createFilePathParentDirectories( savingSettings.volumesFilePath );

        if ( savingSettings.saveProjections )
            Utils.createFilePathParentDirectories( savingSettings.projectionsFilePath );

        AbstractImgSaver saver = factory.getSaver( savingSettings, saveExecutorService );
        saver.setProgressListener( progressListener );
        saver.startSave();

        return saver;
    }

    public static <T extends RealType<T> & NativeType<T>>
    RandomAccessibleInterval shearImage(RandomAccessibleInterval rai, ShearingSettings shearingSettings)
    {
        System.out.println("Shear Factor X " + shearingSettings.shearingFactorX);
        System.out.println("Shear Factor Y " + shearingSettings.shearingFactorY);

        List<RandomAccessibleInterval<T>> timeTracks = new ArrayList<>();
        int nTimeFrames = (int) rai.dimension( DimensionOrder.T );
        int nChannels = (int) rai.dimension( DimensionOrder.C );
        System.out.println("Shear Factor X " + shearingSettings.shearingFactorX);
        System.out.println("Shear Factor Y " + shearingSettings.shearingFactorY);
        AffineTransform3D affine = new AffineTransform3D();
        affine.set(shearingSettings.shearingFactorX, 0, 2);
        affine.set(shearingSettings.shearingFactorY, 1, 2);
        List<ApplyShearToRAI> tasks = new ArrayList<>();
       // long startTime = System.currentTimeMillis();
        for (int t = 0; t < nTimeFrames; ++t) {
            ApplyShearToRAI task = new ApplyShearToRAI(rai, t, nChannels, affine, shearingSettings.interpolationFactory);
            task.fork();
            tasks.add(task);
        }
        for (ApplyShearToRAI task : tasks) {
            timeTracks.add((RandomAccessibleInterval) task.join());
        }
        final RandomAccessibleInterval sheared = Views.stack( timeTracks );
//        System.out.println("Time elapsed (ms) " + (System.currentTimeMillis() - startTime));
        return sheared;
    }

    public static <T extends RealType<T> & NativeType<T>>
    RandomAccessibleInterval shearImage5D( RandomAccessibleInterval rai5D, ShearingSettings shearingSettings)
    {
        final AffineTransform affine5D = new AffineTransform( 5 );
        affine5D.set(shearingSettings.shearingFactorX, 0, 2);
        affine5D.set(shearingSettings.shearingFactorY, 1, 2);

        RealRandomAccessible rra = Views.interpolate(
                Views.extendZero( rai5D ),
                new NearestNeighborInterpolatorFactory());

        AffineRandomAccessible af = RealViews.affine( rra, affine5D );

        final FinalInterval interval5D = getInterval5DAfterShearing( rai5D, shearingSettings );

        RandomAccessibleInterval intervalView = Views.interval( af, interval5D );

        return intervalView;
    }

    private static FinalInterval getInterval5DAfterShearing(
            RandomAccessibleInterval rai5D,
            ShearingSettings shearingSettings )
    {
        AffineTransform3D affine3DtoEstimateBoundsAfterTransformation = new AffineTransform3D();
        affine3DtoEstimateBoundsAfterTransformation.set(shearingSettings.shearingFactorX, 0, 2);
        affine3DtoEstimateBoundsAfterTransformation.set(shearingSettings.shearingFactorY, 1, 2);


        final IntervalView intervalView3D = Views.hyperSlice( Views.hyperSlice( rai5D, DimensionOrder.T, 0 ), DimensionOrder.C, 0 );
        FinalRealInterval transformedRealInterval = affine3DtoEstimateBoundsAfterTransformation.estimateBounds( intervalView3D );

        final Interval interval = Intervals.largestContainedInterval( transformedRealInterval );
        final long[] min = new long[ 5 ];
        final long[] max = new long[ 5 ];
        rai5D.min( min );
        rai5D.max( max );
        interval.min( min );
        interval.max( max );
        return new FinalInterval( min, max );
    }


    private static class ApplyShearToRAI<T extends RealType<T> & NativeType<T>> extends RecursiveTask<RandomAccessibleInterval> {

        private RandomAccessibleInterval rai;
        private int t;
        private final int nChannels;
        private final AffineTransform3D affine;
        private InterpolatorFactory interpolatorFactory;

        public ApplyShearToRAI(RandomAccessibleInterval rai, int time, int nChannels, AffineTransform3D affine,InterpolatorFactory interpolatorFactory) {
            this.rai = rai;
            this.t = time;
            this.nChannels = nChannels;
            this.affine = affine;
            this.interpolatorFactory = interpolatorFactory;
        }

        @Override
        protected RandomAccessibleInterval<T> compute() {
            List<RandomAccessibleInterval<T>> channelTracks = new ArrayList<>();
            RandomAccessibleInterval tStep = Views.hyperSlice(rai, DimensionOrder.T, t);
            for (int channel = 0; channel < nChannels; ++channel) {
                RandomAccessibleInterval cStep = Views.hyperSlice(tStep, DimensionOrder.C, channel);
                RealRandomAccessible real = Views.interpolate(Views.extendZero(cStep),this.interpolatorFactory);
                AffineRandomAccessible af = RealViews.affine(real, affine);
                FinalRealInterval transformedRealInterval = affine.estimateBounds(cStep);
                FinalInterval transformedInterval = Utils.asIntegerInterval(transformedRealInterval);
                RandomAccessibleInterval intervalView = Views.interval(af, transformedInterval);
                channelTracks.add(intervalView);
            }
            return Views.stack(channelTracks);
        }
    }


    public static <T extends RealType<T>> RandomAccessibleInterval
    unsignedByteTypeConverter(
            RandomAccessibleInterval rai,
            DisplaySettings displaySettings)
    {
        RandomAccessibleInterval<UnsignedByteType> newRai;
        if (!(Util.getTypeFromInterval(rai) instanceof UnsignedByteType)){
            newRai = Converters.convert(
                    rai,
                    new RealUnsignedByteConverter<T>(
                            displaySettings.getDisplayRangeMin(),
                            displaySettings.getDisplayRangeMax()),
                    new UnsignedByteType());
        }else{
            newRai = rai;
        }
        return newRai;
    }
}
