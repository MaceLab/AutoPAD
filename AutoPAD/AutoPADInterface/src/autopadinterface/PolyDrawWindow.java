package autopadinterface;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PolyDrawWindow extends Dialog {
	private Shell dialog;
	private Font fontui;
	private Text restext;
	private Text buftext;
	
	private int x=0;
	private int tx = 0;
	private int y=0;
	private int ty = 0;
	
	private int cx=0;
	private int cy=0;
	
	private int moving=-1;
	
	private PolyPoint[][] points;
	public PolyDrawWindow(Shell source, int inx, int iny, Font infontui){
		super(source);
		
		x=inx;
		y=iny;
		tx = x*2+1;
		ty = y*2+1;
		cx=x;
		cy=y;
		fontui=infontui;
		
		points = new PolyPoint[tx][ty];
		
		dialog = new Shell(source, SWT.DIALOG_TRIM);
		dialog.setLayout(new GridLayout());//new FillLayout());
		dialog.setText("PolyDraw");
		
		dialog.setSize(x*90+20,y*90+90);
		Composite pdcomp = new Composite(dialog, SWT.BORDER);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true; // fill up
		gd.horizontalAlignment = SWT.FILL;
		GridLayout gl = new GridLayout();
		gl.numColumns = tx;
		gl.makeColumnsEqualWidth=true;
		gl.horizontalSpacing=(x*100/(tx*2));
		gl.verticalSpacing=(y*100/(ty*2));
		//gl.marginHeight=0;
		//gl.marginWidth=0;
		
		pdcomp.setLayout(gl);
		pdcomp.setLayoutData(gd);
		
		int i=0;
		while(i<tx){
			int o=0;
			while(o<ty){
				//Label lab = new Label(pdcomp, SWT.NULL);
				/*lab.setText("x");
				lab.setFont(fontui);
				if(i==cx && o==cy){
					lab.setText("O");
				}*/
				points[i][o] = new PolyPoint(this,pdcomp,i,o,fontui);
				if(i==cx && o==cy){
					points[i][o].center();
				}
				o++;
			}
			i++;
		}
		
		Composite pd2comp = new Composite(dialog, SWT.BORDER);
		pd2comp.setLayout(new RowLayout());
		//pd2comp.setLayoutData(gd);
		
		Label reslab = new Label(pd2comp, SWT.NULL);
		reslab.setFont(fontui);
		reslab.setText("Resolution: ");
		
		restext = new Text(pd2comp, SWT.BORDER);
		restext.setFont(fontui);
		restext.setText("1");
		restext.setLayoutData(new RowData(40, 22));
		
		Label buflab = new Label(pd2comp, SWT.NULL);
		buflab.setFont(fontui);
		buflab.setText("Buffer: ");
		
		buftext = new Text(pd2comp, SWT.BORDER);
		buftext.setFont(fontui);
		buftext.setText("0.5");
		buftext.setLayoutData(new RowData(40, 22));
		
		Button transbtn = new Button(pd2comp, SWT.PUSH);
		transbtn.setText(" Export ");
		transbtn.setFont(fontui);
		transbtn.addSelectionListener(new PolyDrawTransSelectionAdapter(this));
		
		dialog.open();
		
		dialog.addDisposeListener(new PolyDrawWindowDisposeListener());
	}
	
	public int hit(int inx, int iny){
		int max=0;
		int i=0;
		while(i<tx){
			int o=0;
			while(o<ty){
				if(points[i][o].getVal()>max){
					max=points[i][o].getVal();
				}
				o++;
			}
			i++;
		}
		
		if(moving!=-1){
			i=0;
			while(i<tx){
				int o=0;
				while(o<ty){
					if(points[i][o].getVal()==-2){
						points[i][o].hit(true);
					}
					o++;
				}
				i++;
			}
			int temp=moving;
			moving=-1;
			return temp;
		}
		return max+1;
	}
	public void unhit(int inx, int iny, int inval){
		// need to unravel
		int i=0;
		while(i<tx){
			int o=0;
			while(o<ty){
				if(points[i][o].getVal()>inval){
					points[i][o].lower();
				}
				o++;
			}
			i++;
		}
		if(inx==cx && iny==cy){
			points[inx][iny].center();
		}
	}
	
	public void translate(double res, double buffer){
		int max = hit(0,0);
		
		int i=0;
		while(i<128){
			AutoPADInterface.removeVertex(max,false);
			i++;
		}
		
		int a=1;
		while(a<max){
			i=0;
			while(i<tx){
				int o=0;
				while(o<ty){
					if(points[i][o].getVal()==a){
						double vx = -(double)(i-cx)*res;
						double vy = -(double)(o-cy)*res;
						
						AutoPADInterface.setVertex(a,"" + vx, "" + vy, "-1", 
								"" + buffer, "-1",false);
					}
					o++;
				}
				i++;
			}
			a++;
		}
		AutoPADInterface.setPoly();
		AutoPADInterface.preview();
	}
	
	public void callTranslate(){
		double res=1;
		double buffer=1;
		try{
			res = Double.parseDouble(restext.getText());
			buffer=Double.parseDouble(buftext.getText());
		}catch(Exception e){
			return;
		}
		translate(res,buffer);
	}
	
	public void move(int inval){
		if(moving!=-1){
			int i=0;
			while(i<tx){
				int o=0;
				while(o<ty){
					if(points[i][o].getVal()==-2){
						points[i][o].hit(false);
					}
					o++;
				}
				i++;
			}
		}
		moving=inval;
	}
	
	public void cancelMove(){
		moving=-1;
	}
	
	public int getMoving(){
		return moving;
	}
	
	public void checkCenter(int inx, int iny){
		if(inx==cx && iny==cy){
			points[inx][iny].center();
		}
	}
	
	public boolean isDisposed(){
		return dialog.isDisposed();
	}
}

class PolyPoint{
	private Composite comp;
	private Button btn;
	private PolyDrawWindow source;
	private int x=0;
	private int y=0;
	
	private int val=-1;
	private int oval=-1;
	PolyPoint(PolyDrawWindow insource, Composite incomp, int inx, int iny, Font fontui){
		source=insource;
		comp=incomp;
		x=inx; y=iny;
		
		btn = new Button(comp,SWT.TOGGLE);
		btn.setSize(40,20);
		GridData bd = new GridData();
		bd.heightHint=20;
		bd.widthHint=20;
		btn.setLayoutData(bd);
		btn.setText(" ");
		btn.setFont(fontui);
		btn.setBackground(new Color(incomp.getDisplay(),255,255,255));
		btn.addSelectionListener(new PolyPointBtnSelectionAdapter(this));
	}
	
	public void hit(boolean move){
		int moving = source.getMoving();
		if(moving!=-1 && val>=0){
			oval=val;
			val = source.hit(x,y);
			btn.setText("" + val);
			btn.setBackground(new Color(comp.getDisplay(),0,0,0));
			btn.setSelection(true);
			source.move(oval);
			return;
		}
		if(val==-1){
			val = source.hit(x,y);
			btn.setText("" + val);
			btn.setBackground(new Color(comp.getDisplay(),0,0,0));
			btn.setSelection(true);
		}else if(val!=-2){
			btn.setText("?");
			source.move(val);
			oval=val;
			val=-2;
			btn.setSelection(true);
			btn.setBackground(new Color(comp.getDisplay(),255,0,0));
		}else{
			btn.setText("");
			if(!move){
				source.unhit(x,y,oval);
				source.cancelMove();
			}else{
				source.checkCenter(x,y);
			}
			btn.setBackground(new Color(comp.getDisplay(),255,255,255));
			btn.setSelection(false);
			val=-1;
			oval=-1;
		}
	}
	
	public int getVal(){
		return val;
	}
	
	public void lower(){
		val--;
		btn.setText("" + val);
	}
	
	public void center(){
		btn.setText("@");
	}
	
}

class PolyPointBtnSelectionAdapter extends SelectionAdapter{
	PolyPoint boss;
	public PolyPointBtnSelectionAdapter(PolyPoint inboss){
		boss=inboss;
	}
	@Override
	public void widgetSelected(SelectionEvent e){
		boss.hit(false);
	}
}

class PolyDrawTransSelectionAdapter extends SelectionAdapter{
	PolyDrawWindow boss;
	public PolyDrawTransSelectionAdapter(PolyDrawWindow inboss){
		boss=inboss;
	}
	@Override
	public void widgetSelected(SelectionEvent e){
		boss.callTranslate();
	}
}



class PolyDrawWindowDisposeListener implements DisposeListener {
	PolyDrawWindowDisposeListener(){

	}
	@Override
	public void widgetDisposed(DisposeEvent e){

	}
}
