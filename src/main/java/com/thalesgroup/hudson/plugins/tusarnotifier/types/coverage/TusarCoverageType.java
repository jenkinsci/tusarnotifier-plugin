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

package com.thalesgroup.hudson.plugins.tusarnotifier.types.coverage;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.CoverageTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.type.CoverageType;
import com.thalesgroup.dtkit.metrics.model.InputMetric;
import com.thalesgroup.dtkit.util.validator.ValidationService;
import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class TusarCoverageType extends CoverageType {

    private static TusaCoverageTypeDescriptor DESCRIPTOR = new TusaCoverageTypeDescriptor();

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public TusarCoverageType(String pattern, boolean faildedIfNotNew, boolean deleteOutputFiles) {
        super(pattern, faildedIfNotNew, deleteOutputFiles);
    }

    public CoverageTypeDescriptor<? extends CoverageType> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static class TusaCoverageTypeDescriptor extends CoverageTypeDescriptor<TusarCoverageType> {

        public TusaCoverageTypeDescriptor() {
            super(TusarCoverageType.class, null);
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException("The descriptor registry with the called getId() method is not used. The descriptor redefines its own getInputMetric() method.");
        }

        @Override
        public InputMetric getInputMetric() {
            //return InputMetricFactory.getInstance(TusarCoverageInputMetric.class);
            InputMetric inputMetric = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ValidationService.class);
                }
            }).getInstance(TusarCoverageInputMetric.class);
            return inputMetric;
        }
    }
}