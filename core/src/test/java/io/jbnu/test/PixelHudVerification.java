package io.jbnu.test;

/** Pure display/layout checks; game mechanics stay covered by the existing integration suite. */
public final class PixelHudVerification {
    public static void main(String[] args) {
        for (int number = 1; number <= StageFactory.STAGE_COUNT; number++) {
            StageDefinition stage = StageFactory.createStage(number);
            check(PixelHudRenderer.semesterText(stage.getYear(), stage.getSemester()).equals(
                "YEAR " + ((number + 1) / 2) + " / SEMESTER " + ((number - 1) % 2 + 1)), "Live semester label");
            int total = stage.getGradeItems().size;
            check(PixelHudRenderer.itemsText(0, total).equals("0 / " + total), "Empty inventory");
            check(PixelHudRenderer.itemsText(total, total).equals(total + " / " + total), "Full inventory");
            check(PixelHudRenderer.scoreText(total * 5).equals(Integer.toString(total * 5)), "Item score only");
        }
        for (float width : new float[]{640, 800, 1280, 1920}) {
            PixelHudRenderer.Layout layout = new PixelHudRenderer.Layout(width, 600);
            check(Math.abs(layout.scale - .85f) < .001f || width == 640, "Uniform target scale / narrow fit");
            check(layout.left >= 14 && layout.left + layout.rowWidth <= width - 14, "Symmetric outer margins");
            check(Math.abs(layout.left - (width - layout.left - layout.rowWidth)) < .001, "Centered HUD group");
            float gap = PixelHudRenderer.Layout.GAP * layout.scale;
            check(Math.abs(layout.scoreX - layout.left - PixelHudRenderer.Layout.YEAR_WIDTH * layout.scale - gap) < .001, "Year/score gap");
            check(Math.abs(layout.itemsX - layout.scoreX - PixelHudRenderer.Layout.SCORE_WIDTH * layout.scale - gap) < .001, "Score/items gap");
            for (int maxLives : new int[]{5, 6, 12}) {
                int columns = layout.heartsPerRow(maxLives);
                for (int i = 0; i < maxLives; i++) {
                    float right = layout.heartsX(maxLives) + i % columns * 18 + 14;
                    check(right <= PixelHudRenderer.Layout.ITEMS_WIDTH - 10, "Extra-life hearts inside shared items/lives width");
                    check(18 + i / columns * 16 < layout.livesHeight(maxLives), "Wrapped hearts fit vertically");
                }
                check(layout.livesHeight(maxLives) * layout.scale < 28 || maxLives > 9, "Compact single-row lives panel");
            }
        }        System.out.println("Pixel HUD passed: eight semester/item labels, item-only score, margins, extra-life heart wrapping.");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
