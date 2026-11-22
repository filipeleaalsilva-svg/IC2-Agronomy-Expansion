package br.cropmanager;

import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

public class UiNumbersTest {
    @Test public void abbreviatesWithoutHidingRounding() {
        assertEquals("20k", UiNumbers.compact(20000, Locale.US));
        assertEquals("~12.6k", UiNumbers.compact(12599, Locale.US));
        assertEquals("~20k", UiNumbers.compact(19999, Locale.US));
        assertEquals("0", UiNumbers.compact(0, Locale.US));
        assertEquals("999", UiNumbers.compact(999, Locale.US));
        assertEquals("1k", UiNumbers.compact(1000, Locale.US));
    }
    @Test public void localizesAndRetainsExactTooltip() {
        Locale pt = new Locale("pt", "BR");
        assertEquals("~12,6k", UiNumbers.compact(12599, pt));
        assertEquals("12.599", UiNumbers.exact(12599, pt));
        assertEquals("12,599", UiNumbers.exact(12599, Locale.US));
        assertEquals("~3,3", UiNumbers.compact(10.0 / 3, pt));
    }
}
