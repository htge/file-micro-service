package com.htge.upload.controller.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public class FileItem {
    InputStream inputStream;
    Long size;
    Long currentPart;
    Long offset;

    public FileItem(InputStream inputStream, Long size, Long currentPart, Long offset) {
        this.inputStream = inputStream;
        this.size = size;
        this.currentPart = currentPart;
        this.offset = offset;
    }

    public FileItem() {

    }
}
