package weka.core;

public class PWattWiFiDistance extends DBmWiFiDistance {

    @Override
    protected double dBmDiff(double val1, double val2) {
        return super.dBmDiff(pikoWattToDBm(val1), pikoWattToDBm(val2));
    }

    private double pikoWattToDBm(double pWatt) {
        if (pWatt <= 0) {
            return -100;
        } else {
            return 10 * Math.log10(pWatt) - 90;
        }
    }
}
