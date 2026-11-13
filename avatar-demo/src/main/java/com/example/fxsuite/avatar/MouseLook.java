package com.example.fxsuite.avatar;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

/**
 * Mouse look without a button held, as games do it: while the mouse is captured, moving it
 * left and right turns and up and down tilts, however far it goes. Clicks are left alone, for
 * actions.
 *
 * <p>JavaFX has no pointer lock, so capturing hides the cursor, and whenever it strays too far
 * from the window's centre a {@link Robot} puts it back. Turning is measured from one mouse
 * event to the next, so where the cursor sits does not matter, and the event after each jump
 * back only resets the reference, never counting as movement.
 *
 * <p>The mouse is captured only while the window has the focus, so switching away frees it;
 * {@link #toggle} frees it or takes it back.
 */
final class MouseLook {

    /** Degrees of turn or tilt per pixel the mouse moves. */
    static final double SENSITIVITY = 0.15;
    /** How far, in pixels, the hidden cursor may wander from the centre before it is put back. */
    private static final double LEASH = 120;

    private final Stage stage;
    private final Scene scene;
    private final Robot robot = new Robot();
    private boolean wanted;
    /** Held free for a while, whatever is wanted: while a menu is open over the window. */
    private boolean held;
    private boolean captured;
    private double lastX = Double.NaN;
    private double lastY;
    private double turn;
    private double tilt;

    MouseLook(final Stage stage, final Scene scene, final boolean wanted) {
        this.stage = stage;
        this.scene = scene;
        this.wanted = wanted;
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, this::moved);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::moved);
        stage.focusedProperty().addListener(focus);
        update();
    }

    /** Following the window's focus: captured only while the window has it. */
    private final javafx.beans.value.ChangeListener<Boolean> focus = (seen, was, is) -> update();

    /** Lets the mouse go for good, and stops following the window: the game it was for is over. */
    void close() {
        stage.focusedProperty().removeListener(focus);
        wanted = false;
        update();
    }

    boolean captured() {
        return captured;
    }

    /** Frees the mouse, or takes it back. */
    void toggle() {
        wanted = !wanted;
        update();
    }

    /** Holds the mouse free while {@code free} (a menu is open), then goes back to as it was. */
    void holdFree(final boolean free) {
        held = free;
        update();
    }

    /** Degrees to turn right since last asked. */
    double takeTurn() {
        final double t = turn;
        turn = 0;
        return t;
    }

    /** Degrees to tilt up since last asked. */
    double takeTilt() {
        final double t = tilt;
        tilt = 0;
        return t;
    }

    private void update() {
        final boolean should = wanted && !held && stage.isFocused();
        if (should == captured) {
            return;
        }
        captured = should;
        scene.setCursor(should ? Cursor.NONE : Cursor.DEFAULT);
        if (should) {
            recentre();
        }
    }

    private void moved(final MouseEvent e) {
        if (!captured) {
            return;
        }
        final double x = e.getScreenX();
        final double y = e.getScreenY();
        if (!Double.isNaN(lastX)) {
            turn += (x - lastX) * SENSITIVITY;
            tilt -= (y - lastY) * SENSITIVITY;
        }
        lastX = x;
        lastY = y;
        if (Math.hypot(x - centreX(), y - centreY()) > LEASH) {
            recentre();
        }
    }

    /** Puts the cursor back in the middle; the next event only sets the reference. */
    private void recentre() {
        robot.mouseMove(centreX(), centreY());
        lastX = Double.NaN;
    }

    private double centreX() {
        return stage.getX() + stage.getWidth() / 2;
    }

    private double centreY() {
        return stage.getY() + stage.getHeight() / 2;
    }
}
