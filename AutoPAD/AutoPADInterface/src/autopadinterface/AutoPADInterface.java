package autopadinterface;
 
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import autopad.Interpreter;
import autopad.MapBoss;
import autopad.MapNode;

import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.events.*;
 
 
/*  
 * TODO:
 *  - perhaps a way to load existing reference scripts, fill out the input variables and have it be rendered as a node
 *  - perhaps a tab of preset polygon functions (triangles, hexagons, etc)
 *  - flesh out view menu 
 *  - quicksave feature
 */

public class AutoPADInterface {
	private static boolean use_checks=false; // whether or not to give each nodeitem a check next to it in the nodetree 
											// (with which the user can uncheck to disable that item and its children)
	private static boolean useicons=false; // whether or not to use icons for the tabs (like 'rect', 'circ', etc).
	
	private static Tree nodetree; // holds all of the nodes and their data for this device. 
	private static int maxnodes = 1248*4; // maximum number of nodes + property nodes that can be stored
										  // some lists have a factor of 4 less than this value
										  // if they do not need to store as many items. 
	private static TreeItem[] nodeitems = new TreeItem[maxnodes]; // a list of all the items in the nodetree
	private static int[] nodetype = new int[maxnodes]; // corresponding to the last list, a list of what type each node is
	// for instance, nodetype[51] would be the type (listed below) of the item at nodeitems[51]
	// List of node types:
	// 0 = node node
	// 1 = place node
	// 2 = color node
	// 3 = rect node
	// 4 = buffer node 
	// 5 = circle node
	// 6 = cut node
	// 7 = precise node
	// 8 = space node
	// 9 = fill node
	// 10 = buffer box
	// 11 = cut overlap
	// 12 = poly
	// 13 = vex
	// 14 = rot center
	// 15 = outline
	// 16 = extra
	// 17 = ref { }
	// 18 = var
	// 19 = poly curve weight
	// 20 = text node
	// 21 = pdfsize
	// 22 = pdfmargin
	// 23 = text size node
	// 24 = text font type node
	// 25 = corner node
	// 26 = perpspace node
	// 27 = corner curve node
	// 28 = cut scale node
	// 29 = rescale node
	// The "insertForced" function must be expanded to match these when this list is expanded
	private static TreeItem[] headernodeitems = new TreeItem[maxnodes/4]; 
	// a list of all the header nodes (>) within the nodetree. Crucial because these nodes contain the regular nodes
	// (any node that contains other items is a header node in this design)
	private static int headernodes = 0; 
	// headernode count -- enumerates the above list.
	private static MapNode[] headernode = new MapNode[maxnodes/4];  
	// the MapNode objects associated with the headernodes
	// each header has a corresponding MapNode which holds its data, especially Z level and angle
	private static int current = 0; // currently selected headernode
	// used for determining where to place new additions
	
	private static int nodes = 0; 
	// node count -- all items in the tree are nodes
	private static int[] nodesource = new int[maxnodes];
	// points to which node spawned this one, i.e. who's the parent
	private static double[] nodeangle = new double[maxnodes/4]; // stores the angle of header nodes 
																// (may be redundant, should be removed in future update)
	private static int[] nodeplace = new int[maxnodes/4]; // stores the place value of each headernode
														  // each headernode has a unique placeid by which other nodes find it
	private static boolean[] nodecolorset = new boolean[maxnodes/4]; // list of whether each headernode has a custom color
	private static Color[] nodecolors = new Color[maxnodes/4]; // list of each headernode's custom color, if it has one
	
	private static int current_z = 0; // current Z level (which layer is being working on)
	private static int last_ref_z = 0; // when building references, keeps track of the last working Z level

	private static Tree layertree; // special nodetree for the layer panel, which only shows nodes from one Z level at a time
	private static TreeItem[] layerorignodeitems = new TreeItem[maxnodes]; // the actual items the layer tree is based on
	private static TreeItem[] layernewnodeitems = new TreeItem[maxnodes]; // copies of the above items, made for the layer tree
	private static int layernodes = 0; // how many items are in the layer tree
	private static int currentlayertabz = 0; // which layer is being displayed on the layer tree tab

	private static Tree reftree; // special nodetree for the references panel, used for building references
	private static TreeItem[] refnodeitems = new TreeItem[maxnodes]; // all of the nodeitems in the reference panel
	private static int refnodes = 0; // number of nodeitems in the above list
	private static int[] refnodetype = new int[maxnodes]; // as before, keeps track of what type of node each nodeitem in the 
	// above list is. 
	
	private static TreeItem[] refheadernodeitems = new TreeItem[maxnodes/4]; // all of the headernodes in the reference panel
	private static int refheadernodes = 0; // number of headernodes in the above list
	private static int[] refplace = new int[maxnodes/4]; // keeps track of the placeid of each headernode in the reference panel
	// (though in this case, the placeid is the tempid, because references don't have static places)
	private static int currentref = 0; // which reference header node is currently selected for modification
	// used to figure where to add new refnodes
	
	
	private static Tree comtree; // special nodetree for combined array layers
	private static TreeItem[] comnodeitems = new TreeItem[maxnodes]; // list of the nodeitems in the combined nodetree
	private static int comnodes = 0; // enumerates the above list
	private static int[] comid = new int[maxnodes/4]; // stores the id of each headernode in the comtree
	private static int[] comnodetype = new int[maxnodes]; // stores the type of each node in the comtree
	
	private static TreeItem[] comheadernodeitems = new TreeItem[maxnodes/4]; // list of headers in the comtree
	private static int comheadernodes =0; // enumerates above list
	private static int currentcom = 0; // keeps track of which com node is currently selected for modification

	// The following are UI elements that need to be accessed outside of main
	private static Composite editcomp;
	
	private static TabFolder tabfolder;
	private static TabFolder tabfolder2;
	private static int current_tree_tab = 1; // keeps track of which panel is open (nodetree, comtree, reftree, layertree)
	// 1 = nodetree, 2 = reftree, 3 = layertree, 4 = comtree

	private static Text circradtextbox;
	private static Text circinradtextbox;
	private static Text circbuftextbox;
	private static Text rectlentextbox;
	private static Text rectwidtextbox;
	private static Text rectcornerlentextbox;
	private static Text rectcornerwidtextbox;
	private static Text rectcornerangletextbox;
	private static Text rectcornercurvetextbox;
	private static Text rectbuflentextbox;
	private static Text rectbufwidtextbox;
	private static Text redcolortextbox;
	private static Text greencolortextbox;
	private static Text bluecolortextbox;
	private static Text spacetextbox;
	private static Text perpspacetextbox;
	private static Text filltextbox;
	private static Text bufboxtextbox;
	private static Text bufboxtextbox2;
	private static Text polyxtextbox;
	private static Text polyytextbox;
	private static Text polyttextbox;
	private static Text polybtextbox;
	private static Text polyetextbox;
	private static Text polyvextextbox;
	private static Text crotxtextbox;
	private static Text crotytextbox;
	private static Text crvwtextbox;
	private static Text newnodelayertextbox;
	private static Text extralentextbox;
	private static Text extrawidtextbox;
	private static Text refsettextbox;
	private static Text refangtextbox;
	private static Text varidtextbox;
	private static Text varvaltextbox;
	private static Text texttextbox; 
	private static Text textfonttypebox;
	private static Text textfontsizebox;
	private static Text pdfsizextextbox;
	private static Text pdfsizeytextbox;
	private static Text pdfmarginxtextbox;
	private static Text pdfmarginytextbox;
	private static Text pendxtextbox;
	private static Text cutscaletextbox;
	private static Text rescaletextbox;

	private static Text layertabtextbox;
	private static Text reftabtextbox;
	private static Text comtabtextbox;
	private static Text comtabidtextbox;
	private static Text comtablayertextbox;
	private static Text comtabsourcetextbox;
	private static Text comtabcenterplacetextbox;
	
	private static MenuItem viewcolorbyzitem;
	
	private static Combo polytypedropdown;

	// the following are seeds for the node coloring schema 
	// check getNodeColor(int) for a view of how these are used to generate a color
	// These are multiplicative factors for periodic functions that define the RGB content 
	// of the color, so changing these values causes the periods of R,G and B to change, so that
	// they overlap in a different repeating sequence of colors
	// colseed is saved in preferences and used to generate the other values
	private static int colseed = 0;
	private static int colseed1 = -1;
	private static int colseed2 = -1;
	private static int colseed3 = -1;
	private static int colseed4 = -1;

	private static boolean colorbyz = true; // whether or not to color nodes by their layers or to give
	// each header node a different color. Stored in preferences

	// the following are various fonts used to build the UI, constructed in main.
	private static Font font8;
	private static Font font10;
	private static Font fontui;
	private static Font fontuibold;

	// components of the UI. The window itself is the shell. 
	private static Shell shell;
	private static Display display;
	
	private static TreeItem lastrclicked; // keeps track of which nodeitem was last right clicked (used for r-click menu)
	private static TreeItem fillitem; // keeps track of the $FILL node, of which there can (should) only be one
	private static String filllen = "11.81"; // keeps track of the aforementioned node's fill value
	// by default, 11.81 makes this value correspond to the centimeter scale (e.g. a rectangle of width 1x1 will be
	// printed as a 1mm x 1mm square).
	
	
	private static boolean haslastsavedpath = false; // whether or not this device has been saved yet
	// if it has been saved already, the next time the user goes to save they will not be prompted to select a save location
	private static String lastsavedpath = ""; // stores the save location if this device has been saved already
	
	private static MapNode[] previewnodes; // list of the mapnodes in the generated preview, used to get 
	// information from the preview
	private static int previewnodecount = 0; // enumerates the above list
	private static boolean previewing = false; // whether or not the preview is active
	private static MapBoss previewmapboss; // the MapBoss is used to generate the selection maps when a node is
	// selected on the preview (i.e. it gives the outline of the selected node). Inherited from the Intepreter
	private static PreviewBoss previewboss; // keeps track of all the preview windows and their layers
	private static int[] prevselcol; // stores the color of the outline of selected nodes. Saved in preferences
	
	private static boolean previewauto = true; // whether or not to automatically generate new previews whenever
	// anything is changed. Handled through ActionHook()
	private static double previewfillfactor = 0.25; // the scale factor of the preview compared to the actual device. 
	// by default, set to 0.25, which means previews will be 1/4th the size of the actual device.
	// necessary to speed up the preview process. Saved in preferences
	
	private static TreeItem oldselitem; // keeps track of the last selected node (especially to recolor it after its been selected)
	private static Color oldselbackground; // what color did the aforementioned node use to have?
	 
	private static int undomax = 128; // how many undos to keep track of. Saved as backups in the backup folder
	//private static UndoHistory[] undolist;
	private static int undocurrent=0; // used to determine what number the next undo backup should be saved as
	private static boolean undoing = false; // whether or not the program is currently in the process of undoing something
	
	// these windows stored here so opening more than one at once can be avoided:
	private static PreferenceWindow prefwindow=null; // preference popup window
	private static PolyDrawWindow polydrawwindow=null; // polydraw popup window
	
	private static int polydrawx=5; // dimensions (radius) of the polydraw popup
	private static int polydrawy=5;
	

	@SuppressWarnings("unused")
	public static void main(String[] args){
		
		// this bit of code is used often to determine the proper directory slashing depending on OS
		String targetpath = System.getProperty("user.dir");
		if(targetpath.contains("/")){
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
			targetpath = targetpath + "icons/";
		}else{
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
			targetpath = targetpath + "icons\\";
		}
		
		
		// Establish random color seeds:
		// The functions used here are arbitrary.
		// The goal is to produce large (absolutely speaking) values so that
		// each color is very much different from the next.
		// (considering that smaller values produce smaller jumps)
		colseed1 = (int) (Math.random() * 300) + 10;
		colseed2 = 2 * ((int) (Math.random() * 150) - colseed1) + 15;
		colseed3 = 3 * ((int) (Math.random() * 75) + colseed2) + 20;
		colseed4 = (int) (Math.random() * 300) - colseed3 + 35;
		
		colseed=colseed4;
		
		// setup preview boss
		previewboss = new PreviewBoss();
		prevselcol = new int[3];
		prevselcol[0]=255; // selection outline color by default is purple
		prevselcol[1]=0;
		prevselcol[2]=255;
		
		// try to load the preferences. It is significant that this comes after the default color schema above:
		// that way, if a color scheme was saved, it will load over the default.
		loadPreferences();

		// Setting up the program window:
		display = new Display();
		shell = new Shell(display); // primary shell, will hold most of the UI
		GridLayout shellout = new GridLayout();
		shellout.numColumns = 2;
		shell.setLayout(shellout);
		shell.setText("AutoPAD Interface"); // name of the program (appears on the top menu bar)

		RowLayout rlayout = new RowLayout(); // generic row layout data that
											 // will be used for most components
		rlayout.justify = true; // Set the whole UI (more or less) to justify

		// Composites are essentially boxes full of components.
		// Note on reading UI code:
		// each component has two necessary variables for its constructor:
		// 1. What element to place this new component in
		// 2. What kind of border to use
		// Take note of #1 (ex. shell in the next line) to see where ui
		// components are being placed
		editcomp = new Composite(shell, SWT.BORDER); 
		// the editcomp Composite will hold most of our UI elements on the left side of the screen
		editcomp.setLayout(new GridLayout()); 
		// gridlayout so some of our components will have access to "grabExcessHorizontalSpace" et al

		// Trees are linked lists of nodes, essentially
		final TabFolder treefolder = new TabFolder(shell, SWT.BORDER);
		treefolder.setLayoutData(new GridData(780, 700)); // the size of this component is determined here
		// building all of the different tree folders:
		final TabItem treefoldertab1 = new TabItem(treefolder, SWT.NULL);
		treefoldertab1.setText("NodeTree");
		final TabItem treefoldertab2 = new TabItem(treefolder, SWT.NULL);
		treefoldertab2.setText("References");
		final TabItem treefoldertab3 = new TabItem(treefolder, SWT.NULL);
		treefoldertab3.setText("Layer");
		final TabItem treefoldertab4 = new TabItem(treefolder, SWT.NULL);
		treefoldertab4.setText("Combine");

		// this selection event keeps track of which folder is currently open
		// this way, if the reference tree is open the program won't accidentally add nodes to the nodetree
		// when the user is making modifications.
		treefolder.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(
					org.eclipse.swt.events.SelectionEvent event){
				TabItem ctab = treefolder.getSelection()[0]; // This should be your TabItem/CTabItem
				if(ctab.equals(treefoldertab1)){
					current_tree_tab = 1;
				}else if(ctab.equals(treefoldertab2)){
					current_tree_tab = 2;
				}else if(ctab.equals(treefoldertab3)){
					current_tree_tab = 3;
				}else if(ctab.equals(treefoldertab4)){
					current_tree_tab = 4;
				}
			}
		});
		if(!use_checks){ // generate the node tree depending on whether or not each item should have a checkbox
			nodetree = new Tree(treefolder, SWT.BORDER);
		}else{
			nodetree = new Tree(treefolder, SWT.BORDER | SWT.CHECK); 
		}
		// nodetree will be where we store all of the nodes for the current project
		nodetree.setBackground(new Color(display, 240, 240, 240)); // give the entire tree a simple off-white background
		nodetree.setLinesVisible(true); // these are the horizontal guidelines between nodes
		treefoldertab1.setControl(nodetree);
		// Font preparation work:
		FontData[] fd = nodetree.getFont().getFontData(); 
		// grab the base font from default values such as those assigned to the nodetree
		// automatically by SWT. This ensures we always have a working font data without much work on our part.
		FontData[] fd2 = nodetree.getFont().getFontData(); 
		
		FontData[] fd3 = nodetree.getFont().getFontData();
		
		FontData[] fd4 = nodetree.getFont().getFontData(); 
		int i = 0;
		while(i < fd.length){ // In some cases, multiple fonts may be
								// returned; this accounts for that scenario
								// (though usually we only get one)
			fd[i].setHeight(10);
			fd2[i].setHeight(8);
			fd[i].setStyle(SWT.BOLD);
			fd3[i].setHeight(12);
			fd4[i].setHeight(12);;
			fd4[i].setStyle(SWT.BOLD);
			i++;
		}
		font10 = new Font(display, fd); // 10sz, bold; used for header nodes (the ones that begin with a carat >)
		font8 = new Font(display, fd2); // 8sz; used for non-header nodes and hint-text in the editcomp ui
		fontui = new Font(display, fd3); // 12sz; used for textboxes, buttons and labels in the editcomp ui
		fontuibold = new Font(display, fd4); // bold version of above
		nodetree.setFont(font8); // nodetree is 8sz by default. In the addNode  and addNode_forced functions, we
		treefolder.setFont(fontui); // set header nodes to be 10sz.
		

		// Dispose Listeners are triggered when their object is disposed
		// (removed)
		nodetree.addDisposeListener(new DisposeListener(){ // this is frankly a niche case that will never be relevant
			public void widgetDisposed(DisposeEvent e){ // because the nodetree is only disposed when the program exits
				font10.dispose();
				font8.dispose(); 
				fontui.dispose(); 
			} 
		});

		// Selection listener
		nodetree.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event){
				if(event.detail == SWT.CHECK){ // only handle cases where a check was selected
					TreeItem item = (TreeItem) event.item; // get the item the user selected
					boolean checked = item.getChecked();
					checkItems(item, checked); // these functions handle checking/unchecking
					checkPath(item.getParentItem(), checked, false);
				}
			}
		});
		// MouseDown listener
		nodetree.addListener(SWT.MouseDown, new Listener(){
			@Override
			public void handleEvent(Event event){
				Point point = new Point(event.x, event.y);
				TreeItem item = nodetree.getItem(point); // get the item that was clicked
				if(item != null){
					lastrclicked=item;
					selectTreeItem(item); // this function handles updating the  currently selected item
				}
			}
		});

		// Reference tab
		Composite reftabcomp = new Composite(treefolder, SWT.NULL);
		reftabcomp.setLayout(new GridLayout());
		treefoldertab2.setControl(reftabcomp);
		// references:
		// each reference is its own base level node

		Composite refcomp = new Composite(reftabcomp, SWT.BORDER);
		GridLayout rcglayout = new GridLayout();
		rcglayout.numColumns = 5;
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true; // fill up
		gd.horizontalAlignment = SWT.FILL;
		refcomp.setLayoutData(gd);
		refcomp.setLayout(rcglayout);

		Label refcomplabel = new Label(refcomp, SWT.NULL);
		refcomplabel.setText("Reference Title : ");
		refcomplabel.setFont(fontui);
		reftabtextbox = new Text(refcomp, SWT.BORDER);
		reftabtextbox.setLayoutData(new GridData(40, 22));
		reftabtextbox.setText("0");
		reftabtextbox.setFont(fontui);
		Button reftabcreatebutton = new Button(refcomp, SWT.PUSH);
		reftabcreatebutton.setText("Create");
		reftabcreatebutton.setFont(fontui);

		reftabcreatebutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				createRef(reftabtextbox.getText());
			}
		});

		reftree = new Tree(reftabcomp, SWT.BORDER);
		reftree.setBackground(new Color(display, 240, 240, 240));
		reftree.setLinesVisible(true);
		// layertree.setBounds(clientArea.x, clientArea.y, 800,650);
		reftree.setFont(font8);

		// MouseDown listener
		reftree.addListener(SWT.MouseDown, new Listener(){
			@Override
			public void handleEvent(Event event){
				Point point = new Point(event.x, event.y);
				TreeItem rtitem = reftree.getItem(point); // get the item that
															// was clicked
				if(rtitem == null){
					return;
				}
				lastrclicked=rtitem;
				int i = 0;
				while(i < refheadernodes){
					if(refheadernodeitems[i].equals(rtitem)){
						currentref = i;
						return;
					}
					i++;
				}
				if(rtitem.getParentItem() != null){
					rtitem = rtitem.getParentItem();
				}
				i = 0;
				while(i < refheadernodes){
					if(refheadernodeitems[i].equals(rtitem)){
						currentref = i;
						break;
					}
					i++;
				}
			}
		});

		// Layer tab
		Composite layertabcomp = new Composite(treefolder, SWT.NULL);
		layertabcomp.setLayout(new GridLayout());
		treefoldertab3.setControl(layertabcomp);

		Composite layercomp = new Composite(layertabcomp, SWT.BORDER);
		GridLayout lcglayout = new GridLayout();
		lcglayout.numColumns = 5;
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true; // fill up
		gd.horizontalAlignment = SWT.FILL;
		layercomp.setLayoutData(gd);
		layercomp.setLayout(lcglayout);

		Label layercomplabel = new Label(layercomp, SWT.NULL);
		layercomplabel.setText("Layer : ");
		layercomplabel.setFont(fontui);
		layertabtextbox = new Text(layercomp, SWT.BORDER);
		layertabtextbox.setLayoutData(new GridData(40, 22));
		layertabtextbox.setText("0");
		
		layertabtextbox.setFont(fontui);
		SimpleTooltip layertabtextbox_tooltip = new SimpleTooltip(shell,layertabtextbox,"Vertical layer of the device.");
		Button layertabviewbutton = new Button(layercomp, SWT.PUSH);
		layertabviewbutton.setText("View");
		layertabviewbutton.setFont(fontui);

		layertabviewbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				int viewz = 0;
				try {
					viewz = Integer.parseInt(layertabtextbox.getText());
				}catch (Exception ex){
				}
				viewLayerTab(viewz); // this function handles adding new header
										// nodes
				currentlayertabz = viewz;
			}
		});

		layertree = new Tree(layertabcomp, SWT.BORDER);
		layertree.setBackground(new Color(display, 240, 240, 240));
		layertree.setLinesVisible(true);
		// layertree.setBounds(clientArea.x, clientArea.y, 800,650);
		layertree.setFont(font8);

		// MouseDown listener
		layertree.addListener(SWT.MouseDown, new Listener(){
			@Override
			public void handleEvent(Event event){
				Point point = new Point(event.x, event.y);
				TreeItem ltitem = layertree.getItem(point); // get the item that
															// was clicked
				if(ltitem == null){
					return;
				}
				
				TreeItem item = ltitem;
				int i = 0;
				while(i < layernodes){
					if(layernewnodeitems[i].equals(ltitem)){
						item = layerorignodeitems[i];
						break;
					}
					i++;
				}
				if(item != null){
					lastrclicked=item;
					int nnid = findHeaderNodeId(item);
					if(nnid == -1){
						nnid = findNodeId(item);
						if(nnid != -1){
							current = nodesource[nnid];
							if(current == -1){
								current = 0;
							}
						}
					}else{
						current = nnid;
					}
				}
			}
		});
		
		// Combine tab
		Composite comtabcomp = new Composite(treefolder, SWT.NULL);
		comtabcomp.setLayout(new GridLayout());
		treefoldertab4.setControl(comtabcomp);

		Composite comcomp = new Composite(comtabcomp, SWT.BORDER);
		GridLayout ccglayout = new GridLayout();
		ccglayout.numColumns = 10;
		comcomp.setLayoutData(gd);
		comcomp.setLayout(ccglayout);
		// contains new combine text and new combine button

		Label comcomplabel = new Label(comcomp, SWT.NULL);
		comcomplabel.setText("ID : ");
		comcomplabel.setFont(fontui);
		comtabtextbox = new Text(comcomp, SWT.BORDER);
		comtabtextbox.setLayoutData(new GridData(40, 22));
		comtabtextbox.setText("0");
		comtabtextbox.setFont(fontui);
		Button comtabcreatebutton = new Button(comcomp, SWT.PUSH);
		comtabcreatebutton.setText("Create Combine");
		comtabcreatebutton.setFont(fontui);

		comtabcreatebutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				createCom(comtabtextbox.getText());
				int i=0;
				try{
					i = Integer.parseInt(comtabtextbox.getText());
				}catch (Exception ex){return;}
				i++;
				comtabtextbox.setText("" + i);
			}
		});
		
		Composite comcomp2 = new Composite(comtabcomp, SWT.BORDER);
		comcomp2.setLayoutData(gd);
		comcomp2.setLayout(ccglayout);
		// contains new ID text and new ID button, layer text and button, directional dropdown and text and button
		
		Label comcomp2_1label = new Label(comcomp2, SWT.NULL);
		comcomp2_1label.setText("ID : ");
		comcomp2_1label.setFont(fontui);
		comtabidtextbox = new Text(comcomp2, SWT.BORDER);
		comtabidtextbox.setLayoutData(new GridData(40, 22));
		comtabidtextbox.setText("0");
		comtabidtextbox.setFont(fontui);
		Label comcomp2_2label = new Label(comcomp2, SWT.NULL);
		comcomp2_2label.setText("Layer : ");
		comcomp2_2label.setFont(fontui);
		comtablayertextbox = new Text(comcomp2, SWT.BORDER);
		comtablayertextbox.setLayoutData(new GridData(40, 22));
		comtablayertextbox.setText("0");
		comtablayertextbox.setFont(fontui);
		Button comtabcreateidbutton = new Button(comcomp2, SWT.PUSH);
		comtabcreateidbutton.setText("New ID");
		comtabcreateidbutton.setFont(fontui);

		comtabcreateidbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addComNode(comheadernodeitems[currentcom],"#ID " + comtabidtextbox.getText(),2);
				//currentcom=comheadernodes-1;
				
				if(comheadernodes-1<0){return;}
				addComNode(comheadernodeitems[comheadernodes-1],"#LAYER " + comtablayertextbox.getText(),3);
				comheadernodeitems[comheadernodes-1].setExpanded(true);
				int i=0;
				try{
					i = Integer.parseInt(comtabidtextbox.getText());
				}catch (Exception ex){return;}
				i++;
				comtabidtextbox.setText("" + i);
			}
		});
		
		final Combo comtabdirdropdown = new Combo(comcomp2, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		comtabdirdropdown.setFont(fontui);
		comtabdirdropdown.add("LEFT");
		comtabdirdropdown.add("RIGHT");
		comtabdirdropdown.add("UP");
		comtabdirdropdown.add("DOWN");
		comtabdirdropdown.select(0);
		
		/*comtabdirdropdown.addSelectionListener(new SelectionListener(){
			public void widgetSelected(SelectionEvent e){
				
			}

			public void widgetDefaultSelected(SelectionEvent e){
				// required but doesn't do anything in this case
			}
		});*/
		
		Label comcomp2_3label = new Label(comcomp2, SWT.NULL);
		comcomp2_3label.setText("Source ID : ");
		comcomp2_3label.setFont(fontui);
		comtabsourcetextbox = new Text(comcomp2, SWT.BORDER);
		comtabsourcetextbox.setLayoutData(new GridData(40, 22));
		comtabsourcetextbox.setText("0");
		comtabsourcetextbox.setFont(fontui);
		Button comtabsetdirbutton = new Button(comcomp2, SWT.PUSH);
		comtabsetdirbutton.setText("Direction");
		comtabsetdirbutton.setFont(fontui);

		comtabsetdirbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				int ins = currentcom;
				if(currentcom<0){
					currentcom=0;
				}
				if(comheadernodeitems[currentcom]!=null && !comheadernodeitems[currentcom].isDisposed()){
					if(comheadernodeitems[currentcom].getText().toUpperCase().startsWith("#COMBINE")){
						ins = comheadernodes-1;
					}
					addComNode(comheadernodeitems[ins],"#" + comtabdirdropdown.getText() + " " + comtabsourcetextbox.getText(),4);
				}
			}
		});
		
		Composite comcomp3 = new Composite(comtabcomp, SWT.BORDER);
		comcomp3.setLayoutData(gd);
		comcomp3.setLayout(ccglayout);
		// contains flipx and flipy button, rotational dropdown and button
		
		Button comtabflipxbutton = new Button(comcomp3, SWT.PUSH);
		comtabflipxbutton.setText("Flip X");
		comtabflipxbutton.setFont(fontui);

		comtabflipxbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				int ins = currentcom;
				if(currentcom<0){
					currentcom=0;
				}
				if(comheadernodeitems[currentcom]!=null && !comheadernodeitems[currentcom].isDisposed()){
					if(comheadernodeitems[currentcom].getText().toUpperCase().startsWith("#COMBINE")){
						ins = comheadernodes-1;
					}
					addComNode(comheadernodeitems[ins],"#FLIPX",5);
				}
			}
		});
		
		Button comtabflipybutton = new Button(comcomp3, SWT.PUSH);
		comtabflipybutton.setText("Flip Y");
		comtabflipybutton.setFont(fontui);

		comtabflipybutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				int ins = currentcom;
				if(currentcom<0){
					currentcom=0;
				}
				if(comheadernodeitems[currentcom]!=null && !comheadernodeitems[currentcom].isDisposed()){
					if(comheadernodeitems[currentcom].getText().toUpperCase().startsWith("#COMBINE")){
						ins = comheadernodes-1;
					}
					addComNode(comheadernodeitems[ins],"#FLIPY",6);
				}
			}
		});
		
		final Combo comtabrotdropdown = new Combo(comcomp3, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		comtabrotdropdown.setFont(fontui);
		comtabrotdropdown.add("0");
		comtabrotdropdown.add("90");
		comtabrotdropdown.add("180");
		comtabrotdropdown.add("270");
		comtabrotdropdown.select(0);
		
		comtabrotdropdown.addSelectionListener(new SelectionListener(){
			public void widgetSelected(SelectionEvent e){

			}

			public void widgetDefaultSelected(SelectionEvent e){
				// required but doesn't do anything in this case
			}
		});
		
		Button comtabrotbutton = new Button(comcomp3, SWT.PUSH);
		comtabrotbutton.setText("Rotate");
		comtabrotbutton.setFont(fontui);

		comtabrotbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				int ins = currentcom;
				if(currentcom<0){
					currentcom=0;
				}
				if(comheadernodeitems[currentcom]!=null && !comheadernodeitems[currentcom].isDisposed()){
					if(comheadernodeitems[currentcom].getText().toUpperCase().startsWith("#COMBINE")){
						ins = comheadernodes-1;
					}
					addComNode(comheadernodeitems[ins],"#ROTATE " + comtabrotdropdown.getText(),7);
				}
			}
		});
		
		Composite comcomp4 = new Composite(comtabcomp, SWT.BORDER);
		comcomp4.setLayoutData(gd);
		comcomp4.setLayout(ccglayout);
		// contains squarespace and drawboxes buttons
		
		Button comtabssbutton = new Button(comcomp4, SWT.PUSH);
		comtabssbutton.setText("Force Squares");
		comtabssbutton.setFont(fontui);

		comtabssbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if(currentcom<0){
					currentcom=0;
				}
				TreeItem realins = comheadernodeitems[currentcom];
				if(realins==null && !comheadernodeitems[currentcom].isDisposed()){return;}
				while(!realins.getText().toUpperCase().startsWith("#COMBINE")){
					realins=realins.getParentItem();
				}
				addComNode(realins,"#SQUARE",8);
			}
		});
		
		Button comtabdrawboxbutton = new Button(comcomp4, SWT.PUSH);
		comtabdrawboxbutton.setText("Draw Boxes");
		comtabdrawboxbutton.setFont(fontui);

		comtabdrawboxbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if(currentcom<0){
					currentcom=0;
				}
				TreeItem realins = comheadernodeitems[currentcom];
				if(realins==null && !comheadernodeitems[currentcom].isDisposed()){return;}
				while(!realins.getText().toUpperCase().startsWith("#COMBINE")){
					realins=realins.getParentItem();
				}
				addComNode(realins,"#BOX",9);
			}
		});
		
		Label comcomp4label = new Label(comcomp4, SWT.NULL);
		comcomp4label.setText("Place : ");
		comcomp4label.setFont(fontui);
		comtabcenterplacetextbox = new Text(comcomp4, SWT.BORDER);
		comtabcenterplacetextbox.setLayoutData(new GridData(40, 22));
		comtabcenterplacetextbox.setText("0");
		comtabcenterplacetextbox.setFont(fontui);
		
		Button comtabsetcenterbutton = new Button(comcomp4, SWT.PUSH);
		comtabsetcenterbutton.setText("Set Center");
		comtabsetcenterbutton.setFont(fontui);
		
		comtabsetcenterbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if(currentcom<0){
					currentcom=0;
				}
				TreeItem realins = comheadernodeitems[currentcom];
				if(realins==null && !comheadernodeitems[currentcom].isDisposed()){return;}
				while(!realins.getText().toUpperCase().startsWith("#COMBINE")){
					realins=realins.getParentItem();
				}
				addComNode(realins,"#CENTER " + comtabcenterplacetextbox.getText(),10);
			}
		});

		comtree = new Tree(comtabcomp, SWT.BORDER);
		comtree.setBackground(new Color(display, 240, 240, 240));
		comtree.setLinesVisible(true);
		// layertree.setBounds(clientArea.x, clientArea.y, 800,650);
		comtree.setFont(font8);

		// MouseDown listener
		comtree.addListener(SWT.MouseDown, new Listener(){
			@Override
			public void handleEvent(Event event){
				Point point = new Point(event.x, event.y);
				TreeItem rtitem = comtree.getItem(point); // get the item that
															// was clicked
				if(rtitem == null){
					return;
				}
				lastrclicked=rtitem;
				int i = 0;
				while(i < comheadernodes){
					if(comheadernodeitems[i].equals(rtitem)){
						currentcom = i;
						return;
					}
					i++;
				}
				if(rtitem.getParentItem() != null){
					rtitem = rtitem.getParentItem();
				}
				i = 0;
				while(i < comheadernodes){
					if(comheadernodeitems[i].equals(rtitem)){
						currentcom = i;
						break;
					}
					i++;
				}
			}
		});
		

		// Node preparation work: create the default nodes
		nodesource[0] = -1; 
		// this is the only node that has a source of -1,
		// sometimes this is relevant and must be accounted for
		// ex. when you pull headernodeitems[nodesource[i]] make sure i isn't 0
		nodeitems[0] = new TreeItem(nodetree, 0);
		nodeitems[0].setText(">0"); // the first header node (this should be the
									// only node on this level, all others
									// should have this
		nodeangle[0] = 0; // one as a parent)
		headernode[0] = new MapNode(0, 0); // worth noting here that this node is
											// fixed at layer 0.
		headernodeitems[0] = nodeitems[0]; // <-- the only node where this
											// equality is true.
		nodetype[0] = 0; // recall 0 is the type of header nodes
		nodeplace[0]=0;
		headernodes++; // recall headernodes is the count of header nodes
		nodes++; // whereas nodes is the count of total nodes (inc. header and
					// non-header)
		autoInsertPlace(nodeitems[0], 0); // this function is used to insert the
		// "$PLACE #" nodes that each header node must have set the font of the header node we
		// made to the header font
		headernodeitems[0].setFont(font10); 
		setupNode(nodeitems[nodes - 2], headernodes - 1); 
		// this function handles polishing off the new nodes we make
		// it checks them and opens them up automatically, as well as sets the
		// background color
		// we don't need to call this for the place node or the fill node (see
		// next line) as those functions call it already
		setFill("11.81"); // create the default fill node (there should only be one
						// per script, so we might as well make it
						// automatically)
		// recall the fill node is necessary to tell the DeviceDraw.java to
		// actually create an image. The number is the pixel conversion
		// factor (ex. Circle 1 becomes a circle of radius 20 pixels with $FILL
		// 20)
		
		oldselitem = nodeitems[0];
		oldselbackground = nodeitems[0].getBackground();

		// Now we fill out the editcomp UI
		// First is the UI for making new header nodes
		Composite newnodecomp = new Composite(editcomp, SWT.BORDER);
		GridLayout nnglayout = new GridLayout();
		nnglayout.numColumns = 5; // this should actually be 4 but excess
									// columns don't affect us here as we only
									// use one row.
		newnodecomp.setLayout(nnglayout);
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true; // always use these two variables
												// in tandem
		gd.horizontalAlignment = SWT.FILL; // they don't seem to work
											// independently as you would
											// imagine
		newnodecomp.setLayoutData(gd);// new GridData(336,30));

		Composite newnodetextcomp = new Composite(newnodecomp, SWT.NULL); 
		// comp used to hold layer,angle labels & text boxes
		GridLayout nntcglayout = new GridLayout();
		nntcglayout.numColumns = 2; // will be a 2x2 grid
		newnodetextcomp.setLayout(nntcglayout);
		// Angle : label
		Label newnodelabel = new Label(newnodetextcomp, SWT.NULL);
		newnodelabel.setText("Angle : ");
		newnodelabel.setFont(fontui);
		// angle text box for new nodes
		final Text newnodeangletextbox = new Text(newnodetextcomp, SWT.BORDER);
		newnodeangletextbox.setLayoutData(new GridData(40, 22)); // <-- these numbers determine the size of the text box
		newnodeangletextbox.setText("0");
		newnodeangletextbox.setFont(fontui);
		SimpleTooltip newnodeangletextbox_tooltip = new SimpleTooltip(shell,newnodeangletextbox,"Direction of the node from the last.");

		// Layer : label
		Label newnodelabel2 = new Label(newnodetextcomp, SWT.NULL);
		newnodelabel2.setText("Layer : ");
		newnodelabel2.setFont(fontui);
		// layer text box for new nodes
		newnodelayertextbox = new Text(newnodetextcomp, SWT.BORDER);
		newnodelayertextbox.setLayoutData(new GridData(40, 22));
		newnodelayertextbox.setText("0");
		newnodelayertextbox.setFont(fontui);
		SimpleTooltip newnodelayertextbox_tooltip = new SimpleTooltip(shell,newnodelayertextbox,"Vertical layer of the device.");

		// New button; for making new header nodes at currently selected header
		// node
		Button newnode = new Button(newnodecomp, SWT.PUSH);
		newnode.setText("New");
		newnode.setFont(fontui);
		newnode.setLayoutData(new GridData(60, 40));
		SimpleTooltip newnode_tooltip = new SimpleTooltip(shell,newnode,"Add a new node, attached to the currently selected node.");
		// Selection Listener: triggered when the button is pressed
		newnode.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				try {
					current_z = Integer.parseInt(newnodelayertextbox.getText());
					// update the current layer to whatever is in the layertextbox
					// this is important because addnode will add a different layer
					// if current_z changes instead of an angle node
				}catch (Exception ex){
				}
				if(current_tree_tab == 2){
					if(refheadernodeitems[currentref]!=null){
						addNode(refheadernodeitems[currentref],
							newnodeangletextbox.getText());
					}
					actionHook(false);
					return;
				}
				if(headernodeitems[current]!=null){
					addNode(headernodeitems[current], newnodeangletextbox.getText()); 
				}
				actionHook(false);
				// this function handles adding new header nodes
				// recall that headernodeitems[current] will be the currently
				// selected header node (and thus parent of the new node)
			}
		});
		// Set button; for changing the angle/layer value of an existing node
		// (the currently selected node)
		Button setnode = new Button(newnodecomp, SWT.PUSH);
		setnode.setText("Set");
		setnode.setLayoutData(new GridData(60, 40));
		setnode.setFont(fontui);
		SimpleTooltip setnode_tooltip = new SimpleTooltip(shell,setnode,"Set the currently-selected node's layer or angle.");
		// Selection Listener; triggered by button press
		setnode.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if(current==0 && current_tree_tab==1){ // cannot update the first headernode
					return;
				}
				try {
					current_z = Integer.parseInt(newnodelayertextbox.getText()); // update current layer
				}catch (Exception ex){
				}
				if(current_tree_tab ==2){ // ref nodes can only set angle
					if(refheadernodeitems[currentref]!=null){
						String firstpart = refheadernodeitems[currentref].getText();
						firstpart = firstpart.substring(0,firstpart.indexOf("|"));
						System.out.println(" FIRST PART " + firstpart);
						refheadernodeitems[currentref].setText(firstpart  + "|>ANGLE "
								+ newnodeangletextbox.getText());
						actionHook(true);
					}
					return;
				} // end reference pathway
				
				// need to loop through nodes to find which nodeitem
				// corresponds to this headernodeitem
				int i = 0;
				while(i < nodes){
					if(nodeitems[i].equals(headernodeitems[current])){
						i = nodesource[i]; // now we can grab the nodesource
											// of the current node
						// we need this because each header node has > and
						// then the nodesource id of
						// their parent as the first bit of their text.
						// However, nodesource is indexed
						// by node id and not headernode id.
						break;
					}
					i++;
				}

				// There are two types of header nodes, and which one we
				// have will change how we need to update here.
				if(headernodeitems[current].getText().startsWith(
						">" + nodeplace[i] + "|>LAYER")){
					// if it's a layer node, then we can only update the
					// layer value
					headernodeitems[current].setText(">" + nodeplace[i] + "|>LAYER "
							+ newnodelayertextbox.getText()); // technically
																// current_z
					// would work here as we already updated it to be the
					// value of the textbox

					// Handling recoloring in case color-by-layer is on
					// requires extensive code:
					int newz = 0;
					try {
						newz = Integer.parseInt(newnodelayertextbox
								.getText()); // first determine the new z
												// level
					}catch (Exception ex){
					}

					int[] newnodez = new int[headernodes];
					// we need to have a list of what the new Z levels for each affected node will be
					// recall that angle-children nodes of this node depend
					// upon this node for their layer, so we need to update
					// them
					// however if we set the new Z in our loop it will mess
					// with the logic as one of the checks we perform is
					// whether or not
					// there is an unbroken chain of the same Z level from
					// the child node to the parent (else the child depends
					// on a different
					// node for its Z level)
					int ni = 0;
					while(ni < headernodes){ // loop through all header
												// nodes
						newnodez[ni] = headernode[ni].getFillZ(); 
						// find that header node's Z
						boolean linked = false; // is this node a child of
												// the modified node?
						boolean nozchange = true; 
						// is there an unbroken chain of a constant Z
						// level between this node and the modified node?
						TreeItem curitem = headernodeitems[ni];
						// starting at this node, search up the tree until we run out of nodes to search through
						// (i.e. get past >0)
						while(curitem != null){ 
							int curi = findHeaderNodeId(curitem); 
							// this function finds the headernode id based on the TreeItem of the node
							if(curitem.equals(headernodeitems[current])){
								// if we ran into the modified node, then this node IS linked to it
								linked = true;
							}
							if(headernode[curi].getFillZ() != headernode[current]
									.getFillZ()){ // if we run into a different Z level, there isn't
								nozchange = false; // an unbroken chain of a  constant Z level between this node and the modified
							}
							if(linked || !nozchange){
								break;
							} // in either case, leave the loop (either hit
								// the end (modified node) or failed)
							curitem = curitem.getParentItem(); 
							// if we're still in the loop, grab the next node up on the chain
						}
						if(linked && nozchange){
							// only if the node is linked to the modified AND is on the same uninterrupted Z plane
							// do we set its Z to the new Z of the modified node
							newnodez[ni] = newz; 
							// recall that we set it in this temporary array
							// first to avoid messing with future searching in this loop
						}
						ni++;
					}
					ni = 0;
					while(ni < headernodes){ // now set all the new Z values
						headernode[ni].setFillZ(newnodez[ni]);
						ni++;
					}
					headernode[current].setFillZ(newz);
					if(colorbyz){ // if we're coloring by layer, update the colors
						refreshColors(); // this function refreshes node colors
					}
					actionHook(true);
				}else{
					// comparably, updating the angle is much easier.
					headernodeitems[current].setText(">" + nodeplace[i] + "|>ANGLE "
							+ newnodeangletextbox.getText());
					actionHook(true);
				}
			}
		}); // end of listener
		// Clear button; removes a node (and its children!)
		Button clearnode = new Button(newnodecomp, SWT.PUSH);
		clearnode.setText("Clear");
		clearnode.setFont(fontui);
		clearnode.setLayoutData(new GridData(60, 40));
		SimpleTooltip clearnode_tooltip = new SimpleTooltip(shell,clearnode,"Delete the currently selected node.");
		// Selection Listener; button press triggers this
		clearnode.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if(current != 0 && current_tree_tab==1){ // Cannot remove >0 node (the reader should note a trend, here)
					headernodeitems[current].removeAll(); // this function removes all children nodes
					headernodeitems[current].dispose(); // this removes the node itself
					current = 0; // reset current to 0, so that other functions that rely upon current don't get
					// null pointers from this process.
					actionHook(true);
				}else if(currentref!=0 && current_tree_tab==2){
					refheadernodeitems[currentref].removeAll();
					refheadernodeitems[currentref].dispose();
					currentref=0;
					actionHook(true);
				}
			}
		});

		// Now, begin construction of the first tab folder
		// this folder is the shape folder, it contains the Rect(angle),
		// Circ(le), and Poly(gon) tabs
		tabfolder = new TabFolder(editcomp, SWT.BORDER);
		tabfolder.setLayoutData(new GridData(330, 270)); // <-- these numbers refer to the size of this component
		TabItem f1tab1item = new TabItem(tabfolder, SWT.NULL);
		f1tab1item.setText("Rect"); // our first tab; the Rect tab will have rectangle functions in it
		if(useicons){
			f1tab1item.setImage(new Image(display,targetpath + "ICONRect.png"));
		}
		tabfolder.setFont(fontui); // note here that tabitems don't hold their own font data;
		// they share the font data that their tabfolder holds

		// Rect Properties
		Composite recttabcomp = new Composite(tabfolder, SWT.BORDER);
		recttabcomp.setLayout(new GridLayout());
		f1tab1item.setControl(recttabcomp); 
		// each tabitem has ONE component attached to it, setControl determines which component that is

		Label rectlabel0 = new Label(recttabcomp, SWT.NULL);
		rectlabel0.setText("Rectangle Size");
		rectlabel0.setFont(fontui);

		Composite rectcomp = new Composite(recttabcomp, SWT.NONE);
		rectcomp.setLayout(rlayout);
		// rect length label
		Label rectlabel1 = new Label(rectcomp, SWT.NULL);
		rectlabel1.setFont(fontui);
		rectlabel1.setText("Length : ");
		// rect len text box
		rectlentextbox = new Text(rectcomp, SWT.BORDER);
		rectlentextbox.setLayoutData(new RowData(40, 22));
		rectlentextbox.setText("0");
		rectlentextbox.setFont(fontui);
		// rect width label
		Label rectlabel2 = new Label(rectcomp, SWT.NULL);
		rectlabel2.setText("Width : ");
		rectlabel2.setFont(fontui);
		// rect width text box
		rectwidtextbox = new Text(rectcomp, SWT.BORDER);
		rectwidtextbox.setLayoutData(new RowData(40, 22));
		rectwidtextbox.setText("0");
		rectwidtextbox.setFont(fontui);
		// set rect button
		Button setrect = new Button(rectcomp, SWT.PUSH);
		setrect.setText("Set Rect");
		setrect.setFont(fontui);
		SimpleTooltip setrect_tooltip = new SimpleTooltip(shell,setrect,"Make the currently selected node a rectangle.");
		setrect.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setRect(rectlentextbox.getText(), rectwidtextbox.getText());
				// when this button is pressed, call setRect
				// this handles adding or updating a rect node to the current
				// header node
			}
		});

		Label rectbuflabel0 = new Label(recttabcomp, SWT.NULL);
		rectbuflabel0.setText("Buffer Size");
		rectbuflabel0.setFont(fontui);

		Composite rectbufcomp = new Composite(recttabcomp, SWT.NONE);
		rectbufcomp.setLayout(rlayout);
		// rect buffer length label
		Label rectbuflabel1 = new Label(rectbufcomp, SWT.NULL);
		rectbuflabel1.setFont(fontui);
		rectbuflabel1.setText("Length : ");
		// rect buffer length text box
		rectbuflentextbox = new Text(rectbufcomp, SWT.BORDER);
		rectbuflentextbox.setLayoutData(new RowData(40, 22));
		rectbuflentextbox.setText("0");
		rectbuflentextbox.setFont(fontui);
		// rect buffer width label
		Label rectbuflabel2 = new Label(rectbufcomp, SWT.NULL);
		rectbuflabel2.setText("Width : ");
		rectbuflabel2.setFont(fontui);
		// rect buffer width text box
		rectbufwidtextbox = new Text(rectbufcomp, SWT.BORDER);
		rectbufwidtextbox.setLayoutData(new RowData(40, 22));
		rectbufwidtextbox.setText("0");
		rectbufwidtextbox.setFont(fontui);
		// set rect buffer button
		Button setrectbuf = new Button(rectbufcomp, SWT.PUSH);
		setrectbuf.setText("Set Buffer");
		setrectbuf.setFont(fontui);
		SimpleTooltip setrectbuf_tooltip = new SimpleTooltip(shell,setrectbuf,"Give the selected node a black buffer border.");
		setrectbuf.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setRectBuf(rectbuflentextbox.getText(),
						rectbufwidtextbox.getText());
				// adds or updates a buffer node based on the data in the above
				// text boxes when you press this button
			}
		});
		
		Label rectcornerlabel0 = new Label(recttabcomp, SWT.NULL);
		rectcornerlabel0.setText("Corner Piece");
		rectcornerlabel0.setFont(fontui);

		Composite rectcornercomp = new Composite(recttabcomp, SWT.NONE);
		rectcornercomp.setLayout(rlayout);
		// corner length label
		Label rectcornerlabel1 = new Label(rectcornercomp, SWT.NULL);
		rectcornerlabel1.setFont(fontui);
		rectcornerlabel1.setText("Base : ");
		// corner len text box
		rectcornerlentextbox = new Text(rectcornercomp, SWT.BORDER);
		rectcornerlentextbox.setLayoutData(new RowData(35, 22));
		rectcornerlentextbox.setText("0");
		rectcornerlentextbox.setFont(fontui);
		// corner width label
		Label rectcornerlabel2 = new Label(rectcornercomp, SWT.NULL);
		rectcornerlabel2.setText("Height : ");
		rectcornerlabel2.setFont(fontui);
		// corner width text box
		rectcornerwidtextbox = new Text(rectcornercomp, SWT.BORDER);
		rectcornerwidtextbox.setLayoutData(new RowData(35, 22));
		rectcornerwidtextbox.setText("0");
		rectcornerwidtextbox.setFont(fontui);
		
		Composite rectcornercomp2 = new Composite(recttabcomp, SWT.NONE);
		rectcornercomp2.setLayout(rlayout);
		// corner angle label
		Label rectcornerlabel3 = new Label(rectcornercomp, SWT.NULL);
		rectcornerlabel3.setText("Angle : ");
		rectcornerlabel3.setFont(fontui);
		// corner angle text box
		rectcornerangletextbox = new Text(rectcornercomp, SWT.BORDER);
		rectcornerangletextbox.setLayoutData(new RowData(35, 22));
		rectcornerangletextbox.setText("90");
		rectcornerangletextbox.setFont(fontui);
		// set corner button
		Button setrectcorner = new Button(rectcornercomp2, SWT.PUSH);
		setrectcorner.setText("Set Corner");
		setrectcorner.setFont(fontui);
		SimpleTooltip setrectcorner_tooltip = new SimpleTooltip(shell,setrectcorner,"Make the currently selected node a cornerpiece.");
		setrectcorner.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setCorner(rectcornerlentextbox.getText(), rectcornerwidtextbox.getText(), rectcornerangletextbox.getText());
				// when this button is pressed, call setRect
				// this handles adding or updating a rect node to the current
				// header node
			}
		});
		// corner curveweight text box
		Label rectcornerlabel4 = new Label(rectcornercomp2, SWT.NULL);
		rectcornerlabel4.setText("Curve : ");
		rectcornerlabel4.setFont(fontui);
		// rect width text box
		rectcornercurvetextbox = new Text(rectcornercomp2, SWT.BORDER);
		rectcornercurvetextbox.setLayoutData(new RowData(35, 22));
		rectcornercurvetextbox.setText("0.0");
		rectcornercurvetextbox.setFont(fontui);
		// set corner curve button
		Button setrectcornercurve = new Button(rectcornercomp2, SWT.PUSH);
		setrectcornercurve.setText("Set Curve");
		setrectcornercurve.setFont(fontui);
		SimpleTooltip setrectcornercurve_tooltip = new SimpleTooltip(shell,setrectcornercurve,"Give this cornerpiece a curve.");
		setrectcornercurve.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setCornerCurve(rectcornercurvetextbox.getText());
				// when this button is pressed, call setRect
				// this handles adding or updating a rect node to the current
				// header node
			}
		});
		// END Rect Properties

		// Next tab: Circ (for circle components)
		TabItem f1tab2item = new TabItem(tabfolder, SWT.NULL);
		f1tab2item.setText("Circ");
		if(useicons){
			f1tab2item.setImage(new Image(display,targetpath + "ICONCirc.png"));
		}

		// Circ Properties
		Composite circtabcomp = new Composite(tabfolder, SWT.BORDER);
		circtabcomp.setLayout(new GridLayout());
		f1tab2item.setControl(circtabcomp);

		Label circlabel0 = new Label(circtabcomp, SWT.NULL);
		circlabel0.setText("Circle Size");
		circlabel0.setFont(fontui);

		Composite circcomp = new Composite(circtabcomp, SWT.NONE);
		circcomp.setLayout(rlayout);
		// circle radius label
		Label circlabel1 = new Label(circcomp, SWT.NULL);
		circlabel1.setText("Radius : ");
		circlabel1.setFont(fontui);
		// circle radius text box
		circradtextbox = new Text(circcomp, SWT.BORDER);
		circradtextbox.setLayoutData(new RowData(40, 22));
		circradtextbox.setText("0");
		circradtextbox.setFont(fontui);
		// circle inner radius label
		Label circlabel2 = new Label(circcomp, SWT.NULL);
		circlabel2.setText("Inner : ");
		circlabel2.setFont(fontui);
		// circle inner radius text box
		circinradtextbox = new Text(circcomp, SWT.BORDER);
		circinradtextbox.setLayoutData(new RowData(40, 22));
		circinradtextbox.setText("0");
		circinradtextbox.setFont(fontui);
		// set circle button
		Button setcirc = new Button(circcomp, SWT.PUSH);
		setcirc.setText("Set Circle");
		setcirc.setFont(fontui);
		SimpleTooltip setcirc_tooltip = new SimpleTooltip(shell,setcirc,"Make the currently selected node a circle.");
		setcirc.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setCirc(circradtextbox.getText(), circinradtextbox.getText());
				// adds or updates a circle node when pressed
			}
		});

		Label circbuflabel0 = new Label(circtabcomp, SWT.NULL);
		circbuflabel0.setText("Buffer Size");
		circbuflabel0.setFont(fontui);

		Composite circbufcomp = new Composite(circtabcomp, SWT.NONE);
		circbufcomp.setLayout(rlayout);
		// circle buffer length label
		Label circbuflabel1 = new Label(circbufcomp, SWT.NULL);
		circbuflabel1.setText("Length : ");
		circbuflabel1.setFont(fontui);
		// circle buffer length text box
		circbuftextbox = new Text(circbufcomp, SWT.BORDER);
		circbuftextbox.setLayoutData(new RowData(40, 22));
		circbuftextbox.setText("0");
		circbuftextbox.setFont(fontui);
		// set circle buffer button
		Button setcircbuf = new Button(circbufcomp, SWT.PUSH);
		setcircbuf.setText("Set Buffer");
		setcircbuf.setFont(fontui);
		SimpleTooltip setcircbuf_tooltip = new SimpleTooltip(shell,setcircbuf,"Give the selected node a black buffer border.");
		setcircbuf.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setBuf(circbuftextbox.getText());
				// sets or updates the buffer of a circle
				// note that circle buffers differ from rectangle buffers as
				// they can be single-value
				// also note that rect buffers work with circles and vice versa
			}
		});
		// END Circ Proprerties
		
		// Next tab: Text (for text components)
		
		// was added after the fact, so these are '2a' instead of, say, '3'
		TabItem f1tab2aitem = new TabItem(tabfolder, SWT.NULL);
		f1tab2aitem.setText("Text");
		if(useicons){
			f1tab2aitem.setImage(new Image(display,targetpath + "ICONText.png"));
		}

		// Circ Properties
		Composite texttabcomp = new Composite(tabfolder, SWT.BORDER);
		texttabcomp.setLayout(new GridLayout());
		f1tab2aitem.setControl(texttabcomp);

		Label textlabel0 = new Label(texttabcomp, SWT.NULL);
		textlabel0.setText("Text");
		textlabel0.setFont(fontui);

		Composite textcomp = new Composite(texttabcomp, SWT.NONE);
		textcomp.setLayout(rlayout);
		// text label
		Label textlabel1 = new Label(textcomp, SWT.NULL);
		textlabel1.setText("Text : ");
		textlabel1.setFont(fontui);
		// text text box
		texttextbox = new Text(textcomp, SWT.BORDER);
		texttextbox.setLayoutData(new RowData(160, 22));
		texttextbox.setText("");
		texttextbox.setFont(fontui);
		// set text button
		Button settext = new Button(textcomp, SWT.PUSH);
		settext.setText("Set Text");
		settext.setFont(fontui);
		SimpleTooltip settext_tooltip = new SimpleTooltip(shell,settext,"Make the currently selected node a text.");
		settext.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setText(texttextbox.getText());
				// adds or updates a circle node when pressed
			}
		});
		// fontsize
		Composite textcomp2 = new Composite(texttabcomp, SWT.NONE);
		textcomp2.setLayout(rlayout);
		// font size label
		Label textlabel2 = new Label(textcomp2, SWT.NULL);
		textlabel2.setText("Scale : ");
		textlabel2.setFont(fontui);
		// font size text box
		textfontsizebox = new Text(textcomp2, SWT.BORDER);
		textfontsizebox.setLayoutData(new RowData(100, 22));
		textfontsizebox.setText("1.0");
		textfontsizebox.setFont(fontui);
		// set font size button
		Button settextsize = new Button(textcomp2, SWT.PUSH);
		settextsize.setText("Set Font Scale");
		settextsize.setFont(fontui);
		SimpleTooltip settextsize_tooltip = new SimpleTooltip(shell,settextsize,"Change the size of the text font.");
		settextsize.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setFontSize(textfontsizebox.getText());
				// adds or updates a circle node when pressed
			}
		});
		// font
		Composite textcomp3 = new Composite(texttabcomp, SWT.NONE);
		textcomp3.setLayout(rlayout);
		// font type label
		Label textlabel3 = new Label(textcomp3, SWT.NULL);
		textlabel3.setText("Font : ");
		textlabel3.setFont(fontui);
		// font type text box
		textfonttypebox = new Text(textcomp3, SWT.BORDER);
		textfonttypebox.setLayoutData(new RowData(160, 22));
		textfonttypebox.setText("Verdana");
		textfonttypebox.setFont(fontui);
		// set font type button
		Button settextfont = new Button(textcomp3, SWT.PUSH);
		settextfont.setText("Set Font");
		settextfont.setFont(fontui);
		SimpleTooltip settextfont_tooltip = new SimpleTooltip(shell,settextfont,"Change the font of the text.");
		settextfont.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setFontType(textfonttypebox.getText());
				// adds or updates a circle node when pressed
			}
		});
		// END Text Properties

		// Poly tab: components for polygons
		TabItem f1tab3item = new TabItem(tabfolder, SWT.NULL);
		f1tab3item.setText("Poly");
		if(useicons){
			f1tab3item.setImage(new Image(display,targetpath + "ICONPoly.png"));
		}

		// Poly Properties
		Composite polytabcomp = new Composite(tabfolder, SWT.BORDER);
		polytabcomp.setLayout(new GridLayout());
		f1tab3item.setControl(polytabcomp);

		// set poly button
		Button openpolyeditor = new Button(polytabcomp, SWT.PUSH);
		openpolyeditor.setText("Open Polygon Editor");
		openpolyeditor.setFont(fontui);
		SimpleTooltip openpolyeditor_tooltip = new SimpleTooltip(shell,openpolyeditor,"Open the polygon editor window. Design will be exported to the currently selected node.");
		openpolyeditor.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				openPolyDraw();
				// adds a poly node which flags this header node as a polygon
			}
		});
		
		// set poly button
		Button setpoly = new Button(polytabcomp, SWT.PUSH);
		setpoly.setText("Set Poly");
		setpoly.setFont(fontui);
		SimpleTooltip setpoly_tooltip = new SimpleTooltip(shell,setpoly,"Make the currently selected node a polygon.");
		setpoly.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setPoly();
				// adds a poly node which flags this header node as a polygon
			}
		});

		// the following deals with the vertex values for polygons
		// note vertex is sometimes abbreviated as vex here
		Composite polyvextotalcomp = new Composite(polytabcomp, SWT.BORDER);
		GridLayout pvtcglayout = new GridLayout();
		polyvextotalcomp.setLayout(pvtcglayout);

		Composite polycomp = new Composite(polyvextotalcomp, SWT.NONE);
		polycomp.setLayout(rlayout);

		// poly vertex X label
		Label polylabel1 = new Label(polycomp, SWT.NULL);
		polylabel1.setText("X : ");
		polylabel1.setFont(fontui);
		// poly vertex X text box
		polyxtextbox = new Text(polycomp, SWT.BORDER);
		polyxtextbox.setLayoutData(new RowData(40, 22));
		polyxtextbox.setText("0");
		polyxtextbox.setFont(fontui);
		// poly vertex Y label
		Label polylabel2 = new Label(polycomp, SWT.NULL);
		polylabel2.setText("Y : ");
		polylabel2.setFont(fontui);
		// poly vertex Y text box
		polyytextbox = new Text(polycomp, SWT.BORDER);
		polyytextbox.setLayoutData(new RowData(40, 22));
		polyytextbox.setText("0");
		polyytextbox.setFont(fontui);

		Composite polycomp2 = new Composite(polyvextotalcomp, SWT.NONE);
		polycomp2.setLayout(rlayout);
		
		
		// (poly vertex type used to be on the top row)
		// poly vertex type text box
		/*polyttextbox = new Text(polycomp, SWT.BORDER);
		polyttextbox.setLayoutData(new RowData(40, 22));
		polyttextbox.setText("0");
		polyttextbox.setFont(fontui);*/
		// poly vertex buffer label
		Label polylabel4 = new Label(polycomp, SWT.NULL);
		polylabel4.setText("Buffer : ");
		polylabel4.setFont(fontui);
		// poly vertex buffer text box
		polybtextbox = new Text(polycomp, SWT.BORDER);
		polybtextbox.setLayoutData(new RowData(40, 22));
		polybtextbox.setText("0");
		polybtextbox.setFont(fontui);
		// poly vertex extra label
		Label polylabel5 = new Label(polycomp2, SWT.NULL);
		polylabel5.setText("Extra : ");
		polylabel5.setFont(fontui);
		// poly vertex extra text box
		polyetextbox = new Text(polycomp2, SWT.BORDER);
		polyetextbox.setLayoutData(new RowData(40, 22));
		polyetextbox.setText("0");
		polyetextbox.setFont(fontui);
		
		// poly vertex type label
		Label polylabel3 = new Label(polycomp2, SWT.NULL);
		polylabel3.setText("Type : ");
		polylabel3.setFont(fontui); 
		// poly vertex type drop down
		polytypedropdown = new Combo(polycomp2, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		polytypedropdown.setFont(fontui);
		polytypedropdown.add("0: Straight");
		polytypedropdown.add("1: Curve Up");
		polytypedropdown.add("2: Curve Down");
		polytypedropdown.select(0);
		
		polytypedropdown.addSelectionListener(new SelectionListener(){
			public void widgetSelected(SelectionEvent e){

			}

			public void widgetDefaultSelected(SelectionEvent e){
				// required but doesn't do anything in this case
			}
		});
		
		// poly vertex add vertex button
		Button addvex = new Button(polyvextotalcomp, SWT.PUSH);
		addvex.setText("Add Vertex");
		addvex.setFont(fontui);
		SimpleTooltip addvex_tooltip = new SimpleTooltip(shell,addvex,"Add a vertex to the currently selected polygon. Vertices are ordered in the order they are added.");
		addvex.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				
				addVertex(polyxtextbox.getText(), polyytextbox.getText(),
						polytypedropdown.getText().substring(0,1), polybtextbox.getText(),
						polyetextbox.getText(),true);
				// adds a vertex to the current header node based on the set values
			}
		});

		// composite for the set/remove specific vertex UI group
		Composite polycomp3 = new Composite(polytabcomp, SWT.NONE);
		GridLayout pc3glayout = new GridLayout();
		pc3glayout.numColumns = 4;
		// pc3glayout.makeColumnsEqualWidth=true;
		polycomp3.setLayout(pc3glayout);

		GridData pc3gbutton = new GridData(); // grid data used to get access to
												// the following two values
		pc3gbutton.horizontalAlignment = SWT.FILL;
		pc3gbutton.grabExcessHorizontalSpace = true;
		polycomp3.setLayoutData(pc3gbutton);

		// poly vertex vertex id label
		Label polylabel6 = new Label(polycomp3, SWT.NULL);
		polylabel6.setText("Vertex : ");
		polylabel6.setFont(fontui);
		// poly vertex vertex id text box
		polyvextextbox = new Text(polycomp3, SWT.BORDER);
		polyvextextbox.setLayoutData(new GridData(40, 22));
		polyvextextbox.setText("1");
		polyvextextbox.setFont(fontui);

		// poly vertex set vertex button
		Button setvex = new Button(polycomp3, SWT.PUSH);
		setvex.setText("Set");
		setvex.setFont(fontui);
		setvex.setLayoutData(pc3gbutton);
		SimpleTooltip setvex_tooltip = new SimpleTooltip(shell,setvex,"Change the numbered vertex.");
		setvex.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				int vexi = 0;
				try {
					vexi = Integer.parseInt(polyvextextbox.getText());
				}catch (Exception ex){
				}
				setVertex(vexi, polyxtextbox.getText(), polyytextbox.getText(),
						polytypedropdown.getText().substring(0,1), polybtextbox.getText(),
						polyetextbox.getText(),true);
				// sets the values of a specific vertex in the current header
			}
		});

		// poly vertex remove vertex button
		Button remvex = new Button(polycomp3, SWT.PUSH);
		remvex.setText("Remove");
		remvex.setFont(fontui);
		remvex.setLayoutData(pc3gbutton);
		SimpleTooltip remvex_tooltip = new SimpleTooltip(shell,remvex,"Remove the numbered vertex.");
		remvex.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				int vexi = 0;
				try {
					vexi = Integer.parseInt(polyvextextbox.getText());
				}catch (Exception ex){
				}
				removeVertex(vexi,true);
				// eliminates a specific vertex from the current header
			}
		});

		// END POLY 1

		// poly tab 2
		TabItem f1tab4item = new TabItem(tabfolder, SWT.NULL);
		f1tab4item.setText("II");

		// Poly 2 Properties
		Composite polytab2comp = new Composite(tabfolder, SWT.BORDER);
		polytab2comp.setLayout(new GridLayout());
		f1tab4item.setControl(polytab2comp);
		
		Composite polycomp4 = new Composite(polytab2comp, SWT.NONE);
		polycomp4.setLayout(rlayout);

		// poly set outline button
		Button setoutline = new Button(polycomp4, SWT.PUSH);
		setoutline.setText("Outline");
		setoutline.setFont(fontui);
		SimpleTooltip setoutline_tooltip = new SimpleTooltip(shell,setoutline,"Make the current polygon an outline only (not filled in).");
		setoutline.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setOutline();
				// flags a polygon as not being filled in
			}
		});
		// poly outline hint label
		//Label outlinelabel = new Label(polycomp4, SWT.NULL);
		//outlinelabel.setText(" (Polygons with Outline \n are not filled in)");
		// outlinelabel.setFont(fontui);

		Composite poly2comp1 = new Composite(polytab2comp, SWT.NONE);
		poly2comp1.setLayout(rlayout);

		// Poly center of rotation X label
		Label crotlabel1 = new Label(poly2comp1, SWT.NULL);
		crotlabel1.setText("X : ");
		crotlabel1.setFont(fontui);
		// Poly center of rotation X text box
		crotxtextbox = new Text(poly2comp1, SWT.BORDER);
		crotxtextbox.setLayoutData(new RowData(40, 22));
		crotxtextbox.setText("0");
		crotxtextbox.setFont(fontui);

		// Poly center of rotation Y label
		Label crotlabel2 = new Label(poly2comp1, SWT.NULL);
		crotlabel2.setText("Y : ");
		crotlabel2.setFont(fontui);
		// Poly center of rotation Y text box
		crotytextbox = new Text(poly2comp1, SWT.BORDER);
		crotytextbox.setLayoutData(new RowData(40, 22));
		crotytextbox.setText("0");
		crotytextbox.setFont(fontui);

		// Poly set center of rotation button
		Button setcrot = new Button(poly2comp1, SWT.PUSH);
		setcrot.setText("Set Rotation Center");
		setcrot.setFont(fontui);
		SimpleTooltip setcrot_tooltip = new SimpleTooltip(shell,setcrot,"Change the rotation center of this polygon, its pivot point, from the default of 0,0.");
		setcrot.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setCrot(crotxtextbox.getText(), crotytextbox.getText());
				// adds a node for center of rotation adjustment (normally CROT
				// is at 0,0)
			}
		});
		
		Composite poly2comp2 = new Composite(polytab2comp, SWT.NONE);
		poly2comp2.setLayout(rlayout);
		
		// Curve Weight of polygon connection
		Label crvwlabel = new Label(poly2comp2, SWT.NULL);
		crvwlabel.setText("Weight : ");
		crvwlabel.setFont(fontui);
		// Poly center of rotation Y text box
		crvwtextbox = new Text(poly2comp2, SWT.BORDER);
		crvwtextbox.setLayoutData(new RowData(40, 22));
		crvwtextbox.setText("1");
		crvwtextbox.setFont(fontui);

		// Poly set center of rotation button
		Button setcrvw = new Button(poly2comp2, SWT.PUSH);
		setcrvw.setText("Set Curve Weight");
		setcrvw.setFont(fontui);
		SimpleTooltip setcrvw_tooltip = new SimpleTooltip(shell,setcrvw,"Change the curve weight for the previous vertex (added after currently selected vertex).");
		setcrvw.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setCrvw(crvwtextbox.getText());
			}
		});
		
		/*Composite poly2comp3 = new Composite(polytab2comp, SWT.NONE);
		poly2comp3.setLayout(rlayout);
		
		// Curve Weight of polygon connection
		Label pendxlabel = new Label(poly2comp3, SWT.NULL);
		pendxlabel.setText("Endpoint Length : ");
		pendxlabel.setFont(fontui);
		// Poly center of rotation Y text box
		pendxtextbox = new Text(poly2comp3, SWT.BORDER);
		pendxtextbox.setLayoutData(new RowData(40, 22));
		pendxtextbox.setText("0");
		pendxtextbox.setFont(fontui);

		// Poly set center of rotation button
		Button setpendx = new Button(poly2comp3, SWT.PUSH);
		setpendx.setText("Set");
		setpendx.setFont(fontui);
		setpendx.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setPolyendx(pendxtextbox.getText());
				// adds a node for center of rotation adjustment (normally CROT
				// is at 0,0)
			}
		});*/

		// END Poly (finally!)

		

		// END Reference
		tabfolder2 = new TabFolder(editcomp, SWT.BORDER);
		tabfolder2.setLayoutData(new GridData(330, 270));
		TabItem f2tab1item = new TabItem(tabfolder2, SWT.NULL);
		f2tab1item.setText("Props");
		tabfolder2.setFont(fontui);
		// color should go in properties tab
		// as well as cut, precise, etc

		Composite proptabcomp = new Composite(tabfolder2, SWT.BORDER);
		proptabcomp.setLayout(new GridLayout());
		f2tab1item.setControl(proptabcomp);

		// Space Proprerties
		Label spacelabel0 = new Label(proptabcomp, SWT.NULL);
		spacelabel0.setText("Space");
		spacelabel0.setFont(fontui);

		Composite spacecomp = new Composite(proptabcomp, SWT.NONE);
		spacecomp.setLayout(rlayout);
		// prop space label
		Label spacelabel1 = new Label(spacecomp, SWT.NULL);
		spacelabel1.setText("Spacing : ");
		spacelabel1.setFont(fontui);
		// prop space text box
		spacetextbox = new Text(spacecomp, SWT.BORDER);
		spacetextbox.setLayoutData(new RowData(40, 22));
		spacetextbox.setText("0");
		spacetextbox.setFont(fontui);
		// prop set space button
		Button setspace = new Button(spacecomp, SWT.PUSH);
		setspace.setText("Set Space");
		setspace.setFont(fontui);
		SimpleTooltip setspace_tooltip = new SimpleTooltip(shell,setspace,"Change the distance of the current node from its source node.");
		setspace.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setSpace(spacetextbox.getText());
				// adds/updates a space node on current header node
			}
		});
		//Label spacelabel2 = new Label(spacecomp, SWT.NULL);
		// spacelabel2.setFont(fontui);
		// prop space hint label
		//spacelabel2.setText(" (How far this node \n is from the last)");
		
		Composite perpspacecomp = new Composite(proptabcomp, SWT.NONE);
		perpspacecomp.setLayout(rlayout);
		// prop space label
		Label perpspacelabel1 = new Label(perpspacecomp, SWT.NULL);
		perpspacelabel1.setText("Perpendicular : ");
		perpspacelabel1.setFont(fontui);
		// prop space text box
		perpspacetextbox = new Text(perpspacecomp, SWT.BORDER);
		perpspacetextbox.setLayoutData(new RowData(40, 22));
		perpspacetextbox.setText("0");
		perpspacetextbox.setFont(fontui);
		// prop set space button
		Button setperpspace = new Button(perpspacecomp, SWT.PUSH);
		setperpspace.setText("Set PerpSpace");
		setperpspace.setFont(fontui);
		SimpleTooltip setperpspace_tooltip = new SimpleTooltip(shell,setperpspace,"Change the distance of the current node from its source node in the direction perpendicular to its angle.");
		setperpspace.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setPerpSpace(perpspacetextbox.getText());
				// adds/updates a space node on current header node
			}
		});
		//Label spacelabel2 = new Label(spacecomp, SWT.NULL);
		// spacelabel2.setFont(fontui);
		// prop space hint label
		//spacelabel2.setText(" (How far this node \n is from the last)");
		// END Space

		// Color Properties
		Label colorlabel = new Label(proptabcomp, SWT.NULL);
		colorlabel.setText("Color");
		colorlabel.setFont(fontui);

		Composite colorcomp = new Composite(proptabcomp, SWT.NONE);
		colorcomp.setLayout(rlayout);

		// prop color red label
		Label colorrlabel = new Label(colorcomp, SWT.NULL);
		colorrlabel.setText("R : ");
		colorrlabel.setFont(fontui);
		// prop color red text box
		redcolortextbox = new Text(colorcomp, SWT.BORDER);
		redcolortextbox.setLayoutData(new RowData(35, 22));
		redcolortextbox.setText("1.0");
		redcolortextbox.setFont(fontui);
		// prop color green label
		Label colorglabel = new Label(colorcomp, SWT.NULL);
		colorglabel.setText("G : ");
		colorglabel.setFont(fontui);
		// prop color green text box
		greencolortextbox = new Text(colorcomp, SWT.BORDER);
		greencolortextbox.setLayoutData(new RowData(35, 22));
		greencolortextbox.setText("1.0");
		greencolortextbox.setFont(fontui);
		// prop color blue label
		Label colorblabel = new Label(colorcomp, SWT.NULL);
		colorblabel.setText("B : ");
		colorblabel.setFont(fontui);
		// prop color blue text box
		bluecolortextbox = new Text(colorcomp, SWT.BORDER);
		bluecolortextbox.setLayoutData(new RowData(35, 22));
		bluecolortextbox.setText("1.0");
		bluecolortextbox.setFont(fontui);
		// prop set color button
		Button setcolor = new Button(colorcomp, SWT.PUSH);
		setcolor.setText(" Set ");
		setcolor.setFont(fontui);
		SimpleTooltip setcolor_tooltip = new SimpleTooltip(shell,setcolor,"Change the color of this node.");
		setcolor.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setColor(redcolortextbox.getText(),
						greencolortextbox.getText(), bluecolortextbox.getText());
				// adds/updates a color node on current header node
			}
		});
		// prop pick color button
		Button pickcolor = new Button(colorcomp, SWT.PUSH);
		pickcolor.setText("Pick");
		pickcolor.setFont(fontui);
		SimpleTooltip pickcolor_tooltip = new SimpleTooltip(shell,pickcolor,"Open a color selection window.");
		pickcolor.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				ColorDialog cpicker = new ColorDialog(shell); 
				// ColorDialog is a default color picker dialog
				RGB col = cpicker.open();
				if(col != null){ // if the user picked a color, set the
									// textboxes to match.
					redcolortextbox.setText("" + (double)(col.red)/255.0);
					greencolortextbox.setText("" + (double)(col.green)/255.0);
					bluecolortextbox.setText("" + (double)(col.blue)/255.0);
				}
			}
		});
		// END Color Properties
		// Cut
		Label cutlabel0 = new Label(proptabcomp, SWT.NULL);
		cutlabel0.setText("Cut Layers");
		cutlabel0.setFont(fontui);

		Composite cutcomp = new Composite(proptabcomp, SWT.NONE);
		cutcomp.setLayout(rlayout);
		// prop set cut button
		Button setcut = new Button(cutcomp, SWT.PUSH);
		setcut.setText("Cut");
		setcut.setFont(fontui);
		SimpleTooltip setcut_tooltip = new SimpleTooltip(shell,setcut,"Add this node to the cut layer.");
		setcut.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setCut();
				// adds/removes a cut node to the current header node
			}
		});
		
		cutscaletextbox = new Text(cutcomp, SWT.BORDER);
		cutscaletextbox.setLayoutData(new RowData(35, 22));
		cutscaletextbox.setText("1");
		cutscaletextbox.setFont(fontui);

		Button setcutscale = new Button(cutcomp, SWT.PUSH);
		setcutscale.setText("Set Cut Scale");
		setcutscale.setFont(fontui);
		SimpleTooltip setcutscale_tooltip = new SimpleTooltip(shell,setcutscale,"How many pixels wide are cut outlines.");
		setcutscale.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setCutScale(cutscaletextbox.getText());
				// adds/removes a cut node to the current header node
			}
		});
		// prop cut hint label
		//Label cutlabel = new Label(cutcomp, SWT.NULL);
		//cutlabel.setText(" (Cut nodes will have their outline \n included on the cut layers)");
		// cutlabel.setFont(fontui);

		//Composite cutovercomp = new Composite(proptabcomp, SWT.NONE);
		//cutovercomp.setLayout(rlayout);
		// prop set cut overlap button
		/*Button setcutover = new Button(cutcomp, SWT.PUSH);
		setcutover.setText("Cut Overlap");
		setcutover.setFont(fontui);
		SimpleTooltip setcutover_tooltip = new SimpleTooltip(shell,setcutover,"If cut overlap is enabled, overlapping edges won't be removed.");
		setcutover.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setCutOverlap();
				// adds/removes a cut overlap node to the current header node
			}
		});*/
		// prop cut overlap hint label
		//Label cutoverlabel = new Label(cutovercomp, SWT.NULL);
		// cutoverlabel.setFont(fontui);
		//cutoverlabel
		//		.setText(" (If Cut Overlap is enabled, over-\n-lapping edges won't be removed)");

		// END Cut

		TabItem f2tab2item = new TabItem(tabfolder2, SWT.NULL);
		f2tab2item.setText("II");
		tabfolder2.setFont(fontui);
		// extra properties tab

		Composite proptab2comp = new Composite(tabfolder2, SWT.BORDER);
		proptab2comp.setLayout(new GridLayout());
		f2tab2item.setControl(proptab2comp);

		// Precise
		Label preciselabel0 = new Label(proptab2comp, SWT.NULL);
		preciselabel0.setText("Precision");
		preciselabel0.setFont(fontui);

		Composite precisecomp = new Composite(proptab2comp, SWT.NONE);
		precisecomp.setLayout(rlayout);
		// prop II precise button
		Button setprecise = new Button(precisecomp, SWT.PUSH);
		setprecise.setText("Precise");
		setprecise.setFont(fontui);
		SimpleTooltip setprecise_tooltip = new SimpleTooltip(shell,setprecise,"Make this node's connection to its source precise-- it will be placed on the edge of its source rather than overlapping.");
		setprecise.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setPrecise();
				// adds/removes a precise node on current header node
			}
		});
		// prop II precise hint label
		//Label preciselabel = new Label(precisecomp, SWT.NULL);
		//preciselabel
		//		.setText(" (Precise nodes will be placed on the edges of objects \n rather than overlapping)");
		// preciselabel.setFont(fontui);

		// END Precise
		// Extra Proprerty
		Label extralabel0 = new Label(proptab2comp, SWT.NULL);
		extralabel0.setText("Extra");
		extralabel0.setFont(fontui);

		Composite extracomp = new Composite(proptab2comp, SWT.NONE);
		extracomp.setLayout(rlayout);

		Label extralabel1 = new Label(extracomp, SWT.NULL);
		extralabel1.setText("Length : ");
		extralabel1.setFont(fontui);

		extralentextbox = new Text(extracomp, SWT.BORDER);
		extralentextbox.setLayoutData(new RowData(40, 22));
		extralentextbox.setText("0");
		extralentextbox.setFont(fontui);

		Label extralabel2 = new Label(extracomp, SWT.NULL);
		extralabel2.setText("Height : ");
		extralabel2.setFont(fontui);

		extrawidtextbox = new Text(extracomp, SWT.BORDER);
		extrawidtextbox.setLayoutData(new RowData(40, 22));
		extrawidtextbox.setText("0");
		extrawidtextbox.setFont(fontui);

		Button setextra = new Button(extracomp, SWT.PUSH);
		setextra.setText("Set Extra");
		setextra.setFont(fontui);
		SimpleTooltip setextra_tooltip = new SimpleTooltip(shell,setextra,"Change this node's extra space, additional whitespace on its edges.");
		setextra.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setExtra(extralentextbox.getText(), extrawidtextbox.getText());
			}
		});
		Label extralabel3 = new Label(proptab2comp, SWT.NULL);
		//extralabel3
		//		.setText(" (How much extra whitespace is on the edges of this node)");
		// END Extra
		// Var Proprerty
		Label varlabel0 = new Label(proptab2comp, SWT.NULL);
		varlabel0.setText("Variables");
		varlabel0.setFont(fontui);

		Composite varcomp = new Composite(proptab2comp, SWT.NONE);
		varcomp.setLayout(rlayout);

		Label varlabel1 = new Label(varcomp, SWT.NULL);
		varlabel1.setText("ID : ");
		varlabel1.setFont(fontui);

		varidtextbox = new Text(varcomp, SWT.BORDER);
		varidtextbox.setLayoutData(new RowData(40, 22));
		varidtextbox.setText("0");
		varidtextbox.setFont(fontui);

		Label varlabel2 = new Label(varcomp, SWT.NULL);
		varlabel2.setText("Value : ");
		varlabel2.setFont(fontui);

		varvaltextbox = new Text(varcomp, SWT.BORDER);
		varvaltextbox.setLayoutData(new RowData(40, 22));
		varvaltextbox.setText("0");
		varvaltextbox.setFont(fontui);

		Button setvar = new Button(varcomp, SWT.PUSH);
		setvar.setText("Set");
		setvar.setFont(fontui);
		setvar.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addVar(varidtextbox.getText(), varvaltextbox.getText());
			}
		});

		Button remvar = new Button(varcomp, SWT.PUSH);
		remvar.setText("Remove");
		remvar.setFont(fontui);
		remvar.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				removeVar(varidtextbox.getText(), varvaltextbox.getText());
			}
		});

		// END Var
		
		
		// ref tab
		TabItem f1tab5item = new TabItem(tabfolder2, SWT.NULL);
		f1tab5item.setText("Ref");

		// Reference

		Composite refshapetabcomp = new Composite(tabfolder2, SWT.BORDER);
		refshapetabcomp.setLayout(new GridLayout());
		f1tab5item.setControl(refshapetabcomp);

		Label refshapetablabel = new Label(refshapetabcomp, SWT.NULL);
		refshapetablabel.setText("References");
		refshapetablabel.setFont(fontui);

		Composite refsetcomp = new Composite(refshapetabcomp, SWT.NULL);
		refsetcomp.setLayout(new RowLayout());
		Label refshapetitlelabel = new Label(refsetcomp, SWT.NULL);
		refshapetitlelabel.setText("Title : ");
		refshapetitlelabel.setFont(fontui);

		refsettextbox = new Text(refsetcomp, SWT.BORDER);
		refsettextbox.setLayoutData(new RowData(70, 22));
		refsettextbox.setText("0");
		refsettextbox.setFont(fontui);

		Label refshapeanglelabel = new Label(refsetcomp, SWT.NULL);
		refshapeanglelabel.setText("Angle : ");
		refshapeanglelabel.setFont(fontui);

		refangtextbox = new Text(refsetcomp, SWT.BORDER);
		refangtextbox.setLayoutData(new RowData(40, 22));
		refangtextbox.setText("0");
		refangtextbox.setFont(fontui);

		Composite refset2comp = new Composite(refshapetabcomp, SWT.NULL);
		refset2comp.setLayout(new RowLayout());

		Button addref = new Button(refset2comp, SWT.PUSH);
		addref.setText(" Add ");
		addref.setFont(fontui);
		SimpleTooltip addref_tooltip = new SimpleTooltip(shell,addref,"Add a reference attached to this node. References are segments of duplicated code.");
		addref.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addRef(refsettextbox.getText(), refangtextbox.getText());
				// adds a node for center of rotation adjustment (normally CROT
				// is at 0,0)
			}
		});
		Button remref = new Button(refset2comp, SWT.PUSH);
		remref.setText(" Remove ");
		remref.setFont(fontui);
		SimpleTooltip remref_tooltip = new SimpleTooltip(shell,remref,"Remove the specified reference from this node.");
		remref.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				removeRef(refsettextbox.getText(), refangtextbox.getText());
				// adds a node for center of rotation adjustment (normally CROT
				// is at 0,0)
			}
		});
		// END REF

		TabItem f2tab3item = new TabItem(tabfolder2, SWT.NULL);
		f2tab3item.setText("Global");
		// global tab for properties that should be put on >0

		// Global Properties

		Composite globaltabcomp = new Composite(tabfolder2, SWT.BORDER);
		globaltabcomp.setLayout(new GridLayout());
		f2tab3item.setControl(globaltabcomp);

		// Fill

		Composite fillcomp = new Composite(globaltabcomp, SWT.NONE);
		fillcomp.setLayout(rlayout);
		// global fill label
		Label filllabel1 = new Label(fillcomp, SWT.NULL);
		filllabel1.setText("Fill : ");
		filllabel1.setFont(fontui);
		// global fill text box
		filltextbox = new Text(fillcomp, SWT.BORDER);
		filltextbox.setLayoutData(new RowData(40, 22));
		filltextbox.setText("11.81");
		filltextbox.setFont(fontui);
		// global set fill button
		Button setfill = new Button(fillcomp, SWT.PUSH);
		setfill.setText("Set Fill");
		setfill.setFont(fontui);
		SimpleTooltip setfill_tooltip = new SimpleTooltip(shell,setfill,"Change the fillsize, conversion to pixels, of this device. This changes the image size.");
		setfill.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setFill(filltextbox.getText());
				// updates the fill node on >0
			}
		});
		
		Composite fillcomp2 = new Composite(globaltabcomp, SWT.NONE);
		fillcomp2.setLayout(rlayout);
		
		Label filllabel2 = new Label(fillcomp2, SWT.NULL);
		filllabel2.setText("Rescale : ");
		filllabel2.setFont(fontui);
		
		rescaletextbox = new Text(fillcomp2, SWT.BORDER);
		rescaletextbox.setLayoutData(new RowData(35, 22));
		rescaletextbox.setText("1.0");
		rescaletextbox.setFont(fontui);

		Button setrescale = new Button(fillcomp2, SWT.PUSH);
		setrescale.setText("Set Rescale");
		setrescale.setFont(fontui);
		SimpleTooltip setrescale_tooltip = new SimpleTooltip(shell,setrescale,"Multiplies all dimensions by the given factor.");
		setrescale.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setReScale(rescaletextbox.getText());
				// adds/removes a cut node to the current header node
			}
		});
		// global fill hint label
		//Label filllabel2 = new Label(fillcomp, SWT.NULL);
		//filllabel2.setText(" (conversion to pixels)");
		// filllabel2.setFont(fontui);

		// END Fill
		// BUFFER_BOX

		Label bufboxlabel0 = new Label(globaltabcomp, SWT.NULL);
		bufboxlabel0.setText("Buffer Box");
		bufboxlabel0.setFont(fontui);

		Composite bufboxcomp = new Composite(globaltabcomp, SWT.NONE);
		bufboxcomp.setLayout(rlayout);
		// global buffer box length label
		Label bufboxlabel1 = new Label(bufboxcomp, SWT.NULL);
		bufboxlabel1.setText("Length : ");
		bufboxlabel1.setFont(fontui);
		// global buffer box length text box
		bufboxtextbox = new Text(bufboxcomp, SWT.BORDER);
		bufboxtextbox.setLayoutData(new RowData(40, 22));
		bufboxtextbox.setText("0");
		bufboxtextbox.setFont(fontui);
		// global buffer box height label
		Label bufboxlabel2 = new Label(bufboxcomp, SWT.NULL);
		bufboxlabel2.setText("Width : ");
		bufboxlabel2.setFont(fontui);
		// global buffer box height text box
		bufboxtextbox2 = new Text(bufboxcomp, SWT.BORDER);
		bufboxtextbox2.setLayoutData(new RowData(40, 22));
		bufboxtextbox2.setText("0");
		bufboxtextbox2.setFont(fontui);
		Composite bufboxcomp2 = new Composite(globaltabcomp, SWT.NONE);
		bufboxcomp2.setLayout(new RowLayout());
		// global set buffer box button
		Button setbufbox = new Button(bufboxcomp2, SWT.PUSH);
		setbufbox.setText("Set Buffer Box");
		setbufbox.setFont(fontui);
		SimpleTooltip setbufbox_tooltip = new SimpleTooltip(shell,setbufbox,"Set the bufferbox for this device: fill the entire device with black. The length and height are extra whitespace on the edges.");
		setbufbox.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setBufBox(bufboxtextbox.getText(), bufboxtextbox2.getText());
				// adds/removes buffer box node on header node
			}
		});
		// global buffer box hint label
		//Label bufboxlabel3 = new Label(bufboxcomp2, SWT.NULL);
		//bufboxlabel3
		//		.setText(" (Replace buffer with a single box. Length and \n Height are extra white spacing on each side)");
		// bufboxlabel3.setFont(fontui);
		// END BUFFER_BOX
		
		
		TabItem f2tab4item = new TabItem(tabfolder2, SWT.NULL);
		f2tab4item.setText("PDF");
		// PDF Tab

		// PDF Properties

		Composite pdftabcomp = new Composite(tabfolder2, SWT.BORDER);
		pdftabcomp.setLayout(new GridLayout());
		f2tab4item.setControl(pdftabcomp);
		// PDF Settings
		Composite pdfsizecomp = new Composite(pdftabcomp, SWT.NONE);
		pdfsizecomp.setLayout(rlayout);
		// global buffer box length label
		Label pdfsizelabel1 = new Label(pdfsizecomp, SWT.NULL);
		pdfsizelabel1.setText("Length (X) : ");
		pdfsizelabel1.setFont(fontui);
		// global buffer box length text box
		pdfsizextextbox = new Text(pdfsizecomp, SWT.BORDER);
		pdfsizextextbox.setLayoutData(new RowData(40, 22));
		pdfsizextextbox.setText("0");
		pdfsizextextbox.setFont(fontui);
		// global buffer box height label
		Label pdfsizelabel2 = new Label(pdfsizecomp, SWT.NULL);
		pdfsizelabel2.setText("Width (Y) : ");
		pdfsizelabel2.setFont(fontui);
		// global buffer box height text box
		pdfsizeytextbox = new Text(pdfsizecomp, SWT.BORDER);
		pdfsizeytextbox.setLayoutData(new RowData(40, 22));
		pdfsizeytextbox.setText("0");
		pdfsizeytextbox.setFont(fontui);
		Composite pdfsizecomp2 = new Composite(pdftabcomp, SWT.NONE);
		pdfsizecomp2.setLayout(new RowLayout());
		// global set buffer box button
		Button setpdfsizein = new Button(pdfsizecomp2, SWT.PUSH);
		setpdfsizein.setText("Set PDF Size (in)");
		setpdfsizein.setFont(fontui);
		SimpleTooltip setpdfsizein_tooltip = new SimpleTooltip(shell,setpdfsizein,"Set the size, in inches, of the output PDF document.");
		setpdfsizein.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setPDFSizein(pdfsizextextbox.getText(), pdfsizeytextbox.getText());
				// adds/removes buffer box node on header node
			}
		});
		Button setpdfsizecm = new Button(pdfsizecomp2, SWT.PUSH);
		setpdfsizecm.setText("Set PDF Size (cm)");
		setpdfsizecm.setFont(fontui);
		SimpleTooltip setpdfsizecm_tooltip = new SimpleTooltip(shell,setpdfsizecm,"Set the size, in centimeters, of the output PDF document.");
		setpdfsizecm.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setPDFSizecm(pdfsizextextbox.getText(), pdfsizeytextbox.getText());
				// adds/removes buffer box node on header node
			}
		});
		// PDF margins
		Composite pdfmargincomp = new Composite(pdftabcomp, SWT.NONE);
		pdfmargincomp.setLayout(rlayout);
		// global buffer box length label
		Label pdfmarginlabel1 = new Label(pdfmargincomp, SWT.NULL);
		pdfmarginlabel1.setText("Length : ");
		pdfmarginlabel1.setFont(fontui);
		// global buffer box length text box
		pdfmarginxtextbox = new Text(pdfmargincomp, SWT.BORDER);
		pdfmarginxtextbox.setLayoutData(new RowData(40, 22));
		pdfmarginxtextbox.setText("0");
		pdfmarginxtextbox.setFont(fontui);
		// global buffer box height label
		Label pdfmarginlabel2 = new Label(pdfmargincomp, SWT.NULL);
		pdfmarginlabel2.setText("Width : ");
		pdfmarginlabel2.setFont(fontui);
		// global buffer box height text box
		pdfmarginytextbox = new Text(pdfmargincomp, SWT.BORDER);
		pdfmarginytextbox.setLayoutData(new RowData(40, 22));
		pdfmarginytextbox.setText("0");
		pdfmarginytextbox.setFont(fontui);
		Composite pdfmargincomp2 = new Composite(pdftabcomp, SWT.NONE);
		pdfmargincomp2.setLayout(new RowLayout());
		// global set buffer box button
		Button setpdfmarginin = new Button(pdfmargincomp2, SWT.PUSH);
		setpdfmarginin.setText("Set PDF Margin (in)");
		setpdfmarginin.setFont(fontui);
		SimpleTooltip setpdfmarginin_tooltip = new SimpleTooltip(shell,setpdfmarginin,"Set the size, in inches, of the PDF's margins.");
		setpdfmarginin.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setPDFMarginin(pdfmarginxtextbox.getText(), pdfmarginytextbox.getText());
				// adds/removes buffer box node on header node
			}
		});
		Button setpdfmargincm = new Button(pdfmargincomp2, SWT.PUSH);
		setpdfmargincm.setText("Set PDF Margin (cm)");
		setpdfmargincm.setFont(fontui);
		SimpleTooltip setpdfmargincm_tooltip = new SimpleTooltip(shell,setpdfmargincm,"Set the size, in centimeters, of the PDF's margins.");
		setpdfmargincm.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				setPDFMargincm(pdfmarginxtextbox.getText(), pdfmarginytextbox.getText());
				// adds/removes buffer box node on header node
			}
		});
		
		
		// now establish the menubar (this is the top bar that has drop down
		// menus such as File)
		Rectangle clientArea = shell.getClientArea();
		shell.setFont(fontui);

		Menu bar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(bar);

		// menu file tab
		MenuItem fileItem = new MenuItem(bar, SWT.CASCADE);
		fileItem.setText("&File");

		final Menu menu = new Menu(shell, SWT.DROP_DOWN);
		fileItem.setMenu(menu);

		// menu save button
		final MenuItem filesaveitem = new MenuItem(menu, SWT.PUSH);
		filesaveitem.setText("&Save\tCTRL+S"); // note the format here displays the keyboard shortcut to the side
		filesaveitem.setAccelerator(SWT.CTRL + 'S'); // establishes the keyboard shortcut

		filesaveitem.addSelectionListener(new SelectionAdapter(){ 
			// when the save button is pressed:
			@Override
			public void widgetSelected(SelectionEvent e){
				// open filedialog
				if(haslastsavedpath){
					saveFile(lastsavedpath);
					return;
				}
				FileDialog filedialog = new FileDialog(shell, SWT.SAVE);
				filedialog.setFilterPath("./");
				// where does the file searcher start? (./ is the folder the  program is in)
				filedialog.setText("Save Script"); // title of the file dialog
				String[] fe = new String[2]; // possible file types
				fe[0] = "*.txt";
				fe[1] = "*.*";
				filedialog.setFilterExtensions(fe); // set file types
				String path = filedialog.open(); // grab the path that is gotten from the filedialog
				if(path!=null){
					saveFile(path); // this function saves the current
									// script at that path
					lastsavedpath=path;
					haslastsavedpath=true;
				}
			}
		});
		
		final MenuItem filesaveasitem = new MenuItem(menu, SWT.PUSH);
		filesaveasitem.setText("&Save As"); // note the format here displays the keyboard shortcut to the side
		//filesaveasitem.setAccelerator(SWT.CTRL + 'S'); // establishes the keyboard shortcut

		filesaveasitem.addSelectionListener(new SelectionAdapter(){ 
			// when the save button is pressed:
			@Override
			public void widgetSelected(SelectionEvent e){
				// open filedialog
				FileDialog filedialog = new FileDialog(shell, SWT.SAVE);
				filedialog.setFilterPath("./");
				// where does the file searcher start? (./ is the folder the  program is in)
				filedialog.setText("Save Script"); // title of the file dialog
				String[] fe = new String[2]; // possible file types
				fe[0] = "*.txt";
				fe[1] = "*.*";
				filedialog.setFilterExtensions(fe); // set file types
				String path = filedialog.open(); // grab the path that is gotten from the filedialog
				if(path!=null){
					saveFile(path); // this function saves the current
									// script at that path
					lastsavedpath=path;
					haslastsavedpath=true;
				}
			}
		});

		// menu open button
		final MenuItem fileopenitem = new MenuItem(menu, SWT.PUSH);
		fileopenitem.setText("&Open\tCTRL+O");
		fileopenitem.setAccelerator(SWT.CTRL + 'O'); // again, sets up keyboard shortcut

		fileopenitem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				FileDialog filedialog = new FileDialog(shell, SWT.OPEN); // see above
				filedialog.setFilterPath("./");
				filedialog.setText("Open Script");
				String[] fe = new String[2];
				fe[0] = "*.txt";
				fe[1] = "*.*";
				filedialog.setFilterExtensions(fe);
				String path = filedialog.open();
				if(path!=null){
					try{
						openFile(path); // this function tries to open the selected script file
					}catch(Exception e2){
						// failed to open file
					}
				}
			}
		});
		
		MenuItem filesepitem0 = new MenuItem(menu, SWT.SEPARATOR); 
		
		

		//MenuItem filesepitem1 = new MenuItem(menu, SWT.SEPARATOR); 
		// a separator is a small bar in the menu

		//MenuItem filesepitem2 = new MenuItem(menu, SWT.SEPARATOR); 
		// a separator is a small bar in the menu
		// menu exit button
		final MenuItem fileexititem = new MenuItem(menu, SWT.PUSH);
		fileexititem.setText("E&xit");

		fileexititem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				saveFile("./Backup.txt"); // save a backup
				System.exit(0); // exit the program
			}
		});
		
		// edit menu
		
		MenuItem editItem = new MenuItem(bar, SWT.CASCADE);
		editItem.setText("&Edit");

		final Menu editmenu = new Menu(shell, SWT.DROP_DOWN);
		editItem.setMenu(editmenu);
		
		final MenuItem fileundoitem = new MenuItem(editmenu, SWT.PUSH);
		fileundoitem.setText("Undo\tCtrl+Z");
	    fileundoitem.setAccelerator(SWT.CTRL + 'Z');

		fileundoitem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				undo(); // clear all nodes when pressed
			}
		});
		
		final MenuItem fileredoitem = new MenuItem(editmenu, SWT.PUSH);
		fileredoitem.setText("Redo\tCtrl+R");
	    fileredoitem.setAccelerator(SWT.CTRL + 'R');

		fileredoitem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				redo(); // clear all nodes when pressed
			}
		});
		
		final MenuItem fileclearitem = new MenuItem(editmenu, SWT.PUSH);
		fileclearitem.setText("C&lear");

		fileclearitem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				clearAll(); // clear all nodes when pressed
			}
		});
		
		final MenuItem editsplititem = new MenuItem(editmenu, SWT.SEPARATOR);
		
		final MenuItem editprefitem = new MenuItem(editmenu, SWT.PUSH);
		editprefitem.setText("P&references");
		
		editprefitem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if(prefwindow!=null){
					if(prefwindow.isDisposed()){
						prefwindow = new PreferenceWindow(shell,fontui,fontuibold,
								prevselcol,colorbyz,colseed,previewauto,
								previewfillfactor,polydrawx,polydrawy);
					}
				}else{
					prefwindow = new PreferenceWindow(shell,fontui,fontuibold,
							prevselcol,colorbyz,colseed,previewauto,
							previewfillfactor,polydrawx,polydrawy);
				}
			}
		});
		
		// build menu
		
		MenuItem buildItem = new MenuItem(bar, SWT.CASCADE);
		buildItem.setText("&Build");

		final Menu buildmenu = new Menu(shell, SWT.DROP_DOWN);
		buildItem.setMenu(buildmenu);
		
		// menu run button
		final MenuItem fileprevitem = new MenuItem(buildmenu, SWT.PUSH);
		fileprevitem.setText("&Preview\tCTRL+P"); // note the format here displays the keyboard shortcut to the side
		fileprevitem.setAccelerator(SWT.CTRL + 'P'); // establishes the keyboard shortcut

		fileprevitem.addSelectionListener(new SelectionAdapter(){ 
			@Override
			public void widgetSelected(SelectionEvent e){
				// creates a preview of the current script
				preview();
			}
		});
		
		// menu run button
		final MenuItem filecompileitem = new MenuItem(buildmenu, SWT.PUSH);
		filecompileitem.setText("&Run\tCTRL+R"); // note the format here displays the keyboard shortcut to the side
		filecompileitem.setAccelerator(SWT.CTRL + 'R'); // establishes the keyboard shortcut

		filecompileitem.addSelectionListener(new SelectionAdapter(){ 
			@Override
			public void widgetSelected(SelectionEvent e){
				// saves pdfs/images from current script
				compile(false);
			}
		});
		
		final MenuItem filecompileasitem = new MenuItem(buildmenu, SWT.PUSH);
		filecompileasitem.setText("&Run As"); // note the format here displays the keyboard shortcut to the side
		//filecompileasitem.setAccelerator(SWT.CTRL + 'R'); // establishes the keyboard shortcut

		filecompileasitem.addSelectionListener(new SelectionAdapter(){ 
			@Override
			public void widgetSelected(SelectionEvent e){
				// saves pdfs/images from current script
				compile(true);
			}
		});
		
		/*// menu run button
		final MenuItem filerunitem = new MenuItem(buildmenu, SWT.PUSH);
		filerunitem.setText("&Open and Run\tCTRL+R"); // note the format here displays the keyboard shortcut to the side
		filerunitem.setAccelerator(SWT.CTRL + 'R'); // establishes the keyboard shortcut

		filerunitem.addSelectionListener(new SelectionAdapter(){ 
			@Override
			public void widgetSelected(SelectionEvent e){
				// open filedialog
				FileDialog filedialog = new FileDialog(shell, SWT.OPEN); 
				filedialog.setFilterPath("./");
				// where does the file searcher start? (./ is the folder the  program is in)
				filedialog.setText("Run Script"); // title of the file dialog
				String[] fe = new String[2]; // possible file types
				fe[0] = "*.txt";
				fe[1] = "*.*";
				filedialog.setFilterExtensions(fe); // set file types
				String path = filedialog.open(); // grab the path that is gotten from the filedialog
				if(path!=null){
					Interpreter.digest(path); 
					Interpreter.flush();
				}
			}
		});*/
		
		// menu target button
		final MenuItem filetargetitem = new MenuItem(buildmenu, SWT.PUSH);
		filetargetitem.setText("&Target Folder\tCTRL+T"); // note the format here displays the keyboard shortcut to the side
		filetargetitem.setAccelerator(SWT.CTRL + 'T'); // establishes the keyboard shortcut

		filetargetitem.addSelectionListener(new SelectionAdapter(){ 
			@Override
			public void widgetSelected(SelectionEvent e){
				// open filedialog
				DirectoryDialog dirdialog = new DirectoryDialog(shell);
				dirdialog.setFilterPath("./");
				
				dirdialog.setText("Target an Output Directory");
				String newdir = dirdialog.open();
				if(newdir!=null){
					if(!newdir.endsWith("/") && !newdir.endsWith("\\")){
						if(newdir.contains("\\")){ // if the last slash gets cut off, restore it
							newdir = newdir + "\\"; // or else files will be saved in the directory below.
						}else{
							newdir=newdir+"/";
						}
					}
					Interpreter.setTarget(newdir);
				}
			}
		});
		
		// view menu
		// menu file tab
		MenuItem viewItem = new MenuItem(bar, SWT.CASCADE);
		viewItem.setText("&View");

		final Menu viewmenu = new Menu(shell, SWT.DROP_DOWN);
		viewItem.setMenu(viewmenu);

		viewcolorbyzitem = new MenuItem(viewmenu, SWT.CHECK);
		viewcolorbyzitem.setText("&Color by Layer");
		viewcolorbyzitem.setSelection(colorbyz);

		viewcolorbyzitem.addSelectionListener(new SelectionAdapter(){ 
			@Override
			public void widgetSelected(SelectionEvent e){
				colorbyz=!colorbyz;
				setColorByZ(colorbyz);
			}
		});
		
		final MenuItem viewchangecolorsitem = new MenuItem(viewmenu, SWT.PUSH);
		viewchangecolorsitem.setText("&Change Colors");

		viewchangecolorsitem.addSelectionListener(new SelectionAdapter(){ 
			@Override
			public void widgetSelected(SelectionEvent e){
				colseed1 = (int) (Math.random() * 300) + 10;
				colseed2 = 2 * ((int) (Math.random() * 150) - colseed1) + 15;
				colseed3 = 3 * ((int) (Math.random() * 75) + colseed2) + 20;
				colseed4 = (int) (Math.random() * 300) - colseed3 + 35;
				refreshColors();
			}
		});

		// establish right-click menu
		final Menu nodetreemenu = new Menu(nodetree);
		nodetree.setMenu(nodetreemenu);
		nodetreemenu.addMenuListener(new MenuAdapter(){
			public void menuShown(MenuEvent e){
				MenuItem[] items = nodetreemenu.getItems();
				int i=0;
				while(i<items.length){
					items[i].dispose();
					i++;
				}
				MenuItem menudelete = new MenuItem(nodetreemenu,SWT.NONE);
				menudelete.setText("Delete");
				menudelete.addSelectionListener(new SelectionAdapter(){ 
					@Override
					public void widgetSelected(SelectionEvent e){
						if(lastrclicked!=null){
							int id = findNodeId(lastrclicked);
							if(id==-1){
								return;
							}
							if((nodetype[id]!=0 || nodesource[id]!=-1) && nodetype[id]!=1 && nodetype[id]!=9){
								lastrclicked.dispose();
								if(nodetype[id]==0){
									current=nodesource[id];
								}
								nodetype[id]=-1;
							}
							actionHook(true);
						}
					}
				});
			}
		});
		
		final Menu layertreemenu = new Menu(layertree);
		layertree.setMenu(layertreemenu);
		layertreemenu.addMenuListener(new MenuAdapter(){
			public void menuShown(MenuEvent e){
				MenuItem[] items = layertreemenu.getItems();
				int i=0;
				while(i<items.length){
					items[i].dispose();
					i++;
				}
				MenuItem menudelete = new MenuItem(layertreemenu,SWT.NONE);
				menudelete.setText("Delete");
				menudelete.addSelectionListener(new SelectionAdapter(){ 
					@Override
					public void widgetSelected(SelectionEvent e){
						if(lastrclicked!=null){
							int id = findNodeId(lastrclicked);
							if(id==-1){
								return;
							}
							if((nodetype[id]!=0 || nodesource[id]!=-1) && nodetype[id]!=1 && nodetype[id]!=9){
								lastrclicked.dispose();
								if(nodetype[id]==0){
									current=nodesource[id];
								}
								nodetype[id]=-1;
							}
							actionHook(true);
						}
					}
				});
			}
		});
		
		final Menu reftreemenu = new Menu(reftree);
		reftree.setMenu(reftreemenu);
		reftreemenu.addMenuListener(new MenuAdapter(){
			public void menuShown(MenuEvent e){
				MenuItem[] items = reftreemenu.getItems();
				int i=0;
				while(i<items.length){
					items[i].dispose();
					i++;
				}
				MenuItem menudelete = new MenuItem(reftreemenu,SWT.NONE);
				menudelete.setText("Delete");
				menudelete.addSelectionListener(new SelectionAdapter(){ 
					@Override
					public void widgetSelected(SelectionEvent e){
						if(lastrclicked!=null){
							int id = findRefNodeId(lastrclicked);
							if(id==-1){
								return;
							}
							if(refnodetype[id]!=1 && refnodetype[id]!=9){
								lastrclicked.dispose();
								//if(refnodetype[id]==0){
								//	currentref=0;
								//}
								refnodetype[id]=-1;
							}
							actionHook(true);
						}
					}
				});
			}
		});
		
		final Menu comtreemenu = new Menu(comtree);
		comtree.setMenu(comtreemenu);
		comtreemenu.addMenuListener(new MenuAdapter(){
			public void menuShown(MenuEvent e){
				MenuItem[] items = comtreemenu.getItems();
				int i=0;
				while(i<items.length){
					items[i].dispose();
					i++;
				}
				MenuItem menudelete = new MenuItem(comtreemenu,SWT.NONE);
				menudelete.setText("Delete");
				menudelete.addSelectionListener(new SelectionAdapter(){ 
					@Override
					public void widgetSelected(SelectionEvent e){
						if(lastrclicked!=null){
							int id=-1;
							int i = 0;
							while(i < comnodes){
								if(comnodeitems[i].equals(lastrclicked)){
									id = i;
									break;
								}
								i++;
							}
							if(id==-1){
								return;
							}
							if(comheadernodeitems[currentcom].equals(lastrclicked)){
								currentcom=0;
							}
							if(comnodetype[id]!=3){
								lastrclicked.dispose();
								//if(refnodetype[id]==0){
								//	currentref=0;
								//}
								comnodetype[id]=-1;
							}
							
						}
					}
				});
			}
		});
		
		// finally, establishing the size of the tree
		nodetree.setBounds(clientArea.x, clientArea.y, 670, 700);
		nodetree.setSize(670, 700);
		nodetree.setLayoutData(new RowData(670, 700)); 
		reftree.setBounds(clientArea.x, clientArea.y, 750, 620);
		reftree.setSize(750, 620);
		reftree.setLayoutData(new GridData(750, 620)); 
		layertree.setBounds(clientArea.x, clientArea.y, 750, 620);
		layertree.setSize(750, 620);
		layertree.setLayoutData(new GridData(750, 620)); 
		comtree.setBounds(clientArea.x, clientArea.y, 750, 470);
		comtree.setSize(750, 470);
		comtree.setLayoutData(new GridData(750, 470)); 
		// pack the shell, set the window size and open it up
		shell.pack();
		shell.setSize(1200, 800);
		shell.open();
		
		
		
		// now wait for the shell to be X'd out of and close the program when
		// that happens
		while(!shell.isDisposed()){
			if(!display.readAndDispatch()){
				display.sleep();
			}
		}
		display.dispose();

	}
	
	// createCom(
	//		String, this is the title of the combined layer
	//		)
	// - makes a new combined layer in the combined layer tab with the given title
	// - makes the top-level node for said layer
	static void createCom(String title){
		// first check if the ref already exists and destroy it if it does
		int i =0;
		while(i<comheadernodes){
			if(!comheadernodeitems[i].isDisposed()){
				if(comheadernodeitems[i].getText().equalsIgnoreCase("#COMBINE " + title)){
					comheadernodeitems[i].setText("NULL");
					comheadernodeitems[i].clearAll(true);
					comheadernodeitems[i].dispose();
					return;
				}
			}
			i++;
		}
		TreeItem ritem = new TreeItem(comtree, SWT.NULL);
		ritem.setFont(font10);
		ritem.setText("#COMBINE " + title);

		comnodeitems[comnodes] = ritem;
		comnodetype[comnodes] = -1;
		comnodes++;
		comheadernodeitems[comheadernodes] = ritem;
		comheadernodes++;
		
		TreeItem ritemend = new TreeItem(ritem, SWT.NULL);
		ritemend.setFont(font8);
		ritemend.setText("#FILL " + title);
		ritemend.setExpanded(true);
		comnodeitems[comnodes] = ritemend;
		comnodetype[comnodes] = -1;
		comnodes++;
		currentcom = comheadernodes - 1;
		ritem.setExpanded(true);
		
	}
	
	// addComNode(
	//		TreeItem, this is where to add the new item (which item is the parent?)
	//		String, this is the textual content of the new item
	//		int, this is the type id of the node to be added
	//		)
	// - adds a new node with the given text and type to the specified parent node
	// - if such a node already exists (same text) then it will dispose of that node instead (like a toggle)
	// - used for the combined layer tab
	static void addComNode(TreeItem insitem, String text, int type){
		if(comnodes<=0){
			return;
		}
		int i =0;
		while(i<comnodes){
			if(!comnodeitems[i].isDisposed()){
				if(comnodeitems[i].getText().equalsIgnoreCase(text) && comnodeitems[i].getParentItem().equals(insitem)){
					comnodeitems[i].setText("NULL");
					comnodeitems[i].dispose();
					return;
				}
			}
			i++;
		}
		// if we got through that, this node doesn't already exist
		TreeItem newcomitem =comnodeitems[0];
		if(insitem.isDisposed()){
			currentcom--;
			if(currentcom>=0){
				while(comheadernodeitems[currentcom].isDisposed() && currentcom>0){
					currentcom--;
				}
			}
			return;
		}
		if(insitem.getText().toUpperCase().startsWith("#COMBINE")){
			newcomitem=new TreeItem(insitem, SWT.NULL, insitem.getItems().length-1);
		}else{
			newcomitem=new TreeItem(insitem, SWT.NULL);
		}
		newcomitem.setText(text);
		newcomitem.setFont(font8);
		if(text.length()>=3){
			if(text.substring(0,3).equalsIgnoreCase("#ID")){
				newcomitem.setFont(font10);
				comheadernodeitems[comheadernodes]=newcomitem;
				comheadernodes++;
			}
		}
		newcomitem.setExpanded(true);
		comnodeitems[comnodes]=newcomitem;
		comnodes++;
	}
	
	

	// createRef(
	//		String, this is the title of the reference
	//		)
	// - creates a new reference in the references tab with the specified title
	// - creates the { and } nodes for that reference
	static void createRef(String title){
		// first check if the ref already exists and destroy it if it does
		int i =0;
		while(i<refheadernodes){
			if(!refheadernodeitems[i].isDisposed()){
				if(refheadernodeitems[i].getText().equalsIgnoreCase("{ " + title)){
					refheadernodeitems[i].setText("NULL");
					refheadernodeitems[i].clearAll(true);
					refheadernodeitems[i].dispose();
					return;
				}
			}
			i++;
		}
		TreeItem ritem = new TreeItem(reftree, SWT.NULL);
		ritem.setFont(font10);
		ritem.setText("{ " + title);

		refnodeitems[refnodes] = ritem;
		refnodetype[refnodes] = -1;
		refnodes++;

		refplace[refheadernodes]=firstFreeTemp();
		refnodeitems[refnodes] = new TreeItem(ritem, 0);
		refnodeitems[refnodes].setText("$TEMP " + refplace[refheadernodes]);
		refnodeitems[refnodes].setFont(font8);
		refnodetype[refnodes] = 1;
		refnodeitems[refnodes].setExpanded(true);
		refnodes++;

		refheadernodeitems[refheadernodes] = ritem;
		refheadernodes++;
		TreeItem ritemend = new TreeItem(ritem, SWT.NULL);
		ritemend.setFont(font10);
		ritemend.setText("}");
		ritemend.setExpanded(true);
		refnodeitems[refnodes] = ritemend;
		refnodetype[refnodes] = -1;
		refnodes++;
		currentref = refheadernodes - 1;
		ritem.setExpanded(true);
	}

	// addRef(
	//		String, this is the title of the reference
	//		String, this is the angle adjustment for this reference (all angles will have this added to them)
	// 		)
	// - adds a reference node (type 17) to the regular nodetree with specified title and angle adjustment
	static void addRef(String len, String ang){
		int[] over = { 17 };
		setPropertyNode("{ " + len + " } ANGLE " + ang, 17, false, over, false,
				true);
	}

	// removeRef(
	//		String, this is the title of the reference
	//		String, this is the angle adjustment of the refence
	//		)
	// - removes the reference node from the nodetree that has the specified title and angle
	static void removeRef(String len, String ang){
		// first check if current has an existing rect node
		if(current_tree_tab == 2){
			int i = 0;
			while(i < refnodes){
				if(!refnodeitems[i].isDisposed() && refnodeitems[i] != null){
					TreeItem refparent = refnodeitems[i].getParentItem();
					if(refparent != null){
						if(refparent.equals(refheadernodeitems[currentref])
								&& refnodetype[i] == 17
								&& refnodeitems[i].getText().equalsIgnoreCase(
										"{ " + len + " } ANGLE " + ang)){
							refnodeitems[i].dispose();
							refnodetype[i] = -1;
							if(refnodes - 1 == i){
								refnodes--;
							}
							actionHook(true);
							return;
						}
					}
				}
				i++;
			}
			return;
		}
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 17){
					if(nodeitems[i].getText().equalsIgnoreCase(
							"{ " + len + " } ANGLE " + ang)){
						nodeitems[i].dispose();
						nodetype[i] = -1;
						if(nodes - 1 == i){
							nodes--;
						}
						actionHook(true);
						return;
					}
					// return;
				}
			}
			i++;
		}
		viewLayerTab(currentlayertabz);
		actionHook(true);
	}
	
	// getRefTemp(
	//		TreeItem, this is the header item to get the temp id of
	//		)
	// - takes in an item in the reference tree and tells you what temp id it is
	// - must take in the header node (i.e. the TreeItem that the temp TreeItem is attached to)
	static int getRefTemp(TreeItem refitem){
		TreeItem[] citems = refitem.getItems();
		int i=0;
		while(i<citems.length){
			if(!citems[i].isDisposed()){
				if(citems[i].getText().toUpperCase().startsWith("$TEMP")){
					int out = 0;
					try{
						out = Integer.parseInt(citems[i].getText().substring(6));
					}catch(Exception e){
						
					}
					return out;
				}
			}
			i++;
		}
		return 0;
	}
	
	// firstFreePlace()
	// - finds the first unused nodeplace
	static int firstFreePlace(){
		boolean[] free = new boolean[maxnodes];
		int i=0;
		
		while(i<maxnodes){
			free[i]=true;
			i++;
		}
		i =0;
		while(i<headernodes){
			if(nodeplace[i]>=0){
				free[nodeplace[i]]=false;
			}
			i++;
		}
		i=0;
		while(i<maxnodes){
			if(free[i]){
				return i;
			}
			i++;
		}
		return maxnodes-1;
	}
	
	// getHeaderNodeIdFromPlace(
	//		int, this is the placeid to find a node from
	//		)
	// - given a place id, finds the corresponding header node
	static int getHeaderNodeIdFromPlace(int placeid){
		int i=0;
		while(i<headernodes){
			if(nodeplace[i]==placeid){
				return i;
			}
			i++;
		}
		return -1;
	}
	
	// 
	static MapNode getHeaderNode(int hid){
		return headernode[hid];
	}
	
	static TreeItem getHeaderNodeItem(int hid){
		return headernodeitems[hid];
	}
	
	static int getCurrentID(){
		return current;
	}
	
	static TreeItem getCurrent(){
		return getHeaderNodeItem(current);
	}

	
	// firstFreeTemp()
	// - finds the first unused temp id (for reference tab)
	static int firstFreeTemp(){
		boolean[] free = new boolean[maxnodes];
		int i=0;
		while(i<maxnodes){
			free[i]=true;
			i++;
		}
		i =0;
		while(i<refheadernodes){
			if(refplace[i]>=0){
				free[refplace[i]]=false;
			}
			i++;
		}
		i=0;
		while(i<maxnodes){
			if(free[i]){
				return i;
			}
			i++;
		}
		return maxnodes-1;
	}
	
	// refNodeNodeIdFromPlace(
	//		int, this is the temp id to search for
	//		)
	// - given the temp id of a reference header node, finds the corresponding reference header node.
	static int getRefHeaderNodeIdFromPlace(int placeid){
		int i=0;
		while(i<refheadernodes){
			if(refplace[i]==placeid){
				return i;
			}
			i++;
		}
		return -1;
	}

	// addVar(
	//		String, this is the ID of the variable to set
	//		String, this is the value to set that variable to
	//		)
	// - makes the variable of specified ID have the specified value
	// - (ex VAR[ID] = value)
	// - these nodes are type 18
	static void addVar(String len, String ang){
		int[] over = { 18 };
		setPropertyNode("$VAR " + len + " " + ang, 18, false, over, false, true);
	}

	// removeVar(
	//		String, this is the ID of the var node to get rid of
	//		String, this is the value of the var node to get rid of(depreciated)
	//		)
	// - removes the node with specified ID
	static void removeVar(String len, String ang){
		if(current_tree_tab == 2){
			int i = 0;
			while(i < refnodes){
				if(!refnodeitems[i].isDisposed() && refnodeitems[i] != null){
					TreeItem refparent = refnodeitems[i].getParentItem();
					if(refparent != null){
						if(refparent.equals(refheadernodeitems[currentref])
								&& refnodetype[i] == 18
								&& refnodeitems[i].getText()
										.substring(0, 5 + len.length())
										.equalsIgnoreCase("$VAR " + len)){
							refnodeitems[i].dispose();
							refnodetype[i] = -1;
							if(refnodes - 1 == i){
								refnodes--;
							}
							return;
						}
					}
				}
				i++;
			}
			return;
		}
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 18){
					if(nodeitems[i].getText().substring(0, 5 + len.length())
							.equalsIgnoreCase("$VAR " + len)){
						nodeitems[i].dispose();
						nodetype[i] = -1;
						if(nodes - 1 == i){
							nodes--;
						}
						return;
					}
					// return;
				}
			}
			i++;
		}
		viewLayerTab(currentlayertabz);
	}

	// viewLayerTab(
	//		Int, this is the current layer to view
	//		)
	// - updates the layer tab to show the nodes with the specified layer
	static void viewLayerTab(int z){
		// first, clear out the layer tab
		layernodes = 0;
		layertree.removeAll();

		boolean[] shown = new boolean[headernodes];
		TreeItem[] nnitems = new TreeItem[headernodes];
		int i = 0;
		while(i < headernodes){
			shown[i] = false;
			i++;
		}
		// now, fill it up
		i = 0;
		while(i < headernodes){
			if(headernode[i].getFillZ() == z && !headernodeitems[i].isDisposed()){
				TreeItem titem;
				if(shown[i]){
					titem = new TreeItem(nnitems[i], SWT.NULL);
				}else{
					titem = new TreeItem(layertree, SWT.NULL);
				}
				layernewnodeitems[layernodes] = titem;
				layerorignodeitems[layernodes] = headernodeitems[i];
				layernodes++;
				titem.setText(headernodeitems[i].getText());
				titem.setFont(headernodeitems[i].getFont());
				shown[i] = true;
				TreeItem[] child = headernodeitems[i].getItems();
				int h = 0;
				while(h < child.length){
					if(child[h].getText().startsWith(">")){
						int nnid = findHeaderNodeId(child[h]);
						if(nnid != -1){
							nnitems[findHeaderNodeId(child[h])] = titem;
							shown[findHeaderNodeId(child[h])] = true;
						}
					}else{
						TreeItem citem = new TreeItem(titem, SWT.NULL);
						citem.setText(child[h].getText());
						citem.setFont(child[h].getFont());
						citem.setExpanded(true);
						layernewnodeitems[layernodes] = citem;
						layerorignodeitems[layernodes] = child[h];
						layernodes++;
					}
					h++;
				}
				titem.setExpanded(true);
			}
			i++;
		}
	}

	// setupNode(
	// TreeItem, this is the node to setup
	// int, this is the headernode that this node is attached to
	// )
	// - Handles setup of a new node in the tree, makes sure it is open,
	// checked, and colored.
	// - Each node is colored like its parent, unless it is a new headernode, so
	// we need to know the source.
	// - This should be called on every node you add.
	static void setupNode(TreeItem n, int sourceid){
		n.setExpanded(true);
		n.setChecked(true);
		if(current_tree_tab == 2){
			return;
		}
		if(!nodecolorset[sourceid]){
			nodecolors[sourceid] = getNodeColor(sourceid);

			if(n.equals(headernodeitems[sourceid])){
				final int sid = sourceid;
				n.addDisposeListener(new DisposeListener(){
					public void widgetDisposed(DisposeEvent e){
						nodecolors[sid].dispose();
						nodecolorset[sid] = false;
					}
				});
			}
		}
		n.setBackground(nodecolors[sourceid]);
	}

	// findNodeNodeId(
	// TreeItem, the item that you're trying to find the headernode id of
	// )
	// - takes in a node and tells you its headernodeid if it has one, else it
	// returns -1
	static int findHeaderNodeId(TreeItem item){
		int i = 0;
		while(i < headernodes){
			if(headernodeitems[i].equals(item)){
				return i;
			}
			i++;
		}
		return -1;
	}
	
	// findRefNodeNodeId(
	// TreeItem, the item that you're trying to find the headernode id of
	// )
	// - takes in a refnode and tells you its refheadernodeid if it has one, else it
	// returns -1
	static int findRefHeaderNodeId(TreeItem item){
		int i = 0;
		while(i < refheadernodes){
			if(refheadernodeitems[i].equals(item)){
				return i;
			}
			i++;
		}
		return -1;
	}

	// findNodeId(
	// TreeItem, the item that you're trying to find the node id of
	// )
	// - takes in a node and tells you its nodeid if it has one, else it returns
	// -1
	static int findNodeId(TreeItem item){
		int i = 0;
		while(i < nodes){
			if(nodeitems[i].equals(item)){
				return i;
			}
			i++;
		}
		return -1;
	}
	
	// findRefNodeId(
	// TreeItem, the item that you're trying to find the refnode id of
	// )
	// - takes in a refnode and tells you its refnodeid if it has one, else it returns
	// -1
	static int findRefNodeId(TreeItem item){
		int i = 0;
		while(i < refnodes){
			if(refnodeitems[i].equals(item)){
				return i;
			}
			i++;
		}
		return -1;
	}

	// refreshColors()
	// - refreshes the node colors of the entire tree
	static void refreshColors(){
		int ni = 0;
		while(ni < nodes){

			// if(nodesource[ni]==current){
			if(nodeitems[ni].isDisposed() == false){
				int si = nodesource[ni];
				if(si==-1){
					si = findHeaderNodeId(nodeitems[ni]);
				}
				if(nodeitems[ni].getText().startsWith(">")){
					si = findHeaderNodeId(nodeitems[ni]);
				}
				if(si != -1){
					if(nodecolorset[si]){
						nodecolors[si].dispose();
					}
					nodecolors[si] = getNodeColor(si);

					if(nodeitems[ni].equals(headernodeitems[si])){
						final int sid =si;
						nodeitems[ni].addDisposeListener(new DisposeListener(){
							public void widgetDisposed(DisposeEvent e){
								
								if(!nodecolors[sid].isDisposed()){
									nodecolors[sid].dispose();
									nodecolorset[sid] = false;
								}
							}
						});
					}
					nodeitems[ni].setBackground(nodecolors[si]);
					if(nodeitems[ni].equals(oldselitem)){
						oldselbackground=nodeitems[ni].getBackground();
					}
				}
				// }
			}
			ni++;
		}
	}

	// addNode(
	// TreeItem, this is the parent of the new node
	// String, this is the angle that the new node is at
	// )
	// Also: this uses current_z to determine which layer the new node is on
	// current_z is sourced from the newnodelayertextbox.getText() whenever
	// the new node button is pressed (see main)
	// - Add a new headernode to the tree under the parent item and at the
	// specified angle
	static void addNode(TreeItem item, String angle){
		if(item==null){
			return;
		}
		if(item.isDisposed()){
			return;
		}
		if(current_tree_tab == 2){
			if(item.getText().startsWith("{")){
				refnodeitems[refnodes] = new TreeItem(item, 0, item.getItemCount()-1);
			}else{
				refnodeitems[refnodes] = new TreeItem(item, 0);
			}
			refnodetype[refnodes] = 0;
			refheadernodeitems[refheadernodes] = refnodeitems[refnodes];
			int i = 0;
			int sid = 0;
			while(i < refheadernodes){
				if(item.equals(refheadernodeitems[i])){
					sid = i;
					break;
				}
				i++;
			}
			int current_ref_z = Integer.parseInt(newnodelayertextbox.getText());
			last_ref_z = 0;
			TreeItem par = refnodeitems[refnodes].getParentItem();
			while(par!=null){
				int addz=0;
				try{
					String t=par.getText();
					if(t.indexOf("LAYER")==-1){
						addz=0;
					}else{
						int dist = t.indexOf(") . LAYER) + ") + 13;
						addz = Integer.parseInt(t.substring(dist,t.length()-1));
					}
				}catch(Exception e){
					addz=0;
				}
				last_ref_z+=addz;
				par = par.getParentItem();
				if(addz!=0){ // only need to find one layer-setting node 
					break; // because they're not additive
				} // however this is still a while loop, because some nodes may not be layer-setting
			}
			if(current_ref_z != last_ref_z){
				//int dist_from_root = 0;
				int first_temp =  getRefTemp(item);
				par = refnodeitems[refnodes].getParentItem();
				while(par!=null){
					//dist_from_root++;
					first_temp =  getRefTemp(par);
					par = par.getParentItem();
				}
				refnodeitems[refnodes].setText(">TEMP " + refplace[sid] + "|>LAYER (((TEMP " + first_temp + ") . LAYER) + " + current_ref_z + ")");
				//refnodeitems[refnodes].setText(">TEMP " + sid + "|>LAYER (((BACK " + dist_from_root + ") . LAYER) + " + current_ref_z + ")");
			}else{
				refnodeitems[refnodes].setText(">TEMP " + refplace[sid] + "|>ANGLE " + angle);
			}
			last_ref_z=current_ref_z;
			refnodeitems[refnodes].setFont(font10);
			refnodeitems[refnodes].setExpanded(true);
			refnodes++;

			currentref = refheadernodes;

			refheadernodes++;
			refplace[(refheadernodes -1)] = firstFreeTemp();
			refnodeitems[refnodes] = new TreeItem(refnodeitems[refnodes - 1], 0);
			refnodeitems[refnodes].setText("$TEMP " + refplace[(refheadernodes - 1)]);
			refnodeitems[refnodes].setFont(font8);
			refnodetype[refnodes]=1;
			refnodeitems[refnodes].setExpanded(true);
			refnodes++;
			refnodeitems[refnodes - 2].setExpanded(true);
			return;
		}
		int i = 0;
		TreeItem sourceitem = headernodeitems[0];
		int sourceid = 0;
		while(i < headernodes){
			if(headernodeitems[i].equals(item)){
				sourceitem = headernodeitems[i];
				sourceid = i;
				break;
			}
			i++;
		}
		if(i==headernodes){
			return;
		}
		nodesource[nodes] = i;
		if(i == 0){
			nodeitems[nodes] = new TreeItem(item, 0,
					sourceitem.getItemCount() - 1);
		}else{
			nodeitems[nodes] = new TreeItem(item, 0);
		}
		int last_z = headernode[sourceid].getFillZ();
		if(current_z != last_z){
			nodeitems[nodes].setText(">" + nodeplace[current] + "|>LAYER " + current_z);
		}else{
			nodeitems[nodes].setText(">" + nodeplace[current] + "|>ANGLE " + angle);
		}
		try {
			nodeangle[headernodes] = Double.parseDouble(angle);
		}catch (Exception e){
		}
		headernode[headernodes] = new MapNode(headernodes, current_z);
		headernodeitems[headernodes] = nodeitems[nodes];
		headernodeitems[headernodes].setFont(font10);
		nodetype[nodes] = 0;
		headernodes++;
		nodes++;
		autoInsertPlace(nodeitems[nodes - 1], headernodes - 1);
		setupNode(nodeitems[nodes - 2], headernodes - 1);
		viewLayerTab(currentlayertabz);
		current = headernodes - 1;
		
		previewboss.flushSelections();
		previewboss.select(nodeplace[headernodes-1],headernode[headernodes-1].getFillZ());
	}

	// addNode_forced(
	// TreeItem, this is the parent node that you're adding to
	// String, this is the text content of the new node (what it says)
	// int, this is the layer to add the node to
	// boolean, if true adds this to the reference panel
	// )
	// - Same as above, but lets you specify entirely what the node says instead
	// of it being preconstructed
	// - Used to import special headernode types, like TEMP or BACK
	static void addNodeForced(TreeItem item, String text, int z, boolean ref){
		
		if(ref){
			if(item.getText().startsWith("{")){
				refnodeitems[refnodes] = new TreeItem(item, 0, item.getItemCount()-1);
			}else{
				refnodeitems[refnodes] = new TreeItem(item, 0);
			}
			refnodetype[refnodes] = 0;
			refheadernodeitems[refheadernodes] = refnodeitems[refnodes];
			/*
			 * int i=0; int sid = 0; while(i<refheadernodes){
			 * if(item.equals(refheadernodeitems[i])){ sid=i; break; } i++; }
			 */
			refnodeitems[refnodes].setText(text);
			refnodeitems[refnodes].setFont(font10);
			refnodeitems[refnodes].setExpanded(true);
			refnodes++;

			currentref = refheadernodes;

			refheadernodes++;
			refplace[(refheadernodes -1)] = firstFreeTemp();
			refnodeitems[refnodes] = new TreeItem(refnodeitems[refnodes - 1], 0);
			refnodeitems[refnodes].setText("$TEMP " + refplace[(refheadernodes - 1)]);
			refnodeitems[refnodes].setFont(font8);
			refnodeitems[refnodes].setExpanded(true);
			refnodes++;
			refnodeitems[refnodes - 2].setExpanded(true);
			return;
		}

		int i = 0;
		TreeItem sourceitem = headernodeitems[0];
		while(i < headernodes){
			if(headernodeitems[i].equals(item)){
				sourceitem = headernodeitems[i];
				break;
			}
			i++;
		}
		nodesource[nodes] = i;
		if(item==null){
			item=sourceitem;
		}
		if(item==null){
			nodeitems[nodes] = new TreeItem(nodetree, 0,findInsertionPoint(i,false));
		}else{
			if(i == 0){
				nodeitems[nodes] = new TreeItem(item, 0, findInsertionPoint(i,false));
			}else{
				nodeitems[nodes] = new TreeItem(item, 0,findInsertionPoint(i,false));
			}
		}
		nodeitems[nodes].setText(text);
		nodeangle[headernodes] = -1;
		headernode[headernodes] = new MapNode(headernodes, z);
		headernodeitems[headernodes] = nodeitems[nodes];
		headernodeitems[headernodes].setFont(font10);
		nodetype[nodes] = 0;
		headernodes++;
		nodes++;
		autoInsertPlace(nodeitems[nodes - 1], headernodes - 1);
		setupNode(nodeitems[nodes - 2], headernodes - 1);

		current = headernodes - 1;
	}

	// autoInsertPlace(
	// TreeItem, the parent node that you're adding to
	// int, the id of the parent headernode (i.e. it's headernode count)
	// )
	// - Inserts a place node to the new node.
	// - Should be called on every headernode you add.
	static void autoInsertPlace(TreeItem item, int id){
		TreeItem placeitem = new TreeItem(item, 0);
		if(id!=0){
			nodeplace[id]=firstFreePlace();
		}else{
			nodeplace[id]=0;
		}
		System.out.println(" NODE PLACE " + nodeplace[id]);
		placeitem.setText("$PLACE " + nodeplace[id]);
		nodeitems[nodes] = placeitem;
		int i = 0;
		while(i < headernodes){
			if(headernodeitems[i].equals(item)){
				break;
			}
			i++;
		}
		nodesource[nodes] = i;
		nodetype[nodes] = 1;
		nodes++;
		setupNode(nodeitems[nodes - 1], i);
	}

	// insertForced(
	// TreeItem, parent node you're inserting into
	// String, text of the new node
	// int, sourceid (headernode count) of the parent node
	// )
	// - Allows you to insert a node with any text
	// - Used to import unsupported nodetypes
	static void insertForced(TreeItem insitem, String len, int cur, boolean ref){
		TreeItem coloritem;
		
		if(ref){
			if(insitem.getText().startsWith("{")){
				coloritem = new TreeItem(insitem, 0, insitem.getItemCount()-1);
			}else{
				coloritem = new TreeItem(insitem, 0);
			}
			coloritem.setText(len);
			refnodeitems[refnodes] = coloritem;
			refnodetype[refnodes] = 100;
			len = len.toUpperCase();
			if(len.startsWith("$TEMP")){
				refnodetype[refnodes] = 1;
				try{
					refplace[findRefHeaderNodeId(insitem)] = Integer.parseInt(len.substring(6));
				}catch(Exception e){
				}
				// destroy existing temp nodes
				TreeItem[] citems = insitem.getItems();
				int i=0;
				while(i<citems.length){
					if(!citems[i].isDisposed()){
						if(citems[i].getText().startsWith("$TEMP") && !citems[i].equals(coloritem)){
							citems[i].dispose();
						}
					}
					i++;
				}
			}else if(len.startsWith("$COLOR")){
				refnodetype[refnodes] = 2;
			}else if(len.startsWith("$RECT")){
				refnodetype[refnodes] = 3;
			}else if(len.startsWith("$BUFFER")
					&& !len.startsWith("$BUFFER_BOX")){
				refnodetype[refnodes] = 4;
			}else if(len.startsWith("$CIRCLE")){
				refnodetype[refnodes] = 5;
			}else if(len.startsWith("$CUT")
					&& !len.startsWith("$CUT_OVERLAP")&& !len.startsWith("$CUT_SCALE")){
				refnodetype[refnodes] = 6;
			}else if(len.startsWith("$PRECISE")){
				refnodetype[refnodes] = 7;
			}else if(len.startsWith("$SPACE")){
				refnodetype[refnodes] = 8;
			}else if(len.startsWith("$FILL")){
				refnodetype[refnodes] = 9;
			}else if(len.startsWith("$BUFFER_BOX")){
				refnodetype[refnodes] = 10;
			}else if(len.startsWith("$CUT_OVERLAP")){
				refnodetype[refnodes] = 11;
			}else if(len.startsWith("$POLY_ENDX")){
				refnodetype[refnodes]=25;
			}else if(len.startsWith("$POLY")){
				refnodetype[refnodes] = 12;
			}else if(len.startsWith("$VERTEX")){
				refnodetype[refnodes] = 13;
			}else if(len.startsWith("$ROT_CENTER")){
				refnodetype[refnodes] = 14;
			}else if(len.startsWith("$OUTLINE")){
				refnodetype[refnodes] = 15;
			}else if(len.startsWith("$EXTRA")){
				refnodetype[refnodes] = 16;
			}else if(len.startsWith("{") && len.contains("}")){
				refnodetype[refnodes] = 17;
			}else if(len.startsWith("$VAR")){
				refnodetype[refnodes] = 18;
			}else if(len.startsWith("$CURVE_WEIGHT")){
				refnodetype[refnodes] = 19;
			}else if(len.startsWith("$TEXT_SIZE")){
				refnodetype[refnodes]=23;
			}else if(len.startsWith("$TEXT_FONT")){
				refnodetype[refnodes]=24;
			}else if(len.startsWith("$TEXT")){
				refnodetype[refnodes]=20;
			}else if(len.startsWith("$PDF_SIZE")){
				refnodetype[refnodes]=21;
			}else if(len.startsWith("$PDF_MARGIN")){
				refnodetype[refnodes]=22;
			}else if(len.startsWith("$CORNER_CURVE")){
				refnodetype[refnodes]=27;
			}else if(len.startsWith("$CORNER")){
				refnodetype[refnodes]=25;
			}else if(len.startsWith("$PERPSPACE")){
				refnodetype[refnodes]=26; 
			}else if(len.startsWith("$CUT_SCALE")){
				refnodetype[refnodes]=28;
			}else if(len.startsWith("$RESCALE")){
				refnodetype[refnodes]=29;
			}
			
			coloritem.setFont(font8);
			coloritem.setExpanded(true);
			refnodes++;
			return;
		}
		int i = 0;
		while(i < headernodes){
			if(headernodeitems[i].equals(insitem)){
				break;
			}
			i++;
		}
		coloritem = new TreeItem(insitem, 0,findInsertionPoint(i, false));
		coloritem.setText(len);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = cur;
		nodetype[nodes] = 100;
		len = len.toUpperCase();
		if(len.startsWith("$PLACE")){
			nodetype[nodes] = 1;
			try{
				nodeplace[findHeaderNodeId(insitem)] = Integer.parseInt(len.substring(7));
			}catch(Exception e){
				
			}
			// destroy existing place nodes
			TreeItem[] citems = insitem.getItems();
			i=0;
			while(i<citems.length){
				if(!citems[i].isDisposed()){
					if(citems[i].getText().startsWith("$PLACE") && !citems[i].equals(nodeitems[nodes])){
						citems[i].dispose();
					}
				}
				i++;
			}
		}else if(len.startsWith("$COLOR")){
			nodetype[nodes] = 2;
		}else if(len.startsWith("$RECT")){
			nodetype[nodes] = 3;
		}else if(len.startsWith("$BUFFER") && !len.startsWith("$BUFFER_BOX")){
			nodetype[nodes] = 4;
		}else if(len.startsWith("$CIRCLE")){
			nodetype[nodes] = 5;
		}else if(len.startsWith("$CUT") && !len.startsWith("$CUT_OVERLAP")){
			nodetype[nodes] = 6;
		}else if(len.startsWith("$PRECISE")){
			nodetype[nodes] = 7;
		}else if(len.startsWith("$SPACE")){
			nodetype[nodes] = 8;
		}else if(len.startsWith("$FILL")){
			nodetype[nodes] = 9;
		}else if(len.startsWith("$BUFFER_BOX")){
			nodetype[nodes] = 10;
		}else if(len.startsWith("$CUT_OVERLAP")){
			nodetype[nodes] = 11;
		}else if(len.startsWith("$POLY")){
			nodetype[nodes] = 12;
		}else if(len.startsWith("$VERTEX")){
			nodetype[nodes] = 13;
		}else if(len.startsWith("$ROT_CENTER")){
			nodetype[nodes] = 14;
		}else if(len.startsWith("$OUTLINE")){
			nodetype[nodes] = 15;
		}else if(len.startsWith("$EXTRA")){
			nodetype[nodes] = 16;
		}else if(len.startsWith("{") && len.contains("}")){
			nodetype[nodes] = 17;
		}else if(len.startsWith("$VAR")){
			nodetype[nodes] = 18;
		}else if(len.startsWith("$TEXT_SIZE")){
			nodetype[nodes]=23;
		}else if(len.startsWith("$TEXT_FONT")){
			nodetype[nodes]=24;
		}else if(len.startsWith("$TEXT")){
			nodetype[nodes]=20;
		}else if(len.startsWith("$PDF_SIZE")){
			nodetype[nodes]=21;
		}else if(len.startsWith("$PDF_MARGIN")){
			nodetype[nodes]=22;
		}else if(len.startsWith("$CORNER_CURVE")){
			nodetype[nodes]=27;
		}else if(len.startsWith("$CORNER")){
			nodetype[nodes]=25;
		}else if(len.startsWith("$PERPSPACE")){
			nodetype[nodes]=26;
		}
		setupNode(nodeitems[nodes], cur);
		nodes++;
	}

	// setPropertyNode(
	// String, this is the text of the new node
	// Int, this is the nodetype of the new node
	// Boolean, this is whether or not to delete if there's an existing node
	// Int[], this is the list of nodetypes that this one can overwrite
	// Boolean, this is whether or not the new node has priority (true = comes
	// in at top of list)
	// )
	// - used by most node-setting functions
	// - adds a node or replaces/deletes an existing one based on context
	// NOTE: KEEP A LIST OF FUNCTIONS THAT DO NOT USE THIS ONE:
	// - setVertex
	// - setBufBox
	// - setFill
	// - removeVertex
	static void setPropertyNode(String text, int type, boolean delete,
			int[] overridetypes, boolean priority, boolean ignore_same){
		if(current_tree_tab == 2){
			if(!ignore_same){
				int i = 0;
				while(i < refnodes){
					if(!refnodeitems[i].isDisposed()){	
						TreeItem refparent = refnodeitems[i].getParentItem();
						if(refparent != null && !refparent.isDisposed()){
							if(refparent.equals(refheadernodeitems[currentref])){
								boolean got_override = false;
								int g = 0;
								while(g < overridetypes.length){
									if(refnodetype[i] == overridetypes[g]){
										got_override = true;
									}
									g++;
								}
								if(refnodetype[i] == type || got_override){
									if(delete){
										refnodeitems[i].dispose();
										if(i == refnodes - 1){
											refnodes--;
										}
										actionHook(true);
										return;
									}else{
										refnodeitems[i].setText(text);
										refnodetype[i] = type;
										actionHook(true);
									}
									return;
								}
							}
						}
					}
					i++;
				}
			}
			if(refheadernodeitems[currentref]!=null && !refheadernodeitems[currentref].isDisposed()){
				TreeItem coloritem = new TreeItem(refheadernodeitems[currentref], 0,
						findRefInsertionPoint(currentref, priority));
				coloritem.setText(text);
				refnodeitems[refnodes] = coloritem;
				// refnodesource[refnodes]=current;
				refnodetype[refnodes] = type;
				coloritem.setFont(font8);
				coloritem.setExpanded(true);
				// setupNode(refnodeitems[refnodes],currentref);
				refnodes++;
			}
			actionHook(true);
			return;
		}
		// TODO
		// if no priority, then use oldselnode as insertion point.
		if(!ignore_same){
			int i = 0;
			while(i < nodes){
				if(nodesource[i] == current){
					boolean got_override = false;
					int g = 0;
					while(g < overridetypes.length){
						if(nodetype[i] == overridetypes[g]){
							got_override = true;
						}
						g++;
					}
					if(nodetype[i] == type || got_override){
						if(delete){
							nodeitems[i].dispose();
							if(i == nodes - 1){
								nodes--;
							}
							actionHook(true);
							return;
						}else{
							nodeitems[i].setText(text);
							nodetype[i] = type;
							actionHook(true);
						}
						return;
					}
				}
				i++;
			}
		}
		TreeItem coloritem = new TreeItem(headernodeitems[current], 0,
				findInsertionPoint(current, priority));
		coloritem.setText(text);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = type;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
		
		actionHook(true);
	}

	// setBuf(
	// String, this is the buffer length value ($BUFFER + len)
	// )
	// - Used to insert a $BUFFER node (type 4) at the current node
	// - (Recall current node is whichever headernode the user has selected last)
	static void setBuf(String len){
		int[] over = { 4 };
		setPropertyNode("$BUFFER " + len, 4, false, over, false, false);
	}

	// setRectBuf(
	// String, this is the buffer length value ($BUFFER + len + wid)
	// String, this is the buffer width value ( ^ )
	// )
	// - Used to insert a $BUFFER node (type 4) at the current node
	// - This buffer has both width and length dimensions so it is not uniform
	static void setRectBuf(String len, String wid){
		int[] over = { 4 };
		setPropertyNode("$BUFFER " + len + " " + wid, 4, false, over, false,
				false);
	}

	// setCirc(
	// String, this is the circle radius
	// String, this is the circle inner radius (0 for a circle, non-zero if itis a disk)
	// - Used to insert $CIRCLE nodes (type 5)
	// - if wid is non-zero, it will be a disk
	static void setCirc(String len, String wid){
		// first check if current has an existing circ node
		double iwid = -1;
		boolean failed = false;
		try {
			iwid = Double.parseDouble(wid);
		}catch (Exception e){
			iwid = -1;
			failed = true;
		}
		String text = "$CIRCLE";
		if(iwid == 0 || failed){
			text = "$CIRCLE " + len;
		}else{
			text = "$CIRCLE " + len + " " + wid;
		}
		int[] over = { 3, 5, 12, 20, 25 };
		setPropertyNode(text, 5, false, over, true, false);
	}

	// updateCirc()
	// - Used to update text boxes in the "Circ" tab when you select a headernode
	// that has a circle node attached to it
	static void updateCirc(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 5){
					if(getWordCount(nodeitems[i].getText()) == 3){
						String[] words = getWords(nodeitems[i].getText(), 3);
						circradtextbox.setText(words[1]);
						circinradtextbox.setText(words[2]);
					}else{
						String[] words = getWords(nodeitems[i].getText(), 2);
						circradtextbox.setText(words[1]);
					}
					return;
				}
			}
			i++;
		}
	}
	
	// setText(
	// String, this is the string of text
	// - Used to insert $TEXT nodes (type 20)
	static void setText(String intxt){
		String text = "$TEXT " + intxt;
		int[] over = { 3, 5, 12, 20, 25 };
		setPropertyNode(text, 20, false, over, true, false);
	}
	
	// setFontSize(
	// String, this is the scale of the font size (ex. 1.0)
	// - Used to insert $TEXT_SIZE nodes (type 23)
	static void setFontSize(String intxt){
		String text = "$TEXT_SIZE " + intxt;
		int[] over = { 23 };
		setPropertyNode(text, 23, false, over, true, false);
	}
	
	// setFontType(
	// String, this is the scale of the font size (ex. 1.0)
	// - Used to insert $TEXT_SIZE nodes (type 23)
	static void setFontType(String intxt){
		String text = "$TEXT_FONT " + intxt;
		int[] over = { 24 };
		setPropertyNode(text, 24, false, over, true, false);
	}
	
	// updateText()
	// - Used to update text boxes in the "Text" tab when you select a headernode
	// that has a text node attached to it
	static void updateText(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 20){
					String words = nodeitems[i].getText().replace("$TEXT ","");
					texttextbox.setText(words);
				}
				if(nodetype[i] == 23){
					String words = nodeitems[i].getText().replace("$TEXT_SIZE ","");
					textfontsizebox.setText(words);
				}
				if(nodetype[i] == 24){
					String words = nodeitems[i].getText().replace("$TEXT_FONT ","");
					textfonttypebox.setText(words);
				}
			}
			i++;
		}
	}

	// setExtra(
	// String, this is the extra length
	// String, this is the extra width
	// )
	// - Used to insert $EXTRA nodes (type 16)
	static void setExtra(String len, String wid){
		int[] over = { 16 };
		setPropertyNode("$EXTRA " + len + " " + wid, 16, false, over, false,
				false);
	}

	// updateExtra()
	// - Used to update text boxes in the "Circ" tab when you select a headernode
	// that has a circle node attached to it
	static void updateExtra(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 16){
					String[] words = getWords(nodeitems[i].getText(), 3);
					extralentextbox.setText(words[1]);
					extrawidtextbox.setText(words[2]);
					return;
				}
			}
			i++;
		}
	}

	// setFill(
	// String, this is the pixel conversion of the new Fill node
	// )
	// - Used to insert a new Fill node (type 9)
	// - There can only be one Fill node at a time, so if one exists it will
	// overwrite that one.
	static void setFill(String len){
		// first check if current has an existing fill node
		int i = 0;
		while(i < nodes){
			// if(nodesource[i]==current){
			// for Fill, there can only be one node
			if(nodetype[i] == 9){
				nodeitems[i].setText(">0|$FILL " + len);
				filllen=len;
				actionHook(true);
				return;
			}
			// }
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[current], 0,
				findInsertionPoint(current, true));
		coloritem.setText(">0|$FILL " + len);
		filllen=len;
		fillitem=coloritem;
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 9;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
		actionHook(true);
	}
	
	// setCutScale(
	// String, this is the new cut scale (px width of cut outlines)
	// )
	// - Used to insert a new Fill node (type 9)
	// - There can only be one Fill node at a time, so if one exists it will
	// overwrite that one.
	static void setCutScale(String len){
		// first check if current has an existing fill node
		int i = 0;
		while(i < nodes){
			// if(nodesource[i]==current){
			// for Fill, there can only be one node
			if(nodetype[i] == 28){
				nodeitems[i].setText(">0|$CUT_SCALE " + len);
				actionHook(true);
				return;
			}
			// }
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[0], 0,
				findInsertionPoint(0, true));
		coloritem.setText(">0|$CUT_SCALE " + len);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 28;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
		actionHook(true);
	}
	
	// setReScale(
	// String, this is the new cut scale (px width of cut outlines)
	// )
	// - Used to insert a new Fill node (type 9)
	// - There can only be one Fill node at a time, so if one exists it will
	// overwrite that one.
	static void setReScale(String len){
		// first check if current has an existing fill node
		int i = 0;
		while(i < nodes){
			// if(nodesource[i]==current){
			// for Fill, there can only be one node
			if(nodetype[i] == 29){
				nodeitems[i].setText(">0|$RESCALE " + len);
				actionHook(true);
				return;
			}
			// }
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[0], 0,
				findInsertionPoint(0, true));
		coloritem.setText(">0|$RESCALE " + len);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 29;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
		actionHook(true);
	}

	// setSpace(
	// String, this is the length of the spacing
	// )
	// - Used to add a new Space node (type 8)
	// - Determines how far this headernode is from its parent
	static void setSpace(String len){
		// first check if current has an existing space node
		int[] over = { 8 };
		setPropertyNode("$SPACE " + len, 8, false, over, true, false);
	}
	
	// setPerpSpace(
	// String, this is the length of the spacing
	// )
	// - Used to add a new Space node (type 8)
	// - Determines how far this headernode is from its parent
	static void setPerpSpace(String len){
		// first check if current has an existing space node
		int[] over = { 26 };
		setPropertyNode("$PERPSPACE " + len, 26, false, over, true, false);
	}

	// setBufBox(
	// String, this is the buffer length value ($BUFFER + len + wid)
	// String, this is the buffer width value ( ^ )
	// )
	// - Used to insert a $BUFFER_BOX node (type 10) at the end of the design
	static void setBufBox(String len, String wid){
		// first check if current has an existing bufbox node
		int i = 0;
		while(i < nodes){
			// if(nodesource[i]==current){
			// there can only be one bufbox
			if(nodetype[i] == 10){
				if(nodeitems[i].getText().equalsIgnoreCase("$BUFFER_BOX " + len + " " + wid)){
					nodeitems[i].dispose();
					if(i == nodes - 1){
						nodes--;
					}
					nodetype[i]=-1;
				}else{
					nodeitems[i].setText("$BUFFER_BOX " + len + " " + wid);
				}
				actionHook(true);
				return;
			}
			// }
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[current], 0,
				findInsertionPoint(current, false));
		coloritem.setText("$BUFFER_BOX " + len + " " + wid);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 10;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
		actionHook(true);
	}
	
	// setPDFSizein(
	// String, this is the pdf length
	// String, this is the pdf height
	// )
	// - Used to insert a $PDF_SIZE_IN (type 21) node at the end of the script
	static void setPDFSizein(String len, String wid){
		// first check if current has an existing bufbox node
		int i = 0;
		while(i < nodes){
			// if(nodesource[i]==current){
			// there can only be one bufbox
			if(nodetype[i] == 21){
				nodeitems[i].dispose();
				if(i == nodes - 1){
					nodes--;
				}
				nodetype[i]=-1;
				return;
			}
			// }
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[current], 0,
				findInsertionPoint(current, false));
		coloritem.setText("$PDF_SIZE_IN " + len + " " + wid);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 21;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
	}
	// Same as above, but for cm sizes
	static void setPDFSizecm(String len, String wid){
		// first check if current has an existing bufbox node
		int i = 0;
		while(i < nodes){
			// if(nodesource[i]==current){
			// there can only be one bufbox
			if(nodetype[i] == 21){
				nodeitems[i].dispose();
				if(i == nodes - 1){
					nodes--;
				}
				nodetype[i]=-1;
				return;
			}
			// }
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[current], 0,
				findInsertionPoint(current, false));
		coloritem.setText("$PDF_SIZE_CM " + len + " " + wid);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 21;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
	}
	
	// setPDFMarginin(
	// String, this is the pdf margin length
	// String, this is the pdf margin height
	// )
	// - Used to insert a $PDF_MARGIN_IN (type 22) node at the end of the script
	static void setPDFMarginin(String len, String wid){
		// first check if current has an existing bufbox node
		int i = 0;
		while(i < nodes){
			// if(nodesource[i]==current){
			// there can only be one bufbox
			if(nodetype[i] == 22){
				nodeitems[i].dispose();
				if(i == nodes - 1){
					nodes--;
				}
				nodetype[i]=-1;
				return;
			}
			// }
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[current], 0,
				findInsertionPoint(current, false));
		coloritem.setText("$PDF_MARGIN_IN " + len + " " + wid);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 22;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
	}
	// Same as above, but for cm sizes
	static void setPDFMargincm(String len, String wid){
		// first check if current has an existing bufbox node
		int i = 0;
		while(i < nodes){
			// if(nodesource[i]==current){
			// there can only be one bufbox
			if(nodetype[i] == 22){
				nodeitems[i].dispose();
				if(i == nodes - 1){
					nodes--;
				}
				nodetype[i]=-1;
				return;
			}
			// }
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[current], 0,
				findInsertionPoint(current, false));
		coloritem.setText("$PDF_MARGIN_CM " + len + " " + wid);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 22;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
	}

	// updateSpace()
	// - Used to update the Space textbox in the properties tab when the user
	// selects a new headernode
	static void updateSpace(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 8){
					String[] words = getWords(nodeitems[i].getText(), 2);
					spacetextbox.setText(words[1]);
					return;
				}
			}
			i++;
		}
	}
	
	// updatePerpSpace()
	// - Used to update the Space textbox in the properties tab when the user
	// selects a new headernode
	static void updatePerpSpace(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 26){
					String[] words = getWords(nodeitems[i].getText(), 2);
					perpspacetextbox.setText(words[1]);
					return;
				}
			}
			i++;
		}
	}

	// setCut()
	// - Adds a new Cut node (type 6)
	static void setCut(){
		int[] over = { 6 };
		setPropertyNode("$CUT", 6, true, over, false, false);
	}

	// setCutOverlap()
	// - Adds a new CutOverlap node (type 11)
	static void setCutOverlap(){
		// first check if current has an existing cut node
		int[] over = { 11 };
		setPropertyNode("$CUT_OVERLAP", 11, true, over, false, false);
	}

	// setPrecise()
	// - adds a new Precise node (type 7)
	static void setPrecise(){
		// first check if current has an existing precise node
		int[] over = { 7 };
		setPropertyNode("$PRECISE", 7, true, over, false, false);
	}

	// setOutline()
	// - turns on Polygon Outline mode (type 14)
	static void setOutline(){
		// first check if current has an existing cut node
		int[] over = { 15 };
		setPropertyNode("$OUTLINE", 15, true, over, false, false);
	}

	// setPoly()
	// - Adds a new Poly node (type 12)
	static void setPoly(){
		// first check if current has an existing cut node
		int[] over = { 3, 5, 12, 20, 25 };
		setPropertyNode("$POLY", 12, false, over, true, false);
	}

	// setVertex(
	// int, this is the number of the vertex (1+)
	// String, x of the vertex
	// String, y of the vertex
	// String, type of the vertex
	// String, buffer of the vertex
	// String, extra of the vertex
	// )
	// - Adds a new Rectangle node (type 13) of len and wid length & width
	static void setVertex(int num, String vx, String vy, String vt, String vb,
			String ve, boolean doprev){
		// first check if current has an existing rect node
		if(current_tree_tab == 2){
			int i = 0;
			int c = 0;
			while(i < refnodes){
				TreeItem refparent = refnodeitems[i].getParentItem();
				if(refparent != null){
					if(refparent.equals(refheadernodeitems[currentref])){
						if(refnodetype[i] == 13){
							c++;
							if(c == num){
								String ntxt = refnodeitems[i].getText();
								ntxt = ntxt.substring(6);
								double[] valout = Interpreter.parseValues(ntxt,5,false);
								int ctype = (int)(valout[2]);
								if(ctype==-1){
									ctype=0;
								}
								double bf = valout[3];
								if((int)(bf)==-1){
									bf=0;
								}
								double extra = valout[4];
								if((int)(extra)==-1){
									extra=0;
								}
								if(vt.equals("-1")){
									vt=""+ctype;
								}
								if(ve.equals("-1")){
									ve=""+extra;
								}
								
								refnodeitems[i].setText("$VERTEX " + vx + " "
										+ vy + " " + vt + " " + vb + " " + ve);
								return;
							}
							// return;
						}
					}
				}
				i++;
			}
			if(refheadernodeitems[currentref]!=null){
				TreeItem coloritem = new TreeItem(refheadernodeitems[currentref], 0,
						findInsertionPoint(currentref, false));
				if(vt.equals("-1")){
					vt="0";
				}
				if(ve.equals("-1")){
					ve="0";
				}
				coloritem.setText("$VERTEX " + vx + " " + vy + " " + vt + " " + vb
						+ " " + ve);
				refnodeitems[refnodes] = coloritem;
				refnodetype[refnodes] = 13;
				setupNode(refnodeitems[refnodes], currentref);
				refnodes++;
			}
			return;
		}
		int i = 0;
		int c = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 13){
					c++;
					if(c == num){
						String ntxt = nodeitems[i].getText();
						ntxt = ntxt.substring(6);
						double[] valout = Interpreter.parseValues(ntxt,5,false);
						int ctype = (int)(valout[2]);
						if(ctype==-1){
							ctype=0;
						}
						double bf = valout[3];
						if((int)(bf)==-1){
							bf=0;
						}
						double extra = valout[4];
						if((int)(extra)==-1){
							extra=0;
						}
						if(vt.equals("-1")){
							vt=""+ctype;
						}
						if(ve.equals("-1")){
							ve=""+extra;
						}
						nodeitems[i].setText("$VERTEX " + vx + " " + vy + " "
								+ vt + " " + vb + " " + ve);
						return;
					}
					// return;
				}
			}
			i++;
		}
		TreeItem coloritem = new TreeItem(headernodeitems[current], 0,
				findInsertionPoint(current, false));
		if(vt.equals("-1")){
			vt="0";
		}
		if(ve.equals("-1")){
			ve="0";
		}
		coloritem.setText("$VERTEX " + vx + " " + vy + " " + vt + " " + vb
				+ " " + ve);
		nodeitems[nodes] = coloritem;
		nodesource[nodes] = current;
		nodetype[nodes] = 13;
		setupNode(nodeitems[nodes], current);
		nodes++;
		viewLayerTab(currentlayertabz);
		
		//if(doprev){
		//	preview();
		//}
		actionHook(doprev);
	}

	// addVertex( see above sans first argument )
	// - rerouts to setVertex but with an infinitely (relatively speaking) large
	// vertex number, so it always results in the addition
	// of a new vertex
	static void addVertex(String vx, String vy, String vt, String vb, String ve, boolean doprev){
		setVertex(10000, vx, vy, vt, vb, ve,doprev);
	}

	// removeVertex(
	// Int, the number of the vertex to remove
	// )
	// - removes the vertex at position (num)
	static void removeVertex(int num, boolean doprev){
		// first check if current has an existing rect node
		if(current_tree_tab == 2){
			int i = 0;
			int c = 0;
			while(i < refnodes){
				if(refnodeitems[i] != null && !refnodeitems[i].isDisposed()){
					TreeItem refparent = refnodeitems[i].getParentItem();
					if(refparent != null){
						if(refparent.equals(refheadernodeitems[currentref])){
							if(refnodetype[i] == 13){
								c++;
								if(c == num){
									refnodeitems[i].dispose();
									refnodetype[i] = -1;
									if(refnodes - 1 == i){
										refnodes--;
									}
									return;
								}
								// return;
							}
						}
					}
				}
				i++;
			}
			return;
		}
		int i = 0;
		int c = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 13){
					c++;
					if(c == num){
						nodeitems[i].dispose();
						nodetype[i] = -1;
						if(i<nodes-1){
							if(nodetype[i+1]==19){
								nodeitems[i+1].dispose();
								nodetype[i+1] = -1;
							}
						}
						if(nodes - 1 == i){
							nodes--;
						}
						return;
					}
					// return;
				}
			}
			i++;
		}
		viewLayerTab(currentlayertabz);
		if(doprev){
			preview();
		}
	}

	// setCROT(
	// String, this is the x of the center of rotation
	// String, this is the y of the center of rotation
	// )
	// - sets the center of rotation for the polygon
	static void setCrot(String len, String wid){
		// first check if current has an existing rect node
		int[] over = { 14 };
		setPropertyNode("$ROT_CENTER " + len + " " + wid, 14, false, over,
				false, false);
	}

	// updateCROT()
	// - Updates the textboxes in the polygon tab when the user selects a
	// headernode with a poly node with a CROT modification
	static void updateCrot(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 14){
					String[] words = getWords(nodeitems[i].getText(), 3);
					crotxtextbox.setText(words[1]);
					crotytextbox.setText(words[2]);
					return;
				}
			}
			i++;
		}
	}
	
	
	// setCrvw(
	// String, this is the curve weight
	// )
	// - sets the polygon connection curve weight for the last connection
	static void setCrvw(String len){
		// first check if current has an existing rect node
		int[] over = { 19 };
		setPropertyNode("$CURVE_WEIGHT " + len, 19, false, over,
				false, true);
		// TODO
		// need to add after oldselitem
		// or if there's already one right after oldselitem, replace it.
	}
	
	// setPolyendx(
	// String, this is the curve weight
	// )
	// - sets the polygon connection curve weight for the last connection
	static void setPolyendx(String len){
		// first check if current has an existing rect node
		int[] over = { 25 };
		setPropertyNode("$POLY_ENDX " + len, 25, false, over,
				false, true);
	}

	// setRect(
	// String, this is the length of the Rectangle
	// String, this is the width of the Rectangle
	// )
	// - Adds a new Rectangle node (type 3) of len and wid length & width
	static void setRect(String len, String wid){
		// first check if current has an existing rect node
		int[] over = { 3, 5, 12, 20, 25 };
		setPropertyNode("$RECT " + len + " " + wid, 3, false, over, true, false);
	}

	// updateRect()
	// - Updates the textboxes in the rectangle tab when the user selects a
	// headernode with a rect node
	static void updateRect(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 3 && nodeitems[i]!=null){
					String[] words = getWords(nodeitems[i].getText(), 3);
					rectlentextbox.setText(words[1]);
					rectwidtextbox.setText(words[2]);
					return;
				}
			}
			i++;
		}
	}
	
	// setCorner(
	// String, this is the base of the corner
	// String, this is the height of the corner
	// String, this is the angle of the corner (Degrees)
	// )
	// - Adds a new Corner node (type 25) and gives it a base, height, angle
	static void setCorner(String len, String wid, String angle){
		// first check if current has an existing rect node
		int[] over = { 3, 5, 12, 20, 25 };
		setPropertyNode("$CORNER " + len + " " + wid + " " + angle, 25, false, over, true, false);
	}

	// updateCorner()
	// - Updates the textboxes in the rectangle tab when the user selects a
	// headernode with a rect node
	static void updateCorner(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 25 && nodeitems[i]!=null){
					String[] words = getWords(nodeitems[i].getText(), 4);
					rectcornerlentextbox.setText(words[1]);
					rectcornerwidtextbox.setText(words[2]);
					rectcornerangletextbox.setText(words[3]);
					return;
				}
			}
			i++;
		}
	}
	
	// setCorner(
	// String, this is the base of the corner
	// String, this is the height of the corner
	// String, this is the angle of the corner (Degrees)
	// )
	// - Adds a new Corner node (type 25) and gives it a base, height, angle
	static void setCornerCurve(String len){
		// first check if current has an existing rect node
		int[] over = { 27 };
		setPropertyNode("$CORNER_CURVE " + len, 27, false, over, true, false);
	}

	// updateCorner()
	// - Updates the textboxes in the rectangle tab when the user selects a
	// headernode with a rect node
	static void updateCornerCurve(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 27 && nodeitems[i]!=null){
					String[] words = getWords(nodeitems[i].getText(), 2);
					rectcornercurvetextbox.setText(words[1]);
					return;
				}
			}
			i++;
		}
	}

	// updateBuf()
	// - Updates buffer textboxes in circle and rect tabs when the user selects
	// a headernode with a buffer
	static void updateBuf(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 4){
					if(getWordCount(nodeitems[i].getText()) == 3){
						String[] words = getWords(nodeitems[i].getText(), 3);
						rectbuflentextbox.setText(words[1]);
						rectbufwidtextbox.setText(words[2]);
						circbuftextbox.setText(words[1]);
					}else{
						String[] words = getWords(nodeitems[i].getText(), 2);
						rectbuflentextbox.setText(words[1]);
						//rectbufwidtextbox.setText("0");
						circbuftextbox.setText(words[1]);
					}
					return;
				}
			}
			i++;
		}
	}

	// setColor(
	// String, red color value
	// String, green color value
	// String, blue color value
	// )
	// - Adds a new color node (type 2) to the current headernode
	static void setColor(String r, String g, String b){
		// first check if current has a color node
		int[] over = { 2 };
		setPropertyNode("$COLOR " + r + " " + g + " " + b, 2, false, over,
				false, false);
	}

	// updateColor()
	// - Updates the color textboxes in the properties tab when you select a
	// headernode that has a color node
	static void updateColor(){
		int i = 0;
		while(i < nodes){
			if(nodesource[i] == current){
				if(nodetype[i] == 2){
					String[] words = getWords(nodeitems[i].getText(), 4);
					redcolortextbox.setText(words[1]);
					greencolortextbox.setText(words[2]);
					bluecolortextbox.setText(words[3]);
					return;
				}
			}
			i++;
		}
	}

	// getWords(
	// String, this is the text to search
	// int, this is the max number of words to grab
	// )
	// - Gets a list of words from an input string
	// - a word is defined as a closed set of parenthesis or units separated by
	// spaces
	// Ex. The brown fox == {"the", "brown", "fox"}
	// Ex. Color (2 + (4 - 1)) (1 + 1) == {"Color", "(2 + (4 - 1))", "(1 + 1)"}
	static String[] getWords(String in, int max){
		String[] out = new String[max];
		int c = 0;
		while(c < max){
			out[c] = "";
			c++;
		}
		c = 0;
		int filled = 0;
		String curstr = "";
		int parens = 0;
		while(c < in.length() && filled < max){
			if(in.substring(c, c + 1).equalsIgnoreCase(" ") && parens == 0){
				out[filled] = curstr;
				filled++;
				curstr = "";

			}else{
				curstr = curstr + "" + in.substring(c, c + 1);
				if(in.substring(c, c + 1).equalsIgnoreCase("(")){
					parens++;
				}else if(in.substring(c, c + 1).equalsIgnoreCase(")")){
					parens--;
				}
			}
			c++;
		}
		if(filled < max){
			out[filled] = curstr;
		}
		return out;
	}

	// getWordCount(
	// String, this is the input text to search
	// )
	// - returns the number of distinct word units in the text
	// - see above for definition of word
	static int getWordCount(String in){
		int c = 0;
		int filled = 0;
		int parens = 0;
		while(c < in.length()){
			if(in.substring(c, c + 1).equalsIgnoreCase(" ") && parens == 0){
				filled++;
			}else{
				if(in.substring(c, c + 1).equalsIgnoreCase("(")){
					parens++;
				}else if(in.substring(c, c + 1).equalsIgnoreCase(")")){
					parens--;
				}
			}
			c++;
		}
		if(!in.endsWith(" ")){
			filled++;
		}
		return filled;
	}

	// checkPath(
	// TreeItem, this is the selected node
	// Boolean, this is whether that node is already checked
	// Boolean, this is whether that node is already grayed
	// )
	// - Called when the user checks a node
	// - Handles changing the checked state
	static void checkPath(TreeItem item, boolean checked, boolean grayed){
		if(item != null){
			if(grayed){
				checked = true;
			}else{
				int i = 0;
				TreeItem[] items = item.getItems();
				while(i < items.length){
					TreeItem child = items[i];
					if(child.getGrayed() || checked != child.getChecked()){
						checked = true;
						grayed = true;
						break;
					}
					i++;
				}
			}
			item.setChecked(checked);
			item.setGrayed(grayed);
			checkPath(item.getParentItem(), checked, grayed);
		}
	}

	// checkItems(
	// TreeItem, this is the selected node
	// Boolean, this is whether that node is already checked
	// )
	// - Called when the user checks a node
	// - Handles changing the checked state
	static void checkItems(TreeItem item, boolean checked){
		item.setGrayed(false);
		item.setChecked(checked);
		TreeItem[] items = item.getItems();
		int i = 0;
		while(i < items.length){
			checkItems(items[i], checked);
			i++;
		}
	}

	// selectTreeItem(
	// TreeItem, this is the selected node
	// )
	// - Called when the user selects a node
	// - Calls all of the update functions to update the textboxes in the UI
	static void selectTreeItem(TreeItem item){
		if(!oldselitem.isDisposed()){
			oldselitem.setBackground(oldselbackground);
		}
		oldselbackground=item.getBackground();
		item.setBackground(new Color(display,255,255,255));
		oldselitem=item;
		
		int i = 0;
		while(i < headernodes){
			if(headernodeitems[i].equals(item)){
				current = i;
				updateColor();
				updateRect();
				updateBuf();
				updateCirc();
				updateSpace();
				updatePerpSpace();
				updateCrot();
				updateExtra();
				updateText();
				updateCorner();
				updateCornerCurve();
				previewboss.flushSelections();
				previewboss.select(nodeplace[i],getHeaderNode(i).getFillZ());
				return;
			}
			i++;
		}
		// if we get here, then this item is not a > node
		/*int nodeid = 0;
		i = 0;
		while(i < nodes){
			if(nodeitems[i].equals(item)){
				nodeid = i;
				break;
			}
			i++;
		}
		int nodesourceid = 0;
		i = 0;
		while(i < headernodes){
			if(headernodeitems[i].equals(nodeitems[nodesource[nodeid]])){
				nodesourceid = i;
				System.out.println(" FOUND " + nodesourceid);
				break;
			}
			i++;
		}
		System.out.println(" FOUND C " + headernodeitems[nodesourceid].getText());
		System.out.println(" SRC " + nodesource[nodeid] + " NODE ID " + nodeid);*/
		TreeItem srcitem = item.getParentItem();
		
		int nodesourceid = 0;
		i = 0;
		while(i < headernodes){
			if(headernodeitems[i].equals(srcitem)){
				nodesourceid = i;
				break;
			}
			i++;
		}
		current = nodesourceid;
		
		int nodeid = 0;
		i = 0;
		while(i < nodes){
			if(nodeitems[i].equals(item)){
				nodeid = i;
				break;
			}
			i++;
		}
		if(nodetype[nodeid]==13){
			// vertex selection code
			// if the user clicks on a vertex node, updates the setvertex box
			// to have the corresponding vertex id
			i = 0;
			int c = 0;
			while(i < nodes){
				if(nodesource[i] == current){
					if(nodetype[i] == 13){
						c++;
						if(i==nodeid){
							polyvextextbox.setText("" + c);
							break;
						}
					}
				}
				i++;
			}
			String ntxt = item.getText();
			ntxt = ntxt.substring(6);
			double[] valout = Interpreter.parseValues(ntxt,5,false);
			int ctype = (int)(valout[2]);
			if(ctype==-1){
				ctype=0;
			}
			double bf = valout[3];
			if((int)(bf)==-1){
				bf=0;
			}
			double extra = valout[4];
			if((int)(extra)==-1){
				extra=0;
			}
			polyxtextbox.setText("" + valout[0]);
			polyytextbox.setText("" + valout[1]);
			polytypedropdown.select(ctype);
			polybtextbox.setText("" + bf);
			polyetextbox.setText("" + extra);
		}
		
		previewboss.flushSelections();
		previewboss.select(nodeplace[nodesourceid],getHeaderNode(nodesourceid).getFillZ());
		updateColor();
		updateRect();
		updateBuf();
		// TODO: sort out some selection issues:
		// when references are present, selection tends to fail
		// occasionally selecting from the tree highlights the wrong thing?
	}

	// getNodeColor(
	// int, this is the headernode id of the parent node
	// )
	// - Gives the unique(-ish) color associated with the id of the parent
	static Color getNodeColor(int nodesourceid){
		int cr = 0;
		int cg = 0;
		int cb = 0;
		if(colorbyz){
			nodesourceid = headernode[nodesourceid].getFillZ()+1;
		}else{
			nodesourceid++;
		}
		cr = (int) (210 + 45 * Math.sin(nodesourceid * colseed1));
		cg = (int) (210 + 45 * Math.cos(nodesourceid * colseed2));
		cb = (int) (200 + 25 * Math.sin(nodesourceid * colseed3) - 20 * Math
				.cos(nodesourceid * colseed4));
		return new Color(display.getCurrent(), cr, cg, cb);
	}

	// findInsertionPoint(
	// int, this is the headernode id of the parent node
	// boolean, this is whether or not the node insertion has priority (priority
	// nodes are inserted at the top of the chain)
	// )
	// - Used to find where a new node should be added to the chain
	static int findInsertionPoint(int sourceid, boolean priority){
		if(priority){
			return 1;
		}
		int i = 0;
		TreeItem sourcenode = nodeitems[0];
		while(i < nodes){
			if(nodeitems[i].equals(headernodeitems[sourceid])){
				sourcenode = nodeitems[i];
				break;
			}
			i++;
		}
		int lastgood = 0;
		TreeItem[] itemlist = sourcenode.getItems();
		i = 0;
		while(i < itemlist.length){
			if(itemlist[i].getText().substring(0, 1).equalsIgnoreCase(">")){
				break;
			}else{
				lastgood = i;
			}
			i++;
		}
		return lastgood + 1;
	}

	// findRefInsertionPoint(
	// int, this is the headernode id of the parent node
	// boolean, this is whether or not the node insertion has priority (priority
	// nodes are inserted at the top of the chain)
	// )
	// - Used to find where a new node should be added to the chain
	static int findRefInsertionPoint(int sourceid, boolean priority){
		if(priority){
			return 1;
		}
		int i = 0;
		TreeItem sourcenode = refnodeitems[0];
		while(i < refnodes){
			if(refnodeitems[i].equals(refheadernodeitems[sourceid])){
				sourcenode = refnodeitems[i];
				break;
			}
			i++;
		}
		int lastgood = 0;
		TreeItem[] itemlist = sourcenode.getItems();
		i = 0;
		while(i < itemlist.length){
			if(itemlist[i].getText().substring(0, 1).equalsIgnoreCase(">")
					|| itemlist[i].getText().substring(0, 1)
							.equalsIgnoreCase("}")){
				break;
			}else{
				lastgood = i;
			}
			i++;
		}
		return lastgood+1;
	}

	// output(
	// TreeItem[], a list of the nodes to output
	// FileWriter, the filewriter to output the text onto
	// )
	// - Used as part of saving the data to a file.
	// - Recursive; handles the nodes you feed in, and then calls itself on
	// their children nodes
	// - i.e. you should only have to call this on node 0
	static void output(TreeItem[] initems, FileWriter fw, boolean ref,String pathname){
		int i = 0;
		while(i < initems.length){
			TreeItem item = initems[i];
			String textout = item.getText();
			//System.out.println("" + textout);
			String totalts = "";
			if(!item.getChecked() && !ref && use_checks){
				textout = "\"" + textout;
			}
			while(item.getParentItem() != null){
				textout = "\t" + textout;
				totalts = totalts + "\t";
				item = item.getParentItem();
			}
			textout = textout.replaceAll("\\|",
					System.getProperty("line.separator") + totalts);
			//System.out.println("" + textout);

			try{
				if(textout.toUpperCase().contains("$FILL")){
					String fname = pathname;
					if(fname.contains("/")){
						fname = fname.substring(fname.lastIndexOf("/")+1);
					}
					if(fname.contains("\\")){
						fname = fname.substring(fname.lastIndexOf("\\")+1);
					}
					fw.write("" + textout + " " + fname + System.getProperty("line.separator"));
				}else{
					fw.write("" + textout + System.getProperty("line.separator"));
				}
			}catch (Exception e){
			}
			//System.out.println(textout);
			output(initems[i].getItems(), fw, ref,pathname);
			i++;
		}
	}

	// openFile(
	// String, the path of the file to open (Ex. "C:/...")
	// )
	// - Used to load a saved file into the program
	// - Replaces all existing nodes with those of the file
	static void openFile(String path){
		File f = new File(path);
		if(!f.exists()){
			//System.out.println(" File does not exist! ");
			return;
		}
		
		clearAll();
		FileReader fis;
		BufferedReader bis;
		//f = new File(path);
		int cur = 0;
		int curz = 0;
		boolean ref = false;
		int refnum = 0;
		int rcur = 0;
		//int rtempcur = 0;

		try {
			fis = new FileReader(f);
			bis = new BufferedReader(fis);
			while(bis.ready()){
				String text = bis.readLine();
				text = text.trim();
				text=text.toUpperCase();
				if(text.startsWith("#")){
					if(text.toUpperCase().startsWith("#COMBINE")){
						createCom(text.substring(9));
					}else{
						int intype = 0;
						// first figure out the type
						if(text.toUpperCase().startsWith("#ID")){
							intype = 2;
						}else if(text.toUpperCase().startsWith("#LAYER")){
							intype = 3;
						}else if(text.toUpperCase().startsWith("#LEFT") || text.toUpperCase().startsWith("#RIGHT") 
								|| text.toUpperCase().startsWith("#UP") || text.toUpperCase().startsWith("#DOWN")){
							intype = 4;
						}else if(text.toUpperCase().startsWith("#FLIPX")){
							intype = 5;
						}else if(text.toUpperCase().startsWith("#FLIPY")){
							intype = 6;
						}else if(text.toUpperCase().startsWith("#ROTATE")){
							intype = 7;
						}else if(text.toUpperCase().startsWith("#SQUARE") || text.toUpperCase().startsWith("#SQUARESPACE")){
							intype = 8;
						}else if(text.toUpperCase().startsWith("#BOX") || text.toUpperCase().startsWith("#DRAWBOXES")){
							intype = 9;
						}
						if(intype>=3 && intype<=7){
							int ins = currentcom;
							if(comheadernodeitems[currentcom].getText().toUpperCase().startsWith("#COMBINE")){
								ins = comheadernodes-1;
							}
							addComNode(comheadernodeitems[ins],text,intype);
						}else{
							addComNode(comheadernodeitems[currentcom],text,intype);
						}
					}
				}else if(text.startsWith("{")){
					if(text.contains("}")){
						if(ref){
							insertForced(refheadernodeitems[rcur], text, rcur,ref);
						}else{
							insertForced(headernodeitems[cur], text, cur, ref);
						}
					}else{
						if(text.length() >= 2){
							createRef(text.substring(2));
							//System.out.println(" MAKING REF " + text);
						}
						rcur = currentref; // TRIAL
						
						refnum++;
						ref = true;
					}
				}else if(text.startsWith("}")){
					refnum--;
					if(refnum <= 0){
						ref = false;
						refnum = 0;
					}
				}else if(text.startsWith(">")){
					boolean temppass=false;
					String rest = text.substring(1);
					try {
						int num = Integer.parseInt(rest);
						cur = getHeaderNodeIdFromPlace(num);
						curz = headernode[cur].getFillZ();
					}catch (Exception e){
						if(rest.toUpperCase().startsWith("Z")){
							try {
								curz = Integer.parseInt(rest.substring(2));
								if(ref){
									addNodeForced(refheadernodeitems[rcur], ">TEMP "
											+ refplace[rcur] + "|>LAYER " + curz, curz, ref);
								}else{
									addNodeForced(headernodeitems[cur], ">" + nodeplace[cur]
											+ "|>LAYER " + curz, curz, ref);
								}
							}catch (Exception e2){
								if(ref){
									addNodeForced(refheadernodeitems[rcur], ">TEMP "
											+ refplace[rcur] + "|>LAYER " + rest.substring(2), curz, ref);
								}else{
									addNodeForced(headernodeitems[cur], ">" + nodeplace[cur]
											+ "|>LAYER " + rest.substring(2), curz, ref);
								}
							}
							
						}else if(rest.toUpperCase().startsWith("LAYER")){
							try {
								curz = Integer.parseInt(rest.substring(6));
								if(ref){
									addNodeForced(refheadernodeitems[rcur], ">TEMP "
											+ refplace[rcur] + "|>LAYER " + curz, curz, ref);
								}else{
									addNodeForced(headernodeitems[cur], ">" + nodeplace[cur]
											+ "|>LAYER " + curz, curz, ref);
								}
							}catch (Exception e2){
								if(ref){
									addNodeForced(refheadernodeitems[rcur], ">TEMP "
											+ refplace[rcur] + "|>LAYER " + rest.substring(6), curz, ref);
								}else{
									addNodeForced(headernodeitems[cur], ">" + nodeplace[cur]
											+ "|>LAYER " + rest.substring(6), curz, ref);
								}
							}
							
						}else{
							if(ref){
								if(rest.toUpperCase().startsWith("TEMP")){
									// addNode_forced(refheadernodeitems[rcur],">"
									// + rest, curz,ref);
									// rtempcur=
									String rest2 = rest.substring(5);
									//System.out.println(rest);
									try{
										int num = Integer.parseInt(rest2);
										rcur = getRefHeaderNodeIdFromPlace(num);
										//System.out.println(" RCUR " + rcur);
									}catch(Exception ex){
										
									}
									temppass=true;
								}else{
									//System.out.println(" ADDING RCUR " + rcur);
									//System.out.println(" ADDING AT " + rcur + " PLACE: " + refplace[rcur]);
									addNodeForced(refheadernodeitems[rcur],
											">TEMP " + refplace[rcur] + "|>" + rest,
											curz, ref);
								}
							}else{
								if(cur<0){cur=0;}
								addNodeForced(headernodeitems[cur], ">" + nodeplace[cur]
									+ "|>" + rest, curz, ref);
							}
						}
						if(ref){
							if(!temppass){
								rcur = refheadernodes - 1;
							}
						}else{
							cur = headernodes - 1;
						}

					}
				}else if(text.startsWith("$")){
					/*if(text.toUpperCase().startsWith("$PLACE") && !ref){
						text = "$PLACE " + forcenodecount;
						forcenodecount++;
					} else*/
					//if(!text.toUpperCase().startsWith("$PLACE")
							//&& !(ref && text.toUpperCase().startsWith("$TEMP"))){
						if(text.startsWith("$FILL")){
							/*int i = 0;
							while(i < nodes){
								if(nodetype[i] == 9){
									nodeitems[i].dispose();
									nodetype[i] = -1;
								}
								i++;
							}
							text = ">0|" + text.substring(0,text.indexOf(" ", 6));*/
							setFill(text.substring(text.indexOf(" "),text.indexOf(" ", 6)));
						}else{// vert vex vx
							if(text.startsWith("$VERT")
									&& !text.startsWith("$VERTEX")){
								text.replaceAll("$VERT", "$VERTEX");
							}
							if(text.startsWith("$VEX")
									&& !text.startsWith("$VERTEX")){
								text.replaceAll("$VEX", "$VERTEX");
							}
							if(text.startsWith("$VX")
									&& !text.startsWith("$VERTEX")){
								text.replaceAll("$VX", "$VERTEX");
							}
							if(text.startsWith("$POLYGON")){
								text.replaceAll("$POLYGON", "$POLY");
							}
							if(text.startsWith("$COLOR")){
								String rest = text.substring(6);
								if(rest.startsWith("WHI")){ // handle color shortcuts
									text = "$COLOR 255 255 255";
								}else if(rest.startsWith("BLA")){
									text = "$COLOR 0 0 0";
								}else if(rest.startsWith("BLU")){
									text = "$COLOR 0 0 255";
								}else if(rest.startsWith("RED")){
									text = "$COLOR 255 0 0";
								}else if(rest.startsWith("GRE")){
									text = "$COLOR 0 255 0";
								}else if(rest.startsWith("YEL")){
									text = "$COLOR 255 255 0";
								}else if(rest.startsWith("PUR")){
									text = "$COLOR 255 0 255";
								}else if(rest.startsWith("ORA")){
									text = "$COLOR 255 125 0";
								}else if(rest.startsWith("GRA")){
									text = "$COLOR 125 125 125";
								}else if(rest.startsWith("TEA")){
									text = "$COLOR 0 255 255";
								}
							}
							if(ref){
								insertForced(refheadernodeitems[rcur], text, rcur, ref);
							}else{
								insertForced(headernodeitems[cur], text, cur, ref);
							}
						}
					//}
				}
			}
		}catch (Exception e){
			System.out.println("!!! FAILED TO LOAD FILE " + path);
			e.printStackTrace();
		}
		refreshColors();
		viewLayerTab(currentlayertabz);
		haslastsavedpath=true;
		lastsavedpath=path;
	}

	// saveFile(
	// String, the path to save the file at
	// )
	// - Used to save the existing data as a script .txt file
	static void saveFile(String path){
		File f;
		f = new File(path);
		FileWriter fw;
		try {
			fw = new FileWriter(f);
			TreeItem[] refstart = new TreeItem[1];

			refstart = reftree.getItems();
			output(refstart, fw, true,path.substring(path.lastIndexOf("\\")+1));

			TreeItem[] start = new TreeItem[1];
			start[0] = headernodeitems[0];
			output(start, fw, false,path.substring(path.lastIndexOf("\\")+1));
			// fw.write(inline + System.getProperty ("line.separator"));
			
			TreeItem[] comstart = comtree.getItems();
			output(comstart, fw, true,path.substring(path.lastIndexOf("\\")+1));

			fw.close();
		}catch (Exception e){
			System.out.println("!!! FAILED TO WRITE FILE " + path);
		}
	}

	// clearAll()
	// - Clears all nodes and restarts the data, essentially
	static void clearAll(){
		// flush the system;
		int i = 1;
		while(i < nodes){
			nodeitems[i].dispose();
			nodetype[i] = -1;
			i++;
		}
		nodes = 1;
		headernodes = 1;
		current = 0;
		current_z = 0;

		i = 0;
		while(i < refnodes){
			refnodeitems[i].dispose();
			refnodetype[i] = -1;
			i++;
		}
		refnodes = 0;
		refheadernodes = 0;
		currentref = 0;
		
		i = 0;
		while(i < comnodes){
			comnodeitems[i].dispose();
			comnodetype[i] = -1;
			i++;
		}
		comnodes = 0;
		comheadernodes = 0;
		currentcom = 0;

		autoInsertPlace(nodeitems[0], 0);
		setupNode(nodeitems[nodes - 1], 0);
		setFill("11.81");
		
		haslastsavedpath=false;
	}
	
	static void preview(){
		String outpath = System.getProperty("user.dir");
		String targetpath = System.getProperty("user.dir");
		if(outpath.contains("/")){
			outpath = outpath+"/previews/preview.txt";
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
			targetpath = targetpath + "previews/";
		}else{
			outpath=outpath+"\\previews\\preview.txt";
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
			targetpath = targetpath + "previews\\";
		}
		double lenint=11.81;
		try{
			lenint = Double.parseDouble(filllen);
		}catch(Exception e){
			
		}
		fillitem.setText(">0|$FILL " + (lenint*previewfillfactor));
		saveFile(outpath);
		fillitem.setText(">0|$FILL " + (lenint));
		String targ = Interpreter.getTarget();
		Interpreter.setPreviewMode(true);
		Interpreter.setTarget(targetpath);
		Interpreter.digest(outpath);
		previewnodes = Interpreter.getNodes();
		previewnodecount = Interpreter.getNodeCount();
		previewmapboss = Interpreter.getMapBoss();
		previewboss.setup(previewmapboss.getLayers());
		int[] ltoz = Interpreter.getLayerFromZList(previewmapboss.getLayers());
		Interpreter.setPreviewMode(false);
		Interpreter.flush();
		Interpreter.setTarget(targ);
		
		int nid=0;
		while(nid<previewmapboss.getLayers()){
			try{
				String prevpath=  targetpath + " " + (lenint*previewfillfactor) + " preview.txt_" + ltoz[nid] + ".png";
				Image previmg = new Image(display, prevpath);
				//DevicePreview prevdialog = new DevicePreview(shell,previmg,layer + " Preview",layer);
				previewboss.setLayer(shell,previmg,prevpath,ltoz[nid] + " Preview",nid,ltoz[nid]);
			}catch(Exception e){

			}
			nid++;
		}
		
		/*int nid = 0;
		int[] layerlist = new int[headernodes];
		int layerlistcount = 0;
		while(nid<headernodes){
			int layer = headernode[nid].getFillZ();
			int i=0;
			boolean repeat = false;
			while(i<layerlistcount){
				if(layer==layerlist[i]){
					repeat=true;
					break;
				}
				i++;
			}
			if(!repeat){
				try{
					String prevpath=  targetpath + " " + (lenint*previewfillfactor) + " preview.txt_" + layer + ".png";
					Image previmg = new Image(display, prevpath);
					//DevicePreview prevdialog = new DevicePreview(shell,previmg,layer + " Preview",layer);
					previewboss.setLayer(shell,previmg,prevpath,layer + " Preview",layerlistcount,layer);
					layerlist[layerlistcount]=layer;
					layerlistcount++;
				}catch(Exception e){
	
				}
			}
			nid++;
		}*/
		
		previewing=true;
	}
	
	static void setPreviewSelectionColor(int ir, int ig, int ib){
		prevselcol[0]=ir; prevselcol[1]=ig; prevselcol[2]=ib;
		savePreferences();
	}
	
	static int findPreviewNodeID(double x, double y, int z){
		if(!previewing || previewnodecount<=0){
			return -1; // cannot search through nodes that don't exist
		}
		int node = previewmapboss.getNodeAt((int)x, (int)y, z);
		return node;
	}
	
	static int findHeaderNodeFromMapNodeID(int inid){
		// returns the headernodeid that contains the corresponding $PLACE function
		int placeid = previewmapboss.getNode(inid).getPlaceID();
		int headernodeid = getHeaderNodeIdFromPlace(placeid);
		return headernodeid;
	}
	
	static Image getPreviewSelectionImage(int inz, int innode){
		String outpath = System.getProperty("user.dir");
		String targetpath = System.getProperty("user.dir");
		if(outpath.contains("/")){
			outpath = outpath+"/previews/preview.txt";
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
			targetpath = targetpath + "previews/";
		}else{
			outpath=outpath+"\\previews\\preview.txt";
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
			targetpath = targetpath + "previews\\";
		}
		double lenint=11.81;
		try{
			lenint = Double.parseDouble(filllen);
		}catch(Exception e){
			
		}
		previewmapboss.makeSelectionMap(inz, innode, prevselcol);
		Image img = null;
		try{
			img = new Image(display, targetpath + " " + (lenint*previewfillfactor) + " preview.txt_" + inz + "_prevselmap.png");
		}catch(Exception e){
			e.printStackTrace();
		}
		return img;
	}
	
	static void compile(boolean as){
		String outpath = lastsavedpath;
		if(!haslastsavedpath || as){
			FileDialog filedialog = new FileDialog(shell, SWT.SAVE);
			filedialog.setFilterPath("./");
			// where does the file searcher start? (./ is the folder the  program is in)
			filedialog.setText("Save Script"); // title of the file dialog
			String[] fe = new String[2]; // possible file types
			fe[0] = "*.txt";
			fe[1] = "*.*";
			filedialog.setFilterExtensions(fe); // set file types
			outpath = filedialog.open(); // grab the path that is gotten from the filedialog
			if(outpath!=null){
				lastsavedpath=outpath;
				haslastsavedpath=true;
			}
		}
		if(outpath==null){
			return;
		}
		
		/*String outpath = System.getProperty("user.dir");
		String targetpath = System.getProperty("user.dir");
		if(outpath.contains("/")){
			outpath = outpath+"/preview.txt";
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
		}else{
			outpath=outpath+"\\preview.txt";
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
		}*/
		
		saveFile(outpath);

		//String targ = Interpreter.getTarget();
		//Interpreter.setTarget(targetpath);
		Interpreter.digest(outpath);
		Interpreter.flush();
		//Interpreter.setTarget(targ);
	}
	
	static void actionHook(boolean prev){
		// set up undo history
		if(!undoing){
			undocurrent++;
			if(undocurrent>undomax){
				undocurrent=0;
			}
			System.out.println("ACTED :::  UNDO CURRENT " + undocurrent);
			saveFile("./backups/BACKUP_" + undocurrent + ".txt");
		}
		// this method is admittedly primitive and unoptimized but it works 
		// without error and without the introduction of much, much more code.
		// it would be better for performance to create a system that only needs
		// to hold on to a list of changes and can undo them rather than this 
		// state system.
		
		
		// call preview to update the preview windows
		if(prev && previewauto){
			preview();
		}
	}
	

	
	static void undo(){
		undocurrent--;
		if(undocurrent<0){
			undocurrent=undomax;
		}
		undoing=true;
		System.out.println("UNDID :::  UNDO CURRENT " + undocurrent);
		String outpath = System.getProperty("user.dir");
		String targetpath = System.getProperty("user.dir");
		if(outpath.contains("/")){
			outpath = outpath+"/backups/BACKUP_" + undocurrent + ".txt";
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
			targetpath = targetpath + "backups/";
		}else{
			outpath=outpath+"\\backups\\BACKUP_" + undocurrent + ".txt";
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
			targetpath = targetpath + "backups\\";
		}
		
		
		openFile(targetpath + "BACKUP_" + undocurrent + ".txt");
		undoing=false;
		preview();
	}
	static void redo(){
		undocurrent++;
		if(undocurrent>undomax){
			undocurrent=0;
		}
		undoing=true;
		String outpath = System.getProperty("user.dir");
		String targetpath = System.getProperty("user.dir");
		if(outpath.contains("/")){
			outpath = outpath+"/backups/BACKUP_" + undocurrent + ".txt";
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
			targetpath = targetpath + "backups/";
		}else{
			outpath=outpath+"\\backups\\BACKUP_" + undocurrent + ".txt";
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
			targetpath = targetpath + "backups\\";
		}
		
		
		openFile(targetpath + "BACKUP_" + undocurrent + ".txt");
		undoing=false;
		preview();
	}
	
	
	static void loadPreferences(){
		String outpath = System.getProperty("user.dir");
		String targetpath = System.getProperty("user.dir");
		if(outpath.contains("/")){
			outpath = outpath+"/config.txt";
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
		}else{
			outpath=outpath+"\\config.txt";
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
		}
		
		File f = new File(outpath);
		if(!f.exists()){
			return;
		}
		
		FileReader fis;
		BufferedReader bis;
		
		int line = 0;

		try {
			fis = new FileReader(f);
			bis = new BufferedReader(fis);
			while(bis.ready()){
				String text = bis.readLine();
				text = text.trim();
				int num = 0;
				try{
					num = Integer.parseInt(text);
				}catch(Exception e){
					
				}
				if(line==0){
					prevselcol[0]=num;
				}else if(line==1){
					prevselcol[1]=num;
				}else if(line==2){
					prevselcol[2]=num;
				}else if(line==3){
					if(num==1){
						colorbyz=true;
					}else{
						colorbyz=false;
					}
				}else if(line==4){
					colseed=num;
				}else if(line==5){
					if(num==1){
						previewauto=true;
					}else{
						previewauto=false;
					}
				}else if(line==6){
					try{
						double dbl = Double.parseDouble(text);
						previewfillfactor=dbl;
					}catch(Exception e){
						
					}
				}else if(line==7){
					polydrawx=num;
				}else if(line==8){
					polydrawy=num;
				}
				
				line++;
			}
		}catch (Exception e){
			System.out.println("!!! FAILED TO LOAD FILE " + outpath);
			e.printStackTrace();
		}
		setColorByZ(colorbyz);
		setColorSeed(colseed);
	}
	
	static void savePreferences(){
		String outpath = System.getProperty("user.dir");
		String targetpath = System.getProperty("user.dir");
		if(outpath.contains("/")){
			outpath = outpath+"/config.txt";
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
		}else{
			outpath=outpath+"\\config.txt";
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
		}
		File f;
		f = new File(outpath);
		FileWriter fw;
		try {
			fw = new FileWriter(f);
			
			fw.write("" + prevselcol[0] + "\n");
			fw.write("" + prevselcol[1] + "\n");
			fw.write("" + prevselcol[2] + "\n");
			if(colorbyz){
				fw.write("1\n");
			}else{
				fw.write("0\n");
			}
			fw.write("" + colseed + "\n");
			if(previewauto){
				fw.write("1\n");
			}else{
				fw.write("0\n");
			}
			fw.write("" + previewfillfactor + "\n");
			fw.write("" + polydrawx + "\n");
			fw.write("" + polydrawy + "\n");
			
			fw.close();
		}catch (Exception e){
			System.out.println("!!! FAILED TO WRITE FILE " + outpath);
		}
	}
	
	static void setColorByZ(Boolean inb){
		colorbyz=inb;
		if(viewcolorbyzitem!=null){
			viewcolorbyzitem.setSelection(colorbyz);
		}
		if(prefwindow!=null){
			if(!prefwindow.isDisposed()){
				prefwindow.receiveColorByZ(inb);
			}
		}
		refreshColors();
		savePreferences();
	}
	
	static void setColorSeed(int inseed){
		colseed=inseed;
		
		double realseed = colseed;
		int afac = 10;
		int bfac = 15;
		int cfac = 20;
		int dfac = 35;
		double efac = 300;
		double ffac = 150;
		double gfac= 75;
		realseed=Math.abs(realseed);
		while(realseed>1){
			realseed/=10;
			afac++;
			bfac--;
			cfac-=2;
			dfac+=2;
			if(realseed>=100 && realseed<1000){
				efac=realseed;
			}else if(realseed>=1000 && realseed<10000){
				ffac=Math.sqrt(realseed);
			}else if(realseed>=10000 && realseed<100000){
				gfac=Math.sqrt(realseed)/10;
			}else if(realseed>=100000){
				afac*=2;
				bfac/=2;
				cfac*=2;
				dfac/=2;
			}
		}
		
		colseed1 = (int) (realseed * efac) + afac;
		colseed2 = 2 * ((int) (realseed * ffac) - colseed1) + bfac;
		colseed3 = 3 * ((int) (realseed * gfac) + colseed2) + cfac;
		colseed4 = (int) (realseed * 300) - colseed3 + dfac;
		
		if(prefwindow!=null){
			if(!prefwindow.isDisposed()){
				prefwindow.receiveColorSeed(colseed);
			}
		}
		
		refreshColors();
		savePreferences();
	}
	
	static void setPreviewAuto(Boolean inb){
		previewauto=inb;
		if(prefwindow!=null){
			if(!prefwindow.isDisposed()){
				prefwindow.receivePreviewAuto(previewauto);
			}
		}
		savePreferences();
	}
	
	static void setPreviewFillFactor(double inf){
		previewfillfactor=inf;
		if(prefwindow!=null){
			if(!prefwindow.isDisposed()){
				prefwindow.receivePreviewFillFactor(previewfillfactor);
			}
		}
		savePreferences();
		preview();
	}
	
	static void setPolyDrawSize(int inx, int iny){
		polydrawx=inx;
		polydrawy=iny;
		if(prefwindow!=null){
			if(!prefwindow.isDisposed()){
				prefwindow.receivePolyDrawSize(inx,iny);
			}
		}
		savePreferences();
	}
	
	static String makeColorPreview(int x, int w, int h){
		String outpath = System.getProperty("user.dir");
		String targetpath = System.getProperty("user.dir");
		if(outpath.contains("/")){
			if(!targetpath.endsWith("/")){
				targetpath = targetpath + "/";
			}
			targetpath = targetpath + "previews/";
		}else{
			if(!targetpath.endsWith("\\")){
				targetpath = targetpath + "\\";
			}
			targetpath = targetpath + "previews\\";
		}
		BufferedImage img = new BufferedImage(x*w,h,BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = img.getRaster();
		int i=0;
		while(i<x){
			int cr = (int) (210 + 45 * Math.sin(i * colseed1));
			int cg = (int) (210 + 45 * Math.cos(i * colseed2));
			int cb = (int) (200 + 25 * Math.sin(i * colseed3) - 20 * Math
				.cos(i * colseed4));
			int[] col = new int[3];
			col[0]=cr; col[1]=cg; col[2]=cb;
			int o=0;
			while(o<h){
				int a = 0;
				while(a<w){
					raster.setPixel(i*w+a, o, col);
					a++;
				}
				o++;
			}
			i++;
		}
		try{
			ImageIO.write(img, "PNG", new File(targetpath + "colorpreview.png"));
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		return targetpath + "colorpreview.png";
	}
	
	static void openPolyDraw(){
		if(polydrawwindow!=null){
			if(!polydrawwindow.isDisposed()){
				return;
			}
		}
		polydrawwindow = new PolyDrawWindow(shell,polydrawx,polydrawy,fontui);
	}
}
