package co.loystar.loystarbusiness.utils;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Created by ordgen on 11/15/17.
 */

public class AlphaNumericInputFilter implements InputFilter {
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dStart, int dEnd) {

        // Only keep characters that are alphanumeric
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            }
        }

        // If all characters are valid, return null, otherwise only return the filtered characters
        boolean allCharactersValid = (builder.length() == end - start);
        return allCharactersValid ? null : builder.toString();
    }
}
