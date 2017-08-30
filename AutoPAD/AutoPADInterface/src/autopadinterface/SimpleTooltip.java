package autopadinterface;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;

// SimpleTooltip is a helper class used to quickly add tooltips to controls. 
public class SimpleTooltip {
	private Control source;
	private Shell shell;
	private ToolTip tip;
	public SimpleTooltip(Shell inshell, Control holder, String tiptext){
		shell=inshell;
		source=holder;
		tip = new ToolTip(shell, SWT.BALLOON); // tip is the actual tooltip
		tip.setMessage(tiptext);
		
		holder.addMouseTrackListener(new SimpleTooltipMouseTrackListener(this));
	}
	// show(): display the tooltip
	public void show(MouseEvent e){
		Rectangle rec = source.getBounds();
		Point eloc = source.toDisplay(e.x,e.y);
		tip.setLocation(eloc.x+rec.width/2,eloc.y+rec.height/2); // some adjustments here to keep the tooltip from hiding the object in question
		tip.setVisible(true); // e.g. it would be bad if the tooltip hid the button it was referring to
	}
	// hide(): self-explanatory
	public void hide(){
		tip.setVisible(false);
	}
}

// subclass used to listen to mouse movements 
class SimpleTooltipMouseTrackListener implements MouseTrackListener{
	private SimpleTooltip source;
	public SimpleTooltipMouseTrackListener(SimpleTooltip insource){
		source=insource;
	}
	@Override
	public void mouseEnter(MouseEvent e) {

	}

	@Override
	public void mouseExit(MouseEvent e) {
		source.hide();
	}

	@Override
	public void mouseHover(MouseEvent e) {
		source.show(e);
	}
}
