/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 * Author : Grégory Boissinot, Guillaume Tanier                                 *
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
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.lang.reflect.Constructor;
import java.util.*;

public class TusarNotifier extends Notifier {

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

    @Extension(ordinal = 0)
    public static final class GenericPublisherDescriptor extends BuildStepDescriptor<Publisher> {

        private static DescriptorExtensionList<TestsType, TestsTypeDescriptor<?>> testsDescriptorExtensionList;
        private static DescriptorExtensionList<CoverageType, CoverageTypeDescriptor<?>> coverageDescriptorExtensionList;
        private static DescriptorExtensionList<ViolationsType, ViolationsTypeDescriptor<?>> violationsDescriptorExtensionList;
        private static DescriptorExtensionList<MeasuresType, MeasuresTypeDescriptor<?>> measuresDescriptorExtensionList;

        public GenericPublisherDescriptor() {
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
