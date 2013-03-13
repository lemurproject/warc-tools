package lemur.warc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

/**
 * Command line utility for working with WARC files.
 * 
 * It provides the following commands:
 * 
 *  sample   Sample randomly selected records.
 *  count    Counts the number of records in the files.
 *  extract  Extracts records that match the values on a given header
 *
 *
 */
public class WarcTools {

    enum Command {
        SAMPLE, EXTRACT, COUNT
    }

    public static void writeRecord(WarcWriter writer, WarcRecord record) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) record.getPayload()
                .getTotalLength());
        /*
         * bos.write(record.getHttpHeader().getHeader()); bos.write(content);
         * bos.close(); byte[] processed = bos.toByteArray();
         * 
         * System.out.printf("header: %s payload: %s bytes: %s total: %s\n",
         * record.header.contentLength, record.getPayload().getTotalLength(),
         * content.length, record.header.headerBytes.length + content.length);
         * 
         * writer.writeRawHeader(record.header.headerBytes,
         * record.getPayload().getTotalLength());
         * writer.writePayload(processed); writer.closeRecord();
         */
    }

    /**
     * Extracts a random sample of the records from the input files and writes
     * them into the output file.
     * 
     * @param opts
     * @return
     * @throws IOException
     */
    public static int sample(Namespace opts) throws IOException {
        float ratio = opts.getFloat("ratio");

        Random rand = new Random();

        File outFile = (File) opts.get("output");
        FileOutputStream outStream = new FileOutputStream(outFile);
        WarcWriter writer = WarcWriterFactory.getWriter(outStream, true);

        List<File> inputFiles = opts.getList("input");

        int nRecords = 0;
        int nTotal = 0;

        for (File inputFile : inputFiles) {
            InputStream inStream = new GZIPInputStream(new FileInputStream(inputFile));
            WarcReader reader = WarcReaderFactory.getReader(inStream);

            Iterator<WarcRecord> recordIter = reader.iterator();

            while (recordIter.hasNext()) {
                WarcRecord record = recordIter.next();
                if (rand.nextFloat() <= ratio) {
                    writer.writeRawHeader(record.header.headerBytes, record.getPayload().getTotalLength());
                    writer.streamPayload(record.getPayload().getInputStreamComplete());
                    writer.closeRecord();

                    nRecords++;
                }
                nTotal++;
            }
            reader.close();
        }
        writer.close();

        System.err.printf("%d/%d records written\n", nRecords, nTotal);
        return 0;
    }

    /**
     * Counts the number of records in the file and prints it into the stdout.
     * 
     * @param opts
     * @return
     * @throws IOException
     */
    public static int count(Namespace opts) throws IOException {
        List<File> inputFiles = opts.getList("input");

        int nRecords = 0;

        for (File inputFile : inputFiles) {
            InputStream inStream = new GZIPInputStream(new FileInputStream(inputFile));
            WarcReader reader = WarcReaderFactory.getReader(inStream);

            Iterator<WarcRecord> recordIter = reader.iterator();

            while (recordIter.hasNext()) {
                recordIter.next();
                nRecords++;
            }
            reader.close();
        }
        System.out.println(nRecords);
        return 0;
    }

    /**
     * Extracts records whose values of a header match a list of expressions.
     * 
     * @param opts
     * @return
     * @throws IOException 
     */
    public static int extract(Namespace opts) throws IOException {
        String headerName = opts.getString("header");
        File valuesFile = (File) opts.get("values");
        List<Pattern> values = RecordExtractor.loadValues(valuesFile);
        
        List<File> inputFiles = opts.getList("input");
        File outFile = (File) opts.get("output");
        FileOutputStream outStream = new FileOutputStream(outFile);
        WarcWriter writer = WarcWriterFactory.getWriter(outStream, true);

        RecordExtractor extractor = new RecordExtractor(writer, headerName, values);

        int nMatched = 0;        
        for (File inputFile : inputFiles) {
            InputStream inStream = new GZIPInputStream(new FileInputStream(inputFile));
            WarcReader reader = WarcReaderFactory.getReader(inStream);
            nMatched = extractor.extractRecords(reader);
            reader.close();
        }
        writer.close();
        System.err.printf("%d records matched.\n", nMatched);
        return 0;
    }

    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("WarcTools");

        Subparsers subparsers = parser.addSubparsers();

        Subparser pSample = subparsers.addParser("sample")
                .setDefault("command", Command.SAMPLE)
                .help("Sample randomly selected records.");
        pSample.addArgument("ratio").type(Float.class)
                .help("Fraction of the input records to be included [0, 1]");
        pSample.addArgument("output").type(Arguments.fileType().verifyCanWriteParent())
                .help("Output file (.warc.gz)");
        pSample.addArgument("input").nargs("+")
                .type(Arguments.fileType().verifyCanRead()).help("Input file");

        Subparser pCount = subparsers.addParser("count")
                .setDefault("command", Command.COUNT)
                .help("Counts the number of records in the files.");
        pCount.addArgument("input").nargs("+").type(Arguments.fileType().verifyCanRead())
                .help("Input file");

        Subparser pExtract = subparsers.addParser("extract")
                .help("Extracts records that match the values on a given header")
                .setDefault("command", Command.EXTRACT);
        pExtract.addArgument("header").help("Name of the header");
        pExtract.addArgument("values").type(Arguments.fileType().verifyCanRead())
                .help("File with the list of regular expressions to match the values");
        pExtract.addArgument("output").type(Arguments.fileType().verifyCanWriteParent())
                .help("Output file");
        pExtract.addArgument("input").nargs("+")
                .type(Arguments.fileType().verifyCanRead()).help("Input file");

        Namespace opts = parser.parseArgsOrFail(args);
        Command cmd = (Command) opts.get("command");
        int rtn = 0;
        switch (cmd) {
        case COUNT:
            rtn = count(opts);
            break;
        case SAMPLE:
            rtn = sample(opts);
            break;
        case EXTRACT:
            rtn = extract(opts);
            break;
        }
        System.exit(rtn);
    }
}
