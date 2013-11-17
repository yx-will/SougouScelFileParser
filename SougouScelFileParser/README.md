SougouScelFileParser
====================

搜狗细胞词库文件解析工具 a Java parser for Sougou thesaurus file (.scel)

    String filePath = "test.scel";
    SougouThesaurus thesaurus = new SougouScelFileParser().readScelFile(filePath);
    System.out.println("词库名: " + thesaurus.name);
    System.out.println("词库类别: " + thesaurus.category);
    System.out.println("词库描述: " + thesaurus.description);
    System.out.println("词库示例: " + thesaurus.example);
    for (SougouWord sw : thesaurus.swList) {
        System.out.println("{" + sw.wordString + "," + sw.pinYinString + "," + sw.termFrequency + "}");
    }
