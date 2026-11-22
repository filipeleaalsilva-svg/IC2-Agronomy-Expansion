package br.cropmanager;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Test;
import static org.junit.Assert.*;

public class CropSurveyTest {
    @Test public void averagesAndRangesUseOnlyRealNonWeedSamples() {
        CropSurvey survey = new CropSurvey();
        survey.add("ic2", "wheat", true, false, new int[] {4,8,12,15});
        survey.add("ic2", "wheat", true, false, new int[] {8,4,16,11});
        survey.add("ic2", "weed", true, true, new int[] {100,100,100,100});
        NBTTagCompound report = survey.write();
        assertEquals(2, report.getInteger("Count"));
        assertArrayEquals(new int[] {12,12,28,26}, report.getIntArray("Sum"));
        assertArrayEquals(new int[] {4,4,12,11}, report.getIntArray("Min"));
        assertArrayEquals(new int[] {8,8,16,15}, report.getIntArray("Max"));
        NBTTagCompound wheat = report.getTagList("Species",10).getCompoundTagAt(0);
        assertEquals("wheat",wheat.getString("Id"));
        assertArrayEquals(report.getIntArray("Sum"),wheat.getIntArray("Sum"));
    }

    @Test public void unidentifiedPlantsShareOneAnonymousGroupWithoutLeakingSpecies() {
        CropSurvey survey = new CropSurvey();
        survey.add("secret", "gold", false, false, new int[] {1,2,3,4});
        survey.add("secret", "diamond", false, false, new int[] {4,3,2,1});
        NBTTagList rows = survey.write().getTagList("Species",10);
        assertEquals(1,rows.tagCount());
        assertEquals("",rows.getCompoundTagAt(0).getString("Owner"));
        assertEquals("",rows.getCompoundTagAt(0).getString("Id"));
        assertEquals(2,rows.getCompoundTagAt(0).getInteger("Count"));
        assertFalse(survey.write().toString().contains("secret"));
    }

    @Test public void deterministicOrderUsesQuantityThenSpeciesIdentity() {
        CropSurvey survey = new CropSurvey();
        survey.add("z", "b", true, false, new int[4]);
        survey.add("z", "a", true, false, new int[4]);
        survey.add("a", "c", true, false, new int[4]);
        survey.add("z", "b", true, false, new int[4]);
        NBTTagList rows=survey.write().getTagList("Species",10);
        assertEquals("b", rows.getCompoundTagAt(0).getString("Id"));
        assertEquals("c", rows.getCompoundTagAt(1).getString("Id"));
        assertEquals("a", rows.getCompoundTagAt(2).getString("Id"));
    }

    @Test public void publishedArraysRemainImmutableAsNextSamplesArrive() {
        CropSurvey survey = new CropSurvey();
        survey.add("ic2", "wheat", true, false, new int[] {1,2,3,4});
        NBTTagCompound first = survey.write();
        survey.add("ic2", "wheat", true, false, new int[] {5,6,7,8});
        assertEquals(1,first.getInteger("Count"));
        assertArrayEquals(new int[] {1,2,3,4},first.getIntArray("Sum"));
        assertArrayEquals(new int[] {1,2,3,4},first.getTagList("Species",10).getCompoundTagAt(0).getIntArray("Sum"));
        assertEquals(2,survey.write().getInteger("Count"));
    }

    @Test public void emptySurveyHasNoImaginaryEnvironmentOrSpecies() {
        NBTTagCompound report = new CropSurvey().write();
        assertEquals(0,report.getInteger("Count"));
        assertEquals(0,report.getTagList("Species",10).tagCount());
        assertArrayEquals(new int[4],report.getIntArray("Sum"));
    }
}
