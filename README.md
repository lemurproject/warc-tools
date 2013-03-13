
==========
warc-tools
==========

Command line utility for working with WARC files.

It provides the following commands:
 
- Sample randomly selected records.
- Count the number of records in the files.
- Extract records that match the values on a given header.

# Usage

    java -jar warc-tools.jar [-h] {sample,count,extract} ..

Use -h to list the available commands and their options.

# Examples

* Count all the records on a list of .warc.gz files:

    WarcTools count file1.warc.gz file2.warc.gz ...
    >> 34524

* Create a sample with 10% of the records on a list of .warc.gz files: 

    WarcTools sample 0.1 output.warc.gz file1.warc.gz file2.warc.gz ...
    >> 152 / 1572 records written.

* Create a .warc.gz file with the records whose header 'WARC-Target-URI'
  matches a regular expression:

  1. Create a text file with the list of possible regular expressions that you
     want to match. For example, supose that you want to extract the records
     corresponding to URLs that contain the strings '.org' and '.edu'. Writhe
     the following lines into a file (for example patterns.txt):

        .*\.org.*
        .*\.edu.*

  2. Use WarcTools as follows:

     WarcTools extract WARC-Target-URI patterns.txt output.warc.gz file1.warc.gz file2.warc.gz ...

# License

warc-tools is distributed under a BSD License.



