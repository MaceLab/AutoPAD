package autopad;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


// the purpose of the map boss is to hold the final list of nodes and the node id raster 
// so that the preview class can work at finding the nodes that correspond to different parts
// of the output images. The interpreter passes this the final list of nodes and the final node id
// raster which is an int[][] map of the image where each pixel corresponds to a node id rather than a color value.
public class MapBoss {
	private MapNode[] nodes;
	private int nodecount = 0;
	private int[][][] layerraster;
	private boolean layerrastermade=false;
	private int layers=0;
	private int[] layertoz;
	private boolean[] layerused;
	private String[] imagepath;
	
	public MapBoss(){
		layers=-1; // uninitialized
	}
	public MapBoss(int inz, int[] inltz){
		layers=inz;
		layertoz=inltz;
	}
	// getLayers(): simply returns the number of layers}
	public int getLayers(){
		return layers;
	}
	// getLayerFromZ(): returns the layer count id of a given z-layer
	// for instance, if the z-levels are -3,0,5, they would be stored as {-3,0,5} or
	// [0]=-3, [1]=0, [2]=5, and getLayerFromZ(5)=2
	// returns -1 if the layer is not in the list already
	public int getLayerFromZ(int iz){
		int i=0;
		while(i<layers){
			if(layertoz[i]==iz){
				return i;
			}
			i++;
		}
		return -1;
	}
	// getFreeLayer(): returns the first unused layer (or -1 if all are used)
	public int getFreeLayer(){
		int i=0;
		while(i<layers){
			if(!layerused[i]){
				return i;
			}
			i++;
		}
		return -1;
	}
	// setLayer(): fills the first free layer (if the layer does not already exist, otherwise updates the existing layer)
	// with a given map and imagepath.
	public void setLayer(int inz, int[][] raster, String inpath){
		if(!layerrastermade){
			//System.out.println(" MAKING LAYER RASTER IN MAPBOSS XLEN " + raster.length + " YLEN " + raster[0].length);
			layerraster = new int[raster.length][raster[0].length][layers];
			imagepath = new String[layers];
			layerused = new boolean[layers];
			int i=0;
			while(i<layers){
				layerused[i]=false;
				i++;
			}
			layerrastermade=true;
		}
		
		int test = getLayerFromZ(inz);
		if(test==-1){
			test=getFreeLayer();
			if(test==-1){
				System.out.println(" ERROR: No more free layers in preview map boss!");
				return;
			}
			layerused[test]=true;
			layertoz[test]=inz;
		}
		
		inz=test;
		
		// given a raster and a layer id, put into storage
		int i=0;
		while(i<raster.length){
			int o=0;
			while(o<raster[0].length){
				layerraster[i][o][inz]=raster[i][o];
				o++;
			}
			i++;
		}
		imagepath[inz]=inpath;
	}
	
	// setNodes(): fills the nodes with a given list of mapnodes
	public void setNodes(MapNode[] innodes, int innodecount){
		nodes=innodes;
		nodecount=innodecount;
	}
	
	// getNodeAt(): given an x,y,z position, returns the mapnode id of the node present there
	public int getNodeAt(int x, int y, int z){
		z = getLayerFromZ(z);
		if(x<0 || x>=layerraster.length || y<0 || y>=layerraster[0].length || z<0 || z>=layers){
			return -1;
		}
		return layerraster[x][y][z];
	}
	// getNode(): returns a mapnode, given a mapnode id
	public MapNode getNode(int id){
		if(id==-1){
			return nodes[0];
		}
		return nodes[id];
	}
	// makeSelectionMap(): given a z-layer, a node, and a color, makes an image of that layer with that node outlined
	public void makeSelectionMap(int inz, int node, int[] col){
		BufferedImage img;
		inz = getLayerFromZ(inz);
		if(inz==-1){
			System.out.println(" ERROR: tried to make selection map from null layer raster!");
			return;
		}
		//System.out.println(" MAKING IMAGE AT " + imagepath[inz]);
		try{
			img = ImageIO.read(new File(imagepath[inz]));
		}catch(IOException e){
			e.printStackTrace();
			return;//new BufferedImage(1, 1, 1);
		}
		WritableRaster raster=img.getRaster();
		//int[] col = new int[3];
		//col[0]=255;col[1]=0;col[2]=255;
		int i=0;
		while(i<layerraster.length){
			int o=0;
			while(o<layerraster[0].length){
				//if(layerraster[i][o][inz]==node){
				if(nodeAdjacent(i,o,inz,node) && layerraster[i][o][inz]!=node){
					raster.setPixel(i, o, col);
					if(i>0){
						raster.setPixel(i-1,o,col);
					}
					if(o>0){
						raster.setPixel(i,o-1,col);
					}
					if(i<layerraster.length-1){
						raster.setPixel(i+1,o,col);
					}
					if(o<layerraster[0].length-1){
						raster.setPixel(i,o+1,col);
					}
				}
				o++;
			}
			i++;
		}
		//System.out.println(" SAVING IMAGE AT " + imagepath[inz].replace(".png", "_prevselmap.png"));
		try{
			ImageIO.write(img, "PNG", new File(imagepath[inz].replace(".png", "_prevselmap.png")));
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		//return img;
	}
	// nodeAdjacent(): given an x,y,z position, returns true if the given node is adjacent to that position.
	private boolean nodeAdjacent(int x, int y, int z, int node){
		if(x>0){
			if(layerraster[x-1][y][z]==node){
				return true;
			}
		}
		if(x<layerraster.length-1){
			if(layerraster[x+1][y][z]==node){
				return true;
			}
		}
		if(y>0){
			if(layerraster[x][y-1][z]==node){
				return true;
			}
		}
		if(y<layerraster[0].length-1){
			if(layerraster[x][y+1][z]==node){
				return true;
			}
		}
		return false;
	}
}
