package br.ufu.facom.ereno.featureEngineering;

import br.ufu.facom.ereno.messages.EthernetFrame;
import br.ufu.facom.ereno.messages.Goose;
import br.ufu.facom.ereno.messages.Sv;

import java.util.ArrayList;
import java.util.logging.Logger;

public class ProtocolCorrelation {


    /**
     * Finds the closest SV message to the given GOOSE timestamp.
     * @param svs List of SV messages (must be non-null and non-empty)
     * @param goose GOOSE message to correlate (must be non-null)
     * @return Closest SV message or null if no suitable message found
     * @throws IllegalArgumentException if inputs are invalid
     */


    public static Sv getCorrespondingSVFrame(ArrayList<EthernetFrame> svs, Goose goose) {
        // Validate inputs
        if (svs == null || goose == null) {
            throw new IllegalArgumentException("Input parameters cannot be null");
        }
        if (svs.isEmpty()) {
            Logger.getLogger(ProtocolCorrelation.class.getName())
                    .warning("Empty SV messages list");
            return null;
        }

        // Get adjusted timestamps
        double gooseTimestamp = goose.getTimestamp();
        int intGTS = (int) gooseTimestamp;

        // Find closest SV
        Sv closestSv = null;
        double minTimeDiff = Double.MAX_VALUE;

        for (EthernetFrame frame : svs) {
            if (!(frame instanceof Sv)) continue;

            Sv sv = (Sv) frame;
            sv.setRelativeness(intGTS);
            double timeDiff = Math.abs(sv.getTime() - gooseTimestamp);

            if (timeDiff < minTimeDiff) {
                minTimeDiff = timeDiff;
                closestSv = sv;
            }
        }

        if (closestSv == null) {
            Logger.getLogger(ProtocolCorrelation.class.getName())
                    .warning("No valid SV messages found for GOOSE timestamp: " + gooseTimestamp);
        }

        return closestSv;
    }
    public static SVCycle getCorrespondingSVFrameCycle(ArrayList<EthernetFrame> svs, Goose goose, int numCycleMsgs) {
        int low = 0;
        int high = svs.size() - 1;
        int index = -1;
        double gooseTimestamp = goose.getTimestamp();


        // To fix the excessive (redundant) SV message generation
        int intGTS = (int) gooseTimestamp;
        double relativeGooseTimestamp = gooseTimestamp - intGTS;
        double svTime = 0;
        // end


        while (low <= high) {
            int mid = (low + high) >>> 1; // equivalent to (low + high) / 2, but more efficient.
            Sv svMessage = (Sv) svs.get(mid);
            svMessage.setRelativeness(intGTS);
            svTime = (svMessage).getTime();

            if (svTime < gooseTimestamp) {
                index = mid; // Found a candidate, but continue searching for a closer candidate.
                low = mid + 1; // Try to find a SV time closer to GOOSE Timestamp.
            } else {
                // If the SV time is equal or higher, looks to the inferior part.
                high = mid - 1;
            }
        }

        Sv[] cycleMsgs = new Sv[numCycleMsgs];
        index = index + 1; // to address a rounding bug
        if (index < 80) {
            index = 80;
        }
        for (int i = 0; i < numCycleMsgs; i++) {
            cycleMsgs[i] = ((Sv) svs.get(index - numCycleMsgs + i));
        }


        SVCycle cycle = new SVCycle(cycleMsgs);
        cycle.computeMetrics();

        return cycle; // an invalid index will be returned if no SV messages are available before the given GOOSE
    }


    //@TODO: Verificar se este método ainda faz sentido após o CSV e ARFF writters
    public static Sv getCorrespondingSV(ArrayList<Sv> svs, Goose goose) {
        Logger.getLogger("getCorrespondingSV").info("There are " + svs.size() + " SV messages.");
        Logger.getLogger("getCorrespondingSV").info("SV time range: " + svs.get(0).getTime() + " to " + svs.get(svs.size() - 1).getTime() + " - GOOSE Timestamp: " + goose.getTimestamp());
        int low = 0;
        int high = svs.size() - 1;
        int index = -1;
        double gooseTimestamp = goose.getTimestamp();

        // To fix the excessive (redundant) SV message generation
        int intGTS = (int) gooseTimestamp;
        double relativeGooseTimestamp = gooseTimestamp - intGTS;
        double svTime = 0;
        // en

        while (low <= high) {
            int mid = (low + high) >>> 1; // Equivalente a (low + high) / 2, mas mais eficiente.
            svTime = svs.get(mid).getTime();

            if (svTime < relativeGooseTimestamp) {
                index = mid; // Found a candidate, but continue searching for a closer candidate.
                low = mid + 1; // Try to find a SV time closer to GOOSE Timestamp.
            } else {
                // If the SV time is equal or higher, looks to the inferior part.
                high = mid - 1;
            }
        }

        if (index > 0) {
            Logger.getLogger("getCorrespondingSV").info("The last SV message was sent at " + svs.get(index).getTime() + ", before the GOOSE at " + goose.getTimestamp());
            return svs.get(index); // an invalid index will be returned if no SV messages are available before the given GOOSE
        }
        return null;
    }


    //@TODO: Verificar se este método ainda faz sentido após o CSV e ARFF writters
    // Finds the last SV bofore the GOOSE
    public static SVCycle getCorrespondingSVCycle(ArrayList<Sv> svs, Goose goose, int numCycleMsgs) {
        int low = 0;
        int high = svs.size() - 1;
        int index = -1;
        double gooseTimestamp = goose.getTimestamp();

        // To fix the excessive (redundant) SV message generation
        int intGTS = (int) gooseTimestamp;
        double relativeGooseTimestamp = gooseTimestamp - intGTS;
        double svTime = 0;
        // end

        while (low <= high) {
            int mid = (low + high) >>> 1; // equivalent to (low + high) / 2, but more efficient.
            svTime = svs.get(mid).getTime();

            if (svTime < relativeGooseTimestamp) {
                index = mid; // Found a candidate, but continue searching for a closer candidate.
                low = mid + 1; // Try to find a SV time closer to GOOSE Timestamp.
            } else {
                // If the SV time is equal or higher, looks to the inferior part.
                high = mid - 1;
            }
        }

        Sv[] cycleMsgs = new Sv[numCycleMsgs];
        index = index + 1; // to address a rounding bug
        if (index < 80) {
            index = 80;
        }
        for (int i = 0; i < numCycleMsgs; i++) {
//            System.out.println("index(" + index + ") - numCycleMsgs(" + numCycleMsgs + ") + i(" + i + ") = (" + (index - numCycleMsgs + i) + ")");
            cycleMsgs[i] = svs.get(index - numCycleMsgs + i);
        }


        SVCycle cycle = new SVCycle(cycleMsgs);
        cycle.computeMetrics();


        return cycle; // an invalid index will be returned if no SV messages are available before the given GOOSE
    }

    //@TODO: Verificar se este método ainda faz sentido após o CSV e ARFF writters
    // @TODO: Verificar se o relative GOOSE não afetou este método, pois o SV pode não ter offset
    public static int getCorrespondingGoose(ArrayList<Goose> gooses, Sv sv) {
        int low = 0;
        int high = gooses.size() - 1;
        int index = -1;
        double svTime = sv.getTime();
//        gooses.remove(0);
        Logger.getLogger("ProtocolCorrelation").info("Searching for a GOOSE sent after " + svTime + " in the range from " + gooses.get(0).getTimestamp() + " to " + gooses.get(gooses.size() - 1).getTimestamp());

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double gooseTimestamp = gooses.get(mid).getTimestamp();

            if (svTime > gooseTimestamp) {
                index = mid; // Found a candidate, but continue searching for a closer candidate.
                low = mid + 1; // Try to find a SV time closer to GOOSE Timestamp.
            } else {
                // If the SV time is equal or higher, looks to the inferior part.
                high = mid - 1;
            }
        }
        Logger.getLogger("ProtocolCorrelation").info("The last GOOSE sent before " + svTime + " in the range from " + gooses.get(0).getTimestamp() + " to " + gooses.get(gooses.size() - 1).getTimestamp());

        return index;
    }

}
