
package com.wttech.aem.contentor.core.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class SearchUtils {

	private static final String WORD_DELIMITER = " ";

	private static final int WORD_MIN_LENGTH = 1;

	private SearchUtils() {
		// intentionally empty
	}

	public static boolean containsWord(final String subject, List<String> searches) {
		return containsWord(subject, searches, WORD_MIN_LENGTH);
	}

	public static boolean containsWord(final String subject, List<String> searches, int minWordLength) {
		for (String search : searches) {
			if (containsWord(subject, search, minWordLength)) {
				return true;
			}
		}

		return false;
	}

	public static boolean containsWord(final String subject, final String search) {
		return containsWord(subject, search, WORD_MIN_LENGTH);
	}

	public static boolean containsWord(final String subject, final String search, int minWordLength) {
		if (FilenameUtils.wildcardMatch(search, subject, IOCase.INSENSITIVE)) {
			return true;
		}

		for (String words : splitWords(subject)) {
			for (String textWord : splitWords(search)) {
				final String p = StringUtils.trimToEmpty(words);
				final String t = StringUtils.trimToEmpty(textWord);

				if ((!p.isEmpty() && !t.isEmpty()) && (t.length() >= minWordLength) && (p.length()
						>= minWordLength) && (p.contains(t) || t.contains(p))) {
					return true;
				}
			}
		}

		return false;
	}

	private static String[] splitWords(String phrase) {
		return StringUtils.trimToEmpty(phrase).toLowerCase().split(WORD_DELIMITER);
	}
}
