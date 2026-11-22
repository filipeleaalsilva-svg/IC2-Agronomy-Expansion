package br.cropmanager;

import net.minecraft.nbt.NBTTagCompound;

/** Tank rules and a persistent stop latch; independent of any monitor being loaded. */
public final class AutomationRules {
    public int mode, waterPercent = -1, weedPercent = 20;
    public int fertilizerMinimum = -1, energyPercent = -1;
    public boolean stopOnOutputFull;
    public boolean stopped;
    private boolean weedLatched;
    private static boolean below(int amount,int capacity,int threshold) {
        return threshold == 0 ? amount == 0 : (long)amount * 100 < (long)capacity * threshold;
    }
    boolean matches(int water,int waterCapacity,int weed,int weedCapacity) {
        return matches(water,waterCapacity,weed,weedCapacity,Integer.MAX_VALUE,100,100,false);
    }
    boolean matches(int water,int waterCapacity,int weed,int weedCapacity,int fertilizer,double energy,int energyCapacity,boolean outputFull) {
        if(mode == 0)return false;
        int selected=0, hits=0;
        if(waterPercent>=0){selected++;if(below(water,waterCapacity,waterPercent))hits++;}
        if(weedPercent>=0){selected++;if(below(weed,weedCapacity,weedPercent))hits++;}
        if(fertilizerMinimum>=0){selected++;if(fertilizerMinimum==0?fertilizer==0:fertilizer<fertilizerMinimum)hits++;}
        if(energyPercent>=0){selected++;if(below((int)Math.floor(energy),energyCapacity,energyPercent))hits++;}
        if(stopOnOutputFull){selected++;if(outputFull)hits++;}
        return selected>0 && (mode==1 ? hits>0 : hits==selected);
    }
    public boolean update(int water,int waterCapacity,int weed,int weedCapacity) {
        return update(water,waterCapacity,weed,weedCapacity,Integer.MAX_VALUE,100,100,false);
    }
    public boolean update(int water,int waterCapacity,int weed,int weedCapacity,int fertilizer,double energy,int energyCapacity,boolean outputFull) {
        boolean previous=stopped;
        boolean match=matches(water,waterCapacity,weed,weedCapacity,fertilizer,energy,energyCapacity,outputFull);
        if(mode==0){stopped=false;weedLatched=false;}
        else if(match){
            if(!stopped)weedLatched=weedPercent>=0 && below(weed,weedCapacity,weedPercent);
            stopped=true;
        } else if(stopped && (!weedLatched || weedCapacity>0 && weed>=weedCapacity)){stopped=false;weedLatched=false;}
        return previous!=stopped;
    }
    private static int step(int value,int direction) {
        if(direction>0)return value<0?0:value>=100?-1:Math.min(100,value+5);
        return value<0?100:value==0?-1:Math.max(0,value-5);
    }
    public boolean control(int action) {
        if(action==220)mode=(mode+1)%3;
        else if(action==221)waterPercent=step(waterPercent,-1);
        else if(action==222)waterPercent=step(waterPercent,1);
        else if(action==223)weedPercent=step(weedPercent,-1);
        else if(action==224)weedPercent=step(weedPercent,1);
        else if(action==225){mode=2;waterPercent=0;weedPercent=0;fertilizerMinimum=-1;energyPercent=-1;stopOnOutputFull=false;}
        else if(action==226)fertilizerMinimum=fertilizerMinimum<0?0:fertilizerMinimum>=192?-1:fertilizerMinimum+16;
        else if(action==227)fertilizerMinimum=fertilizerMinimum<0?192:fertilizerMinimum==0?-1:Math.max(0,fertilizerMinimum-16);
        else if(action==228)energyPercent=step(energyPercent,-1);
        else if(action==229)energyPercent=step(energyPercent,1);
        else if(action==230)stopOnOutputFull=!stopOnOutputFull;
        else return false;
        return true;
    }
    public NBTTagCompound write() {NBTTagCompound t=new NBTTagCompound();t.setInteger("Mode",mode);t.setInteger("Water",waterPercent);t.setInteger("Weed",weedPercent);t.setInteger("Fertilizer",fertilizerMinimum);t.setInteger("Energy",energyPercent);t.setBoolean("OutputFull",stopOnOutputFull);t.setBoolean("Stopped",stopped);t.setBoolean("WeedLatched",weedLatched);return t;}
    public void read(NBTTagCompound t) {
        mode=Math.max(0,Math.min(2,t.getInteger("Mode")));
        waterPercent=t.hasKey("Water")?Math.max(-1,Math.min(100,t.getInteger("Water"))):-1;
        weedPercent=t.hasKey("Weed")?Math.max(-1,Math.min(100,t.getInteger("Weed"))):20;
        fertilizerMinimum=t.hasKey("Fertilizer")?Math.max(-1,Math.min(192,t.getInteger("Fertilizer"))):-1;
        energyPercent=t.hasKey("Energy")?Math.max(-1,Math.min(100,t.getInteger("Energy"))):-1;
        stopOnOutputFull=t.getBoolean("OutputFull");
        stopped=mode!=0 && t.getBoolean("Stopped");
        weedLatched=stopped && (t.hasKey("WeedLatched")?t.getBoolean("WeedLatched"):weedPercent>=0);
    }
}
