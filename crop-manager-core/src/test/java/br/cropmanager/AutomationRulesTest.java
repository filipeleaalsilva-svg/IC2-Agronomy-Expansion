package br.cropmanager;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutomationRulesTest {
    @Test
    public void mode0SempreDesligaEAgendaDestrave() {
        AutomationRules rules = new AutomationRules();
        rules.mode = 1;
        rules.waterPercent = 50;
        assertTrue(rules.update(0, 20000, 20000, 20000));
        assertTrue(rules.stopped);

        rules.mode = 0;
        assertTrue(rules.update(0, 20000, 20000, 20000));
        assertFalse(rules.stopped);
    }

    @Test
    public void mode1DisparaEmQualquerCondicao() {
        AutomationRules rules = new AutomationRules();
        rules.mode = 1;
        rules.waterPercent = 20;
        rules.weedPercent = 20;
        assertTrue(rules.update(1000, 20000, 20000, 20000));
        assertTrue(rules.stopped);
    }

    @Test
    public void mode2ExigeTodasCondicoes() {
        AutomationRules rules = new AutomationRules();
        rules.mode = 2;
        rules.waterPercent = 20;
        rules.weedPercent = 20;
        assertFalse(rules.update(1000, 20000, 20000, 20000));
        assertFalse(rules.stopped);
        assertTrue(rules.update(1000, 20000, 1000, 20000));
        assertTrue(rules.stopped);
    }

    @Test
    public void weedLatchedExigeTanque100PorCentoParaRetomar() {
        AutomationRules rules = new AutomationRules();
        rules.mode = 1;
        rules.weedPercent = 20;
        assertTrue(rules.update(20000, 20000, 1000, 20000));
        assertTrue(rules.stopped);

        assertFalse(rules.update(20000, 20000, 19999, 20000));
        assertTrue(rules.stopped);

        assertTrue(rules.update(20000, 20000, 20000, 20000));
        assertFalse(rules.stopped);
    }

    @Test
    public void roundTripNbtPreservaTravaWeedLatched() {
        AutomationRules rules = new AutomationRules();
        rules.mode = 1;
        rules.weedPercent = 20;
        rules.update(20000, 20000, 1000, 20000);
        NBTTagCompound tag = rules.write();

        AutomationRules loaded = new AutomationRules();
        loaded.read(tag);
        assertTrue(loaded.stopped);
        assertFalse(loaded.update(20000, 20000, 15000, 20000));
        assertTrue(loaded.stopped);
        assertTrue(loaded.update(20000, 20000, 20000, 20000));
        assertFalse(loaded.stopped);
    }
}
