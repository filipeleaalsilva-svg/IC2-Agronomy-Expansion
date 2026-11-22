package br.cropmanager;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;
public class AutomationRulesTest {
 @Test public void weedStopLatchesUntilCompletelyFull(){AutomationRules r=new AutomationRules();r.mode=1;r.waterPercent=-1;r.weedPercent=20;r.update(5000,20000,3999,20000);assertTrue(r.stopped);r.update(5000,20000,19999,20000);assertTrue(r.stopped);r.update(5000,20000,20000,20000);assertFalse(r.stopped);}
 @Test public void exactThresholdDoesNotStop(){AutomationRules r=new AutomationRules();r.mode=1;r.update(0,20000,4000,20000);assertFalse(r.stopped);r.update(0,20000,3999,20000);assertTrue(r.stopped);}
 @Test public void allEmptyRequiresBothAndKeepsStopUntilWeedFull(){AutomationRules r=new AutomationRules();r.control(225);r.update(1,20000,0,20000);assertFalse(r.stopped);r.update(0,20000,0,20000);assertTrue(r.stopped);r.update(1,20000,10000,20000);assertTrue(r.stopped);r.update(1,20000,20000,20000);assertFalse(r.stopped);}
 @Test public void originalConditionMustAlsoClear(){AutomationRules r=new AutomationRules();r.mode=1;r.waterPercent=20;r.update(0,20000,20000,20000);assertTrue(r.stopped);r.update(3999,20000,20000,20000);assertTrue(r.stopped);r.update(4000,20000,20000,20000);assertFalse(r.stopped);}
 @Test public void settingsAndLatchPersistAndDisabledDoesNotTrigger(){AutomationRules r=new AutomationRules();r.mode=1;r.stopped=true;NBTTagCompound tag=r.write();AutomationRules copy=new AutomationRules();copy.read(tag);assertTrue(copy.stopped);copy.read(new NBTTagCompound());copy.update(0,20000,0,20000);assertFalse(copy.stopped);assertFalse(copy.control(999));copy.mode=2;copy.waterPercent=-1;copy.weedPercent=-1;assertFalse(copy.matches(0,20000,0,20000));}
}
