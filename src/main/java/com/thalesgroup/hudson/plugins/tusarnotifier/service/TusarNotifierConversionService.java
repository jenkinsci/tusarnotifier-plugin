/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 * Author : Gregory Boissinot                                                   *
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

package com.thalesgroup.hudson.plugins.tusarnotifier.service;

import com.google.inject.Inject;
import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.metrics.hudson.api.type.MetricsType;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException;
import com.thalesgroup.hudson.plugins.tusarnotifier.transformer.TusarToolInfo;

import java.io.File;
import java.io.Serializable;


public class TusarNotifierConversionService implements Serializable {

    private TusarNotifierLog xUnitLog;

    @Inject
    @SuppressWarnings("unused")
    void load(TusarNotifierLog xUnitLog) {
        this.xUnitLog = xUnitLog;
    }

    /**
     * Prepares the conversion by adding specific behavior for the CustomType
     *
     * @param tusarToolInfo the tusarToolInfo info wrapper object
     * @param workspace     the current workspace
     * @throws com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException
     *          an XUnitException is thrown if there is a preparation error.
     */
    private void prepareConversion(TusarToolInfo tusarToolInfo, File workspace) throws TusarNotifierException {
        MetricsType metricsType = tusarToolInfo.getMetricsType();
//        if (metricsType.getClass() == CustomType.class) {
//            String xsl = ((CustomType) testType).getCustomXSL();
//            File xslFile = new File(workspace, xsl);
//            if (!xslFile.exists()) {
//                throw new XUnitException("The input xsl '" + xsl + "' relative to the workspace '" + workspace + "'doesn't exist.");
//            }
//            xUnitToolInfo.setCusXSLFile(xslFile);
//        }
    }


    /**
     * Converts the inputFile into a JUnit output file
     *
     * @param tusarToolInfo   the tusar tool info wrapper object
     * @param inputFile       the input file to be converted
     * @param workspace       the workspace
     * @param outputDirectory the output parent directory that contains the TUSAR output file
     * @return the converted file
     * @throws com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException
     *          an XUnitException is thrown if there is a conversion error.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public File convert(TusarToolInfo tusarToolInfo, File inputFile, File workspace, File outputDirectory) throws TusarNotifierException {

        //Prepare the conversion when there is a custom type
        prepareConversion(tusarToolInfo, workspace);

        MetricsType metricsType = tusarToolInfo.getMetricsType();

        InputMetric inputMetric = metricsType.getInputMetric();

        final String TUSAR_FILE_POSTFIX = ".xml";
        final String TUSAR_FILE_PREFIX = "TUSAR-";
//        File parent = new File(outputDirectory, inputMetric.getToolName());
//        parent.mkdirs();
//        if (!parent.exists()) {
//            throw new TusarNotifierException("Can't create " + parent);
//        }
//        File junitTargetFile = new File(parent, TUSAR_FILE_PREFIX + inputFile.hashCode() + TUSAR_FILE_POSTFIX);
        File junitTargetFile = new File(outputDirectory, TUSAR_FILE_PREFIX + inputFile.hashCode() + TUSAR_FILE_POSTFIX);
        xUnitLog.info("Converting '" + inputFile + "' .");
        try {

//            //Set the XSL for custom type
//            if (testType.getClass() == CustomType.class) {
//                ((CustomInputMetric) inputMetric).setCustomXSLFile(xUnitToolInfo.getCusXSLFile());
//            }

            inputMetric.convert(inputFile, junitTargetFile);
        } catch (ConversionException ce) {
            throw new TusarNotifierException("Conversion error", ce);
        }

        return junitTargetFile;
    }
}
