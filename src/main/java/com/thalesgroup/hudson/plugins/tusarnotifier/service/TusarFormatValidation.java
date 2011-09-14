package com.thalesgroup.hudson.plugins.tusarnotifier.service;

import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.tusar.model.TusarModel;

import java.io.File;
import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class TusarFormatValidation implements Serializable {

    public boolean isTusarFormat(File inputXMLFile) {

        if (inputXMLFile == null) {
            throw new NullPointerException("A file must be set.");
        }

        for (OutputMetric outputMetric : TusarModel.getAllTUSAROutput()) {
            if (outputMetric.validate(inputXMLFile).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
