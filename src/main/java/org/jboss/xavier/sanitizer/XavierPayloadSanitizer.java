package org.jboss.xavier.sanitizer;

import com.jayway.jsonpath.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class XavierPayloadSanitizer
{
    private static String inputFileName;
    private static String outputFileName;
    private static String issuesConditionsForJSONFileName;
    private static List<String> failingVMPaths;

    public static void main(final String[] args)
    {
        failingVMPaths = new ArrayList<>();
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
                issuesConditionsForJSONFileName = argsList.get(argsList.indexOf(arg) + 1);
            }
        });

        String jsonIssuesConditions;

        if(issuesConditionsForJSONFileName == null || issuesConditionsForJSONFileName.equals(""))
        {
            jsonIssuesConditions = getShippedIssuesConditions();

        }
        else
        {
            jsonIssuesConditions = readInputFile(issuesConditionsForJSONFileName, "Issues Conditions file ");
        }

        String jsonFileToBeSanitized = readInputFile(inputFileName, "Input file to sanitize ");

        List<String> vmsIssues = splitConditions(jsonIssuesConditions,"###vms");
        List<String> hostsIssues = splitConditions(jsonIssuesConditions,"###hosts");
        List<String> clustersIssues = splitConditions(jsonIssuesConditions,"###clusters");
        try
        {
            String outputJsonString = sanitize(jsonFileToBeSanitized, vmsIssues, hostsIssues, clustersIssues);

            writeStringToFile(outputFileName, outputJsonString);

            printRetiredVMList(outputJsonString);
        }
        catch(InvalidJsonException ije)
        {
            throw new RuntimeException(inputFileName + " is not a json-formatted file");
        }

    }

    private static String readInputFile(String inputFile, String description)
    {
        if (inputFile == null)
        {
            throw new RuntimeException(description + "path not specified");
        }
        Path inputPath = Paths.get(inputFile);
        if (!Files.exists(inputPath))
        {
            throw new RuntimeException(description + "path does not exist. Path=" + inputPath.toString());
        }
        try
        {
            return new String(Files.readAllBytes(inputPath), StandardCharsets.UTF_8);
        }
        catch(IOException ioe)
        {
            throw new RuntimeException(description + "could not be read");
        }
    }

    private static String getShippedIssuesConditions()
    {
        InputStream in = XavierPayloadSanitizer.class.getClassLoader().getResourceAsStream("issues_conditions.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        return reader.lines().collect(Collectors.joining("\n"));
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

        String resultString = markVmsAsRetired(fileToBeSanitized,"$.ManageIQ::Providers::Vmware::InfraManager[*].vms[?(@.id in [" + vmsInvalidElementIds+ "])]");
        resultString = markVmsAsRetired(resultString,"$.ManageIQ::Providers::Vmware::InfraManager[*].vms[?(@.host.ems_ref in [" + hostsInvalidElementIds+ "])]");


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

    private static String markVmsAsRetired(String fileToBeSanitized, String vmSelectionQuery)
    {
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST, Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS).build();
        DocumentContext parsedFile = JsonPath.using(conf).parse(fileToBeSanitized);

        List<String> errorPaths = new ArrayList<String>();

        errorPaths.addAll(parsedFile.read(vmSelectionQuery));


        errorPaths.stream().forEach( errorPath -> {
            parsedFile.put(errorPath,"retired",new Integer(1));
        });

        failingVMPaths.addAll(errorPaths);

        return parsedFile.jsonString();
    }

    private static void printRetiredVMList(String fileToBeSanitized)
    {
        Configuration conf = Configuration.builder().options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS).build();
        DocumentContext parsedFile = JsonPath.using(conf).parse(fileToBeSanitized);

        List<Integer> retiredIds = new ArrayList<Integer>();

        failingVMPaths.stream().forEach( errorPath -> {
            retiredIds.addAll(parsedFile.read(errorPath + ".id"));
        });

        Collections.sort(retiredIds);
        retiredIds.forEach(id -> System.out.println("VM in file " + outputFileName + " marked as retired: id=" + id.toString()));
    }

    private static void writeStringToFile(String path, String fileText)
    {
        if (path == null || path.isEmpty())
        {
            String[] splitPath = inputFileName.split("\\.");
            DateFormat df = new SimpleDateFormat("YYYYMMDDHHmm");
            String nowAsString = df.format(Calendar.getInstance().getTime());
            path = splitPath[0].concat("_sanitized").concat(nowAsString).concat(".json");
            outputFileName = path;
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8))) {
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
