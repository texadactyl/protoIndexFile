import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class reader {
    private static final String INDEX_FILE_PATH = "index.bin";
    private static final String DATA_FILE_PATH = "data.bin";
    private static final int ITEM_LENGTH = 32;

    public static void main(String[] args) {
        try {
            // Load index map.
            Map<Integer, Long> indexMap = loadindexMapAsBinary(INDEX_FILE_PATH);
            System.out.println("Loaded index file: " + indexMap);

            // Retrieve records based on record numbers
            int[] recordNumbers = {0, 101, 2002, 347, 10001};
            for (int recordNumber : recordNumbers) {
                readDataByRecordNumber(DATA_FILE_PATH, indexMap, recordNumber);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load the index map from the specified index file path.
    private static Map<Integer, Long> loadindexMapAsBinary(String filePath) throws IOException {
        Map<Integer, Long> indexMap = new HashMap<>();

        try (DataInputStream dis = new DataInputStream(new FileInputStream(filePath))) {
            // Ensure the stream uses little-endian byte order
            while (dis.available() >= 12) { // 4 bytes for key (int), 8 bytes for value (long)
                int key = Integer.reverseBytes(dis.readInt());  // Convert to big-endian
                long value = Long.reverseBytes(dis.readLong()); // Convert to big-endian
                indexMap.put(key, value);
            }
        }
        
        return indexMap;
    }

    // Given the data file path, index map, and record number, read and report the data record contents.
    private static void readDataByRecordNumber(String dataPath, Map<Integer, Long> indexMap, int recordNumber) {
        Long offset = indexMap.get(recordNumber);
        if (offset == null) {
            System.out.println("Record number " + recordNumber + " not found in the index.");
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
                    System.out.println("  Class: " + readFixedString(dataFile, ITEM_LENGTH));
                    System.out.println("  Method: " + readFixedString(dataFile, ITEM_LENGTH));
                    System.out.println("  Type: " + readFixedString(dataFile, ITEM_LENGTH));
                    break;

                case 'I': // Integer Change
                    System.out.println("Integer Change:");
                    System.out.println("  Old Value: " + readFixedString(dataFile, ITEM_LENGTH));
                    System.out.println("  New Value: " + readFixedString(dataFile, ITEM_LENGTH));
                    break;

                case 'E': // End Frame
                    System.out.println("End Frame:");
                    System.out.println("  Class: " + readFixedString(dataFile, ITEM_LENGTH));
                    System.out.println("  Method: " + readFixedString(dataFile, ITEM_LENGTH));
                    System.out.println("  Type: " + readFixedString(dataFile, ITEM_LENGTH));
                    break;

                default:
                    System.out.println("Unknown record type: " + rid);
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error reading record at offset " + offset + ": " + e.getMessage());
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

