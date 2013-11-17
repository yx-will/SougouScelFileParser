/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package will.code.sougou.scel.parser;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import will.code.sougou.scel.model.SougouThesaurus;
import will.code.sougou.scel.model.SougouWord;

/**
 *
 * @author will
 */
public class SougouScelFileParserTest {

    @Test
    public void testSougouScelFileParser1() throws IOException {
        SougouScelFileParser sougouScelFileParser = new SougouScelFileParser();
        SougouThesaurus thesaurus = sougouScelFileParser.readScelFile("src/test/resources/test.scel");
        Assert.assertNotNull(thesaurus.swList);
        print(thesaurus);
    }

    @Test
    public void testByteToShort() {
        byte lb = (byte) 254;//1111 1110
        byte hb = (byte) 255;//1111 1111
        int s = new SougouScelFileParser().print2BytesToPositiveInteger(lb, hb);
        Assert.assertEquals(65534, s);
    }

    private void print(SougouThesaurus thesaurus) {
        System.out.println("词库名: " + thesaurus.name);
        System.out.println("词库类别: " + thesaurus.category);
        System.out.println("词库描述: " + thesaurus.description);
        System.out.println("词库示例: " + thesaurus.example);
        for (SougouWord sw : thesaurus.swList) {
            System.out.println("{" + sw.wordString + "," + sw.pinYinString + "," + sw.termFrequency + "}");
        }
    }
}
