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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Singleton;
import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.CoverageTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.MeasureTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.TestTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.ViolationsTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.type.*;
import com.thalesgroup.hudson.plugins.tusarnotifier.service.TusarNotifierConversionService;
import com.thalesgroup.hudson.plugins.tusarnotifier.service.TusarNotifierLog;
import com.thalesgroup.hudson.plugins.tusarnotifier.service.TusarNotifierReportProcessingService;
import com.thalesgroup.hudson.plugins.tusarnotifier.service.TusarNotifierValidationService;
import com.thalesgroup.hudson.plugins.tusarnotifier.transformer.TusarNotifierTransformer;
import com.thalesgroup.hudson.plugins.tusarnotifier.transformer.TusarToolInfo;
import com.thalesgroup.hudson.plugins.tusarnotifier.util.TusarNotifierLogger;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TusarNotifier extends Notifier {

    final String generatedFolder = "generatedTUSARFiles";
    final String generatedTests = generatedFolder + "/TESTS";
    final String generatedCoverage = generatedFolder + "/COVERAGE";
    final String generatedMeasures = generatedFolder + "/MEASURES";
    final String generatedViolations = generatedFolder + "/VIOLATIONS";

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


    private boolean processInputMetricType(final AbstractBuild<?, ?> build, final BuildListener listener, MetricsType metricsType, FilePath outputFileParent) throws IOException, InterruptedException {

        final TusarNotifierLogger tusarNotifierLog = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(BuildListener.class).toInstance(listener);
            }
        }).getInstance(TusarNotifierLogger.class);

        //Retrieves the pattern
        String newExpandedPattern = metricsType.getPattern();
        newExpandedPattern = newExpandedPattern.replaceAll("[\t\r\n]+", " ");
        newExpandedPattern = Util.replaceMacro(newExpandedPattern, build.getEnvironment(listener));

        //Build a new build info
        final TusarToolInfo tusarToolInfo = new TusarToolInfo(metricsType, new File(outputFileParent.toURI()), newExpandedPattern, build.getTimeInMillis());

        // Archiving tool reports into JUnit files
        TusarNotifierTransformer tusarNotifierTransformer = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(BuildListener.class).toInstance(listener);
                bind(TusarToolInfo.class).toInstance(tusarToolInfo);
                bind(TusarNotifierValidationService.class).in(Singleton.class);
                bind(TusarNotifierConversionService.class).in(Singleton.class);
                bind(TusarNotifierLog.class).in(Singleton.class);
                bind(TusarNotifierReportProcessingService.class).in(Singleton.class);
            }
        }).getInstance(TusarNotifierTransformer.class);

        boolean resultTransformation = build.getWorkspace().act(tusarNotifierTransformer);
        if (!resultTransformation) {
            build.setResult(Result.FAILURE);
            tusarNotifierLog.info("Stopping recording.");
            return false;
        }

        return true;


    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {


        final StringBuffer sb = new StringBuffer();

        final TusarNotifierLogger tusarNotifierLog = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(BuildListener.class).toInstance(listener);
            }
        }).getInstance(TusarNotifierLogger.class);
        tusarNotifierLog.info("Starting converting.");


        TusarNotifierReportProcessingService tusarNotifierReportProcessingService = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(BuildListener.class).toInstance(listener);
            }
        }).getInstance(TusarNotifierReportProcessingService.class);


        boolean isInvoked = false;

        // Apply conversion for all tests tools
        if (tests.length != 0) {
            FilePath outputFileParent = new FilePath(build.getWorkspace(), generatedTests);
            outputFileParent.mkdirs();
            for (TestType testsType : tests) {
                tusarNotifierLog.info("Processing " + testsType.getDescriptor().getDisplayName());
                if (!tusarNotifierReportProcessingService.isEmptyPattern(testsType.getPattern())) {
                    boolean result = processInputMetricType(build, listener, testsType, outputFileParent);
                    if (result) {
                        isInvoked = true;
                    }
                }
            }
            sb.append(";").append(generatedTests);
        }
        if (coverages.length != 0) {
            FilePath outputFileParent = new FilePath(build.getWorkspace(), generatedCoverage);
            outputFileParent.mkdirs();
            for (CoverageType coverageType : coverages) {
                tusarNotifierLog.info("Processing " + coverageType.getDescriptor().getDisplayName());
                if (!tusarNotifierReportProcessingService.isEmptyPattern(coverageType.getPattern())) {
                    boolean result = processInputMetricType(build, listener, coverageType, outputFileParent);
                    if (result) {
                        isInvoked = true;
                    }
                }
            }
            sb.append(";").append(generatedCoverage);
        }
        if (violations.length != 0) {
            FilePath outputFileParent = new FilePath(build.getWorkspace(), generatedViolations);
            outputFileParent.mkdirs();
            for (ViolationsType violationsType : violations) {
                tusarNotifierLog.info("Processing " + violationsType.getDescriptor().getDisplayName());
                if (!tusarNotifierReportProcessingService.isEmptyPattern(violationsType.getPattern())) {
                    boolean result = processInputMetricType(build, listener, violationsType, outputFileParent);
                    if (result) {
                        isInvoked = true;
                    }
                }
            }
            sb.append(";").append(generatedViolations);
        }
        if (measures.length != 0) {
            FilePath outputFileParent = new FilePath(build.getWorkspace(), generatedMeasures);
            outputFileParent.mkdirs();
            for (MeasureType measureType : measures) {
                tusarNotifierLog.info("Processing " + measureType.getDescriptor().getDisplayName());
                if (!tusarNotifierReportProcessingService.isEmptyPattern(measureType.getPattern())) {
                    boolean result = processInputMetricType(build, listener, measureType, outputFileParent);
                    if (result) {
                        isInvoked = true;
                    }
                }
            }
            sb.append(";").append(generatedMeasures);
        }


        // Remove the first character
        sb.delete(0, 1);

        List<ParameterValue> parameterValues = new ArrayList<ParameterValue>();
        parameterValues.add(new StringParameterValue("sonar.language", "tusar"));
        parameterValues.add(new StringParameterValue("sonar.tusar.reportsPaths", sb.toString()));
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
