package com.example.fxsuite.avatar.roads;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kinds of road across ({@link Section}), as text: courses named once, then each kind, its strips from its left
 * edge to its right, its line among them where it is ({@code |}). Lengths in m, cm or mm; falls and grades in %;
 * side slopes as so far across for each one up.
 *
 * <pre>
 * courses carriageway: asphalt 40mm, binder 60mm, base 200mm, subbase 250mm
 *
 * section "two-lane road" grade=6% vertical=60m fill=2 cut=1.5 ditch=1.2m,0.6m bank=6m cutting=8m
 *   verge    2.5m  grass        fall=5%
 *   lane     3.5m  carriageway  fall=2.5%
 *   |
 *   lane     3.5m  carriageway  fall=2.5%
 *   verge    2.5m  grass        fall=5%
 * </pre>
 *
 * <p>A strip's {@code fall} is going away from the line, so a footway rising from its kerb falls less than nothing;
 * its {@code rise} is the step up at its inner edge, a kerb's upstand. A strip naming courses is paved, built of them;
 * one naming a cover ({@code grass}, {@code earth}, {@code gravel}) is ground, graded to it and covered so.
 */
public final class Sections {

    /** The kinds drawn so far. */
    public static final String STANDARD = """
            # what the paved strips are built of, top down; a ground strip names its cover: grass, earth, gravel
            courses carriageway: asphalt 40mm, binder 60mm, base 200mm, subbase 250mm
            courses lane: asphalt 30mm, base 150mm, subbase 150mm
            courses footway: paving 60mm, sand 30mm, base 150mm
            courses kerb: concrete 250mm, base 150mm
            courses path: gravel 50mm, base 100mm

            section "two-lane road" grade=6% vertical=60m fill=2 cut=1.5 ditch=1.2m,0.6m bank=6m cutting=8m
              verge     2.5m   grass        fall=5%
              shoulder  1m     carriageway  fall=2.5%
              lane      3.5m   carriageway  fall=2.5%
              |
              lane      3.5m   carriageway  fall=2.5%
              shoulder  1m     carriageway  fall=2.5%
              verge     2.5m   grass        fall=5%

            section "town street" grade=8% vertical=30m fill=1.5 cut=1 ditch=0,0 bank=4m cutting=5m
              footway   2m     footway      fall=-2.5%
              kerb      15cm   kerb         rise=12cm
              lane      3.25m  carriageway  fall=2.5%
              |
              lane      3.25m  carriageway  fall=2.5%
              kerb      15cm   kerb         rise=12cm
              footway   2m     footway      fall=-2.5%

            # one way of a divided road: its line at its inner edge, by the median, its half of which is its own
            section "carriageway" grade=5% vertical=100m fill=2 cut=1.5 ditch=1.5m,0.8m bank=8m cutting=10m
              verge     3m     grass        fall=5%
              shoulder  3.3m   carriageway  fall=2.5%
              lane      3.65m  carriageway  fall=2.5%
              lane      3.65m  carriageway  fall=2.5%
              shoulder  1m     carriageway  fall=2.5%
              |
              median    1.5m   grass        fall=5%

            section "country lane" grade=10% vertical=25m fill=1.5 cut=1 ditch=0.8m,0.4m bank=4m cutting=5m
              verge     1m     grass        fall=6%
              lane      2.75m  lane         fall=3%
              |
              lane      2.75m  lane         fall=3%
              verge     1m     grass        fall=6%

            section "footpath" grade=12% vertical=5m fill=1.5 cut=1 ditch=0,0 bank=3m cutting=3m
              path      1m     path         fall=2%
              |
              path      1m     path         fall=2%
            """;

    private Sections() {
    }

    /** The kinds {@code text} describes, by name, in its order. Throws, saying which line and why, if it cannot be read. */
    public static Map<String, Section> parse(final String text) {
        return read(text).sections();
    }

    /** Courses by name and kinds by name, each in the order written: what a text of kinds says. */
    public record Catalogue(Map<String, List<Course>> courses, Map<String, Section> sections) {
    }

    /** What {@code text} says: its courses and its kinds. Throws, saying which line and why, if it cannot be read. */
    public static Catalogue read(final String text) {
        final Map<String, List<Course>> courses = new LinkedHashMap<>();
        final Map<String, Section> sections = new LinkedHashMap<>();
        Building building = null;
        final String[] lines = text.split("\\R", -1);
        for (int n = 0; n < lines.length; n++) {
            final String line = stripped(lines[n]);
            if (line.isEmpty()) {
                continue;
            }
            try {
                if (line.startsWith("courses ")) {
                    building = done(building, sections);
                    final int colon = line.indexOf(':');
                    if (colon < 0) {
                        throw new IllegalArgumentException("courses name: stuff thickness, ...");
                    }
                    final String name = line.substring("courses ".length(), colon).trim();
                    final List<Course> list = new ArrayList<>();
                    for (final String part : line.substring(colon + 1).split(",")) {
                        final String[] bits = part.trim().split("\\s+");
                        if (bits.length != 2) {
                            throw new IllegalArgumentException("a course is a stuff and a thickness: \"" + part.trim() + "\"");
                        }
                        list.add(new Course(stuff(bits[0]), length(bits[1])));
                    }
                    courses.put(name, list);
                } else if (line.startsWith("section ")) {
                    building = done(building, sections);
                    building = section(line);
                } else if (line.equals("|")) {
                    if (building == null || building.lined) {
                        throw new IllegalArgumentException("a line belongs in a section, once");
                    }
                    building.lined = true;
                } else {
                    if (building == null) {
                        throw new IllegalArgumentException("a strip belongs in a section");
                    }
                    final Strip strip = strip(line, courses);
                    (building.lined ? building.right : building.left).add(strip);
                }
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("line " + (n + 1) + ": " + e.getMessage(), e);
            }
        }
        done(building, sections);
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("no sections");
        }
        return new Catalogue(courses, sections);
    }

    /** The standard kinds. */
    public static Map<String, Section> standard() {
        return parse(STANDARD);
    }

    /** A section being read: its attributes, its strips so far either side of its line. */
    private static final class Building {
        String name;
        double grade = 0.06;
        double vertical = 6_000;
        double fill = Earthworks.ROAD.fill();
        double cut = Earthworks.ROAD.cut();
        double ditchWide = Earthworks.ROAD.ditchWide();
        double ditchDeep = Earthworks.ROAD.ditchDeep();
        double bank = Earthworks.ROAD.bank();
        double cutting = Earthworks.ROAD.cutting();
        boolean lined;
        final List<Strip> left = new ArrayList<>();
        final List<Strip> right = new ArrayList<>();
    }


    /**
     * {@code catalogue} written as text, as {@link #read} reads it: its courses, then each kind, its strips from its
     * left edge to its right, its line among them. A strip's courses by the name of courses written the same; any
     * not among them written under a name of their own.
     */
    public static String write(final Catalogue catalogue) {
        final Map<String, List<Course>> courses = new LinkedHashMap<>(catalogue.courses());
        for (final Section s : catalogue.sections().values()) {
            for (final Section.Laid l : s.laid()) {
                if (l.strip().paved() && name(courses, l.strip().courses()) == null) {
                    courses.put(l.strip().use().written() + (courses.size() + 1), l.strip().courses());
                }
            }
        }
        final StringBuilder out = new StringBuilder(
                "# what the paved strips are built of, top down; a ground strip names its cover: grass, earth, gravel\n");
        courses.forEach((name, list) -> {
            out.append("courses ").append(name).append(':');
            for (int k = 0; k < list.size(); k++) {
                out.append(k == 0 ? " " : ", ").append(list.get(k).stuff().written()).append(' ')
                        .append(written(list.get(k).thick()));
            }
            out.append('\n');
        });
        for (final Section s : catalogue.sections().values()) {
            final Earthworks e = s.earthworks();
            out.append('\n').append("section \"").append(s.name()).append("\" grade=").append(percent(s.grade()))
                    .append(" vertical=").append(written(s.vertical())).append(" fill=").append(number(e.fill()))
                    .append(" cut=").append(number(e.cut())).append(" ditch=").append(written(e.ditchWide())).append(',')
                    .append(written(e.ditchDeep())).append(" bank=").append(written(e.bank())).append(" cutting=")
                    .append(written(e.cutting())).append('\n');
            for (final Strip strip : s.left().reversed()) {
                out.append(line(strip, courses));
            }
            out.append("  |\n");
            for (final Strip strip : s.right()) {
                out.append(line(strip, courses));
            }
        }
        return out.toString();
    }

    private static String line(final Strip s, final Map<String, List<Course>> courses) {
        final StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "  %-9s %-6s %-12s", s.use().written(),
                written(s.width()), s.paved() ? name(courses, s.courses()) : s.cover().written()));
        if (s.fall() != 0) {
            line.append(" fall=").append(percent(s.fall()));
        }
        if (s.rise() != 0) {
            line.append(" rise=").append(written(s.rise()));
        }
        return line.toString().stripTrailing() + "\n";
    }

    private static String name(final Map<String, List<Course>> courses, final List<Course> list) {
        for (final Map.Entry<String, List<Course>> e : courses.entrySet()) {
            if (e.getValue().equals(list)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** A length, in cm, as it is best written: in m from a metre up, in mm if not whole cm, else in cm. */
    static String written(final double cm) {
        if (cm == 0) {
            return "0";
        }
        if (Math.abs(cm) >= 100) {
            return number(cm / 100) + "m";
        }
        if (Math.abs(cm - Math.rint(cm)) > 1e-9) {
            return number(cm * 10) + "mm";
        }
        return number(cm) + "cm";
    }

    private static String percent(final double fraction) {
        return number(fraction * 100) + "%";
    }

    /** A number with as few figures as it needs, at most four after the point. */
    private static String number(final double v) {
        final String s = new java.math.BigDecimal(Math.rint(v * 10_000) / 10_000).setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        return s.equals("-0") ? "0" : s;
    }

    private static Building done(final Building b, final Map<String, Section> sections) {
        if (b != null) {
            if (!b.lined) {
                throw new IllegalArgumentException("\"" + b.name + "\" has no line among its strips (|)");
            }
            sections.put(b.name, new Section(b.name, b.left.reversed(), b.right,
                    new Earthworks(b.fill, b.cut, b.ditchWide, b.ditchDeep, b.bank, b.cutting), b.grade, b.vertical));
        }
        return null;
    }

    private static Building section(final String line) {
        final int open = line.indexOf('"');
        final int close = line.indexOf('"', open + 1);
        if (open < 0 || close < 0) {
            throw new IllegalArgumentException("a section is named in quotes");
        }
        final Building b = new Building();
        b.name = line.substring(open + 1, close);
        for (final String part : line.substring(close + 1).trim().split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            final String[] kv = keyValue(part);
            switch (kv[0]) {
                case "grade" -> b.grade = fraction(kv[1]);
                case "vertical" -> b.vertical = length(kv[1]);
                case "fill" -> b.fill = ratio(kv[1]);
                case "cut" -> b.cut = ratio(kv[1]);
                case "ditch" -> {
                    final String[] wd = kv[1].split(",");
                    if (wd.length != 2) {
                        throw new IllegalArgumentException("a ditch is so wide, so deep: ditch=1.2m,0.6m");
                    }
                    b.ditchWide = length(wd[0]);
                    b.ditchDeep = length(wd[1]);
                }
                case "bank" -> b.bank = length(kv[1]);
                case "cutting" -> b.cutting = length(kv[1]);
                default -> throw new IllegalArgumentException("a section has no \"" + kv[0] + "\"");
            }
        }
        return b;
    }

    private static Strip strip(final String line, final Map<String, List<Course>> courses) {
        final String[] bits = line.split("\\s+");
        if (bits.length < 3) {
            throw new IllegalArgumentException("a strip is a use, a width and its courses or its cover: lane 3.5m carriageway");
        }
        final Strip.Use use;
        try {
            use = Strip.Use.valueOf(bits[0].toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("no such strip as \"" + bits[0] + "\"");
        }
        // courses written before it, by name, if there are; else a cover
        final List<Course> built = courses.get(bits[2]);
        Cover cover = null;
        if (built == null) {
            try {
                cover = Cover.valueOf(bits[2].toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("no courses \"" + bits[2] + "\" written before it, nor such a cover");
            }
        }
        double fall = 0;
        double rise = 0;
        for (int k = 3; k < bits.length; k++) {
            final String[] kv = keyValue(bits[k]);
            switch (kv[0]) {
                case "fall" -> fall = fraction(kv[1]);
                case "rise" -> rise = length(kv[1]);
                default -> throw new IllegalArgumentException("a strip has no \"" + kv[0] + "\"");
            }
        }
        return built != null ? Strip.paved(use, length(bits[1]), built, fall, rise)
                : Strip.ground(use, length(bits[1]), cover, fall, rise);
    }

    private static String[] keyValue(final String part) {
        final int eq = part.indexOf('=');
        if (eq <= 0) {
            throw new IllegalArgumentException("\"" + part + "\" is not name=value");
        }
        return new String[] {part.substring(0, eq), part.substring(eq + 1)};
    }

    private static Course.Stuff stuff(final String name) {
        try {
            return Course.Stuff.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("no such stuff as \"" + name + "\"");
        }
    }

    /** A length in m, cm or mm, in cm. */
    static double length(final String s) {
        final double scale;
        final String number;
        if (s.endsWith("mm")) {
            scale = 0.1;
            number = s.substring(0, s.length() - 2);
        } else if (s.endsWith("cm")) {
            scale = 1;
            number = s.substring(0, s.length() - 2);
        } else if (s.endsWith("m")) {
            scale = 100;
            number = s.substring(0, s.length() - 1);
        } else {
            scale = 100;
            number = s;
        }
        return number(number, s) * scale;
    }

    /** A percentage, as a fraction. */
    static double fraction(final String s) {
        return s.endsWith("%") ? number(s.substring(0, s.length() - 1), s) / 100 : number(s, s);
    }

    /** A side slope: so far across for each one up, written 2 or 1:2. */
    static double ratio(final String s) {
        final int colon = s.indexOf(':');
        return colon < 0 ? number(s, s) : number(s.substring(colon + 1), s) / number(s.substring(0, colon), s);
    }

    private static double number(final String s, final String whole) {
        try {
            return Double.parseDouble(s);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("\"" + whole + "\" is not a number");
        }
    }

    private static String stripped(final String line) {
        final int hash = line.indexOf('#');
        return (hash < 0 ? line : line.substring(0, hash)).trim();
    }
}
