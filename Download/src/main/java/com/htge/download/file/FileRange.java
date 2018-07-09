package com.htge.download.file;

public class FileRange { //package-private类，包内可见
    private long start;
    private long end;
    private long total;
    private boolean rangeMode;

    public FileRange(long total) {
        if (total < 0) {
            throw new IllegalArgumentException("total: "+total);
        }
        this.start = 0;
        this.end = total - 1;
        this.total = total;
        this.rangeMode = false;
    }

    public FileRange(String rangeString, long total) { //xxx-xxx
        if (total < 0) {
            throw new IllegalArgumentException("total: "+total);
        }
        String[] strs = rangeString.split("-");
        if (strs.length != 1 && strs.length != 2) {
            throw new IllegalArgumentException("rangeString: "+rangeString);
        }
        if (strs.length == 1) { //xxx-
            start = Long.parseLong(strs[0]);
            if (start < 0) {
                throw new IllegalArgumentException("rangeString: "+rangeString);
            }
            end = total-1;
        } else {
            if (strs[0].length() == 0 && strs[1].length() == 0) {
                throw new IllegalArgumentException("rangeString: "+rangeString);
            }
            if (strs[0].length() == 0) { //-xxx
                Long length = Long.parseLong(strs[1]);
                if (length > total) {
                    throw new IllegalArgumentException("rangeString: "+rangeString);
                }
                end = total-1;
                start = end-length+1;
            } else { //xxx-xxx
                start = Long.parseLong(strs[0]);
                end = Long.parseLong(strs[1]);
                if (start < 0 || end > total || end < start) {
                    throw new IllegalArgumentException("rangeString: "+rangeString);
                }
                if (end == total) {
                    end--;
                }
            }
        }
        this.total = total;
        this.rangeMode = true;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getTotal() {
        return total;
    }

    public boolean isRangeMode() { return rangeMode; }
}
