package de.embl.cba.bdp2.open.fileseries;

import de.embl.cba.bdp2.log.Logger;

import java.io.RandomAccessFile;

public class TiffRowsRawReader
{
	private int stripMin;
	private int stripMax;
	private int minRow;
	private int numRows;
	private static final boolean test = false;
	private boolean hasStrips;
	private boolean isCompressed;
	private int minCol;
	private int numCols;
	private int maxRow;

	/**
	 * TODO: Is RandomAccessFile really the fastest here?
	 * @return
	 */
	public byte[] read( BDP2FileInfo fi, RandomAccessFile in, int minRowRequested, int numRowsRequested, int minColRequested, int numColsRequested )
	{
		int numBytesToRead;
		long readStart;
		this.minRow = minRowRequested;
		this.numRows = numRowsRequested;
		this.maxRow = ( minRowRequested + numRowsRequested -  1 );
		this.minCol = minColRequested;
		this.numCols = numColsRequested;

		final int bytesPerRow = fi.width * fi.bytesPerPixel;
		hasStrips = fi.stripOffsets != null && fi.stripOffsets.length > 1;
		isCompressed = ( fi.compression == TiffDecompressor.ZIP || fi.compression == TiffDecompressor.LZW || fi.compression == TiffDecompressor.PACK_BITS );

		if ( hasStrips )
		{
			// Convert rows to corrsponding strips
			stripMin = ( int ) ( 1.0 * minRow / fi.rowsPerStrip );
			stripMax = ( int ) ( 1.0 * maxRow / fi.rowsPerStrip );

			// Adapt to how many rows and columns actually have been reading.
			// Strips can contain multiple rows.
			// Read the whole strip, as it could be compressed.
			minRow = stripMin * fi.rowsPerStrip;
			maxRow = stripMax * fi.rowsPerStrip;
			numRows = maxRow - minRow + 1;
			minCol = 0;
			numCols = fi.width;

			readStart = fi.stripOffsets[ stripMin ];

			numBytesToRead = 0;
			for ( int s = stripMin; s <= stripMax; s++ )
				numBytesToRead += fi.stripLengths[ s ];
		}
		else // no strips (or all data in one compressed strip)
		{
			if ( isCompressed )
			{
				// read the whole plane
				readStart = fi.offset;
				numBytesToRead = ( int ) fi.stripLengths[ 0 ];

				minRow = 0;
				maxRow = fi.height - 1;
				numRows = fi.height;
				minCol = 0;
				numCols = fi.width;

			}
			else // no compression, no strips
			{
				// read only the actually requested pixels
				readStart = fi.offset + (long) minRow * bytesPerRow + minCol * fi.bytesPerPixel;
				numBytesToRead = numRows * numCols * fi.bytesPerPixel;
			}
		}


		byte[] buffer = new byte[ numBytesToRead ];

		try
		{
			if ( numBytesToRead <= 0 ||  readStart + numBytesToRead > in.length() )
			{
				Logger.warn( "hasStrips: " + hasStrips );
				Logger.warn( "file length [bytes]: " + in.length() );
				Logger.warn( "attempt to read until [bytes]: " + ( readStart + numBytesToRead - 1 ) );
				Logger.warn( "minRowRequested: " + minRowRequested );
				Logger.warn( "numRowsRequested: " + numRowsRequested );
				Logger.warn( "fileInfo.compression: " + fi.compression );
				Logger.warn( "fileInfo.height: " + fi.height );
				Logger.error( "Error during file reading. See log window for more information" );
				throw new RuntimeException( "Error during reading of Tiff plane." );
			}

			if ( minCol > 0 || numCols < fi.width ) // read column subset
			{
				long posInFile = readStart;
				int posInBuffer = 0;
				final int numBytesToReadPerRow = numCols * fi.bytesPerPixel;
				for ( int row = minRow; row < maxRow; row++ )
				{
					in.seek( posInFile );
					posInBuffer += in.read( buffer, posInBuffer, numBytesToReadPerRow );
					posInFile += bytesPerRow;
				}
			}
			else // read all rows in one go
			{
				in.seek( readStart );
				in.readFully( buffer );
			}
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}

		return buffer;
	}

	public int getStripMin()
	{
		return stripMin;
	}

	public int getStripMax()
	{
		return stripMax;
	}

	public boolean hasStrips()
	{
		return hasStrips;
	}

	public boolean isCompressed()
	{
		return isCompressed;
	}

	public int getMinRow()
	{
		return minRow;
	}

	public int getNumRows()
	{
		return numRows;
	}

	public int getMinCol()
	{
		return minCol;
	}

	public int getNumCols()
	{
		return numCols;
	}

	public int getMaxRow()
	{
		return maxRow;
	}
}
