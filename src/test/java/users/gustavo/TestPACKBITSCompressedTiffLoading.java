package users.gustavo;

import de.embl.cba.bdp2.Image;
import de.embl.cba.bdp2.loading.files.FileInfos;
import de.embl.cba.bdp2.ui.BigDataProcessor2;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TestPACKBITSCompressedTiffLoading
{
	public static  < R extends RealType< R > & NativeType< R > > void main( String[] args )
	{
		final Image< R > image = BigDataProcessor2.openImage(
				"/Users/tischer/Desktop/gustavo/issue1",
				FileInfos.SINGLE_CHANNEL_TIMELAPSE,
				".*.tif"
		);

		BigDataProcessor2.showImage( image, true );

//		final ImagePlus imagePlus = IJ.openImage( "/Users/tischer/Desktop/gustavo/P12_Ch1-registered-T0006.tif" );

	}
}
