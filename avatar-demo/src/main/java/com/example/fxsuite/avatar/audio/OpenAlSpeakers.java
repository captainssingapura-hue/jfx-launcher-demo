package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_FALSE;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.AL_LOOPING;
import static org.lwjgl.openal.AL10.AL_ORIENTATION;
import static org.lwjgl.openal.AL10.AL_PITCH;
import static org.lwjgl.openal.AL10.AL_PLAYING;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.AL_REFERENCE_DISTANCE;
import static org.lwjgl.openal.AL10.AL_ROLLOFF_FACTOR;
import static org.lwjgl.openal.AL10.AL_SOURCE_RELATIVE;
import static org.lwjgl.openal.AL10.AL_SOURCE_STATE;
import static org.lwjgl.openal.AL10.AL_TRUE;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alDistanceModel;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alGetSourcei;
import static org.lwjgl.openal.AL10.alListener3f;
import static org.lwjgl.openal.AL10.alListenerf;
import static org.lwjgl.openal.AL10.alListenerfv;
import static org.lwjgl.openal.AL10.alSource3f;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;
import static org.lwjgl.openal.AL11.AL_INVERSE_DISTANCE_CLAMPED;
import static org.lwjgl.openal.AL10.AL_MAX_DISTANCE;
import static org.lwjgl.openal.ALC10.ALC_DEVICE_SPECIFIER;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetString;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.openal.EnumerateAllExt.ALC_ALL_DEVICES_SPECIFIER;

/**
 * Sound through OpenAL (OpenAL Soft, through LWJGL): it places, pitches and mixes every sound
 * itself, on its own thread. The one output that needs a native library; if that will not
 * load, or there is no device, {@link Outputs} falls back to Java Sound.
 *
 * <p>OpenAL's axes are not the simulation's: its y is up and it looks down -z, where ours looks
 * down +z with y down. Turning half round the x axis takes one to the other: (x, -y, -z).
 */
final class OpenAlSpeakers implements Speakers {

    /** One-shots that can sound at once; past that, the oldest gives way. */
    private static final int VOICES = 24;
    private static final String MEMORY_BACKEND = "org.lwjgl.system.memoryBackend";

    private final long device;
    private final long context;
    private final String name;
    private final Map<Clip, Integer> buffers = new IdentityHashMap<>();
    private final int[] voices = new int[VOICES];
    private final List<Integer> loops = new ArrayList<>();
    private int next;

    private OpenAlSpeakers(final long device, final long context, final String name) {
        this.device = device;
        this.context = context;
        this.name = name;
        alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);
        for (int i = 0; i < VOICES; i++) {
            voices[i] = source();
        }
    }

    static Speakers open() {
        // LWJGL reaches native memory through sun.misc.Unsafe by default, which JDK 25 warns
        // about; its FFM way is a little slower, which a few calls a frame never notice
        if (System.getProperty(MEMORY_BACKEND) == null) {
            System.setProperty(MEMORY_BACKEND, "ffm");
        }
        final long device = alcOpenDevice((ByteBuffer) null);
        if (device == 0) {
            throw new IllegalStateException("no audio device");
        }
        long context = 0;
        try {
            final ALCCapabilities abilities = ALC.createCapabilities(device);
            context = alcCreateContext(device, (IntBuffer) null);
            if (context == 0 || !alcMakeContextCurrent(context)) {
                throw new IllegalStateException("could not start OpenAL on the device");
            }
            AL.createCapabilities(abilities);
            // the full name ("OpenAL Soft on Speakers (...)") where the device can say it
            final String name = abilities.ALC_ENUMERATE_ALL_EXT
                    ? alcGetString(device, ALC_ALL_DEVICES_SPECIFIER)
                    : alcGetString(device, ALC_DEVICE_SPECIFIER);
            return new OpenAlSpeakers(device, context, name);
        } catch (RuntimeException | LinkageError e) {
            if (context != 0) {
                alcMakeContextCurrent(0);
                alcDestroyContext(context);
            }
            alcCloseDevice(device);
            throw e;
        }
    }

    @Override
    public Kind kind() {
        return Kind.OPENAL;
    }

    @Override
    public String device() {
        return name;
    }

    @Override
    public void listen(final Vec3 at, final double yaw) {
        final double y = Math.toRadians(yaw);
        alListener3f(AL_POSITION, (float) at.x(), (float) -at.y(), (float) -at.z());
        // facing (sin, 0, cos) in ours is (sin, 0, -cos) in OpenAL's; up is up
        alListenerfv(AL_ORIENTATION, new float[] {(float) Math.sin(y), 0, (float) -Math.cos(y), 0, 1, 0});
    }

    @Override
    public void volume(final double volume) {
        alListenerf(AL_GAIN, (float) Math.max(0, volume));
    }

    @Override
    public void play(final Clip clip, final double gain, final double pitch) {
        play(clip, gain, pitch, null);
    }

    @Override
    public void play(final Clip clip, final double gain, final double pitch, final Vec3 at, final double near) {
        final int voice = freeVoice();
        alSourceStop(voice);
        alSourcef(voice, AL_REFERENCE_DISTANCE, (float) near);
        alSourcei(voice, AL_BUFFER, buffer(clip));
        alSourcei(voice, AL_LOOPING, AL_FALSE);
        alSourcef(voice, AL_GAIN, (float) Math.max(0, gain));
        alSourcef(voice, AL_PITCH, (float) Math.max(0.01, pitch));
        place(voice, at);
        alSourcePlay(voice);
    }

    @Override
    public Loop loop(final Clip clip) {
        final int source = source();
        alSourcei(source, AL_BUFFER, buffer(clip));
        alSourcei(source, AL_LOOPING, AL_TRUE);
        alSourcef(source, AL_GAIN, 0);
        place(source, null);
        alSourcePlay(source);
        loops.add(source);
        return new Loop() {
            private boolean stopped;

            @Override
            public void set(final double gain, final double pitch) {
                if (!stopped) {
                    alSourcef(source, AL_GAIN, (float) Math.max(0, gain));
                    alSourcef(source, AL_PITCH, (float) Math.max(0.01, pitch));
                }
            }

            @Override
            public void place(final Vec3 at) {
                if (!stopped) {
                    OpenAlSpeakers.place(source, at);
                }
            }

            @Override
            public void stop() {
                if (!stopped) {
                    stopped = true;
                    alSourceStop(source);
                    alDeleteSources(source);
                    loops.remove(Integer.valueOf(source));
                }
            }
        };
    }

    @Override
    public void close() {
        for (final int source : voices) {
            alSourceStop(source);
            alDeleteSources(source);
        }
        for (final int source : loops) {
            alSourceStop(source);
            alDeleteSources(source);
        }
        loops.clear();
        for (final int buffer : buffers.values()) {
            alDeleteBuffers(buffer);
        }
        buffers.clear();
        alcMakeContextCurrent(0);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    /** A source, set to fade with distance as {@link Space} says. */
    private static int source() {
        final int source = alGenSources();
        alSourcef(source, AL_REFERENCE_DISTANCE, (float) Space.NEAR);
        alSourcef(source, AL_MAX_DISTANCE, (float) Space.FAR);
        alSourcef(source, AL_ROLLOFF_FACTOR, (float) Space.ROLLOFF);
        return source;
    }

    /** Null: at the listener, wherever it goes. */
    private static void place(final int source, final Vec3 at) {
        if (at == null) {
            alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
            alSource3f(source, AL_POSITION, 0, 0, 0);
        } else {
            alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE);
            alSource3f(source, AL_POSITION, (float) at.x(), (float) -at.y(), (float) -at.z());
        }
    }

    /** Uploaded once per clip, then reused. */
    private int buffer(final Clip clip) {
        return buffers.computeIfAbsent(clip, c -> {
            final int buffer = alGenBuffers();
            alBufferData(buffer, AL_FORMAT_MONO16, c.pcm16(), Clip.RATE);
            return buffer;
        });
    }

    /** A voice that has finished, or else the one used longest ago. */
    private int freeVoice() {
        for (int i = 0; i < VOICES; i++) {
            final int voice = voices[(next + i) % VOICES];
            if (alGetSourcei(voice, AL_SOURCE_STATE) != AL_PLAYING) {
                next = (next + i + 1) % VOICES;
                return voice;
            }
        }
        final int oldest = voices[next];
        next = (next + 1) % VOICES;
        return oldest;
    }
}
