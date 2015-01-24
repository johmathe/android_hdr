package edu.stanford.ee368.hdrserver;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Hashtable;

import javax.imageio.ImageIO;

/**
 * various utilities for buffered creation and decoding
 * @author Tim Wong
 *
 */
public class BufferedImageUtils 
{
    private static ColorSpace grayColorSpace = ColorSpace.getInstance (ColorSpace.CS_GRAY);    

    /**
     * create BufferedImage from data buffer
     * @param dataBuffer 
     * @param width
     * @param height
     * @param bitsPerPixel
     * @return
     */
    public static BufferedImage createBufferedImage (DataBuffer dataBuffer,
            int width,
            int height,
            int bitsPerPixel)
    {
        ColorModel cm = createGrayColorModel(dataBuffer, bitsPerPixel);
        SampleModel sm = createSampleModel(cm, width, height);
        WritableRaster raster = createWritableRaster(sm, dataBuffer);
        BufferedImage image = createBufferedImage(cm, raster);
        return image;
    }

    /**
     * create gray scale buffered image from byte data array
     * @param byteData
     * @param width
     * @param height
     * @return Buffered image
     */
    public static BufferedImage createBufferedImage ( byte[] byteData,
            int width, int height )
    {
        DataBuffer buffer = new DataBufferByte( byteData, width * height );
        return createBufferedImage( buffer, width, height, 8 );    	
    }
    
    /**
     * Read the file and return it as gray scale buffered image     
     * @param filePath
     * @return image as buffered image
     * @throws Exception
     */
    public static BufferedImage decodeForGrayBufferedImage( String filePath ) throws Exception
    {
        BufferedImage bi = null;
		try
		{
			File file = new File( filePath );
			RenderedImage im = ImageIO.read( file );
            Dimension imageSize = new Dimension(im.getWidth(), im.getHeight());
            bi = new BufferedImage(imageSize.width, imageSize.height,
                                                 BufferedImage.TYPE_BYTE_GRAY);
            drawImage(bi, im);			
		}
		catch ( Exception e )
		{
			System.out.println("cannot read file " + filePath + " as gray buffered image " );
		}       
        return bi;
    }

    /**
     * Read the file and return it as RGB buffered image     
     * @param filePath
     * @return image as buffered image
     * @throws Exception
     */
    public static BufferedImage decodeForRGBBufferedImage(String filePath) throws Exception
    {
        BufferedImage bi = null;
		try
		{
			File file = new File( filePath );
			RenderedImage im = ImageIO.read( file );
            Dimension imageSize = new Dimension(im.getWidth(), im.getHeight());
            bi = new BufferedImage(imageSize.width, imageSize.height,
                                                 BufferedImage.TYPE_INT_ARGB );
            drawImage(bi, im);			
		}
		catch ( Exception e )
		{
			System.out.println("cannot read file:  " + filePath + " as RGB buffered image");
		}       
        return bi;
    }
    
    
    // This method is made synchronized to avoid threading problems in ColorSpace
    // and ColorModel classes. Please refer to JDK bug no. 4863795 for more
    // details.
    private synchronized static ColorModel createGrayColorModel (DataBuffer dataBuffer,
                                                int bitsPerPixel)
    {
        boolean hasAlpha = false;
        return new ComponentColorModel (grayColorSpace,
                                        new int[]{bitsPerPixel},
                                        hasAlpha,
                                        hasAlpha,
                                        Transparency.TRANSLUCENT,
                                        dataBuffer.getDataType());
    }

    private static SampleModel createSampleModel (ColorModel cm,
            int width,
            int height)
    {
        return cm.createCompatibleSampleModel(width, height);
    }
    
    private static BufferedImage createBufferedImage (ColorModel cm,
            WritableRaster raster)
    {
        boolean isRasterPremultiplied = true;
        Hashtable properties = null;
        return new BufferedImage(cm, raster, isRasterPremultiplied, properties);
    }
    
    private static WritableRaster createWritableRaster (SampleModel sm,
                  DataBuffer dataBuffer)
    {
        Point origin = new Point(0,0);
        return Raster.createWritableRaster(sm, dataBuffer, origin);
    }

    //Draw the rendered image into the buffered image
    private static void drawImage(BufferedImage bi, RenderedImage im)
    {
        Graphics2D g = bi.createGraphics();
        g.drawRenderedImage(im, new AffineTransform());
        g.dispose();
    }
    
}
