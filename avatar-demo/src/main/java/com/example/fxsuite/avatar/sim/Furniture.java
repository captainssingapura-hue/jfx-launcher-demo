package com.example.fxsuite.avatar.sim;

/**
 * What a piece of furniture is ({@link Course.Kind#FURNITURE}): a loose thing ({@link Loose}) to
 * stand things on, push about, and break. How it looks is the graphics' to say.
 *
 * @param title     what it is called
 * @param length    how long it is, across its own frame
 * @param width     how deep
 * @param height    how tall
 * @param toughness how much it takes ({@link Weapon#blow})
 * @param give      how readily it is pushed, 0 to 1
 * @param landing   how fast it can come down and land whole
 * @param seat      how far back of its middle whoever sits on it sits
 */
public enum Furniture implements Loose {

    /** A table of planks on a pair of pallets: twenty rifle rounds, or two strokes of the glaive; heavy to push. */
    TABLE("table", 240, 100, 70, 200, 0.45, 1600, 0),
    /** A wooden stool to sit on: four rifle rounds; light. */
    STOOL("stool", 32, 32, 45, 40, 0.9, 1400, 0),
    /** A little scooter, its front along its frame, solid as far as its seat: fifteen rifle rounds; heavy-ish. */
    SCOOTER("scooter", 34, 100, 57, 150, 0.6, 1200, 8);

    private final String title;
    private final double length;
    private final double width;
    private final double height;
    private final double toughness;
    private final double give;
    private final double landing;
    private final double seat;

    Furniture(final String title, final double length, final double width, final double height,
              final double toughness, final double give, final double landing, final double seat) {
        this.seat = seat;
        this.title = title;
        this.length = length;
        this.width = width;
        this.height = height;
        this.toughness = toughness;
        this.give = give;
        this.landing = landing;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public double width() {
        return width;
    }

    @Override
    public double height() {
        return height;
    }

    @Override
    public double toughness() {
        return toughness;
    }

    @Override
    public double give() {
        return give;
    }

    /** How heavy: the table of planks and pallets sixty kilograms, a stool four, the little scooter ninety. */
    @Override
    public double mass() {
        return switch (this) {
            case TABLE -> 60;
            case STOOL -> 4;
            case SCOOTER -> 90;
        };
    }

    @Override
    public double landing() {
        return landing;
    }

    /** How far back of its middle whoever sits on it sits, along its frame. */
    public double seat() {
        return seat;
    }
}
