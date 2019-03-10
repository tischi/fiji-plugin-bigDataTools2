package de.embl.cba.bdp2.process;

import bdv.tools.brightness.SliderPanel;
import bdv.util.BoundedValue;
import de.embl.cba.bdp2.fileinfosource.FileInfoConstants;
import de.embl.cba.bdp2.ui.BdvMenus;
import de.embl.cba.bdp2.viewers.ImageViewer;
import de.embl.cba.lazyalgorithm.LazyDownsampler;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class BinnedView< T extends RealType< T > & NativeType< T > > extends JFrame
{
	private static Point dialogLocation;

	public BinnedView( final ImageViewer imageViewer  )
	{

		final RandomAccessibleInterval< T > rai = imageViewer.getRai();

		long[] span = new long[]{1,1,0};

		final RandomAccessibleInterval< T > downsampledView =
				new LazyDownsampler<>( rai, span ).getDownsampledView();

		ImageViewer newImageViewer = imageViewer.newImageViewer();

		final double[] originalVoxelSize = imageViewer.getVoxelSize();

		newImageViewer.show(
				downsampledView,
				FileInfoConstants.UNSIGNEDBYTE_STREAM_NAME,
				getBinnedVoxelSize( span, originalVoxelSize ),
				imageViewer.getCalibrationUnit(),
				true);

		newImageViewer.addMenus( new BdvMenus() );

		showBinningAdjustmentDialog( rai, originalVoxelSize, newImageViewer, span );

	}

	private double[] getBinnedVoxelSize( long[] span, double[] voxelSize )
	{
		for ( int d = 0; d < 3; d++ )
		{
			voxelSize[ d ] /= 2 * span[ d ] + 1;
		}
		return voxelSize;
	}

	private void showBinningAdjustmentDialog(
			RandomAccessibleInterval< T > rai,
			double[] originalVoxelSize,
			ImageViewer imageViewer,
			long[] span )
	{
		final JFrame frame = new JFrame( "Binning" );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

		final ArrayList< BoundedValue > boundedValues = new ArrayList<>();
		final ArrayList< SliderPanel > sliderPanels = new ArrayList<>();

		for ( int d = 0; d < 3; d++ )
		{
			boundedValues.add( new BoundedValue(
					0, 10, ( int ) span[ d ] ) );
			sliderPanels.add(
					new SliderPanel(
							"Binning span dimension " + d ,
								boundedValues.get( d ),
								1));
		}



		class UpdateListener implements BoundedValue.UpdateListener
		{
			@Override
			public void update()
			{
				final long[] span = new long[ 3 ];

				for ( int d = 0; d < 3; d++ )
				{
					span[ d ] = boundedValues.get( d ).getCurrentValue();
					sliderPanels.get( d ).update();
				}

				final RandomAccessibleInterval< T > downsampledView =
						new LazyDownsampler<>( rai, span ).getDownsampledView();

				final double[] binnedVoxelSize = getBinnedVoxelSize( span, originalVoxelSize );

				imageViewer.show(
						downsampledView,
						imageViewer.getImageName(),
						binnedVoxelSize,
						imageViewer.getCalibrationUnit(),
						false );
			}
		}

		final UpdateListener updateListener = new UpdateListener();

		for ( int d = 0; d < 3; d++ )
			boundedValues.get( d ).setUpdateListener( updateListener );

		for ( int d = 0; d < 3; d++ )
			panel.add( sliderPanels.get( d ) );


		frame.setContentPane( panel );
		frame.setBounds(
				MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				120, 10);
		frame.setResizable( false );
		frame.pack();
		frame.setVisible( true );
		if ( dialogLocation != null )
			frame.setLocation( dialogLocation );
	}


}