package weka.core;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

public class DBmWiFiDistance extends EuclideanDistance {

    private int noiseLevel = 5;

    public String globalInfo() {
        return "if abs(dBmDiff) > noiseLevel then euclidean_dist else 0\n" + super.globalInfo();
    }

    @Override
    protected double difference(int index, double val1, double val2) {
        switch (m_Data.attribute(index).type()) {
            case Attribute.NOMINAL:
                if (Utils.isMissingValue(val1) || Utils.isMissingValue(val2)
                        || ((int) val1 != (int) val2)) {
                    return 1;
                } else {
                    return 0;
                }

            case Attribute.NUMERIC:
                if (Utils.isMissingValue(val1) || Utils.isMissingValue(val2)) {
                    if (Utils.isMissingValue(val1) && Utils.isMissingValue(val2)) {
                        return (m_Ranges[index][R_MAX] - m_Ranges[index][R_MIN]);
                    } else {
                        double diff;
                        if (Utils.isMissingValue(val2)) {
                            diff = val1;
                        } else {
                            diff = val2;
                        }
                        if ((m_Ranges[index][R_MAX] - diff) > (diff - m_Ranges[index][R_MIN])) {
                            return m_Ranges[index][R_MAX] - diff;
                        } else {
                            return diff - m_Ranges[index][R_MIN];
                        }
                    }
                } else {
                    if (aboveNoiseLevel(dBmDiff(val1, val2))) {
                        return val1 - val2;
                    }
                }

            default:
                return 0;
        }
    }

    protected double dBmDiff(double val1, double val2) {
        return val1 - val2;
    }

    private boolean aboveNoiseLevel(double diff) {
        return Math.abs(diff) > noiseLevel;
    }

    @Override
    public void setOptions(String[] options) throws Exception {
        super.setOptions(options);
        String knnString = Utils.getOption('N', options);
        if (knnString.length() != 0) {
            setNoiseLevel(Integer.parseInt(knnString));
        } else {
            setNoiseLevel(5);
        }
    }

    @Override
    public String[] getOptions() {
        Vector<String> options = new Vector<>();
        Collections.addAll(options, super.getOptions());
        options.add("-N");
        options.add("" + getNoiseLevel());
        return options.toArray(new String[0]);
    }

    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> result = new Vector<>();

        result.add(new Option(
                "\tSpecifies list of columns to used in the calculation of the \n"
                        + "\tdistance. 'first' and 'last' are valid indices.\n"
                        + "\t(default: first-last)", "R", 1, "-R <col1,col2-col4,...>"));

        result.addElement(new Option("\tInvert matching sense of column indices.",
                "V", 0, "-V"));

        result.addElement(new Option("\tChange WiFi noise level [dBm] above which the distance will be calculated (usually around 5 dBm).",
                "N", 1, "-N"));

        return result.elements();
    }

    public int getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(int noiseLevel) {
        this.noiseLevel = noiseLevel;
    }
}
