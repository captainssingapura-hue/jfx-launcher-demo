package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * What a grid's cells keep: found again by their cells, either side of nothing, along rows, columns and diagonals
 * (where a key's two halves put together alone would crowd), as many as are kept; kept again in place of what was.
 */
class CellMapTest {

    @Test
    void whatEachCellKeepsIsFoundByItsCellAndNothingElseIs() {
        final CellMap<String> map = new CellMap<>();
        final Map<Long, String> expected = new HashMap<>();
        for (long i = -40; i <= 40; i++) {
            for (long j = -40; j <= 40; j++) {
                if ((i + j) % 3 == 0) {
                    map.put(CellMap.key(i, j), i + "," + j);
                    expected.put(CellMap.key(i, j), i + "," + j);
                }
            }
        }
        assertEquals(expected.size(), map.size());
        for (long i = -41; i <= 41; i++) {
            for (long j = -41; j <= 41; j++) {
                assertEquals(expected.get(CellMap.key(i, j)), map.get(CellMap.key(i, j)), i + "," + j);
            }
        }
        final int[] seen = {0};
        map.forEach((k, v) -> {
            assertEquals(expected.get(k), v);
            seen[0]++;
        });
        assertEquals(expected.size(), seen[0]);
    }

    @Test
    void aCellKeepsWhatItIsLastGivenAndListsGrowAsTheyAreAddedTo() {
        final CellMap<String> map = new CellMap<>();
        map.put(CellMap.key(3, -7), "first");
        map.put(CellMap.key(3, -7), "again");
        assertEquals(1, map.size());
        assertEquals("again", map.get(CellMap.key(3, -7)));
        assertNull(map.get(CellMap.key(-7, 3)));

        final CellMap<CellMap.Ints> lists = new CellMap<>();
        final CellMap.Ints first = lists.computeIfAbsent(CellMap.key(0, 0), CellMap.Ints::new);
        for (int k = 0; k < 10; k++) {
            lists.computeIfAbsent(CellMap.key(0, 0), CellMap.Ints::new).add(k);
        }
        assertSame(first, lists.get(CellMap.key(0, 0)));
        assertEquals(9, first.last());
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, first.toArray());
    }
}
