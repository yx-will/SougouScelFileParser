/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package will.code.sougou.scel.parser;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.io.FileUtils;
import will.code.sougou.scel.model.SougouThesaurus;
import will.code.sougou.scel.model.SougouWord;

/**
 * This Java implementation is ported from a python implementation by Zhang
 * zhenhu. http://blog.csdn.net/zhangzhenhu/article/details/7014271
 *
 * #搜狗的scel词库就是保存的文本的unicode编码，每两个字节一个字符（中文汉字或者英文字母） #找出其每部分的偏移位置即可 #主要两部分
 * #1.全局拼音表，貌似是所有的拼音组合，字典序 # 格式为(index,len,pinyin)的列表 # index: 两个字节的整数 代表这个拼音的索引
 * # len: 两个字节的整数 拼音的字节长度 # pinyin: 当前的拼音，每个字符两个字节，总长len # #2.汉语词组表 #
 * 格式为(same,py_table_len,py_table,{word_len,word,ext_len,ext})的一个列表 # same: 两个字节
 * 整数 同音词数量 # py_table_len: 两个字节 整数 # py_table: 整数列表，每个整数两个字节,每个整数代表一个拼音的索引 # #
 * word_len:两个字节 整数 代表中文词组字节数长度 # word: 中文词组,每个中文汉字两个字节，总长度word_len # ext_len:
 * 两个字节 整数 代表扩展信息的长度，好像都是10 # ext: 扩展信息 前两个字节是一个整数(不知道是不是词频) 后八个字节全是0 # #
 * {word_len,word,ext_len,ext} 一共重复same次 同音词 相同拼音表 *
 *
 *
 */

/*
 * @author will
 */
public class SougouScelFileParser {
    private static final String FILE_ENCODING = "UTF-16LE";
    private static final int PY_START = 0x1540;
    private static final int CN_WORD_START = 0x2628;
    private static final int THESAURUS_NAME_START = 0x130;
    private static final int THESAURUS_NAME_END = 0x338;
    private static final int THESAURUS_CATEGORY_START = 0x338;
    private static final int THESAURUS_CATEGORY_END = 0x540;
    private static final int THESAURUS_DESCRIPTION_START = 0x540;
    private static final int THESAURUS_DESCRIPTION_END = 0xd40;
    private static final int THESAURUS_EXAMPLE_START = 0xd40;
    private static final int THESAURUS_EXAMPLE_END = PY_START;
    private static final String SCEL_HEADER_FIRST_12_BYTES_PATTERN = "401500004443530101000000";

    public SougouThesaurus readScelFile(String filePathName) throws IOException {

        byte[] allBytes = FileUtils.readFileToByteArray(new File(filePathName));
        ByteBuffer bb = ByteBuffer.wrap(allBytes);

        String headerString = getNextNBytesAsHexString(12, bb);
        if (!headerString.equals(SCEL_HEADER_FIRST_12_BYTES_PATTERN)) {
            System.out.println(headerString);
            throw new RuntimeException("not a valid .scel file?");
        }
        
        System.out.println(this.getNextNBytesAsString(4, bb));

        SougouThesaurus thesaurus = new SougouThesaurus();

        bb.position(THESAURUS_NAME_START);

        int length = THESAURUS_NAME_END - THESAURUS_NAME_START;
        String cikuName = getNextNBytesAsString(length, bb);
        thesaurus.name = cikuName;

        length = THESAURUS_CATEGORY_END - THESAURUS_CATEGORY_START;
        String cikuCategory = getNextNBytesAsString(length, bb);
        thesaurus.category = cikuCategory;

        length = THESAURUS_DESCRIPTION_END - THESAURUS_DESCRIPTION_START;
        String cikuDescription = getNextNBytesAsString(length, bb);
        thesaurus.description = cikuDescription;

        length = THESAURUS_EXAMPLE_END - THESAURUS_EXAMPLE_START;
        String cikuExample = getNextNBytesAsString(length, bb);
        thesaurus.example = cikuExample;

        Map<Integer, String> pinyinTable = this.readPinYinTable(bb);
        if (pinyinTable != null) {
            thesaurus.swList = this.readWordList(bb, pinyinTable);
            Collections.sort(thesaurus.swList);
        }

        return thesaurus;

    }

    private String getNextNBytesAsString(int n, ByteBuffer byteBuffer) throws UnsupportedEncodingException {
        byte[] bytes = copyNextNBytes(n, byteBuffer);
        return printAsString(bytes);
    }

    private String getNextNBytesAsHexString(int n, ByteBuffer byteBuffer) throws UnsupportedEncodingException {
        byte[] bytes = copyNextNBytes(n, byteBuffer);
        return printAsHexString(bytes);
    }

    private byte[] copyNextNBytes(int n, ByteBuffer byteBuffer) {
        byte[] bytes = new byte[n];
        for (int i = 0; i < n; i++) {
            bytes[i] = byteBuffer.get();
        }
        return bytes;
    }

    public Map<Integer, String> readPinYinTable(ByteBuffer bb) throws UnsupportedEncodingException {

        bb.position(PY_START);

        String first4BytesHex = this.getNextNBytesAsHexString(4, bb);
        if (!first4BytesHex.equals("9D010000")) {
            return null;
        }

        //byte[] pinYinByteArray = Arrays.copyOfRange(data, 4, data.length);
        //{[2 bytes: index][2 bytes: length][pinyin string]}

        //int pinYinByteArrayLength = pinYinByteArray.length;
        //int position = 0;
        Map<Integer, String> pinyinTable = new HashMap<>();

        while (bb.hasRemaining()) {
            int pinyinIndex = print2BytesToPositiveInteger(bb.get(), bb.get());

            int pinyinLength = print2BytesToPositiveInteger(bb.get(), bb.get());

            if (pinyinLength > bb.remaining()) {//possible invalid value, skip it.
                bb.position(bb.position() + bb.remaining());
            } else {
                byte[] pinyinDataBytes = this.copyNextNBytes(pinyinLength, bb);
                String pinyin = this.printAsString(pinyinDataBytes);
                if (pinyin.matches("[a-zA-Z]+")) {
                    pinyinTable.put(pinyinIndex, pinyin);
                }
            }
        }

        return pinyinTable;
    }

    private List<SougouWord> readWordList(ByteBuffer bb, Map<Integer, String> globalPinyinTable) throws UnsupportedEncodingException {

        bb.position(CN_WORD_START);

        List<SougouWord> wordList = new ArrayList<>();

        while (bb.hasRemaining()) {

            int samePinyinCount = print2BytesToPositiveInteger(bb.get(), bb.get());
            int pyIndexBytesLength = print2BytesToPositiveInteger(bb.get(), bb.get());

            byte[] pyIndexBytes = this.copyNextNBytes(pyIndexBytesLength, bb);
            String pinyin = this.lookupPinYin(pyIndexBytes, globalPinyinTable);

            for (int i = 0; i < samePinyinCount; i++) {
                //chinese word length in bytes
                int cnWordLength = print2BytesToPositiveInteger(bb.get(), bb.get());
                String cnWord = this.getNextNBytesAsString(cnWordLength, bb);

                //extended data length in bytes

                int extendedDataLength = print2BytesToPositiveInteger(bb.get(), bb.get());
                ByteBuffer bbDup = bb.duplicate();
                bb.position(bb.position() + extendedDataLength);

                //term frequency bytes
                int termFrequency = print2BytesToPositiveInteger(bbDup.get(), bbDup.get());
                wordList.add(new SougouWord(cnWord, pinyin, termFrequency));

            }
        }

        return wordList;

    }

    private String lookupPinYin(byte[] pyIndexBytes, Map<Integer, String> globalPinYinTable) {

        int position = 0;
        int length = pyIndexBytes.length;

        StringBuilder sb = new StringBuilder();
        while (position < length) {
            int index = print2BytesToPositiveInteger(pyIndexBytes[position], pyIndexBytes[position + 1]);
            if (index >= 0) {
                String pinyin = globalPinYinTable.get(index);
                if (pinyin != null) {
                    sb.append(pinyin);
                } else {
                    sb.append("...");
                }
            } else {
                sb.append("...");
            }
            position += 2;
        }

        return sb.toString();

    }

    protected int print2BytesToPositiveInteger(byte lowByte, byte highByte) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);//low bytes -> high
        bb.put(lowByte);
        bb.put(highByte);
        bb.position(0);
        return bb.getInt();
    }

    //http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
    private String printAsHexString(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }

    private String printAsString(byte[] bytes) throws UnsupportedEncodingException {
        return new String(bytes, FILE_ENCODING);
    }
}