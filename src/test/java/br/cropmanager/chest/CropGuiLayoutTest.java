package br.cropmanager.chest;

import br.cropmanager.chest.client.CropGuiLayout;
import br.cropmanager.chest.client.CropGuiLayout.Region;
import org.junit.Test;
import static org.junit.Assert.*;

public class CropGuiLayoutTest {
    @Test public void displayRowsFitAndNeverOverlapSlotsOrEachOther() {
        Region[] rows = new Region[7];
        rows[0] = CropGuiLayout.HEADER; rows[1] = CropGuiLayout.NAME;
        for (int i = 0; i < 3; i++) rows[2 + i] = new Region(CropGuiLayout.TEXT_LEFT, CropGuiLayout.STAT_Y[i], CropGuiLayout.TEXT_WIDTH, 9);
        rows[5] = CropGuiLayout.PRODUCES; rows[6] = CropGuiLayout.PRODUCTS;
        for (int i = 0; i < rows.length; i++) {
            assertTrue(CropGuiLayout.PANEL.contains(rows[i]));
            assertFalse(rows[i].intersects(CropGuiLayout.GRID));
            assertFalse(rows[i].intersects(CropGuiLayout.PLAYER));
            for (int j = 0; j < i; j++) assertFalse(rows[i].intersects(rows[j]));
        }
        assertTrue(CropGuiLayout.NAME.y - CropGuiLayout.HEADER.y - CropGuiLayout.HEADER.height >= 2);
        assertTrue(CropGuiLayout.STAT_Y[0] - CropGuiLayout.NAME.y - CropGuiLayout.NAME.height >= 1);
        assertFalse(CropGuiLayout.PANEL.intersects(CropGuiLayout.GRID));
        assertFalse(CropGuiLayout.PANEL.intersects(CropGuiLayout.PLAYER));
    }
    @Test public void productIconsStayCenteredInsideTheDisplay() {
        for (int count = 1; count <= CropGuiLayout.PRODUCTS_PER_ROW; count++) {
            int x = CropGuiLayout.productStart(count);
            assertTrue(CropGuiLayout.PRODUCTS.contains(new Region(x, CropGuiLayout.PRODUCT_Y, count * 16, 16)));
            assertEquals(CropGuiLayout.TEXT_LEFT + CropGuiLayout.TEXT_WIDTH / 2, x + count * 8);
        }
    }
    @Test public void iconControlsFitTheSearchHeaderAndPageButtonsStayBelowGrid() {
        Region[] toolbarControls = {
                new Region(7, 108, 86, 14),
                new Region(99, 107, 14, 16),
                new Region(115, 107, 14, 16),
                new Region(131, 107, 16, 16),
                new Region(149, 107, 18, 16),
                new Region(169, 107, 18, 16)
        };
        Region window = new Region(0, 0, CropGuiLayout.WIDTH, CropGuiLayout.HEIGHT);
        for (Region control : toolbarControls) {
            assertTrue(window.contains(control));
            assertFalse(control.intersects(CropGuiLayout.PANEL));
            assertFalse(control.intersects(CropGuiLayout.GRID));
            assertFalse(control.intersects(CropGuiLayout.PLAYER));
        }
        assertTrue(CropGuiLayout.GRID.y + CropGuiLayout.GRID.height < 133);
    }
    @Test public void fiveByFiveGridFitsTheWindowAndStaysClearOfAnalysisAndPlayerSlots() {
        assertEquals(25, br.cropmanager.chest.inventory.SeedInventory.PAGE_SIZE);
        assertEquals(5, br.cropmanager.chest.inventory.SeedInventory.COLUMNS);
        assertTrue(new Region(0, 0, CropGuiLayout.WIDTH, CropGuiLayout.HEIGHT).contains(CropGuiLayout.GRID));
        assertTrue(new Region(0, 0, CropGuiLayout.WIDTH, CropGuiLayout.HEIGHT).contains(CropGuiLayout.PLAYER));
        assertFalse(CropGuiLayout.PANEL.intersects(CropGuiLayout.GRID));
        assertFalse(CropGuiLayout.PANEL.intersects(CropGuiLayout.PLAYER));
        assertFalse(CropGuiLayout.GRID.intersects(CropGuiLayout.PLAYER));
    }
    @Test public void minimumMinecraftGuiKeepsTabsDrawerAndButtonsOnScreen() {
        int width = 320, height = 240, left = CropGuiLayout.guiLeft(width, true), top = (height - CropGuiLayout.HEIGHT) / 2;
        Region screen = new Region(0, 0, width, height);
        assertTrue(screen.contains(new Region(left, top, CropGuiLayout.WIDTH, CropGuiLayout.HEIGHT)));
        Region drawer = CropGuiLayout.drawer(width, left, top);
        assertTrue(screen.contains(drawer));
        for (int tab = 0; tab < 3; tab++) {
            Region button = CropGuiLayout.tab(left, top, tab);
            assertTrue(screen.contains(button));
            assertFalse(drawer.intersects(button));
            assertFalse(button.contains(button.x - 1, button.y));
            assertFalse(button.contains(button.x + button.width, button.y));
        }
        assertTrue(drawer.contains(new Region(drawer.x + 8, drawer.y + 29, 78, 16)));
        assertTrue(drawer.contains(new Region(drawer.x + 8, drawer.y + 54, 78, 16)));
        // The narrow drawer deliberately covers slots; this region is the mouse/key guard.
        assertTrue(drawer.contains(left + 150, top + 50));
        assertFalse(drawer.contains(left + 60, top + 50));
    }
    @Test public void wideGuiOpensDrawerOutsideInventoryWithoutOverlappingIt() {
        int width = 854, left = CropGuiLayout.guiLeft(width, true), top = 40;
        Region drawer = CropGuiLayout.drawer(width, left, top);
        assertFalse(drawer.intersects(new Region(left, top, CropGuiLayout.WIDTH, CropGuiLayout.HEIGHT)));
        assertTrue(new Region(0, 0, width, 480).contains(drawer));
    }
}
