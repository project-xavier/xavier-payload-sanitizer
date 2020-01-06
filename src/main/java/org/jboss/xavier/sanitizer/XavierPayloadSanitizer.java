package org.jboss.xavier.sanitizer;

import com.jayway.jsonpath.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class XavierPayloadSanitizer
{
    private static String inputFileName;
    private static String outputFileName;
    private static String issuesConditionsJSONFileName;

    public static void main(final String[] args)
    {
        List<String> argsList = Arrays.asList(args);
        argsList.forEach(arg -> {
            if( arg.equals("--input"))
            {
                inputFileName = argsList.get(argsList.indexOf(arg) + 1);
            }
            if( arg.equals("--output"))
            {
                outputFileName = argsList.get(argsList.indexOf(arg) + 1);
            }

            if( arg.equals("--issues"))
            {
                issuesConditionsJSONFileName = argsList.get(argsList.indexOf(arg) + 1);
            }
        });

        String jsonFileToBeSanitized = readInputFile(inputFileName, "Input file to sanitize");

        String jsonIssuesConditions = readInputFile(issuesConditionsJSONFileName, "Issues Conditions file ");
        String[] issues = jsonIssuesConditions.split("\\R");

        String outputJsonString = sanitize(jsonFileToBeSanitized, issues);

        writeStringToFile(outputFileName, outputJsonString);

    }

    private static String readInputFile(String inputFile, String description)
    {
        if (inputFile == null)
        {
            throw new RuntimeException(description + "path not specified");
        }
        try
        {
            return new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);
        }
        catch(IOException ioe)
        {
            throw new RuntimeException(description + "could not be read");
        }
    }

    private static String sanitize(String fileToBeSanitized, String[] issuesConditions)
    {
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST,Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS).build();
        DocumentContext parsedFile = JsonPath.using(conf).parse(fileToBeSanitized);

        List<String> errorPaths = new ArrayList<String>();

        Arrays.stream(issuesConditions).forEach(issuesCondition -> {
            errorPaths.addAll(parsedFile.read(issuesCondition));
        });

        errorPaths.stream().forEach( errorPath -> parsedFile.put(errorPath,"retired",new Integer(1)));

        return parsedFile.jsonString();
    }

    private static void writeStringToFile(String path, String fileText)
    {
        if (path == null || path.isEmpty())
        {
            String[] splitPath = inputFileName.split("\\.");
            path = splitPath[0].concat("_sanitized.json");
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(path), "utf-8"))) {
            writer.write(fileText);
        }
        catch(FileNotFoundException fnfe)
        {
            throw new RuntimeException("Output file could not be found");
        }
        catch(IOException ioe)
        {
            throw new RuntimeException("Output file could not be written to");
        }

    }

}
