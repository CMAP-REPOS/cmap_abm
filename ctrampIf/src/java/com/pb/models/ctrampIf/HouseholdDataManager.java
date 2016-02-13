package com.pb.models.ctrampIf;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.IndexSort;
import com.pb.common.util.ObjectUtil;
import com.pb.common.util.SeededRandom;
import com.pb.common.model.ChoiceModelApplication;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ConcreteAlternative;
import com.pb.models.ctrampIf.jppf.CtrampApplication;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Random;

import umontreal.iro.lecuyer.probdist.LognormalDist;
import umontreal.iro.lecuyer.probdist.NormalDist;

import org.apache.log4j.Logger;

/**
 * @author Jim Hicks
 *
 * Class for managing household and person object data read from synthetic population files.
 */
public abstract class HouseholdDataManager implements HouseholdDataManagerIf, Serializable {

    protected transient Logger logger = Logger.getLogger(HouseholdDataManager.class);

    protected static int MAX_HHS_PER_FILE = 100000;    
    protected static int MAX_BYTES_HH_OBJECT = 30000;
    protected static int NUMBER_OF_IN_MEMORY_HHS = 10000;
    

    public static final String PROPERTIES_SYNPOP_INPUT_HH   = "PopulationSynthesizer.InputToCTRAMP.HouseholdFile";
    public static final String PROPERTIES_SYNPOP_INPUT_HH_FIELD_NAME   = "PopulationSynthesizer.InputToCTRAMP.HouseholdFile.Zone.Field.Name";
    public static final String PROPERTIES_SYNPOP_INPUT_PERS = "PopulationSynthesizer.InputToCTRAMP.PersonFile";
    public static final String PROPERTIES_WORK_OCCUPATION_CODES = "PopulationSynthesizer.InputToCTRAMP.WorkOccupationCodesFile"; 
    
    public static final String PROPERTIES_WORK_OCCUPATION_INDCEN_MIN_FIELD_NAME = "IndCenMin";
    public static final String PROPERTIES_WORK_OCCUPATION_INDCEN_MAX_FIELD_NAME = "IndCenMax";
    public static final String PROPERTIES_WORK_OCCUPATION_INDCEN_WORKOCC_FIELD_NAME = "workerOccupation";
	
	
    public static final String RANDOM_SEED_NAME = "Model.Random.Seed";

    public static final String HH_ID_FIELD_NAME              = "HHID";
    public String HH_HOME_TAZ_FIELD_NAME                     = "TAZ";
    public static final String HH_INCOME_CATEGORY_FIELD_NAME = "hinccat1";
    public static final String HH_INCOME_DOLLARS_FIELD_NAME  = "HINC";
    public static final String HH_WORKERS_FIELD_NAME         = "hworkers";
    public static final String HH_AUTOS_FIELD_NAME           = "VEHICL";
    public static final String HH_SIZE_FIELD_NAME            = "PERSONS";
    public static final String HH_TYPE_FIELD_NAME            = "HHT";
    public static final String HH_BLDGSZ_FIELD_NAME          = "BLDGSZ";
    
    public String[] hhHeadings = {
        HH_ID_FIELD_NAME,
        HH_HOME_TAZ_FIELD_NAME,
        HH_INCOME_CATEGORY_FIELD_NAME,
        HH_INCOME_DOLLARS_FIELD_NAME,
        HH_WORKERS_FIELD_NAME,
        HH_AUTOS_FIELD_NAME,
        HH_SIZE_FIELD_NAME,
        HH_TYPE_FIELD_NAME
    };
    
    
    
    public static final String PERSON_HH_ID_FIELD_NAME               = "HHID";
    public static final String PERSON_PERSON_ID_FIELD_NAME           = "PERID";
    public static final String PERSON_AGE_FIELD_NAME                 = "AGE";
    public static final String PERSON_GENDER_FIELD_NAME              = "SEX";
    public static final String PERSON_EMPLOYMENT_CATEGORY_FIELD_NAME = "pemploy";
    public static final String PERSON_STUDENT_CATEGORY_FIELD_NAME    = "pstudent";
    public static final String PERSON_TYPE_CATEGORY_FIELD_NAME       = "ptype";
    public static final String PERSON_EDUC_CATEGORY_FIELD_NAME       = "EDUC";
    public static final String PERSON_INDCEN_CATEGORY_FIELD_NAME     = "INDCEN";

    public static final String[] persHeadings = {
        PERSON_HH_ID_FIELD_NAME,
        PERSON_PERSON_ID_FIELD_NAME,
        PERSON_AGE_FIELD_NAME,
        PERSON_GENDER_FIELD_NAME,
        PERSON_EMPLOYMENT_CATEGORY_FIELD_NAME,
        PERSON_STUDENT_CATEGORY_FIELD_NAME,
        PERSON_TYPE_CATEGORY_FIELD_NAME,
        PERSON_EDUC_CATEGORY_FIELD_NAME,
        PERSON_INDCEN_CATEGORY_FIELD_NAME
    };
    
    
    public static final String PROPERTIES_HOUSEHOLD_TRACE_LIST = "Debug.Trace.HouseholdIdList";


    private static final String PROPERTIES_MIN_VALUE_OF_TIME_KEY                         = "HouseholdManager.MinValueOfTime";
    private static final String PROPERTIES_MAX_VALUE_OF_TIME_KEY                         = "HouseholdManager.MaxValueOfTime";
    private static final String PROPERTIES_MEAN_VALUE_OF_TIME_VALUES_KEY                 = "HouseholdManager.MeanValueOfTime.Values"; 
    private static final String PROPERTIES_MEAN_VALUE_OF_TIME_INCOME_LIMITS_KEY          = "HouseholdManager.MeanValueOfTime.Income.Limits"; 
    private static final String PROPERTIES_HH_VALUE_OF_TIME_MULTIPLIER_FOR_UNDER_18_KEY  = "HouseholdManager.HH.ValueOfTime.Multiplier.Under18";  
    private static final String PROPERTIES_MEAN_VALUE_OF_TIME_MULTIPLIER_FOR_MU_KEY      = "HouseholdManager.Mean.ValueOfTime.Multiplier.Mu";  
    private static final String PROPERTIES_VALUE_OF_TIME_LOGNORMAL_SIGMA_KEY             = "HouseholdManager.ValueOfTime.Lognormal.Sigma";  
    
    protected float hhValueOfTimeMultiplierForPersonUnder18; 
    protected double meanValueOfTimeMultiplierBeforeLogForMu; 
    protected double valueOfTimeLognormalSigma;
    
    protected float minValueOfTime; 
    protected float maxValueOfTime; 
    protected float[] meanValueOfTime;  
    protected int[] incomeDollarLimitsForValueOfTime;                    // upper limits of income ranges; 0-based; length of array gives the number of income segments;
    protected LognormalDist[] valueOfTimeDistribution; 

    
    protected HashMap<String, String> propertyMap;
    
    protected String projectDirectory;
    protected String outputHouseholdFileName;
    protected String outputPersonFileName;

    protected TazDataIf tazDataManager;
    protected ModelStructure modelStructure;

    protected TableDataSet hhTable;
    protected TableDataSet personTable;

    protected HashSet<Integer> householdTraceSet;

    protected HouseholdIf[] fullHhArray;
    protected int[] hhIndexArray;
    protected int numberOfHouseholds;

    protected int inputRandomSeed;
    protected int numPeriods;
    protected int firstPeriod;


    protected float sampleRate;
    protected int sampleSeed;
    
    private TableDataSet workerOccupationCodes;
  

    
    public HouseholdDataManager() {
    }

    
    
    /**
     * Associate data in hh and person TableDataSets read from synthetic population files with
     * Household objects and Person objects with Households.
     */
    protected abstract void mapTablesToHouseholdObjects();



    public String testRemote() {
        System.out.println("testRemote() called by remote process.");
        return String.format("testRemote() method in %s called.", this.getClass().getCanonicalName() );
    }


    public void setDebugHhIdsFromHashmap () {

        householdTraceSet = new HashSet<Integer>();

        // get the household ids for which debug info is required
        String householdTraceStringList = propertyMap.get( PROPERTIES_HOUSEHOLD_TRACE_LIST );

        if ( householdTraceStringList != null ) {
            StringTokenizer householdTokenizer = new StringTokenizer(householdTraceStringList,",");
            while(householdTokenizer.hasMoreTokens()){
                String listValue = householdTokenizer.nextToken();
                int idValue = Integer.parseInt( listValue.trim() );
                householdTraceSet.add( idValue );
            }
        }
        
        logger.info("Household Trace Set: " + householdTraceSet); 
    }



    public void setupHouseholdDataManager( ModelStructure modelStructure, TazDataIf tazDataManager, String inputHouseholdFileName, String inputPersonFileName ) {

        this.tazDataManager = tazDataManager;
        this.modelStructure = modelStructure;

        //get HH zone field name
        HH_HOME_TAZ_FIELD_NAME = propertyMap.get(PROPERTIES_SYNPOP_INPUT_HH_FIELD_NAME );
        
        // read synthetic population files
        readHouseholdData(inputHouseholdFileName);
        readPersonData(inputPersonFileName);
    	
        // Set the seed for the JVM default SeededRandom object - should only be used to set the order for the
        // HH index array so that hhs can be processed in an arbitrary order as opposed to the order imposed by
        // the synthetic population generator.
        // The seed was set as a command line argument for the model run, or the default if no argument supplied
        SeededRandom.setSeed( sampleSeed );
        
        // the seed read from the properties file controls seeding the Household object random number generator objects.
        inputRandomSeed = Integer.parseInt( propertyMap.get( HouseholdDataManager.RANDOM_SEED_NAME ) );

        // map synthetic population table data to objects to be used by CT-RAMP
    	mapTablesToHouseholdObjects();
    	hhTable = null;
    	personTable = null;
    	
        numberOfHouseholds = fullHhArray.length;
        logPersonSummary( fullHhArray );            

        setTraceHouseholdSet();

    }
    
    


    public void setPropertyFileValues ( HashMap<String, String> propertyMap ) {
        
        String propertyValue = "";
        this.propertyMap = propertyMap;

        // save the project specific parameters in class attributes
        this.projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );

        setDebugHhIdsFromHashmap ();

        propertyValue = propertyMap.get( CtrampApplication.PROPERTIES_SCHEDULING_NUMBER_OF_TIME_PERIODS );
        if ( propertyValue == null )
            numPeriods = 0;
        else
            numPeriods = Integer.parseInt( propertyValue );

        propertyValue = propertyMap.get( CtrampApplication.PROPERTIES_SCHEDULING_FIRST_TIME_PERIOD );
        if ( propertyValue == null )
            firstPeriod = 0;
        else
            firstPeriod = Integer.parseInt( propertyValue );

    }


    public int[] getRandomOrderHhIndexArray( int numHhs ) {
        
        int[] data = new int[numHhs];
        for ( int i=0; i < numHhs; i++ )
            data[i] = (int)(100000000*SeededRandom.getRandom());

        return IndexSort.indexSort( data );

    }


    public int[] getHomeTazOrderHhIndexArray( int[] hhSortArray ) {
        return IndexSort.indexSort( hhSortArray );
    }


    private void resetRandom(HouseholdIf h, int count) {
        // get the household's Random
        Random r = h.getHhRandom();

        int seed = inputRandomSeed + h.getHhId();
        r.setSeed( seed );

        // select count Random draws to reset this household's Random to it's state prior to
        // the model run for which model results were stored in HouseholdDataManager.
        for ( int i=0; i < count; i++ )
            r.nextDouble();

        // reset the randomCount for the household's Random
        h.setHhRandomCount(count);
    }


    public void resetUwslRandom( int iter ) {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current random count for the end of the shadow price iteration passed in.
                // this value was set at the end of UsualWorkSchoolLocation model step for the given iter.
                // if < 0, random count should be set to 0.
                int uwslCount = 0;
                if ( iter >= 0 ) {
                    uwslCount = tempHhs[r].getUwslRandomCount( iter );
                }

                // draw uwslCount random numbers from the household's Random
                resetRandom(tempHhs[r], uwslCount);
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetAoRandom(  int iter ) {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int aoCount = tempHhs[r].getUwslRandomCount( iter );

                // draw stlCount random numbers
                resetRandom( tempHhs[r], aoCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetFpRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int fpCount = tempHhs[r].getAoRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], fpCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetCdapRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int cdapCount = tempHhs[r].getFpRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], cdapCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetImtfRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int imtfCount = tempHhs[r].getCdapRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], imtfCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetImtodRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int imtodCount = tempHhs[r].getImtfRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], imtodCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetImmcRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to mode choice model from the Household object.
                // this value was set at the end of time of day step.
                int immcCount = tempHhs[r].getImtodRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], immcCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }

    
    public void resetJtfRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int jtfCount = tempHhs[r].getImtodRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], jtfCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetJtlRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int jtlCount = tempHhs[r].getJtfRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], jtlCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetJtodRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int jtodCount = tempHhs[r].getJtlRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], jtodCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }

    public void resetJmcRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to mode choice model from the Household object.
                // this value was set at the end of time of day step.
                int jmcCount = tempHhs[r].getJtodRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], jmcCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }

    public void resetInmtfRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int inmtfCount = tempHhs[r].getJtodRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], inmtfCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetInmtlRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int inmtlCount = tempHhs[r].getInmtfRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], inmtlCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetInmtodRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int inmtodCount = tempHhs[r].getInmtlRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], inmtodCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetInmmcRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to mode choice model from the Household object.
                // this value was set at the end of time of day step.
                int inmmcCount = tempHhs[r].getInmtodRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], inmmcCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }
    
    
    public void resetAwfRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int awfCount = tempHhs[r].getInmtodRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], awfCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetAwlRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int awlCount = tempHhs[r].getAwfRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], awlCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetAwtodRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int awtodCount = tempHhs[r].getAwlRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], awtodCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetAwmcRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to mode choice model from the Household object.
                // this value was set at the end of time of day step.
                int awmcCount = tempHhs[r].getAwtodRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], awmcCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }

    public void resetStfRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int stfCount = tempHhs[r].getAwtodRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], stfCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;

        }

    }


    public void resetStlRandom() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ ) {
                // get the current count prior to stop location model from the Household object.
                // this value was set at the end of stop frequency model step.
                int stlCount = tempHhs[r].getStfRandomCount();

                // draw stlCount random numbers
                resetRandom( tempHhs[r], stlCount );
            }
            setHhArray( tempHhs, startRange );

            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }
        
    }


    // this is called at the end of UsualWorkSchoolLocation model step.
    public void setUwslRandomCount( int iter ) {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setUwslRandomCount( iter, tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of Auto Ownership model step.
    public void setAoRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setAoRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of Auto Ownership model step.
    public void setFpRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setFpRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of Coordinated Daily Activity Pattern model step.
    public void setCdapRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setCdapRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of Individual Mandatory Tour Frequency model step.
    public void setImtfRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setImtfRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of Individual Mandatory Tour Departure and Duration model step.
    public void setImtodRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setImtodRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of At-work Subtour Frequency model step.
    public void setAwfRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setAwfRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of At-work Subtour Location Choice model step.
    public void setAwlRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setAwlRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of At-work Subtour Departure time, duration and mode choice model step.
    public void setAwtodRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setAwtodRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of Joint Tour Frequency model step.
    public void setJtfRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setJtfRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }


    // this is called at the end of Joint Tour Destination Choice model step.
    public void setJtlRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setJtlRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }

    // this is called at the end of Joint Tour departure and duration Choice model step.
    public void setJtodRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setJtodRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }

    // this is called at the end of Individual non-mandatory Tour frequency Choice model step.
    public void setInmtfRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setInmtfRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }

    // this is called at the end of Individual non-mandatory Tour destination choice Choice model step.
    public void setInmtlRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setInmtlRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }

    // this is called at the end of Individual non-mandatory Tour departure and duration choice Choice model step.
    public void setInmtodRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setInmtodRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }

    // this is called at the end of Stop Frequency Choice model step.
    public void setStfRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setStfRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }

    // this is called at the end of Stop Location Choice model step.
    public void setStlRandomCount() {

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setStlRandomCount( tempHhs[r].getHhRandomCount() );
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

    }



    /**
     *  set the hh id for which debugging info from choice models applied to this household will be logged if debug logging.
     */
    public void setDebugHouseholdId( int debugHhId, boolean value ) {
        int index = hhIndexArray[debugHhId];
        HouseholdIf tempHh = getHhArrayElement( index );
        tempHh.setDebugChoiceModels(value);
        setHhArrayElement( tempHh, index );
    }

    
    /**
     * Sets the HashSet used to trace households for debug purposes and sets the
     * debug switch for each of the listed households. Also sets
     */
    public void setTraceHouseholdSet() {
        
        // loop through the households in the set and set the trace switches
        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            for ( int r=0; r < tempHhs.length; r++ )
                tempHhs[r].setDebugChoiceModels(false);
            setHhArray( tempHhs, startRange );
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

        
        for( int id : householdTraceSet ) {
            int index = hhIndexArray[id];
            HouseholdIf tempHh = getHhArrayElement( index );
            tempHh.setDebugChoiceModels(true);
            setHhArrayElement( tempHh, index );
        }
        
    }
    


    /**
     * Sets the sample rate used to run the model for a portion of the households.
     * @param sampleRate, proportion of total households for which to run the model [0.0, 1.0].
     */
    public void setHouseholdSampleRate( float sampleRate, int sampleSeed ) {
        this.sampleRate = sampleRate;
        this.sampleSeed = sampleSeed;
    }
    


    public void setHhArray(HouseholdIf[] hhArray) {
        fullHhArray = hhArray;
    }
    
    
    public void setHhArray( HouseholdIf[] tempHhs, int startIndex ) {
        for (int i=0; i < tempHhs.length; i++) {
            fullHhArray[startIndex + i] = tempHhs[i]; 
        }
    }

    public void setHhArrayElement( HouseholdIf tempHh, int index ) {
        fullHhArray[index] = tempHh;
    }
    
    
    /**
     * return the array of Household objects holding the synthetic population and choice model outcomes.
     * @return hhs
     */
    public HouseholdIf[] getHhArray() {
        return fullHhArray;
    }
    

    public HouseholdIf[] getHhArray(int first, int last) {
        HouseholdIf[] tempHhs = null;
        tempHhs = new HouseholdIf[last-first+1];

        for (int i=0; i < tempHhs.length; i++) {
            tempHhs[i] = fullHhArray[first + i]; 
        }
        
        return tempHhs;
    }

    
    public HouseholdIf getHhArrayElement( int index ) {
        return fullHhArray[index];
    }
    
    
    public int getArrayIndex( int hhId ){
        int i = hhIndexArray[hhId];
        return i;
    }
    
    /**
     * return the number of household objects read from the synthetic population.
     * @return
     */
    public int getNumHouseholds() {
        return numberOfHouseholds;
    }
    
    

    /**
     * set walk segment (0-none, 1-short, 2-long walk to transit access) for the origin for this tour
     */
    public short getInitialOriginWalkSegment (short taz, double randomNumber) {
        double[] proportions = tazDataManager.getZonalWalkPercentagesForTaz( taz );
        return (short)ChoiceModelApplication.getMonteCarloSelection(proportions, randomNumber);
    }




    private void readHouseholdData( String inputHouseholdFileName ) {
        
        // construct input household file name from properties file values
        String fileName = projectDirectory + "/" + inputHouseholdFileName;

        try{
            logger.info( "reading popsyn household data file." );
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet( "," + reader.getDelimSet() );
            hhTable = reader.readFile(new File( fileName ));
        }
        catch(Exception e){
            logger.fatal( String.format( "Exception occurred reading synthetic household data file: %s into TableDataSet object.", fileName ) );
            throw new RuntimeException(e);
        }
        
    }

    
    private void readPersonData( String inputPersonFileName ) {
        
        // construct input person file name from properties file values
        String fileName = projectDirectory + "/" + inputPersonFileName;

        try{
            logger.info( "reading popsyn person data file." );
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet( "," + reader.getDelimSet() );
            personTable = reader.readFile(new File( fileName ));
        }
        catch(Exception e){
            logger.fatal( String.format( "Exception occurred reading synthetic person data file: %s into TableDataSet object.", fileName ) );
            throw new RuntimeException(e);
        }
        
    }


    
    
    public void logPersonSummary( HouseholdIf[] hhs ) {

        HashMap<String, HashMap<String, int[]>> summaryResults;

        summaryResults = new HashMap<String, HashMap<String,int[]>>();

        for(int i=0; i < hhs.length; ++i){

            HouseholdIf household = hhs[i];
            
            PersonIf[] personArray = household.getPersons();
            for (int j = 1; j < personArray.length; ++j) {
                PersonIf person = personArray[j];
                String personType = person.getPersonType();

                String employmentStatus = person.getPersonEmploymentCategory();
                String studentStatus = person.getPersonStudentCategory();
                int age = person.getAge();
                int ageCategory;
                if (age <= 5) {
                    ageCategory = 0;
                } else if (age <= 15) {
                    ageCategory = 1;
                } else if (age <= 18) {
                    ageCategory = 2;
                } else if (age <= 24) {
                    ageCategory = 3;
                }   else if (age <= 44) {
                    ageCategory = 4;
                } else if (age <= 64) {
                    ageCategory = 5;
                } else {
                    ageCategory = 6;
                }

                if (summaryResults.containsKey(personType)) {
                    //have person type
                    if (summaryResults.get(personType).containsKey(employmentStatus)) {
                        //have employment category
                        summaryResults.get(personType).get(employmentStatus)[ageCategory]+=1;
                    } else {
                        //don't have employment category
                        summaryResults.get(personType).put(employmentStatus, new int[7]);
                        summaryResults.get(personType).get(employmentStatus)[ageCategory]+=1;
                    }
                    if (summaryResults.get(personType).containsKey(studentStatus)) {
                        //have student category
                        summaryResults.get(personType).get(studentStatus)[ageCategory]+=1;
                    } else {
                        //don't have student category
                        summaryResults.get(personType).put(studentStatus, new int[7]);
                        summaryResults.get(personType).get(studentStatus)[ageCategory]+=1;
                    }
                } else {
                    //don't have person type
                    summaryResults.put(personType, new HashMap<String,int[]>());
                    summaryResults.get(personType).put(studentStatus, new int[7]);
                    summaryResults.get(personType).get(studentStatus)[ageCategory]+=1;
                    summaryResults.get(personType).put(employmentStatus, new int[7]);
                    summaryResults.get(personType).get(employmentStatus)[ageCategory]+=1;
                }
            }
        }
        String headerRow = String.format("%5s\t", "Age\t");
        for (String empCategory: PersonIf.employmentCategoryNameArray) {
            headerRow += String.format("%16s\t", empCategory );
        }
        for (String stuCategory: PersonIf.studentCategoryNameArray) {
            headerRow += String.format("%16s\t", stuCategory );
        }
        String[] ageCategories = {"0-5","6-15","16-18","19-24","25-44","45-64","65+"};

        for (String personType: summaryResults.keySet()) {

            logger.info("Summary for person type: " + personType);

            logger.info(headerRow);
            String row = "";

            HashMap<String, int[]> personTypeSummary = summaryResults.get(personType);

            for (int j = 0; j<ageCategories.length;++j) {
                row = String.format("%5s\t", ageCategories[j]);
                for (String empCategory: PersonIf.employmentCategoryNameArray) {
                    if (personTypeSummary.containsKey(empCategory)) {
                        row += String.format("%16d\t", personTypeSummary.get(empCategory)[j]);
                    } else row += String.format("%16d\t", 0);
                }
                for (String stuCategory: PersonIf.studentCategoryNameArray) {
                    if (personTypeSummary.containsKey(stuCategory)) {
                        row += String.format("%16d\t", personTypeSummary.get(stuCategory)[j]);
                    } else row += String.format("%16d\t", 0);
                }
                logger.info(row);
            }

        }

    }



    
    public int[][][] getTourPurposePersonsByHomeZone( String[] purposeList ) {

        int numZones = tazDataManager.getNumberOfZones();
        int numWalkSubzones = tazDataManager.getNumberOfSubZones();
        
        int[][][] personsWithMandatoryPurpose = new int[purposeList.length][numZones+1][numWalkSubzones];

        
        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            // hhs is dimesioned to number of households + 1.
            int count=0;
            for ( int r=0; r < tempHhs.length; r++ ) {

                PersonIf[] persons = tempHhs[r].getPersons();

                int homeZone = tempHhs[r].getHhTaz();
                int homeSubZone = tempHhs[r].getHhWalkSubzone();
                
                for ( int p=1; p < persons.length; p++) {

                    PersonIf person = persons[p];
                    
                    int purposeIndex = -1;
                    try {

                        if ( person.getPersonIsWorker() == 1 ) {
                            
                            purposeIndex = person.getWorkLocationPurposeIndex();
                            personsWithMandatoryPurpose[purposeIndex][homeZone][homeSubZone] ++;

                        }

                        if ( person.getPersonIsPreschoolChild() == 1 || person.getPersonIsStudentDriving() == 1 || person.getPersonIsStudentNonDriving() == 1 ) {
                            
                            purposeIndex = person.getSchoolLocationPurposeIndex();
                            personsWithMandatoryPurpose[purposeIndex][homeZone][homeSubZone] ++;

                        }
                        else if ( person.getPersonIsUniversityStudent() == 1 ) {
                            
                            purposeIndex = person.getUniversityLocationPurposeIndex();
                            personsWithMandatoryPurpose[purposeIndex][homeZone][homeSubZone] ++;

                        }

                        count++;
                        
                    }
                    catch ( RuntimeException e ) {
                        logger.error ( String.format("exception caught summing workers/students by origin zone for household table record r=%d, startRange=%d, endRange=%d, count=%d.", r, startRange, endRange, count ) );
                        throw e;
                    }

                }
                
            } // r (households)

            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }
        
        return personsWithMandatoryPurpose;

    }


   

    
    public int[][] getIndividualNonMandatoryToursByHomeZoneSubZone( String purposeString ) {

        // dimension the array
        int numZones = tazDataManager.getNumberOfZones();
        int numWalkSubzones = tazDataManager.getNumberOfSubZones();
        
        int[][] individualNonMandatoryTours = new int[numZones+1][numWalkSubzones];

        // hhs is dimesioned to number of households + 1.
        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            // hhs is dimesioned to number of households + 1.
            int count = 0;
            for ( int r=0; r < tempHhs.length; r++ ) {


                PersonIf[] persons = tempHhs[r].getPersons();
        
                for ( int p=1; p < persons.length; p++) {
        
                    PersonIf person = persons[p];
                    
                    ArrayList<TourIf> it = person.getListOfIndividualNonMandatoryTours();
        
                    try {
        
                        if ( it.size() == 0 )
                            continue;
        
                        for ( TourIf tour : it ) {
                            // increment the segment count if it's the right purpose
                            String tourPurpose = tour.getTourPrimaryPurpose();
                            if ( purposeString.startsWith( tourPurpose ) ) {
                                int homeZone = tempHhs[r].getHhTaz();
                                int homeSubZone = tempHhs[r].getHhWalkSubzone();
                                individualNonMandatoryTours[homeZone][homeSubZone] ++;
                                count++;
                            }
                        }
        
                    }
                    catch ( RuntimeException e ) {
                        logger.error ( String.format("exception caught counting number of individualNonMandatory tours for purpose: %s, for household table record r=%d, personNum=%d, startRange=%d, endRange=%d, count=%d.", purposeString, r, person.getPersonNum(), startRange, endRange, count ) );
                        throw e;
                    }
        
                }

            } // r (households)

            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }
    
        return individualNonMandatoryTours;
        
    }



    public double[][][] getMandatoryToursByDestZoneSubZone() {

        // dimension the array
        int numZones = tazDataManager.getNumberOfZones();
        int numWalkSubzones = tazDataManager.getNumberOfSubZones();

        // get correspondence between mandatory purpose names and their array indices
        String[] mandatoryPurposes = modelStructure.getDcModelPurposeList( ModelStructure.MANDATORY_CATEGORY );
        HashMap<String,Integer> tourPurposeIndexMap = new HashMap<String,Integer>();
        for ( int p=0; p < mandatoryPurposes.length; p++ )
            tourPurposeIndexMap.put( mandatoryPurposes[p], p );
                
        double[][][] mandatoryTours = new double[mandatoryPurposes.length][numZones+1][numWalkSubzones];

        

        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            // hhs is dimesioned to number of households + 1.
            int count = 0;
            for ( int r=0; r < tempHhs.length; r++ ) {

                PersonIf[] persons = tempHhs[r].getPersons();
    
                for ( int p=1; p < persons.length; p++) {
    
                    PersonIf person = persons[p];
                    
                    String purposeName = "";
                    int purposeIndex = -1;
                    int destZone = -1;
                    int destSubZone = -1;
                    try {
    
                        if ( person.getPersonIsWorker() == 1 && person.getPersonWorkLocationZone() > 0 ) {
                            
                            purposeIndex = person.getWorkLocationPurposeIndex();
                            destZone = person.getPersonWorkLocationZone();
                            destSubZone = person.getPersonWorkLocationSubZone();
                            mandatoryTours[purposeIndex][destZone][destSubZone]++;
    
                        }
    
                        if ( person.getPersonIsPreschoolChild() == 1 || person.getPersonIsStudentDriving() == 1 || person.getPersonIsStudentNonDriving() == 1 && person.getPersonSchoolLocationZone() > 0 ) {
                            
                            purposeIndex = person.getSchoolLocationPurposeIndex();
                            destZone = person.getPersonSchoolLocationZone();
                            destSubZone = person.getPersonSchoolLocationSubZone();
                            mandatoryTours[purposeIndex][destZone][destSubZone]++;
    
                        }
                        else if ( person.getPersonIsUniversityStudent() == 1 && person.getPersonSchoolLocationZone() > 0 ) {
                            
                            purposeIndex = person.getUniversityLocationPurposeIndex();
                            destZone = person.getPersonSchoolLocationZone();
                            destSubZone = person.getPersonSchoolLocationSubZone();
                            mandatoryTours[purposeIndex][destZone][destSubZone]++;
    
                        }
    
                    }
                    catch ( RuntimeException e ) {
                        logger.error ( String.format("exception caught counting number of mandatory tour destinations for purpose: %s, for household table record r=%d, personNum=%d.", purposeName, r, person.getPersonNum() ) );
                        logger.error ( String.format("     r = %d.", r ) );
                        logger.error ( String.format("     household id = %d.", tempHhs[r] ) );
                        logger.error ( String.format("     personNum = %d.", person.getPersonNum() ) );
                        logger.error ( String.format("     purpose name = %s, purposeIndex = %d.", purposeName, purposeIndex ) );
                        logger.error ( String.format("     destZone = %d, destSubZone = %d.", destZone, destSubZone ) );
                        logger.error ( String.format("     startRange = %d, endRange = %d, count = %d.", startRange, endRange, count ) );
                    }
    
                }
            
            } // r (households)
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

        return mandatoryTours;
        
    }



    public int[][] getJointToursByHomeZoneSubZone( String purposeString ) {

        // dimension the array
        int numZones = tazDataManager.getNumberOfZones();
        int numWalkSubzones = tazDataManager.getNumberOfSubZones();

        int[][] jointTours = new int[numZones+1][numWalkSubzones];

        // hhs is dimesioned to number of households + 1.
        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            // hhs is dimesioned to number of households + 1.
            int count = 0;
            for ( int r=0; r < tempHhs.length; r++ ) {

                try {
    
                    TourIf[] jt = tempHhs[r].getJointTourArray();
    
                    if ( jt == null )
                        continue;
    
                    for ( int i=0; i < jt.length; i++ ) {
                        // increment the segment count if it's the right purpose
                        if ( jt[i].getTourPrimaryPurpose().equalsIgnoreCase( purposeString )) {
                            int homeZone = tempHhs[r].getHhTaz();
                            int homeSubZone = tempHhs[r].getHhWalkSubzone();
                            jointTours[homeZone][homeSubZone] ++;
                            count++;
                        }
                    }
    
                }
                catch ( RuntimeException e ) {
                    logger.error ( String.format("exception caught counting number of joint tours for purpose: %s, for household table record r=%d, startRange=%d, endRange=%d, count=%d.", purposeString, r, startRange, endRange, count ) );
                    throw e;
                }

            } // r (households)
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

        return jointTours;
    }


    public int[][] getAtWorkSubtoursByWorkZoneSubZone( String purposeString ) {

        // dimension the array
        int numZones = tazDataManager.getNumberOfZones();
        int numWalkSubzones = tazDataManager.getNumberOfSubZones();
        
        int[][] subtours = new int[numZones+1][numWalkSubzones];

        // hhs is dimesioned to number of households + 1.
        int startRange = 0;
        int endRange = 0;
        while ( endRange < getNumHouseholds() ) {
            
            endRange = startRange + NUMBER_OF_IN_MEMORY_HHS;
            if ( endRange + NUMBER_OF_IN_MEMORY_HHS > getNumHouseholds() )
                endRange = getNumHouseholds();
            
            HouseholdIf[] tempHhs = getHhArray( startRange, endRange-1 );
            // hhs is dimesioned to number of households + 1.
            int count = 0;
            for ( int r=0; r < tempHhs.length; r++ ) {


                PersonIf[] persons = tempHhs[r].getPersons();
    
                for ( int p=1; p < persons.length; p++) {
    
                    PersonIf person = persons[p];
                    
                    ArrayList<TourIf> subtourList = person.getListOfAtWorkSubtours();
    
                    try {
    
                        if ( subtourList.size() == 0 )
                            continue;
    
                        for ( TourIf tour : subtourList ) {
                            // increment the segment count if it's the right purpose
                            String tourPurpose = tour.getTourPrimaryPurpose();
                            if ( tourPurpose.startsWith( purposeString ) ) {
                                int workZone = tour.getTourOrigTaz();
                                int workSubZone = tour.getTourOrigWalkSubzone();
                                subtours[workZone][workSubZone] ++;
                                count++;
                            }
                        }
    
                    }
                    catch ( RuntimeException e ) {
                        logger.error ( String.format("exception caught counting number of at-work subtours for purpose: %s, for household table record r=%d, personNum=%d, startRange=%d, endRange=%d, count=%d.", purposeString, r, person.getPersonNum(), startRange, endRange, count ) );
                        throw e;
                    }
    
                }

            } // r (households)
            
            startRange += NUMBER_OF_IN_MEMORY_HHS;
            
        }

        return subtours;
    }

    /**
     * Assigns each individual person their own value of time, 
     * drawing from a lognormal distribution as a function of income.  
     */
    protected void setDistributedValuesOfTime () {
        
        // read in values from property file
        setValueOfTimePropertyFileValues(); 
        
        // set up the probability distributions
        for (int i=0; i < valueOfTimeDistribution.length; i++) {
            double mu = Math.log(meanValueOfTime[i] * meanValueOfTimeMultiplierBeforeLogForMu);
            valueOfTimeDistribution[i] = new LognormalDist(mu, valueOfTimeLognormalSigma); 
        }
        
        for(int i=0; i < fullHhArray.length; ++i){
            
            HouseholdIf household = fullHhArray[i];
            
            // each HH gets a VOT for consistency
            double rnum = household.getHhRandom().nextDouble();
            int incomeCategory = getIncomeIndexForValueOfTime( household.getIncomeInDollars() ); 
            double hhValueOfTime = valueOfTimeDistribution[incomeCategory-1].inverseF(rnum); 

            // constrain to logical min and max values
            if (hhValueOfTime < minValueOfTime) hhValueOfTime = minValueOfTime; 
            if (hhValueOfTime > maxValueOfTime) hhValueOfTime = maxValueOfTime; 
                        
            // adults get the full value, and children 2/3 (1-based)
            PersonIf[] personArray = household.getPersons();
            for (int j = 1; j < personArray.length; ++j) {
                PersonIf person = personArray[j];
                
                int age = person.getAge(); 
                if (age < 18)
                    person.setValueOfTime((float) hhValueOfTime * hhValueOfTimeMultiplierForPersonUnder18); 
                else
                    person.setValueOfTime((float) hhValueOfTime);              
            }
        }
    }
    
    /**
     * Assigns each individual person their own walk preferences
     */
    protected void setDistributedWalkPrefernces() throws IOException {
        
    	//parameters
    	float minWalkTimeWeight = (float) 3.5;
    	float maxWalkTimeWeight = (float) 1.0;
    	float minWalkSpeed      = (float) 1.0;
    	float maxWalkSpeed      = (float) 4.0;
    	float minMaxWalk        = (float) 0.5;
    	float maxMaxWalk        = (float) 3.0;
    	int minZscore = -3;
        int maxZscore = 3;
        
        //for binning people by walk propensity
        float walk_prop_cl_max  = (float) 0.3;
        float walk_prop_c2_max  = (float) 0.6;
        
        // read in walk preferences values from file
        TableDataSet walkPreferences;
    	String WALK_PREFERENCES_FILE_NAME = "HouseholdManager.WalkPreferences.FileName";
    	String fileName = propertyMap.get( WALK_PREFERENCES_FILE_NAME );
        CSVFileReader csvReader = new CSVFileReader();
        try {
        	logger.info("Reading " + fileName);
        	walkPreferences = csvReader.readFile( new File(fileName));
        } catch (IOException e) {
            logger.error ( "IOException reading file: " + WALK_PREFERENCES_FILE_NAME, e );
            throw new IOException();
        }

        // set up normal distribution
        NormalDist normalDistribution = new NormalDist(); 
        
        //loop thru hhs and persons
        for(int i=0; i < fullHhArray.length; ++i) {
            
            HouseholdIf household = fullHhArray[i];
            PersonIf[] personArray = household.getPersons();
            
            for (int j = 1; j < personArray.length; ++j) {
            
            	PersonIf person = personArray[j];
                int age = person.getAge();
              
                //get and truncate zscore value
            	double rnum = household.getHhRandom().nextDouble();
                float zscore = (float)normalDistribution.inverseF(rnum);
                zscore = zscore < minZscore ? minZscore : zscore;
                zscore = zscore > maxZscore ? maxZscore : zscore;
                
                //get propensities by age
                walkPreferences.buildIndex(1);  //age column
                float min = walkPreferences.getIndexedValueAt(age, "Lower 0.5%");
                float median = walkPreferences.getIndexedValueAt(age, "Median 50%");
                float max = walkPreferences.getIndexedValueAt(age, "Upper 99.5%");
                
                float propensity;
                float lower_value = median + (min - median) * zscore / minZscore;
                float upper_value = median + (max - median) * zscore / maxZscore;
                propensity = zscore < 0 ? lower_value : upper_value;

                //parameters
                float walkTime = propensity * maxWalkTimeWeight + (1 - propensity) * minWalkTimeWeight;
                float walkSpeed = propensity * maxWalkSpeed + (1 - propensity) * minWalkSpeed;
                float maxWalk = propensity * maxMaxWalk + (1 - propensity) * minMaxWalk;
                
                //set values
                if (propensity < walk_prop_cl_max) {
                	person.setWalkPropClass(1);
                } else if(propensity < walk_prop_c2_max) {
                	person.setWalkPropClass(2);
                } else {
                	person.setWalkPropClass(3);
                }
                
                person.setWalkTimeWeight(walkTime);
                person.setWalkSpeed(walkSpeed);
                person.setMaxWalk(maxWalk);
                
                
            }
        }
    }
    
    
    protected void setUserClass () {
        
        //parameters
    	double constantC1 = 1.1;
    	double age35pC1 = 0.06;
    	double income35to60C1 = 0.14;
    	double income60pC1 = -1.25;
    	double pnrC1 = -1.85;
    	double knrC1 = -1.62;
    	double workC1 = -0.61;
    	
    	double constantC2 = 0.34;
    	double age35pC2 = -0.24;
    	double income35to60C2 = 0.19;
    	double income60pC2 = -1.25;
    	double pnrC2 = -1.85;
    	double knrC2 = 0.34;
    	double workC2 = -0.71;
    	
    	//user class models
    	final String[] model_names = {"user_class_work_walk","user_class_work_pnr","user_class_work_knr","user_class_non_work_walk","user_class_non_work_pnr","user_class_non_work_knr"};
        
        for(int i=0; i < fullHhArray.length; ++i){
            
            HouseholdIf household = fullHhArray[i];
            PersonIf[] personArray = household.getPersons();
            
            for (int j = 1; j < personArray.length; ++j) {
                PersonIf person = personArray[j];
                
                int age = person.getAge();
                int income = household.getIncomeInDollars();
                
                int isAge35p = (age >= 35) ? 1 : 0;
                int isIncome35to60 = (income >= 35000) & (income < 60000) ? 1 : 0;
                int isIncome60p = (income >= 60000) ? 1 : 0;
                
                //non-work
                double walkUtilC1 = constantC1 + age35pC1 * isAge35p + income35to60C1 * isIncome35to60 + income60pC1 * isIncome60p;
                double pnrUtilC1 = constantC1 + age35pC1 * isAge35p + income35to60C1 * isIncome35to60 + income60pC1 * isIncome60p + pnrC1;
                double knrUtilC1 = constantC1 + age35pC1 * isAge35p + income35to60C1 * isIncome35to60 + income60pC1 * isIncome60p + knrC1;
                
                double walkUtilC2 = constantC2 + age35pC2 * isAge35p + income35to60C2 * isIncome35to60 + income60pC2 * isIncome60p;
                double pnrUtilC2 = constantC2 + age35pC2 * isAge35p + income35to60C2 * isIncome35to60 + income60pC2 * isIncome60p + pnrC2;
                double knrUtilC2 = constantC2 + age35pC2 * isAge35p + income35to60C2 * isIncome35to60 + income60pC2 * isIncome60p + knrC2;
                
                //work
                double walkUtilC1_Work = walkUtilC1 + workC1;
                double pnrUtilC1_Work = pnrUtilC1 + workC1;
                double knrUtilC1_Work = knrUtilC1 + workC1;
                
                double walkUtilC2_Work = walkUtilC2 + workC2;
                double pnrUtilC2_Work = pnrUtilC2 + workC2;
                double knrUtilC2_Work = knrUtilC2 + workC2;
                
                // draw number
                double rnum = household.getHhRandom().nextDouble();
                
            	//create simple models
                LogitModel[] models = new LogitModel[model_names.length];                
                for(int k=0; k<models.length; k++) {
                	
                	//create alternatives
                	models[k] = new LogitModel(model_names[k]);
                	ConcreteAlternative c1 = new ConcreteAlternative("1",1);
                	ConcreteAlternative c2 = new ConcreteAlternative("2",2);
                	ConcreteAlternative c3 = new ConcreteAlternative("3",3);
                	models[k].addAlternative(c1);
                	models[k].addAlternative(c2);
                	models[k].addAlternative(c3);
                	
                	if(models[k].getName()=="user_class_work_walk") {
                		c1.setUtility(walkUtilC1_Work);
                    	c2.setUtility(walkUtilC2_Work);
                    	c3.setUtility(0); //class 3 util is the base util
                    } else if (models[k].getName()=="user_class_work_pnr") {
                		c1.setUtility(pnrUtilC1_Work);
                    	c2.setUtility(pnrUtilC2_Work);
                    	c3.setUtility(0); //class 3 util is the base util
                    } else if (models[k].getName()=="user_class_work_knr") {
                		c1.setUtility(knrUtilC1_Work);
                    	c2.setUtility(knrUtilC2_Work);
                    	c3.setUtility(0); //class 3 util is the base util
                    } else if (models[k].getName()=="user_class_non_work_walk") {
                		c1.setUtility(walkUtilC1);
                    	c2.setUtility(walkUtilC2);
                    	c3.setUtility(0); //class 3 util is the base util
                    } else if (models[k].getName()=="user_class_non_work_pnr") {
                		c1.setUtility(pnrUtilC1);
                    	c2.setUtility(pnrUtilC2);
                    	c3.setUtility(0); //class 3 util is the base util
                	} else if (models[k].getName()=="user_class_non_work_knr") {
	            		c1.setUtility(knrUtilC1);
	                	c2.setUtility(knrUtilC2);
	                	c3.setUtility(0); //class 3 util is the base util
	                }
                	
                	//make selection
                	double logsum = models[k].getUtility();
                	models[k].calculateProbabilities();
                	int user_class = Integer.parseInt(models[k].chooseAlternative(rnum).getName());
                	person.setUserClass(models[k].getName(), user_class);
                	
                }
                
            }
        }
    }
    
    public void readWorkerOccupationFile () throws IOException {
    	
    	
    	String fileName = propertyMap.get( PROPERTIES_WORK_OCCUPATION_CODES );
        CSVFileReader csvReader = new CSVFileReader();
        try {
        	logger.info("Reading " + fileName);
        	workerOccupationCodes = csvReader.readFile( new File(fileName));
        } catch (IOException e) {
            logger.error ( "IOException reading file: " + PROPERTIES_WORK_OCCUPATION_CODES, e );
            throw new IOException();
        }
    }
    
    public int getPersonWorkOccupation( int industryCensus ) {
    	
    	int min[] = workerOccupationCodes.getColumnAsInt(PROPERTIES_WORK_OCCUPATION_INDCEN_MIN_FIELD_NAME);
		int max[] = workerOccupationCodes.getColumnAsInt(PROPERTIES_WORK_OCCUPATION_INDCEN_MAX_FIELD_NAME);
		int occ[] = workerOccupationCodes.getColumnAsInt(PROPERTIES_WORK_OCCUPATION_INDCEN_WORKOCC_FIELD_NAME);
		
    	for (int i=0; i< min.length; i++) {
    		if(industryCensus >= min[i] & industryCensus <= max[i]) {
    			return occ[i]; 
    		}
    	}
    	 
    	return -1; 
    }
    
    /**
     * Sets additional properties specific to MTC, included distributed value-of-time information
     */
    private void setValueOfTimePropertyFileValues ( ) {

        boolean errorFlag = false;
        String propertyValue = "";
        
        propertyValue = propertyMap.get( PROPERTIES_MIN_VALUE_OF_TIME_KEY );
        if ( propertyValue == null ) {
            logger.error( "property file key missing: " + PROPERTIES_MIN_VALUE_OF_TIME_KEY + ", not able to set min value of time value." );
            errorFlag = true;
        }
        else
            minValueOfTime = Float.parseFloat( propertyValue );  
        
        propertyValue = propertyMap.get( PROPERTIES_MAX_VALUE_OF_TIME_KEY );
        if ( propertyValue == null ) {
            logger.error( "property file key missing: " + PROPERTIES_MAX_VALUE_OF_TIME_KEY + ", not able to set max value of time value." );
            errorFlag = true;
        }
        else
            maxValueOfTime = Float.parseFloat( propertyValue );  

        // mean values of time by income category are specified as a "comma-sparated" list of float values
        // the number of mean values in the lsit determines the number of income categories for value of time
        // the number of upper limit income dollar values is expected to be number of mean values - 1.
        int numIncomeCategories = -1;
        String meanValueOfTimesPropertyValue = propertyMap.get( PROPERTIES_MEAN_VALUE_OF_TIME_VALUES_KEY );
        if ( meanValueOfTimesPropertyValue == null ) {
            logger.error( "property file key missing: " + PROPERTIES_MEAN_VALUE_OF_TIME_VALUES_KEY + ", not able to set mean value of time values." );
            errorFlag = true;
        }
        else {
            
            ArrayList<Float> valueList = new ArrayList<Float>();
            StringTokenizer valueTokenizer = new StringTokenizer( meanValueOfTimesPropertyValue, "," );
            while( valueTokenizer.hasMoreTokens() ) {
                String listValue = valueTokenizer.nextToken();
                float value = Float.parseFloat( listValue.trim() );
                valueList.add( value );
            }

            numIncomeCategories = valueList.size();
            meanValueOfTime = new float[numIncomeCategories];  
            valueOfTimeDistribution = new LognormalDist[numIncomeCategories];
            
            for ( int i=0; i < numIncomeCategories; i++ )
                meanValueOfTime[i] = valueList.get(i);
        }
        
        // read the upper limit values for value of time income ranges.
        // there should be exactly 1 less than the number of mean value of time values - any other value is an error.
        String valueOfTimeIncomesPropertyValue = propertyMap.get( PROPERTIES_MEAN_VALUE_OF_TIME_INCOME_LIMITS_KEY );
        if ( valueOfTimeIncomesPropertyValue == null ) {
            logger.error( "property file key missing: " + PROPERTIES_MEAN_VALUE_OF_TIME_INCOME_LIMITS_KEY + ", not able to set upper limits for value of time income ranges." );
            errorFlag = true;
        }
        else {
            
            ArrayList<Integer> valueList = new ArrayList<Integer>();
            StringTokenizer valueTokenizer = new StringTokenizer( valueOfTimeIncomesPropertyValue, "," );
            while( valueTokenizer.hasMoreTokens() ) {
                String listValue = valueTokenizer.nextToken();
                int value = Integer.parseInt( listValue.trim() );
                valueList.add( value );
            }

            int numIncomeValues = valueList.size();
            if ( numIncomeValues != (numIncomeCategories - 1) ) {
                Exception e = new RuntimeException();
                logger.error( "an error occurred reading properties file values for distributed value of time calculations." );
                logger.error( "the mean value of time values property=" + meanValueOfTimesPropertyValue + " specifies " + numIncomeCategories + " mean values, thus " + numIncomeCategories + " income ranges." );
                logger.error( "the value of time income range values property=" + valueOfTimeIncomesPropertyValue + " specifies " + numIncomeValues + " income range limit values." );
                logger.error( "there should be exactly " + (numIncomeCategories-1) +  " income range limit values for " + numIncomeCategories + " mean value of time values.", e );
                System.exit(-1);
            }
            
            // set the income dollar value upper limits for value of time income ranges
            incomeDollarLimitsForValueOfTime = new int[numIncomeValues+1];  
            for ( int i=0; i < numIncomeValues; i++ )
                incomeDollarLimitsForValueOfTime[i] = valueList.get(i);
            
            incomeDollarLimitsForValueOfTime[numIncomeValues] = Integer.MAX_VALUE;
        }
        
        propertyValue = propertyMap.get( PROPERTIES_HH_VALUE_OF_TIME_MULTIPLIER_FOR_UNDER_18_KEY );
        if ( propertyValue == null ) {
            logger.error( "property file key missing: " + PROPERTIES_HH_VALUE_OF_TIME_MULTIPLIER_FOR_UNDER_18_KEY + ", not able to set hh value of time multiplier for kids in hh under age 18." );
            errorFlag = true;
        }
        else
            hhValueOfTimeMultiplierForPersonUnder18 = Float.parseFloat( propertyValue );  
        
        propertyValue = propertyMap.get( PROPERTIES_MEAN_VALUE_OF_TIME_MULTIPLIER_FOR_MU_KEY );
        if ( propertyValue == null ) {
            logger.error( "property file key missing: " + PROPERTIES_MEAN_VALUE_OF_TIME_MULTIPLIER_FOR_MU_KEY + ", not able to set lognormal distribution mu parameter multiplier." );
            errorFlag = true;
        }
        else
            meanValueOfTimeMultiplierBeforeLogForMu = Float.parseFloat( propertyValue );  
        
        propertyValue = propertyMap.get( PROPERTIES_VALUE_OF_TIME_LOGNORMAL_SIGMA_KEY );
        if ( propertyValue == null ) {
            logger.error( "property file key missing: " + PROPERTIES_VALUE_OF_TIME_LOGNORMAL_SIGMA_KEY + ", not able to set lognormal distribution sigma parameter." );
            errorFlag = true;
        }
        else
            valueOfTimeLognormalSigma = Float.parseFloat( propertyValue );  

        if ( errorFlag ) {
            Exception e = new RuntimeException();
            logger.error( "errors occurred reading properties file values for distributed value of time calculations.", e );
            System.exit(-1);
        }
        
   }

    private int getIncomeIndexForValueOfTime( int incomeInDollars ) {
        int returnValue = -1;
        for (int i=0; i < incomeDollarLimitsForValueOfTime.length; i++ ) {
            if ( incomeInDollars < incomeDollarLimitsForValueOfTime[i] ) {
                // return a 1s based index value
                returnValue = i + 1;
                break;
            }
        }
        
        return returnValue;
    }
    
}