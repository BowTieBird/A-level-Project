float W = /*/ 900; /*/1200;//720;
float H = /*/ 600; /*/800; //480;
boolean FULL_SCREEN = false;
boolean DISPLAY_HELP_ON_STARTUP = false;
int MEMORY_LENGTH = 4;
float EDGE_BOUNDARY = 10;
float SIZE_MULT = 1.0;

color bg = color(49, 52, 63);
color pauseOverlayColor = color(49, 52, 63, 100);
color helpOverlayColor = color(49, 52, 63, 200);

String font = "Lucida Sans Typewriter Regular";

boolean paused = false;
boolean cheekyDevMode = false;
int helpMode = DISPLAY_HELP_ON_STARTUP ? 1 : 0; 
int helpMax = 8;

Draggable dragging;
Selectable selected;
Typable typing;

RAM ram;
CPU cpu;
Output out;
Input inp;
ALU alu;
Accumulator acc;
ProgramCounter pc;

ParticleManager particles;
HardwareManager hardware;
ScriptManager scripts;
TagManager tags;

Drawer drawer;
ControlPanel control;

void settings() { 
  if (FULL_SCREEN) {
    W = displayWidth/SIZE_MULT;
    H = displayHeight/SIZE_MULT;
    fullScreen();
  } else size(int(W*SIZE_MULT), int(H*SIZE_MULT));
}

void setup() {
  noStroke();
  textFont(createFont(font, 60));

  particles = new ParticleManager();
  hardware = new HardwareManager();
  scripts = new ScriptManager();
  tags = new TagManager();

  drawer = new Drawer();
  control = new ControlPanel();

  saveAllStates("new");
}

void draw() {
  background(bg);

  scale(SIZE_MULT);

  if (!paused) doFrame();

  hardware.show();
  scripts.show();
  tags.show();
  particles.show();

  drawer.show();
  control.show();

  if (paused) pauseOverlay();
  if (helpMode > 0) helpOverlay();

  if (dragging instanceof Script) dragging.show();
  if (dragging instanceof Tag) dragging.show();

  if (cheekyDevMode) devFrame();
}

void doFrame() {
  scripts.run();
  hardware.run();
  particles.run();
}

void pauseOverlay() {
  fill(pauseOverlayColor);
  rect(0, 0, W, H);
  fill(255, 200);
  textAlign(CENTER, CENTER);
  text("Paused", W/2, H/2);
  textSize(12);
  text("Press 'P' to resume", W/2, H/2+40);
}

void helpOverlay() {
  fill(helpOverlayColor);
  rect(0, 0, W, H);
  control.showHelp();
  switch (helpMode) {
  case 1:
  case 2:
    drawer.helpOverlay(helpMode);
    break;
  case 3:
  case 4:
  case 5:
  case 6:
    hardware.helpOverlay(helpMode);
    break;
  case 7:
    control.helpOverlay();
    break;
  }
}

void mousePressed() {
  if (selected != null) selected.deSelect();
  control.mousePress();
  drawer.mousePress();
  tags.mousePress();
  scripts.mousePress();
  hardware.mousePress();
}

void mouseDragged() {
  if (dragging != null) dragging.drag();
}

void mouseReleased() {
  if (dragging != null) dragging.release();
}

void keyPressed() {
  if (key == ENTER) {
    if (selected == inp) inp.input();
    if (selected == control) control.enter();
    if (selected != null) selected.deSelect();
  } else if (key != CODED) {
    if (typing != null) typing.type(key);
    if (selected == scripts) scripts.type();
    if (selected == tags) tags.type();
    if (selected == control) control.type();
    if (selected == null) {
      if (key == 'p' || key == ' ') pause();
      if (key == 'c') ram.assemble();
      if (key == 'r') pc.step();
      if (key == 'h') help();
      if (key == '.') devMode();
    }
  }
}

void pause() {
  paused ^= true;
}

void help() {
  helpMode++;
  helpMode %= helpMax;
}

void mouseWheel(MouseEvent event) {
  float s = event.getCount();
  drawer.scroll(s);
  scripts.scroll(s);
}

void saveAllStates(String filename) {
  ArrayList<String> states = new ArrayList<String>();
  states.addAll(hardware.saveStates());
  states.addAll(scripts.saveStates());
  states.addAll(particles.saveStates());
  println(states);
  saveStrings(filename + ".txt", states.toArray(new String[0]));
}

void loadAllStates(String filename) {
  try {
    ArrayList<String> states = new ArrayList<String>();
    for (String str : loadStrings(filename + ".txt")) states.add(str);
    println(states);
    hardware.loadStates(states);
    tags.loadStates(states);
    scripts.loadStates(states);
    //particles.loadStates(states);
  } 
  catch(Exception e) {
    println(e);
  }
}
