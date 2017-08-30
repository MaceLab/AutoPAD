package autopad;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.swt.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.*;

public class AutoPAD {
	  
	private static Shell shell;
	private static Display display;
	private static Text targettext;
	private static String lastvalidinpath="";

	public static void main(String[] args) {
		// This builds the simple interface
		
		display = new Display();
		shell = new Shell(display);
		shell.setText("AutoPAD Interpreter");
		GridLayout glay = new GridLayout();
		GridData gd = new GridData();
		shell.setLayout(glay);
		gd.grabExcessHorizontalSpace=true;
		gd.horizontalAlignment = SWT.FILL;
		gd.verticalAlignment = SWT.FILL;
		
		// Font preparation work:
		FontData[] fd = shell.getFont().getFontData(); 
		int i = 0;
		while (i < fd.length) { 
			// In some cases, multiple fonts may be returned; this accounts for that scenario
			fd[i].setHeight(10);
			i++;
		}
		Composite textcomp = new Composite(shell,SWT.NULL);
		textcomp.setLayout(new RowLayout());
		textcomp.setLayoutData(gd);
		
		Font font10 = new Font(display, fd);
		// textbox, holds path to the script to be run
		final Text digesttext = new Text(textcomp, SWT.BORDER);
		digesttext.setFont(font10);
		digesttext.setLayoutData(new RowData(350,25));
		
		Button runbutton = new Button(textcomp, SWT.PUSH);
		runbutton.setText("Open");
		runbutton.setFont(font10);
		runbutton.setLayoutData(new RowData(75,30));
		
		// when pressed, the open button provides the path of the file that will be 
		// run and puts it into the digesttext.
		runbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				FileDialog filedialog = new FileDialog(shell, SWT.OPEN); // see above
				String inpath = targettext.getText();
				// if a valid digesttext path has been selected already, start the file browser at that directory
				// otherwise start in the directory of the targettext
				if(!lastvalidinpath.equalsIgnoreCase("")){ 
					inpath=lastvalidinpath;
				}
				filedialog.setFilterPath(inpath);
				filedialog.setText("Open Script");
				String[] fe = new String[2];
				fe[0] = "*.txt";
				fe[1] = "*.*";
				filedialog.setFilterExtensions(fe);
				String path = filedialog.open();
				if(path!=null){
					digesttext.setText(path);
					lastvalidinpath=path; // if a valid path was received, start there next time
				}
			}
		});
		
		Composite textcomp2 = new Composite(shell,SWT.NULL);
		textcomp2.setLayout(new RowLayout());
		textcomp2.setLayoutData(gd);
		
		
		targettext = new Text(textcomp2, SWT.BORDER);
		targettext.setFont(font10);
		targettext.setLayoutData(new RowData(350,25));

		// defaults the target directory to the 'out' folder in the directory where this program is located
		String tarstr = System.getProperty("user.dir");
		if(tarstr.contains("\\")){ // different OS systems use different slash conventions
			tarstr=tarstr+"\\out\\";
		}else{
			tarstr=tarstr+"/out/";
		}
		targettext.setText(tarstr);
		
		Button targetbutton = new Button(textcomp2, SWT.PUSH);
		targetbutton.setText("Target");
		targetbutton.setFont(font10);
		targetbutton.setLayoutData(new RowData(75,30));
		
		// when the target button is pressed, open the directory browser and allow a new output 
		// directory to be selected by the user.
		targetbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				DirectoryDialog dirdialog = new DirectoryDialog(shell);
				dirdialog.setFilterPath(targettext.getText());
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
					targettext.setText(newdir);
				}
			}
		});
		
		Button digestbutton = new Button(shell, SWT.PUSH);
		digestbutton.setText(" Run ");
		digestbutton.setFont(font10);
		digestbutton.setLayoutData(gd);

		// when the digestbutton is pressed, load the script in digesttext and run it, outputting
		// the files into targettext.
		digestbutton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if(digesttext.getText().length()>0){
					Interpreter.setTarget(targettext.getText());
					Interpreter.digest(digesttext.getText());
					Interpreter.flush();
				}
				// this function tries to open the selected script file
			}
		});
		
		// open the "auto.txt" and digest all files within upon running the program
		File fauto = new File("./auto.txt");
		FileReader fis;
		BufferedReader bis;
		try{
			fis = new FileReader(fauto);
			bis = new BufferedReader(fis);
			while(bis.ready()){
				String text = bis.readLine();
				if(text.length()>0){
					if(!text.substring(0,1).equalsIgnoreCase("\"")){ // ignore lines starting with a quotation
						Interpreter.digest(text);
						digesttext.setText(System.getProperty("user.dir") + "\\" + text);
						Interpreter.flush();
					}
				}
			}
		}catch(Exception e){
			System.out.println("!!! Failed to load auto.txt");
		}  
		
		// pack the shell, set the window size and open it up
		shell.pack();
		shell.setSize(470, 170);
		shell.open();
		// now wait for the shell to be X'd out of and close the program when that happens
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

}
