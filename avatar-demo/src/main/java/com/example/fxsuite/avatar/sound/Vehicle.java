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
 * @param torque   the engine's most torque, in newton metres, at the best of its revs: off boost, if it
 *                 has a charger, which multiplies it ({@link Charger#pull})
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
 * @param governed how fast it may go, in km/h, its speed limited there as its maker set it; 0, as
 *                 fast as it will
 */
public record Vehicle(String name, Engine engine, double mass, double torque, double[] gears, double last,
                      double wheel, double drag, double grip, double shift, double cvt, double lowest, double highest,
                      double governed) {

    public Vehicle {
        if (cvt > 0 ? lowest <= highest || highest <= 0 : gears.length == 0) {
            throw new IllegalArgumentException(name + ": gears, or a CVT's range");
        }
        gears = gears.clone();
    }

    public static final Vehicle HATCHBACK = new Vehicle("Hatchback", Engine.I4, 1150, 160,
            new double[] {3.45, 1.95, 1.30, 0.97, 0.78}, 4.1, 0.30, 0.65, 1.0, 0.35, 0, 0, 0, 0);
    public static final Vehicle RALLY = new Vehicle("Rally coupe", Engine.I5.with(Turbo.STANDARD), 1300, 240,
            new double[] {3.6, 2.13, 1.46, 1.07, 0.84}, 3.9, 0.31, 0.70, 1.15, 0.25, 0, 0, 0, 0);
    public static final Vehicle SALOON = new Vehicle("Saloon", Engine.V6, 1550, 330,
            new double[] {3.9, 2.3, 1.5, 1.15, 0.92, 0.75}, 3.6, 0.33, 0.70, 1.0, 0.3, 0, 0, 0, 0);
    public static final Vehicle MUSCLE = new Vehicle("Muscle car", Engine.V8, 1700, 560,
            new double[] {2.97, 2.07, 1.43, 1.00, 0.84, 0.56}, 3.73, 0.34, 0.75, 0.95, 0.35, 0, 0, 0, 0);
    public static final Vehicle SUPERCAR = new Vehicle("Supercar", Engine.FLAT_V8, 1450, 650,
            new double[] {3.1, 2.2, 1.7, 1.35, 1.1, 0.9, 0.75}, 3.9, 0.34, 0.60, 1.25, 0.1, 0, 0, 0, 0);
    public static final Vehicle SCOOTER_50 = new Vehicle("Scooter, 50 cc", Engine.SCOOTER_2T, 170, 4.5,
            new double[0], 1, 0.20, 0.55, 0.6, 0, 7800, 24, 8.5, 0);
    public static final Vehicle SCOOTER_125 = new Vehicle("Scooter, 125 cc", Engine.SCOOTER, 195, 11,
            new double[0], 1, 0.22, 0.60, 0.6, 0, 7200, 20, 7, 0);
    public static final Vehicle DUAL_SPORT = new Vehicle("Dual-sport bike", Engine.THUMPER, 245, 55,
            new double[] {2.5, 1.7, 1.3, 1.05, 0.88}, 5.8, 0.31, 0.60, 0.9, 0.15, 0, 0, 0, 0);
    public static final Vehicle CRUISER = new Vehicle("Cruiser", Engine.V_TWIN, 395, 140,
            new double[] {3.0, 2.0, 1.5, 1.2, 1.0, 0.85}, 3.2, 0.33, 0.70, 0.8, 0.25, 0, 0, 0, 0);
    /**
     * A big boxer cruiser: 1.8 litres, 158 newton metres at 3,000, six gears and a shaft to the back
     * wheel; 345 kg of it, its rider aboard 425, sat upright in the wind: lazy to rev, strong low
     * down, about 180 km/h flat out.
     */
    public static final Vehicle BOXER_CRUISER = new Vehicle("Boxer cruiser", Engine.BOXER, 425, 158,
            new double[] {2.438, 1.696, 1.296, 1.065, 0.903, 0.784}, 3.71, 0.32, 0.85, 0.8, 0.3, 0, 0, 0, 0);
    /**
     * The boxer with its sidecar: the same engine and gears, but nearly 500 kg of it, the sidecar's
     * body broadside to the wind: slower to pull away, and slower flat out.
     */
    public static final Vehicle BOXER_OUTFIT = new Vehicle("Boxer with sidecar", Engine.BOXER, 496, 158,
            new double[] {2.438, 1.696, 1.296, 1.065, 0.903, 0.784}, 3.71, 0.32, 1.25, 0.75, 0.3, 0, 0, 0, 0);
    /** The boxer with an ice cream cart on its sidecar: 60 kg more than with the tub, the freezer and its umbrella in the wind. */
    public static final Vehicle BOXER_ICE_CREAM = new Vehicle("Boxer with ice cream cart", Engine.BOXER, 556, 158,
            new double[] {2.438, 1.696, 1.296, 1.065, 0.903, 0.784}, 3.71, 0.32, 1.55, 0.7, 0.3, 0, 0, 0, 0);
    public static final Vehicle SPORT_BIKE = new Vehicle("Sport bike", Engine.L_TWIN, 275, 120,
            new double[] {2.6, 1.9, 1.55, 1.3, 1.15, 1.04}, 5.0, 0.31, 0.45, 1.0, 0.08, 0, 0, 0, 0);
    public static final Vehicle CLASSIC_BIKE = new Vehicle("Classic bike", Engine.PARALLEL_TWIN, 295, 60,
            new double[] {2.6, 1.8, 1.35, 1.1, 0.95}, 5.2, 0.32, 0.60, 0.85, 0.2, 0, 0, 0, 0);

    /**
     * The game's own: the car its roadster and its van are built on, a V6 in it; the pickup, a V8,
     * its speed limited to 185 km/h; and the truck, a V8 too, heavy, its eight gears tall at the top,
     * limited to 120. The two-wheelers are the scooter, the sport bike and the cruiser above.
     */
    public static final Vehicle CAR = new Vehicle("Car (roadster, van)", Engine.V6, 1450, 330,
            new double[] {3.6, 2.1, 1.45, 1.1, 0.87, 0.7}, 3.6, 0.32, 0.70, 1.0, 0.12, 0, 0, 0, 0);
    public static final Vehicle PICKUP = new Vehicle("Pickup", Engine.V8, 2100, 520,
            new double[] {4.0, 2.4, 1.6, 1.15, 0.85, 0.67}, 3.55, 0.38, 1.05, 0.9, 0.15, 0, 0, 0, 185);
    public static final Vehicle TRUCK = new Vehicle("Truck", Engine.V8, 6500, 900,
            new double[] {6.4, 4.8, 3.7, 2.9, 2.3, 1.95, 1.7, 1.5}, 5.0, 0.50, 5.0, 0.8, 0.2, 0, 0, 0, 120);

    /** One for each engine. */
    public static final List<Vehicle> ALL = List.of(HATCHBACK, RALLY, SALOON, MUSCLE, SUPERCAR,
            SCOOTER_50, SCOOTER_125, DUAL_SPORT, CRUISER, BOXER_CRUISER, BOXER_OUTFIT, BOXER_ICE_CREAM, SPORT_BIKE, CLASSIC_BIKE, CAR,
            PICKUP,
            TRUCK);

    /** The vehicle {@code engine} drives, however it is charged. */
    public static Vehicle of(final Engine engine) {
        return ALL.stream().filter(v -> v.engine.key().equals(engine.key())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("nothing drives " + engine));
    }

    /** The same vehicle, {@code fitted} with another engine: the same one charged otherwise, say. */
    public Vehicle with(final Engine fitted) {
        return new Vehicle(name, fitted, mass, torque, gears, last, wheel, drag, grip, shift, cvt, lowest, highest,
                governed);
    }

    @Override
    public double[] gears() {
        return gears.clone();
    }

    /**
     * The vehicle a land vehicle of the game's, built on {@code chassis}, is: what its engine's revs
     * and its top speed are worked out from, the same on the engine bench as in the game.
     */
    public static Vehicle of(final com.example.fxsuite.avatar.sim.Course.Chassis chassis) {
        if (chassis == com.example.fxsuite.avatar.sim.Course.SCOOTER) {
            return SCOOTER_125;
        }
        if (chassis == com.example.fxsuite.avatar.sim.Course.SPORT_BIKE) {
            return SPORT_BIKE;
        }
        if (chassis == com.example.fxsuite.avatar.sim.Course.BOXER_ICE_CREAM) {
            return BOXER_ICE_CREAM;
        }
        if (chassis == com.example.fxsuite.avatar.sim.Course.BOXER_SIDECAR) {
            return BOXER_OUTFIT;
        }
        if (chassis == com.example.fxsuite.avatar.sim.Course.BOXER) {
            return BOXER_CRUISER;
        }
        if (chassis == com.example.fxsuite.avatar.sim.Course.CRUISER) {
            return CRUISER;
        }
        if (chassis == com.example.fxsuite.avatar.sim.Course.PICKUP_CHASSIS) {
            return PICKUP;
        }
        return chassis == com.example.fxsuite.avatar.sim.Course.TRUCK_CHASSIS ? TRUCK : CAR;
    }

    /**
     * How fast its engine turns in gear {@code gear} (1 the first), going {@code speed} metres a
     * second: the wheels' turns, through the gear and the final drive.
     */
    public double rpm(final int gear, final double speed) {
        return speed / wheel * 60 / (2 * Math.PI) * ratio(gear);
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
