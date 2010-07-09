/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 * Author : Gregory Boissinot, Guillaume Tanier                                 *
 *                                                                              *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 *******************************************************************************/


package com.thalesgroup.hudson.plugins.tusarnotifier;

import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.metrics.hudson.api.type.*;
import com.thalesgroup.dtkit.util.converter.ConvertException;
import com.thalesgroup.dtkit.util.validator.ValidatorError;
import com.thalesgroup.dtkit.util.validator.ValidatorException;
import com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException;
import com.thalesgroup.hudson.plugins.tusarnotifier.util.TusarNotifierLogger;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import junit.framework.Assert;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class TusarTransformer implements FilePath.FileCallable<String>, Serializable {

    private TaskListener listener;

    private TestType[] typesTests;
    private CoverageType[] typesCoverage;
    private ViolationsType[] typesViolations;
    private MeasureType[] typesMeasures;


    public TusarTransformer(TaskListener listener, TestType[] typesTests, CoverageType[] typesCoverage, ViolationsType[] typesViolations, MeasureType[] typesMeasures) {
        this.listener = listener;
        this.typesTests = typesTests;
        this.typesCoverage = typesCoverage;
        this.typesViolations = typesViolations;
        this.typesMeasures = typesMeasures;
    }

    /**
     * Get the name display in the tool
     *
     * @param tool the current tool
     *
     * @return the label in UI of the tool
     * @throws TusarNotifierException
     *
     */
    private String getDisplayName(MetricsType tool) throws TusarNotifierException {

        if (tool instanceof TestType) {
            return ((TestType) tool).getDescriptor().getDisplayName();
        }
        if (tool instanceof MeasureType) {
            return ((MeasureType) tool).getDescriptor().getDisplayName();
        }
        if (tool instanceof ViolationsType) {
            return ((ViolationsType) tool).getDescriptor().getDisplayName();
        }
        if (tool instanceof CoverageType) {
            return ((CoverageType) tool).getDescriptor().getDisplayName();
        }
        throw new TusarNotifierException();
    }


    /**
     * /**
     * Collect reports from the given parentpath and the pattern, while
     * filtering out all files that were created before the given time.
     *
     * @param listener   the listener
     * @param tool       the current tool
     * @param parentPath the parent output directory
     * @param pattern    pattern to seach files
     * @return an array of strings
     * @throws com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException
     *
     */
    private List<String> findtReports(TaskListener listener, MetricsType tool, File parentPath, String pattern)
            throws TusarNotifierException {
        FileSet fs = Util.createFileSet(parentPath, pattern);
        DirectoryScanner ds = fs.getDirectoryScanner();
        String[] xunitFiles = ds.getIncludedFiles();

        if (xunitFiles.length == 0) {
            String msg = "[ERROR] - No test report file(s) were found with the pattern '"
                    + pattern + "' relative to '" + parentPath + "' for the ressource '" + getDisplayName(tool) + "'."
                    + "  Did you enter a pattern relative to the correct directory?"
                    + "  Did you generate the result report(s) for '" + getDisplayName(tool) + "'?";
            TusarNotifierLogger.log(listener, msg);
            return null;
        }

        return Arrays.asList(xunitFiles);
    }

    /**
     * Convert
     *
     * @param listener
     * @param workspace
     * @param generatedDirectory
     * @param metricsType
     * @return
     * @throws com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException
     *
     */
    private boolean convertTusar(TaskListener listener, File workspace, File generatedDirectory, MetricsType metricsType)
            throws TusarNotifierException {

        //Get the associated inputMetric object
        InputMetric inputMetric = metricsType.getInputMetric();
        Assert.assertNotNull(inputMetric);

        TusarNotifierLogger.log(listener, "Starting to process " + inputMetric.getLabel());

        //Retrieve the pattern and filter it with environment variables
        String curPattern = metricsType.getPattern();
        curPattern = curPattern.replaceAll("[\t\r\n]+", " ");
        //curPattern = Util.replaceMacro(curPattern, listener);

        //Get all input files matching the user pattern
        List<String> resultFiles = findtReports(listener, metricsType, workspace, curPattern);
        if (resultFiles == null || resultFiles.size() == 0) {
            return false;
        }

        try {

            TusarNotifierLogger.log(listener, "[" + getDisplayName(metricsType) + "] - Processing " + resultFiles.size() + " files with the pattern '" + metricsType.getPattern() + "' relative to '" + workspace + "'.");
            for (String resultFileName : resultFiles) {
                File resultFile = new File(workspace, resultFileName);

                if (resultFile.length() == 0) {
                    //Ignore the empty result file (some reason)
                    String msg = "[WARNING] - The file '" + resultFile.getPath() + "' is empty. This file has been ignored.";
                    TusarNotifierLogger.log(listener, msg);
                    for (ValidatorError validatorError : inputMetric.getInputValidationErrors()) {
                        TusarNotifierLogger.log(listener, "[WARNING] " + validatorError.toString());
                    }
                    continue;
                }

                //Validate the input file (nom empty)
                if (!inputMetric.validateInputFile(resultFile)) {

                    //Ignore invalid files
                    TusarNotifierLogger.log(listener, "[WARNING] - The file '" + resultFile + "' is an invalid file. It has been ignored.");
                    continue;
                }

                // Process the conversion
                File tusarTargetFile = new File(generatedDirectory, resultFile.hashCode() + ".xml");
                TusarNotifierLogger.log(listener, "[INFO] - Converting '" + resultFile + "' .");
                inputMetric.convert(resultFile, tusarTargetFile);


                //Validate the output
                boolean validateOutput = inputMetric.validateOutputFile(tusarTargetFile);
                if (!validateOutput) {
                    TusarNotifierLogger.log(listener, "[WARNING] - The converted file for the input file '" + resultFile + "' doesn't match the TUSAR format");
                    for (ValidatorError validatorError : inputMetric.getOutputValidationErrors()) {
                        TusarNotifierLogger.log(listener, "[WARNING] " + validatorError.toString());
                    }
                    TusarNotifierLogger.log(listener, "Ending to process " + inputMetric.getLabel());
                    return false;
                }
            }
        }
        catch (ConvertException ce) {
            throw new TusarNotifierException("Can't convert " + inputMetric, ce);
        }
        catch (ValidatorException vae) {
            throw new TusarNotifierException("Can't validate " + inputMetric, vae);
        }

        TusarNotifierLogger.log(listener, "Ending to process " + inputMetric.getLabel());
        return true;
    }


    public String invoke(File workspace, hudson.remoting.VirtualChannel channel) throws IOException, InterruptedException {

        final StringBuffer sb = new StringBuffer();

        final String generatedFolder = "generatedTUSARFiles";
        final String generatedTests = generatedFolder + "/TESTS";
        final String generatedCoverage = generatedFolder + "/COVERAGE";
        final String generatedMeasures = generatedFolder + "/MEASURES";
        final String generatedViolations = generatedFolder + "/VIOLATIONS";

        try {

            // Apply conversion for all tests tools
            if (typesTests.length != 0) {

                File outputTestsFileParent = new File(workspace, generatedTests);
                if (!outputTestsFileParent.mkdirs()) {
                    TusarNotifierLogger.log(listener, "Can't create " + outputTestsFileParent + ". Maybe the directory already exists.");
                }

                for (TestType testsType : typesTests) {
                    boolean convertOK = convertTusar(listener, workspace, outputTestsFileParent, testsType);
                    if (!convertOK) {
                        return null;
                    }
                }
                sb.append(";").append(generatedTests);
            }

            // Apply conversion for all coverage tools
            if (typesCoverage.length != 0) {

                File outputCoverageFileParent = new File(workspace, generatedCoverage);
                if (!outputCoverageFileParent.mkdirs()) {
                    TusarNotifierLogger.log(listener, "Can't create " + outputCoverageFileParent + ". Maybe the directory already exists.");
                }

                for (CoverageType coverageType : typesCoverage) {
                    boolean convertOK = convertTusar(listener, workspace, outputCoverageFileParent, coverageType);
                    if (!convertOK) {
                        return null;
                    }

                }
                sb.append(";").append(generatedCoverage);
            }

            //-- Apply conversion for all violations tools
            if (typesViolations.length != 0) {

                File outputViolationsFileParent = new File(workspace, generatedViolations);
                if (!outputViolationsFileParent.mkdirs()) {
                    TusarNotifierLogger.log(listener, "Can't create " + outputViolationsFileParent + ". Maybe the directory already exists.");
                }

                for (ViolationsType violationsType : typesViolations) {
                    boolean convertOK = convertTusar(listener, workspace, outputViolationsFileParent, violationsType);
                    if (!convertOK) {
                        return null;
                    }
                }
                sb.append(";").append(generatedViolations);

            }

            // Apply conversion for all measures tools
            if (typesMeasures.length != 0) {

                File outputMeasuresFileParent = new File(workspace, generatedMeasures);
                if (!outputMeasuresFileParent.mkdirs()) {
                    TusarNotifierLogger.log(listener, "Can't create " + outputMeasuresFileParent + ". Maybe the directory already exists.");
                }


                for (MeasureType measuresType : typesMeasures) {
                    boolean convertOK = convertTusar(listener, workspace, outputMeasuresFileParent, measuresType);
                    if (!convertOK) {
                        return null;
                    }
                }
                sb.append(";").append(generatedMeasures);
            }

            // Remove the first character
            sb.delete(0, 1);


        }
        catch (Exception e) {
            TusarNotifierLogger.log(listener, "Tusar notifier error : " + e);
            return null;
        }


        return sb.toString();
    }

}
