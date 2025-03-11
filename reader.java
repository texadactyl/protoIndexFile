import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class reader {
    private static String INDEX_FILE_PATH = "saucisse.index";
    private static String DATA_FILE_PATH = "saucisse.data";
    private static int ITEM_LENGTH = 32;
    private static int DETAIL_RCD_COUNT = 13;

    public static void main(String[] args) throws IOException {
    
         // Load index map.
        Map<Integer, Long> indexMap = loadindexMapAsBinary(INDEX_FILE_PATH);
        int numRecords = indexMap.size();
        
        System.out.printf("Loaded index file (%d): ", numRecords);
        System.out.println(indexMap);

        // Retrieve record 0 (Begin Frame).
        readDataByRecordNumber(DATA_FILE_PATH, indexMap, 0);
        
        // Retrieve DETAIL_RCD_COUNT records, selected randomly.
        SecureRandom sr = new SecureRandom();
        int randInt = 0;
        for (int ix = 0; ix < DETAIL_RCD_COUNT; ix++) {
            while (randInt == 0) {
                randInt = sr.nextInt(numRecords);
            }
            readDataByRecordNumber(DATA_FILE_PATH, indexMap, randInt);
            randInt = 0;
        }
        
        // Retrieve the last record (End Frame).
        readDataByRecordNumber(DATA_FILE_PATH, indexMap, numRecords - 1);
            
    }

    // Read 4 bytes from an opened FileInputStream in Little Endian order and return an int to caller.
    private static int readIntLE(FileInputStream fis) throws IOException {
        byte[] bytes = new byte[4];
        int bytesRead = fis.read(bytes);
        if (bytesRead < 4) {
            throw new IOException("readIntLE: Unexpected end of file");
        }       
        return (bytes[0] & 0xFF) | 
               ((bytes[1] & 0xFF) << 8) | 
               ((bytes[2] & 0xFF) << 16) | 
               ((bytes[3] & 0xFF) << 24);
    }

    // Read 8 bytes from an opened FileInputStream in Little Endian order and return a long to caller.
    public static long readLongLE(FileInputStream fis) throws IOException {
        byte[] bytes = new byte[8];
        int bytesRead = fis.read(bytes);
        if (bytesRead < 8) {
            throw new IOException("readLongLE: Unexpected end of file");
        }
        return (bytes[0] & 0xFFL) | 
               ((bytes[1] & 0xFFL) << 8) | 
               ((bytes[2] & 0xFFL) << 16) | 
               ((bytes[3] & 0xFFL) << 24) | 
               ((bytes[4] & 0xFFL) << 32) | 
               ((bytes[5] & 0xFFL) << 40) | 
               ((bytes[6] & 0xFFL) << 48) | 
               ((bytes[7] & 0xFFL) << 56);
    }

    // Load the index map from the specified index file path.
    private static Map<Integer, Long> loadindexMapAsBinary(String indexFilePath) throws IOException {

        Map<Integer, Long> indexMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(indexFilePath)) {
            // Ensure the stream uses little-endian byte order
            while (fis.available() >= 12) { // 4 bytes for key (int), 8 bytes for value (long)
                int key = readIntLE(fis);                 
                long value = readLongLE(fis);
                indexMap.put(key, value);
            }
        }
        
        return indexMap;
    }

    // Given the data file path, index map, and record number, read and report the data record contents.
    private static void readDataByRecordNumber(String dataPath, Map<Integer, Long> indexMap, int recordNumber) {
        Long offset = indexMap.get(recordNumber);
        if (offset == null) {
            System.out.printf("Record number %d not found in the index.\n", recordNumber);
            return;
        }

        try (RandomAccessFile dataFile = new RandomAccessFile(dataPath, "r")) {
            dataFile.seek(offset);

            // Read Record Type (RID)
            byte rid = dataFile.readByte();
            String filler = readFixedString(dataFile, 15);
            switch (rid) {
                case 'B': // Begin Frame
                    System.out.println("Begin Frame:");
                    System.out.printf("  Class: %s\n", readFixedString(dataFile, ITEM_LENGTH));
                    System.out.printf("  Method: %s\n", readFixedString(dataFile, ITEM_LENGTH));
                    System.out.printf("  Type: %s\n", readFixedString(dataFile, ITEM_LENGTH));
                    break;

                case 'I': // Integer Change
                    System.out.println("Integer Change:");
                    System.out.printf("  Old Value: %s\n", readFixedString(dataFile, ITEM_LENGTH));
                    System.out.printf("  New Value: %s\n", readFixedString(dataFile, ITEM_LENGTH));
                    break;

                case 'E': // End Frame
                    System.out.println("End Frame:");
                    System.out.printf("  Class: %s\n", readFixedString(dataFile, ITEM_LENGTH));
                    System.out.printf("  Method: %s\n", readFixedString(dataFile, ITEM_LENGTH));
                    System.out.printf("  Type: %s\n", readFixedString(dataFile, ITEM_LENGTH));
                    break;

                default:
                    System.out.printf("Unknown record type: %02x\n", rid);
                    break;
            }
        } catch (IOException e) {
            System.out.printf("Error reading data record %s at offset %d: ", recordNumber, offset, e.getMessage());
        }
    }

    // Read a given number of bytes.
    // Convert to String and trim.
    // Return result to caller.
    private static String readFixedString(RandomAccessFile file, int size) throws IOException {
        byte[] bytes = new byte[size];
        file.readFully(bytes);
        return new String(bytes).trim();
    }
}

