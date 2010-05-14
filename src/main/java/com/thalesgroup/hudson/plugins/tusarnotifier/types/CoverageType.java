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

package com.thalesgroup.hudson.plugins.tusarnotifier.types;

import com.thalesgroup.hudson.plugins.tusarnotifier.descriptors.CoverageTypeDescriptor;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.lang.reflect.Constructor;

public class CoverageType implements ExtensionPoint, Describable<CoverageType>, Serializable {

    private String inputTypeKey;

    private String pattern;

    private transient CoverageTypeDescriptor<?> descriptor;

    @DataBoundConstructor
    public CoverageType(String inputTypeKey, String pattern) {
        this.inputTypeKey = inputTypeKey;
        this.pattern = pattern;
        this.createDescriptor();
    }

    public CoverageType(String inputTypeKey) {
        this.inputTypeKey = inputTypeKey;
        this.createDescriptor();
    }

    public String getPattern() {
        return pattern;
    }

    public String getInputTypeKey() {
        return inputTypeKey;
    }

    public CoverageTypeDescriptor<?> getDescriptor() {
        if (descriptor == null) {
            this.createDescriptor();
        }
        return descriptor;
    }

    private void createDescriptor() {
        try {
            Constructor<CoverageTypeDescriptor> constr = CoverageTypeDescriptor.class.getDeclaredConstructor(CoverageType.class);
            descriptor = constr.newInstance(this);
        }
        catch (Exception e) {
            System.err.print("Descriptor creation error : " + e);
        }
    }

}
