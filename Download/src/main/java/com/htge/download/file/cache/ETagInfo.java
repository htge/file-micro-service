package com.htge.download.file.cache;

import java.io.Serializable;

class ETagInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ETag;
    private long LastModified;
    private long size;

    void setETag(String ETag) {
        this.ETag = ETag;
    }

    String getETag() {
        return ETag;
    }

    void setLastModified(long lastModified) {
        LastModified = lastModified;
    }

    long getLastModified() {
        return LastModified;
    }

    void setSize(long size) {
        this.size = size;
    }

    long getSize() {
        return size;
    }
}
