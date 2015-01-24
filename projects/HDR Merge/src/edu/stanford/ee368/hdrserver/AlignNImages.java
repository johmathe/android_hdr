package edu.stanford.ee368.hdrserver;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import jjil.core.Error;
import jjil.core.RgbImage;
import jjil.j2se.RgbImageJ2se;

/**
 * align N images and return the aligned image a RgbImages array
 * @author Tim Wong
 *
 */
public class AlignNImages 
{
	private static final int NUM_SEGMENTS_X = 7;
	private static final int NUM_SEGMENTS_Y = 6;
	private static final int HIST_DOWN_SAMPLE = 8;
	private static final int N_LAYERS = 6;
	private String []hdrFileNames;
	private BufferedImage[] unalignedImages;
	private int referenceImgIndex = 0;
		
	// constructor, read N images from file
	public AlignNImages( String []fileNames ) throws Exception
	{
		hdrFileNames = (String [])fileNames.clone();		
		int nImages = fileNames.length;
		int [][]histograms = new int[nImages][];
		unalignedImages = new BufferedImage[nImages];
		for ( int i = 0; i < nImages; i++ )
		{
			try
			{
				// read images as gray scale image, don't read as RGB since it take up too much memory
				unalignedImages[i] = BufferedImageUtils.decodeForGrayBufferedImage( fileNames[i] );
				histograms[i] = getDownSampledHistogram( unalignedImages[i], HIST_DOWN_SAMPLE );
			}
			catch ( Exception e )
			{
				System.out.println( "Alignment failed, cannot read " + fileNames[i] );
				throw e;
			}
		}			
		
		// find the image with the max gray level spread 
		int maxEPD = 0; // max end point different
		for ( int i = 0; i < nImages; i++ )
		{
			// no need to do full resolution histogram, down sample it 
			int nPixelsHisto = (int)( unalignedImages[i].getHeight() / HIST_DOWN_SAMPLE ) *
							   (int)( unalignedImages[i].getWidth() / HIST_DOWN_SAMPLE );
			int threshold = nPixelsHisto /20; // get 5% threshold point
			int sum = 0;
			int m;
			// search for low end point (LEP )
			for ( m = 0; m < histograms[i].length; m++ )
			{
				sum += histograms[i][m];
				if ( sum > threshold )
					break;
			}
			int lep = m; // Low end point on the histogram
			threshold = nPixelsHisto - threshold; // high end threshold 
			for ( ; m < histograms[i].length; m++ )
			{
				sum += histograms[i][m];
				if ( sum > threshold )
					break;				
			}
			int hep = m; // high end point on the histogram
			if (( hep - lep ) > maxEPD )
			{
				maxEPD = hep - lep;
				referenceImgIndex = i;
			}
		}
		log( "reference image = " + hdrFileNames[referenceImgIndex] );	
	}
	
	/**
	 * Align the images and return the aligned images RgbImage
	 * @return RgbImage array
	 * @throws Exception
	 * @throws Error
	 */
	public RgbImage[] alignImages() throws Exception, Error
	{
		if ( unalignedImages == null )
		{
			log( "no images in buffer ");
			return null;
		}
		int nImages = unalignedImages.length; 
		if ( nImages < 2 )
		{
			log( " no image under test " );
			return null;			
		}
		RgbImage [] alignedImages = new RgbImage[nImages];
		AffineTransform [] affines = new AffineTransform[nImages];
		int xDim = unalignedImages[0].getWidth();
		int yDim = unalignedImages[0].getHeight();
        DataBufferByte dataBufferRef = (DataBufferByte)unalignedImages[referenceImgIndex].getRaster().getDataBuffer();
		ImgAlignment aligner = new ImgAlignment( dataBufferRef.getData(), xDim, yDim, N_LAYERS);

		// find the image bound
		aligner.nSegmentX = NUM_SEGMENTS_X;
		aligner.nSegmentY = NUM_SEGMENTS_Y;
		int left = 0;
		int right = xDim -1;
		int top = 0;
		int bot = yDim - 1;
		Point2D [] ptsSrc = new Point2D[4]; 
		Point2D [] ptsDst = new Point2D[4]; 
		ptsSrc[0] = new Point2D.Double(left,  top);
		ptsSrc[1] = new Point2D.Double(right, top);
		ptsSrc[2] = new Point2D.Double(left,  bot);
		ptsSrc[3] = new Point2D.Double(right, bot);
		
		for ( int i = 0; i < nImages; i++ )
		{			
			if ( i != referenceImgIndex )
			{
		        DataBufferByte dataBufferSrc = (DataBufferByte)unalignedImages[i].getRaster().getDataBuffer();
		    	affines[i] = aligner.alignImages( dataBufferSrc.getData());
		    	affines[i].createInverse().transform(ptsSrc, 0, ptsDst, 0, 4 );
		    	// get bounds
		    	left = Math.max( left, (int)ptsDst[0].getX());
		    	left = Math.max( left, (int)ptsDst[2].getX());
		    	right = Math.min( right, (int)ptsDst[1].getX());
		    	right = Math.min( right, (int)ptsDst[3].getX());
		    	top = Math.max( top, (int)ptsDst[0].getY());
		    	top = Math.max( top, (int)ptsDst[1].getY());
		    	bot = Math.min( bot, (int)ptsDst[2].getY());
		    	bot = Math.min( bot, (int)ptsDst[3].getY());
		    	log( affines[i].toString());
			}
			else
			{
				affines[i] = new AffineTransform();
			}
		}
		
		int w = right - left;
		int h = bot - top;
		// now read the images as RGB and do the alignment and cut get the bounded region
		for ( int i = 0; i < nImages; i++ )
		{			
	    	try
	    	{
	    		BufferedImage bufferImg =BufferedImageUtils.decodeForRGBBufferedImage(hdrFileNames[i]);
	    		alignedImages[i]= performAffineTransform( bufferImg, affines[i], left, top, w, h );
	    		if ( aligner.LOG_DETAILS )
	    		{
	    			// log the three patch images to file
		    		for ( int k = 0; k < 3; k++ )
		    		{
		    			BufferedImage bi = unalignedImages[i].getSubimage( aligner.rects[k].x, aligner.rects[k].y,
		    				aligner.rects[k].width, aligner.rects[k].height);
						RgbImageJ2se j2sea = new RgbImageJ2se();	    			
						RgbImage patchImg = j2sea.toRgbImage( bi);					
						j2sea.toFile( patchImg, "D:\\Pictures\\patch_" + i + "_" + k + ".jpg" );
		    		}
	    		}
	    		
	    	}
	    	catch ( Exception e )
	    	{
	    		alignedImages[i]= null;
	    	}
		}
		return alignedImages;
	}
	
	/**
	 * perform affine transformation
	 * @param img - image to transform
	 * @param affine - affine matrix
	 * @param x - starting X coordinate
	 * @param y - starting Y coordinate
	 * @param width - width of sub region
	 * @param height - height of sub region
	 * @return
	 * @throws Exception
	 */
	private RgbImage performAffineTransform( BufferedImage img, AffineTransform affine, 
			int x, int y, int width, int height ) throws Exception
	{
        long t1 =System.currentTimeMillis();
		AffineTransform invAffine = affine.createInverse();
        AffineTransformOp op = new AffineTransformOp( invAffine, AffineTransformOp.TYPE_BILINEAR );
        BufferedImage dstBuffer = new BufferedImage( img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB );
        op.filter( img, dstBuffer );
        BufferedImage bi = dstBuffer.getSubimage(x, y, width, height );
		RgbImage returnImg = RgbImageJ2se.toRgbImage( bi);
        long t4 =System.currentTimeMillis();
        log( "transformation time = " + (t4-t1));
        return returnImg;
	}	
	
	private int[] getDownSampledHistogram( BufferedImage img, int downSample )
	{
        DataBufferByte dataBuffer = (DataBufferByte)img.getRaster().getDataBuffer();
    	byte data[] = dataBuffer.getData();
    	int [] histogram = new int[256];
    	int w = img.getWidth();
    	int h = img.getHeight();
    	for ( int y = 0; y < h; y+= downSample)
    	{
    		for ( int x = 0; x < w; x+= downSample )
    		{
    			int d = data[y*w + x];
    			if ( d < 0 ) d += 256;
    			histogram[d]++;
    		}
    	}
		return histogram;
	}
	
	private void log( String str )
	{
		if (ImgAlignment.LOG_DETAILS )
		{
			System.out.println( str );
		}
	}
}
