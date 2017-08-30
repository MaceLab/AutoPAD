package autopad;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class Interpreter {

	private static int nodesize = 1248;
	
	private static MapNode[] placemap = new MapNode[nodesize]; // stores mapnodes based on their placeids. 
	// only stores mapnodes that have placeids.
	private static int places = 1; // enumerates the number of filled slots in the above list.
	
	private static MapNode[] tempmap = new MapNode[nodesize];  // stores mapnodes temporarily based on their tempids
	// will be overwritten by further temp declarations as the script is interpreted.
	//private static int temps = 0;
	
	private static MapNode[] allnodes = new MapNode[nodesize]; // stores each and every mapnode. Each mapnode is a header node
	// containing properties that are stored in the mapnode class.
	private static int nodecount=1; // final tally of each and every node in the device
	
	private static MapBoss mapboss = new MapBoss(); // keeps track of where each node in the device is located on 
	// generated previews, used by the devicedrawinterface to manage selection outlines of nodes.
	
	// used to determine the minimum dimensions of the device depending on the furthest positions in each direction
	// that are occupied by nodes
	private static double furthest_pos_x =0; // 
	//private static MapNode furthest_pos_x_node;
	private static double furthest_neg_x =0;
	//private static MapNode furthest_neg_x_node;
	private static double furthest_pos_y =0;
	//private static MapNode furthest_pos_y_node;
	private static double furthest_neg_y =0;
	//private static MapNode furthest_neg_y_node;
	
	private static boolean debug = true; // some messages are only displayed if debug is true
	private static boolean olddebug=true; // keeps track when debug needs to be temporarily disabled, what the old value was

	private static MapNode[] previous_nodes = new MapNode[64]; // keeps track of nodes for the BACK command
	private static int previous_node_slot=0; // iterator in the above list
	
	private static double[] temp_vars = new double[64]; // stores variables created by the VAR commands
	
	private static boolean buffer_box = false; // whether or not to fill the entire device with a black buffer box
	private static double buffer_box_x = 0; // dimensions of the box
	private static double buffer_box_y = 0;
	
	private static String fillname = "out"; // what to title the output files
	private static double fillsize = 0; // scale size of the output files (script units to pixels)
	private static double scalechange = 1.0; // scale by which to modify all input dimensions (i.e. Circle 0.1 with (scalechange=10) -> Circle 1.0)
	
	private static boolean cut_overlap = false; // whether to allow overlapping shapes to remain in cut images
	private static int cut_scale = 1; // amount of pixels to leave in a cut outline
	
	private static boolean autospace=false; // depreciated; used to set each rectangle to have a small negative spacing
	
	private static boolean finalized=false; // whether the dimensions of the device have been finalized
	
	private static CombineMap[] combines = new CombineMap[64]; // stores the combine map data 
	private static boolean combineon = false; // whether or not to process combines
	private static int current_combine = 0; // which combine is being built currently
	private static int previous_comnode_slot=0; // keeps track of BACK command for combines only
	private static int[] previous_comnodes = new int[64]; // see above
	private static int comtemps = 0; // keeps track of TEMP command for combines only
	private static int[] comtempmap = new int[nodesize]; // see above
	
	private static String targetpath = "./out/"; // where to output files
	
	// old angles are used to redeploy the depreciated system 
	// where angles went from 0 = left counterclockwise
	private static boolean old_angles = false;
	
	// enabling exact_corner_buffer leads to corner pieces using triangular buffers.
	// This is disabled by default because the rectangular buffers work better and look better.
	private static boolean exact_corner_buffer=false;
	
	
	// PDF settings
	private static boolean dopdf = true; // if on, each image is compiled into a pdf
	private static int pdfpagestyle = -1; // -1 = letter, 0-6 = A0-A6, -2 = custom
	private static int pdfmarginx = 36; // by default, half an inch page margins
	private static int pdfmarginy = 36; // recall that 72px = 1in so 36px = 0.5in
	private static int pdfforcesizex = 0; // if pagestyle is -2, uses a custom page of this size
	private static int pdfforcesizey = 0; 
	private static double pdfscale = 1.0/4.167;
	
	private static int[] replaceblack = new int[]{35,31,32};
	
	
	private static boolean previewmode=false; // in preview mode (activated by devicedrawinterface), some features are
	// disabled for the sake of generation speed
	
	
	// flush() is used to clear everything out and prepare the interpreter for a new script.
	public static void flush(){
		placemap = new MapNode[nodesize];
		places = 1; // doesn't start at 0, because a node with place 0 must always exist as the origin
		
		tempmap = new MapNode[nodesize];
		//private static int temps = 0;
		
		allnodes = new MapNode[nodesize];
		nodecount=1;
		
		furthest_pos_x =0;
		furthest_neg_x =0;
		furthest_pos_y =0;
		furthest_neg_y =0;

		previous_nodes = new MapNode[64];
		previous_node_slot=0;
		
		temp_vars = new double[64];
		
		buffer_box = false;
		buffer_box_x = 0;
		buffer_box_y = 0;
		
		cut_overlap = false;
		
		autospace=false; 
		
		finalized=false;
		
		combines = new CombineMap[64];
		combineon = false;
		current_combine = 0;
		previous_comnode_slot=0;
		previous_comnodes = new int[64];
		comtemps = 0;
		comtempmap = new int[nodesize];
		
		old_angles=false;
		
		mapboss = new MapBoss();
		
		scalechange=1.0;
	}
	
	// setTarget() is used to set the 'target path', which is the directory location where files will be output
	public static void setTarget(String newpath){
		targetpath=newpath;
	}
	// getTarget() simply gives the targetpath
	public static String getTarget(){
		return targetpath;
	}
	// getNodes() returns a list of all nodes in the device (i.e. allnodes[])
	public static MapNode[] getNodes(){
		// should only be called after finalized
		// however not making this a hard rule since it won't break anything
		// to call it before then
		if(!finalized){
			System.out.println("WARNING: getting nodes before they are finalized");
		}
		return allnodes;
	}
	// getNodeCount() simply gives the nodecount (of allnodes)
	public static int getNodeCount(){
		return nodecount;
	}
	// getMapBoss() gives the MapBoss object that has been created by device generation.
	// should not be called prior to finalization.
	public static MapBoss getMapBoss(){
		if(!finalized){
			System.out.println("WARNING: getting mapboss before nodes are finalized");
		}
		return mapboss;
	}
	
	// digest() is the interface between script and interpreter. Takes in a pathname to a txt file and then reads that txt file
	// generating out all of the mapnode data as it goes. Digest should be called first, or else there will be no data to 
	// generate images from.
	public static void digest(String pathname){
		// load a grammar via txt file and digest it into a device
		placemap[0] = new MapNode(0,0); // point 0 is always the center point.
		allnodes[0]=placemap[0];
		nodecount=1;
		places=1;
		
		previous_nodes = new MapNode[64]; // most likely, these have already been cleared up by a flush() 
		previous_node_slot=0;			  // but, just in case, the previous list should be cleared up
		
		// objects for reading the text file specified by pathname
		// the ordering is: bis(fis(f))
		// then, only bis needs to be accessed. 
		File f;
		FileReader fis;
		BufferedReader bis;
		
		// the following are used for loading reference files 
		// up to 16 references may be nested within each other by these means.
		// 16 is an arbitrary cutoff.
		int use_fref=0; // keeps track of what level of reference nesting the digestion is currently at
			// for instance, at 0 there are no references being read, at 3 there are 3 layers of references being read
		String[] frefnames = new String[16];
		File[] fref = new File[16]; // if a reference file is loaded
		FileReader[] fisref = new FileReader[16]; // whether the program will actually handle 16 open files at once is
		BufferedReader[] bisref = new BufferedReader[16]; // questionable
		double[] angle_change= new double[16]; // difference in angle, used when loading a reference
		int[] refrepeats=new int[16];
		
		int line=0;
		int current_z=0;
		//boolean combineon=false;
		int combinecur=0;
		int combineid=0;
		MapNode current = placemap[0];
		
		f = new File(pathname);
		try{
			fis = new FileReader(f);
			bis = new BufferedReader(fis);	
			bisref[0]=bis; // these definitions are only to prevent them from being uninitialized
			fref[0]=f;	  // they will never actually be accessed in this form.
			while(bis.ready()){
				line++;
				String text = "  ";
				if(use_fref>0){ // if a reference is active, read that instead of the main file.
					try{
						text=bisref[use_fref].readLine();
						if(!bisref[use_fref].ready()){
							refrepeats[use_fref]--; 
							if(refrepeats[use_fref]<=0){
								use_fref--; // once the file ends and the command is out of repeats, move down a layer of nesting
								angle_change[use_fref+1]=0;
							}else{
								// if repeats are still active, reload the same reference
								System.out.println(" LOADING [" + frefnames[use_fref] + "]");
								fref[use_fref] = new File("./in/ref/ref" + frefnames[use_fref] + ".txt");
								try{
									fisref[use_fref]=new FileReader(fref[use_fref]);
									bisref[use_fref]=new BufferedReader(fisref[use_fref]);
								}catch(Exception e){
									use_fref--;
									angle_change[use_fref+1]=0;
									System.out.println("!!! ERROR READING REFERENCE FILE" + fref[use_fref].getPath());
								}
							}
						}
					}catch(Exception e){
						System.out.println("!!! ERROR READING REFERENCE FILE" + fref[use_fref].getPath());
						use_fref--;
						angle_change[use_fref+1]=0;
						text=bis.readLine();
					}
				}else{
					text = bis.readLine(); // if references are inactive, read from the main file
				}
				text=text.trim(); // remove leading and trailing whitespace from the text
				if(debug){
					System.out.println("& << " + text);
				}
				
				// now the line is actually digested:
				int[][] regout = regex(text,"\"",1);
				if(regout[0][0]==-1){ // if the line begins with a quote, it is not parsed
					regout = regex(text,"\\>",1);
					// > indicates a header node
					if(regout[0][0]!=-1){
						// if a > is found, that means this line needs to be parsed as a header node
						String rest = text.substring(1); // cut off the >
						double[] values = parseValues(" " + rest,1,false); // attempt to parse the rest of the line for numbers
						char first = rest.charAt(0); // backup check to be sure that this is a numerical value
						if(values[0]!=-1 && Character.isDigit(first)){
							// it's a number, search for the relevant placemap point
							values = parseValues(" " + rest,1,false);
							int out = (int)(values[0]);
							if(out>=10000){ // shortcut: placeids over 10000 correspond to absolute node ids rather than placeids
								// for instance, 10001 gives the first node after the origin node (10000-10001 = 1)
								// rarely useful, but available for those occasions. 
								out-=10000;
								current = allnodes[out];
							}else{
								if(out>=nodesize || out<0){out=0;}
								MapNode p = placemap[out];
								current=p;
							}
							current_z = current.getFillZ();
							previous_nodes[previous_node_slot]=current; // add this header node to the previous list
							previous_node_slot++;
							if(previous_node_slot>=64){
								previous_node_slot=0;
							}	
						}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("BACK")){
							// if it's not a number assignment, start checking the other options
							// back accesses the previous node list, i.e. it goes BACK to whatever the last node was
							// or whatever the nth last node was.
							values = parseValues(rest,1,false);
							int goback = (int)(values[0])+1; // this way, BACK 1 is the node before this one and 
							int spot = previous_node_slot; // BACK 0 is this node
							while(goback>0){
								spot--;
								goback--;
								if(spot<0){
									spot=63;
								}
							}
							current=previous_nodes[spot];
							current_z = current.getFillZ();
							previous_nodes[previous_node_slot]=current;
							previous_node_slot++; 
							if(previous_node_slot>=64){
								previous_node_slot=0;
							}	
						}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("TEMP")){
							// TEMP accesses the tempid list, functions almost exactly as ># and the placeid system
							// except that tempids are made to be thrown away and overwritten as is convenient
							// whereas a placeid, once set, should never be set to any other node.
							values = parseValues(rest,1,false);
							int tempnum = (int)(values[0]);
							if(tempmap[tempnum]!=null){
								current = tempmap[tempnum];
								current_z = current.getFillZ();
								previous_nodes[previous_node_slot]=current;
								previous_node_slot++;
								if(previous_node_slot>=64){
									previous_node_slot=0;
								}	
							}
						}else if(carefulSubstring(rest,0,1).equalsIgnoreCase("Z") || carefulSubstring(rest,0,5).equalsIgnoreCase("LAYER")){
							// >Z or >LAYER allow for a new node to be made on a specific layer of the device.
							values = parseValues(rest,1,false);
							current_z = (int)(values[0]);
							MapNode p = new MapNode(nodecount,current_z);
							allnodes[nodecount]=p;
							p.setPlaceID(nodecount+10000);
							nodecount++;
							double sangle = current.getNeighborAngle(0);
							double bangle = sangle; // angle opposite of sangle
							bangle+=180; // some angle arrangement
							// this is so the new node continues in the same direction as it's source
							// this is done by looking at it's source's source: if it's source is A and it's source's source is B,
							// if A->B is 90deg, that means B->A is the opposite of that: 270deg. The new node should continue 
							// as B->A does, so that the three form a line.
							if(bangle>=360){
								bangle-=360;
							}		
							p.setNeighbor(current,sangle,0,current_z);
							current.setNeighbor(p,bangle,0,p.getFillZ());
							current=p;
							previous_nodes[previous_node_slot]=current;
							previous_node_slot++;
							if(previous_node_slot>=64){
								previous_node_slot=0;
							}	
						}else{
							// must be a direction
							double sangle = 0;//search angle
							// there are 4 directional shortcuts: LEFT RIGHT UP and DOWN
							if(rest.equalsIgnoreCase("LEFT")){
								sangle=0;
							}else if(rest.equalsIgnoreCase("RIGHT")){
								sangle=180;
							}else if(rest.equalsIgnoreCase("UP")){
								sangle=90;
							}else if(rest.equalsIgnoreCase("DOWN")){
								sangle=270;
							}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("ANGLE")){ // but more often ANGLE is simply used
								values = parseValues(rest,1,false);
								sangle=values[0];
								if(!old_angles){
									sangle=180-sangle; // this conversion is needed to make the angles behave as expected:
									// 0deg points to the right, 90deg points up, etc.
								}
							}
							
							// all angles of references must be adjusted by the adjustment factor given when the
							// reference was called, for instance if a reference is given an angle of 45, all angles
							// within will have 45 deg added.
							int r = use_fref;
							while(r>=0){
								sangle+=angle_change[r];
								r--;
							}
							while(sangle>360){ sangle-=360; }
							while(sangle<0){ sangle+=360; }
							
							double bangle = sangle; // angle opposite of sangle
							bangle+=180;
							if(bangle>=360){
								bangle-=360;
							}
							// finally: produce the new node
							MapNode p = new MapNode(nodecount,current_z);
							allnodes[nodecount]=p;
							p.setPlaceID(nodecount+10000);
							nodecount++;
							p.setNeighbor(current,sangle,0,current_z);
							current.setNeighbor(p,bangle,0,current_z);
							current=p;
							current_z = current.getFillZ();
							previous_nodes[previous_node_slot]=current;
							previous_node_slot++;
							if(previous_node_slot>=64){
								previous_node_slot=0;
							}	
						}
					}else {
						// not a > node, so start checking the other varieties
						regout = regex(text,"\\$",1);
						if(regout[0][0]!=-1){
							// $ marks a command
							String rest = text.substring(regout[0][0]+1); 
							if(carefulSubstring(rest,0,10).equalsIgnoreCase("OLD_ANGLES")){
								old_angles=!old_angles;
							}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("PLACE") && places<nodesize){
								// defines a placeid for this headernode
								double[] valout = parseValues(rest.substring(5),1,false);
								int at = (int)(valout[0]);
								placemap[at]=current;
								current.setPlaceID(at);
								places++;
							}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("COLOR")){
								// sets the color of this node (e.g. changes the color of the circles, rectangles therein)
								double[] valout = parseValues(rest.substring(5),3,false);
								if(valout[0]==-1){
									// a number of shortcuts are included and must be checked if
									// the parsevalues does not return a valid value (-1)
									String colshortcut = carefulSubstring(rest, 6, 9);
									String fullshortcut = rest.substring(6);
									if(colshortcut.equalsIgnoreCase("WHI") || fullshortcut.equalsIgnoreCase("WHITE")){
										current.setColor(255,255,255);
									}else if(colshortcut.equalsIgnoreCase("BLA") || fullshortcut.equalsIgnoreCase("BLACK")){
										current.setColor(35,32,32);
									}else if(colshortcut.equalsIgnoreCase("BLU") || fullshortcut.equalsIgnoreCase("BLUE")){
										current.setColor(0,0,255);
									}else if(colshortcut.equalsIgnoreCase("RED") || fullshortcut.equalsIgnoreCase("RED")){
										current.setColor(255,0,0);
									}else if(colshortcut.equalsIgnoreCase("GRE") || fullshortcut.equalsIgnoreCase("GREEN")){
										current.setColor(0,255,0);
									}else if(colshortcut.equalsIgnoreCase("ORA") || fullshortcut.equalsIgnoreCase("ORANGE")){
										current.setColor(255,125,0);
									}else if(colshortcut.equalsIgnoreCase("PUR") || fullshortcut.equalsIgnoreCase("PURPLE")){
										current.setColor(255,0,255);
									}else if(colshortcut.equalsIgnoreCase("YEL") || fullshortcut.equalsIgnoreCase("YELLOW")){
										current.setColor(255,255,0);
									}else if(colshortcut.equalsIgnoreCase("GRA") || fullshortcut.equalsIgnoreCase("GRAY")){
										current.setColor(125,125,125);
									}else if(colshortcut.equalsIgnoreCase("TEA") || fullshortcut.equalsIgnoreCase("TEAL")){
										current.setColor(0,255,255);
									}else{
										// if no shortcut is used, attempt to set the color from the values given.
										current.setColor((int)(valout[0]),(int)(valout[1]),(int)(valout[2]));
									}
								}else{
									// if a valid value is found, set the color to the values given
									if(valout[0]<=1 && valout[1]<=1 && valout[2]<=1 && valout[0]>=0 && valout[1]>=0 && valout[2]>=0){
										// if each of the values is below 1.0, assume that a 0.0-1.0 double color scale is
										// being used instead of the 0-255 integer scale,
										// and convert to the 0-255 integer scale.
										valout[0]*=255;
										valout[1]*=255;
										valout[2]*=255;
									}
									current.setColor((int)(valout[0]),(int)(valout[1]),(int)(valout[2]));
								}
							}else if(carefulSubstring(rest,0,10).equalsIgnoreCase("BLACKCOLOR")){
								// sets the color of this node (e.g. changes the color of the circles, rectangles therein)
								double[] valout = parseValues(rest.substring(10),3,false);
								if(valout[0]==-1){
									// a number of shortcuts are included and must be checked if
									// the parsevalues does not return a valid value (-1)
									String colshortcut = carefulSubstring(rest, 11, 14);
									String fullshortcut = rest.substring(11);
									if(colshortcut.equalsIgnoreCase("WHI") || fullshortcut.equalsIgnoreCase("WHITE")){
										replaceblack=new int[]{255,255,255};
									}else if(colshortcut.equalsIgnoreCase("BLA") || fullshortcut.equalsIgnoreCase("BLACK")){
										replaceblack=new int[]{35,32,32};
									}else if(colshortcut.equalsIgnoreCase("BLU") || fullshortcut.equalsIgnoreCase("BLUE")){
										replaceblack=new int[]{0,0,255};
									}else if(colshortcut.equalsIgnoreCase("RED") || fullshortcut.equalsIgnoreCase("RED")){
										replaceblack=new int[]{255,0,0};
									}else if(colshortcut.equalsIgnoreCase("GRE") || fullshortcut.equalsIgnoreCase("GREEN")){
										replaceblack=new int[]{0,255,0};
									}else if(colshortcut.equalsIgnoreCase("ORA") || fullshortcut.equalsIgnoreCase("ORANGE")){
										replaceblack=new int[]{255,125,0};
									}else if(colshortcut.equalsIgnoreCase("PUR") || fullshortcut.equalsIgnoreCase("PURPLE")){
										replaceblack=new int[]{255,0,255};
									}else if(colshortcut.equalsIgnoreCase("YEL") || fullshortcut.equalsIgnoreCase("YELLOW")){
										replaceblack=new int[]{255,255,0};
									}else if(colshortcut.equalsIgnoreCase("GRA") || fullshortcut.equalsIgnoreCase("GRAY")){
										replaceblack=new int[]{125,125,125};
									}else if(colshortcut.equalsIgnoreCase("TEA") || fullshortcut.equalsIgnoreCase("TEAL")){
										replaceblack=new int[]{0,255,255};
									}else{
										// if no shortcut is used, attempt to set the color from the values given.
										replaceblack=new int[]{(int)(valout[0]),(int)(valout[1]),(int)(valout[2])};
									}
								}else{
									// if a valid value is found, set the color to the values given
									if(valout[0]<=1 && valout[1]<=1 && valout[2]<=1 && valout[0]>=0 && valout[1]>=0 && valout[2]>=0){
										// if each of the values is below 1.0, assume that a 0.0-1.0 double color scale is
										// being used instead of the 0-255 integer scale,
										// and convert to the 0-255 integer scale.
										valout[0]*=255;
										valout[1]*=255;
										valout[2]*=255;
									}
									replaceblack=new int[]{(int)(valout[0]),(int)(valout[1]),(int)(valout[2])};
								}
							}else if(carefulSubstring(rest,0,6).equalsIgnoreCase("CIRCLE") || carefulSubstring(rest,0,4).equalsIgnoreCase("DISK")){
								// simply sets the node to be a circle. Takes a radius and optionally an inner radius
								double[] valout = parseValues(rest.substring(6),2,false);
								current.setCircle(valout[0]);
								if(valout[1]!=-1){ // if the second value is not invalid, set the inner radius as well
									current.setCircleInner(valout[1]);
								}
							}else if(carefulSubstring(rest,0,12).equalsIgnoreCase("CIRCLE_INNER")){
								// sets just the inner radius of an existing circle (does nothing without also calling $CIRCLE)
								double[] valout = parseValues(rest.substring(12),1,false);
								current.setCircleInner(valout[1]);
							}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("RECT")){
								// simply sets the node to be a rectangle. Takes length and width in that order, if no width
								// is given, assumes a width equal to the length (i.e. a square)
								double[] valout = parseValues(rest.substring(4),2,false);
								if(valout[1]!=-1){
									current.setRect(valout[0],valout[1]);
								}else{
									current.setRect(valout[0],valout[0]);
								}
							}else if(carefulSubstring(rest,0,12).equalsIgnoreCase("CORNER_CURVE")){
								// used to determine how much curve the corner has to it. Default is 0, a straight line.
								// 1.0 is a full curve. Does nothing without $CORNER being set.
								double[] valout = parseValues(rest.substring(12),1,false);
								current.setCornerCurveweight(valout[0]);
							}else if(carefulSubstring(rest,0,6).equalsIgnoreCase("CORNER")){
								// sets the node to be a corner. Takes in a base, a length (note: not the height), and the angle
								// between those two sides. If the angle given is negative, flips which side the length refers to.
								double[] valout = parseValues(rest.substring(6),3,false);
								current.setCorner(valout[0],valout[1],valout[2]);
							}else if(carefulSubstring(rest,0,6).equalsIgnoreCase("BUFFER") && !carefulSubstring(rest,0,10).equalsIgnoreCase("BUFFER_BOX")){
								// sets the buffer spacing of the current node. 
								double[] valout = parseValues(rest.substring(6),2,false);
								if(valout[1]!=-1){
									current.setBuffer(valout[0],valout[1]);
								}else{ // if no second value is given, assumes the width is the same as the length.
									current.setBuffer(valout[0],valout[0]);
								}
								if(current.isPolygon()){ // this node is a polygon, sets the buffer at each vertex
									current.setPolygonBuffer(valout[0]);
								}
							}else if(carefulSubstring(rest,0,11).equalsIgnoreCase("CUT_OVERLAP")){
								// global command, determines whether or not overlapping shapes should be removed from cut maps
								// if cut_overlap is true, then overlap is allowable. If cut_overlap is false, overlap is removed.
								cut_overlap=true;
							}else if(carefulSubstring(rest,0,9).equalsIgnoreCase("CUT_SCALE")){
								double[] valout = parseValues(rest.substring(9),1,false);
								cut_scale=(int)(valout[0]);
							}else if(carefulSubstring(rest,0,3).equalsIgnoreCase("CUT") && !carefulSubstring(rest,0,11).equalsIgnoreCase("CUT_OVERLAP")){
								// sets this node to be included on the cutmap 
								// cutmaps are images of just the outlines of the device.
								current.setCut();
							}else if(carefulSubstring(rest,0,7).equalsIgnoreCase("PRECISE")){
								// sets this node to be precise; precise makes circles and rectangles align more exactly
								// so there is no overlap.
								current.setPrecise();
							}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("EXTRA")){
								// give extra spacing on each end of this node. Useful for making the device larger than its 
								// minimum dimensions, if such a thing is desired.
								double[] valout = parseValues(rest.substring(5),2,false);
								if(valout[1]!=-1){
									current.setExtra(valout[0],valout[1]);
								}else{ // as before, if no second value is specified, use hte first value for both
									current.setExtra(valout[0],valout[0]);
								}
								if(current.isPolygon()){ // similarly to buffer, set the extra of each polygon vertex
									current.setPolygonExtra(valout[0]);
								}
							}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("SPACE")){
								// sets extra space of this node from its source.
								double[] valout = parseValues(rest.substring(5),1,false);
								current.setSpace(valout[0]);
							}else if(carefulSubstring(rest,0,9).equalsIgnoreCase("PERPSPACE")){
								// sets extra space of this node from its source, on the axis perpendicular to its source.
								double[] valout = parseValues(rest.substring(9),1,false);
								current.setPerpSpace(valout[0]);
							}else if(carefulSubstring(rest,0,7).equalsIgnoreCase("RESCALE")){
								// global command (should only be issued once) that specifies the unit-to-pixel scale of the device
								// specifies the name of the output files, and triggers the fill command.
								// Because it triggers the fill command, this should only be called at the end of the script,
								// when no more data needs to be loaded.
								double[] valout = parseValues(rest.substring(7),1,false);
								scalechange = valout[0];
							}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("FILL")){
								// global command (should only be issued once) that specifies the unit-to-pixel scale of the device
								// specifies the name of the output files, and triggers the fill command.
								// Because it triggers the fill command, this should only be called at the end of the script,
								// when no more data needs to be loaded.
								double[] valout = parseValues(rest.substring(4),1,false);
								fillname = rest.substring(4);
								fillsize = valout[0];
								fill(current,rest.substring(4),valout[0]);
							}else if(carefulSubstring(rest,0,13).equalsIgnoreCase("PDF_MARGIN_CM")){
								// sets the output PDF margins (extra space on each side), in centimeters
								double[] valout = parseValues(rest.substring(13),2,false);
								double s1 = valout[0];
								double s2 = valout[1];
								if(s2==-1){ // if no width given, set width equal to the length
									s2=s1;
								}
								pdfmarginx=(int)(s1*28.35); // 28.35 is the conversion from cm to pixels
								pdfmarginy=(int)(s2*28.35);
							}else if(carefulSubstring(rest,0,13).equalsIgnoreCase("PDF_MARGIN_IN")){
								// same as above, but for inches
								double[] valout = parseValues(rest.substring(13),2,false);
								double s1 = valout[0];
								double s2 = valout[1];
								if(s2==-1){
									s2=s1;
								}
								pdfmarginx=(int)(s1*72); // 72 px = 1 in
								pdfmarginy=(int)(s2*72);
							}else if(carefulSubstring(rest,0,10).equalsIgnoreCase("PDF_MARGIN") ||
									carefulSubstring(rest,0,13).equalsIgnoreCase("PDF_MARGIN_PX")){
								// same as above, but in raw pixels
								double[] valout = parseValues(rest.substring(13),2,false);
								double s1 = valout[0];
								double s2 = valout[1];
								if(s2==-1){
									s2=s1;
								}
								pdfmarginx=(int)(s1); // no conversion needed
								pdfmarginy=(int)(s2);
							}else if(carefulSubstring(rest,0,11).equalsIgnoreCase("PDF_SIZE_IN")){
								// sets the size of the PDF page itself in inches (the size of the paper 
								// that this will be printed on should be used).
								double[] valout = parseValues(rest.substring(11),2,false);
								double s1 = valout[0];
								double s2 = valout[1];
								if(s2==-1){
									s2=s1;
								}
								pdfpagestyle=-2;
								pdfforcesizex=(int)(s1*72);
								pdfforcesizey=(int)(s2*72);
							}else if(carefulSubstring(rest,0,11).equalsIgnoreCase("PDF_SIZE_CM")){
								// see above
								double[] valout = parseValues(rest.substring(11),2,false);
								double s1 = valout[0];
								double s2 = valout[1];
								if(s2==-1){
									s2=s1;
								}
								pdfpagestyle=-2;
								pdfforcesizex=(int)(s1*28.35);
								pdfforcesizey=(int)(s2*28.35);	
							}else if(carefulSubstring(rest,0,11).equalsIgnoreCase("PDF_SIZE_PX")||
									carefulSubstring(rest,0,8).equalsIgnoreCase("PDF_SIZE")){
								// see above
								double[] valout = parseValues(rest.substring(11),2,false);
								double s1 = valout[0];
								double s2 = valout[1];
								if(s2==-1){
									s2=s1;
								}
								pdfpagestyle=-2;
								pdfforcesizex=(int)(s1);
								pdfforcesizey=(int)(s2);
							}else if(carefulSubstring(rest,0,7).equalsIgnoreCase("PDF_OFF")){
								dopdf=false;
								// disables PDF generation
							}else if(carefulSubstring(rest,0,3).equalsIgnoreCase("PDF")){
								// contains some shortcuts 
								// forms of this command:
								// $PDF = lettersize, 1/2 in margin (= 36px)
								// $PDF 0-6 = A0-A6 (ex $PDF 1 = A1)
								// $PDF # # = margins
								// $PDF 0-6 # # = A0-A6 and margins
								double[] valout = parseValues(rest.substring(3),3,false);
								int v1 = (int)(valout[0]);
								int v2 = (int)(valout[1]);
								int v3 = (int)(valout[2]);
								dopdf=true;
								if(v1 == -1){
									// lettersize, 1/2 in margin
									pdfmarginx=36;
									pdfmarginy=36;
									pdfpagestyle=-1; 
								}else{
									if(v2 == -1){
										// custom page size
										pdfpagestyle = v1;
									}else if(v3 == -1){
										// custom margins
										pdfmarginx=v1;
										pdfmarginy=v2;
									}else{
										// custom page size and custom margins
										pdfpagestyle=v1;
										pdfmarginx=v2;
										pdfmarginy=v3;
									}
								}
							}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("DEBUG")){
								// disables or enables debug
								debug=(!debug);
							}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("TEMP")){
								// gives this node a tempid that may be referenced.
								// be aware that temp ids can be overwritten.
								double[] valout = parseValues(rest.substring(4),1,false);
								tempmap[(int)(valout[0])]=current;
							}else if(carefulSubstring(rest,0,14).equalsIgnoreCase("POLY_ENDPOINTX") 
									|| carefulSubstring(rest,0,9).equalsIgnoreCase("POLY_ENDX") 
									|| carefulSubstring(rest,0,17).equalsIgnoreCase("POLYGON_ENDPOINTX") 
									|| carefulSubstring(rest,0,12).equalsIgnoreCase("POLYGON_ENDX")){
								// affects where nodes attached to polygons begin
								int sz = 14;
								if(carefulSubstring(rest,0,9).equalsIgnoreCase("POLY_ENDX")){
									sz=9;
								} else if(carefulSubstring(rest,0,17).equalsIgnoreCase("POLYGON_ENDPOINTX")){
									sz=17;
								} else if(carefulSubstring(rest,0,12).equalsIgnoreCase("POLYGON_ENDX")){
									sz=12;
								}
								double[] valout = parseValues(rest.substring(sz),1,false);
								current.setPolygonEndpoint(valout[0]);
							}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("POLY") 
									|| carefulSubstring(rest,0,7).equalsIgnoreCase("POLYGON")){
								// flags this node as a polygon. Must still be filled with vertices
								current.setPolygon();
							}else if( (carefulSubstring(rest,0,6).equalsIgnoreCase("VERTEX")||
									carefulSubstring(rest,0,4).equalsIgnoreCase("VERT") ||
									carefulSubstring(rest,0,3).equalsIgnoreCase("VEX") ||
									carefulSubstring(rest,0,2).equalsIgnoreCase("VX")) && current.isPolygon()){
								// defines a vertex on the polygon.
								int sz = 4;
								if(carefulSubstring(rest,0,6).equalsIgnoreCase("VERTEX")){ sz=6; }
								if(carefulSubstring(rest,0,3).equalsIgnoreCase("VEX")){ sz=3; }
								if(carefulSubstring(rest,0,2).equalsIgnoreCase("VX")){ sz=2; }
								double[] valout = parseValues(rest.substring(sz),5,false);
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
								current.addPolygonVertex(valout[0],valout[1],bf,ctype,extra);
							}else if(carefulSubstring(rest,0,10).equalsIgnoreCase("ROT_CENTER")){ 
								// determines the rotational center of the polygon.
								double[] valout = parseValues(rest.substring(10),2,false);
								current.setPolygonCrot(valout[0], valout[1]);
							}else if(carefulSubstring(rest,0,12).equalsIgnoreCase("CURVE_WEIGHT")){ 
								// determines the curve weight of the last vertex 
								double[] valout = parseValues(rest.substring(12),1,false);
								current.setPolygonCurveWeight(current.getPolygonVs()-1, valout[0]);
							}else if(carefulSubstring(rest,0,7).equalsIgnoreCase("OUTLINE")){
								// sets the polygon to be an outline.
								current.setPolygonFilled(false);
							}else if(carefulSubstring(rest,0,3).equalsIgnoreCase("VAR")){
								// sets a variable's value
								// takes in a variable ID (0-64) and a value
								double[] valout = parseValues(rest.substring(1),2,false);
								if((int)(valout[0])>=0 && (int)(valout[0])<64){
									temp_vars[(int)(valout[0])]=valout[1];
								}
							}else if(carefulSubstring(rest,0,10).equalsIgnoreCase("BUFFER_BOX")){
								// global command; sets the whole device to be surrounded by a black buffer box.
								// takes in the dimensions of that box.
								buffer_box=true; 
								double[] valout = parseValues(rest.substring(9),2,false);
								buffer_box_x=valout[0];
								buffer_box_y=valout[1];
							}else if(carefulSubstring(rest,0,9).equalsIgnoreCase("TEXT_FONT")){
								// sets the font of the text on this node
								current.setFontName(rest.substring(10));
							}else if(carefulSubstring(rest,0,9).equalsIgnoreCase("TEXT_SIZE")){
								// sets the size of the text on this node
								// note that this is not pt size but relative instead to the scale factor.
								// more information about this in the manual.
								double[] valout = parseValues(rest.substring(9),1,false);
								current.setFontSize(valout[0]);
							}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("TEXT")){
								// set this node to be a text node
								// takes in the text to be displayed.
								current.setText(rest.substring(5));
							}else if(carefulSubstring(rest,0,11).equalsIgnoreCase("NOTEXTSPACE")){
								// command that disables text spacing 
								// text spacing automatically positions the text away from its source node depending
								// on its length.
								current.setTextSpace(true);
							}
						}
						regout = regex(text,"\\{",1);
						if(regout[0][0]==0){
							regout = regex(text,"\\}",1);
							if(regout[0][0]!=-1){
								// a reference is denoted by { refid }
								// read a reference file
								String rest = text.substring(1);
								// parse the remaining values
								// valchk will give 0 if a value doesn't exist
								// valout will hold the actual values
								double[] valchk = parseValues(rest.substring(rest.indexOf("}")),11,true);
								double[] valout = parseValues(rest.substring(rest.indexOf("}")),11,false);
								String innards = rest.substring(0,rest.indexOf("}"));
								double[] innardsval = parseValues(innards,1,false);
								int val = (int)(innardsval[0]);
								String refname = "" + val;
								if(val==-1){ // if the inside of the { } is not a number, use the text inside, instead
									refname = innards.substring(0,innards.length()-1);
									if(refname.endsWith(" ")){
										refname = refname.substring(0,refname.length()-1);
									}
									if(refname.startsWith(" ")){
										refname = refname.substring(1);
									}
								}
								boolean is_x=false;
								boolean is_angle=false;
								// most of the following is dedicated to allowing the various forms of { ref } to exist
								// in all their permutations. See the glossary for a full listing thereof.
								String beyond = rest.substring(rest.indexOf("}")+1);
								int xpos = beyond.toUpperCase().indexOf("X"); // repeat can be specified by addition of an X
								if(xpos<2 && xpos>=0){
									is_x=true;
								}
								if(beyond.toUpperCase().indexOf("ANGLE") != -1){
									is_angle=true;
								}
								int angle_val = 0;
								if(is_x){ 
									angle_val=1; 
									refrepeats[use_fref+1]=(int)(valout[0]);
								}else{
									refrepeats[use_fref+1]=1;
								}
								if(is_angle){
									angle_change[use_fref+1]=valout[angle_val];
								}else{
									angle_val=0;
									if(!is_x){
										angle_val=-1;
									}
								}
								int istart = 1+angle_val;
								int i =1+angle_val; // the leftover values are set to variables in the order listed
								while(i<11){ // ex. the first value after the angle 
									if((int)(valchk[i])!=0){
										temp_vars[i-istart]=valout[i];
									}
									i++;
								}
								// increase the ref nest level and attempt to open the new reference
								use_fref++;
								System.out.println(" LOADING [" + refname + "]");
								fref[use_fref] = new File("./in/ref/ref" + refname + ".txt");
								frefnames[use_fref]=refname;
								try{
									fisref[use_fref]=new FileReader(fref[use_fref]);
									bisref[use_fref]=new BufferedReader(fisref[use_fref]);
								}catch(Exception e){
									System.out.println("!!! ERROR READING REFERENCE FILE" + fref[use_fref].getPath());
									use_fref--;
								}
							}else{
								// without a close bracket on the same line, the command is not calling up the reference
								// but instead defining it, this makes a new reference file for later use
								// start writing a new file:
								
								String rest = text.substring(1);
								double[] valout = parseValues(rest,1,false);
								int val = (int)(valout[0]);
								String refname = "" + val;
								if(val == -1){ // as before, if the ref name isn't a number, grab the text instead
									refname = rest.substring(0,rest.length());
									if(refname.endsWith(" ")){
										refname = refname.substring(0,refname.length()-1);
									}
									if(refname.startsWith(" ")){
										refname = refname.substring(1);
									}
								}
								// write the actual ref file:
								File f3;
								f3 = new File("./in/ref/ref" + refname + ".txt");
								FileWriter fw3;
								try{
									fw3 = new FileWriter(f3); 
									boolean keepwriting=true;
									while(keepwriting){
										String inline = bis.readLine();
										if(debug){
											System.out.println(" REF --> " + inline);
										}
										line++;
										int[][] regout2 = regex(inline.trim(),"\\}",1);
										if(regout2[0][0]==0){
											keepwriting=false;// once the end brace is found, stop writing the reference
										}else{
											fw3.write(inline + System.getProperty ("line.separator"));
										}
									}
									fw3.write("\"END REFERENCE" + System.getProperty ("line.separator"));
									fw3.close();
								}catch(Exception e){
									System.out.println("!!! ERROR WRITING REFERENCE FILE " + refname + "... DOES /in/ref/ FOLDER EXIST?");
								}
								if(debug){
									System.out.println(" WROTE REFERENCE " + refname);
								}
							}
						}
						regout = regex(text,"\\#",1);
						if(regout[0][0]==0){
							// # used for combine code
							String rest = text.substring(regout[0][0]+1); 
							
							if(carefulSubstring(rest,0,7).equalsIgnoreCase("COMBINE")){
								// defines a new combine map
								// needs to be done before other commands; can't fill up a map if the map doesn't exist.
								double[] valout = parseValues(rest.substring(7),1,false);
								int val = (int)(valout[0]);
								combinecur=val;
								combines[combinecur] = new CombineMap(combinecur,targetpath);
								combines[combinecur].conferPDFSettings(dopdf,pdfpagestyle,pdfmarginx,pdfmarginy,
										pdfforcesizex,pdfforcesizey,pdfscale);
								
								if(buffer_box){
									combines[combinecur].setBufferBox((int)(buffer_box_x*fillsize), 
											(int)(buffer_box_y*fillsize));
								}
								combineon=true; // signifies that a map exists
							}
							if(combineon){ // only pass if a map actually exists to be modified
								if(carefulSubstring(rest,0,2).equalsIgnoreCase("ID")){
									// ID specifies the ID of this combine map node
									// proceeding commands will be applied to this node
									double[] valout = parseValues(rest.substring(2),1,false);
									if(valout[0]>=20000){
										valout[0]-=20000;
									}
									int val = (int)(valout[0]);
									combineid=val;
									combines[combinecur].addNode(combineid);
									previous_comnodes[previous_comnode_slot] = combineid; // note that a seperate previous list
									previous_comnode_slot++; // is used for combinemaps and their nodes
									if(previous_comnode_slot>=64){
										previous_comnode_slot=0;
									}
								}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("LAYER")){
									// sets the layer of the currrent node
									// The layer is replicated from the actual device; e.g. LAYER 1 makes this part
									// of the combine map equivalent to layer z=1 of the device.
									double[] valout = parseValues(rest.substring(5),1,false);
									int val = (int)(valout[0]);
									combines[combinecur].setLayer(combineid, val);
								}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("LEFT")){
									// directional settings; denotes the space-relationship of each combinemapnode
									double[] valout = parseValues(rest.substring(4),1,false);
									if(valout[0]>=20000){
										valout[0]-=20000;
									}
									int val = (int)(valout[0]);
									combines[combinecur].setDir(combineid, 1, val);
								}else if(carefulSubstring(rest,0,2).equalsIgnoreCase("UP")){
									double[] valout = parseValues(rest.substring(2),1,false);
									if(valout[0]>=20000){
										valout[0]-=20000;
									}
									int val = (int)(valout[0]);
									combines[combinecur].setDir(combineid, 2, val);
								}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("RIGHT")){
									double[] valout = parseValues(rest.substring(5),1,false);
									if(valout[0]>=20000){
										valout[0]-=20000;
									}
									int val = (int)(valout[0]);
									combines[combinecur].setDir(combineid, 3, val);
								}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("DOWN")){
									double[] valout = parseValues(rest.substring(4),1,false);
									if(valout[0]>=20000){
										valout[0]-=20000;
									}
									int val = (int)(valout[0]);
									combines[combinecur].setDir(combineid, 0, val);
								}else if(carefulSubstring(rest,0,6).equalsIgnoreCase("ROTATE")){
									// determines rotation of this combinemapnode
									// only accepts 90/180/270deg
									// rotates the layer by that amount.
									double[] valout = parseValues(rest.substring(6),1,false);
									int val = (int)(valout[0]);
									if(val==90){
										val=1;
									}else if(val==180){
										val=2;
									}else if(val==270){
										val=3;
									}else{
										val=0;
									}
									combines[combinecur].setRotate(combineid, val);
								}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("FLIPX")){
									// flipX and flipY determine whether or not the layer should be mirrored 
									// horizontally, vertically, or both.
									combines[combinecur].setFlipX(combineid,true);
								}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("FLIPY")){
									combines[combinecur].setFlipY(combineid,true);
								}else if(carefulSubstring(rest,0,11).equalsIgnoreCase("SQUARESPACE") 
										|| carefulSubstring(rest,0,6).equalsIgnoreCase("SQUARE")){
									// if squarespace is active, the combinemap will be made to fit square dimensions.
									combines[combinecur].setSquareSpace();
								}else if(carefulSubstring(rest,0,9).equalsIgnoreCase("DRAWBOXES") 
										|| carefulSubstring(rest,0,3).equalsIgnoreCase("BOX")){
									// drawboxes determines whether or not to draw the outline of each combinemapnode's boundaries
									combines[combinecur].setDrawBoxes();
								}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("FILL")){
									// the fill command, as before, works to finish the combinemap and triggers compilation into
									// actual images. Fill here does not specify the scale, however, only the output name.
									combines[combinecur].compile(fillname, rest.substring(5));
								}else if(carefulSubstring(rest,0,4).equalsIgnoreCase("TEMP")){
									// set a temp id for this combinemapnode
									double[] valout = parseValues(rest.substring(2),1,false);
									int val = (int)(valout[0]);
									if(val>=0 && val<nodesize){
										comtempmap[val] = combineid;
									}
								}else if(carefulSubstring(rest,0,3).equalsIgnoreCase("VAR")){
									// allows a var to be set, as in regular commands. Takes an id (0-63) and a value
									double[] valout = parseValues(rest.substring(1),2,false);
									if((int)(valout[0])>=0 && (int)(valout[0])<64){
										temp_vars[(int)(valout[0])]=valout[1];
									}
								}else if(carefulSubstring(rest,0,6).equalsIgnoreCase("CENTER")){
									// determines whether or not to force one node's x/y position to be in the center
									// for each of the combinemapnodes. This is used to force alignment over a specific point, 
									// no matter rotation or flipping.
									double[] valout = parseValues(rest.substring(1),1,false);
									int out = (int)(valout[0]);
									MapNode p = allnodes[0];
									if(out>=10000){ // same shortcut as seen earlier: >10000 node are set to absolute ids
										out-=10000;
										p = allnodes[out];
									}else{
										if(out>=nodesize || out<0){out=0;}
										p = placemap[out];
									}
									combines[combinecur].setForceSameCenter((int)(fillsize*(Math.abs(furthest_neg_x)+p.getFillX())),
											(int)(fillsize*(Math.abs(furthest_neg_y)+p.getFillY())));
									
								}else if(carefulSubstring(rest,0,5).equalsIgnoreCase("SHEET")){
									double[] valout = parseValues(rest.substring(1),3,false);
									// SHEET LAYER X Y
									// automatically repeats the same layer XxY times
									int lay = (int)(valout[0]);
									int xlen = (int)(valout[1]);
									int ylen = (int)(valout[2]);
									int lastid = combines[combinecur].getLastId(); // give lastid to be used
									int i=0;
									while(i<xlen){
										// need to keep track of the values used up in this process
										// start at (lastid) +1
										// then increase in y direction from lastid+2 to lastid+1+ylen
										// reset to (lastid+1+ylen)+1
										// then increase in y direction from (lastid+1+ylen)+2 to (lastid+1+ylen)+1+ylen
										int id = lastid+1;
										int conid = lastid;
										if(i>0){
											// attach to node id(i) - ylen
											conid = id-ylen;
										}
										// ID 
										combineid=id;
										combines[combinecur].addNode(combineid);
										previous_comnodes[previous_comnode_slot] = combineid;
										previous_comnode_slot++;
										if(previous_comnode_slot>=64){
											previous_comnode_slot=0;
										}
										// LAYER
										combines[combinecur].setLayer(combineid, lay);
										// POS
										if(id!=0){ // if it's 0, don't need a pos
											combines[combinecur].setDir(combineid, 3, lastid);
										}
										lastid=id;
										
										int o=0;
										while(o<ylen){
											// ID
											id = lastid+1;
											conid = lastid;
											combineid=id;
											combines[combinecur].addNode(combineid);
											previous_comnodes[previous_comnode_slot] = combineid;
											previous_comnode_slot++;
											if(previous_comnode_slot>=64){
												previous_comnode_slot=0;
											}
											// LAYER
											combines[combinecur].setLayer(combineid, lay);
											// POS
											combines[combinecur].setDir(combineid, 0, lastid);
											lastid =id;
											
											o++;
										}
										i++;
									}
								}
							}
						}else if(text.indexOf("{")==-1){ // unless we are calling a reference, not having #
							combineon=false; // at the linestart signifies the end of the combine map
						}
					}
				}
			}
			fis.close();
			bis.close();
		}catch (Exception e){
			e.printStackTrace();
			System.out.println("!!! Specification Read Failure");
			return;
		}

	}
	
	// carefulSubstring() is a helper function that avoids going over a string's length
	public static String carefulSubstring(String input, int min, int max){
		// if the string is not long enough, just gets all of it
		if(input.length()>=max){
			return input.substring(min,max);
		}else if(input.length()>=min){
			return input.substring(min);
		}
		return input;
	}
	
	// parsevalues() is an extensive function that converts a number or a set of operations into a single number.
	public static double[] parseValues(String input,int max, boolean retvalues){
		input = input + " ";
		// reference reduction
		// reduces references into number
		// ex (1 . len) gives the length of <1>
		// (2 . color_red) gives the red value of <2>
		// Parsevalues reduction
		// reduces groupings into numbers
		// ex (1 + (2 - 3)) becomes (1 + -1) becomes (0) = 0
		//String digit = "\\-\\d.^\\s";
		String superdigit = "-?\\d*(\\.\\d+)?";//"[" + digit + "]+";
		// the above will be used constantly in spaces where numbers might exist
		// it detects numbers of both integer and double forms (4 vs 4.0)
		int loops=0;
		int[][] regout_test = regex(input,"[\\(\\)]",max);
		while(regout_test[0][0]!=-1){
			// all of these tests share the same format:
			// first, a regex test is preformed, searching for a specific operator 
			// then, the found operators are looped through, processed, and their space in the text is replaced by 
			// their output values. 
			
			int[][] regout = regex(input,"\\(" + superdigit + " \\. [^\\)]+\\)",max);
			int i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					String chase =input.substring(regout[i][0]+1,regout[i][1]-1);
					double[] vals = parseValues(" " + chase + " ",2,retvalues);
					int a = (int)(vals[0]);
					MapNode reqnode = new MapNode(-1,0);
					if(a<10000){
						reqnode = placemap[a];
					}else if(a<20000){
						reqnode = allnodes[a-10000];
						String chaselen = "" + a + " . ";
						String textid = chase.substring(chaselen.length());
						//System.out.println("TEXTID " + textid);
						if(debug){
							if(previous_node_slot-1!=-1){
								System.out.println(" FETCHED " + reqnode.getValueFromString(textid) + " TO "
								+ (10000 + previous_nodes[previous_node_slot-1].getNodeID()));
							}else{
								System.out.println(" FETCHED " + reqnode.getValueFromString(textid) + " TO NO PRIOR?");
							}
						}
						double dist = (reqnode.getValueFromString(textid));
						input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
						regout = regex(input,"\\(" + superdigit + " \\. [^\\)]+\\)",max);
					}else{
						int reala = a-=20000;
						CombineMap comb = combines[current_combine];
						
						String chaselen = "" + a + " . ";
						String textid = chase.substring(chaselen.length());
						double dist = comb.getValueFromString(reala,textid);
						input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
						regout = regex(input,"\\(" + superdigit + " \\. [^\\)]+\\)",max);
					}
					//int[][] regout2 = regex(chase,"[\\w^\\)]+",max);
					//System.out.println(" CHASE " + chase);
					/*System.out.println(" R1 " + regout2[1][0] + " R2 " + regout2[1][1]);
					String textid = chase.substring(regout2[1][0],regout2[1][1]);
					if(chase.substring(chase.length()-1).equalsIgnoreCase(")")){
						chase = chase.substring(0,chase.length()-1);
					}*/
					
				}
				i++;
			}
			regout = regex(input,"\\((?i)VAR(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					int a = (int)(vals[0]);
					input = input.substring(0,regout[i][0]) + temp_vars[a] + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)VAR(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)BACK(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					int goback = (int)(vals[0])+1; // back 0 is referencing yourself, this way
					int a =0;
					if(!combineon){
						int spot = previous_node_slot;
						while(goback>0){
							spot--;
							goback--;
							if(spot<0){
								spot=63;
							}
						}
						
						a = 10000 + previous_nodes[spot].getNodeID();
					}else{
						int spot = previous_comnode_slot;
						while(goback>0){
							spot--;
							goback--;
							if(spot<0){
								spot=63;
							}
						}
						
						a = 20000 + previous_comnodes[spot];
					}
					input = input.substring(0,regout[i][0]) + a + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)BACK(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)TEMP(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					int tempnum = (int)(vals[0]);
					int a =0;
					if(!combineon){
						a = 10000 + tempmap[tempnum].getNodeID();
					}else{
						a = 20000 + comtempmap[tempnum];
					}
					input = input.substring(0,regout[i][0]) + a + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)TEMP(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)COMBINE(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					int tempnum = (int)(vals[0]);
					int a =tempnum-20000;
					input = input.substring(0,regout[i][0]) + a + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)COMBINE(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)NODE(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					int tempnum = (int)(vals[0]);
					int a = tempnum;
					if(a>=10000){
						a-=10000;
					}
					input = input.substring(0,regout[i][0]) + a + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)NODE(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)ID(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					int tempnum = (int)(vals[0]);
					int a =tempnum;
					if(a>=10000){
						if(!combineon){
							a-=10000;
						}else{
							a-=20000;
						}
					}
					input = input.substring(0,regout[i][0]) + a + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)ID(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)RANDOM(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					
					input = input.substring(0,regout[i][0]) + (Math.random()*vals[0]) + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)RANDOM(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)FLOOR(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					
					input = input.substring(0,regout[i][0]) + (Math.floor(vals[0])) + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)FLOOR(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)CEIL(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					
					input = input.substring(0,regout[i][0]) + (Math.ceil(vals[0])) + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)CEIL(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)ROUND(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					
					input = input.substring(0,regout[i][0]) + (Math.round(vals[0])) + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)ROUND(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)SIN(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					
					input = input.substring(0,regout[i][0]) + (Math.sin(vals[0])) + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)SIN(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)COS(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					
					input = input.substring(0,regout[i][0]) + (Math.cos(vals[0])) + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)COS(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\((?i)TAN(?-i) " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",1,retvalues);
					
					input = input.substring(0,regout[i][0]) + (Math.tan(vals[0])) + input.substring(regout[i][1]);
					regout = regex(input,"\\((?i)TAN(?-i) " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\(" + superdigit + " to " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",2,retvalues);
					int a = (int)(vals[0]);
					int b = (int)(vals[1]);
					MapNode anode = placemap[0];
					MapNode bnode = placemap[0];
					if(a>=10000){
						anode = allnodes[a-10000];
					}else{
						anode = placemap[a];
					}
					if(b>=10000){
						bnode = allnodes[b-10000];
					}else{
						bnode = placemap[b];
					}
					double[] points = pathToNode(anode,bnode);
					double dist = Math.sqrt(Math.pow(points[0], 2)+Math.pow(points[1], 2));
					input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
					regout = regex(input,"\\(" + superdigit + " to " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\(" + superdigit + " \\^ " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",2,retvalues);
					double dist = Math.pow(vals[0], vals[1]);
					input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
					regout = regex(input,"\\(" + superdigit + " ^ " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\(" + superdigit + " \\/ " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",2,retvalues);
					double dist = vals[0]/vals[1];
					input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
					regout = regex(input,"\\(" + superdigit + " \\/ " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\(" + superdigit + " \\* " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",2,retvalues);
					double dist = vals[0]*vals[1];
					input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
					regout = regex(input,"\\(" + superdigit + " \\* " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\(" + superdigit + " % " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",2,retvalues);
					double dist = vals[0]%vals[1];
					input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
					regout = regex(input,"\\(" + superdigit + " % " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\(" + superdigit + " - " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",2,retvalues);
					double dist = vals[0]-vals[1];
					input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
					regout = regex(input,"\\(" + superdigit + " - " + superdigit + "\\)",max);
				}
				i++;
			}
			regout = regex(input,"\\(" + superdigit + " \\+ " + superdigit + "\\)",max);
			i=0;
			while(i<max){
				if(regout[i][0]!=-1){
					double[] vals = parseValues(" " + input.substring(regout[i][0]+1,regout[i][1]-1) + " ",2,retvalues);
					double dist = vals[0]+vals[1];
					input = input.substring(0,regout[i][0]) + dist + input.substring(regout[i][1]);
					regout = regex(input,"\\(" + superdigit + " \\+ " + superdigit + "\\)",max);
				}
				i++;
			}
			if(loops<10 || (loops>=100 && loops<110)){
				if(debug){
					System.out.println(" " + input);
				}
			}
			if(loops==100){
				// if the loop is stuck after 100 iterations, assume that the operations given are malformed
				// (otherwise it implies the user used over 100 levels of nesting...)
				// at this point there are two options: abort and lose all of the data
				// or make some assumptions and try to reconstruct a valid operation
				// the latter case will be taken
				
				// these assumptions could be destructive to properly formed operations, so its not worth
				// preforming them by default at the first loop. Two passes will be made:
				// 1) iterate over the characters in the remaining text. Each time an operator is approached, add a space on each
				// side of it (unless that side has a parenthesis adjacent)
				// 2) remove all parentheses from the remaining text, and iterate over it grabbing sections,
				// enclosing them in parentheses, and reconstructing the text with these new sections.
				// this particular method will fail on nested groups 
				
				// it is okay to have a low success rate here because, the user should not input
				// malformed operators to begin with. 
				System.out.println(" ERROR: could not parse values... attempting to fix input");
				int c=1;
				String newinput = "";
				String lastch= "" + input.charAt(0);
				while(c<input.length()-1){
					String ch = "" + input.charAt(c);
					String nextch = "" + input.charAt(c+1);
					newinput = newinput + lastch;
					if(ch.equalsIgnoreCase("+") || ch.equalsIgnoreCase("-") || ch.equalsIgnoreCase("*") || ch.equalsIgnoreCase("^")
							|| ch.equalsIgnoreCase("%") || ch.equalsIgnoreCase("/")){
						if(!lastch.equalsIgnoreCase("(")){
							newinput = newinput + " ";
						}
						newinput = newinput + ch;
						if(!nextch.equalsIgnoreCase(")")){
							newinput = newinput + " "; 
						}
						lastch=nextch;
						c++;
						if(c>=input.length()){
							newinput = newinput + nextch;
							break;
						}
					}else{
						lastch=ch;
					}
					c++;
					if(c==input.length()-1){
						newinput = newinput+input.charAt(c);
					}
				}
				if(debug){
					System.out.println("      ... OLD INPUT: " + input);
					System.out.println("      ... INTERMEDIATE: " + newinput);
				}
				c=0;
				newinput = newinput.trim();
				input=newinput;
				input = input.replaceAll("\\(","");
				input = input.replaceAll("\\)","");
				newinput = "";
				String section = "";
				boolean hasop=false;
				boolean hassecondspace=false;
				while(c<input.length()){
					String ch ="" +input.charAt(c);
					if(ch.equalsIgnoreCase("+") || ch.equalsIgnoreCase("-") || ch.equalsIgnoreCase("*") || ch.equalsIgnoreCase("^")
							|| ch.equalsIgnoreCase("%") || ch.equalsIgnoreCase("/")){
							hasop=true;
					}
					if(ch.equalsIgnoreCase(" ") && hasop){
						if(hassecondspace){
							section = "(" + section + ")";
							newinput = newinput + section;
							section = "";
							hasop=false;
							hassecondspace=false;
						}else{
							hassecondspace=true;
						}
					}
					section = section + ch;
					c++;
				}
				if(hasop){
					section = "(" + section + ")";
				}
				newinput = " " +newinput + section;
				
				if(debug){
					System.out.println("      ... CORRECTION ATTEMPT: " + newinput);
				}
				input=newinput;
			}
			if(loops>1000){
				System.out.println(" ERROR: could not parse values! Break point forced");
				break;
			}
			loops++;
			regout_test = regex(input,"[\\(\\)]",max); // only continue iterating if there are still parentheses left
		}
		input = input + " "; // trailing space necessary for proper regout2 detection below 
						     // (otherwise substrings are a character short)
		
		// finally, grab the actual values remaining from the iterations
		double[] outvals = new double[max];
		int i=0;
		while(i<max){
			outvals[i]=-1;
			i++;
		}
		String outstring = "[  ";
		int curvals =0;
		int[][] regout2 = regex(input,"\\ \\-?[\\d.]+(?=[^\\)])",max); // it would be useful to consult a regex guide
		// but basically this is searching for numbers that have a space in front of them
		i=0;
		while(i<max){
			if(regout2[i][0]!=-1){
				String text = input.substring(regout2[i][0],regout2[i][1]);
				try{
					outvals[curvals]=Double.parseDouble(text);
					outstring = outstring + outvals[curvals] + " ";
					curvals++;
				}catch(Exception E){}
			}
			i++;
		}
		outstring = outstring + " ]";
		if(debug){
			System.out.println("VALS : " + outstring);
		}
		if(retvalues){
			i =0;
			while(i<max){
				if(curvals>i){
					outvals[i]=1;
				}else{
					outvals[i]=0;
				}
				i++;
			}
		}
		return outvals;
		
	}
	
	// pathToNode() takes in two nodes and returns the x,y distance between them
	// returns a double[2] of the form double[]{x,y}
	public static double[] pathToNode(MapNode start, MapNode stop){
		// !: returns [x distance, y distance] from start to stop
		
		// note:
		// if you follow the path of nodes back by always going to the first neighbor 
		// you'll get back to node 0
		// this is because the first neighbor is the node that created the prior node
		
		// strategy: follow start and stop back to node 0
		// this will create the path from node a to node b
		// even if excessive it will always work
		
		double startx=0;
		double starty=0;
		boolean atzero=false;
		MapNode current_node=start;
		while(!atzero){
			if(current_node.equals(placemap[0])){
				atzero=true;
				break;
			}else{
				MapNode next_node = current_node.getNeighbor(0); 
				double angle = current_node.getNeighborAngle(0);
				double dist = current_node.getRealDistanceToNeighbor(0);
				startx -= Math.cos(Math.toRadians(angle))*dist;
				starty -= Math.sin(Math.toRadians(angle))*dist; 
				current_node=next_node; 
			}
		}
		double stopx=0;
		double stopy=0;
		atzero=false;
		current_node=stop;
		while(!atzero){
			if(current_node.equals(placemap[0])){
				atzero=true;
				break;
			}else{
				MapNode next_node = current_node.getNeighbor(0); 
				double angle = current_node.getNeighborAngle(0);
				double dist = current_node.getRealDistanceToNeighbor(0);
				stopx -= Math.cos(Math.toRadians(angle))*dist;
				stopy -= Math.sin(Math.toRadians(angle))*dist; 
				current_node=next_node; 
			}
		}
		
		double pointx = stopx-startx; // how far is stop from start?
		double pointy = stopy-starty;
		double[] out = new double[2];
		out[0]=pointx; out[1]=pointy;
		return out;
	}
	
	// regex() is a helper function for performing regex tests, takes in the text, the regex to search for, and
	// a maximum number of results to find. 
	public static int[][] regex(String input, String test, int max){
		// note: restricted characters are
		// <([{\^-=$!|]})?*+.>
		// and must be accessed via \\
		// also note \\d is a standin for "any digits"
		Pattern testpattern = Pattern.compile(test);

		Matcher matcher = testpattern.matcher(input);

		int match=0;
		int[][] matches = new int[max][2];
		int i=0;
		while(i<max){
			matches[i][0]=-1;
			matches[i][1]=-1;
			i++;
		}
		while (matcher.find() && match<max) {
			matches[match][0]=matcher.start();
			matches[match][1]=matcher.end();
			// could also pass matcher.group() here? which is the text it matched
			match++;
		}
		
		return matches;
	}
	
	// fill() is a core function that, once all the data is loaded via digest, takes
	// the existing data and compiles it into actual images and PDF files.
	public static void fill(MapNode current, String name, double pixconv){
		// first, calculate some PDF parameters
		if(scalechange!=1.0){
			int i =0;
			while(i<nodecount){
				allnodes[i].rescale(scalechange);
				i++;
			}
		}

		PDRectangle pdfstyle = PDPage.PAGE_SIZE_LETTER;
		if(dopdf && !previewmode){
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
		
		// calculate x,y positions for each node
		// calculate max sizes
		// draw each node
		
		// strategy:
		// start at <0>, x=0, y=0
		// path to all neighbors, giving them x and y coordinates based on distance from <0>
		// path to their neighbors and so on until all nodes have coordinates
		
		int lowest_z = 0;
		int highest_z = 0;
		
		// first sort z's
		int i=0;
		while(i<nodecount){
			int iz = allnodes[i].getFillZ();
			if(iz<lowest_z){
				lowest_z=iz;
			}
			if(iz>highest_z){
				highest_z=iz;
			}
			i++;
		}
		
		
		// this is just some groundwork
		// Z's in the scripts can have any value, positive or negative, between the ranges of allowable integers in java
		// and do not need to be sequentially arranged (e.g. a script may have a layer 3 and a layer 5 but no layer 4). 
		// new_zs is the actual count of unique layers
		// new_z is a list of sequential layers (ex. if we have layers 0,1,3,5 and 25, we have 
		//    new_z: 0  1  2  3  4
		//	  layer: 0  1  3  5  25
		// thus this is easier to iterate over and manage in general. Also necessary for the MapBoss
		int new_zs =0; 
		int[] new_z=new int[1];
		i=lowest_z;
		if(debug){
			System.out.print("REAL LAYERS: ");
		}
		while(i<=highest_z){
			int o=0;
			while(o<nodecount){
				if(allnodes[o].getFillZ()==i){
					if(debug){
						System.out.print("" + i + ", ");
					}
					new_zs++;
					int[] old_z=new_z;
					new_z = new int[new_zs];
					if(new_zs>1){
						int p=0;
						while(p<new_zs-1){
							new_z[p]=old_z[p];
							p++;
						}
						new_z[new_zs-1]=i;
					}else{
						new_z[0]=i;
					}
					break;
				}
				o++;
			}
			i++;
		}
		if(debug){
			System.out.println("");
		}
		
		if(debug){ // in debug mode, list all of the layers that will be generated.
			i=0;
			System.out.print("     LAYERS: ");
			while(i<new_zs){
				System.out.print("" + new_z[i] + ", ");
				i++;
			}
			System.out.println("");
		}
		
		// first, autospace values should be handled if autospace is enabled
		if(!finalized){
			i=0;
			if(autospace){ 
				while(i<nodecount){
					if(allnodes[i].isRect() || allnodes[i].isCorner()){
						if(allnodes[i].getNeighborDistance(0)==0){
							// in old versions, autospace was used to give each rectangle a slight overlap with its parent.
							double newspace = allnodes[i].getRectLen() / -10.0; 
							allnodes[i].setDistance(0,newspace);
						}
					}
					i++;
				}
			}
			// finalize text spacing
			i=0;
			while(i<nodecount){
				MapNode inode = allnodes[i];
				if(inode.getText().length()>0){
					if(inode.getTextSpace() && i>0){
						// in order to figure out text spacing, each text needs to be loaded
						// and tested for length w/ the corresponding fill size
						BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); // dummy image for testing
						FontMetrics fm = img.getGraphics().getFontMetrics(new Font(inode.getFontName(), Font.BOLD, (int)(pixconv*inode.getFontSize())));
						int width = fm.stringWidth(inode.getText());
						inode.setDistance(0, inode.getNeighborDistance(0)+((double)(width)/(double)(pixconv)));
						
						inode.getNeighbor(0).setDistanceByNode(inode,inode.getNeighborDistance(0)+((double)(width)/(double)(pixconv)));
						if(debug){
							System.out.println(" SPACE " + width + " = " + ((double)(width)/(double)(pixconv)));
						}
					}
				}
				i++;
			}
			// additionally, finalize all polygon vertices
			i=0;
			while(i<nodecount){
				if(allnodes[i].isPolygon()){
					allnodes[i].finalizePolygonVs();
				}
				i++;
			}
			
			
			placemap[0].setFillXY(0, 0); // the origin point always exists at (0,0)-- it is the center point.
			furthest_pos_x=-1; furthest_neg_x=1; furthest_pos_y=-1; furthest_neg_y=1;
			//furthest_pos_x_node=placemap[0];furthest_neg_x_node=placemap[0];
			//furthest_pos_y_node=placemap[0];furthest_neg_y_node=placemap[0];
			finalizeXY(placemap[0],pixconv,0,0,true);
			i=0;
			while(i<placemap[0].getNeighbors()){
				MapNode nei = placemap[0].getNeighbor(i);
				if(!nei.isFillXY()){
					finalizeXY(nei,pixconv,0,0,false); // sort through the whole web and finalize each node
					// moving down the tree from the source so each parent has a position that can
					// be referenced before its children need to find positions
					// otherwise relative positioning would break down as there would be nothing to be
					// relative to.
				}
				i++;
			}
			finalized=true;
		}
		// this should fill out all the positions on the device
		// however first the total device dimensions need to be established
		
		// This should be done by keeping track of the furthest x,y positions in each direction
		// as finalize_xy proceeds
		
		double db_total_x = furthest_pos_x+Math.abs(furthest_neg_x); 
		double db_total_y = furthest_pos_y+Math.abs(furthest_neg_y);
		if(buffer_box){
			db_total_x+=buffer_box_x*2;
			db_total_y+=buffer_box_y*2;
		}
		int total_x = (int)(pixconv*db_total_x);
		int total_y = (int)(pixconv*db_total_y);
		
		if(total_x==0){ total_x=1; }
		if(total_y==0){ total_y=1; }
		
		double db_center_x = Math.abs(furthest_neg_x);
		double db_center_y = Math.abs(furthest_neg_y);
		if(buffer_box){
			db_center_x+=buffer_box_x;
			db_center_y+=buffer_box_y;
		}
		int center_x = (int)(pixconv*db_center_x);
		int center_y = (int)(pixconv*db_center_y);
		
		// mapboss holds a listing of all the nodes in their finalized forms
		// and is used to build nodeid rasters for the preview windows' selection feature
		mapboss = new MapBoss(new_zs,new_z);
		mapboss.setNodes(allnodes, nodecount);
		
		
		
		int newzz = 0;//lowest_z;
		while(newzz < new_zs){//highest_z){
			int zz = new_z[newzz]; 
			// i.e.: newzz is from the sequential new_z list of layers (internal list)
			//       zz is from the original script layer definitions (nonsequential, not necessarily positive, etc)
			
			// first create the image raster for this layer. 
			// we will grab every node on this layer and draw onto this image raster per node
			BufferedImage image = new BufferedImage(total_x, total_y, BufferedImage.TYPE_INT_RGB);
			if(debug){
				System.out.println("IMAGE SIZE: " + total_x + ", " + total_y);
			}
			WritableRaster raster = image.getRaster();
			int[] white = new int[3];
			white[0]=255; white[1]=255; white[2]=255;
			int[] black = new int[3];
			black[0]=0; black[1]=0; black[2]=0;
			// @CHANGE TESTING
			black=replaceblack;
			// now create the node id raster for this layer.
			int[][] nodeidraster = new int[total_x][total_y];
			// start every layer by making it entirely white by default
			drawRectangle(raster,0,0,raster.getWidth(),raster.getHeight(),white,nodeidraster,-1);
			
			// FIRST PASS
			// In this pass, grab each node and draw black around its bufferspace
			
			if(buffer_box){ // if buffer_box is being used, the background is simply filled black
				// instead of grabbing each node and giving it a buffer.
				drawRectangle(raster,(int)(pixconv*buffer_box_x),(int)(pixconv*buffer_box_y),
						total_x-(int)(pixconv*buffer_box_x*2),total_y-(int)(pixconv*buffer_box_y*2),black,nodeidraster,-1);
			}else{
				i =0;
				while(i<nodecount){
					if(debug){
						System.out.println("BUFFERING NODE " + i);
					}
					MapNode inode = allnodes[i];
					int iz = inode.getFillZ();
					if(iz==zz){ // only draw a node if it's on the current z-level layer
						int ix = center_x + (int)(pixconv*inode.getFillX());
						int iy = center_y + (int)(pixconv*inode.getFillY());
						if(inode.isRect()){// && inode.getRectStartNeighbor()!=-1){
							double angle = 180;
							if(inode.getRectStartNeighbor()!=-1){ // if it has a parent, get its angle from the parent connection
								// otherwise the angle will default to 180, like the origin node.
								int snode_num = inode.getRectStartNeighbor();
								MapNode snode = inode.getNeighbor(snode_num);
								angle = inode.getNeighborAngle(snode_num);
							}
							if(inode.getBufferX()!=0 || inode.getBufferY()!=0){ // only draw buffer if it has non-zero dimensions
								double rlen = inode.getRectLen()+inode.getBufferX()*2; // the actual dimensions include the rectangle's
								double rwid = inode.getRectWid()+inode.getBufferY()*2;
								
								int irwid = (int)(rwid*pixconv);
								int irlen = (int)(rlen*pixconv);
								drawRectAngle(raster,ix,iy,irlen,irwid,black,Math.toRadians(angle),(int)(inode.getBufferX()*pixconv),
										irwid/2,nodeidraster,i);
							}
						}else if(inode.isCorner()){
							double startangle = 180;
							if(inode.getRectStartNeighbor()!=-1){ // same angle logic as rectangle
								int snode_num = inode.getRectStartNeighbor();
								MapNode snode = inode.getNeighbor(snode_num);
								startangle = inode.getNeighborAngle(snode_num);
							}
							if(inode.getBufferX()!=0 || inode.getBufferY()!=0){
								double rbase = inode.getCornerBase()+inode.getBufferX()*2;
								double rhei = inode.getCornerHeight()+inode.getBufferY()*2;
								int irbase = (int)(rbase*pixconv);
								int irhei = (int)(rhei*pixconv);
								
								int mod=0;
								if(inode.getCornerAngle()<0){
									mod = -(int)(inode.getBufferX()*pixconv/2)*2;
								}
								if(exact_corner_buffer){
									// option that gives corner's triangular buffers rather than rectangular buffers
									drawCorner(raster,ix,iy,irbase,irhei,black,Math.toRadians(startangle),Math.toRadians(inode.getCornerAngle()),
											inode.getCornerCurveweight(),0,irbase/2+(int)(inode.getBufferX()*pixconv/2)+mod,nodeidraster,i);
								}else{
									mod=1;
									if(startangle==180){
										mod=-1;
										// this specific angle fails to align properly, presumably (or, obviously) because of an error within the
										// drawRectAngle function. However, this issue does not seem to present itself anywhere else and it is simpler 
										// to take this naive approach.
										// more specifically, this issue is caused by the separation of 'sinangle==0' cases in the drawRectAngle function
									}
									double radangle = Math.toRadians(startangle);
									
									drawRectAngle(raster,ix,iy,irhei,irbase,black,Math.toRadians(startangle),irhei/2, // (int)(inode.getBufferX()*pixconv)
											irbase/2-(int)(inode.getPerpSpace()*pixconv*mod),nodeidraster,i);
								}
							}
						}else if(inode.isCircle()){
							// angle logic is notably absent here because a circle is rotationally symmetric 
							if(inode.getBufferX()!=0 || inode.getBufferY()!=0){
								if(inode.getBufferX()==inode.getBufferY()){
									drawCircle(raster,ix,iy,(int)(pixconv*(inode.getCircleRadius()+inode.getBufferX())),
											black,nodeidraster,i);
								}else{
									// an obscure case: if a circle has differing buffer dimensions, it is given a
									// rectangular buffering instead of a circular buffering.
									drawRectangle(raster,ix-(int)(pixconv*(inode.getCircleRadius()+inode.getBufferX())),
											iy-(int)(pixconv*(inode.getCircleRadius()+inode.getBufferY())),
											(int)(pixconv*(inode.getCircleRadius()+inode.getBufferX())),
											(int)(pixconv*(inode.getCircleRadius()+inode.getBufferY())),
											black,nodeidraster,i);
								}
							}
						}else if(inode.isPolygon()){
							double angle = 180;
							if(inode.getRectStartNeighbor()!=-1){
								angle = inode.getNeighborAngle(inode.getRectStartNeighbor());
							}
							double[][] polypoints = inode.getPolygonBPoints(pixconv,0);
							int[] polyx = new int[inode.getPolygonVs()];
							int[] polyy = new int[inode.getPolygonVs()];
							int pi =0;
							while(pi<inode.getPolygonVs()){
								polyx[pi]=(int)(polypoints[pi][0]*pixconv);
								polyy[pi]=(int)(polypoints[pi][1]*pixconv);
								pi++;
							}
							int[] polyc = inode.getPolygonCtypes();
							double[] polycw = inode.getPolygonCurveWeights();
							drawPoly(raster, ix,iy, polyx,polyy, polyc, polycw, inode.getPolygonVs(), 
									black, Math.toRadians(angle), (int)(inode.getPolygonCrotX()*pixconv), 
									(int)(inode.getPolygonCrotY()*pixconv), inode.getPolygonFilled(),
									nodeidraster,i);
							
							
						}
					}
					i++;
				}
			}
			// SECOND PASS
			// In this pass, every node is grabbed again and is actually drawn this time.
			i =0;
			while(i<nodecount){
				if(debug){
					System.out.println("RENDERING NODE " + i);
				}
				MapNode inode = allnodes[i];
				int iz = inode.getFillZ();
				if(iz==zz){
					int ix = center_x + (int)(pixconv*inode.getFillX());
					int iy = center_y + (int)(pixconv*inode.getFillY());
					if(inode.isRect()){// && inode.getRectStartNeighbor()!=-1){
						double angle = 180; // same angle logic as before
						if(inode.getRectStartNeighbor()!=-1){
							int snode_num = inode.getRectStartNeighbor();
							MapNode snode = inode.getNeighbor(snode_num);
							angle = inode.getNeighborAngle(snode_num);
						}
						double rlen = inode.getRectLen();
						double rwid = inode.getRectWid();
						
						int irwid = (int)(rwid*pixconv);
						int irlen = (int)(rlen*pixconv);
						drawRectAngle(raster,ix,iy,irlen,irwid,inode.getColor(),Math.toRadians(angle),0,irwid/2,
								nodeidraster,i);
					}else if(inode.isCorner()){
						double startangle = 180;
						if(inode.getRectStartNeighbor()!=-1){
							int snode_num = inode.getRectStartNeighbor();
							MapNode snode = inode.getNeighbor(snode_num);
							startangle = inode.getNeighborAngle(snode_num);
						}
						double rbase = inode.getCornerBase();
						double rhei = inode.getCornerHeight();
						int irbase = (int)(rbase*pixconv);
						int irhei = (int)(rhei*pixconv);
						drawCorner(raster,ix,iy,irbase,irhei,inode.getColor(),Math.toRadians(startangle),Math.toRadians(inode.getCornerAngle()),
								inode.getCornerCurveweight(),0,irbase/2,nodeidraster,i);
					}else if(inode.isCircle()){
						if(inode.getCircleInner()!=-1){
							// if the circle has an inner radius specified, draw a disk instead of a full circle
							drawDisk(raster,ix,iy,(int)(pixconv*inode.getCircleRadius()),(int)(pixconv*inode.getCircleInner()),
									inode.getColor(),nodeidraster,i);
						}else{
							drawCircle(raster,ix,iy,(int)(pixconv*inode.getCircleRadius()),inode.getColor(),nodeidraster,i);
						}
					}else if(inode.isPolygon()){
						double angle = 180;
						if(inode.getRectStartNeighbor()!=-1){
							angle = inode.getNeighborAngle(inode.getRectStartNeighbor());
						}
						int[] polyx = new int[inode.getPolygonVs()];
						int[] polyy = new int[inode.getPolygonVs()];
						int pi =0;
						while(pi<inode.getPolygonVs()){
							polyx[pi]=(int)(inode.getPolygonX(pi)*pixconv);
							polyy[pi]=(int)(inode.getPolygonY(pi)*pixconv);
							pi++;
						}
						int[] polyc = inode.getPolygonCtypes();
						double[] polycw = inode.getPolygonCurveWeights();
						drawPoly(raster, ix,iy, polyx,polyy, polyc, polycw, inode.getPolygonVs(), 
								inode.getColor(), Math.toRadians(angle), (int)(inode.getPolygonCrotX()*pixconv), 
								(int)(inode.getPolygonCrotY()*pixconv), inode.getPolygonFilled(),
								nodeidraster,i);
					}
				}
				i++;
			}
			
			// DRAW TEXT
			// check each node if it has text, and if it does, draw said text now (i.e., text always is drawn on top)
			i=0;
			while(i<nodecount){
				MapNode inode = allnodes[i];
				if(inode.getFillZ()==zz){
					int ix = center_x + (int)(pixconv*inode.getFillX());
					int iy = center_y + (int)(pixconv*inode.getFillY());
					
					if(inode.getText().length()>0){
						BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
						FontMetrics fm = img.getGraphics().getFontMetrics(new Font(inode.getFontName(), Font.BOLD, (int)(pixconv*inode.getFontSize())));
						int width = fm.stringWidth(inode.getText());
						int height = (int)(inode.getFontSize()*pixconv);
						int len = width;
						
						if(debug){
							System.out.println("TEXT " + inode.getText());
						}
						Graphics g = image.getGraphics();
						Graphics2D g2 = (Graphics2D)(g);
						AffineTransform old = g2.getTransform();
						if(i>0){
							double ang2 = Math.toRadians(180+inode.getNeighborAngle(inode.getRectStartNeighbor()));
							int newix2 = ix-(int)(len*Math.cos(ang2)-height/2*Math.sin(ang2)); // re: rotation matrix
							int newiy2 = iy-(int)(len*Math.sin(ang2)+height/2*Math.cos(ang2));
							
							if(inode.getRectStartNeighbor()!=-1){
								g2.rotate(Math.toRadians(180+inode.getNeighborAngle(inode.getRectStartNeighbor())));
							}
						}
						int[] car = inode.getColor();
						g2.setColor(new Color(car[0],car[1],car[2]));
						g2.setFont(new Font(inode.getFontName(), Font.BOLD, (int)(pixconv*inode.getFontSize())));
						if(inode.getRectStartNeighbor()!=-1){
							double ang = Math.toRadians(180+inode.getNeighborAngle(inode.getRectStartNeighbor()));
							int newix = ix-(int)(len*Math.cos(ang));//-height*0*Math.sin(ang)); // re: rotation matrix
							int newiy = iy-(int)(len*Math.sin(ang));//+height*0*Math.cos(ang)); 
							// the height term only causes unwanted perturbations, but is remained here for the sake
							// of seeing what the complete rotation matrix would look like
							
							int finalix = (int)(newix*Math.cos(-ang)-newiy*Math.sin(-ang));
							int finaliy = (int)(newix*Math.sin(-ang)+newiy*Math.cos(-ang));
							
							g2.drawString(inode.getText(),finalix,finaliy);
						}else{
							g2.drawString(inode.getText(), ix-len,iy+height);
						}
						g2.setTransform(old);
					}
				}
				i++;
			}

			// FINISH
			// finally, save the image that was generated and handle pdf exporting
			mapboss.setLayer(zz, nodeidraster, targetpath + name + "_" + zz + ".png");
			if(!previewmode){
				System.out.println("SAVING IMAGE AT " + targetpath + name + "_" + zz + ".png");
				cleanImage(raster);
			}
			try{
				ImageIO.write(image, "PNG", new File(targetpath + name + "_" + zz + ".png"));
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
			if(dopdf && !previewmode){
				PDFExport.makePDF(targetpath + name + "_" + zz + ".pdf",
						targetpath + name + "_" + zz + ".png",pdfstyle,pdfmarginx,pdfmarginy,pdfscale);
			}
			
			// now make the cut layers
			// these are separate images which only have cut-specified nodes drawn, and those only in outline form.
			if(!previewmode){
				image = new BufferedImage(total_x, total_y, BufferedImage.TYPE_INT_RGB);
				System.out.println("IMAGE SIZE: " + total_x + ", " + total_y);
				raster = image.getRaster();
				//as before, establish a new image and fill it with white only.
				drawRectangle(raster,0,0,raster.getWidth(),raster.getHeight(),white,nodeidraster,i);
				
				boolean anycuts = false; // will be used to keep track of whether or not any nodes were actually drawn
				// if nothing exists on this layer with the 'cut' property, this image will simply be discarded and not saved.
				
				int[] pink = new int[]{255,155,155};
				
				// FIRST PASS
				// In this pass, grab each node and draw black around its bufferspace
				
				if(buffer_box){ // if buffer_box is being used, the background is simply filled black
					// instead of grabbing each node and giving it a buffer.
					drawRectangle(raster,(int)(pixconv*buffer_box_x),(int)(pixconv*buffer_box_y),
							total_x-(int)(pixconv*buffer_box_x*2),total_y-(int)(pixconv*buffer_box_y*2),black,nodeidraster,-1);
				}else{
					i =0;
					while(i<nodecount){
						if(debug){
							System.out.println("BUFFERING NODE " + i);
						}
						MapNode inode = allnodes[i];
						int iz = inode.getFillZ();
						if(iz==zz){ // only draw a node if it's on the current z-level layer
							int ix = center_x + (int)(pixconv*inode.getFillX());
							int iy = center_y + (int)(pixconv*inode.getFillY());
							if(inode.isRect()){// && inode.getRectStartNeighbor()!=-1){
								double angle = 180;
								if(inode.getRectStartNeighbor()!=-1){ // if it has a parent, get its angle from the parent connection
									// otherwise the angle will default to 180, like the origin node.
									int snode_num = inode.getRectStartNeighbor();
									MapNode snode = inode.getNeighbor(snode_num);
									angle = inode.getNeighborAngle(snode_num);
								}
								if(inode.getBufferX()!=0 || inode.getBufferY()!=0){ // only draw buffer if it has non-zero dimensions
									double rlen = inode.getRectLen()+inode.getBufferX()*2; // the actual dimensions include the rectangle's
									double rwid = inode.getRectWid()+inode.getBufferY()*2;
									
									int irwid = (int)(rwid*pixconv);
									int irlen = (int)(rlen*pixconv);
									drawRectAngle(raster,ix,iy,irlen,irwid,black,Math.toRadians(angle),(int)(inode.getBufferX()*pixconv),
											irwid/2,nodeidraster,i);
								}
							}else if(inode.isCorner()){
								double startangle = 180;
								if(inode.getRectStartNeighbor()!=-1){ // same angle logic as rectangle
									int snode_num = inode.getRectStartNeighbor();
									MapNode snode = inode.getNeighbor(snode_num);
									startangle = inode.getNeighborAngle(snode_num);
								}
								if(inode.getBufferX()!=0 || inode.getBufferY()!=0){
									double rbase = inode.getCornerBase()+inode.getBufferX()*2;
									double rhei = inode.getCornerHeight()+inode.getBufferY()*2;
									int irbase = (int)(rbase*pixconv);
									int irhei = (int)(rhei*pixconv);
									
									int mod=0;
									if(inode.getCornerAngle()<0){
										mod = -(int)(inode.getBufferX()*pixconv/2)*2;
									}
									if(exact_corner_buffer){
										// option that gives corner's triangular buffers rather than rectangular buffers
										drawCorner(raster,ix,iy,irbase,irhei,black,Math.toRadians(startangle),Math.toRadians(inode.getCornerAngle()),
												inode.getCornerCurveweight(),0,irbase/2+(int)(inode.getBufferX()*pixconv/2)+mod,nodeidraster,i);
									}else{
										mod=1;
										if(startangle==180){
											mod=-1;
											// this specific angle fails to align properly, presumably (or, obviously) because of an error within the
											// drawRectAngle function. However, this issue does not seem to present itself anywhere else and it is simpler 
											// to take this naive approach.
											// more specifically, this issue is caused by the separation of 'sinangle==0' cases in the drawRectAngle function
										}
										double radangle = Math.toRadians(startangle);
										
										drawRectAngle(raster,ix,iy,irhei,irbase,black,Math.toRadians(startangle),irhei/2, // (int)(inode.getBufferX()*pixconv)
												irbase/2-(int)(inode.getPerpSpace()*pixconv*mod),nodeidraster,i);
									}
								}
							}else if(inode.isCircle()){
								// angle logic is notably absent here because a circle is rotationally symmetric 
								if(inode.getBufferX()!=0 || inode.getBufferY()!=0){
									if(inode.getBufferX()==inode.getBufferY()){
										drawCircle(raster,ix,iy,(int)(pixconv*(inode.getCircleRadius()+inode.getBufferX())),
												black,nodeidraster,i);
									}else{
										// an obscure case: if a circle has differing buffer dimensions, it is given a
										// rectangular buffering instead of a circular buffering.
										drawRectangle(raster,ix-(int)(pixconv*(inode.getCircleRadius()+inode.getBufferX())),
												iy-(int)(pixconv*(inode.getCircleRadius()+inode.getBufferY())),
												(int)(pixconv*(inode.getCircleRadius()+inode.getBufferX())),
												(int)(pixconv*(inode.getCircleRadius()+inode.getBufferY())),
												black,nodeidraster,i);
									}
								}
							}else if(inode.isPolygon()){
								double angle = 180;
								if(inode.getRectStartNeighbor()!=-1){
									angle = inode.getNeighborAngle(inode.getRectStartNeighbor());
								}
								double[][] polypoints = inode.getPolygonBPoints(pixconv,0);
								int[] polyx = new int[inode.getPolygonVs()];
								int[] polyy = new int[inode.getPolygonVs()];
								int pi =0;
								while(pi<inode.getPolygonVs()){
									polyx[pi]=(int)(polypoints[pi][0]*pixconv);
									polyy[pi]=(int)(polypoints[pi][1]*pixconv);
									pi++;
								}
								int[] polyc = inode.getPolygonCtypes();
								double[] polycw = inode.getPolygonCurveWeights();
								drawPoly(raster, ix,iy, polyx,polyy, polyc, polycw, inode.getPolygonVs(), 
										black, Math.toRadians(angle), (int)(inode.getPolygonCrotX()*pixconv), 
										(int)(inode.getPolygonCrotY()*pixconv), inode.getPolygonFilled(),
										nodeidraster,i);
								
								
							}
						}
						i++;
					}
				}
				// SECOND PASS
				// In this pass, every node is grabbed again and is actually drawn this time.
				i =0;
				while(i<nodecount){
					if(debug){ 
						System.out.println("RENDERING NODE " + i);
					} 
					MapNode inode = allnodes[i];
					int iz = inode.getFillZ();
					if(iz==zz && inode.getCut()){
						anycuts=true;
						int ix = center_x + (int)(pixconv*inode.getFillX());
						int iy = center_y + (int)(pixconv*inode.getFillY());
						if(inode.isRect()){// && inode.getRectStartNeighbor()!=-1){
							double angle = 180; // same angle logic as before
							if(inode.getRectStartNeighbor()!=-1){
								int snode_num = inode.getRectStartNeighbor();
								MapNode snode = inode.getNeighbor(snode_num);
								angle = inode.getNeighborAngle(snode_num);
							}
							double rlen = inode.getRectLen();
							double rwid = inode.getRectWid();
							
							int irwid = (int)(rwid*pixconv);
							int irlen = (int)(rlen*pixconv);
							drawRectAngle(raster,ix,iy,irlen,irwid,pink,Math.toRadians(angle),0,irwid/2,
									nodeidraster,i);
						}else if(inode.isCorner()){
							double startangle = 180;
							if(inode.getRectStartNeighbor()!=-1){
								int snode_num = inode.getRectStartNeighbor();
								MapNode snode = inode.getNeighbor(snode_num);
								startangle = inode.getNeighborAngle(snode_num);
							}
							double rbase = inode.getCornerBase();
							double rhei = inode.getCornerHeight();
							int irbase = (int)(rbase*pixconv);
							int irhei = (int)(rhei*pixconv);
							drawCorner(raster,ix,iy,irbase,irhei,pink,Math.toRadians(startangle),Math.toRadians(inode.getCornerAngle()),
									inode.getCornerCurveweight(),0,irbase/2,nodeidraster,i);
						}else if(inode.isCircle()){
							if(inode.getCircleInner()!=-1){
								// if the circle has an inner radius specified, draw a disk instead of a full circle
								drawDisk(raster,ix,iy,(int)(pixconv*inode.getCircleRadius()),(int)(pixconv*inode.getCircleInner()),
										pink,nodeidraster,i);
							}else{
								drawCircle(raster,ix,iy,(int)(pixconv*inode.getCircleRadius()),pink,nodeidraster,i);
							}
						}else if(inode.isPolygon()){
							double angle = 180;
							if(inode.getRectStartNeighbor()!=-1){
								angle = inode.getNeighborAngle(inode.getRectStartNeighbor());
							}
							int[] polyx = new int[inode.getPolygonVs()];
							int[] polyy = new int[inode.getPolygonVs()];
							int pi =0;
							while(pi<inode.getPolygonVs()){
								polyx[pi]=(int)(inode.getPolygonX(pi)*pixconv);
								polyy[pi]=(int)(inode.getPolygonY(pi)*pixconv);
								pi++;
							}
							int[] polyc = inode.getPolygonCtypes();
							double[] polycw = inode.getPolygonCurveWeights();
							drawPoly(raster, ix,iy, polyx,polyy, polyc, polycw, inode.getPolygonVs(), 
									pink, Math.toRadians(angle), (int)(inode.getPolygonCrotX()*pixconv), 
									(int)(inode.getPolygonCrotY()*pixconv), inode.getPolygonFilled(),
									nodeidraster,i);
						}
					}
					i++;
				}
				
				if(anycuts){
					// image processing
					// the algorithm here is simple:
					// loop through the entire image array
					// if a pixel does not touch a pink spot, set it to white
					// afterwards, loop again to change all the pink to white
					int j =0;
					while(j<image.getWidth()){
						int k =0;
						while(k<image.getHeight()){
							int[] px = raster.getPixel(j, k, new int[3]);
							if(px[0]==black[0] && px[1]==black[1] && px[2]==black[2]){ // only check pixel if it is black
								boolean touchespink=false;
								
								int a=-1;
								while(a<=1 && !touchespink){
									int b = -1;
									while(b<=1 && !touchespink){
										if(a!=0 || b!=0){ // ignore the pixel itself
											int nx = j+a;
											int ny = k+b;
											if(nx>=0 && nx<image.getWidth() && ny>=0 && ny<image.getHeight()){ // only check px in bounds
												int[] npx = raster.getPixel(nx,ny,new int[3]);
												if(npx[0]==pink[0] && npx[1]==pink[1] && npx[2]==pink[2]){
													touchespink=true;
												}
											}
										}
										b++;
									}
									a++;
								}
								
								if(!touchespink){
									raster.setPixel(j, k, white);
								}
							}
							k++;
						}
						j++;
					}
					
					int reps = 1;
					while(reps<cut_scale){
						boolean[][] touchesblack = new boolean[image.getWidth()][image.getHeight()];
						j =0;
						while(j<image.getWidth()){
							int k =0;
							while(k<image.getHeight()){
								int[] px = raster.getPixel(j, k, new int[3]);
								touchesblack[j][k]=false;
								if(px[0]==white[0] && px[1]==white[1] && px[2]==white[2]){ // only check pixel if it is black
									
									int a=-1;
									while(a<=1 && !touchesblack[j][k]){
										int b = -1;
										while(b<=1 && !touchesblack[j][k]){
											if(a!=0 || b!=0){ // ignore the pixel itself
												int nx = j+a;
												int ny = k+b;
												if(nx>=0 && nx<image.getWidth() && ny>=0 && ny<image.getHeight()){ // only check px in bounds
													int[] npx = raster.getPixel(nx,ny,new int[3]);
													if(npx[0]==black[0] && npx[1]==black[1] && npx[2]==black[2]){
														touchesblack[j][k]=true;
													}
												}
											}
											b++;
										}
										a++;
									}
									
								}
								k++;
							}
							j++;
						}
						
						j =0;
						while(j<image.getWidth()){
							int k =0;
							while(k<image.getHeight()){
								int[] px = raster.getPixel(j, k, new int[3]);
								if(!touchesblack[j][k]){
									raster.setPixel(j, k, black);
								}
								k++;
							}
							j++;
						}
						reps++;
					}
					
					j =0;
					while(j<image.getWidth()){
						int k =0;
						while(k<image.getHeight()){
							int[] px = raster.getPixel(j, k, new int[3]);
							if(px[0]==pink[0] && px[1]==pink[1] && px[2]==pink[2]){ // only check pixel if it is black
								raster.setPixel(j, k, white);
							}
							k++;
						}
						j++;
					}
				}
				
				// as before, only save these layers if any cuts were actually made.
				if(anycuts && !previewmode){
					System.out.println("SAVING IMAGE AT " + targetpath + name + "_" + zz + "_cut.png");
					try{
						ImageIO.write(image, "PNG", new File(targetpath + name + "_" + zz + "_cut.png"));
					}catch(IOException ioe){
						ioe.printStackTrace();
					}
					if(dopdf){
						PDFExport.makePDF(targetpath + name + "_" + zz + "_cut.pdf",
								targetpath + name + "_" + zz + "_cut.png",pdfstyle,pdfmarginx,pdfmarginy,pdfscale);
					}
				}
			}
			newzz++; // go to next layer and repeat
		}
		
	}
	
	// finalizeXY() is a function that is used to set the position of a mapnode (used on each mapnode during filling) 
	// and also to determine the maximum boundaries of the device. 
	public static void finalizeXY(MapNode current, double pixconv, double pastx, double pasty, boolean zero){
		double posx = 0;
		double posy = 0;
		double negx = 0;
		double negy = 0; // used to keep track of the furthest bounds of this piece
		// will compare later to the furthest current bounds of the device and update if this 
		// piece would go over those bounds.
		double lastx=pastx;
		double lasty=pasty;
		double buffer_x = current.getBufferX()+current.getExtraX();
		double buffer_y = current.getBufferY()+current.getExtraY();
		double angle = 180;
		double dist = 0;
		double perpdist = 0;
		if(!zero){
			MapNode last_node = current.getNeighbor(0); 
			angle = current.getNeighborAngle(0);
			dist = current.getRealDistanceToNeighbor(0);
			perpdist = current.getPerpSpace();
		}
		
		pastx -= Math.cos(Math.toRadians(angle))*dist;
		pasty -= Math.sin(Math.toRadians(angle))*dist; 
		double perpangle = angle + 90; // the perpendicular angle can be found by simply adding 90deg
		pastx -= Math.cos(Math.toRadians(perpangle))*perpdist;
		pasty -= Math.sin(Math.toRadians(perpangle))*perpdist; 
		current.setFillXY(pastx,pasty);
		
		if(current.isPolygon()){
			// 1. get the bufferspace
			// 2. loop through each vertex
			// polyfinal
			double[][] bf_space = current.getPolygonBPoints(100,0);
			double ca = Math.cos(Math.toRadians(angle));
			double sa = Math.sin(Math.toRadians(angle));
			double realx = pastx;
			double realy = pasty;
			int i =0;
			i =0;
			while(i<current.getPolygonVs()){
				double tx = bf_space[i][0];
				double ty = bf_space[i][1];
				bf_space[i][0] = realx + (tx * ca - ty * sa);
				bf_space[i][1] = realy + (ty * ca + tx * sa);
				i++;
			}
			i=0;
			while(i<current.getPolygonVs()){
				if(bf_space[i][0]>posx){ posx=bf_space[i][0]; }
				if(bf_space[i][0]<negx){ negx=bf_space[i][0]; }
				if(bf_space[i][1]>posy){ posy=bf_space[i][1]; }
				if(bf_space[i][1]<negy){ negy=bf_space[i][1]; }
				i++;
			}
		}else if(current.isRect() || current.isCorner() || current.isCorner()){
			double ca = Math.cos(Math.toRadians(angle));
			double sa = Math.sin(Math.toRadians(angle)); // cosangle and sinangle
			
			double rectlen = current.getRectLen()+buffer_x*2;
			double rectwid = current.getRectWid()+buffer_y*2;
			if(current.isCorner()){ // corners can use the same dimension-seeking code but with their own dimensions.
				// this introduces a slight inaccuracy as the corner piece is missing one vertex that the rectangle piece has, but this can only cause
				// overestimates in the shape size, which are acceptable (whereas underestimates are more problematic)
				rectlen = current.getCornerHeight()+buffer_x*2;
				rectwid = current.getCornerBase()+buffer_y*2;
			}
			double rectcx = buffer_x;
			double rectcy = rectwid/2;
			if(current.isCorner()){
				rectcy = current.getCornerPerpSpace();
				rectcx = current.getRealCornerHeight();
			}
			
			double[] rectx = new double[4];
			double[] recty = new double[4];
			double[] rot_rectx = new double[4];
			double[] rot_recty = new double[4];
			
			rectx[0] = 0 - rectcx;
			recty[0] = 0 - rectcy;
			
			rectx[1] = 0 - rectcx;
			recty[1] = rectwid-rectcy;
			
			rectx[2] = rectlen-rectcx;
			recty[2] = 0 - rectcy;
			
			rectx[3] = rectlen-rectcx;
			recty[3] = rectwid-rectcy;
			
			if(current.isCorner()){
				rectx[0] = 0 - rectlen/2;
				recty[0] = 0 -buffer_y + rectcy-current.getCornerBase();
				
				rectx[1] = 0 - rectlen/2;
				recty[1] = 0 +buffer_y - current.getCornerPerpSpace();
				
				rectx[2] = rectlen/2;
				recty[2] = 0 -buffer_y + rectcy-current.getCornerBase();
				
				rectx[3] = rectlen/2;
				recty[3] = 0 +buffer_y - current.getCornerPerpSpace();
			}
			
			double realx = pastx;//-buffer_x*ca;
			double realy = pasty;//-buffer_y*sa;
			
			int c=0;
			while(c<4){
				rot_rectx[c] = rectx[c] * ca - recty[c] * sa;
				rot_recty[c] = recty[c] * ca + rectx[c] * sa;
				rot_rectx[c] += realx;
				rot_recty[c] += realy;
				c++;
			}
			
			//need to check each of the rot points for furthest positions
			c = 0;
			while(c<4){
				if(rot_rectx[c]>posx){
					posx=rot_rectx[c];
				}
				if(rot_rectx[c]<negx){
					negx=rot_rectx[c];
				}
				if(rot_recty[c]>posy){
					posy=rot_recty[c];
				}
				if(rot_recty[c]<negy){
					negy=rot_recty[c];
				}
				c++;
			}
			
			double o_buffer_x=buffer_x;
			buffer_x = (buffer_x)*Math.cos(Math.toRadians(angle))-(buffer_y)*Math.sin(Math.toRadians(angle));
			buffer_y = (buffer_y)*Math.cos(Math.toRadians(angle))+(o_buffer_x)*Math.sin(Math.toRadians(angle));
			
		}
		
		if(current.isCircle()){
			buffer_x+=current.getCircleRadius();
			buffer_y+=current.getCircleRadius();
		}
		
		if(!current.isRect() && !current.isPolygon() && !current.isCorner()){
			posx = pastx+buffer_x;
			posy = pasty+buffer_y;
			negx = pastx-buffer_x;
			negy = pasty-buffer_y;
		}
		
		
		
		if(posx>furthest_pos_x){
			furthest_pos_x=posx;
		}
		if(negx<furthest_neg_x){
			furthest_neg_x=negx;
		}
		if(posy>furthest_pos_y){
			furthest_pos_y=posy;
		}
		if(negy<furthest_neg_y){
			furthest_neg_y=negy;
		}
		if(!zero){
			int i=1;
			while(i<current.getNeighbors()){
				MapNode nei = current.getNeighbor(i);
				if(!nei.isFillXY()){
					finalizeXY(nei,pixconv,pastx,pasty,false);
				}
				i++;
			}
		}
		
	}

	// drawRectangle(): used to draw rectangles without rotation (effectively depreciated by drawRectAngle())
	public static void drawRectangle(WritableRaster raster, int x, int y, int w, int h, int[] col, int[][] idraster, int id){
		int i =x;
		while(i<x+w && i<raster.getWidth()){
			int o=y;
			while(o<y+h && o<raster.getHeight()){
				if(i>=0 && i<raster.getWidth() && o>=0 && o<raster.getHeight()){
					raster.setPixel(i,o,col);
					idraster[i][o]=id;
				}
				o++;
			}
			i++;
		} 
	}
	
	// drawRectAngleOutline(): used to draw rectangular outlines with an angle
	public static void drawRectAngleOutline(WritableRaster raster, int x, int y, int len, int wid, int[] col,
			double angle, int cx, int cy, int[][] idraster, int id){
		int[] invx = new int[4];
		int[] invy = new int[4];
		int[] ctype = new int[4];
		invx[0]=0;
		invy[0]=wid/2;
		ctype[0]=0;
		invx[1]=len;
		invy[1]=wid/2;
		ctype[1]=0;
		invx[2]=len;
		invy[2]=-wid/2;
		ctype[2]=0;
		invx[3]=0;
		invy[3]=-wid/2;
		ctype[3]=0;
		
		double[] cweight = new double[4];
		cweight[0]=0; cweight[1]=0; cweight[2]=0; cweight[3]=0;
		
		drawPoly(raster, x, y, invx, invy, ctype, cweight,
				4, col, angle, cx, cy, false, idraster, id);
	}
	
	// drawRectAngle(): used to draw rectangles with rotation.
	public static void drawRectAngle(WritableRaster raster, int x, int y, int len, int wid, int[] col,
			double angle, int cx, int cy, int[][] idraster, int id){
		double cosangle = Math.cos(angle);
		double sinangle = Math.sin(angle);
		if(Math.abs(cosangle)<0.01){ // this clamping is necessary because, for instance, math likes to return
			cosangle=0;				 // "1.22x10^-16" instead of "0" for angle = 180deg = pi
		}							 // which leads to slight, unwanted rotations at large distances from the pivot
		if(Math.abs(sinangle)<0.01){
			sinangle=0; // angle is 0 or 180
		}
		if(Math.abs(cosangle)>0.99){
			cosangle=cosangle/Math.abs(cosangle);
		}
		if(Math.abs(sinangle)>0.99){
			sinangle=sinangle/Math.abs(sinangle);
		}
		
		// vertices
		// used for double-checking
		/*int[] vx = new int[4];
		int[] vy = new int[4];
		int[] rot_vx = new int[4];
		int[] rot_vy = new int[4];
		
		vx[0]= 0 -cx;
		vy[0]= 0 -cy;
		vx[1]= 0 -cx;
		vy[1]= wid-1-cy;
		vx[2]= len-1-cx;
		vy[2]= 0 -cy;
		vx[3]= len-1-cx;
		vy[3]= wid-1-cy;
		
		int i=0;
		while(i<4){
			rot_vx[i] = x+(int)(vx[i] * cosangle - vy[i]*sinangle);
			rot_vy[i] = y+(int)(vy[i] * cosangle + vx[i]*sinangle);
			i++;
		}*/
		int mcx = len+wid;
		int mcy = wid+len; // center coords of the map
		
		boolean[][] map = new boolean[(len+wid)*2][(wid+len)*2];
		int i=0;
		while(i<(len+wid)*2){
			int o=0;
			while(o<(len+wid)*2){
				map[i][o]=false;
				if(sinangle==0){
					int realx = i-mcx;
					int realy = o-mcy;
					if(cosangle<0){
						realx-=cx;
						realy += cy;
						if(realx>-len && realx<=0 && realy>0 && realy<wid){
							map[i][o]=true;
						}
					}else{
						realx+=cx;
						realy += cy;
						if(realx>=0 && realx<len && realy>0 && realy<wid){
							map[i][o]=true;
						}
					}
				}else{
				
					double rotx = i-mcx;
					double roty = o-mcy;
					double nx = roty+(rotx/sinangle)*cosangle;
					nx/= (((cosangle*cosangle)/sinangle)+sinangle);//roty / (((cosangle - rotx)/sinangle)*cosangle+sinangle);
					double ny = (nx*cosangle - rotx)/sinangle;
					double realx = nx+cx;
					double realy = ny+cy;
					
					if(realx>=0 && realx<len && realy>=0 && realy<wid){
						//System.out.println("OUT : : : " + realx + " " + realy);
						map[i][o]=true;
					}
				
				}
				
				o++;
			}
			i++;
		}
		
		//int cx = len/2;
		//int cy = wid/2;
//		double dbi =0;
//		while(dbi<(double)(len)){
//			double dbo =0;
//			while(dbo<(double)(wid)){
//				if(true){
//					break;
//				}
//				double nx = dbi - cx;
//				double ny = dbo - cy;
//				
//				int rotx = (int)((nx * cosangle - ny*sinangle));
//				int roty = (int)((ny * cosangle + nx*sinangle));
//				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
//					map[rotx+mcx][roty+mcy]=true;
//				}
//				rotx++;
//				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
//					map[rotx+mcx][roty+mcy]=true;
//				}
//				roty++;
//				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
//					map[rotx+mcx][roty+mcy]=true;
//				}
//				rotx--;
//				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
//					map[rotx+mcx][roty+mcy]=true;
//				}
//				rotx--;
//				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
//					map[rotx+mcx][roty+mcy]=true;
//				}
//				roty--;
//				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
//					map[rotx+mcx][roty+mcy]=true;
//				}
//				roty--;
//				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
//					map[rotx+mcx][roty+mcy]=true;
//				}
//				rotx++;
//				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
//					map[rotx+mcx][roty+mcy]=true;
//				}
//				dbo+=0.1;
//			}
//			dbi+=0.1;
//		}
//		// rotation like this will create holes in the solid body
//		// we need to inflate to remove these holes
//		boolean changed = true;
//		while(changed){
//			changed=false;
//			i=0;
//			while(i<(len+wid)*2){
//				int o =0;
//				while(o<(len+wid)*2){
//					if(true){
//						break;
//					}
//					if(map[i][o]==false){
//						int surr = 0; // surrounding trues
//						if(i>0){
//							if(map[i-1][o]==true){ surr++; }
//						}
//						if(o>0){
//							if(map[i][o-1]==true){ surr++; }
//							/*if(i<(len+wid)*2-1){
//								if(map[i+1][o-1]==true){ surr++; }
//							}
//							if(i>0){
//								if(map[i-1][o-1]==true){ surr++; }
//							}*/
//						}
//						if(i<(len+wid)*2-1){
//							if(map[i+1][o]==true){ surr++; }
//						}
//						if(o<(len+wid)*2-1){
//							if(map[i][o+1]==true){ surr++; }
//							/*if(i<(len+wid)*2-1){
//								if(map[i+1][o+1]==true){ surr++; }
//							}
//							if(i>0){
//								if(map[i-1][o+1]==true){ surr++; }
//							}*/
//						}
//						if(surr>=5){
//							// the algorithm here is basically to fill in spaces that have 3 or more filled neighbors
//							// this works out because on the edges of the rectangle a space can only have 2 filled neighbors
//							// but in the interior can have 3 or more
//							map[i][o]=true;
//							changed=true;
//						}
//					}
//					o++;
//				}
//				i++;
//			}
//		}
		// now match up 'map' with the image raster
		// and plop 'color' down wherever map is true
		// center of the rotation should be centered over x,y
		int thrws = 0;
		i=0;
		while(i<(len+wid)*2){
			int o =0;
			while(o<(len+wid)*2){
				int realx = i - mcx + x;
				int realy = o - mcy + y;
				if(map[i][o]==true){
					try{
						raster.setPixel(realx,realy,col);
						idraster[realx][realy]=id;
					}catch(Exception e){
						if(thrws<5){
							if(debug){
								System.out.println("ERROR: OUT OF BOUNDS [" + realx + "," + realy + "]");
							}
						}
						thrws++;
					}
				}
				o++;
			}
			i++;
		}
	}
	
	// drawCorner(): used to draw cornerpieces with an angle
	public static void drawCorner(WritableRaster raster, int x, int y, int base, int hei, int[] col,
			double angle, double endangle, double curveweight, int cx, int cy, int[][] idraster, int id){
		angle += Math.toRadians(180); // automatic adjustment because this code was written backwards, accidentally
		
		int len=hei;
		int wid=base;
		
		boolean flipped=false;
		if(endangle<0){
			flipped=true;
			endangle=Math.abs(endangle);
		}
		
		double cosangle = Math.cos(angle);
		double sinangle = Math.sin(angle);
		if(Math.abs(cosangle)<0.01){ // this clamping is necessary because, for instance, math likes to return
			cosangle=0;				 // "1.22x10^-16" instead of "0" for angle = 180deg = pi
		}							 // which leads to slight, unwanted rotations at large distances from the pivot
		if(Math.abs(sinangle)<0.01){
			sinangle=0;
		}
		if(Math.abs(cosangle)>0.99){
			cosangle=cosangle/Math.abs(cosangle);
		}
		if(Math.abs(sinangle)>0.99){
			sinangle=sinangle/Math.abs(sinangle);
		}	
		
		double cosendangle = Math.cos(endangle);
		double sinendangle = Math.sin(endangle);
		if(Math.abs(cosendangle)<0.01){ // this clamping is necessary because, for instance, math likes to return
			cosendangle=0;				 // "1.22x10^-16" instead of "0" for angle = 180deg = pi
		}							 // which leads to slight, unwanted rotations at large distances from the pivot
		if(Math.abs(sinendangle)<0.01){
			sinendangle=0;
		}
		if(Math.abs(cosendangle)>0.99){
			cosendangle=cosendangle/Math.abs(cosendangle); // the sign function would also work here
		}
		if(Math.abs(sinendangle)>0.99){
			sinendangle=sinendangle/Math.abs(sinendangle);
		}	
		
		boolean[][] map = new boolean[(len+wid)*2][(wid+len)*2];
		int i=0;
		while(i<len*2){
			int o=0;
			while(o<wid*2){
				map[i][o]=false;
				o++;
			}
			i++;
		}
		int mcx = len+wid;
		int mcy = wid+len; // center coords of the map
		//int cx = len/2;
		//int cy = wid/2;
		
		// need to find where the top vertex would be
		// slope = s = sin(a)/cos(a)
		// y = s*x, and line should end when sqrt(x*x+y*y) = height
		// y*y = s*s*x*x so sqrt(xx+ssxx) = h
		// (1+ss)xx = hh, so xx = hh/(1+ss) and x = sqrt(hh/(1+ss))
		double slope = 99999; // default case is endangle = 90, slope would be infinite
		double invslope = -((double)(len)/(double)(wid));
		int vertx=0;
		int verty=len;
		if(cosendangle!=0){ // if the angle is 90, don't bother with this (esp. don't divide by 0)
			slope=sinendangle/cosendangle;
			vertx = (int)(Math.sqrt(len*len/(1+slope*slope)));
			verty = (int)(slope*vertx);
			if(wid-vertx!=0){
				invslope = -(verty/(wid-vertx));
				if(flipped){
					invslope = -(verty/(wid-vertx));
				}
			}else{
				invslope=-9999;
			}
		}
		
		
		double cenx = wid/2;
		double midx = (wid-vertx)/2;
		double perpdist = midx;//cenx-midx;
		double perpangle = angle + Math.toRadians(90);
		
		int extra_space=3;
		if(!flipped){
			//cy-=1;
			cy-=perpdist;//Math.sin(perpangle)*perpdist;
			cx+=verty/2+extra_space;//-(wid-vertx)/2;
		}else{
			cy-=2;
			cy+=perpdist;//Math.sin(perpangle)*perpdist;
			//cy-=wid;
			cx+=verty/2+extra_space;//-(wid-vertx)/2;
		}
		//cx+=Math.cos(perpangle)*perpdist;
		//System.out.println(" PERPANGLE " + perpangle + " PERPDIST " + perpdist + 
		//		" SIN " +  Math.sin(perpangle) + " -- " + (Math.sin(perpangle)*perpdist));
		
		//System.out.println(" CORNER CENTER " + (verty/2) + " -- " + ((wid-vertx)/2) + " CX :: " + cx);
				
		if(flipped){
			vertx = wid-vertx;
		}
		int mod = extra_space+0;
		
		
		//System.out.println(" VERTX " + vertx);
		i =0;
		while(i<len+mod){
			double curve = 1.0;
			double curvestep = (curveweight/(double)(wid-vertx));
			if(flipped){
				curve = 1.0+curveweight;
				curvestep = (curveweight/(double)(vertx));
				curvestep = -curvestep;
			}
			
			double totalrad = Math.toRadians(endangle);
			double radstep = totalrad/(double)(wid-vertx);
			if(flipped){
				radstep = totalrad/(double)(vertx);
			}
			
			int o =0;
			int widmod=0;
			if(sinangle==0){
				widmod=1;
			}
			while(o<wid-widmod){
				int nx = i - cx;
				int ny = o - cy;
				// the difference between the rectAngle and corner scripts is:
				// there needs to be a cutoff implemented here in order to account for the missing vertex.
				// there are two segments: before vertx and after
				boolean pass = true;
				if(!flipped){
					if(o<vertx){
						int limy = (int)(slope*o);
						if(i>limy+mod){
							pass=false;
						}
					}else{
						// the slope of the connector line is invslope
						double crv = Math.sin((Math.acos((double)(o-vertx)/(double)(wid-vertx))/radstep)*radstep)*(verty);
						int limy = (int)( (curveweight*crv + (1.0-curveweight)*(verty+invslope*(o-vertx))) );
						//int limy = (int)(curve*(invslope*(o-vertx)+verty));
						if(i>limy+mod){
							pass=false;
						}
						//curve+=curvestep;
					}
				}else{
					// if it is flipped, everything goes the opposite way 'round
					if(o<vertx){
						//int limy = (int)(curve*(verty+invslope*(vertx-o)));
						double crv = Math.sin((Math.acos((double)(vertx-o)/(double)(vertx))/radstep)*radstep)*verty;
						int limy = (int)( (curveweight*crv + (1.0-curveweight)*(verty+invslope*(vertx-o))) );
						if(i>limy+mod){
							pass=false;
						}
						//curve+=curvestep;
					}else{
						int limy = (int)(-slope*(o-vertx)+verty);
						if(i>limy+mod){
							pass=false;
						}
					}
				}
				
				int rotx = (int)((nx * cosangle - ny*sinangle));
				int roty = (int)((ny * cosangle + nx*sinangle)); // the +1 here is a magic-number adjustment (i.e. empirical w/o theory supporting it)
				//pass=true;
				if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2 && pass){
					map[rotx+mcx][roty+mcy]=true;
				}
				o++;
			}
			i++;
		}

		// rotation like this will create holes in the solid body
		// we need to inflate to remove these holes
		boolean changed = true;
		while(changed){
			changed=false;
			i=0;
			while(i<(len+wid)*2){
				int o =0;
				while(o<(len+wid)*2){
					if(map[i][o]==false){
						int surr = 0; // surrounding trues
						if(i>0){
							if(map[i-1][o]==true){ surr++; }
						}
						if(o>0){
							if(map[i][o-1]==true){ surr++; }
						}
						if(i<(len+wid)*2-1){
							if(map[i+1][o]==true){ surr++; }
						}
						if(o<(len+wid)*2-1){
							if(map[i][o+1]==true){ surr++; }
						}
						if(surr>=3){
							// the algorithm here is basically to fill in spaces that have 3 or more filled neighbors
							// this works out because on the edges of the rectangle a space can only have 2 filled neighbors
							// but in the interior can have 3 or more
							map[i][o]=true;
							changed=true;
						}
					}
					o++;
				}
				i++;
			}
		}
		// now match up 'map' with the image raster
		// and plop 'color' down wherever map is true
		// center of the rotation should be centered over x,y
		int thrws = 0;
		i=0;
		while(i<(len+wid)*2){
			int o =0;
			while(o<(len+wid)*2){
				int realx = i - mcx + x;
				int realy = o - mcy + y;
				if(map[i][o]==true){
					try{
						raster.setPixel(realx,realy,col);
						idraster[realx][realy]=id;
					}catch(Exception e){
						if(thrws<5){
							if(debug){
								System.out.println("ERROR: OUT OF BOUNDS [" + realx + "," + realy + "]");
							}
						}
						thrws++;
					}
				}
				o++;
			}
			i++;
		}
	}
	
	// drawPoly(): used to draw polygons from a list of vertices, at some rotation.
	public static void drawPoly(WritableRaster raster, int x, int y, int[] invx, int[] invy, int[] ctype, double[] cweight,
			int vs, int[] col, double angle, int crotx, int croty, boolean filled, int[][] idraster, int id){
		//drawpoly
		int[] vx = new int[vs];
		int[] vy = new int[vs];
		int i =0;
		while(i<vs){ // this duplication is to avoid the original input array being modified
			vx[i]=invx[i];
			vy[i]=invy[i];
			i++;
		}
		//figure out the unrotated dimensions
		int len = 0;
		int wid = 0;
		int negx = 0;
		int posx = 0;
		int negy = 0;
		int posy = 0;
		// DISABLE: point rotation is disabled, moving to a new algorithm
		// instead, we will generate the whole polygon at 0deg and rotate the whole thing after
		// rotate the points
		/*
		i=0;
		while(i<vs){
			int ox = vx[i]-crotx; // old values
			int oy = vy[i]-croty;
			vx[i] = (int)(ox * Math.cos(angle) - oy*Math.sin(angle)); // set new values (rotated)
			vy[i] = (int)(oy * Math.cos(angle) + ox*Math.sin(angle));
			i++;
		}*/
		i=0;
		while(i<vs){
			if(vx[i]<negx){ negx = vx[i]; }
			if(vx[i]>posx){ posx = vx[i]; }
			if(vy[i]<negy){ negy = vy[i]; }
			if(vy[i]>posy){ posy = vy[i]; }
			i++;
		}
		len = Math.abs(negx)+posx;
		wid = Math.abs(negy)+posy;
		int mcx = len+wid;
		int mcy = wid+len;
		// map the polygon
		boolean[][] map = new boolean[mcx*2][mcy*2];
		boolean[][] map_fill = new boolean[mcx*2][mcy*2];
		i=0;
		while(i<mcx*2){
			int o=0;
			while(o<mcy*2){
				map[i][o]=false;
				map_fill[i][o]=false;
				o++;
			}
			i++;
		}
		// move the points to center
		i=0;
		while(i<vs){
			vx[i] = mcx+vx[i]; // set new values (rotated)
			vy[i] = mcy+vy[i];
			i++;
		}
		// draw the points
		i=0;
		while(i<vs){
			// draw lines from vertex to vertex
			int ox = vx[i]; // original point
			int oy = vy[i];
			int nx=0; // next point
			int ny=0;
			if(i+1 < vs){
				nx = vx[i+1];
				ny = vy[i+1];
			}else{ // if we're at the end, loop back to first point
				nx = vx[0];
				ny = vy[0];
			}
			double cx = ox; // current position ( tracking )
			double cy = oy; 
			double slope = 0;
			double xchange = 1;
			if(nx<cx){ 
				xchange = -1; // if we're going backwards in x, xchange should be negative   
			}
			if(nx==cx){
				xchange=0;
				slope = 1;
			}else{
				slope = (double)((ny-oy))/(double)((nx-ox)); // rise/run
			}
			if(ny<oy){
				slope=-1*Math.abs(slope);
			}else{
				slope=Math.abs(slope);
			}
			
			while(Math.abs(slope)>0.5){ // reduce slope so we don't skip any steps
				slope/=2;
				xchange/=2;
			}
			
			if(ctype[i]==0){
				int enough = -1; // once we are 'close enough' to the point, it'll stop shortly after
				while( ((int)(cx)!=nx || (int)(cy)!=ny  ) && cx<mcx*2 && cy<mcy*2 && cx>=0 && cy>=0 && enough!=0){
					map[(int)(cx)][(int)(cy)]=true;
					
					cy+=slope;
					cx+=xchange;
					if(enough>0){
						enough--;
					}
					if( ((int)(cx)-1== nx || (int)(cx)+1==nx || (int)(cx)==nx) && 
						((int)(cy)-1== ny || (int)(cy)+1==ny || (int)(cy)==ny) &&
						enough==-1){
						enough=1;   
					}
				}
			}else if(ctype[i]==1 || ctype[i]==2){
				// curve up or down
				//IMPLEMENTING A CURVE:
				// - ellipse = a * cos(phi), b * sin(phi)
				// a = horz distance
				// b = vert distance
				// should just iterate through phi from 0 to 90deg?
				
				
				map[ox][oy]=true;
				map[nx][ny]=true;
				
				double[] pack = curveOrientation((double)(ox),(double)(oy),(double)(nx),(double)(ny),ctype[i]);
				cx = pack[0];
				cy = pack[1];
				int xmod = (int)(pack[2]);
				int mod = (int)(pack[3]);
				int a = (int)(pack[4]);
				int b = (int)(pack[5]);
				int dist_x = (int)(pack[6]);
				int dist_y = (int)(pack[7]);
				double iang=0;
				double angend=90;
				
				int lastxpos=(int)(cx);
				int lastypos=(int)(cy);

				while(iang<=angend){
					double riang = Math.toRadians(iang);
					int xpos = (int)(cx+xmod*a*Math.cos(riang)+dist_x);
					int ypos = (int)(cy+mod*b*Math.sin(riang)+dist_y);
					double[] npos = Interpreter.curveWeight(xpos, ypos, ctype[i], ox, oy, nx, ny, cweight[i]);
					xpos = (int)(npos[0]);
					ypos = (int)(npos[1]);
					if(xpos>=0 && xpos<mcx*2 && ypos>=0 && ypos<mcy*2){
						map[xpos][ypos]=true;
						map=interp(map,lastxpos,lastypos,xpos,ypos);
					}
					lastxpos=xpos;
					lastypos=ypos;
					iang+=0.01;
				}
				int nextx = nx;
				int nexty = ny;
				if(cx==nx && cy==ny){
					nextx=ox;
					nexty=oy;
				}
				map=interp(map, lastxpos, lastypos, nextx, nexty);
			}

			i++;
		}
		//if(debug){
			//output_map(map,mcx*2,mcy*2,"poly1_" + x + " " + y + " " + invx[0] + " " + invy[0]);
		//}
		//after drawing all the lines the next necessary part is to fill in the shape
		// discovering how to fill in the polygon will be no easy task
		
		// 1. if a point has an odd number of lines on each side of it, it is 'inside'
		// 2. if a point has an even number of lines on each side, it is 'outside'
		
		//so:
		// 1. find an interior point
		// 2. grow that point until it encompasses the shape
		
		if(filled){
			int point_loops=0;
			boolean point_found=false;
			while(!point_found && point_loops<2000){
				int r=(int)(Math.random()*vs);
				int px = vx[r];
				int py = vy[r];
				px = px + (5 - (int)(Math.random()*10));
				py = py + (5 - (int)(Math.random()*10));
				if(px>=map.length){
					px=map.length-1;
				}
				if(px<0){ 
					px=0;
				}
				if(py>=map[0].length){
					py=map[0].length-1;
				}
				if(py<0){
					py=0;
				}
				//now that we have a random point
				//let's see if it's interior by scanning in each cardinal direction
				if(map[px][py]==false){
					int valids = 0;
					//pos x
					i=px;
					int count=0;
					boolean onblack=false;
					
					// this may be a little obtuse looking but the gist of it is:
					// when we find a filled region, that's a 'line'
					// we don't want to double count this region, so we turn off counting
					// until we hit unfilled space again (hence "onblack", which is
					// true when we're already in a filled space and if it is true we
					// don't increment the count value.

					while(i<mcx*2){
						if(!onblack && map[i][py]){
							count++;
							onblack=true;
						}else if (onblack && !map[i][py]){
							onblack=false;
						}
						i++;
					}
					if(count%2>0){
						valids++;
					}
					
					i=px; // neg x
					count=0;
					onblack=false;
					
					while(i>=0){
						if(!onblack && map[i][py]){
							count++;
							onblack=true;
						}else if (onblack && !map[i][py]){
							onblack=false;
						}
						i--;
					}
					if(count%2>0){
						valids++;
					}
					
					i=py; // pos y
					count=0;
					onblack=false;
					
					while(i<(len+wid)*2){
						if(!onblack && map[px][i]){
							count++;
							onblack=true;
						}else if (onblack && !map[px][i]){
							onblack=false;
						}
						i++;
					}
					if(count%2>0){
						valids++;
					}
					
					i=py; // neg y
					count=0;
					onblack=false;
					
					while(i>=0){
						if(!onblack && map[px][i]){
							count++;
							onblack=true;
						}else if (onblack && !map[px][i]){
							onblack=false;
						}
						i--;
					}
					if(count%2>0){
						valids++;
					}

					point_loops++;
					if(valids>=4){
						point_found=true;
						map_fill[px][py]=true;
						map[px][py]=true;
					}
				}
			}
				
			// now px py is a point on the inside
			//map_fill[px][py]=true;
			//map[px][py]=true;
			boolean changed=true;
			while(changed){ // repeat this process until no more changes are made to the map
				changed=false;
				i=0;
				while(i<mcx*2){ // loop through the whole x,y of the map
					int o=0;
					while(o<mcy*2){
						if(map_fill[i][o]){ // if this space is marked to fill, expand it to adjacent, empty spaces
							map_fill[i][o]=false;
							changed=true;
							if(i>0){
								if(map[i-1][o]==false){ // only expand into spaces that aren't already filled
									map[i-1][o]=true;
									map_fill[i-1][o]=true; // mark the expansion space to continue expanding
								}
							}
							if(o>0){
								if(map[i][o-1]==false){
									map[i][o-1]=true;
									map_fill[i][o-1]=true;
								}
							}
							if(i<(mcx*2)-1){
								if(map[i+1][o]==false){
									map[i+1][o]=true;
									map_fill[i+1][o]=true;
								}
							}
							if(o<(mcy*2)-1){
								if(map[i][o+1]==false){
									map[i][o+1]=true;
									map_fill[i][o+1]=true;
								}
							}
						}
						o++;
					}
					i++;
				}
			}
		}	// filled check
		//if(debug){
			//output_map(map,mcx*2,mcy*2,"poly2_" + x + " " + y + " " + invx[0] + " " + invy[0]);
		//}  
		
		
		double cosangle = Math.cos(angle);
		double sinangle = Math.sin(angle);
		if(Math.abs(cosangle)<0.01){ // this clamping is necessary because, for instance, math likes to return
			cosangle=0;				 // "1.22x10^-16" instead of "0" for angle = 180deg = pi
		}							 // which leads to slight, unwanted rotations at large distances from the pivot
		if(Math.abs(sinangle)<0.01){
			sinangle=0;
		}
		if(Math.abs(cosangle)>0.99){
			cosangle=cosangle/Math.abs(cosangle);
		}
		if(Math.abs(sinangle)>0.99){
			sinangle=sinangle/Math.abs(sinangle);
		}
		boolean[][] newmap = new boolean[mcx*2][mcy*2];
		int cx = mcx;
		int cy = mcy;
		i =0;
		while(i<mcx*2){
			int o =0;
			while(o<mcy*2){
				if(map[i][o]){
					int nx = i - cx;
					int ny = o - cy;
					
					int rotx = (int)((nx * cosangle - ny*sinangle));
					int roty = (int)((ny * cosangle + nx*sinangle));
					if(rotx+mcx>=0 && rotx+mcx <(len+wid)*2 && roty+mcy>=0 && roty+mcy<(len+wid)*2){
						newmap[rotx+mcx][roty+mcy]=true;
					}
				}
				o++;
			}
			i++;
		}
		map=newmap;
		if(filled){
			// rotation like this will create holes in the solid body
			// we need to inflate to remove these holes
			boolean changed = true;
			while(changed){
				changed=false;
				i=0;
				while(i<(len+wid)*2){
					int o =0;
					while(o<(len+wid)*2){
						if(map[i][o]==false){
							int surr = 0; // surrounding trues
							if(i>0){
								if(map[i-1][o]==true){ surr++; }
							}
							if(o>0){
								if(map[i][o-1]==true){ surr++; }
							}
							if(i<(len+wid)*2-1){
								if(map[i+1][o]==true){ surr++; }
							}
							if(o<(len+wid)*2-1){
								if(map[i][o+1]==true){ surr++; }
							}
							if(surr>=3){
								map[i][o]=true;
								changed=true;
							}
						}
						o++;
					}
					i++;
				}
			}
		}
		
		// now match up 'map' with the image raster
		// and plop 'color' down wherever map is true
		// center of the rotation should be centered over x,y
		int failures = 0;
		i=0;
		while(i<mcx*2){
			int o =0;
			while(o<mcy*2){
				int realx = i - mcx + x;
				int realy = o - mcy + y;
				if(map[i][o]==true){
					try{
						raster.setPixel(realx,realy,col);
						idraster[realx][realy]=id;
					}catch(Exception e){
						if(failures<10){
							System.out.println("ERROR: OUT OF BOUNDS [" + realx + "," + realy + "]");
						}
						failures++;
					}
				}
				o++;
			}
			i++;
		}
		if(failures>10){
			System.out.println("FAILURES:: " + failures);
		}
		
	}
	
	// drawCircle(): used to draw circles
	public static void drawCircle(WritableRaster raster, int cx, int cy, int rad, int[] col, int[][] idraster, int id){
		int x = cx-rad;
		int y = cy-rad;
		int w = rad*2;
		int h = rad*2;
		int i =x;
		while(i<x+w && i<raster.getWidth()){
			int o=y;
			while(o<y+h && o<raster.getHeight()){
				int dx = cx-i;
				int dy = cy-o;
				int dx2 = dx*dx;
				int dy2 = dy*dy;
				if(Math.sqrt(dx2+dy2)<=rad){
					try{
						raster.setPixel(i,o,col);
						idraster[i][o]=id;
					}catch(Exception e){
						System.out.println("ERROR: OUT OF BOUNDS [" + i + "," + o + "]");
					}
				}
				o++;
			}
			i++;
		} 
	}
	
	// drawDisk(): used to draw disks (circles with some amount of interior missing)
	public static void drawDisk(WritableRaster raster, int cx, int cy, int rad, int inrad, int[] col, int[][] idraster, int id){
		int x = cx-rad;
		int y = cy-rad;
		int w = rad*2;
		int h = rad*2;
		int i =x;
		while(i<x+w && i<raster.getWidth()){
			int o=y;
			while(o<y+h && o<raster.getHeight()){
				int dx = cx-i;
				int dy = cy-o;
				int dx2 = dx*dx;
				int dy2 = dy*dy;
				if(Math.sqrt(dx2+dy2)<=rad && Math.sqrt(dx2+dy2)>=inrad){
					try{
						raster.setPixel(i,o,col);
						idraster[i][o]=id;
					}catch(Exception e){
						System.out.println("ERROR: OUT OF BOUNDS [" + i + "," + o + "]");
					}
				}
				o++;
			}
			i++;
		} 
	}
	
	// outputMap(): used for debugging to show fragmented pieces of nodes
	public static void outputMap(boolean[][] omap, int x, int y, String name){
		BufferedImage image = new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = image.getRaster();
		
		int i=0;
		while(i<x){
			int o=0;
			while(o<y){
				try{
					int[] col = new int[3];
					if(omap[i][o]){
						col[0]=0; col[1]=0; col[2]=0;
					}else{
						col[0]=255; col[1]=255; col[2]=255;
					}
					raster.setPixel(i,o,col);
				}catch(Exception e){
					
				}
				o++;
			}
			i++;
		}
			
		/*System.out.println("SAVING IMAGE AT ./out/temp_map_" + name + ".png");
		try{
			ImageIO.write(image, "PNG", new File("./out/temp_map_" + name + ".png"));
		}catch(IOException ioe){}*/
		
	}
	
	// curveOrientation(): used in polygon formation to determine how curvature is applied
	public static double[] curveOrientation(double ox, double oy, double nx, double ny, int ctype){
		// spits out:
		// [0] = cx
		// [1] = cy
		// [2] = xmod
		// [3] = mod
		// [4] = a
		// [5] = b
		// [6] = dist_x
		// [7] = dist_y
		
		int mod = 1;
		if(ctype==2){
			mod=-1;
		}
		double a = nx-ox;
		double b = ny-oy;
		
		int xmod = 1;

		if(ox<nx){
			xmod=-1*mod;
		}
		if(ox>nx){
			xmod=-1*mod;
			if(oy<ny){
				xmod*=-1;	
			}
		}
		if(oy<ny){
			mod*=-1;
		}

		// alternate solution: 
		double dist_x = 0;
		double dist_y = 0;
		
		dist_x = (xmod*a*Math.cos(Math.toRadians(0))) * -1;
		dist_y = (mod*b*Math.sin(Math.toRadians(0))) * -1;
		double cx = ox;
		double cy = oy;
		
		if((xmod*a*Math.cos(Math.toRadians(45)) < 0 && nx<ox) || (xmod*a*Math.cos(Math.toRadians(45)) > 0 && nx>ox)){
			cx=nx;
			cy=ny;
			if((mod*b*Math.sin(Math.toRadians(45)) < 0 && ny<oy) || (mod*b*Math.sin(Math.toRadians(45)) > 0 && ny>oy)){
				// if ny<oy, we need to be going positive (cy = ny now)
				mod*=-1;
			}
		}else{
			if((mod*b*Math.sin(Math.toRadians(45)) < 0 && ny>oy) || (mod*b*Math.sin(Math.toRadians(45)) > 0 && ny<oy)){
				mod*=-1;
			}
		}
		
		double[] pack = new double[8];
		pack[0]=cx;
		pack[1]=cy;
		pack[2]=xmod;
		pack[3]=mod;
		pack[4]=a;
		pack[5]=b;
		pack[6]=dist_x;
		pack[7]=dist_y;
		return pack;
	}
	
	// curveWeight(): used in polygon formation to produce proper curves
	public static double[] curveWeight(double cx, double cy, double ctype,
			double ox, double oy, double nx, double ny, double cweight){
		double dx = nx-ox;
		double dy = ny-oy;
		
		
		
		double[] ret = new double[4];
		ret[0]=cx;
		ret[1]=cy;
		ret[2]=0;
		ret[3]=0;
		if(cweight!=1){
			//System.out.println("GETTING CURVE c.. " + cx + " " + cy + " o.. " + ox + " " + oy + " n.. " + nx + " " + ny + " cw.. " + cweight);
			if(dx!=0){
				if(dy!=0){
					double slope=dy/dx;
					double oslope = -dx/dy;//Math.abs(dx/dy);
					ret[2]=slope;
					ret[3]=oslope;
					/*if(ctype==1){ 
						oslope*=-1; 
						// if the curve line is above the straight line, should be going negative
						// as to approach the straight line.
					}*/
					//System.out.println(" SLOPE " + slope + " o " + oslope);
					// find yints
					double yintc = cy - oslope*cx;
					double yinto = oy - slope*ox;
					
					double intx = (yinto-yintc)/(oslope-slope);
					double inty = oslope*intx+yintc;
					//intx+=ox;
					//System.out.println(" INT " + intx + " " + inty);
					
					ret[0] = (intx*(1-cweight))+(cx*cweight);
					ret[1] = (inty*(1-cweight))+(cy*cweight);
					//System.out.println(" RET " + ret[0] + " " + ret[1]);
				}else{
					// special case, slope is horizontal
					// so curve is same as straight line
				}
			}else{
				// special case
				// the slope is vertical
				// curve will be same as straight line
			}
		}
		return ret;
	}
	
	// interp(): used in polygon formation to draw lines between vertices
	public static boolean[][] interp(boolean[][] map, int ox, int oy, int nx, int ny){
		// takes in a boolean map
		// starts from ox and oy, goes to nx and ny in a straight line, setting true
		int dx = nx-ox;
		int dy = ny-oy;
		if(dy!=0){
			if(dx!=0){
				double slope = Math.abs((double)(dy)/(double)(dx))*Math.signum(dy);
				double xmod = Math.signum(dx);
				while(Math.abs(slope)>0.25){
					slope/=2;
					xmod/=2;
				}
				double cx = ox;
				double cy = oy;
				int realx = (int)(cx);
				int realy = (int)(cy);
				while((realx!=nx || realy!=ny) && 
						(Math.signum(dx)==Math.signum(nx-realx) || 
						 Math.signum(dy)==Math.signum(ny-realy))){
					cx+=xmod;
					cy+=slope;
					realx = (int)(cx);
					realy = (int)(cy);
					if(realx>=0 && realx<map.length && realy>=0 && realy<map[0].length){
						map[realx][realy]=true;
					}
				}
			}else{
				// special case: vert line
				while(oy!=ny){
					if(ox>=0 && ox<map.length && oy>=0 && oy<map[0].length){
						map[ox][oy]=true;
					}
					oy+=Math.signum(dy);
				}
			}
		}else{
			// special case: horz line
			while(ox!=nx){
				if(ox>=0 && ox<map.length && oy>=0 && oy<map[0].length){
					map[ox][oy]=true;
				}
				ox+=Math.signum(dx);
			}
		}
		return map;
	}
	
	// drawInterp() is the same as above but affects rasters instead of boolean maps
	public static void drawInterp(WritableRaster raster, int[] col, int ox, int oy, int nx, int ny){
		// takes in a boolean map
		// starts from ox and oy, goes to nx and ny in a straight line, setting true
		int dx = nx-ox;
		int dy = ny-oy;
		if(dy!=0){
			if(dx!=0){
				double slope = Math.abs((double)(dy)/(double)(dx))*Math.signum(dy);
				double xmod = Math.signum(dx);
				while(Math.abs(slope)>0.5){
					slope/=2;
					xmod/=2;
				}
				double cx = ox;
				double cy = oy;
				int realx = (int)(cx);
				int realy = (int)(cy);
				while(realx!=nx || realy!=ny){
					cx+=xmod;
					cy+=slope;
					realx = (int)(cx);
					realy = (int)(cy);
					if(realx>=0 && realx<raster.getWidth() && realy>=0 && realy<raster.getHeight()){
						raster.setPixel(realx, realy, col);
					}
				}
			}else{
				// special case: vert line
				while(oy!=ny){
					if(ox>=0 && ox<raster.getWidth() && oy>=0 && oy<raster.getHeight()){
						raster.setPixel(ox, oy, col);
					}
					oy+=Math.signum(dy);
				}
			}
		}else{
			// special case: horz line
			while(ox!=nx){
				if(ox>=0 && ox<raster.getWidth() && oy>=0 && oy<raster.getHeight()){
					raster.setPixel(ox, oy, col);
				}
				ox+=Math.signum(dx);
			}
		}
	}
	//public static void drawText(BufferedImage image, int[] col, int ox, int oy, String text){
		// TODO
	//}
	
	// setPreviewMode(): simply used to set whether or not this generation is a preview (if it is,
	// turns off various features for the sake of speed).
	public static void setPreviewMode(boolean inb){
		previewmode=inb;
		if(previewmode){
			olddebug=debug;
			debug=false;
		}else{
			debug=olddebug;
		}
	}
	
	// getLayerFromZList(): takes in a number of layers to search for and then returns a list containing
	// all of the layer ids of the layers in the device (for example if the layers present in the device were:
	// -3, 0, 5, 6, and 10, it would return int[]{-3,0,5,6,10}
	public static int[] getLayerFromZList(int layers){
		int[] out = new int[layers];
		int i=0;
		while(i<layers){
			out[i]=-99999;
			i++;
		}
		int layercount=0;
		boolean repeat = false;
		i=0;
		while(i<nodecount && layercount < layers){
			repeat=false;
			MapNode mn = allnodes[i];
			int z = mn.getFillZ();
			
			int o=0;
			while(o<layercount){
				if(out[o]==z){
					repeat=true;
					break;
				}
				o++;
			}
			if(repeat==false){
				out[layercount]=z;
				layercount++;
			}
			i++;
		}
		i=layercount;
		while(i<layers){
			out[i]=0; // fill up excess slots
			i++;
		}
		return out;
	}
	
	// cleanImage(): takes in an image raster and cleans it by removing stray pixels
	// a final safeguard against mistakes made by the interpreter.
	public static void cleanImage(WritableRaster raster){
		int[] col = new int[3];
		int[] scol = new int[3];
		int i=0;
		while(i<raster.getWidth()){
			int o=0;
			while(o<raster.getHeight()){
				boolean skip=false;
				col = raster.getPixel(i, o, col);
				if(i>0){
					scol = raster.getPixel(i-1,o,scol);
					if(col[0] == scol[0] && col[1] == scol[1] && col[2] == scol[2]){
						skip=true;
					}
				}
				if(o>0 && !skip){
					scol = raster.getPixel(i,o-1,scol);
					if(col[0] == scol[0] && col[1] == scol[1] && col[2] == scol[2]){
						skip=true;
					}
				}
				if(i<raster.getWidth()-1 && !skip){
					scol = raster.getPixel(i+1,o,scol);
					if(col[0] == scol[0] && col[1] == scol[1] && col[2] == scol[2]){
						skip=true;
					}
				}
				if(o<raster.getHeight()-1 && !skip){
					scol = raster.getPixel(i,o+1,scol);
					if(col[0] == scol[0] && col[1] == scol[1] && col[2] == scol[2]){
						skip=true;
					}
				}
				if(!skip){
					raster.setPixel(i,o,scol);
				}
				
				o++;
			}
			i++;
		}
	}
}
