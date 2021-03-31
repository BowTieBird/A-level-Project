class Drawer extends Box {
  color fg = color(109, 113, 127);
  color mg = color(38, 40, 49);
  color bg = color(30, 35, 40);
  
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

  void update() {
    outBtn.setOffset(w, 0);
    outBtn.updatePos();
    binBtn.setOffset(w, h - btnSize);
    binBtn.updatePos();
  }

  void show() {
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

  void mousePress() {
    if (outBtn.mouseOver()) flipOut();
    else if (out) shelves.mousePress();
  }

  void flipOut() {
    out ^= true;
    outBtn.flip();
    float w = out ? maxWidth : 0;
    setWidth(w);
    update();
  }
  
  boolean toBin() { return binBtn.mouseOver(); }
  
  void scroll(float s) { if(mouseOver()) shelves.scroll(s); }
  
  Block loadNewBlock(ArrayList<String> state, int k) {
    return shelves.loadNewBlock(state, k);
  }
  
  void helpOverlay(int helpMode) {
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
    shelves.add(new OperandBlock("BRA", 600, getBlockHue(1.5)," <- BRAnch to the given address"));
    shelves.add(new OperandBlock("BRZ", 700, getBlockHue(2.5)," <- BRanch to the given address if the accumulator is Zero"));
    shelves.add(new OperandBlock("BRP", 800, getBlockHue(4),  " <- BRanch to the given address if the accumulator is Positive"));
    shelves.add(new        Block("INP", 901, getBlockHue(5),  " <- INPut a value into the accumulator"));
    shelves.add(new        Block("OUT", 902, getBlockHue(6.5)," <- OUTput the value of the accumulator"));
    shelves.add(new        Block("HLT", 000, getBlockHue(7.5)," <- HaLT the program (empty RAM address defaults to this)")); 
    shelves.add(new OperandBlock("DAT", 000, getBlockHue(8),  " <- DATa block: use for storing the value of a variable"));
    setPos();
  }
  
  Block loadNewBlock(ArrayList<String> state, int k) {
    for (Block shelf : shelves) if (state.get(6).equals(shelf.getText())) return shelf.copy();
    return null;
  }
  
  color getBlockHue(float i) {
    colorMode(HSB);
    color c = color(255*i/n, 255, 255);
    colorMode(RGB);
    return c;
  }

  void setPos() {
    float shelfY = shelfOffset - scroll;
    for (Block shelf : shelves) {
      shelf.setPos(shelfX, shelfY); 
      shelfY += shelfHeight;
    }
  }

  void show() {
    for (Block shelf : shelves) shelf.show();
  }

  void scroll(float s) {
    if (scrollMax > H) {
      scroll += s * scrollFactor;
      scroll = constrain(scroll, 0, scrollMax - H);
      setPos();
    }
  }

  void mousePress() {
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
  
  void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(LEFT, CENTER);
    for (Block shelf : shelves) { shelf.helpOverlay(); }
  }
}

class ControlPanel extends Box {
  color mg = color(38, 40, 49);
  color bg = color(30, 35, 40);
  color assembleArrowColor = color(17, 72, 166);
  color runArrowColor = color(54, 173, 55);
  color resetArrowColor = color(255,50,100);
  color sliderColor = color(109, 113, 127);
  color speedColor1 = color(8, 116, 232);
  color speedColor2 = color(10, 237, 255);
  color newColor = color(255);
  color saveColor = color(63, 188, 206);
  color loadColor = color(255, 255, 0);
  color helpColor = color(160, 32, 240);
  
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
  
  void setUpControl(Box control, int row) {
    control.setParent(this);
    control.setSize(btnSize, btnSize);
    control.setOffset(-btnSize*(1 + floor(row*(btnSize+1)/ H)), (row % floor(H/btnSize))*btnSize);
    control.updatePos();
    controls.add(control);
  }
  
  void show() {
    for (Box btn : controls) btn.show();
    if (savePopUp != null) savePopUp.show();
    if (loadPopUp != null) loadPopUp.show(); 
  }
  
  void mousePress() {    
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
  
  void selectPopUp(PopUp p) {
    p.select();
    selectedPopUp = p;
    select();
  }
  
  void clearPopUps() {
    savePopUp = null;
    loadPopUp = null;
  }
  
  void savePopUp() {
    if (savePopUp == null) {
      clearPopUps();
      savePopUp = new PopUp(saveBtn, "Save", lastSave);
      savePopUp.setBackground(mg);
      selectPopUp(savePopUp);
    } else commenceSave();
  }
   
  void loadPopUp() {
    if (loadPopUp == null) {
      clearPopUps();
      loadPopUp = new PopUp(loadBtn, "Load", lastSave);
      loadPopUp.setBackground(mg);
      selectPopUp(loadPopUp);
    } else commenceLoad();
  }
  
  void deSelect() {
    if (savePopUp != null) savePopUp.deSelect();
    if (loadPopUp != null) loadPopUp.deSelect();
    super.deSelect();
  }
  
  void type() {
    if (savePopUp != null) savePopUp.updateWidth();
    if (loadPopUp != null) loadPopUp.updateWidth();
  }
  
  void enter() {
    if (selectedPopUp == savePopUp) commenceSave();
    if (selectedPopUp == loadPopUp) commenceLoad();
  }
  
  void commenceSave() {
    lastSave = savePopUp.getText();
    clearPopUps();
    saveAllStates(lastSave);
  }
  
  void commenceLoad() {   
    lastSave = loadPopUp.getText();
    clearPopUps();
    loadAllStates(lastSave);
  }
  
  float getParticleSpeed() {
    return particleSpeedSlider.getValue();
  }
  
  boolean maxParticleSpeed() {
    return particleSpeedSlider.atMax();
  }
  
  void helpOverlay() {
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
  
  void showHelp() {
    helpBtn.show();
  }
}