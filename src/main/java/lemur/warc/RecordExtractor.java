package lemur.warc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.jwat.common.HeaderLine;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;

public class RecordExtractor {

    private String headerName;
    private Iterable<Pattern> values;
    private WarcWriter writer;
    
    public RecordExtractor(WarcWriter writer, String headerName, Iterable<Pattern> values) {
        this.writer = writer;
        this.headerName = headerName;
        this.values = values;
    }

    /**
     * Checks whether a record matches the given values for the header.
     * 
     * @param record
     * @return
     */
    public boolean matches(WarcRecord record) {
        HeaderLine headerLine = record.header.getHeader(headerName);
        if (headerLine == null) {
            return false;
        }
        for (Pattern value : values) {
            if (value.matcher(headerLine.value).matches()) {
                return true;
            }
        }
        return false;
    }

    public int extractRecords(WarcReader reader) throws IOException {
        int nMatched = 0;
        Iterator<WarcRecord> inputIter = reader.iterator();
        while (inputIter.hasNext()) {
            WarcRecord record = inputIter.next();
            if (matches(record)) {
                writer.writeRawHeader(record.header.headerBytes, record.getPayload().getTotalLength());
                writer.streamPayload(record.getPayload().getInputStreamComplete());
                writer.closeRecord();
                
                nMatched++;
            }
        }
        return nMatched;
    }
    
    /**
     * Reads a text file with the regular expression used to match the values
     * of the header.
     * 
     * @param input
     * @return
     * @throws IOException
     */
    public static List<Pattern> loadValues(File input) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(input));
        String line = null;
        List<Pattern> values = new ArrayList<Pattern>();
        while ((line = reader.readLine()) != null){
            Pattern value = Pattern.compile(line);
            values.add(value);
        }
        reader.close();
        return values;
    }
    
}
