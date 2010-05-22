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

import com.thalesgroup.hudson.library.tusarconversion.ConversionType;
import com.thalesgroup.hudson.library.tusarconversion.ConversionUtil;
import com.thalesgroup.hudson.library.tusarconversion.exception.ConversionException;
import com.thalesgroup.hudson.library.tusarconversion.model.InputType;
import com.thalesgroup.hudson.plugins.tusarnotifier.descriptors.CoverageTypeDescriptor;
import com.thalesgroup.hudson.plugins.tusarnotifier.descriptors.MeasuresTypeDescriptor;
import com.thalesgroup.hudson.plugins.tusarnotifier.descriptors.TestsTypeDescriptor;
import com.thalesgroup.hudson.plugins.tusarnotifier.descriptors.ViolationsTypeDescriptor;
import com.thalesgroup.hudson.plugins.tusarnotifier.types.CoverageType;
import com.thalesgroup.hudson.plugins.tusarnotifier.types.MeasuresType;
import com.thalesgroup.hudson.plugins.tusarnotifier.types.TestsType;
import com.thalesgroup.hudson.plugins.tusarnotifier.types.ViolationsType;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.logging.Logger;

public class TusarNotifier extends Notifier {

    private static final Logger LOG = Logger.getLogger(TusarNotifier.class.getName());

    private TestsType[] typesTests;
    private CoverageType[] typesCoverage;
    private ViolationsType[] typesViolations;
    private MeasuresType[] typesMeasures;

    public TusarNotifier(TestsType[] typesTests, CoverageType[] typesCoverage,
                         ViolationsType[] typesViolations, MeasuresType[] typesMeasures) {
        this.typesTests = typesTests;
        this.typesCoverage = typesCoverage;
        this.typesViolations = typesViolations;
        this.typesMeasures = typesMeasures;
    }

    public TestsType[] getTypesTests() {
        return typesTests;
    }

    public CoverageType[] getTypesCoverage() {
        return typesCoverage;
    }

    public ViolationsType[] getTypesViolations() {
        return typesViolations;
    }

    public MeasuresType[] getTypesMeasures() {
        return typesMeasures;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {

        final String delimiter = " ";
        final String tusarLanguage = "-Dsonar.language=tusar";
        final String testsArgs = "-Dsonar.surefire.reportsPath=";
        final String coverageArgs = "-Dsonar.tusar.coverage.reportsPath=";
        final String violationsArgs = "-Dsonar.tusar.violations.reportsPath=";
        final String measuresArgs = "-Dsonar.tusar.measures.reportsPath=";
        final StringBuffer sonarArgs = new StringBuffer();

        boolean resultConversion = build.getWorkspace().act(new FileCallable<Boolean>() {

            private void convertTusar(File workspace, String categoryName, String inputTypeKey, String pattern) throws IOException, InterruptedException, ConversionException {

                assert workspace != null;
                assert categoryName != null;
                assert inputTypeKey != null;
                assert pattern != null;


                final String generatedFoderName = "generatedTUSARFiles";
                InputType inputType = ConversionType.TESTS.getAll().get(inputTypeKey);
                File outputFileParent = new File(workspace, generatedFoderName + "/" + categoryName);
                outputFileParent.mkdirs();

                File outputFile = new File(outputFileParent, "/result-junit-" + pattern.replace("/", "_"));
                outputFile.createNewFile();
                File inputFile = new File(workspace, pattern);
                if (!inputFile.exists()) {
                    throw new IOException("No input files with the pattern " + pattern + " have been found.");
                }
                conversion(inputType, new File(workspace, pattern), outputFile);
            }

            public Boolean invoke(File workspace, hudson.remoting.VirtualChannel channel) throws IOException, InterruptedException {

                try {

                    // Apply conversion for all tests tools
                    for (TestsType testsType : typesTests) {
                        convertTusar(workspace, "TESTS", testsType.getInputTypeKey(), testsType.getPattern());
                    }
                    if (typesTests.length != 0) {
                        sonarArgs.append(delimiter);
                        sonarArgs.append(testsArgs);
                    }

                    // Apply conversion for all coverage tools
                    for (CoverageType coverageType : typesCoverage) {
                        convertTusar(workspace, "COVERAGE", coverageType.getInputTypeKey(), coverageType.getPattern());
                    }
                    if (typesCoverage.length != 0) {
                        sonarArgs.append(delimiter);
                        sonarArgs.append(coverageArgs);
                    }

                    // Apply conversion for all measures tools
                    for (MeasuresType measuresType : typesMeasures) {
                        convertTusar(workspace, "MEASURES", measuresType.getInputTypeKey(), measuresType.getPattern());
                    }
                    if (typesMeasures.length != 0) {
                        sonarArgs.append(delimiter);
                        sonarArgs.append(measuresArgs);
                    }

                    //-- Apply conversion for all violations tools
                    for (ViolationsType violationsType : typesViolations) {
                        convertTusar(workspace, "VIOLATIONS", violationsType.getInputTypeKey(), violationsType.getPattern());
                    }
                    if (typesViolations.length != 0) {
                        sonarArgs.append(delimiter);
                        sonarArgs.append(violationsArgs);
                    }
                }
                catch (Exception e) {
                    listener.getLogger().print("Tusar notifier error : " + e);
                    return false;
                }

                return true;
            }

        }

        );

        if (!resultConversion) {
            build.setResult(Result.FAILURE);
            return false;
        } else {
            //Export TUSAR Arguments as Hudosn build paramaters for Sonar Light mode
            sonarArgs.insert(0, tusarLanguage);
            build.getBuildVariables().put("SONAR_TUSAR_ARGS", sonarArgs.toString());
            return true;
        }

    }


    private void conversion(InputType type, File inputFile, File outputFile) throws IOException, ConversionException {

        InputStream inputStream = new FileInputStream(inputFile);
        OutputStream outputStream = new FileOutputStream(outputFile);

        ConversionUtil.convert(type, inputStream, outputStream);
    }


    @Extension(ordinal = 0)
    public static final class TusarNotifierDescriptor extends BuildStepDescriptor<Publisher> {

        private static DescriptorExtensionList<TestsType, TestsTypeDescriptor<?>> testsDescriptorExtensionList;
        private static DescriptorExtensionList<CoverageType, CoverageTypeDescriptor<?>> coverageDescriptorExtensionList;
        private static DescriptorExtensionList<ViolationsType, ViolationsTypeDescriptor<?>> violationsDescriptorExtensionList;
        private static DescriptorExtensionList<MeasuresType, MeasuresTypeDescriptor<?>> measuresDescriptorExtensionList;

        public TusarNotifierDescriptor() {
            super(TusarNotifier.class);
            load();
        }

        static {
            //TODO : a changer: charge au demarrage Hudson actuellement
            testsDescriptorExtensionList = initTests();
            coverageDescriptorExtensionList = initCoverage();
            violationsDescriptorExtensionList = initViolations();
            measuresDescriptorExtensionList = initMeasures();
        }

        public static DescriptorExtensionList<TestsType, TestsTypeDescriptor<?>> initTests() {
            DescriptorExtensionList<TestsType, TestsTypeDescriptor<?>> testsList = DescriptorExtensionList.create(Hudson.getInstance(), TestsType.class);

            try {
                for (Map.Entry<String, InputType> entry : ConversionType.TESTS.getAll().entrySet()) {
                    Constructor<TestsType> constr = TestsType.class.getDeclaredConstructor(String.class);
                    TestsType type = constr.newInstance(entry.getKey());
                    testsList.add(type.getDescriptor());
                }
            }
            catch (Exception e) {
                System.err.print("Descriptor creation error : " + e);
            }

            return testsList;
        }

        public static DescriptorExtensionList<CoverageType, CoverageTypeDescriptor<?>> initCoverage() {
            DescriptorExtensionList<CoverageType, CoverageTypeDescriptor<?>> coverageList = DescriptorExtensionList.create(Hudson.getInstance(), CoverageType.class);
            try {
                for (Map.Entry<String, InputType> entry : ConversionType.COVERAGE.getAll().entrySet()) {
                    Constructor<CoverageType> constr = CoverageType.class.getDeclaredConstructor(String.class);
                    CoverageType type = constr.newInstance(entry.getKey());
                    coverageList.add(type.getDescriptor());
                }
            }
            catch (Exception e) {
                System.err.print("Descriptor creation error : " + e);
            }

            return coverageList;
        }

        public static DescriptorExtensionList<ViolationsType, ViolationsTypeDescriptor<?>> initViolations() {
            DescriptorExtensionList<ViolationsType, ViolationsTypeDescriptor<?>> violationsList = DescriptorExtensionList.create(Hudson.getInstance(), ViolationsType.class);
            try {
                for (Map.Entry<String, InputType> entry : ConversionType.VIOLATIONS.getAll().entrySet()) {
                    Constructor<ViolationsType> constr = ViolationsType.class.getDeclaredConstructor(String.class);
                    ViolationsType type = constr.newInstance(entry.getKey());
                    violationsList.add(type.getDescriptor());
                }
            }
            catch (Exception e) {
                System.err.print("Descriptor creation error : " + e);
            }

            return violationsList;
        }

        public static DescriptorExtensionList<MeasuresType, MeasuresTypeDescriptor<?>> initMeasures() {
            DescriptorExtensionList<MeasuresType, MeasuresTypeDescriptor<?>> measuresList = DescriptorExtensionList.create(Hudson.getInstance(), MeasuresType.class);
            try {
                for (Map.Entry<String, InputType> entry : ConversionType.MEASURES.getAll().entrySet()) {
                    Constructor<MeasuresType> constr = MeasuresType.class.getDeclaredConstructor(String.class);
                    MeasuresType type = constr.newInstance(entry.getKey());
                    measuresList.add(type.getDescriptor());
                }
            }
            catch (Exception e) {
                System.err.print("Descriptor creation error : " + e);
            }

            return measuresList;
        }

        @Override
        public String getDisplayName() {
            return "Generic conversion";
        }

        @Override
        public boolean isApplicable(Class type) {
            return true;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/tusarnotifier/help.html";

        }


        public DescriptorExtensionList<TestsType, TestsTypeDescriptor<?>> getAllTests() {
            return testsDescriptorExtensionList;
        }

        public DescriptorExtensionList<CoverageType, CoverageTypeDescriptor<?>> getAllCoverage() {
            return coverageDescriptorExtensionList;
        }

        public DescriptorExtensionList<ViolationsType, ViolationsTypeDescriptor<?>> getAllViolations() {
            return violationsDescriptorExtensionList;
        }

        public DescriptorExtensionList<MeasuresType, MeasuresTypeDescriptor<?>> getAllMeasures() {
            return measuresDescriptorExtensionList;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {

            Collection<TestsTypeDescriptor<?>> testsDescriptors = new ArrayList<TestsTypeDescriptor<?>>();
            Collection<CoverageTypeDescriptor<?>> coverageDescriptors = new ArrayList<CoverageTypeDescriptor<?>>();
            Collection<ViolationsTypeDescriptor<?>> violationsDescriptors = new ArrayList<ViolationsTypeDescriptor<?>>();
            Collection<MeasuresTypeDescriptor<?>> measuresDescriptors = new ArrayList<MeasuresTypeDescriptor<?>>();

            Iterable<Descriptor> iterableTests = Hudson.getInstance().getDescriptorList(TestsType.class).listLegacyInstances();
            for (Iterator<Descriptor> it = iterableTests.iterator(); it.hasNext();) {
                Descriptor desc = it.next();
                if (desc instanceof TestsTypeDescriptor) {
                    testsDescriptors.add((TestsTypeDescriptor) desc);
                }
            }

            Iterable<Descriptor> iterableCoverage = Hudson.getInstance().getDescriptorList(CoverageType.class).listLegacyInstances();
            for (Iterator<Descriptor> it = iterableCoverage.iterator(); it.hasNext();) {
                Descriptor desc = it.next();
                if (desc instanceof CoverageTypeDescriptor) {
                    coverageDescriptors.add((CoverageTypeDescriptor) desc);
                }
            }

            Iterable<Descriptor> iterableViolations = Hudson.getInstance().getDescriptorList(ViolationsType.class).listLegacyInstances();
            for (Iterator<Descriptor> it = iterableViolations.iterator(); it.hasNext();) {
                Descriptor desc = it.next();
                if (desc instanceof ViolationsTypeDescriptor) {
                    violationsDescriptors.add((ViolationsTypeDescriptor) desc);
                }
            }

            Iterable<Descriptor> iterableMeasures = Hudson.getInstance().getDescriptorList(MeasuresType.class).listLegacyInstances();
            for (Iterator<Descriptor> it = iterableMeasures.iterator(); it.hasNext();) {
                Descriptor desc = it.next();
                if (desc instanceof MeasuresTypeDescriptor<?>) {
                    measuresDescriptors.add((MeasuresTypeDescriptor) desc);
                }
            }

            List<TestsType> tests = Descriptor.newInstancesFromHeteroList(
                    req, formData, "tests", testsDescriptors);
            List<CoverageType> coverage = Descriptor.newInstancesFromHeteroList(
                    req, formData, "coverage", coverageDescriptors);
            List<ViolationsType> violations = Descriptor.newInstancesFromHeteroList(
                    req, formData, "violations", violationsDescriptors);
            List<MeasuresType> measures = Descriptor.newInstancesFromHeteroList(
                    req, formData, "measures", measuresDescriptors);

            return new TusarNotifier(tests.toArray(new TestsType[tests.size()]),
                    coverage.toArray(new CoverageType[coverage.size()]),
                    violations.toArray(new ViolationsType[violations.size()]),
                    measures.toArray(new MeasuresType[measures.size()]));
        }
    }

}
