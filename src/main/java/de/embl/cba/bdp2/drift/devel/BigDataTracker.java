package de.embl.cba.bdp2.drift.devel;

import de.embl.cba.bdp2.drift.track.TrackingSettings;
import de.embl.cba.bdp2.utils.DimensionOrder;
import de.embl.cba.bdp2.utils.Utils;
import de.embl.cba.bdp2.viewers.BdvImageViewer;
import de.embl.cba.bdp2.utils.Point3D;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BigDataTracker< R extends RealType< R > & NativeType< R > > {

	public static ExecutorService trackerThreadPool;

    public OldTrack< R > oldTrackResults;
    public TrackingSettings< R > trackingSettings;

    public BigDataTracker(){
        kickOffThreadPack(Runtime.getRuntime().availableProcessors() * 2); //TODO: decide if this n threads is ok --ashis
    }

    public void kickOffThreadPack(int numThreads){
        if(null == trackerThreadPool
                ||  trackerThreadPool.isTerminated()){
            trackerThreadPool = Executors.newFixedThreadPool(numThreads);
        }
    }

    public void shutdownThreadPack(){
        Utils.shutdownThreadPack( trackerThreadPool,5);
    }

    // TODO:
    // is the imageViewer needed???
    // separate image from settings
    public AbstractObjectTracker trackObject( TrackingSettings< R > trackingSettings, BdvImageViewer imageViewer )
    {
//        this.trackingSettings = trackingSettings;
//        Point3D minInit = trackingSettings.pMin;
//        Point3D maXinit = trackingSettings.pMax;
//        AtomicBoolean stop = new AtomicBoolean(false);
//        AbstractObjectTracker objectTracker = new ObjectTracker( trackingSettings, stop );
//        BigDataProcessor2.trackerThreadPool.submit(()-> {
//                    this.trackResults = objectTracker.computeTrack();
//            if(!stop.get()) {
//
//                final BdvImageViewer newTrackedView = imageViewer.showImageInNewWindow(
//                        imageViewer.getImage().newImage( trackingSettings.rai ) );
//
//                if(newTrackedView instanceof BdvImageViewer) {
//                    TrackedAreaBoxOverlay tabo = new TrackedAreaBoxOverlay(this.trackResults,
//                            ((BdvHandleFrame) (newTrackedView).getBdvStackSource().getBdvHandle()).getBigDataViewer().getViewer(),
//                            ((BdvHandleFrame) (newTrackedView).getBdvStackSource().getBdvHandle()).getBigDataViewer().getSetupAssignments(), 9991,
//                            Intervals.createMinMax((long) minInit.getX(), (long) minInit.getY(), (long) minInit.getZ(), (long) maXinit.getX(), (long) maXinit.getY(), (long) maXinit.getZ()));
//                }
//            }else{
//                this.trackResults = null; // Qualifying trackResults for garbage collection.
//            }
//        });
        return null;
    }

    public< T extends RealType< T > & NativeType< T >> void showTrackedObjects(
           BdvImageViewer imageViewer)
    {
        if( oldTrackResults !=null) {
            List<RandomAccessibleInterval<T>> tracks = new ArrayList<>();
            int nChannels = (int) trackingSettings.rai.dimension( DimensionOrder.C );
            for (Map.Entry<Integer, Point3D[]> entry : this.oldTrackResults.locations.entrySet()) {
                Point3D[] pMinMax = entry.getValue();
                long[] range = {(long) pMinMax[0].getX(),
                                (long) pMinMax[0].getY(),
                                (long) pMinMax[0].getZ(),
                                0,
                                entry.getKey(),
                                (long) pMinMax[1].getX(),
                                (long) pMinMax[1].getY(),
                                (long) pMinMax[1].getZ(),
                                nChannels-1,
                                entry.getKey()}; //XYZCT order
                FinalInterval trackedInterval = Intervals.createMinMax(range);
                RandomAccessibleInterval trackedRegion = Views.interval((RandomAccessible) Views.extendZero(trackingSettings.rai ), trackedInterval); // Views.extendZero in case range is beyond the original image.
                RandomAccessibleInterval timeRemovedRAI = Views.zeroMin(Views.hyperSlice(trackedRegion,4, entry.getKey()));
                tracks.add(timeRemovedRAI);
            }

            RandomAccessibleInterval stackedRAI = Views.stack(tracks);
            imageViewer.showImageInNewWindow( imageViewer.getImage().newImage( stackedRAI ) );

        }
    }
}