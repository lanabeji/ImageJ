package ij;

import ij.gui.ProgressBar;
import ij.io.LogStream;
import ij.macro.Interpreter;
import ij.plugin.Macro_Runner;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.text.TextPanel;

import java.awt.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

import static ij.IJ.*;

public class OSvalidation {

    /** SansSerif, plain, 10-point font */
    public static Font font10 = new Font("SansSerif", Font.PLAIN, 10);
    /** SansSerif, plain, 12-point font */
    public static Font font12 = ImageJ.SansSerif12;

    /** Image display modes */
    public static final int COMPOSITE=1, COLOR=2, GRAYSCALE=3;

    public static final String URL = "http://imagej.nih.gov/ij";
    public static final int ALL_KEYS = -1;

    /** Use setDebugMode(boolean) to enable/disable debug mode. */
    public static boolean debugMode;

    public static boolean hideProcessStackDialog;

    public static final char micronSymbol = '\u00B5';
    public static final char angstromSymbol = '\u00C5';
    public static final char degreeSymbol = '\u00B0';

    private static ImageJ ij;
    private static java.applet.Applet applet;
    private static ProgressBar progressBar;
    private static TextPanel textPanel;
    private static String osname, osarch;
    private static boolean isMac, isWin, isLinux, is64Bit;
    private static int javaVersion;
    private static boolean controlDown, altDown, spaceDown, shiftDown;
    private static boolean macroRunning;
    private static Thread previousThread;
    private static TextPanel logPanel;
    private static ClassLoader classLoader;
    private static boolean memMessageDisplayed;
    private static long maxMemory;
    private static boolean escapePressed;
    private static boolean redirectErrorMessages;
    private static boolean suppressPluginNotFoundError;
    private static Hashtable commandTable;
    private static Vector eventListeners = new Vector();
    private static String lastErrorMessage;
    private static Properties properties;	private static DecimalFormat[] df;
    private static DecimalFormat[] sf;
    private static DecimalFormatSymbols dfs;
    private static boolean trustManagerCreated;
    private static String smoothMacro;
    private static Interpreter macroInterpreter;
    private static boolean protectStatusBar;
    private static Thread statusBarThread;

    static {
        osname = System.getProperty("os.name");
        isWin = osname.startsWith("Windows");
        isMac = !isWin && osname.startsWith("Mac");
        isLinux = osname.startsWith("Linux");
        String version = System.getProperty("java.version");
        if (version==null || version.length()<2)
            version = "1.8";
        if (version.startsWith("1.8"))
            javaVersion = 8;
        else if (version.charAt(0)=='1' && Character.isDigit(version.charAt(1)))
            javaVersion = 10 + (version.charAt(1) - '0');
        else if (version.charAt(0)=='2' && Character.isDigit(version.charAt(1)))
            javaVersion = 20 + (version.charAt(1) - '0');
        else if (version.startsWith("1.6"))
            javaVersion = 6;
        else if (version.startsWith("1.9")||version.startsWith("9"))
            javaVersion = 9;
        else if (version.startsWith("1.7"))
            javaVersion = 7;
        else
            javaVersion = 8;
        dfs = new DecimalFormatSymbols(Locale.US);
        df = new DecimalFormat[10];
        df[0] = new DecimalFormat("0", dfs);
        df[1] = new DecimalFormat("0.0", dfs);
        df[2] = new DecimalFormat("0.00", dfs);
        df[3] = new DecimalFormat("0.000", dfs);
        df[4] = new DecimalFormat("0.0000", dfs);
        df[5] = new DecimalFormat("0.00000", dfs);
        df[6] = new DecimalFormat("0.000000", dfs);
        df[7] = new DecimalFormat("0.0000000", dfs);
        df[8] = new DecimalFormat("0.00000000", dfs);
        df[9] = new DecimalFormat("0.000000000", dfs);
        df[0].setRoundingMode(RoundingMode.HALF_UP);
    }




    static void cleanup() {
        ij=null; applet=null; progressBar=null; textPanel=null;
    }

    /**Returns a reference to the "ImageJ" frame.*/
    public static ImageJ getInstance() {
        return ij;
    }

    /**Enable/disable debug mode.*/
    public static void setDebugMode(boolean b) {
        debugMode = b;
        LogStream.redirectSystem(debugMode);
    }

    /** Runs the macro contained in the string <code>macro</code>
     on the current thread. Returns any string value returned by
     the macro, null if the macro does not return a value, or
     "[aborted]" if the macro was aborted due to an error. The
     equivalent macro function is eval(). */
    public static String runMacro(String macro) {
        return runMacro(macro, "");
    }

    /** Runs the macro contained in the string <code>macro</code>
     on the current thread. The optional string argument can be
     retrieved in the called macro using the getArgument() macro
     function. Returns any string value returned by the macro, null
     if the macro does not return a value, or "[aborted]" if the
     macro was aborted due to an error.  */
    public static String runMacro(String macro, String arg) {
        Macro_Runner mr = new Macro_Runner();
        return mr.runMacro(macro, arg);
    }

    /** Runs the specified macro or script file in the current thread.
     The file is assumed to be in the macros folder
     unless <code>name</code> is a full path.
     The optional string argument (<code>arg</code>) can be retrieved in the called
     macro or script using the getArgument() function.
     Returns any string value returned by the macro, or null. Scripts always return null.
     The equivalent macro function is runMacro(). */
    public static String runMacroFile(String name, String arg) {
        Macro_Runner mr = new Macro_Runner();
        return mr.runMacroFile(name, arg);
    }

    /** Runs the specified macro file. */
    public static String runMacroFile(String name) {
        return runMacroFile(name, null);
    }

    /** Runs the specified plugin using the specified image. */
    public static Object runPlugIn(ImagePlus imp, String className, String arg) {
        if (imp!=null) {
            ImagePlus temp = WindowManager.getTempCurrentImage();
            WindowManager.setTempCurrentImage(imp);
            Object o = runPlugIn("", className, arg);
            WindowManager.setTempCurrentImage(temp);
            return o;
        } else
            return runPlugIn(className, arg);
    }

    /** Runs the specified plugin and returns a reference to it. */
    public static Object runPlugIn(String className, String arg) {
        return runPlugIn("", className, arg);
    }

    /** Runs the specified plugin and returns a reference to it. */
    public static Object runPlugIn(String commandName, String className, String arg) {
        if (arg==null) arg = "";
        if (IJ.debugMode)
            log("runPlugIn: "+className+argument(arg));
        // Load using custom classloader if this is a user
        // plugin and we are not running as an applet
        if (!className.startsWith("ij.") && applet==null)
            return runUserPlugIn(commandName, className, arg, false);
        Object thePlugIn=null;
        try {
            Class c = Class.forName(className);
            thePlugIn = c.newInstance();
            if (thePlugIn instanceof PlugIn)
                ((PlugIn)thePlugIn).run(arg);
            else
                new PlugInFilterRunner(thePlugIn, commandName, arg);
        } catch (ClassNotFoundException e) {
            if (!(className!=null && className.startsWith("ij.plugin.MacAdapter"))) {
                log("Plugin or class not found: \"" + className + "\"\n(" + e+")");
                String path = Prefs.getCustomPropsPath();
                if (path!=null);
                log("Error may be due to custom properties at " + path);
            }
        }
        catch (InstantiationException e) {log("Unable to load plugin (ins)");}
        catch (IllegalAccessException e) {log("Unable to load plugin, possibly \nbecause it is not public.");}
        redirectErrorMessages = false;
        return thePlugIn;
    }

    static Object runUserPlugIn(String commandName, String className, String arg, boolean createNewLoader) {
        if (IJ.debugMode)
            log("runUserPlugIn: "+className+", arg="+argument(arg));
        if (applet!=null) return null;
        if (createNewLoader)
            classLoader = null;
        ClassLoader loader = getClassLoader();
        Object thePlugIn = null;
        try {
            thePlugIn = (loader.loadClass(className)).newInstance();
            if (thePlugIn instanceof PlugIn)
                ((PlugIn)thePlugIn).run(arg);
            else if (thePlugIn instanceof PlugInFilter)
                new PlugInFilterRunner(thePlugIn, commandName, arg);
        }
        catch (ClassNotFoundException e) {
            if (className.startsWith("macro:"))
                runMacro(className.substring(6));
            else if (className.contains("_")  && !suppressPluginNotFoundError)
                error("Plugin or class not found: \"" + className + "\"\n(" + e+")");
        }
        catch (NoClassDefFoundError e) {
            int dotIndex = className.indexOf('.');
            if (dotIndex>=0 && className.contains("_")) {
                // rerun plugin after removing folder name
                if (debugMode) log("runUserPlugIn: rerunning "+className);
                return runUserPlugIn(commandName, className.substring(dotIndex+1), arg, createNewLoader);
            }
            if (className.contains("_") && !suppressPluginNotFoundError)
                error("Run User Plugin", "Class not found while attempting to run \"" + className + "\"\n \n   " + e);
        }
        catch (InstantiationException e) {error("Unable to load plugin (ins)");}
        catch (IllegalAccessException e) {error("Unable to load plugin, possibly \nbecause it is not public.");}
        if (thePlugIn!=null && !"HandleExtraFileTypes".equals(className))
            redirectErrorMessages = false;
        suppressPluginNotFoundError = false;
        return thePlugIn;
    }

    private static String argument(String arg) {
        return arg!=null && !arg.equals("") && !arg.contains("\n")?"(\""+arg+"\")":"";
    }

    static void wrongType(int capabilities, String cmd) {
        String s = "\""+cmd+"\" requires an image of type:\n \n";
        if ((capabilities&PlugInFilter.DOES_8G)!=0) s +=  "    8-bit grayscale\n";
        if ((capabilities&PlugInFilter.DOES_8C)!=0) s +=  "    8-bit color\n";
        if ((capabilities&PlugInFilter.DOES_16)!=0) s +=  "    16-bit grayscale\n";
        if ((capabilities&PlugInFilter.DOES_32)!=0) s +=  "    32-bit (float) grayscale\n";
        if ((capabilities&PlugInFilter.DOES_RGB)!=0) s += "    RGB color\n";
        error(s);
    }
}
