package com.pb.models.ctrampIf;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.Serializable;

import com.pb.common.calculator.DataEntry;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixReader;


/**
 * @author Jim Hicks
 *
 * Class for managing matrix data in a remote process and accessed by UECs using RMI.
 */
public class MatrixDataServer implements MatrixDataServerIf, Serializable {

    protected static Logger logger = Logger.getLogger(MatrixDataServer.class);

    //version 2.3.2 drops the 32bit JVM stuff since not needed
    private static final String VERSION = "2.3.2, 04jan2013";
    
    // These are used if the server is started manually by running this class's main().  If so, these must be defined consistently with
    // any class that acts as a client to the server, i.e. the client must know the address and port as well.
    private static final String MATRIX_DATA_SERVER_ADDRESS = "127.0.0.1";
    private static final int MATRIX_DATA_SERVER_PORT = 1171;
    public static final String MATRIX_DATA_SERVER_NAME = MatrixDataServer.class.getCanonicalName();
    private static final String MATRIX_DATA_SERVER_LABEL = "matrix server";

    private static final boolean SAVE_MATRIX_DATA_IN_MAP = true;

    private HashMap<String, DataEntry> matrixEntryMap;
    private HashMap<String, Matrix> matrixMap;

    private boolean useMatrixMap;
    
    
    public MatrixDataServer() {

        // create the HashMap objects to keep track of matrix data read by the server
        matrixEntryMap = new HashMap<String, DataEntry>();
        matrixMap = new HashMap<String, Matrix>();
    }


    public String testRemote() {
        logger.info("testRemote() called by remote process.");
        return String.format("testRemote() method in %s called.", this.getClass().getCanonicalName() );
    }
    
    
    public String testRemote( String remoteObjectName )
    {
        logger.info("testRemote() called by remote process: " + remoteObjectName + "." );
        return String.format("testRemote() method in %s called by %s.", this.getClass().getCanonicalName(), remoteObjectName);
    }


    /**
     * Return the Matrix object identified by the DataEntry argument.  If it exists in the cache,
     * return that object.  If it does not yet exist, read the Matrix data, store in the cache, and return it.
     *
     * @param matrixEntry is an object with details about the name and location of a matrix read by a UEC object.
     * @return a Matrix object from the cached set.
     */
    public synchronized Matrix getMatrix( DataEntry matrixEntry ) {

        Matrix m;

        String name = matrixEntry.name;

        if ( matrixEntryMap.containsKey( name ) ) {
            m = matrixMap.get( name );
        }
        else {
            m = readMatrix( matrixEntry );
            
            if ( useMatrixMap ) {
                matrixMap.put ( name, m );
                matrixEntryMap.put( name, matrixEntry );
            }
        }

        return m;
    }


    public void clear() {
        matrixMap.clear();
        matrixEntryMap.clear();
    }

    
    public void setUseMatrixMap( boolean useMap ) {
        useMatrixMap = useMap;
    }
    
    
    /*
     * Read a matrix.
     *
     * @param matrixEntry a DataEntry describing the matrix to read
     * @return a Matrix
     */
    private Matrix readMatrix(DataEntry matrixEntry) {

        Matrix matrix;
        String fileName = matrixEntry.fileName;

        if (matrixEntry.format.equalsIgnoreCase("emme2")) {
            MatrixReader mr = MatrixReader.createReader(MatrixType.EMME2, new File(fileName));
            matrix = mr.readMatrix(matrixEntry.matrixName);
            logger.info(String.format("read %s %s", fileName, matrixEntry.matrixName));
            
        } else if (matrixEntry.format.equalsIgnoreCase("binary")) {
            MatrixReader mr = MatrixReader.createReader(MatrixType.BINARY, new File(fileName));
            matrix = mr.readMatrix();
            logger.info(String.format("read %s %s", fileName, matrixEntry.matrixName));
            
        } else if (matrixEntry.format.equalsIgnoreCase("zip") || matrixEntry.format.equalsIgnoreCase("zmx")) {
            MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP, new File(fileName));
            matrix = mr.readMatrix();
            logger.info(String.format("read %s %s", fileName, matrixEntry.matrixName));
            
        } else if (matrixEntry.format.equalsIgnoreCase("tpplus")) {
            MatrixReader mr = MatrixReader.createReader(MatrixType.TPPLUS, new File(fileName));
            matrix = mr.readMatrix(matrixEntry.matrixName);
            logger.info(String.format("read %s %s", fileName, matrixEntry.matrixName));
            
        } else if (matrixEntry.format.equalsIgnoreCase("transcad")) {
        	MatrixReader mr = MatrixReader.createReader(MatrixType.TRANSCAD, new File(fileName));
            matrix = mr.readMatrix(matrixEntry.matrixName);
            logger.info(String.format("read %s %s", fileName, matrixEntry.matrixName));
            
        } else {
            throw new RuntimeException("unsupported matrix type: " + matrixEntry.format);
        }


        //Use token name from control file for matrix name (not name from underlying matrix)
        matrix.setName(matrixEntry.name);

        return matrix;
    }



    public void start32BitMatrixIoServer( MatrixType mType ) {
        //do nothing
    }


    public void stop32BitMatrixIoServer() {
    	//do nothing 
    }


    public static void main(String args[]) throws Exception {

        String serverAddress = MATRIX_DATA_SERVER_ADDRESS;
        int serverPort = MATRIX_DATA_SERVER_PORT;
        String className = MATRIX_DATA_SERVER_NAME;
        String serverLabel = MATRIX_DATA_SERVER_LABEL;
        boolean serverUseMap = SAVE_MATRIX_DATA_IN_MAP;
        
        for (int i=0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-hostname"))
                serverAddress = args[i+1];
            else if (args[i].equalsIgnoreCase("-port"))
                serverPort = Integer.parseInt( args[i+1] );
            else if (args[i].equalsIgnoreCase("-label"))
                serverLabel = args[i+1];
            else if (args[i].equalsIgnoreCase("-useMap"))
                serverUseMap = Boolean.getBoolean( args[i+1] );
        }
        
        MatrixDataServer matrixServer = new MatrixDataServer();
        matrixServer.setUseMatrixMap( serverUseMap );
        
        
        // bind this concrete object with the cajo library objects for managing RMI
        boolean serverWaiting = true;
        int count = 0;
        while ( serverWaiting ) {
            try {
                Remote.config( serverAddress, serverPort, null, 0 );
                ItemServer.bind( matrixServer, className );
                serverWaiting = false;
            }
            catch ( Exception e ) {
                TimeUnit.SECONDS.sleep(1);
                e.printStackTrace();
                System.out.println( "try number" + count++ );
            }
            if ( count == 3 ) {
                throw new RuntimeException();
            }
        }

        
        // log that the server started
        System.out.println( String.format("%s version %s started on: %s:%d", serverLabel, VERSION, serverAddress, serverPort) );


    }
    
    public void writeMatrixFile(String fileName, Matrix[] m) {
    	
        System.out.println( "write matrix not currently implemented" );
    } 

}