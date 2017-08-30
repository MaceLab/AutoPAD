package autopadinterface;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

// PreviewBoss is used to keep track of all the preview windows currently being displayed
// and is accessed in order to access those and update them whenever the device has changed.
public class PreviewBoss {
	private DevicePreview[] previewlist;
	private int previews = 0;
	private boolean[] previewready;
	public PreviewBoss(){
		previewlist = new DevicePreview[1];
		previewready = new boolean[1];
	}
	
	// setup(): used to set the size of the relevant lists to whatever the number of layers in the device is.
	// moves data from old lists to the new ones.
	public void setup(int layers){
		DevicePreview[] oldlist = previewlist;
		previewlist = new DevicePreview[layers];
		boolean[] oldready = previewready;
		previewready = new boolean[layers];
		int i=0;
		while(i<layers){
			previewready[i]=false;
			i++;
		}
		i=0;
		while(i<previews){
			if(i<layers){
				previewlist[i]=oldlist[i];
				previewready[i]=oldready[i];
			}else{
				oldlist[i].end();
			}
			i++;
		}
		i=0;
		
		previews=layers;
	}
	
	// setLayer(): makes a new window if one doesn't exist and updates the existing window if one does exist.
	// Used to update the image for a given layer.
	public void setLayer(Shell source, Image img, String imgpath, String name, int count, int inz){
		if(count>=previews || count<0){
			return; 
		}
		if(!previewready[count]){
			previewlist[count] = new DevicePreview(source,this,img,imgpath,name,count,inz);
			int lastlayer = findLayerAbove(count);
			if(lastlayer==-1){
				int nextlayer = findLayerBelow(count);
				if(nextlayer==-1){
					previewlist[count].corner();
				}else{
					previewlist[count].above(previewlist[nextlayer]);
				}
			}else{
				previewlist[count].below(previewlist[lastlayer]);
			}
			previewready[count]=true;
		}else{
			previewlist[count].update(img,name,inz,imgpath,false);
			previewlist[count].updatePos();
		}
	}
	
	// findLayerAbove(): iterates over the layer list and finds which layer, if any, is higher (i.e. has a smaller Z value) than the given id
	// returns -1 if none is found
	public int findLayerAbove(int id){
		int z = previewlist[id].getZ();
		int foundz = z;
		int foundi = -1;
		int i=0; 
		while(i<previewlist.length){
			if(previewready[i]){
				int iz = previewlist[i].getZ();
				if(iz<z && (iz>foundz || foundi==-1)){ // if no option has been found yet, ignore the condition that new options be closer to the source id
					foundz=iz;
					foundi=i;
				}
			}
			i++;
		}
		
		return foundi;
	}
	
	// findLayerBelow(): iterates over the layer list and finds which layer, if any, is lower (i.e. has a greater Z value) than the given id
	// returns -1 if none is found
	public int findLayerBelow(int id){
		int z = previewlist[id].getZ();
		int foundz = z;
		int foundi = -1;
		int i=0; 
		while(i<previewlist.length){
			if(previewready[i]){
				int iz = previewlist[i].getZ();
				if(iz>z && (iz<foundz || foundi==-1)){ // if no option has been found yet, ignore the condition that new options be closer to the source id
					foundz=iz;
					foundi=i;
				}
			}
			i++;
		}
		
		return foundi;
	}
	
	// lost(): called when a window is closed, updates previewready so the boss knows to reopen it
	public void lost(int count){
		if(count<previewready.length){
			previewready[count]=false;
		}
	}
	
	// flushSelections(): iterates through each window and empties the selection
	public void flushSelections(){
		int i=0;
		while(i<previews){
			if(previewready[i]){
				previewlist[i].flushSelection();
			}
			i++;
		}
	}
	
	// select(): selects a specific node and informs the relevant window
	public void select(int nodeid, int z){
		if(z>=0 && z<previews){
			if(previewready[z]){
				previewlist[z].select(nodeid);
			}
		}
	}
}
