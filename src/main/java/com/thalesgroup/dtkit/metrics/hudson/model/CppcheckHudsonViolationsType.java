package com.thalesgroup.dtkit.metrics.hudson.model;

import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.ViolationsTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.type.ViolationsType;

public class CppcheckHudsonViolationsType extends ViolationsType {

    public CppcheckHudsonViolationsType(String pattern, boolean faildedIfNotNew, boolean deleteOutputFiles) {
        super(pattern, faildedIfNotNew, deleteOutputFiles);
    }

    public ViolationsTypeDescriptor<? extends ViolationsType> getDescriptor() {
        return null;
    }

    public Object readResolve() {
        return new CppcheckTusarHudsonViolationsType(this.getPattern(), this.isFaildedIfNotNew(), this.isDeleteOutputFiles());
    }

}


