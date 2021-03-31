import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Avengers_4_1_1 extends PApplet {

float W = /*/ 900; /*/1200;//720;
float H = /*/ 600; /*/800; //480;
boolean FULL_SCREEN = false;
boolean DISPLAY_HELP_ON_STARTUP = false;
int MEMORY_LENGTH = 4;
float EDGE_BOUNDARY = 10;
float SIZE_MULT = 1.0f;

int bg = color(49, 52, 63);
int pauseOverlayColor = color(49, 52, 63, 100);
int helpOverlayColor = color(49, 52, 63, 200);

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

public void settings() { 
  if (FULL_SCREEN) {
    W = displayWidth/SIZE_MULT;
    H = displayHeight/SIZE_MULT;
    fullScreen();
  } else size(PApplet.parseInt(W*SIZE_MULT), PApplet.parseInt(H*SIZE_MULT));
}

public void setup() {
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

public void draw() {
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

public void doFrame() {
  scripts.run();
  hardware.run();
  particles.run();
}

public void pauseOverlay() {
  fill(pauseOverlayColor);
  rect(0, 0, W, H);
  fill(255, 200);
  textAlign(CENTER, CENTER);
  text("Paused", W/2, H/2);
  textSize(12);
  text("Press 'P' to resume", W/2, H/2+40);
}

public void helpOverlay() {
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

public void mousePressed() {
  if (selected != null) selected.deSelect();
  control.mousePress();
  drawer.mousePress();
  tags.mousePress();
  scripts.mousePress();
  hardware.mousePress();
}

public void mouseDragged() {
  if (dragging != null) dragging.drag();
}

public void mouseReleased() {
  if (dragging != null) dragging.release();
}

public void keyPressed() {
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

public void pause() {
  paused ^= true;
}

public void help() {
  helpMode++;
  helpMode %= helpMax;
}

public void mouseWheel(MouseEvent event) {
  float s = event.getCount();
  drawer.scroll(s);
  scripts.scroll(s);
}

public void saveAllStates(String filename) {
  ArrayList<String> states = new ArrayList<String>();
  states.addAll(hardware.saveStates());
  states.addAll(scripts.saveStates());
  states.addAll(particles.saveStates());
  println(states);
  saveStrings(filename + ".txt", states.toArray(new String[0]));
}

public void loadAllStates(String filename) {
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
class Block extends TextBox { 
  float blockWidth = 60;
  float blockHeight = 25;
  float textX = 9, textY = 2;
  float tagY = -8; 

  int mg = color(255);
  int hl = color(227, 232, 41);

  int baseCode;
  int min = 3;

  int regIndex;
  public int getRegIndex() { return regIndex; }
  public void setRegIndex(int r) { regIndex = r; }
  
  String helpText;

  public float getTotalHeight() { return h + slot.getHeight(); }

  Slot slot;
  Tab tab;
  Notch notch;
  Tag tag;

  public Slot getSlot() { return slot; }
  public Tab getTab() { return tab; }
  public Notch getNotch() { return notch; }
  public Tag getTag() { return tag; }

  Block(String mnemonic, int code, int bg, String help) {
    setText(mnemonic);
    setTextColor(mg);
    setPadding(textY);
    setLeftAlign(LEFT);
    setTextOffset(textX);

    baseCode = code;
    setBackground(bg);

    setSize(blockWidth, blockHeight);
    helpText = help;

    slot = new Slot(this);
    tab = new Tab(this);
    notch = new Notch(this);
  }

  public Block copy() {
    Block newBlock = new Block(getText(), baseCode, bg, helpText);
    newBlock.setPos(x, y);
    newBlock.updateWidth();
    if (tag != null) newBlock.addTag(tags.addTag(tag.copy()));
    return newBlock;
  }

  public void setPos(float x, float y) {
    super.setPos(x, y);
    slot.updatePos();
    tab.updatePos();
    notch.updatePos();
    if (tag != null) tag.updatePos();
  }

  public void updateWidth() {
    slot.updateWidth();
  }

  public void show() {
    super.show();
    slot.show();
    tab.show();
    if (tag == null) { 
      if (notch.mouseOver()) notch.show();
    } else tag.show();
  }

  public boolean mouseOver() {
    return (super.mouseOver() || slot.mouseOver() || tab.mouseOver());
  }

  public void assembleTag(ArrayList<Tag> blockTags) {
    assemble(0);
  }

  public void assemble(int k) {
    MemoryAddress r =  ram.getRegister(regIndex);
    r.setWillHaveData(true);
    r.assemble(setUpNewParticle());
  }

  public Particle setUpNewParticle() {
    Particle newParticle = new Particle(getCode());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(bg);
    return newParticle;
  }

  public String getCode() { 
    return nf(baseCode, min);
  }

  public Block checkForTag(Tag current) {
    if (tag == null && current.isNear(notch)) return this;
    return null;
  }

  public Tag addTag(Tag current) {
    tag = current;
    tag.setParent(this);
    tag.setOffset(-tag.getTotalWidth(), tagY);
    tag.updatePos();
    return tag;
  }

  public Tag makeNewTag() {
    if (tag == null && notch.mouseOver()) {      
      return tags.addTag(addTag(new Tag(this)));
    } else return null;
  }

  public void updateTag(Tag t) {
    if (t == tag) tag.setOffsetX(-tag.getTotalWidth());
  }

  public void detach(Tag t) {
    if (t == tag) tag = null;
  }

  public void change(float s) {
    // TODO
  }

  public void run() {
  }

  public Script getScript() {
    return (Script)parent;
  }

  public boolean hasStartTag() {
    return tag instanceof StartTag;
  }

  public ArrayList<String> saveState(int k) {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#Block " + k);
    state.addAll(super.saveState());
    state.add(getText());
    if (tag != null) {
      state.add("#Tag " + k);
      if (tag instanceof StartTag) state.add("#StartTag " + k);
      else state.add(tag.getText());
    }
    return state;
  }
  
  public void helpOverlay() {
    text(helpText, x + w, cy);
  }
}

class OperandBlock extends Block {
  Typable operand = new Typable();
  float opPadding = 3;
  float opOffset = 55;

  float textX = 5;

  Tag opTag;

  SnapPoint attach;
  ParticleAttractor tagAssemble;
  
  public String getOpText() {
    return (opTag == null) ? operand.getText() : opTag.getText();
  }

  OperandBlock(String m, int code, int bg, String help) {
    super(m, code, bg, help);
    setTextOffset(textX);
    setUpOperand();
  }

  public void setUpOperand() {    
    operand.setParent(this);    
    operand.setHeight(h + slot.getHeight() - 2*opPadding);
    operand.setOffset(opOffset, opPadding - slot.getHeight());
    operand.setPadding(opPadding);
    operand.setTextOffset(opPadding);

    attach = new SnapPoint(this);
    tagAssemble = new ParticleAttractor(operand);

    updateWidth();
  }

  public void setPos(float x, float y) {
    super.setPos(x, y);
    operand.updatePos();
    attach.setCenter(operand.getCenterX(), operand.getCenterY());
    if (opTag != null) opTag.updatePos();
  }

  public Block copy() {
    OperandBlock newBlock = new OperandBlock(getText(), baseCode, bg, helpText);
    newBlock.setPos(x, y);
    newBlock.setOperandText(operand.getText());
    newBlock.updateWidth();
    if (tag != null) newBlock.addTag(tags.addTag(tag.copy()));
    if (opTag != null) newBlock.addOpTag(tags.addTag(opTag.copy()));
    return newBlock;
  }

  public void setOperandText(String contents) {
    operand.setText(contents);
  }

  public void show() {
    super.show();
    operand.show();
    if (opTag != null) opTag.show();
  }

  public void select() {
    if (opTag == null) operand.select();
    super.select();
  }

  public void deSelect() {
    operand.deSelect();
    super.deSelect();
  }

  public void updateWidth() {
    if (opTag == null) operand.updateLength();
    else operand.setWidth(opTag.getWidth());
    setWidth(opOffset + operand.getWidth() + opPadding);
    slot.updateWidth();
    attach.setCenter(operand.getCenterX(), operand.getCenterY());
  }

  public Particle setUpNewParticle() {
    Particle newParticle = super.setUpNewParticle();
    newParticle.setPos(operand.getCenterX(), operand.getCenterY());
    return newParticle;
  }

  public String getCode() {
    return nf(baseCode + operand.getIntText(), min);
  }

  public Tag addOpTag(Tag current) {
    opTag = current;
    opTag.setParent(this);
    opTag.setOffset(opOffset, opPadding - slot.getHeight());
    operand.setText("");
    updateWidth();
    opTag.updatePos();
    return tag;
  }

  public void assembleTag(ArrayList<Tag> blockTags) {
    String op = getOpText();
    for (Tag tag : blockTags) {
      if (tag.getText().equals(op)) {
        tagAssemble.addParticle(tag.setUpNewParticle());
        return;
      }
    }
    assemble(PApplet.parseInt(op));
  }

  public void assemble(int o) {
    Particle p = setUpNewParticle();
    p.setData(nf(baseCode + o, min));
    MemoryAddress r =  ram.getRegister(regIndex);
    r.setWillHaveData(true);
    r.assemble(p);
  }

  public void run() {
    if (tagAssemble.gotInput()) assemble(tagAssemble.input);
  }

  public OperandBlock checkAttach(Tag current) {
    if (opTag == null && current.isNear(attach)) return this;
    return null;
  }
  
  public void updateTag(Tag t) {
    super.updateTag(t);
    if (t == opTag) updateWidth();
  }

  public void detach(Tag t) {
    super.detach(t);
    if (t == opTag) opTag = null;
  }

  public ArrayList<String> saveState(int k) {
    ArrayList<String> state = new ArrayList<String>();
    state.addAll(super.saveState(k));
    if (opTag != null) {
      state.add("#OperandTag " + k);
      if (tag instanceof StartTag) state.add("#OperandStartTag " + k);
      else state.add(opTag.getText());
    }
    state.add("#Operand " + k);
    state.add(operand.getText());
    return state;
  }
}
class CPU extends Hardware {
  int bg = color(230, 230, 10);
  int fg = color(205, 46, 0, 180);
  float startX = 225, startY = H-270;
  float startWidth = 180, startHeight = 190;

  float registerHeight = 25;
  float registerOffset = 30;
  float registerShelfHeight = 50;
  float registerLabelSize = 10;
  float registerLabelOffset = 55;

  float labelSize = 20;
  float labelOffset = 20;

  ArrayList<TextBox> registers = new ArrayList<TextBox>();
  TextBox CIR = new FTextBox(1);
  TextBox MAR = new FTextBox(2);
  TextBox MDR = new FTextBox(MEMORY_LENGTH);
  public int getCIRValue() { return CIR.getIntText(); }

  int dataColor = color(254, 3, 234);
  int addressColor = color(245, 10, 10);
  int instructionColor = color(201, 8, 106);

  boolean instructionLoaded = false;

  ParticleAttractor returnCount = new ParticleAttractor(MAR);
  ParticleAttractor returnInstruction = new ParticleAttractor(MDR);
  ParticleAttractor loadAddress = new ParticleAttractor(MAR);
  ParticleAttractor loadInstruction = new ParticleAttractor(CIR);
  ParticleAttractor returnData = new ParticleAttractor(MDR);
  ParticleAttractor checkForBranch = new ParticleAttractor(MDR);

  CPU() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);

    setUpRegisters();

    setUpLabel();
    updatePos();
  }

  public void setUpRegisters() {
    setUpCIR();
    setUpMAR();
    setUpMDR();
    float registerY = -registerShelfHeight;
    for (TextBox register : registers) {
      register.setParent(this);
      register.setHeight(registerHeight);
      register.updateLength();
      register.setOffset(registerOffset, registerY);
      register.setLabelLeftAlign(LEFT);
      register.setLabelOffset(registerLabelOffset);
      register.setLabelSize(registerLabelSize);
      register.setLabelColor(fg);
      registerY += registerShelfHeight;
    }
  }

  public void setUpCIR() {
    CIR.setLabel("Current" + "\n" + "Instruction" + "\n" + "Register");
    registers.add(CIR);
  }

  public void setUpMAR() {
    MAR.setLabel("Memory Address" + "\n" + "Register");
    registers.add(MAR);
  }

  public void setUpMDR() {
    MDR.setLabel("Memory Data" + "\n" + "Register");
    registers.add(MDR);
  }

  public void setUpLabel() {
    setLabel("CPU");
    setLabelTopAlign(TOP);
    setLabelSize(labelSize);
    setLabelOffset(labelOffset);
    setLabelColor(fg);
  }

  public void show() {
    super.show();
    for (TextBox register : registers) register.show();
  }

  public void drag() {
    super.drag();
    updatePos();
  }

  public void updatePos() {
    for (TextBox register : registers) {
      register.setX(cx);
      register.setCenterY(cy + labelOffset);
    }
  }

  public void release() {
    select();
    super.release();
  }

  public void returnCount(Particle p) {
    returnCount.addParticle(p);
  }

  public void returnInstruction(Particle p) {
    returnInstruction.addParticle(p);
  }

  public void returnData(Particle p) {
    returnData.addParticle(p);
  }

  public void checkForBranch(Particle p) {
    checkForBranch.addParticle(p);
  }

  public void fetchInstruction(int c) {
    MAR.setText(c);
    ram.getRegister(c).fetchInstruction(setUpParticle(MAR, addressColor));
  }

  public void decode(int d) {
    MDR.setText(d);
    loadAddress.addParticle(setUpParticleWithData(MDR, addressColor, nf(d % 100, 2)));
    loadInstruction.addParticle(setUpParticleWithData(MDR, instructionColor,  nf(d / 100, 1)));
  }

  public void loadAddress(int d) {
    MAR.setText(d);
    checkForExecute();
  }

  public void loadInstruction(int d) {
    CIR.setText(d);
    checkForExecute();
  }

  public void checkForExecute() {
    if (instructionLoaded) execute();
    instructionLoaded ^= true;
  }

  public void execute() {
    int instruction = CIR.getIntText();
    switch (instruction) {
    case 1: //ADD
    case 2: //SUBTRACT
    case 5: //LOAD ADDRESS
      fetchData();
      break;
    case 3: //STORE
      store();
      break;
    case 6: //BRANCH
      branch();
      break;
    case 7: //BRANCH IF ZERO
    case 8: //BRANCH IF POSITIVE
      acc.checkForBranch();
      break;
    case 9:
      int address = MAR.getIntText();
      switch (address) {
      case 01:
        input();
        break;
      case 02:
        output();
        break;        
      }
      break;
    default: //HALT
      break;
    }
  }
  
  public void fetchData() {
    MemoryAddress address = ram.getRegister(MAR.getIntText());
    address.fetchData(setUpParticle(MAR, addressColor));
  }
  
  public void calc() {
    acc.calcAcc();
    alu.calcData(setUpParticle(MDR, dataColor));
  }
  
  public void load() {
    acc.load(setUpParticle(MDR, dataColor));
  }
  
  public void store() {
    acc.store(setUpParticle(MAR, addressColor));
  }
  
  public void branch() {
    pc.branch(setUpParticle(MAR, addressColor));
  }
  
  public void checkForBranch(int b) {
    MDR.setText(b);
    int instruction = CIR.getIntText();
    if (instruction == 7) branchIf(b == 0);
    if (instruction == 8) branchIf(b > 0);
  }
  
  public void branchIf(boolean condition) {
    if (condition) branch();
    else pc.step();
  }
  
  public void input() {
    inp.input(setUpParticleWithData(CIR, instructionColor, "INP"));
  }
  
  public void output() {
    acc.output();
  }

  public void run() {
    if (returnCount.gotInput()) fetchInstruction(returnCount.input());
    if (returnInstruction.gotInput()) decode(returnInstruction.input());
    if (loadAddress.gotInput()) loadAddress(loadAddress.input());
    if (loadInstruction.gotInput()) loadInstruction(loadInstruction.input());
    if (returnData.gotInput()) {
      MDR.setText(returnData.input());
      if (getCIRValue() == 5) load();
      else calc();
    }
    if (checkForBranch.gotInput()) checkForBranch(checkForBranch.input());
  }

  public Particle setUpParticle(TextBox r, int c) {
    Particle newParticle = new Particle(r.getText());
    newParticle.setPos(r.getCenterX(), r.getCenterY());
    newParticle.setBackground(c);
    return newParticle;
  }
  
  public Particle setUpParticleWithData(TextBox r, int c, String d) {
    Particle newParticle = setUpParticle(r, c);
    newParticle.setData(d);
    return newParticle;
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#CPU");
    states.addAll(super.saveState());
    for (TextBox register : registers) states.add(register.getText());
    return states;
  }
  
  public void loadState(ArrayList<String> states) {
    int index = states.indexOf("#CPU");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 10));
    super.loadState(state);
    CIR.setText(state.get(6));
    MAR.setText(state.get(7));
    MDR.setText(PApplet.parseInt(state.get(8)));
    updatePos();
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, BOTTOM);
    text("\\/", cx, y);
    text("The CPU is the brains of the computer.", cx, y - textAscent());
    textAlign(LEFT, CENTER);
    text(" <- The CIR stores the current instruction.", x + w, CIR.getCenterY());
    text(" <- The MAR stores the address of a register in the RAM.", x + w, MAR.getCenterY());
    text(" <- The MDR stores the data from the address.", x + w, MDR.getCenterY());
  }
}
class ScriptManager extends Selectable {
  ArrayList<Script> scripts = new ArrayList<Script>();
  
  Script selectedScript;

  public void show() {
    for (Script script : scripts) if (!script.isLocked()) script.show();
  }

  public Script addNewScript(ArrayList<Block> blocks) {
    Script newScript = new Script(blocks);
    scripts.add(newScript);
    return newScript;
  }

  public void mousePress() {
    if (dragging == null) {
      for (int i = scripts.size() - 1; i >= 0; i--) {
        Script current = scripts.get(i);
        if (current.makeNewTag() == null) {
          if (current.mouseOver()) {
            if (mouseButton == RIGHT) {
              Script duplicate = current.duplicate();
              duplicate.drag();
            } else {
              if (current.getMouseOver() > 0) current.splitScript();
              current.drag();
            }
            break;
          }
        } else break;
      }
    }
  }

  public void release(Script current) {
    scripts.remove(current);
    if (!drawer.toBin()) {
      Script above = checkAbove(current);
      Script below = checkBelow(current);
      if (above != null) above.addAtBottom(current);
      else if (below != null) below.addAtTop(current);
      else scripts.add(current);
    }
  }
  
  public void scroll(float s) {
    for (Script script : scripts) {
      if (script.mouseOver()) {
        script.scroll(s);
        break;
      }
    }
  }
  
  public void select(Script current) {
    selectedScript = current;
    current.select();
    super.select();
  }
  
  public void deSelect() {
    selectedScript.deSelect();
    super.deSelect();
  }
  
  public void type() {
    selectedScript.updateWidth();
  }

  public Script checkAbove(Script current) {
    for (Script other : scripts) if (current != other) if (current.getTop().isNear(other.getBottom())) return other;
    return null;
  }

  public Script checkBelow(Script current) {
    for (Script other : scripts) if (current != other) if (current.getBottom().isNear(other.getTop())) return other;
    return null;
  }
  
  public Block checkForTag(Tag current) {
    for (Script other : scripts) {
      Block found = other.checkForTag(current);
      if (found != null) return found;
    }
    return null;
  }
  
  public OperandBlock checkAttach(Tag current) {
    for (Script other : scripts) {
      OperandBlock found = other.checkAttach(current);
      if (found != null) return found;
    }
    return null;
  }
  
  public void run() { for(Script script : scripts) script.run(); }
  
  public void assemble() {
    ArrayList<Tag> blockTags = new ArrayList<Tag>();
    Script start = getStartingScript();
    if (start != null) start.getTags(0);
    for (Script script : scripts) if (script != start) blockTags.addAll(script.getTags(ram.nextRegister()));
    if (start != null) start.assemble(blockTags);
    for (Script script : scripts) if (script != start) script.assemble(blockTags);
  }
  
  public Script getStartingScript() {
    for (Script script : scripts) if (script.isStartingScript()) return script;
    return null;
  }
  
  public ArrayList<String> saveStates() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#Scripts");
    int n = scripts.size();
    states.add(str(n));
    for (int k = 0; k < n; k++) states.addAll(scripts.get(k).saveState(k));
    return states;
  }
  
  public void loadStates(ArrayList<String> states) {
    scripts = new ArrayList<Script>();
    int k = 0;
    while (states.contains("#Script " + k)) {
      int startIndex = states.indexOf("#Script " + k);
      int endIndex = states.contains("#Script " + (k+1)) ? states.indexOf("#Script " + (k+1)) : states.size();
      ArrayList<String> state = new ArrayList<String>(states.subList(startIndex + 1, endIndex));
      Script script = addNewScript(loadNewScript(state, k));
      script.loadState(state);
      k++;
    }
  }
  
  public ArrayList<Block> loadNewScript(ArrayList<String> states, int k) {
    ArrayList<Block> blocks = new ArrayList<Block>();
    int dk = 0;
    while (states.contains("#Block " + dk)) {
      int startIndex = states.indexOf("#Block " + dk);    
      ArrayList<String> state = new ArrayList<String>(states.subList(startIndex + 1, states.size()));
      Block block = drawer.loadNewBlock(state, dk);
      if (state.contains("#Operand " + dk)) {
        int index = state.indexOf("#Operand " + dk);
        OperandBlock opBlock = (OperandBlock)block;
        opBlock.setOperandText(state.get(index + 1));
        opBlock.updateWidth();
        blocks.add(opBlock);
        if (state.contains("#OperandTag " + dk)) {
          int opIndex = state.indexOf("#OperandTag " + dk);
          if (state.contains("#OperandStartTag " + dk)) opBlock.addOpTag(tags.addTag(new StartTag(block)));
          else {
            Tag newTag = new Tag(opBlock);
            newTag.setText(state.get(opIndex + 1));
            newTag.updateWidth();
            opBlock.addOpTag(tags.addTag(newTag));
          }
        }
      } else blocks.add(block);
      if (state.contains("#Tag " + dk)) {
        int index = state.indexOf("#Tag " + dk);
        if (state.contains("#StartTag " + dk)) block.addTag(tags.addTag(new StartTag(block)));
        else {
          Tag newTag = new Tag(block);
          newTag.setText(state.get(index + 1));
          newTag.updateWidth();
          block.addTag(tags.addTag(newTag));
        }
      }
      dk++;
    }
    return blocks;
  }
}

class TagManager extends Selectable {
  ArrayList<Tag> tags = new ArrayList<Tag>();
  
  Tag selectedTag;

  public void show() {
    for (Tag tag : tags) if (!tag.isLocked() && !tag.hasParent()) tag.show();
  }

  public Tag addTag(Tag t) {
    tags.add(t);
    return t;
  }

  public void mousePress() {
    if (dragging == null) {
      for (int i = tags.size() - 1; i >= 0; i--) {
        Tag current = tags.get(i);
        if (current.mouseOver()) {
          if (mouseButton == RIGHT) {
            Tag copy = current.copy();
            if (copy != null) addTag(copy).drag();
          } else {
            current.detach();
            current.drag();
          }
          break;
        }
      }
    }
  }

  public void release(Tag current) {
    if (!drawer.toBin()) {
      Block tagBlock = scripts.checkForTag(current);
      OperandBlock attachBlock = scripts.checkAttach(current);
      if (tagBlock != null) tagBlock.addTag(current);
      else if (attachBlock != null) attachBlock.addOpTag(current);
    } else tags.remove(current);
  }
  
  public void select(Tag current) {
    selectedTag = current;
    current.select();
    super.select();
  }
  
  public void deSelect() {
    selectedTag.deSelect();
    super.deSelect();
  }
  
  public void type() {
    selectedTag.updateWidth();
  }
  
  public ArrayList<String> saveStates() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#Tags");
    for (Tag tag : tags) states.addAll(tag.saveState());
    return states;
  }
  
  public void loadStates(ArrayList<String> states) {
    tags = new ArrayList<Tag>();
    int k = 0;
    while (states.contains("#EmptyTag " + k)) {
      //int startIndex = states.indexOf("#EmptyTag " + k);
      //ArrayList<String> state = new ArrayList<String>(states.subList(startIndex + 1, states.size()));
      //Tag tag = new Tag(null);
      //tag.loadState(state);
      //k++;
    }
  }
}

class HardwareManager {
  ArrayList<Hardware> hardware = new ArrayList<Hardware>();
  
  HardwareManager() {
    hardware.add(ram = new RAM());
    hardware.add(cpu = new CPU());
    hardware.add(out = new Output());
    hardware.add(inp = new Input());
    hardware.add(alu = new ALU());
    hardware.add(acc = new Accumulator());
    hardware.add(pc  = new ProgramCounter());
  }
  
  public void run() { for (Hardware component : hardware) component.run(); }
  
  public void show() { for (Hardware component : hardware) component.show(); }
  
  public void mousePress() {
    for (int i = hardware.size()-1; i >= 0; i--) hardware.get(i).mousePress();
  }
  
  public void reset() {
    particles.clearAll(); // What do I want to reset???
    for (Hardware component : hardware) component.reset();
  }
  
  public ArrayList<String> saveStates() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#Hardware");
    for (Hardware component : hardware) states.addAll(component.saveState());
    return states;
  }
  
  public void loadStates(ArrayList<String> states) {
    for (Hardware component : hardware) component.loadState(states);
  }
  
  public void helpOverlay(int helpMode) {
    if (helpMode == 3) {
      ram.helpOverlay();
      acc.helpOverlay();
    } else if (helpMode == 4) {
      cpu.helpOverlay();
    } else if (helpMode == 5) {
      pc.helpOverlay();
      alu.helpOverlay();
    } else if (helpMode == 6) {
      inp.helpOverlay();
      out.helpOverlay();
    }
  }
}

class ParticleManager {
  ArrayList<ParticleAttractor> attractors = new ArrayList<ParticleAttractor>();
  public void clearAll() { for (ParticleAttractor p : attractors) p.clearAll(); }
  
  float particleSpeed = 0.5f;
  public float getParticleSpeed() { return particleSpeed; }
  public void setParticleSpeed(float s) { particleSpeed = s; }
  
  public void run() {
    setParticleSpeed(control.getParticleSpeed());
    for (ParticleAttractor p : attractors) p.run();
  }
  
  public void show() { for (ParticleAttractor p : attractors) p.show(); }
  
  public void addAttractor(ParticleAttractor p) { attractors.add(p); }
  
  public ArrayList<String> saveStates() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#ParticleAttractors");
    //for (ParticleAttractor p : attractors) states.addAll(p.saveStates());
    return states;
  }
}

class ParticleAttractor extends Selectable {
  ArrayList<Particle> attractedParticles = new ArrayList<Particle>();
  
  public void clearAll() { attractedParticles.clear(); }
  
  int input;
  Slate target;
  
  ParticleAttractor(Slate _target) {
    particles.addAttractor(this);
    target = _target;
  }
  
  public Slate getTarget() { return target; }
  
  public void addParticle(Particle particle) {
    particle.setTarget(this);
    attractedParticles.add(particle);
  }
  
  public void run() { for (Particle p : attractedParticles) p.update(); }
  
  public void show() { for (Particle p : attractedParticles) p.show(); }
  
  public PVector targetGravity() {
    return new PVector(target.getCenterX(), target.getCenterY());
  }
  
  public float distance(Particle p) { return dist(p.pos.x, p.pos.y, target.getCenterX(), target.getCenterY()); }
  
  public Particle getInput() {
    for (Particle particle : attractedParticles) if (particle.arrived()) return particle;
    return null;
  }
  
  public boolean gotInput() {
    for (Particle particle : attractedParticles) if (particle.arrived()) {
      input = particle.getIntData();
      removeParticle(particle);
      return true;
    }
    return false;
  }
  
  public int input() { return input; }
 
  public void removeParticle(Particle particle) { attractedParticles.remove(particle); }
}
class Drawer extends Box {
  int fg = color(109, 113, 127);
  int mg = color(38, 40, 49);
  int bg = color(30, 35, 40);
  
  float maxWidth = 200;
  float barWidth = 10;
  float btnSize = 50;

  float scroll = 0;

  boolean out = true;

  LRArrowBtn outBtn;
  BinBtn binBtn;

  ShelfManager shelves;

  Drawer() {
    setSize(maxWidth, H);
    setBackground(bg);

    outBtn = new LRArrowBtn(false);
    setUpButton(outBtn);

    binBtn = new BinBtn();
    setUpButton(binBtn);

    shelves = new ShelfManager(); 

    update();
  }

  private void setUpButton(Button btn) {
    btn.setParent(this);
    btn.setSize(btnSize, btnSize);
    btn.setColors(mg, fg, fg, mg);
  }

  public void update() {
    outBtn.setOffset(w, 0);
    outBtn.updatePos();
    binBtn.setOffset(w, h - btnSize);
    binBtn.updatePos();
  }

  public void show() {
    super.show();
    if (out) shelves.show();
    showBar();
  }

  private void showBar() {
    fill(mg);
    rect(w, 0, barWidth, h);
    outBtn.show();
    binBtn.show();
  }

  public void mousePress() {
    if (outBtn.mouseOver()) flipOut();
    else if (out) shelves.mousePress();
  }

  public void flipOut() {
    out ^= true;
    outBtn.flip();
    float w = out ? maxWidth : 0;
    setWidth(w);
    update();
  }
  
  public boolean toBin() { return binBtn.mouseOver(); }
  
  public void scroll(float s) { if(mouseOver()) shelves.scroll(s); }
  
  public Block loadNewBlock(ArrayList<String> state, int k) {
    return shelves.loadNewBlock(state, k);
  }
  
  public void helpOverlay(int helpMode) {
    if (helpMode == 1) {
      show();
      fill(255);
      textSize(20);
      textAlign(LEFT, CENTER);
      text(" <- Close or open the block drawer", outBtn.getX() + outBtn.getWidth(), outBtn.getCenterY());
      text(" <- Drag scripts here to delete them", binBtn.getX() + binBtn.getWidth(), binBtn.getCenterY());
    } else if (helpMode == 2) {
      if (!out) flipOut(); 
      shelves.helpOverlay();
    }
  }
}

class ShelfManager {
  int n = 11;

  float shelfOffset = 30;
  float shelfX = 40;
  float shelfHeight = 62;

  float scroll = 0;
  float scrollFactor = 40;
  float scrollMax = n*shelfHeight + shelfOffset;

  ArrayList<Block> shelves = new ArrayList<Block>();

  ShelfManager() {
    shelves.add(new OperandBlock("ADD", 100, getBlockHue(9),  " <- ADD the contents of the given address to the accumulator"));
    shelves.add(new OperandBlock("SUB", 200, getBlockHue(10), " <- SUBtract the contents of the given address from the accumulator"));
    shelves.add(new OperandBlock("STO", 300, getBlockHue(0),  " <- STOre the contents of the accumulator in the given address"));
    shelves.add(new OperandBlock("LDA", 500, getBlockHue(1),  " <- LoaD the contents of the given address into the Accumulator"));
    shelves.add(new OperandBlock("BRA", 600, getBlockHue(1.5f)," <- BRAnch to the given address"));
    shelves.add(new OperandBlock("BRZ", 700, getBlockHue(2.5f)," <- BRanch to the given address if the accumulator is Zero"));
    shelves.add(new OperandBlock("BRP", 800, getBlockHue(4),  " <- BRanch to the given address if the accumulator is Positive"));
    shelves.add(new        Block("INP", 901, getBlockHue(5),  " <- INPut a value into the accumulator"));
    shelves.add(new        Block("OUT", 902, getBlockHue(6.5f)," <- OUTput the value of the accumulator"));
    shelves.add(new        Block("HLT", 000, getBlockHue(7.5f)," <- HaLT the program (empty RAM address defaults to this)")); 
    shelves.add(new OperandBlock("DAT", 000, getBlockHue(8),  " <- DATa block: use for storing the value of a variable"));
    setPos();
  }
  
  public Block loadNewBlock(ArrayList<String> state, int k) {
    for (Block shelf : shelves) if (state.get(6).equals(shelf.getText())) return shelf.copy();
    return null;
  }
  
  public int getBlockHue(float i) {
    colorMode(HSB);
    int c = color(255*i/n, 255, 255);
    colorMode(RGB);
    return c;
  }

  public void setPos() {
    float shelfY = shelfOffset - scroll;
    for (Block shelf : shelves) {
      shelf.setPos(shelfX, shelfY); 
      shelfY += shelfHeight;
    }
  }

  public void show() {
    for (Block shelf : shelves) shelf.show();
  }

  public void scroll(float s) {
    if (scrollMax > H) {
      scroll += s * scrollFactor;
      scroll = constrain(scroll, 0, scrollMax - H);
      setPos();
    }
  }

  public void mousePress() {
    if (dragging == null) {
      for (Block shelf : shelves) {
        if (shelf.mouseOver()) {
          ArrayList<Block> newBlockArray = new ArrayList<Block>();
          newBlockArray.add(shelf.copy());
          Script newScript = scripts.addNewScript(newBlockArray);
          newScript.drag();
          break;
        }
      }
    }
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(LEFT, CENTER);
    for (Block shelf : shelves) { shelf.helpOverlay(); }
  }
}

class ControlPanel extends Box {
  int mg = color(38, 40, 49);
  int bg = color(30, 35, 40);
  int assembleArrowColor = color(17, 72, 166);
  int runArrowColor = color(54, 173, 55);
  int resetArrowColor = color(255,50,100);
  int sliderColor = color(109, 113, 127);
  int speedColor1 = color(8, 116, 232);
  int speedColor2 = color(10, 237, 255);
  int newColor = color(255);
  int saveColor = color(63, 188, 206);
  int loadColor = color(255, 255, 0);
  int helpColor = color(160, 32, 240);
  
  float btnSize = 60;
  
  ArrayList<Box> controls = new ArrayList<Box>();
  
  LRArrowBtn assembleBtn;
  LRArrowBtn runBtn;
  PauseBtn pauseBtn;
  UndoBtn resetBtn;
  SliderBase particleSpeedSlider;
  PlusBtn newBtn;
  SaveBtn saveBtn;
  LoadBtn loadBtn;
  QstnBtn helpBtn;
  
  PopUp savePopUp, loadPopUp;
  String lastSave = "";
  
  PopUp selectedPopUp;
  
  ControlPanel() {
    setHeight(H);
    setX(W);
    
    assembleBtn = new LRArrowBtn(true);
    setUpControl(assembleBtn, 0);
    assembleBtn.setColors(mg, assembleArrowColor, assembleArrowColor, mg);
    
    runBtn = new LRArrowBtn(true);
    setUpControl(runBtn, 1);
    runBtn.setColors(mg, runArrowColor, runArrowColor, mg);
    
    pauseBtn = new PauseBtn();
    setUpControl(pauseBtn, 2);
    pauseBtn.setColors(mg, sliderColor, sliderColor, mg);
    
    resetBtn = new UndoBtn();
    setUpControl(resetBtn, 3);
    resetBtn.setColors(mg, resetArrowColor, resetArrowColor, mg);
    
    particleSpeedSlider = new SliderBase();
    setUpControl(particleSpeedSlider, 4);
    particleSpeedSlider.setColors(bg, sliderColor, speedColor1, 0, speedColor2);
    particleSpeedSlider.setHeight(btnSize*4);
    particleSpeedSlider.setUpSlider();
    
    newBtn = new PlusBtn();
    setUpControl(newBtn, 8);
    newBtn.setColors(mg, runArrowColor, newColor, newColor);
    
    saveBtn = new SaveBtn();
    setUpControl(saveBtn, 9);
    saveBtn.setColors(mg, saveColor, saveColor, mg);
    
    loadBtn = new LoadBtn();
    setUpControl(loadBtn, 10);
    loadBtn.setColors(mg, saveColor, loadColor, loadColor);
    
    helpBtn = new QstnBtn();
    setUpControl(helpBtn, 11);
    helpBtn.setColors(mg, helpColor, helpColor, mg);
  }
  
  public void setUpControl(Box control, int row) {
    control.setParent(this);
    control.setSize(btnSize, btnSize);
    control.setOffset(-btnSize*(1 + floor(row*(btnSize+1)/ H)), (row % floor(H/btnSize))*btnSize);
    control.updatePos();
    controls.add(control);
  }
  
  public void show() {
    for (Box btn : controls) btn.show();
    if (savePopUp != null) savePopUp.show();
    if (loadPopUp != null) loadPopUp.show(); 
  }
  
  public void mousePress() {    
    if (assembleBtn.mouseOver()) ram.assemble();
    if (runBtn.mouseOver()) pc.step();
    if (pauseBtn.mouseOver()) pause();
    if (resetBtn.mouseOver()) hardware.reset();
    if (particleSpeedSlider.mouseOver()) particleSpeedSlider.mousePress();
    if (newBtn.mouseOver()) loadAllStates("new");
    if (saveBtn.mouseOver()) savePopUp();
    if (loadBtn.mouseOver()) loadPopUp();
    if (helpBtn.mouseOver()) help();
  }
  
  public void selectPopUp(PopUp p) {
    p.select();
    selectedPopUp = p;
    select();
  }
  
  public void clearPopUps() {
    savePopUp = null;
    loadPopUp = null;
  }
  
  public void savePopUp() {
    if (savePopUp == null) {
      clearPopUps();
      savePopUp = new PopUp(saveBtn, "Save", lastSave);
      savePopUp.setBackground(mg);
      selectPopUp(savePopUp);
    } else commenceSave();
  }
   
  public void loadPopUp() {
    if (loadPopUp == null) {
      clearPopUps();
      loadPopUp = new PopUp(loadBtn, "Load", lastSave);
      loadPopUp.setBackground(mg);
      selectPopUp(loadPopUp);
    } else commenceLoad();
  }
  
  public void deSelect() {
    if (savePopUp != null) savePopUp.deSelect();
    if (loadPopUp != null) loadPopUp.deSelect();
    super.deSelect();
  }
  
  public void type() {
    if (savePopUp != null) savePopUp.updateWidth();
    if (loadPopUp != null) loadPopUp.updateWidth();
  }
  
  public void enter() {
    if (selectedPopUp == savePopUp) commenceSave();
    if (selectedPopUp == loadPopUp) commenceLoad();
  }
  
  public void commenceSave() {
    lastSave = savePopUp.getText();
    clearPopUps();
    saveAllStates(lastSave);
  }
  
  public void commenceLoad() {   
    lastSave = loadPopUp.getText();
    clearPopUps();
    loadAllStates(lastSave);
  }
  
  public float getParticleSpeed() {
    return particleSpeedSlider.getValue();
  }
  
  public boolean maxParticleSpeed() {
    return particleSpeedSlider.atMax();
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(RIGHT, CENTER);
    text("Assemble the scripts into the RAM (c) -> ", assembleBtn.getX(), assembleBtn.getCenterY());
    text("Run the program loaded in the RAM (r) -> ", runBtn.getX(), runBtn.getCenterY());
    text("Pause the program (p) -> ", pauseBtn.getX(), pauseBtn.getCenterY());
    text("Reset the data, Output and Accumulator -> ", resetBtn.getX(), resetBtn.getCenterY());
    text("Change the speed of the data. Slide to bottom for turbo mode! -> ", particleSpeedSlider.getX(), particleSpeedSlider.getCenterY());
    text("Create a new program (RESETS ALL SCRIPTS!!) -> ", newBtn.getX(), newBtn.getCenterY());
    text("Save your program and setup to a file -> ", saveBtn.getX(), saveBtn.getCenterY());
    text("Load a previous program or one of the example programs -> ", loadBtn.getX(), loadBtn.getCenterY());
    text("Click to hide/show help text -> ", helpBtn.getX(), helpBtn.getCenterY());
  }
  
  public void showHelp() {
    helpBtn.show();
  }
}
class ProgramCounter extends Hardware {
  int bg = color(250, 148, 1);
  int countColor = color(250, 50, 220);
  float startX = 420, startY = H-160;
  float startWidth = 55, startHeight = 60;

  float pcHeight = 23;
  float pcOffset = 15;
  float labelOffset = 18;
  
  FTextBox pc = new FTextBox(2);
  
  ParticleAttractor incrementPC = new ParticleAttractor(pc);
  ParticleAttractor branch = new ParticleAttractor(pc);

  ProgramCounter() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);

    setUpLabel();
    setUpPC();
    updatePos();
  }
  
  public void setUpLabel() {
    setLabel("Program"+"\n"+"Counter");
    setLabelOffset(labelOffset);
  }

  public void setUpPC() {
    pc.setParent(this);
    pc.setOffsetY(pcOffset);
    pc.setHeight(pcHeight);
    pc.updateLength();
  }
  
  public void show() {
    super.show();
    pc.show();
  }
  
  public void drag() {
    super.drag();
    updatePos();
  }
  
  public void updatePos() {
    pc.setCenter(cx, y);
  }
  
  public Particle setUpNewParticle() {
    Particle newParticle = new Particle(pc.getText());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(bg);
    return newParticle;
  }
  
  public void step() {
    returnCount();
    incrementPC();
  }
  
  public void returnCount() {
    cpu.returnCount(setUpNewParticle());
  }
  
  public void incrementPC() {
    alu.incrementPC(setUpNewParticle());
  }
  
  public void returnIncrement(Particle p) {
    incrementPC.addParticle(p);
  }
  
  public void branch(Particle p) {
    branch.addParticle(p);
  }
  
  public void branch(int b) {
    pc.setText(b);
    step();
  }
  
  public void run() {
    if (incrementPC.gotInput()) pc.setText(incrementPC.input());
    if (branch.gotInput()) branch(branch.input());
  }
  
  public void reset() {
    pc.setText(0);
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#PC");
    state.addAll(super.saveState());
    state.add(pc.getText());
    return state;
  }
  
  public void loadState(ArrayList<String> states) {
    int index = states.indexOf("#PC");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 8));
    super.loadState(state);
    pc.setText(state.get(6));
    updatePos();
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, TOP);
    text(" <- The Program Counter stores the address of the current instruction.", x + w, y, 500, 100);
  }
}

class Accumulator extends Hardware {
  int bg = color(240, 23, 89);
  float startX = 300, startY = H-70;
  float startWidth = max(100, (MEMORY_LENGTH)*15), startHeight = 60;

  float accHeight = 25;
  float accOffset = 20;
  float labelOffset = 15;
  
  FTextBox acc = new FTextBox(MEMORY_LENGTH);
  
  ParticleAttractor load = new ParticleAttractor(acc);
  ParticleAttractor store = new ParticleAttractor(acc);
  ParticleAttractor input = new ParticleAttractor(acc);

  Accumulator() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);
    
    setUpLabel();
    setUpAcc();
    updatePos();
  }
  
  public void setUpLabel() {
    setLabel("Accumulator");
    setLabelOffset(labelOffset);
  }

  public void setUpAcc() {
    acc.setParent(this);
    acc.setHeight(accHeight);
    acc.setOffsetY(accOffset);
    acc.updateLength();
  }
  
  public void show() {
    super.show();
    acc.show();
  }
  
  public void drag() {
    super.drag();
    updatePos();
  }
  
  public void updatePos() {
    acc.setCenter(cx, y);
  }
  
  public void calcAcc() {
    alu.calcAcc(setUpNewParticle());
  }
  
  public void load(Particle particle) {
    load.addParticle(particle);
  }
  
  public void load() {
    acc.setText(load.input());
    pc.step();
  }  
  
  public void store(Particle particle) {
    store.addParticle(particle);
  }
  
  public void store(int s) {
    MemoryAddress r = ram.getRegister(s);
    r.store(setUpNewParticle());
  }
  
  public void checkForBranch() {
    cpu.checkForBranch(setUpNewParticle());
  }
  
  public void run() {
    if (load.gotInput()) load();
    if (store.gotInput()) store(store.input());
  }
  
  public void reset() {
    acc.setText(0);
  }
  
  public void output() {
    out.output(setUpNewParticle());
    pc.step();
  }
  
  public Particle setUpNewParticle() {
    Particle newParticle = new Particle(acc.getText());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(bg);
    return newParticle;
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#Acc");
    state.addAll(super.saveState());
    state.add(acc.getText());
    return state;
  }
  
  public void loadState(ArrayList<String> states) {
    int index = states.indexOf("#Acc");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 8));
    super.loadState(state);
    acc.setText(PApplet.parseInt(state.get(6)));
    updatePos();
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, BOTTOM);
    text("\\/", cx, y);
    text("The Accumulator stores a value.", cx, y - 3*textAscent());
    text("ADD or SUB Blocks change the value.", cx, y - 2*textAscent());
    text("Branch Blocks check the value.", cx, y - textAscent());
  }
}

class ALU extends Hardware {
  int bg = color(163, 1, 163);
  float startX = 415, startY = H-90;
  float startWidth = 120, startHeight = 80;
  
  float labelSize = 15;
   
  ParticleAttractor incrementPC = new ParticleAttractor(this);
  ParticleAttractor calcAcc = new ParticleAttractor(this);
  ParticleAttractor calcData = new ParticleAttractor(this);
  
  ALU() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);
    setUpLabel();
  }
  
  public void setUpLabel() {
    setLabel("Arithmetic" + "\n" + "Logic" + "\n" + "Unit");
    setLabelTopAlign(CENTER);
    setLabelSize(labelSize);
  }
  
  public void incrementPC(Particle particle) {
    incrementPC.addParticle(particle);
  }
  
  public void calcAcc(Particle particle) {
    calcAcc.addParticle(particle);
  }  

  public void calcData(Particle particle) {
    calcData.addParticle(particle);
  }
  
  public void run() {
    Particle PCParticle = incrementPC.getInput();
    if (PCParticle != null) {
      PCParticle.setData(calculate(PCParticle, 1, 2));
      incrementPC.removeParticle(PCParticle);
      pc.returnIncrement(PCParticle);
    }
    Particle AccParticle = calcAcc.getInput();
    if (AccParticle != null) {
      if (calcData.gotInput()) {
        int data = 0;
        int instruction = cpu.getCIRValue();
        if (instruction == 1) data = calcData.input();  
        if (instruction == 2) data = -calcData.input();
        AccParticle.setData(calculate(AccParticle, data, MEMORY_LENGTH));
        calcAcc.removeParticle(AccParticle);
        acc.load(AccParticle);
      }
    }
  }
  
  public String calculate(Particle p, int a, int mod) {
    return nf((p.getIntData() + a) % PApplet.parseInt(pow(10, mod)), mod);
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#ALU");
    state.addAll(super.saveState());
    return state;
  }
  
  public void loadState(ArrayList<String> states) {
    int index = states.indexOf("#ALU");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 7));
    super.loadState(state);
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, CENTER);
    text(" <- This unit deals with adding, subtracting and other logic.", x + w, y, 400, 100);
  }
}

class Input extends Hardware {
  int bg = color(92, 255, 147);
  float startX = 260, startY =  60;
  float startWidth = 150, startHeight = 80;

  float labelOffset = 20;
  float labelSize = 20;

  float inputY = 15;
  float inputWidth = 100;
  float inputHeight = 30;
  int inputLength = 8;
  
  boolean active = false;

  Typable inp = new Typable();

  ParticleAttractor input = new ParticleAttractor(inp);

  Input() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);
    
    setUpLabel();
    setUpInput();
    updatePos();
  }
  
  public void setUpLabel() {
    setLabel("Input");
    setLabelSize(labelSize);
    setLabelOffset(labelOffset);
  }

  public void setUpInput() {
    inp.setParent(this);
    inp.setOffsetY(inputY);
    inp.setSize(inputWidth, inputHeight);
    inp.setLimits(inputLength, inputLength);
    inp.updateLength();
  }

  public void show() {
    super.show();
    inp.show();
  }

  public void drag() {
    super.drag();
    updatePos();
  }
  
  public void release() {
    super.release();
    if (active) select();
  }

  public void updatePos() {
    inp.setCenterX(cx);
    inp.setY(y);
  }

  public void select() {
    inp.select();
    super.select();
  }
  
  public void deSelect() {
    inp.deSelect();
    super.deSelect();
  }
  
  public void input(Particle particle) {
    input.addParticle(particle);
  }
  
  public void run() {
    if (input.gotInput()) activate();
  }
  
  public void activate() {
    active = true;
    select();
  }
  
  public void input() {
    acc.load(setUpNewParticle());
    active = false;
  }
  
  public Particle setUpNewParticle() {
    Particle newParticle = new Particle(inp.getText());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(bg);
    return newParticle;
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#Input");
    state.addAll(super.saveState());
    state.add(inp.getText());
    return state;
  }
  
  public void loadState(ArrayList<String> states) {
    int index = states.indexOf("#Input");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 8));
    super.loadState(state);
    inp.setText(state.get(6));
    updatePos();
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, TOP);
    text("^", cx, y + h);
    text("The Input Block lets you input a value into your program.", x - 50, y + h + textAscent(), w + 100, 300);
  }
}

class Output extends Hardware {
  int bg = color(210, 71, 121);
  float startWidth = 180, startHeight = 200;
  float startX = W-startWidth-80, startY = H-210;

  float outputWidth = 160;
  float outputHeight = 30;
  float outputOffset = 40;
  
  float labelSize = 20;
  float labelOffset = 18;

  int n = 5;
  TextBox[] outs = new TextBox[n];
  ParticleAttractor[] outputs = new ParticleAttractor[n];

  Output() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);

    setUpLabel();
    setUpOutputs();
    updatePos();
  }
  
  public void setUpLabel() {
    setLabel("Output");
    setLabelTopAlign(TOP);
    setLabelSize(labelSize);
    setLabelOffset(labelOffset);
  }

  public void setUpOutputs() {
    for (int i = 0; i < n; i++) {
      TextBox output = new TextBox();
      output.setParent(this);
      output.setOffsetY(outputOffset);
      output.setSize(outputWidth, outputHeight);
      output.setMin(MEMORY_LENGTH);
      outputOffset += outputHeight;
      outputs[i] = new ParticleAttractor(output); 
      outs[i] = output;
    }
  }

  public void show() {
    super.show();
    for (TextBox output : outs) output.show();
  }

  public void drag() {
    super.drag();
    updatePos();
  }

  public void updatePos() {
    for (TextBox output : outs) {
      output.setCenterX(cx);
      output.setY(y);
    }
  }
  
  public void output(Particle particle) {
    for (int i = 0; i < n; i++) if (i == n - 1 || outs[i].getText() == "") {
      outputs[i].addParticle(particle);
      break;
    }
  }
  
  public void run() {
    for (int i = 0; i < n; i++) {
      ParticleAttractor output = outputs[i];
      if (output.gotInput()) {
        if (i == n - 1 && outs[i].getText() != "") shift();
        outs[i].setText(output.input());
      }
    }
  }
  
  public void shift() {
    for (int i = 0; i < n - 1; i++) {
      String next = outs[i+1].getText();
      outs[i].setText(next);
    }
  }
  
  public void reset() {
    for (TextBox output : outs) {
      output.setText("");
    }
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#Output");
    state.addAll(super.saveState());
    for (TextBox output : outs) state.add(output.getText());
    return state;
  }
  
  public void loadState(ArrayList<String> states) {
    int index = states.indexOf("#Output");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 7));
    ArrayList<String> outputStates = new ArrayList<String>(states.subList(index + 7, index + 13));
    super.loadState(state);
    for (int i = 0; i < n; i++) outs[i].setText(outputStates.get(i));
    updatePos();
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, CENTER);
    text("Output from the Output Block will appear here. Up to 5 values will be stored at a time. -> ", x, y, -400, h);
  }
}
class Particle extends Colored {
  int FG = color(255);

  float R = 15;
  float TEXT_SIZE = 16;

  float GRAVITY_FACTOR = 0.1f;
  float DRAG_FACTOR = 0.01f;
  float BEETLING_FACTOR = .1f; //Optional for beetling eg .4
  
  float v0 = random(10);
  float t = 0;
  float dt = random(3);

  PVector pos = new PVector();
  PVector vel = PVector.mult(PVector.random2D(), v0 * sqrt(particles.getParticleSpeed()));
  PVector acc = new PVector();

  ParticleAttractor target;
  public void setTarget(ParticleAttractor p) { target = p; }

  String data = "";
  public String getData() { return data; }
  public int getIntData() { return PApplet.parseInt(data); }

  Particle(String d) {
    setData(d);
  }
  
  public void setData(String d) {
    data = d;
  }

  public void setPos(float x, float y) {
    pos.set(x, y);
  }

  public void update() {
    applyForce(targetGravity());
    applyForce(beetling());
    applyForce(drag());

    vel.add(acc);
    pos.add(vel);
    acc.mult(0); 
    
    t += dt;
  }
  
  public void applyForce(PVector force) {
    acc.add(force);
    if (cheekyDevMode) devForce(this, force);
  }

  public PVector targetGravity() {
    PVector targetGravity = target.targetGravity().sub(pos).setMag(sin(radians(t)) + 2);  
    return targetGravity.mult(GRAVITY_FACTOR * particles.getParticleSpeed());
  }

  public PVector beetling() {
    return PVector.random2D().mult(BEETLING_FACTOR * sqrt(particles.getParticleSpeed()));
  }

  public PVector drag() {
    return PVector.mult(vel, -vel.mag() * DRAG_FACTOR);
  }

  public void show() {
    fill(bg);
    ellipseMode(RADIUS);
    ellipse(pos.x, pos.y, R, R);
    showText();

    if (cheekyDevMode) devReticle(this);
  }

  public void showText() {
    fill(FG);
    textSize(TEXT_SIZE);
    textAlign(CENTER, CENTER);
    text(data, pos.x, pos.y);
  }
  
  public boolean arrived() {
    return control.maxParticleSpeed() || target.distance(this) <= R*constrain(targetGravity().mag(), 1, 2);
  }
  
  public void add(int b) {
    data = nf(getIntData() + b, data.length());
  }
}
class RAM extends Hardware {  
  int m = min(PApplet.parseInt(W/100), 10);
  int n = m*m;
  int len = MEMORY_LENGTH;
  
  float memOffset = 5;
  float memXPadding = 10, memYPadding = 19;
  float memWidth = len * 10 - 1, memHeight = 20;
  float memLabelOffset = -5;
  
  float RAMWidth = m*(memWidth + memXPadding);
  float RAMHeight = m*(memHeight + memYPadding);
  float startX = W-RAMWidth-70, startY = 10;
  float labelHeight = 20;
  float labelSize = 15;
  
  int bg = color(17, 72, 166);
  
  StartTag startTagSpawner = new StartTag(this);
  
  MemoryAddress[] memory = new MemoryAddress[n];
  
  public MemoryAddress getRegister(int k) {
    return memory[k % n];
  }
  
  RAM() {
    setBackground(bg);
    setSize(RAMWidth, RAMHeight + labelHeight);
    setPos(startX, startY);
    
    setUpStartTagSpawner();
    setUpLabel();    
    setUpRegisters();
    updatePos();
  }
  
  public void setUpStartTagSpawner() {
    startTagSpawner.setOffsetX(-startTagSpawner.getTotalWidth());
  }
  
  public void setUpLabel() {
    setLabel("RAM");
    setLabelOffset(labelSize);
    setLabelSize(labelSize);
  }
  
  public void setUpRegisters() {
    for (int k = 0; k < memory.length; k++) {
      float registerX = memOffset + (k % m)*(memWidth + memXPadding);
      float registerY = memOffset + (k / m)*(memHeight + memYPadding);
      
      MemoryAddress current = new MemoryAddress(len);
      current.setParent(this);
      current.setSize(memWidth, memHeight);
      current.setOffset(registerX, registerY);
      current.setLabelOffset(memLabelOffset);
      current.setLabel(str(k));
      //current.updateLength();
      memory[k] = current;
    }
  }
  
  public void updatePos() {
    for (MemoryAddress register : memory) register.updatePos();
    startTagSpawner.updatePos();
  }
  
  public void show() {
    super.show();
    for (MemoryAddress register : memory) register.show();
    startTagSpawner.show();
  } 

  public void drag() {
    super.drag();
    updatePos();
  }
  
  public void mousePress() {
    if (startTagSpawner.mouseOver()) {
      Tag newStartTag = startTagSpawner.copy();
      newStartTag.drag();
      tags.addTag(newStartTag);
    } else super.mousePress();
  }
  
  public void clearData() {
    for (MemoryAddress register : memory) register.setWillHaveData(false);
  }
  
  public void assemble() {
    clearData();
    scripts.assemble();
  }
  
  public int nextRegister() {
    for (int i = 0; i < n; i++) if (!memory[i].willHaveData()) return i;
    clearData();
    return 0;
  }
  
  public void run() {
    for (MemoryAddress r : memory) r.run();
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#RAM");
    states.addAll(super.saveState());
    for (int k = 0; k < n; k++) states.addAll(memory[k].saveState(k));
    return states;
  }
  
  public void loadState(ArrayList<String> states) {
    int index = states.indexOf("#RAM");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 8));
    super.loadState(state);
    int k = 0;
    while (states.contains("#MemoryAddress " + k) && k < n) { 
      memory[k].loadState(states, k);
      k++;
    }
    setSize(RAMWidth, RAMHeight + labelHeight);
    updatePos();
  }
  
  public void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, TOP);
    text("^ The RAM is where the program is stored.", cx, y + h);
    text("Press the blue compile arrow to load the program into the RAM.", cx, y + h + textAscent());
    textAlign(CENTER, TOP);
    text("A script with this tag will be the first script loaded into the RAM. ->", startTagSpawner.getX(), startTagSpawner.getCenterY(), -250, 500);
  }
}

class MemoryAddress extends FTextBox {
  boolean willHaveData = false;
  public boolean willHaveData() { return willHaveData; }
  public void setWillHaveData(boolean b) { willHaveData = b; }
  
  MemoryAddress(int n) { super(n); }
  
  int ramParticleColor = color(10, 0, 200);
  
  ParticleAttractor assemble = new ParticleAttractor(this);
  ParticleAttractor fetchInstruction = new ParticleAttractor(this);
  ParticleAttractor fetchData = new ParticleAttractor(this);
  ParticleAttractor store = new ParticleAttractor(this);
  
  public void assemble(Particle p) {
    assemble.addParticle(p);
  }
  
  public void fetchInstruction(Particle p) {
    fetchInstruction.addParticle(p);
  }
  
  public void returnInstruction() {
    cpu.returnInstruction(setUpNewParticle());
  }
  
  public void fetchData(Particle p) {
    fetchData.addParticle(p);
  }
  
  public void returnData() {
    cpu.returnData(setUpNewParticle());
  }
  
  public void store(Particle p) {
    store.addParticle(p);
  }
  
  public void store(int s) {
    setText(s);
    pc.step();
  }
  
  public void run() {
    if (assemble.gotInput()) setText(assemble.input());
    if (fetchInstruction.gotInput()) returnInstruction();
    if (fetchData.gotInput()) returnData();
    if (store.gotInput()) store(store.input());
  }
  
  public Particle setUpNewParticle() {
    Particle newParticle = new Particle(getText());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(ramParticleColor);
    return newParticle;
  }
  
  public ArrayList<String> saveState(int k) {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#MemoryAddress " + k);
    states.add(getText());
    return states;
  }
  
  public void loadState(ArrayList<String> states, int k) {
    int index = states.indexOf("#MemoryAddress " + k);
    setText(PApplet.parseInt(states.get(index + 1)));
    updatePos();
  }
}
class Script extends Draggable {
  ArrayList<Block> script = new ArrayList<Block>();
  Block selectedBlock;

  Slot top;
  Tab bottom;

  public float getTotalHeight() {
    float h = 0;
    for (Block block : script) h += block.getTotalHeight();
    return h;
  }
  
  public ArrayList<Block> getScript() { return script; }
  
  public int getSize() { return script.size(); }
  
  public Slot getTop() { return top; }
  public Tab getBottom() { return bottom; }

  Script(ArrayList<Block> blocks) {
    script.addAll(blocks);
    for (Block block : blocks) block.setParent(this);
    updateConnector();
    Block first = script.get(0);
    setPos(first.getX(), first.getY());
  }
  
  public void updateConnector() {
    top = script.get(0).getSlot();
    bottom = script.get(script.size() - 1).getTab();
  }
  
  public void setPos(float _x, float _y) {
    super.setPos(_x, _y);
    update();
  }
  
  public void update() {
    float h = 0;
    for (Block block : script) {
      block.setPos(x, y + h);
      h += block.getTotalHeight();
    }
  }

  public void show() {
    for (Block block : script) { block.show(); }
  }
  
  public boolean mouseOver() {
    for (Block block : script) if (block.mouseOver()) return true;
    return false;
  }

  public int getMouseOver() {
    for (Block block : script) if (block.mouseOver()) return script.indexOf(block);
    return -1;
  }
  
  public void release() {
    scripts.select(this);
    super.release();
    scripts.release(this);
  }
  
  public void select() {
    int index = getMouseOver();
    if (index == -1) index = 0;
    selectedBlock = script.get(index);
    selectedBlock.select();
    super.select();
  }
  
  public void deSelect() {
    selectedBlock.deSelect();
    super.deSelect();
  }
  
  public void updateWidth() {
    selectedBlock.updateWidth();
  }
    
  public void addAtTop(Script other) {
    float newY = y - other.getTotalHeight();
    script.addAll(0, other.getScript());
    updateConnector();
    setPos(x, newY);
  }
  
  public void addAtBottom(Script other) {
    int n = script.size();
    script.addAll(n, other.getScript());
    updateConnector();
    update();
  }
  
  public void splitScript() {
    int index = getMouseOver();
    Script split = scripts.addNewScript(new ArrayList(script.subList(0, index)));
    float newY = y + split.getTotalHeight();
    for (int i = 0; i < index; i++) script.remove(0);
    updateConnector();
    setPos(x, newY);
  }
  
  public Script duplicate() {
    int index = getMouseOver();
    ArrayList<Block> newScript = new ArrayList<Block>();
    for (int i = index; i < script.size(); i++) newScript.add(script.get(i).copy());
    return scripts.addNewScript(newScript);
  }
  
  public Block checkForTag(Tag current) {  
    for (Block other : script) {
      Block found = other.checkForTag(current);
      if (found != null) return found;
    }
    return null;
  }
  
  public OperandBlock checkAttach(Tag current) {  
    for (Block other : script) {
      if (other instanceof OperandBlock) {
        OperandBlock found = (OperandBlock)other;
        found = found.checkAttach(current);
        if (found != null) return found;
      }
    }
    return null;
  }
  
  public Tag makeNewTag() {
    for (Block block : script) {
      Tag found = block.makeNewTag();
      if (found != null) return found;
    }
    return null;
  }
  
  public void scroll(float s) {
    for (Block block : script) {
      block.change(s);
      break;
    }
  }
  
  public void assemble(ArrayList<Tag> blockTags) {
    for (Block block : script) block.assembleTag(blockTags);
  }
  
  public void run() {
    for (Block block : script) { block.run(); }
  }
  
  public boolean isStartingScript() {
    for (Block block : script) if (block.hasStartTag()) return true;
    return false;    
  }  
  
  public ArrayList<Tag> getTags(int k) {
    ArrayList<Tag> getTags = new ArrayList<Tag>();
    for (int dk = 0; dk < script.size(); dk++) {
      Block block = script.get(dk);
      block.setRegIndex(k + dk);
      ram.getRegister(k + dk).setWillHaveData(true);
      if (!(block.getTag() == null || block.getTag() instanceof StartTag)) getTags.add(block.getTag());
    }
    return getTags;
  }
  
  public ArrayList<String> saveState(int k) {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#Script " + k);
    states.addAll(super.saveState());
    int n = script.size();
    states.add(str(n));
    for (int dk = 0; dk < n; dk++) states.addAll(script.get(dk).saveState(dk));
    return states;
  }
}
class LRArrowBtn extends Button {
  boolean right = true;

  LRArrowBtn(boolean _right) { right = _right; } 

  public void show() {
    super.show();
    arrow();
  }

  public void arrow() {
    fill(fg);
    float l = w*sqrt(2)/4;
    if (right) triangle(x+l, y+h/4, x+w*3/4, y+h/2, x+l, y+h*3/4);
    else triangle(x+w-l, y+h/4, x+w/4, y+h/2, x+w-l, y+h*3/4);
  }
  
  public void flip() { right ^= true; }
}

class BinBtn extends Button {
  public void show() {
    super.show();
    drawBin();
  }
  
  public void drawBin() {
    fill(fg);
    rect(x + w/3, y + h/2, w/3, h/3);
    rect(x + 3*w/12, y + h/3, w/2, h/12);
    rect(x + w/3, y + 3*h/12, w/3, h/12 + 1);
  }
}

class PauseBtn extends Button {
  public void show() {
    super.show();
    pause();
  }
  
  public void pause() {
    fill(fg);
    rect(cx+0.05f*w, y+0.25f*h,0.2f*w,0.5f*h);
    rect(cx-0.05f*w, y+0.25f*h,-0.2f*w,0.5f*h);
  }
}

class UndoBtn extends Button {
  public void show() {
    super.show();
    arrow();
  }
  
  public void arrow() {
    ellipseMode(RADIUS);
    fill(fg);
    float min = min(w, h);
    float r1 = 3*min/10;
    ellipse(cx, cy, r1, r1);
    
    fill(bg);
    float r2 = 2*min/10;
    ellipse(cx, cy, r2, r2);
    rect(x, y, w/2, h/2);
    
    fill(fg);
    float d = (r1-r2)*2;
    triangle(cx, cy-r1-d/2, cx-min/2+d*sqrt(2), cy-(r1+r2)/2, cx, cy-r2+d/2);
    float r3 = (r1-r2)/2;
    ellipse(cx-r3-r2, cy, r3, r3);
  }
}

class SpeedBtn extends SliderBtn {  
  public void show() {
    super.show();
    arrows();
  }
  
  public void arrows() {
    fill(fg);
    float l = w*sqrt(2)/8;
    triangle(x+l, y+h/4, x+w/2, y+h/2, x+l, y+h*3/4);
    triangle(x+w/2, y+h/4, x+w-l, y+h/2, x+w/2, y+h*3/4);
  }
}

class PlusBtn extends Button {
  float plusWidth = 0.08f;
  float plusHeight = 0.3f;
  
  public void show() {
    super.show();
    plus();
  }
  
  public void plus() {   
    fill(fg);
    rect(cx-plusHeight*w, cy-plusWidth*h, 2*plusHeight*w, 2*plusWidth*h);
    rect(cx-plusWidth*w, cy-plusHeight*h, 2*plusWidth*w, 2*plusHeight*h);
  }
}

class SaveBtn extends Button {
  public void show() {
    super.show();
    disk();
  }
  
  public void disk() {
    fill(fg);
    float l = 0.1f;
    rect(x+w*l, y+h*l, w*(1-2*l), h*(1-2*l));
    
    fill(bg);
    rect(x+w*2*l, y+h*1.5f*l, w*(1-4*l), h*(0.5f-1.5f*l));
  }
}

class LoadBtn extends Button {
  public void show() {
    super.show();
    folder();
  }
  
  public void folder() {
    fill(fg);
    float tx = 0.6f;
    float ty = 0.05f;
    float lx = 0.4f;
    float ly = 0.35f;
    rect(cx-w*lx,cy-h*ly,2*w*lx,2*h*ly);
    fill(bg);
    rect(x+w*tx,cy-h*ly,w*(1-tx),2*h*ty);
  }
}

class QstnBtn extends Button {
  public void show(){
    super.show();
    question();
  }
  
  public void question() {
    ellipseMode(RADIUS);
    fill(fg);
    float min = min(w, h);
    float r1 = 2.5f*min/10;
    float t = min/10;
    ellipse(cx, cy-t, r1, r1);
    
    fill(bg);
    float r2 = 1.5f*min/10;
    ellipse(cx, cy-t, r2, r2);
    rect(x, cy-t, w/2, h/2);
    
    fill(fg);
    float r3 = (r1-r2)/2;
    ellipse(cx-2*t, cy-t, r3, r3);
    ellipse(cx, cy+t, r3, r3);
    float r4 = 0.8f*min/10;
    ellipse(cx, cy+3*t, r4, r4);
  }
}

class SnapPoint extends Box {
  SnapPoint(Box parent) {
    setParent(parent);
  }
  
  float snapRadius = 30;
  
  public boolean isNear(SnapPoint other) {
    float otherX = other.getCenterX();
    float otherY = other.getCenterY();
    float d = dist(cx, cy, otherX, otherY);
    return d <= snapRadius;
  }
}

class Connector extends SnapPoint {
  float connectorOffset = 20;
  float connectorWidth = 20;
  float connectorHeight = 10;
  
  Connector(Box parent) {
    super(parent);
    setBackground(parent.getBackground());
    setSize(connectorWidth, connectorHeight);
  }
}

class Slot extends Connector {
  Box left = new Box();
  Box right = new Box();
  
  Slot(Block parent) {
    super(parent);
    setOffset(connectorOffset, -connectorHeight);
    
    left.setParent(this);
    left.setBackground(bg);
    left.setOffsetX(-connectorOffset);
    
    right.setParent(this);
    right.setBackground(bg);
    right.setOffsetX(connectorWidth);
    
    updateWidth();
  }
  
  public void updateWidth() {
    left.setSize(connectorOffset, connectorHeight);
    right.setSize(parent.getWidth() - connectorOffset - connectorWidth, connectorHeight);
  }
  
  public void show() {
    left.show();
    right.show();
    
    if (cheekyDevMode) {
      if (mouseOver()) devDraw(this);
      if (mouseOver()) devSnap(this);
    }
  }
  
  public void updatePos() {
    super.updatePos();
    left.updatePos();
    right.updatePos();
  }
  
  public boolean mouseOver() { return (left.mouseOver() || right.mouseOver()); }
}

class Tab extends Connector {
  Tab(Block parent) {
    super(parent);
    setOffset(connectorOffset, parent.getHeight());
  }
  
  public void show() {
    super.show();  
    if (cheekyDevMode) if (mouseOver()) devSnap(this);
  }
}

class Notch extends SnapPoint {
  float r = 10;
  float notchWidth = 20;
  float plusWidth = 0.7f;
  float plusHeight = 0.15f;
  
  int bg = color(0, 200, 0);
  int plusColor = color(255);
  
  Notch(Block parent) {
    super(parent);
    setBackground(bg);
    setSize(notchWidth, parent.getTotalHeight());
    setOffset(-notchWidth/2,-parent.getSlot().getHeight());
  }
  
  public void show() {
    fill(bg);
    ellipseMode(RADIUS);
    ellipse(cx, cy, r, r);
    
    fill(plusColor);
    rect(cx-plusHeight*r, cy-plusWidth*r, 2*plusHeight*r, 2*plusWidth*r);
    rect(cx-plusWidth*r, cy-plusHeight*r, 2*plusWidth*r, 2*plusHeight*r);
  }
}

class Pointer extends SnapPoint {
  float triangleWidth = 0.5f;
  
  Pointer(Box parent) {
    super(parent);
    setBackground(parent.getBackground());
    setOffsetX(parent.getWidth());
    setHeight(parent.getHeight());
    setWidth(h*triangleWidth);
  }
  
  public void show() {
    fill(bg);
    triangle(x,y,x+triangleWidth*h,y+h/2,x,y+h);
    if (cheekyDevMode) if (mouseOver()) devSnap(this);
  }
  
  public boolean mouseOver() {
    return mouseX > x && abs(mouseX-x)/triangleWidth+2*abs(mouseY-y-h/2) < h;
  }
}
class Selectable {
  boolean thisSelected = false;
  public boolean isSelected() { return thisSelected; }
  
  public void select() {
    selected = this;
    thisSelected = true;
  }
  
  public void deSelect() {
    selected = null;
    thisSelected = false;
  }
}

class Colored extends Selectable {
  int bg = color(255);
  public int getBackground() { return bg; }
  public void setBackground(int _bg) { bg = _bg; }
}

class Slate extends Colored {
    /*
  (_x, _y) __dx__
          |      |
         dy      dy
          |__dx__(x, y)____w_____
                      |          |
                      |          |
                      h (cx, cy) h
                      |          |
                      |____w_____|
  */
  
  float  w = 10, h = 10;
  float  x = 0,  y = 0;
  float cx = 5, cy = 5;
  float dx = 0, dy = 0;
  
  public float getWidth() { return w; }
  public float getHeight() { return h; }
  public float getX() { return x; }
  public float getY() { return y; }
  public float getCenterX() { return cx; }
  public float getCenterY() { return cy; }
  
  public void setOffsetX(float _dx) { dx = _dx; }
  public void setOffsetY(float _dy) { dy = _dy; }
  
  public void setWidth(float _w) {
     w = _w;
    cx = x + w/2;
  }
  
  public void setHeight(float _h) {
     h = _h;
    cy = y + h/2;
  }
  
  public void setX(float _x) { 
     x = _x + dx;
    cx = x + w/2;
  }
 
  public void setY(float _y) { 
     y = _y + dy;
    cy = y + h/2;
  }
  
  public void setCenterX(float _cx) {
    cx = _cx + dx;
     x = cx - w/2;
  }
  
  public void setCenterY(float _cy) {
    cy = _cy + dy;
     y = cy - h/2;
  }

  public void setOffset(float dx, float dy) {
    setOffsetX(dx);
    setOffsetY(dy);
  }

  public void setSize(float w, float h) {
    setWidth(w);
    setHeight(h);
  } 
  
  public void setPos(float x, float y) {
    setX(x);
    setY(y);
  }
  
  public void setCenter(float cx, float cy) {
    setCenterX(cx);  
    setCenterY(cy);
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add(str(x));
    state.add(str(y));
    state.add(str(w));
    state.add(str(h));
    state.add(str(dx));
    state.add(str(dy));
    return state;
  }
  
  public void loadState(ArrayList<String> state) {
    setOffset(PApplet.parseInt(state.get(4)), PApplet.parseInt(state.get(5)));
    setSize(PApplet.parseInt(state.get(2)), PApplet.parseInt(state.get(3)));
    setPos(constrain(PApplet.parseInt(state.get(0)), EDGE_BOUNDARY, W - EDGE_BOUNDARY), constrain(PApplet.parseInt(state.get(1)), EDGE_BOUNDARY, H - EDGE_BOUNDARY));
  }
}

class Child extends Slate {
  Box parent;  
  public void setParent(Box _parent) { parent = _parent;  }  
  public boolean hasParent() { return parent != null; }
  public void updatePos() { setPos(parent.getX(), parent.getY()); }
}

class Box extends Child {  
  public void show() {
    fill(bg);
    rect(x, y, w, h);
    
    if(cheekyDevMode) if (mouseOver()) devDraw(this);
  }  
  
  public boolean mouseOver() {
    return mouseX/SIZE_MULT > x && mouseX/SIZE_MULT < x+w && mouseY/SIZE_MULT > y && mouseY/SIZE_MULT < y+h;
  }
}

class LBox extends Box {
  int labelC = color(255, 180);
  float labelSize = 11;
  float labelOffset = 0;
  String label = "";
  
  int leftAlign = CENTER;
  int topAlign = BOTTOM;
  public void setLabelLeftAlign(int l) { leftAlign = l; }
  public void setLabelTopAlign(int t) { topAlign = t; }
  
  public void setLabel(String l) { label = l; }
  public void setLabelColor(int c) { labelC = c; }
  public void setLabelSize(float s) { labelSize = s; }
  public void setLabelOffset(float o) { labelOffset = o; }
  
  public void show() {
    super.show();
    showLabel();
  }
  
  public void showLabel() {
    fill(labelC);
    textSize(labelSize);
    textAlign(CENTER, CENTER);
         if (leftAlign == LEFT)  text(label, x-labelOffset, cy);
    else if (topAlign == BOTTOM) text(label, cx, y+h-labelOffset);
    else if (topAlign == CENTER) text(label, cx, cy-labelOffset);
    else if (topAlign == TOP)    text(label, cx, y+labelOffset);
  }
}

class Draggable extends LBox {
  boolean locked = false;
  public boolean isLocked() { return locked; }
  
  public void lock() {
    locked = true;
    dragging = this;
    setOffset(x - mouseX/SIZE_MULT, y - mouseY/SIZE_MULT);
  }
  
  public void release() {
    locked = false;
    dragging = null;
    setOffset(0, 0);
  }
  
  public void drag() {
    if (!locked) lock();
    setPos(constrain(mouseX/SIZE_MULT, EDGE_BOUNDARY, W - EDGE_BOUNDARY), constrain(mouseY/SIZE_MULT, EDGE_BOUNDARY, H - EDGE_BOUNDARY));
  }
  
  public void mousePress() {
    if (dragging == null) if (mouseOver()) drag();
  }
}

class TextBox extends Draggable {
  int fg = color(0);
  public void setTextColor(int _fg) { fg = _fg; }
  
  int leftAlign = CENTER;
  int topAlign = CENTER;
  public void setLeftAlign(int l) { leftAlign = l; }
  public void setTopAlign(int t) { topAlign = t; }
  
  String contents = "";
  float contentsPad = 2;
  float textOffset = 0;
  int min = 4;
  int max = 50;
  
  public String getText() { return contents; }
  public int getIntText() { return PApplet.parseInt(contents); }
  public int getLength() { return contents.length(); }
  
  public void setText(String _contents) { contents = _contents; }
  public void setText(int _contents) {
    contents = nf(_contents % PApplet.parseInt(pow(10, min)), min);
  }
  
  public void setPadding(float padding) { contentsPad = padding; }
  public void setTextSize() { textSize(h - 2*contentsPad); }
  public void setTextOffset(float _offset) { textOffset = _offset; }
  
  public void setMin(int _min) { min = _min; }
  public void setMax(int _max) { max = _max; }
  
  public void setLimits(int min, int max) {
    setMin(min);
    setMax(max);
  }
  
  public void show() {
    super.show();
    showText();
  }
  
  public void showText() {
    textAlign(leftAlign, topAlign);
    setTextSize();
    fill(fg);
    if (leftAlign == LEFT) text(contents, x + textOffset, cy - contentsPad);
    if (leftAlign == CENTER) text(contents, cx + textOffset, cy - contentsPad);
  }
  
  public void updateLength() {
    int l = getLength();
    float c = charWidth();
    if (l < min) setWidth(floor(min*c + 2*contentsPad));
    else setWidth(floor(textWidth(contents))+2*contentsPad);
  }
  
  public float charWidth() {
    setTextSize();
    return textWidth('a');
  }
}

class Button extends TextBox {
  int hbg, ubg;
  int fg, hfg, ufg;
  boolean highlighted = false;
    
  public void highlight() {
    highlighted = true;
    bg = hbg;
    fg = hfg;
  }
  
  public void unHighlight() {
    highlighted = false;
    bg = ubg;
    fg = ufg;
  }
  
  public void setColors(int _bg, int _hbg, int _fg, int _hfg) {
    ubg = bg = _bg;
    hbg = _hbg;
    ufg = fg = _fg;
    hfg = _hfg;
  }
  
  public void show() {
    if (highlightCondition()) highlight();
    else if (highlighted) unHighlight();
    super.show();
  }
  
  public boolean highlightCondition() {
    return mouseOver();
  }
}

class SliderBase extends Box {
  boolean vertical = true;
  SliderBtn slider = new SpeedBtn();

  float min = 0.05f;
  float max = 100;
  float scl = 10000;
  float inc = 10;
  float lrp = 2.75f;
  float start = 1.0f;
  
  float value = 1.0f;
  public float getValue() { return value; }
  float top;
  public float getTop() { return top; }
  
  public void setUpSlider() {
    slider.setParent(this);
    slider.updatePos();
    slider.setSize(getWidth(), getWidth());
    top = y + h - slider.getHeight();
    slider.setY(calculateY(start));
  }
  
  public void setColors(int bg, int sbg, int shbg, int sfg, int shfg) {
    setBackground(bg);
    slider.setColors(sbg, shbg, sfg, shfg);
  }
  
  public void show() {
    super.show();
    slider.show();
  }
  
  public void mousePress() {
    select();
    if (slider.mouseOver()) slide();
  }
  
  public void slide() {
    slider.drag();
  }
  
  public float calculateValue() {
    return calculateValue(slider.getY() - y);
  }
  
  public float calculateValue(float yval) {
    return value = pow(yval, lrp) / scl + min;
  }
  
  public float calculateY(float v) {
    value = v;
    return pow(scl*(v - min), 1/lrp) + y;
  }
  
  public boolean atMax() {
    return slider.getY() == y + h - slider.getHeight();
  }
}

class SliderBtn extends Button {
  public void drag() {
    super.drag();
    setOffsetX(0);
    SliderBase p = (SliderBase)parent;
    setPos(parent.getX(), constrain(mouseY/SIZE_MULT, p.getY() - dy, p.getTop() - dy));
    p.calculateValue();
  }
  
  public void release() {
    super.release();
    SliderBase p = (SliderBase)parent;
    p.deSelect();
  }
  
  public boolean highlightCondition() {
    SliderBase p = (SliderBase)parent;
    return y == p.getTop();
  }
}

class Typable extends TextBox {
  float flashTime = 15;
  float flashCount = 0;
  boolean cursorFlashing = false;
  float cursorWidth = 10;
  
  Typable() {
    setLeftAlign(LEFT);
  }
  
  public void show() {
    super.show();
    if (thisSelected) flashCursor();
  }
  
  public void select() {
    super.select();
    typing = this;
    resetFlash();
  }
  
  public void deSelect() {
    super.deSelect();
    typing = null;
  }
  
  public void flashCursor() {
    if (flashCount <= 0) {
      cursorFlashing ^= true;
      flashCount = flashTime;
    }
    flashCount--;
    if (cursorFlashing) showCursor();
  }
  
  public void resetFlash() {
    cursorFlashing = true;
    flashCount = flashTime;
  }
  
  public void showCursor() {
    int l = getLength();
    float c = charWidth();
    fill(fg);
    rect(x + l*c + contentsPad, y + contentsPad, cursorWidth, h - 2*contentsPad);
  }
    
  public void type(char c) {
    resetFlash();
    int l = getLength();
    if (c == BACKSPACE) {
      if (l > 0) contents = contents.substring(0, l-1);
    } else if (l < max) contents += c;
    updateLength();
  }
}

class PopUp extends LBox {
  float popUpWidth = 200, popUpHeight = 100;
  float triangleWidth = 20, triangleHeight = 20;
  public float getTotalWidth() { return getWidth() + triangleWidth; }
  
  float textboxX, textboxY = 20;
  float textboxWidth = 180, textboxHeight = 35;
  float labelSize = 25;
  int inputMin = 8, inputMax = 32;
  public String getText() { return textbox.getText(); }  
  
  Typable textbox = new Typable();
  
  PopUp(Box parent, String label, String t) {
    setParent(parent);
    setSize(popUpWidth, popUpHeight);
    setUpInput();
    setUpLabel(label);    
    textbox.setText(t);
    updateWidth();
  }  
   
  public void setUpInput() {
    textbox.setParent(this);
    textbox.setLimits(inputMin, inputMax);
    textbox.setSize(textboxWidth, textboxHeight);
    textbox.updateLength();
    textboxX = popUpWidth/2 - textbox.getWidth()/2;
    textbox.setOffset(textboxX, textboxY);
  }
  
  public void setUpLabel(String label) {
    setLabel(label);
    setLabelOffset(labelSize);
    setLabelSize(labelSize);
  }
  
  public void show() {
    super.show();
    textbox.show();
    pointer();
  }
  
  public void select() {
    textbox.select();
    super.select();    
  }
  
  public void deSelect() {
    textbox.deSelect();
    super.deSelect();
    control.clearPopUps(); // :P
  }
  
  public void updatePos() {
    setOffsetX(-getTotalWidth());
    setX(parent.getX());
    setCenterY(parent.getCenterY());
    textbox.updatePos();
  }
    
  public void updateWidth() {
    textbox.updateLength();
    setWidth(2*textboxX + textbox.getWidth());
    updatePos();
  }
  
  public void pointer() {
    fill(bg);
    triangle(x+w,cy-triangleHeight,x+w+triangleWidth,cy,x+w,cy+triangleHeight);
  }
}

class FTextBox extends TextBox {
  FTextBox(int n) {
    setMin(n);
    setText(0);
  }
}

class Hardware extends Draggable {
  public void run() {}
  public void reset() {}
}
class Tag extends Typable {
  float tagHeight = 30;
  float pointerMult = 0.5f;
  
  int bg = color(33, 183, 33);
  int mg = color(255);
  int hl = color(227, 232, 41);
  int particleColor = color(0, 202, 0);
  
  float textPadding = 3;
  
  public float getTotalWidth() { return w + pointer.getWidth(); }
  
  Pointer pointer;
  
  Tag(Box p) {
    setParent(p);
    setBackground(bg);
    setTextColor(mg);
    setHeight(tagHeight);
    
    setMin(3);
    setPadding(textPadding);
    setTextOffset(textPadding);
    updateLength();
    
    pointer = new Pointer(this);
  }
  
  public Tag copy() {
    Tag newTag = new Tag(null);
    newTag.setPos(x, y);
    newTag.setText(getText());
    newTag.updateWidth();
    return newTag;
  }
  
  public void setPos(float x, float y) {
    super.setPos(x, y);
    pointer.updatePos();
  }
  
  public void updateWidth() {
    updateLength();
    if (parent instanceof Block) {
      Block p = (Block)parent; 
      p.updateTag(this);
    }
    if (hasParent()) updatePos();
    pointer.setOffsetX(getWidth());
    pointer.updatePos();
  }
  
  public void show() {
    pointer.show();
    super.show();
  }
  
  public boolean mouseOver() {
    return (super.mouseOver() || pointer.mouseOver());
  }
  
  public void release() {
    tags.select(this);
    super.release();
    tags.release(this);
  }
  
  public boolean isNear(SnapPoint other) {
    return pointer.isNear(other);
  }
  
  public void detach() {
    if (parent instanceof Block) {
      Block p = (Block)parent; 
      p.detach(this);
    }
    setParent(null);
  }
  
  public int getRegisterOfTag() {
      if (parent instanceof Block) {
      Block b = (Block)parent;
      return b.getRegIndex();
    }
    return 0;
  }
  
  public Particle setUpNewParticle() {
    Particle newParticle = new Particle(nf(getRegisterOfTag(), 2));
    newParticle.setPos(cx, cy);
    newParticle.setBackground(particleColor);
    newParticle.show();
    return newParticle;
  }
  
  public ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#EmptyTag");
    state.add(str(getX()));
    state.add(str(getY()));
    return state;
  }
}

class StartTag extends Tag {
  StartTag(Box parent) {
    super(parent);
  }
  
  public void show() {
    super.show();
    flag();
  }
  
  public void flag() {
    stroke(10);
    fill(particleColor);
    float dw = 8;
    float dz = 5;
    float p = 18, q = 12;
    float theta = 0.1f;
    pushMatrix();
    translate(cx - dw, cy);
    rotate(theta);
    line(0,h/2-dz,0,dz-h/2);
    rect(0,dz-h/2,p, q);
    popMatrix();
    noStroke();
  }
  
  public void release() {
    super.release();
    deSelect();
  }
  
  public Tag copy() {
    StartTag newTag = new StartTag(null);
    newTag.setPos(x, y);
    return newTag;
  }
  
  public Script getStartingScript() {
    Block block = (Block)parent;
    if (block == null) return null;
    else return block.getScript();
  }
}
public void devMode() {
  cheekyDevMode ^= true;
}
public void devFrame() {
  devForces();
  devText();
}
public void devDraw(Box s) {
  if (s.mouseOver()) { 
    fill(0); 
    rect(s.x, s.y, 6, 6);
  }
  stroke(0);
  float r = min(s.w, s.h)/5;
  line(s.x, s.y, s.x+r, s.y      );
  line(s.x, s.y, s.x, s.y+r    );
  line(s.x+s.w, s.y, s.x+s.w-r, s.y      );
  line(s.x+s.w, s.y, s.x+s.w, s.y+r    );
  line(s.x, s.y+s.h, s.x+r, s.y+s.h  );
  line(s.x, s.y+s.h, s.x, s.y+s.h-r);
  line(s.x+s.w, s.y+s.h, s.x+s.w-r, s.y+s.h  );
  line(s.x+s.w, s.y+s.h, s.x+s.w, s.y+s.h-r);
  line(s.cx-r, s.cy, s.cx+r, s.cy  );
  line(s.cx, s.cy-r, s.cx, s.cy+r);
  noStroke();
}

public void devSnap(SnapPoint c) {
  fill(205, 100);
  ellipseMode(CENTER);
  ellipse(c.cx, c.cy, c.snapRadius, c.snapRadius);
}

public void devReticle(Particle p) {
  stroke(p.bg);
  noFill();
  //ellipse(p.target.target.getCenterX(), p.target.target.getCenterY(), p.r, p.r);
  noStroke();
}

float forceMultiplier = 150;

ArrayList<Particle> pForce = new ArrayList<Particle>();
ArrayList<PVector> fForce = new ArrayList<PVector>();

public void devForce(Particle p, PVector f) {
  pForce.add(p);
  fForce.add(f);
}

public void devForces() {
  stroke(color(255, 255, 0));
  for (int i = 0; i < fForce.size(); i++) {
    Particle p = pForce.get(i);
    PVector f = fForce.get(i);
    PVector fmult = f.copy().mult(forceMultiplier);
    float arrowHeadSize = fmult.mag()/5;
    PVector pos1 = p.pos.copy();
    PVector pos2 = PVector.add(pos1, fmult);
    float angle = atan2(fmult.y, fmult.x);
    line(pos1.x, pos1.y, pos2.x, pos2.y);
    line(pos2.x, pos2.y, (pos2.x+cos(angle+PI*0.75f)*arrowHeadSize), (pos2.y+sin(angle+PI*0.75f)*arrowHeadSize));
    line(pos2.x, pos2.y, (pos2.x+cos(angle+PI*1.25f)*arrowHeadSize), (pos2.y+sin(angle+PI*1.25f)*arrowHeadSize));
  }
  noStroke();
  pForce.clear();
  fForce.clear();
}

float devColumnWidth = 40;
float devRowHeight = 15;
int devRow = 0;

public void devText() {
  fill(200);
  textSize(devRowHeight);
  textAlign(RIGHT, DOWN);
  devRow = 0;
  devText("fps:", str((int)frameRate));
  devText("scripts size:", str(scripts.scripts.size()));
  devText("tags size:", str(tags.tags.size()));
}

public void devText(String label, String obj) {
  text(label, W - devColumnWidth, H - devRowHeight * devRow);
  text(obj, W, H - devRowHeight * devRow);
  devRow++;
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Avengers_4_1_1" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
