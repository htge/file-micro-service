package com.htge.download.file.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.util.Comparator;

public class
FileDateComparator implements Comparator<File> {
	private String getFileLastModifiedStr(File file) {
		long lastModified = file.lastModified();
		Date date = new Date(lastModified);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(date);
	}

	public int compare(File file1, File file2) {
		String day0 = getFileLastModifiedStr(file1);
		String day1 = getFileLastModifiedStr(file2);
		return day0.compareTo(day1);
	}
}
