package test.open;

import de.embl.cba.bdp2.BigDataProcessor2;
import de.embl.cba.bdp2.image.Image;

import static de.embl.cba.bdp2.open.core.NamingScheme.*;

public class TestOpenMultiChannelZLIBCompressedTiffVolumes
{
    public static void main(String[] args)
    {
        final String directory = "/Users/tischer/Documents/bigdataprocessor2/src/test/resources/test/tiff-nc2-nt6-zlib";

        final Image image = BigDataProcessor2.openImage(
                directory,
                MULTI_CHANNEL_VOLUMES + OME_TIF,
                ".*"
        );

        BigDataProcessor2.showImage( image, true );
    }
}