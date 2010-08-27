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


package com.thalesgroup.hudson.plugins.tusarnotifier.transformer;

import com.google.inject.Inject;
import com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException;
import com.thalesgroup.hudson.plugins.tusarnotifier.service.TusarNotifierConversionService;
import com.thalesgroup.hudson.plugins.tusarnotifier.service.TusarNotifierLog;
import com.thalesgroup.hudson.plugins.tusarnotifier.service.TusarNotifierReportProcessingService;
import com.thalesgroup.hudson.plugins.tusarnotifier.service.TusarNotifierValidationService;
import hudson.FilePath;
import hudson.util.IOException2;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class TusarNotifierTransformer implements FilePath.FileCallable<Boolean>, Serializable {

    private TusarNotifierReportProcessingService tusarNotifierReportProcessingService;

    private TusarNotifierConversionService tusarNotifierConversionService;

    private TusarNotifierValidationService tusarNotifierValidationService;

    private TusarToolInfo tusarToolInfo;

    private TusarNotifierLog tusarNotifierLog;

    @Inject
    @SuppressWarnings("unused")
    void loadService(
            TusarNotifierReportProcessingService tusarNotifierReportProcessingService,
            TusarNotifierConversionService tusarNotifierConversionService,
            TusarNotifierValidationService tusarNotifierValidationService,
            TusarToolInfo tusarToolInfo,
            TusarNotifierLog tusarNotifierLog) {
        this.tusarNotifierReportProcessingService = tusarNotifierReportProcessingService;
        this.tusarNotifierConversionService = tusarNotifierConversionService;
        this.tusarNotifierValidationService = tusarNotifierValidationService;
        this.tusarToolInfo = tusarToolInfo;
        this.tusarNotifierLog = tusarNotifierLog;
    }

    public Boolean invoke(File ws, hudson.remoting.VirtualChannel channel) throws IOException, InterruptedException {

        try {

            //Gets all input files matching the user pattern
            List<String> resultFiles = tusarNotifierReportProcessingService.findReports(tusarToolInfo, ws, tusarToolInfo.getExpandedPattern());
            if (resultFiles.size() == 0) {
                return false;
            }

            //Checks the timestamp for each test file if the UI option is checked (true by default)
            if (!tusarNotifierReportProcessingService.checkIfFindsFilesNewFiles(tusarToolInfo, resultFiles, ws)) {
                return false;
            }

            for (String curFileName : resultFiles) {

                File curFile = tusarNotifierReportProcessingService.getCurrentReport(ws, curFileName);

                if (!tusarNotifierValidationService.checkFileIsNotEmpty(curFile)) {
                    //Ignore the empty result file (some reason)
                    String msg = "The file '" + curFile.getPath() + "' is empty. This file has been ignored.";
                    tusarNotifierLog.warning(msg);
                    return false;
                }

                //Validates Input file
                if (!tusarNotifierValidationService.validateInputFile(tusarToolInfo, curFile)) {
                    tusarNotifierLog.warning("The file '" + curFile + "' has been ignored.");
                    return false;
                }

                //Convert the input file
                File tusarTargetFile = tusarNotifierConversionService.convert(tusarToolInfo, curFile, ws, tusarToolInfo.getOutputDir());


                //Validates converted file
                boolean result = tusarNotifierValidationService.validateOutputFile(tusarToolInfo, curFile, tusarTargetFile);
                if (!result) {
                    return false;
                }
            }

        }
        catch (TusarNotifierException xe) {
            throw new IOException2("There are some problems during the conversion into JUnit reports: " + xe.getMessage(), xe);
        }

        return true;
    }

}
