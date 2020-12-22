package me.lucko.luckperms.common.util;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PaginatedTest {

    @Test
    void testSimple() {
        Paginated<String> paginated = new Paginated<>(ImmutableList.of("one", "two", "three", "four", "five"));
        assertEquals(3, paginated.getMaxPages(2));
        assertEquals(1, paginated.getMaxPages(5));
        assertEquals(1, paginated.getMaxPages(6));

        List<Paginated.Entry<String>> page1 = paginated.getPage(1, 2);
        assertEquals(2, page1.size());
        assertEquals("one", page1.get(0).value());
        assertEquals(1, page1.get(0).position());
        assertEquals("two", page1.get(1).value());
        assertEquals(2, page1.get(1).position());

        List<Paginated.Entry<String>> page2 = paginated.getPage(2, 2);
        assertEquals(2, page2.size());
        assertEquals("three", page2.get(0).value());
        assertEquals(3, page2.get(0).position());
        assertEquals("four", page2.get(1).value());
        assertEquals(4, page2.get(1).position());

        List<Paginated.Entry<String>> page3 = paginated.getPage(3, 2);
        assertEquals(1, page3.size());
        assertEquals("five", page3.get(0).value());
        assertEquals(5, page3.get(0).position());

        assertThrows(IllegalArgumentException.class, () -> paginated.getPage(4, 2));
        assertThrows(IllegalArgumentException.class, () -> paginated.getPage(0, 2));
        assertThrows(IllegalArgumentException.class, () -> paginated.getPage(-1, 2));
    }

}
