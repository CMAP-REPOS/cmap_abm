package com.pb.cmap.tourBased;

import com.pb.models.ctrampIf.ModelStructure;
import com.pb.cmap.ctramp.Person;
import com.pb.cmap.ctramp.Household;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.ctrampIf.HouseholdDataManager;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TazDataIf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

/**
 * @author Jim Hicks
 *
 * Class for managing household and person object data read from synthetic population files.
 */
public class CmapHouseholdDataManager extends HouseholdDataManager {

    public static final String HH_DATA_SERVER_NAME = CmapHouseholdDataManager.class.getCanonicalName();
    public static final String HH_DATA_SERVER_ADDRESS = "127.0.0.1";
    public static final int HH_DATA_SERVER_PORT = 1139;


    

    public CmapHouseholdDataManager() {
        super ();
    }




    // this method gets called by ArcTourBasedModel, if the HouseholdDataManagerIf instance is and ArcHouseholdDataManager object.
    public void mapTablesToHouseholdObjects( String inputHouseholdFileName, String inputPersonFileName, ModelStructure modelStructure, TazDataIf tazDataManager ) {
        setupHouseholdDataManager( modelStructure, tazDataManager, inputHouseholdFileName, inputPersonFileName );
    }
    
    
    /**
     * Associate data in hh and person TableDataSets read from synthetic population files with
     * Household objects and Person objects with Households.
     *
     */
     public void mapTablesToHouseholdObjects() {

         logger.info( "mapping popsyn household and person data records to objects." );
         
         //read in worker occupation codes mapping
         try {
        	 readWorkerOccupationFile();
         } catch (Exception e) {
             logger.fatal( String.format("exception reading worker occupation code mapping"));
             throw new RuntimeException(e);
         }        

        int id = -1;
        int invalidPersonTypeCount = 0;

        // get the maximum HH id value to use to dimension the hhIndex correspondence array.
        // the hhIndex array will store the hhArray index number for the given hh index.
        int maxHhId = 0;
        for ( int r=1; r <= hhTable.getRowCount(); r++ ) {
            id = (int)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_ID_FIELD_NAME ) );
            if ( id > maxHhId )
                maxHhId = id;
        }
        hhIndexArray = new int[maxHhId+1];

        
        // get an index array for households sorted in random order
        int[] randomSortedIndices = getRandomOrderHhIndexArray( hhTable.getRowCount() );


        fullHhArray = null;

        int numHouseholdsInSample = (int)( hhTable.getRowCount() * sampleRate );
        HouseholdIf[] hhArray = new HouseholdIf[numHouseholdsInSample];

        int[] hhTazSortArray = new int[numHouseholdsInSample];
        for ( int i=0; i < numHouseholdsInSample; i++ ) {
            int r = randomSortedIndices[i] + 1;
            int htaz = (int)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_HOME_TAZ_FIELD_NAME ) );
            hhTazSortArray[i] = htaz;
        }
        
        // get an index array for households sorted in order of home taz
        int[] newOrder = new int[numHouseholdsInSample];
        int[] sortedIndices = getHomeTazOrderHhIndexArray( hhTazSortArray );
        for ( int i=0; i < sortedIndices.length; i++ ) {
            int k = sortedIndices[i];
            newOrder[k] = i;
        }

        
        
        
        // for each household in the sample
        for ( int i=0; i < numHouseholdsInSample; i++ ) {

            int r = randomSortedIndices[i] + 1;

            try {

                // create a Household object
                HouseholdIf hh = new Household( modelStructure );

                // get required values from table record and store in Household object
                id = (int)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_ID_FIELD_NAME ) );
                hh.setHhId ( id, inputRandomSeed );

                // set the household in the hhIndexArray in random order
                //int index = sortedIndices[r-1];
                int newIndex = newOrder[i];
                hhIndexArray[hh.getHhId()] = newIndex;

                short htaz = (short)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_HOME_TAZ_FIELD_NAME ) );
                hh.setHhTaz ( htaz );

                double rn = hh.getHhRandom().nextDouble();
                short origWalkSubzone = getInitialOriginWalkSegment ( htaz, rn );
                hh.setHhWalkSubzone(origWalkSubzone);

                // autos could be modeled or from PUMA
                short numAutos = (short)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_AUTOS_FIELD_NAME ) );
                hh.setHhAutos ( numAutos );

                // set the hhSize variable and create Person objects for each person
                short numPersons = (short)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_SIZE_FIELD_NAME ) );
                hh.setHhSize ( numPersons );

                short numWorkers = (short)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_WORKERS_FIELD_NAME ) );
                hh.setHhWorkers ( numWorkers );

                int incomeInDollars = (int)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_INCOME_DOLLARS_FIELD_NAME ));
                hh.setHhIncomeInDollars ( incomeInDollars );

                short type = (short)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_TYPE_FIELD_NAME ));
                hh.setHhType( type );
                
                int bldgsz = (int) hhTable.getValueAt(r, hhTable.getColumnPosition(HH_BLDGSZ_FIELD_NAME));
                hh.setHhBldgsz(bldgsz);

                hh.initializeWindows();
                hhArray[newIndex] = hh;

            }
            catch (Exception e) {

                logger.fatal( String.format( "exception caught mapping household data record to a Household object, r=%d, id=%d.", r, id) );
                throw new RuntimeException(e);

            }

        }



        int[] personHhStart = new int[maxHhId+1];
        int[] personHhEnd = new int[maxHhId+1];

        // get hhid for person record 1
        int hhid = (int)personTable.getValueAt( 1, personTable.getColumnPosition( PERSON_HH_ID_FIELD_NAME ) );
        personHhStart[hhid] = 1; 
        int oldHhid = hhid;

        for ( int r=1; r <= personTable.getRowCount(); r++ ) {

            // get the Household object for this person data to be stored in
            hhid = (int)personTable.getValueAt( r, personTable.getColumnPosition( PERSON_HH_ID_FIELD_NAME ) );
                
            if ( hhid > oldHhid ) {
                personHhEnd[oldHhid] = r-1; 
                oldHhid = hhid;
                personHhStart[hhid] = r; 
            }

        }
        personHhEnd[hhid] = personTable.getRowCount(); 

        
        for ( int i=0; i < numHouseholdsInSample; i++ ) {
            
            int r = randomSortedIndices[i] + 1;

            hhid = (int)hhTable.getValueAt( r, hhTable.getColumnPosition( HH_ID_FIELD_NAME ) );

            int index = hhIndexArray[hhid];
            HouseholdIf hh = hhArray[index];
            
            int persNum = 1;
            for ( int p=personHhStart[hhid]; p <= personHhEnd[hhid]; p++ ) {
                
                // get the Person object for this person data to be stored in
                int persId = (int)personTable.getValueAt( p, personTable.getColumnPosition( PERSON_PERSON_ID_FIELD_NAME ) );
                PersonIf person = hh.getPerson ( persNum++ );
                person.setPersId( persId );

                // get required values from table record and store in Person object
                int age = (int)personTable.getValueAt( p, personTable.getColumnPosition( PERSON_AGE_FIELD_NAME ) );
                person.setPersAge ( age );

                int gender = (int)personTable.getValueAt( p, personTable.getColumnPosition( PERSON_GENDER_FIELD_NAME ) );
                person.setPersGender ( gender );

                // Employment status (1-employed FT, 2-employed PT, 3-not employed, 4-under age 16)
                int empCat = (int)personTable.getValueAt( p, personTable.getColumnPosition( PERSON_EMPLOYMENT_CATEGORY_FIELD_NAME ) );
                person.setPersEmploymentCategory ( empCat );

                // Student status (1 - student in grade or high school; 2 - student in college or higher; 3 - not a student)
                int studentCat = (int)personTable.getValueAt( p, personTable.getColumnPosition(PERSON_STUDENT_CATEGORY_FIELD_NAME));
                person.setPersStudentCategory(studentCat);

                // Person type (1-FT worker age 16+, 2-PT worker nonstudent age 16+, 3-university student, 4-nonworker nonstudent age 16-64,
                // 5-nonworker nonstudent age 65+, 6-"age 16-19 student, not FT wrkr or univ stud", 7-age 6-15 schpred, 8  under age 6 presch
                int personType = (int)personTable.getValueAt( p, personTable.getColumnPosition( PERSON_TYPE_CATEGORY_FIELD_NAME ) );
                person.setPersonTypeCategory ( personType );

                // Person education attainment - PUMS EDUC attribute
                int personEduc = (int)personTable.getValueAt( p, personTable.getColumnPosition( PERSON_EDUC_CATEGORY_FIELD_NAME ) );
                person.setPersonEducAttainment( personEduc );
                
                // Person census industry - PUMS INDCEN attribute
                int personIndCen = (int)personTable.getValueAt( p, personTable.getColumnPosition( PERSON_INDCEN_CATEGORY_FIELD_NAME ) );
                person.setPersonIndustryCensus( personIndCen );                
             
                // Person worker occupation
                person.setPersonWorkerOccupation(getPersonWorkOccupation(personIndCen));
                
                // if person is a university student but has school age student category value, reset student category value
                if ( personType == Person.PersonType.University_student.ordinal() && studentCat != Person.StudentStatus.STUDENT_COLLEGE_OR_HIGHER.ordinal() ) {
                    studentCat = Person.StudentStatus.STUDENT_COLLEGE_OR_HIGHER.ordinal();
                    person.setPersStudentCategory( studentCat );
                    invalidPersonTypeCount++;
                }
                // if person is a student of any kind but has full-time employment status, reset student category value to non-student
                else if ( studentCat != Person.StudentStatus.NON_STUDENT.ordinal() && empCat == Person.EmployStatus.FULL_TIME.ordinal() ) {
                    studentCat = Person.StudentStatus.NON_STUDENT.ordinal();
                    person.setPersStudentCategory( studentCat );
                    invalidPersonTypeCount++;
                }


                // check consistency of student category and person type
                if ( studentCat == Person.StudentStatus.NON_STUDENT.ordinal() ) {

                    if ( person.getPersonIsStudentNonDriving() == 1 || person.getPersonIsStudentDriving() == 1 ) {
                        studentCat = Person.StudentStatus.STUDENT_HIGH_SCHOOL_OR_LESS.ordinal();
                        person.setPersStudentCategory( studentCat );
                        invalidPersonTypeCount++;
                    }

                }


                // set usual work and school location choice segments
                if ( personType == Person.EmployStatus.FULL_TIME.ordinal() || personType == Person.EmployStatus.PART_TIME.ordinal() ) {
                    String purposeName = modelStructure.getWorkPurposeFromOccupation( person.getPersonWorkerOccupation() );
                    int purposeIndex = modelStructure.getDcModelPurposeIndex( purposeName );
                    person.setWorkLocationPurposeIndex( purposeIndex );
                }
                else if ( studentCat == Person.StudentStatus.STUDENT_COLLEGE_OR_HIGHER.ordinal() ) {
                    String purposeName = modelStructure.getUniversityPurpose();
                    int purposeIndex = modelStructure.getDcModelPurposeIndex( purposeName );
                    person.setUniversityLocationPurposeIndex( purposeIndex );
                    
                    // set work purpose if part-time worker
                    if ( empCat == 2 ) {
                        purposeName = modelStructure.getWorkPurposeFromOccupation( person.getPersonWorkerOccupation() );
                        purposeIndex = modelStructure.getDcModelPurposeIndex( purposeName );
                        person.setWorkLocationPurposeIndex( purposeIndex );
                    }
                }
                else if ( person.getPersonIsPreschoolChild() == 1 || studentCat == Person.StudentStatus.STUDENT_HIGH_SCHOOL_OR_LESS.ordinal() ) {
                    String purposeName = modelStructure.getSchoolPurpose( person.getAge() );
                    int purposeIndex = modelStructure.getDcModelPurposeIndex( purposeName );
                    person.setSchoolLocationPurposeIndex( purposeIndex );
                    
                    // set work purpose if part-time worker
                    if ( empCat == 2 ) {
                        purposeName = modelStructure.getWorkPurposeFromOccupation( person.getPersonWorkerOccupation() );
                        purposeIndex = modelStructure.getDcModelPurposeIndex( purposeName );
                        person.setWorkLocationPurposeIndex( purposeIndex );
                    }
                }
                
            }
            
        }            
            

        fullHhArray = hhArray;

        logger.warn ( invalidPersonTypeCount + " person records had their type changed.");

        //set person value-of-time
        logger.info("Setting distributed values of time. "); 
        setDistributedValuesOfTime(); 
        
        logger.info("Setting distributed person walk preferences. "); 
        try {
        	setDistributedWalkPrefernces();
        } catch (Exception e) {
            logger.fatal( String.format("exception reading person walk preferences file"));
            throw new RuntimeException(e);
        }
        
        //set person user class
        logger.info("Setting person user class. "); 
        setUserClass(); 
        
    }




    /**
     * if called, must be called after readData so that the size of the full population is known.
     * @param hhFileName
     * @param persFileName
     * @param numHhs
     */
    public void createSamplePopulationFiles ( String hhFileName, String persFileName, String newHhFileName, String newPersFileName, int numHhs ) {

    	int maximumHhId = 0;
    	for( int i=0; i< fullHhArray.length; i++ ){
    		int id = fullHhArray[i].getHhId();
    		if( id > maximumHhId )
    		    maximumHhId = id;
    	}

        int[] testHhs = new int[maximumHhId+1];

        int[] sortedIndices = getRandomOrderHhIndexArray( fullHhArray.length );

        for ( int i=0; i < numHhs; i++) {
            int k = sortedIndices[i];
            int hhId = fullHhArray[k].getHhId();
            testHhs[hhId] = 1;
        }


        String hString = "";
        int hCount = 0;
        try {

            logger.info( String.format( "writing sample household file for %d households", numHhs ) );

            PrintWriter out = new PrintWriter ( new BufferedWriter( new FileWriter(newHhFileName) ) ) ;
            BufferedReader in = new BufferedReader( new FileReader(hhFileName) );

            // read headers and write to output files
            hString = in.readLine();
            out.write( hString + "\n" );
            hCount++;
            int count = 0;

            while ( ( hString = in.readLine() ) != null ) {
                hCount++;
                int endOfField = hString.indexOf(',');
                int hhId = Integer.parseInt( hString.substring(0, endOfField) );

                // if it's a sample hh, write the hh and the person records
                if ( testHhs[hhId] == 1 ) {
                    out.write( hString + "\n" );
                    count++;
                    if ( count == numHhs )
                        break;
                }
            }

            out.close();

        }
        catch (IOException e) {
            logger.fatal ("IO Exception caught creating sample synpop household file.");
            logger.fatal (String.format("reading hh file = %s, writing sample hh file = %s.", hhFileName, newHhFileName) );
            logger.fatal (String.format("hString = %s, hCount = %d.", hString, hCount) );
        }


        String pString = "";
        int pCount = 0;
        try {

            logger.info( String.format( "writing sample person file for selected households" ) );

            PrintWriter out = new PrintWriter ( new BufferedWriter ( new FileWriter (newPersFileName) ) ) ;
            BufferedReader in = new BufferedReader( new FileReader(persFileName) );

            // read headers and write to output files
            pString = in.readLine();
            out.write( pString + "\n" );
            pCount++;
            int count = 0;
            int oldId = 0;
            while ( ( pString = in.readLine() ) != null ) {
                pCount++;
                int endOfField = pString.indexOf(',');
                int hhId = Integer.parseInt( pString.substring(0, endOfField) );

                // if it's a sample hh, write the hh and the person records
                if ( testHhs[hhId] == 1 ) {
                    out.write( pString + "\n" );
                    if ( hhId > oldId )
                        count++;
                }
                else {
                    if ( count == numHhs )
                        break;
                }

                oldId = hhId;

            }

            out.close();

        }
        catch (IOException e) {
            logger.fatal ("IO Exception caught creating sample synpop person file.");
            logger.fatal (String.format("reading person file = %s, writing sample person file = %s.", persFileName, newPersFileName) );
            logger.fatal (String.format("pString = %s, pCount = %d.", pString, pCount) );
        }


    }


    public static void main(String args[]) throws Exception {

        String serverAddress = HH_DATA_SERVER_ADDRESS;
        int serverPort = HH_DATA_SERVER_PORT;

        
        // optional arguments
        for (int i=0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-hostname")) {
                serverAddress = args[i+1];
            }

            if (args[i].equalsIgnoreCase("-port")) {
                serverPort = Integer.parseInt( args[i+1] );
            }
        }
            
        Remote.config( serverAddress, serverPort, null, 0 );

        CmapHouseholdDataManager hhDataManager = new CmapHouseholdDataManager();

        ItemServer.bind( hhDataManager, HH_DATA_SERVER_NAME );

        System.out.println( String.format("CmapHouseholdDataManager server class started on: %s:%d", serverAddress, serverPort ) );
        
    }

}
