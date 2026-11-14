package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A run kept as its inputs, played back to the same run, and caught when it is not. */
class RecordingTest {

    /** A busy minute's worth: the Gatling gun, turning, walking, the pack, flying, aiming up. */
    private static final Script BUSY = Script.parse(
            "DIGIT3;FIRE+@40:2;W+SHIFT+@-15:1.5;J;F;SPACE:1;FIRE+^10:1.5;W:1;DIGIT1;FIRE+@25:2");

    private record Kept(String text, List<Event> events, Play play) {
    }

    /** Plays {@code script} from the start, keeping its inputs as it goes. */
    private static Kept keep(final Script script) {
        final StringWriter out = new StringWriter();
        final Recording.Recorder recorder = new Recording.Recorder(out, false);
        Play play = Play.start(false);
        final List<Event> events = new ArrayList<>();
        for (long step = 1; step <= script.length(); step++) {
            final Controls controls = script.controls(step);
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
            recorder.step(step, controls, play);
        }
        recorder.close();
        return new Kept(out.toString(), events, play);
    }

    private static Recording read(final String text) throws IOException {
        return Recording.read(new StringReader(text));
    }

    @Test
    void whatIsKeptReadsBackAsTheSameInputsEveryStep() throws IOException {
        final Recording recording = read(keep(BUSY).text());
        assertEquals(BUSY.length(), recording.length());
        assertEquals(Play.RULES, recording.rules());
        for (long step = 1; step <= BUSY.length(); step++) {
            assertEquals(BUSY.controls(step), recording.controls(step), "step " + step);
        }
        assertEquals(BUSY.length() / Recording.CHECK_EVERY, recording.checks(), "a checksum a second");
    }

    @Test
    void playedBackItIsTheSameRunToTheBit() throws IOException {
        final Kept kept = keep(BUSY);
        final Recording.Replayed replayed = read(kept.text()).replay();
        assertTrue(kept.events().stream().anyMatch(e -> e instanceof Event.Fired));
        assertEquals(0, replayed.astray(), "every checksum matched");
        assertEquals(kept.events(), replayed.events());
        assertEquals(kept.play().avatar(), replayed.play().avatar());
        assertEquals(kept.play().checksum(), replayed.play().checksum());
    }

    @Test
    void aKeyHeldForSecondsIsOneLine() {
        final String text = keep(Script.parse("W:2;:1")).text();
        final List<String> steps = text.lines().filter(l -> Character.isDigit(l.charAt(0))).toList();
        // standing still from the start, walking from step 61, standing again from step 301
        assertEquals(3, steps.size(), text);
        assertTrue(steps.get(1).startsWith("61 1.0 "), steps.get(1));
        assertTrue(steps.get(2).startsWith("301 0.0 "), steps.get(2));
    }

    @Test
    void numbersComeBackToTheBitAndWhatWasDoneOnceOnlyInItsOwnStep() throws IOException {
        final Controls odd = new Controls(1, -1, 0.1 + 0.2, 0, true, false, 1.0 / 3,
                List.of(Action.JUMP, new Action.Draw(Weapon.GLAIVE)));
        final StringWriter out = new StringWriter();
        final Recording.Recorder recorder = new Recording.Recorder(out, true);
        recorder.step(1, odd, Play.start(true));
        recorder.step(2, odd.with(List.of()), Play.start(true));
        recorder.close();
        final Recording recording = read(out.toString());
        assertTrue(recording.pack());
        assertEquals(odd, recording.controls(1));
        assertEquals(Double.doubleToLongBits(0.1 + 0.2), Double.doubleToLongBits(recording.controls(1).turn()));
        assertEquals(odd.with(List.of()), recording.controls(2), "the held keys carry on; the jump does not");
        assertEquals(1, out.toString().lines().filter(l -> Character.isDigit(l.charAt(0))).count(),
                "and the second step needed no line of its own");
    }

    @Test
    void theOtherButtonIsKeptAndALineKeptBeforeThereWasOneStillReads() throws IOException {
        final Controls bombing = new Controls(0, 0, 0, 0, true, false, 0, List.of(Action.JUMP, Action.GUN), true);
        final StringWriter out = new StringWriter();
        final Recording.Recorder recorder = new Recording.Recorder(out, false);
        recorder.step(1, bombing, Play.start(false));
        recorder.close();
        assertEquals(bombing, read(out.toString()).controls(1));
        // written before there was another button: straight from the aim to what was done once
        final Recording old = read("avatar-inputs 1\nrules 1\nstart pack=false\n1 1.0 0.0 0.0 0.0 true false 0.0 JUMP\n");
        assertEquals(new Controls(1, 0, 0, 0, true, false, 0, List.of(Action.JUMP)), old.controls(1));
    }

    @Test
    void aReplayThatGoesAstrayIsCaughtAtTheNextChecksum() throws IOException {
        // the first line says the avatar walked from step 1, which it did not
        final String text = keep(BUSY).text().replaceFirst("\n1 0\\.0 ", "\n1 1.0 ");
        final Recording.Replayed replayed = read(text).replay();
        assertEquals(Recording.CHECK_EVERY, replayed.astray());
    }

    @Test
    void itRefusesAnotherFormatAndSaysWhichLineItCouldNotRead() {
        assertThrows(IllegalArgumentException.class, () -> read("avatar-inputs 2\nrules 1\nstart pack=false\n"));
        assertThrows(IllegalArgumentException.class, () -> read("something else entirely\n"));
        final IllegalArgumentException bad = assertThrows(IllegalArgumentException.class,
                () -> read("avatar-inputs 1\nrules 1\nstart pack=false\n1 0.0 0.0 0.0 0.0 maybe false 0.0\n"));
        assertTrue(bad.getMessage().startsWith("line 4"), bad.getMessage());
    }

    @Test
    void aRecordingCutShortStillReadsUpToItsLastLine() throws IOException {
        final String whole = keep(BUSY).text();
        final String cut = whole.substring(0, whole.indexOf("check 360"));
        final Recording recording = read(cut);
        assertTrue(recording.length() >= 240 && recording.length() < 360, "to " + recording.length());
        assertEquals(0, recording.replay().astray());
    }
}
