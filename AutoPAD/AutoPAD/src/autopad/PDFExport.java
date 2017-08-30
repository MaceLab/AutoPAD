package autopad;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.ConvertColorspace;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;

/*
 * observations:
 * 	- 0,0 is at the bottom-left corner of the pdf file
 *  - the pdf is 612 x 792 pixels   612,792
 *  - which corresponds to 8.5 by 11 inches
 *  - or 21.59 by 27.94 cm
 *  - ergo every 72 pixels = 1 in 
 *  - and every 28.35 pixels = 1 cm
 *  SO: inch scale should be fill 72
 *  	cm scale should be fill 28.35
 *  and then we just draw directly into pdfs.
 *  
 *  For non-lettersized paper, the pixel/cm/in conversions are the same, but the max pixels is different
 */

public class PDFExport {
	
		// example:
		//makePDF("./here.pdf","./this.png",PDPage.PAGE_SIZE_LETTER,15,15,(1.0/8.0));
		// PAGE_SIZE_A# where # is 0-6 and PAGE_SIZE_LETTER are valid

	public static double scale2in(double infillsize){
		// ex if we generated the image at 30 and now we want it to be at inch scale
		// we need to multiply by 72/30
		return 72.0/infillsize;
	}
	public static double scale2cm(double infillsize){
		// same logic as scale2in
		return 28.35/infillsize;
	}
	public static void makePDF(String filepath, String imagepath, PDRectangle pagestyle, int margx, int margy, double scale){
		int psizex = (int)(pagestyle.getWidth());
		int psizey = (int)(pagestyle.getHeight());
		
		PDDocument doc = null;
		try{
			doc = new PDDocument();
			PDPage page = new PDPage(pagestyle);
			
			doc.addPage(page);
			
			PDXObjectImage ximg = null;
			BufferedImage img = null;
			try{
				img = ImageIO.read(new File(imagepath));
				ximg= new PDPixelMap(doc,img);//new PDJpeg(doc,img);
				// need to use PDPixelMap over PDJpeg as the jpeg format results in quality losses
			}catch(IOException e){
				e.printStackTrace();
			}
			PDPageContentStream cstream =null;
			try{
				cstream = new PDPageContentStream(doc,page);
				int x = margx;
				while(x<psizex){
					int y=margy;
					while(y<psizey){
						if(x+(int)(img.getWidth()*scale)<psizex-margx && y+(int)(img.getHeight()*scale)<psizey-margy){
							cstream.drawXObject(ximg,x,y,(int)(img.getWidth()*scale),(int)(img.getHeight()*scale));
							//System.out.println(" ( " + x + " , " + y + " )");
						}
						y+=(int)(img.getHeight()*scale);
					}
					x+=(int)(img.getWidth()*scale);
				}
				cstream.close();
				
				doc.save(filepath);
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}finally{
			if(doc!=null){
				try{
					doc.close();
					System.out.println(" Saved .PDF at " + filepath);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

}
