package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;
import java.util.Random;

/** Damage boundaries, independent polygon SAT oracle, and every authored spike at three frame rates. */
public final class SpikeCollisionVerification {
    public static void main(String[] args) {
        verifyBoundaries();
        verifyPolygonOracle();
        for (int fps : new int[] {30, 60, 120}) verifyStages(fps);
    }

    private static void verifyBoundaries() {
        TimedTrap fixed = TimedTrap.fixedSpikes(100, 60, 144, 72);
        SpikeLayout s = fixed.getSpikeLayout();
        near(s.getLeftX(0), 104, "First triangle left");
        near(s.getCenterX(0), 118, "First triangle center");
        near(s.getBottomY(), 76, "Base top");
        near(s.getWidth(), 28, "Triangle base width");
        near(s.getHeight(), 30.8f, "Visible rise");
        near(s.getTopY(), 106.8f, "Visible tip");
        check(s.overlaps(new Rectangle(117.9f, 106.7f, .2f, .2f)), "Actual tip penetration hits");
        check(!s.overlaps(new Rectangle(117.9f, s.getTopY(), .2f, 4)), "Zero-area touch above tip is safe");
        check(s.overlaps(new Rectangle(110.95f, 91.4f, .1f, .2f)), "Sloping side penetration hits");
        check(!s.overlaps(new Rectangle(110.8f, 91.4f, .1f, .2f)), "Outside sloping side is safe");
        check(!s.overlaps(new Rectangle(135, 76, 2, 1)), "Gap between triangle bases is safe");
        check(!s.overlaps(new Rectangle(120, 106.7f, 32, 44)), "Full player fits between tips near their tops");
        check(!s.overlaps(new Rectangle(100, 60, 144, 16)), "Base decorations alone are safe");
        check(!fixed.isDangerousTo(new Player(102, 107.8f)), "Old invisible upper damage band removed");
        check(!fixed.isDangerousTo(new Player(102, 32)), "Player head at base top does not hit decorations");
        check(!fixed.isDangerousTo(new Player(68, 80)) && !fixed.isDangerousTo(new Player(244, 80)),
            "Left/right outside artwork are safe");

        Player contact = new Player(102, 106.7f);
        TimedTrap timed = TimedTrap.cyclingSpikes(100, 60, 144, 72, 1, .5, 2, 0);
        check(!timed.isDangerousTo(contact), "SAFE tip is harmless");
        timed.update(1);
        check(timed.getState() == TrapState.WARNING && !timed.isDangerousTo(contact), "WARNING tip is harmless");
        timed.update(.5);
        check(timed.getState() == TrapState.ACTIVE && timed.isDangerousTo(contact), "ACTIVE visible tip damages");
        check(!timed.isDangerousTo(new Player(102, 107.8f)), "ACTIVE upper air remains safe");
        timed.update(2);
        check(timed.getState() == TrapState.SAFE && !timed.isDangerousTo(contact), "Cycle returns to safe");
        fixed.update(100);
        check(fixed.getState() == TrapState.ACTIVE && fixed.isDangerousTo(contact), "Fixed remains dangerous");
        TimedTrap legacy = new TimedTrap(100, 60, 48, 24, 3);
        check(legacy.getSpikeLayout() == null && legacy.isDangerousTo(new Player(110, 60)),
            "Legacy rectangle trap behavior unchanged");
    }

    private static void verifyPolygonOracle() {
        Random random = new Random(20261107L);
        for (int[] size : new int[][] {{24,24},{56,24},{72,28},{96,48},{144,72}}) {
            Rectangle bounds = new Rectangle(100, 60, size[0], size[1]);
            SpikeLayout layout = new SpikeLayout(bounds);
            for (int n = 0; n < 2000; n++) {
                Rectangle body = new Rectangle(70 + random.nextFloat() * (size[0] + 60),
                    30 + random.nextFloat() * (size[1] + 60), .1f + random.nextFloat() * 36,
                    .1f + random.nextFloat() * 50);
                boolean expected = false;
                for (int i = 0; i < SpikeLayout.COUNT; i++) expected |= polygonOverlap(body, new float[] {
                    layout.getLeftX(i), layout.getBottomY(), layout.getCenterX(i), layout.getTopY(),
                    layout.getLeftX(i) + layout.getWidth(), layout.getBottomY()});
                check(layout.overlaps(body) == expected, "Cross-section collision agrees with independent polygon SAT");
            }
        }
    }

    // Independent separating-axis test: rectangle X/Y and all three triangle edge normals.
    private static boolean polygonOverlap(Rectangle r, float[] triangle) {
        float[] rectangle = {r.x,r.y,r.x+r.width,r.y,r.x+r.width,r.y+r.height,r.x,r.y+r.height};
        for (int axis = 0; axis < 5; axis++) {
            double nx = axis == 0 ? 1 : 0, ny = axis == 1 ? 1 : 0;
            if (axis >= 2) {
                int a = (axis - 2) * 2, b = (a + 2) % 6;
                nx = triangle[b+1] - triangle[a+1]; ny = triangle[a] - triangle[b];
            }
            double rmin=Double.POSITIVE_INFINITY,rmax=Double.NEGATIVE_INFINITY;
            double tmin=Double.POSITIVE_INFINITY,tmax=Double.NEGATIVE_INFINITY;
            for (int i=0;i<8;i+=2) { double p=rectangle[i]*nx+rectangle[i+1]*ny; rmin=Math.min(rmin,p);rmax=Math.max(rmax,p); }
            for (int i=0;i<6;i+=2) { double p=triangle[i]*nx+triangle[i+1]*ny;tmin=Math.min(tmin,p);tmax=Math.max(tmax,p); }
            if (rmax <= tmin || tmax <= rmin) return false;
        }
        return true;
    }

    private static void verifyStages(int fps) {
        int count = 0;
        for (int stage=1;stage<=8;stage++) for (TimedTrap trap:StageFactory.createStage(stage).getTimedTraps()) {
            if (!trap.usesSpikeDesign()) continue;
            Rectangle authored = trap.getBounds();
            SpikeLayout layout = trap.getSpikeLayout();
            int ticks=0;
            while (trap.getState()!=TrapState.ACTIVE) { trap.update(1.0/fps);check(++ticks<fps*10,"ACTIVE reachable"); }
            float center=layout.getCenterX(0);
            check(!trap.isDangerousTo(new Player(center-16,layout.getTopY()+1)), "Above artwork safe on every stage");
            check(trap.isDangerousTo(new Player(center-16,layout.getTopY()-.05f)), "Visible tip hit on every stage");
            check(!trap.isDangerousTo(new Player(center-16,layout.getBottomY()-44)), "Decorations harmless on every stage");
            float gapCenter=(layout.getCenterX(0)+layout.getCenterX(1))*.5f;
            float gap=layout.getCellWidth()-layout.getWidth();
            check(!layout.overlaps(new Rectangle(gapCenter-gap*.25f,layout.getBottomY(),gap*.5f,.1f)), "Individual base gaps safe");
            check(trap.getBounds().equals(authored), "Authored bounds unchanged");
            trap.reset();check(trap.getSpikeLayout()==layout,"Reset reuses immutable geometry");
            near(trap.copyInitial().getSpikeLayout().getTopY(),layout.getTopY(),"Copy shares geometry rules");
            count++;
        }
        check(count==27,"All 27 authored spikes covered");
        System.out.println(fps+" fps: spike collision passed: 27 traps, no invisible upper hits, visible tips/sides, base/gap safety, unchanged states; 10000 SAT cases passed.");
    }
    private static void near(float a,float b,String message) { check(Math.abs(a-b)<.001f,message); }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
