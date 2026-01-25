package io.github.fastmcp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageCursorTest {
    
    @Test
    void testFirstCursor_DefaultValues() {
        PageCursor cursor = PageCursor.first();
        
        assertEquals(0, cursor.offset());
        assertEquals(50, cursor.limit());
    }
    
    @Test
    void testEncodeDecode_RoundTrip() {
        PageCursor original = PageCursor.of(100, 25);
        String encoded = original.encode();
        PageCursor decoded = PageCursor.parse(encoded);
        
        assertEquals(original.offset(), decoded.offset());
        assertEquals(original.limit(), decoded.limit());
    }
    
    @Test
    void testNextCursor_IncrementsOffset() {
        PageCursor cursor = PageCursor.of(10, 5);
        PageCursor next = cursor.next();
        
        assertEquals(15, next.offset());
        assertEquals(5, next.limit());
    }
    
    @Test
    void testParseNull_ReturnsFirst() {
        PageCursor cursor = PageCursor.parse(null);
        
        assertEquals(0, cursor.offset());
        assertEquals(50, cursor.limit());
    }
    
    @Test
    void testParseEmpty_ReturnsFirst() {
        PageCursor cursor = PageCursor.parse("");
        
        assertEquals(0, cursor.offset());
        assertEquals(50, cursor.limit());
    }
    
    @Test
    void testParseInvalid_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            PageCursor.parse("invalid-cursor-string");
        });
    }
}
