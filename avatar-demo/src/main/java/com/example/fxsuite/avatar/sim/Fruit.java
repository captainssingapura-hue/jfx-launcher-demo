package com.example.fxsuite.avatar.sim;

/**
 * What a piece of fruit is ({@link Course.Kind#FRUIT}): how big, and how much it takes before it
 * bursts. Every fruit is a target to be shot or cut, splashes juice when struck, and bursts
 * when broken; what it looks like, and the colour of its insides, are the graphics' to say.
 *
 * @param title     what it is called
 * @param length    how long it is, lying as it lies: across, for a watermelon on its side
 * @param width     how wide
 * @param height    how tall its body stands (a pineapple's crown is over and above, and not solid)
 * @param toughness how much it takes ({@link Weapon#blow})
 * @param heft      how big it is to hear, 0 to 1: a bigger one bursts deeper
 */
public enum Fruit {

    /** A watermelon, lying on its side: three rifle rounds, four of the Gatling gun, one stroke of the glaive. */
    WATERMELON("watermelon", 36, 28, 26, 25, 1),
    /** A pineapple, standing on end: two rifle rounds, or a stroke of the glaive. */
    PINEAPPLE("pineapple", 18, 18, 28, 15, 0.4);

    private final String title;
    private final double length;
    private final double width;
    private final double height;
    private final double toughness;
    private final double heft;

    Fruit(final String title, final double length, final double width, final double height, final double toughness,
          final double heft) {
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

    /** Its solid box, its middle {@code across} and {@code along} in the frame of what it is on, sitting on what is {@code on} high. */
    public Course.Section at(final double across, final double along, final double on) {
        return new Course.Section(across, along, on + height / 2, length / 2, width / 2, height / 2);
    }
}
