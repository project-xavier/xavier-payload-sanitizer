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
        List<String> vmsIssues = splitConditions(jsonIssuesConditions,"###vms");
        List<String> hostsIssues = splitConditions(jsonIssuesConditions,"###hosts");
        List<String> clustersIssues = splitConditions(jsonIssuesConditions,"###clusters");

        String outputJsonString = sanitize(jsonFileToBeSanitized, vmsIssues, hostsIssues,clustersIssues);

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



    private static List<String> splitConditions(String inputConditionsString, String sectionHeader)
    {
        if (inputConditionsString == null)
        {
            throw new RuntimeException("No issues conditions found");
        }
        List<String> returnIssues = new ArrayList<>();
        String[] issues = inputConditionsString.split("\\R");
        boolean currentSectionWanted = false;
        for (String issue:issues)
        {
            if(issue.equals(sectionHeader))
            {
                currentSectionWanted = true;
                continue;
            }else if (issue.startsWith("###"))
            {
                currentSectionWanted = false;
                continue;
            }
            if(currentSectionWanted)
            {
                returnIssues.add(issue);
            }
        }

        return returnIssues;

    }

    private static String sanitize(String fileToBeSanitized, List<String> vmsIssuesConditions, List<String> hostsIssuesConditions, List<String> clustersIssuesConditions)
    {
        String vmsInvalidElementIds = findInvalidElements(fileToBeSanitized, vmsIssuesConditions);
        String hostsInvalidElementIds = findInvalidElements(fileToBeSanitized, hostsIssuesConditions);
        String clustersInvalidElementIds = findInvalidElements(fileToBeSanitized, clustersIssuesConditions);

        String resultString = retireVms(fileToBeSanitized,"$.ManageIQ::Providers::Vmware::InfraManager[*].vms[?(@.id in [" + vmsInvalidElementIds+ "])]");
        resultString = retireVms(resultString,"$.ManageIQ::Providers::Vmware::InfraManager[*].vms[?(@.host.ems_ref in [" + hostsInvalidElementIds+ "])]");



        return resultString;
    }



    private static String findInvalidElements(String fileToBeSanitized, List<String> issuesConditions)
    {
        Configuration conf = Configuration.builder().options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS).build();
        DocumentContext parsedFile = JsonPath.using(conf).parse(fileToBeSanitized);

        List<Object> errorIds = new LinkedList<>();

        issuesConditions.forEach(issuesCondition -> {
            errorIds.addAll(parsedFile.read(issuesCondition));
        });

        List<String> errorIdStringsList = new ArrayList<>();

        errorIds.forEach(id -> errorIdStringsList.add(String.valueOf(id)));

        return String.join(",",errorIdStringsList);
    }

    private static String retireVms(String fileToBeSanitized, String vmSelectionQuery)
    {
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST, Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS).build();
        DocumentContext parsedFile = JsonPath.using(conf).parse(fileToBeSanitized);

        List<String> errorPaths = new ArrayList<String>();

        errorPaths.addAll(parsedFile.read(vmSelectionQuery));


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
