/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *
 *  Produce simplified form from ISH file.
 *
 *       Parameters:
 *           1st = Input File Name
 *           2nd = Output File Name
 *           3rd = Logging level
 *           4th = Logging Filter #1
 *           5th = Logging Filter #2
 *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Date:       Developer  PR/CR #   Description of changes
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *
 * 03/10/2011  ras        ????      Created.
 *
 * 06/06/2012  ras        ????      Added AW1-AW4 (Automated Present Weather)
 *                                  Corrected problem when OC1 was missing
 *
 * 06/21/2012  ras        ????      Modified Wind Dir logic to set value to 990 when
 *                                  Type code is 'V'
 *                                  Added MW4 (Manual Present Weather)
 *
 * 03/24/2015  ras        ????      Added GDx logic to fill in Sky Cover info when GF1
 *                                  is missing.
 *
 * 05/04/2016  ras        ????      May The Fourth be with you as you seek and destroy
 *                                  the bug in the AT1 element that causes the false
 *                                  detection of AWx, AUx, and MWx elements.
 *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

import java.io.*;
//import java.lang.Math.*;
import java.util.Date;
import java.util.*;
//import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/**
 *   Mainline logic.
 */
public class ishJava
{
    static String sProgramName      = "ishJava.java";
    static String sDebugName        = "ishJava";
    static String sInFileName       = "";
    static String sOutFileName      = "";
    static FileOutputStream fDebug  = null;

    static boolean bVerbose         = false;
    static boolean bOnce            = false;
    static boolean bStdErr          = false;

    static String sOutputRecord     = "";
    static String sControlSection   = "";
    static String sMandatorySection = "";
    static int    iCounter          = 0;
    static int    iLength           = 0;
    static int    iOffset           = 25;
    static int    iObsKey           = 0;
    static int    iWork             = 0;
    static String[] sWW1234;
    static String[] sAW1234;
    static float  fWork             = 0;
    static float  fWorkSave         = 0;
    static String sConcat           = "";
    static String sConcatDate       = "";
    static String sConcatMonth      = "";
    static String sMessage          = "";

    static final int    iPROD           = 0;                   // Only print basic run info
    static final int    iDEBUG          = 1;                   // Print lots of info, such as debug messages
    static int          iLogLevel       = 0;                   // Default value for this run.
    static String       p_sFilter1      = "None";
    static String       p_sFilter2      = "None";

    static NumberFormat  fmt03 = new DecimalFormat("000");         // 3-char int   (keep leading zeros)
    static NumberFormat fmt4_1 = new DecimalFormat("#0.0");        // 4-char float
    static NumberFormat fmt6_1 = new DecimalFormat("###0.0");      // 6-char float
    static NumberFormat fmt5_2 = new DecimalFormat("##0.00");      // 5-char float
    static NumberFormat  fmt02 = new DecimalFormat("#0");          // 2-char int

    //  Fields making up the Control Data Section.
    static String sCDS          = "";
    static String sCDS_Fill1    = "";
    static String sCDS_ID       = "";
    static String sCDS_Wban     = "";
    static String sCDS_Year     = "";
    static String sCDS_Month    = "";
    static String sCDS_Day      = "";
    static String sCDS_Hour     = "";
    static String sCDS_Minute   = "";
    static String sCDS_Source   = "";
    static String sCDS_Fill2    = "";
    static String sCDS_Type     = "";
    static String sCDS_Fill3    = "";
    //  Fields making up the Mandatory Data Section.
    static String sMDS          = "";
    static String sMDS_Dir      = "";
    static String sMDS_DirQ     = "";
    static String sMDS_DirType  = "";
    static String sMDS_Spd      = "";
    static String sMDS_Fill2    = "";
    static String sMDS_Clg      = "";
    static String sMDS_Fill3    = "";
    static String sMDS_Vsb      = "";
    static String sMDS_Fill4    = "";
    static String sMDS_TempSign = "";
    static String sMDS_Temp     = "";
    static String sMDS_Fill5    = "";
    static String sMDS_DewpSign = "";
    static String sMDS_Dewp     = "";
    static String sMDS_Fill6    = "";
    static String sMDS_Slp      = "";
    static String sMDS_Fill7    = "";

    //  REM offset
    static int    iREM_IndexOf  = 0;

    //  Fields making up the OC1 element
//    Sample Element=[OC1...]
    static int    iOC1_IndexOf  = 0;
    static int    iOC1_Length   = 8;
    static String sOC1          = "";
    static String sOC1_Fill1    = "";
    static String sOC1_Gus      = "";
    static String sOC1_Fill2    = "";

    //  Fields making up the GF1 element
    static int    iGF1_IndexOf  = 0;
    static int    iGF1_Length   = 26;
    static String sGF1          = "";
    static String sGF1_Fill1    = "";
    static String sGF1_Skc      = "";
    static String sGF1_Fill2    = "";
    static String sGF1_Low      = "";
    static String sGF1_Fill3    = "";
    static String sGF1_Med      = "";
    static String sGF1_Fill4    = "";
    static String sGF1_Hi       = "";
    static String sGF1_Fill5    = "";

    //  Fields making up the GDx element (Only need 1st 6 bytes) // 03/24/2015  ras
    static int    iGDx_IndexOf  = 0;
    static int    iGDx_Length   = 6;
    static String sGDx          = "";   // Length 6
    static String sGDx_Cov1     = "";   // Length 1
    static String sGDx_Cov2     = "";   // Length 2
    static String sGDx_Skc      = "";

    //    static int    iMW_Counter   = 0;
//  Fields making up the MW1-7 elements
    static int    iMW1_IndexOf  = 0;
    static int    iMW1_Length   = 6;
    static String sMW1          = "";
    static String sMW1_Fill1    = "";
    static String sMW1_Ww       = "";
    static String sMW1_Fill2    = "";

    static int    iMW2_IndexOf  = 0;
    static int    iMW2_Length   = 6;
    static String sMW2          = "";
    static String sMW2_Fill1    = "";
    static String sMW2_Ww       = "";
    static String sMW2_Fill2    = "";

    static int    iMW3_IndexOf  = 0;
    static int    iMW3_Length   = 6;
    static String sMW3          = "";
    static String sMW3_Fill1    = "";
    static String sMW3_Ww       = "";
    static String sMW3_Fill2    = "";

    static int    iMW4_IndexOf  = 0;
    static int    iMW4_Length   = 6;
    static String sMW4          = "";
    static String sMW4_Fill1    = "";
    static String sMW4_Ww       = "";
    static String sMW4_Fill2    = "";

    //  Fields making up the AY1 element
    static int    iAY1_IndexOf  = 0;
    static int    iAY1_Length   = 8;
    static String sAY1          = "";
    static String sAY1_Fill1    = "";
    static String sAY1_Pw       = "";
    static String sAY1_Fill2    = "";

    //  Fields making up the MA1 element
    static int    iMA1_IndexOf  = 0;
    static int    iMA1_Length   = 15;
    static String sMA1          = "";
    static String sMA1_Fill1    = "";
    static String sMA1_Alt      = "";
    static String sMA1_Fill2    = "";
    static String sMA1_Stp      = "";
    static String sMA1_Fill3    = "";

    // Max/Min fields
    static String sMaxTemp      = "";
    static String sMinTemp      = "";

    //  Fields making up the KA1 element
    static int    iKA1_IndexOf  = 0;
    static int    iKA1_Length   = 13;
    static String sKA1          = "";
    static String sKA1_Fill1    = "";
    static String sKA1_Code     = "";
    static String sKA1_Temp     = "";
    static String sKA1_Fill2    = "";

    //  Fields making up the KA2 element
    static int    iKA2_IndexOf  = 0;
    static int    iKA2_Length   = 13;
    static String sKA2          = "";
    static String sKA2_Fill1    = "";
    static String sKA2_Code     = "";
    static String sKA2_Temp     = "";
    static String sKA2_Fill2    = "";

    // Precip fields
    static String sPcp01        = "*****";
    static String sPcp01t       = " ";
    static String sPcp06        = "*****";
    static String sPcp06t       = " ";
    static String sPcp24        = "*****";
    static String sPcp24t       = " ";
    static String sPcp12        = "*****";
    static String sPcp12t       = " ";

    //  Fields making up the AA1 element
    static int    iAA1_IndexOf  = 0;
    static int    iAA1_Length   = 11;
    static String sAA1          = "";
    static String sAA1_Fill1    = "";
    static String sAA1_Hours    = "";
    static String sAA1_Pcp      = "";
    static String sAA1_Trace    = "";
    static String sAA1_Fill2    = "";

    //  Fields making up the AA2 element
    static int    iAA2_IndexOf  = 0;
    static int    iAA2_Length   = 11;
    static String sAA2          = "";
    static String sAA2_Fill1    = "";
    static String sAA2_Hours    = "";
    static String sAA2_Pcp      = "";
    static String sAA2_Trace    = "";
    static String sAA2_Fill2    = "";

    //  Fields making up the AA3 element
    static int    iAA3_IndexOf  = 0;
    static int    iAA3_Length   = 11;
    static String sAA3          = "";
    static String sAA3_Fill1    = "";
    static String sAA3_Hours    = "";
    static String sAA3_Pcp      = "";
    static String sAA3_Trace    = "";
    static String sAA3_Fill2    = "";

    //  Fields making up the AA4 element
    static int    iAA4_IndexOf  = 0;
    static int    iAA4_Length   = 11;
    static String sAA4          = "";
    static String sAA4_Fill1    = "";
    static String sAA4_Hours    = "";
    static String sAA4_Pcp      = "";
    static String sAA4_Trace    = "";
    static String sAA4_Fill2    = "";

    //  Fields making up the AJ1 element
    static int    iAJ1_IndexOf  = 0;
    static int    iAJ1_Length   = 17;
    static String sAJ1          = "";
    static String sAJ1_Fill1    = "";
    static String sAJ1_Sd       = "";
    static String sAJ1_Fill2    = "";

    //  Fields making up the AW1-4 elements
    static int    iAW1_IndexOf  = 0;
    static int    iAW1_Length   = 6;
    static String sAW1          = "";
    static String sAW1_Fill1    = "";
    static String sAW1_Zz       = "";
    static String sAW1_Fill2    = "";

    static int    iAW2_IndexOf  = 0;
    static int    iAW2_Length   = 6;
    static String sAW2          = "";
    static String sAW2_Fill1    = "";
    static String sAW2_Zz       = "";
    static String sAW2_Fill2    = "";

    static int    iAW3_IndexOf  = 0;
    static int    iAW3_Length   = 6;
    static String sAW3          = "";
    static String sAW3_Fill1    = "";
    static String sAW3_Zz       = "";
    static String sAW3_Fill2    = "";

    static int    iAW4_IndexOf  = 0;
    static int    iAW4_Length   = 6;
    static String sAW4          = "";
    static String sAW4_Fill1    = "";
    static String sAW4_Zz       = "";
    static String sAW4_Fill2    = "";

    static int    iAT1_IndexOf  = 0;
    static int    iAT2_IndexOf  = 0;
    static int    iAT3_IndexOf  = 0;
    static int    iAT4_IndexOf  = 0;
    static int    iAT5_IndexOf  = 0;
    static int    iAT6_IndexOf  = 0;
    static int    iAT7_IndexOf  = 0;
    static int    iAT8_IndexOf  = 0;
    static int    iATx_Length   = 12;
    static int    iATx_Cut      = 0;

    static String sHeader       = "  USAF  WBAN YR--MODAHRMN DIR SPD GUS CLG SKC L M H  VSB "+
            "MW MW MW MW AW AW AW AW W TEMP DEWP    SLP   ALT    STP MAX MIN PCP01 "+
            "PCP06 PCP24 PCPXX SD\n";

    public static void main(String[] args)
    {
//        logIt(fDebug, iPROD, false, "---------------------------- Begin "+sProgramName);          // Append output to log.
//        logIt(fDebug, iPROD, false, "Number of args found=["+args.length+"]");                    // Append output to log.

// Process args
        if (args.length <= 1)
        {
            bStdErr=true;
            logIt(fDebug, iPROD, false, "Error. Input and Output filenames required.");            // Append output to log.
            System.exit(77);
        }

        if (args.length >= 2)
        {
            sInFileName     = args[0];
            sOutFileName    = args[1];
        }

        if (args.length >= 3)
        {
            if (args[2].equals("0") ||
                    args[2].equals("1"))
            {
                iLogLevel = Integer.parseInt(args[2]);                      // Safe to convert to int.
            }
            else
            {
                logIt(fDebug, iPROD, false, "Invalid log message level parameter=["+args[2]+"].  Must be 0 or 1.  Defaulting to ["+iLogLevel+"]");
            }
        }

        if (args.length >= 4)
        {
            p_sFilter1 = args[3];
        }

        if (args.length >= 5)
        {
            p_sFilter2 = args[4];
        }

//        sOutFileName  = sInFileName+".java.out";

        logIt(fDebug, iDEBUG, false, "        Input Filename=["+sInFileName+"]");                // Append output to log.
        logIt(fDebug, iDEBUG, false, "       Output Filename=["+sOutFileName+"]");               // Append output to log.
        logIt(fDebug, iDEBUG, false, "         Logging Level=["+iLogLevel+"]");                  // Append output to log.
        logIt(fDebug, iDEBUG, false, "1st Log Message Filter=["+p_sFilter1+"]");
        logIt(fDebug, iDEBUG, false, "2nd Log Message Filter=["+p_sFilter2+"]");
// End of args

        try
        {
            BufferedReader fInReader        = new BufferedReader(new FileReader(sInFileName));

            FileWriter fFixed               = new FileWriter(sOutFileName);
            BufferedWriter fFixedWriter     = new BufferedWriter(fFixed);

            fFixedWriter.write(sHeader);           // Put header into output file.

            try
            {
                String line     = null;
                String lineHold = null;

                while (( line = fInReader.readLine()) != null)
                {
                    iCounter++;
//                    iOffset         = 25;
                    iLength         = line.length();
//                    logIt(fDebug, iDEBUG, false, "Record # "+iCounter+" had iLength=["+iLength+"]");
//                    System.out.println(line);

// See where the REM section begins
                    iREM_IndexOf    = line.indexOf("REM");
                    if (iREM_IndexOf == -1)
                    {
                        iREM_IndexOf = 9999;      // If no REM section then set to high value
                    }

                    getCDS(line);   //  Fields making up the Control Data Section.

                    sConcat      = sCDS_ID+"-"+sCDS_Wban+"-"+sCDS_Year+"-"+sCDS_Month+"-"+sCDS_Day+" "+sCDS_Hour+":"+sCDS_Minute;
                    sConcatDate  = sCDS_Year+"-"+sCDS_Month+"-"+sCDS_Day;
                    sConcatMonth = sCDS_Year+"-"+sCDS_Month;


// =-=-=-=-=-=-=-=-=-=-=-=-=-= Filter out all but a certain station/date =-=-=-=-=-=-=-=-=-=-=-=-=-=
//                    if ( (! sConcatDate.equals("2011-01-01")) && (! sConcatDate.equals("2010-01-02")) )
//                    if ( (! sConcatDate.equals("2012-04-12")) )           // Whole Day
//                    if ( (! sConcatMonth.equals("2009-04")) )           // Whole month
//                    {
//                        continue;
//                    }
//
//                    logIt(fDebug, iDEBUG, false, "line=["+line+"] ");
//
//                    logIt(fDebug, iDEBUG, false, "Record # "+iCounter+" had sConcat=["+sConcat+"]");
//
//                    if (iCounter >= 100)
//                    {
//                        logIt(fDebug, iDEBUG, false, "Max count reached.  Stopping...");
//                        fFixedWriter.flush();
//                        fFixedWriter.close();
//                        System.exit(22);
//                    }
// =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= Done =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=


// If there is a generated record for an AT1 element, skip it
//                    iAT1_IndexOf    = line.indexOf("AT1");
//                    if (iAT1_IndexOf > 0)
                    if (sCDS_Source.equals("O"))
                    {
                        logIt(fDebug, iDEBUG, false, "Found summary record generated to hold AT1 element in ["+sConcat+"] Source=["+sCDS_Source+"] Skipping record...");
                        continue;
                    }

//                    logIt(fDebug, iDEBUG, false, "================================ Source=["+sCDS_Source+"] Type=["+sCDS_Type+"]");
                    if (sCDS_Type.equals("SOD "))
                    {
                        iAT1_IndexOf    = line.indexOf("AT1");
                        iAT2_IndexOf    = line.indexOf("AT2");
                        iAT3_IndexOf    = line.indexOf("AT3");
                        iAT4_IndexOf    = line.indexOf("AT4");
                        iAT5_IndexOf    = line.indexOf("AT5");
                        iAT6_IndexOf    = line.indexOf("AT6");
                        iAT7_IndexOf    = line.indexOf("AT7");
                        iAT8_IndexOf    = line.indexOf("AT8");

                        if (iAT1_IndexOf > 0)
                        {
                            iATx_Cut = iAT1_IndexOf - iAT1_IndexOf + iATx_Length;
                        }
                        if (iAT2_IndexOf > 0)
                        {
                            iATx_Cut = iAT2_IndexOf - iAT1_IndexOf + iATx_Length;
                        }
                        if (iAT3_IndexOf > 0)
                        {
                            iATx_Cut = iAT3_IndexOf - iAT1_IndexOf + iATx_Length;
                        }
                        if (iAT4_IndexOf > 0)
                        {
                            iATx_Cut = iAT4_IndexOf - iAT1_IndexOf + iATx_Length;
                        }
                        if (iAT5_IndexOf > 0)
                        {
                            iATx_Cut = iAT5_IndexOf - iAT1_IndexOf + iATx_Length;
                        }
                        if (iAT6_IndexOf > 0)
                        {
                            iATx_Cut = iAT6_IndexOf - iAT1_IndexOf + iATx_Length;
                        }
                        if (iAT7_IndexOf > 0)
                        {
                            iATx_Cut = iAT7_IndexOf - iAT1_IndexOf + iATx_Length;
                        }
                        if (iAT8_IndexOf > 0)
                        {
                            iATx_Cut = iAT8_IndexOf - iAT1_IndexOf + iATx_Length;
                        }

//                        logIt(fDebug, iDEBUG, false, "Found SOD record in ["+sConcat+"] AT1=["+iAT1_IndexOf+"] AT2=["+iAT2_IndexOf+"] AT3=["+iAT3_IndexOf+"] AT4=["+iAT4_IndexOf
//                                                                                    +"] AT5=["+iAT5_IndexOf+"] AT6=["+iAT6_IndexOf+"] AT7=["+iAT7_IndexOf+"] AT8=["+iAT8_IndexOf
//                                                                                    +"] Length=["+iATx_Cut+"]");
                        if (iAT1_IndexOf > 0)
                        {
                            lineHold = line.substring(0,iAT1_IndexOf)+line.substring(iAT1_IndexOf+iATx_Cut,iLength);
                            line = lineHold;                                                // Rip out the AT elements and continue processing...
                            logIt(fDebug, iDEBUG, false, "Removed ATx elements for length of "+iATx_Cut+" characters");
//                            logIt(fDebug, iDEBUG, false, "Old line=["+line+"]");
//                            logIt(fDebug, iDEBUG, false, "New line=["+lineHold+"]");
                        }
//                        continue;
                    }

                    getMDS(line);   //  Fields making up the Mandatory Data Section.
                    getOC1(line);   //  Fields making up the OC1 element.
                    getGDx(line);   //  Fields making up the GDx element.	// 03/24/2015  ras
                    getGF1(line);   //  Fields making up the GF1 element.
                    getMW1(line);   //  Fields making up the MW1 element.
                    getMW2(line);   //  Fields making up the MW2 element.
                    getMW3(line);   //  Fields making up the MW3 element.
                    getMW4(line);   //  Fields making up the MW3 element.       // 06/21/2012  ras
                    getAY1(line);   //  Fields making up the AY1 element.
                    getMA1(line);   //  Fields making up the MA1 element.
                    sMaxTemp        = "***";
                    sMinTemp        = "***";
                    getKA1(line);   //  Fields making up the KA1 element.
                    getKA2(line);   //  Fields making up the KA2 element.
                    sPcp01          = "*****";
                    sPcp01t         = " ";
                    sPcp06          = "*****";
                    sPcp06t         = " ";
                    sPcp24          = "*****";
                    sPcp24t         = " ";
                    sPcp12          = "*****";
                    sPcp12t         = " ";
                    getAA1(line);   //  Fields making up the AA1 element.
                    getAA2(line);   //  Fields making up the AA2 element.
                    getAA3(line);   //  Fields making up the AA3 element.
                    getAA4(line);   //  Fields making up the AA4 element.
                    getAJ1(line);   //  Fields making up the AJ1 element.
                    getAW1(line);   //  Fields making up the AW1 element.       // 06/06/2012  ras
                    getAW2(line);   //  Fields making up the AW2 element.       // 06/06/2012  ras
                    getAW3(line);   //  Fields making up the AW3 element.       // 06/06/2012  ras
                    getAW4(line);   //  Fields making up the AW4 element.       // 06/06/2012  ras

// Begin formatting output record..............................................................

// Post-processing format changes
                    if ( sCDS_Wban.equals("99999") )    // Show WBAN=99999 as missing "*****" in output file
                    {
                        sCDS_Wban   = "*****";
                    }
// Build Control Data Section
                    sControlSection = sCDS_ID+" "+sCDS_Wban+" "+sCDS_Year+sCDS_Month+sCDS_Day+sCDS_Hour+sCDS_Minute;

// Sort Present Weather elements
                    sWW1234 = new String[] {sMW1_Ww,sMW2_Ww,sMW3_Ww,sMW4_Ww};
                    Arrays.sort(sWW1234);

// Sort Present Weather (Automated) elements
                    sAW1234 = new String[] {sAW1_Zz,sAW2_Zz,sAW3_Zz,sAW4_Zz};
                    Arrays.sort(sAW1234);

// Build Mandatory Data Section + the rest of the record
                    sMandatorySection = sMDS_Dir+" "+sMDS_Spd+" "+sOC1_Gus+" "+sMDS_Clg
                            +" "+sGF1_Skc+" "+sGF1_Low+" "+sGF1_Med+" "+sGF1_Hi+" "+sMDS_Vsb
                            +" "+sWW1234[3]+" "+sWW1234[2]+" "+sWW1234[1]+" "+sWW1234[0]
                            +" "+sAW1234[3]+" "+sAW1234[2]+" "+sAW1234[1]+" "+sAW1234[0]+" "+sAY1_Pw
                            +" "+sMDS_Temp+" "+sMDS_Dewp+" "+sMDS_Slp+" "+sMA1_Alt+" "+sMA1_Stp
                            +" "+sMaxTemp+" "+sMinTemp+" "+sPcp01+sPcp01t+sPcp06+sPcp06t+sPcp24+sPcp24t+sPcp12+sPcp12t
                            +sAJ1_Sd;

                    sOutputRecord = sControlSection+" "+sMandatorySection;  // Put it all together
                    fFixedWriter.write(sOutputRecord+"\n");                 // Write out the record

                }  // while read

            }
            catch (IOException ex) {
                System.err.println(sProgramName+": IOException 2. Error=[" + ex.getMessage()+"]");
                System.err.println(sProgramName+": Stack trace follows:");
                ex.printStackTrace();
                System.exit(2);
            }

            fInReader.close();
            fFixedWriter.flush();
            fFixedWriter.close();

        } catch (Exception e) {                                                               //Catch exception if any
            sMessage=sProgramName+": Unspecified Exception 1. Error=[" + e.getMessage()+"]";
            bStdErr=true;
            logIt(fDebug, iPROD, false, sMessage);         // Append output to log.
            System.err.println(sProgramName+": Stack trace follows:");
            e.printStackTrace();
            System.exit(1);
        }

        logIt(fDebug, iDEBUG, false, "Processed "+iCounter+" records");
        logIt(fDebug, iDEBUG, false, "Done.");

    }   // End of main()

    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// formatInt - Right-justifies an int.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static String formatInt(int i, int len)
    {
        final String blanks = "                 ";
        String s = Integer.toString(i);
        if (s.length() < len)
            s = blanks.substring(0, len - s.length()) + s;
        return s;
    }


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// formatFloat - Right-justifies a float.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static String formatFloat(float i, int len)
    {
        final String blanks = "                 ";
        String s = Float.toString(i);
        if (s.length() < len)
            s = blanks.substring(0, len - s.length()) + s;
        return s;
    }


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getCDS - Get CDS section and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getCDS(String p_sRecd)
    {
        //  Extract fields making up the Control Data Section.
        sCDS            = p_sRecd.substring(0,60);
        sCDS_Fill1      = p_sRecd.substring(0,4);
        sCDS_ID         = p_sRecd.substring(4,10);
        sCDS_Wban       = p_sRecd.substring(10,15);
        sCDS_Year       = p_sRecd.substring(15,19);
        sCDS_Month      = p_sRecd.substring(19,21);
        sCDS_Day        = p_sRecd.substring(21,23);
        sCDS_Hour       = p_sRecd.substring(23,25);
        sCDS_Minute     = p_sRecd.substring(25,27);
        sCDS_Source     = p_sRecd.substring(27,28);
        sCDS_Fill2      = p_sRecd.substring(28,41);
        sCDS_Type       = p_sRecd.substring(41,45);
        sCDS_Fill3      = p_sRecd.substring(45,60);
    }  // End of getCDS


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getMDS - Get MDS section and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getMDS(String p_sRecd)
    {
        //  Extract fields making up the Mandatory Data Section.
        sMDS            = p_sRecd.substring(60,105);
        sMDS_Dir        = p_sRecd.substring(60,63);
        sMDS_DirQ       = p_sRecd.substring(63,64);
        sMDS_DirType    = p_sRecd.substring(64,65);
        sMDS_Spd        = p_sRecd.substring(65,69);
        sMDS_Fill2      = p_sRecd.substring(69,70);
        sMDS_Clg        = p_sRecd.substring(70,75);
        sMDS_Fill3      = p_sRecd.substring(75,78);
        sMDS_Vsb        = p_sRecd.substring(78,84);
        sMDS_Fill4      = p_sRecd.substring(84,87);
        sMDS_TempSign   = p_sRecd.substring(87,88);
        sMDS_Temp       = p_sRecd.substring(88,92);
        sMDS_Fill5      = p_sRecd.substring(92,93);
        sMDS_DewpSign   = p_sRecd.substring(93,94);
        sMDS_Dewp       = p_sRecd.substring(94,98);
        sMDS_Fill6      = p_sRecd.substring(98,99);
        sMDS_Slp        = p_sRecd.substring(99,104);
        sMDS_Fill7      = p_sRecd.substring(104,105);

        if(sMDS_Dir.equals("999"))
        {
            sMDS_Dir = "***";
        }

        if(sMDS_DirType.equals("V"))                        // 06/21/2012  ras
        {
            sMDS_Dir = "990";
        }

//        logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sMDS_Dir=["+sMDS_Dir+"] sMDS_DirQ=["+sMDS_DirQ+"] sMDS_DirType=["+sMDS_DirType+"]");     // temporary - ras

        if(sMDS_Spd.equals("9999"))
        {
            sMDS_Spd = "***";
        }
        else
        {
//                      System.out.println("sMDS=["+sMDS+"] Spd=["+sMDS_Spd+"]");
            iWork     = Integer.parseInt(sMDS_Spd);                   // Convert to integer
//                      System.out.println("iWork=["+iWork+"]");
            iWork     = (int)(((float)iWork / 10.0) * 2.237 + .5);    // Convert Meters Per Second to Miles Per Hour
//                      System.out.println("iWork=["+iWork+"]");
//                      sMDS_Spd  = fmt3.format(iWork);
            sMDS_Spd  = formatInt(iWork,3);
//                      System.out.println("Spd=["+sMDS_Spd+"]");
//          logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sMDS_Spd=["+sMDS_Spd+"]");     // temporary - ras
        }

        if(sMDS_Clg.equals("99999"))
        {
            sMDS_Clg = "***";
        }
        else
        {
            try
            {
                iWork     = Integer.parseInt(sMDS_Clg);                 // Convert to integer
            }
            catch (Exception e)
            {
                logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sMDS_Clg value could not be converted to integer=["+sMDS_Clg+"]");
                sMDS_Clg   = "***";                                     // Data error.  Set to missing.
            }
            if( ! sMDS_Clg.equals("***") )
            {
                iWork     = (int)(((float)iWork * 3.281) / 100.0 + .5);   // Convert Meters to Hundreds of Feet
                sMDS_Clg  = formatInt(iWork,3);
//                logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sMDS_Clg=["+sMDS_Clg+"]");     // temporary - ras
            }
        }

        if(sMDS_Vsb.equals("999999"))
        {
            sMDS_Vsb = "****";
        }
        else
        {
            fWork     = Float.parseFloat(sMDS_Vsb);                 // Convert to floating point
            fWork     = ((float)(fWork * (float) 0.000625));                // Convert Meters to Miles using CDO's value
//            fWork     = ((float)(fWork * (float) 0.000621371192237334));    // Convert Meters to Miles
            fWorkSave = fWork;                                      // Save this value for possible display
            if (fWork > 99.9)
            {
                fWork = (float)99.0;                                 // Set to value that will fit
            }

            if (fWork == (float)10.058125)                          // Match CDO       2011-04-28  ras
            {
                logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sMDS_Vsb value rounded to 10 miles");
                fWork = (float)10.0;
            }
            sMDS_Vsb = fmt4_1.format(fWork);
            sMDS_Vsb = String.format("%4s",sMDS_Vsb);
        }

        if(sMDS_Temp.equals("9999"))
        {
            sMDS_Temp = "****";
        }
        else
        {
//                        System.out.println(sMDS_Temp);
            iWork     = Integer.parseInt(sMDS_Temp);     // Convert to integer
            if (sMDS_TempSign.equals("-"))
            {
                iWork*=-1;
            }
            if(iWork < -178)
            {
                iWork = (int)(((float)iWork / 10.0) * 1.8 + 32.0 - .5);  // Handle temps below 0F
            }
            else
            {
                iWork = (int)(((float)iWork / 10.0) * 1.8 + 32.0 + .5);
            }
            sMDS_Temp = formatInt(iWork,4);
//                        System.out.println(sMDS_Temp);
        }

        if(sMDS_Dewp.equals("9999"))
        {
            sMDS_Dewp = "****";
        }
        else
        {
//                        System.out.println(sMDS_Dewp);
            iWork     = Integer.parseInt(sMDS_Dewp);     // Convert to integer
            if (sMDS_DewpSign.equals("-"))
            {
                iWork*=-1;
            }
            if(iWork < -178)
            {
                iWork = (int)(((float)iWork / 10.0) * 1.8 + 32.0 - .5);  // Handle temps below 0F
            }
            else
            {
                iWork = (int)(((float)iWork / 10.0) * 1.8 + 32.0 + .5);
            }
            sMDS_Dewp = formatInt(iWork,4);
//                        System.out.println(sMDS_Dewp);
        }

        if(sMDS_Slp.equals("99999"))
        {
            sMDS_Slp = "******";
        }
        else
        {
            fWork     = Float.parseFloat(sMDS_Slp);                 // Convert to floating point
            fWork     = ((float)(fWork / 10.0));                    // Convert convert Hectopascals to Millibars
            sMDS_Slp  = fmt6_1.format(fWork);
            sMDS_Slp = String.format("%6s",sMDS_Slp);
        }
    }  // End of getMDS


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getGDx - Get highest GDx element (can be 1 thru 6) and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getGDx(String p_sRecd)
    {
        int iGDx_Looper = 6;
        sGDx            = "";
        sGDx_Cov1       = "";
        sGDx_Cov2       = "";
        iGDx_IndexOf 	= 0;
        sGDx_Skc        = "***";

        while ( iGDx_Looper > 0 )
        {
            iGDx_IndexOf = p_sRecd.indexOf("GD"+Integer.toString(iGDx_Looper));

            if ( (iGDx_IndexOf >= 0) && (iGDx_IndexOf < iREM_IndexOf) )
            {
//     	   logIt(fDebug, iDEBUG, false, "GD"+Integer.toString(iGDx_Looper)+" found.");
                iGDx_Looper = -1;
                continue;
            }
            iGDx_Looper--;
        }

        if ( (iGDx_IndexOf >= 0) && (iGDx_IndexOf < iREM_IndexOf) )
        {
            sGDx        = p_sRecd.substring(iGDx_IndexOf,iGDx_IndexOf+iGDx_Length);
            sGDx_Cov1   = sGDx.substring(3,4);
            sGDx_Cov2   = sGDx.substring(4,6);

            try
            {
                iWork       = Integer.parseInt(sGDx_Cov1);   // Convert to integer
            }
            catch (Exception e)
            {
                logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sGDx_Cov1 value could not be converted to integer=["+sGDx_Cov1+"]");
                iWork = 999;
                sGDx_Skc   = "***";                                     // Data error.  Set to missing.
            }

            if( iWork < 9 )
            {
                switch (iWork)
                {
                    case 0:  sGDx_Skc = "CLR";
                        break;
                    case 1:  sGDx_Skc = "FEW";
                        break;
                    case 2:  sGDx_Skc = "SCT";
                        break;
                    case 3:  sGDx_Skc = "BKN";
                        break;
                    case 4:  sGDx_Skc = "OVC";
                        break;
                    case 5:  sGDx_Skc = "OBS";
                        break;
                    case 6:  sGDx_Skc = "POB";
                        break;
                    default: sGDx_Skc = "***";
                        break;
                }
            }
            else
            {
                try
                {
                    iWork       = Integer.parseInt(sGDx_Cov2);   // Convert to integer
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sGDx_Cov2 value could not be converted to integer=["+sGDx_Cov2+"]");
                    iWork = 999;
                    sGDx_Skc   = "***";                                     // Data error.  Set to missing.
                }

                switch (iWork)
                {
                    case 0:  sGDx_Skc = "CLR";
                        break;
                    case 1:
                    case 2:  sGDx_Skc = "FEW";
                        break;
                    case 3:
                    case 4:
                    case 11:
                    case 12:
                    case 13: sGDx_Skc = "SCT";
                        break;
                    case 5:
                    case 6:
                    case 7:
                    case 14:
                    case 15:
                    case 16: sGDx_Skc = "BKN";
                        break;
                    case 8:
                    case 17:
                    case 18:
                    case 19: sGDx_Skc = "OVC";
                        break;
                    case 9:
                    case 10: sGDx_Skc = "POB";
                        break;
                    default: sGDx_Skc = "***";
                        break;
                }

            }
        }

    }  // End of getGDx


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getOC1 - Get OC1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getOC1(String p_sRecd)
    {
        sOC1            = "";
        sOC1_Fill1      = "";
        sOC1_Gus        = "***";
        sOC1_Fill2      = "";
        iOC1_IndexOf    = p_sRecd.indexOf("OC1");
        if ( (iOC1_IndexOf >= 0) && (iOC1_IndexOf < iREM_IndexOf) )
        {
            sOC1        = p_sRecd.substring(iOC1_IndexOf,iOC1_IndexOf+iOC1_Length);
            sOC1_Fill1  = sOC1.substring(1,3);  // 3
            sOC1_Gus    = sOC1.substring(3,7);  // 4
            sOC1_Fill2  = sOC1.substring(7,8);  // 1

            if(sOC1_Gus.equals("9999"))                             // 06/06/2012  ras
            {
                sOC1_Gus    = "***";
//                logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sOC1_Gus missing=["+sOC1_Gus+"]");     // temporary - ras
            }
            else
            {
                try
                {
                    iWork   = Integer.parseInt(sOC1_Gus);                   // Convert to integer
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sOC1_Gus value could not be converted to integer=["+sOC1_Gus+"]");
                    sOC1_Gus = "***";             // Data error.  Set to missing.
                }
                if( ! sOC1_Gus.equals("***") )
                {
                    iWork       = (int)(((float)iWork / 10.0) * 2.237 + .5);    // Convert Meters Per Second to Miles Per Hour
                    sOC1_Gus    = formatInt(iWork,3);
//                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sOC1_Gus=["+sOC1_Gus+"]");     // temporary - ras
                }
            }
        }
    }  // End of getOC1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getGF1 - Get GF1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getGF1(String p_sRecd)
    {
        sGF1            = "";
        sGF1_Fill1      = "";
        sGF1_Skc        = "***";
        sGF1_Fill2      = "";
        sGF1_Low        = "*";
        sGF1_Fill3      = "";
        sGF1_Med        = "*";
        sGF1_Fill4      = "";
        sGF1_Hi         = "*";
        sGF1_Fill5      = "";
        iGF1_IndexOf    = p_sRecd.indexOf("GF1");
        if ( (iGF1_IndexOf >= 0) && (iGF1_IndexOf < iREM_IndexOf) )
        {
            sGF1        = p_sRecd.substring(iGF1_IndexOf,iGF1_IndexOf+iGF1_Length);
            sGF1_Fill1  = sGF1.substring(1,3);
            sGF1_Skc    = sGF1.substring(3,5);
            sGF1_Fill2  = sGF1.substring(5,11);
            sGF1_Low    = sGF1.substring(11,13);
            sGF1_Fill3  = sGF1.substring(13,20);
            sGF1_Med    = sGF1.substring(20,22);
            sGF1_Fill4  = sGF1.substring(22,23);
            sGF1_Hi     = sGF1.substring(23,25);
            sGF1_Fill5  = sGF1.substring(25,26);
        }
        else					// Use GDx info when GF1 is missing			// 03/24/2015  ras
        {
            if ( iGDx_IndexOf > 0 )
            {
                logIt(fDebug, iDEBUG, false, "GDx found. Using GDx in place of GF1 info.");
                sGF1_Skc    = sGDx_Skc;
                sGF1_Low    = "*";
                sGF1_Med    = "*";
                sGF1_Hi     = "*";
            }
        }

        if ( (iGF1_IndexOf >= 0) && (iGF1_IndexOf < iREM_IndexOf) )
        {
            if(sGF1_Skc.equals("99"))
            {
                sGF1_Skc    = "***";
            }
            else
            {
//                            System.out.println("DateTime=["+sConcat+"] GF1=["+sGF1+"]  Skc=["+sGF1_Skc+"]");
                try
                {
                    iWork       = Integer.parseInt(sGF1_Skc);   // Convert to integer
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sGF1_Skc value could not be converted to integer=["+sGF1_Skc+"]");
                    sGF1_Skc   = "***";                                     // Data error.  Set to missing.
                }
                if( ! sGF1_Skc.equals("***") )
                {
                    if(iWork == 0)                          { sGF1_Skc = "CLR"; }
                    else { if((iWork >= 1) && (iWork <= 4)) { sGF1_Skc = "SCT"; }
                    else { if((iWork >= 5) && (iWork <= 7)) { sGF1_Skc = "BKN"; }
                    else { if(iWork == 8)                   { sGF1_Skc = "OVC"; }
                    else { if(iWork == 9)                   { sGF1_Skc = "OBS"; }
                    else { if(iWork == 10)                  { sGF1_Skc = "POB"; }}}}}}
                }
            }
            if(sGF1_Low.equals("99"))       // Low cloud type
            {
                sGF1_Low = "*";
            }
            else
            {
                sGF1_Low = sGF1_Low.substring(1,2);
            }

            if(sGF1_Med.equals("99"))       // Med cloud type
            {
                sGF1_Med = "*";
            }
            else
            {
                sGF1_Med = sGF1_Med.substring(1,2);
            }

            if(sGF1_Hi.equals("99"))        // High cloud type
            {
                sGF1_Hi = "*";
            }
            else
            {
                sGF1_Hi = sGF1_Hi.substring(1,2);
            }
        }
    }  // End of getGF1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getMW1 - Get MW1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getMW1(String p_sRecd)
    {
        sMW1            = "";
        sMW1_Fill1      = "";
        sMW1_Ww         = "**";
        sMW1_Fill2      = "";
        iMW1_IndexOf    = p_sRecd.indexOf("MW1");
        if ( (iMW1_IndexOf >= 0) && (iMW1_IndexOf < iREM_IndexOf) )
        {
            sMW1        = p_sRecd.substring(iMW1_IndexOf,iMW1_IndexOf+iMW1_Length);
            sMW1_Fill1  = sMW1.substring(1,3);  // 3
            sMW1_Ww     = sMW1.substring(3,5);  // 2
            sMW1_Fill2  = sMW1.substring(5,6);  // 1
//                        System.out.println("MW1=["+sMW1+"] Ww=["+sMW1_Ww+"]");
        }
    }  // End of getMW1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getMW2 - Get MW2 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getMW2(String p_sRecd)
    {
        sMW2            = "";
        sMW2_Fill1      = "";
        sMW2_Ww         = "**";
        sMW2_Fill2      = "";
        iMW2_IndexOf    = p_sRecd.indexOf("MW2");
        if ( (iMW2_IndexOf >= 0) && (iMW2_IndexOf < iREM_IndexOf) )
        {
            sMW2        = p_sRecd.substring(iMW2_IndexOf,iMW2_IndexOf+iMW2_Length);
            sMW2_Fill1  = sMW2.substring(1,3);  // 3
            sMW2_Ww     = sMW2.substring(3,5);  // 2
            sMW2_Fill2  = sMW2.substring(5,6);  // 1
//                        System.out.println("MW2=["+sMW2+"] Ww=["+sMW2_Ww+"]");
        }
    }  // End of getMW2


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getMW3 - Get MW3 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getMW3(String p_sRecd)
    {
        sMW3            = "";
        sMW3_Fill1      = "";
        sMW3_Ww         = "**";
        sMW3_Fill2      = "";
        iMW3_IndexOf    = p_sRecd.indexOf("MW3");
        if ( (iMW3_IndexOf >= 0) && (iMW3_IndexOf < iREM_IndexOf) )
        {
            sMW3        = p_sRecd.substring(iMW3_IndexOf,iMW3_IndexOf+iMW3_Length);
            sMW3_Fill1  = sMW3.substring(1,3);  // 3
            sMW3_Ww     = sMW3.substring(3,5);  // 2
            sMW3_Fill2  = sMW3.substring(5,6);  // 1
//                        System.out.println("MW3=["+sMW3+"] Ww=["+sMW3_Ww+"]");
        }
    }  // End of getMW3


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getMW4 - Get MW4 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getMW4(String p_sRecd)
    {
        sMW4            = "";
        sMW4_Fill1      = "";
        sMW4_Ww         = "**";
        sMW4_Fill2      = "";
        iMW4_IndexOf    = p_sRecd.indexOf("MW4");
        if ( (iMW4_IndexOf >= 0) && (iMW4_IndexOf < iREM_IndexOf) )
        {
            sMW4        = p_sRecd.substring(iMW4_IndexOf,iMW4_IndexOf+iMW4_Length);
            sMW4_Fill1  = sMW4.substring(1,3);  // 3
            sMW4_Ww     = sMW4.substring(3,5);  // 2
            sMW4_Fill2  = sMW4.substring(5,6);  // 1
//            logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sMW4_Ww=["+sMW4_Ww+"]");     // temporary - ras
        }
    }  // End of getMW4


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAY1 - Get AY1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAY1(String p_sRecd)
    {
        sAY1            = "";
        sAY1_Fill1      = "";
        sAY1_Pw         = "*";
        sAY1_Fill2      = "";
        iAY1_IndexOf    = p_sRecd.indexOf("AY1");
        if ( (iAY1_IndexOf >= 0) && (iAY1_IndexOf < iREM_IndexOf) )
        {
            sAY1        = p_sRecd.substring(iAY1_IndexOf,iAY1_IndexOf+iAY1_Length);
            sAY1_Fill1  = sAY1.substring(1,3);  // 3
            sAY1_Pw     = sAY1.substring(3,4);  // 1
            sAY1_Fill2  = sAY1.substring(4,8);  // 4
//                        System.out.println("AY1=["+sAY1+"] Pw=["+sAY1_Pw+"]");
        }
    }  // End of getAY1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getMA1 - Get MA1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getMA1(String p_sRecd)
    {
        sMA1            = "";
        sMA1_Fill1      = "";
        sMA1_Alt        = "*****";
        sMA1_Fill2      = "";
        sMA1_Stp        = "******";
        sMA1_Fill3      = "";
        iMA1_IndexOf    = p_sRecd.indexOf("MA1");
        if ( (iMA1_IndexOf >= 0) && (iMA1_IndexOf < iREM_IndexOf) )
        {
            sMA1        = p_sRecd.substring(iMA1_IndexOf,iMA1_IndexOf+iMA1_Length);
            sMA1_Fill1  = sMA1.substring(1,3);      // 3
            sMA1_Alt    = sMA1.substring(3,8);      // 5
            sMA1_Fill2  = sMA1.substring(8,9);      // 1
            sMA1_Stp    = sMA1.substring(9,14);     // 5
            sMA1_Fill3  = sMA1.substring(14,15);    // 1

            if(sMA1_Alt.equals("99999"))
            {
                sMA1_Alt    = "*****";
            }
            else
            {
                try
                {
                    fWork       = Float.parseFloat(sMA1_Alt);                 // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sMA1_Alt value could not be converted to floating point=["+sMA1_Alt+"]");
                    sMA1_Alt  = "*****";                                      // Data error.  Set to missing.
                }
                if( ! sMA1_Alt.equals("*****") )
                {
                    fWork       = ((float)((fWork / 10.0)*100.0) / (float) 3386.39);    // Convert Hectopascals to Inches
                    sMA1_Alt    = fmt5_2.format(fWork);
                    sMA1_Alt    = String.format("%5s",sMA1_Alt);
                }
            }
            if(sMA1_Stp.equals("99999"))
            {
                sMA1_Stp    = "******";
            }
            else
            {
                try
                {
                    fWork       = Float.parseFloat(sMA1_Stp);                 // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sMA1_Stp value could not be converted to floating point=["+sMA1_Stp+"]");
                    sMA1_Stp  = "******";                                     // Data error.  Set to missing.
                }
                if( ! sMA1_Stp.equals("******") )
                {
                    fWork     	= ((float)(fWork / 10.0));                    // Convert convert Hectopascals to Millibars
                    sMA1_Stp  	= fmt6_1.format(fWork);
                    sMA1_Stp    = String.format("%6s",sMA1_Stp);
                }
            }
        }
    }  // End of getMA1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getKA1 - Get KA1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getKA1(String p_sRecd)
    {
        sKA1            = "";
        sKA1_Fill1      = "";
        sKA1_Code       = "*";
        sKA1_Temp       = "***";
        sKA1_Fill2      = "";
        iKA1_IndexOf    = p_sRecd.indexOf("KA1");
        if ( (iKA1_IndexOf >= 0) && (iKA1_IndexOf < iREM_IndexOf) )
        {
            sKA1        = p_sRecd.substring(iKA1_IndexOf,iKA1_IndexOf+iKA1_Length);
            sKA1_Fill1  = sKA1.substring(1,6);   // 6
            sKA1_Code   = sKA1.substring(6,7);   // 1
            sKA1_Temp   = sKA1.substring(7,12);  // 5
            sKA1_Fill2  = sKA1.substring(12,13); // 1
//                        System.out.println("KA1=["+sKA1+"] Code=["+sKA1_Code+"] Temp=["+sKA1_Temp+"]");
            if(sKA1_Temp.equals("+9999"))
            {
                sKA1_Temp   = "***";
            }
            else
            {
                try
                {
                    fWork       = Float.parseFloat(sKA1_Temp);                  // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sKA1_Temp value could not be converted to floating point=["+sKA1_Temp+"]");
                    sKA1_Temp  = "***";                                         // Data error.  Set to missing.
                }
                if( ! sKA1_Temp.equals("***") )
                {
                    if(fWork < -178)
                    {
                        fWork = (int)(((float)fWork / 10.0) * 1.8 + 32.0 - .5);  // Handle temps below 0F
                    }
                    else
                    {
                        fWork = (int)(((float)fWork / 10.0) * 1.8 + 32.0 + .5);
                    }
                    if(sKA1_Code.equals("N"))
                    {
                        sMinTemp = formatInt( (int) fWork,3);
                    }
                    else
                    {
                        if(sKA1_Code.equals("M"))
                        {
                            sMaxTemp = formatInt( (int) fWork,3);
                        }
                    }
                }
            }
        }
    }  // End of getKA1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getKA2 - Get KA2 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getKA2(String p_sRecd)
    {
        sKA2            = "";
        sKA2_Fill1      = "";
        sKA2_Code       = "*";
        sKA2_Temp       = "***";
        sKA2_Fill2      = "";
        iKA2_IndexOf    = p_sRecd.indexOf("KA2");
        if ( (iKA2_IndexOf >= 0) && (iKA2_IndexOf < iREM_IndexOf) )
        {
            sKA2        = p_sRecd.substring(iKA2_IndexOf,iKA2_IndexOf+iKA2_Length);
            sKA2_Fill1  = sKA2.substring(1,6);   // 6
            sKA2_Code   = sKA2.substring(6,7);   // 1
            sKA2_Temp   = sKA2.substring(7,12);  // 5
            sKA2_Fill2  = sKA2.substring(12,13); // 1
//                        System.out.println("KA2=["+sKA2+"] Code=["+sKA2_Code+"] Temp=["+sKA2_Temp+"]");
            if(sKA2_Temp.equals("+9999"))
            {
                sKA2_Temp   = "***";
            }
            else
            {
                try
                {
                    fWork       = Float.parseFloat(sKA2_Temp);                 // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sKA2_Temp value could not be converted to floating point=["+sKA2_Temp+"]");
                    sKA2_Temp = "***";             // Data error.  Set to missing.
                }
                if( ! sKA2_Temp.equals("***") )
                {
                    if(fWork < -178)
                    {
                        fWork = (int)(((float)fWork / 10.0) * 1.8 + 32.0 - .5);  // Handle temps below 0F
                    }
                    else
                    {
                        fWork = (int)(((float)fWork / 10.0) * 1.8 + 32.0 + .5);
                    }
                    if(sKA2_Code.equals("N"))
                    {
                        sMinTemp = formatInt( (int) fWork,3);
                    }
                    else
                    {
                        if(sKA2_Code.equals("M"))
                        {
                            sMaxTemp = formatInt( (int) fWork,3);
                        }
                    }
                }
            }
        }
    }  // End of getKA2


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAA1 - Get AA1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAA1(String p_sRecd)
    {
        sAA1            = "";
        sAA1_Fill1      = "";
        sAA1_Hours      = "";
        sAA1_Pcp        = "";
        sAA1_Trace      = "";
        sAA1_Fill2      = "";
        iAA1_IndexOf    = p_sRecd.indexOf("AA1");
        if ( (iAA1_IndexOf >= 0) && (iAA1_IndexOf < iREM_IndexOf) )
        {
            sAA1        = p_sRecd.substring(iAA1_IndexOf,iAA1_IndexOf+iAA1_Length);
            sAA1_Fill1  = sAA1.substring(1,3);   // 3
            sAA1_Hours  = sAA1.substring(3,5);   // 2
            sAA1_Pcp    = sAA1.substring(5,9);   // 4
            sAA1_Trace  = sAA1.substring(9,10);  // 1
            sAA1_Fill2  = sAA1.substring(10,11); // 1
//                        System.out.println("AA1=["+sAA1+"] Pcp=["+sAA1_Pcp+"]");
            if( sAA1_Pcp.equals("9999") )
            {
                sAA1_Pcp = "*****";
            }
            else
            {
                try
                {
                    fWork         = Float.parseFloat(sAA1_Pcp);       // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] AA1_Pcp value could not be converted to floating point=["+sAA1_Pcp+"]");
                    sAA1_Pcp = "*****";             // Data error.  Set to missing.
                }
                if( ! sAA1_Pcp.equals("*****") )
                {
                    setPcp(sAA1_Hours,sAA1_Trace);
                }
            }
        }
    }  // End of getAA1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAA2 - Get AA2 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAA2(String p_sRecd)
    {
        sAA2            = "";
        sAA2_Fill1      = "";
        sAA2_Hours      = "";
        sAA2_Pcp        = "";
        sAA2_Trace      = "";
        sAA2_Fill2      = "";
        iAA2_IndexOf    = p_sRecd.indexOf("AA2");
        if ( (iAA2_IndexOf >= 0) && (iAA2_IndexOf < iREM_IndexOf) )
        {
//                        System.out.println("DateTime=["+sConcat+"] iAA2_IndexOf=["+iAA2_IndexOf+"] iAA2_Length=["+iAA2_Length+"] Line Length=["+iLength+"]");
            sAA2        = p_sRecd.substring(iAA2_IndexOf,iAA2_IndexOf+iAA2_Length);
            sAA2_Fill1  = sAA2.substring(1,3);   // 3
            sAA2_Hours  = sAA2.substring(3,5);   // 2
            sAA2_Pcp    = sAA2.substring(5,9);   // 4
            sAA2_Trace  = sAA2.substring(9,10);  // 1
            sAA2_Fill2  = sAA2.substring(10,11); // 1
//                        System.out.println("AA2=["+sAA2+"] Pcp=["+sAA2_Pcp+"]");
            if( sAA2_Pcp.equals("9999") )
            {
                sAA2_Pcp = "*****";
            }
            else
            {
                try
                {
                    fWork         = Float.parseFloat(sAA2_Pcp);       // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] AA2_Pcp value could not be converted to floating point=["+sAA2_Pcp+"]");
                    sAA2_Pcp = "*****";             // Data error.  Set to missing.
                }
                if( ! sAA2_Pcp.equals("*****") )
                {
                    setPcp(sAA2_Hours,sAA2_Trace);
                }
            }
        }
    }  // End of getAA2


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAA3 - Get AA3 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAA3(String p_sRecd)
    {
        sAA3            = "";
        sAA3_Fill1      = "";
        sAA3_Hours      = "";
        sAA3_Pcp        = "";
        sAA3_Trace      = "";
        sAA3_Fill2      = "";
        iAA3_IndexOf    = p_sRecd.indexOf("AA3");
        if ( (iAA3_IndexOf >= 0) && (iAA3_IndexOf < iREM_IndexOf) )
        {
//                        System.out.println("DateTime=["+sConcat+"] iAA3_IndexOf=["+iAA3_IndexOf+"] iAA3_Length=["+iAA3_Length+"] Line Length=["+iLength+"]");
            sAA3        = p_sRecd.substring(iAA3_IndexOf,iAA3_IndexOf+iAA3_Length);
            sAA3_Fill1  = sAA3.substring(1,3);   // 3
            sAA3_Hours  = sAA3.substring(3,5);   // 2
            sAA3_Pcp    = sAA3.substring(5,9);   // 4
            sAA3_Trace  = sAA3.substring(9,10);  // 1
            sAA3_Fill2  = sAA3.substring(10,11); // 1
//                        System.out.println("AA3=["+sAA3+"] Pcp=["+sAA3_Pcp+"]");
            if( sAA3_Pcp.equals("9999") )
            {
                sAA3_Pcp = "*****";
            }
            else
            {
                try
                {
                    fWork         = Float.parseFloat(sAA3_Pcp);       // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iPROD, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] AA3_Pcp value could not be converted to floating point=["+sAA3_Pcp+"]");
                    sAA3_Pcp = "*****";             // Data error.  Set to missing.
                }
                if( ! sAA3_Pcp.equals("*****") )
                {
                    setPcp(sAA3_Hours,sAA3_Trace);
                }
            }
        }
    }  // End of getAA3


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAA4 - Get AA4 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAA4(String p_sRecd)
    {
        sAA4            = "";
        sAA4_Fill1      = "";
        sAA4_Hours      = "";
        sAA4_Pcp        = "";
        sAA4_Trace      = "";
        sAA4_Fill2      = "";
        iAA4_IndexOf    = p_sRecd.indexOf("AA4");
        if ( (iAA4_IndexOf >= 0) && (iAA4_IndexOf < iREM_IndexOf) )
        {
//                        System.out.println("DateTime=["+sConcat+"] iAA4_IndexOf=["+iAA4_IndexOf+"] iAA4_Length=["+iAA4_Length+"] Line Length=["+iLength+"]");
            sAA4        = p_sRecd.substring(iAA4_IndexOf,iAA4_IndexOf+iAA4_Length);
            sAA4_Fill1  = sAA4.substring(1,3);   // 3
            sAA4_Hours  = sAA4.substring(3,5);   // 2
            sAA4_Pcp    = sAA4.substring(5,9);   // 4
            sAA4_Trace  = sAA4.substring(9,10);  // 1
            sAA4_Fill2  = sAA4.substring(10,11); // 1
//                        System.out.println("AA4=["+sAA4+"] Pcp=["+sAA4_Pcp+"]");
            if( sAA4_Pcp.equals("9999") )
            {
                sAA4_Pcp = "*****";
            }
            else
            {
                try
                {
                    fWork         = Float.parseFloat(sAA4_Pcp);       // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] AA4_Pcp value could not be converted to floating point=["+sAA4_Pcp+"]");
                    sAA4_Pcp = "*****";             // Data error.  Set to missing.
                }
                if( ! sAA4_Pcp.equals("*****") )
                {
                    setPcp(sAA4_Hours,sAA4_Trace);
                }
            }
        }
    }  // End of getAA4


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// setPcp - Take AA elements and set Precip values.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void setPcp(String p_sHours, String p_sTrace)
    {
        fWork         = (fWork / (float) 10.0) * (float) .03937008;       // Convert precip depths from Millimeters to Inches
        if( p_sHours.equals("01") )
        {
            sPcp01      = fmt5_2.format(fWork);
            sPcp01      = String.format("%5s",sPcp01);
            if( p_sTrace.equals("2") )
            {
                sPcp01t = "T";
            }
        }
        else
        {
            if( p_sHours.equals("06") )
            {
                sPcp06      = fmt5_2.format(fWork);
                sPcp06      = String.format("%5s",sPcp06);
                if( p_sTrace.equals("2") )
                {
                    sPcp06t = "T";
                }
            }
            else
            {
                if( p_sHours.equals("24") )
                {
                    sPcp24      = fmt5_2.format(fWork);
                    sPcp24      = String.format("%5s",sPcp24);
                    if( p_sTrace.equals("2") )
                    {
                        sPcp24t = "T";
                    }
                }
                else
                {
                    sPcp12      = fmt5_2.format(fWork);
                    sPcp12      = String.format("%5s",sPcp12);
                    if( p_sTrace.equals("2") )
                    {
                        sPcp12t = "T";
                    }
                }
            }
        }
    }  // End of setPcp


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAJ1 - Get AJ1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAJ1(String p_sRecd)
    {
        sAJ1            = "";
        sAJ1_Fill1      = "";
        sAJ1_Sd         = "**";
        sAJ1_Fill2      = "";
        iAJ1_IndexOf    = p_sRecd.indexOf("AJ1");
        if ( (iAJ1_IndexOf >= 0) && (iAJ1_IndexOf < iREM_IndexOf) )
        {
            sAJ1        = p_sRecd.substring(iAJ1_IndexOf,iAJ1_IndexOf+iAJ1_Length);
            sAJ1_Fill1  = sAJ1.substring(1,3);  // 3
            sAJ1_Sd     = sAJ1.substring(3,7);  // 4
            sAJ1_Fill2  = sAJ1.substring(7,17); // 10
//                        System.out.println("AJ1_Fill1=["+sAJ1_Fill1+"] Sd=["+sAJ1_Sd+"]");
            if( sAJ1_Sd.equals("9999") )
            {
                sAJ1_Sd         = "**";
            }
            else
            {
                try
                {
                    fWork = Float.parseFloat(sAJ1_Sd);       // Convert to floating point
                }
                catch (Exception e)
                {
                    logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sAJ1_Sd value could not be converted to floating point=["+sAJ1_Sd+"]");
                    sAJ1_Sd   = "**";             // Data error.  Set to missing.
                }
                if( ! sAJ1_Sd.equals("**") )
                {
                    iWork     = (int) (fWork * (float) .3937008 + .5);       // Convert precip depths from Millimeters to Inches
                    sAJ1_Sd   = fmt02.format(iWork);
                    sAJ1_Sd   = String.format("%2s",sAJ1_Sd);
                }
            }
        }
    }  // End of getAJ1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAW1 - Get AW1 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAW1(String p_sRecd)
    {
        sAW1            = "";
        sAW1_Fill1      = "";
        sAW1_Zz         = "**";
        sAW1_Fill2      = "";
        iAW1_IndexOf    = p_sRecd.indexOf("AW1");
        if ( (iAW1_IndexOf >= 0) && (iAW1_IndexOf < iREM_IndexOf) )
        {
            sAW1        = p_sRecd.substring(iAW1_IndexOf,iAW1_IndexOf+iAW1_Length);
            sAW1_Fill1  = sAW1.substring(1,3);  // 3
            sAW1_Zz     = sAW1.substring(3,5);  // 2
            sAW1_Fill2  = sAW1.substring(5,6);  // 1
//            logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sAW1_Zz=["+sAW1_Zz+"]");     // temporary - ras
        }
    }  // End of getAW1


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAW2 - Get AW2 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAW2(String p_sRecd)
    {
        sAW2            = "";
        sAW2_Fill1      = "";
        sAW2_Zz         = "**";
        sAW2_Fill2      = "";
        iAW2_IndexOf    = p_sRecd.indexOf("AW2");
        if ( (iAW2_IndexOf >= 0) && (iAW2_IndexOf < iREM_IndexOf) )
        {
            sAW2        = p_sRecd.substring(iAW2_IndexOf,iAW2_IndexOf+iAW2_Length);
            sAW2_Fill1  = sAW2.substring(1,3);  // 3
            sAW2_Zz     = sAW2.substring(3,5);  // 2
            sAW2_Fill2  = sAW2.substring(5,6);  // 1
//            logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sAW2_Zz=["+sAW2_Zz+"]");     // temporary - ras
        }
    }  // End of getAW2


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAW3 - Get AW3 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAW3(String p_sRecd)
    {
        sAW3            = "";
        sAW3_Fill1      = "";
        sAW3_Zz         = "**";
        sAW3_Fill2      = "";
        iAW3_IndexOf    = p_sRecd.indexOf("AW3");
        if ( (iAW3_IndexOf >= 0) && (iAW3_IndexOf < iREM_IndexOf) )
        {
            sAW3        = p_sRecd.substring(iAW3_IndexOf,iAW3_IndexOf+iAW3_Length);
            sAW3_Fill1  = sAW3.substring(1,3);  // 3
            sAW3_Zz     = sAW3.substring(3,5);  // 2
            sAW3_Fill2  = sAW3.substring(5,6);  // 1
//            logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sAW3_Zz=["+sAW3_Zz+"]");     // temporary - ras
        }
    }  // End of getAW3


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// getAW4 - Get AW4 element and format its output.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static void getAW4(String p_sRecd)
    {
        sAW4            = "";
        sAW4_Fill1      = "";
        sAW4_Zz         = "**";
        sAW4_Fill2      = "";
        iAW4_IndexOf    = p_sRecd.indexOf("AW4");
        if ( (iAW4_IndexOf >= 0) && (iAW4_IndexOf < iREM_IndexOf) )
        {
            sAW4        = p_sRecd.substring(iAW4_IndexOf,iAW4_IndexOf+iAW4_Length);
            sAW4_Fill1  = sAW4.substring(1,3);  // 3
            sAW4_Zz     = sAW4.substring(3,5);  // 2
            sAW4_Fill2  = sAW4.substring(5,6);  // 1
//          logIt(fDebug, iDEBUG, false, "sInFileName=["+sInFileName+"] DateTime=["+sConcat+"] sAW4_Zz=["+sAW4_Zz+"]");     // temporary - ras
        }
    }  // End of getAW4


    //=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// logIt - Append records to the log file.
//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static int logIt(FileOutputStream p_fDebug, int p_iLogLevel, boolean p_bFilter, String p_sIn)
    {
        int iRetCode=99;                                        // Set default return code to something crazy.
        String sMessageFormatted="";

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        sMessageFormatted=sProgramName+": "+format.format(now)+"_"+p_sIn;

        if (bStdErr)
        {
            System.err.println(sMessageFormatted);              // Error   mode will echo message to standard error
        }

        if (bVerbose)
        {
            System.out.println(sMessageFormatted);              // Verbose mode will echo message to screen
        }

        if (iLogLevel < p_iLogLevel)                            // 04/01/2009  ras
        {
            return 0;                                           // No logging for this
        }

        if (p_bFilter)                                          // 04/01/2009  ras
        {
            if  (p_sFilter1.equals("None"))                     // 04/01/2009  ras
            {
            }
            else
            {
                if  (sConcat.equals(p_sFilter1)  ||             // 04/01/2009  ras    // Life is good
                        sConcat.equals(p_sFilter2))
                {
                }
                else
                {
                    return 0;                                   // 04/01/2009  ras    // No logging for this
                }
            }
        }

        try {
            p_fDebug = new FileOutputStream (sDebugName+".debug", true);                // Append mode.
            new PrintStream(p_fDebug).println (format.format(now)+"_"+p_sIn);           // Write output to debug log.
            iRetCode=0;                                                                 // Good.
            p_fDebug.close();
        }
        catch (IOException e) {
            System.out.println("5. Unable to open debug log");
            System.err.println(sProgramName+": Stack trace follows:");
            e.printStackTrace();
            System.exit(5);
        }
        catch (Exception e) {
            iRetCode=6;                                                                 // An error occurred.
            System.err.println(sProgramName+": Unspecified Exception in logIt. Error=[" + e.getMessage()+"]");
            System.err.println(sProgramName+": Stack trace follows:");
            e.printStackTrace();
            System.exit(6);
        }
        return iRetCode;
    }  // End of logIt

}