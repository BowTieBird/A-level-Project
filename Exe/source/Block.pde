class Block extends TextBox { 
  float blockWidth = 60;
  float blockHeight = 25;
  float textX = 9, textY = 2;
  float tagY = -8; 

  color mg = color(255);
  color hl = color(227, 232, 41);

  int baseCode;
  int min = 3;

  int regIndex;
  int getRegIndex() { return regIndex; }
  void setRegIndex(int r) { regIndex = r; }
  
  String helpText;

  float getTotalHeight() { return h + slot.getHeight(); }

  Slot slot;
  Tab tab;
  Notch notch;
  Tag tag;

  Slot getSlot() { return slot; }
  Tab getTab() { return tab; }
  Notch getNotch() { return notch; }
  Tag getTag() { return tag; }

  Block(String mnemonic, int code, color bg, String help) {
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

  Block copy() {
    Block newBlock = new Block(getText(), baseCode, bg, helpText);
    newBlock.setPos(x, y);
    newBlock.updateWidth();
    if (tag != null) newBlock.addTag(tags.addTag(tag.copy()));
    return newBlock;
  }

  void setPos(float x, float y) {
    super.setPos(x, y);
    slot.updatePos();
    tab.updatePos();
    notch.updatePos();
    if (tag != null) tag.updatePos();
  }

  void updateWidth() {
    slot.updateWidth();
  }

  void show() {
    super.show();
    slot.show();
    tab.show();
    if (tag == null) { 
      if (notch.mouseOver()) notch.show();
    } else tag.show();
  }

  boolean mouseOver() {
    return (super.mouseOver() || slot.mouseOver() || tab.mouseOver());
  }

  void assembleTag(ArrayList<Tag> blockTags) {
    assemble(0);
  }

  void assemble(int k) {
    MemoryAddress r =  ram.getRegister(regIndex);
    r.setWillHaveData(true);
    r.assemble(setUpNewParticle());
  }

  Particle setUpNewParticle() {
    Particle newParticle = new Particle(getCode());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(bg);
    return newParticle;
  }

  String getCode() { 
    return nf(baseCode, min);
  }

  Block checkForTag(Tag current) {
    if (tag == null && current.isNear(notch)) return this;
    return null;
  }

  Tag addTag(Tag current) {
    tag = current;
    tag.setParent(this);
    tag.setOffset(-tag.getTotalWidth(), tagY);
    tag.updatePos();
    return tag;
  }

  Tag makeNewTag() {
    if (tag == null && notch.mouseOver()) {      
      return tags.addTag(addTag(new Tag(this)));
    } else return null;
  }

  void updateTag(Tag t) {
    if (t == tag) tag.setOffsetX(-tag.getTotalWidth());
  }

  void detach(Tag t) {
    if (t == tag) tag = null;
  }

  void change(float s) {
    // TODO
  }

  void run() {
  }

  Script getScript() {
    return (Script)parent;
  }

  boolean hasStartTag() {
    return tag instanceof StartTag;
  }

  ArrayList<String> saveState(int k) {
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
  
  void helpOverlay() {
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
  
  String getOpText() {
    return (opTag == null) ? operand.getText() : opTag.getText();
  }

  OperandBlock(String m, int code, color bg, String help) {
    super(m, code, bg, help);
    setTextOffset(textX);
    setUpOperand();
  }

  void setUpOperand() {    
    operand.setParent(this);    
    operand.setHeight(h + slot.getHeight() - 2*opPadding);
    operand.setOffset(opOffset, opPadding - slot.getHeight());
    operand.setPadding(opPadding);
    operand.setTextOffset(opPadding);

    attach = new SnapPoint(this);
    tagAssemble = new ParticleAttractor(operand);

    updateWidth();
  }

  void setPos(float x, float y) {
    super.setPos(x, y);
    operand.updatePos();
    attach.setCenter(operand.getCenterX(), operand.getCenterY());
    if (opTag != null) opTag.updatePos();
  }

  Block copy() {
    OperandBlock newBlock = new OperandBlock(getText(), baseCode, bg, helpText);
    newBlock.setPos(x, y);
    newBlock.setOperandText(operand.getText());
    newBlock.updateWidth();
    if (tag != null) newBlock.addTag(tags.addTag(tag.copy()));
    if (opTag != null) newBlock.addOpTag(tags.addTag(opTag.copy()));
    return newBlock;
  }

  void setOperandText(String contents) {
    operand.setText(contents);
  }

  void show() {
    super.show();
    operand.show();
    if (opTag != null) opTag.show();
  }

  void select() {
    if (opTag == null) operand.select();
    super.select();
  }

  void deSelect() {
    operand.deSelect();
    super.deSelect();
  }

  void updateWidth() {
    if (opTag == null) operand.updateLength();
    else operand.setWidth(opTag.getWidth());
    setWidth(opOffset + operand.getWidth() + opPadding);
    slot.updateWidth();
    attach.setCenter(operand.getCenterX(), operand.getCenterY());
  }

  Particle setUpNewParticle() {
    Particle newParticle = super.setUpNewParticle();
    newParticle.setPos(operand.getCenterX(), operand.getCenterY());
    return newParticle;
  }

  String getCode() {
    return nf(baseCode + operand.getIntText(), min);
  }

  Tag addOpTag(Tag current) {
    opTag = current;
    opTag.setParent(this);
    opTag.setOffset(opOffset, opPadding - slot.getHeight());
    operand.setText("");
    updateWidth();
    opTag.updatePos();
    return tag;
  }

  void assembleTag(ArrayList<Tag> blockTags) {
    String op = getOpText();
    for (Tag tag : blockTags) {
      if (tag.getText().equals(op)) {
        tagAssemble.addParticle(tag.setUpNewParticle());
        return;
      }
    }
    assemble(int(op));
  }

  void assemble(int o) {
    Particle p = setUpNewParticle();
    p.setData(nf(baseCode + o, min));
    MemoryAddress r =  ram.getRegister(regIndex);
    r.setWillHaveData(true);
    r.assemble(p);
  }

  void run() {
    if (tagAssemble.gotInput()) assemble(tagAssemble.input);
  }

  OperandBlock checkAttach(Tag current) {
    if (opTag == null && current.isNear(attach)) return this;
    return null;
  }
  
  void updateTag(Tag t) {
    super.updateTag(t);
    if (t == opTag) updateWidth();
  }

  void detach(Tag t) {
    super.detach(t);
    if (t == opTag) opTag = null;
  }

  ArrayList<String> saveState(int k) {
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
