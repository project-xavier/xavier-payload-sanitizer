[![Build Status](https://travis-ci.org/project-xavier/xavier-payload-sanitizer.svg?branch=master)](https://travis-ci.org/project-xavier/xavier-payload-sanitizer)
[![codecov](https://codecov.io/gh/project-xavier/xavier-payload-sanitizer/branch/master/graph/badge.svg)](https://codecov.io/gh/project-xavier/xavier-payload-sanitizer)

Sanitizes input payload json files to xavier-analytics. 

Checks input files for error conditions specified by the lines in an input issues conditions file, an example of which is included in the resources folder and can be used as a default.

Sets failing VM elements to be 'retired' in the output file so the report generation process ignores the failing element. 
## Issues Conditions File Format

1. This file is divided into sections containing conditions for different sections of the payload file.
1. The sections are divided by header lines.
1. At the moment there are 2 functional header lines , `###vms` and `###hosts`
1. Following each header line, jsonpath query strings can be placed that identify an instance of that section of the payload file which meets a particular error condition. Each error condition should occupy a new line in the file.
1. An error condition line in the `###vms` section should identify its id, eg `$.ManageIQ::Providers::Vmware::InfraManager[*].vms[?(@.host == null )].id`
1. An error condition line in the `###hosts` section should identify its ems_ref, eg `$.ManageIQ::Providers::Vmware::InfraManager[*].hosts[?(@.ems_cluster == null )].ems_ref`

## Build

1. From the project root run (to include project dependencies in jar)
1. `mvn clean assembly:assembly -DdescriptorId=jar-with-dependencies`

## Usage

There are 3 possible input arguments which can be included in any order:

1. `--input` followed by a path to a failing input payload file which needs to be sanitized. Mandatory.
1. `--output` followed by the desired path to the sanitized output file. If this argument is omitted or has a value of null or empty, the output file will be produced in the same folder as the input file with a file name based on the input file name with _sanitized.json at the end.

    eg an input file name of input.json will result in an output file named input_sanitized.json
1. `--issues` followed by the path to the issues conditions file to use. If this argument is not supplied the issues conditions file shipped inside the jar will be used as a default. 

The tool is intended to be used from the command line, eg running from the same folder as the jar:

`java -jar xavier-payload-sanitizer-0.0.1-SNAPSHOT-jar-with-dependencies.jar --input /{path to input file}/cfme_inventory_0.json --issues /{path to issues conditions file}/issues_conditions.txt --output /{path to output file}/sanitizedJson.json`

