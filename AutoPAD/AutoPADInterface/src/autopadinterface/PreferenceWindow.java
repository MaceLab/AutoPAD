package autopadinterface;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PreferenceWindow extends Dialog {
	private Shell dialog;
	private Button laycolbtn1, laycolbtn2, prevautobtn1;
	private boolean colorbyz=false;
	private Text laycoltext1, prevfilltext1, polydrawtext1, polydrawtext2;
	private int seed;
	private boolean previewauto=true;
	private double previewfillfactor = 0.25;
	
	private Canvas imagepane;
	private PreferenceWindowSeedPaintListener paintlistener;
	
	private int polydrawx,polydrawy;
	
	public PreferenceWindow(Shell source, Font fontui, Font font10, int[] col, 
			boolean incolorbyz, int inseed, boolean inpreviewauto,
			double inpreviewfillfactor, int inpolydrawx, int inpolydrawy){
		super(source);
		
		colorbyz=incolorbyz;
		seed=inseed;
		previewauto=inpreviewauto;
		previewfillfactor=inpreviewfillfactor;
		polydrawx=inpolydrawx;
		polydrawy=inpolydrawy;
		
		dialog = new Shell(source, SWT.DIALOG_TRIM);
		dialog.setLayout(new FillLayout());
		dialog.setText("Preferences");
		
		dialog.setSize(353,640);
		Composite prefcomp = new Composite(dialog, SWT.BORDER);
		
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true; // fill up
		//gd.horizontalAlignment = SWT.FILL;
		
		prefcomp.setLayout(new GridLayout());
		prefcomp.setLayoutData(gd);
		
		Label preflab1 = new Label(prefcomp, SWT.NULL);
		preflab1.setText("Preview Settings");
		preflab1.setFont(font10);
		
		Composite selcolcomp = new Composite(prefcomp, SWT.BORDER);
		selcolcomp.setLayout(new GridLayout());
		selcolcomp.setLayoutData(gd);
		
		Label selcollabel = new Label(selcolcomp, SWT.NULL);
		selcollabel.setText("Preview Selection Color");
		selcollabel.setFont(fontui);
		
		Composite colorcomp = new Composite(selcolcomp, SWT.NONE);
		colorcomp.setLayout(new RowLayout());

		// prop color red label
		Label colorrlabel = new Label(colorcomp, SWT.NULL);
		colorrlabel.setText("R : ");
		colorrlabel.setFont(fontui);
		// prop color red text box
		final Text redcolortextbox = new Text(colorcomp, SWT.BORDER);
		redcolortextbox.setLayoutData(new RowData(35, 22));
		redcolortextbox.setText("" + col[0]);
		redcolortextbox.setFont(fontui);
		// prop color green label
		Label colorglabel = new Label(colorcomp, SWT.NULL);
		colorglabel.setText("G : ");
		colorglabel.setFont(fontui);
		// prop color green text box
		final Text greencolortextbox = new Text(colorcomp, SWT.BORDER);
		greencolortextbox.setLayoutData(new RowData(35, 22));
		greencolortextbox.setText("" + col[1]);
		greencolortextbox.setFont(fontui);
		// prop color blue label
		Label colorblabel = new Label(colorcomp, SWT.NULL);
		colorblabel.setText("B : ");
		colorblabel.setFont(fontui);
		// prop color blue text box
		final Text bluecolortextbox = new Text(colorcomp, SWT.BORDER);
		bluecolortextbox.setLayoutData(new RowData(35, 22));
		bluecolortextbox.setText("" + col[2]);
		bluecolortextbox.setFont(fontui);
		// prop set color button
		Button setcolor = new Button(colorcomp, SWT.PUSH);
		setcolor.setText(" Set ");
		setcolor.setFont(fontui);
		setcolor.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setSelColor(redcolortextbox.getText(),
						greencolortextbox.getText(), bluecolortextbox.getText());
				// adds/updates a color node on current header node
			}
		});
		// prop pick color button
		Button pickcolor = new Button(colorcomp, SWT.PUSH);
		pickcolor.setText("Pick");
		pickcolor.setFont(fontui);
		pickcolor.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				ColorDialog cpicker = new ColorDialog(dialog); 
				// ColorDialog is a default color picker dialog
				RGB col = cpicker.open();
				if(col != null){ // if the user picked a color, set the
									// textboxes to match.
					redcolortextbox.setText("" + col.red);
					greencolortextbox.setText("" + col.green);
					bluecolortextbox.setText("" + col.blue);
				}
			}
		});
		
		Composite prevautocomp = new Composite(prefcomp, SWT.BORDER);
		prevautocomp.setLayout(new GridLayout());
		prevautocomp.setLayoutData(gd);
		
		Label prevautolabel = new Label(prevautocomp, SWT.NULL);
		prevautolabel.setText("Automatic Preview");
		prevautolabel.setFont(fontui);
		
		Composite prevauto2comp = new Composite(prevautocomp, SWT.NONE);
		prevauto2comp.setLayout(new RowLayout());
		
		prevautobtn1 = new Button(prevauto2comp, SWT.TOGGLE);
		prevautobtn1.setText(" AutoPreview ");
		prevautobtn1.setFont(fontui);
		prevautobtn1.addSelectionListener(new PreviewAutoSelectionListener(this));
		prevautobtn1.setSelection(previewauto);
		
		Composite prevfillcomp = new Composite(prefcomp, SWT.BORDER);
		prevfillcomp.setLayout(new GridLayout());
		prevfillcomp.setLayoutData(gd);
		
		Label prevfilllabel = new Label(prevfillcomp, SWT.NULL);
		prevfilllabel.setText("Preview Size (fraction of actual)");
		prevfilllabel.setFont(fontui);
		
		Composite prevfill2comp = new Composite(prevfillcomp, SWT.NONE);
		prevfill2comp.setLayout(new RowLayout());
		
		Label prevfilllab3 = new Label(prevfill2comp, SWT.NULL);
		prevfilllab3.setText("Size Ratio: ");
		prevfilllab3.setFont(fontui);
		
		prevfilltext1 = new Text(prevfill2comp,SWT.BORDER);
		prevfilltext1.setLayoutData(new RowData(65, 22));
		prevfilltext1.setText("" + previewfillfactor);
		prevfilltext1.setFont(fontui);
		
		Button prevfillbtn1 = new Button(prevfill2comp, SWT.PUSH);
		prevfillbtn1.setText(" Set ");
		prevfillbtn1.setFont(fontui);
		prevfillbtn1.addSelectionListener(new PreviewFillSelectionListener(this));
		
		
		
		Label preflab2 = new Label(prefcomp, SWT.NULL);
		preflab2.setText("Polygon Editor");
		preflab2.setFont(font10);
		
		Composite polydrawcomp = new Composite(prefcomp, SWT.BORDER);
		polydrawcomp.setLayout(new GridLayout());
		polydrawcomp.setLayoutData(gd);
		
		Label polydrawlab1 = new Label(polydrawcomp, SWT.NULL);
		polydrawlab1.setText("Size of Grid");
		polydrawlab1.setFont(fontui);
		
		Composite polydraw2comp = new Composite(polydrawcomp, SWT.NONE);
		polydraw2comp.setLayout(new RowLayout());
		
		Label polydrawlab2 = new Label(polydraw2comp, SWT.NULL);
		polydrawlab2.setText("X Radius: ");
		polydrawlab2.setFont(fontui);
		
		polydrawtext1 = new Text(polydraw2comp,SWT.BORDER);
		polydrawtext1.setLayoutData(new RowData(45, 22));
		polydrawtext1.setText("" + polydrawx);
		polydrawtext1.setFont(fontui);
		
		Label polydrawlab3 = new Label(polydraw2comp, SWT.NULL);
		polydrawlab3.setText("Y Radius: ");
		polydrawlab3.setFont(fontui);
		
		polydrawtext2 = new Text(polydraw2comp,SWT.BORDER);
		polydrawtext2.setLayoutData(new RowData(45, 22));
		polydrawtext2.setText("" + polydrawy);
		polydrawtext2.setFont(fontui);
		
		Button polydrawbtn1 = new Button(polydraw2comp, SWT.PUSH);
		polydrawbtn1.setText(" Set ");
		polydrawbtn1.setFont(fontui);
		polydrawbtn1.addSelectionListener(new PolyDrawPrefSelectionListener(this));
		
		
		Label preflab3 = new Label(prefcomp, SWT.NULL);
		preflab3.setText("Node Coloring");
		preflab3.setFont(font10);
		
		Composite laycolcomp = new Composite(prefcomp, SWT.BORDER);
		laycolcomp.setLayout(new GridLayout());
		laycolcomp.setLayoutData(gd);
		
		Label laycollab1 = new Label(laycolcomp, SWT.NULL);
		laycollab1.setText("Coloring Type");
		laycollab1.setFont(fontui);
		
		Composite laycol2comp = new Composite(laycolcomp, SWT.NONE);
		laycol2comp.setLayout(new RowLayout());
		
		laycolbtn1 = new Button(laycol2comp, SWT.TOGGLE);
		laycolbtn1.setText(" By Node ");
		laycolbtn1.setFont(fontui);
		laycolbtn1.addSelectionListener(new LayerColorSelectionListener(this));
		
		laycolbtn2 = new Button(laycol2comp, SWT.TOGGLE);
		laycolbtn2.setText(" By Layer ");
		laycolbtn2.setFont(fontui);
		laycolbtn2.addSelectionListener(new LayerColorSelectionListener(this));
		
		laycolbtn1.setSelection(!colorbyz);
		laycolbtn2.setSelection(colorbyz);
		
		Composite laycolcomp2 = new Composite(prefcomp, SWT.BORDER);
		laycolcomp2.setLayout(new GridLayout());
		laycolcomp2.setLayoutData(gd);
		
		Label laycollab2 = new Label(laycolcomp2, SWT.NULL);
		laycollab2.setText("Coloring Seed");
		laycollab2.setFont(fontui);
		
		Composite laycol3comp = new Composite(laycolcomp2, SWT.NONE);
		laycol3comp.setLayout(new RowLayout());
		
		Label laycollab3 = new Label(laycol3comp, SWT.NULL);
		laycollab3.setText("Seed: ");
		laycollab3.setFont(fontui);
		
		laycoltext1 = new Text(laycol3comp,SWT.BORDER);
		laycoltext1.setLayoutData(new RowData(110, 22));
		laycoltext1.setText("" + seed);
		laycoltext1.setFont(fontui);
		
		Button laycolbtn3 = new Button(laycol3comp, SWT.PUSH);
		laycolbtn3.setText(" Set ");
		laycolbtn3.setFont(fontui);
		laycolbtn3.addSelectionListener(new LayerColorSetSelectionListener(this));
		
		Button laycolbtn4 = new Button(laycol3comp, SWT.PUSH);
		laycolbtn4.setText(" Randomize ");
		laycolbtn4.setFont(fontui);
		laycolbtn4.addSelectionListener(new LayerColorRandomizeSelectionListener(this));
		
		
		String imgpath = AutoPADInterface.makeColorPreview(128, 4, 22);
		Image img = null;
		try{
			img = new Image(dialog.getDisplay(), imgpath);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		Composite laycol4comp = new Composite(prefcomp, SWT.NONE);
		GridLayout prevgl = new GridLayout();
		prevgl.numColumns=2;
		laycol4comp.setLayout(prevgl);
		
		Label laycollab4 = new Label(laycol4comp, SWT.NULL);
		laycollab4.setText("Preview: ");
		laycollab4.setFont(fontui);
		imagepane = new Canvas(laycol4comp, SWT.NULL);
		paintlistener = new PreferenceWindowSeedPaintListener(img);
		imagepane.addPaintListener(paintlistener);
		Rectangle imgsize = img.getBounds();
		imagepane.setSize(imgsize.width+5,imgsize.height+35);
		GridData imggd = new GridData();
		imggd.widthHint = imgsize.width;
		imggd.heightHint = imgsize.height;
		imagepane.setLayoutData(imggd);
		
		dialog.open();
		
		dialog.addDisposeListener(new PreferenceWindowDisposeListener());
	}
	
	public void setColorByZ(boolean inb){
		AutoPADInterface.setColorByZ(inb);
	}
	public void receiveColorByZ(boolean inb){
		colorbyz=inb;
		laycolbtn1.setSelection(!inb);
		laycolbtn2.setSelection(inb);
	}
	public void invertColorByZ(){
		colorbyz=!colorbyz;
		setColorByZ(colorbyz);
	}
	
	public void setPreviewAuto(boolean inb){
		AutoPADInterface.setPreviewAuto(inb);
	}
	public void receivePreviewAuto(boolean inb){
		previewauto=inb;
		prevautobtn1.setSelection(inb);
	}
	public void invertPreviewAuto(){
		previewauto=!previewauto;
		setPreviewAuto(previewauto);
	}
	
	public void setSelColor(String r, String g, String b){
		int ir=0;
		int ig=0;
		int ib=0;
		try{
			ir = Integer.parseInt(r);
			ig = Integer.parseInt(g);
			ib = Integer.parseInt(b);
		}catch(Exception e){
			return;
		}
		AutoPADInterface.setPreviewSelectionColor(ir,ig,ib);
	}
	
	public void setLayerColorSeed(){
		int test = 0;
		try{
			test = Integer.parseInt(laycoltext1.getText());
		}catch(Exception e){
			randomizeLayerColorSeed();
			return;
		}
		seed = test;
		laycoltext1.setText("" + seed);
		AutoPADInterface.setColorSeed(seed);
	}
	
	public void randomizeLayerColorSeed(){
		int a = (int)(Math.random()*1000000);
		laycoltext1.setText("" + a);
		setLayerColorSeed();
	}
	
	public void receiveColorSeed(int inseed){
		seed=inseed;
		laycoltext1.setText("" + seed);
		String imgpath = AutoPADInterface.makeColorPreview(128, 4, 22);
		Image img = null;
		try{
			img = new Image(dialog.getDisplay(), imgpath);
		}catch(Exception e){
			e.printStackTrace();
		}
		paintlistener.end();
		paintlistener.update(img);
		
		// update the canvas size:
		Rectangle imgsize = img.getBounds();
		imagepane.setSize(imgsize.width+5,imgsize.height+35);
		
		// update the imagepane to show the new image:
		dialog.getDisplay().asyncExec(new Runnable () { 
			public void run() { 
				if (!imagepane.isDisposed()) { 
					imagepane.redraw(); 
				} 
			} 
		}); 
	}
	
	public void setPreviewFillFactor(){
		double test = 0;
		try{
			test = Double.parseDouble(prevfilltext1.getText());
		}catch(Exception e){
			test=0.25;
		}
		previewfillfactor = test;
		prevfilltext1.setText("" + previewfillfactor);
		AutoPADInterface.setPreviewFillFactor(previewfillfactor);
	}
	
	public void receivePreviewFillFactor(double inf){
		previewfillfactor=inf;
		prevfilltext1.setText("" + previewfillfactor);
	}
	
	public void setPolyDrawSize(){
		int testx= 0;
		int testy= 0;
		try{
			testx = Integer.parseInt(polydrawtext1.getText());
			testy = Integer.parseInt(polydrawtext2.getText());
		}catch(Exception e){
			testx=5;
			testy=5;
		}
		polydrawx=testx;
		polydrawy=testy;
		polydrawtext1.setText("" + polydrawx);
		polydrawtext2.setText("" + polydrawy);
		AutoPADInterface.setPolyDrawSize(polydrawx,polydrawy);
	}
	
	public void receivePolyDrawSize(int inx, int iny){
		polydrawx=inx;
		polydrawy=iny;
		polydrawtext1.setText("" + polydrawx);
		polydrawtext2.setText("" + polydrawy);
	}
	
	public boolean isDisposed(){
		return dialog.isDisposed();
		
	}
}


class PreferenceWindowDisposeListener implements DisposeListener {
	PreferenceWindowDisposeListener(){

	}
	@Override
	public void widgetDisposed(DisposeEvent e){

	}
}

class LayerColorSelectionListener extends SelectionAdapter {
	private PreferenceWindow boss;
	LayerColorSelectionListener(PreferenceWindow inboss){
		boss=inboss;
	}
	@Override
	public void widgetSelected(SelectionEvent e){
		boss.invertColorByZ();
		// adds/updates a color node on current header node
	}
}
class LayerColorSetSelectionListener extends SelectionAdapter {
	private PreferenceWindow boss;
	LayerColorSetSelectionListener(PreferenceWindow inboss){
		boss=inboss;
	}
	@Override
	public void widgetSelected(SelectionEvent e){
		boss.setLayerColorSeed();
		// adds/updates a color node on current header node
	}
}
class LayerColorRandomizeSelectionListener extends SelectionAdapter {
	private PreferenceWindow boss;
	LayerColorRandomizeSelectionListener(PreferenceWindow inboss){
		boss=inboss;
	}
	@Override
	public void widgetSelected(SelectionEvent e){
		boss.randomizeLayerColorSeed();
		// adds/updates a color node on current header node
	}
}
class PreviewAutoSelectionListener extends SelectionAdapter {
	private PreferenceWindow boss;
	PreviewAutoSelectionListener(PreferenceWindow inboss){
		boss=inboss;
	}
	@Override
	public void widgetSelected(SelectionEvent e){
		boss.invertPreviewAuto();
		// adds/updates a color node on current header node
	}
}
class PreviewFillSelectionListener extends SelectionAdapter {
	private PreferenceWindow boss;
	PreviewFillSelectionListener(PreferenceWindow inboss){
		boss=inboss;
	}
	@Override
	public void widgetSelected(SelectionEvent e){
		boss.setPreviewFillFactor();
		// adds/updates a color node on current header node
	}
}
class PreferenceWindowSeedPaintListener implements PaintListener {
	private Image img;
	PreferenceWindowSeedPaintListener(Image inimg){
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

class PolyDrawPrefSelectionListener extends SelectionAdapter {
	private PreferenceWindow boss;
	PolyDrawPrefSelectionListener(PreferenceWindow inboss){
		boss=inboss;
	}
	@Override
	public void widgetSelected(SelectionEvent e){
		boss.setPolyDrawSize();
		// adds/updates a color node on current header node
	}
}