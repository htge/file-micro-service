package com.htge.download.file.cache;

import java.nio.file.Path;

public interface ETagCache {
    void generateTree(Path path);
    String getETag(Path path);
    void setETag(Path path, String eTag);
    void removeETag(Path path);
}
