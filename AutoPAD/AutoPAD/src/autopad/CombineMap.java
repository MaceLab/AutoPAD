package autopad;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class CombineMap {
	private int combineid =0;
	final private int maxnode = 1248;
	private int[] nodeid = new int[maxnode];
	private int[] nodelayer = new int[maxnode];
	private boolean[] nodeflipx = new boolean[maxnode];
	private boolean[] nodeflipy = new boolean[maxnode];
	private int[] noderotate = new int[maxnode]; // 0 = 0, 1 = 90, 2 = 180, 3 = 270
	private int[] nodedir = new int[maxnode]; // 0 = up, 1 = right, 2 = down, 3 = left
	private int[] nodealign = new int[maxnode]; // 0 = center, 1 = left, 2 = right
	private int[] nodesource = new int[maxnode]; // which node are you from?
	private int nodecount = 0;
	private int lowest_layer = 0;
	private int highest_layer = 0;
	private boolean squarespace=false;
	private boolean drawboxes=false;
	
	private int lastid = -1;
	
	private boolean bufferbox=false;
	private int bufferbox_x = 0;
	private int bufferbox_y =0;
	
	private boolean forcesamecenter = false; // if enabled, forces folding alignment
	private int centerx=0;
	private int centery=0;
	
	private boolean dopdf=false;
	private int pdfpagestyle=-1;
	private int pdfmarginx=36;
	private int pdfmarginy=36;
	private int pdfforcesizex=0;
	private int pdfforcesizey=0;
	private double pdfscale=1.0/4.167;
	
	private String targetpath="";
	
	public CombineMap(int id, String intargetpath){
		combineid=id;
		targetpath=intargetpath;
	}
	public void conferPDFSettings(boolean indopdf, int inpagestyle, int inmarginx, int inmarginy, int infx, int infy, double inpdfscale){
		dopdf=indopdf;
		pdfpagestyle=inpagestyle;
		pdfmarginx=inmarginx;
		pdfmarginy=inmarginy;
		pdfforcesizex=infx;
		pdfforcesizey=infy;
		pdfscale=inpdfscale;
	}
	public void setBufferBox(int x, int y){
		bufferbox_x=x;
		bufferbox_y=y;
		bufferbox=true;
	}
	public void addNode(int id){
		nodeid[nodecount]=id;
		nodesource[id]=-1;
		lastid=id;
		nodecount++;
	}
	public int getLastId(){
		return lastid;
	}
	public void setLayer(int id, int layer){
		nodelayer[id]=layer;
		if(layer > highest_layer || nodecount == 1){ highest_layer = layer; }
		if(layer < lowest_layer || nodecount == 1){ lowest_layer = layer; }
	}
	public void setDir(int id, int dir, int source){
		nodesource[id]=source;
		nodedir[id]=dir;
	}
	public void setRotate(int id, int dir){
		noderotate[id]=dir;
	}
	public void setFlipX(int id, boolean flip){
		nodeflipx[id]=flip;
	}
	public void setFlipY(int id, boolean flip){
		nodeflipy[id]=flip;
	}
	public void setAlign(int id, int align){
		nodealign[id]=align; // TODO not fully implemented
	}
	public void setSquareSpace(){
		squarespace=true;
	}
	public void setDrawBoxes(){
		drawboxes=true;
	}
	public void setForceSameCenter(int x, int y){
		forcesamecenter=true;
		centerx=x;
		centery=y;
	}
	
	//
	
	public double getValueFromString(int inid, String textid){
		System.out.println(" TEXTID " + textid);
		if(textid.length()>=6){
			if(textid.substring(0,6).equalsIgnoreCase("SOURCE")){
				return (double)(nodesource[inid]+20000);
			}
		}
		if(textid.length()>=3){
			if(textid.substring(0,3).equalsIgnoreCase("DIR")){
				return (double)(nodedir[inid]);
			}
		}
		if(textid.length()>=6){
			if(textid.substring(0,6).equalsIgnoreCase("ROTATE")){
				return (double)(noderotate[inid]);
			}
		}
		if(textid.length()>=5){
			if(textid.substring(0,5).equalsIgnoreCase("LAYER")){
				return (double)(nodelayer[inid]);
			}
		}
		
		
		return -1;
	}
	
	//
	
	public void compile(String fillname, String combinename){
		// first, calculate some PDF parameters
		PDRectangle pdfstyle = PDPage.PAGE_SIZE_LETTER;
		if(dopdf){
			if(pdfpagestyle==-2){
				pdfstyle = new PDRectangle(pdfforcesizex,pdfforcesizey);
			}else if(pdfpagestyle==0){
				pdfstyle = PDPage.PAGE_SIZE_A0;
			}else if(pdfpagestyle==1){
				pdfstyle = PDPage.PAGE_SIZE_A1;
			}else if(pdfpagestyle==2){
				pdfstyle = PDPage.PAGE_SIZE_A2;
			}else if(pdfpagestyle==3){
				pdfstyle = PDPage.PAGE_SIZE_A3;
			}else if(pdfpagestyle==4){
				pdfstyle = PDPage.PAGE_SIZE_A4;
			}else if(pdfpagestyle==5){
				pdfstyle = PDPage.PAGE_SIZE_A5;
			}else if(pdfpagestyle==6){
				pdfstyle = PDPage.PAGE_SIZE_A6;
			}
		}
		
		System.out.println(" COMPILING " + fillname + "_" + combinename);
		// first determine total size
		int total_x = 0;
		int total_y = 0;
		int totallayer = Math.abs(lowest_layer) + Math.abs(highest_layer);
		int minlayer = Math.abs(lowest_layer);
		int[] layerx = new int[totallayer+1];
		int[] layery = new int[totallayer+1];
		int[] reallayerx = new int[totallayer+1];
		int[] reallayery = new int[totallayer+1];
		int maxx =0;
		int maxy =0;
		int i =0;
		while(i<nodecount){
			int nid = nodeid[i];
			try{
				BufferedImage timage = ImageIO.read(new File("./out/" + fillname + "_" + nodelayer[nid] + ".png"));
				layerx[nodelayer[nid]+minlayer]=timage.getWidth();
				layery[nodelayer[nid]+minlayer]=timage.getHeight();
				reallayerx[nodelayer[nid]+minlayer]=timage.getWidth();
				reallayery[nodelayer[nid]+minlayer]=timage.getHeight();
				if(layerx[nodelayer[nid]]>maxx){
					maxx=layerx[nodelayer[nid]];
				}
				if(layery[nodelayer[nid]]>maxy){
					maxy=layery[nodelayer[nid]];
				}
			}catch(Exception e){
				System.out.println(" COULDN'T LOAD LAYER " + nodelayer[nid]);
				return;
			}
			i++;
		}
		int max = maxx;
		if(maxy>maxx){
			max=maxy;
		}// TODO: add bufferbox to max instead of taking away from current size
		
		if(squarespace){
			if(forcesamecenter){
				max+= Math.max(Math.max(centerx,centery),Math.max(Math.abs(max-centerx),Math.abs(max-centery)));
			}
			i=0;
			while(i<nodecount){
				int nid = nodeid[i];
				layerx[nodelayer[nid]]=max;
				layery[nodelayer[nid]]=max;
				i++;
			}
		}
		
		
		
		
		int negx =1000;
		int posx =-1000;
		int negy =1000;
		int posy =-1000;
		int nodeposx[] = new int[maxnode];
		int nodeposy[] = new int[maxnode];
		
		i=0;
		while(i<nodecount){
			int nid = nodeid[i];
			int[] pos = pathToNode(nid,layerx,layery,minlayer);
			nodeposx[nid]=pos[0];
			nodeposy[nid]=pos[1];
			int postestx = pos[0];
			int postesty = pos[1];
			if(noderotate[nid]== 1 || noderotate[nid]==3){
				postestx+=layery[minlayer+nodelayer[nid]];
				postesty+=layerx[minlayer+nodelayer[nid]];
			}else{
				postestx+=layerx[minlayer+nodelayer[nid]];
				postesty+=layery[minlayer+nodelayer[nid]];
			}
			if(postestx>posx){
				posx=postestx;
			}
			if(pos[0]<negx){
				negx=pos[0];
			}
			if(postesty>posy){
				posy=postesty;
			}
			if(pos[1]<negy){
				negy=pos[1];
			}
			i++;
		}
		total_x = posx-negx;
		total_y = posy-negy;
		if(drawboxes){
			total_x++;
			total_y++;
		}
		
		
		
		
		
		BufferedImage combineimage = new BufferedImage(total_x, total_y, BufferedImage.TYPE_INT_RGB);
		int[][] noderaster = new int[total_x][total_y];
		int[] white = new int[3];
		white[0]=255; white[1]=255; white[2]=255;
		Interpreter.drawRectangle(combineimage.getRaster(),0,0,combineimage.getWidth(),combineimage.getHeight(),
				white, noderaster, -1);
		int[] black = new int[3];
		black[0]=0; black[1]=0; black[2]=0;
		// scout out positions of each node
		
		
		i=0;
		while(i<nodecount){
			int nid= nodeid[i];
			nodeposx[nid]+=Math.abs(negx);
			nodeposy[nid]+=Math.abs(negy);
			i++;
		}
		
		int[] adjust_x = new int[nodecount];
		int[] adjust_y = new int[nodecount];
		if(squarespace && forcesamecenter){
			i=0;
			while(i<nodecount){
				int nid=nodeid[i];
				try{
					//BufferedImage timage = ImageIO.read(new File("./out/" + fillname + "_" + nodelayer[nid] + ".png"));
					
					int realx = nodeposx[nid]+centerx;
					int realy = nodeposy[nid]+centery;
					int signx = 1;
					int signy = 1;
					if(nodeflipx[nid] || noderotate[nid]==2){
						realx = nodeposx[nid]+max-centerx;
						//signx=-1;
					}
					if(nodeflipy[nid] || noderotate[nid]==2){
						realy = nodeposy[nid]+max-centery;
					}
					if(noderotate[nid]==1){
						realx = nodeposx[nid]+centery;
						realy = nodeposy[nid]+max-centerx;
						//signy=-1;
						if(nodeflipx[nid]){
							realx = nodeposx[nid]+max-centery;
						}
						if(nodeflipy[nid]){
							realy = nodeposy[nid]+centerx;
						}
					}
					if(noderotate[nid]==3){
						realx = nodeposx[nid]+centery;
						realy = nodeposy[nid]+centerx;
						if(nodeflipx[nid]){
							realx = nodeposx[nid]+max-centery;
						}
						if(nodeflipy[nid]){
							realy = nodeposy[nid]+max-centerx;
						}
					}
					
					adjust_x[i]=(realx-nodeposx[nid])*signx;
					adjust_y[i]=(realy-nodeposy[nid])*signy;
					
				}catch(Exception e){
					System.out.println("Failed to load image");
					return;
				}
				i++;
			}
		}
		
		if(bufferbox && squarespace){
			int m =0;
			while(m < total_x){
				int n =0;
				while(n < total_y){
					Interpreter.drawRectangle(combineimage.getRaster(),m+bufferbox_x,n+bufferbox_y,
							max-bufferbox_x*2,max-bufferbox_y*2,black, noderaster, -1);
					n+=max;
				}
				m+=max;
			}
		}
		// now draw all of these
		int uniformdistx =0;
		int uniformdisty =0;
		if(forcesamecenter){
			uniformdistx = max/2 - centerx;
			uniformdisty = max/2 - centery;
		}
		i=0;
		while(i<nodecount){
			int nid = nodeid[i];
			int wid = layerx[minlayer+nodelayer[nid]];
			int hei = layery[minlayer+nodelayer[nid]];
			if(noderotate[nid]==1 || noderotate[nid]==3){
				int tempx = wid;
				wid=hei;
				hei=tempx;
			}

			try{
				BufferedImage timage = ImageIO.read(new File("./out/" + fillname + "_" + nodelayer[nid] + ".png"));
				int h =0;
				while(h<timage.getWidth()){
					int j=0;
					while(j<timage.getHeight()){
						int realx = h+nodeposx[nid];
						int realy = j+nodeposy[nid];
						if(nodeflipx[nid] || noderotate[nid]==2){
							realx = nodeposx[nid]+wid-h;
						}
						if(nodeflipy[nid] || noderotate[nid]==2){
							realy = nodeposy[nid]+hei-j;
						}
						if(noderotate[nid]==1){
							realx = nodeposx[nid]+j;
							realy = nodeposy[nid]-h+hei;
							if(nodeflipx[nid]){
								realx = nodeposx[nid]+wid-j;
							}
							if(nodeflipy[nid]){
								realy = nodeposy[nid]+h;
							}
						}
						if(noderotate[nid]==3){
							realx = nodeposx[nid]+j;
							realy = nodeposy[nid]+h;
							if(nodeflipx[nid]){
								realx = nodeposx[nid]+wid-j;
								
							}
							if(nodeflipy[nid]){
								realy = nodeposy[nid]+hei-h;
							}
						}
						int[] col = timage.getRaster().getPixel(h,j,new int[3]);
						if(squarespace && !forcesamecenter){
							// center 
							int thei = timage.getHeight();
							int twid = timage.getWidth();
							if(noderotate[nid]==1 || noderotate[nid]==3){
								int temphei = thei;
								thei=twid;
								twid=temphei;
							}
							if(twid<max){
								realx+=(max-twid)/2;
							}
							if(thei<max){
								if(noderotate[nid]==1){
									realy-=(max-thei)/2;
								}else{
									realy+=(max-thei)/2;
								}
							}
						}
						// adjust_x, adjust_y is the center
						// when h,j = centerx, centery, output realx,realy should = adjust_x,adjust_y

						realx+=centerx;
						realy+=centery;
						realx-=adjust_x[i];
						realy-=adjust_y[i];

						// push centerx,centery into the actual center
						realx+=uniformdistx;
						realy+=uniformdisty;
						if(realx>=0 && realy>=0 && realx<total_x && realy<total_y){
							combineimage.getRaster().setPixel(realx,realy,col);
						}
						j++;
					}
					h++;
				}
			}catch(Exception e){e.printStackTrace();return;}
			i++;
		}
		/*i=0; // debug
		while(i<nodecount){
			int nid = nodeid[i];
			System.out.println(" X " + nodeposx[nid] + " Y " + nodeposy[nid]);
			combineimage.getRaster().setPixel(nodeposx[nid],nodeposy[nid],new int[]{255,0,0});
			i++;
		}*/
		
		if(drawboxes){
			i=0;
			while(i<nodecount){
				int nid = nodeid[i];
				int hei = layery[nodelayer[nid]];
				int wid = layerx[nodelayer[nid]];
				if(noderotate[nid]>0){
					int temp = hei;
					hei=wid;
					wid=temp;
				}
				Interpreter.drawRectAngleOutline(combineimage.getRaster(), nodeposx[nid], nodeposy[nid] + hei/2,
						wid, hei, new int[]{0,0,0}, 0, 0, -layery[nodelayer[nid]]/2, noderaster, -1); 
				i++;
			}
		}
		try{
			ImageIO.write(combineimage, "PNG", new File(targetpath + fillname + combinename + "_" + combineid + ".png"));
		}catch(Exception e){
			System.out.println("!!! Failed to output combine map");
			return;
		}
		if(dopdf){
			PDFExport.makePDF(targetpath + fillname + combinename + "_" + combineid + ".pdf",
					targetpath + fillname + combinename + "_" + combineid + ".png",pdfstyle,pdfmarginx,pdfmarginy,pdfscale);
		}
	}
	
	
	public int[] pathToNode(int id, int[] layerx, int[] layery, int minlayer){
		int[] out = new int[2];
		out[0]=0;
		out[1]=0;
		int curnode=id;
		if(curnode == nodesource[curnode]){
			System.out.println(" !!! ERROR: The source of this combine node is itself!");
			System.exit(1);
		}
		while(nodesource[curnode]!=-1){
			int addx = layerx[minlayer+nodelayer[curnode]];
			int addy = layery[minlayer+nodelayer[curnode]];
			if(noderotate[curnode]==1 || noderotate[curnode]==3){
				int tempx = addx;
				addx=addy;
				addy=tempx;
			}
			int nextnode = nodesource[curnode];
			int addx2 = layerx[minlayer+nodelayer[nextnode]];
			int addy2 = layery[minlayer+nodelayer[nextnode]];
			if(noderotate[nextnode]==1 || noderotate[nextnode]==3){
				int tempx = addx2;
				addx2=addy2;
				addy2=tempx;
			}
			// TODO: somehow handle this better (alignment sometimes doesn't function for non-center alignments
			if(nodedir[curnode]==0){
				//up
				out[1]+=addy/2;
				out[1]+= addy2/2;
				if(nodealign[curnode]==1){
					out[0]+=addx2/4;
				}else if(nodealign[curnode]==2){
					out[0]-=addx2/4;
				}
			}else if(nodedir[curnode]==1){
				//right
				out[0]-=addx/2;
				out[0]-= addx2/2;
				if(nodealign[curnode]==1){
					out[0]-=addy2/4;
				}else if(nodealign[curnode]==2){
					out[0]+=addy2/4;
				}
			}else if(nodedir[curnode]==2){
				//down
				out[1]-=addy/2;
				out[1]-= addy2/2;
				if(nodealign[curnode]==1){
					out[0]-=addx2/4;
				}else if(nodealign[curnode]==2){
					out[0]+=addx2/4;
				}
			}else if(nodedir[curnode]==3){
				//left
				out[0]+=addx/2;
				out[0]+= addx2/2;
				if(nodealign[curnode]==1){
					out[0]+=addy2/4;
				}else if(nodealign[curnode]==2){
					out[0]-=addy2/4;
				}
			}
			
			
			curnode=nodesource[curnode];
		}
		
		// now we have center x,y, we want top-left
		if(noderotate[id]==1 || noderotate[id]==3){
			out[0]-=layery[minlayer+nodelayer[id]]/2;
			out[1]-=layerx[minlayer+nodelayer[id]]/2;
		}else{
			out[0]-=layerx[minlayer+nodelayer[id]]/2;
			out[1]-=layery[minlayer+nodelayer[id]]/2;
		}
		return out;
	}
	
	
}
