package com.gravypod.SleepServer;

import java.util.regex.Pattern;

public class Constants {
	
	/**
	 * Tag to find text within the file
	 */
	public static final Pattern TAG_REGEX = Pattern.compile("<\\?sleep(.*?)\\?>");
}
