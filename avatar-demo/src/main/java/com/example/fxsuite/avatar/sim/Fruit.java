package com.example.fxsuite.avatar.sim;

/**
 * What a piece of fruit is ({@link Course.Kind#FRUIT}): a loose thing ({@link Loose}), how big, how
 * much it takes before it bursts, and how hard a fall it comes through: a watermelon not much of
 * one. Every fruit is a target to be shot or cut, splashes juice when struck, and bursts
 * when broken; what it looks like, and the colour of its insides, are the graphics' to say.
 *
 * @param title     what it is called
 * @param length    how long it is, lying as it lies: across, for a watermelon on its side
 * @param width     how wide
 * @param height    how tall its body stands (a pineapple's crown is over and above, and not solid)
 * @param toughness how much it takes ({@link Weapon#blow})
 * @param heft      how big it is to hear, 0 to 1: a bigger one bursts deeper
 * @param landing   how fast it can come down and land whole
 */
public enum Fruit implements Loose {

    /** A watermelon, lying on its side: three rifle rounds, four of the Gatling gun, one stroke of the glaive. */
    WATERMELON("watermelon", 36, 28, 26, 25, 1, 900),
    /** A pineapple, standing on end: two rifle rounds, or a stroke of the glaive. */
    PINEAPPLE("pineapple", 18, 18, 28, 15, 0.4, 1100);

    private final String title;
    private final double length;
    private final double width;
    private final double height;
    private final double toughness;
    private final double heft;
    private final double landing;

    Fruit(final String title, final double length, final double width, final double height, final double toughness,
          final double heft, final double landing) {
        this.landing = landing;
        this.title = title;
        this.length = length;
        this.width = width;
        this.height = height;
        this.toughness = toughness;
        this.heft = heft;
    }

    public String title() {
        return title;
    }

    public double length() {
        return length;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double toughness() {
        return toughness;
    }

    public double heft() {
        return heft;
    }

    /** Fruit is light: pushed, it goes as fast as whatever pushes it. */
    @Override
    public double give() {
        return 1;
    }

    /** How heavy: a watermelon a good eight kilograms, a pineapple under two. */
    @Override
    public double mass() {
        return this == WATERMELON ? 8 : 1.8;
    }

    @Override
    public double landing() {
        return landing;
    }
}
