import com.htge.download.file.FileRange;
import org.junit.Assert;
import org.junit.Test;

public class FileRangeTest {
    @Test
    public void rangeTest() {
        //只传长度的接口
        //正常情况
        FileRange largeRange = new FileRange(1000000000000L);
        Assert.assertTrue(largeRange.getStart() == 0);
        Assert.assertTrue(largeRange.getEnd() == 999999999999L);
        Assert.assertTrue(largeRange.getTotal() == 1000000000000L);
        Assert.assertTrue(!largeRange.isRangeMode());

        FileRange zeroRange = new FileRange(0);
        Assert.assertTrue(zeroRange.getStart() == 0);
        Assert.assertTrue(zeroRange.getEnd() == -1);
        Assert.assertTrue(zeroRange.getTotal() == 0);
        Assert.assertTrue(!zeroRange.isRangeMode());

        try {
            //不能初始化的情况
            new FileRange(-1);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void rangeStrTest() {
        //字符串接口：传非法参数
        try {
            new FileRange("0-", -1);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new FileRange("-", 0);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new FileRange("-1-", 0);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new FileRange("0-结尾", 0);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new FileRange("开头-0", 0);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new FileRange("fjaewiopncqk123123-wejcweqoncpkasdn123123", 0);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new FileRange("12312321fjae-1231231341234wejcwe", 0);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new FileRange("10000-1", 20000);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            String rangeStr = "0-10000";
            new FileRange(rangeStr, 100);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            String rangeStr = "-10000";
            new FileRange(rangeStr, 100);
            Assert.assertFalse(true);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        //合法的数据
        FileRange zeroRange = new FileRange("0-0", 0);
        Assert.assertTrue(zeroRange.getStart() == 0);
        Assert.assertTrue(zeroRange.getEnd() == -1);
        Assert.assertTrue(zeroRange.getTotal() == 0);
        Assert.assertTrue(zeroRange.isRangeMode());

        FileRange allRange = new FileRange("100000000000-200000000000", 5000000000000L);
        Assert.assertTrue(allRange.getStart() == 100000000000L);
        Assert.assertTrue(allRange.getEnd() == 200000000000L);
        Assert.assertTrue(allRange.getTotal() == 5000000000000L);
        Assert.assertTrue(allRange.isRangeMode());

        FileRange leftRange = new FileRange("0-", 10000000000L);
        Assert.assertTrue(leftRange.getStart() == 0);
        Assert.assertTrue(leftRange.getEnd() == 9999999999L);
        Assert.assertTrue(leftRange.getTotal() == 10000000000L);
        Assert.assertTrue(leftRange.isRangeMode());

        FileRange rightRange = new FileRange("-9999999999", 10000000000L);
        Assert.assertTrue(rightRange.getStart() == 1);
        Assert.assertTrue(rightRange.getEnd() == 9999999999L);
        Assert.assertTrue(rightRange.getTotal() == 10000000000L);
        Assert.assertTrue(rightRange.isRangeMode());
    }
}
