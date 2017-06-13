// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespafeeder;

import com.yahoo.vespa.config.content.LoadTypeConfig;
import com.yahoo.feedapi.DummySessionFactory;
import com.yahoo.feedapi.MessageBusSessionFactory;
import com.yahoo.feedapi.MessagePropertyProcessor;
import com.yahoo.feedapi.SessionFactory;
import com.yahoo.vespaclient.config.FeederConfig;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.lang.System.out;

/**
 * Argument parsing class for the vespa feeder.
 */
public class Arguments {
    public FeederConfig getFeederConfig() {
        return new FeederConfig(feederConfigBuilder);
    }

    public List<String> getFiles() {
        return files;
    }

    public String getMode() {
        return mode;
    }

    public boolean isVerbose() {
        return verbose;
    }

    private FeederConfig.Builder feederConfigBuilder = new FeederConfig.Builder();
    private List<String> files = new ArrayList<String>();
    private String dumpDocumentsFile = null;
    private String mode = "standard";
    private boolean validateOnly = false;
    private boolean verbose = false;
    SessionFactory sessionFactory = null;
    MessagePropertyProcessor propertyProcessor = null;
    private String priority = null;

    public MessagePropertyProcessor getPropertyProcessor() {
        return propertyProcessor;
    }

    public void help() {
        out.println("This is a tool for feeding xml (deprecated) or json data to a Vespa application.\n" +
                "\n" +
                "The options are:\n" +
                "  --abortondataerror arg (true) Whether or not to abort if the xml input has \n" +
                "                                errors (true|false).\n" +
                "  --abortonsenderror arg (true) Whether or not to abort if an error occured while\n" +
                "                                sending operations to Vespa (true|false).\n" +
                "  --file arg                    The name of the input files to read. These can \n" +
                "                                also be passed as arguments without the option \n" +
                "                                prefix. If none is given, this tool parses \n" +
                "                                identifiers from standard in.\n" +
                "  -h [ --help ]                 Shows this help page.\n" +
                "  --maxpending arg              The maximum number of operations that are allowed\n" +
                "                                to be pending at any given time. NOTE: This disables dynamic throttling. Use with care.\n" +
                "  --maxpendingsize arg          The maximum size (in bytes) of operations that \n" +
                "                                are allowed to be pending at any given time. \n" +
                "  --maxfeedrate arg             Limits the feed rate to the given number (operations/second). You may still want to increase\n" +
                "                                the max pending size if your feed rate doesn't reach the desired number.\n" +
                "  --mode arg (=standard)        The mode to run vespa-feeder in (standard | benchmark).\n" +
                "  --noretry                     Turns off retries of recoverable failures.\n" +
                "  --retrydelay arg (=1)         The time (in seconds) to wait between retries of \n" +
                "                                a failed operation.\n" +
                "  --route arg (=default)        The route to send the data to.\n" +
                "  --timeout arg (=180)          The time (in seconds) allowed for sending \n" +
                "                                operations.\n" +
                "  --trace arg (=0)              The trace level of network traffic.\n" +
                "  --validate                    Run validation tool on input files instead of \n" +
                "                                feeding them.\n" +
                "  --dumpDocuments <filename>    Specify a file where documents in the put are serialized.\n" +
                "  --priority arg                Specify priority of sent messages (see documentation for priority values)\n" +
                "  --create-if-non-existent      Enable setting of create-if-non-existent to true on all document updates in the given xml feed.\n" +
                "  -v [ --verbose ]              Enable verbose output of progress.\n");
    }

    public class HelpShownException extends Exception {

    }

    public Arguments(String[] argList, SessionFactory factory) throws HelpShownException, FileNotFoundException {
        parse(argList);

        if (factory != null) {
            sessionFactory = factory;
        } else if (validateOnly) {
            if (dumpDocumentsFile != null) {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dumpDocumentsFile));
                sessionFactory = new DummySessionFactory(null, out);
            } else {
                sessionFactory = new DummySessionFactory(null, null);
            }
        } else {
            sessionFactory = new MessageBusSessionFactory(propertyProcessor);
        }
    }

    void parse(String[] argList) throws HelpShownException {
        List<String> args = new LinkedList<String>();
        args.addAll(Arrays.asList(argList));

        while (!args.isEmpty()) {
            String arg = args.remove(0);

            if (arg.equals("-h") || arg.equals("--help")) {
                help();
                throw new HelpShownException();
            } else if ("--abortondataerror".equals(arg)) {
                feederConfigBuilder.abortondocumenterror(getBoolean(getParam(args, arg)));
            } else if ("--abortonsenderror".equals(arg)) {
                feederConfigBuilder.abortonsenderror(getBoolean(getParam(args, arg)));
            } else if ("--file".equals(arg)) {
                files.add(getParam(args, arg));
            } else if ("--maxpending".equals(arg)) {
                feederConfigBuilder.maxpendingdocs(Integer.parseInt(getParam(args, arg)));
            } else if ("--maxpendingsize".equals(arg)) {
                feederConfigBuilder.maxpendingbytes(Integer.parseInt(getParam(args, arg)));
            } else if ("--mode".equals(arg)) {
                mode = getParam(args, arg);
            } else if ("--noretry".equals(arg)) {
                feederConfigBuilder.retryenabled(false);
            } else if ("--retrydelay".equals(arg)) {
                feederConfigBuilder.retrydelay(Integer.parseInt(getParam(args, arg)));
            } else if ("--route".equals(arg)) {
                feederConfigBuilder.route(getParam(args, arg));
            } else if ("--timeout".equals(arg)) {
                feederConfigBuilder.timeout(Double.parseDouble(getParam(args, arg)));
            } else if ("--trace".equals(arg)) {
                feederConfigBuilder.tracelevel(Integer.parseInt(getParam(args, arg)));
            } else if ("--validate".equals(arg)) {
                validateOnly = true;
            } else if ("--dumpDocuments".equals(arg)) {
                dumpDocumentsFile = getParam(args, arg);
            } else if ("--maxfeedrate".equals(arg)) {
                feederConfigBuilder.maxfeedrate(Double.parseDouble(getParam(args, arg)));
            } else if ("--create-if-non-existent".equals(arg)) {
                feederConfigBuilder.createifnonexistent(true);
            } else if ("-v".equals(arg) || "--verbose".equals(arg)) {
                verbose = true;
            } else if ("--priority".equals(arg)) {
                priority = getParam(args, arg);
            } else {
                files.add(arg);
            }
        }

        propertyProcessor = new MessagePropertyProcessor(getFeederConfig(), new LoadTypeConfig(new LoadTypeConfig.Builder()));
    }

    private String getParam(List<String> args, String arg) throws IllegalArgumentException {
        try {
            return args.remove(0);
        } catch (Exception e) {
            System.err.println("--" + arg + " requires an argument");
            throw new IllegalArgumentException(arg);
        }
    }

    private Boolean getBoolean(String arg) {
        if (arg.equalsIgnoreCase("yes")) {
            return true;
        } else if (arg.equalsIgnoreCase("no")) {
            return false;
        } else {
            return Boolean.parseBoolean(arg);
        }
    }

    public String getPriority() {
        return priority;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }


}
