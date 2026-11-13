package com.example.fxsuite.avatar.sound;

import java.util.List;

/**
 * A vehicle an {@link Engine} drives, as far as its sound needs: how heavy it is, how much the
 * engine pulls, its gears, its wheels, and how hard the air pushes back. Enough to drive it
 * ({@link Driving}) and hear the engine do it: pulling away, up through the gears, lifting off,
 * braking down through them.
 *
 * @param name     what it is
 * @param engine   what drives it
 * @param mass     how heavy, rider or driver aboard, in kilograms
 * @param torque   the engine's most torque, in newton metres, at the best of its revs
 * @param gears    the gearbox's ratios, first to top; none for a {@link #cvt} one
 * @param last     the final drive: chain or axle, and any primary drive, together
 * @param wheel    the driven wheel's radius, in metres
 * @param drag     the air's drag: the drag coefficient times the frontal area, in square metres
 * @param grip     the hardest it can speed up, in g, before its tyres spin or, a bike, its front wheel lifts
 * @param shift    how long a change of gear takes, the clutch out and the throttle off, in seconds
 * @param cvt      for a CVT (a scooter's): the revs it holds at full throttle as it gathers speed;
 *                 0 for a gearbox
 * @param lowest   for a CVT, its lowest ratio and its highest, all told (engine turns to wheel turns)
 * @param highest
 */
public record Vehicle(String name, Engine engine, double mass, double torque, double[] gears, double last,
                      double wheel, double drag, double grip, double shift, double cvt, double lowest, double highest) {

    public Vehicle {
        if (cvt > 0 ? lowest <= highest || highest <= 0 : gears.length == 0) {
            throw new IllegalArgumentException(name + ": gears, or a CVT's range");
        }
        gears = gears.clone();
    }

    public static final Vehicle HATCHBACK = new Vehicle("Hatchback", Engine.I4, 1150, 160,
            new double[] {3.45, 1.95, 1.30, 0.97, 0.78}, 4.1, 0.30, 0.65, 1.0, 0.35, 0, 0, 0);
    public static final Vehicle SALOON = new Vehicle("Saloon", Engine.V6, 1550, 330,
            new double[] {3.9, 2.3, 1.5, 1.15, 0.92, 0.75}, 3.6, 0.33, 0.70, 1.0, 0.3, 0, 0, 0);
    public static final Vehicle MUSCLE = new Vehicle("Muscle car", Engine.V8, 1700, 560,
            new double[] {2.97, 2.07, 1.43, 1.00, 0.84, 0.56}, 3.73, 0.34, 0.75, 0.95, 0.35, 0, 0, 0);
    public static final Vehicle SUPERCAR = new Vehicle("Supercar", Engine.FLAT_V8, 1450, 650,
            new double[] {3.1, 2.2, 1.7, 1.35, 1.1, 0.9, 0.75}, 3.9, 0.34, 0.60, 1.25, 0.1, 0, 0, 0);
    public static final Vehicle SCOOTER_50 = new Vehicle("Scooter, 50 cc", Engine.SCOOTER_2T, 170, 4.5,
            new double[0], 1, 0.20, 0.55, 0.6, 0, 7800, 24, 8.5);
    public static final Vehicle SCOOTER_125 = new Vehicle("Scooter, 125 cc", Engine.SCOOTER, 195, 11,
            new double[0], 1, 0.22, 0.60, 0.6, 0, 7200, 20, 7);
    public static final Vehicle DUAL_SPORT = new Vehicle("Dual-sport bike", Engine.THUMPER, 245, 55,
            new double[] {2.5, 1.7, 1.3, 1.05, 0.88}, 5.8, 0.31, 0.60, 0.9, 0.15, 0, 0, 0);
    public static final Vehicle CRUISER = new Vehicle("Cruiser", Engine.V_TWIN, 395, 140,
            new double[] {3.0, 2.0, 1.5, 1.2, 1.0, 0.85}, 3.2, 0.33, 0.70, 0.8, 0.25, 0, 0, 0);
    public static final Vehicle SPORT_BIKE = new Vehicle("Sport bike", Engine.L_TWIN, 275, 120,
            new double[] {2.6, 1.9, 1.55, 1.3, 1.15, 1.04}, 5.0, 0.31, 0.45, 1.0, 0.08, 0, 0, 0);
    public static final Vehicle CLASSIC_BIKE = new Vehicle("Classic bike", Engine.PARALLEL_TWIN, 295, 60,
            new double[] {2.6, 1.8, 1.35, 1.1, 0.95}, 5.2, 0.32, 0.60, 0.85, 0.2, 0, 0, 0);

    /** One for each engine. */
    public static final List<Vehicle> ALL = List.of(HATCHBACK, SALOON, MUSCLE, SUPERCAR,
            SCOOTER_50, SCOOTER_125, DUAL_SPORT, CRUISER, SPORT_BIKE, CLASSIC_BIKE);

    /** The vehicle {@code engine} drives. */
    public static Vehicle of(final Engine engine) {
        return ALL.stream().filter(v -> v.engine.equals(engine)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("nothing drives " + engine));
    }

    @Override
    public double[] gears() {
        return gears.clone();
    }

    /** How many gears it has; none, a CVT. */
    public int count() {
        return gears.length;
    }

    /** All told, how many times the engine turns for each turn of the wheel, in gear {@code gear} (1 the first). */
    public double ratio(final int gear) {
        return gears[gear - 1] * last;
    }

    /**
     * How much the engine pulls at {@code rpm}, flat out, in newton metres: less low down, its
     * most a little past the middle of its range, falling away toward the red line.
     */
    public double torqueAt(final double rpm) {
        final double share = Math.clamp((rpm - engine.idle()) / (engine.redline() - engine.idle()), 0, 1);
        final double shape = share < 0.65 ? 0.65 + 0.35 * Math.sin(Math.PI / 2 * share / 0.65)
                : 1 - 0.15 * (share - 0.65) / 0.35;
        return torque * shape;
    }

    @Override
    public String toString() {
        return name;
    }
}
