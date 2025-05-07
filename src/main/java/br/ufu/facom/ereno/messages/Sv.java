package br.ufu.facom.ereno.messages;

public class Sv extends EthernetFrame {
    float relativeness = 0;// to compensate GOOSE relative (for resource efficiency purpose)
    float time;
    float iA;
    float iB;
    float iC;
    float vA;
    float vB;
    float vC;
    String status; // fault or normal

    public String asCsv() {
        return getTime() + "," + getiA() + "," + getiB() + "," + getiC() + "," + getvA() + "," + getvB() + "," + getvC();
    }

    public String getCSVHeader() {
        return "time, iA, iB, iC, vA, vB, vC";
    }

    public Sv(float time, float iA, float iB, float iC, float vA, float vB, float vC) {
        this.time = time;
        this.iA = iA;
        this.iB = iB;
        this.iC = iC;
        this.vA = vA;
        this.vB = vB;
        this.vC = vC;
    }

    public Sv(float time, float iA, float iB, float iC, float vA, float vB, float vC, String status) {
        this.time = time;
        this.iA = iA;
        this.iB = iB;
        this.iC = iC;
        this.vA = vA;
        this.vB = vB;
        this.vC = vC;
        this.status = status;
    }

    @Override
    public String toString() {
        if (status == null) {
            return relativeness + time + "," + iA + "," + iB + "," + iC + "," + vA + "," + vB + "," + vC;
        } else {
            return relativeness + time + "," + iA + "," + iB + "," + iC + "," + vA + "," + vB + "," + status;
        }
    }

    public float getTime() {
        return relativeness + time;
    }

    public void setRelativeness(int relativeness) {
        this.relativeness = relativeness;
    }


    public void setTime(float time) {
        this.time = time;
    }

    public float getiA() {
        return iA;
    }

    public void setiA(float iA) {
        this.iA = iA;
    }

    public float getiB() {
        return iB;
    }

    public void setiB(float iB) {
        this.iB = iB;
    }

    public float getiC() {
        return iC;
    }

    public void setiC(float iC) {
        this.iC = iC;
    }

    public float getvA() {
        return vA;
    }

    public void setvA(float vA) {
        this.vA = vA;
    }

    public float getvB() {
        return vB;
    }

    public void setvB(float vB) {
        this.vB = vB;
    }

    public float getvC() {
        return vC;
    }

    public void setvC(float vC) {
        this.vC = vC;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
