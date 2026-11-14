package com.example.fxsuite.avatar.sim;

/**
 * A crate, loose: a box of planks, {@code length} across, {@code width} along and {@code height}
 * tall, standing where it was put. Too heavy for the avatar to shove ({@link #give} 0), it is pushed
 * about by vehicles running into it, as their masses and its say ({@link Play}); hit hard enough, it
 * breaks. As tough as its size makes it ({@link Damage#CRATE}), and as heavy as its planks: a
 * hollow box, so its mass goes by its surface, not its volume.
 *
 * @param length its size across, in centimetres
 * @param width  its size along
 * @param height its height
 */
public record Crate(double length, double width, double height) implements Loose {

    /** How heavy its planks are, kilograms to the square metre. */
    public static final double PLANKS = 18;

    /** The crate {@code block} is, standing on the ground. */
    public static Crate of(final Block block) {
        return new Crate(2 * block.halfWidth(), 2 * block.halfDepth(), block.top() - block.bottom());
    }

    @Override
    public String title() {
        return "crate";
    }

    /** As tough as a crate of its size is ({@link Damage#toughness}). */
    @Override
    public double toughness() {
        return Math.round(Damage.CRATE + length * width * height / Damage.CRATE_VOLUME);
    }

    /** Not shoved by hand: it takes a vehicle. */
    @Override
    public double give() {
        return 0;
    }

    /** It does not fall: it stands on the ground. Off something, it would take a long drop. */
    @Override
    public double landing() {
        return 2000;
    }

    /** Its six sides of planks, in square metres, times how heavy they are. */
    @Override
    public double mass() {
        return PLANKS * 2 * (length * width + length * height + width * height) / 10_000;
    }
}
