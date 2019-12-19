Sanitizes input payload json files to xavier-analytics. 

Checks input files for error conditions specified by the lines in an input issues conditions file, an example of which is included in the resources folder and can be used as a default.

Sets failing elements to be 'retired' in the output file so the report generation process ignores the failing element. 
## Build

1. From the project root run (to include project dependencies in jar)
1. `mvn clean assembly:assembly -DdescriptorId=jar-with-dependencies`

##Usage

There are 3 possible input arguments which can be included in any order:

1. `--input` followed by a path to a failing input payload file which needs to be sanitized. Mandatory.
1. `--output` followed by the desired path to the sanitized output file. If this argument is omitted or has a value of null or empty, the output file will be produced in the same folder as the input file with a file name based on the input file name with _sanitized.json at the end.

    eg an input file name of input.json will result in an output file named input_sanitized.json
1. `--issues` followed by the path to the issues conditions file to use.

The tool is intended to be used from the command line, eg running from the same folder as the jar:

`java -cp xavier-payload-sanitizer-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.jboss.xavier.sanitizer.XavierPayloadSanitizer --input /{path to input file}/cfme_inventory_0.json --issues /{path to issues conditions file}/issues_conditions.json --output /{path to output file}/sanitizedJson.json`