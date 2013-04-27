package net.rptools.intern;

public abstract class TestConstants {
    final public static String SEP = System.getProperty("file.separator");
    final public static String TEST_IMAGE = "Test.png";
    final public static String HTTP_IMAGE = "http://localhost:8080/" + TEST_IMAGE;
    final public static String MY_ID = "1234";
    final public static String USER_DIR = System.getProperty("user.dir") + SEP;
    final public static String TEST_DIR = ".maptool" + SEP + "resources" + SEP;
    final public static String TEST_DIR2 = ".maptool" + SEP + "resources2" + SEP;
    final public static String TEST_ZIP = TEST_DIR + "test.zip";
    final public static String TEST_ZIP_FULL = USER_DIR + TEST_ZIP;
}
