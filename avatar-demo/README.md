# avatar-demo — a box-rig avatar in plain JavaFX 3D

A standalone experiment: a small robot built from boxes that walks, runs and jumps around a
world of pillars, crates and floating gates, followed by a chase camera. Put on its jet-pack
and it flies as a drone does: upright, tipped gently toward where it goes, holding its place
when let go. Everything is heard as well as seen. Two sound profiles give the same world two
characters, and a separate sound bench is for working on the sounds.

![Hovering among the pillars, the wings spread](images/flying.jpg)

- **No mesh or skin.** Each limb is a rigid part hung from a joint. The animation is worked
  out each frame from how the body moves, the way blocky game characters are animated.
- **A solid world.** The avatar slides along what it runs into, stands on top of blocks,
  jumps up onto the lower crates (try the staircase straight ahead of the start), falls off
  edges, and lands on pillars it flies down onto. The layout (`Course`) is plain data that is
  both drawn and collided with, so what you see is what stops you.
- **A jet with swing wings.** Avatar and pack are drawn as a fighter jet standing on its
  tail, held up by the thrust of its twin engines. The wings lie in the plane of the back and
  pivot there as a variable-sweep fighter's do. They are tucked along the engines on the
  ground, spread wide to hover, and swept further back the faster it flies. They are as solid
  as the body: a wing that meets a post stops the flight, and one that clips an edge glances
  off.
- **Weapons.** Two AK-style rifles, one in each hand, held from the hip as a soldier fires
  from it: each stock clamped between the upper arm and the body, the forearm folded along the
  rifle to the pistol grip. They fire in turn, left and right, each forearm kicking up as it
  fires, with a flash at the muzzle and a brass casing thrown out. Or a glaive as Guan Yu
  carried, a long pole with a broad crescent blade, standing upright in the right hand. Its
  strokes go in turn: a cleave over the head, then a sweep across. Or a Gatling gun with a
  motor, held at the right hip by its grip, the left hand reaching across to a lever on top.
  The left arm is worked out each frame to keep its hand on the lever wherever the gun points.
  Its six barrels spin up before it fires, 2,400 rounds a minute, and spin down when let go.
  The robot holds it dead steady, every round along one line, with a tracer every third round
  and casings pouring out.
- **Aiming and hits.** The rifles shoot straight ahead of the body, wherever the head or the
  camera is looking round: to aim, turn the body. Up and down follow the view instead: tilt it
  up from where it rests and the guns aim up as far, tilt it down and they aim down, up to
  50° either way for the rifles and 30° for the heavier Gatling gun. At rest, and walking, they
  shoot level. A crosshair out in the world marks the
  line the rounds fly along, where it meets a block or the ground, or 30 m off. It is only a
  mark: it never steers a round. Being in front of the avatar, it never hides it. Each round
  flies from its own muzzle, spread a little as rounds from the hip are, with a tracer. Where
  it strikes a block it throws sparks and leaves a bullet hole; on the ground, a puff of dust.
  The glaive strikes whatever is within its reach as the blade comes through. A cleave chops
  into the ground in front, and a sweep catches what is beside; on a block the blade leaves a
  gash. The marks fade after a while.
- **Targets and damage.** A range of wooden target dummies stands off to the left of the start,
  more behind it, and drones hover all round. High over everything, far out, three great
  armoured airships go slowly round the start, a lap taking the best part of two minutes. Each
  is 18 m long, with a stepped steel hull banded in yellow, a cross of tail fins, a lit gondola
  underneath and an engine with a turning propeller either side. Each part of one is solid, so
  it can be landed on, and whatever stands on one is carried along with it, turned as it turns,
  and can walk about on it as it goes. They, and the crates, can be broken; pillars and
  gates stand whatever is thrown at them. Every round or stroke takes some of what a piece can
  take:
  - a rifle round takes 10, a Gatling round 8 (but 40 a second), and a stroke of the glaive 100;
  - a dummy can take 300, a drone 120, and an airship 3,000: most of ten seconds of the
    Gatling gun. A crate takes from about 90 to 600, by its size.

  As it wears down it darkens and cracks, and a bar over it shows what it has left. An airship
  catches fire instead, in more places as it nears its end. The blow that takes the last of it
  breaks it: a crate or dummy bursts into pieces that tumble, bounce and come to rest, and a
  drone goes up in a fireball. An airship goes up in blast after blast along its hull and rains
  wreckage from the sky. Broken is for good, until the demo starts again. It is gone from the world, so rounds fly on through where it stood and whatever stood
  on it falls. The HUD counts the targets down and the crates broken.
- **Bombs.** Each airship lets a bomb go every three seconds, in turn, wherever it is: carpet
  bombing. A bomb falls from under the gondola, carrying on the way the airship was going, and
  goes off at the first thing it meets, in a fireball, embers and, on the ground, clods of earth
  thrown up. Every crate or target within 3.5 m of it takes 150: a drone or a small crate goes at
  once, a dummy takes two. The robot takes no harm from them yet.
- **Platforms, flown like magic carpets.** Two steel platforms float just off the ground, edged
  in yellow and black, a thruster glowing under each corner and a console at the front: a
  small one, 3 m by 2.4 m, off to the right of the start, and a broad barge, 10 m by 6 m, off to
  the left. A platform is as big as it is made for what it is for (`Course.Deck`). Jump onto
  one and stand behind its console, and the HUD offers the controls (T). At them, the keys that
  would move the robot fly the platform: W and S ahead and back, A and D sideways, the mouse
  turns it, Space climbs and Ctrl descends, from just off the ground to 45 m up, Shift to go
  carefully. It eases up to speed and to a stop, and stops short of anything in its way, with
  room over the deck for whoever rides it. The robot stands at the console and is carried and
  turned with it, and can still draw a weapon and fire. T again lets go, and it hangs where it
  is. F at the controls cuts its engine: its thrusters go dark, it cannot be flown, and it drops
  until it settles on whatever is under it (the ground, a crate or pillar, an airship, another
  platform, or the robot's head) and rests there. Resting on something that moves, it is
  docked: set one down on an airship and it goes round with it, the pilot aboard; set the small
  one down on the barge and it goes wherever the barge is flown. F again starts it, and it lifts
  off from there. Whatever moves (the airships, the platforms) is drawn where the simulation says it was at
  its last two steps, part way between, and marks on it go with it.
- **Vehicles.** The platforms and an airship to be flown are vehicles (`Course.Craft`): each
  handles as it does (`Course.Handling`), takes damage and breaks like anything else, and, at
  its controls, the camera is the vehicle's: it follows the vehicle from as far back as it is
  big. A platform is open: the robot rides it in plain sight. An airship is closed: walk in
  under its gondola, take it (T) at the hatch in its floor, and the robot is inside and out of
  sight. W and S fly it ahead and back, the mouse turns it (slowly, as an airship turns),
  Space climbs and Ctrl descends, never below its moorings; it has no sideways and its engines
  are never cut. A click drops a bomb from under the gondola, going as the airship goes; held,
  a stick of them, one every 0.4 s. Its bay holds a dozen, and loads them back, one a second,
  when it is down at its moorings (no bombs are dropped there). Low over its own bombs, it takes
  their blast too. T again and the robot drops out of the hatch, from wherever it is. Broken
  while flown, a vehicle throws out whoever flies it. At a vehicle's controls the HUD is the
  vehicle's: how fast and high it goes, how much of it is left, its engine or its bombs, and
  its own keys in place of the robot's.
- **The fruit stall.** On the barge stands a fruit stall: a table of planks on pallets under a
  red and white awning, a sign (西瓜), a scale, a row of striped watermelons and a few more on the
  deck, and three pineapples standing at the other end. The stallholder sits on a stool behind it; a customer stands before
  it, and another sits on a scooter parked to one side. All of it goes with the barge wherever
  it is flown, and standing on it rides too. Every piece of fruit is one kind of thing (`Fruit` says which, how big and how tough), to
  be shot or cut: a watermelon takes three rifle rounds, four of the Gatling gun or one stroke of
  the glaive, a pineapple two rounds. Struck, it splashes juice, red or gold, and the
  stallholder shakes a fist; burst, it throws its flesh and skin, seeds or leaves, with a wet
  splat, deeper for a watermelon, and everyone jumps, hands in the air. The HUD counts the fruit
  smashed.
- **Loose things: furniture and fruit.** The table and the fruit are loose things (`Prop`): each
  stands on whatever is under it (the deck, the table, the ground), and goes where that goes, so
  the fruit rides the table and the table rides the barge. Walk into one and push, and it slides
  the way you push it, as readily as it gives (`Loose#give`): fruit at your pace, the heavy table
  at under half of it; it slides to a stop, and stops short of anything in its way. Push one
  into another and you push both: a row of melons goes along together, front to back, heavier
  going the more there is (each one's give adds up as weights do), and a melon pushed against the
  table pushes the table; against a pillar, nothing moves. What stands on it goes with it: push
  the table and its fruit goes too. Pushed off an edge, or with what
  held it up broken, it falls; landing harder than it can take (`Loose#landing`), it breaks, so a
  watermelon dropped from three metres bursts while one off the table is fine. Furniture
  (`Furniture`) is broken like anything else: the table takes twenty rifle rounds or two strokes
  of the glaive, and what was on it drops to the deck. The stallholder's stool and the scooter
  are furniture too, and whoever sits sits on top of theirs: push the stool or the scooter and
  they ride along on it; push them, or break it, and they come off it to stand on the deck.
- **People.** The people at the stall are characters in the world (`Person`), each with a body
  like the robot's: they stand on whatever is under them, ride the barge wherever it is flown,
  and fall. Walk into someone and they are shoved out of the way, sent along at your pace and a
  little more; shove them off the edge of the deck and they fall off. A push gets anyone sitting
  to their feet, and people pushed into each other part. Rounds, the glaive and bombs strike
  them: each hit knocks them back a little and they stagger. A fall hurts them too, the harder
  they come down the more: stepping off the barge at the ground does nothing, a fall of four
  metres costs a third of them, seven or more is the end. Six rifle rounds, a stroke of the
  glaive, a bomb nearby or a long fall, and they are destroyed: they blow apart in a puff of
  fire, in their own colours, and are gone. The HUD counts the people down.

## Build and run

From the repository root:

```bash
mvn -pl avatar-demo -am package
java -jar avatar-demo/target/avatar-demo.jar
```

It opens on a choice of demos: the **playground**, everything at once, or one built round a
single vehicle, open and simple, so the vehicle is all there is to look at: the **flying
platform**, among a few pillars and gates, and the **airship**, moored by the start, with a yard of crates beyond it to
bomb (`Demo`).
Click one or press its number. In a demo, the button at the top right, or a right click
anywhere, opens a menu of them: switch straight to another, or start this one again. Tab
comes back to the choice.
`-Davatar.demo=AIRSHIP` (or `PLATFORM`, `PLAYGROUND`) goes straight to one.

One self-contained jar carries JavaFX and the OpenAL libraries. The same jar opens the sound
bench with `--bench`, or writes every sound to WAV files with `--sounds [folder]` (see
[Sound](#sound)).

## Controls

The mouse turns the avatar with no button held, as in games. The cursor is hidden and kept in
the window, so clicks stay free for actions.

| Key | On foot | In flight |
|---|---|---|
| Mouse | Turn | Turn |
| W / S | Ahead, back | Ahead, back |
| A / D | Sideways, without turning | Sideways |
| (no Shift) | Run | Full boost |
| Shift (held) | Walk | Cruise |
| Space | Jump | Climb |
| Ctrl | | Descend |
| J | Put the jet-pack on, or take it off | Take it off (and fall) |
| T | Take a platform's controls, behind its console, or board an airship, under the hatch in its gondola's floor; or let go of them (step out). At them, W/S/A/D, the mouse, Space and Ctrl fly it, and F starts or cuts a platform's engine | |
| F | Take off (wearing the pack) | Stop flying |
| Q / E (held) | Swing the camera round | Swing the camera round |
| C | Camera behind | Camera behind |
| Scroll | Zoom | Zoom |
| M | Mute or unmute | Mute or unmute |
| P | Next sound profile | Next sound profile |
| 1 | Draw the twin rifles, or put them away | Draw the twin rifles, or put them away |
| 2 | Draw the glaive, or put it away | Draw the glaive, or put it away |
| 3 | Draw the Gatling gun, or put it away | Draw the Gatling gun, or put it away |
| Left click (held) | Fire, or swing; aboard an airship, drop a bomb (held, a stick of them) | Fire, or swing |
| Esc | Free the mouse, or take it back | Free the mouse, or take it back |
| Tab | Back to the choice of demos | Back to the choice of demos |
| Right click | The menu of demos: switch to another, start this one again, or all of them (also the button at the top right) | The same |

As in most shooters, it goes at full speed unless Shift is held. `-Davatar.pack=true` starts
with the jet-pack on. Clicks fire only while the mouse is captured, so a click to take the
mouse back never fires. Aim left and right by turning the body with the mouse, and up and down
by tilting the view with it; looking round (Q/E) does not move the aim.

## Sound

Every footfall is heard, on the ground or on a block, along with each jump, landing (as hard
as the fall), bump and gate flown through, the pack, and its wings swinging. So is every round from each
rifle's muzzle, every stroke of the glaive, and each weapon as it is drawn. The Gatling gun's
motor whines up and down with its spin, and it roars while it fires. What they strike
is heard where it is struck: a round on a block, ticking or ricocheting, or thudding into the
ground; the blade clanging off a block, or chopping into the earth. What breaks is heard where
it stood: a crate or dummy splintering and its pieces clattering down, deeper the bigger it
was, a drone blowing up, or an airship coming down in a chain of blasts with its frame
groaning. Explosions are loud all through, as a blast overloads a microphone, and carry: a
footstep is heard in full only within 3 m, a drone blowing up within 15 m, a bomb within 30 m, an airship coming
down within 60 m. For a moment each one pushes the rest down (the jets, the wind, the Gatling
gun's roar), as games mix them, so it punches through. The nearest airship still in the sky
drones from where it is, faint from the ground and loud alongside it. The jets roar as
hard as they fire, and the wind rises with speed. The listener stands at the avatar but faces
the way the camera does. So the avatar always sounds near, however far the camera pulls
back, and left and right match the screen.

- **Profiles.** Every sound is made from noise, filters and tones, not recordings, in one of
  two profiles:
  - **Robot:** metal and motors, as real as can be made.
  - **Arcade:** an old console's sound chip, with square-wave blips, noise bursts, arpeggios
    and stepping pitch.

  P switches between them while playing; `-Davatar.sounds=arcade` starts with Arcade.
- **Outputs.** OpenAL (through LWJGL) is tried first. If it is missing from the build, its
  native library won't load, or there is no device, the demo falls back to Java Sound, which
  ships with Java, and then to silence. Each fallback says why. `-Davatar.audio=javasound`,
  `silent` or `record` picks the output.

### Sound bench

The sounds are worked on apart from the game, on a bench of their own:

```bash
java -jar avatar-demo/target/avatar-demo.jar --bench
```

- Choose a profile, and every sound and loop changes character with it.
- Play a sound (Space or Enter) softer or harder, take after take, from any direction and
  distance round the listener, at any pitch.
- Run the jet and wind loops, and drive them with thrust and speed sliders by the same rules
  the game uses.
- See each waveform with its length and loudness. Save one sound, or all of them, as WAV
  files.
- Switch the output while it runs. With no device, everything can still be seen and saved.

With no window or sound card at all, `--sounds [folder]` writes every sound of every profile to
WAV files, a folder for each. It also writes two scenes mixed in stereo (footsteps walking
past, the jets flying by) and says which outputs this machine can open.

## Unattended runs

A script drives the demo without a keyboard, for pictures, checks and recordings. It leaves the
real mouse alone, and plays no sound unless `-Davatar.audio` names an output.

```bash
java -Davatar.script="J;F;SPACE:1.2;W+@90:2" -Davatar.snapshot=out.png -jar avatar-demo/target/avatar-demo.jar
```

- **Parts** are separated by `;`. Each holds its keys (joined by `+`) for its seconds, so
  `W+SHIFT:1.5` walks ahead for a second and a half. A part without seconds is a tap, and
  `:1` just waits. An `@90` among a part's keys turns 90° over the part, as the mouse would,
  and `^20` tilts the view up 20° (down, when negative).
  `FIRE` holds the trigger, as the left mouse button would, so `DIGIT1;FIRE:1` fires the
  rifles for a second.
- **It plays in the world's own time.** The script becomes inputs stamped with the step they
  come in at (`Script`), 120 to the second, starting half a second in; while it plays, it alone
  drives the world. So the same script gives the same run every time, however the frames fall,
  and the tests play scripts with no window at all. The keys mean the same to a script as at
  the keyboard (`Keys`).
- **`-Davatar.inputs=run.inputs`** keeps the run's inputs as it goes, whether from the keyboard
  and mouse or a script (`Recording`), and **`-Davatar.replay=run.inputs`** plays them back from
  where the run started: the same run, to the bit, seen and heard as it was. Only the inputs are
  kept, a line when they change, so a key held for a minute is one line; what follows from them
  (where the avatar went, what broke) is stepped again. A checksum of the world every second
  tells the replay it is still the same run, or where it first went astray (the rules changed,
  say, or the file was edited). The camera's own keys are not kept: they change nothing in the
  world.
- **Frame times** are printed once a second (`frames: 60 fps | frame 16.7 ms, worst 32.1 | update
  0.4 ms, worst 0.7`), as the HUD shows them: how many frames, the average and worst gap between
  them, and how long the demo's own work took in each, apart from JavaFX's drawing. The worst is
  what is felt as a stutter. On the HUD the line turns amber below 55 fps, or when a frame takes
  twice as long as it should.
- **The demo** a script plays in is the playground, or `-Davatar.demo` names another. A
  recording keeps the demo it was made in, and its replay plays there.
- **`-Davatar.snapshot=out.png`** saves the window just before the script ends, and exits.
  `-Davatar.settle=<ms>` picks the moment instead, counted in the world's time.
- **`-Davatar.audio=record -Davatar.record=flight.wav`** writes a stereo WAV of exactly what
  would have played, kept in time with the game. It is for hearing a run afterwards, or
  checking it without speakers:

```bash
java -Davatar.audio=record -Davatar.record=flight.wav -Davatar.script="W+SHIFT:1.6;J:0.6;F:0.3;SPACE:1.2;W:2.5;CONTROL:3.5;:1" -Davatar.snapshot=flight.png -jar avatar-demo/target/avatar-demo.jar
```

## How the code is layered

One simulation is the single truth. Each output reads it, the graphics and the sound alike,
and none writes to it. So the sound could be added without touching the graphics, and every
rule of the world can be tested without a window or a sound card.

The simulation keeps its own time (`Clock`), apart from the frame rate. It always moves in
steps of exactly 1/120 s: two for each frame at 60 a second, more or fewer as the frames run
short or long, with what is left over carried on to the next frame. So the world comes out the
same, to the last bit, at 30, 60 or 144 frames a second, or an uneven mix of them. What is seen
and heard is the world between its last two steps, blended by how far on the next one is, so it
glides rather than ticking. A frame that runs very long takes no more than a tenth of a second
of steps, and lets the rest go rather than falling ever further behind.

| Package | What it is |
|---|---|
| `sim` | The one truth, stepped on its own clock (`Clock`; `Play` steps the avatar, the damage and what moves together: the airships on their routes and their bombs, `Sky`, and the platforms moved from their consoles, `Lift`, with what rides on them; the loose things, furniture and fruit, `Prop`; and the people, `Person`; whoever stands on what moves is carried with it), from inputs: what the keys mean to it (`Keys`), from the keyboard or a `Script` stamped by step, kept and played back as a `Recording`. What happened in each step is written to a `Journal`, stamped by step, which the effects and the sound each read through a reader of their own; the simulation never reads it. The avatar (`Avatar`, `Body`), its swing wings (`Wings`), and what it holds (`Weapon`; in `Arms`, the rounds and strokes, and what they strike). The world: `Course`, `Block`, `Obstacles` (and rays into it, meeting a `Hit` on a numbered piece), the gates' openings (`Gate`), and what the weapons have done to it (`Damage`: how worn each piece is, what is broken, and what still stands). The `Event`s each step reports: footfalls and landings (how hard, and on the ground or a block), jumps, bumps, take-offs, the pack, and each gate flown through, each round fired and stroke swung, what each struck, and what broke. Pure maths, with its own `Vec3`: no JavaFX at all |
| `view` | The viewpoint (`Chase`): the camera for the graphics, and which way the listener faces. Pure |
| `look` | The graphics: `Gait` turns the simulation into a `Pose`, weapon holds and strokes included, with `Reach` working out an arm to put its hand on a point (the Gatling gun's lever); `BoxRig`, `JetPack`, `Arsenal` (the weapons, their flashes and casings), `Impacts` (tracers, sparks, dust, marks), `Wreckage` (the pieces of what broke, a drone's fireball, a bomb going off), `Bombs` (those falling), `Stall` (the people, standing, sat or knocked down, and how they react), `Reticle` (the crosshair, out in the world) and `World` (the course, the targets, their wear and the bars over them) draw it in JavaFX |
| `sound` | What is heard. `Sound` lists what can be heard. A `Profile` decides how all of it sounds, and how the loops follow thrust and speed: `Robot` or `Arcade` (`Profiles`). `Cues` turns each frame's events, thrust and speed into what to play and from where. Pure: no JavaFX, no sound library |
| `audio` | Where it comes out (`Speakers`): OpenAL through LWJGL, Java Sound, or silence, whichever opens (`Outputs`). Or, only when asked for, a recording to a WAV file (`RecordingSpeakers`). One spatial model (`Space`) serves all. For Java Sound and the recording, a software mixer (`Mix`) places and mixes the voices. No JavaFX, and only `OpenAlSpeakers` touches LWJGL |
| `hear` | The game's sound, as `look` is its graphics (`Hearing`). Each frame it plays what `Cues` says, keeps the clips it has made, and steers the loops. No JavaFX |
| `bench` | The sound bench (`SoundBench`) and the WAV renderer (`Render`) |
| (root) | The app: input (`MouseLook`, keys), the frame loop, the HUD |

All but the drawing, the devices and the app is unit-tested, and a test (`LayersTest`) keeps
each layer to these imports.
