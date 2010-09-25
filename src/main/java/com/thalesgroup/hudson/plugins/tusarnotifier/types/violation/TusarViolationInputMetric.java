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

package com.thalesgroup.hudson.plugins.tusarnotifier.types.violation;

import com.thalesgroup.dtkit.metrics.model.InputMetricOther;
import com.thalesgroup.dtkit.metrics.model.InputType;
import com.thalesgroup.dtkit.tusar.model.TusarModel;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.validator.ValidationError;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class TusarViolationInputMetric extends InputMetricOther {

    @Override
    public InputType getToolType() {
        return InputType.VIOLATION;
    }

    @Override
    public String getToolName() {
        return "Tusar";
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    /**
     * Convert an input file to an output file
     * Give your conversion process
     * Input and Output files are relatives to the filesystem where the process is executed on (like Hudson agent)
     *
     * @param inputFile the input file to convert
     * @param outFile   the output file to convert
     * @throws com.thalesgroup.dtkit.util.converter.ConversionException
     *          an application Exception to throw when there is an error of conversion
     *          The exception is catched by the API client (as Hudson plugin)
     */
    @Override
    public void convert(File inputFile, File outFile) throws ConversionException {
        //Copy input to output
        try {
            FileUtils.copyFile(inputFile, outFile, false);
        } catch (IOException ioe) {
            throw new ConversionException("Conversion error occur- Can't copy file from " + inputFile + "to " + outFile, ioe);
        }
    }

    /*
     *  Gives the validation process for the input file
     *
     * @return true if the input file is valid, false otherwise
     */
    @Override
    public boolean validateInputFile(File inputXMLFile) throws ValidationException {
        List<ValidationError> errors = TusarModel.OUTPUT_TUSAR_1_0.validate(inputXMLFile);
        this.setInputValidationErrors(errors);
        return errors.isEmpty();
    }

    /*
     *  Gives the validation process for the output file
     *
     * @return true if the input file is valid, false otherwise
     */
    @Override
    public boolean validateOutputFile(File inputXMLFile) throws ValidationException {
        List<ValidationError> errors = TusarModel.OUTPUT_TUSAR_1_0.validate(inputXMLFile);
        this.setOutputValidationErrors(errors);
        return errors.isEmpty();
    }

}
