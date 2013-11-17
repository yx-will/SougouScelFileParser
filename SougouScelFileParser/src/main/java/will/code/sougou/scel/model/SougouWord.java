/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package will.code.sougou.scel.model;

/**
 *
 * @author will
 */
public class SougouWord implements Comparable {

    public SougouWord(String word, String pinYinString, Integer frequency) {
        this.wordString = word;
        this.pinYinString = pinYinString;
        this.termFrequency = frequency;
    }
    public Integer termFrequency;
    public String wordString;
    public String pinYinString;

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        } else {
            SougouWord sw = (SougouWord) o;

            if (sw.pinYinString != null && this.pinYinString != null) {
                int v = this.pinYinString.compareTo(sw.pinYinString);
                if (v == 0) {
                    if (sw.termFrequency != null && this.termFrequency != null) {
                        return this.termFrequency.compareTo(sw.termFrequency);
                    } else {
                        if (sw.termFrequency == null) {
                            return -1;
                        } else if (this.termFrequency == null) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                } else {
                    return v;
                }
            } else {
                if (sw.pinYinString == null) {
                    return -1;
                } else if (this.pinYinString == null) {
                    return 1;
                } else {
                    return 0;
                }
            }


        }
    }
}
