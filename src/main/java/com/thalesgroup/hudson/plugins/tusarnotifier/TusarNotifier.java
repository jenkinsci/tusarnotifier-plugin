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

import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.CoverageTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.MeasureTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.TestTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.ViolationsTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.type.CoverageType;
import com.thalesgroup.dtkit.metrics.hudson.api.type.MeasureType;
import com.thalesgroup.dtkit.metrics.hudson.api.type.TestType;
import com.thalesgroup.dtkit.metrics.hudson.api.type.ViolationsType;
import com.thalesgroup.hudson.plugins.tusarnotifier.util.TusarNotifierLogger;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TusarNotifier extends Notifier {

    private TestType[] tests;
    private CoverageType[] coverages;
    private ViolationsType[] violations;
    private MeasureType[] measures;

    public TusarNotifier(TestType[] tests,
                         CoverageType[] coverages,
                         ViolationsType[] violations,
                         MeasureType[] measures) {
        this.tests = tests;
        this.coverages = coverages;
        this.violations = violations;
        this.measures = measures;
    }

    @SuppressWarnings("unused")
    public TestType[] getTests() {
        return tests;
    }

    @SuppressWarnings("unused")
    public CoverageType[] getCoverages() {
        return coverages;
    }

    @SuppressWarnings("unused")
    public ViolationsType[] getViolations() {
        return violations;
    }

    @SuppressWarnings("unused")
    public MeasureType[] getMeasures() {
        return measures;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {

        TusarNotifierLogger.log(listener, "Starting processing all analysis reports.");

        TusarTransformer tusarTransformer = new TusarTransformer(launcher.getListener(), tests, coverages, violations, measures);

        String result = build.getWorkspace().act(tusarTransformer);
        if (result == null) {
            build.setResult(Result.FAILURE);
            TusarNotifierLogger.log(listener, "Stopping TUSARNOFIFIER.");
            return true;
        }

        List<ParameterValue> parameterValues = new ArrayList<ParameterValue>();
        parameterValues.add(new StringParameterValue("sonar.language", "tusar"));
        parameterValues.add(new StringParameterValue("sonar.tusar.reportsPaths", result));
        build.addAction(new ParametersAction(parameterValues));

        return true;
    }


    @Extension(ordinal = 1)
    @SuppressWarnings("unused")
    public static final class TusarNotifierDescriptor extends BuildStepDescriptor<Publisher> {


        public TusarNotifierDescriptor() {
            super(TusarNotifier.class);
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/tusarnotifier/help.html";
        }


        @Override
        public String getDisplayName() {
            return "TUSAR Notifier";
        }

        public DescriptorExtensionList<TestType, TestTypeDescriptor<?>> getListTestDescriptors() {
            return TestTypeDescriptor.all();
        }

        public DescriptorExtensionList<ViolationsType, ViolationsTypeDescriptor<?>> getListViolationDescriptors() {
            return ViolationsTypeDescriptor.all();
        }


        public DescriptorExtensionList<MeasureType, MeasureTypeDescriptor<?>> getListMeasureDescriptors() {
            return MeasureTypeDescriptor.all();
        }


        public DescriptorExtensionList<CoverageType, CoverageTypeDescriptor<?>> getListCoverageDescriptors() {
            return CoverageTypeDescriptor.all();
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {

            List<TestType> tests = Descriptor.newInstancesFromHeteroList(req, formData, "tests", getListTestDescriptors());
            List<CoverageType> coverages = Descriptor.newInstancesFromHeteroList(req, formData, "coverages", getListCoverageDescriptors());
            List<ViolationsType> violations = Descriptor.newInstancesFromHeteroList(req, formData, "violations", getListViolationDescriptors());
            List<MeasureType> measures = Descriptor.newInstancesFromHeteroList(req, formData, "measures", getListMeasureDescriptors());

            return new TusarNotifier(tests.toArray(new TestType[tests.size()]),
                    coverages.toArray(new CoverageType[coverages.size()]),
                    violations.toArray(new ViolationsType[violations.size()]),
                    measures.toArray(new MeasureType[measures.size()])
            );
        }
    }

}
