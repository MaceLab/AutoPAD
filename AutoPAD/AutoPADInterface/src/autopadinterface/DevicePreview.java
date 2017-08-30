package autopadinterface;



import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

// DevicePreview class is used for preview windows. All these windows do is hold an image that can be selected
// and updated.
public class DevicePreview extends Dialog {
	private int z;
	private Shell dialog;
	private Canvas imagepane;
	private DevicePreviewPaintListener paintlistener;
	private PreviewBoss boss;
	private int selected = -1;
	private String preselectionimagepath;
	private int xpos = 0;
	private int ypos = 0;

	public DevicePreview(Shell source, PreviewBoss inboss, Image img, String imgpath, String name, int count, int inz){
		super(source);
		
		z=inz;
		boss=inboss;
		
		dialog = new Shell(source, SWT.DIALOG_TRIM);// | SWT.APPLICATION_MODAL);
		dialog.setLayout(new FillLayout());
		Rectangle rect = img.getBounds();
		//5 and 35 are empirically derived fudge factors
		dialog.setSize(rect.width+5,rect.height+35);
		dialog.setText(name);
		
		imagepane = new Canvas(dialog, SWT.NULL);
		paintlistener = new DevicePreviewPaintListener(img);
		preselectionimagepath=imgpath;
		imagepane.addPaintListener(paintlistener);
		
		imagepane.addMouseListener(new DevicePreviewMouseListener(this));
		
		dialog.open();
		dialog.addDisposeListener(new DevicePreviewDisposeListener(inboss,count));
	}
	// update(): is called in order to change the displayed image
	public void update(Image img, String name, int inz, String imgpath, boolean sel){
		if(img==null){
			System.out.println(" ERROR! Passed a null image to preview update!");
			return;
		}
		
		if(!sel){
			preselectionimagepath=imgpath;
		}
		
		if(selected!=-1 && z==inz && !sel){
			update(AutoPADInterface.getPreviewSelectionImage(inz, selected),name,z,preselectionimagepath,true);
			return;
		}else if(selected!=-1 && !sel){
			selected=-1;
		}
		z=inz;
		dialog.setText(name);
		paintlistener.end();
		//imagepane.removePaintListener(paintlistener);
		paintlistener.update(img);
		
		// update the canvas size:
		Rectangle imgsize = img.getBounds();
		dialog.setSize(imgsize.width+5,imgsize.height+35);
		
		// update the imagepane to show the new image:
		dialog.getDisplay().asyncExec(new Runnable () { 
			public void run() { 
				if (!imagepane.isDisposed()) { 
					imagepane.redraw(); 
				} 
			} 
		}); 
		
	}
	// end(): called in order to dispatch of the window
	public void end(){
		dialog.dispose();
	}
	// click(): called by the mouse listener whenever the user clicks on the image.
	// click() handles the selection of nodes via mouse.
	public void click(double x, double y){
		// first find the node clicked on via the noderaster:
		int nodeid = AutoPADInterface.findPreviewNodeID(x,y,z);
		if(nodeid==-1){
			flushSelection();
			return; // no node found
		}
		// now figure out the headernodeid of that node (since current uses the headernodeid):
		int headernodeid = AutoPADInterface.findHeaderNodeFromMapNodeID(nodeid);
		if(headernodeid==-1){
			return; // no node found
		}
		//TreeItem oldt = DeviceDrawInterface.getCurrent();
		TreeItem newt = AutoPADInterface.getHeaderNodeItem(headernodeid);
		AutoPADInterface.selectTreeItem(newt);
		
		boss.flushSelections();
		select(nodeid);
	}
	// flushSelection(): empties the selection and updates the image to accommodate this fact
	public void flushSelection(){
		if(selected!=-1){
			selected=-1;
			update(new Image(dialog.getDisplay(),preselectionimagepath),dialog.getText(),z,preselectionimagepath,false);
		}
	}
	// select(): select a node and update the image to accomodate this
	public void select(int nodeid){
		selected=nodeid;
		update(AutoPADInterface.getPreviewSelectionImage(z, nodeid),dialog.getText(),z,preselectionimagepath,true);
	}
	// corner(): places this window in the corner of the screen
	public void corner() {
		Rectangle bounds = dialog.getDisplay().getBounds();
		Point size = dialog.getSize();
		int x = (bounds.width-size.x);
		int y = 0;//size.y;//(bounds.height-size.y);
		dialog.setBounds(x,y,size.x,size.y);
	}
	// getDialog(): simply returns this window
	public Shell getDialog(){
		return dialog;
	}
	// getZ(): simply returns the z level of this window
	public int getZ(){
		return z;
	}
	// below(): places this window below another devicePreview window
	public void below(DevicePreview indp) {
		Rectangle bounds = indp.getDialog().getBounds();
		Point othersize = indp.getDialog().getSize();
		Point size = dialog.getSize();
		dialog.setBounds(bounds.x,bounds.y+othersize.y,size.x,size.y);
	}
	// above(): places this window below another devicePreview window
	public void above(DevicePreview indp) {
		Rectangle bounds = indp.getDialog().getBounds();
		//Point othersize = indp.getDialog().getSize();
		Point size = dialog.getSize();
		dialog.setBounds(bounds.x,bounds.y-size.y,size.x,size.y);
	}
	// updatePos(): attempts to update the position of this window based on changes in the device dimensions
	public void updatePos(){
		Rectangle screenbounds = dialog.getDisplay().getBounds();
		Rectangle bounds = dialog.getBounds();
		Point size = dialog.getSize();
		/*if(xpos==bounds.x && ypos==bounds.y){
			if(size.x != xsize){
				int dif = xsize-size.x;
				xpos+=dif;
			}
			if(size.y !=ysize){
				int dif = ysize-size.y;
				ypos-=dif;
			}
		}else{
			xpos=bounds.x;
			ypos=bounds.y;
		}*/
		xpos=bounds.x;
		ypos=bounds.y;
		if(xpos+size.x>screenbounds.width){
			xpos -= (xpos+size.x-screenbounds.width);
		}
		if(ypos+size.y>screenbounds.height){
			ypos -= (ypos+size.y-screenbounds.height);
		}
		if(xpos<0){
			xpos=0;
		}
		if(ypos<0){
			ypos =0;
		}
		dialog.setBounds(xpos,ypos,size.x,size.y);
	}
}

//subclasses:
// DevicePreviewPaintListener: used to update the displayed image
class DevicePreviewPaintListener implements PaintListener {
	private Image img;
	DevicePreviewPaintListener(Image inimg){
		img=inimg;
	}
	public void update(Image inimg){
		img=inimg;
	}
	public void end(){
		img.dispose();
	}
	@Override
	public void paintControl(PaintEvent e) {
		e.gc.drawImage(img, 0, 0);
		//System.out.println(" GOT PAINT EVENT");
	}
}
// DevicePreviewDisposeListener: used to check when this window is closed so that its boss may be alerted
// and know to open a new copy when a new preview is generated.
class DevicePreviewDisposeListener implements DisposeListener {
	private PreviewBoss boss;
	private int count;
	DevicePreviewDisposeListener(PreviewBoss inboss, int incount){
		boss=inboss;
		count=incount;
	}
	@Override
	public void widgetDisposed(DisposeEvent e){
		boss.lost(count);
	}
}
// DevicePreviewMouseListener: used to check when the user clicks on the image, so that the node
// which was clicked on can be found and selected.
class DevicePreviewMouseListener extends MouseAdapter {
	private DevicePreview owner;
	DevicePreviewMouseListener(DevicePreview inowner){
		owner=inowner;
	}
	
	@Override
	public void mouseDown(MouseEvent e){
		
	}
	
	@Override
	public void mouseUp(MouseEvent e){
		Point size = ((Control)e.widget).getSize();
		// node selection
		//System.out.println(" MOUSE UP AT " + e.x + " by " + e.y);
		if(!(e.x>=0 && e.x<=size.x && e.y>=0 && e.y<=size.y)){
			return; // out of range, stop.
		}
		// Need to correlate this x,y position to a part of the device.
		// that information is in the Interpreter and needs to be transferred here.
		owner.click(e.x,e.y);
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent e){
		
	}
}
