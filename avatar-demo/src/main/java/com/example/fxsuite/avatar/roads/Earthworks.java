package com.example.fxsuite.avatar.roads;

/**
 * What a kind of road asks of the ground where the ground carries it: its side slopes, so far across for each
 * one up, built up ({@code fill}) and cut down ({@code cut}); a ditch at the foot of a cutting, so wide and so
 * deep ({@code ditchWide}, {@code ditchDeep}, cm; none if either is nothing); and the highest bank ({@code bank})
 * and the deepest cutting ({@code cutting}) it takes before something else must carry it.
 */
public record Earthworks(double fill, double cut, double ditchWide, double ditchDeep, double bank, double cutting) {

    /** As a road is built: banks 1 in 2, cuttings 1 in 1.5, a ditch 1.2 m wide and 0.6 m deep, 6 m and 8 m at most. */
    public static final Earthworks ROAD = new Earthworks(2, 1.5, 120, 60, 600, 800);

    public Earthworks {
        if (!(fill > 0) || !(cut > 0) || ditchWide < 0 || ditchDeep < 0 || !(bank > 0) || !(cutting > 0)) {
            throw new IllegalArgumentException("earthworks slope some way, and go so far");
        }
    }

    /** Whether there is a ditch at the foot of a cutting. */
    public boolean ditched() {
        return ditchWide > 0 && ditchDeep > 0;
    }
}
